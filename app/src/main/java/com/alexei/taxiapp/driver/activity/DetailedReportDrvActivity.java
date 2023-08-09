package com.alexei.taxiapp.driver.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.SelectPointInMapActivity;
import com.alexei.taxiapp.databinding.ActivityReportDrvBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.DataTaximeter;
import com.alexei.taxiapp.server.model.RouteInfoModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DetailedReportDrvActivity extends AppCompatActivity {
    private AppDatabase db;
    private ExecutorService executorService;
    private List<DataTaximeter> taximeterList;
    private FirebaseAuth auth;
    private LayoutInflater inflater;
    private Future future;
    private String currentUserId;
    private ActivityReportDrvBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityReportDrvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_detailed_report_drv);

        setRequestedOrientation(getResources().getConfiguration().orientation);
        taximeterList = new ArrayList<>();
        inflater = LayoutInflater.from(this);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы
        executorService = Executors.newFixedThreadPool(2);
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() == null) {
            finish();
        } else {
            currentUserId = auth.getCurrentUser().getUid();
            Intent intent = getIntent();
            if (intent != null) {
                long timer = intent.getLongExtra("timer", 0);
                if (timer > 0) {

                    getAllTaximeter(timer);

                } else {
                    finish();
                }
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (future != null) {
            future.cancel(true);
        }
        executorService.shutdown();
        super.onDestroy();
    }

    private void getAllTaximeter(long timer) {
//        flBlockLoadDsetVisibility(View.VISIBLE);

        future = executorService.submit(() -> {

            taximeterList = db.getTaximeterDAO().getByDateTaximeter(timer, (timer + Util.MILLISECOND_IN_DAY), currentUserId);

            for (DataTaximeter t : taximeterList) {
                if (!future.isCancelled()) {
                    addRow(taximeterList.indexOf(t) + 1,
                            t.getStartTime(),
                            t.getRoute(),
                            t.getRate().toString(),
                            t.getExecuteSecond(),
                            t.getAmount(),
                            t.getCurrency());
                }
            }

            runOnUiThread(() -> {
//                flBlockLoadDetStat.setVisibility(View.GONE);

                TableRow row = (TableRow) binding.tableStatistic.getChildAt(0);
                TextView tv = (TextView) row.findViewById(R.id.tvCellTitle);
                tv.setText(Util.formatDate.format(timer));

                double amount = taximeterList.stream().mapToDouble(t -> t.getAmount()).sum();
                String s = String.format("%.2f", amount);

                TableRow tr = (TableRow) inflater.inflate(R.layout.table_detailed_statistic_total_item, null);
                TextView t = (TextView) tr.findViewById(R.id.col);
                t.setText(getString(R.string.t_total_amound) + s);

                binding.tableStatistic.addView(tr);

            });
        });
    }

    public void addRow(long id, long timer, RouteInfoModel route, String rate, long exSec, double amount, String currency) {

        TableRow tr = (TableRow) inflater.inflate(R.layout.table_detailed_statistic_item, null);

        TextView tv = (TextView) tr.findViewById(R.id.col);
        tv.setText(Long.toString(id));

        tv = (TextView) tr.findViewById(R.id.col1);
        tv.setText(Util.formatTime.format(timer));

        Button btn = (Button) tr.findViewById(R.id.col2);
        btn.setTag(route);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(DetailedReportDrvActivity.this, SelectPointInMapActivity.class);
                intent.putExtra("route", (RouteInfoModel) view.getTag());
                startActivity(intent);
            }
        });

        tv = (TextView) tr.findViewById(R.id.col3);
        tv.setText(rate);

        tv = (TextView) tr.findViewById(R.id.col4);
        tv.setText(Util.getCountMin(exSec));

        tv = (TextView) tr.findViewById(R.id.col5);
        String s = String.format("%.2f", amount);
        tv.setText(s);

        tv = (TextView) tr.findViewById(R.id.col6);
        tv.setText(currency);

        runOnUiThread(() -> {
            binding.tableStatistic.addView(tr); //добавляем созданную строку в таблицу
        });
    }
}