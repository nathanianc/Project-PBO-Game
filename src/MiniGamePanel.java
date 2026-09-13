import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MiniGamePanel extends JPanel {
    private GameEngine engine;
    private Runnable onStateChanged;

    // Jawaban Benar Berurutan Sesuai Concept Inheritance
    private final String[] correctOrder = {
            "class Hewan { void bersuara() {} }",
            "class Kucing extends Hewan { }",
            "{ Kucing meow = new Kucing(); }"
    };

    // Gambar kepingan puzzle untuk tiap baris jawaban (urutan index harus SAMA PERSIS dengan correctOrder di atas)
    private final String[] pieceImagePaths = {
            "assets/puzzle_piece_hewan.png",          // "class Hewan { void bersuara() {} }" (3 baris kode -> lebih tinggi)
            "assets/puzzle_piece_kucing_extends.png", // "class Kucing extends Hewan { }"
            "assets/puzzle_piece_kucing_meow.png"     // "Kucing meow = new Kucing();"
    };

    private static final String LAPTOP_BG_PATH = "assets/minigame_laptop_bg.png";

    // Ukuran ASLI gambar layar laptop (rasio 16:9). Dipakai buat jaga rasio pas digambar (letterbox),
    // jadi nggak pernah keliatan gepeng/melar walau ukuran window-nya beda-beda.
    private static final int BG_NATIVE_W = 1024;
    private static final int BG_NATIVE_H = 576;

    // --- KOORDINAT PROPORSIONAL (0.0 - 1.0) MENGIKUTI POSISI GARIS KODE DI assets/minigame_laptop_bg.png ---
    // SEMUA ukuran & posisi kepingan dihitung ULANG dari nilai-nilai ini setiap kali digambar (bukan disimpan
    // sekali di awal) - jadi tetap presisi walau window di-resize atau di-maximize kapan saja.
    private static final double SLOT_REL_X = 0.098;
    private static final double SLOT_REL_W = 0.844;
    private static final double[] SLOT_REL_Y = {0.181, 0.400, 0.473}; // baris 1-3, 4, 5 di layar laptop
    private static final double[] SLOT_REL_H = {0.219, 0.073, 0.073}; // slot ke-0 lebih tinggi (nampung 3 baris kode)
    private static final double TRAY_TOP_REL_Y = 0.565;
    private static final int TRAY_GAP = 10;

    private List<PuzzlePiece> pieces = new ArrayList<>();
    private List<TargetSlot> slots = new ArrayList<>();
    private PuzzlePiece draggedPiece = null;
    private Point dragMouseOffset = new Point();
    private Image laptopBg;
    private boolean puzzleInitialized = false;

    // Cache posisi/ukuran hasil gambar TERAKHIR per piece, dipakai buat hit-test klik mouse.
    // Selalu di-refresh tiap paintComponent, jadi selalu sinkron dengan tampilan yang beneran kelihatan.
    private final Map<PuzzlePiece, Rectangle> lastDrawnBounds = new HashMap<>();

    public MiniGamePanel(GameEngine engine, Runnable onStateChanged) {
        this.engine = engine;
        this.onStateChanged = onStateChanged;

        setLayout(new BorderLayout());
        setBackground(new Color(30, 39, 46));

        laptopBg = new ImageIcon(LAPTOP_BG_PATH).getImage();

        PuzzleCanvas canvas = new PuzzleCanvas();

        JButton btnSubmit = new JButton("Periksa & Lanjut \u27A4");
        btnSubmit.setFont(new Font("SansSerif", Font.PLAIN, 13));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setBackground(new Color(35, 39, 42, 235));
        btnSubmit.setOpaque(true);
        btnSubmit.setContentAreaFilled(true);
        btnSubmit.setBorderPainted(false);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.setBorder(BorderFactory.createEmptyBorder(8, 14, 8, 14));
        btnSubmit.addActionListener(e -> checkAnswer());

        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 12, 16));
        bottomPanel.add(btnSubmit);

        add(canvas, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    private void initPuzzle() {
        pieces.clear();
        slots.clear();

        for (int i = 0; i < correctOrder.length; i++) {
            slots.add(new TargetSlot(i));
        }

        List<Integer> order = new ArrayList<>();
        for (int i = 0; i < correctOrder.length; i++) order.add(i);
        Collections.shuffle(order);

        for (int idx : order) {
            ImageIcon icon = new ImageIcon(pieceImagePaths[idx]);
            double aspect = (double) Math.max(1, icon.getIconWidth()) / Math.max(1, icon.getIconHeight());
            pieces.add(new PuzzlePiece(idx, correctOrder[idx], icon.getImage(), aspect));
        }

        puzzleInitialized = true;
    }

    // Menghitung area gambar layar laptop yang beneran kegambar di canvas (letterboxed, rasio 16:9 terjaga)
    private static Rectangle computeImageArea(int canvasW, int canvasH) {
        double scale = Math.min((double) canvasW / BG_NATIVE_W, (double) canvasH / BG_NATIVE_H);
        int w = (int) (BG_NATIVE_W * scale);
        int h = (int) (BG_NATIVE_H * scale);
        int x = (canvasW - w) / 2;
        int y = (canvasH - h) / 2;
        return new Rectangle(x, y, w, h);
    }

    // Ukuran tampil piece SELALU dihitung ulang dari imgArea SAAT INI (bukan disimpan), biar nggak pernah
    // desync walau canvas resize di antara dua repaint.
    private static Dimension computeDisplaySize(PuzzlePiece p, Rectangle imgArea) {
        int h = (int) (SLOT_REL_H[p.typeIndex] * imgArea.height);
        int w = (int) (h * p.aspect);
        return new Dimension(w, h);
    }

    private void checkAnswer() {
        if (!puzzleInitialized) return;

        boolean isCorrect = true;
        for (int i = 0; i < slots.size(); i++) {
            TargetSlot slot = slots.get(i);
            if (slot.placedPiece == null || !slot.placedPiece.text.equals(correctOrder[i])) {
                isCorrect = false;
                break;
            }
        }

        ScoreStrategy strategy = isCorrect ? new MiniGameSuccessStrategy() : new MiniGameFailStrategy();
        engine.addScore(strategy.calculateScore());

        if (isCorrect) {
            JOptionPane.showMessageDialog(this,
                    "PROGRAM BEBAS ERROR!\n(+20 Poin)",
                    "SUCCEED",
                    JOptionPane.INFORMATION_MESSAGE);
            engine.goToScene(61);
        } else {
            JOptionPane.showMessageDialog(this,
                    "PROGRAM MASIH ERROR!\nUrutan kodingan belum tepat (-10 Poin)",
                    "Mini Game Gagal",
                    JOptionPane.ERROR_MESSAGE);
            engine.goToScene(62);
        }

        onStateChanged.run();
    }

    private class PuzzleCanvas extends JPanel {
        public PuzzleCanvas() {
            setOpaque(true);
            setBackground(new Color(210, 211, 214)); // warna letterbox, senada abu-abu aluminium MacBook

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (int i = pieces.size() - 1; i >= 0; i--) {
                        PuzzlePiece p = pieces.get(i);
                        Rectangle b = lastDrawnBounds.get(p);
                        if (b != null && b.contains(e.getPoint())) {
                            draggedPiece = p;
                            dragMouseOffset = new Point(e.getX() - b.x, e.getY() - b.y);
                            if (p.currentSlot != null) {
                                p.currentSlot.placedPiece = null;
                                p.currentSlot = null;
                            }
                            p.freeX = b.x;
                            p.freeY = b.y;
                            pieces.remove(i);
                            pieces.add(p);
                            repaint();
                            break;
                        }
                    }
                }

                @Override
                public void mouseDragged(MouseEvent e) {
                    if (draggedPiece != null) {
                        draggedPiece.freeX = e.getX() - dragMouseOffset.x;
                        draggedPiece.freeY = e.getY() - dragMouseOffset.y;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedPiece != null) {
                        Rectangle imgArea = computeImageArea(getWidth(), getHeight());
                        Dimension size = computeDisplaySize(draggedPiece, imgArea);
                        Rectangle dragBounds = new Rectangle(draggedPiece.freeX, draggedPiece.freeY, size.width, size.height);

                        for (TargetSlot slot : slots) {
                            Rectangle slotBounds = slot.getBounds(imgArea);
                            if (slot.placedPiece == null && slotBounds.intersects(dragBounds)) {
                                // Nempel ke slot: posisi & ukuran full-fresh dari slot + rasio piece (lihat draw()),
                                // bukan dari nilai freeX/freeY lama - jadi otomatis presisi di ukuran window manapun.
                                draggedPiece.currentSlot = slot;
                                draggedPiece.freeX = null;
                                draggedPiece.freeY = null;
                                slot.placedPiece = draggedPiece;
                                break;
                            }
                        }
                        draggedPiece = null;
                        repaint();
                    }
                }
            };

            addMouseListener(adapter);
            addMouseMotionListener(adapter);
        }

        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);

            if (!puzzleInitialized) {
                initPuzzle();
            }

            Rectangle imgArea = computeImageArea(getWidth(), getHeight());

            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

            // 0. Gambar layar laptop TETAP RASIO 16:9 (letterbox), nggak pernah gepeng/melar
            if (laptopBg != null) {
                g2d.drawImage(laptopBg, imgArea.x, imgArea.y, imgArea.width, imgArea.height, this);
            }

            // 1. Highlight tipis di atas slot yang MASIH KOSONG (biar kelihatan target drop-nya)
            for (TargetSlot slot : slots) {
                if (slot.placedPiece == null) {
                    Rectangle b = slot.getBounds(imgArea);
                    g2d.setColor(new Color(255, 255, 255, 35));
                    g2d.fillRoundRect(b.x, b.y, b.width, b.height, 6, 6);
                    g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{8}, 0));
                    g2d.setColor(new Color(255, 234, 167, 200));
                    g2d.drawRoundRect(b.x, b.y, b.width, b.height, 6, 6);
                }
            }

            // 2. Gambar kepingan puzzle - posisi & ukuran DIHITUNG ULANG di sini, setiap saat, dari imgArea SAAT INI.
            // Tray dibagi 2 kolom: piece 3-baris (typeIndex 0) di kolom KIRI, dua piece pendek ditumpuk
            // di kolom KANAN sebelahnya - biar hemat tempat vertikal dan nggak nembus keluar layar laptop.
            int trayX = imgArea.x + (int) (SLOT_REL_X * imgArea.width);
            int trayTopY = imgArea.y + (int) (TRAY_TOP_REL_Y * imgArea.height);

            Map<PuzzlePiece, Dimension> traySizes = new HashMap<>();
            int tallColumnWidth = 0;
            for (PuzzlePiece p : pieces) {
                boolean inTray = p != draggedPiece && p.currentSlot == null && p.freeX == null;
                if (inTray) {
                    Dimension size = computeDisplaySize(p, imgArea);
                    traySizes.put(p, size);
                    if (p.typeIndex == 0) tallColumnWidth = size.width;
                }
            }

            int leftY = trayTopY;
            int rightX = trayX + tallColumnWidth + TRAY_GAP;
            int rightY = trayTopY;

            for (PuzzlePiece p : pieces) {
                Rectangle b;

                if (p == draggedPiece) {
                    Dimension size = computeDisplaySize(p, imgArea);
                    b = new Rectangle(p.freeX, p.freeY, size.width, size.height);
                } else if (p.currentSlot != null) {
                    Dimension size = computeDisplaySize(p, imgArea);
                    Rectangle sb = p.currentSlot.getBounds(imgArea);
                    b = new Rectangle(sb.x, sb.y + (sb.height - size.height) / 2, size.width, size.height);
                } else if (p.freeX != null) {
                    // sudah pernah di-drag tapi dilepas di luar slot -> tetap di posisi bebas terakhir
                    Dimension size = computeDisplaySize(p, imgArea);
                    b = new Rectangle(p.freeX, p.freeY, size.width, size.height);
                } else if (p.typeIndex == 0) {
                    // piece 3-baris -> kolom kiri
                    Dimension size = traySizes.get(p);
                    b = new Rectangle(trayX, leftY, size.width, size.height);
                    leftY += size.height + TRAY_GAP;
                } else {
                    // piece 1-baris -> ditumpuk di kolom kanan
                    Dimension size = traySizes.get(p);
                    b = new Rectangle(rightX, rightY, size.width, size.height);
                    rightY += size.height + TRAY_GAP;
                }

                lastDrawnBounds.put(p, b);
                p.draw(g2d, b);
            }
        }
    }

    private static class PuzzlePiece {
        int typeIndex; // 0,1,2 sesuai urutan di correctOrder / pieceImagePaths
        String text;
        Image img;
        double aspect; // nativeWidth / nativeHeight, dipakai biar kepingan nggak pernah gepeng
        TargetSlot currentSlot = null;
        Integer freeX = null, freeY = null; // posisi bebas (lagi di-drag / dilepas di luar slot)

        public PuzzlePiece(int typeIndex, String text, Image img, double aspect) {
            this.typeIndex = typeIndex;
            this.text = text;
            this.img = img;
            this.aspect = aspect;
        }

        public void draw(Graphics2D g2d, Rectangle b) {
            if (img != null) {
                g2d.drawImage(img, b.x, b.y, b.width, b.height, null);
            } else {
                // Fallback kalau file gambarnya belum ketemu, biar nggak invisible pas testing
                g2d.setColor(new Color(41, 128, 185));
                g2d.fillRoundRect(b.x, b.y, b.width, b.height, 12, 12);
                g2d.setColor(Color.WHITE);
                g2d.setFont(new Font("Monospaced", Font.BOLD, 13));
                g2d.drawString(text, b.x + 10, b.y + b.height / 2);
            }
        }
    }

    private static class TargetSlot {
        int index; // dipetakan ke SLOT_REL_Y[index] / SLOT_REL_H[index]
        PuzzlePiece placedPiece = null;

        public TargetSlot(int index) {
            this.index = index;
        }

        public Rectangle getBounds(Rectangle imgArea) {
            int x = imgArea.x + (int) (SLOT_REL_X * imgArea.width);
            int y = imgArea.y + (int) (SLOT_REL_Y[index] * imgArea.height);
            int w = (int) (SLOT_REL_W * imgArea.width);
            int h = (int) (SLOT_REL_H[index] * imgArea.height);
            return new Rectangle(x, y, w, h);
        }
    }
}