import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;
import javax.swing.text.BadLocationException;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;

public class PlayingPanel extends JPanel {
    private GameEngine engine;
    private Runnable onStateChanged;
    private Image bgImage;
    private Image charImage;

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
        this.engine = engine;
        this.onStateChanged = onStateChanged;

        setLayout(new BorderLayout());

        Scene current = engine.getCurrentScene();
        if (current == null) return;

        // Load Aset Background & Karakter
        Background currentBg = current.getBackground();
        Character activeChar = engine.getActiveCharacter();

        if (currentBg != null) {
            bgImage = new ImageIcon(currentBg.getImagePath()).getImage();
        }
        if (activeChar != null) {
            charImage = new ImageIcon(activeChar.getImagePath()).getImage();
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
                JButton btnOpt = createStyledOptionButton((i + 1) + ". " + opts.get(i).getButtonText());
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

        Timer typingTimer = new Timer(20, null);
        typingTimer.addActionListener(e -> {
            if (charIndex[0] < textToType.length()) {
                try {
                    dialogDoc.insertString(dialogDoc.getLength(), String.valueOf(textToType.charAt(charIndex[0])), null);
                    dialogDoc.setParagraphAttributes(0, dialogDoc.getLength(), dialogLineSpacing, false);
                } catch (BadLocationException ignored) {
                }
                charIndex[0]++;
            } else {
                ((Timer) e.getSource()).stop();
                finishTyping.run();
            }
        });
        typingTimer.start();

        // --- SKIP ANIMASI KETIK PAKAI TOMBOL SPASI ---
        // WHEN_IN_FOCUSED_WINDOW dipakai biar tetap kepencet walau fokus lagi di komponen lain (misal tombol).
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "skipTyping");
        getActionMap().put("skipTyping", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (typingTimer.isRunning()) {
                    typingTimer.stop();
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
        dialogWrapper.setBorder(BorderFactory.createEmptyBorder(25, 0, 0, 0));

        final int[] currentOffset = {25};
        Timer slideTimer = new Timer(12, null);
        slideTimer.addActionListener(e -> {
            if (currentOffset[0] > 0) {
                currentOffset[0] -= 1;
                dialogWrapper.setBorder(BorderFactory.createEmptyBorder(currentOffset[0], 0, 0, 0));
                dialogWrapper.revalidate();
            } else {
                ((Timer) e.getSource()).stop();
            }
        });
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

    private JButton createStyledOptionButton(String text) {
        JButton btn = new JButton(text);
        btn.setFont(PixelFont.get(10f));
        btn.setForeground(Color.WHITE);
        btn.setBackground(new Color(35, 39, 42, 235));
        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        // 1. Gambar Background Scene
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }

        // 2. Gambar Karakter (Skala ~82% dari Tinggi Layar, Pas & Tidak Kekecilan/Jumbo)
        Character activeChar = engine.getActiveCharacter();
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

            g.drawImage(charImage, xPos, yPos, charWidth, charHeight, this);
        }
    }
}