import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class StisVisualNovel extends JFrame {
    // --- TRANSISI FADE ANTARA LAYAR "DAY" DAN LAYAR CERITA/PERMAINAN ---
    // Layar cerita -> DAY : fade-out selama FADE_OUT_MS (jadi gelap)
    // DAY -> layar cerita : fade-in selama FADE_IN_MS (dari gelap)
    // Total sekitar 2 detik (1 detik + 1 detik). Ubah dua angka ini kalau mau lebih cepat/lambat.
    private static final int FADE_OUT_MS = 1000;
    private static final int FADE_IN_MS = 1000;
    private static final Color FADE_COLOR = new Color(12, 12, 16); // sama dengan warna latar DayTransitionPanel

    private GameEngine engine = new GameEngine();
    private JPanel mainContainer;

    private final FadeGlass fadeGlass = new FadeGlass();
    private GameState shownState = null;   // layar yang SEDANG tampil
    private boolean fadingOut = false;     // true selama fade-out berjalan
    private boolean exiting = false;       // true begitu urutan keluar (gambar penutup) dimulai
    private boolean swapPending = false;   // true selama menunggu jeda kompensasi latensi audio (lihat renderScreen)

    public StisVisualNovel() {
        setTitle("STIS Survival Story");
        setSize(1024, 576); // Resolusi Standar 16:9
        // Tombol tutup jendela TIDAK langsung mematikan program: tampilkan dulu gambar penutup (fade-in/out)
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                beginExit();
            }
        });
        installQuitHandler();
        setLocationRelativeTo(null);
        mainContainer = new JPanel(new CardLayout());
        add(mainContainer);

        setGlassPane(fadeGlass); // lapisan gelap untuk efek fade (di atas semua layar)

        // Laporkan di console kalau ada nama file aset yang tidak cocok dengan isi folder assets
        SoundManager.checkAssets(PixelUI.BACKGROUND_PATH, "assets/ending.jpeg", "assets/btn-restart.png", "assets/btn-back.png");

        // "Pemanasan" suara efek lebih awal supaya suara pertama tidak terlambat
        SoundManager.preload(SoundManager.SFX_TEXT_EFFECT, SoundManager.SFX_POP_UP,
                SoundManager.SFX_OPTION_BEST, SoundManager.SFX_OPTION_RISKY, SoundManager.SFX_OPTION_BAD,
                "assets/click_sfx_fixed.wav", "assets/sfx_buk.wav", "assets/sfx_win.wav", "assets/sfx_lose.wav",
                SoundManager.SFX_SAMMY_LARI, SoundManager.SFX_SAMMY_JATUH, SoundManager.SFX_SAMMY_KEJEDOT,
                SoundManager.SFX_CORRECT, SoundManager.SFX_INCORRECT, SoundManager.SFX_SLURP);

        renderScreen();
    }

    // Method untuk mengganti layar secara dinamis berdasarkan GameState.
    // Perpindahan dari/ke layar DAY dibungkus efek fade; perpindahan lain langsung ganti.
    // Memulai urutan keluar: gambar penutup (fade-in, tahan, fade-out), lalu program ditutup.
    // Dipanggil dari tutup jendela, Cmd+Q, dan tombol Exit (lewat engine.requestExit()).
    public void beginExit() {
        if (exiting) return;
        engine.requestExit();
        renderScreen();
    }

    private void startExitSequence() {
        if (exiting) return;
        exiting = true;
        fadingOut = false;
        swapPending = false;

        SoundManager.stopBGMWithFade(2000);   // musik & ambience memudar selama urutan penutup
        SoundManager.stopLoopSFX(100);

        // Layar yang sedang tampil meredup dulu ke gelap (ini juga membatalkan fade/penundaan yang mungkin sedang jalan),
        // lalu gambar penutup muncul dengan fade-in
        fadeGlass.fade(fadeGlass.currentAlpha(), 1f, 500, () -> {
            showScreen(GameState.EXITING, 0);
            fadeGlass.hideNow();
        });
    }

    private void exitNow() {
        dispose();
        System.exit(0);
    }

    // Cmd+Q di macOS juga diarahkan ke urutan penutup (kalau platformnya mendukung)
    private void installQuitHandler() {
        try {
            if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.APP_QUIT_HANDLER)) {
                Desktop.getDesktop().setQuitHandler((e, response) -> {
                    response.cancelQuit();
                    SwingUtilities.invokeLater(this::beginExit);
                });
            }
        } catch (Exception | Error ignored) {
            // platform tidak mendukung: abaikan
        }
    }

    public void renderScreen() {
        if (engine.getCurrentState() == GameState.EXITING) {
            startExitSequence();
            return;
        }
        if (exiting || fadingOut || swapPending) return; // lagi keluar / fade-out / menunggu pergantian layar; jangan diganggu

        GameState next = engine.getCurrentState();
        boolean enteringDay = next == GameState.DAY_TRANSITION
                && shownState != null && shownState != GameState.DAY_TRANSITION;
        boolean leavingDay = shownState == GameState.DAY_TRANSITION
                && next != GameState.DAY_TRANSITION;

        if (enteringDay) {
            // Game baru dimulai (dari Menu Utama atau layar ending): musik menu / ending / sisa musik mini game
            // dihentikan DI SINI JUGA, terlepas dari apa yang dilakukan tombol di layar sebelumnya.
            if (shownState == GameState.MAIN_MENU || shownState == GameState.GAME_OVER) {
                SoundManager.stopBGMWithFade(600);
            }

            // Layar lama meredup dulu sampai gelap, baru diganti layar DAY
            fadingOut = true;
            fadeGlass.fade(0f, 1f, FADE_OUT_MS, () -> {
                fadingOut = false;
                showScreen(engine.getCurrentState(), 0);
                fadeGlass.hideNow(); // layar DAY sendiri gelap, jadi pergantiannya mulus
            });
        } else if (leavingDay) {
            // Layar cerita langsung dipasang di balik lapisan gelap, lalu lapisan gelapnya memudar.
            // Animasi intro layar cerita (pop-up karakter, ketik teks) ditunda sampai fade selesai.
            showScreen(next, FADE_IN_MS);
            fadeGlass.fade(1f, 0f, FADE_IN_MS, fadeGlass::hideNow);
        } else if (shownState == GameState.PLAYING && next == GameState.PLAYING) {
            // Lanjut ke dialog / pilihan berikutnya. Layar baru DIBUAT sekarang (jadi suaranya mulai diatur),
            // tapi layar lama dibiarkan tampil selama jeda latensi audio layar baru itu, baru diganti.
            // Dengan begitu background, sprite, kotak dialog, dan teks berganti BARENG, tepat saat suaranya terdengar.
            final JPanel incoming = buildScreen(next, 0);
            final int lead = (incoming instanceof PlayingPanel) ? ((PlayingPanel) incoming).getVisualLeadMs() : 0;
            if (lead > 0) {
                swapPending = true;
                // Selama menunggu, klik & keyboard ke layar lama diabaikan (supaya "Lanjut" tidak bisa kepencet dobel)
                KeyboardFocusManager.getCurrentKeyboardFocusManager().clearGlobalFocusOwner();
                fadeGlass.blockInput(lead, () -> {
                    swapPending = false;
                    installScreen(next, incoming);
                });
            } else {
                installScreen(next, incoming);
            }
        } else {
            showScreen(next, 0);
        }
    }

    private void showScreen(GameState state, int introDelayMs) {
        installScreen(state, buildScreen(state, introDelayMs));
    }

    // Membuat panel untuk sebuah state (belum dipasang ke layar)
    private JPanel buildScreen(GameState state, int introDelayMs) {
        switch (state) {
            case MAIN_MENU:
                return new MainMenuPanel(engine, this::renderScreen);
            case HOW_TO_PLAY:
                return new HowToPlayPanel(engine, this::renderScreen);
            case DAY_TRANSITION:
                // Layar hitam transisi scene muncul di sini: ambience scene berikutnya dimulai / dihentikan.
                // (DayTransitionPanel juga memanggilnya; aman dipanggil dua kali karena hasilnya sama.)
                engine.startSceneAmbience();
                return new DayTransitionPanel(engine, this::renderScreen);
            case PLAYING:
                return new PlayingPanel(engine, this::renderScreen, introDelayMs);
            case MINI_GAME:
                return new MiniGamePanel(engine, this::renderScreen);
            case GAME_OVER:
                return new GameOverPanel(engine, this::renderScreen);
            case EXITING:
                return new ExitPanel(this::exitNow);
            default:
                return new JPanel();
        }
    }

    // Memasang panel ke layar (mengganti panel yang sedang tampil)
    private void installScreen(GameState state, JPanel panel) {
        mainContainer.removeAll();
        mainContainer.add(panel);

        shownState = state;
        mainContainer.revalidate();
        mainContainer.repaint();
    }

    // Lapisan kaca transparan di atas seluruh layar: digambar gelap dengan alpha yang dianimasikan.
    // Selama terlihat, klik mouse diblokir supaya nggak tembus ke tombol di layar bawahnya.
    private static class FadeGlass extends JComponent {
        private float alpha = 0f;
        private Timer timer;

        FadeGlass() {
            setOpaque(false);
            addMouseListener(new MouseAdapter() {});         // "menelan" klik
            addMouseMotionListener(new MouseMotionAdapter() {});
        }

        void fade(float from, float to, int durationMs, Runnable onDone) {
            if (timer != null) timer.stop();
            alpha = from;
            setVisible(true);
            repaint();

            final long start = System.currentTimeMillis();
            timer = new Timer(16, null);
            timer.addActionListener(e -> {
                float t = Math.min(1f, (System.currentTimeMillis() - start) / (float) durationMs);
                float smooth = t * t * (3f - 2f * t); // smoothstep: pelan di awal & akhir
                alpha = from + (to - from) * smooth;
                repaint();
                if (t >= 1f) {
                    ((Timer) e.getSource()).stop();
                    if (onDone != null) onDone.run();
                }
            });
            timer.start();
        }

        // Cuma memblokir klik selama durationMs (tidak menggambar apa-apa), lalu menjalankan onDone
        void blockInput(int durationMs, Runnable onDone) {
            if (timer != null) timer.stop();
            alpha = 0f;
            setVisible(true);
            timer = new Timer(Math.max(1, durationMs), e -> {
                ((Timer) e.getSource()).stop();
                hideNow();
                if (onDone != null) onDone.run();
            });
            timer.setRepeats(false);
            timer.start();
        }

        float currentAlpha() {
            return isVisible() ? alpha : 0f;
        }

        void hideNow() {
            if (timer != null) timer.stop();
            alpha = 0f;
            setVisible(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
            g2.setColor(FADE_COLOR);
            g2.fillRect(0, 0, getWidth(), getHeight());
            g2.dispose();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StisVisualNovel().setVisible(true));
    }
}