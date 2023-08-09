package com.alexei.taxiapp.server.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.util.Util;

import java.util.List;

public class AdapterMsgListDrv extends RecyclerView.Adapter<AdapterMsgListDrv.MessageViewHolder> {
    private List<InfoDriverReg> arrDriverMessages;

    private OnSelectListener onSelectItemListener;

    public interface OnSelectListener {
        void onSelectItem(InfoDriverReg info, int position);
    }

    public void setSelectListener(OnSelectListener listener) {
        this.onSelectItemListener = listener;
    }

    public AdapterMsgListDrv(List<InfoDriverReg> arrDriverMessages) {

        this.arrDriverMessages = arrDriverMessages;
    }


    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.messge_list_item, parent, false);
        MessageViewHolder viewHolder = new MessageViewHolder(view);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {

        InfoDriverReg drvInfo = arrDriverMessages.get(holder.getAdapterPosition());
        holder.tvMsgListTitle.setText(Util.formatTimeDate.format(drvInfo.getMessage().getCreateTime()) + "\n" + drvInfo.getName() + " (" + drvInfo.getCallSign() + ")");
        holder.tvMsgListMessage.setText(drvInfo.getMessage().getMsg());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onSelectItemListener.onSelectItem(drvInfo,holder.getAdapterPosition());

            }
        });
    }


    @Override
    public int getItemCount() {
        return arrDriverMessages.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvMsgListTitle;
        private final TextView tvMsgListMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            this.tvMsgListTitle = itemView.findViewById(R.id.tvMsgListTitle);
            this.tvMsgListMessage = itemView.findViewById(R.id.tvMsgListMsg);
        }
    }
}
