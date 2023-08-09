package com.alexei.taxiapp.driver.adapter;

import android.annotation.SuppressLint;
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
import com.alexei.taxiapp.driver.model.ServerModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

public class AdapterAvailableSrvList extends RecyclerView.Adapter<AdapterAvailableSrvList.ServerViewHolder> {
    /* access modifiers changed from: private */
    public SelectItemListener adapterListener;
    private ArrayList<ServerModel> arrServer;

    public interface SelectItemListener {
        void onMenuItemClick(MenuItem menuItem, ServerModel serverModel, int i);

        void onSelectItem(ServerModel serverModel, int i);
    }

    public void setListener(SelectItemListener listener) {
        this.adapterListener = listener;
    }

    public AdapterAvailableSrvList(ArrayList<ServerModel> arrServer2) {
        this.arrServer = arrServer2;
    }

    public ServerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ServerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.server_available_list_item, parent, false));
    }

    public void onBindViewHolder(final ServerViewHolder holder,  int pos) {
        final ServerModel serverModel = this.arrServer.get(pos);
        holder.tvNameSrv.setText(serverModel.getName());
        if (serverModel.getServices().isEmpty()) {
            holder.tvTypeActivty.setVisibility(View.GONE);
        } else {
            holder.tvTypeActivty.setVisibility(View.VISIBLE);
            holder.tvTypeActivty.setText(R.string.type_activity);
            holder.tvTypeActivty.append("\n" + serverModel.getServices());
        }
        if (serverModel.getPhone().isEmpty()) {
            holder.tvSrvPhoneContact.setVisibility(View.GONE);
        } else {
            holder.tvSrvPhoneContact.setVisibility(View.VISIBLE);
            holder.tvSrvPhoneContact.setText(R.string.t_phone);
            holder.tvSrvPhoneContact.append(" " + serverModel.getPhone());
        }
        final PopupMenu popupMenu = new PopupMenu(App.context, holder.butAvailableMenu);
        popupMenu.inflate(R.menu.adapter_avai_srv_list_menu);
        Menu menu = popupMenu.getMenu();
        switch (serverModel.getStatus()) {
            case 0:
                holder.textViewStatusServer.setText(R.string.connection_denied);
                holder.textViewStatusServer.setTextColor(Color.rgb(190, 0, 0));
                disabledItemMenu(menu, false, false);
                break;
            case 5:
                holder.textViewStatusServer.setText(R.string.success_req_connect);
                holder.textViewStatusServer.setTextColor(Color.rgb(0, 190, 0));
                disabledItemMenu(menu, false, true);
                break;
            case 8:
                holder.textViewStatusServer.setText(R.string.to_host_no_connect);
                holder.textViewStatusServer.setTextColor(Color.rgb(190, 0, 0));
                disabledItemMenu(menu, true, false);
                break;
            case 9:
                holder.textViewStatusServer.setText(R.string.to_host_connect);
                holder.textViewStatusServer.setTextColor(Color.rgb(0, 190, 0));
                disabledItemMenu(menu, false, true);
                break;
            case 10:
                holder.textViewStatusServer.setText(R.string.not_define);
                holder.textViewStatusServer.setTextColor(Color.rgb(190, 0, 0));
                disabledItemMenu(menu, false, false);
                break;
            case 11:
                holder.textViewStatusServer.setText(R.string.debug_send_request);
                holder.textViewStatusServer.setTextColor(Color.rgb(0, 0, 190));
                disabledItemMenu(menu, false, false);
                break;
            default:
                holder.textViewStatusServer.setText(R.string.unknown);
                holder.textViewStatusServer.setTextColor(Color.rgb(190, 0, 0));
                disabledItemMenu(menu, false, false);
                break;
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AdapterAvailableSrvList.this.adapterListener.onSelectItem(serverModel, holder.getAbsoluteAdapterPosition());
            }
        });
        holder.butAvailableMenu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                popupMenu.show();
                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem menuItem) {
                        AdapterAvailableSrvList.this.adapterListener.onMenuItemClick(menuItem, serverModel, holder.getAbsoluteAdapterPosition());
                        return true;
                    }
                });
            }
        });
    }

    private void disabledItemMenu(Menu menu, boolean conn, boolean disconnect) {
        menu.findItem(R.id.avaiSendDataForConnect).setEnabled(conn);
        menu.findItem(R.id.avaiDelConnect).setEnabled(disconnect);
    }

    public int getItemCount() {
        return this.arrServer.size();
    }

    public static class ServerViewHolder extends RecyclerView.ViewHolder {
        /* access modifiers changed from: private */
        public final FloatingActionButton butAvailableMenu;
        /* access modifiers changed from: private */
        public final TextView textViewStatusServer;
        /* access modifiers changed from: private */
        public final TextView tvNameSrv;
        /* access modifiers changed from: private */
        public final TextView tvSrvPhoneContact;
        /* access modifiers changed from: private */
        public final TextView tvTypeActivty;

        public ServerViewHolder(View itemView) {
            super(itemView);
            this.tvSrvPhoneContact = (TextView) itemView.findViewById(R.id.tvSrvPhoneContact);
            this.tvTypeActivty = (TextView) itemView.findViewById(R.id.tvTypeActivty);
            this.butAvailableMenu = (FloatingActionButton) itemView.findViewById(R.id.butAvaiMenu);
            this.tvNameSrv = (TextView) itemView.findViewById(R.id.tvNameAvaiServer);
            this.textViewStatusServer = (TextView) itemView.findViewById(R.id.tvStatusAvaiServer);
        }
    }
}

