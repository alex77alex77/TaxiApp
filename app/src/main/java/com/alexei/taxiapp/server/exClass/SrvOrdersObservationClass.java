package com.alexei.taxiapp.server.exClass;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.db.DataBlockClient;
import com.alexei.taxiapp.server.model.MsgModel;
import com.alexei.taxiapp.db.SettingServer;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SrvOrdersObservationClass {
    private Context context = App.context;
    private AppDatabase db;
    private ReportWriteClass reportWriteClass;
    private FirebaseUser currentUser;
    private ServerInformsAboutEventsClass eventsClass;
    private ExecutorService executorservice;
    private static SrvOrdersObservationClass instance;

    private Map<DatabaseReference, ValueEventListener> mapListeners;
    private Map<DatabaseReference, ChildEventListener> mapChildListeners;
    private List<SrvAssignDriverClass> assignDrvList = new ArrayList<>();

    private SettingServer setting = ServerInformsAboutEventsClass.setting;

    public static ArrayList<InfoOrder> ordersList = new ArrayList<>();

    private DatabaseReference refServer;


    public ArrayList<InfoOrder> getOrdersList() {
        return ordersList;
    }

    public void cancelOrder(InfoOrder order) {

        order.setDriverUid("");
        order.setStatus(Util.WAIT_SEND_ORDER_STATUS);

        Map<String, Object> map = new HashMap<>();
        map.put("/freeOrders/" + order.getKeyOrder(), null);
        map.put("/keysO/" + order.getKeyOrder() + "/dial", null);
        refServer.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {

                }
            }
        });
    }

    public void deleteOrder(InfoOrder order) {
        Map<String, Object> map = new HashMap<>();
        map.put("/freeOrders/" + order.getKeyOrder(), null);
        map.put("/keysO/" + order.getKeyOrder() + "/dial", null);

        refServer.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    ordersList.remove(order);
                    onUpdateListener.onRemove();
                }
            }
        });
    }

    public void clearCompletedOrders() {
        ordersList.forEach(o -> {
            if (o.getStatus() == Util.ROUTE_FINISHED_ORDER_STATUS) {
                deleteOrder(o);
            }
        });
    }

    //------------------------------------------Listener
    private OnUpdateListener onUpdateListener;

    public interface OnUpdateListener {

        void onWaitTimeOut(InfoOrder order, DatabaseReference orderRef, String driverUid);

        void onMsgForSrvChange(InfoOrder order, DatabaseReference orderRef);

        void onChangeStatus(InfoOrder order);

        void onUpdateDriverUid(InfoOrder order, DatabaseReference orderRef);

        void onTimerRouteFinish(InfoOrder order, DatabaseReference orderRef, long timer);

        void onRemove();
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        this.onUpdateListener = listener;
    }


    //--------------------------


    public SrvOrdersObservationClass(DatabaseReference refServer, FirebaseUser currentUser) {
        this.refServer = refServer;
        this.currentUser = currentUser;

        this.mapListeners = new HashMap<>();
        this.mapChildListeners = new HashMap<>();

        this.db = Room.databaseBuilder(App.context, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы
        this.executorservice = Executors.newFixedThreadPool(2);
        this.eventsClass = ServerInformsAboutEventsClass.getInstance();

        setListeners();
        runWriteReportClass();

        getOrdersOnSrv();

    }

    private void setListeners() {
        setOnUpdateListener(new OnUpdateListener() {
            @Override
            public void onWaitTimeOut(InfoOrder order, DatabaseReference orderRef, String driverUid) {

            }

            @Override
            public void onMsgForSrvChange(InfoOrder order, DatabaseReference orderRef) {
                if (order.getMsgS().getMsg().length() > 0) {

                    eventsClass.onListener.onEvents(1);
                }
            }

            @Override
            public void onChangeStatus(InfoOrder order) {

            }

            @Override
            public void onUpdateDriverUid(InfoOrder order, DatabaseReference orderRef) {

            }

            @Override
            public void onTimerRouteFinish(InfoOrder order, DatabaseReference orderRef, long timer) {

            }

            @Override
            public void onRemove() {

            }
        });
    }


    private void runWriteReportClass() {

        reportWriteClass = ReportWriteClass.getInstance(context);//отчет

        reportWriteClass.setOnListeners(new ReportWriteClass.OnUpdateListener() {
            @Override
            public void onError(String desc) {
                Toast.makeText(context, desc, Toast.LENGTH_LONG).show();
            }

            @Override
            public void onLoad() {

            }
        });
    }

    public static synchronized SrvOrdersObservationClass getInstance(DatabaseReference refServer, FirebaseUser currentUser) {
        if (instance == null) {
            instance = new SrvOrdersObservationClass(refServer, currentUser);

        }
        return instance;
    }


    private void getOrdersOnSrv() {
        if (mapChildListeners.get(refServer.child("keysO")) == null) {
            ChildEventListener listener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                    if (snapshot.exists()) {
                        getOrderRef(snapshot.getKey());
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    String key = snapshot.getKey();
                    if (key != null) {
                        removeAllListenerByKey(key, mapListeners);

                        InfoOrder order=ordersList.stream().filter(o->Objects.equals(o.getKeyOrder(), key)).findAny().orElse(null);
                        if (order!=null){
                            if(!order.getClientUid().isEmpty() && order.getStatus()!= Util.ROUTE_FINISHED_ORDER_STATUS){//если не завершен и заказ клиента

                                order.setStatus(Util.KILL_ORDER_STATUS);
                                onUpdateListener.onChangeStatus(order);
                            }else if(order.getStatus()!= Util.WAIT_SEND_ORDER_STATUS){//если не отменяется(увод в буфер) сервером

                                ordersList.remove(order);
                            }
                        }

                        onUpdateListener.onRemove();
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            refServer.child("keysO").addChildEventListener(listener);
            mapChildListeners.put(refServer.child("keysO"), listener);
        }
    }

    private void getOrderRef(String key) {
        refServer.child("freeOrders").child(key).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    InfoOrder order = snapshot.getValue(InfoOrder.class);
                    if (order != null) {

                        executorservice.submit(() -> handlerGetOrderRef(order, snapshot.getRef()));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void handlerGetOrderRef(InfoOrder order, DatabaseReference ref) {

        ordersList.removeIf(o -> Objects.equals(o.getKeyOrder(), order.getKeyOrder()));
        ordersList.add(order);

        executorservice.submit(() -> {
            if (!order.getClientUid().isEmpty()) {
                getClientNameInDb(order);
            }

            createStatusListener(order, ref);
            createMsgForSrvListener(order, ref);
            createDrvUidListener(order, ref);
            createTimeFinishListener(order, ref);
        });
    }


    private void getClientNameInDb(InfoOrder order) {
        DataBlockClient client = db.getBlockClientDAO().getKeyBlock(order.getClientUid(), currentUser.getUid());
        if (client != null) {
            order.setClientName(" - " + client.getName());
        }
    }

    private void createTimeFinishListener(InfoOrder order, DatabaseReference orderRef) {
        if (mapListeners.get(orderRef.child("timeF")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        handlerTF(order, orderRef, snapshot.getValue(Long.class));

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            orderRef.child("timeF").addValueEventListener(listener);
            mapListeners.put(orderRef.child("timeF"), listener);
        }
    }

    private void handlerTF(InfoOrder order, DatabaseReference orderRef, Long timer) {
        if (timer != null) {
            order.setTimeF(timer);
            onUpdateListener.onTimerRouteFinish(order, orderRef, timer);

        }
    }


    private void createDrvUidListener(InfoOrder order, DatabaseReference orderRef) {//назначение водителя
        if (mapListeners.get(orderRef.child("driverUid")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        handlerDrvUid(order, orderRef, snapshot.getValue(String.class));

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            orderRef.child("driverUid").addValueEventListener(listener);
            mapListeners.put(orderRef.child("driverUid"), listener);
        }
    }

    private void createMsgForSrvListener(InfoOrder order, DatabaseReference orderRef) {
        if (mapListeners.get(orderRef.child("msgS")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        MsgModel msg = snapshot.getValue(MsgModel.class);

                        if (msg != null) {

                            order.setMsgS(msg);
                            onUpdateListener.onMsgForSrvChange(order, orderRef);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            orderRef.child("msgS").addValueEventListener(listener);
            mapListeners.put(orderRef.child("msgS"), listener);
        }
    }

    private void createStatusListener(InfoOrder order, DatabaseReference orderRef) {

        if (mapListeners.get(orderRef.child("status")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Integer status = snapshot.getValue(Integer.class);
                        if (status != null) {

                            handlerStatus(order, orderRef, snapshot.getValue(Integer.class));
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            orderRef.child("status").addValueEventListener(listener);
            mapListeners.put(orderRef.child("status"), listener);
        }
    }

    private void handlerDrvUid(InfoOrder order, DatabaseReference orderRef, String drvUid) {
        if (drvUid != null) {
            order.setDriverUid(drvUid);
            onUpdateListener.onUpdateDriverUid(order, orderRef);

        }
    }

    private void handlerStatus(InfoOrder order, DatabaseReference orderRef, Integer status) {

        order.setStatus(status);


        switch (status) {
            case Util.CANCEL_ORDER_STATUS:

                if (orderRef.getKey() != null) {
                    removeAllListenerByKey(orderRef.getKey(), mapListeners);
                }
                break;
            case Util.ROUTE_FINISHED_ORDER_STATUS:

                if (orderRef.getKey() != null) {
                    removeAllListenerByKey(orderRef.getKey(), mapListeners);
                }
                reportWriteClass.notifyChangeReport(order);//----finish
                break;
            case Util.FREE_ORDER_STATUS://---------------------отправлен в свободные

                reportWriteClass.notifyChangeReport(order);//-------------сброшен
                initAssignDriver(order, orderRef);//слушаем звонящего на заказ водителей и назначаем
                break;
            case Util.ASSIGN_ORDER_STATUS:

                reportWriteClass.notifyChangeReport(order); //------------start
                break;
        }
        onUpdateListener.onChangeStatus(order);
    }

    //    ***************************************************          НАЗНАЧЕНИЕ ЗВОНЯЩЕГО ВОДИТЕЛЯ НА ЗАКАЗ
//                                                                  КЛАСС ЗАПУСКАЕТСЯ И ЖДЕТ ВОДИТЕЛЕЙ
    private void initAssignDriver(InfoOrder order, DatabaseReference orderRef) {
        executorservice.submit(() -> {

            if (currentUser != null) {

                SrvAssignDriverClass assignDrv = new SrvAssignDriverClass(order, orderRef, setting.getWaitAccumulation(), setting.getWaitAccept(), refServer);//КЛАСС слушает поле dialAssignOrder и НАЗНАЧАЕТ ВОДИТЕЛЯ
                assignDrvList.add(assignDrv);

                assignDrv.setOnListeners(new SrvAssignDriverClass.OnListeners() {
                    @Override
                    public void onTimeOut(DatabaseReference pathOrderRef, String driverUid) {

                        onUpdateListener.onWaitTimeOut(order, orderRef, driverUid);
                    }

                    @Override
                    public void onError(DatabaseReference pathOrderRef, String message) {
                        assignDrv.recoveryResources();
                        assignDrvList.remove(assignDrv);
                    }

                    @Override
                    public void onComplete(InfoOrder order, DatabaseReference pathOrderRef) {
                        assignDrv.recoveryResources();
                        assignDrvList.remove(assignDrv);
                    }
                });
            }
        });
    }


    public void recoveryResources() {
        Util.removeAllChildListener(mapChildListeners);
        Util.removeAllValueListener(mapListeners);

        assignDrvList.clear();
    }

    private void removeAllListenerByKey(String keyObj, Map<DatabaseReference, ValueEventListener> listeners) {
        if (!keyObj.isEmpty()) {

            for (Map.Entry<DatabaseReference, ValueEventListener> entry : listeners.entrySet()) {
                String[] s = entry.getKey().toString().split("/");
                if (Arrays.asList(s).contains(keyObj)) {
                    if (entry.getValue() != null) {
                        entry.getKey().removeEventListener(entry.getValue());
                        entry.setValue(null);
                    }
                }
            }
        }
    }
}
