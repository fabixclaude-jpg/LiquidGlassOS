package app.liquidglass.os;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.widget.FrameLayout;

/**
 * The liquid glass panel.
 * Draws, in order: a blurred slice of the wallpaper, a tint, a soft top sheen, then (above the
 * children) a bright specular rim that is brighter at the top-left and bottom-right edges,
 * which is what makes iOS glass look like a thick lens.
 */
public class GlassView extends FrameLayout {

    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bmp = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Path clip = new Path();
    private final RectF rect = new RectF();
    private final int[] loc = new int[2];

    private float radius;
    private boolean useWallpaper = true;
    private boolean forceDark = false;

    public GlassView(Context c) { this(c, 24); }

    public GlassView(Context c, float radiusDp) {
        super(c);
        radius = U.dp(c, radiusDp);
        setWillNotDraw(false);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(Math.max(1.5f, U.dp(c, 1f)));
    }

    public void setRadiusDp(float dp) { radius = U.dp(getContext(), dp); invalidate(); }

    /** Pass false inside overlay windows that are blurred by the system instead. */
    public void setUseWallpaper(boolean v) { useWallpaper = v; invalidate(); }

    public void setForceDark(boolean v) { forceDark = v; invalidate(); }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        super.onSizeChanged(w, h, ow, oh);
        rect.set(0, 0, w, h);
        clip.reset();
        clip.addRoundRect(rect, radius, radius, Path.Direction.CW);

        sheen.setShader(new LinearGradient(0, 0, 0, h * 0.7f,
                0x40FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        rim.setShader(new LinearGradient(0, 0, w, h,
                new int[]{0xE6FFFFFF, 0x26FFFFFF, 0x1AFFFFFF, 0x99FFFFFF},
                new float[]{0f, 0.35f, 0.65f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getWidth() == 0 || getHeight() == 0) return;
        Context c = getContext();
        boolean dark = forceDark || Prefs.theme(c) == 1;
        boolean solid = Prefs.solid(c);
        int tint = Prefs.tint(c);

        canvas.save();
        canvas.clipPath(clip);

        // Every glass panel blurs something: the wallpaper on the home and lock screens,
        // or the blurred screenshot / wallpaper backdrop inside overlay windows.
        Bitmap blurredBehind = null;
        if (!solid) {
            blurredBehind = useWallpaper ? Wallpaper.blurred(c) : Backdrop.resolve(c);
            if (blurredBehind != null) {
                getLocationOnScreen(loc);
                canvas.drawBitmap(blurredBehind, -loc[0], -loc[1], bmp);
            }
        }

        int alpha;
        int base = dark ? 0x000000 : 0xFFFFFF;
        if (solid) {
            alpha = 235;
            base = dark ? 0x1C1C1E : 0xF2F2F7;
        } else if (blurredBehind != null) {
            alpha = 30 + Math.round(tint * 1.1f);
            if (dark) alpha = Math.min(255, alpha + 40);
        } else {
            alpha = 60 + Math.round(tint * 1.3f);
        }
        fill.setColor((Math.min(255, alpha) << 24) | base);
        canvas.drawRect(rect, fill);

        if (!solid) canvas.drawRect(rect, sheen);
        canvas.restore();
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        if (Prefs.solid(getContext())) return;
        float inset = rim.getStrokeWidth() / 2f;
        canvas.drawRoundRect(inset, inset, getWidth() - inset, getHeight() - inset,
                radius, radius, rim);
    }
}
