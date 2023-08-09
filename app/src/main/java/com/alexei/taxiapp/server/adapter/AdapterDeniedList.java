package com.alexei.taxiapp.server.adapter;

import android.annotation.SuppressLint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.driver.model.DeniedDrvModel;
import com.alexei.taxiapp.util.Util;

import java.util.ArrayList;

public class AdapterDeniedList extends RecyclerView.Adapter<AdapterDeniedList.MsgViewHolder> {
    private ArrayList<DeniedDrvModel> deniedDrvList;

    public OnSelectListener onSelectItemListener;

    public interface OnSelectListener {
        void onSelectItem(DeniedDrvModel deniedDrvModel, int i);
    }

    public void setSelectListener(OnSelectListener listener) {
        this.onSelectItemListener = listener;
    }

    public AdapterDeniedList(ArrayList<DeniedDrvModel> arrDenies) {
        this.deniedDrvList = arrDenies;
    }

    @NonNull
    public MsgViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.deny_messages_item, parent, false);
        return new MsgViewHolder(view);
    }

    public void onBindViewHolder(MsgViewHolder holder, @SuppressLint("RecyclerView") int position) {
        final DeniedDrvModel deniedModel = this.deniedDrvList.get(position);
        holder.tvDenyListMsgTime.setText(Util.formatTimeDate.format(deniedModel.getDataReq().getTs()));
        holder.tvDenyListMsgTitle.setText(R.string.req_to_connect);
        holder.tvDenyListMsgTitle.append(deniedModel.getDataReq().getTitle());
        holder.tvDenyListMsg.setText(R.string.reason_cancel);
        holder.tvDenyListMsg.append(deniedModel.getErrDescription());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AdapterDeniedList.this.onSelectItemListener.onSelectItem(deniedModel, holder.getAbsoluteAdapterPosition());
            }
        });
    }

    public int getItemCount() {
        return this.deniedDrvList.size();
    }

    public static class MsgViewHolder extends RecyclerView.ViewHolder {
        public final TextView tvDenyListMsg;
        public final TextView tvDenyListMsgTime;
        public final TextView tvDenyListMsgTitle;

        public MsgViewHolder(View itemView) {
            super(itemView);
            this.tvDenyListMsgTitle = (TextView) itemView.findViewById(R.id.tvDenyListMsgTitle);
            this.tvDenyListMsgTime = (TextView) itemView.findViewById(R.id.tvDenyListMsgTime);
            this.tvDenyListMsg = (TextView) itemView.findViewById(R.id.tvDenyListMsg);
        }
    }
}
