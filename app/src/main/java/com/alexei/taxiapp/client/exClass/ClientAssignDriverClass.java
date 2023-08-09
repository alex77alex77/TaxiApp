package com.alexei.taxiapp.client.exClass;

import androidx.annotation.NonNull;

import com.alexei.taxiapp.client.model.KeysOFieldsModel;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class ClientAssignDriverClass {
    private String keyOrder;
    private InfoOrder infoOrder;
    private DatabaseReference dialRef;
    private DatabaseReference srvRef;
    private DatabaseReference orderRef;
    private ScheduledExecutorService scheduledExecutorService;//Executors.newScheduledThreadPool(1);
    private ExecutorService executorservice;// = Executors.newSingleThreadExecutor();
    private ScheduledFuture<?> scheduledFutureAccumTimer;
    private ScheduledFuture<?> scheduledFutureBreak;
    private ArrayList<String> rejectedDrivers;//----------------------------Uid водители не претенденты на выполнение заказа
    private ArrayList<String> dialAssignDrivers;//----------------------------Uid водители не претенденты на выполнение заказа

    private final Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();

    private int sumPeriod = 0;
    private boolean blnExecutor = false;// контроль для выполнения


    private OnCompleteAssignDriverListener onCompleteListener;

    public interface OnCompleteAssignDriverListener {
        void onComplete(DatabaseReference pathOrderRef, String drvSelKey, boolean success);
        void onDeletedOrder(DatabaseReference orderRef, InfoOrder order);
    }

    public void setOnListener(OnCompleteAssignDriverListener listener) {
        this.onCompleteListener = listener;
    }


    public ClientAssignDriverClass(DatabaseReference orderRef, DatabaseReference srvRef, String keyOrder, InfoOrder order) {
        this.infoOrder = order;
        this.keyOrder = keyOrder;
        this.srvRef = srvRef;
        this.orderRef = orderRef;
        this.dialRef = srvRef.child("keysO").child(keyOrder).child("dial");
        this.scheduledExecutorService = Executors.newScheduledThreadPool(1);
        this.executorservice = Executors.newFixedThreadPool(2);//.newSingleThreadExecutor();
        this.rejectedDrivers = new ArrayList<>();
        this.dialAssignDrivers = new ArrayList<>();

        executorservice.submit(() -> {

            accumulationDriveTask();
        });

    }


    private void accumulationDriveTask() {
        blnExecutor = true;
        sumPeriod = 0;

        Runnable task = () -> {

            if (blnExecutor) {
                sumPeriod++;

                if (dialAssignDrivers.size() > 0) {

                    if (sumPeriod > 2) {//--------------------3 sek

                        scheduledFutureAccumTimer.cancel(true);
                        blnExecutor = false;
                        assignDriverTask(orderRef);
                    }

                }
            }
        };
        scheduledFutureAccumTimer = scheduledExecutorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);


        ValueEventListener dialListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotDial) {
                if (snapshotDial.exists()) {

                    executorservice.submit(() -> {
                        String key = snapshotDial.getValue(String.class);
                        if (key != null) {
                            if (key.length() > 1) {
                                if (blnExecutor) {
                                    if (!dialAssignDrivers.contains(key)) {
                                        dialAssignDrivers.add(key);//---------------собираем водителей()
                                        snapshotDial.getRef().setValue("");//-----------------очистка поля для вхождения нового водителя
                                    }
                                }
                            }
                        }
                    });

                } else {//нарушена структура

                    killRef("");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        dialRef.addValueEventListener(dialListener);
        mapListeners.put(dialRef, dialListener);

        //----------------инициирование события- добавления ордера если нет водителей

    }


    private String getPriorityDriver() {
//------------нужна сортировка нежелательные в конец списка

        for (int i = 0; i < dialAssignDrivers.size(); i++) {
            String element = dialAssignDrivers.get(i);
            if (rejectedDrivers.contains(element)) {
                Collections.rotate(dialAssignDrivers.subList(i, dialAssignDrivers.size()), -1);
            }
        }

        return dialAssignDrivers.get(0);//--------------------------приоритет-первый
    }

    public DatabaseReference getOrderRef() {
        return orderRef;
    }

    private void assignDriverTask(DatabaseReference orderRef) {

        ValueEventListener driverUidEventListener = new ValueEventListener() {//child("driverUid")
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshotDrvUid) {

                executorservice.submit(() -> {

                    if (snapshotDrvUid.exists()) {

//---------------------------------
                        String selectedDriver = getPriorityDriver();//-----------------------фильтрование-отбор относительно ордера
//---------------------------------

                        String drvUid = snapshotDrvUid.getValue(String.class);
                        if (drvUid != null) {
                            if (drvUid.length() == 0) {//-------------------------------------не назначен
                                if (selectedDriver != null) {
//                                    -------назначение
                                    snapshotDrvUid.getRef().setValue(selectedDriver, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                            if (error == null) {
                                                //-------------------------
                                                Runnable task = () -> {//------------------------ожидание- отклика нет от водителя(определит статус)
                                                    rejectedDrivers.add(selectedDriver);//1------список нежелательных претендентов
                                                    resetAssignDriver(orderRef);//----------удаляем чтобы заново произвести назначение водителя------нужно уведомить водителя!!!!!
                                                };
                                                scheduledFutureBreak = scheduledExecutorService.schedule(task, 15, TimeUnit.SECONDS);//15 сек простоя и сброс водителя
                                                //-------------------------
                                                //ждем отклика от водителя который изменит статус заказа
                                                ValueEventListener statusListener = new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot snapshotStatus) {

                                                        if (snapshotStatus.exists()) {
                                                            Integer status = snapshotStatus.getValue(Integer.class);
                                                            if (status != null) {
                                                                if (status == Util.ASSIGN_ORDER_STATUS) {//--------------------есть отклик от водителя

                                                                    recoverResources();
                                                                    onCompleteListener.onComplete(orderRef, selectedDriver, true);
                                                                }
                                                            }
                                                        } else {//структура ордера сломана

                                                            killRef(selectedDriver);

                                                        }
                                                    }

                                                    @Override
                                                    public void onCancelled(@NonNull DatabaseError error) {

                                                        onCompleteListener.onComplete(orderRef, selectedDriver, false);
                                                    }
                                                };

                                                orderRef.child("status").addValueEventListener(statusListener);
                                                mapListeners.put(orderRef.child("status"), statusListener);

                                            } else {//структура ордера сломана

                                                killRef(selectedDriver);

                                            }
                                        }
                                    });
                                } else {

                                    resetAssignDriver(orderRef);
                                }
                            }
                        }
                    }

                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                onCompleteListener.onComplete(orderRef, "", false);
            }
        };
        orderRef.child("driverUid").addValueEventListener(driverUidEventListener);
        mapListeners.put(orderRef.child("driverUid"), driverUidEventListener);// mapListenersAssignDriver.put(orderRef.child("driverUid"), driverUidEventListener);

    }

    private void killRef(String selectedDriver) {
        Map<String, Object> map = new HashMap<>();
        map.put("/freeOrders/"+keyOrder , null);
        map.put("/keysO/"+keyOrder+"/dial", null);

        srvRef.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if(error==null){
                    infoOrder.setStatus(Util.KILL_ORDER_STATUS);
                    onCompleteListener.onDeletedOrder(orderRef,infoOrder);
                }
            }
        });
    }

    private void resetAssignDriver(DatabaseReference orderRef) {

        orderRef.child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer s = snapshot.getValue(Integer.class);

                    if (s != null) {
                        if (s != Util.ASSIGN_ORDER_STATUS) {

                            if (scheduledFutureAccumTimer != null) scheduledFutureAccumTimer.cancel(true);
                            dialAssignDrivers.clear();
                            Util.removeAllValueListener(mapListeners);

                            Map<String, Object> map = new HashMap<>();
                            map.put("/keysO/" + keyOrder, new KeysOFieldsModel(""));
                            map.put("/freeOrders/" + keyOrder + "/driverUid", "");
                            map.put("/freeOrders/" + keyOrder + "/status", Util.FREE_ORDER_STATUS);

                            srvRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                    if (error == null) {

                                        accumulationDriveTask();//собираем всех желающих
                                    } else {

                                        onCompleteListener.onComplete(orderRef, "", false);
                                    }
                                }
                            });
                        }
                    }
                } else {//структура сломана

                    killRef("");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    protected void finalize() throws Throwable {
        recoverResources();
        super.finalize();
    }


    public void recoverResources() {
        rejectedDrivers.clear();
        dialAssignDrivers.clear();
        Util.removeAllValueListener(mapListeners);

        if (scheduledFutureAccumTimer != null) scheduledFutureAccumTimer.cancel(true);
        if (scheduledFutureBreak != null) scheduledFutureBreak.cancel(true);

        executorservice.shutdown();
        scheduledExecutorService.shutdown();
    }

}

