package app.liquidglass.os;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.hardware.camera2.CameraManager;
import android.media.AudioManager;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.provider.AlarmClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/** The liquid glass control center panel. Build it with ControlCenter.build(). */
public final class ControlCenter {
    private ControlCenter() {}

    private static boolean torchOn = false;
    private static boolean askedWrite = false;

    // ------------------------------------------------------------------------------------------
    // building blocks
    // ------------------------------------------------------------------------------------------

    /** Round connectivity button: coloured when on, grey glass when off. */
    static class Round extends View {
        private final String kind;
        private int onColor;
        private boolean on;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);

        Round(Context c, String kind, int onColor, boolean on) {
            super(c);
            this.kind = kind;
            this.onColor = onColor;
            this.on = on;
            rim.setStyle(Paint.Style.STROKE);
            rim.setStrokeWidth(U.dp(c, 1));
            rim.setColor(0x66FFFFFF);
        }

        void setOn(boolean v) { on = v; invalidate(); }

        @Override
        protected void onDraw(Canvas cv) {
            float r = Math.min(getWidth(), getHeight()) / 2f - 1;
            p.setColor(on ? onColor : 0x66808080);
            cv.drawCircle(getWidth() / 2f, getHeight() / 2f, r, p);
            cv.drawCircle(getWidth() / 2f, getHeight() / 2f, r, rim);
            float s = r * 1.0f;
            Glyph.draw(cv, kind, getWidth() / 2f - s / 2f, getHeight() / 2f - s / 2f, s, 0xFFFFFFFF);
        }
    }

    /** Square glass tile with a glyph and an optional label. Turns white when active. */
    static class Tile extends GlassView {
        private final View overlay;
        private final Glyph.View2 glyph;
        private final TextView label;
        private final String kind;

        Tile(Context c, String kind, String text) {
            super(c, 26);
            this.kind = kind;
            setUseWallpaper(false);
            overlay = new View(c);
            GradientDrawable g = new GradientDrawable();
            g.setColor(0xF2FFFFFF);
            g.setCornerRadius(U.dp(c, 26));
            overlay.setBackground(g);
            overlay.setVisibility(View.GONE);
            addView(overlay, new FrameLayout.LayoutParams(-1, -1));

            glyph = new Glyph.View2(c, kind);
            label = new TextView(c);
            if (text == null) {
                addView(glyph, new FrameLayout.LayoutParams(U.dp(c, 30), U.dp(c, 30), Gravity.CENTER));
            } else {
                LinearLayout row = new LinearLayout(c);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(U.dp(c, 18), 0, U.dp(c, 12), 0);
                row.addView(glyph, new LinearLayout.LayoutParams(U.dp(c, 26), U.dp(c, 26)));
                label.setText(text);
                label.setTextColor(0xFFFFFFFF);
                label.setTextSize(15);
                label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
                label.setPadding(U.dp(c, 10), 0, 0, 0);
                row.addView(label);
                addView(row, new FrameLayout.LayoutParams(-1, -1));
            }
            setActive(false);
        }

        void setActive(boolean on) {
            overlay.setVisibility(on ? View.VISIBLE : View.GONE);
            int col = on ? 0xFF2C2C2E : 0xFFFFFFFF;
            glyph.set(kind, col);
            label.setTextColor(col);
        }
    }

    private static LinearLayout.LayoutParams lp(int w, int h, float weight, int l, int t, int r, int b) {
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(w, h, weight);
        p.setMargins(l, t, r, b);
        return p;
    }

    private static void go(Context c, Intent i, Runnable close) {
        try {
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            c.startActivity(i);
        } catch (Exception e) {
            Toast.makeText(c, "Not available on this phone", Toast.LENGTH_SHORT).show();
        }
        if (close != null) close.run();
    }

    private static boolean canWrite(Context c) {
        return Build.VERSION.SDK_INT < 23 || Settings.System.canWrite(c);
    }

    private static void askWrite(Context c) {
        if (askedWrite) return;
        askedWrite = true;
        Intent i = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:" + c.getPackageName()));
        i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try { c.startActivity(i); } catch (Exception ignored) { }
    }

    // ------------------------------------------------------------------------------------------
    // the panel
    // ------------------------------------------------------------------------------------------

    public static View build(final Context c, final Runnable close) {
        askedWrite = false;
        final ContentResolver cr = c.getContentResolver();
        int m = U.dp(c, 6);

        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(U.dp(c, 12), U.dp(c, 8), U.dp(c, 12), U.dp(c, 8));

        // ---------- row A: connectivity block + media tile
        LinearLayout rowA = new LinearLayout(c);
        GlassView conn = new GlassView(c, 32);
        conn.setUseWallpaper(false);
        LinearLayout grid = new LinearLayout(c);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(U.dp(c, 14), U.dp(c, 14), U.dp(c, 14), U.dp(c, 14));
        conn.addView(grid, new FrameLayout.LayoutParams(-1, -1));

        boolean airplane = Settings.Global.getInt(cr, Settings.Global.AIRPLANE_MODE_ON, 0) == 1;
        boolean cellular = Settings.Global.getInt(cr, "mobile_data", 1) == 1;
        boolean bt = Settings.Global.getInt(cr, "bluetooth_on", 0) == 1;
        boolean wifi = false;
        try {
            wifi = ((WifiManager) c.getApplicationContext().getSystemService(Context.WIFI_SERVICE)).isWifiEnabled();
        } catch (Exception ignored) { }

        final Round rAir = new Round(c, "airplane", 0xFFFF9F0A, airplane);
        final Round rCell = new Round(c, "cell", 0xFF30D158, cellular && !airplane);
        final Round rWifi = new Round(c, "wifi", 0xFF0A84FF, wifi);
        final Round rBt = new Round(c, "bluetooth", 0xFF0A84FF, bt);

        LinearLayout g1 = new LinearLayout(c);
        g1.addView(rAir, lp(0, -1, 1f, 0, 0, m, 0));
        g1.addView(rCell, lp(0, -1, 1f, m, 0, 0, 0));
        LinearLayout g2 = new LinearLayout(c);
        g2.addView(rWifi, lp(0, -1, 1f, 0, 0, m, 0));
        g2.addView(rBt, lp(0, -1, 1f, m, 0, 0, 0));
        grid.addView(g1, lp(-1, 0, 1f, 0, 0, 0, m));
        grid.addView(g2, lp(-1, 0, 1f, 0, m, 0, 0));

        rAir.setOnClickListener(v -> go(c, new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS), close));
        rCell.setOnClickListener(v -> go(c, new Intent(Settings.ACTION_WIRELESS_SETTINGS), close));
        rBt.setOnClickListener(v -> go(c, new Intent(Settings.ACTION_BLUETOOTH_SETTINGS), close));
        rWifi.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 29) go(c, new Intent(Settings.Panel.ACTION_WIFI), close);
            else go(c, new Intent(Settings.ACTION_WIFI_SETTINGS), close);
        });

        Tile media = new Tile(c, "play", "Music");
        media.setOnClickListener(v -> go(c,
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC), close));

        rowA.addView(conn, lp(0, -1, 1f, 0, 0, m, 0));
        rowA.addView(media, lp(0, -1, 1f, m, 0, 0, 0));
        col.addView(rowA, lp(-1, U.dp(c, 150), 0f, 0, 0, 0, U.dp(c, 12)));

        // ---------- row B: small tiles + focus, brightness, volume
        LinearLayout rowB = new LinearLayout(c);
        LinearLayout left = new LinearLayout(c);
        left.setOrientation(LinearLayout.VERTICAL);

        LinearLayout small = new LinearLayout(c);
        final Tile rot = new Tile(c, "rotate", null);
        boolean locked = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1) == 0;
        rot.setActive(locked);
        rot.setOnClickListener(v -> {
            if (!canWrite(c)) { askWrite(c); close.run(); return; }
            boolean nowLocked = Settings.System.getInt(cr, Settings.System.ACCELEROMETER_ROTATION, 1) == 0;
            Settings.System.putInt(cr, Settings.System.ACCELEROMETER_ROTATION, nowLocked ? 1 : 0);
            rot.setActive(!nowLocked);
        });
        Tile mirror = new Tile(c, "mirror", null);
        mirror.setOnClickListener(v -> go(c, new Intent(Settings.ACTION_CAST_SETTINGS), close));
        small.addView(rot, lp(0, -1, 1f, 0, 0, m, 0));
        small.addView(mirror, lp(0, -1, 1f, m, 0, 0, 0));

        Tile focus = new Tile(c, "moon", "Focus");
        focus.setOnClickListener(v -> go(c, new Intent(Settings.ACTION_SOUND_SETTINGS), close));

        left.addView(small, lp(-1, 0, 1f, 0, 0, 0, m));
        left.addView(focus, lp(-1, 0, 1f, 0, m, 0, 0));

        GlassSlider brightness = new GlassSlider(c, true, "sun");
        brightness.setValue(Settings.System.getInt(cr, Settings.System.SCREEN_BRIGHTNESS, 128) / 255f);
        brightness.setListener((val, user) -> {
            if (!canWrite(c)) { askWrite(c); close.run(); return; }
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS_MODE,
                    Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
            Settings.System.putInt(cr, Settings.System.SCREEN_BRIGHTNESS, Math.max(5, Math.round(val * 255f)));
        });

        final AudioManager am = (AudioManager) c.getSystemService(Context.AUDIO_SERVICE);
        final int maxVol = Math.max(1, am.getStreamMaxVolume(AudioManager.STREAM_MUSIC));
        GlassSlider volume = new GlassSlider(c, true, "speaker");
        volume.setValue(am.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) maxVol);
        volume.setListener((val, user) ->
                am.setStreamVolume(AudioManager.STREAM_MUSIC, Math.round(val * maxVol), 0));

        rowB.addView(left, lp(0, -1, 2f, 0, 0, m, 0));
        rowB.addView(brightness, lp(0, -1, 1f, m, 0, m, 0));
        rowB.addView(volume, lp(0, -1, 1f, m, 0, 0, 0));
        col.addView(rowB, lp(-1, U.dp(c, 150), 0f, 0, 0, 0, U.dp(c, 12)));

        // ---------- row C: flashlight, timer, calculator, camera
        LinearLayout rowC = new LinearLayout(c);
        final Tile torch = new Tile(c, "torch", null);
        torch.setActive(torchOn);
        torch.setOnClickListener(v -> {
            try {
                CameraManager cm = (CameraManager) c.getSystemService(Context.CAMERA_SERVICE);
                torchOn = !torchOn;
                cm.setTorchMode(cm.getCameraIdList()[0], torchOn);
                torch.setActive(torchOn);
            } catch (Exception e) {
                torchOn = false;
                Toast.makeText(c, "Flashlight not available", Toast.LENGTH_SHORT).show();
            }
        });
        Tile timer = new Tile(c, "timer", null);
        timer.setOnClickListener(v -> go(c, new Intent(AlarmClock.ACTION_SET_TIMER), close));
        Tile calc = new Tile(c, "calc", null);
        calc.setOnClickListener(v -> go(c,
                Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_CALCULATOR), close));
        Tile cam = new Tile(c, "camera", null);
        cam.setOnClickListener(v -> go(c, new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA), close));

        rowC.addView(torch, lp(0, -1, 1f, 0, 0, m, 0));
        rowC.addView(timer, lp(0, -1, 1f, m, 0, m, 0));
        rowC.addView(calc, lp(0, -1, 1f, m, 0, m, 0));
        rowC.addView(cam, lp(0, -1, 1f, m, 0, 0, 0));
        col.addView(rowC, lp(-1, U.dp(c, 76), 0f, 0, 0, 0, U.dp(c, 12)));

        // ---------- customise
        Tile custom = new Tile(c, "sun", "Glass settings");
        custom.setOnClickListener(v -> {
            Intent i = new Intent(c, SettingsActivity.class);
            go(c, i, close);
        });
        col.addView(custom, lp(-1, U.dp(c, 56), 0f, 0, 0, 0, 0));
        return col;
    }
}
