import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.RescaleOp;
import java.util.ArrayList;
import java.util.List;

// Kumpulan helper tampilan yang dipakai bareng oleh GameOverPanel & HowToPlayPanel,
// biar kode gambar background, tombol gambar, dan teks pixel nggak ditulis dobel.
public class PixelUI {

    // --- PALET WARNA ---
    public static final Color GOLD = new Color(255, 234, 167);     // senada warna skor di header
    public static final Color RED = new Color(255, 118, 117);
    public static final Color GREEN = new Color(85, 239, 196);
    public static final Color ORANGE = new Color(253, 203, 110);
    public static final Color TEXT = new Color(240, 240, 240);
    public static final Color TEXT_DIM = new Color(190, 190, 200);
    public static final Color CARD_BG = new Color(0, 0, 0, 195);   // hitam transparan
    public static final Color CARD_BORDER = new Color(255, 255, 255, 70);

    // Gambar latar untuk layar Cara Bermain & layar hasil ending.
    // (Nama file PAKAI SPASI: "latar belakang.jpeg". Ubah di sini saja kalau nama file-nya berubah.)
    public static final String BACKGROUND_PATH = "assets/latar belakang.jpeg";

    private PixelUI() {}

    // ------------------------------------------------------------------
    // BACKGROUND: gambar dibuat "cover" (memenuhi layar, rasio terjaga, kelebihan dipotong)
    // dan hasil scale-nya di-cache, jadi nggak berat walau panel sering di-repaint.
    // ------------------------------------------------------------------
    public static class CoverBackground {
        private final Image source;
        private BufferedImage cache;
        private int cacheW = -1, cacheH = -1;

        public CoverBackground(String path) {
            Image img = new ImageIcon(path).getImage();
            this.source = (img != null && img.getWidth(null) > 0) ? img : null;
        }

        public boolean isLoaded() {
            return source != null;
        }

        public void paint(Graphics g, int w, int h) {
            if (source == null || w <= 0 || h <= 0) return;

            if (cache == null || cacheW != w || cacheH != h) {
                int iw = source.getWidth(null);
                int ih = source.getHeight(null);
                double scale = Math.max((double) w / iw, (double) h / ih);
                int dw = (int) Math.ceil(iw * scale);
                int dh = (int) Math.ceil(ih * scale);

                cache = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
                Graphics2D g2 = cache.createGraphics();
                g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
                g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
                g2.drawImage(source, (w - dw) / 2, (h - dh) / 2, dw, dh, null);
                g2.dispose();
                cacheW = w;
                cacheH = h;
            }
            g.drawImage(cache, 0, 0, null);
        }
    }

    // ------------------------------------------------------------------
    // TOMBOL GAMBAR: area transparan di sekeliling gambar otomatis dipotong (auto-crop).
    // Gambarnya digambar ulang mengikuti ukuran tombol saat ini (jadi ikut membesar kalau jendela di-maximize),
    // dan ada efek terang saat di-hover. Kalau file gambarnya tidak ketemu, otomatis jadi tombol teks pixel
    // biar game nggak rusak.
    // ------------------------------------------------------------------
    public static class ImageButton extends JButton {
        private final BufferedImage source;   // gambar yang sudah di-crop
        private BufferedImage normal, hover;
        private int cachedW = -1, cachedH = -1;

        ImageButton(BufferedImage source) {
            this.source = source;
            setRolloverEnabled(true);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setOpaque(false);
            setMargin(new Insets(0, 0, 0, 0));
            setBorder(null);
            setCursor(new Cursor(Cursor.HAND_CURSOR));
        }

        // Tinggi tombol untuk lebar tertentu (rasio gambar terjaga)
        public int heightForWidth(int w) {
            return Math.max(1, Math.round((float) source.getHeight() * w / source.getWidth()));
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            if (w <= 0 || h <= 0) return;
            if (normal == null || cachedW != w || cachedH != h) {
                normal = scaleSmooth(source, w, h);
                hover = new RescaleOp(1.15f, 0f, null).filter(normal, null); // versi hover: sedikit lebih terang
                cachedW = w;
                cachedH = h;
            }
            g.drawImage(getModel().isRollover() ? hover : normal, 0, 0, null);
        }
    }

    public static JButton createImageButton(String path, int targetWidth, String fallbackText) {
        Image raw = new ImageIcon(path).getImage();
        int rw = raw.getWidth(null);
        int rh = raw.getHeight(null);

        if (rw <= 0 || rh <= 0) {
            System.err.println("PixelUI: gambar tombol tidak ditemukan: " + path);
            return createFallbackButton(fallbackText, targetWidth);
        }

        BufferedImage src = new BufferedImage(rw, rh, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g0 = src.createGraphics();
        g0.drawImage(raw, 0, 0, null);
        g0.dispose();

        Rectangle box = opaqueBounds(src);
        ImageButton btn = new ImageButton(src.getSubimage(box.x, box.y, box.width, box.height));
        btn.setPreferredSize(new Dimension(targetWidth, btn.heightForWidth(targetWidth)));
        return btn;
    }

    // Tinggi tombol untuk lebar tertentu (tombol gambar ATAU tombol teks cadangan)
    public static int buttonHeight(JButton btn, int width) {
        if (btn instanceof ImageButton) return ((ImageButton) btn).heightForWidth(width);
        return Math.round(width * 0.34f);
    }

    private static JButton createFallbackButton(String text, int width) {
        JButton btn = new JButton(text);
        btn.setFont(PixelFont.get(12f));
        btn.setForeground(Color.BLACK);
        btn.setBackground(new Color(255, 120, 30));
        btn.setOpaque(true);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.setPreferredSize(new Dimension(width, Math.round(width * 0.34f)));
        return btn;
    }

    // ------------------------------------------------------------------
    // MEMUAT GAMBAR DENGAN DIAGNOSIS
    // Cara biasa (ImageIcon) DIAM-DIAM menghasilkan gambar kosong kalau filenya bermasalah, sehingga sprite/latar
    // "tidak mau muncul" tanpa penjelasan. Method ini memuat dengan cara yang sama, tapi kalau gagal:
    //  1) mencoba jalur cadangan ImageIO (lebih toleran, mis. PNG 16-bit), dan
    //  2) kalau tetap gagal, mencetak di console PENYEBABNYA: file tidak ada, atau file ada tapi isinya bukan
    //     format yang bisa dibaca Java (mis. WebP/HEIC yang cuma diganti namanya jadi .png).
    // Pesan yang sama hanya dicetak sekali per file.
    // ------------------------------------------------------------------
    private static final java.util.Set<String> warnedImages = java.util.concurrent.ConcurrentHashMap.newKeySet();

    public static Image loadImageSafely(String path, String label) {
        ImageIcon icon = new ImageIcon(path);
        Image img = icon.getImage();
        // Berhasil hanya kalau ukurannya terbaca DAN proses memuatnya tidak error
        // (gambar yang rusak sebagian bisa punya ukuran tapi tetap tampil kosong)
        if (img != null && img.getWidth(null) > 0 && icon.getImageLoadStatus() != java.awt.MediaTracker.ERRORED) return img;

        java.io.File f = new java.io.File(path);
        boolean firstTime = warnedImages.add(path);

        if (!f.exists()) {
            if (firstTime) {
                System.out.println("[Gambar] FILE TIDAK ADA: " + path + " (dipakai untuk " + label + "). Cek nama & ekstensi ASLI di folder assets "
                        + "- Finder bisa menyembunyikan ekstensi, jadi file yang tampak 'x.png' bisa jadi bernama 'x.png.png'.");
            }
            return img;
        }

        // Jalur cadangan: ImageIO
        String reason = "";
        try {
            java.awt.image.BufferedImage bi = javax.imageio.ImageIO.read(f);
            if (bi != null) {
                if (firstTime) System.out.println("[Gambar] " + path + " tidak terbaca cara biasa, tapi berhasil lewat jalur cadangan (ImageIO) - dipakai untuk " + label);
                return bi;
            }
        } catch (Exception e) {
            reason = " (" + e.getMessage() + ")";
        }

        if (firstTime) {
            System.out.println("[Gambar] FILE ADA tapi TIDAK BISA DIBACA: " + path + " (dipakai untuk " + label + "). Isi file sebenarnya: "
                    + detectFormat(f) + reason + ". Kalau ini bukan PNG/JPEG asli, buka di Preview lalu File > Export... pilih PNG, simpan dengan nama yang sama.");
        }
        return img;
    }

    // Menebak format asli file dari beberapa byte pertamanya (bukan dari ekstensi)
    private static String detectFormat(java.io.File f) {
        try (java.io.InputStream in = new java.io.FileInputStream(f)) {
            byte[] h = in.readNBytes(16);
            if (h.length == 0) return "KOSONG (0 byte)";
            String s = new String(h, java.nio.charset.StandardCharsets.ISO_8859_1);
            if (h.length >= 4 && (h[0] & 0xFF) == 0x89 && s.startsWith("\u0089PNG")) return "PNG (tapi rusak / tidak didukung)";
            if (h.length >= 3 && (h[0] & 0xFF) == 0xFF && (h[1] & 0xFF) == 0xD8) return "JPEG (dengan nama .png?)";
            if (s.startsWith("GIF8")) return "GIF";
            if (s.startsWith("RIFF") && s.length() >= 12 && s.startsWith("WEBP", 8)) return "WebP (Java tidak bisa membaca WebP)";
            if (s.length() >= 8 && s.startsWith("ftyp", 4)) return "HEIC/AVIF (Java tidak bisa membacanya)";
            if (s.startsWith("BM")) return "BMP";
            if (s.startsWith("II*") || s.startsWith("MM")) return "TIFF";
            return "format tidak dikenal";
        } catch (Exception e) {
            return "tidak bisa dibaca (" + e.getMessage() + ")";
        }
    }

    // Kotak terkecil yang memuat semua piksel tidak-transparan
    private static Rectangle opaqueBounds(BufferedImage img) {
        int w = img.getWidth(), h = img.getHeight();
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                if (((img.getRGB(x, y) >>> 24) & 0xFF) > 8) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        if (maxX < 0) return new Rectangle(0, 0, w, h); // gambar kosong: jangan dipotong
        return new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
    }

    // Scale turun bertahap (dibagi dua-dua) biar hasilnya halus, tidak bergerigi
    private static BufferedImage scaleSmooth(BufferedImage src, int tw, int th) {
        BufferedImage current = src;
        int cw = src.getWidth(), ch = src.getHeight();
        while (cw / 2 >= tw && ch / 2 >= th) {
            cw /= 2;
            ch /= 2;
            current = drawScaled(current, cw, ch);
        }
        return drawScaled(current, tw, th);
    }

    private static BufferedImage drawScaled(BufferedImage src, int w, int h) {
        BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = out.createGraphics();
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.drawImage(src, 0, 0, w, h, null);
        g2.dispose();
        return out;
    }

    // ------------------------------------------------------------------
    // TOMBOL "DITEKAN": dipakai untuk tombol Lanjut, pilihan jawaban, dan Periksa & Lanjut.
    // Saat kursor di atasnya tombol terlihat seperti SEDANG DITEKAN: latar lebih gelap, ada bayangan di sisi atas
    // (seolah tombolnya masuk ke dalam), dan tulisan turun 1 piksel. Saat tombol mouse ditahan makin gelap.
    // Semua digambar sendiri, jadi tampilannya sama di semua Look & Feel (tidak dicampuri warna bawaan sistem).
    // ------------------------------------------------------------------
    public static class PressButton extends JButton {
        private static final Color NORMAL_BG = new Color(35, 39, 42, 235);
        private static final Color HOVER_BG = new Color(15, 17, 19, 250);     // kursor di atas tombol
        private static final Color PRESSED_BG = new Color(6, 7, 8, 255);      // tombol mouse ditahan
        private static final Color HOVER_FG = new Color(208, 208, 208);       // tulisan sedikit meredup
        private static final javax.swing.border.Border NORMAL_BORDER = BorderFactory.createEmptyBorder(8, 14, 8, 14);
        private static final javax.swing.border.Border PRESSED_BORDER = BorderFactory.createEmptyBorder(9, 14, 7, 14); // tulisan turun 1 px

        private boolean hover = false;
        private boolean pressed = false;

        public PressButton(String text) {
            super(text);
            setForeground(Color.WHITE);
            setOpaque(false);
            setContentAreaFilled(false);   // latar digambar sendiri di paintComponent
            setBorderPainted(false);
            setFocusPainted(false);
            setBorder(NORMAL_BORDER);
            setCursor(new Cursor(Cursor.HAND_CURSOR));

            addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseEntered(java.awt.event.MouseEvent e) {
                    hover = true;
                    refresh();
                }

                @Override
                public void mouseExited(java.awt.event.MouseEvent e) {
                    hover = false;
                    pressed = false;
                    refresh();
                }

                @Override
                public void mousePressed(java.awt.event.MouseEvent e) {
                    pressed = true;
                    refresh();
                }

                @Override
                public void mouseReleased(java.awt.event.MouseEvent e) {
                    pressed = false;
                    refresh();
                }
            });
        }

        private void refresh() {
            setForeground(hover ? HOVER_FG : Color.WHITE);
            setBorder(hover ? PRESSED_BORDER : NORMAL_BORDER);
            repaint();
        }

        @Override
        protected void paintComponent(Graphics g) {
            int w = getWidth(), h = getHeight();
            g.setColor(pressed ? PRESSED_BG : (hover ? HOVER_BG : NORMAL_BG));
            g.fillRect(0, 0, w, h);

            super.paintComponent(g); // tulisan

            if (hover) {
                // Bayangan bagian dalam di sisi atas (gelap) + garis tipis terang di sisi bawah = kesan "masuk ke dalam"
                g.setColor(new Color(0, 0, 0, 150));
                g.fillRect(0, 0, w, 2);
                g.setColor(new Color(0, 0, 0, 90));
                g.fillRect(0, 2, w, 2);
                g.setColor(new Color(0, 0, 0, 40));
                g.fillRect(0, 4, w, 2);
                g.setColor(new Color(255, 255, 255, 28));
                g.fillRect(0, h - 1, w, 1);
            }
        }
    }

    // ------------------------------------------------------------------
    // KOTAK HITAM TRANSPARAN (card) + BORDER TIPIS
    // ------------------------------------------------------------------
    public static void paintCard(Graphics2D g2, int w, int h) {
        g2.setColor(CARD_BG);
        g2.fillRect(0, 0, w, h);
        g2.setColor(CARD_BORDER);
        g2.drawRect(0, 0, w - 1, h - 1);
    }

    // ------------------------------------------------------------------
    // TEKS: word-wrap + gambar teks (rata tengah / rata kiri)
    // ------------------------------------------------------------------
    public static List<String> wrap(FontMetrics fm, String text, int maxWidth) {
        List<String> lines = new ArrayList<>();
        StringBuilder line = new StringBuilder();
        for (String word : text.split(" ")) {
            String test = line.length() == 0 ? word : line + " " + word;
            if (line.length() == 0 || fm.stringWidth(test) <= maxWidth) {
                line = new StringBuilder(test);
            } else {
                lines.add(line.toString());
                line = new StringBuilder(word);
            }
        }
        if (line.length() > 0) lines.add(line.toString());
        return lines;
    }

    // Gambar teks satu baris, rata tengah terhadap centerX. baselineY = posisi garis dasar huruf.
    public static void drawCentered(Graphics2D g2, String text, Font font, Color color, int centerX, int baselineY) {
        g2.setFont(font);
        g2.setColor(color);
        int w = g2.getFontMetrics().stringWidth(text);
        g2.drawString(text, centerX - w / 2, baselineY);
    }

    // Gambar paragraf yang otomatis turun baris. Mengembalikan baselineY untuk baris SETELAH paragraf ini.
    public static int drawWrapped(Graphics2D g2, String text, Font font, Color color,
                                  int x, int baselineY, int maxWidth, int lineHeight, boolean center) {
        g2.setFont(font);
        g2.setColor(color);
        FontMetrics fm = g2.getFontMetrics();
        int y = baselineY;
        for (String line : wrap(fm, text, maxWidth)) {
            int lx = center ? x + (maxWidth - fm.stringWidth(line)) / 2 : x;
            g2.drawString(line, lx, y);
            y += lineHeight;
        }
        return y;
    }

    // Font pixel dengan ukuran dasar dikali skala (dibulatkan biar tetap tajam)
    public static Font font(float baseSize, float scale) {
        return PixelFont.get(Math.max(6, Math.round(baseSize * scale)));
    }

    // Untuk mengukur tinggi layout tanpa perlu komponen tampil (dipakai buat menghitung tinggi card)
    public static Graphics2D measureGraphics() {
        return new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).createGraphics();
    }
}