package app.liquidglass.os;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;

/** Home screen pages with snapping. Pages are added with addPage(). */
public class PagerView extends HorizontalScrollView {

    public interface Listener { void onPage(int page, int count); }

    private final LinearLayout strip;
    private Listener listener;
    private int pageWidth;
    private int startPage;
    private int lastReported = -1;

    public PagerView(Context c) {
        super(c);
        setHorizontalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
        setFillViewport(true);
        strip = new LinearLayout(c);
        strip.setOrientation(LinearLayout.HORIZONTAL);
        addView(strip, new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT));
    }

    public void setListener(Listener l) { listener = l; }

    public void clearPages() { strip.removeAllViews(); lastReported = -1; scrollTo(0, 0); }

    public int pageCount() { return strip.getChildCount(); }

    public void addPage(View page) {
        strip.addView(page, new LinearLayout.LayoutParams(U.screen(getContext()).x,
                LinearLayout.LayoutParams.MATCH_PARENT));
        pageWidth = U.screen(getContext()).x;
        report(0);
    }

    private int currentPage() {
        if (pageWidth == 0) return 0;
        return Math.round(getScrollX() / (float) pageWidth);
    }

    private void report(int page) {
        if (listener != null && page != lastReported) {
            lastReported = page;
            listener.onPage(page, strip.getChildCount());
        }
    }

    @Override
    protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        U.invalidateGlass(this);
        report(currentPage());
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        int a = ev.getActionMasked();
        if (a == MotionEvent.ACTION_DOWN) startPage = currentPage();
        boolean handled = super.onTouchEvent(ev);
        if ((a == MotionEvent.ACTION_UP || a == MotionEvent.ACTION_CANCEL) && pageWidth > 0) {
            int dx = getScrollX() - startPage * pageWidth;
            int target = startPage;
            if (dx > pageWidth * 0.15f) target = startPage + 1;
            else if (dx < -pageWidth * 0.15f) target = startPage - 1;
            target = Math.max(0, Math.min(strip.getChildCount() - 1, target));
            smoothScrollTo(target * pageWidth, 0);
            return true;
        }
        return handled;
    }
}
