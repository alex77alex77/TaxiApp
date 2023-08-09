package com.alexei.taxiapp.server.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.RatesSrv;

import java.util.ArrayList;

public class AdapterRatesList extends RecyclerView.Adapter<AdapterRatesList.ViewHolder> {
    private ArrayList<RatesSrv> ratesList;

    private OnSelectListener onSelectItemListener;

    public interface OnSelectListener {
        void onSelectItem(RatesSrv rate, int position);
    }

    public void setSelectListener(OnSelectListener listener) {
        this.onSelectItemListener = listener;
    }


    public AdapterRatesList(ArrayList<RatesSrv> ratesList) {
        this.ratesList = ratesList;
    }


    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.rate_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(view);

        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        RatesSrv rate = ratesList.get(position);
        if (rate != null) {

            holder.tvType.setText(rate.getTitle());
            holder.tvKm.setText(String.valueOf(rate.getKm()));
            holder.tvMin.setText(String.valueOf(rate.getMin()));
            holder.tvDefWait.setText(String.valueOf(rate.getDefWait()));
            holder.tvFixedAmount.setText(String.valueOf(rate.getFixedAmount()));
            holder.tvHourlyRate.setText(String.valueOf(rate.getHourlyRate()));

            holder.itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    onSelectItemListener.onSelectItem(rate, holder.getAbsoluteAdapterPosition());

                }
            });
        }
    }


    @Override
    public int getItemCount() {
        return ratesList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvType;
        private final TextView tvKm;
        private final TextView tvMin;

        private final TextView tvDefWait;
        private final TextView tvFixedAmount;
        private final TextView tvHourlyRate;


        public ViewHolder(@NonNull View itemView) {
            super(itemView);//----Возвращает внешний вид из файла макета, связанном с привязкой.

            this.tvType = itemView.findViewById(R.id.tvDlgTitle);
            this.tvKm = itemView.findViewById(R.id.tvKm);
            this.tvMin = itemView.findViewById(R.id.tvMin);

            this.tvDefWait = itemView.findViewById(R.id.tvDefWait);
            this.tvFixedAmount = itemView.findViewById(R.id.tvFixedAmount);
            this.tvHourlyRate = itemView.findViewById(R.id.tvHourlyRate);

        }
    }


}
