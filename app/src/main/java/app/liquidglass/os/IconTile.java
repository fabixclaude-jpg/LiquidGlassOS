package app.liquidglass.os;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.drawable.AdaptiveIconDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.view.View;

/** An app icon drawn as an iOS rounded square with a glass sheen and rim light on top. */
public class IconTile extends View {
    private Drawable icon;
    private final Paint base = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sheen = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint rim = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path clip = new Path();
    private final RectF r = new RectF();

    public IconTile(Context c, Drawable icon) {
        super(c);
        this.icon = icon;
        base.setColor(0x66FFFFFF);
        rim.setStyle(Paint.Style.STROKE);
        rim.setStrokeWidth(U.dp(c, 1));
    }

    @Override
    protected void onSizeChanged(int w, int h, int ow, int oh) {
        r.set(0, 0, w, h);
        float rad = w * 0.235f; // iOS-style corner proportion
        clip.reset();
        clip.addRoundRect(r, rad, rad, Path.Direction.CW);
        sheen.setShader(new LinearGradient(0, 0, 0, h * 0.6f, 0x55FFFFFF, 0x00FFFFFF, Shader.TileMode.CLAMP));
        rim.setShader(new LinearGradient(0, 0, w, h,
                new int[]{0xD9FFFFFF, 0x1FFFFFFF, 0x1FFFFFFF, 0x80FFFFFF},
                new float[]{0f, 0.35f, 0.65f, 1f}, Shader.TileMode.CLAMP));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        float rad = getWidth() * 0.235f;
        canvas.save();
        canvas.clipPath(clip);
        canvas.drawRect(r, base);
        if (icon != null) {
            boolean adaptive = Build.VERSION.SDK_INT >= 26 && icon instanceof AdaptiveIconDrawable;
            int inset = adaptive ? 0 : Math.round(getWidth() * 0.12f);
            int ex = adaptive ? Math.round(getWidth() * 0.17f) : 0; // adaptive icons carry extra bleed
            icon.setBounds(inset - ex, inset - ex, getWidth() - inset + ex, getHeight() - inset + ex);
            icon.draw(canvas);
        }
        canvas.drawRect(r, sheen);
        canvas.restore();
        float s = rim.getStrokeWidth() / 2f;
        canvas.drawRoundRect(s, s, getWidth() - s, getHeight() - s, rad, rad, rim);
    }
}
