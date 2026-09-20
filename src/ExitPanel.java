import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Layar penutup saat program akan keluar: gambar assets/ending.jpeg muncul dengan FADE-IN, ditahan sebentar,
// lalu FADE-OUT ke hitam, dan setelah itu onFinished dijalankan (di StisVisualNovel: menutup program).
// Klik / Spasi / Enter mempercepat: langsung lanjut ke fade-out (setelah fade-in selesai).
public class ExitPanel extends JPanel {
    private static final String IMAGE_PATH = "assets/ending.jpeg";

    private static final int FADE_IN_MS = 1000;
    private static final int HOLD_MS = 2500;
    private static final int FADE_OUT_MS = 1000;

    private final PixelUI.CoverBackground image = new PixelUI.CoverBackground(IMAGE_PATH);
    private final Runnable onFinished;
    private final Timer timer;
    private long startTime;
    private float alpha = 0f;
    private boolean finished = false;

    public ExitPanel(Runnable onFinished) {
        this.onFinished = onFinished;

        setOpaque(true);
        setBackground(new Color(12, 12, 16)); // sama dengan warna lapisan fade & layar DAY, jadi pergantiannya mulus

        // Klik / Spasi / Enter = lewati bagian "tahan" (hanya setelah fade-in selesai)
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                skipHold();
            }
        });
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "skipExit");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "skipExit");
        getActionMap().put("skipExit", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                skipHold();
            }
        });

        startTime = System.currentTimeMillis();
        timer = new Timer(16, e -> tick());
        if (!image.isLoaded()) {
            // Gambar penutup tidak ditemukan: jangan menahan pemain dengan layar hitam, langsung keluar
            System.out.println("[ExitPanel] gambar " + IMAGE_PATH + " TIDAK DITEMUKAN -> keluar tanpa gambar penutup");
            SwingUtilities.invokeLater(this::finish);
            return;
        }
        timer.start();
    }

    private void tick() {
        long t = System.currentTimeMillis() - startTime;
        if (t < FADE_IN_MS) {
            alpha = (float) t / FADE_IN_MS;
        } else if (t < FADE_IN_MS + HOLD_MS) {
            alpha = 1f;
        } else if (t < FADE_IN_MS + HOLD_MS + FADE_OUT_MS) {
            alpha = 1f - (float) (t - FADE_IN_MS - HOLD_MS) / FADE_OUT_MS;
        } else {
            alpha = 0f;
            finish();
            return;
        }
        repaint();
    }

    // Loncat ke awal fade-out (alpha sudah 1, jadi tidak ada lonjakan tampilan)
    private void skipHold() {
        long t = System.currentTimeMillis() - startTime;
        if (t >= FADE_IN_MS && t < FADE_IN_MS + HOLD_MS) {
            startTime -= (FADE_IN_MS + HOLD_MS) - t;
        }
    }

    private void finish() {
        if (finished) return;
        finished = true;
        timer.stop();
        onFinished.run();
    }

    @Override
    public void removeNotify() {
        timer.stop();
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // latar gelap
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));
        image.paint(g2, getWidth(), getHeight());
        g2.dispose();
    }
}