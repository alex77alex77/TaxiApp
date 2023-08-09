package com.alexei.taxiapp.server.adapter;

import android.content.Context;
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

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

public class AdapterServerDriverList extends RecyclerView.Adapter<AdapterServerDriverList.DriversViewHolder> {
    private Context context;
    private List<InfoDriverReg> arrDrivers;

    private SelectListener adapterListener;

    public interface SelectListener {
        void onReadMsg(InfoDriverReg drv);

        void onMenuItemClick(MenuItem menuItem, InfoDriverReg drv);

        void onSendMsgClick(String str, InfoDriverReg drv);
    }

    public void setListener(SelectListener listener) {
        this.adapterListener = listener;
    }

    public AdapterServerDriverList(Context context, List<InfoDriverReg> arrDrivers) {
        this.context = context;
        this.arrDrivers = arrDrivers;
    }

    @NonNull
    @Override
    public DriversViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.server_driver_list_item, parent, false);
        DriversViewHolder viewHolder = new DriversViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull DriversViewHolder holder, int position) {
        InfoDriverReg drv = arrDrivers.get(position);
        holder.textViewServerDriveId.setText(drv.getName() + "(" + drv.getCallSign() + ")");

        PopupMenu popupMenu = new PopupMenu(context, holder.fabMenuDriverToServer);
        popupMenu.inflate(R.menu.command_driver_in_adapter_menu);
        Menu menu = popupMenu.getMenu();

        if (drv.getShiftStatus() == Util.SHIFT_OPEN_DRV_STATUS) {

            String t = (drv.getOpenShiftTime() > 0) ? Util.formatTimeDate.format(drv.getOpenShiftTime()) : Util.formatTimeDate.format(System.currentTimeMillis());
            String s = context.getString(R.string.shift_open) + t;
            holder.tvStateShift.setText(s);
            holder.tvStateShift.setTextColor(Color.rgb(0, 180, 0));
        } else {

            String t1 = (drv.getCloseShiftTime() > 0) ? Util.formatTimeDate.format(drv.getCloseShiftTime()) : "";
            String t2 = (drv.getFinishTimeShift() > 0) ? context.getString(R.string.end_shift) + Util.formatTimeDate.format(drv.getFinishTimeShift()) : "";
            String s = context.getString(R.string.shift_close) + t1 + t2;

            holder.tvStateShift.setText(s);
            holder.tvStateShift.setTextColor(Color.RED);
        }

        switch (drv.getStatusToHostSrv()) {
            case Util.UNKNOWN_DRIVER_STATUS:
                holder.tvStateShift.setVisibility(View.GONE);
                holder.tvServerDriveStatus.setTextColor(Color.RED);
                holder.tvServerDriveStatus.setText(R.string.unknown);
                defEnabledElementMenu(menu, false, false, false, false, false, false, false);
                break;
            case Util.CONNECTED_TO_SERVER_DRIVER_STATUS:
                holder.tvStateShift.setVisibility(View.VISIBLE);
                holder.tvServerDriveStatus.setTextColor(Color.rgb(0, 0, 205));
                holder.tvServerDriveStatus.setText(R.string.info_not_received);//
                defEnabledElementMenu(menu, true, true, true, false, true, true, false);

                switch (drv.getStatusShared()) {
                    case Util.FREE_DRIVER_STATUS:

                        holder.tvServerDriveStatus.setTextColor(Color.rgb(255, 165, 32));
                        holder.tvServerDriveStatus.setText(R.string.status_free);
                        break;
                    case Util.BUSY_DRIVER_STATUS:

                        holder.tvServerDriveStatus.setTextColor(Color.rgb(0, 180, 0));
                        holder.tvServerDriveStatus.setText(R.string.status_busy);
                        break;
                }
                break;
            case Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS:
                holder.tvStateShift.setVisibility(View.VISIBLE);
                holder.tvServerDriveStatus.setTextColor(Color.RED);
                holder.tvServerDriveStatus.setText(R.string.status_block);
                defEnabledElementMenu(menu, false, true, false, true, true, true, false);
                break;
            case Util.NOT_REF_DRIVER_STATUS_TMP:
                holder.tvStateShift.setVisibility(View.GONE);
                holder.tvServerDriveStatus.setTextColor(Color.RED);
                holder.tvServerDriveStatus.setText(R.string.status_not_connect);
                defEnabledElementMenu(menu, false, false, false, false, true, true, false);
                break;
            case Util.SHIFT_CLOSE_DRV_STATUS:
                holder.tvStateShift.setVisibility(View.VISIBLE);
                holder.tvServerDriveStatus.setTextColor(Color.rgb(0, 0, 180));
                holder.tvServerDriveStatus.setText(R.string.shift_close);
                defEnabledElementMenu(menu, true, true, true, false, true, true, true);
                break;
        }


        if (drv.getSosModel() != null) {
            holder.tvSOSDrvList.setVisibility(View.VISIBLE);
        } else {
            holder.tvSOSDrvList.setVisibility(View.GONE);
        }


        if (drv.getMessage().getMsg().length() > 0) {
            holder.ibEmail.setVisibility(View.VISIBLE);
        } else {
            holder.ibEmail.setVisibility(View.GONE);
        }

        holder.ibEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                adapterListener.onReadMsg(drv);

            }
        });

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.actionAdapterSendMessage:
                        if (holder.blockSendMessageLL.getVisibility() == View.GONE) {
                            holder.blockSendMessageLL.setVisibility(View.VISIBLE);
                        } else {
                            holder.blockSendMessageLL.setVisibility(View.GONE);
                        }
                        break;
                }
                adapterListener.onMenuItemClick(item, drv);

                return true;
            }
        });

        holder.fabMenuDriverToServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });

        holder.ibSendMsgDrvInAdapter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (holder.etMsgDrvInAdapter.length() > 0) {
                    adapterListener.onSendMsgClick(holder.etMsgDrvInAdapter.getText().toString(), drv);

                    holder.blockSendMessageLL.setVisibility(View.GONE);
                }

            }
        });

    }

    private void defEnabledElementMenu(Menu menu, boolean bDisplayInMap, boolean bSendMessage, boolean bUnConnect, boolean bBlock,
                                       boolean bInfo, boolean bPhone, boolean bRoute) {
        menu.findItem(R.id.actionAdapterDisplayInMap).setEnabled(bDisplayInMap);
        menu.findItem(R.id.actionAdapterSendMessage).setEnabled(bSendMessage);
        menu.findItem(R.id.actionAdapterBlockDrv).setEnabled(bUnConnect);
        menu.findItem(R.id.actionAdapterUnblockDrv).setEnabled(bBlock);
        menu.findItem(R.id.actionAdapterInfoDriver).setEnabled(bInfo);
        menu.findItem(R.id.actionAdapterPhone).setEnabled(bPhone);
        menu.findItem(R.id.actionAdapterRoute).setEnabled(bRoute);
//        menu.findItem(R.id.actionAdapterOpenSwift).setEnabled(bOpenSwift);

//        menu.findItem(R.id.actionAdapterConnectDrv).setEnabled(bConnect);

    }


    @Override
    public int getItemCount() {
        return arrDrivers.size();
    }

    public static class DriversViewHolder extends RecyclerView.ViewHolder {
        private TextView tvStateShift;
        private TextView textViewServerDriveId;
        private TextView tvServerDriveStatus;
        private EditText etMsgDrvInAdapter;
        private FloatingActionButton fabMenuDriverToServer;
        private TextView tvSOSDrvList;

        private ImageButton ibSendMsgDrvInAdapter;
        private ImageButton ibEmail;
        private LinearLayout blockSendMessageLL;


        public DriversViewHolder(@NonNull View itemView) {
            super(itemView);

            this.tvStateShift = itemView.findViewById(R.id.tvStateShift);
            this.tvSOSDrvList = itemView.findViewById(R.id.tvSOSDrvList);
            this.ibEmail = itemView.findViewById(R.id.ibEmailDrvList);
            this.blockSendMessageLL = itemView.findViewById(R.id.blockSendMessageLL);
            this.ibSendMsgDrvInAdapter = itemView.findViewById(R.id.imageButtonSendMessageDriverInAdapter);
            this.etMsgDrvInAdapter = itemView.findViewById(R.id.editTextMessageDriverInAdapter);
            this.fabMenuDriverToServer = itemView.findViewById(R.id.fabMenuDriverToServer);
            this.textViewServerDriveId = itemView.findViewById(R.id.textViewServerDriveId);

            this.tvServerDriveStatus = itemView.findViewById(R.id.textViewServerDriveStatus);


        }
    }
}
