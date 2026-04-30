package com.reveny.virtualinject.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.button.MaterialButton;
import com.reveny.virtualinject.R;
import com.reveny.virtualinject.model.AppItem;
import com.reveny.virtualinject.model.CloneConfig;
import com.reveny.virtualinject.store.CloneStore;
import com.reveny.virtualinject.ui.activity.MainActivity;
import com.reveny.virtualinject.util.Utility;
import com.vcore.BlackBoxCore;
import com.vcore.entity.pm.InstallResult;

public class ProgressFragment extends BaseFragment {
    private static final String ARG_PKG = "pkg";
    private static final String ARG_SO  = "so";

    private String packageName, soFilePath;
    private AppItem appItem;

    private ImageView heroIcon;
    private LinearLayout installingState, doneState;
    private TextView appNameText, progressStep, progressPct, doneLibStatus;
    private View progressFill;
    private MaterialButton btnLaunchNow, btnSeeClones;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private boolean destroyed = false;
    private int currentPct = 0;

    public static ProgressFragment newInstance(String pkg, String so) {
        ProgressFragment f = new ProgressFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PKG, pkg);
        b.putString(ARG_SO, so);
        f.setArguments(b);
        return f;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = requireArguments().getString(ARG_PKG);
        soFilePath  = requireArguments().getString(ARG_SO);
        appItem     = Utility.getAppItem(requireContext(), packageName);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_progress, container, false);
        heroIcon        = root.findViewById(R.id.hero_icon);
        installingState = root.findViewById(R.id.installing_state);
        doneState       = root.findViewById(R.id.done_state);
        appNameText     = root.findViewById(R.id.progress_app_name);
        progressStep    = root.findViewById(R.id.progress_step);
        progressPct     = root.findViewById(R.id.progress_pct);
        progressFill    = root.findViewById(R.id.progress_fill);
        doneLibStatus   = root.findViewById(R.id.done_lib_status);
        btnLaunchNow    = root.findViewById(R.id.btn_launch_now);
        btnSeeClones    = root.findViewById(R.id.btn_see_clones);

        if (appItem != null) heroIcon.setImageDrawable(appItem.getIcon());
        appNameText.setText(appItem != null ? appItem.getName() : packageName);
        btnLaunchNow.setOnClickListener(v -> launchAndGoHome());
        btnSeeClones.setOnClickListener(v -> goHome());

        startInstallFlow();
        return root;
    }

    // ─────────────────────────────────────────────────────────────────────────
    private void startInstallFlow() {
        new Thread(() -> {
            try {
                setStep(R.string.progress_step_env);
                animateTo(20); sleep(300);

                // Install via BlackBox — runs on background thread, that's fine
                InstallResult result = BlackBoxCore.get().installPackageAsUser(packageName, 0);

                if (result == null || !result.success) {
                    String msg = result != null ? result.msg : "Unknown error";
                    postError("❌ Install gagal: " + msg);
                    return;
                }

                setStep(R.string.progress_step_pkg);
                animateTo(55); sleep(400);

                if (soFilePath != null) {
                    setStep(R.string.progress_step_so);
                    animateTo(78); sleep(350);
                }

                setStep(R.string.progress_step_verify);
                animateTo(92); sleep(300);

                // Save config to SharedPrefs — safe on background thread
                CloneStore.get(requireContext()).saveClone(
                        new CloneConfig(packageName, soFilePath));

                // ✅ ALL view updates go through handler.post — no more threading violations
                handler.post(() -> {
                    if (destroyed) return;
                    animateToOnMain(100, () -> showDone());
                });

            } catch (Exception e) {
                postError("❌ " + e.getMessage());
            }
        }).start();
    }

    /** Post step label to main thread */
    private void setStep(int resId) {
        final String label = getString(resId);
        handler.post(() -> { if (!destroyed) progressStep.setText(label); });
    }

    /** Animate progress from background thread — each tick posted to main thread */
    private void animateTo(int target) {
        while (currentPct < target) {
            currentPct = Math.min(currentPct + 2, target);
            final int p = currentPct;
            handler.post(() -> { if (!destroyed) applyProgress(p); });
            sleep(18);
        }
    }

    /** Animate progress on main thread (called via handler.post) with callback when done */
    private void animateToOnMain(int target, Runnable onDone) {
        if (currentPct >= target) { if (onDone != null) onDone.run(); return; }
        currentPct = Math.min(currentPct + 3, target);
        applyProgress(currentPct);
        if (currentPct < target) {
            handler.postDelayed(() -> animateToOnMain(target, onDone), 16);
        } else {
            if (onDone != null) onDone.run();
        }
    }

    /** ✅ Only called from main thread */
    private void applyProgress(int pct) {
        if (progressFill == null || progressPct == null) return;
        ViewGroup parent = (ViewGroup) progressFill.getParent();
        if (parent == null) return;
        int maxW = parent.getWidth();
        if (maxW > 0) {
            progressFill.getLayoutParams().width = (int) (maxW * pct / 100f);
            progressFill.requestLayout();
        }
        progressPct.setText(pct + "%");
    }

    private void postError(String msg) {
        handler.post(() -> {
            if (destroyed) return;
            progressStep.setText(msg);
            progressStep.setTextColor(requireContext().getColor(R.color.vi_red));
        });
    }

    // ── Done ─────────────────────────────────────────────────────────────────
    private void showDone() {
        if (destroyed) return;
        installingState.setVisibility(View.GONE);
        doneState.setVisibility(View.VISIBLE);
        if (soFilePath != null) {
            String name = soFilePath.substring(soFilePath.lastIndexOf('/') + 1);
            doneLibStatus.setText(getString(R.string.lib_active_fmt, name));
        } else {
            doneLibStatus.setText(R.string.no_lib_active_short);
        }
        if (appItem != null) {
            btnLaunchNow.setText("🚀 Launch " + appItem.getName());
        }
    }

    // ── Navigation ────────────────────────────────────────────────────────────
    private void launchAndGoHome() {
        try {
            if (BlackBoxCore.get().isInstalled(packageName, 0)) {
                BlackBoxCore.get().launchApk(packageName, 0);
            }
        } catch (Exception ignored) {}
        goHome();
    }

    private void goHome() {
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).goHomeAndShowCloned(true);
        }
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    @Override public void onDestroyView() {
        super.onDestroyView();
        destroyed = true;
        handler.removeCallbacksAndMessages(null);
    }

    private static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}
