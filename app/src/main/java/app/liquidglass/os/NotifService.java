package app.liquidglass.os;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Collects the phone's notifications so they can be shown as glass cards on the lock screen and notification center. */
public class NotifService extends NotificationListenerService {

    public static final class Item {
        public String key;
        public String pkg;
        public String app;
        public String title;
        public String text;
        public long time;
        public PendingIntent intent;
    }

    public static final List<Item> items = new ArrayList<>();
    public static Runnable onChange;
    private static NotifService instance;

    @Override
    public void onListenerConnected() {
        instance = this;
        rebuild();
    }

    @Override
    public void onListenerDisconnected() {
        if (instance == this) instance = null;
    }

    @Override
    public void onNotificationPosted(StatusBarNotification sbn) { rebuild(); }

    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) { rebuild(); }

    public static void dismiss(String key) {
        if (instance != null) {
            try { instance.cancelNotification(key); } catch (Exception ignored) { }
        }
    }

    public static void dismissAll() {
        if (instance != null) {
            try { instance.cancelAllNotifications(); } catch (Exception ignored) { }
        }
    }

    private void rebuild() {
        List<Item> fresh = new ArrayList<>();
        try {
            PackageManager pm = getPackageManager();
            StatusBarNotification[] active = getActiveNotifications();
            if (active != null) {
                for (StatusBarNotification s : active) {
                    Notification n = s.getNotification();
                    if (n == null) continue;
                    if ((n.flags & Notification.FLAG_GROUP_SUMMARY) != 0) continue;
                    if (s.isOngoing()) continue;
                    if (s.getPackageName().equals(getPackageName())) continue;
                    Item it = new Item();
                    it.key = s.getKey();
                    it.pkg = s.getPackageName();
                    it.time = s.getPostTime();
                    it.intent = n.contentIntent;
                    try {
                        it.app = String.valueOf(pm.getApplicationLabel(pm.getApplicationInfo(it.pkg, 0)));
                    } catch (Exception e) {
                        it.app = it.pkg;
                    }
                    CharSequence t = n.extras.getCharSequence(Notification.EXTRA_TITLE);
                    CharSequence x = n.extras.getCharSequence(Notification.EXTRA_TEXT);
                    it.title = t == null ? "" : t.toString();
                    it.text = x == null ? "" : x.toString();
                    if (it.title.length() == 0 && it.text.length() == 0) continue;
                    fresh.add(it);
                }
            }
        } catch (Exception ignored) {
            // listener not ready yet
        }
        Collections.sort(fresh, new Comparator<Item>() {
            @Override public int compare(Item a, Item b) { return Long.compare(b.time, a.time); }
        });
        synchronized (items) {
            items.clear();
            items.addAll(fresh);
        }
        final Runnable r = onChange;
        if (r != null) {
            android.os.Handler h = new android.os.Handler(android.os.Looper.getMainLooper());
            h.post(r);
        }
    }
}
