package app.liquidglass.os;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.os.BatteryManager;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

/** Home screen widgets, all built on the glass panel. */
public final class Widgets {
    private Widgets() {}

    private static TextClock clock(Context c, String f12, String f24, int sp, int color, boolean bold) {
        TextClock t = new TextClock(c);
        t.setFormat12Hour(f12);
        t.setFormat24Hour(f24);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setTypeface(Typeface.create(bold ? "sans-serif-medium" : "sans-serif-light", Typeface.NORMAL));
        t.setIncludeFontPadding(false);
        return t;
    }

    /** Big time + date card. */
    public static class ClockCard extends GlassView {
        public ClockCard(Context c) {
            super(c, 26);
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER_VERTICAL);
            int p = U.dp(c, 16);
            col.setPadding(p, p, p, p);
            col.addView(clock(c, "h:mm", "HH:mm", 44, 0xFFFFFFFF, false));
            col.addView(clock(c, "EEEE, MMM d", "EEEE, MMM d", 14, 0xCCFFFFFF, true));
            addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
    }

    /** Day-of-week and date number card. */
    public static class CalendarCard extends GlassView {
        public CalendarCard(Context c) {
            super(c, 26);
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            col.addView(clock(c, "EEE", "EEE", 13, 0xFFFF6B6B, true));
            col.addView(clock(c, "d", "d", 46, 0xFFFFFFFF, false));
            addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }
    }

    /** Battery ring card. */
    public static class BatteryCard extends GlassView {
        private final Ring ring;
        private final BroadcastReceiver rx = new BroadcastReceiver() {
            @Override public void onReceive(Context context, Intent i) { ring.update(i); }
        };

        public BatteryCard(Context c) {
            super(c, 26);
            ring = new Ring(c);
            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            col.setGravity(Gravity.CENTER);
            int p = U.dp(c, 12);
            col.setPadding(p, p, p, p);
            col.addView(ring, new LinearLayout.LayoutParams(U.dp(c, 64), U.dp(c, 64)));
            TextView label = new TextView(c);
            label.setText("Battery");
            label.setTextColor(0xCCFFFFFF);
            label.setTextSize(12);
            label.setGravity(Gravity.CENTER);
            col.addView(label);
            addView(col, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
        }

        @Override
        protected void onAttachedToWindow() {
            super.onAttachedToWindow();
            Intent sticky = getContext().registerReceiver(rx, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (sticky != null) ring.update(sticky);
        }

        @Override
        protected void onDetachedFromWindow() {
            super.onDetachedFromWindow();
            try { getContext().unregisterReceiver(rx); } catch (Exception ignored) { }
        }

        private static class Ring extends View {
            private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final Paint text = new Paint(Paint.ANTI_ALIAS_FLAG);
            private final RectF box = new RectF();
            private int pct = 100;
            private boolean charging = false;

            Ring(Context c) {
                super(c);
                float w = U.dp(c, 7);
                track.setStyle(Paint.Style.STROKE);
                track.setStrokeWidth(w);
                track.setColor(0x44FFFFFF);
                arc.setStyle(Paint.Style.STROKE);
                arc.setStrokeWidth(w);
                arc.setStrokeCap(Paint.Cap.ROUND);
                text.setColor(0xFFFFFFFF);
                text.setTextAlign(Paint.Align.CENTER);
                text.setTextSize(U.dp(c, 16));
                text.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
            }

            void update(Intent i) {
                int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, 100);
                int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
                int status = i.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
                pct = Math.round(level * 100f / Math.max(1, scale));
                charging = status == BatteryManager.BATTERY_STATUS_CHARGING
                        || status == BatteryManager.BATTERY_STATUS_FULL;
                invalidate();
            }

            @Override
            protected void onDraw(Canvas cv) {
                float pad = track.getStrokeWidth();
                box.set(pad, pad, getWidth() - pad, getHeight() - pad);
                cv.drawArc(box, 0, 360, false, track);
                arc.setColor(charging ? 0xFF4CD964 : (pct <= 20 ? 0xFFFF453A : 0xFFFFFFFF));
                cv.drawArc(box, -90, 360f * pct / 100f, false, arc);
                float y = getHeight() / 2f - (text.descent() + text.ascent()) / 2f;
                cv.drawText(pct + "%", getWidth() / 2f, y, text);
            }
        }
    }
}
