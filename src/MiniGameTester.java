import javax.swing.*;

public class MiniGameTester {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            GameEngine engine = new GameEngine();
            engine.startNewGame();   // load semua scene dari StoryDataLoader + set state PLAYING
            engine.goToScene(60);    // langsung lompat ke Scene 60 (Puzzle Inheritance Java)

            JFrame frame = new JFrame("Test - Puzzle Balok Kodingan PBO");
            frame.setSize(1024, 576);
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            frame.setLocationRelativeTo(null);

            MiniGamePanel panel = new MiniGamePanel(engine, () -> {
                System.out.println("State sekarang : " + engine.getCurrentState());
                System.out.println("Skor sekarang  : " + engine.getTotalScore());
                if (engine.getCurrentState() != GameState.MINI_GAME) {
                    System.out.println("Mini game selesai, scene sekarang: "
                            + engine.getCurrentScene().getSceneId());
                }
            });

            frame.add(panel);
            frame.setVisible(true);
        });
    }
}
