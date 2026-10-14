package app.liquidglass.os;

import android.graphics.Bitmap;

/** Plain box blur. Works on every Android version, no RenderScript needed. */
public final class Blur {
    private Blur() {}

    /**
     * Returns a blurred copy of src, same size as src.
     * The image is shrunk first (fast), blurred three times (approximates a gaussian), then scaled back up.
     */
    public static Bitmap blur(Bitmap src, int shrink, int radius) {
        int w = Math.max(8, src.getWidth() / shrink);
        int h = Math.max(8, src.getHeight() / shrink);
        Bitmap small = Bitmap.createScaledBitmap(src, w, h, true);
        int[] px = new int[w * h];
        small.getPixels(px, 0, w, 0, 0, w, h);
        int[] tmp = new int[w * h];
        for (int i = 0; i < 3; i++) {
            pass(px, tmp, w, h, radius, true);
            pass(tmp, px, w, h, radius, false);
        }
        Bitmap out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        out.setPixels(px, 0, w, 0, 0, w, h);
        Bitmap big = Bitmap.createScaledBitmap(out, src.getWidth(), src.getHeight(), true);
        if (small != src) small.recycle();
        out.recycle();
        return big;
    }

    private static void pass(int[] in, int[] out, int w, int h, int r, boolean horizontal) {
        int len = horizontal ? w : h;
        int lines = horizontal ? h : w;
        for (int line = 0; line < lines; line++) {
            for (int i = 0; i < len; i++) {
                int a = 0, rr = 0, g = 0, b = 0, n = 0;
                int from = Math.max(0, i - r);
                int to = Math.min(len - 1, i + r);
                for (int k = from; k <= to; k++) {
                    int idx = horizontal ? (line * w + k) : (k * w + line);
                    int p = in[idx];
                    a += (p >>> 24);
                    rr += (p >> 16) & 0xFF;
                    g += (p >> 8) & 0xFF;
                    b += p & 0xFF;
                    n++;
                }
                int o = horizontal ? (line * w + i) : (i * w + line);
                out[o] = ((a / n) << 24) | ((rr / n) << 16) | ((g / n) << 8) | (b / n);
            }
        }
    }
}
