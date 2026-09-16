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
        backgrounds.put("otista", new Background("Otista III", "assets/otista.jpg"));
        backgrounds.put("gerbang_samping", new Background("Gerbang Samping StiS", "assets/gerbang_samping.jpg"));
        backgrounds.put("audit", new Background("Auditorium STIS", "assets/audit_pkkmb.jpg"));
        backgrounds.put("kelas", new Background("Ruang Kelas PBO", "assets/bg-kelas.jpeg"));
        backgrounds.put("kos", new Background("Kamar Kos", "assets/kos.jpg"));
        backgrounds.put("warnet", new Background("Warnet", "assets/warnet.jpg"));
        backgrounds.put("lab_komputer", new Background("Lab Komputer", "assets/bg_lab.png"));
        backgrounds.put("lobby", new Background("Lab Komputer", "assets/lobby.jpg"));
        backgrounds.put("ruang_presentasi", new Background("presentasi", "assets/presentasi.jpeg"));
        backgrounds.put("audit_end", new Background("Audit Ending", "assets/audit8.jpeg"));
        backgrounds.put("kantin", new Background("Kantin STIS", "assets/kantin.jpg"));
        // POSE SAMMY
        characters.put("sammy_telat", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_telat.png"));
        characters.put("sammy_lega", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_lega.png"));
        characters.put("sammy_normal", new Character("Sammy", "Mahasiswa STIS", "assets/sammy.png"));
        characters.put("sammy_panik", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_panik.png"));
        characters.put("sammy_pusing", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_pusing.png"));
        characters.put("sammy_nyontek", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_nyontek.png"));
        characters.put("sammy_jengkel", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_jengkel.png"));
        characters.put("sammy_yes", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_yes.png"));
        characters.put("sammy_kebelet", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_kebelet.png"));
        characters.put("sammy_melas", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_melas.png"));
        characters.put("sammy_minta", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_minta.png"));
        characters.put("sammy_lari", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_lari.png"));
        characters.put("sammy_laptop", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_laptop.png"));
        characters.put("sammy_nugas", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_nugas.png"));
        characters.put("sammy_pinjol", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_pinjol.png"));
        characters.put("sammy_mie", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_mie.png"));
        characters.put("sammy_pasrah_mie", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_pasrah_mie.png"));
        characters.put("sammy_dipanggil", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_dipanggil.png"));
        characters.put("sammy_ngantuk", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_ngantuk.png"));
        characters.put("sammy_bijak", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_bijak.png"));
        characters.put("sammy_hormat", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_hormat.png"));
        characters.put("sammy_capek", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_capek.png"));
        characters.put("sammy_cuek", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_cuek.png"));
        characters.put("sammy_opini", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_opini.png"));
        characters.put("sammy_bingung", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_bingung.png"));
        characters.put("sammy_kejedot", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_kejedot.png"));
        characters.put("sammy_tenang", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_tenang.png"));
        characters.put("sammy_ngeles", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_ngeles.png"));
        characters.put("sammy_ngeyel", new Character("Sammy", "Mahasiswa STIS", "assets/sammy_ngeyel.png"));
        //SATPAM
        characters.put("satpam", new Character("Satpam", "Mahasiswa STIS", "assets/satpam_marah.png"));

        // POSE DHITO
        characters.put("dhito", new Character("Dhito", "Teman Seangkatan", "assets/dhito.png"));
        characters.put("dhito_jengkel", new Character("Dhito", "Teman Seangkatan", "assets/dhito_jengkel.png"));
        characters.put("dhito_panik", new Character("Dhito", "Teman Seangkatan", "assets/dhito_panik.png"));
        characters.put("dhito_cemas", new Character("Dhito", "Teman Seangkatan", "assets/dhito_cemas.png"));
        characters.put("dhito_setuju", new Character("Dhito", "Teman Seangkatan", "assets/dhito_setuju.png"));

        // POSE PAK IBNU
        characters.put("pak_ibnu", new Character("Pak Ibnu", "Dosen PBO", "assets/pak_ibnu.png"));
        characters.put("pak_ibnu_biasa", new Character("Pak Ibnu", "Dosen PBO", "assets/pak_ibnu_biasa.png"));
        // POSE THANIA
        characters.put("thania", new Character("Nathania", "Teman Kelompok", "assets/thania.png"));
        characters.put("thania_suntuk", new Character("Nathania", "Teman Kelompok", "assets/nat_suntuk.png"));
        characters.put("thania_marah", new Character("Nathania", "Teman Kelompok", "assets/nat_marah.png"));
        characters.put("thania_tanya", new Character("Nathania", "Teman Kelompok", "assets/nat_tanya.png"));
        characters.put("thania_tunjuk", new Character("Nathania", "Teman Kelompok", "assets/nat_tunjuk.png"));
        characters.put("thania_khawatir", new Character("Nathania", "Teman Kelompok", "assets/nat_khawatir.png"));

        // POSE NELA
        characters.put("nela_manggil", new Character("Nela", "Teman Kelompok", "assets/nela_manggil.png"));
        characters.put("nela_senyum", new Character("Nela", "Teman Kelompok", "assets/nela_senyum.png"));


    }

    private static Character getChar(String key) { return characters.get(key); }
    private static Background getBg(String key) { return backgrounds.get(key); }

    public static List<Scene> loadAllScenes() {
        initAssets();

        List<Scene> scenes = new ArrayList<>();
        scenes.addAll(createScene1Branch());
        scenes.addAll(createScene2Branch());
        scenes.addAll(createScene3Branch());
        scenes.addAll(createScene4Branch());
        scenes.addAll(createScene5Branch());
        scenes.addAll(createScene6Branch());
        scenes.addAll(createScene7Branch());
        scenes.addAll(createScene8Branch());

        return scenes;
    }

    // --- SCENE 1 ---
    private static List<Scene> createScene1Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s1 = new Scene(1, "Terlambat Orientasi", getBg("otista"));
        s1.addDialog("Sammy: \"Aduh baru hari pertama udah telat! Gimana nih?\"", getChar("sammy_telat"));

        // Pilihan mengarah ke Sub-Scene ID 11, 12, 13
        s1.addOption("Memanjat pagar samping", 11, new RiskyOptionStrategy());
        s1.addOption("Masuk lewat pintu belakang diam-diam", 12, new BestOptionStrategy());
        s1.addOption("Menunggu orientasi selesai di luar", 13, new BadOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene 2
        Scene s11 = new Scene(11, "Terlambat Orientasi", getBg("gerbang_samping"), 2);
        s11.addDialog("Sammy: \"*tap ... tap ... bukkk\"", getChar("sammy_lari"));
        s11.addDialog("Satpam: \"HEY NGAPAIN KAMU! LAPOR BAAK DULU SANA!\"", getChar("satpam"));
        s11.addDialog("Sammy: \"Waduh ....\"", getChar("sammy_melas"));

        Scene s12 = new Scene(12, "Terlambat Orientasi", getBg("audit"), 2);
        s12.addDialog("Sammy: \"Fyuh... aman, ga ketahuan senior.\"", getChar("sammy_lega"));

        Scene s13 = new Scene(13, "Terlambat Orientasi", getBg("kelas"), 2);
        s13.addDialog("Dhito: \"....\"", getChar("dhito"));
        s13.addDialog("Sammy: \"Hai, salam kenal aku Sammy. Kita sekelas ya. Nanti aku lihat catatan kamu ya.\"", getChar("sammy_minta"));
        s13.addDialog("Dhito: \"Dih!\"", getChar("dhito_jengkel"));

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
        s2.addDialog("Pak Ibnu: \"Oll, salam kenal saya Pak Ibnu. Hari ini kita quiz mendadak PBO ya!\"", getChar("pak_ibnu_biasa"));
        s2.addDialog("Sammy: \"Aduhh baru juga masuk perkuliahan, kok udah quiz aja. Gimana nih?\"", getChar("sammy_panik"));

        s2.addOption("Jawab sesuai keyakinan & analisa sendiri", 21, new BestOptionStrategy());
        s2.addOption("Menyontek jawaban Dhito", 22, new BadOptionStrategy());
        s2.addOption("Minta izin ke toilet karena cemas", 23, new RiskyOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene 6
        Scene s21 = new Scene(21, "Quiz Mendadak PBO", getBg("kelas"), 3);
        s21.addDialog("Sammy: \"YASSS! Soalnya bisa kukerjakan!\"", getChar("sammy_yes"));

        Scene s22 = new Scene(22, "Quiz Mendadak PBO", getBg("kelas"), 3);
        s22.addDialog("Sammy: \"Dhit, jawabannya apa?\"", getChar("sammy_nyontek"));
        s22.addDialog("Dhito: \"C\"",getChar("dhito"));
        s22.addDialog("tetot");
        s22.addDialog("Sammy: \"Gimana sih, dhit?\"", getChar("sammy_jengkel"));
        s22.addDialog("Dhito: \"Yo ndak tau, salah sendiri nanya-nanya. Wong aku ya bingung.\"", getChar("dhito_jengkel"));

        Scene s23 = new Scene(23, "Quiz Mendadak PBO", getBg("kelas"), 3);
        s23.addDialog("Sammy: \"Pak, saya izin ke toilet sebentar...\"", getChar("sammy_kebelet"));
        s23.addDialog("Pak Ibnu: \"Oke guys, kita tunggu Sammy ya!\"", getChar("pak_ibnu"));
        s23.addDialog("Sammy: \"Loh ...\"", getChar("sammy_nyontek"));

        branch.add(s2);
        branch.add(s21);
        branch.add(s22);
        branch.add(s23);
        return branch;
    }

//--- SCENE 3 ---
    private static List<Scene> createScene3Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s3 = new Scene(3, "Masalah di Kos", getBg("kos"));
        s3.addDialog("Sammy: \"Udah menuju akhir bulan gini, sisa duit bulanan tipis bet. Mana RAM laptop lagi rusak, kalau ga diperbaikin secepatnya, semua tugas pasti jadi terhambat. Ngapain ya enaknya?\"", getChar("sammy_laptop"));

        // Pilihan mengarah ke Sub-Scene ID 31, 32, 33
        s3.addOption("Servis laptop pakai sisa uang makan, makan mi instan seminggu", 31, new BestOptionStrategy());
        s3.addOption("Pinjam uang dari pinjol ilegal demi beli laptop baru", 32, new BadOptionStrategy());
        s3.addOption("Biarkan laptop rusak, kerjakan tugas di rental komputer saat malam hari", 33, new RiskyOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene berikutnya (sesuaikan nomor tujuan)
        Scene s31 = new Scene(31, "Masalah di Kos", getBg("kos"), 4);
        s31.addDialog("Sammy: \"Yaudah, mending diservis dulu laptopnya. Tugas jangan sampai kehambat.\"", getChar("sammy_pasrah_mie"));
        s31.addDialog("Sammy: \"*Slurp!!!* Mie instan lagi, mie instan lagi... yang penting laptop sehat.\"", getChar("sammy_mie"));

        Scene s32 = new Scene(32, "Masalah di Kos", getBg("kos"), 4);
        s32.addDialog("Sammy: \"Okey, aku pinjol aja lah biar bisa beli laptop.\"", getChar("sammy_pinjol"));
        s32.addDialog("Sammy: \"Semoga aja nanti bisa kebayar tepat waktu...\"", getChar("sammy_pinjol"));

        Scene s33 = new Scene(33, "Masalah di Kos", getBg("warnet"), 4);
        s33.addDialog("Sammy: \"Yaudah, laptopnya dibiarin dulu deh. Ngerjain tugas di rental komputer aja tiap malam.\"", getChar("sammy_nugas"));


        branch.add(s3);
        branch.add(s31);
        branch.add(s32);
        branch.add(s33);
        return branch;
    }
    private static List<Scene> createScene4Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s4 = new Scene(4, "Lempar Tanggung Jawab", getBg("kantin"));
        s4.addDialog("Nat: \"Eh... tugas kita kemarin masih banyak yang belum kelar guys\"", getChar("thania_suntuk"));
        s4.addDialog("Dhito: \"Iya yaa... Kita keteteran banget dengan tugas yang lain\"", getChar("dhito_panik"));
        s4.addDialog("Sammy: \"...\"", getChar("sammy_pusing"));

        // Pilihan mengarah ke Sub-Scene ID 41, 42, 43
        s4.addOption("Mengambil alih semua peran pembuatan kode tanpa memberitahu anggota lain", 41, new RiskyOptionStrategy());
        s4.addOption("Mengadakan rapat darurat, membagi task tegas berdasarkan kemampuan, dan bikin checklist deadline", 42, new BestOptionStrategy());
        s4.addOption("Ikut pasif dan membiarkan tugas terbengkalai sampai ada yang memulai", 43, new BadOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene 5
        Scene s41 = new Scene(41, "Lempar Tanggung Jawab", getBg("kantin"), 5);
        s41.addDialog("Sammy: \"Udahlah, biar aku aja yang kerjain semua kodenya sendiri.\"", getChar("sammy_capek"));
        s41.addDialog("Nat: \"Eh, Sam? Kok kamu diem-diem ngerjain sendiri?\"", getChar("thania_tanya"));
        s41.addDialog("Sammy: \"Nggak apa-apa kok... cuma capek aja mikirin bagi tugasnya.\"", getChar("sammy_capek"));

        Scene s42 = new Scene(42, "Lempar Tanggung Jawab", getBg("kantin"), 5);
        s42.addDialog("Sammy: \"Guys, gimana kalau kita rapat bentar sekarang? Kita bagi tugas jelas, terus bikin checklist deadline biar nggak keteteran lagi.\"", getChar("sammy_opini"));
        s42.addDialog("Dhito: \"Nah, ide bagus tuh! Aku pegang bagian logic-nya deh.\"", getChar("dhito_setuju"));
        s42.addDialog("Nat: \"Aku bagian UI-nya ya, biar jelas juga progress-nya.\"", getChar("thania_tunjuk"));
        s42.addDialog("Sammy: \"Sip, langsung gaskeun!\"", getChar("sammy_yes"));

        Scene s43 = new Scene(43, "Lempar Tanggung Jawab", getBg("kantin"), 5);
        s43.addDialog("Sammy: \"Ah, ntar juga ada yang mulai duluan kali.\"", getChar("sammy_cuek"));
        s43.addDialog("Dhito: \"...(diem, nungguin ada yang mulai)\"", getChar("dhito_cemas"));
        s43.addDialog("Nat: \"Kalau gini terus, tugasnya bisa numpuk terus lho.\"", getChar("thania_marah"));

        branch.add(s4);
        branch.add(s41);
        branch.add(s42);
        branch.add(s43);
        return branch;
    }

    // --- SCENE 5 (AJAKAN JADI KETUA PANITIA) ---
    private static List<Scene> createScene5Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s5 = new Scene(5, "Ajakan Jadi Ketua Panitia", getBg("lobby"));
        s5.addDialog("Ka Nela: \"Sammy! Sini dulu, kaka mau ngomong\"", getChar("nela_manggil"));
        s5.addDialog("Sammy: \"Halo kak Nela, ada apa kak memanggil saya\"", getChar("sammy_dipanggil"));
        s5.addDialog("Ka Nela: \"Jadi begini sam. Tahun ini kan kita bakal ada kegiatan Liliefors, semacam talkshow dan kompetisi kepenulisan kaya gitu lah sam. Kira kira kamu tertarik ga ya untuk jadi Ketua Panitianya?\"", getChar("nela_manggil"));
        s5.addDialog("Sammy: \"Hmm....\"", getChar("sammy_bingung"));

        // Pilihan mengarah ke Sub-Scene ID 51, 52, 53
        s5.addOption("Menerima tugas panitia dan bergadang setiap hari tanpa tidur untuk mengerjakan keduanya", 51, new RiskyOptionStrategy());
        s5.addOption("Menolak sopan jadi ketua panitia, menawarkan diri jadi staf biasa agar fokus belajar", 52, new BestOptionStrategy());
        s5.addOption("Menerima tugas panitia dan mengabaikan proyek PBO sementara waktu", 53, new BadOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene 6
        Scene s51 = new Scene(51, "Ajakan Jadi Ketua Panitia", getBg("kelas"), 6);
        s51.addDialog("Nat: \"Sam, kamu nggak papa?\"", getChar("thania_khawatir"));
        s51.addDialog("Sammy: (ngantuk2) \"nggak papa kok, aku masih sanggup\"", getChar("sammy_ngantuk"));
        s51.addDialog("Sammy: *kejedot meja* \"adoh\"", getChar("sammy_kejedot"));

        Scene s52 = new Scene(52, "Ajakan Jadi Ketua Panitia", getBg("lobby"), 6);
        s52.addDialog("Sammy: \"Terima kasih banyak atas kepercayaannya Kak Nela, tapi mohon maaf banget minggu ini saya ada deadline proyek besar PBO. Kalau boleh, saya minta izin jadi staf biasa aja Kak biar tetap bisa bantu-bantu?\"", getChar("sammy_bijak"));
        s52.addDialog("Ka Nela: \"Oh gitu, gapapa banget Sam! Makasih ya udah jujur, kelarin dulu proyekmu baru nanti join tim staf ya.\"", getChar("nela_senyum"));

        Scene s53 = new Scene(53, "Ajakan Jadi Ketua Panitia", getBg("lobby"), 6);
        s53.addDialog("Sammy: \"Siap Kak Nela, aku terima tugasnya! Proyek PBO gampang lah, bisa aku urus belakangan yang penting Liliefors sukses dulu.\"", getChar("sammy_hormat"));

        branch.add(s5);
        branch.add(s51);
        branch.add(s52);
        branch.add(s53);
        return branch;
    }

    // --- SCENE 6 (MINI GAME) ---
    private static List<Scene> createScene6Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s6 = new Scene(6, "Troubleshooting Program", getBg("lab_komputer"), 60);
        s6.addDialog("Nat: \"Guys, aku lagi jalanin program kita dan muncul error. Minta tolong diperbaikin ges.\"", getChar("thania_tanya"));
        s6.addDialog("Sammy: \"Iya, kayanya ada urutan kodingan yang salah, deh.\"", getChar("sammy_pusing"));
        s6.addDialog("Dhito: \"Kita suruh penonton kita untuk perbaikin aja kali ya?\"", getChar("dhito"));

        Scene s6_game = new Scene(60, "Puzzle Inheritance Java", getBg("lab_komputer"));
        s6_game.setMiniGame(true);

        Scene s6_win = new Scene(61, "Troubleshooting Berhasil", getBg("lab_komputer"), 7);
        s6_win.addDialog("Nat: \"Wah, mantap! Programnya langsung jalan tanpa error!\"", getChar("thania"));
        s6_win.addDialog("Sammy: \"Yesss! Makasih ya udah bantuin urutin kodingannya!\"", getChar("sammy_yes"));
        s6_win.addDialog("Dhito: \"Gas lanjut garap bagian lain!\"", getChar("dhito_setuju"));

        Scene s6_lose = new Scene(62, "Troubleshooting Gagal", getBg("lab_komputer"), 7);
        s6_lose.addDialog("Nat: \"Aduh, masih error nih. Kayaknya urutannya masih ada yang kebalik.\"", getChar("thania_tanya"));
        s6_lose.addDialog("Sammy: \"Huft... ya sudah deh, biar aku coba periksa manual lagi.\"", getChar("sammy_pusing"));

        branch.add(s6);
        branch.add(s6_game);
        branch.add(s6_win);
        branch.add(s6_lose);
        return branch;
    }



    // --- SCENE 7 (CRASH SAAT PRESENTASI: INHERITANCE ERROR) ---
    private static List<Scene> createScene7Branch() {
        List<Scene> branch = new ArrayList<>();

        Scene s7 = new Scene(7, "Crash Saat Presentasi", getBg("ruang_presentasi"));
        s7.addDialog("Sammy: \"Guys, kok tiba-tiba crash ya...\"", getChar("sammy_panik"));
        s7.addDialog("Pak Ibnu: \"Sammy, program kamu kok berhenti pas manggil inheritance Hewan. Ada apa?\"", getChar("pak_ibnu"));

        // Pilihan mengarah ke Sub-Scene ID 71, 72, 73
        s7.addOption("Tetap tenang & jujur menjelaskan exception secara teknis", 71, new BestOptionStrategy());
        s7.addOption("Menyalahkan koneksi internet lab & restart laptop", 72, new BadOptionStrategy());
        s7.addOption("Berargumen dengan dosen, menyalahkan data uji coba", 73, new RiskyOptionStrategy());

        // Sub-Scene Hasil Pilihan -> Lanjut ke Scene 8
        Scene s71 = new Scene(71, "Crash Saat Presentasi", getBg("ruang_presentasi"), 8);
        s71.addDialog("Sammy: \"Mohon maaf Pak, ada kesalahan logika override pada parent class Hewan, tapi saya sudah siapkan blok try-catch ini untuk menangani exception-nya secara langsung.\"", getChar("sammy_tenang"));
        s71.addDialog("Pak Ibnu: \"Bagus, itu baru namanya paham konsep. Coba jalankan lagi.\"", getChar("pak_ibnu_biasa"));

        Scene s72 = new Scene(72, "Crash Saat Presentasi", getBg("ruang_presentasi"), 8);
        s72.addDialog("Sammy: \"Wah, ini pasti gara-gara koneksi Wi-Fi lab lemot banget Pak! Saya restart laptop dulu ya!\"", getChar("sammy_ngeles"));
        s72.addDialog("*langsung tekan tombol power*", getChar("sammy_ngeles"));
        s72.addDialog("Pak Ibnu: \"Loh, program lokal kok disalahin ke Wi-Fi... Coba dicek lagi kodenya, Sam.\"", getChar("pak_ibnu"));

        Scene s73 = new Scene(73, "Crash Saat Presentasi", getBg("ruang_presentasi"), 8);
        s73.addDialog("Sammy: \"Bukan salah kodenya Pak, tapi data uji coba dari Bapak yang input-nya aneh dan gak sesuai!\"", getChar("sammy_ngeyel"));
        s73.addDialog("Pak Ibnu: \"Sammy, coba dicek dulu baik-baik, jangan buru-buru nyalahin data ujinya.\"", getChar("pak_ibnu"));

        branch.add(s7);
        branch.add(s71);
        branch.add(s72);
        branch.add(s73);
        return branch;
    }

    private static List<Scene> createScene8Branch() {
        List<Scene> branch = new ArrayList<>();

        // Ending 1: IPK tinggi (poin akumulasi bagus)
        Scene s8_good = new Scene(8, "Ending Bahagia", getBg("audit_end"));
        s8_good.setEnding(true);
        s8_good.addDialog("Sammy: \"Puji Tuhan, akhirnya setelah kerja kerasku selama ini, aku dapat IPK 4.00. Ga sia-sia yaa selama ini aku belajar dengan sungguh-sungguh, memahami setiap permasalahan, dan bisa menjadi pemimpin yang baik untuk kelompokku.\"", getChar("sammy_yes"));

        // Ending 2: Drop out (poin akumulasi buruk)
        Scene s8_bad = new Scene(9, "Ending Drop Out", getBg("kos"));
        s8_bad.setEnding(true);
        s8_bad.addDialog("Sammy: \"DEMIII APAA?!!! NILAI AKU SEGINI?! OH TIDAK, AKU TELAH DI DROP OUT. APA YANG TELAH KUPERBUAT SELAMA INI??\"", getChar("sammy_panik"));
        // Tombol restart ("Mau Buat Sammy Bahagia?") ditampilkan di layer UI setelah scene ini,
        // memanggil method restart di GameEngine - lihat catatan di atas.

        branch.add(s8_good);
        branch.add(s8_bad);
        return branch;
    }


}