package com.reveny.virtualinject.adapter;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.reveny.virtualinject.R;
import com.reveny.virtualinject.model.CloneConfig;

import java.util.ArrayList;
import java.util.List;

public class ClonedAppsAdapter extends RecyclerView.Adapter<ClonedAppsAdapter.VH> {

    public interface OnItemClickListener {
        void onItemClick(CloneConfig config);
    }

    private final List<CloneConfig> list = new ArrayList<>();
    private OnItemClickListener listener;
    private PackageManager pm;

    public ClonedAppsAdapter(PackageManager pm) { this.pm = pm; }

    public void setOnItemClickListener(OnItemClickListener l) { this.listener = l; }

    public void setData(List<CloneConfig> data) {
        list.clear();
        list.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_cloned_app, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        CloneConfig cfg = list.get(pos);
        // App icon + name from real PackageManager
        try {
            android.content.pm.ApplicationInfo info = pm.getApplicationInfo(cfg.getPackageName(), 0);
            Drawable icon = pm.getApplicationIcon(info);
            h.icon.setImageDrawable(icon);
            h.name.setText(pm.getApplicationLabel(info));
        } catch (PackageManager.NameNotFoundException e) {
            h.icon.setImageResource(android.R.drawable.sym_def_app_icon);
            h.name.setText(cfg.getPackageName());
        }
        // .so status
        if (cfg.hasSo()) {
            String fileName = cfg.getSoFilePath();
            int slash = fileName.lastIndexOf('/');
            if (slash >= 0) fileName = fileName.substring(slash + 1);
            h.soStatus.setText("💉 " + fileName);
            h.soStatus.setTextColor(h.itemView.getContext().getColor(R.color.vi_green));
        } else {
            h.soStatus.setText(h.itemView.getContext().getString(R.string.so_none));
            h.soStatus.setTextColor(h.itemView.getContext().getColor(R.color.vi_text_hint));
        }
        h.itemView.setOnClickListener(v -> { if (listener != null) listener.onItemClick(cfg); });
    }

    @Override public int getItemCount() { return list.size(); }

    static class VH extends RecyclerView.ViewHolder {
        ImageView icon;
        TextView name, soStatus;
        VH(View v) {
            super(v);
            icon = v.findViewById(R.id.app_icon);
            name = v.findViewById(R.id.app_name);
            soStatus = v.findViewById(R.id.so_status);
        }
    }
}
