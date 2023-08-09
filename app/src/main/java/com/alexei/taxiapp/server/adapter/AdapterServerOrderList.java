package com.alexei.taxiapp.server.adapter;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class AdapterServerOrderList extends RecyclerView.Adapter<AdapterServerOrderList.OrdersViewHolder> {
    private ArrayList<InfoOrder> arrOrders;
    private Context context = App.context;
  
    public OnListener onListener;

    public interface OnListener {
        void onAdapterItemLongClick(InfoOrder infoOrder);

        void onMailClick(int i, InfoOrder infoOrder);

        void onMenuItemClick(MenuItem menuItem, InfoOrder infoOrder);
    }

    public AdapterServerOrderList(ArrayList<InfoOrder> arrOrders2) {
        this.arrOrders = arrOrders2;
    }

    public void setOnListener(OnListener listener) {
        this.onListener = listener;
    }

    public OrdersViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new OrdersViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.server_order_list_item, parent, false));
    }

    public void onBindViewHolder(final OrdersViewHolder holder, int pos) {
        final InfoOrder order = this.arrOrders.get(pos);
        final PopupMenu popupMenu = new PopupMenu(this.context, holder.fabMenuOrderToServer);
        popupMenu.inflate(R.menu.server_order_list_adapter_menu);
        Menu menu = popupMenu.getMenu();
        holder.textViewServerOrderId.setText((order.getClientUid().length() > 1 ? new StringBuilder().append(this.context.getString(R.string.client)).append(order.getClientName()) : new StringBuilder().append(this.context.getString(R.string.char_num)).append(order.getId())).append("\n").toString());
        holder.textViewServerOrderId.append(Util.formatTimeDate.format(order.getTimestamp()));
        holder.tvAddressServerOrder.setText(R.string.where_from);
        holder.tvAddressServerOrder.append("\n");
        holder.tvAddressServerOrder.append(Util.getAddress(order.getFrom().getLatitude(), order.getFrom().getLongitude(), 0));
        holder.tvAddressServerOrder.append(this.context.getString(R.string.t_there_to));
        holder.tvAddressServerOrder.append(Util.getAddress(order.getTo().getLatitude(), order.getTo().getLongitude(), 0));
        if (order.getNote().length() > 0) {
            holder.tvAddressServerOrder.append(this.context.getString(R.string._t_note_));
            holder.tvAddressServerOrder.append(order.getNote());
        }
        holder.tvAddressServerOrder.append(this.context.getString(R.string._t_transport_) + order.getTypeTr());
        holder.tvAddressServerOrder.append(this.context.getString(R.string._t_rate_) + order.getRate().toString());
        if (order.getMsgS().getMsg().length() > 0) {
            holder.tvMailFromDrv.setVisibility(View.VISIBLE);
        } else {
            holder.tvMailFromDrv.setVisibility(View.GONE);
        }
        if (!order.getClientUid().equals("")) {
            menu.findItem(R.id.menuOrderClient).setEnabled(true);
            menu.findItem(R.id.actionOrderCancel).setVisible(false);
            menu.findItem(R.id.actionOrderSendToAll).setVisible(false);
            menu.findItem(R.id.actionOrderSendToCallSign).setVisible(false);
        }
        handlerStatusOrder(order, holder, menu);
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                AdapterServerOrderList.this.onListener.onMenuItemClick(item, order);
                return true;
            }
        });
        holder.fabMenuOrderToServer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                popupMenu.show();
            }
        });
        holder.itemView.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View view) {
                AdapterServerOrderList.this.onListener.onAdapterItemLongClick(order);
                return true;
            }
        });
        holder.tvMailFromDrv.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AdapterServerOrderList.this.onListener.onMailClick(holder.getAdapterPosition(), order);
            }
        });
    }

    private void handlerStatusOrder(InfoOrder order, OrdersViewHolder holder, Menu menu) {
        switch (order.getStatus()) {
            case -2:
                holder.textViewServerOrderStatus.setTextColor(Color.RED);
                holder.textViewServerOrderStatus.setText(R.string.order_deleted);
                defEnabledElementMenu(menu, false, false, false, false);
                return;
            case -1:
                holder.textViewServerOrderStatus.setTextColor(Color.RED);
                holder.textViewServerOrderStatus.setText(R.string.canceled);
                defEnabledElementMenu(menu, false, false, false, false);
                return;
            case 1:
            case 10:
                holder.textViewServerOrderStatus.setTextColor(Color.GREEN);
                holder.textViewServerOrderStatus.setText(R.string.assigned_to_drv);
                defEnabledElementMenu(menu, false, false, true, true);
                return;
            case 2:
                holder.textViewServerOrderStatus.setTextColor(Color.BLUE);
                holder.textViewServerOrderStatus.setText(R.string.status_send_drv);
                defEnabledElementMenu(menu, false, false, true, false);
                return;
            case 3:
                holder.textViewServerOrderStatus.setTextColor(Color.BLUE);
                holder.textViewServerOrderStatus.setText(R.string.wait_send);
                defEnabledElementMenu(menu, true, true, false, false);
                return;
            case 5:
                holder.textViewServerOrderStatus.setTextColor(Color.BLUE);
                holder.textViewServerOrderStatus.setText(R.string.st_free_order);
                defEnabledElementMenu(menu, false, false, true, false);
                return;
            case 9:
                holder.textViewServerOrderStatus.setTextColor(Color.GREEN);
                holder.textViewServerOrderStatus.setText(R.string.in_progress);
                defEnabledElementMenu(menu, false, false, true, true);
                return;
            case 11:
                holder.textViewServerOrderStatus.setTextColor(Color.YELLOW);
                holder.textViewServerOrderStatus.setText(R.string.completed);
                defEnabledElementMenu(menu, false, false, false, false);
                return;
            default:
                return;
        }
    }

    public int getItemCount() {
        return this.arrOrders.size();
    }

    public static class OrdersViewHolder extends RecyclerView.ViewHolder {

        public FloatingActionButton fabMenuOrderToServer;
      
        public TextView textViewServerOrderId;
      
        public TextView textViewServerOrderStatus;
      
        public TextView tvAddressServerOrder;
      
        public TextView tvMailFromDrv;

        public OrdersViewHolder(View itemView) {
            super(itemView);
            this.tvMailFromDrv = (TextView) itemView.findViewById(R.id.tvMailFromDrv);
            this.fabMenuOrderToServer = (FloatingActionButton) itemView.findViewById(R.id.fabMenuOrderToServer);
            this.textViewServerOrderId = (TextView) itemView.findViewById(R.id.textViewServerOrderId);
            this.tvAddressServerOrder = (TextView) itemView.findViewById(R.id.tvAddressServerOrder);
            this.textViewServerOrderStatus = (TextView) itemView.findViewById(R.id.textViewServerOrderStatus);
        }
    }

    private void defEnabledElementMenu(Menu menu, boolean bSend, boolean bSendAll, boolean bCancel, boolean bDriver) {
        menu.findItem(R.id.actionOrderSendToCallSign).setEnabled(bSend);
        menu.findItem(R.id.actionOrderSendToAll).setEnabled(bSendAll);
        menu.findItem(R.id.actionOrderCancel).setEnabled(bCancel);
        menu.findItem(R.id.menuOrderDrv).setEnabled(bDriver);
    }
}
