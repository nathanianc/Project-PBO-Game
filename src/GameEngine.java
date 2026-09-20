import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameEngine {
    // --- KONSTANTA ENDING ---
    // Scene 71/72/73 mengarah ke ENDING_CHECK_ID (scene "virtual", tidak ada di sceneMap).
    // Di goToScene(), ID ini otomatis diganti jadi GOOD_ENDING_ID atau BAD_ENDING_ID sesuai skor.
    public static final int ENDING_CHECK_ID = 80;
    public static final int GOOD_ENDING_ID = 8;
    public static final int BAD_ENDING_ID = 9;
    public static final int GOOD_ENDING_MIN_SCORE = 50; // skor >= 50 = Good Ending

    private GameState currentState;
    private int totalScore;

    // --- ANIMASI POP-UP KARAKTER ---
    // Menyimpan siapa & di scene mana karakter terakhir yang ditampilkan,
    // supaya sprite hanya "naik dari bawah" saat karakter baru muncul (bukan tiap ganti baris dialog).
    private String activeAmbience = null;   // ambience yang sedang berjalan (null = tidak ada)

    private int lastSpriteSceneId = -1;
    private String lastSpriteSpeaker = null;

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
        this.lastSpriteSceneId = -1;
        this.lastSpriteSpeaker = null;
        this.activeAmbience = null;
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
        // Scene 1 punya transisi "DAY 1", jadi state awalnya DAY_TRANSITION (bukan langsung PLAYING)
        this.currentState = stateFor(currentScene);
    }

    // Menentukan layar apa yang tampil untuk sebuah scene:
    // transisi hari dulu (kalau ada) -> mini game -> dialog biasa
    private GameState stateFor(Scene scene) {
        if (scene == null) return GameState.PLAYING;
        if (scene.hasDayTransition()) return GameState.DAY_TRANSITION;
        return scene.isMiniGame() ? GameState.MINI_GAME : GameState.PLAYING;
    }

    // Dipanggil DayTransitionPanel setelah animasi "DAY X" selesai (atau di-skip)
    public void finishDayTransition() {
        if (currentState != GameState.DAY_TRANSITION || currentScene == null) return;
        this.currentState = currentScene.isMiniGame() ? GameState.MINI_GAME : GameState.PLAYING;
        startEndingMusicIfNeeded(); // kartu DAY selesai -> kalau ini scene ending, mulai musiknya
    }

    // Program diminta keluar (tombol Exit / tutup jendela): tampilkan layar penutup dulu
    public void requestExit() {
        this.currentState = GameState.EXITING;
    }

    public void openHowToPlay() {
        this.currentState = GameState.HOW_TO_PLAY;
    }

    public void returnToMainMenu() {
        this.currentState = GameState.MAIN_MENU;
    }

    // Pindah ke dialog berikutnya ATAU pindah ke scene berikutnya jika dialog habis
    public void nextDialogOrScene() {
        // Hanya boleh maju dialog kalau layar dialog yang sedang aktif. Ini juga mencegah dobel-klik
        // "Lanjut" saat layar lagi fade ke transisi DAY (state sudah berpindah, tapi layar lama belum hilang).
        if (currentState != GameState.PLAYING || currentScene == null) return;

        // 1. Jika masih ada dialog awal di scene saat ini, maju ke dialog berikutnya
        if (currentDialogIndex < currentScene.getDialogs().size() - 1) {
            currentDialogIndex++;
        }
        // 2. Jika dialog di scene ini sudah habis dan ini adalah Sub-Scene (punya target next scene)
        else if (currentScene.getDefaultNextSceneId() != -1) {
            goToScene(currentScene.getDefaultNextSceneId());
        }
        // 3. Jika ini scene ENDING dan dialognya sudah habis, tampilkan layar hasil akhir (skor + restart)
        else if (currentScene.isEnding()) {
            this.currentState = GameState.GAME_OVER;
        }
    }

    // Eksekusi Pilihan Jawaban Pemain (Dipanggil dari PlayingPanel)
    public void chooseOption(int optionIndex) {
        if (currentState != GameState.PLAYING || currentScene == null || !currentScene.hasOptions()) return;

        Option selectedOption = currentScene.getOptions().get(optionIndex);

        // Kalkulasi Poin menggunakan Strategy Pattern
        totalScore += selectedOption.getPoints();

        // Suara umpan balik sesuai kualitas pilihan (+10 / -5 / -10)
        SoundManager.playSFX(optionSoundFor(selectedOption.getPoints()));

        // Langsung lompat ke Sub-Scene hasil pilihan opsi!
        goToScene(selectedOption.getNextSceneId());
    }

    // Memilih file suara berdasarkan bobot poin sebuah opsi
    private static String optionSoundFor(int points) {
        if (points > 0) return SoundManager.SFX_OPTION_BEST;
        if (points >= -5) return SoundManager.SFX_OPTION_RISKY;
        return SoundManager.SFX_OPTION_BAD;
    }

    // Menyalakan / mengganti / mematikan ambience sesuai scene yang sedang aktif.
    // Dipanggil saat layar HITAM transisi scene muncul (DayTransitionPanel), atau langsung saat pindah scene
    // yang tidak punya kartu DAY (sub-scene).
    //  - scene punya ambience baru -> dimulai (crossfade dari ambience sebelumnya); yang sama dibiarkan lanjut
    //  - scene tidak punya ambience tapi ada ambience yang sedang jalan -> fade-out
    public void startSceneAmbience() {
        if (currentScene == null) return;
        String want = currentScene.getAmbience();
        if (want != null) {
            if (!want.equals(activeAmbience)) {
                System.out.println("[Ambience] scene " + currentScene.getSceneId() + ": MULAI " + want
                        + " (volume x" + currentScene.getAmbienceVolume() + ")");
                SoundManager.playAmbience(want, currentScene.getAmbienceVolume());
            }
            activeAmbience = want;
        } else if (activeAmbience != null) {
            System.out.println("[Ambience] scene " + currentScene.getSceneId() + ": HENTIKAN " + activeAmbience);
            SoundManager.stopBGMWithFade(1000);
            activeAmbience = null;
        }
    }

    // Musik ending (good/bad) dimulai saat scene ending BENAR-BENAR tampil, bukan saat kartu "AKHIR SEMESTER",
    // supaya kartu itu tidak membocorkan hasilnya lewat musik.
    private void startEndingMusicIfNeeded() {
        if (currentScene == null || !currentScene.isEnding()) return;
        String track = currentScene.getSceneId() == GOOD_ENDING_ID ? SoundManager.BGM_GOOD_ENDING : SoundManager.BGM_BAD_ENDING;
        activeAmbience = null; // musik ending menggantikan ambience (lewat crossfade)
        SoundManager.playBGM(track, false); // sekali jalan; menggantikan musik lama dengan crossfade
    }

    // Method navigasi berpindah ke Scene ID tertentu (Termasuk pemicu BGM/SFX!)
    public void goToScene(int sceneId) {
        // Scene "penentu ending": ganti ke Good/Bad Ending sesuai total skor saat ini
        if (sceneId == ENDING_CHECK_ID) {
            sceneId = isGoodEnding() ? GOOD_ENDING_ID : BAD_ENDING_ID;
        }

        if (sceneMap.containsKey(sceneId)) {
            currentScene = sceneMap.get(sceneId);
            currentDialogIndex = 0; // Reset index dialog ke awal scene baru

            // Urutan layar: transisi "DAY X" (kalau scene-nya punya) -> mini game / dialog biasa
            this.currentState = stateFor(currentScene);

            // Scene tanpa kartu DAY (sub-scene, ending tanpa kartu): ambience & musik ending langsung diatur.
            // (Scene dengan kartu DAY: ambience baru diatur saat layar hitamnya muncul, lihat DayTransitionPanel.)
            if (currentState != GameState.DAY_TRANSITION) {
                startSceneAmbience();
                startEndingMusicIfNeeded();
            }

            // --- TRIGER SFX & BGM OTOMATIS BERDASARKAN SCENE ---
            if (sceneId == 11) {
                // (Suara lari & jatuh di scene ini sekarang dipasang langsung pada baris dialognya di StoryDataLoader)
            } else if (sceneId == 60) {
                // BGM Khusus Mini Game Puzzle
                activeAmbience = null; // musik mini game menggantikan ambience
                SoundManager.playBGM(SoundManager.BGM_MINI_GAME);
            } else if (sceneId == 61) {
                // SFX Menang Mini Game
                SoundManager.playSFX("assets/sfx_win.wav");
                SoundManager.stopBGMWithFade(1000); // musik khusus mini game berhenti setelah mini game selesai
            } else if (sceneId == 62) {
                // SFX Gagal Mini Game
                SoundManager.playSFX("assets/sfx_lose.wav");
                SoundManager.stopBGMWithFade(1000);
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

    // Evaluasi Ending (Kriteria Skor >= GOOD_ENDING_MIN_SCORE)
    public boolean isGoodEnding() {
        return totalScore >= GOOD_ENDING_MIN_SCORE;
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

    // Dipanggil PlayingPanel saat dibangun. TRUE = sprite karakter aktif harus muncul dengan animasi
    // naik dari bawah layar: yaitu saat pindah scene, atau saat pembicara berganti orang.
    // (Ganti pose karakter yang sama di scene yang sama TIDAK memicu pop-up.)
    public boolean consumeCharacterPopup() {
        Character c = getActiveCharacter();
        if (currentScene == null || c == null) {
            lastSpriteSpeaker = null;   // baris narasi tanpa karakter: karakter berikutnya muncul lagi dari bawah
            return false;
        }
        boolean popUp = currentScene.getSceneId() != lastSpriteSceneId
                || !c.getName().equals(lastSpriteSpeaker);
        lastSpriteSceneId = currentScene.getSceneId();
        lastSpriteSpeaker = c.getName();
        return popUp;
    }

    // Mengambil baris dialog yang sedang aktif (untuk membaca efek suaranya)
    public Dialog getActiveDialog() {
        if (currentScene != null && !currentScene.getDialogs().isEmpty()) {
            return currentScene.getDialogs().get(currentDialogIndex);
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