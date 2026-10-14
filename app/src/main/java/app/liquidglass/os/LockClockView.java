package app.liquidglass.os;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.format.DateFormat;
import android.view.View;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * The big liquid glass lock screen clock.
 *
 * The digits are tall and condensed, and filled with a blurred slice of the wallpaper plus a white
 * gradient, so the picture shows through them like frosted glass. A bright rim and a soft glow sell the
 * lens effect. Height, colour, glass/solid and vertical position all come from Prefs, so the
 * "Font and Color" editor can change them live.
 */
public class LockClockView extends View {

    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint through = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint body = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint date = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Rect bounds = new Rect();
    private final Matrix m = new Matrix();
    private final int[] loc = new int[2];

    private String timeText = "";
    private String dateText = "";

    private final BroadcastReceiver tick = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { refresh(); }
    };

    public LockClockView(Context c) {
        super(c);
        setLayerType(LAYER_TYPE_SOFTWARE, null); // needed for blur mask filters on every version
        glow.setMaskFilter(new BlurMaskFilter(U.dp(c, 18), BlurMaskFilter.Blur.NORMAL));
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(U.dp(c, 1.6f));
        date.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        date.setTextAlign(Paint.Align.CENTER);
        date.setTextSize(U.dp(c, 21));
        date.setShadowLayer(8, 0, 2, 0x55000000);
        refresh();
    }

    /** The three clock faces offered in the lock screen editor. */
    public static Typeface face(int which) {
        switch (which) {
            case 1:  return Typeface.create("sans-serif-medium", Typeface.NORMAL);
            case 2:  return Typeface.create("sans-serif-light", Typeface.NORMAL);
            default: return Typeface.create("sans-serif-condensed", Typeface.BOLD);
        }
    }

    public void refresh() {
        Context c = getContext();
        Calendar cal = Calendar.getInstance();
        boolean h24 = DateFormat.is24HourFormat(c);
        int h = cal.get(Calendar.HOUR_OF_DAY);
        if (!h24) { h = h % 12; if (h == 0) h = 12; }
        int min = cal.get(Calendar.MINUTE);
        timeText = (h24 && h < 10 ? "0" : "") + h + ":" + (min < 10 ? "0" : "") + min;
        dateText = DateFormat.format("EEEE, MMMM d", new Date()).toString();
        invalidate();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        IntentFilter f = new IntentFilter();
        f.addAction(Intent.ACTION_TIME_TICK);
        f.addAction(Intent.ACTION_TIME_CHANGED);
        f.addAction(Intent.ACTION_TIMEZONE_CHANGED);
        getContext().registerReceiver(tick, f);
        refresh();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        try { getContext().unregisterReceiver(tick); } catch (Exception ignored) { }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Context c = getContext();
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        Typeface face = face(Prefs.clockFont(c));
        for (Paint p : new Paint[]{glow, through, body, rim}) p.setTypeface(face);

        int color = Prefs.clockColor(c);
        boolean glass = Prefs.clockGlass(c);
        float stretch = 1f + Prefs.clockHeight(c) / 100f * 1.1f;

        // Pick a text size so the widest possible time ("00:00") fits with side margins.
        float size = 100f;
        body.setTextSize(size);
        float full = body.measureText("00:00");
        size = size * (w * 0.88f) / full;
        for (Paint p : new Paint[]{glow, through, body, rim}) p.setTextSize(size);
        float tw = body.measureText(timeText);
        body.getTextBounds(timeText, 0, timeText.length(), bounds);
        float digitH = bounds.height() * stretch;

        // Vertical position: Prefs.clockY 0..100 moves the clock from the top to the lower part of the screen.
        float topLimit = U.dp(c, 120);
        float bottomLimit = h * 0.62f;
        float top = topLimit + (Math.max(topLimit, bottomLimit - digitH) - topLimit) * Prefs.clockY(c) / 100f;
        float baseline = top + digitH;
        float x = (w - tw) / 2f;

        // date above the digits
        date.setColor(0xFFFFFFFF);
        canvas.drawText(dateText, w / 2f, top - U.dp(c, 14), date);

        canvas.save();
        canvas.translate(x, baseline);
        canvas.scale(1f, stretch);

        // 1. soft glow under the digits
        glow.setColor(glass ? 0x55FFFFFF : 0x33000000);
        canvas.drawText(timeText, 0, 0, glow);

        if (glass) {
            // 2. wallpaper seen through the glass (blurred, lifted a little)
            Bitmap blurred = Wallpaper.blurred(c);
            if (blurred != null) {
                getLocationOnScreen(loc);
                BitmapShader shader = new BitmapShader(blurred, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
                // The canvas is translated to (x, baseline) and scaled vertically, so map screen-space
                // bitmap pixels back into that local space for the shader.
                m.setTranslate(-loc[0] - x, -loc[1] - baseline);
                m.postScale(1f, 1f / stretch);
                shader.setLocalMatrix(m);
                through.setShader(shader);
                canvas.drawText(timeText, 0, 0, through);
            }
            // 3. white body gradient: brighter on top, clearer at the bottom
            int a1 = (color & 0x00FFFFFF) | 0xA6000000;
            int a2 = (color & 0x00FFFFFF) | 0x38000000;
            body.setShader(new LinearGradient(0, -bounds.height(), 0, 0, a1, a2, Shader.TileMode.CLAMP));
            canvas.drawText(timeText, 0, 0, body);
        } else {
            body.setShader(null);
            body.setColor(color | 0xFF000000);
            canvas.drawText(timeText, 0, 0, body);
        }

        // 4. specular rim
        rim.setShader(new LinearGradient(0, -bounds.height(), tw, 0,
                new int[]{0xF2FFFFFF, 0x33FFFFFF, 0x26FFFFFF, 0xB3FFFFFF},
                new float[]{0f, 0.4f, 0.65f, 1f}, Shader.TileMode.CLAMP));
        canvas.drawText(timeText, 0, 0, rim);

        canvas.restore();
    }

    /** Where the bottom of the digits ended up, so other views can sit below them. */
    public int digitsBottom() {
        Context c = getContext();
        int h = getHeight();
        body.setTypeface(face(Prefs.clockFont(c)));
        body.setTextSize(100f);
        float full = body.measureText("00:00");
        body.setTextSize(100f * (getWidth() * 0.88f) / Math.max(1f, full));
        float stretch = 1f + Prefs.clockHeight(c) / 100f * 1.1f;
        body.getTextBounds(timeText, 0, timeText.length(), bounds);
        float digitH = bounds.height() * stretch;
        float topLimit = U.dp(c, 120);
        float bottomLimit = h * 0.62f;
        float top = topLimit + (Math.max(topLimit, bottomLimit - digitH) - topLimit) * Prefs.clockY(c) / 100f;
        return Math.round(top + digitH);
    }

    @Override
    public String toString() { return String.format(Locale.US, "LockClockView(%s)", timeText); }
}
