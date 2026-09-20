import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

// Layar hitam transisi antar-scene: "DAY 1" + subjudul, fade-in -> tahan -> fade-out,
// lalu otomatis lanjut ke scene. Bisa di-skip pakai klik / Spasi / Enter.
public class DayTransitionPanel extends JPanel {
    private static final int FADE_IN_MS = 800;
    private static final int HOLD_MS = 1500;
    private static final int FADE_OUT_MS = 800;
    private static final int TOTAL_MS = FADE_IN_MS + HOLD_MS + FADE_OUT_MS;

    // Skip diabaikan sebentar di awal, biar klik "Lanjut" yang dobel/kecepetan
    // nggak langsung melewati transisinya.
    private static final int SKIP_LOCK_MS = 600;

    private final GameEngine engine;
    private final Runnable onStateChanged;
    private final String dayLabel;
    private final String daySubtitle;

    private final Timer animTimer;
    private final long startTime;
    private float alpha = 0f;
    private boolean finished = false;

    public DayTransitionPanel(GameEngine engine, Runnable onStateChanged) {
        this.engine = engine;
        this.onStateChanged = onStateChanged;

        Scene scene = engine.getCurrentScene();
        this.dayLabel = (scene != null && scene.getDayLabel() != null) ? scene.getDayLabel() : "";
        this.daySubtitle = (scene != null) ? scene.getDaySubtitle() : "";

        setOpaque(true);
        setBackground(new Color(12, 12, 16));

        // Layar hitam transisi scene sudah muncul: saatnya ambience scene berikutnya dimulai
        // (atau dihentikan kalau scene berikutnya tidak punya ambience)
        engine.startSceneAmbience();

        // --- SKIP: klik mouse ---
        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                trySkip();
            }
        });

        // --- SKIP: tombol Spasi / Enter ---
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_SPACE, 0), "skipDay");
        getInputMap(WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "skipDay");
        getActionMap().put("skipDay", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                trySkip();
            }
        });

        // --- ANIMASI ---
        startTime = System.currentTimeMillis();
        animTimer = new Timer(16, e -> tick());
        animTimer.start();
    }

    private void tick() {
        long elapsed = System.currentTimeMillis() - startTime;

        if (elapsed < FADE_IN_MS) {
            alpha = (float) elapsed / FADE_IN_MS;
        } else if (elapsed < FADE_IN_MS + HOLD_MS) {
            alpha = 1.0f;
        } else if (elapsed < TOTAL_MS) {
            alpha = 1.0f - (float) (elapsed - FADE_IN_MS - HOLD_MS) / FADE_OUT_MS;
        } else {
            alpha = 0f;
            finish();
            return;
        }
        repaint();
    }

    private void trySkip() {
        if (System.currentTimeMillis() - startTime >= SKIP_LOCK_MS) {
            finish();
        }
    }

    // Dijamin cuma jalan sekali (walau timer & skip kepencet bersamaan)
    private void finish() {
        if (finished) return;
        finished = true;
        animTimer.stop();
        engine.finishDayTransition();
        onStateChanged.run();
    }

    @Override
    public void removeNotify() {
        animTimer.stop(); // jaga-jaga kalau panel dibuang duluan
        super.removeNotify();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g); // isi background hitam

        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, Math.max(0f, Math.min(1f, alpha))));

        int w = getWidth();
        int h = getHeight();
        int maxTextWidth = (int) (w * 0.9);

        // Judul besar ("DAY 1") - otomatis dikecilkan kalau kepanjangan
        Font titleFont = fitFont(g2, dayLabel, 48f, maxTextWidth, 20f);
        g2.setFont(titleFont);
        FontMetrics titleFm = g2.getFontMetrics();
        int titleW = titleFm.stringWidth(dayLabel);
        int titleY = h / 2;
        g2.setColor(Color.WHITE);
        g2.drawString(dayLabel, (w - titleW) / 2, titleY);

        // Garis pemisah tipis warna emas (senada warna skor di header)
        int ruleY = titleY + 24;
        g2.setColor(new Color(255, 234, 167));
        g2.fillRect((w - titleW) / 2, ruleY, titleW, 2);

        // Subjudul
        Font subFont = fitFont(g2, daySubtitle, 14f, maxTextWidth, 8f);
        g2.setFont(subFont);
        FontMetrics subFm = g2.getFontMetrics();
        int subW = subFm.stringWidth(daySubtitle);
        g2.setColor(new Color(190, 190, 200));
        g2.drawString(daySubtitle, (w - subW) / 2, ruleY + 40);

        g2.dispose();
    }

    // Kecilkan ukuran font bertahap sampai teks muat di lebar maksimal
    private Font fitFont(Graphics2D g2, String text, float startSize, int maxWidth, float minSize) {
        float size = startSize;
        Font font = PixelFont.get(size);
        while (size > minSize && g2.getFontMetrics(font).stringWidth(text) > maxWidth) {
            size -= 2f;
            font = PixelFont.get(size);
        }
        return font;
    }
}