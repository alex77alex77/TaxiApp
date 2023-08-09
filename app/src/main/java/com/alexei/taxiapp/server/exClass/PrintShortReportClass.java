package com.alexei.taxiapp.server.exClass;

import android.content.Context;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.util.Util;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PrintShortReportClass {
    private Context context;

    private static PrintShortReportClass instance;
    private ExecutorService executorservice;

    private List<InfoOrder> orders;
    private List<InfoDriverReg> drivers;

    public static boolean stopExecute = false;
    //------------------------------------------Listener
    private OnListener onListener;

    public interface OnListener {
        void onChangeReport(String report, int type);
    }

    public void setOnListeners(OnListener listener) {
        this.onListener = listener;
    }

    public static synchronized PrintShortReportClass getInstance() {
        if (instance == null) {
            instance = new PrintShortReportClass();

        }
        return instance;
    }

    public PrintShortReportClass() {
        executorservice = Executors.newFixedThreadPool(2);
        this.orders = SrvOrdersObservationClass.ordersList;//ссылка
        this.drivers = SrvDriversObservationClass.allDrivers;//ссылка

        context = App.context;

    }


    public void notifyChangeReport(int type) {

        if (!stopExecute) {//не остановлен

            executorservice.submit(() -> {

                String str = "";

                if (type == 1) {
                    str += context.getString(R.string.t_total_m) + (drivers.size());

                    str += context.getString(R.string.t_free) + (drivers.stream()
                            .filter(d -> d.getStatusShared() == Util.FREE_DRIVER_STATUS &&
                                    d.getStatusToHostSrv() != Util.NOT_REF_DRIVER_STATUS_TMP &&
                                    d.getStatusToHostSrv() != Util.UNKNOWN_DRIVER_STATUS &&
                                    d.getStatusToHostSrv() != Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS)
                            .count());

                    str += context.getString(R.string.t_busy) + (drivers.stream()
                            .filter(d -> d.getStatusShared() == Util.BUSY_DRIVER_STATUS &&
                                    d.getStatusToHostSrv() != Util.NOT_REF_DRIVER_STATUS_TMP &&
                                    d.getStatusToHostSrv() != Util.UNKNOWN_DRIVER_STATUS &&
                                    d.getStatusToHostSrv() != Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS)
                            .count());

                    str += context.getString(R.string.t_help) + (drivers.stream()
                            .filter(d -> d.getSosModel() != null)
                            .count());

                    str += context.getString(R.string.t_unknown) + (drivers.stream()
                            .filter(d -> d.getStatusToHostSrv() == Util.UNKNOWN_DRIVER_STATUS)
                            .count());

                    str += context.getString(R.string.no_connected) + (drivers.stream()
                            .filter(d -> d.getStatusToHostSrv() == Util.NOT_REF_DRIVER_STATUS_TMP)
                            .count());

                    str += context.getString(R.string.t_blocked) + (drivers.stream()
                            .filter(d -> d.getStatusToHostSrv() == Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS) //|| d.getStatusToHostSrv() == Util.NOT_HOST_TRANSPORT_DRIVER_STATUS_TMP
                            .count());

                }

                if (type == 2) {

                    str += context.getString(R.string.t_total_m) + (orders.size());

                    str += context.getString(R.string.t_free) + (orders.stream()
                            .filter(o -> o.getStatus() == Util.FREE_ORDER_STATUS)
                            .count());

                    str += context.getString(R.string.t_completed) + (orders.stream()
                            .filter(o -> o.getStatus() == Util.ROUTE_FINISHED_ORDER_STATUS)
                            .count());

                    str += context.getString(R.string.t_prepared) + (orders.stream()
                            .filter(o -> o.getStatus() == Util.WAIT_SEND_ORDER_STATUS)
                            .count());

                    str += context.getString(R.string.t_in_progress) + (orders.stream()
                            .filter(o -> o.getStatus() != Util.FREE_ORDER_STATUS &&
                                    o.getStatus() != Util.ROUTE_FINISHED_ORDER_STATUS &&
                                    o.getStatus() != Util.WAIT_SEND_ORDER_STATUS &&
                                    o.getStatus() != Util.KILL_ORDER_STATUS)
                            .count());

                }

                onListener.onChangeReport(str, type);
            });
        }
    }

}
