package app.liquidglass.os;

import android.app.Activity;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.TextView;

/**
 * "Font and Color" editor for the lock screen clock, laid out like the iOS sheet:
 * live clock preview on the wallpaper, and a glass sheet at the bottom with font styles, a size slider,
 * colour swatches, a position slider and a Glass / Solid switch.
 */
public class LockEditorActivity extends Activity {

    private static final int[] COLORS = {
            0xFFFFFFFF, 0xFFA7E3FF, 0xFFB8C8FF, 0xFFE0CCFF, 0xFFE6A8FF,
            0xFFFFB3C7, 0xFFFFD6A5, 0xFFFFF1A8, 0xFFB9F6CA, 0xFF9AA0A6};

    private FrameLayout root;
    private LockClockView clock;
    private FrameLayout sheetHolder;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        root = new FrameLayout(this);
        setContentView(root);

        root.addView(new LauncherActivity.WallpaperView(this), new FrameLayout.LayoutParams(-1, -1));
        clock = new LockClockView(this);
        root.addView(clock, new FrameLayout.LayoutParams(-1, -1));

        // dashed-style frame around the clock, like the editor in iOS
        sheetHolder = new FrameLayout(this);
        root.addView(sheetHolder, new FrameLayout.LayoutParams(-1, -1));
        buildSheet();
    }

    private void buildSheet() {
        sheetHolder.removeAllViews();

        GlassView sheet = new GlassView(this, 32);
        sheet.setForceDark(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int p = U.dp(this, 20);
        col.setPadding(p, U.dp(this, 16), p, U.dp(this, 18));
        sheet.addView(col, new FrameLayout.LayoutParams(-1, -2));

        // title row with Done on the right
        FrameLayout head = new FrameLayout(this);
        TextView title = new TextView(this);
        title.setText("Font & Color");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(18);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        head.addView(title, new FrameLayout.LayoutParams(-2, -2, Gravity.CENTER));
        TextView done = new TextView(this);
        done.setText("Done");
        done.setTextColor(0xFFFFFFFF);
        done.setTextSize(16);
        done.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        done.setOnClickListener(v -> finish());
        head.addView(done, new FrameLayout.LayoutParams(-2, -2, Gravity.END | Gravity.CENTER_VERTICAL));
        col.addView(head, new LinearLayout.LayoutParams(-1, U.dp(this, 36)));

        // 1. font styles
        LinearLayout fonts = new LinearLayout(this);
        for (int i = 0; i < 3; i++) fonts.addView(fontChoice(i), new LinearLayout.LayoutParams(0, U.dp(this, 56), 1f));
        LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(-1, -2);
        flp.topMargin = U.dp(this, 14);
        col.addView(fonts, flp);

        // 2. height slider
        col.addView(label("Height"));
        col.addView(slider("sun", Prefs.clockHeight(this) / 100f, v -> {
            Prefs.setClockHeight(this, Math.round(v * 100));
            clock.invalidate();
        }), sliderLp());

        // 3. colours
        col.addView(label("Colour"));
        HorizontalScrollView hs = new HorizontalScrollView(this);
        hs.setHorizontalScrollBarEnabled(false);
        LinearLayout sw = new LinearLayout(this);
        hs.addView(sw);
        for (int c : COLORS) sw.addView(swatch(c));
        col.addView(hs, new LinearLayout.LayoutParams(-1, U.dp(this, 52)));

        // 4. position slider (lets the clock sit lower on the screen)
        col.addView(label("Position"));
        col.addView(slider("sun", Prefs.clockY(this) / 100f, v -> {
            Prefs.setClockY(this, Math.round(v * 100));
            clock.invalidate();
        }), sliderLp());

        // 5. glass / solid
        LinearLayout seg = new LinearLayout(this);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(0x33FFFFFF);
        bg.setCornerRadius(U.dp(this, 22));
        seg.setBackground(bg);
        seg.setPadding(U.dp(this, 3), U.dp(this, 3), U.dp(this, 3), U.dp(this, 3));
        seg.addView(segment("Glass", Prefs.clockGlass(this), true), new LinearLayout.LayoutParams(0, U.dp(this, 40), 1f));
        seg.addView(segment("Solid", !Prefs.clockGlass(this), false), new LinearLayout.LayoutParams(0, U.dp(this, 40), 1f));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, -2);
        slp.topMargin = U.dp(this, 16);
        col.addView(seg, slp);

        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(-1, -2, Gravity.BOTTOM);
        lp.setMargins(U.dp(this, 10), 0, U.dp(this, 10), U.dp(this, 12));
        sheetHolder.addView(sheet, lp);
    }

    // ---------------------------------------------------------------------------------

    private interface FloatCb { void on(float v); }

    private TextView label(String t) {
        TextView v = new TextView(this);
        v.setText(t);
        v.setTextColor(0xB3FFFFFF);
        v.setTextSize(12);
        v.setPadding(0, U.dp(this, 12), 0, U.dp(this, 4));
        return v;
    }

    private LinearLayout.LayoutParams sliderLp() {
        return new LinearLayout.LayoutParams(-1, U.dp(this, 36));
    }

    private GlassSlider slider(String glyph, float value, final FloatCb cb) {
        GlassSlider s = new GlassSlider(this, false, glyph);
        s.setValue(value);
        s.setListener((v, user) -> cb.on(v));
        return s;
    }

    /** A "12" sample drawn in one of the clock faces. */
    private View fontChoice(final int which) {
        final boolean selected = Prefs.clockFont(this) == which;
        final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(LockClockView.face(which));
        p.setColor(0xFFFFFFFF);
        p.setTextAlign(Paint.Align.CENTER);
        p.setTextSize(U.dp(this, 34));
        final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
        frame.setStyle(Paint.Style.STROKE);
        frame.setStrokeWidth(U.dp(this, 2));
        frame.setColor(0x66FFFFFF);

        View v = new View(this) {
            @Override protected void onDraw(Canvas cv) {
                if (selected) {
                    cv.drawRoundRect(U.dp(getContext(), 8), 2, getWidth() - U.dp(getContext(), 8),
                            getHeight() - 2, U.dp(getContext(), 14), U.dp(getContext(), 14), frame);
                }
                float y = getHeight() / 2f - (p.descent() + p.ascent()) / 2f;
                cv.drawText("12", getWidth() / 2f, y, p);
            }
        };
        v.setOnClickListener(x -> {
            Prefs.setClockFont(this, which);
            clock.invalidate();
            buildSheet();
        });
        return v;
    }

    private View swatch(final int color) {
        final boolean selected = (Prefs.clockColor(this) | 0xFF000000) == color;
        View v = new View(this) {
            private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
            @Override protected void onDraw(Canvas cv) {
                float cx = getWidth() / 2f, cy = getHeight() / 2f;
                float r = Math.min(cx, cy) - U.dp(getContext(), 4);
                fill.setColor(color);
                cv.drawCircle(cx, cy, r, fill);
                if (selected) {
                    ring.setStyle(Paint.Style.STROKE);
                    ring.setStrokeWidth(U.dp(getContext(), 2.5f));
                    ring.setColor(0xFFFFFFFF);
                    cv.drawCircle(cx, cy, r + U.dp(getContext(), 4), ring);
                }
            }
        };
        v.setOnClickListener(x -> {
            Prefs.setClockColor(this, color);
            clock.invalidate();
            buildSheet();
        });
        v.setLayoutParams(new LinearLayout.LayoutParams(U.dp(this, 48), U.dp(this, 48)));
        return v;
    }

    private View segment(String text, boolean selected, final boolean glassValue) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setGravity(Gravity.CENTER);
        t.setTextSize(15);
        t.setTextColor(selected ? 0xFF1C1C1E : 0xFFFFFFFF);
        if (selected) {
            GradientDrawable g = new GradientDrawable();
            g.setColor(0xF2FFFFFF);
            g.setCornerRadius(U.dp(this, 19));
            t.setBackground(g);
        }
        t.setOnClickListener(v -> {
            Prefs.setClockGlass(this, glassValue);
            clock.invalidate();
            buildSheet();
        });
        return t;
    }

    @Override
    protected void onResume() {
        super.onResume();
        clock.refresh();
    }

    @Override
    public void onBackPressed() { finish(); }

}
