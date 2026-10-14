package app.liquidglass.os;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.view.View;

/**
 * SF-Symbols-style icons drawn with plain canvas paths, so the control center needs no image files.
 * Everything is described in a unit square and scaled to the requested size.
 */
public final class Glyph {
    private Glyph() {}

    public static void draw(Canvas cv, String kind, float x, float y, float s, int color) {
        Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
        line.setColor(color);
        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(s * 0.085f);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
        fill.setColor(color);
        fill.setStyle(Paint.Style.FILL);

        cv.save();
        cv.translate(x, y);
        cv.scale(s, s);
        // paints are in pixels, canvas is scaled, so convert stroke back to unit space
        line.setStrokeWidth(0.085f);

        switch (kind) {
            case "wifi": {
                RectF r;
                for (int i = 1; i <= 3; i++) {
                    float rad = 0.17f * i;
                    r = new RectF(0.5f - rad, 0.78f - rad, 0.5f + rad, 0.78f + rad);
                    cv.drawArc(r, 225, 90, false, line);
                }
                cv.drawCircle(0.5f, 0.78f, 0.055f, fill);
                break;
            }
            case "bluetooth": {
                Path p = new Path();
                p.moveTo(0.28f, 0.30f);
                p.lineTo(0.70f, 0.70f);
                p.lineTo(0.50f, 0.90f);
                p.lineTo(0.50f, 0.10f);
                p.lineTo(0.70f, 0.30f);
                p.lineTo(0.28f, 0.70f);
                cv.drawPath(p, line);
                break;
            }
            case "airplane": {
                float[] pts = {0.5f, 0.05f, 0.58f, 0.35f, 0.95f, 0.58f, 0.95f, 0.68f, 0.58f, 0.58f,
                        0.55f, 0.82f, 0.70f, 0.92f, 0.70f, 0.97f, 0.5f, 0.92f, 0.30f, 0.97f, 0.30f, 0.92f,
                        0.45f, 0.82f, 0.42f, 0.58f, 0.05f, 0.68f, 0.05f, 0.58f, 0.42f, 0.35f};
                Path p = new Path();
                p.moveTo(pts[0], pts[1]);
                for (int i = 2; i < pts.length; i += 2) p.lineTo(pts[i], pts[i + 1]);
                p.close();
                cv.drawPath(p, fill);
                break;
            }
            case "cell": {
                for (int i = 0; i < 4; i++) {
                    float h = 0.22f + i * 0.2f;
                    float left = 0.10f + i * 0.22f;
                    cv.drawRoundRect(new RectF(left, 0.92f - h, left + 0.14f, 0.92f), 0.04f, 0.04f, fill);
                }
                break;
            }
            case "rotate": {
                cv.drawArc(new RectF(0.12f, 0.12f, 0.88f, 0.88f), -60, 290, false, line);
                Path head = new Path();
                head.moveTo(0.60f, 0.06f);
                head.lineTo(0.84f, 0.20f);
                head.lineTo(0.56f, 0.28f);
                head.close();
                cv.drawPath(head, fill);
                cv.drawRoundRect(new RectF(0.36f, 0.46f, 0.64f, 0.68f), 0.04f, 0.04f, fill);
                cv.drawArc(new RectF(0.41f, 0.32f, 0.59f, 0.56f), 180, 180, false, line);
                break;
            }
            case "mirror": {
                cv.drawRoundRect(new RectF(0.10f, 0.22f, 0.70f, 0.68f), 0.07f, 0.07f, line);
                cv.drawRoundRect(new RectF(0.40f, 0.42f, 0.90f, 0.82f), 0.07f, 0.07f, line);
                break;
            }
            case "moon": {
                Path a = new Path();
                a.addCircle(0.5f, 0.5f, 0.38f, Path.Direction.CW);
                Path b = new Path();
                b.addCircle(0.68f, 0.38f, 0.32f, Path.Direction.CW);
                a.op(b, Path.Op.DIFFERENCE);
                cv.drawPath(a, fill);
                break;
            }
            case "torch": {
                Path p = new Path();
                p.moveTo(0.34f, 0.08f);
                p.lineTo(0.66f, 0.08f);
                p.lineTo(0.66f, 0.26f);
                p.lineTo(0.58f, 0.36f);
                p.lineTo(0.58f, 0.92f);
                p.lineTo(0.42f, 0.92f);
                p.lineTo(0.42f, 0.36f);
                p.lineTo(0.34f, 0.26f);
                p.close();
                cv.drawPath(p, fill);
                break;
            }
            case "timer": {
                cv.drawCircle(0.5f, 0.56f, 0.34f, line);
                cv.drawLine(0.5f, 0.56f, 0.5f, 0.36f, line);
                cv.drawLine(0.40f, 0.08f, 0.60f, 0.08f, line);
                cv.drawLine(0.5f, 0.08f, 0.5f, 0.2f, line);
                break;
            }
            case "calc": {
                cv.drawRoundRect(new RectF(0.18f, 0.08f, 0.82f, 0.92f), 0.10f, 0.10f, line);
                cv.drawRoundRect(new RectF(0.28f, 0.18f, 0.72f, 0.36f), 0.03f, 0.03f, fill);
                for (int r = 0; r < 3; r++)
                    for (int c = 0; c < 3; c++)
                        cv.drawCircle(0.34f + c * 0.16f, 0.50f + r * 0.14f, 0.032f, fill);
                break;
            }
            case "camera": {
                cv.drawRoundRect(new RectF(0.08f, 0.26f, 0.92f, 0.82f), 0.10f, 0.10f, line);
                cv.drawCircle(0.5f, 0.54f, 0.17f, line);
                cv.drawLine(0.34f, 0.26f, 0.40f, 0.16f, line);
                cv.drawLine(0.40f, 0.16f, 0.60f, 0.16f, line);
                cv.drawLine(0.60f, 0.16f, 0.66f, 0.26f, line);
                break;
            }
            case "sun": {
                cv.drawCircle(0.5f, 0.5f, 0.17f, fill);
                for (int i = 0; i < 8; i++) {
                    double a = Math.PI / 4 * i;
                    float c = (float) Math.cos(a), sn = (float) Math.sin(a);
                    cv.drawLine(0.5f + c * 0.27f, 0.5f + sn * 0.27f, 0.5f + c * 0.40f, 0.5f + sn * 0.40f, line);
                }
                break;
            }
            case "speaker": {
                Path p = new Path();
                p.moveTo(0.12f, 0.38f);
                p.lineTo(0.30f, 0.38f);
                p.lineTo(0.50f, 0.20f);
                p.lineTo(0.50f, 0.80f);
                p.lineTo(0.30f, 0.62f);
                p.lineTo(0.12f, 0.62f);
                p.close();
                cv.drawPath(p, fill);
                cv.drawArc(new RectF(0.40f, 0.34f, 0.72f, 0.66f), -45, 90, false, line);
                cv.drawArc(new RectF(0.34f, 0.22f, 0.90f, 0.78f), -45, 90, false, line);
                break;
            }
            case "play": {
                Path p = new Path();
                p.moveTo(0.30f, 0.16f);
                p.lineTo(0.84f, 0.50f);
                p.lineTo(0.30f, 0.84f);
                p.close();
                cv.drawPath(p, fill);
                break;
            }
            default:
                cv.drawCircle(0.5f, 0.5f, 0.3f, line);
        }
        cv.restore();
    }

    /** A view that just shows one glyph. */
    public static class View2 extends View {
        private String kind;
        private int color = 0xFFFFFFFF;

        public View2(Context c, String kind) { super(c); this.kind = kind; }

        public void set(String kind, int color) { this.kind = kind; this.color = color; invalidate(); }

        @Override
        protected void onDraw(Canvas canvas) {
            float s = Math.min(getWidth(), getHeight());
            Glyph.draw(canvas, kind, (getWidth() - s) / 2f, (getHeight() - s) / 2f, s, color);
        }
    }
}
