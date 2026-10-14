package app.liquidglass.os;

import android.content.Context;
import android.widget.ScrollView;

/** ScrollView that repaints its glass panels while scrolling, so the blur stays locked to the screen. */
public class GlassScroll extends ScrollView {
    public GlassScroll(Context c) {
        super(c);
        setVerticalScrollBarEnabled(false);
        setOverScrollMode(OVER_SCROLL_NEVER);
    }

    @Override
    protected void onScrollChanged(int l, int t, int ol, int ot) {
        super.onScrollChanged(l, t, ol, ot);
        U.invalidateGlass(this);
    }
}
