import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MiniGamePanel extends JPanel {
    private GameEngine engine;
    private Runnable onStateChanged;

    // Jawaban Benar Berurutan Sesuai Concept Inheritance
    private final String[] correctOrder = {
            "class Hewan { void bersuara() {} }",
            "class Kucing extends Hewan {",
            "Kucing meow = new Kucing();"
    };

    private List<PuzzlePiece> pieces = new ArrayList<>();
    private List<TargetSlot> slots = new ArrayList<>();
    private PuzzlePiece draggedPiece = null;
    private Point dragOffset = new Point();

    public MiniGamePanel(GameEngine engine, Runnable onStateChanged) {
        this.engine = engine;
        this.onStateChanged = onStateChanged;

        setLayout(new BorderLayout());
        setBackground(new Color(30, 39, 46));

        // Header Panel
        JPanel headerPanel = new JPanel(new GridLayout(2, 1, 0, 5));
        headerPanel.setOpaque(false);
        headerPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));

        JLabel lblTitle = new JLabel("🧩 PUZZLE BALOK KODINGAN PBO", SwingConstants.CENTER);
        lblTitle.setFont(new Font("SansSerif", Font.BOLD, 22));
        lblTitle.setForeground(new Color(255, 234, 167));

        JLabel lblInstruction = new JLabel("Susun urutan 3 blok kode di bawah agar konsep Inheritance Java-nya benar!", SwingConstants.CENTER);
        lblInstruction.setFont(new Font("SansSerif", Font.PLAIN, 15));
        lblInstruction.setForeground(Color.LIGHT_GRAY);

        headerPanel.add(lblTitle);
        headerPanel.add(lblInstruction);

        // Canvas Area tempat Drag and Drop Puzzle
        PuzzleCanvas canvas = new PuzzleCanvas();

        // Tombol Periksa Jawaban
        JButton btnSubmit = new JButton("KUNCI & PERIKSA JAWABAN ➔");
        btnSubmit.setFont(new Font("SansSerif", Font.BOLD, 15));
        btnSubmit.setBackground(new Color(0, 184, 148));
        btnSubmit.setForeground(Color.WHITE);
        btnSubmit.setFocusPainted(false);
        btnSubmit.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSubmit.setBorder(BorderFactory.createEmptyBorder(12, 25, 12, 25));
        btnSubmit.addActionListener(e -> checkAnswer());

        JPanel bottomPanel = new JPanel();
        bottomPanel.setOpaque(false);
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));
        bottomPanel.add(btnSubmit);

        add(headerPanel, BorderLayout.NORTH);
        add(canvas, BorderLayout.CENTER);
        add(bottomPanel, BorderLayout.SOUTH);

        initPuzzle();
    }

    private void initPuzzle() {
        pieces.clear();
        slots.clear();

        // 3 Slot Target Menyusun (Vertikal ke Bawah)
        int slotX = 260;
        int startSlotY = 30;
        int pieceWidth = 480;
        int pieceHeight = 55;

        for (int i = 0; i < correctOrder.length; i++) {
            slots.add(new TargetSlot(slotX, startSlotY + (i * 65), pieceWidth, pieceHeight, i));
        }

        // Acak potongan balok kodingan di area bawah
        List<String> shuffledText = new ArrayList<>();
        Collections.addAll(shuffledText, correctOrder);
        Collections.shuffle(shuffledText);

        int startPieceY = 240;
        for (int i = 0; i < shuffledText.size(); i++) {
            pieces.add(new PuzzlePiece(shuffledText.get(i), slotX, startPieceY + (i * 65), pieceWidth, pieceHeight));
        }
    }

    private void checkAnswer() {
        boolean isCorrect = true;
        for (int i = 0; i < slots.size(); i++) {
            TargetSlot slot = slots.get(i);
            if (slot.placedPiece == null || !slot.placedPiece.text.equals(correctOrder[i])) {
                isCorrect = false;
                break;
            }
        }

        if (isCorrect) {
            engine.addScore(20); // Benar: +20 Poin
            JOptionPane.showMessageDialog(this,
                    "🎉 PROGRAM BEBAS ERROR!\nUrutan Inheritance Sempurna (+20 Poin)",
                    "Mini Game Berhasil",
                    JOptionPane.INFORMATION_MESSAGE);
            engine.goToScene(61); // Jump ke Sub-Scene Win
        } else {
            engine.addScore(-10); // Salah: -10 Poin
            JOptionPane.showMessageDialog(this,
                    "❌ PROGRAM MASIH ERROR!\nUrutan kodingan belum tepat (-10 Poin)",
                    "Mini Game Gagal",
                    JOptionPane.ERROR_MESSAGE);
            engine.goToScene(62); // Jump ke Sub-Scene Lose
        }

        onStateChanged.run(); // Refresh GUI
    }

    private class PuzzleCanvas extends JPanel {
        public PuzzleCanvas() {
            setOpaque(false);

            MouseAdapter adapter = new MouseAdapter() {
                @Override
                public void mousePressed(MouseEvent e) {
                    for (int i = pieces.size() - 1; i >= 0; i--) {
                        PuzzlePiece p = pieces.get(i);
                        if (p.contains(e.getPoint())) {
                            draggedPiece = p;
                            dragOffset = new Point(e.getX() - p.x, e.getY() - p.y);
                            if (p.currentSlot != null) {
                                p.currentSlot.placedPiece = null;
                                p.currentSlot = null;
                            }
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
                        draggedPiece.x = e.getX() - dragOffset.x;
                        draggedPiece.y = e.getY() - dragOffset.y;
                        repaint();
                    }
                }

                @Override
                public void mouseReleased(MouseEvent e) {
                    if (draggedPiece != null) {
                        for (TargetSlot slot : slots) {
                            if (slot.placedPiece == null && slot.getBounds().intersects(draggedPiece.getBounds())) {
                                draggedPiece.x = slot.x;
                                draggedPiece.y = slot.y;
                                draggedPiece.currentSlot = slot;
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
            Graphics2D g2d = (Graphics2D) g;
            g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            // 1. Gambar 3 Slot Target Kosong (Garis Putus-putus)
            g2d.setStroke(new BasicStroke(2, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10, new float[]{8}, 0));
            for (int i = 0; i < slots.size(); i++) {
                TargetSlot slot = slots.get(i);
                g2d.setColor(new Color(108, 122, 137, 180));
                g2d.drawRoundRect(slot.x, slot.y, slot.width, slot.height, 12, 12);
                g2d.setColor(new Color(255, 255, 255, 25));
                g2d.fillRoundRect(slot.x, slot.y, slot.width, slot.height, 12, 12);

                g2d.setColor(Color.GRAY);
                g2d.setFont(new Font("SansSerif", Font.BOLD, 14));
                g2d.drawString("Baris " + (i + 1), slot.x - 65, slot.y + 33);
            }

            // 2. Gambar Balok Puzzle Kodingan
            for (PuzzlePiece p : pieces) {
                p.draw(g2d);
            }
        }
    }

    private static class PuzzlePiece {
        String text;
        int x, y, width, height;
        TargetSlot currentSlot = null;

        public PuzzlePiece(String text, int x, int y, int width, int height) {
            this.text = text;
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public boolean contains(Point p) { return getBounds().contains(p); }
        public Rectangle getBounds() { return new Rectangle(x, y, width, height); }

        public void draw(Graphics2D g2d) {
            g2d.setColor(new Color(41, 128, 185));
            g2d.fillRoundRect(x, y, width, height, 12, 12);

            g2d.setColor(new Color(52, 152, 219));
            g2d.setStroke(new BasicStroke(2));
            g2d.drawRoundRect(x, y, width, height, 12, 12);

            g2d.setColor(Color.WHITE);
            g2d.setFont(new Font("Monospaced", Font.BOLD, 15));
            FontMetrics fm = g2d.getFontMetrics();
            int textX = x + (width - fm.stringWidth(text)) / 2;
            int textY = y + ((height - fm.getHeight()) / 2) + fm.getAscent();
            g2d.drawString(text, textX, textY);
        }
    }

    private static class TargetSlot {
        int x, y, width, height, index;
        PuzzlePiece placedPiece = null;

        public TargetSlot(int x, int y, int width, int height, int index) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.index = index;
        }

        public Rectangle getBounds() { return new Rectangle(x, y, width, height); }
    }
}
