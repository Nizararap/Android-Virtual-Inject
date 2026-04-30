package com.reveny.virtualinject.store;

import android.content.Context;
import android.content.SharedPreferences;
import com.reveny.virtualinject.model.CloneConfig;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Persists clone configs (packageName → soFilePath) using SharedPreferences.
 * soFilePath stored as "" (empty string) when no library.
 */
public class CloneStore {
    private static final String PREFS_NAME = "vi_clones";
    private static CloneStore instance;
    private final SharedPreferences prefs;

    private CloneStore(Context ctx) {
        prefs = ctx.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static CloneStore get(Context ctx) {
        if (instance == null) instance = new CloneStore(ctx);
        return instance;
    }

    public void saveClone(CloneConfig config) {
        prefs.edit()
            .putString(config.getPackageName(), config.getSoFilePath() != null ? config.getSoFilePath() : "")
            .apply();
    }

    public void deleteClone(String packageName) {
        prefs.edit().remove(packageName).apply();
    }

    public boolean isCloned(String packageName) {
        return prefs.contains(packageName);
    }

    public CloneConfig getConfig(String packageName) {
        if (!isCloned(packageName)) return null;
        String so = prefs.getString(packageName, "");
        return new CloneConfig(packageName, so.isEmpty() ? null : so);
    }

    public List<CloneConfig> getAllClones() {
        List<CloneConfig> list = new ArrayList<>();
        Map<String, ?> all = prefs.getAll();
        for (Map.Entry<String, ?> e : all.entrySet()) {
            String so = (String) e.getValue();
            list.add(new CloneConfig(e.getKey(), so.isEmpty() ? null : so));
        }
        return list;
    }

    public void updateSo(String packageName, String soFilePath) {
        prefs.edit()
            .putString(packageName, soFilePath != null ? soFilePath : "")
            .apply();
    }
}
