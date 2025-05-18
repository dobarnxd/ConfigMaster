package com.example.routerkonfiguralo;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class RouterAdapter extends RecyclerView.Adapter<RouterAdapter.RouterViewHolder> {
    private List<Router> routerList;
    private OnRouterDeleteListener deleteListener;
    private OnRouterEditListener editListener;
    private int lastPosition = -1;

    public interface OnRouterDeleteListener {
        void onRouterDelete(String routerId);
    }

    public interface OnRouterEditListener {
        void onRouterEdit(String routerId);
    }

    public RouterAdapter(List<Router> routerList, OnRouterDeleteListener deleteListener, OnRouterEditListener editListener) {
        this.routerList = routerList;
        this.deleteListener = deleteListener;
        this.editListener = editListener;
    }

    @NonNull
    @Override
    public RouterViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_router, parent, false);
        return new RouterViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RouterViewHolder holder, @SuppressLint("RecyclerView") int position) {
        Router router = routerList.get(position);
        holder.routerName.setText(router.getName());
        holder.routerIp.setText(router.getIpAddress());
        holder.routerModel.setText(router.getModel());
        holder.routerStatus.setText(router.isOnline() ? "Online" : "Offline");
        holder.routerStatus.setTextColor(holder.itemView.getContext().getColor(
                router.isOnline() ? android.R.color.holo_green_dark : android.R.color.holo_red_dark));

        holder.deleteButton.setOnClickListener(v -> {
            if (deleteListener != null) {
                deleteListener.onRouterDelete(router.getId());
            }
        });

        holder.editButton.setOnClickListener(v -> {
            if (editListener != null) {
                editListener.onRouterEdit(router.getId());
            }
        });

        // Apply animation
        if (position > lastPosition) {
            Animation animation = AnimationUtils.loadAnimation(holder.itemView.getContext(), R.anim.item_animation);
            holder.itemView.startAnimation(animation);
            lastPosition = position;
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull RouterViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        holder.itemView.clearAnimation();
    }

    @Override
    public int getItemCount() {
        return routerList.size();
    }

    public void updateData(List<Router> newList) {
        this.routerList = newList;
        lastPosition = -1; // Reset animation position
        notifyDataSetChanged();
    }

    static class RouterViewHolder extends RecyclerView.ViewHolder {
        TextView routerName;
        TextView routerIp;
        TextView routerModel;
        TextView routerStatus;
        ImageButton deleteButton;
        ImageButton editButton;

        RouterViewHolder(View itemView) {
            super(itemView);
            routerName = itemView.findViewById(R.id.routerName);
            routerIp = itemView.findViewById(R.id.routerIp);
            routerModel = itemView.findViewById(R.id.routerModel);
            routerStatus = itemView.findViewById(R.id.routerStatus);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            editButton = itemView.findViewById(R.id.editButton);
        }
    }
} 