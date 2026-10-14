package app.liquidglass.os;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

/** The home screen: wallpaper, widgets, app pages, search pill and the liquid glass dock. */
public class LauncherActivity extends Activity {

    private static final int DOCK_MAX = 4;

    private FrameLayout root;
    private LinearLayout content;
    private WallpaperView wallpaperView;
    private PagerView pager;
    private LinearLayout dots;
    private GlassView dock;
    private LinearLayout dockRow;
    private FrameLayout overlay;
    private List<Apps.Info> all = new ArrayList<>();

    /** Draws the sharp wallpaper, lined up with the blurred copy used by glass panels. */
    static class WallpaperView extends View {
        private final int[] loc = new int[2];
        WallpaperView(Context c) { super(c); }

        @Override
        protected void onDraw(Canvas canvas) {
            android.graphics.Bitmap b = Wallpaper.sharp(getContext());
            getLocationOnScreen(loc);
            if (b != null) canvas.drawBitmap(b, -loc[0], -loc[1], null);
            if (Prefs.theme(getContext()) == 1) canvas.drawColor(0x55000000);
        }
    }

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
        root = new FrameLayout(this);
        setContentView(root);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        Wallpaper.ensure(this);
        wallpaperView.invalidate();
        populate();
    }

    @Override
    protected void onNewIntent(Intent i) {
        super.onNewIntent(i);
        closeOverlay();
        if (pager != null) pager.smoothScrollTo(0, 0);
    }

    @Override
    public void onBackPressed() {
        if (overlay != null) closeOverlay();
        // A launcher never closes itself.
    }

    // ---------------------------------------------------------------------------------
    // layout
    // ---------------------------------------------------------------------------------

    private void build() {
        root.removeAllViews();

        wallpaperView = new WallpaperView(this);
        root.addView(wallpaperView, new FrameLayout.LayoutParams(-1, -1));

        content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        root.addView(content, new FrameLayout.LayoutParams(-1, -1));
        content.setPadding(0, statusBarHeight(), 0, 0);
        root.setOnApplyWindowInsetsListener((v, ins) -> {
            content.setPadding(0, ins.getSystemWindowInsetTop(), 0, ins.getSystemWindowInsetBottom());
            return ins;
        });

        pager = new PagerView(this);
        content.addView(pager, new LinearLayout.LayoutParams(-1, 0, 1f));

        dots = new LinearLayout(this);
        dots.setGravity(Gravity.CENTER);
        content.addView(dots, new LinearLayout.LayoutParams(-1, U.dp(this, 20)));
        pager.setListener((page, count) -> updateDots(page, count));

        // search pill
        GlassView pill = new GlassView(this, 22);
        TextView pt = new TextView(this);
        pt.setText("Search");
        pt.setTextColor(0xFFFFFFFF);
        pt.setTextSize(15);
        pt.setGravity(Gravity.CENTER);
        pill.addView(pt, new FrameLayout.LayoutParams(-1, -1));
        pill.setOnClickListener(v -> openSearch());
        LinearLayout.LayoutParams pillLp = new LinearLayout.LayoutParams(U.dp(this, 160), U.dp(this, 44));
        pillLp.gravity = Gravity.CENTER_HORIZONTAL;
        pillLp.bottomMargin = U.dp(this, 10);
        content.addView(pill, pillLp);

        // dock
        dock = new GlassView(this, 34);
        dockRow = new LinearLayout(this);
        dockRow.setOrientation(LinearLayout.HORIZONTAL);
        dockRow.setGravity(Gravity.CENTER_VERTICAL);
        dock.addView(dockRow, new FrameLayout.LayoutParams(-1, -1));
        LinearLayout.LayoutParams dockLp = new LinearLayout.LayoutParams(-1, U.dp(this, 92));
        dockLp.setMargins(U.dp(this, 14), 0, U.dp(this, 14), U.dp(this, 14));
        content.addView(dock, dockLp);
    }

    private int statusBarHeight() {
        int id = getResources().getIdentifier("status_bar_height", "dimen", "android");
        return id > 0 ? getResources().getDimensionPixelSize(id) : U.dp(this, 24);
    }

    private void populate() {
        all = Apps.load(this);

        // dock apps
        if (!Prefs.pinnedChosen(this)) Prefs.setPinned(this, Apps.defaultDock(this));
        List<String> pinned = Prefs.pinned(this);
        List<Apps.Info> dockApps = new ArrayList<>();
        for (String p : pinned) {
            Apps.Info i = Apps.find(all, p);
            if (i != null && dockApps.size() < DOCK_MAX) dockApps.add(i);
        }

        dockRow.removeAllViews();
        if (dockApps.isEmpty()) {
            TextView hint = new TextView(this);
            hint.setText("Long-press an app to pin it here");
            hint.setTextColor(0xCCFFFFFF);
            hint.setGravity(Gravity.CENTER);
            dockRow.addView(hint, new LinearLayout.LayoutParams(-1, -1));
        }
        for (Apps.Info i : dockApps) {
            View v = makeIcon(i, false);
            dockRow.addView(v, new LinearLayout.LayoutParams(0, -1, 1f));
        }

        // grid apps (everything not in the dock)
        List<Apps.Info> grid = new ArrayList<>();
        for (Apps.Info i : all) if (!dockApps.contains(i)) grid.add(i);

        pager.clearPages();
        int idx = 0;
        int pageNo = 0;
        while (idx < grid.size() || pageNo == 0) {
            boolean first = pageNo == 0;
            int rows = first ? 4 : 6;
            int per = 4 * rows;
            LinearLayout page = new LinearLayout(this);
            page.setOrientation(LinearLayout.VERTICAL);
            page.setPadding(U.dp(this, 10), U.dp(this, 8), U.dp(this, 10), 0);
            page.setLongClickable(true);
            page.setOnLongClickListener(v -> {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            });
            if (first) page.addView(widgetRow(), new LinearLayout.LayoutParams(-1, U.dp(this, 140)));

            IconGrid g = new IconGrid(this, 4, rows);
            for (int k = 0; k < per && idx < grid.size(); k++, idx++) {
                g.addView(makeIcon(grid.get(idx), true));
            }
            LinearLayout.LayoutParams glp = new LinearLayout.LayoutParams(-1, 0, 1f);
            glp.topMargin = first ? U.dp(this, 10) : 0;
            page.addView(g, glp);
            pager.addPage(page);
            pageNo++;
        }
        updateDots(0, pager.pageCount());
    }

    private View widgetRow() {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        int m = U.dp(this, 6);
        row.setPadding(U.dp(this, 6), 0, U.dp(this, 6), 0);
        LinearLayout.LayoutParams a = new LinearLayout.LayoutParams(0, -1, 2f);
        LinearLayout.LayoutParams b = new LinearLayout.LayoutParams(0, -1, 1f);
        LinearLayout.LayoutParams c = new LinearLayout.LayoutParams(0, -1, 1f);
        a.setMargins(0, 0, m, 0);
        b.setMargins(m, 0, m, 0);
        c.setMargins(m, 0, 0, 0);
        row.addView(new Widgets.ClockCard(this), a);
        row.addView(new Widgets.CalendarCard(this), b);
        row.addView(new Widgets.BatteryCard(this), c);
        return row;
    }

    private void updateDots(int page, int count) {
        dots.removeAllViews();
        for (int i = 0; i < count; i++) {
            View d = new View(this);
            GradientDrawable g = new GradientDrawable();
            g.setShape(GradientDrawable.OVAL);
            g.setColor(i == page ? 0xFFFFFFFF : 0x66FFFFFF);
            d.setBackground(g);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(U.dp(this, 7), U.dp(this, 7));
            lp.setMargins(U.dp(this, 4), 0, U.dp(this, 4), 0);
            dots.addView(d, lp);
        }
    }

    // ---------------------------------------------------------------------------------
    // icons, launching, menu
    // ---------------------------------------------------------------------------------

    private View makeIcon(final Apps.Info app, boolean withLabel) {
        LinearLayout cell = new LinearLayout(this);
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);
        cell.addView(new IconTile(this, app.icon), new LinearLayout.LayoutParams(U.dp(this, 60), U.dp(this, 60)));
        if (withLabel) {
            TextView t = new TextView(this);
            t.setText(app.label);
            t.setTextColor(0xFFFFFFFF);
            t.setTextSize(11);
            t.setMaxLines(1);
            t.setEllipsize(TextUtils.TruncateAt.END);
            t.setGravity(Gravity.CENTER);
            t.setShadowLayer(4, 0, 1, 0x99000000);
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
            lp.topMargin = U.dp(this, 5);
            lp.leftMargin = U.dp(this, 2);
            lp.rightMargin = U.dp(this, 2);
            cell.addView(t, lp);
        }
        cell.setOnClickListener(v -> launch(app));
        cell.setOnLongClickListener(v -> { showMenu(v, app); return true; });
        return cell;
    }

    private void launch(Apps.Info app) {
        if (app.pkg.equals(getPackageName())) {
            startActivity(new Intent(this, SettingsActivity.class));
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.addCategory(Intent.CATEGORY_LAUNCHER);
            i.setComponent(app.component);
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(this, "Can't open " + app.label, Toast.LENGTH_SHORT).show();
        }
    }

    private void showMenu(View anchor, final Apps.Info app) {
        closeOverlay();
        overlay = new FrameLayout(this);
        overlay.setBackgroundColor(0x33000000);
        overlay.setOnClickListener(v -> closeOverlay());
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        final boolean isPinned = Prefs.pinned(this).contains(app.pkg);
        GlassView card = new GlassView(this, 22);
        card.setForceDark(true);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        card.addView(col, new FrameLayout.LayoutParams(-1, -2));

        col.addView(menuRow(isPinned ? "Remove from Dock" : "Pin to Dock", v -> {
            List<String> p = Prefs.pinned(this);
            if (isPinned) p.remove(app.pkg);
            else if (p.size() >= DOCK_MAX) { Toast.makeText(this, "Dock is full", Toast.LENGTH_SHORT).show(); closeOverlay(); return; }
            else p.add(app.pkg);
            Prefs.setPinned(this, p);
            closeOverlay();
            populate();
        }));
        col.addView(menuRow("App info", v -> {
            closeOverlay();
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:" + app.pkg)));
        }));
        col.addView(menuRow("Uninstall", v -> {
            closeOverlay();
            startActivity(new Intent(Intent.ACTION_DELETE, Uri.parse("package:" + app.pkg)));
        }));

        int[] loc = new int[2];
        anchor.getLocationOnScreen(loc);
        int cardW = U.dp(this, 200);
        int cardH = U.dp(this, 3 * 46);
        int screenW = U.screen(this).x;
        int screenH = U.screen(this).y;
        int left = Math.max(U.dp(this, 12), Math.min(screenW - cardW - U.dp(this, 12), loc[0] + anchor.getWidth() / 2 - cardW / 2));
        int top = loc[1] + anchor.getHeight() + U.dp(this, 6);
        if (top + cardH > screenH - U.dp(this, 120)) top = loc[1] - cardH - U.dp(this, 6);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(cardW, cardH);
        lp.gravity = Gravity.TOP | Gravity.START;
        lp.leftMargin = left;
        lp.topMargin = top;
        overlay.addView(card, lp);
    }

    private TextView menuRow(String text, View.OnClickListener l) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(0xFFFFFFFF);
        t.setTextSize(16);
        t.setGravity(Gravity.CENTER_VERTICAL);
        t.setPadding(U.dp(this, 18), 0, U.dp(this, 18), 0);
        t.setLayoutParams(new LinearLayout.LayoutParams(-1, U.dp(this, 46)));
        t.setOnClickListener(l);
        return t;
    }

    private void closeOverlay() {
        if (overlay != null) {
            View focus = overlay.findFocus();
            if (focus != null) {
                InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(focus.getWindowToken(), 0);
            }
            root.removeView(overlay);
            overlay = null;
        }
    }

    // ---------------------------------------------------------------------------------
    // search
    // ---------------------------------------------------------------------------------

    private void openSearch() {
        closeOverlay();
        overlay = new FrameLayout(this);
        root.addView(overlay, new FrameLayout.LayoutParams(-1, -1));

        GlassView bg = new GlassView(this, 0);
        bg.setForceDark(true);
        overlay.addView(bg, new FrameLayout.LayoutParams(-1, -1));

        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        col.setPadding(U.dp(this, 16), statusBarHeight() + U.dp(this, 12), U.dp(this, 16), U.dp(this, 16));
        overlay.addView(col, new FrameLayout.LayoutParams(-1, -1));

        GlassView field = new GlassView(this, 22);
        final EditText et = new EditText(this);
        et.setHint("Search apps");
        et.setHintTextColor(0x99FFFFFF);
        et.setTextColor(0xFFFFFFFF);
        et.setTextSize(17);
        et.setSingleLine(true);
        et.setBackground(null);
        et.setPadding(U.dp(this, 18), 0, U.dp(this, 18), 0);
        field.addView(et, new FrameLayout.LayoutParams(-1, -1));
        col.addView(field, new LinearLayout.LayoutParams(-1, U.dp(this, 48)));

        ScrollView sv = new ScrollView(this);
        final LinearLayout results = new LinearLayout(this);
        results.setOrientation(LinearLayout.VERTICAL);
        sv.addView(results, new ViewGroup.LayoutParams(-1, -2));
        LinearLayout.LayoutParams slp = new LinearLayout.LayoutParams(-1, 0, 1f);
        slp.topMargin = U.dp(this, 12);
        col.addView(sv, slp);

        fillResults(results, "");
        et.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void onTextChanged(CharSequence s, int a, int b, int c) { }
            @Override public void afterTextChanged(Editable e) { fillResults(results, e.toString()); }
        });

        et.requestFocus();
        et.postDelayed(() -> {
            InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
        }, 150);
    }

    private void fillResults(LinearLayout results, String q) {
        results.removeAllViews();
        String s = q.trim().toLowerCase();
        for (final Apps.Info i : all) {
            if (s.length() > 0 && !i.label.toLowerCase().contains(s)) continue;
            LinearLayout row = new LinearLayout(this);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(0, U.dp(this, 6), 0, U.dp(this, 6));
            row.addView(new IconTile(this, i.icon), new LinearLayout.LayoutParams(U.dp(this, 46), U.dp(this, 46)));
            TextView t = new TextView(this);
            t.setText(i.label);
            t.setTextColor(0xFFFFFFFF);
            t.setTextSize(17);
            t.setPadding(U.dp(this, 14), 0, 0, 0);
            row.addView(t, new LinearLayout.LayoutParams(-1, -2));
            row.setOnClickListener(v -> { closeOverlay(); launch(i); });
            results.addView(row, new LinearLayout.LayoutParams(-1, -2));
        }
    }
}
