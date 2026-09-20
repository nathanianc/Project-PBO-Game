import javax.swing.*;
import java.awt.*;

// Layar "Cara Bermain": background gambar, kotak hitam transparan (nggak memenuhi layar),
// isi petunjuk pakai font pixel, dan tombol BACK bergambar di kanan bawah.
// Ukuran kotak & tulisan otomatis menyesuaikan ukuran jendela (ikut membesar kalau jendela di-maximize).
public class HowToPlayPanel extends JPanel {
    // Taruh file ini di folder assets/ (background: lihat PixelUI.BACKGROUND_PATH -> "latar belakang.jpeg")
    private static final String BTN_BACK_PATH = "assets/btn-back.png";
    private static final String CLICK_SFX = "assets/click_sfx_fixed.wav";

    private static final float BASE_W = 1024f, BASE_H = 548f;  // ukuran acuan (isi jendela 1024x576 di Mac)
    private static final float TEXT_BOOST = 1.25f;              // tulisan dibuat lebih besar dari skala jendela (dikecilkan otomatis kalau tak muat)
    private static final float CARD_WIDTH_RATIO = 0.92f;        // lebar kotak = 92% lebar jendela

    private final GameEngine engine;
    private final Runnable onStateChanged;
    private final PixelUI.CoverBackground background = new PixelUI.CoverBackground(PixelUI.BACKGROUND_PATH);
    private final JButton btnBack;

    // Hasil hitung tata letak terakhir
    private final Rectangle card = new Rectangle();
    private float textScale = 1f;

    public HowToPlayPanel(GameEngine engine, Runnable onStateChanged) {
        this.engine = engine;
        this.onStateChanged = onStateChanged;

        setLayout(null);
        setBackground(new Color(34, 40, 49));

        // --- KANAN BAWAH: TOMBOL BACK ---
        btnBack = PixelUI.createImageButton(BTN_BACK_PATH, 190, "BACK");
        btnBack.addActionListener(e -> {
            SoundManager.playSFX(CLICK_SFX);
            engine.returnToMainMenu();
            onStateChanged.run();
        });
        add(btnBack);
    }

    // Menghitung ulang ukuran & posisi kotak + tombol setiap kali panel berubah ukuran
    @Override
    public void doLayout() {
        int W = getWidth(), H = getHeight();
        if (W <= 0 || H <= 0) return;

        float s = Math.min(W / BASE_W, H / BASE_H);

        // Tombol BACK di kanan bawah
        int btnW = Math.round(190 * s);
        int btnH = PixelUI.buttonHeight(btnBack, btnW);
        int margin = Math.round(22 * s);
        btnBack.setBounds(W - btnW - margin, H - btnH - margin, btnW, btnH);

        // Area untuk kotak: di atas tombol BACK
        int top = Math.round(20 * s);
        int bottomLimit = H - btnH - margin - Math.round(10 * s);
        int maxH = bottomLimit - top;
        int cardW = Math.round(W * CARD_WIDTH_RATIO);

        // Mulai dari tulisan besar, kecilkan sedikit-sedikit sampai isinya muat di area
        Graphics2D g2 = PixelUI.measureGraphics();
        float ts = s * TEXT_BOOST;
        int contentH = layoutContent(g2, cardW, false, ts);
        while (contentH > maxH && ts > 0.4f) {
            ts *= 0.96f;
            contentH = layoutContent(g2, cardW, false, ts);
        }
        g2.dispose();

        textScale = ts;
        int cardH = Math.min(contentH, maxH);
        card.setBounds((W - cardW) / 2, top + (maxH - cardH) / 2, cardW, cardH);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        background.paint(g, getWidth(), getHeight());

        if (card.width <= 0) return;
        Graphics2D g2 = (Graphics2D) g.create();
        g2.translate(card.x, card.y);
        PixelUI.paintCard(g2, card.width, card.height);
        layoutContent(g2, card.width, true, textScale);
        g2.dispose();
    }

    // Format poin dari Strategy Pattern, contoh: +10 / -5 / -10
    private static String pts(ScoreStrategy s) {
        return String.format("%+d", s.calculateScore());
    }

    // Menggambar satu bagian (judul kecil emas + paragraf). Mengembalikan baselineY berikutnya.
    private int section(Graphics2D g2, boolean draw, int y, int pad, int innerW, float ts, String heading, String body) {
        Font hFont = PixelUI.font(11f, ts);
        Font bFont = PixelUI.font(10f, ts);
        int bodyLine = Math.round(18 * ts);
        if (draw) {
            g2.setFont(hFont);
            g2.setColor(PixelUI.GOLD);
            g2.drawString(heading, pad, y);
        }
        y += Math.round(20 * ts);
        if (draw) {
            y = PixelUI.drawWrapped(g2, body, bFont, PixelUI.TEXT, pad, y, innerW, bodyLine, false);
        } else {
            y += PixelUI.wrap(g2.getFontMetrics(bFont), body, innerW).size() * bodyLine;
        }
        return y;
    }

    // draw = true -> menggambar; draw = false -> cuma menghitung tinggi total isi kotak
    // ts = skala teks/jarak (1.0 = ukuran acuan)
    private int layoutContent(Graphics2D g2, int w, boolean draw, float ts) {
        int cx = w / 2;
        int pad = Math.round(36 * ts);
        int innerW = w - pad * 2;
        int gap = Math.round(12 * ts);   // jarak antar bagian
        int bodyLine = Math.round(18 * ts);

        int y = Math.round(44 * ts);

        // Judul
        if (draw) PixelUI.drawCentered(g2, "CARA BERMAIN", PixelUI.font(22f, ts), Color.WHITE, cx, y);
        y += Math.round(16 * ts);
        if (draw) {
            g2.setColor(PixelUI.GOLD);
            g2.fillRect(cx - Math.round(110 * ts), y, Math.round(220 * ts), Math.max(2, Math.round(2 * ts)));
        }
        y += Math.round(36 * ts);

        // 1. Tujuan
        y = section(g2, draw, y, pad, innerW, ts, "TUJUAN",
                "Bantu Sammy bertahan hidup di kampus Politeknik Statistika STIS sampai akhir semester!");
        y += gap;

        // 2. Pilihan & poin (angka diambil langsung dari class Strategy)
        y = section(g2, draw, y, pad, innerW, ts, "PILIHAN & POIN",
                "Di tiap scene kamu memilih 1 dari 3 tindakan. Setiap pilihan punya bobot poin:");
        y += Math.round(4 * ts);
        if (draw) {
            g2.setFont(PixelUI.font(10f, ts));
            FontMetrics fm = g2.getFontMetrics();
            String[][] chips = {
                    {"TEPAT ", pts(new BestOptionStrategy())},
                    {"BERISIKO ", pts(new RiskyOptionStrategy())},
                    {"BURUK ", pts(new BadOptionStrategy())}
            };
            Color[] colors = {PixelUI.GREEN, PixelUI.ORANGE, PixelUI.RED};
            int x = pad;
            for (int i = 0; i < chips.length; i++) {
                g2.setColor(PixelUI.TEXT);
                g2.drawString(chips[i][0], x, y);
                x += fm.stringWidth(chips[i][0]);
                g2.setColor(colors[i]);
                g2.drawString(chips[i][1], x, y);
                x += fm.stringWidth(chips[i][1]) + Math.round(34 * ts);
            }
        }
        y += bodyLine + gap;

        // 3. Mini game
        y = section(g2, draw, y, pad, innerW, ts, "MINI GAME",
                "Di tengah cerita ada puzzle. Seret kepingan puzzle ke urutan yang benar, lalu klik Periksa. "
                        + "Benar " + pts(new MiniGameSuccessStrategy()) + " poin, salah " + pts(new MiniGameFailStrategy()) + " poin.");
        y += gap;

        // 4. Kontrol
        y = section(g2, draw, y, pad, innerW, ts, "KONTROL",
                "Klik tombol LANJUT untuk membaca dialog berikutnya. Tekan SPASI untuk skip.");

        return y + Math.round(20 * ts); // padding bawah
    }
}