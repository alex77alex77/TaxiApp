package com.alexei.taxiapp.driver.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.driver.activity.ListFreeOrdersActivity;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class AdapterFreeOrders extends RecyclerView.Adapter<AdapterFreeOrders.ElementViewHolder> {

    private Context context= App.context;
    private ListFreeOrdersActivity activity;
    private ArrayList<InfoOrder> orderArrayList;

    public AdapterFreeOrders(ListFreeOrdersActivity listFreeOrdersActivity, ArrayList<InfoOrder> arrOrders) {
        this.activity = listFreeOrdersActivity;
        this.orderArrayList = arrOrders;
    }

    @NonNull
    @Override
    public ElementViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.free_order_item, parent, false);
        ElementViewHolder viewHolder = new ElementViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ElementViewHolder holder, int position) {
        InfoOrder infoOrder = orderArrayList.get(position);

        holder.tvTimeCreatedOrder.setText(infoOrder.getProviderName() + " - " + Util.formatTimeDate.format(infoOrder.getTimestamp()));
        holder.textViewDistance.setText(context.getString(R.string.distance_to_client) );
        holder.textViewDistance.append(infoOrder.getDistanceToClient());

        holder.tvAddressFrom.setText(context.getString(R.string.where_from ));
        holder.tvAddressFrom.append("\n" + Util.getAddress( infoOrder.getFrom().getLatitude(), infoOrder.getFrom().getLongitude(), Util.TYPE_ADDRESS_LONG));
        holder.tvAddressTo.setText(context.getString(R.string.where_to));
        holder.tvAddressTo.append("\n" + Util.getAddress( infoOrder.getTo().getLatitude(), infoOrder.getTo().getLongitude(), Util.TYPE_ADDRESS_LONG));
        holder.tvFreeOrderType.setText(context.getString(R.string.t_type));
        holder.tvFreeOrderType.append(infoOrder.getTypeTr());
        holder.tvRate.setText(context.getString(R.string.t_rate));
        holder.tvRate.append(infoOrder.getRate().toString());

        holder.fabSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.selectedItem(infoOrder, holder.getAbsoluteAdapterPosition());
            }
        });
        holder.tvShowDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.showDotFrom(infoOrder);
            }
        });
        holder.tvShowDot2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                activity.showDotTo(infoOrder);
            }
        });
    }


    @Override
    public int getItemCount() {
        return orderArrayList.size();
    }

    public static class ElementViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvShowDot;
        private final TextView tvShowDot2;
        private final TextView tvRate;
        private final TextView tvTimeCreatedOrder;
        private final TextView tvAddressFrom;
        private final TextView tvAddressTo;
        private final TextView tvFreeOrderType;
        private final TextView textViewDistance;
        private final FloatingActionButton fabSelect;

        public ElementViewHolder(@NonNull View itemView) {
            super(itemView);
            this.tvShowDot = itemView.findViewById(R.id.tvShowDot);
            this.tvShowDot2 = itemView.findViewById(R.id.tvShowDot2);
            this.tvRate = itemView.findViewById(R.id.tvRate);
            this.tvFreeOrderType = itemView.findViewById(R.id.tvFreeOrderType);
            this.tvTimeCreatedOrder = itemView.findViewById(R.id.tvTimeCreatedOrder);
            this.tvAddressFrom = itemView.findViewById(R.id.tvFreeOrderAddressFrom);
            this.tvAddressTo = itemView.findViewById(R.id.tvFreeOrderAddressTo);
            this.textViewDistance = itemView.findViewById(R.id.tvDistance);
            this.fabSelect = itemView.findViewById(R.id.fabSelect);
        }
    }
}

