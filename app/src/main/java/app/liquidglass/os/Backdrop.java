package app.liquidglass.os;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Build;
import android.view.View;

/**
 * The blurred picture that overlay panels (control center, notification center) draw behind themselves.
 *
 * Android 11+: a blurred screenshot of whatever app is underneath, taken by the accessibility service.
 * Older versions: the blurred wallpaper. Android 12+ without a screenshot: nothing, because the window
 * itself is blurred by the system (FLAG_BLUR_BEHIND).
 */
public final class Backdrop {
    private Backdrop() {}

    private static Bitmap shot;

    public static void set(Bitmap b) { shot = b; }

    public static boolean hasShot() { return shot != null; }

    public static Bitmap resolve(Context c) {
        if (shot != null) return shot;
        if (Build.VERSION.SDK_INT < 31) return Wallpaper.blurred(c);
        return null;
    }

    /** Full-screen layer that paints the backdrop plus a light dark tint. */
    public static class Layer extends View {
        private final int[] loc = new int[2];

        public Layer(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas canvas) {
            Bitmap b = resolve(getContext());
            if (b != null) {
                getLocationOnScreen(loc);
                canvas.drawBitmap(b, -loc[0], -loc[1], null);
            }
            canvas.drawColor(0x40000000);
        }
    }
}
