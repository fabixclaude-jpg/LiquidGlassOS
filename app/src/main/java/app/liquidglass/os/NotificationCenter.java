package app.liquidglass.os;

import android.content.Context;
import android.graphics.Typeface;
import android.provider.Settings;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Date;

/** The liquid glass notification center panel (the replacement for the stock pull-down shade). */
public final class NotificationCenter {
    private NotificationCenter() {}

    public static boolean accessEnabled(Context c) {
        String s = Settings.Secure.getString(c.getContentResolver(), "enabled_notification_listeners");
        return s != null && s.contains(c.getPackageName());
    }

    public static View build(final Context c, final Runnable close) {
        LinearLayout col = new LinearLayout(c);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(U.dp(c, 16), U.dp(c, 8), U.dp(c, 16), U.dp(c, 8));

        // header: title on the left, clear pill on the right
        LinearLayout head = new LinearLayout(c);
        head.setGravity(Gravity.CENTER_VERTICAL);
        LinearLayout titles = new LinearLayout(c);
        titles.setOrientation(LinearLayout.VERTICAL);
        TextView title = new TextView(c);
        title.setText("Notification Center");
        title.setTextColor(0xFFFFFFFF);
        title.setTextSize(24);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        TextView date = new TextView(c);
        date.setText(DateFormat.format("EEEE, MMMM d", new Date()));
        date.setTextColor(0xB3FFFFFF);
        date.setTextSize(14);
        titles.addView(title);
        titles.addView(date);
        head.addView(titles, new LinearLayout.LayoutParams(0, -2, 1f));

        GlassView clear = new GlassView(c, 18);
        clear.setUseWallpaper(false);
        TextView ct = new TextView(c);
        ct.setText("Clear");
        ct.setTextColor(0xFFFFFFFF);
        ct.setTextSize(14);
        ct.setGravity(Gravity.CENTER);
        clear.addView(ct, new FrameLayout.LayoutParams(-1, -1));
        head.addView(clear, new LinearLayout.LayoutParams(U.dp(c, 72), U.dp(c, 36)));
        col.addView(head, new LinearLayout.LayoutParams(-1, -2));

        final LinearLayout list = new LinearLayout(c);
        list.setOrientation(LinearLayout.VERTICAL);
        ScrollView sv = new ScrollView(c);
        sv.setVerticalScrollBarEnabled(false);
        sv.addView(list, new FrameLayout.LayoutParams(-1, -2));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, U.dp(c, 420));
        slp.topMargin = U.dp(c, 14);
        col.addView(sv, slp);

        clear.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { NotifService.dismissAll(); }
        });

        final Runnable refresh = new Runnable() {
            @Override public void run() {
                boolean empty;
                synchronized (NotifService.items) { empty = NotifService.items.isEmpty(); }
                if (empty) {
                    list.removeAllViews();
                    TextView t = new TextView(c);
                    t.setText(accessEnabled(c) ? "No Notifications"
                            : "Turn on notification access in Glass Settings to see notifications here.");
                    t.setTextColor(0xB3FFFFFF);
                    t.setTextSize(16);
                    t.setGravity(Gravity.CENTER);
                    t.setPadding(0, U.dp(c, 60), 0, 0);
                    list.addView(t, new LinearLayout.LayoutParams(-1, -2));
                } else {
                    NotifCards.fill(c, list, 30, true, false, close);
                }
            }
        };
        refresh.run();
        NotifService.onChange = refresh;
        return col;
    }
}
