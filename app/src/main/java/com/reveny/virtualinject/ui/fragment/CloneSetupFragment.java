package com.reveny.virtualinject.ui.fragment;

import android.app.Activity;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
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

public class CloneSetupFragment extends BaseFragment {
    private static final String ARG_PKG = "pkg";
    private static final int REQ_PICK_SO = 200;
    private static final String TAG = "CloneSetup";

    private String packageName;
    private AppItem appItem;

    // UI
    private ImageView heroIcon;
    private TextView heroName, heroPkg, filePickerLabel;
    private LinearLayout optionNoLib, optionWithLib, filePickerSection, fileChip, btnPickFile;
    private TextView fileChipName, fileChipRemove;
    private View radioNoLib, radioWithLib;
    private MaterialButton btnClone;

    private boolean withLib = false;
    private String soFilePath = null;

    public static CloneSetupFragment newInstance(String pkg) {
        CloneSetupFragment f = new CloneSetupFragment();
        Bundle b = new Bundle();
        b.putString(ARG_PKG, pkg);
        f.setArguments(b);
        return f;
    }

    @Override public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = requireArguments().getString(ARG_PKG);
        appItem = Utility.getAppItem(requireContext(), packageName);
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_clone_setup, container, false);
        bindViews(root);
        bindData();
        setupListeners();
        return root;
    }

    private void bindViews(View root) {
        root.findViewById(R.id.btn_back).setOnClickListener(v -> requireActivity().onBackPressed());
        heroIcon         = root.findViewById(R.id.hero_icon);
        heroName         = root.findViewById(R.id.hero_name);
        heroPkg          = root.findViewById(R.id.hero_pkg);
        optionNoLib      = root.findViewById(R.id.option_no_lib);
        optionWithLib    = root.findViewById(R.id.option_with_lib);
        radioNoLib       = root.findViewById(R.id.radio_no_lib);
        radioWithLib     = root.findViewById(R.id.radio_with_lib);
        filePickerSection = root.findViewById(R.id.file_picker_section);
        btnPickFile      = root.findViewById(R.id.btn_pick_file);
        filePickerLabel  = root.findViewById(R.id.file_picker_label);
        fileChip         = root.findViewById(R.id.file_chip);
        fileChipName     = root.findViewById(R.id.file_chip_name);
        fileChipRemove   = root.findViewById(R.id.file_chip_remove);
        btnClone         = root.findViewById(R.id.btn_clone);
    }

    private void bindData() {
        if (appItem != null) {
            heroIcon.setImageDrawable(appItem.getIcon());
            heroName.setText(appItem.getName());
        } else {
            heroName.setText(packageName);
        }
        heroPkg.setText(packageName);
    }

    private void setupListeners() {
        optionNoLib.setOnClickListener(v -> selectOption(false));
        optionWithLib.setOnClickListener(v -> selectOption(true));
        btnPickFile.setOnClickListener(v -> openFilePicker());
        fileChipRemove.setOnClickListener(v -> clearSoFile());
        btnClone.setOnClickListener(v -> startClone());
    }

    private void selectOption(boolean withLibrary) {
        withLib = withLibrary;
        // radio states
        radioNoLib.setBackgroundResource(withLibrary ? R.drawable.bg_radio_off : R.drawable.bg_radio_on);
        radioWithLib.setBackgroundResource(withLibrary ? R.drawable.bg_radio_on : R.drawable.bg_radio_off);
        // choice backgrounds
        optionNoLib.setBackgroundResource(withLibrary ? R.drawable.bg_choice : R.drawable.bg_choice_selected);
        optionWithLib.setBackgroundResource(withLibrary ? R.drawable.bg_choice_selected : R.drawable.bg_choice);
        // show/hide file picker
        filePickerSection.setVisibility(withLibrary ? View.VISIBLE : View.GONE);
        if (!withLibrary) clearSoFile();
        updateCloneButton();
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/octet-stream"});
        startActivityForResult(Intent.createChooser(intent, "Pilih file .so"), REQ_PICK_SO);
    }

    private void clearSoFile() {
        soFilePath = null;
        fileChip.setVisibility(View.GONE);
        filePickerLabel.setText(R.string.pick_so_file);
        filePickerLabel.setTextColor(requireContext().getColor(R.color.vi_text_hint));
        updateCloneButton();
    }

    private void updateCloneButton() {
        boolean ready = !withLib || soFilePath != null;
        btnClone.setVisibility(ready ? View.VISIBLE : View.GONE);
        if (appItem != null) {
            btnClone.setText(getString(R.string.btn_clone, appItem.getName()));
        }
    }

    private void startClone() {
        btnClone.setEnabled(false);
        ProgressFragment frag = ProgressFragment.newInstance(packageName, soFilePath);
        ((MainActivity) requireActivity()).pushFragment(frag);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != REQ_PICK_SO || resultCode != Activity.RESULT_OK) return;
        if (data == null || data.getData() == null) {
            Toast.makeText(requireContext(), getString(R.string.err_invalid_file), Toast.LENGTH_SHORT).show();
            return;
        }
        Uri uri = data.getData();
        String uriPath = uri.getPath();
        if (uriPath == null || !uriPath.endsWith(".so")) {
            Toast.makeText(requireContext(), getString(R.string.err_invalid_file), Toast.LENGTH_SHORT).show();
            return;
        }
        // Copy to cache
        File dest = new File(requireContext().getCacheDir(), "libinject_" + packageName.replace(".", "_") + ".so");
        try (InputStream in = requireContext().getContentResolver().openInputStream(uri);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) out.write(buf, 0, n);
            soFilePath = dest.getAbsolutePath();
            String name = uriPath.substring(uriPath.lastIndexOf('/') + 1);
            fileChipName.setText(name);
            filePickerLabel.setText(name);
            filePickerLabel.setTextColor(requireContext().getColor(R.color.vi_text_primary));
            fileChip.setVisibility(View.VISIBLE);
            updateCloneButton();
        } catch (IOException e) {
            Log.e(TAG, "Copy failed", e);
            Toast.makeText(requireContext(), getString(R.string.err_copy_failed), Toast.LENGTH_SHORT).show();
        }
    }
}
