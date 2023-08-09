package com.alexei.taxiapp.driver.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.DataRoute;
import com.alexei.taxiapp.util.Util;

import java.util.List;

public class AdapterRoutes extends RecyclerView.Adapter<AdapterRoutes.ViewHolder> {

    private List<DataRoute> routes;
    private OnSelectListener onSelectItemListener;

    public interface OnSelectListener {
        void onSelectItem(DataRoute route);
    }

    public void setSelectListener(OnSelectListener listener) {
        this.onSelectItemListener = listener;
    }


    public AdapterRoutes(List<DataRoute> routes) {
        this.routes = routes;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.route_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        DataRoute route = routes.get(position);
        holder.tvRouteTime.setText(Util.formatTimeDate.format(route.getTime()));

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                onSelectItemListener.onSelectItem(route);
            }
        });
    }


    @Override
    public int getItemCount() {
        return routes.size();
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {

        private final TextView tvRouteTime;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            this.tvRouteTime = itemView.findViewById(R.id.tvRouteTime);

        }
    }
}

