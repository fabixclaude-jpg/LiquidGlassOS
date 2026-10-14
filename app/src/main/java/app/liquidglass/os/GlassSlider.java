package app.liquidglass.os;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.MotionEvent;
import android.view.View;

/**
 * iOS style slider: a tall glass pill that fills from the bottom (vertical) or the left (horizontal),
 * with an icon on it. Used for brightness and volume in the control center and for the editor sliders.
 */
public class GlassSlider extends View {

    public interface Listener { void onChange(float value, boolean fromUser); }

    private final boolean vertical;
    private final String glyph;
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF r = new RectF();
    private float value = 0.5f;
    private Listener listener;
    private boolean enabledTouch = true;

    public GlassSlider(Context c, boolean vertical, String glyph) {
        super(c);
        this.vertical = vertical;
        this.glyph = glyph;
        track.setColor(0x40FFFFFF);
        fill.setColor(0xF2FFFFFF);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(U.dp(c, 1));
        rim.setColor(0x80FFFFFF);
    }

    public void setListener(Listener l) { listener = l; }

    public void setValue(float v) {
        value = Math.max(0f, Math.min(1f, v));
        invalidate();
    }

    public float getValue() { return value; }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        r.set(0, 0, w, h);
        float rad = (vertical ? w : h) * 0.5f;
        if (vertical) rad = Math.min(rad, U.dp(getContext(), 30));
        clip.reset();
        clip.addRoundRect(r, rad, rad, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas cv) {
        float rad = Math.min(U.dp(getContext(), 30), (vertical ? getWidth() : getHeight()) * 0.5f);
        cv.save();
        cv.clipPath(clip);
        cv.drawRect(r, track);
        if (vertical) {
            cv.drawRect(0, getHeight() * (1f - value), getWidth(), getHeight(), fill);
        } else {
            cv.drawRect(0, 0, getWidth() * value, getHeight(), fill);
        }
        cv.restore();
        cv.drawRoundRect(0.5f, 0.5f, getWidth() - 0.5f, getHeight() - 0.5f, rad, rad, rim);

        // glyph near the bottom (vertical) or the left (horizontal); dark where the fill is under it
        float s = U.dp(getContext(), 24);
        float gx, gy;
        boolean covered;
        if (vertical) {
            gx = (getWidth() - s) / 2f;
            gy = getHeight() - s - U.dp(getContext(), 16);
            covered = getHeight() * (1f - value) < gy + s / 2f;
        } else {
            gx = U.dp(getContext(), 14);
            gy = (getHeight() - s) / 2f;
            covered = getWidth() * value > gx + s / 2f;
        }
        Glyph.draw(cv, glyph, gx, gy, s, covered ? 0xFF6E6E73 : 0xFFFFFFFF);
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (!enabledTouch) return false;
        int a = e.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) getParent().requestDisallowInterceptTouchEvent(true);
        if (a == MotionEvent.ACTION_DOWN || a == MotionEvent.ACTION_MOVE || a == MotionEvent.ACTION_UP) {
            float v = vertical ? 1f - e.getY() / getHeight() : e.getX() / getWidth();
            setValue(v);
            if (listener != null) listener.onChange(value, true);
            return true;
        }
        return a == MotionEvent.ACTION_CANCEL;
    }
}
