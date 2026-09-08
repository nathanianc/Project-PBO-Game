import javax.swing.*;
import java.awt.*;

    public class HowToPlayPanel extends JPanel {
        private GameEngine engine;
        private Runnable onStateChanged;

        public HowToPlayPanel(GameEngine engine, Runnable onStateChanged) {
            this.engine = engine;
            this.onStateChanged = onStateChanged;

            setLayout(new BorderLayout(20, 20));
            setBackground(new Color(34, 40, 49));
            setBorder(BorderFactory.createEmptyBorder(40, 50, 40, 50));

            JLabel title = new JLabel("PETUNJUK PERMAINAN", SwingConstants.CENTER);
            title.setFont(new Font("SansSerif", Font.BOLD, 24));
            title.setForeground(Color.WHITE);

            String text = "<html>" +
                    "1. Bantu <b>Sammy</b> bertahan dari kehidupan kampus Politeknik Statistika STIS.<br><br>" +
                    "2. Setiap pilihan tindakan memberikan bobot poin:<br>" +
                    "   - <b>Tepat</b>: +10 Poin<br>" +
                    "   - <b>Berisiko</b>: -5 Poin<br>" +
                    "   - <b>Buruk</b>: -10 Poin<br>" +
                    "   - <b>Mini Game (Scene 6)</b>: +20 Poin jika sukses!<br><br>" +
                    "3. Kumpulkan skor &ge; 50 untuk dapat <b>Good Ending (IPK 4.00)</b>!" +
                    "</html>";

            JLabel content = new JLabel(text);
            content.setFont(new Font("SansSerif", Font.PLAIN, 16));
            content.setForeground(Color.LIGHT_GRAY);

            JButton btnBack = new JButton("KEMBALI KE MENU");
            btnBack.setFont(new Font("SansSerif", Font.BOLD, 14));
            btnBack.setCursor(new Cursor(Cursor.HAND_CURSOR));
            btnBack.addActionListener(e -> {
                engine.returnToMainMenu();
                onStateChanged.run();
            });

            add(title, BorderLayout.NORTH);
            add(content, BorderLayout.CENTER);
            add(btnBack, BorderLayout.SOUTH);
        }
    }

