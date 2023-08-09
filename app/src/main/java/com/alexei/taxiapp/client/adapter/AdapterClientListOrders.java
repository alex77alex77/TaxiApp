package com.alexei.taxiapp.client.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.client.activity.ClientMapActivity;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class AdapterClientListOrders  extends RecyclerView.Adapter<AdapterClientListOrders.OrdersViewHolder> {

    private ClientMapActivity activity;
    private List<InfoOrder> arrOrders;

    private OnListener adapterListener;

    public interface OnListener {

        void onMenuItemClick(MenuItem menuItem, InfoOrder order);

        void onSelectItemClick(InfoOrder order);

    }

    public void setListener(OnListener listener) {
        this.adapterListener = listener;
    }

    public AdapterClientListOrders(ClientMapActivity clientMapsActivity, List<InfoOrder> arrOrders) {
        this.activity = clientMapsActivity;
        this.arrOrders = arrOrders;
    }

    @NonNull
    @Override
    public OrdersViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.client_order_list_item, parent, false);
        OrdersViewHolder viewHolder = new OrdersViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull OrdersViewHolder holder, int position) {
        InfoOrder order = arrOrders.get(position);

        PopupMenu popupMenu = new PopupMenu(activity, holder.fabMenuClientAdapter);
        popupMenu.inflate(R.menu.client_menu_orders_adapter);
        Menu menu = popupMenu.getMenu();


        holder.textViewId.setText((!order.getProviderName().equals("SHARED") ? order.getProviderName() + " - " : "") + App.context.getString(R.string.char_num) + order.getId());

        StringBuffer str = new StringBuffer();
        str.append(App.context.getString(R.string.where_to));
        str.append("\n");
        str.append(Util.getAddress( order.getTo().getLatitude(), order.getTo().getLongitude(), Util.TYPE_ADDRESS_LONG));
        str.append("\n");
        str.append(App.context.getString(R.string.t_transport));
        str.append("\n");
        str.append(order.getTypeTr());
        str.append("\n");
        str.append(App.context.getString(R.string.t_rate));
        str.append(order.getRate().toString());

        holder.addressToTextView.setText(str);

        switch (order.getStatus()) {
            case Util.FREE_ORDER_STATUS:

                holder.statusTextView.setTextColor(Color.RED);
                holder.statusTextView.setText(R.string.t_find);
                defineEnableItemMenu(menu, order, true, false, true);
                break;
            case Util.ROUTE_FINISHED_ORDER_STATUS:

                holder.statusTextView.setTextColor(Color.rgb(0, 180, 0));
                holder.statusTextView.setText(R.string.order_completed);
                defineEnableItemMenu(menu, order, false, false, false);
                break;
            case Util.ARRIVE_ORDER_STATUS:

                holder.statusTextView.setTextColor(Color.BLUE);
                holder.statusTextView.setText(R.string.waiting_ );
                holder.statusTextView.append(order.getDataAuto());
                defineEnableItemMenu(menu, order, true, true, true);
                break;
            case Util.ASSIGN_ORDER_STATUS:

                holder.statusTextView.setTextColor(Color.BLUE);
                holder.statusTextView.setText(R.string.assigned_to_drv);
                defineEnableItemMenu(menu, order, true, true, true);
                break;
            case Util.EXECUTION_ORDER_STATUS:

                holder.statusTextView.setTextColor(Color.BLUE);
                holder.statusTextView.setText(R.string.order_execute);
                defineEnableItemMenu(menu, order, true, true, false);
                break;
            case Util.SEND_TO_DRV_ORDER_STATUS:

                holder.statusTextView.setTextColor(Color.RED);
                holder.statusTextView.setText(R.string.redirected);
                defineEnableItemMenu(menu, order, true, false, false);
                break;
            case Util.KILL_ORDER_STATUS:

                holder.statusTextView.setTextColor(Color.RED);
                holder.statusTextView.setText(R.string.order_deleted);
                defineEnableItemMenu(menu, order, false, false, false);
                break;
            case Util.CANCEL_ORDER_STATUS:

                holder.statusTextView.setTextColor(Color.RED);
                holder.statusTextView.setText(R.string.order_canceled);
                defineEnableItemMenu(menu, order, false, false, false);
                break;
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                adapterListener.onMenuItemClick(item, order);

                return true;
            }
        });


        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                adapterListener.onSelectItemClick(order);
            }
        });

        holder.fabMenuClientAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                adapterListener.onMenuItemClick(item, order);

                return true;
            }
        });

    }

    private void defineEnableItemMenu(Menu menu, InfoOrder order, boolean bSrv, boolean bDrv, boolean bCancel) {
        if (order.getProviderKey().equals("SHAREDSERVER")) {

            menu.findItem(R.id.actionMsgSrv).setEnabled(false);

            if (order.getDriverUid().equals("")) {
                menu.findItem(R.id.actionMsgDrv).setEnabled(false);
            }else {
                menu.findItem(R.id.actionMsgDrv).setEnabled(bDrv);
            }
        } else {

            if (order.getDriverUid().equals("")) {
                menu.findItem(R.id.actionMsgDrv).setEnabled(false);
            }else {
                menu.findItem(R.id.actionMsgDrv).setEnabled(bDrv);
            }

            menu.findItem(R.id.actionMsgSrv).setEnabled(bSrv);
        }

//        menu.findItem(R.id.actionCancelOrder).setEnabled(bCancel);
    }

    @Override
    public int getItemCount() {
        return arrOrders.size();
    }

    public static class OrdersViewHolder extends RecyclerView.ViewHolder {
        private final TextView textViewId;
        private final TextView addressToTextView;
        private final TextView statusTextView;
        private final FloatingActionButton fabMenuClientAdapter;

        public OrdersViewHolder(@NonNull View itemView) {
            super(itemView);
            this.fabMenuClientAdapter = itemView.findViewById(R.id.fabMenuClientAdapter);
            this.textViewId = itemView.findViewById(R.id.textViewId);
            this.addressToTextView = itemView.findViewById(R.id.tvClientAddressTo);
            this.statusTextView = itemView.findViewById(R.id.statusTextView);
        }
    }
}

