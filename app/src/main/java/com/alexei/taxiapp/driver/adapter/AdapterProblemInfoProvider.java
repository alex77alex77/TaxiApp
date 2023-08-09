package com.alexei.taxiapp.driver.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.driver.model.InfoProviderModel;

import java.util.List;

public class AdapterProblemInfoProvider extends RecyclerView.Adapter<AdapterProblemInfoProvider.MessageViewHolder> {
    private List<InfoProviderModel> infoList;

    public OnSelectListener onListener;

    public interface OnSelectListener {
        void onFix(InfoProviderModel infoProviderModel);

        void onSelectItem(InfoProviderModel infoProviderModel);
    }

    public void setOnListener(OnSelectListener listener) {
        this.onListener = listener;
    }

    public AdapterProblemInfoProvider(List<InfoProviderModel> infoList2) {
        this.infoList = infoList2;
    }

    public MessageViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new MessageViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.info_provider_item, parent, false));
    }


    public void onBindViewHolder(MessageViewHolder holder, int position) {
        final InfoProviderModel model = this.infoList.get(position);
        holder.tvTitle.setText(model.getNameSrv());
        holder.tvMessage.setText(model.getDesc());
        holder.btnFix.setEnabled(model.getState() != 3);
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AdapterProblemInfoProvider.this.onListener.onSelectItem(model);
            }
        });
        holder.btnFix.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                AdapterProblemInfoProvider.this.onListener.onFix(model);
            }
        });
    }

    public int getItemCount() {
        return this.infoList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        private Button btnFix;
        private final TextView tvMessage;
        private final TextView tvTitle;

        public MessageViewHolder(View itemView) {
            super(itemView);
            this.btnFix = (Button) itemView.findViewById(R.id.btnFix);
            this.tvTitle = (TextView) itemView.findViewById(R.id.tvNameInfoProvider);
            this.tvMessage = (TextView) itemView.findViewById(R.id.tvDescInfoProvider);
        }
    }
}

