import javax.swing.*;
import java.awt.*;

// Layar hasil akhir (Good / Bad Ending): background gambar, kotak hitam transparan,
// teks font pixel, dan tombol bergambar (RESTART & BACK).
// Kotak + tombol muncul dengan animasi pop-up (naik dari bawah layar) dan ukurannya mengikuti ukuran jendela.
public class GameOverPanel extends JPanel {
    // Taruh file ini di folder assets/ (background: lihat PixelUI.BACKGROUND_PATH -> "latar belakang.jpeg")
    private static final String BTN_RESTART_PATH = "assets/btn-restart.png";
    private static final String BTN_BACK_PATH = "assets/btn-back.png";
    private static final String CLICK_SFX = "assets/click_sfx_fixed.wav";

    private static final int POPUP_MS = 500;                      // lama animasi pop-up
    private static final float BASE_W = 1024f, BASE_H = 548f;     // ukuran acuan (isi jendela 1024x576 di Mac)

    private final GameEngine engine;
    private final Runnable onStateChanged;
    private final PixelUI.CoverBackground background = new PixelUI.CoverBackground(PixelUI.BACKGROUND_PATH);

    // Hasil akhir DIBEKUKAN saat panel dibuat. Kalau dibaca langsung dari engine, teksnya ikut berubah
    // begitu engine di-reset (misal saat klik RESTART, layar ini masih tampil selama fade-out).
    private final boolean good;
    private final int score;

    private final JButton btnRestart;
    private final JButton btnBack;

    // Hasil hitung tata letak (posisi "normal", tanpa offset pop-up)
    private final Rectangle card = new Rectangle();
    private final Rectangle restartBounds = new Rectangle();
    private final Rectangle backBounds = new Rectangle();
    private float scale = 1f;

    // Animasi pop-up
    private float popProgress = 0f;   // 0 = di luar layar (bawah), 1 = posisi normal
    private Timer popTimer;

    public GameOverPanel(GameEngine engine, Runnable onStateChanged) {
        this.engine = engine;
        this.onStateChanged = onStateChanged;
        this.good = engine.isGoodEnding();
        this.score = engine.getTotalScore();

        setLayout(null);
        setBackground(new Color(20, 20, 20));

        // --- TOMBOL ---
        btnRestart = PixelUI.createImageButton(BTN_RESTART_PATH, 210, "RESTART");
        btnBack = PixelUI.createImageButton(BTN_BACK_PATH, 210, "BACK");

        btnRestart.addActionListener(e -> {
            SoundManager.playSFX(CLICK_SFX);
            SoundManager.stopBGMWithFade(800); // musik ending berhenti pelan-pelan saat main lagi
            engine.startNewGame();
            onStateChanged.run();
        });

        btnBack.addActionListener(e -> {
            SoundManager.playSFX(CLICK_SFX);
            engine.returnToMainMenu();
            onStateChanged.run();
        });

        add(btnRestart);
        add(btnBack);

        // --- POP-UP: kotak + tombol naik dari bawah layar ---
        final long[] start = {0};
        popTimer = new Timer(16, null);
        popTimer.addActionListener(e -> {
            long now = System.currentTimeMillis();
            if (start[0] == 0) {
                // Suara dibunyikan sekarang, kotak baru mulai naik setelah jeda latensi suara "pop"
                start[0] = now + SoundManager.POP_SOUND_LATENCY_MS;
                SoundManager.playSFX(SoundManager.SFX_POP_UP);
            }
            popProgress = Math.max(0f, Math.min(1f, (now - start[0]) / (float) POPUP_MS));
            applyButtonPositions();
            repaint();
            if (popProgress >= 1f) ((Timer) e.getSource()).stop();
        });
        popTimer.start();
    }

    @Override
    public void removeNotify() {
        if (popTimer != null) popTimer.stop();
        super.removeNotify();
    }

    // Seberapa jauh (piksel) grup kotak+tombol masih "turun" di bawah posisi normalnya
    private int popOffset() {
        if (popProgress >= 1f) return 0;
        float eased = 1f - (float) Math.pow(1f - popProgress, 3); // ease-out
        int groupTop = card.y;
        return Math.round((1f - eased) * (getHeight() - groupTop));
    }

    private void applyButtonPositions() {
        int off = popOffset();
        btnRestart.setBounds(restartBounds.x, restartBounds.y + off, restartBounds.width, restartBounds.height);
        btnBack.setBounds(backBounds.x, backBounds.y + off, backBounds.width, backBounds.height);
    }

    // Menghitung ulang ukuran & posisi kotak + tombol setiap kali panel berubah ukuran
    @Override
    public void doLayout() {
        int W = getWidth(), H = getHeight();
        if (W <= 0 || H <= 0) return;

        float s = Math.min(W / BASE_W, H / BASE_H);
        Graphics2D g2 = PixelUI.measureGraphics();

        int cardW, cardH, btnW, btnH, gap, groupH;
        while (true) {
            cardW = Math.min(Math.round(820 * s), Math.round(W * 0.94f));
            cardH = layoutContent(g2, cardW, false, s);
            btnW = Math.round(210 * s);
            btnH = PixelUI.buttonHeight(btnRestart, btnW);
            gap = Math.round(22 * s);
            groupH = cardH + gap + btnH;
            if (groupH <= H * 0.94f || s < 0.4f) break;
            s *= 0.95f;   // kalau belum muat di layar, kecilkan sedikit
        }
        g2.dispose();
        scale = s;

        int top = (H - groupH) / 2;
        card.setBounds((W - cardW) / 2, top, cardW, cardH);

        int btnGap = Math.round(26 * s);
        int rowW = btnW * 2 + btnGap;
        int rowX = (W - rowW) / 2;
        int rowY = top + cardH + gap;
        restartBounds.setBounds(rowX, rowY, btnW, btnH);
        backBounds.setBounds(rowX + btnW + btnGap, rowY, btnW, btnH);

        applyButtonPositions();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        background.paint(g, getWidth(), getHeight());

        if (card.width <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(card.x, card.y + popOffset());
        PixelUI.paintCard(g2, card.width, card.height);
        layoutContent(g2, card.width, true, scale);
        g2.dispose();
    }

    // draw = true  -> menggambar isi kotak
    // draw = false -> cuma menghitung tinggi total (dipakai untuk menentukan ukuran kotak)
    // ts = skala teks/jarak (1.0 = ukuran acuan)
    private int layoutContent(Graphics2D g2, int w, boolean draw, float ts) {
        int minScore = GameEngine.GOOD_ENDING_MIN_SCORE;

        int cx = w / 2;
        int pad = Math.round(34 * ts);
        int innerW = w - pad * 2;
        Color accent = good ? PixelUI.GOLD : PixelUI.RED;

        String body = good
                ? "Sammy berhasil bertahan di STIS dan meraih IPK 4.00! Kerja keras, keputusan bijak, dan kerja sama tim akhirnya terbayar."
                : "Nilai Sammy anjlok dan ia terkena Drop Out... Pilihan-pilihan yang diambil selama ini ternyata berakibat fatal.";
        String hint = good
                ? "Terima kasih sudah membantu Sammy!"
                : "Mau buat Sammy bahagia? Klik restart";

        int y = Math.round(44 * ts);

        // "ENDING 1" / "ENDING 2"
        if (draw) PixelUI.drawCentered(g2, good ? "ENDING" : "ENDING", PixelUI.font(12f, ts), PixelUI.TEXT_DIM, cx, y);

        // Judul besar
        y += Math.round(50 * ts);
        if (draw) PixelUI.drawCentered(g2, good ? "GOOD ENDING" : "BAD ENDING", PixelUI.font(30f, ts), accent, cx, y);

        // Garis pemisah
        y += Math.round(22 * ts);
        if (draw) {
            g2.setColor(accent);
            g2.fillRect(cx - Math.round(130 * ts), y, Math.round(260 * ts), Math.max(2, Math.round(2 * ts)));
        }

        // Cerita singkat
        y += Math.round(40 * ts);
        Font bodyFont = PixelUI.font(12f, ts);
        int bodyLine = Math.round(22 * ts);
        if (draw) {
            y = PixelUI.drawWrapped(g2, body, bodyFont, PixelUI.TEXT, pad, y, innerW, bodyLine, true);
        } else {
            y += PixelUI.wrap(g2.getFontMetrics(bodyFont), body, innerW).size() * bodyLine;
        }

        // Skor
        y += Math.round(16 * ts);
        if (draw) PixelUI.drawCentered(g2, "TOTAL SKOR: " + score, PixelUI.font(16f, ts), PixelUI.GOLD, cx, y);

        // Keterangan syarat
        y += Math.round(32 * ts);
        Font hintFont = PixelUI.font(10f, ts);
        int hintLine = Math.round(18 * ts);
        if (draw) {
            y = PixelUI.drawWrapped(g2, hint, hintFont, PixelUI.TEXT_DIM, pad, y, innerW, hintLine, true);
        } else {
            y += PixelUI.wrap(g2.getFontMetrics(hintFont), hint, innerW).size() * hintLine;
        }

        return y + Math.round(22 * ts); // padding bawah
    }
}