package app.liquidglass.os;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * Glass Settings: set-up shortcuts (default launcher, permissions), wallpapers, themes and glass look.
 * Everything on this screen is a blurred glass panel on top of the wallpaper.
 */
public class SettingsActivity extends Activity {

    private static final int REQ_PHOTO = 7;

    private FrameLayout root;
    private LinearLayout body;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        root = new FrameLayout(this);
        setContentView(root);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        build(); // permissions may have changed while we were in system settings
    }

    // ---------------------------------------------------------------------------------

    private void build() {
        root.removeAllViews();
        root.addView(new LauncherActivity.WallpaperView(this), new FrameLayout.LayoutParams(-1, -1));

        GlassScroll sv = new GlassScroll(this);
        root.addView(sv, new FrameLayout.LayoutParams(-1, -1));
        body = new LinearLayout(this);
        body.setOrientation(LinearLayout.VERTICAL);
        int side = U.dp(this, 16);
        body.setPadding(side, statusBar() + U.dp(this, 16), side, U.dp(this, 40));
        sv.addView(body, new FrameLayout.LayoutParams(-1, -2));

        TextView title = new TextView(this);
        title.setText("Liquid Glass");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(34);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        body.addView(title);

        TextView sub = new TextView(this);
        sub.setText("Set up your iOS-style home screen, lock screen and control center.");
        sub.setTextColor(0xCCFFFFFF);
        sub.setTextSize(15);
        sub.setPadding(0, U.dp(this, 4), 0, U.dp(this, 8));
        body.addView(sub);

        section("SET UP");
        card(
                row("Make this your home screen", "Choose Liquid Glass as the default home app",
                        isDefaultHome(), v -> openHomeSettings()),
                row("Display over other apps", "Needed for the lock screen",
                        canOverlay(), v -> openOverlaySettings()),
                row("Control center and swipe-down panels", "Turn on Liquid Glass in Accessibility",
                        accessibilityOn(), v -> startSettings(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))),
                row("Notification access", "Shows notifications as glass cards",
                        NotificationCenter.accessEnabled(this),
                        v -> startSettings(new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))),
                row("Change system settings", "Lets the sliders set brightness and rotation",
                        canWrite(), v -> openWriteSettings())
        );

        section("LOCK SCREEN");
        card(
                toggleRow("Liquid glass lock screen", "Replaces the stock lock screen when the screen turns off",
                        Prefs.lockEnabled(this), on -> setLock(on)),
                row("Edit lock screen", "Font, colour, glass clock and position", true,
                        v -> startActivity(new Intent(this, LockEditorActivity.class)))
        );

        section("WALLPAPER");
        wallpapers();

        section("LOOK");
        card(
                segmented("Theme", "Light glass", "Dark glass", Prefs.theme(this), i -> {
                    Prefs.setTheme(this, i);
                    build();
                }),
                segmented("Material", "Glass", "Solid", Prefs.solid(this) ? 1 : 0, i -> {
                    Prefs.setSolid(this, i == 1);
                    build();
                }),
                sliderRow("Frost", Prefs.tint(this) / 100f, v -> {
                    Prefs.setTint(this, Math.round(v * 100));
                    U.invalidateGlass(root);
                })
        );

        section("DOCK");
        card(row("Reset dock apps", "Go back to Phone, Messages, Browser and Camera", true, v -> {
            Prefs.setPinned(this, Apps.defaultDock(this));
            Toast.makeText(this, "Dock reset", Toast.LENGTH_SHORT).show();
        }));
    }

    // ---------------------------------------------------------------------------------
    // pieces
    // ---------------------------------------------------------------------------------

    private void section(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xB3FFFFFF);
        t.setTextSize(12);
        t.setLetterSpacing(0.08f);
        t.setPadding(U.dp(this, 8), U.dp(this, 22), 0, U.dp(this, 8));
        body.addView(t);
    }

    private void card(View... rows) {
        GlassView g = new GlassView(this, 24);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        for (int i = 0; i < rows.length; i++) {
            if (i > 0) {
                View line = new View(this);
                line.setBackgroundColor(0x33FFFFFF);
                LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, 1);
                lp.leftMargin = U.dp(this, 16);
                col.addView(line, lp);
            }
            col.addView(rows[i]);
        }
        g.addView(col, new FrameLayout.LayoutParams(-1, -2));
        body.addView(g, new LinearLayout.LayoutParams(-1, -2));
    }

    private LinearLayout textBlock(String title, String sub) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(16);
        col.addView(t);
        if (sub != null) {
            TextView s = new TextView(this);
            s.setText(sub);
            s.setTextColor(0xB3FFFFFF);
            s.setTextSize(12.5f);
            col.addView(s);
        }
        return col;
    }

    private View row(String title, String sub, boolean ok, View.OnClickListener l) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        int p = U.dp(this, 16);
        r.setPadding(p, U.dp(this, 12), p, U.dp(this, 12));
        r.addView(textBlock(title, sub), new LinearLayout.LayoutParams(0, -2, 1f));
        TextView s = new TextView(this);
        s.setText(ok ? "On" : "Set up");
        s.setTextColor(ok ? 0xFF30D158 : 0xFF64B5FF);
        s.setTextSize(14);
        s.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        r.addView(s);
        r.setOnClickListener(l);
        return r;
    }

    private interface BoolCb { void on(boolean v); }
    private interface IntCb { void on(int v); }
    private interface FloatCb { void on(float v); }

    private View toggleRow(String title, String sub, boolean on, final BoolCb cb) {
        LinearLayout r = new LinearLayout(this);
        r.setGravity(Gravity.CENTER_VERTICAL);
        int p = U.dp(this, 16);
        r.setPadding(p, U.dp(this, 12), p, U.dp(this, 12));
        r.addView(textBlock(title, sub), new LinearLayout.LayoutParams(0, -2, 1f));
        final Switch2 sw = new Switch2(this, on);
        r.addView(sw, new LinearLayout.LayoutParams(U.dp(this, 52), U.dp(this, 32)));
        r.setOnClickListener(v -> {
            sw.toggle();
            cb.on(sw.isOn());
        });
        return r;
    }

    /** iOS style on/off switch. */
    static class Switch2 extends View {
        private boolean on;
        private final android.graphics.Paint p = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);

        Switch2(Context c, boolean on) { super(c); this.on = on; }

        boolean isOn() { return on; }
        void toggle() { on = !on; invalidate(); }

        @Override
        protected void onDraw(android.graphics.Canvas cv) {
            float h = getHeight(), w = getWidth();
            p.setColor(on ? 0xFF30D158 : 0x66FFFFFF);
            cv.drawRoundRect(0, 0, w, h, h / 2f, h / 2f, p);
            p.setColor(0xFFFFFFFF);
            float r = h / 2f - U.dp(getContext(), 2);
            cv.drawCircle(on ? w - h / 2f : h / 2f, h / 2f, r, p);
        }
    }

    private View segmented(String title, String a, String b, int selected, final IntCb cb) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = U.dp(this, 16);
        col.setPadding(p, U.dp(this, 12), p, U.dp(this, 12));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(16);
        col.addView(t);

        LinearLayout seg = new LinearLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x33000000);
        bg.setCornerRadius(U.dp(this, 12));
        seg.setBackground(bg);
        seg.setPadding(U.dp(this, 3), U.dp(this, 3), U.dp(this, 3), U.dp(this, 3));
        String[] names = {a, b};
        for (int i = 0; i < 2; i++) {
            final int idx = i;
            TextView o = new TextView(this);
            o.setText(names[i]);
            o.setGravity(Gravity.CENTER);
            o.setTextSize(14);
            boolean sel = i == selected;
            o.setTextColor(sel ? 0xFF1C1C1E : 0xFFFFFFFF);
            if (sel) {
                GradientDrawable g = new GradientDrawable();
                g.setColor(0xF2FFFFFF);
                g.setCornerRadius(U.dp(this, 10));
                o.setBackground(g);
            }
            o.setOnClickListener(v -> cb.on(idx));
            seg.addView(o, new LinearLayout.LayoutParams(0, U.dp(this, 34), 1f));
        }
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = U.dp(this, 8);
        col.addView(seg, lp);
        return col;
    }

    private View sliderRow(String title, float value, final FloatCb cb) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = U.dp(this, 16);
        col.setPadding(p, U.dp(this, 12), p, U.dp(this, 14));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(16);
        col.addView(t);
        GlassSlider s = new GlassSlider(this, false, "sun");
        s.setValue(value);
        s.setListener((v, user) -> cb.on(v));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, U.dp(this, 44));
        lp.topMargin = U.dp(this, 8);
        col.addView(s, lp);
        return col;
    }

    private void wallpapers() {
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout strip = new LinearLayout(this);
        hs.addView(strip);
        int cur = Prefs.wallpaper(this);

        for (int i = 0; i < Wallpaper.NAMES.length; i++) {
            strip.addView(swatch(Wallpaper.NAMES[i], preview(i), cur == i, i));
        }
        strip.addView(swatch("Photo", null, cur == Prefs.WP_GALLERY, Prefs.WP_GALLERY));
        strip.addView(swatch("System", null, cur == Prefs.WP_SYSTEM, Prefs.WP_SYSTEM));
        body.addView(hs, new LinearLayout.LayoutParams(-1, -2));
    }

    private GradientDrawable preview(int id) {
        int[] c = Wallpaper.preview(id);
        GradientDrawable g = new GradientDrawable(GradientDrawable.Orientation.TL_BR,
                new int[]{c[2], c[3], c[4]});
        g.setCornerRadius(U.dp(this, 18));
        return g;
    }

    private View swatch(String name, GradientDrawable art, boolean selected, final int id) {
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setGravity(Gravity.CENTER_HORIZONTAL);
        int w = U.dp(this, 74);
        int h = U.dp(this, 120);

        FrameLayout tile = new FrameLayout(this);
        if (art != null) {
            tile.setBackground(art);
        } else {
            GlassView g = new GlassView(this, 18);
            TextView plus = new TextView(this);
            plus.setText(id == Prefs.WP_GALLERY ? "+" : "OS");
            plus.setTextColor(0xFFFFFFFF);
            plus.setTextSize(24);
            plus.setGravity(Gravity.CENTER);
            g.addView(plus, new FrameLayout.LayoutParams(-1, -1));
            tile.addView(g, new FrameLayout.LayoutParams(-1, -1));
        }
        if (selected) {
            View ring = new View(this);
            GradientDrawable rg = new GradientDrawable();
            rg.setStroke(U.dp(this, 3), 0xFFFFFFFF);
            rg.setCornerRadius(U.dp(this, 18));
            ring.setBackground(rg);
            tile.addView(ring, new FrameLayout.LayoutParams(-1, -1));
        }
        col.addView(tile, new LinearLayout.LayoutParams(w, h));

        TextView label = new TextView(this);
        label.setText(name);
        label.setTextColor(0xFFFFFFFF);
        label.setTextSize(12);
        label.setGravity(Gravity.CENTER);
        label.setPadding(0, U.dp(this, 5), 0, 0);
        col.addView(label);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-2, -2);
        lp.rightMargin = U.dp(this, 12);
        col.setLayoutParams(lp);
        col.setOnClickListener(v -> pick(id));
        return col;
    }

    private void pick(int id) {
        if (id == Prefs.WP_GALLERY) {
            Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("image/*");
            startActivityForResult(i, REQ_PHOTO);
            return;
        }
        Prefs.setWallpaper(this, id);
        Wallpaper.invalidate();
        build();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (req == REQ_PHOTO && res == RESULT_OK && data != null && data.getData() != null) {
            Uri u = data.getData();
            try {
                getContentResolver().takePersistableUriPermission(u, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            } catch (Exception ignored) { }
            Prefs.setWallpaperUri(this, u.toString());
            Prefs.setWallpaper(this, Prefs.WP_GALLERY);
            Wallpaper.invalidate();
            build();
        }
    }

    // ---------------------------------------------------------------------------------
    // permissions and system shortcuts
    // ---------------------------------------------------------------------------------

    private int statusBar() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : U.dp(this, 24);
    }

    private void startSettings(Intent i) {
        try { startActivity(i); }
        catch (Exception e) { Toast.makeText(this, "Open your phone's Settings and search for this", Toast.LENGTH_LONG).show(); }
    }

    private boolean isDefaultHome() {
        Intent home = new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME);
        android.content.pm.ResolveInfo r = getPackageManager()
                .resolveActivity(home, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY);
        return r != null && r.activityInfo != null && getPackageName().equals(r.activityInfo.packageName);
    }

    private void openHomeSettings() {
        if (Build.VERSION.SDK_INT >= 21) startSettings(new Intent(Settings.ACTION_HOME_SETTINGS));
        else startSettings(new Intent(Settings.ACTION_SETTINGS));
    }

    private boolean canOverlay() {
        return Build.VERSION.SDK_INT < 23 || Settings.canDrawOverlays(this);
    }

    private void openOverlaySettings() {
        startSettings(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName())));
    }

    private boolean canWrite() {
        return Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(this);
    }

    private void openWriteSettings() {
        startSettings(new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + getPackageName())));
    }

    private boolean accessibilityOn() {
        String s = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return s != null && s.contains(getPackageName());
    }

    private void setLock(boolean on) {
        if (on && !canOverlay()) {
            Toast.makeText(this, "Allow \"Display over other apps\" first", Toast.LENGTH_LONG).show();
            openOverlaySettings();
            Prefs.setLockEnabled(this, false);
            build();
            return;
        }
        Prefs.setLockEnabled(this, on);
        if (on) LockService.start(this); else LockService.stop(this);
    }
}
