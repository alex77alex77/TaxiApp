package com.alexei.taxiapp.server.exClass;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.server.model.DataOrderStatusAndDrvUidModel;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class SrvAssignDriverClass {
    private Context context = App.context;
    private InfoOrder order;
    private DatabaseReference dialRef;
    private DatabaseReference srvRef;
    private DatabaseReference orderRef;
    private ScheduledExecutorService scheduledExecutorService;//Executors.newScheduledThreadPool(1);
    private ExecutorService executorservice;// = Executors.newSingleThreadExecutor();
    private ScheduledFuture<?> scheduledFutureTimer;

    private ArrayList<InfoDriverReg> arrayListDialDrivers;//----------------------------Uid водители претенденты на выполнение заказа

    private List<InfoDriverReg> workerDrv;

    private Map<String, InfoDriverReg> mapRejectedDrivers = new HashMap<>();


    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private int waitAccumulation;
    private int waitAccept;

    private int sumPeriod = 0;

    private ScheduledFuture<?> scheduledFutureBreak;

    @Override
    protected void finalize() throws Throwable {
        try {
            recoveryResources();
        } finally {
            super.finalize();
        }

    }


    private OnListeners onListener;

    public interface OnListeners {
        void onTimeOut(DatabaseReference pathOrderRef, String driverUid);

        void onError(DatabaseReference pathOrderRef, String message);

        void onComplete(InfoOrder order, DatabaseReference pathOrderRef);
    }

    public void setOnListeners(OnListeners listener) {
        this.onListener = listener;
    }


    public SrvAssignDriverClass(InfoOrder order, DatabaseReference orderRef, int waitAccumulation, int waitingAccept, DatabaseReference srvRef) {
        this.order = order;
        this.srvRef = srvRef;
        this.orderRef = orderRef;
        this.dialRef = srvRef.child("keysO").child(order.getKeyOrder()).child("dial");

        this.waitAccumulation = waitAccumulation;
        this.waitAccept = waitingAccept;

        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        this.executorservice = Executors.newFixedThreadPool(2);
        this.arrayListDialDrivers = new ArrayList<>();
        this.workerDrv = SrvDriversObservationClass.allDrivers;//ссылка


        assignDriverTheOrder();
    }

    private void assignDriverTheOrder() {

        sumPeriod = 0;

        accumulationDriverTask();
    }


    private void accumulationDriverTask() {//сбор водителей на заказ

        Runnable task = () -> {

            sumPeriod++;

            if (arrayListDialDrivers.size() > 0) {//есть претенденты на заказ

                if (sumPeriod > waitAccumulation) {// -------------------4 sek

                    Util.removeByRefListener(dialRef, mapListeners);//здесь отключение звонилки

                    scheduledFutureTimer.cancel(true);

                    assignDriverTask(orderRef);//назначение

                }
            }

        };
        scheduledFutureTimer = scheduledExecutorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);


        ValueEventListener dialListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotDial) {

                if (snapshotDial.exists()) {

                    executorservice.submit(() -> {

                        String dialKey = snapshotDial.getValue(String.class);

                        if (dialKey != null && dialKey.length() > 0) {

                            InfoDriverReg driver = getAllowedDriverByKey(dialKey);//фильтр - получаем только своего
                            if (driver != null) {
                                arrayListDialDrivers.add(driver);//---------------собираем водителей()
                            }

                            dialRef.setValue("");//-----------------очистка поля для вхождения нового водителя
                        }
                    });
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        dialRef.addValueEventListener(dialListener);
        mapListeners.put(dialRef, dialListener);

    }

    private InfoDriverReg getAllowedDriverByKey(String dialKey) {
//дозвонившейся есть в базе, подключенный, открытая смена
        return workerDrv.stream().filter(d -> d.getDriverUid().equals(dialKey) &&
                d.getStatusToHostSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS &&
                d.getShiftStatus() == Util.SHIFT_OPEN_DRV_STATUS).findAny().orElse(null);
    }


    private InfoDriverReg getPriorityDriver() {

        sortedByPriority();
        sortedByRejectedDriver();//------------сортировка нежелательные в конец списка

        return arrayListDialDrivers.get(0);//--------------------------приоритет-первый
    }


    private void sortedByPriority() {

        Collections.sort(arrayListDialDrivers, new Comparator<InfoDriverReg>() {
            public int compare(InfoDriverReg p1, InfoDriverReg p2) {
                return Integer.compare(p1.getPriority(), p2.getPriority());
            }
        });

        Collections.reverse(arrayListDialDrivers);
    }

    private void sortedByRejectedDriver() {

        if (arrayListDialDrivers.size() > 0) {

            for (InfoDriverReg model : arrayListDialDrivers) {

                if (mapRejectedDrivers.containsKey(model.getDriverUid())) {
                    Collections.swap(arrayListDialDrivers, arrayListDialDrivers.indexOf(model), arrayListDialDrivers.size());

                }
            }
        }
    }

    private void assignDriverTask(DatabaseReference orderRef) {
        InfoDriverReg selDriver = getPriorityDriver();//----------------------получение через фильтр

        if (selDriver != null) {
            Map<String, Object> map = new HashMap<>();
            map.put("driverUid", selDriver.getDriverUid());
            map.put("status", Util.FREE_ORDER_STATUS);//-----начальный статус

            orderRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {
                        runTimeReset(selDriver);//Timer- если за период водитель не назначится то сброс
                        waitChangeStatusOrder(selDriver);//ждем когда статус поменяет водитель, когда получит событие по driverUid
                    }else {
                        recoveryResources();
                        onListener.onError(orderRef, context.getString(R.string.debug_assigned_driver));
                    }
                }
            });

        }else {
            resetAssignDriver();
        }
    }


    private void waitChangeStatusOrder(InfoDriverReg selectedDriver) {

        ValueEventListener statusListener = new ValueEventListener() { //status - отклик от водителя
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotStatus) {

                if (snapshotStatus.exists()) {

                    getDataStatusAndDriverUid(selectedDriver);
                } else {
                    onListener.onError(orderRef, context.getString(R.string.err_state_not_exists));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                onListener.onError(orderRef, error.getMessage());
            }
        };
        orderRef.child("status").addValueEventListener(statusListener);
        mapListeners.put(orderRef.child("status"), statusListener);
    }

    private void getDataStatusAndDriverUid(InfoDriverReg selectedDriver) {

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if(snapshot.exists()){
                    DataOrderStatusAndDrvUidModel dataModel = snapshot.getValue(DataOrderStatusAndDrvUidModel.class);

                    if (dataModel != null) {
                        if (dataModel.getStatus() == Util.ASSIGN_ORDER_STATUS) {//--------есть отклик от водителя -изменил статус ордера
                            if (dataModel.getDriverUid().equals(selectedDriver.getDriverUid())) {//----проверка-назначен мой водитель

                                recoveryResources();
                                onListener.onComplete(order, orderRef);
                            }
                        }
                    } else {
                        onListener.onError(orderRef, context.getString(R.string.err_state_null));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void runTimeReset(InfoDriverReg selectedDriver) {
        if (scheduledFutureBreak != null)
            scheduledFutureBreak.cancel(true);//отменяем таймер сброса для предыдущего водителя

        Runnable task = () -> {//---------------------ожидание -  нет отклика от водителя(определит статус ниже)

            onListener.onTimeOut(orderRef, selectedDriver.getDriverUid());
            mapRejectedDrivers.put(selectedDriver.getDriverUid(), selectedDriver);//1--------------список нежелательных претендентов

            resetAssignDriver();//1------удаляем чтобы заново произвести назначение водителя

        };
        scheduledFutureBreak = scheduledExecutorService.schedule(task, waitAccept, TimeUnit.SECONDS);//12 сек простоя и сброс водителя
    }


    private void resetAssignDriver() {

        if (scheduledFutureTimer != null)
            scheduledFutureTimer.cancel(true);//сброс таймера ожидания набора водителей
        if (scheduledFutureBreak != null)
            scheduledFutureBreak.cancel(true);//сброс таймера когда водитель не отвечает т.е не измененияет поле - status

        arrayListDialDrivers.clear();//сброс всех ранее дозвонившихся
        Util.removeAllValueListener(mapListeners);//удаление всех слушателей

        Map<String, Object> map = new HashMap<>();//
        map.put("/keysO/" + order.getKeyOrder() + "/dial", "");
        map.put("/freeOrders/" + order.getKeyOrder() + "/driverUid", "");
        map.put("/freeOrders/" + order.getKeyOrder() + "/status", Util.FREE_ORDER_STATUS);


        srvRef.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    assignDriverTheOrder();
                } else {
                    onListener.onError(orderRef, error.getMessage());
                }
            }
        });
    }


    public void recoveryResources() {
        if (scheduledFutureTimer != null) scheduledFutureTimer.cancel(true);
        if (scheduledFutureBreak != null) scheduledFutureBreak.cancel(true);

        mapRejectedDrivers.clear();
        arrayListDialDrivers.clear();
        executorservice.shutdown();
        scheduledExecutorService.shutdown();
        Util.removeAllValueListener(mapListeners);
    }
}
