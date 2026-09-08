import javax.sound.sampled.*;
import java.io.File;

public class SoundManager {
    private static Clip bgmClip;
    private static float bgmVolume = 0.8f; // 0.0 (Hening) sampai 1.0 (Maksimal)
    private static float sfxVolume = 1.0f;
    private static boolean isMuted = false;

    // --- 1. PLAY BGM DENGAN CROSSFADE MULUS ---
    public static void playBGM(String filepath) {
        if (isMuted) return;

        new Thread(() -> {
            try {
                File soundFile = new File(filepath);
                if (!soundFile.exists()) return;

                AudioInputStream audioStream = AudioSystem.getAudioInputStream(soundFile);
                Clip newClip = AudioSystem.getClip();
                newClip.open(audioStream);

                // Atur volume BGM awal
                setClipVolume(newClip, bgmVolume);
                newClip.loop(Clip.LOOP_CONTINUOUSLY);

                // Jika ada BGM lama yang jalan, lakukan Crossfade!
                if (bgmClip != null && bgmClip.isRunning()) {
                    crossfadeBGM(bgmClip, newClip);
                } else {
                    bgmClip = newClip;
                    bgmClip.start();
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static void crossfadeBGM(Clip oldClip, Clip newClip) {
        new Thread(() -> {
            try {
                newClip.start();
                int steps = 15;
                int durationMs = 800; // Durasi crossfade 0.8 detik
                int sleepTime = durationMs / steps;

                for (int i = 0; i <= steps; i++) {
                    float factor = (float) i / steps;
                    setClipVolume(oldClip, bgmVolume * (1.0f - factor));
                    setClipVolume(newClip, bgmVolume * factor);
                    Thread.sleep(sleepTime);
                }

                oldClip.stop();
                oldClip.close();
                bgmClip = newClip;

            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    // --- 2. STOP BGM DENGAN FADE OUT ---
    public static void stopBGMWithFade(int fadeDurationMs) {
        if (bgmClip == null || !bgmClip.isRunning()) return;

        new Thread(() -> {
            try {
                int steps = 15;
                int sleepTime = fadeDurationMs / steps;

                for (int i = steps; i >= 0; i--) {
                    if (bgmClip == null) break;
                    float factor = (float) i / steps;
                    setClipVolume(bgmClip, bgmVolume * factor);
                    Thread.sleep(sleepTime);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                stopBGM();
            }
        }).start();
    }

    public static void stopBGM() {
        if (bgmClip != null) {
            if (bgmClip.isRunning()) bgmClip.stop();
            bgmClip.close();
            bgmClip = null;
        }
    }

    // --- 3. PLAY SFX DENGAN VOLUME & OFFSET ---
    public static void playSFX(String filepath) {
        playSFXWithOffset(filepath, 0.0);
    }

    public static void playSFXWithOffset(String filepath, double startSeconds) {
        if (isMuted) return;

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

    // --- 4. FITUR KONTROL VOLUME & MUTE ---
    public static void setBGMVolume(float volume) { // Nilai 0.0f sampai 1.0f
        bgmVolume = Math.max(0.0f, Math.min(1.0f, volume));
        if (bgmClip != null && bgmClip.isRunning()) {
            setClipVolume(bgmClip, bgmVolume);
        }
    }

    public static void setSFXVolume(float volume) {
        sfxVolume = Math.max(0.0f, Math.min(1.0f, volume));
    }

    public static void toggleMute() {
        isMuted = !isMuted;
        if (isMuted) {
            stopBGM();
        }
    }

    public static boolean isMuted() {
        return isMuted;
    }

    // Helper internal untuk atur Desibel (dB) Gain Control
    private static void setClipVolume(Clip clip, float volume) {
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
    }
}