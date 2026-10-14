package app.liquidglass.os;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/** Restarts the lock screen service after a reboot or an app update. */
public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context c, Intent i) {
        if (Prefs.lockEnabled(c)) {
            try { LockService.start(c); } catch (Exception ignored) { }
        }
    }
}
