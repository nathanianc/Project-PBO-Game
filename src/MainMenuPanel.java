import javax.swing.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.image.BufferedImage;

public class MainMenuPanel extends JPanel {
    private Background mainMenuBg;
    private Image bgImage;
    private GameEngine engine;
    private Runnable onStateChanged;

    private JButton btnPlay;
    private JButton btnTutorial;
    private JButton btnExit;

    private int btnW = 200; // Ukuran lebar tombol
    private int btnH = 55;  // Ukuran tinggi tombol

    public MainMenuPanel(GameEngine engine, Runnable onStateChanged) {
        this.engine = engine;
        this.onStateChanged = onStateChanged;

        setLayout(null);

        // --- PUTAR BGM MAIN MENU ---
        SoundManager.playBGM("assets/opening_bgm_fixed.wav");

        // 1. Inisialisasi Background
        mainMenuBg = new Background("Main Menu STIS", "assets/main-menu.jpg");
        bgImage = new ImageIcon(mainMenuBg.getImagePath()).getImage();

        // 2. Buat Tombol dengan Auto-Crop
        btnPlay = createCroppedIconButton("assets/btn-start.png", btnW, btnH);
        btnTutorial = createCroppedIconButton("assets/btn-tutorial.png", btnW, btnH);
        btnExit = createCroppedIconButton("assets/btn-exit.png", btnW, btnH);

        // 3. Event Listener (Ditambahi Sound Effect Klik!)
        btnPlay.addActionListener(e -> {
            SoundManager.playSFX("assets/click_sfx_fixed.wav");
            engine.startNewGame();
            onStateChanged.run();
            SoundManager.stopBGMWithFade(5);
        });

        btnTutorial.addActionListener(e -> {
            SoundManager.playSFX("assets/click_sfx_fixed.wav");
            engine.openHowToPlay();
            onStateChanged.run();
        });

        btnExit.addActionListener(e -> {
            SoundManager.playSFX("assets/click_sfx_fixed.wav");
            engine.requestExit();       // tampilkan gambar penutup (fade-in/out), baru program keluar
            onStateChanged.run();
        });

        add(btnPlay);
        add(btnTutorial);
        add(btnExit);

        // 4. Update posisi tombol secara dinamis
        addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                repositionButtons();
            }
        });
    }

    // TATA LETAK BARU: START di atas, TUTORIAL & EXIT berjejer di bawahnya
    private void repositionButtons() {
        int currentWidth = getWidth();
        int currentHeight = getHeight();

        if (currentWidth <= 0 || currentHeight <= 0) return;

        int gapX = 15; // Jarak horizontal antara Tutorial & Exit
        int gapY = btnH + 10; // Jarak vertikal dari baris atas ke baris bawah

        // Y dasar dari bagian bawah panel
        int startY = currentHeight - 170;

        // 1. BARIS ATAS: START tepat di tengah layar
        int startX = (currentWidth - btnW) / 2;
        btnPlay.setBounds(startX, startY, btnW, btnH);

        // 2. BARIS BAWAH: TUTORIAL (Kiri) dan EXIT (Kanan) berjejer
        int totalWidthRow2 = (btnW * 2) + gapX;
        int row2StartX = (currentWidth - totalWidthRow2) / 2;

        btnTutorial.setBounds(row2StartX, startY + gapY, btnW, btnH);
        btnExit.setBounds(row2StartX + btnW + gapX, startY + gapY, btnW, btnH);
    }

    private JButton createCroppedIconButton(String path, int w, int h) {
        ImageIcon originalIcon = new ImageIcon(path);
        Image srcImg = originalIcon.getImage();

        int srcW = srcImg.getWidth(null);
        int srcH = srcImg.getHeight(null);

        if (srcW > 0 && srcH > 0) {
            BufferedImage buffered = new BufferedImage(srcW, srcH, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2d = buffered.createGraphics();
            g2d.drawImage(srcImg, 0, 0, null);
            g2d.dispose();

            int cropY = (int) (srcH * 0.30);
            int cropH = (int) (srcH * 0.40);
            BufferedImage cropped = buffered.getSubimage(0, cropY, srcW, cropH);

            Image scaled = cropped.getScaledInstance(w, h, Image.SCALE_SMOOTH);
            originalIcon = new ImageIcon(scaled);
        }

        JButton btn = new JButton(originalIcon);
        btn.setContentAreaFilled(false);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        return btn;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (bgImage != null) {
            g.drawImage(bgImage, 0, 0, getWidth(), getHeight(), this);
        }
    }
}