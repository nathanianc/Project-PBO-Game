import javax.sound.sampled.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SoundManager {
    // Cetak log diagnostik ke console ("Run" di IntelliJ). Matikan (false) kalau sudah tidak diperlukan.
    private static final boolean DEBUG_LOG = true;
    private static final String VERSION = "v9";

    // --- FILE EFEK SUARA (taruh di folder assets/) ---
    public static final String SFX_TEXT_EFFECT = firstExisting("assets/text_effect.wav", "assets/text effect.wav"); // suara saat teks dialog muncul (diulang selama teks mengetik)
    public static final String SFX_POP_UP = "assets/pop_up.wav";           // suara saat gambar/karakter/modal "pop up"

    // Suara saat pemain memilih opsi jawaban (dipilih berdasarkan bobot poin opsinya)
    // Nama file mengikuti file aslinya (pakai spasi): "pilihan +10.wav", "pilihan -5.wav", "pilihan -10.wav".
    // Catatan: firstExisting("a", "b") = pakai nama pertama yang filenya ADA di folder assets. Dipakai untuk file yang
    // namanya bisa berspasi atau bergaris bawah; kalau nama aslimu sudah pasti, cukup tulis satu nama saja.
    public static final String SFX_OPTION_BEST = "assets/pilihan +10.wav"; // opsi terbaik  (+10)
    public static final String SFX_OPTION_RISKY = "assets/pilihan -5.wav";                            // opsi berisiko (-5)
    public static final String SFX_OPTION_BAD = "assets/pilihan -10.wav";                             // opsi buruk    (-10)

    private static String firstExisting(String... candidates) {
        for (String c : candidates) {
            if (new File(c).exists()) return c;
        }
        return candidates[0];
    }

    // Suara pada baris dialog tertentu (dipasang di StoryDataLoader lewat Dialog.withSounds)
    public static final String SFX_SAMMY_LARI = firstExisting("assets/sammy lari.wav", "assets/sammy_lari.wav");       // scene 1 pilihan 1: "*tap ... tap ..."
    public static final String SFX_SAMMY_JATUH = firstExisting("assets/sammy jatuh.wav", "assets/sammy_jatuh.wav");     // scene 1 pilihan 1: "... bukkk"
    public static final String SFX_SAMMY_KEJEDOT = firstExisting("assets/sammy kejedot.wav", "assets/sammy_kejedot.wav"); // scene 5 pilihan 1: "*kejedot meja* adoh"

    public static final String SFX_SLURP = "assets/slurp.wav";                 // scene 3 pilihan 1: Sammy makan mie instan

    // Suara benar / salah (scene 2). Nama mengikuti file: correct_sfx.wav & incorrect_sfx.wav
    // (cadangan: correct.wav & incorrect.wav)
    public static final String SFX_CORRECT = firstExisting("assets/correct_sfx.wav", "assets/correct sfx.wav", "assets/correct.wav");         // scene 2 pilihan 1: menjawab benar
    public static final String SFX_INCORRECT = firstExisting("assets/incorrect_sfx.wav", "assets/incorrect sfx.wav", "assets/incorrect.wav");   // scene 2 pilihan 2: "tetot"

    // Ambience (suara suasana tempat). Dipasang per scene di StoryDataLoader lewat Scene.setAmbience(...)
    public static final String AMB_ROAD = "assets/road.wav";        // scene 1: jalan di depan kampus
    public static final String AMB_AC = firstExisting("assets/suara_ac.wav", "assets/suara ac.wav");      // scene 3: kamar kos
    public static final String AMB_KANTIN = "assets/kantin.wav";    // scene 4: kantin

    // Musik latar mini game (diulang selama mini game berlangsung)
    public static final String BGM_MINI_GAME = firstExisting("assets/mini game.wav", "assets/mini_game.wav");

    // Musik ending (panjang, diputar sebagai BGM sekali jalan saat scene ending mulai)
    public static final String BGM_GOOD_ENDING = firstExisting("assets/good_ending.wav", "assets/good ending.wav");
    public static final String BGM_BAD_ENDING = firstExisting("assets/bad_ending.wav", "assets/bad ending.wav");

    // ------------------------------------------------------------------
    // KOMPENSASI LATENSI AUDIO (milidetik) - ada DUA angka, satu untuk tiap jenis suara
    //
    // Efek suara sekarang diputar lewat "mixer" latensi rendah (lihat bagian 3), jadi suara biasanya terdengar
    // hanya ±30-50 ms setelah dibunyikan, yaitu "sesaat setelah" gambarnya mulai bergerak. Karena itu angka
    // default-nya 0: gambar langsung tampil dan tidak ada jeda tambahan setelah klik.
    //
    // Kalau kamu mau suaranya terdengar LEBIH DULU / tepat bareng awal gambar, naikkan angkanya
    // (suara dibunyikan sebesar angka itu sebelum gambar muncul; gambar jadi tertunda selama itu):
    //   - Suara masih TELAT dari gambarnya -> NAIKKAN angkanya (coba +30 tiap kali)
    //   - Suara malah DULUAN dari gambarnya -> TURUNKAN angkanya
    // Kalau di console muncul "mixer SFX GAGAL", berarti perangkat audio tidak mengizinkan jalur latensi rendah dan
    // suara memakai jalur lama yang jauh lebih lambat; di kondisi itu angka ini perlu dinaikkan (bisa sampai 200-500).
    // ------------------------------------------------------------------
    public static final int POP_SOUND_LATENCY_MS = 0;    // suara "pop" (sprite karakter & kotak ending)
    public static final int TEXT_SOUND_LATENCY_MS = 0;   // suara teks mengetik

    private static final long T0 = System.currentTimeMillis();

    static {
        System.out.println("[SoundManager " + VERSION + "] dimuat. Kalau baris ini TIDAK muncul di console, berarti "
                + "SoundManager.java yang lama masih dipakai (build belum ter-update).");
    }

    private static void log(String msg) {
        if (DEBUG_LOG) System.out.println("[Sound +" + (System.currentTimeMillis() - T0) + "ms] " + msg);
    }

    private static Clip bgmClip;                                      // klip BGM yang "aktif" saat ini
    private static final Set<Clip> bgmClips = new HashSet<>();        // SEMUA klip BGM yang sedang terbuka (termasuk yang sedang crossfade)
    private static final Map<Clip, Float> bgmScale = new HashMap<>(); // pengali volume tiap klip (ambience yang terlalu keras bisa diperkecil)
    private static volatile String currentBgmPath = null;             // lagu BGM yang sedang diputar/dimuat (null = tidak ada)
    private static int bgmGeneration = 0;                             // naik setiap ada perintah play/stop BGM baru (lihat isBgmCancelled)
    private static float bgmVolume = 0.8f; // 0.0 (Hening) sampai 1.0 (Maksimal)
    private static float sfxVolume = 1.0f;
    private static boolean isMuted = false;

    // Thread khusus audio: memulai/menghentikan klip di sini supaya thread tampilan (EDT) tidak pernah tertahan
    private static final ExecutorService audioThread = Executors.newSingleThreadExecutor(r -> {
        Thread t = new Thread(r, "SFX-Audio");
        t.setDaemon(true);
        return t;
    });

    // ------------------------------------------------------------------
    // 1. BGM
    //
    // Memuat file BGM yang besar butuh waktu (bisa lebih dari 1 detik). Selama itu pemain mungkin sudah
    // menekan START. Karena itu setiap perintah play/stop menaikkan "bgmGeneration"; setelah selesai memuat,
    // thread pemuat mengecek apakah perintahnya masih yang terbaru. Kalau sudah dibatalkan (misal START ditekan
    // atau ada stop), klip yang baru dimuat langsung dibuang dan TIDAK PERNAH dibunyikan.
    //
    // Semua klip BGM yang terbuka juga dicatat di "bgmClips", dan stopBGM()/stopBGMWithFade() menghentikan
    // SEMUANYA, jadi tidak mungkin ada klip BGM "yatim" yang tetap berbunyi tanpa bisa dihentikan.
    // ------------------------------------------------------------------
    public static void playBGM(String filepath) {
        playBGM(filepath, true);
    }

    // loop = true  : lagu diulang terus (musik menu, musik mini game)
    // loop = false : lagu diputar sekali sampai habis (musik ending)
    public static void playBGM(String filepath, boolean loop) {
        playBGM(filepath, loop, 1.0f, 0);
    }

    // Ambience: suara suasana tempat (dipakai per scene). Diulang terus, masuk pelan-pelan (fade-in 1,5 detik),
    // dan volumenya bisa diperkecil/diperbesar per file lewat volumeScale (1.0 = sama dengan volume musik).
    // Kalau ambience yang sama sudah berjalan, dibiarkan lanjut (tidak diulang dari awal).
    public static void playAmbience(String filepath, float volumeScale) {
        playBGM(filepath, true, volumeScale, 1500);
    }

    // volumeScale = pengali volume khusus klip ini; fadeInMs > 0 = masuk pelan-pelan (kalau tidak sedang crossfade)
    public static void playBGM(String filepath, boolean loop, float volumeScale, int fadeInMs) {
        if (isMuted) return;

        final int gen;
        synchronized (SoundManager.class) {
            // Lagu yang sama sudah diputar/sedang dimuat -> biarkan lanjut, jangan diulang dari awal
            // (contoh: buka Cara Bermain lalu balik ke Menu Utama, musiknya tetap jalan mulus)
            if (filepath.equals(currentBgmPath)) {
                log("playBGM(" + filepath + ") diabaikan: lagu yang sama sudah diputar/dimuat");
                return;
            }
            currentBgmPath = filepath;
            gen = ++bgmGeneration;
        }
        log("playBGM(" + filepath + ", loop=" + loop + ") diminta [perintah #" + gen + "]");

        new Thread(() -> {
            try {
                File soundFile = new File(filepath);
                if (!soundFile.exists()) {
                    log("BGM " + filepath + " TIDAK DITEMUKAN");
                    return;
                }

                Clip newClip = AudioSystem.getClip();
                openClipRobust(newClip, soundFile); // <- bagian yang lama untuk file besar

                final Clip oldClip;
                final boolean crossfade;
                synchronized (SoundManager.class) {
                    if (gen != bgmGeneration) { // dibatalkan selagi dimuat
                        newClip.close();
                        log("BGM " + filepath + " selesai dimuat tapi sudah DIBATALKAN [perintah #" + gen + "], dibuang");
                        return;
                    }
                    oldClip = bgmClip;
                    crossfade = oldClip != null && oldClip.isRunning();
                    bgmClip = newClip;
                    bgmClips.add(newClip);
                    bgmScale.put(newClip, volumeScale);
                    setClipVolume(newClip, (crossfade || fadeInMs > 0) ? 0.0f : bgmVolume * volumeScale);
                    if (loop) newClip.loop(Clip.LOOP_CONTINUOUSLY);
                    else newClip.start();

                    // Lagu lama yang sudah selesai sendiri (tidak sedang berbunyi) langsung dibersihkan
                    if (oldClip != null && !crossfade) {
                        oldClip.stop();
                        oldClip.close();
                        bgmClips.remove(oldClip);
                        bgmScale.remove(oldClip);
                    }
                }
                log("BGM " + filepath + " MULAI berbunyi" + (crossfade ? " (crossfade dari lagu sebelumnya)" : (fadeInMs > 0 ? " (fade-in " + fadeInMs + " ms)" : ""))
                        + (volumeScale != 1.0f ? " [volume x" + volumeScale + "]" : ""));

                // Jika ada BGM lama yang jalan, lakukan Crossfade!
                if (crossfade) crossfadeBGM(oldClip, newClip, gen);
                else if (fadeInMs > 0) fadeInBGM(newClip, volumeScale, fadeInMs, gen);

            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "BGM-Loader").start();
    }

    private static boolean isBgmCancelled(int gen) {
        synchronized (SoundManager.class) {
            return gen != bgmGeneration;
        }
    }

    private static void crossfadeBGM(Clip oldClip, Clip newClip, int gen) {
        new Thread(() -> {
            try {
                int steps = 15;
                int durationMs = 800; // Durasi crossfade 0.8 detik
                int sleepTime = durationMs / steps;

                for (int i = 0; i <= steps; i++) {
                    if (isBgmCancelled(gen)) break; // ada stop / lagu lain diminta: berhenti mengatur volume
                    float factor = (float) i / steps;
                    setClipVolume(oldClip, bgmVolume * scaleOf(oldClip) * (1.0f - factor));
                    setClipVolume(newClip, bgmVolume * scaleOf(newClip) * factor);
                    Thread.sleep(sleepTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                oldClip.stop();
                oldClip.close();
                synchronized (SoundManager.class) {
                    bgmClips.remove(oldClip);
                    bgmScale.remove(oldClip);
                }
            }
        }, "BGM-Crossfade").start();
    }

    private static float scaleOf(Clip c) {
        synchronized (SoundManager.class) {
            Float f = bgmScale.get(c);
            return f == null ? 1.0f : f;
        }
    }

    // Masuk pelan-pelan dari hening sampai volume penuh (dipakai ambience)
    private static void fadeInBGM(Clip clip, float volumeScale, int durationMs, int gen) {
        new Thread(() -> {
            try {
                int steps = 30;
                for (int i = 1; i <= steps; i++) {
                    if (isBgmCancelled(gen)) return; // ada stop / lagu lain: jangan ganggu lagi
                    setClipVolume(clip, bgmVolume * volumeScale * i / steps);
                    Thread.sleep(Math.max(1, durationMs / steps));
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "BGM-FadeIn").start();
    }

    // Membuka klip BGM dari file. Isi file dibaca sampai habis lalu:
    //  - hening di depan/belakang (di bawah ±-50 dBFS) DIBUANG otomatis, jadi musik langsung mulai dan
    //    saat diulang (loop) tidak ada jeda hening panjang di antara putaran;
    //  - file WAV yang headernya rusak (ukuran data "tak terbatas", hasil ekspor streaming) tetap terbaca dengan benar,
    //    tidak membuat Java mengalokasikan memori raksasa.
    private static void openClipRobust(Clip clip, File soundFile) throws Exception {
        byte[] bytes;
        AudioFormat pcm;
        try (AudioInputStream raw = AudioSystem.getAudioInputStream(soundFile)) {
            AudioFormat src = raw.getFormat();
            int ch = src.getChannels();
            pcm = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16, ch, ch * 2, src.getSampleRate(), false);
            try (AudioInputStream conv = AudioSystem.getAudioInputStream(pcm, raw)) {
                bytes = conv.readAllBytes();
            }
        }

        final int ch = pcm.getChannels();
        final int frameSize = pcm.getFrameSize();
        final int frames = bytes.length / frameSize;
        final int thr = 104; // ≈ -50 dBFS

        int first = 0;
        while (first < frames && frameAmplitude(bytes, first, ch) <= thr) first++;
        int last = frames - 1;
        while (last > first && frameAmplitude(bytes, last, ch) <= thr) last--;

        int rate = Math.round(pcm.getSampleRate());
        int start = first >= frames ? 0 : Math.max(0, first - rate * 5 / 1000);            // sisakan 5 ms di depan
        int end = first >= frames ? frames : Math.min(frames, last + 1 + rate * 12 / 1000); // dan 12 ms di belakang
        if (start > 0 || end < frames) {
            log("BGM " + soundFile.getName() + ": hening otomatis dibuang (depan " + start * 1000L / rate + " ms, belakang "
                    + (frames - end) * 1000L / rate + " ms)");
        }
        clip.open(pcm, bytes, start * frameSize, (end - start) * frameSize);
    }

    // Amplitudo terbesar (nilai mutlak, 16-bit) di antara semua saluran pada satu frame
    private static int frameAmplitude(byte[] b, int frame, int channels) {
        int base = frame * channels * 2;
        int amp = 0;
        for (int c = 0; c < channels; c++) {
            int v = (short) ((b[base + 2 * c] & 0xFF) | (b[base + 2 * c + 1] << 8));
            amp = Math.max(amp, Math.abs(v));
        }
        return amp;
    }

    // Mengambil SEMUA klip BGM yang terbuka lalu mengosongkan catatan. Sekaligus membatalkan lagu yang
    // masih dalam proses dimuat.
    private static List<Clip> takeAllBgmClips() {
        synchronized (SoundManager.class) {
            currentBgmPath = null;
            bgmGeneration++;
            List<Clip> all = new ArrayList<>(bgmClips);
            bgmClips.clear();
            bgmClip = null;
            return all;   // (skala volume tiap klip tetap tersimpan sampai klipnya selesai di-fade, lalu dibuang bersama klip)
        }
    }

    // --- 2. STOP BGM DENGAN FADE OUT ---
    public static void stopBGMWithFade(int fadeDurationMs) {
        final List<Clip> clips = takeAllBgmClips();
        log("stopBGMWithFade(" + fadeDurationMs + " ms): " + clips.size() + " klip BGM dihentikan");
        if (clips.isEmpty()) return;

        new Thread(() -> {
            try {
                int steps = 15;
                int sleepTime = fadeDurationMs / steps;

                for (int i = steps; i >= 0; i--) {
                    float factor = (float) i / steps;
                    for (Clip c : clips) setClipVolume(c, bgmVolume * scaleOf(c) * factor);
                    Thread.sleep(sleepTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                for (Clip c : clips) {
                    c.stop();
                    c.close();
                }
                synchronized (SoundManager.class) {
                    for (Clip c : clips) bgmScale.remove(c);
                }
            }
        }, "BGM-Stop").start();
    }

    public static void stopBGM() {
        final List<Clip> clips = takeAllBgmClips();
        log("stopBGM(): " + clips.size() + " klip BGM dihentikan");
        for (Clip c : clips) {
            c.stop();
            c.close();
        }
        synchronized (SoundManager.class) {
            for (Clip c : clips) bgmScale.remove(c);
        }
    }

    // ------------------------------------------------------------------
    // 3. SFX
    //
    // Membuat Clip baru tiap kali suara diputar itu LAMBAT (apalagi di macOS: AudioSystem.getClip() harus
    // mencari mixer dulu, kadang sampai ratusan milidetik) dan waktunya tidak menentu, sehingga suara bisa
    // terlambat dari animasinya. Karena itu file yang didaftarkan lewat preload() dimuat SEKALI di awal dan
    // disimpan dalam keadaan siap; saat diputar tinggal "rewind + start" (hanya beberapa milidetik).
    // ------------------------------------------------------------------
    private static final Map<String, Clip> readyClips = new HashMap<>();

    public static void playSFX(String filepath) {
        playSFXWithOffset(filepath, 0.0);
    }

    // Memutar suara DAN mengembalikan "pegangan"-nya, supaya bisa dihentikan lewat stopSFX() (misal saat teks di-skip).
    // Pegangan bernilai null kalau suara tidak lewat mixer (tidak bisa dihentikan, tapi tetap berbunyi).
    public static Object playSFXHandle(String filepath) {
        return playSFXHandle(filepath, 1.0f);
    }

    // gain = pengali volume khusus suara ini (1.0 = volume asli; >1.0 lebih keras, dengan soft limiter agar tidak pecah)
    public static Object playSFXHandle(String filepath, float gain) {
        if (isMuted) return null;
        Voice v = mixerPlayVoice(filepath, 0, gain);
        if (v != null) return v;
        playSFXWithOffset(filepath, 0.0);
        return null;
    }

    // Menghentikan suara yang dimulai lewat playSFXHandle() dengan fade-out singkat
    public static void stopSFX(Object handle, int fadeMs) {
        if (!(handle instanceof Voice)) return;
        synchronized (mixLock) {
            fadeOutVoice((Voice) handle, Math.max(1, fadeMs) * MIX_RATE / 1000);
        }
    }

    public static void playSFXWithOffset(String filepath, double startSeconds) {
        if (isMuted) return;

        // Jalur terbaik: mixer latensi rendah (suara langsung dicampur ke aliran audio yang sudah berjalan)
        if (mixerPlay(filepath, startSeconds)) return;

        // Jalur cepat: klip siap pakai, dimulai di thread audio (bukan thread tampilan)
        if (startSeconds <= 0) {
            final Clip ready;
            synchronized (SoundManager.class) {
                ready = readyClips.get(filepath);
            }
            if (ready != null) {
                final long requested = System.nanoTime();
                audioThread.execute(() -> {
                    try {
                        synchronized (ready) {
                            ready.stop();
                            ready.setFramePosition(0);
                            setClipVolume(ready, sfxVolume);
                            ready.start();
                        }
                        log("SFX " + filepath + " (klip siap) dimulai, jeda permintaan->start = "
                                + (System.nanoTime() - requested) / 1_000_000 + " ms");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
                return;
            }
            log("SFX " + filepath + " diputar lewat jalur LAMBAT (belum sempat dipreload)");
        }

        new Thread(() -> {
            try {
                File soundFile = new File(filepath);
                if (!soundFile.exists()) return;

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip sfxClip = AudioSystem.getClip();
                sfxClip.open(audioStream);

                setClipVolume(sfxClip, sfxVolume);

                long startMicroseconds = (long) (startSeconds * 1_000_000);
                if (startMicroseconds > 0 && startMicroseconds < sfxClip.getMicrosecondLength()) {
                    sfxClip.setMicrosecondPosition(startMicroseconds);
                }

                sfxClip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        sfxClip.close();
                    }
                });

                sfxClip.start();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // Memeriksa saat game dibuka: mencetak di console SETIAP file aset yang namanya tidak cocok dengan isi folder assets
    // (misal beda spasi vs garis bawah), supaya kesalahan nama langsung kelihatan.
    public static void checkAssets(String... extraPaths) {
        String[] audio = {SFX_TEXT_EFFECT, SFX_POP_UP, SFX_OPTION_BEST, SFX_OPTION_RISKY, SFX_OPTION_BAD,
                SFX_SAMMY_LARI, SFX_SAMMY_JATUH, SFX_SAMMY_KEJEDOT, SFX_CORRECT, SFX_INCORRECT, SFX_SLURP,
                AMB_ROAD, AMB_AC, AMB_KANTIN, BGM_MINI_GAME, BGM_GOOD_ENDING, BGM_BAD_ENDING};
        int missing = 0, total = 0;
        for (String[] group : new String[][]{audio, extraPaths}) {
            for (String p : group) {
                total++;
                if (!new File(p).exists()) {
                    missing++;
                    System.out.println("[ASET TIDAK DITEMUKAN] " + p + "  <- cek nama file di folder assets (spasi atau garis bawah?)");
                }
            }
        }
        System.out.println("[Cek aset] " + (total - missing) + " dari " + total + " file ditemukan"
                + (missing > 0 ? " -> " + missing + " NAMA PERLU DICOCOKKAN (lihat baris ASET TIDAK DITEMUKAN di atas)" : " - semua nama cocok"));
    }

    // --- 3b. DURASI FILE SUARA + PRELOAD ---
    private static final Map<String, Long> durationCache = new HashMap<>();

    // Durasi file suara dalam milidetik (dibaca dari header file, tanpa perlu perangkat audio).
    // Mengembalikan -1 kalau file tidak ada / tidak bisa dibaca.
    public static synchronized long getDurationMs(String filepath) {
        Long cached = durationCache.get(filepath);
        if (cached != null) return cached;

        long ms = -1;
        File f = new File(filepath);
        if (f.exists()) {
            try (AudioInputStream in = AudioSystem.getAudioInputStream(f)) {
                long frames = in.getFrameLength();
                float rate = in.getFormat().getFrameRate();
                if (frames > 0 && rate > 0 && frames / rate < 3 * 3600) ms = Math.round(frames * 1000.0 / rate);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        durationCache.put(filepath, ms);
        return ms;
    }

    // Muat file-file efek suara ke memori lebih awal (di thread terpisah) dan nyalakan mixer latensi rendah.
    // Panggil sekali saat game dibuka. Kalau mixer tidak bisa dinyalakan, file disiapkan sebagai klip siap-pakai
    // (jalur lama) plus "keep-alive" (lihat startKeepAlive).
    public static void preload(String... filepaths) {
        new Thread(() -> {
            for (String p : filepaths) {
                getDurationMs(p);
                File f = new File(p);
                if (!f.exists()) {
                    log("preload: " + p + " TIDAK DITEMUKAN");
                    continue;
                }
                try {
                    short[] decoded = decodeToMixFormat(f);
                    int[] removed = new int[2];               // [0] = ms hening di depan yang dibuang, [1] = di belakang
                    short[] pcm = trimSilence(decoded, removed);
                    long trimmedMs = pcm.length / 2 * 1000L / MIX_RATE;
                    synchronized (mixLock) {
                        sfxPcm.put(p, pcm);
                    }
                    synchronized (SoundManager.class) {
                        durationCache.put(p, trimmedMs);      // durasi yang dipakai sinkronisasi = durasi SETELAH dipotong
                    }
                    log("preload: " + p + " siap (" + trimmedMs + " ms; hening otomatis dibuang: depan "
                            + removed[0] + " ms, belakang " + removed[1] + " ms)");
                } catch (Exception e) {
                    log("preload: " + p + " tidak bisa didekode untuk mixer (" + e + ")");
                }
            }

            boolean mixerOk = startMixer();

            if (!mixerOk) {
                // Cadangan: klip siap-pakai (jalur lama, latensinya lebih besar)
                for (String p : filepaths) {
                    try {
                        File f = new File(p);
                        if (!f.exists()) continue;
                        Clip c = AudioSystem.getClip();
                        c.open(AudioSystem.getAudioInputStream(f));
                        synchronized (SoundManager.class) {
                            readyClips.put(p, c);
                        }
                    } catch (Exception e) {
                        log("preload (cadangan): " + p + " GAGAL disiapkan (" + e + ")");
                    }
                }
                startKeepAlive();
            }
        }, "SFX-Preload").start();
    }

    // "Keep-alive": memutar suara HENING tanpa henti di latar belakang. Perangkat audio (macOS) suka "tidur" kalau
    // tidak ada suara sama sekali, dan membangunkannya butuh puluhan sampai ratusan milidetik. Itulah yang bikin
    // suara pop-up/ketik pertama sering telat kalau sebelumnya hening. Dengan loop hening ini perangkat selalu siaga.
    private static Clip keepAliveClip;

    private static void startKeepAlive() {
        synchronized (SoundManager.class) {
            if (keepAliveClip != null) return;
        }
        try {
            AudioFormat fmt = new AudioFormat(44100f, 16, 2, true, false);
            byte[] silence = new byte[44100 * 4]; // 1 detik hening (semua nol)
            Clip c = AudioSystem.getClip();
            c.open(fmt, silence, 0, silence.length);
            c.loop(Clip.LOOP_CONTINUOUSLY);
            synchronized (SoundManager.class) {
                keepAliveClip = c;
            }
            log("keep-alive (loop hening) aktif");
        } catch (Exception e) {
            log("keep-alive gagal dinyalakan (" + e + ")");
        }
    }

    // --- 3c. SFX BERULANG (LOOP), dipakai untuk suara teks mengetik ---
    private static Clip loopClip;
    private static boolean loopClipShared = false; // true = klip milik readyClips (jangan di-close, dipakai ulang)
    private static int loopGeneration = 0;         // penanda "permintaan terbaru", biar start/stop yang berlomba tidak saling menimpa

    // Mulai memutar file berulang-ulang (tanpa jeda antar putaran) sampai stopLoopSFX() dipanggil.
    public static void startLoopSFX(String filepath) {
        if (isMuted) return;

        // Jalur terbaik: mixer latensi rendah (loop tanpa celah, mulai seketika)
        if (mixerStartLoop(filepath)) return;

        final int gen;
        synchronized (SoundManager.class) {
            gen = ++loopGeneration;
        }

        audioThread.execute(() -> {
            synchronized (SoundManager.class) {
                if (gen != loopGeneration) return; // sudah keburu diminta berhenti
                stopLoopClipNow();                 // loop sebelumnya (kalau masih ada) dihentikan

                // Jalur cepat: klip siap pakai -> rewind lalu loop, langsung bunyi
                Clip ready = readyClips.get(filepath);
                if (ready != null) {
                    try {
                        ready.stop();
                        ready.setFramePosition(0);
                        setClipVolume(ready, sfxVolume);
                        ready.loop(Clip.LOOP_CONTINUOUSLY);
                        loopClip = ready;
                        loopClipShared = true;
                        return;
                    } catch (Exception e) {
                        e.printStackTrace(); // gagal -> coba jalur biasa di bawah
                    }
                }
            }
            loadAndLoopSlow(filepath, gen);
        });
    }

    private static void loadAndLoopSlow(String filepath, int gen) {
        new Thread(() -> {
            try {
                File soundFile = new File(filepath);
                if (!soundFile.exists()) return;

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip clip = AudioSystem.getClip();
                clip.open(audioStream);
                setClipVolume(clip, sfxVolume);

                synchronized (SoundManager.class) {
                    if (gen != loopGeneration) { // sudah keburu diminta berhenti selagi file dimuat
                        clip.close();
                        return;
                    }
                    loopClip = clip;
                    loopClipShared = false;
                    clip.loop(Clip.LOOP_CONTINUOUSLY);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, "SFX-Loop").start();
    }

    // Hentikan loop dengan fade-out singkat (fadeMs) supaya tidak terdengar "klik"/terpotong kasar.
    public static void stopLoopSFX(int fadeMs) {
        mixerStopLoop(fadeMs);

        final Clip clip;
        final boolean shared;
        final int stopGen;
        synchronized (SoundManager.class) {
            stopGen = ++loopGeneration;
            clip = loopClip;
            shared = loopClipShared;
            loopClip = null;
        }
        if (clip == null) return;

        new Thread(() -> {
            try {
                int steps = 6;
                for (int i = steps - 1; i >= 0; i--) {
                    synchronized (SoundManager.class) {
                        if (shared && stopGen != loopGeneration) return; // klip dipakai lagi oleh loop baru: jangan diganggu
                    }
                    setClipVolume(clip, sfxVolume * i / steps);
                    Thread.sleep(Math.max(1, fadeMs / steps));
                }
            } catch (Exception ignored) {
            } finally {
                synchronized (SoundManager.class) {
                    if (!shared) {
                        clip.stop();
                        clip.close();
                    } else if (stopGen == loopGeneration) {
                        clip.stop();
                    }
                }
            }
        }, "SFX-LoopStop").start();
    }

    // Dipanggil di dalam blok synchronized
    private static void stopLoopClipNow() {
        if (loopClip != null) {
            loopClip.stop();
            if (!loopClipShared) loopClip.close();
            loopClip = null;
        }
    }

    // ------------------------------------------------------------------
    // 3d. MIXER SFX LATENSI RENDAH
    //
    // Kenapa suara pop/ketik terasa telat: Java "Clip" memasukkan suara ke buffer perangkat yang besar
    // (sekitar setengah detik), jadi suara baru terdengar SETELAH buffer itu "terpakai". Akibatnya bunyi "pop"
    // bisa baru terdengar saat sprite sudah selesai naik.
    //
    // Solusinya: satu aliran audio (SourceDataLine) dinyalakan SEKALI dan terus berjalan, diisi suara hening.
    // Antreannya sengaja dijaga sangat pendek (±23 ms). Saat ada efek suara, datanya langsung DICAMPUR ke aliran
    // itu, jadi terdengar dalam hitungan milidetik. Aliran yang selalu menyala juga membuat perangkat audio
    // tidak pernah "tidur", dan beberapa suara bisa berbunyi bersamaan (pop + mengetik + suara opsi).
    // Format aliran: 44100 Hz, 16-bit, stereo. File dengan format lain otomatis dikonversi saat preload.
    // ------------------------------------------------------------------
    private static final int MIX_RATE = 44100;
    private static final int MIX_CHUNK_FRAMES = 512;          // ±11,6 ms per potongan yang dicampur
    private static final int MIX_QUEUE_CHUNKS = 2;            // antrean maksimal di perangkat: 2 potongan (±23 ms)
    private static final int MIX_LINE_BUFFER_FRAMES = 4096;   // ukuran buffer yang diminta ke perangkat

    private static class Voice {
        final short[] pcm;        // stereo interleaved (L,R,L,R,...)
        final boolean loop;
        final float gain;
        int pos = 0;              // posisi (dalam frame)
        int fadeLeft = -1;        // sisa frame fade-out (-1 = tidak sedang fade)
        int fadeTotal = 1;
        boolean done = false;

        Voice(short[] pcm, boolean loop, float gain) {
            this.pcm = pcm;
            this.loop = loop;
            this.gain = gain;
        }
    }

    private static final Object mixLock = new Object();
    private static final List<Voice> voices = new ArrayList<>();
    private static final Map<String, short[]> sfxPcm = new HashMap<>();
    private static Voice mixLoopVoice = null;
    private static SourceDataLine mixLine;
    private static volatile boolean mixerRunning = false;

    private static boolean startMixer() {
        try {
            AudioFormat fmt = new AudioFormat(MIX_RATE, 16, 2, true, false);
            DataLine.Info info = new DataLine.Info(SourceDataLine.class, fmt);
            if (!AudioSystem.isLineSupported(info)) {
                log("mixer SFX GAGAL: perangkat tidak mendukung SourceDataLine 44,1 kHz stereo");
                return false;
            }
            SourceDataLine line = (SourceDataLine) AudioSystem.getLine(info);
            line.open(fmt, MIX_LINE_BUFFER_FRAMES * 4);
            line.start();
            mixLine = line;
            mixerRunning = true;

            Thread t = new Thread(SoundManager::mixerLoop, "SFX-Mixer");
            t.setDaemon(true);
            t.setPriority(Thread.MAX_PRIORITY);
            t.start();

            log("mixer SFX aktif (buffer perangkat " + line.getBufferSize() / 4 + " frame = "
                    + line.getBufferSize() / 4 * 1000 / MIX_RATE + " ms; antrean dijaga ±"
                    + MIX_CHUNK_FRAMES * MIX_QUEUE_CHUNKS * 1000 / MIX_RATE + " ms)");
            return true;
        } catch (Exception e) {
            log("mixer SFX GAGAL dinyalakan (" + e + ") -> memakai jalur lama yang lebih lambat");
            return false;
        }
    }

    // Thread yang terus mencampur semua suara aktif menjadi potongan-potongan kecil dan mengirimnya ke perangkat
    private static void mixerLoop() {
        final int chunkBytes = MIX_CHUNK_FRAMES * 4;
        final byte[] out = new byte[chunkBytes];
        final int[] acc = new int[MIX_CHUNK_FRAMES * 2];
        final int targetQueue = chunkBytes * MIX_QUEUE_CHUNKS;
        final SourceDataLine line = mixLine;
        final int bufSize = line.getBufferSize();
        long statAt = System.currentTimeMillis() + 3000;
        int statsLeft = 3, emptyQueue = 0;

        try {
            while (mixerRunning) {
                if (statsLeft > 0 && System.currentTimeMillis() >= statAt) {
                    statsLeft--;
                    statAt += 5000;
                    log("mixer SFX sehat? antrean ±" + (bufSize - line.available()) / 4 * 1000 / MIX_RATE
                            + " ms, antrean kosong " + emptyQueue + "x (kalau sering >0 dan suara retak, naikkan MIX_QUEUE_CHUNKS)");
                }
                // Jaga antrean di perangkat tetap pendek, supaya suara baru tidak menunggu di belakang antrean panjang
                int queued = bufSize - line.available();
                if (queued <= 0) emptyQueue++;
                if (queued >= targetQueue) {
                    Thread.sleep(1);
                    continue;
                }

                Arrays.fill(acc, 0);
                synchronized (mixLock) {
                    for (Iterator<Voice> it = voices.iterator(); it.hasNext(); ) {
                        Voice v = it.next();
                        renderVoice(v, acc);
                        if (v.done) it.remove();
                    }
                }

                for (int i = 0; i < acc.length; i++) {
                    int sample = softLimit(acc[i]);
                    out[2 * i] = (byte) sample;
                    out[2 * i + 1] = (byte) (sample >> 8);
                }
                line.write(out, 0, chunkBytes);
            }
        } catch (Exception e) {
            mixerRunning = false;
            log("mixer SFX BERHENTI karena error (" + e + ")");
        }
    }

    // Pembatas halus: nilai sampai ±29000 (±-1 dBFS) lewat apa adanya; di atas itu dilipat pelan-pelan mendekati batas
    // 16-bit (bukan dipotong keras), jadi suara yang di-boost (gain > 1) tidak "pecah".
    private static final int LIMIT_KNEE = 29000;

    private static int softLimit(int x) {
        int a = Math.abs(x);
        if (a <= LIMIT_KNEE) return x;
        double room = 32767 - LIMIT_KNEE;
        int y = (int) (LIMIT_KNEE + room * Math.tanh((a - LIMIT_KNEE) / room));
        return x < 0 ? -y : y;
    }

    // Menambahkan satu potongan (MIX_CHUNK_FRAMES frame) dari sebuah suara ke akumulator
    private static void renderVoice(Voice v, int[] acc) {
        final short[] pcm = v.pcm;
        final int frames = pcm.length / 2;
        if (frames == 0) {
            v.done = true;
            return;
        }
        for (int f = 0; f < MIX_CHUNK_FRAMES; f++) {
            if (v.pos >= frames) {
                if (v.loop) v.pos = 0;
                else {
                    v.done = true;
                    return;
                }
            }
            float g = v.gain;
            if (v.fadeLeft >= 0) {
                if (v.fadeLeft == 0) {
                    v.done = true;
                    return;
                }
                g *= (float) v.fadeLeft / v.fadeTotal;
                v.fadeLeft--;
            }
            acc[2 * f] += (int) (pcm[2 * v.pos] * g);
            acc[2 * f + 1] += (int) (pcm[2 * v.pos + 1] * g);
            v.pos++;
        }
    }

    // Memutar suara sekali lewat mixer. Mengembalikan false kalau mixer tidak siap / file belum didekode.
    private static boolean mixerPlay(String filepath, double startSeconds) {
        return mixerPlayVoice(filepath, startSeconds) != null;
    }

    // Sama seperti mixerPlay, tapi mengembalikan "pegangan" suara yang sedang berbunyi (null kalau gagal)
    private static Voice mixerPlayVoice(String filepath, double startSeconds) {
        return mixerPlayVoice(filepath, startSeconds, 1.0f);
    }

    private static Voice mixerPlayVoice(String filepath, double startSeconds, float gain) {
        if (!mixerRunning) return null;
        short[] pcm;
        synchronized (mixLock) {
            pcm = sfxPcm.get(filepath);
        }
        if (pcm == null) return null;

        Voice v = new Voice(pcm, false, sfxVolume * gain);
        v.pos = Math.max(0, (int) (startSeconds * MIX_RATE));
        synchronized (mixLock) {
            voices.add(v);
        }
        int queuedMs = -1;
        try {
            queuedMs = (mixLine.getBufferSize() - mixLine.available()) / 4 * 1000 / MIX_RATE;
        } catch (Exception ignored) {
        }
        log("SFX " + filepath + " dicampur ke mixer (antrean perangkat saat ini ±" + queuedMs + " ms = perkiraan latensi)");
        return v;
    }

    private static boolean mixerStartLoop(String filepath) {
        if (!mixerRunning) return false;
        short[] pcm;
        synchronized (mixLock) {
            pcm = sfxPcm.get(filepath);
        }
        if (pcm == null) return false;

        Voice v = new Voice(pcm, true, sfxVolume);
        synchronized (mixLock) {
            fadeOutVoice(mixLoopVoice, 2); // loop sebelumnya (kalau ada) dihentikan
            mixLoopVoice = v;
            voices.add(v);
        }
        return true;
    }

    private static void mixerStopLoop(int fadeMs) {
        synchronized (mixLock) {
            if (mixLoopVoice != null) {
                fadeOutVoice(mixLoopVoice, Math.max(1, fadeMs) * MIX_RATE / 1000);
                mixLoopVoice = null;
            }
        }
    }

    private static void fadeOutVoice(Voice v, int frames) {
        if (v == null) return;
        v.fadeTotal = Math.max(1, frames);
        v.fadeLeft = v.fadeTotal;
    }

    // Memotong keheningan di depan & belakang suara secara OTOMATIS (dijalankan saat preload), jadi bunyinya langsung
    // mulai walau file di folder assets masih versi mentah yang ada hening panjang di depan.
    //  - "hening" = di bawah ±-50 dBFS
    //  - bunyi "nyasar" yang pelan (< -35 dBFS) dan pendek (< 300 ms) di ujung, yang dipisahkan hening >= 250 ms dari
    //    isi utama, ikut dibuang (contoh: bunyi "tik" kecil sebelum hening panjang)
    //  - isi di tengah tidak pernah disentuh
    private static short[] trimSilence(short[] pcm, int[] removedMs) {
        final int frames = pcm.length / 2;
        final int win = MIX_RATE / 100;                 // jendela 10 ms
        final int nWin = frames / win;
        if (nWin < 3) return pcm;

        final int thr = 104;                            // ≈ -50 dBFS
        final int quietPeak = 590;                      // ≈ -35 dBFS
        int[] peak = new int[nWin];
        for (int w = 0; w < nWin; w++) {
            int p = 0;
            for (int i = w * win; i < (w + 1) * win; i++) {
                p = Math.max(p, Math.max(Math.abs(pcm[2 * i]), Math.abs(pcm[2 * i + 1])));
            }
            peak[w] = p;
        }

        // Kelompokkan jendela aktif menjadi segmen (dipisah hening >= 250 ms = 25 jendela)
        List<int[]> segs = new ArrayList<>();       // {jendelaAwal, jendelaAkhir, puncak}
        int segStart = -1, last = -1, segPeak = 0;
        for (int w = 0; w < nWin; w++) {
            if (peak[w] > thr) {
                if (segStart < 0) { segStart = w; segPeak = 0; }
                else if (w - last > 25) {
                    segs.add(new int[]{segStart, last, segPeak});
                    segStart = w; segPeak = 0;
                }
                last = w;
                segPeak = Math.max(segPeak, peak[w]);
            }
        }
        if (segStart < 0) return pcm;                  // semuanya hening: biarkan apa adanya
        segs.add(new int[]{segStart, last, segPeak});

        while (segs.size() > 1 && (segs.get(0)[1] - segs.get(0)[0] + 1) * 10 < 300 && segs.get(0)[2] < quietPeak) segs.remove(0);
        while (segs.size() > 1) {
            int[] tail = segs.get(segs.size() - 1);
            if ((tail[1] - tail[0] + 1) * 10 < 300 && tail[2] < quietPeak) segs.remove(segs.size() - 1);
            else break;
        }

        int firstFrame = segs.get(0)[0] * win;
        while (firstFrame < frames && Math.max(Math.abs(pcm[2 * firstFrame]), Math.abs(pcm[2 * firstFrame + 1])) <= thr) firstFrame++;
        int lastFrame = Math.min(frames - 1, (segs.get(segs.size() - 1)[1] + 1) * win - 1);
        while (lastFrame > firstFrame && Math.max(Math.abs(pcm[2 * lastFrame]), Math.abs(pcm[2 * lastFrame + 1])) <= thr) lastFrame--;

        int start = Math.max(0, firstFrame - MIX_RATE * 5 / 1000);          // sisakan 5 ms di depan
        int end = Math.min(frames, lastFrame + 1 + MIX_RATE * 12 / 1000);   // dan 12 ms di belakang
        removedMs[0] = start * 1000 / MIX_RATE;
        removedMs[1] = (frames - end) * 1000 / MIX_RATE;
        if (start == 0 && end == frames) return pcm;
        return Arrays.copyOfRange(pcm, start * 2, end * 2);
    }

    // Membaca file WAV apa pun (PCM 8/16/24-bit, mono/stereo, laju sampel bebas) menjadi stereo 16-bit 44,1 kHz
    private static short[] decodeToMixFormat(File f) throws Exception {
        try (AudioInputStream raw = AudioSystem.getAudioInputStream(f)) {
            AudioFormat src = raw.getFormat();
            int ch = src.getChannels();
            AudioFormat pcm16 = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, src.getSampleRate(), 16,
                    ch, ch * 2, src.getSampleRate(), false);
            byte[] bytes;
            try (AudioInputStream in = AudioSystem.getAudioInputStream(pcm16, raw)) {
                bytes = in.readAllBytes();
            }

            int frames = bytes.length / (2 * ch);
            float[] left = new float[frames];
            float[] right = new float[frames];
            for (int i = 0; i < frames; i++) {
                int base = i * ch * 2;
                short l = (short) ((bytes[base] & 0xFF) | (bytes[base + 1] << 8));
                short r = l;
                if (ch >= 2) r = (short) ((bytes[base + 2] & 0xFF) | (bytes[base + 3] << 8));
                left[i] = l;
                right[i] = r;
            }

            // Ubah laju sampel ke 44,1 kHz (interpolasi linear) kalau perlu
            double ratio = src.getSampleRate() / (double) MIX_RATE;
            int outFrames = (int) Math.round(frames / ratio);
            short[] out = new short[outFrames * 2];
            for (int n = 0; n < outFrames; n++) {
                double pos = n * ratio;
                int i0 = (int) pos;
                int i1 = Math.min(frames - 1, i0 + 1);
                float frac = (float) (pos - i0);
                if (i0 >= frames) i0 = frames - 1;
                out[2 * n] = (short) Math.round(left[i0] + (left[i1] - left[i0]) * frac);
                out[2 * n + 1] = (short) Math.round(right[i0] + (right[i1] - right[i0]) * frac);
            }
            return out;
        }
    }

    // --- 4. FITUR KONTROL VOLUME & MUTE ---
    public static void setBGMVolume(float volume) { // Nilai 0.0f sampai 1.0f
        bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
        Clip clip;
        synchronized (SoundManager.class) {
            clip = bgmClip;
        }
        if (clip != null && clip.isRunning()) {
            setClipVolume(clip, bgmVolume);
        }
    }

    public static void setSFXVolume(float volume) {
        sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public static void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            stopBGM();
            stopLoopSFX(0);
            synchronized (mixLock) {
                voices.clear();
            }
        }
    }

    public static boolean isMuted() {
        return isMuted;
    }

    // Helper internal untuk atur Desibel (dB) Gain Control
    private static void setClipVolume(Clip clip, float volume) {
        try {
            if (clip != null && clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
                FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
                if (volume <= 0.0f) {
                    gainControl.setValue(gainControl.getMinimum());
                } else {
                    // Konversi skala linier (0-1) ke bentuk Decibels (dB)
                    float dB = (float) (Math.log10(volume) * 20.0);
                    gainControl.setValue(Math.max(gainControl.getMinimum(), Math.min(gainControl.getMaximum(), dB)));
                }
            }
        } catch (Exception ignored) {
            // klip sudah ditutup oleh thread lain: abaikan
        }
    }
}