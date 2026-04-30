package com.reveny.virtualinject.util;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import com.reveny.virtualinject.model.AppItem;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Utility {

    /** Returns all user-installed apps as AppItem (icon + label + pkg) */
    public static List<AppItem> getInstalledAppItems(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<AppItem> result = new ArrayList<>();
        for (ApplicationInfo info : apps) {
            // skip system apps
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            String label = pm.getApplicationLabel(info).toString();
            Drawable icon = pm.getApplicationIcon(info);
            result.add(new AppItem(label, info.packageName, icon));
        }
        // Sort by name
        Collections.sort(result, (a, b) -> a.getName().compareToIgnoreCase(b.getName()));
        return result;
    }

    /** Convenience: return single AppItem or null if not found */
    public static AppItem getAppItem(Context ctx, String packageName) {
        PackageManager pm = ctx.getPackageManager();
        try {
            ApplicationInfo info = pm.getApplicationInfo(packageName, 0);
            String label = pm.getApplicationLabel(info).toString();
            Drawable icon = pm.getApplicationIcon(info);
            return new AppItem(label, packageName, icon);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    /** Legacy: returns list of package names (kept for backward compat) */
    public static List<String> getInstalledApps(Context ctx) {
        PackageManager pm = ctx.getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<String> result = new ArrayList<>();
        for (ApplicationInfo info : apps) {
            if ((info.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            result.add(info.packageName);
        }
        Collections.sort(result);
        return result;
    }
}
