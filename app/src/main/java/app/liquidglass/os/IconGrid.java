package app.liquidglass.os;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;

/** Lays children out in an even cols x rows grid. */
public class IconGrid extends ViewGroup {
    private final int cols;
    private final int rows;

    public IconGrid(Context c, int cols, int rows) {
        super(c);
        this.cols = cols;
        this.rows = rows;
    }

    @Override
    protected void onMeasure(int wSpec, int hSpec) {
        int w = MeasureSpec.getSize(wSpec);
        int h = MeasureSpec.getSize(hSpec);
        int cw = w / cols;
        int ch = h / rows;
        for (int i = 0; i < getChildCount(); i++) {
            getChildAt(i).measure(
                    MeasureSpec.makeMeasureSpec(cw, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(ch, MeasureSpec.EXACTLY));
        }
        setMeasuredDimension(w, h);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int cw = (r - l) / cols;
        int ch = (b - t) / rows;
        for (int i = 0; i < getChildCount(); i++) {
            View v = getChildAt(i);
            int col = i % cols;
            int row = i / cols;
            v.layout(col * cw, row * ch, (col + 1) * cw, (row + 1) * ch);
        }
    }
}
