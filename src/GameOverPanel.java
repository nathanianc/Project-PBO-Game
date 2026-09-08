import javax.swing.*;
import java.awt.*;

public class GameOverPanel extends JPanel {
    private GameEngine engine;
    private Runnable onStateChanged;

    public GameOverPanel(GameEngine engine, Runnable onStateChanged) {
        this.engine = engine;
        this.onStateChanged = onStateChanged;

        setLayout(new GridBagLayout());
        setBackground(new Color(20, 20, 20));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx = 0;
        gbc.gridy = GridBagConstraints.RELATIVE;
        gbc.insets = new Insets(15, 0, 15, 0);

        JLabel textEnding = new JLabel(engine.getEndingStory(), SwingConstants.CENTER);
        textEnding.setForeground(Color.WHITE);

        JButton btnRestart = new JButton("Mau Buat Sammy Bahagia? (Restart)");
        JButton btnMenu = new JButton("Ke Menu Utama");

        btnRestart.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnRestart.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnMenu.setFont(new Font("SansSerif", Font.BOLD, 14));
        btnMenu.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btnRestart.addActionListener(e -> {
            engine.startNewGame();
            onStateChanged.run();
        });

        btnMenu.addActionListener(e -> {
            engine.returnToMainMenu();
            onStateChanged.run();
        });

        add(textEnding, gbc);
        add(btnRestart, gbc);
        add(btnMenu, gbc);
    }
}
