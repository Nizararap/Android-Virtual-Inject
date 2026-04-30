package com.reveny.virtualinject.ui.fragment;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;
import com.reveny.virtualinject.R;
import com.reveny.virtualinject.adapter.AppGridAdapter;
import com.reveny.virtualinject.adapter.ClonedAppsAdapter;
import com.reveny.virtualinject.model.AppItem;
import com.reveny.virtualinject.model.CloneConfig;
import com.reveny.virtualinject.store.CloneStore;
import com.reveny.virtualinject.util.Utility;
import com.reveny.virtualinject.ui.activity.MainActivity;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HomeFragment extends BaseFragment {

    private AppGridAdapter gridAdapter;
    private ClonedAppsAdapter clonedAdapter;
    private RecyclerView rvAll, rvCloned;
    private LinearLayout emptyCloned, searchBar;
    private TextView emptySearch, headerSubtitle, btnGoAdd, searchClear;
    private EditText searchInput;
    private TabLayout tabLayout;

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_home, container, false);
        bindViews(root);
        setupTabs();
        setupSearch();
        setupAllAppsGrid();
        setupClonedList();
        loadApps();
        return root;
    }

    private void bindViews(View root) {
        tabLayout    = root.findViewById(R.id.tab_layout);
        rvAll        = root.findViewById(R.id.rv_all_apps);
        rvCloned     = root.findViewById(R.id.rv_cloned_apps);
        emptyCloned  = root.findViewById(R.id.empty_cloned);
        emptySearch  = root.findViewById(R.id.empty_search);
        searchBar    = root.findViewById(R.id.search_bar);
        searchInput  = root.findViewById(R.id.search_input);
        searchClear  = root.findViewById(R.id.search_clear);
        headerSubtitle = root.findViewById(R.id.header_subtitle);
        btnGoAdd     = root.findViewById(R.id.btn_go_add);
    }

    private void setupTabs() {
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_all_apps));
        tabLayout.addTab(tabLayout.newTab().setText(R.string.tab_cloned));
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override public void onTabSelected(TabLayout.Tab tab) {
                boolean isAll = tab.getPosition() == 0;
                rvAll.setVisibility(isAll ? View.VISIBLE : View.GONE);
                searchBar.setVisibility(isAll ? View.VISIBLE : View.GONE);
                refreshClonedTab(!isAll);
                if (isAll) {
                    headerSubtitle.setText("Tap app untuk di-clone");
                } else {
                    int cnt = CloneStore.get(requireContext()).getAllClones().size();
                    headerSubtitle.setText(cnt + " app ter-clone");
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void setupSearch() {
        searchInput.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                gridAdapter.filter(s.toString());
                searchClear.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
                emptySearch.setVisibility(gridAdapter.getVisibleCount() == 0 ? View.VISIBLE : View.GONE);
            }
            @Override public void afterTextChanged(Editable s) {}
        });
        searchClear.setOnClickListener(v -> searchInput.setText(""));
    }

    private void setupAllAppsGrid() {
        gridAdapter = new AppGridAdapter();
        gridAdapter.setOnAppClickListener(app -> navigateToCloneOrManage(app));
        rvAll.setLayoutManager(new GridLayoutManager(requireContext(), 4));
        rvAll.setAdapter(gridAdapter);
    }

    private void setupClonedList() {
        clonedAdapter = new ClonedAppsAdapter(requireContext().getPackageManager());
        clonedAdapter.setOnItemClickListener(config -> {
            AppItem appItem = Utility.getAppItem(requireContext(), config.getPackageName());
            if (appItem != null) navigateToManage(appItem, config);
        });
        rvCloned.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvCloned.setAdapter(clonedAdapter);
        btnGoAdd.setOnClickListener(v -> tabLayout.selectTab(tabLayout.getTabAt(0)));
    }

    private void loadApps() {
        new Thread(() -> {
            List<AppItem> apps = Utility.getInstalledAppItems(requireContext());
            requireActivity().runOnUiThread(() -> {
                gridAdapter.setApps(apps);
                refreshClonedDots();
            });
        }).start();
    }

    public void refreshClonedDots() {
        if (gridAdapter == null) return;
        List<CloneConfig> clones = CloneStore.get(requireContext()).getAllClones();
        Set<String> pkgs = new HashSet<>();
        for (CloneConfig c : clones) pkgs.add(c.getPackageName());
        gridAdapter.setClonedPackages(pkgs);
    }

    private void refreshClonedTab(boolean show) {
        if (!show) { rvCloned.setVisibility(View.GONE); emptyCloned.setVisibility(View.GONE); return; }
        List<CloneConfig> clones = CloneStore.get(requireContext()).getAllClones();
        clonedAdapter.setData(clones);
        if (clones.isEmpty()) {
            rvCloned.setVisibility(View.GONE);
            emptyCloned.setVisibility(View.VISIBLE);
        } else {
            rvCloned.setVisibility(View.VISIBLE);
            emptyCloned.setVisibility(View.GONE);
        }
    }

    private void navigateToCloneOrManage(AppItem app) {
        CloneConfig existing = CloneStore.get(requireContext()).getConfig(app.getPackageName());
        if (existing != null) {
            navigateToManage(app, existing);
        } else {
            CloneSetupFragment frag = CloneSetupFragment.newInstance(app.getPackageName());
            ((MainActivity) requireActivity()).pushFragment(frag);
        }
    }

    private void navigateToManage(AppItem app, CloneConfig config) {
        ManageFragment frag = ManageFragment.newInstance(app.getPackageName());
        ((MainActivity) requireActivity()).pushFragment(frag);
    }

    @Override public void onResume() {
        super.onResume();
        refreshClonedDots();
    }
}
