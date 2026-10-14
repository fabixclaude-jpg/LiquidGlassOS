package app.liquidglass.os;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Display;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.FrameLayout;

/**
 * Puts the liquid glass control center and notification center on top of the stock ones.
 *
 * Two thin invisible strips sit along the top edge of the screen. Swiping down on the right strip opens the
 * control center, swiping down on the left strip opens the notification center. As a backup, if the stock
 * notification shade opens anyway, it is closed and the glass control center is shown instead.
 */
public class GlassAccessibilityService extends AccessibilityService {

    private WindowManager wm;
    private View hotLeft;
    private View hotRight;
    private FrameLayout panel;
    private long lastShow = 0;

    private final BroadcastReceiver screenRx = new BroadcastReceiver() {
        @Override public void onReceive(Context c, Intent i) { hidePanel(false); }
    };

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        wm = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        addHotspots();
        registerReceiver(screenRx, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    @Override
    public boolean onUnbind(Intent intent) {
        cleanup();
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        cleanup();
        super.onDestroy();
    }

    private void cleanup() {
        hidePanel(false);
        try { if (hotLeft != null) wm.removeView(hotLeft); } catch (Exception ignored) { }
        try { if (hotRight != null) wm.removeView(hotRight); } catch (Exception ignored) { }
        hotLeft = null;
        hotRight = null;
        try { unregisterReceiver(screenRx); } catch (Exception ignored) { }
    }

    @Override
    public void onInterrupt() { }

    // ---------------------------------------------------------------------------------
    // backup: stock shade opened anyway
    // ---------------------------------------------------------------------------------

    @Override
    public void onAccessibilityEvent(AccessibilityEvent e) {
        if (e == null || e.getEventType() != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return;
        CharSequence pkg = e.getPackageName();
        if (pkg == null || !"com.android.systemui".contentEquals(pkg)) return;
        String cls = e.getClassName() == null ? "" : e.getClassName().toString().toLowerCase();
        if (!(cls.contains("shade") || cls.contains("quicksettings") || cls.contains("notificationpanel"))) return;
        if (SystemClock.uptimeMillis() - lastShow < 900) return;
        if (Build.VERSION.SDK_INT >= 31) performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
        showControl();
    }

    // ---------------------------------------------------------------------------------
    // edge strips
    // ---------------------------------------------------------------------------------

    private int overlayType() {
        return Build.VERSION.SDK_INT >= 22
                ? WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
    }

    private void addHotspots() {
        int screenW = U.screen(this).x;
        int h = U.dp(this, 26);
        hotLeft = hotspot(false);
        hotRight = hotspot(true);
        WindowManager.LayoutParams l = new WindowManager.LayoutParams((int) (screenW * 0.6f), h, overlayType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                        | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                        | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                        | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
                PixelFormat.TRANSLUCENT);
        l.gravity = Gravity.TOP | Gravity.START;
        WindowManager.LayoutParams r = new WindowManager.LayoutParams((int) (screenW * 0.4f), h, overlayType(),
                l.flags, PixelFormat.TRANSLUCENT);
        r.gravity = Gravity.TOP | Gravity.END;
        try {
            wm.addView(hotLeft, l);
            wm.addView(hotRight, r);
        } catch (Exception ignored) { }
    }

    private View hotspot(final boolean control) {
        View v = new View(this);
        final float need = U.dp(this, 16);
        v.setOnTouchListener(new View.OnTouchListener() {
            private float downY;
            private boolean fired;
            @Override
            public boolean onTouch(View view, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = e.getRawY();
                        fired = false;
                        return true;
                    case MotionEvent.ACTION_MOVE:
                        if (!fired && e.getRawY() - downY > need) {
                            fired = true;
                            if (control) showControl(); else showNotifs();
                        }
                        return true;
                    default:
                        return true;
                }
            }
        });
        return v;
    }

    // ---------------------------------------------------------------------------------
    // panels
    // ---------------------------------------------------------------------------------

    private boolean busy = false;

    private void showControl() { open(true); }

    private void showNotifs() { open(false); }

    /** Grab a blurred picture of what is on screen, then build and show the requested panel. */
    private void open(final boolean control) {
        if (busy) return;
        busy = true;
        hidePanel(false); // must happen before building, because hiding clears the live-refresh hook
        capture(new Runnable() {
            @Override public void run() {
                busy = false;
                Runnable close = new Runnable() { @Override public void run() { hidePanel(true); } };
                showPanel(control
                        ? ControlCenter.build(GlassAccessibilityService.this, close)
                        : NotificationCenter.build(GlassAccessibilityService.this, close));
            }
        });
    }

    private void capture(final Runnable done) {
        if (Build.VERSION.SDK_INT < 30) {
            Backdrop.set(null);
            done.run();
            return;
        }
        try {
            takeScreenshot(Display.DEFAULT_DISPLAY, getMainExecutor(), new TakeScreenshotCallback() {
                @Override
                public void onSuccess(final ScreenshotResult r) {
                    new Thread(new Runnable() {
                        @Override public void run() {
                            Bitmap out = null;
                            try {
                                Bitmap hw = Bitmap.wrapHardwareBuffer(r.getHardwareBuffer(), r.getColorSpace());
                                Bitmap sw = hw.copy(Bitmap.Config.ARGB_8888, false);
                                r.getHardwareBuffer().close();
                                out = Blur.blur(sw, 10, 6);
                            } catch (Throwable ignored) { }
                            final Bitmap result = out;
                            new Handler(Looper.getMainLooper()).post(new Runnable() {
                                @Override public void run() {
                                    Backdrop.set(result);
                                    done.run();
                                }
                            });
                        }
                    }).start();
                }

                @Override
                public void onFailure(int errorCode) {
                    Backdrop.set(null);
                    done.run();
                }
            });
        } catch (Throwable t) {
            Backdrop.set(null);
            done.run();
        }
    }

    private void showPanel(View content) {
        lastShow = SystemClock.uptimeMillis();

        final FrameLayout root = new FrameLayout(this);
        root.addView(new Backdrop.Layer(this), new FrameLayout.LayoutParams(-1, -1));

        GlassScroll sv = new GlassScroll(this);
        sv.addView(content, new FrameLayout.LayoutParams(-1, -2));
        FrameLayout.LayoutParams clp = new FrameLayout.LayoutParams(-1, -2);
        clp.gravity = Gravity.TOP;
        clp.topMargin = statusBarHeight() + U.dp(this, 4);
        root.addView(sv, clp);
        final View scroller = sv;

        root.setOnTouchListener(new View.OnTouchListener() {
            private float downY;
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                switch (e.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        downY = e.getRawY();
                        return true;
                    case MotionEvent.ACTION_UP: {
                        float dy = e.getRawY() - downY;
                        boolean swipeUp = dy < -U.dp(GlassAccessibilityService.this, 60);
                        boolean tapOutside = Math.abs(dy) < U.dp(GlassAccessibilityService.this, 10)
                                && e.getY() > scroller.getBottom();
                        if (swipeUp || tapOutside) hidePanel(true);
                        return true;
                    }
                    default:
                        return true;
                }
            }
        });

        int flags = WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE;
        // Android 12+: if no screenshot could be taken (secure screen), let the system blur the window instead.
        boolean systemBlur = Build.VERSION.SDK_INT >= 31 && !Backdrop.hasShot();
        if (systemBlur) flags |= WindowManager.LayoutParams.FLAG_BLUR_BEHIND;
        WindowManager.LayoutParams p = new WindowManager.LayoutParams(-1, -1, overlayType(), flags,
                PixelFormat.TRANSLUCENT);
        if (systemBlur) p.setBlurBehindRadius(80);
        try {
            wm.addView(root, p);
            panel = root;
            root.setAlpha(0f);
            sv.setTranslationY(-U.dp(this, 36));
            root.animate().alpha(1f).setDuration(180).start();
            sv.animate().translationY(0f).setDuration(220).start();
        } catch (Exception e) {
            panel = null;
        }
    }

    private void hidePanel(boolean animate) {
        final FrameLayout p = panel;
        if (p == null) return;
        panel = null;
        NotifService.onChange = null;
        if (animate) {
            p.animate().alpha(0f).setDuration(140).withEndAction(new Runnable() {
                @Override public void run() {
                    try { wm.removeView(p); } catch (Exception ignored) { }
                    if (panel == null) Backdrop.set(null); // don't keep a screenshot in memory
                }
            }).start();
        } else {
            try { wm.removeView(p); } catch (Exception ignored) { }
            Backdrop.set(null);
        }
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : U.dp(this, 24);
    }
}
