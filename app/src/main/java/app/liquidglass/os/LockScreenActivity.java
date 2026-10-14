package app.liquidglass.os;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.hardware.camera2.CameraManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * The liquid glass lock screen. It is shown above the stock keyguard (showWhenLocked), with the big glass
 * clock, notification cards, flashlight and camera buttons. Swipe up to unlock.
 */
public class LockScreenActivity extends Activity {

    static LockScreenActivity instance;

    private FrameLayout root;
    private FrameLayout layer;
    private LockClockView clock;
    private LinearLayout notifs;
    private float downY;
    private boolean torchOn = false;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        instance = this;
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        }
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);

        root = new FrameLayout(this);
        setContentView(root);
        root.addView(new LauncherActivity.WallpaperView(this), new FrameLayout.LayoutParams(-1, -1));

        layer = new FrameLayout(this);
        root.addView(layer, new FrameLayout.LayoutParams(-1, -1));

        clock = new LockClockView(this);
        layer.addView(clock, new FrameLayout.LayoutParams(-1, -1));

        notifs = new LinearLayout(this);
        notifs.setOrientation(LinearLayout.VERTICAL);
        FrameLayout.LayoutParams nlp = new FrameLayout.LayoutParams(-1, -2);
        nlp.gravity = Gravity.TOP;
        nlp.leftMargin = U.dp(this, 16);
        nlp.rightMargin = U.dp(this, 16);
        nlp.topMargin = U.dp(this, 400);
        layer.addView(notifs, nlp);

        // flashlight (bottom left) and camera (bottom right)
        layer.addView(circleButton("Torch", new View.OnClickListener() {
            @Override public void onClick(View v) { toggleTorch(); }
        }, Gravity.BOTTOM | Gravity.START));
        layer.addView(circleButton("Cam", new View.OnClickListener() {
            @Override public void onClick(View v) { openCamera(); }
        }, Gravity.BOTTOM | Gravity.END));

        // hint and home bar
        TextView hint = new TextView(this);
        hint.setText("Swipe up to unlock");
        hint.setTextColor(0xCCFFFFFF);
        hint.setTextSize(13);
        hint.setGravity(Gravity.CENTER);
        FrameLayout.LayoutParams hlp = new FrameLayout.LayoutParams(-1, -2);
        hlp.gravity = Gravity.BOTTOM;
        hlp.bottomMargin = U.dp(this, 34);
        layer.addView(hint, hlp);

        View bar = new View(this);
        bar.setBackground(rounded(0xE6FFFFFF, U.dp(this, 3)));
        FrameLayout.LayoutParams blp = new FrameLayout.LayoutParams(U.dp(this, 134), U.dp(this, 5));
        blp.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        blp.bottomMargin = U.dp(this, 14);
        layer.addView(bar, blp);

        root.setOnTouchListener(new View.OnTouchListener() {
            @Override public boolean onTouch(View v, MotionEvent e) { return swipe(e); }
        });

        // place notification cards underneath the digits once we know where they are
        layer.post(new Runnable() {
            @Override public void run() {
                FrameLayout.LayoutParams lp = (FrameLayout.LayoutParams) notifs.getLayoutParams();
                lp.topMargin = clock.digitsBottom() + U.dp(LockScreenActivity.this, 28);
                notifs.setLayoutParams(lp);
            }
        });
        refreshNotifs();
    }

    private android.graphics.drawable.GradientDrawable rounded(int color, int radius) {
        android.graphics.drawable.GradientDrawable g = new android.graphics.drawable.GradientDrawable();
        g.setColor(color);
        g.setCornerRadius(radius);
        return g;
    }

    private View circleButton(String label, View.OnClickListener l, int gravity) {
        GlassView g = new GlassView(this, 28);
        g.setForceDark(true);
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(12);
        t.setGravity(Gravity.CENTER);
        g.addView(t, new FrameLayout.LayoutParams(-1, -1));
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(U.dp(this, 56), U.dp(this, 56));
        lp.gravity = gravity;
        lp.leftMargin = U.dp(this, 40);
        lp.rightMargin = U.dp(this, 40);
        lp.bottomMargin = U.dp(this, 64);
        g.setLayoutParams(lp);
        g.setOnClickListener(l);
        return g;
    }

    private void refreshNotifs() {
        NotifCards.fill(this, notifs, 3, false, true, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        clock.refresh();
        NotifService.onChange = new Runnable() {
            @Override public void run() { refreshNotifs(); }
        };
        refreshNotifs();
    }

    @Override
    protected void onPause() {
        super.onPause();
        NotifService.onChange = null;
    }

    @Override
    protected void onDestroy() {
        if (instance == this) instance = null;
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        // The lock screen cannot be backed out of.
    }

    // ---------------------------------------------------------------------------------

    private boolean swipe(MotionEvent e) {
        switch (e.getActionMasked()) {
            case MotionEvent.ACTION_DOWN:
                downY = e.getRawY();
                return true;
            case MotionEvent.ACTION_MOVE: {
                float dy = Math.max(0, downY - e.getRawY());
                layer.setTranslationY(-dy * 0.9f);
                layer.setAlpha(1f - Math.min(0.6f, dy / root.getHeight()));
                return true;
            }
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL: {
                float dy = downY - e.getRawY();
                if (dy > root.getHeight() * 0.2f) {
                    unlock();
                } else {
                    layer.animate().translationY(0).alpha(1f).setDuration(200).start();
                }
                return true;
            }
            default:
                return false;
        }
    }

    private void unlock() {
        layer.animate().translationY(-root.getHeight() * 0.25f).alpha(0f).setDuration(160).start();
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override public void onDismissSucceeded() { finish(); }
                @Override public void onDismissCancelled() { resetLayer(); }
                @Override public void onDismissError() { finish(); }
            });
        } else {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            layer.postDelayed(new Runnable() {
                @Override public void run() { finish(); }
            }, 250);
        }
    }

    private void resetLayer() {
        layer.animate().translationY(0).alpha(1f).setDuration(200).start();
    }

    private void toggleTorch() {
        try {
            CameraManager cm = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            String id = cm.getCameraIdList()[0];
            torchOn = !torchOn;
            cm.setTorchMode(id, torchOn);
        } catch (Exception e) {
            Toast.makeText(this, "Flashlight not available", Toast.LENGTH_SHORT).show();
        }
    }

    private void openCamera() {
        try {
            Intent i = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA_SECURE);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Camera not available", Toast.LENGTH_SHORT).show();
        }
    }
}
