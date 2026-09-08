import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameEngine {
    private GameState currentState;
    private int totalScore;

    // KUNCI SUB-SCENE: Menyimpan scene berdasarkan ID (sceneId)
    private Map<Integer, Scene> sceneMap;
    private Scene currentScene;
    private int currentDialogIndex;

    public GameEngine() {
        this.currentState = GameState.MAIN_MENU;
        this.sceneMap = new HashMap<>();
        initGame();
    }

    // Inisialisasi ulang game dan memuat seluruh scene
    public void initGame() {
        this.totalScore = 0;
        this.currentDialogIndex = 0;
        this.sceneMap.clear();

        // Load semua list scene dari StoryDataLoader dan masukkan ke Map
        List<Scene> allScenes = StoryDataLoader.loadAllScenes();
        for (Scene s : allScenes) {
            sceneMap.put(s.getSceneId(), s);
        }

        // Scene awal saat game mulai (Scene ID = 1)
        this.currentScene = sceneMap.get(1);
    }

    public void startNewGame() {
        initGame();
        this.currentState = GameState.PLAYING;
    }

    public void openHowToPlay() {
        this.currentState = GameState.HOW_TO_PLAY;
    }

    public void returnToMainMenu() {
        this.currentState = GameState.MAIN_MENU;
    }

    // Pindah ke dialog berikutnya ATAU pindah ke scene berikutnya jika dialog habis
    public void nextDialogOrScene() {
        if (currentScene == null) return;

        // 1. Jika masih ada dialog awal di scene saat ini, maju ke dialog berikutnya
        if (currentDialogIndex < currentScene.getDialogs().size() - 1) {
            currentDialogIndex++;
        }
        // 2. Jika dialog di scene ini sudah habis dan ini adalah Sub-Scene (punya target next scene)
        else if (currentScene.getDefaultNextSceneId() != -1) {
            goToScene(currentScene.getDefaultNextSceneId());
        }
    }

    // Eksekusi Pilihan Jawaban Pemain (Dipanggil dari PlayingPanel)
    public void chooseOption(int optionIndex) {
        if (currentState != GameState.PLAYING || currentScene == null || !currentScene.hasOptions()) return;

        Option selectedOption = currentScene.getOptions().get(optionIndex);

        // Kalkulasi Poin menggunakan Strategy Pattern
        totalScore += selectedOption.getPoints();

        // Langsung lompat ke Sub-Scene hasil pilihan opsi!
        goToScene(selectedOption.getNextSceneId());
    }

    // Method navigasi berpindah ke Scene ID tertentu (Termasuk pemicu BGM/SFX!)
    public void goToScene(int sceneId) {
        if (sceneMap.containsKey(sceneId)) {
            currentScene = sceneMap.get(sceneId);
            currentDialogIndex = 0; // Reset index dialog ke awal scene baru

            // --- TRIGER SFX & BGM OTOMATIS BERDASARKAN SCENE ---
            if (sceneId == 11) {
                // SFX Kejedot / Pagar
                SoundManager.playSFX("assets/sfx_buk.wav");
            } else if (sceneId == 60) {
                // BGM Khusus Mini Game Puzzle
                SoundManager.playBGM("assets/bgm_minigame.wav");
            } else if (sceneId == 61) {
                // SFX Menang Mini Game
                SoundManager.playSFX("assets/sfx_win.wav");
            } else if (sceneId == 62) {
                // SFX Gagal Mini Game
                SoundManager.playSFX("assets/sfx_lose.wav");
            }

        } else {
            // Jika ID scene tidak ditemukan (sudah tamat), masuk ke layar Game Over
            this.currentState = GameState.GAME_OVER;
        }
    }

    // Method khusus penambahan/pengurangan skor (untuk MiniGamePanel)
    public void addScore(int points) {
        this.totalScore += points;
    }

    // Evaluasi Ending (Kriteria Skor >= 50)
    public String getEndingStory() {
        if (totalScore >= 50) {
            return "<html><h2>ENDING 1: GOOD ENDING 🎉</h2><br>" +
                    "Sammy: Puji Tuhan, akhirnya setelah kerja kerasku selama ini, aku dapat IPK 4.00.<br>" +
                    "Ga sia-sia yaa selama ini aku belajar dengan sungguh-sungguh!<br><br>" +
                    "<b>Total Skor Akhir: " + totalScore + "</b></html>";
        } else {
            return "<html><h2>ENDING 2: BAD ENDING 😭</h2><br>" +
                    "Sammy: DEMIII APAA?!!! NILAI AKU SEGINI?! OH TIDAK, AKU TELAH DI DROP OUT...<br>" +
                    "APA YANG TELAH KUPERBUAT SELAMA INI??<br><br>" +
                    "<b>Total Skor Akhir: " + totalScore + "</b></html>";
        }
    }

    // --- GETTER AKTIF UNTUK TAMPILAN GUI ---

    public GameState getCurrentState() { return currentState; }
    public int getTotalScore() { return totalScore; }
    public Scene getCurrentScene() { return currentScene; }

    // Mengambil karakter yang sedang bicara pada dialog aktif
    public Character getActiveCharacter() {
        if (currentScene != null && !currentScene.getDialogs().isEmpty()) {
            return currentScene.getDialogs().get(currentDialogIndex).getSpeaker();
        }
        return null;
    }

    // Mengambil teks dialog yang sedang dibaca
    public String getActiveDialogText() {
        if (currentScene != null && !currentScene.getDialogs().isEmpty()) {
            return currentScene.getDialogs().get(currentDialogIndex).getText();
        }
        return "";
    }

    // Mengecek apakah dialog di scene ini sudah di baris terakhir
    public boolean isLastDialogInScene() {
        if (currentScene == null) return true;
        return currentDialogIndex >= currentScene.getDialogs().size() - 1;
    }
}