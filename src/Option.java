public class Option {
    private String buttonText;
    private int nextSceneId; // ID scene/sub-scene tujuan
    private ScoreStrategy scoreStrategy;

    public Option(String buttonText, int nextSceneId, ScoreStrategy scoreStrategy) {
        this.buttonText = buttonText;
        this.nextSceneId = nextSceneId;
        this.scoreStrategy = scoreStrategy;
    }

    // NAMA METHOD HARUS SAMA PERSIS SESUAI YANG DIPANGGIL ENGINE!
    public int getNextSceneId() {
        return nextSceneId;
    }

    public String getButtonText() {
        return buttonText;
    }

    public int getPoints() {
        return scoreStrategy.calculateScore();
    }
}