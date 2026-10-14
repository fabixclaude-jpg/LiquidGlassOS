package app.liquidglass.os;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.ArrayList;
import java.util.List;

/** Tiny wrapper around SharedPreferences so every screen reads the same settings. */
public final class Prefs {
    private Prefs() {}

    public static final int WP_SYSTEM = -2;
    public static final int WP_GALLERY = -1;

    private static SharedPreferences sp(Context c) {
        return c.getApplicationContext().getSharedPreferences("liquidglass", Context.MODE_PRIVATE);
    }

    // ---- wallpaper -------------------------------------------------------
    public static int wallpaper(Context c) { return sp(c).getInt("wallpaper", 0); }
    public static void setWallpaper(Context c, int id) { sp(c).edit().putInt("wallpaper", id).apply(); }
    public static String wallpaperUri(Context c) { return sp(c).getString("wallpaperUri", null); }
    public static void setWallpaperUri(Context c, String u) { sp(c).edit().putString("wallpaperUri", u).apply(); }

    // ---- glass look ------------------------------------------------------
    /** 0 = light glass, 1 = dark glass */
    public static int theme(Context c) { return sp(c).getInt("theme", 0); }
    public static void setTheme(Context c, int t) { sp(c).edit().putInt("theme", t).apply(); }

    /** 0..100, how strong the frosted tint is */
    public static int tint(Context c) { return sp(c).getInt("tint", 55); }
    public static void setTint(Context c, int v) { sp(c).edit().putInt("tint", v).apply(); }

    /** true = solid panels, false = liquid glass */
    public static boolean solid(Context c) { return sp(c).getBoolean("solid", false); }
    public static void setSolid(Context c, boolean v) { sp(c).edit().putBoolean("solid", v).apply(); }

    // ---- lock screen clock ----------------------------------------------
    public static int clockColor(Context c) { return sp(c).getInt("clockColor", 0xFFFFFFFF); }
    public static void setClockColor(Context c, int v) { sp(c).edit().putInt("clockColor", v).apply(); }

    /** 0..100, stretches the digits taller */
    public static int clockHeight(Context c) { return sp(c).getInt("clockHeight", 70); }
    public static void setClockHeight(Context c, int v) { sp(c).edit().putInt("clockHeight", v).apply(); }

    /** 0..100, how far down the screen the clock sits (0 = top, 100 = lower third) */
    public static int clockY(Context c) { return sp(c).getInt("clockY", 55); }
    public static void setClockY(Context c, int v) { sp(c).edit().putInt("clockY", v).apply(); }

    /** true = see-through glass digits, false = solid digits */
    public static boolean clockGlass(Context c) { return sp(c).getBoolean("clockGlass", true); }
    public static void setClockGlass(Context c, boolean v) { sp(c).edit().putBoolean("clockGlass", v).apply(); }

    /** 0 = tall condensed, 1 = medium, 2 = light */
    public static int clockFont(Context c) { return sp(c).getInt("clockFont", 0); }
    public static void setClockFont(Context c, int v) { sp(c).edit().putInt("clockFont", v).apply(); }

    public static boolean lockEnabled(Context c) { return sp(c).getBoolean("lockEnabled", false); }
    public static void setLockEnabled(Context c, boolean v) { sp(c).edit().putBoolean("lockEnabled", v).apply(); }

    // ---- pinned dock apps --------------------------------------------------
    public static List<String> pinned(Context c) {
        List<String> out = new ArrayList<>();
        String s = sp(c).getString("pinned", "");
        if (s == null || s.length() == 0) return out;
        for (String p : s.split(",")) if (p.length() > 0) out.add(p);
        return out;
    }

    public static void setPinned(Context c, List<String> pkgs) {
        StringBuilder sb = new StringBuilder();
        for (String p : pkgs) {
            if (sb.length() > 0) sb.append(',');
            sb.append(p);
        }
        sp(c).edit().putString("pinned", sb.toString()).apply();
    }

    public static boolean pinnedChosen(Context c) { return sp(c).contains("pinned"); }
}
