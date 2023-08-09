package com.alexei.taxiapp.driver.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityOrderGuideBinding;
import com.alexei.taxiapp.databinding.ActivityReportDrvBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.DataTaximeter;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ReportDrvActivity extends AppCompatActivity {
    private AppDatabase db;
    private ExecutorService executorService;
    private FirebaseAuth auth;
    private String currentUserId;

    private List<DataTaximeter> taximeterList;
    private LayoutInflater inflater;
    private Future future;
    private long lDay = Util.MILLISECOND_IN_DAY;
    private  ActivityReportDrvBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportDrvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_report_drv);
        taximeterList = new ArrayList<>();

        inflater = LayoutInflater.from(this);


        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы
        executorService = Executors.newFixedThreadPool(2);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
        } else {
            currentUserId = auth.getCurrentUser().getUid();

            binding.btnCancelLoad.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (future != null) {
                        future.cancel(true);
                    }
                }
            });
            future = executorService.submit(this::getAllTaximeter);
        }
    }


    private void getAllTaximeter() {
        taximeterList = db.getTaximeterDAO().getAllTaximeter(currentUserId);
        if (taximeterList.size() > 0) {

            Comparator<DataTaximeter> comparator = Comparator.comparing(DataTaximeter::getStartTime);
            DataTaximeter firstObject = taximeterList.stream().filter(t->t.getStartTime()>0).min(comparator).get();
            DataTaximeter lastObject = taximeterList.stream().max(comparator).get();

            long i = 0;
            long timeDay = Util.MILLISECOND_IN_DAY;//(24 * 60 * 60 * 1000)
            long timeFrom = (firstObject.getStartTime() / timeDay) * timeDay;
            long timeTo = (lastObject.getStartTime() / timeDay) * timeDay;
            double totalAmount = 0;

            while (timeFrom <= timeTo) {
                if (!future.isCancelled()){
                    i++;
                    long finalTimeFrom = timeFrom;
                    long completed = taximeterList.stream().filter(t -> t.getStartTime() >= finalTimeFrom && t.getStartTime() < (finalTimeFrom + timeDay)).count();
                    long finalTimeFrom1 = timeFrom;
                    double amount = taximeterList.stream().filter(t -> t.getStartTime() >= finalTimeFrom1 && t.getStartTime() < (finalTimeFrom + timeDay)).mapToDouble(t -> t.getAmount()).sum();
                    totalAmount += amount;
                    //---------------------
                    addRow(i, timeFrom, completed, amount, 0);

                    timeFrom += timeDay;
                }
            }


            String s = String.format("%.2f", totalAmount);

            TableRow tr = (TableRow) inflater.inflate(R.layout.table_drv_statistic_total_item, null);
            TextView t = (TextView) tr.findViewById(R.id.col);
            t.setText(R.string.t_total_amound);
            t.append(s);

            runOnUiThread(() -> {
                binding.tableStatistic.addView(tr);
                binding.flBlockLoadStat.setVisibility(View.GONE);
            });

        } else {

            binding.flBlockLoadStat.setVisibility(View.GONE);
        }

    }

    public void addRow(long id, long timer, long completed, double amount, int c5) {
        TableRow tr = (TableRow) inflater.inflate(R.layout.table_drv_statistic_item, null);


        TextView tv = (TextView) tr.findViewById(R.id.col);
        tv.setText(Long.toString(id));

        tv = (TextView) tr.findViewById(R.id.col1);
        tv.setText((timer > 0) ? ((((timer / lDay) * lDay) == ((System.currentTimeMillis() / lDay) * lDay)) ? getString(R.string.today) + Util.formatTime.format(timer) : Util.formatTimeDate.format(timer)) : "-");

        tv = (TextView) tr.findViewById(R.id.col2);
        tv.setText(Long.toString(completed));

        tv = (TextView) tr.findViewById(R.id.col3);
        String s = String.format("%.2f", amount);
        tv.setText(s);

        Button btn = (Button) tr.findViewById(R.id.col4);
        btn.setTag(timer);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(ReportDrvActivity.this, DetailedReportDrvActivity.class);
                intent.putExtra("timer", (long) view.getTag());
                startActivity(intent);
            }
        });

        runOnUiThread(() -> {

            binding.tvStatusLoad.setText(String.valueOf(id));
            binding.tableStatistic.addView(tr); //добавляем созданную строку в таблицу
        });
    }

    @Override
    protected void onDestroy() {
        if (future != null) {
            future.cancel(true);
        }
        executorService.shutdown();
        super.onDestroy();
    }
}