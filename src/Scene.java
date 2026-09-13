import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

public class Scene {
    private int sceneId;
    private String title;
    private Background background;
    private int defaultNextSceneId;
    private boolean isMiniGame = false;
    private boolean isEnding = false;
    private Color endingColor;

    private List<Dialog> dialogs = new ArrayList<>();
    private List<Option> options = new ArrayList<>();

    // Constructor 1: Scene Utama (Punya Opsi Jawaban)
    public Scene(int sceneId, String title, Background background) {
        this.sceneId = sceneId;
        this.title = title;
        this.background = background;
        this.defaultNextSceneId = -1; // -1 = Butuh aksi pilihan opsi
    }

    // Constructor 2: Sub-Scene / Dialog Lanjutan (Langsung ke scene lain)
    public Scene(int sceneId, String title, Background background, int defaultNextSceneId) {
        this.sceneId = sceneId;
        this.title = title;
        this.background = background;
        this.defaultNextSceneId = defaultNextSceneId;
    }

    public void addDialog(String text, Character speaker) {
        dialogs.add(new Dialog(text, speaker));
    }

    public void addDialog(String text) {
        addDialog(text, null);
    }

    public void addOption(String buttonText, int nextSceneId, ScoreStrategy strategy) {
        if (options.size() < 3) {
            options.add(new Option(buttonText, nextSceneId, strategy));
        }
    }

    // Getters & Setters
    public int getSceneId() {
        return sceneId;
    }

    public String getTitle() {
        return title;
    }

    public Background getBackground() {
        return background;
    }

    public int getDefaultNextSceneId() {
        return defaultNextSceneId;
    }

    public List<Dialog> getDialogs() {
        return dialogs;
    }

    public List<Option> getOptions() {
        return options;
    }

    public boolean hasOptions() {
        return !options.isEmpty();
    }

    public boolean isMiniGame() {
        return isMiniGame;
    }

    public void setMiniGame(boolean miniGame) {
        isMiniGame = miniGame;
    }

    public boolean isEnding() {
        return isEnding;
    }

    public void setEnding(boolean ending) {
        isEnding = ending;
    }

    public void setEnding(boolean ending, Color endingColor) {
        isEnding = ending;
        this.endingColor = endingColor;
    }

    public Color getEndingColor() {
        return endingColor;
    }
}