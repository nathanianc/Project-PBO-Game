import javax.swing.*;
import java.awt.*;

public class StisVisualNovel extends JFrame {
    private GameEngine engine = new GameEngine();
    private JPanel mainContainer;

    public StisVisualNovel() {
        setTitle("STIS Survival Story");
        setSize(1024, 576); // Resolusi Standar 16:9
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        mainContainer = new JPanel(new CardLayout());
        add(mainContainer);

        renderScreen();
    }

    // Method untuk mengganti layar secara dinamis berdasarkan GameState
    public void renderScreen() {
        mainContainer.removeAll();

        switch (engine.getCurrentState()) {
            case MAIN_MENU:
                mainContainer.add(new MainMenuPanel(engine, this::renderScreen));
                break;
            case HOW_TO_PLAY:
                mainContainer.add(new HowToPlayPanel(engine, this::renderScreen));
                break;
            case PLAYING:
                mainContainer.add(new PlayingPanel(engine, this::renderScreen));
                break;
            case MINI_GAME:
                mainContainer.add(new MiniGamePanel(engine, this::renderScreen));
                break;
            case GAME_OVER:
                mainContainer.add(new GameOverPanel(engine, this::renderScreen));
                break;
        }

        mainContainer.revalidate();
        mainContainer.repaint();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new StisVisualNovel().setVisible(true));
    }
}