package app.liquidglass.os;

import android.app.WallpaperManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.RadialGradient;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.net.Uri;

import java.io.InputStream;

/**
 * Builds the wallpaper bitmap (a sharp copy and a blurred copy).
 * Every glass panel draws a slice of the blurred copy behind itself, which gives the frosted look.
 */
public final class Wallpaper {
    private Wallpaper() {}

    public static final String[] NAMES = {"Aurora", "Sunset", "Ocean", "Lavender", "Midnight", "Mint"};

    // base top, base bottom, blob 1, blob 2, blob 3
    private static final int[][] COLORS = {
            {0xFF0B1F4B, 0xFF3A1C71, 0xFF2BD9C5, 0xFF7B5CFF, 0xFF4AA3FF},
            {0xFF2B1055, 0xFFFF7A59, 0xFFFFC371, 0xFFFF5E8A, 0xFF7B4DFF},
            {0xFF021B3A, 0xFF0A6C9C, 0xFF3FD0FF, 0xFF1E5BFF, 0xFF7DF3E1},
            {0xFF6C5CE7, 0xFFE6B8F5, 0xFFFFB6E1, 0xFF9AB8FF, 0xFFD6A8FF},
            {0xFF050816, 0xFF1A1F4D, 0xFF3D5AFE, 0xFF8E24AA, 0xFF00B8D4},
            {0xFF0E3B3A, 0xFFA8F0D1, 0xFF3DDC97, 0xFF2EC4B6, 0xFFE6FFF3},
    };

    /** Colours for drawing a small preview swatch of preset id: {top, bottom, blob1, blob2, blob3}. */
    public static int[] preview(int id) { return COLORS[Math.abs(id) % COLORS.length]; }

    private static Bitmap sharp;
    private static Bitmap blurred;
    private static String key = "";

    public static synchronized Bitmap sharp(Context c) { ensure(c); return sharp; }
    public static synchronized Bitmap blurred(Context c) { ensure(c); return blurred; }

    public static synchronized void invalidate() { key = ""; }

    public static synchronized void ensure(Context c) {
        Point s = U.screen(c);
        String k = Prefs.wallpaper(c) + "|" + Prefs.wallpaperUri(c) + "|" + s.x + "x" + s.y;
        if (sharp != null && blurred != null && k.equals(key)) return;

        Bitmap src = null;
        int id = Prefs.wallpaper(c);
        if (id == Prefs.WP_GALLERY) src = fromUri(c, Prefs.wallpaperUri(c), s.x, s.y);
        else if (id == Prefs.WP_SYSTEM) src = fromSystem(c);
        if (src == null) src = preset(id >= 0 ? id : 0, s.x, s.y);

        sharp = cover(src, s.x, s.y);
        blurred = Blur.blur(sharp, 8, 5);
        key = k;
    }

    // ---- sources -----------------------------------------------------------

    private static Bitmap preset(int id, int w, int h) {
        int[] col = COLORS[Math.abs(id) % COLORS.length];
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new LinearGradient(0, 0, w * 0.4f, h, col[0], col[1], Shader.TileMode.CLAMP));
        cv.drawRect(0, 0, w, h, p);

        blob(cv, w * 0.15f, h * 0.22f, w * 0.85f, col[2]);
        blob(cv, w * 0.90f, h * 0.50f, w * 0.80f, col[3]);
        blob(cv, w * 0.30f, h * 0.88f, w * 0.90f, col[4]);
        blob(cv, w * 0.75f, h * 0.10f, w * 0.55f, col[3]);
        return b;
    }

    private static void blob(Canvas cv, float x, float y, float r, int color) {
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        int solid = (color & 0x00FFFFFF) | 0xCC000000;
        int clear = color & 0x00FFFFFF;
        p.setShader(new RadialGradient(x, y, r, solid, clear, Shader.TileMode.CLAMP));
        cv.drawCircle(x, y, r, p);
    }

    private static Bitmap fromUri(Context c, String uri, int w, int h) {
        if (uri == null) return null;
        try {
            Uri u = Uri.parse(uri);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            InputStream in = c.getContentResolver().openInputStream(u);
            if (in == null) return null;
            BitmapFactory.decodeStream(in, null, o);
            in.close();
            int sample = 1;
            while (o.outWidth / (sample * 2) >= w && o.outHeight / (sample * 2) >= h) sample *= 2;
            BitmapFactory.Options o2 = new BitmapFactory.Options();
            o2.inSampleSize = sample;
            in = c.getContentResolver().openInputStream(u);
            if (in == null) return null;
            Bitmap b = BitmapFactory.decodeStream(in, null, o2);
            in.close();
            return b;
        } catch (Exception e) {
            return null;
        }
    }

    private static Bitmap fromSystem(Context c) {
        try {
            Drawable d = WallpaperManager.getInstance(c).getDrawable();
            if (d == null) return null;
            int dw = Math.max(1, d.getIntrinsicWidth());
            int dh = Math.max(1, d.getIntrinsicHeight());
            Bitmap b = Bitmap.createBitmap(dw, dh, Bitmap.Config.ARGB_8888);
            Canvas cv = new Canvas(b);
            d.setBounds(0, 0, dw, dh);
            d.draw(cv);
            return b;
        } catch (Throwable t) {
            return null;
        }
    }

    /** Scale and crop src so it exactly fills w x h (like centerCrop). */
    private static Bitmap cover(Bitmap src, int w, int h) {
        float scale = Math.max(w / (float) src.getWidth(), h / (float) src.getHeight());
        int sw = Math.round(src.getWidth() * scale);
        int sh = Math.round(src.getHeight() * scale);
        Bitmap scaled = Bitmap.createScaledBitmap(src, Math.max(sw, w), Math.max(sh, h), true);
        int x = Math.max(0, (scaled.getWidth() - w) / 2);
        int y = Math.max(0, (scaled.getHeight() - h) / 2);
        return Bitmap.createBitmap(scaled, x, y, w, h);
    }
}
