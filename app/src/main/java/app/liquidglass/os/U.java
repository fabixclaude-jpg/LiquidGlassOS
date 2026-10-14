package app.liquidglass.os;

import android.content.Context;
import android.graphics.Point;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;

/** Small helpers shared by all screens. */
public final class U {
    private U() {}

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    /** Full physical screen size including system bars, so wallpaper lines up in every window. */
    public static Point screen(Context c) {
        WindowManager wm = (WindowManager) c.getSystemService(Context.WINDOW_SERVICE);
        Display d = wm.getDefaultDisplay();
        DisplayMetrics m = new DisplayMetrics();
        d.getRealMetrics(m);
        return new Point(m.widthPixels, m.heightPixels);
    }

    /** Redraw every glass panel under root (needed after scrolling, because blur depends on position). */
    public static void invalidateGlass(View root) {
        if (root instanceof GlassView) root.invalidate();
        if (root instanceof ViewGroup) {
            ViewGroup g = (ViewGroup) root;
            for (int i = 0; i < g.getChildCount(); i++) invalidateGlass(g.getChildAt(i));
        }
    }
}
