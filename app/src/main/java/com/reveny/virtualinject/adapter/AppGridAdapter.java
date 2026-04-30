package com.reveny.virtualinject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reveny.virtualinject.R;
import com.reveny.virtualinject.model.AppItem;

import java.util.ArrayList;
import java.util.List;

public class AppGridAdapter extends RecyclerView.Adapter<AppGridAdapter.VH> {

    public interface OnAppClickListener {
        void onAppClick(AppItem app);
    }

    private final List<AppItem> fullList = new ArrayList<>();
    private final List<AppItem> visibleList = new ArrayList<>();
    private OnAppClickListener listener;
    private java.util.Set<String> clonedPackages = new java.util.HashSet<>();

    public AppGridAdapter() {}

    public void setOnAppClickListener(OnAppClickListener l) { this.listener = l; }

    public void setApps(List<AppItem> apps) {
        fullList.clear();
        fullList.addAll(apps);
        visibleList.clear();
        visibleList.addAll(apps);
        notifyDataSetChanged();
    }

    public void setClonedPackages(java.util.Set<String> pkgs) {
        this.clonedPackages = pkgs;
        notifyDataSetChanged();
    }

    public void filter(String query) {
        visibleList.clear();
        if (query == null || query.trim().isEmpty()) {
            visibleList.addAll(fullList);
        } else {
            String q = query.toLowerCase().trim();
            for (AppItem a : fullList) {
                if (a.getName().toLowerCase().contains(q) || a.getPackageName().toLowerCase().contains(q)) {
                    visibleList.add(a);
                }
            }
        }
        notifyDataSetChanged();
    }

    public int getVisibleCount() { return visibleList.size(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_app_grid, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        AppItem app = visibleList.get(pos);
        h.icon.setImageDrawable(app.getIcon());
        h.name.setText(app.getName());
        h.cloneDot.setVisibility(clonedPackages.contains(app.getPackageName()) ? View.VISIBLE : View.GONE);
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onAppClick(app); });
    }

    @Override public int getItemCount() { return visibleList.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name;
        View cloneDot;
        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.app_icon);
            name = v.findViewById(R.id.app_name);
            cloneDot = v.findViewById(R.id.clone_dot);
        }
    }
}
