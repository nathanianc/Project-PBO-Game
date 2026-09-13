import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StoryDataLoader {

    private static final Map<String, Character> characters = new HashMap<>();
    private static final Map<String, Background> backgrounds = new HashMap<>();

    private static void initAssets() {
        if (!characters.isEmpty()) return;

        // --- BACKGROUNDS ---
        backgrounds.put("otista", new Background("Otista III", "assets/bg_otista.png"));
        backgrounds.put("kelas", new Background("Ruang Kelas PBO", "assets/bg-kelas.jpeg"));
        backgrounds.put("kos", new Background("Kamar Kos", "assets/bg_kos.png"));
        backgrounds.put("bem", new Background("Sekretariat BEM", "assets/bg_bem.png"));
        backgrounds.put("lab_komputer", new Background("Lab Komputer", "assets/bg_lab.png"));
        backgrounds.put("presentasi", new Background("Kelas Presentasi", "assets/presentasi.png"));

        // POSE SAMMY
        characters.put("sammy_normal", new Character("Sammy", "Mahasiswa STIS", "assets/sammy.png"));
        characters.put("sammy_panik", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_panik.png"));
        characters.put("sammy_pusing", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_pusing.png"));
        characters.put("sammy_nyontek", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_nyontek.png"));
        characters.put("sammy_jengkel", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_jengkel.png"));
        characters.put("sammy_takut", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_takut.png"));
        characters.put("sammy_tenang", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_tenang.png"));
        characters.put("sammy_marah", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_marah.png"));
        characters.put("sammy_kesal", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_kesal.png"));

        // POSE DHITO
        characters.put("dhito", new Character("Dhito", "Teman Seangkatan", "assets/dhito.png"));
        characters.put("dhito_jengkel", new Character("Dhito", "Teman Seangkatan", "assets/dhito_jengkel.png"));

        // POSE PAK IBNU
        characters.put("pak_ibnu", new Character("Pak Ibnu", "Dosen PBO", "assets/pak_ibnu.png"));
        characters.put("ibnu_tanya", new Character("Pak Ibnu", "Dosen PBO", "assets/ibnu_tanya.png"));

        // POSE THANIA
        characters.put("thania", new Character("Nathania", "Teman Kelompok", "assets/thania.png"));

        // POSE NELA
        characters.put("nela", new Character("Nela", "Teman Kelompok", "assets/nela.png"));


    }

    private static Character getChar(String key) { return characters.get(key); }
    private static Background getBg(String key) { return backgrounds.get(key); }

    public static List<Scene> loadAllScenes() {
        initAssets();

        List<Scene> scenes = new ArrayList<>();
        scenes.addAll(createScene1Branch());
        scenes.addAll(createScene2Branch());
        scenes.addAll(createScene6Branch());
        scenes.addAll(createScene7Branch());

        return scenes;
    }

    // --- SCENE 1 ---
    private static List<Scene> createScene1Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s1 = new Scene(1, "Terlambat Orientasi", getBg("otista"));
        s1.addDialog("Sammy: \"Aduh aku udah telat masuk gerbang Kampus Otista! Gimana nih?\"", getChar("sammy_pusing"));

        // Pilihan mengarah ke Sub-Scene ID 11, 12, 13
        s1.addOption("Memanjat pagar samping", 11, new RiskyOptionStrategy());
        s1.addOption("Masuk lewat pintu belakang diam-diam", 12, new BestOptionStrategy());
        s1.addOption("Menunggu orientasi selesai di luar", 13, new BadOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene 2
        Scene s11 = new Scene(11, "Terlambat Orientasi", getBg("otista"), 2);
        s11.addDialog("Satpam: \"Lapor BAAK dulu sana!\"", getChar("sammy_kejedot"));

        Scene s12 = new Scene(12, "Terlambat Orientasi", getBg("otista"), 2);
        s12.addDialog("Sammy: \"Fyuh... aman, ga ketahuan senior.\"", getChar("sammy_happy"));

        Scene s13 = new Scene(13, "Terlambat Orientasi", getBg("otista"), 2);
        s13.addDialog("Dhito: \"Dih, malah nongkrong di warkop?\"", getChar("dhito"));

        branch.add(s1);
        branch.add(s11);
        branch.add(s12);
        branch.add(s13);
        return branch;
    }

    // --- SCENE 2 ---
    private static List<Scene> createScene2Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s2 = new Scene(2, "Quiz Mendadak PBO", getBg("kelas"));
        s2.addDialog("Pak Ibnu: \"Oll, salam kenal saya Pak Ibnu. Hari ini kita quiz mendadak PBO ya!\"", getChar("pak_ibnu"));
        s2.addDialog("Sammy: \"Aduhh baru juga masuk perkuliahan, kok udah quiz aja. Gimana nih?\"", getChar("sammy_panik"));

        s2.addOption("Jawab sesuai keyakinan & analisa sendiri", 21, new BestOptionStrategy());
        s2.addOption("Menyontek jawaban Dhito", 22, new BadOptionStrategy());
        s2.addOption("Minta izin ke toilet karena cemas", 23, new RiskyOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene 6
        Scene s21 = new Scene(21, "Quiz Mendadak PBO", getBg("kelas"), 6);
        s21.addDialog("Sammy: \"YASSS! Soalnya bisa kukerjakan!\"", getChar("sammy_happy"));

        Scene s22 = new Scene(22, "Quiz Mendadak PBO", getBg("kelas"), 6);
        s22.addDialog("Sammy: \"Dhit, jawabannya apa?\"", getChar("sammy_nyontek"));
        s22.addDialog("Dhito: \"C\"",getChar("dhito"));
        s22.addDialog("tetot");
        s22.addDialog("Sammy: \"Gimana sih, dhit?\"", getChar("sammy_jengkel"));
        s22.addDialog("Dhito: \"Yo ndak tau, salah sendiri nanya-nanya. Wong aku ya bingung.\"", getChar("dhito_jengkel"));

        Scene s23 = new Scene(23, "Quiz Mendadak PBO", getBg("kelas"), 6);
        s23.addDialog("Sammy: \"Pak, saya izin ke toilet sebentar...\"", getChar("sammy_kejedot"));
        s23.addDialog("Oke guys, kita tunggu Sammy ya!");

        branch.add(s2);
        branch.add(s21);
        branch.add(s22);
        branch.add(s23);
        return branch;
    }

    // --- SCENE 6 (MINI GAME) ---
    private static List<Scene> createScene6Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s6 = new Scene(6, "Troubleshooting Program", getBg("lab_komputer"), 60);
        s6.addDialog("Nat: \"Guys, aku lagi jalanin program kita dan muncul error. Minta tolong diperbaikin ges.\"", getChar("thania"));
        s6.addDialog("Sammy: \"Iya, kayanya ada urutan kodingan yang salah, deh.\"", getChar("sammy_pusing"));
        s6.addDialog("Dhito: \"Kita suruh penonton kita untuk perbaikin aja kali ya?\"", getChar("dhito"));

        Scene s6_game = new Scene(60, "Puzzle Inheritance Java", getBg("lab_komputer"));
        s6_game.setMiniGame(true);

        Scene s6_win = new Scene(61, "Troubleshooting Berhasil", getBg("lab_komputer"), 7);
        s6_win.addDialog("Nat: \"Wah, mantap! Programnya langsung jalan tanpa error!\"", getChar("thania"));
        s6_win.addDialog("Sammy: \"Yesss! Makasih ya udah bantuin urutin kodingannya!\"", getChar("sammy_happy"));
        s6_win.addDialog("Dhito: \"Gas lanjut garap bagian lain!\"", getChar("dhito"));

        Scene s6_lose = new Scene(62, "Troubleshooting Gagal", getBg("lab_komputer"), 7);
        s6_lose.addDialog("Nat: \"Aduh, masih error nih. Kayaknya urutannya masih ada yang kebalik.\"", getChar("thania"));
        s6_lose.addDialog("Sammy: \"Huft... ya sudah deh, biar aku coba periksa manual lagi.\"", getChar("sammy_pusing"));

        branch.add(s6);
        branch.add(s6_game);
        branch.add(s6_win);
        branch.add(s6_lose);
        return branch;
    }

    private static List<Scene> createScene7Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s7 = new Scene(7, "Presentasi di Kelas", getBg("presentasi"));
        s7.addDialog("Sammy: \"Guys, kok tiba-tiba crash ya...\"", getChar("sammy_takut"));
        s7.addDialog("Pak Ibnu: \"Sammy, program kamu kok berhenti pas manggil inheritance Hewan. Ada apa?\"", getChar("ibnu_tanya"));

        s7.addOption("Tetap tenang & jujur menjelaskan exception secara teknis", 71, new BestOptionStrategy());
        s7.addOption("Menyalahkan koneksi internet lab & restart laptop", 72, new BadOptionStrategy());
        s7.addOption("Berargumen dengan dosen menyalahkan data uji coba", 73, new RiskyOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene 2
        Scene s71 = new Scene(71, "Presentasi di Kelas", getBg("presentasi"), 8);
        s71.addDialog("Sammy: \"Mohon maaf Pak, ada kesalahan logika override pada parent class Hewan, tapi saya sudah siapkan blok try-catch ini untuk menangani exception-nya secara langsung.\"", getChar("sammy_tenang"));

        Scene s72 = new Scene(72, "Presentasi di Kelas", getBg("presentasi"), 8);
        s72.addDialog("Sammy: \"Wah, ini pasti gara-gara koneksi Wi-Fi lab lemot banget Pak! Saya restart laptop dulu ya!\"", getChar("sammy_marah"));

        Scene s73 = new Scene(73, "Presentasi di Kelas", getBg("presentasi"), 8);
        s73.addDialog("Sammy: \"Bukan salah kodenya Pak, tapi data uji coba dari Bapak yang input-nya aneh dan gak sesuai!\"", getChar("sammy_kesal"));

        branch.add(s7);
        branch.add(s71);
        branch.add(s72);
        branch.add(s73);
        return branch;
    }
}