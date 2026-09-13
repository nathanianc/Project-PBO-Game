import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

// Helper buat load font pixel (Press Start 2P) sekali aja terus di-reuse.
// Kalau file font-nya nggak ketemu/gagal di-load, otomatis fallback ke Monospaced
// biar game tetep jalan (nggak crash) walau font custom-nya belum ditaruh.
public class PixelFont {
    private static final String FONT_PATH = "assets/fonts/PressStart2P-Regular.ttf";
    private static Font baseFont;
    private static boolean loadAttempted = false;
    private static final Map<Float, Font> cache = new HashMap<>();

    private static void loadBaseFont() {
        if (loadAttempted) return;
        loadAttempted = true;
        try {
            baseFont = Font.createFont(Font.TRUETYPE_FONT, new File(FONT_PATH)).deriveFont(12f);
            GraphicsEnvironment.getLocalGraphicsEnvironment().registerFont(baseFont);
        } catch (FontFormatException | IOException e) {
            System.err.println("PixelFont: gagal load " + FONT_PATH + ", pakai Monospaced sebagai gantinya.");
            baseFont = null;
        }
    }

    // Pakai ini di seluruh game buat dapetin font pixel di ukuran tertentu.
    // Contoh: PixelFont.get(14f)
    public static Font get(float size) {
        loadBaseFont();
        return cache.computeIfAbsent(size, s ->
                baseFont != null ? baseFont.deriveFont(s) : new Font("Monospaced", Font.PLAIN, Math.round(s))
        );
    }
}
