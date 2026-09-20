public class Dialog {
    private String text;
    private Character speaker;
    private String[] sounds = new String[0];   // efek suara yang mengiringi baris ini (berurutan)
    private float[] soundGains = new float[0];  // pengali volume untuk tiap suara di withSounds() (1.0 = normal)
    private String[] effects = new String[0];  // efek suara "sekali bunyi" di awal baris ini

    public Dialog(String text, Character speaker) {
        this.text = text;
        this.speaker = speaker;
    }

    // Pasang efek suara untuk baris dialog ini. Suara dimainkan BERURUTAN: yang pertama mulai bersamaan dengan
    // teks, tiap suara berikutnya mulai setelah suara sebelumnya selesai. Teks pada baris ini otomatis mengetik
    // selama total durasi suara-suara itu (jadi tulisan & bunyinya jalan bareng), dan suara mengetik biasa
    // dimatikan supaya tidak menutupi efek suaranya.
    // Contoh: s11.addDialog("...", sprite).withSounds(SoundManager.SFX_SAMMY_LARI, SoundManager.SFX_SAMMY_JATUH);
    public Dialog withSounds(String... paths) {
        this.sounds = paths;
        return this;
    }

    // Mengatur volume tiap suara pada withSounds(), urut sesuai daftar suaranya. 1.0 = volume asli file,
    // 2.0 = dua kali lebih keras (+6 dB), dst. Suara yang jadi lebih keras dari batas akan dihaluskan (soft limiter),
    // jadi tidak pecah. Contoh: .withSounds(lari, jatuh).withSoundGains(3.0f, 1.4f)
    public Dialog withSoundGains(float... gains) {
        this.soundGains = gains;
        return this;
    }

    // Efek suara SEKALI BUNYI di awal baris ini (misal jingle "benar"). Beda dengan withSounds(): kecepatan ketik dan
    // suara mengetik TIDAK berubah, dan suaranya dibiarkan berbunyi sampai habis walau teks di-skip / pindah baris.
    public Dialog withSoundEffect(String... paths) {
        this.effects = paths;
        return this;
    }

    public String getText() { return text; }
    public Character getSpeaker() { return speaker; }
    public String[] getSounds() { return sounds; }
    public float getSoundGain(int index) { return index < soundGains.length ? soundGains[index] : 1.0f; }
    public String[] getEffects() { return effects; }
}