package com.alexei.taxiapp.driver.adapter;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;

import androidx.core.internal.view.SupportMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.driver.provider.exClass.ProviderClass;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.database.DatabaseReference;

import java.util.List;

public class AdapterProviders extends RecyclerView.Adapter<AdapterProviders.ServerViewHolder> {
    private List<ProviderClass> arrServer;
    /* access modifiers changed from: private */
    public OnListeners onListeners;

    public interface OnListeners {
        void onMenuItemClick(MenuItem menuItem, ProviderClass providerClass);

        void onSendMsgSrv(DatabaseReference databaseReference, String str);
    }

    public void setListeners(OnListeners listener) {
        this.onListeners = listener;
    }

    public AdapterProviders(List<ProviderClass> arrServer2) {
        this.arrServer = arrServer2;
    }

    public ServerViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ServerViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.server_provider_list_item, parent, false));
    }

    public void onBindViewHolder(final ServerViewHolder holder, int position) {
        final PopupMenu popupMenu = new PopupMenu(App.context, holder.fabMenu);
        popupMenu.inflate(R.menu.adapter_provider_list_menu);
        final Menu menu = popupMenu.getMenu();
        final ProviderClass provider = this.arrServer.get(position);
        handlerStatus(holder, provider, menu);
        holder.tvNameServer.setText(provider.getNameSrv());
        holder.fabMenu.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (provider.getNameSrv().equals("SHARED")) {
                    menu.findItem(R.id.actionMsgProvList).setEnabled(false);
                    menu.findItem(R.id.actionShift).setEnabled(false);
                }
                popupMenu.show();
            }
        });
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            public boolean onMenuItemClick(MenuItem item) {
                if (item.getItemId() == R.id.actionMsgProvList) {
                    if (holder.blockSendMsgLL.getVisibility() == View.GONE) {
                        holder.blockSendMsgLL.setVisibility(View.VISIBLE);
                    } else {
                        holder.blockSendMsgLL.setVisibility(View.GONE);
                    }
                    return true;
                }
                AdapterProviders.this.onListeners.onMenuItemClick(item, provider);
                return true;
            }
        });
        holder.ibSendMsg.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AdapterProviders.this.onListeners.onSendMsgSrv(provider.getSrvRef(), holder.etTextMsg.getText().toString());
                holder.blockSendMsgLL.setVisibility(View.GONE);
            }
        });
    }

    private void defEnabledElementMenu(Menu menu, boolean bMsg, boolean bStop, boolean bContinue, boolean bShift) {
        menu.findItem(R.id.actionMsgProvList).setEnabled(bMsg);
        menu.findItem(R.id.actionStopProvList).setEnabled(bStop);
        menu.findItem(R.id.actionContinueProvList).setEnabled(bContinue);
        menu.findItem(R.id.actionShift).setEnabled(bShift);
    }

    private void handlerStatus(ServerViewHolder holder, ProviderClass server, Menu menu) {
        switch (server.getShiftModel().getStatus()) {
            case 4:
                holder.tvShiftStatus.setText(R.string.no_funds_available);
                holder.tvShiftStatus.setTextColor(Color.RED);
                break;
            case 5:
                holder.tvShiftStatus.setText(R.string.access_disable);
                holder.tvShiftStatus.setTextColor(Color.RED);
                break;
            case 8:
                holder.tvShiftStatus.setText(R.string.shift_close_);
                holder.tvShiftStatus.append(Util.formatTimeDate.format(server.getShiftModel().getTimer()));
                holder.tvShiftStatus.setTextColor(Color.BLUE);
                menu.findItem(R.id.actionShift).setTitle(R.string.cmd_open_shift);
                break;
            case 9:
                holder.tvShiftStatus.setText(R.string.shift_open);
                holder.tvShiftStatus.append(Util.formatTimeDate.format(server.getShiftModel().getTimer()));
                holder.tvShiftStatus.setTextColor(Color.rgb(0, 190, 0));
                menu.findItem(R.id.actionShift).setTitle(R.string.btn_close_shift);
                break;
        }
        if (server.getNameSrv().equals("SHARED")) {
            holder.tvShiftStatus.setVisibility(View.GONE);
        }
        switch (server.getStatusDrvOnSrv()) {
            case -1:
                holder.tvStatusServer.setText(R.string.debug_state_provider);
                holder.tvStatusServer.setTextColor(Color.rgb(190, 0, 0));
                defEnabledElementMenu(menu, true, false, false, false);
                return;
            case 4:
                holder.tvStatusServer.setText(R.string.sate_block);
                holder.tvStatusServer.setTextColor(Color.rgb(190, 0, 0));
                defEnabledElementMenu(menu, true, false, false, false);
                return;
            case 5:
                if (server.getStatusLocal() == 2) {
                    holder.tvStatusServer.setText(R.string.debug_state_pause);
                    holder.tvStatusServer.setTextColor(Color.rgb(190, 0, 0));
                    defEnabledElementMenu(menu, true, false, true, false);
                    return;
                }
                holder.tvStatusServer.setText(R.string.debug_state_connected);
                holder.tvStatusServer.setTextColor(Color.rgb(0, 190, 0));
                defEnabledElementMenu(menu, true, true, false, true);
                return;
            case 8:
                holder.tvStatusServer.setText(R.string.debug_state_not_connected);
                holder.tvStatusServer.setTextColor(Color.rgb(190, 0, 0));
                defEnabledElementMenu(menu, false, false, false, false);
                return;
            default:
                defEnabledElementMenu(menu, false, false, false, false);
        }
    }

    public int getItemCount() {
        return this.arrServer.size();
    }

    public static class ServerViewHolder extends RecyclerView.ViewHolder {

        public final LinearLayout blockSendMsgLL;

        public final EditText etTextMsg;

        public final FloatingActionButton fabMenu;

        public final ImageButton ibSendMsg;

        public final TextView tvNameServer;

        public final TextView tvShiftStatus;

        public final TextView tvStatusServer;

        public ServerViewHolder(View itemView) {
            super(itemView);
            this.tvShiftStatus = (TextView) itemView.findViewById(R.id.tvShiftStatus);
            this.fabMenu = (FloatingActionButton) itemView.findViewById(R.id.fabProvMenu);
            this.tvNameServer = (TextView) itemView.findViewById(R.id.tvNameProvider);
            this.tvStatusServer = (TextView) itemView.findViewById(R.id.tvStatusServer);
            this.blockSendMsgLL = (LinearLayout) itemView.findViewById(R.id.blockSendMsgLL);
            this.etTextMsg = (EditText) itemView.findViewById(R.id.etTextMsg);
            this.ibSendMsg = (ImageButton) itemView.findViewById(R.id.ibSendMsg);
        }
    }
}

