package com.alexei.taxiapp.server.activity;

import android.app.DatePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.SelectPointInMapActivity;
import com.alexei.taxiapp.databinding.ActivityServerReportDrvBinding;
import com.alexei.taxiapp.db.ServerReport;
import com.alexei.taxiapp.server.model.RouteInfoModel;
import com.alexei.taxiapp.util.Util;

import java.util.Calendar;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

public class ServerReportDrvActivity extends AppCompatActivity {
    private static final int TIME_FROM = 1;
    private static final int TIME_TO = 2;

    private ExecutorService executorService;
    private List<ServerReport> reports;
    private LayoutInflater inflater;
    private Future future;

    private String keyDrv;
    private long total = 0;
    private long lDay = Util.MILLISECOND_IN_DAY;
    private long timeFrom = (System.currentTimeMillis() / lDay) * lDay;
    private long timeTo = timeFrom;
    private ActivityServerReportDrvBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(getResources().getConfiguration().orientation);
        binding = ActivityServerReportDrvBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_server_report_drv);
        setRequestedOrientation(getResources().getConfiguration().orientation);

        inflater = LayoutInflater.from(this);

        executorService = Executors.newFixedThreadPool(2);

        Intent intent = getIntent();
        if (intent != null) {

            reports = intent.getParcelableArrayListExtra("reports");
            keyDrv = intent.getStringExtra("keyDrv");
            if (!keyDrv.isEmpty()) {
                binding.tvDateFrom.setText(Util.formatDate.format(timeFrom));
                binding.tvDateTo.setText(Util.formatDate.format(timeTo));

                future = executorService.submit(this::defineDayReport);
            }
        }


        binding.tvDateTo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callDataPicker(TIME_TO);
            }
        });


        binding.tvDateFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                callDataPicker(TIME_FROM);
            }
        });

        binding.btnCancelLoad.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (future != null) {
                    future.cancel(true);
                }
            }
        });
    }


    private void callDataPicker(int i) {
        int mYear, mMonth, mDay, mHour, mMinute;
        // получаем текущую дату
        final Calendar cal = Calendar.getInstance();
        mYear = cal.get(Calendar.YEAR);
        mMonth = cal.get(Calendar.MONTH);
        mDay = cal.get(Calendar.DAY_OF_MONTH);

        // инициализируем диалог выбора даты текущими значениями
        DatePickerDialog datePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {
            @Override
            public void onDateSet(DatePicker view, int y, int m, int d) {
                cal.set(Calendar.YEAR, y);
                cal.set(Calendar.MONTH, m);
                cal.set(Calendar.DAY_OF_MONTH, d);

                long millSec = (cal.getTimeInMillis() / lDay) * lDay;
                if (i == TIME_FROM) {
                    timeFrom = millSec;
                    binding.tvDateFrom.setText(Util.formatDate.format(millSec));
                } else if (i == TIME_TO) {
                    timeTo = millSec;
                    binding.tvDateTo.setText(Util.formatDate.format(millSec));
                }

                defineDayReport();

            }
        }, mYear, mMonth, mDay);

        datePickerDialog.show();

    }

    private void defineDayReport() {

        long fromTime = timeFrom;// (model.getTimerStart() / lDay) * lDay;
        long toTime = timeTo;// (System.currentTimeMillis() / lDay) * lDay;

        //выборка
        createReport(reports.stream().filter(r -> r.getTimerStart() >= fromTime && r.getTimerStart() < (toTime + lDay)).collect(Collectors.toList()));

        runOnUiThread(() -> {
            binding.flBlockReportDrv.setVisibility(View.GONE);
        });

    }

    private void createReport(List<ServerReport> reportList) {

        if (reportList != null) {
            int i = 1;
            while (binding.tableSrvReportDrv.getChildCount() != 1) {
                binding.tableSrvReportDrv.removeViewAt(i);
            }

            List<ServerReport> rowsByDrv = reportList.stream().filter(r -> r.getKeyDrv().equals(keyDrv)).
                    sorted(Comparator.comparingLong(ServerReport::getTimerStart)).collect(Collectors.toList());//сортировка.reversed()

            rowsByDrv.forEach(r -> {
                if (!future.isCancelled()) {
                    total++;
                    addRow(rowsByDrv.indexOf(r) + 1, r.getTimerOpenShift(), r.getTimerStart(), r.getTimerFinish(), r.getStatusOrder(), r.getRoute());
                }
            });


            long countF = rowsByDrv.stream().filter(r -> r.getStatusOrder() == Util.ROUTE_FINISHED_ORDER_STATUS).count();
            long countR = rowsByDrv.stream().filter(r -> r.getStatusOrder() == Util.DROP_ORDER_STATUS).count();
            long countW = rowsByDrv.size();
            TableRow tr = (TableRow) inflater.inflate(R.layout.table_srv_report_drv_total_item, null);
            TextView t = (TextView) tr.findViewById(R.id.col);
            t.setText(getString(R.string.t_result));
            t.append(getString(R.string._assept_));
            t.append("" + countW);
            t.append(getString(R.string._canceled_));
            t.append("" + countR);
            t.append(getString(R.string._completed_));
            t.append("" + countF);

            runOnUiThread(() -> {
                binding.tableSrvReportDrv.addView(tr); //добавляем созданную строку в таблицу
            });
        }
    }

    public void addRow(long id, long tOpen, long tStart, long tFinish, int status, RouteInfoModel route) {
        TableRow tr = (TableRow) inflater.inflate(R.layout.table_srv_report_drv_item, null);

        TextView tv = (TextView) tr.findViewById(R.id.col);
        tv.setText("" + id);

        tv = (TextView) tr.findViewById(R.id.col0);

        tv.setText((tOpen > 0) ? Util.formatTimeDate.format(tOpen) : "");

        tv = (TextView) tr.findViewById(R.id.col1);

        tv.setText((tStart > 0) ? ((((tStart / lDay) * lDay) == ((System.currentTimeMillis() / lDay) * lDay)) ? getString(R.string.today) + Util.formatTime.format(tStart) : Util.formatTimeDate.format(tStart)) : "-");

        tv = (TextView) tr.findViewById(R.id.col2);

        tv.setText((tFinish > 0) ? ((((tFinish / lDay) * lDay) == ((System.currentTimeMillis() / lDay) * lDay)) ? getString(R.string.today) + Util.formatTime.format(tFinish) : Util.formatTimeDate.format(tFinish)) : "-");

        tv = (TextView) tr.findViewById(R.id.col3);
        tv.setText(getDescStatus(status, Util.STATUS_TO_ORDER));

        Button btn = (Button) tr.findViewById(R.id.col4);
        btn.setTag(route);

        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(ServerReportDrvActivity.this, SelectPointInMapActivity.class);
                intent.putExtra("route", (RouteInfoModel) view.getTag());
                startActivity(intent);
            }
        });

        runOnUiThread(() -> {

            binding.tvStatusLoad.setText(R.string.t_total_m);
            binding.tvStatusLoad.append("" + total);

            binding.tableSrvReportDrv.addView(tr); //добавляем созданную строку в таблицу
        });
    }

    private String getDescStatus(int status, int typeBy) {
        String description = getString(R.string.not_define);

        if (typeBy == Util.STATUS_TO_ORDER) {

            switch (status) {
                case Util.ARRIVE_ORDER_STATUS:
                    description = getString(R.string.drv_wait_m);
                    break;
                case Util.EXECUTION_ORDER_STATUS:
                    description = getString(R.string.in_progress_m);
                    break;
                case Util.ROUTE_FINISHED_ORDER_STATUS:
                    description = getString(R.string.completed_m);
                    break;
                case Util.FREE_ORDER_STATUS:
                    description = getString(R.string.status_free);
                    break;
                case Util.ASSIGN_ORDER_STATUS:
                    description = getString(R.string.assigned_m);
                    break;
                case Util.CANCEL_ORDER_STATUS:
                    description = getString(R.string.canceled_m);
                    break;
                case Util.DROP_ORDER_STATUS:
                    description = getString(R.string.drop_m);
                    break;
                case Util.WAIT_SEND_ORDER_STATUS:
                    description = getString(R.string.wait_send_m);
                    break;
                case Util.SEND_TO_DRV_ORDER_STATUS:
                    description = getString(R.string.assigned_to_drv);
                    break;
            }
        }
        return description;
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