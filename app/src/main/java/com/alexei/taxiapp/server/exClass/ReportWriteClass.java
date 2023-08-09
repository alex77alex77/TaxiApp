package com.alexei.taxiapp.server.exClass;

import android.content.Context;

import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.server.model.RouteInfoModel;
import com.alexei.taxiapp.db.ServerReport;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ReportWriteClass {
    private static ReportWriteClass instance;
    public static List<ServerReport> reports = new ArrayList();
    private FirebaseAuth auth = FirebaseAuth.getInstance();
    private String callSign;
    private Context context = App.context;
    private String currUserUid;


    private AppDatabase db;

    private ExecutorService executorservice = Executors.newFixedThreadPool(2);
    private OnUpdateListener onListener;

    private long tClose;
    private long tOpen;

    public interface OnUpdateListener {
        void onError(String str);

        void onLoad();
    }

    public void setOnListeners(OnUpdateListener listener) {
        onListener = listener;
    }

    public static synchronized ReportWriteClass getInstance(Context context2) {
            if (instance == null) {
                instance = new ReportWriteClass(context2);
            }
         return instance;
    }

    public ReportWriteClass(Context context2) {
        db = Room.databaseBuilder(context2, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();
        try {
            currUserUid = auth.getCurrentUser().getUid();
            reports = getAllReport();
        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }

    private List<ServerReport> getAllReport() throws ExecutionException, InterruptedException {
        Callable task = () -> db.getReportServerDAO().getAllReport(currUserUid);

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (List<ServerReport>) future.get();
    }


    public void notifyChangeReport(InfoOrder order) {
        executorservice.submit(()->defineStatus( order));
    }


    private void defineStatus(InfoOrder order) {
        switch (order.getStatus()) {
            case Util.ASSIGN_ORDER_STATUS:
            case Util.ROUTE_FINISHED_ORDER_STATUS:
                writeReport(order);
                break;
            case Util.FREE_ORDER_STATUS:
                writeReportResetOrder(order);
                break;
        }
    }

    private void writeReportResetOrder(InfoOrder order) {
        ServerReport report = reports.stream().filter(r -> r.getKeyOrder().equals(order.getKeyOrder()) && r.getStatusOrder() != Util.ROUTE_FINISHED_ORDER_STATUS).findAny().orElse( null);
        if (report != null) {

            report.setStatusOrder(Util.DROP_ORDER_STATUS);
            db.getReportServerDAO().updateReport(report);
        }
    }


    private void writeReport(InfoOrder order) {
        ServerReport report = (ServerReport) reports.stream().filter(r -> r.getKeyOrder().equals(order.getKeyOrder())).findAny().orElse( null);
        if (report != null) {

            if (order.getStatus() == Util.ROUTE_FINISHED_ORDER_STATUS) {
                report.setTimerFinish(order.getTimeF());
                report.setStatusOrder(order.getStatus());
            } else if (order.getStatus() == Util.ASSIGN_ORDER_STATUS) {
                if (report.getStatusOrder() == Util.DROP_ORDER_STATUS) {
                    report.setTimerStart(System.currentTimeMillis());
                }
                report.setStatusOrder(order.getStatus());
            }
            db.getReportServerDAO().updateReport(report);
            return;
        }
        InfoDriverReg drvDb= db.getDataDriversServerDAO().getDriver(order.getDriverUid(), currUserUid);
        tOpen = 0;
        tClose = 0;
        callSign = "";
        if (drvDb != null) {
            callSign = "" + drvDb.getCallSign();
            if (reports.stream().noneMatch(r -> r.getTimerOpenShift() == drvDb.getOpenShiftTime())) {
                tOpen = drvDb.getOpenShiftTime();
                tClose = drvDb.getCloseShiftTime();
            }
        }
        ServerReport report2 = new ServerReport(currUserUid,
                order.getDriverUid(),
                order.getKeyOrder(),
                new RouteInfoModel(order.getFrom().getLatitude(), order.getFrom().getLongitude(), order.getTo().getLatitude(), order.getTo().getLongitude(), context.getString(R.string.where_from2), context.getString(R.string.where_to2)),
                order.getStatus(),
                System.currentTimeMillis(),
                0,
                tOpen,
                tClose);
        long id = db.getReportServerDAO().addReport(report2);
        if (id > 0) {
            report2.setId(id);
            reports.add(report2);
            return;
        }
        onListener.onError(context.getString(R.string.debug_break_report) + callSign + ")");
    }
}
