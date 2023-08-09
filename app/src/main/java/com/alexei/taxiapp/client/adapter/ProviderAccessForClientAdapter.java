package com.alexei.taxiapp.client.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.client.model.AvailableProviderModel;

import java.util.List;

public class ProviderAccessForClientAdapter extends RecyclerView.Adapter<ProviderAccessForClientAdapter.ViewHolder> {

    public SelectItemListener adapterListener;
    private final List<AvailableProviderModel> arrServer;

    public interface SelectItemListener {
        void onSelectItem(AvailableProviderModel availableProviderModel);
    }

    public void setListener(SelectItemListener listener) {
        adapterListener = listener;
    }

    public ProviderAccessForClientAdapter(List<AvailableProviderModel> arrServer2) {
        arrServer = arrServer2;
    }

    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.messge_list_item, parent, false));
    }

    public void onBindViewHolder(ViewHolder holder, int position) {
        final AvailableProviderModel model = arrServer.get(position);
        holder.tvMsgListTitle.setText(model.getInfoServer().getName());
        holder.tvMsgListMsg.setText(R.string.provides_services);
        holder.tvMsgListMsg.append("\n" + model.getInfoServer().getServices());
        if (!model.getInfoServer().getPhone().isEmpty()) {
            holder.tvMsgListMsg.append("\n " + App.context.getString(R.string.t_phone) + model.getInfoServer().getPhone());
        }
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                adapterListener.onSelectItem(model);
            }
        });
    }

    public int getItemCount() {
        return arrServer.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public final TextView tvMsgListMsg;

        public final TextView tvMsgListTitle;

        public ViewHolder(View itemView) {
            super(itemView);
            tvMsgListTitle = (TextView) itemView.findViewById(R.id.tvMsgListTitle);
            tvMsgListMsg = (TextView) itemView.findViewById(R.id.tvMsgListMsg);
        }
    }
}

