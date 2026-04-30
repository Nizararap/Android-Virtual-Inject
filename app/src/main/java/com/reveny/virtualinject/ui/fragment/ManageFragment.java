package com.reveny.virtualinject.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

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

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class ManageFragment extends BaseFragment {
    private static final String ARG_PKG = "pkg";
    private static final int REQ_PICK_SO = 201;
    private static final String TAG = "ManageFragment";

    private String packageName;
    private AppItem appItem;
    private CloneConfig config;

    // Views
    private ImageView heroIcon;
    private TextView heroName, heroPkg, soStatusText;
    private MaterialButton btnChangeSo, btnResetSo, btnLaunch, btnDeleteClone, btnConfirmReset;
    private LinearLayout soActionRow, swapPanel, resetPanel;
    private LinearLayout btnPickNewSo;
    private TextView newSoLabel, btnCancelSwap, btnCancelReset;

    public static ManageFragment newInstance(String pkg) {
        ManageFragment f = new ManageFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PKG, pkg);
        f.setArguments(b);
        return f;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = requireArguments().getString(ARG_PKG);
        appItem = Utility.getAppItem(requireContext(), packageName);
        config  = CloneStore.get(requireContext()).getConfig(packageName);
        if (config == null) config = new CloneConfig(packageName, null);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_manage, container, false);
        bindViews(root);
        bindData();
        setupListeners();
        return root;
    }

    private void bindViews(View root) {
        root.findViewById(R.id.btn_back).setOnClickListener(v -> requireActivity().onBackPressed());
        heroIcon       = root.findViewById(R.id.hero_icon);
        heroName       = root.findViewById(R.id.hero_name);
        heroPkg        = root.findViewById(R.id.hero_pkg);
        soStatusText   = root.findViewById(R.id.so_status_text);
        btnChangeSo    = root.findViewById(R.id.btn_change_so);
        btnResetSo     = root.findViewById(R.id.btn_reset_so);
        btnLaunch      = root.findViewById(R.id.btn_launch);
        btnDeleteClone = root.findViewById(R.id.btn_delete_clone);
        soActionRow    = root.findViewById(R.id.so_action_row);
        swapPanel      = root.findViewById(R.id.swap_panel);
        resetPanel     = root.findViewById(R.id.reset_panel);
        btnPickNewSo   = root.findViewById(R.id.btn_pick_new_so);
        newSoLabel     = root.findViewById(R.id.new_so_label);
        btnCancelSwap  = root.findViewById(R.id.btn_cancel_swap);
        btnCancelReset = root.findViewById(R.id.btn_cancel_reset);
        btnConfirmReset= root.findViewById(R.id.btn_confirm_reset);
    }

    private void bindData() {
        if (appItem != null) {
            heroIcon.setImageDrawable(appItem.getIcon());
            heroName.setText(appItem.getName());
            btnLaunch.setText(getString(R.string.btn_launch, appItem.getName()));
        } else {
            heroName.setText(packageName);
            btnLaunch.setText(getString(R.string.btn_launch, packageName));
        }
        heroPkg.setText(packageName);
        refreshSoStatus();
    }

    private void refreshSoStatus() {
        if (config.hasSo()) {
            String name = config.getSoFilePath();
            int slash = name.lastIndexOf('/');
            if (slash >= 0) name = name.substring(slash + 1);
            soStatusText.setText("● " + name);
            soStatusText.setTextColor(requireContext().getColor(R.color.vi_green));
            btnChangeSo.setText(R.string.btn_change_so);
            btnResetSo.setVisibility(View.VISIBLE);
        } else {
            soStatusText.setText(R.string.no_lib_active);
            soStatusText.setTextColor(requireContext().getColor(R.color.vi_text_hint));
            btnChangeSo.setText(R.string.btn_install_so);
            btnResetSo.setVisibility(View.GONE);
        }
    }

    private void setupListeners() {
        btnChangeSo.setOnClickListener(v -> {
            swapPanel.setVisibility(View.VISIBLE);
            resetPanel.setVisibility(View.GONE);
            soActionRow.setVisibility(View.GONE);
        });
        btnResetSo.setOnClickListener(v -> {
            resetPanel.setVisibility(View.VISIBLE);
            swapPanel.setVisibility(View.GONE);
            soActionRow.setVisibility(View.GONE);
        });
        btnCancelSwap.setOnClickListener(v -> {
            swapPanel.setVisibility(View.GONE);
            soActionRow.setVisibility(View.VISIBLE);
        });
        btnCancelReset.setOnClickListener(v -> {
            resetPanel.setVisibility(View.GONE);
            soActionRow.setVisibility(View.VISIBLE);
        });
        btnPickNewSo.setOnClickListener(v -> openFilePicker());
        btnConfirmReset.setOnClickListener(v -> doResetSo());
        btnLaunch.setOnClickListener(v -> doLaunch());
        btnDeleteClone.setOnClickListener(v -> doDelete());
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream"});
        startActivityForResult(Intent.createChooser(intent, "Pilih file .so"), REQ_PICK_SO);
    }

    private void doResetSo() {
        config.setSoFilePath(null);
        CloneStore.get(requireContext()).updateSo(packageName, null);
        resetPanel.setVisibility(View.GONE);
        soActionRow.setVisibility(View.VISIBLE);
        refreshSoStatus();
        Toast.makeText(requireContext(), "Library direset", Toast.LENGTH_SHORT).show();
    }

    private void doLaunch() {
        boolean isInstalled = BlackBoxCore.get().isInstalled(packageName, 0);
        if (!isInstalled) {
            Toast.makeText(requireContext(), R.string.err_not_installed, Toast.LENGTH_SHORT).show();
            return;
        }
        BlackBoxCore.get().launchApk(packageName, 0);
    }

    private void doDelete() {
        BlackBoxCore.get().uninstallPackageAsUser(packageName, 0);
        CloneStore.get(requireContext()).deleteClone(packageName);
        Toast.makeText(requireContext(), "Clone dihapus", Toast.LENGTH_SHORT).show();
        requireActivity().onBackPressed();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_SO || resultCode != Activity.RESULT_OK) return;
        if (data == null || data.getData() == null) return;
        Uri uri = data.getData();
        String uriPath = uri.getPath();
        if (uriPath == null || !uriPath.endsWith(".so")) {
            Toast.makeText(requireContext(), R.string.err_invalid_file, Toast.LENGTH_SHORT).show();
            return;
        }
        File dest = new File(requireContext().getCacheDir(), "libinject_" + packageName.replace(".", "_") + ".so");
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[4096]; int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            String newPath = dest.getAbsolutePath();
            config.setSoFilePath(newPath);
            CloneStore.get(requireContext()).updateSo(packageName, newPath);
            swapPanel.setVisibility(View.GONE);
            soActionRow.setVisibility(View.VISIBLE);
            refreshSoStatus();
            String name = uriPath.substring(uriPath.lastIndexOf('/') + 1);
            Toast.makeText(requireContext(), "Library diperbarui: " + name, Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Log.e(TAG, "Copy failed", e);
            Toast.makeText(requireContext(), R.string.err_copy_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
