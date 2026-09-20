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

    // --- AMBIENCE (suara suasana tempat) ---
    // Dimulai saat layar hitam transisi scene, berlanjut ke sub-scene yang memakai file yang sama,
    // dan berhenti (fade-out) saat masuk scene yang tidak punya ambience.
    private String ambience;
    private float ambienceVolume = 1.0f;

    // --- TRANSISI HARI (contoh: "DAY 1" + subjudul) ---
    // Kalau dayLabel diisi, layar transisi akan muncul SEBELUM scene ini dimainkan.
    private String dayLabel;
    private String daySubtitle;

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

    public Dialog addDialog(String text, Character speaker) {
        Dialog d = new Dialog(text, speaker);
        dialogs.add(d);
        return d;
    }

    public Dialog addDialog(String text) {
        return addDialog(text, null);
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

    // Pasang transisi hari di scene ini. Contoh: scene.setDayTransition("DAY 1", "Hari Pertama Orientasi");
    public void setDayTransition(String dayLabel, String daySubtitle) {
        this.dayLabel = dayLabel;
        this.daySubtitle = daySubtitle;
    }

    // Pasang ambience di scene ini. volume: 1.0 = sama dengan volume musik; kecilkan kalau file-nya terlalu keras.
    // Contoh: scene.setAmbience(SoundManager.AMB_KANTIN, 0.2f);
    public void setAmbience(String path, float volume) {
        this.ambience = path;
        this.ambienceVolume = volume;
    }

    public void setAmbience(String path) {
        setAmbience(path, 1.0f);
    }

    public String getAmbience() {
        return ambience;
    }

    public float getAmbienceVolume() {
        return ambienceVolume;
    }

    public boolean hasDayTransition() {
        return dayLabel != null;
    }

    public String getDayLabel() {
        return dayLabel;
    }

    public String getDaySubtitle() {
        return daySubtitle == null ? "" : daySubtitle;
    }
}