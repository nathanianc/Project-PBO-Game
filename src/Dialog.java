public class Dialog {
    private String text;
    private Character speaker;

    public Dialog(String text, Character speaker) {
        this.text = text;
        this.speaker = speaker;
    }

    public String getText() { return text; }
    public Character getSpeaker() { return speaker; }
}
