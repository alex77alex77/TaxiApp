package com.alexei.taxiapp.driver.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.driver.provider.exClass.ProviderClass;
import com.alexei.taxiapp.util.Util;

import java.util.List;

public class AdapterMsgListSrv extends RecyclerView.Adapter<AdapterMsgListSrv.MessageViewHolder> {
    /* access modifiers changed from: private */
    public OnSelectListener onSelectItemListener;
    private List<ProviderClass> providersList;

    public interface OnSelectListener {
        void onSelectItem(ProviderClass providerClass, int i);
    }

    public void setSelectListener(OnSelectListener listener) {
        this.onSelectItemListener = listener;
    }

    public AdapterMsgListSrv(List<ProviderClass> providerList) {
        this.providersList = providerList;
    }

    @NonNull
    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.messge_list_item, parent, false));
    }

    public void onBindViewHolder(final MessageViewHolder holder, int position) {
        final ProviderClass serverModel = this.providersList.get(position);
        holder.tvTitle.setText(Util.formatTimeDate.format(serverModel.getMsgModel().getCreateTime()));
        holder.tvTitle.append("\n" + serverModel.getNameSrv());
        holder.tvMessage.setText(serverModel.getMsgModel().getMsg());
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AdapterMsgListSrv.this.onSelectItemListener.onSelectItem(serverModel, holder.getAbsoluteAdapterPosition());
            }
        });
    }

    public int getItemCount() {
        return this.providersList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvMessage;

        public final TextView tvTitle;

        public MessageViewHolder(View itemView) {
            super(itemView);
            this.tvTitle = (TextView) itemView.findViewById(R.id.tvMsgListTitle);
            this.tvMessage = (TextView) itemView.findViewById(R.id.tvMsgListMsg);
        }
    }
}

