package app.liquidglass.os;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.drawable.Drawable;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Reads the installed apps and works out the default dock apps. */
public final class Apps {
    private Apps() {}

    public static final class Info {
        public String label;
        public String pkg;
        public ComponentName component;
        public Drawable icon;
    }

    public static List<Info> load(Context c) {
        PackageManager pm = c.getPackageManager();
        Intent main = new Intent(Intent.ACTION_MAIN, null);
        main.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> list = pm.queryIntentActivities(main, 0);
        List<Info> out = new ArrayList<>();
        for (ResolveInfo r : list) {
            Info i = new Info();
            i.label = String.valueOf(r.loadLabel(pm));
            i.pkg = r.activityInfo.packageName;
            i.component = new ComponentName(r.activityInfo.packageName, r.activityInfo.name);
            i.icon = r.loadIcon(pm);
            if (i.pkg.equals(c.getPackageName())) i.label = "Glass Settings";
            out.add(i);
        }
        final Collator col = Collator.getInstance();
        Collections.sort(out, new Comparator<Info>() {
            @Override public int compare(Info a, Info b) { return col.compare(a.label, b.label); }
        });
        return out;
    }

    /** Package names for phone, messages, browser and camera if present. */
    public static List<String> defaultDock(Context c) {
        PackageManager pm = c.getPackageManager();
        List<String> out = new ArrayList<>();
        add(out, pm, new Intent(Intent.ACTION_DIAL));
        add(out, pm, new Intent(Intent.ACTION_SENDTO, android.net.Uri.parse("smsto:")));
        add(out, pm, new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://example.com")));
        add(out, pm, new Intent("android.media.action.IMAGE_CAPTURE"));
        return out;
    }

    private static void add(List<String> out, PackageManager pm, Intent i) {
        ResolveInfo r = pm.resolveActivity(i, PackageManager.MATCH_DEFAULT_ONLY);
        if (r != null && r.activityInfo != null) {
            String p = r.activityInfo.packageName;
            if (!"android".equals(p) && !out.contains(p)) out.add(p);
        }
    }

    public static Info find(List<Info> all, String pkg) {
        for (Info i : all) if (i.pkg.equals(pkg)) return i;
        return null;
    }
}
