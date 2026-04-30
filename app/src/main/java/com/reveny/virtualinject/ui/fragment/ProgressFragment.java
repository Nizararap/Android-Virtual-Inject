package com.reveny.virtualinject.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
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

public class ProgressFragment extends BaseFragment {
    private static final String ARG_PKG    = "pkg";
    private static final String ARG_SO     = "so";
    private static final String TAG        = "ProgressFragment";

    private String packageName, soFilePath;
    private AppItem appItem;

    private ImageView heroIcon;
    private LinearLayout installingState, doneState;
    private TextView appNameText, progressStep, progressPct, doneLibStatus;
    private View progressFill;
    private MaterialButton btnLaunchNow, btnSeeClones;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private int currentProgress = 0;
    private boolean isDone = false;

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
        heroIcon       = root.findViewById(R.id.hero_icon);
        installingState= root.findViewById(R.id.installing_state);
        doneState      = root.findViewById(R.id.done_state);
        appNameText    = root.findViewById(R.id.progress_app_name);
        progressStep   = root.findViewById(R.id.progress_step);
        progressPct    = root.findViewById(R.id.progress_pct);
        progressFill   = root.findViewById(R.id.progress_fill);
        doneLibStatus  = root.findViewById(R.id.done_lib_status);
        btnLaunchNow   = root.findViewById(R.id.btn_launch_now);
        btnSeeClones   = root.findViewById(R.id.btn_see_clones);

        if (appItem != null) heroIcon.setImageDrawable(appItem.getIcon());
        appNameText.setText(appItem != null ? appItem.getName() : packageName);

        btnLaunchNow.setOnClickListener(v -> launchAndGoHome());
        btnSeeClones.setOnClickListener(v -> goHome(true));

        startInstallFlow();
        return root;
    }

    private void startInstallFlow() {
        new Thread(() -> {
            try {
                updateStep(R.string.progress_step_env, 25);
                // Actual BlackBox install
                BlackBoxCore.get().installPackageAsUser(packageName, 0);
                updateStep(R.string.progress_step_pkg, 55);
                Thread.sleep(500);
                if (soFilePath != null) {
                    updateStep(R.string.progress_step_so, 80);
                    Thread.sleep(400);
                }
                updateStep(R.string.progress_step_verify, 95);
                Thread.sleep(300);

                boolean installed = BlackBoxCore.get().isInstalled(packageName, 0);
                if (!installed) {
                    handler.post(() -> {
                        progressStep.setText("❌ Gagal menginstall");
                        progressStep.setTextColor(requireContext().getColor(R.color.vi_red));
                    });
                    return;
                }

                // Persist config
                CloneStore.get(requireContext()).saveClone(new CloneConfig(packageName, soFilePath));

                setProgress(100);
                handler.post(this::showDone);

            } catch (Exception e) {
                Log.e(TAG, "Install failed", e);
                handler.post(() -> progressStep.setText("❌ " + e.getMessage()));
            }
        }).start();
    }

    private void updateStep(int stepRes, int targetPct) throws InterruptedException {
        handler.post(() -> progressStep.setText(getString(stepRes)));
        animateProgress(currentProgress, targetPct);
        Thread.sleep(600);
    }

    private void animateProgress(int from, int to) throws InterruptedException {
        for (int i = from; i <= to; i++) {
            final int p = i;
            handler.post(() -> setProgress(p));
            Thread.sleep(18);
        }
        currentProgress = to;
    }

    private void setProgress(int pct) {
        if (progressFill == null) return;
        ViewGroup parent = (ViewGroup) progressFill.getParent();
        int maxW = parent.getWidth();
        if (maxW == 0) {
            progressFill.post(() -> {
                int w = (int)(progressFill.getParent() != null ?
                    ((View) progressFill.getParent()).getWidth() * pct / 100f : 0);
                progressFill.getLayoutParams().width = Math.max(w, 0);
                progressFill.requestLayout();
            });
        } else {
            progressFill.getLayoutParams().width = (int)(maxW * pct / 100f);
            progressFill.requestLayout();
        }
        progressPct.setText(pct + "%");
    }

    private void showDone() {
        isDone = true;
        installingState.setVisibility(View.GONE);
        doneState.setVisibility(View.VISIBLE);
        if (soFilePath != null) {
            String name = soFilePath.substring(soFilePath.lastIndexOf('/') + 1);
            doneLibStatus.setText(getString(R.string.lib_active_fmt, name));
        } else {
            doneLibStatus.setText(R.string.no_lib_active_short);
        }
        if (appItem != null) btnLaunchNow.setText("🚀 Launch " + appItem.getName());
    }

    private void launchAndGoHome() {
        boolean isInstalled = BlackBoxCore.get().isInstalled(packageName, 0);
        if (isInstalled) BlackBoxCore.get().launchApk(packageName, 0);
        goHome(true);
    }

    private void goHome(boolean showClonedTab) {
        MainActivity act = (MainActivity) requireActivity();
        act.goHomeAndShowCloned(showClonedTab);
    }

    @Override public void onDestroyView() {
        super.onDestroyView();
        handler.removeCallbacksAndMessages(null);
    }
}
