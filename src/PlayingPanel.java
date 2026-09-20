import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class PlayingPanel extends JPanel {
    private GameEngine engine;
    private Runnable onStateChanged;
    private Image bgImage;
    private Image charImage;
    private Character shownChar;   // karakter yang tampil di panel ini (dibekukan saat panel dibuat)

    // --- ANIMASI POP-UP KARAKTER (naik dari bawah layar) ---
    private static final int POPUP_MS = 500;    // lama animasi pop-up karakter
    private float popProgress = 1f;             // 0 = di luar layar (bawah), 1 = posisi normal
    private Timer popTimer;
    private int visualLeadMs = 0;   // jeda antara suara dibunyikan dan gambar (sprite/kotak/teks) mulai muncul
    private final List<Object> cueHandles = new ArrayList<>();   // suara-suara baris ini yang sedang berbunyi
    private boolean typingSoundActive = false; // true selama panel INI yang menyalakan suara mengetik
    private Timer typingTimerRef;   // supaya bisa dihentikan kalau panel dibuang sebelum teks selesai mengetik

    // --- ANIMASI KETIK + SUARA (text_effect.wav) ---
    private static final int MS_PER_CHAR = 20;               // kecepatan ketik dasar (ms per huruf)
    // true  = durasi ketik disesuaikan dengan panjang suara: teks selesai tepat di akhir putaran suara,
    //         dan kalau teksnya lebih panjang dari suara, suara diulang (loop).
    // false = kecepatan ketik tetap MS_PER_CHAR; suara diulang/dipotong mengikuti lama teks.
    private static final boolean SYNC_TYPING_TO_SOUND = true;
    private static final int MIN_TYPING_SOUND_MS = 120;      // teks yang mengetiknya lebih singkat dari ini tidak diberi suara
    private static final int TYPING_SOUND_FADE_MS = 30;      // fade-out singkat saat suara dihentikan

    // Class khusus untuk container dialog dengan background menyatu
    private static class RoundedDialogPanel extends JPanel {
        private Color bgColor = new Color(20, 20, 20, 230);

        public RoundedDialogPanel() {
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bgColor);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.setColor(new Color(255, 255, 255, 60));
            g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // Class khusus untuk panel opsi dengan efek Fade-In
    private static class FadePanel extends JPanel {
        private float alpha = 0.0f;

        public FadePanel() {
            setOpaque(false);
        }

        public void setAlpha(float alpha) {
            this.alpha = Math.max(0.0f, Math.min(1.0f, alpha));
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintComponent(g2);
            g2.dispose();
        }

        @Override
        protected void paintChildren(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            super.paintChildren(g2);
            g2.dispose();
        }
    }

    public PlayingPanel(GameEngine engine, Runnable onStateChanged) {
        this(engine, onStateChanged, 0);
    }

    // introDelayMs = jeda sebelum animasi (pop-up karakter, ketik teks, slide dialog) dimulai.
    // Dipakai saat layar cerita lagi fade-in dari transisi DAY, supaya animasinya mulai SETELAH fade selesai.
    public PlayingPanel(GameEngine engine, Runnable onStateChanged, int introDelayMs) {
        this.engine = engine;
        this.onStateChanged = onStateChanged;

        setLayout(new BorderLayout());

        Scene current = engine.getCurrentScene();
        if (current == null) return;

        // Load Aset Background & Karakter
        Background currentBg = current.getBackground();
        Character activeChar = engine.getActiveCharacter();

        if (currentBg != null) {
            bgImage = PixelUI.loadImageSafely(currentBg.getImagePath(), "latar \"" + currentBg.getName() + "\"");
        }
        if (activeChar != null) {
            charImage = PixelUI.loadImageSafely(activeChar.getImagePath(), "karakter " + activeChar.getName());
            shownChar = activeChar;
        }

        // --- POP-UP KARAKTER: naik dari bawah layar selama POPUP_MS ---
        // (consumeCharacterPopup() selalu dipanggil supaya engine bisa mencatat karakter terakhir)
        boolean playPopUp = engine.consumeCharacterPopup();
        final boolean hasPop = playPopUp && charImage != null;
        if (hasPop) {
            popProgress = 0f;
            final long[] popStart = {0};
            final boolean[] popSoundDone = {false};
            popTimer = new Timer(16, null);
            if (introDelayMs > 0) popTimer.setInitialDelay(introDelayMs);
            popTimer.addActionListener(e -> {
                long now = System.currentTimeMillis();
                if (popStart[0] == 0) {
                    // Sprite mulai naik setelah visualLeadMs; suara "pop" dibunyikan lebih dulu sebesar latensinya
                    // sendiri (POP_SOUND_LATENCY_MS) supaya TERDENGAR tepat saat sprite mulai naik.
                    popStart[0] = now + visualLeadMs;
                }
                if (!popSoundDone[0] && now >= popStart[0] - SoundManager.POP_SOUND_LATENCY_MS) {
                    popSoundDone[0] = true;
                    SoundManager.playSFX(SoundManager.SFX_POP_UP);
                }
                popProgress = Math.max(0f, Math.min(1f, (now - popStart[0]) / (float) POPUP_MS));
                repaint();
                if (popProgress >= 1f) ((Timer) e.getSource()).stop();
            });
            popTimer.start();
        }

        // --- HEADER BAR (ATAS LAYAR) ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(new Color(0, 0, 0, 200));
        headerPanel.setBorder(BorderFactory.createEmptyBorder(8, 15, 8, 15));

        JLabel lblTitle = new JLabel(current.getTitle());
        lblTitle.setForeground(Color.WHITE);
        lblTitle.setFont(PixelFont.get(11f));

        JLabel lblScore = new JLabel("Skor: " + engine.getTotalScore() + "  ");
        lblScore.setForeground(new Color(255, 234, 167));
        lblScore.setFont(PixelFont.get(11f));

        headerPanel.add(lblTitle, BorderLayout.WEST);
        headerPanel.add(lblScore, BorderLayout.EAST);

        // --- CONTAINER BAWAH (DIALOG BOX + NAME TAG + OPTIONS) ---
        JPanel bottomContainer = new JPanel(new GridBagLayout());
        bottomContainer.setOpaque(false);
        bottomContainer.setBorder(BorderFactory.createEmptyBorder(0, 0, 20, 0));

        JPanel dialogWrapper = new JPanel();
        dialogWrapper.setLayout(new BoxLayout(dialogWrapper, BoxLayout.Y_AXIS));
        dialogWrapper.setOpaque(false);

// 1. KOTAK NAMA (NAME TAG BOX)
        String rawDialog = engine.getActiveDialogText();
        String speakerName = "";
        String dialogBody = rawDialog != null ? rawDialog : "";

        boolean hasSpeaker = false;

        if (rawDialog != null && rawDialog.contains(":")) {
            String[] parts = rawDialog.split(":", 2);
            speakerName = parts[0].trim();
            dialogBody = parts[1].trim();
            if (dialogBody.startsWith("\"") && dialogBody.endsWith("\"")) {
                dialogBody = dialogBody.substring(1, dialogBody.length() - 1);
            }
            hasSpeaker = true;
        }

// Inisialisasi namePanel cukup 1 kali saja di sini!
        JPanel namePanel = new JPanel();

        if (hasSpeaker) {
            // POSISI KOTAK NAMA: Sammy di KIRI, Tokoh Lain (Thania, Dhito, Pak Ibnu, Ka Nela, dll) di KANAN
            int alignment = speakerName.equalsIgnoreCase("Sammy") ? FlowLayout.LEFT : FlowLayout.RIGHT;

            namePanel.setLayout(new FlowLayout(alignment, 0, 0));
            namePanel.setOpaque(false);
            namePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
            namePanel.setMaximumSize(new Dimension(1100, 30));

            JLabel lblName = new JLabel(" " + speakerName + " ");
            lblName.setFont(PixelFont.get(10f));
            lblName.setForeground(Color.WHITE);
            lblName.setBackground(new Color(45, 52, 54, 240));
            lblName.setOpaque(true);
            lblName.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(200, 200, 200, 100), 1),
                    BorderFactory.createEmptyBorder(10, 18, 10, 18)
            ));
            namePanel.add(lblName);
        } else {
            // Sembunyikan namePanel kalau tidak ada speaker (misal cuma sound effect/narasi)
            namePanel.setVisible(false);
        }

        // 2. KOTAK DIALOG (DIALOG BOX)
        RoundedDialogPanel dialogBoxPanel = new RoundedDialogPanel();
        dialogBoxPanel.setLayout(new BorderLayout());

        JTextPane txtDialog = new JTextPane();
        txtDialog.setFont(PixelFont.get(11f));
        txtDialog.setForeground(new Color(240, 240, 240));
        txtDialog.setOpaque(false);
        txtDialog.setEditable(false);

        // JTextPane otomatis word-wrap; jarak antar baris diatur lewat paragraph attribute di bawah.
        StyledDocument dialogDoc = txtDialog.getStyledDocument();
        SimpleAttributeSet dialogLineSpacing = new SimpleAttributeSet();
        StyleConstants.setLineSpacing(dialogLineSpacing, 0.4f); // jarak antar baris sedikit dilebarkan
        StyleConstants.setForeground(dialogLineSpacing, new Color(240, 240, 240));
        dialogDoc.setParagraphAttributes(0, dialogDoc.getLength(), dialogLineSpacing, false);

        JScrollPane scrollDialog = new JScrollPane(txtDialog);
        scrollDialog.setOpaque(false);
        scrollDialog.getViewport().setOpaque(false);
        scrollDialog.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        dialogBoxPanel.add(scrollDialog, BorderLayout.CENTER);

        Dimension dialogDim = new Dimension(1100, 100);
        dialogBoxPanel.setPreferredSize(dialogDim);
        dialogBoxPanel.setMinimumSize(dialogDim);
        dialogBoxPanel.setMaximumSize(dialogDim);
        dialogBoxPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // 3. PANEL TOMBOL OPSIONAL / NAVIGASI (DENGAN FADE-IN)
        FadePanel optionsPanel = new FadePanel();
        optionsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        optionsPanel.setVisible(false);

        boolean hasOptions = engine.isLastDialogInScene() && current.hasOptions();

        if (hasOptions) {
            optionsPanel.setLayout(new GridLayout(3, 1, 6, 6));
            optionsPanel.setPreferredSize(new Dimension(1100, 120));
            optionsPanel.setMaximumSize(new Dimension(1100, 120));

            List<Option> opts = current.getOptions();
            for (int i = 0; i < opts.size(); i++) {
                final int idx = i;
                JButton btnOpt = createStyledOptionButton(opts.get(i).getButtonText());
                btnOpt.addActionListener(e -> {
                    engine.chooseOption(idx);
                    onStateChanged.run();
                });
                optionsPanel.add(btnOpt);
            }
        } else {
            optionsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            optionsPanel.setPreferredSize(new Dimension(1100, 42));
            optionsPanel.setMaximumSize(new Dimension(1100, 42));

            JButton btnNext = createStyledOptionButton("Lanjut >");
            btnNext.setPreferredSize(new Dimension(140, 40));
            btnNext.setFont(PixelFont.get(10f));
            btnNext.addActionListener(e -> {
                engine.nextDialogOrScene();
                onStateChanged.run();
            });
            optionsPanel.add(btnNext);
        }

        // --- ANIMASI TEKS MENGETIK + DELAY BEDAKAN (OPSI vs LANJUT) ---
        final String textToType = dialogBody;
        final int[] charIndex = {0};

        // Jika opsi pilihan: 1500ms (1.5 detik). Jika tombol lanjut: 500ms (0.5 detik)
        int optionDelay = hasOptions ? 1500 : 500;

        // Logika "sesudah teks selesai muncul" dipisah jadi method sendiri (finishTyping),
        // dipanggil baik saat animasi ketik kelar NORMAL, maupun saat di-skip pakai tombol spasi.
        Runnable finishTyping = () -> {
            Timer delayTimer = new Timer(optionDelay, delayEvent -> {
                ((Timer) delayEvent.getSource()).stop();
                optionsPanel.setVisible(true);

                final float[] alpha = {0.0f};
                Timer fadeTimer = new Timer(20, null);
                fadeTimer.addActionListener(fadeEvent -> {
                    alpha[0] += 0.08f; // Fade-in sedikit lebih cepat & mulus
                    if (alpha[0] >= 1.0f) {
                        optionsPanel.setAlpha(1.0f);
                        ((Timer) fadeEvent.getSource()).stop();
                    } else {
                        optionsPanel.setAlpha(alpha[0]);
                    }
                });
                fadeTimer.start();
            });
            delayTimer.setRepeats(false);
            delayTimer.start();
        };

        // Animasi ketik berbasis WAKTU (bukan per-tick), supaya durasinya bisa dipatok persis
        // dengan panjang suara. Suara text_effect dimulai bersamaan dengan huruf pertama.
        final int textLen = textToType.length();

        // Efek suara khusus baris ini (misal "tap tap" lalu "bukkk"): diputar berurutan dan teks mengetik
        // selama total durasinya, jadi tulisan & bunyinya jalan bareng. Suara mengetik biasa dimatikan di baris ini.
        Dialog activeDialog = engine.getActiveDialog();
        final String[] cues = (activeDialog != null) ? activeDialog.getSounds() : new String[0];
        final long[] cueOffset = new long[cues.length];   // kapan tiap suara mulai (ms sejak huruf pertama)
        long cueSum = 0;
        for (int i = 0; i < cues.length; i++) {
            cueOffset[i] = cueSum;
            cueSum += Math.max(0, SoundManager.getDurationMs(cues[i]));
        }
        final String[] effects = (activeDialog != null) ? activeDialog.getEffects() : new String[0];
        final boolean hasCues = cues.length > 0 && cueSum > 0;
        final int typingTotalMs = hasCues ? (int) cueSum : typingDurationMs(textLen);
        final boolean typingHasSound = !hasCues && typingTotalMs >= MIN_TYPING_SOUND_MS;
        final long[] typeStart = {0};
        final boolean[] typingSoundStarted = {false};
        final int[] cueNext = {hasCues ? 0 : cues.length};
        final boolean[] effectsPlayed = {effects.length == 0};

        // Jeda sampai gambar muncul = latensi suara yang paling besar di antara suara-suara di layar ini
        // (suara pop kalau ada sprite yang pop-up, suara mengetik kalau teksnya cukup panjang untuk diberi suara).
        // Suara yang latensinya lebih kecil dibunyikan sedikit lebih lambat supaya semuanya terdengar bareng gambar.
        visualLeadMs = Math.max(hasPop ? SoundManager.POP_SOUND_LATENCY_MS : 0,
                (typingHasSound || hasCues || effects.length > 0) ? SoundManager.TEXT_SOUND_LATENCY_MS : 0);

        Timer typingTimer = new Timer(15, null);
        typingTimer.addActionListener(e -> {
            long now = System.currentTimeMillis();
            if (typeStart[0] == 0) {
                typeStart[0] = now + visualLeadMs; // huruf pertama muncul setelah visualLeadMs
            }
            long elapsed = now - typeStart[0];

            // Suara mengetik dibunyikan TEXT_SOUND_LATENCY_MS sebelum huruf pertama, supaya terdengar bareng
            if (typingHasSound && !typingSoundStarted[0] && elapsed >= -SoundManager.TEXT_SOUND_LATENCY_MS) {
                typingSoundStarted[0] = true;
                SoundManager.startLoopSFX(SoundManager.SFX_TEXT_EFFECT);
                typingSoundActive = true;
            }
            // Efek "sekali bunyi" di awal baris (dibiarkan berbunyi sampai habis, tidak ikut dibatalkan saat skip)
            if (!effectsPlayed[0] && elapsed >= -SoundManager.TEXT_SOUND_LATENCY_MS) {
                effectsPlayed[0] = true;
                for (String fx : effects) SoundManager.playSFX(fx);
            }
            // Efek suara khusus baris ini: tiap suara mulai tepat saat suara sebelumnya selesai
            while (cueNext[0] < cues.length && elapsed >= cueOffset[cueNext[0]] - SoundManager.TEXT_SOUND_LATENCY_MS) {
                float gain = (activeDialog != null) ? activeDialog.getSoundGain(cueNext[0]) : 1.0f;
                cueHandles.add(SoundManager.playSFXHandle(cues[cueNext[0]], gain));
                cueNext[0]++;
            }
            // ...dan dihentikan sebesar latensi yang sama sebelum huruf terakhir, supaya berhentinya juga bareng
            if (typingSoundActive && elapsed >= typingTotalMs - SoundManager.TEXT_SOUND_LATENCY_MS) {
                SoundManager.stopLoopSFX(TYPING_SOUND_FADE_MS);
                typingSoundActive = false;
            }

            // Berapa huruf yang seharusnya sudah muncul pada saat ini
            // (dibulatkan ke atas: huruf pertama langsung muncul di awal, bukan menunggu 1 "jatah" waktu dulu -
            //  penting untuk baris dengan efek suara panjang yang jatah per hurufnya bisa ratusan ms)
            int target = typingTotalMs <= 0 ? textLen
                    : (elapsed < 0 ? 0 : (int) Math.min(textLen, Math.ceil(elapsed * (double) textLen / typingTotalMs)));
            if (target > charIndex[0]) {
                try {
                    dialogDoc.insertString(dialogDoc.getLength(), textToType.substring(charIndex[0], target), null);
                    dialogDoc.setParagraphAttributes(0, dialogDoc.getLength(), dialogLineSpacing, false);
                } catch (BadLocationException ignored) {
                }
                charIndex[0] = target;
            }

            if (elapsed >= typingTotalMs) {
                ((Timer) e.getSource()).stop();
                if (typingSoundActive) {
                    SoundManager.stopLoopSFX(TYPING_SOUND_FADE_MS);
                    typingSoundActive = false;
                }
                finishTyping.run();
            }
        });
        if (introDelayMs > 0) typingTimer.setInitialDelay(introDelayMs);
        typingTimerRef = typingTimer;
        typingTimer.start();

        // --- SKIP ANIMASI KETIK PAKAI TOMBOL SPASI ---
        // WHEN_IN_FOCUSED_WINDOW dipakai biar tetap kepencet walau fokus lagi di komponen lain (misal tombol).
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "skipTyping");
        getActionMap().put("skipTyping", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (typingTimer.isRunning()) {
                    typingTimer.stop();
                    if (typingSoundActive) {
                        SoundManager.stopLoopSFX(TYPING_SOUND_FADE_MS);
                        typingSoundActive = false;
                    }
                    // Skip = lewati animasinya, termasuk efek suara baris ini (yang belum berbunyi dibatalkan)
                    cueNext[0] = cues.length;
                    stopCues(80);
                    try {
                        dialogDoc.remove(0, dialogDoc.getLength());
                        dialogDoc.insertString(0, textToType, null);
                        dialogDoc.setParagraphAttributes(0, dialogDoc.getLength(), dialogLineSpacing, false);
                    } catch (BadLocationException ignored) {
                    }
                    charIndex[0] = textToType.length();
                    finishTyping.run();
                }
            }
        });

        dialogWrapper.add(namePanel);
        dialogWrapper.add(dialogBoxPanel);
        dialogWrapper.add(Box.createRigidArea(new Dimension(0, 10)));
        dialogWrapper.add(optionsPanel);

        // --- ANIMASI KOTAK DIALOG MUNCUL HALUS (SLIDE-UP EFFECT) ---
        // Kotak dialog (beserta name tag & tombol) disembunyikan dulu dan baru muncul TEPAT saat sprite mulai
        // naik dan huruf pertama mulai mengetik (yaitu setelah jeda kompensasi latensi audio),
        // jadi ketiganya muncul bareng, sementara suaranya sudah dibunyikan lebih dulu.
        dialogWrapper.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));
        dialogWrapper.setVisible(false);

        final int[] currentOffset = {25};
        Timer slideTimer = new Timer(12, null);
        slideTimer.addActionListener(e -> {
            if (!dialogWrapper.isVisible()) dialogWrapper.setVisible(true);
            if (currentOffset[0] > 0) {
                currentOffset[0] -= 1;
                dialogWrapper.setBorder(BorderFactory.createEmptyBorder(currentOffset[0], 0, 0, 0));
                dialogWrapper.revalidate();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
        // (+16 ms = jeda tick pertama timer pop-up & mengetik, supaya kotak, sprite, dan huruf pertama benar-benar serentak)
        slideTimer.setInitialDelay(introDelayMs + visualLeadMs + 16);
        slideTimer.start();

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.CENTER;
        bottomContainer.add(dialogWrapper, gbc);

        add(headerPanel, BorderLayout.NORTH);
        add(bottomContainer, BorderLayout.SOUTH);
    }

    // Jeda (ms) antara suara dibunyikan dan gambar layar ini mulai muncul. Dipakai StisVisualNovel untuk
    // menunda pergantian layar selama itu (lihat renderScreen).
    public int getVisualLeadMs() {
        return visualLeadMs;
    }

    // Lama animasi ketik untuk teks sepanjang textLen huruf.
    // Kalau SYNC_TYPING_TO_SOUND: dibulatkan ke kelipatan panjang suara text_effect (1x, 2x, 3x ...),
    // jadi suara selalu selesai pas saat huruf terakhir muncul. Teks yang sangat pendek (kurang dari setengah
    // panjang suara) tetap mengetik dengan kecepatan normal dan suaranya dipotong di akhir teks.
    private static int typingDurationMs(int textLen) {
        int natural = textLen * MS_PER_CHAR;
        if (!SYNC_TYPING_TO_SOUND) return natural;

        long soundMs = SoundManager.getDurationMs(SoundManager.SFX_TEXT_EFFECT);
        if (soundMs <= 0 || natural < soundMs / 2) return natural; // suara tidak ada / teks sangat pendek

        int loops = Math.max(1, Math.round((float) natural / soundMs));
        return (int) (loops * soundMs);
    }

    // Menghentikan (fade-out) efek-efek suara baris dialog ini yang masih berbunyi
    private void stopCues(int fadeMs) {
        for (Object h : cueHandles) SoundManager.stopSFX(h, fadeMs);
        cueHandles.clear();
    }

    // Tombol "Lanjut" & pilihan jawaban. Efek hover "seperti ditekan" ada di PixelUI.PressButton.
    private JButton createStyledOptionButton(String text) {
        JButton btn = new PixelUI.PressButton(text);
        btn.setFont(PixelFont.get(10f));
        return btn;
    }

    @Override
    public void removeNotify() {
        if (popTimer != null) popTimer.stop(); // jaga-jaga: panel dibuang sebelum animasi selesai
        if (typingTimerRef != null) typingTimerRef.stop();
        stopCues(30);
        // Pastikan suara mengetik tidak "nyangkut" - tapi HANYA kalau panel ini yang menyalakannya.
        // (Kalau tidak dicek, panel lama yang dibuang bisa mematikan suara mengetik milik panel baru.)
        if (typingSoundActive) {
            SoundManager.stopLoopSFX(TYPING_SOUND_FADE_MS);
            typingSoundActive = false;
        }
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 1. Gambar Background Scene
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }

        // 2. Gambar Karakter (Skala ~82% dari Tinggi Layar, Pas & Tidak Kekecilan/Jumbo)
        // Pakai karakter yang DIBEKUKAN saat panel dibuat (shownChar), BUKAN engine.getActiveCharacter().
        // Kalau dibaca langsung dari engine, sprite bakal loncat ke sisi lain saat engine sudah pindah scene
        // tapi panel ini masih tampil (misal selama fade-out ke layar DAY).
        Character activeChar = shownChar;
        if (charImage != null && activeChar != null) {
            // Skala 82% dari tinggi panel (responsif saat window di-resize)
            int charHeight = (int) (getHeight() * 0.82);
            int charWidth = (int) (charHeight * 0.85); // Menjaga rasio lebar gambar

            String activeCharName = activeChar.getName();

            // Sammy di KIRI, Karakter Lain (Thania, Dhito, Pak Ibnu, Ka Nela) di KANAN
            int xPos = (activeCharName != null && activeCharName.equalsIgnoreCase("Sammy"))
                    ? 50
                    : getWidth() - charWidth - 50;

            // Diturunkan sedikit (+30) agar bagian croppingan bawahnya tertutup rapi di balik dialog box
            int yPos = getHeight() - charHeight + 30;

            // Pop-up: sprite mulai dari luar layar (bawah) lalu naik pelan-pelan (ease-out, makin akhir makin pelan)
            if (popProgress < 1f) {
                float eased = 1f - (float) Math.pow(1f - popProgress, 3);
                yPos += (int) ((1f - eased) * charHeight);
            }

            g.drawImage(charImage, xPos, yPos, charWidth, charHeight, this);
        }
    }
}