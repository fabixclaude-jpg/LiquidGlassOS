package app.liquidglass.os;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

/** Builds the glass notification cards used on the lock screen and in the notification center. */
public final class NotifCards {
    private NotifCards() {}

    /**
     * Fills container with up to max cards.
     * @param tappable when true, tapping a card opens the notification and clears it
     * @param onOpen  called after a card was tapped (used to close the panel)
     * @param useWallpaper false for overlay windows that get their blur from the system
     */
    public static void fill(Context c, LinearLayout container, int max, boolean tappable,
                            boolean useWallpaper, final Runnable onOpen) {
        container.removeAllViews();
        List<NotifService.Item> list;
        synchronized (NotifService.items) { list = new ArrayList<>(NotifService.items); }
        int n = 0;
        for (final NotifService.Item it : list) {
            if (n++ >= max) break;
            GlassView card = new GlassView(c, 22);
            card.setUseWallpaper(useWallpaper);
            card.setForceDark(!useWallpaper);

            LinearLayout col = new LinearLayout(c);
            col.setOrientation(LinearLayout.VERTICAL);
            int p = U.dp(c, 14);
            col.setPadding(p, U.dp(c, 10), p, U.dp(c, 12));

            TextView app = new TextView(c);
            app.setText(it.app.toUpperCase());
            app.setTextColor(0xB3FFFFFF);
            app.setTextSize(11);
            app.setLetterSpacing(0.06f);
            col.addView(app);

            if (it.title.length() > 0) {
                TextView t = new TextView(c);
                t.setText(it.title);
                t.setTextColor(0xFFFFFFFF);
                t.setTextSize(15);
                t.setMaxLines(1);
                t.setEllipsize(TextUtils.TruncateAt.END);
                t.setTypeface(android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL));
                col.addView(t);
            }
            if (it.text.length() > 0) {
                TextView t = new TextView(c);
                t.setText(it.text);
                t.setTextColor(0xE6FFFFFF);
                t.setTextSize(14);
                t.setMaxLines(2);
                t.setEllipsize(TextUtils.TruncateAt.END);
                col.addView(t);
            }
            card.addView(col, new FrameLayout.LayoutParams(-1, -2));

            if (tappable) {
                card.setOnClickListener(new View.OnClickListener() {
                    @Override public void onClick(View v) {
                        try { if (it.intent != null) it.intent.send(); } catch (Exception ignored) { }
                        NotifService.dismiss(it.key);
                        if (onOpen != null) onOpen.run();
                    }
                });
            }
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.bottomMargin = U.dp(c, 8);
            container.addView(card, lp);
        }
    }
}
