package com.alexei.taxiapp.driver.provider.exClass;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.db.SettingDrv;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.driver.activity.DriverMapsActivity;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.db.DataOrdersProvider;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.driver.model.RedirectOrderModel;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.alexei.taxiapp.server.model.MsgModel;
import com.alexei.taxiapp.server.model.ShiftModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ProviderClass {

    private FirebaseDatabase database;
    private String currentUserUid;
    private AppDatabase db;
    private DatabaseReference srvRef;
    private DatabaseReference hostServersRef;
    private DatabaseReference drvOnSrvRef;

    private SettingDrv settingDrv = DriverMapsActivity.settingDrv;

    private String nameSrv;

    private ShiftModel shiftModel = new ShiftModel();

    private int statusDrvOnSrv;
    private int statusLocal;

    private ExecutorService executorservice;

    private MsgModel msgModel = new MsgModel();

    private Map<DatabaseReference, ChildEventListener> mapChildListeners = new HashMap<>();
    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private Map<DatabaseReference, ValueEventListener> mapAssignedOrdersListeners = new HashMap<>();

    public List<InfoOrder> orderList = new ArrayList<>();

    //-------------listeners
    private OnConnectListener onProviderListener;

    public interface OnConnectListener {

        void onShiftStatus();//DatabaseReference serverRef, ShiftModel statusModel, String mNameSrv

        void onExecutedAssignedOrder(String keyOrder);//, DatabaseReference serverRef, String name

        void onExecutedRedirectOrder(String keyOrder);

        void onKillRedirectOrder(String keyOrder);

        void onChangeFieldRedirectOrder(RedirectOrderModel model, DatabaseReference orderRef);

        void onChangeStatusOrder(String keyOrder, int status);

        void onNewOrder(InfoOrder order);//DataSnapshot snapshot, DatabaseReference serverRef, String name

        void onRemoveOrder(String keyOrder);//String snapshot, DatabaseReference serverRef, String name

        void onMsgDrv();//MsgModel msg, DatabaseReference serverRef, String name

        void onChangeLocalStatusForProvider();

        void onChangeFieldAssignedOrder(RedirectOrderModel model, DatabaseReference orderRef);//, DatabaseReference serverRef, String name

        void onChangeStatusDrvOnProvider();

    }

    public void setListener(OnConnectListener listener) {
        this.onProviderListener = listener;
    }
    //----------------------


    public ProviderClass(String currentUserUid, DatabaseReference srvRef, String name) {

        this.database = FirebaseDatabase.getInstance();
        this.hostServersRef = database.getReference().child("serverList");
        this.currentUserUid = currentUserUid;
        this.srvRef = srvRef;
        this.nameSrv = name;
        this.drvOnSrvRef = srvRef.child("driversList").child(currentUserUid);

        this.db = Room.databaseBuilder(App.context, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();

        this.executorservice = Executors.newFixedThreadPool(2);


        executorservice.submit(this::initStatusDrvInSrvListener);//слушаем статус водителя и в зависимости от статуса - запускаем/отключаем провайдер
    }


    //запуск в начале и при изменении статуса
    private void initListeners(int statusDrv) {

        DataOrdersProvider dataProvider = db.getDataOrdersProviderDAO().getProvider(srvRef.getKey(), currentUserUid);//сохраненный статус провадера(на паузе или нет)

        if (dataProvider != null) {

            statusLocal = dataProvider.getStatus();
            //проверяем сохраненые команды от водителя
            switch (statusLocal) {

                case Util.RUNNING_PROVIDER_STATUS:

                    if (statusDrvOnSrv == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {//сервер не остановлен

                        db.getDataOrdersProviderDAO().deleteDataProvider(srvRef.getKey(), currentUserUid);//------------удаляем старые действия водителя

                        if (nameSrv.equals("SHARED")) {
                            initFreeOrdersListener();//"SHARED"
                        } else {
                            initSwiftListener();// там ->initFreeOrdersListener(); assignedOrderListener();//назначенный сервером заказ
                            initFieldSrvAssignedOrderListener();//назначенный сервером заказ ВКЛЮЧЕН ПОСТОЯННО ЕСЛИ НЕ НА ПАУЗЕ
                            initFieldSrvRedirectOrderListener();//перенаправленный водителем-сервером заказ ВКЛЮЧЕН ПОСТОЯННО ЕСЛИ НЕ НА ПАУЗЕ
                        }
                    }
                    break;
                case Util.PAUSE_PROVIDER_STATUS:

                    stoppedProvider();
                    break;
            }
        } else {//сохраненого состояния провайдера нет

            if (statusDrv == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {//сервер не остановлен
                if (nameSrv.equals("SHARED")) {
                    initFreeOrdersListener();//"SHARED"
                } else {
                    initSwiftListener();// там ->initFreeOrdersListener(); assignedOrderListener();//назначенный сервером заказ
                    initFieldSrvAssignedOrderListener();//назначенный сервером заказ ВКЛЮЧЕН ПОСТОЯННО ЕСЛИ НЕ НА ПАУЗЕ
                    initFieldSrvRedirectOrderListener();//перенаправленный водителем-сервером заказ ВКЛЮЧЕН ПОСТОЯННО ЕСЛИ НЕ НА ПАУЗЕ
                }
            }
        }

        initMsgDrvListener();//сообщение для меня
    }

    //слушаем поле перенаправления
    private void initFieldSrvRedirectOrderListener() {
        if (mapAssignedOrdersListeners.get(drvOnSrvRef.child("rOrder")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        RedirectOrderModel model = snapshot.getValue(RedirectOrderModel.class);
                        if (model != null) {
                            if (!model.getKeyProvider().equals("") && !model.getKeyOrder().equals("")) {
                                DatabaseReference orderRef;
                                if (model.getKeyProvider().equals("SHAREDSERVER")) {

                                    orderRef = database.getReference().child(model.getKeyProvider()).child("freeOrders").child(model.getKeyOrder());
                                } else {

                                    orderRef = database.getReference().child("serverList").child(model.getKeyProvider()).child("freeOrders").child(model.getKeyOrder());
                                }

                                monitoringAcceptOrderListener(orderRef);//слушаем его состояние удален,назначен для удаления из списка
                                onProviderListener.onChangeFieldRedirectOrder(model, orderRef);//сообщаем что пришел заказ где его поместят в список
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            drvOnSrvRef.child("rOrder").addValueEventListener(listener);
            mapAssignedOrdersListeners.put(drvOnSrvRef.child("rOrder"), listener);
        }
    }

    private void monitoringAcceptOrderListener(DatabaseReference orderRef) {

        if (mapAssignedOrdersListeners.get(orderRef.child("driverUid")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        String driverUid = snapshot.getValue(String.class);
                        if (driverUid != null) {

                            if (!driverUid.equals("")) {//назначен водитель

                                onProviderListener.onExecutedRedirectOrder(orderRef.getKey());//сообщим для удаления из списка,удаление текущего слушателя
                                Util.removeByRefListener(orderRef.child("driverUid"), mapAssignedOrdersListeners);
                            }
                        } else {

                            onProviderListener.onKillRedirectOrder(orderRef.getKey());//сообщим для удаления из списка,удаление текущего слушателя
                            Util.removeByRefListener(orderRef.child("driverUid"), mapAssignedOrdersListeners);
                        }
                    } else {

                        onProviderListener.onKillRedirectOrder(orderRef.getKey());//сообщим для удаления из списка,удаление текущего слушателя
                        Util.removeByRefListener(orderRef.child("driverUid"), mapAssignedOrdersListeners);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            orderRef.child("driverUid").addValueEventListener(listener);
            mapAssignedOrdersListeners.put(orderRef.child("driverUid"), listener);
        }
    }

    private void initSwiftListener() {

        drvOnSrvRef.child("ws").removeValue();//удаляем старый ответ
        drvOnSrvRef.child("shift").setValue(Util.ACTION_GET_NOTIFY_SHIFT);//запрашиваем состояние смены

//***************************************************************************
        if (mapListeners.get(drvOnSrvRef.child("ws")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        ShiftModel mShiftModel = snapshot.getValue(ShiftModel.class);
                        if (mShiftModel != null) {

                            switch (mShiftModel.getStatus()) {
                                case Util.SHIFT_OPEN_DRV_STATUS:

                                    //----------------------
                                    initFreeOrdersListener();
                                    //----------------------

                                    break;
                                case Util.SHIFT_CLOSE_DRV_STATUS:
                                    orderList.clear();
                                    Util.removeByRefChildListener(srvRef.child("keysO"), mapChildListeners);//parent
                                    Util.removeByHostListener(srvRef.child("freeOrders"), mapListeners);//child

                                    break;
                            }

                            if (shiftModel.getStatus() != mShiftModel.getStatus()) {//смена изменена
                                shiftModel = mShiftModel;
                                onProviderListener.onShiftStatus();//srvRef, mShiftModel, nameSrv
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            drvOnSrvRef.child("ws").addValueEventListener(listener);
            mapListeners.put(drvOnSrvRef.child("ws"), listener);
        }
    }

    private void initFieldSrvAssignedOrderListener() {

        if (mapAssignedOrdersListeners.get(drvOnSrvRef.child("assignedOrder")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        RedirectOrderModel model = snapshot.getValue(RedirectOrderModel.class);
                        if (model != null) {
                            if (!model.getKeyProvider().equals("") && !model.getKeyProvider().equals("")) {

                                DatabaseReference orderRef = hostServersRef.child(model.getKeyProvider()).child("freeOrders").child(model.getKeyOrder());
                                monitoringAcceptOrderListener(orderRef);//слушаем его состояние (удален,назначен) для удаления из списка
                                onProviderListener.onChangeFieldAssignedOrder(model, orderRef);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            drvOnSrvRef.child("assignedOrder").addValueEventListener(listener);
            mapAssignedOrdersListeners.put(drvOnSrvRef.child("assignedOrder"), listener);
        }
    }

    private void initMsgDrvListener() {
        if (mapListeners.get(drvOnSrvRef.child("msgD/msg")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        String msg = snapshot.getValue(String.class);
                        if (msg != null && msg.length() > 0) {

                            getDataMsg();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            drvOnSrvRef.child("msgD/msg").addValueEventListener(listener);
            mapListeners.put(drvOnSrvRef.child("msgD/msg"), listener);
        }
    }

    private void getDataMsg() {
        drvOnSrvRef.child("msgD").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                MsgModel msg = snapshot.getValue(MsgModel.class);
                if (msg != null) {
                    setMsgModel(msg);
                    onProviderListener.onMsgDrv();//msg, srvRef, nameSrv
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void initFreeOrdersListener() {

        if (mapChildListeners.get(srvRef.child("keysO")) == null) {
            ChildEventListener listener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {
                    if (snapshot.exists()) {
                        String keyOrder = snapshot.getKey();
                        if (keyOrder != null) {

                            initStatusOrderListener(srvRef.child("freeOrders").child(keyOrder), keyOrder);//отслеживаем его статус если будет -свободен после статуса -занят

                        }
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    orderList.stream().filter(o -> o.getKeyOrder().equals(snapshot.getKey())).findAny().ifPresent(o -> {
                        orderList.remove(o);
                        onProviderListener.onRemoveOrder(o.getKeyOrder());
                    });
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }


            };
            srvRef.child("keysO").addChildEventListener(listener);
            mapChildListeners.put(srvRef.child("keysO"), listener);
        }
    }

    private void getOrder(DatabaseReference orderRef) {

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    InfoOrder order = snapshot.getValue(InfoOrder.class);
                    if (order != null) {

                        if (order.getStatus() == Util.FREE_ORDER_STATUS) {

                            //------------обнуление?!
                            order.setProviderName(nameSrv);
                            order.setProviderKey(srvRef.getKey());
                            order.setDriverUid("");

                            orderList.add(order);
                            onProviderListener.onNewOrder(order);//это должно быть уведомление для всех слушающих
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void initStatusOrderListener(DatabaseReference orderRef, String keyOrder) {

        if (mapListeners.get(orderRef.child("status")) == null) {

            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        Integer status = snapshot.getValue(Integer.class);
                        if (status != null) {

                            if (status == Util.FREE_ORDER_STATUS) {

                                getFromOrderLocation(orderRef);

                            } else {

                                if (status == Util.ROUTE_FINISHED_ORDER_STATUS) {

                                    Util.removeByRefListener(orderRef.child("status"), mapListeners);//remove
                                } else {

                                    onProviderListener.onChangeStatusOrder(orderRef.getKey(), status);
                                }

                                orderList.removeIf(o -> o.getKeyOrder().equals(keyOrder));
                                onProviderListener.onRemoveOrder(keyOrder);
                            }
                        }
                    } else {

                        Map<String, Object> map = new HashMap<>();
                        map.put("/freeOrders/" + keyOrder, null);
                        map.put("/keysO/" + keyOrder + "/dial", null);

                        srvRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {

//                                if (error == null) {

                                Util.removeByHostListener(orderRef, mapListeners);//remove
                                orderList.stream().filter(o -> o.getKeyOrder().equals(orderRef.getKey())).findAny().ifPresent(o -> {
                                    orderList.remove(o);
                                    onProviderListener.onRemoveOrder(o.getKeyOrder());
                                });
//                                }
                            }
                        });
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

    private void getFromOrderLocation(DatabaseReference orderRef) {

        orderRef.child("from").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    DataLocation fromLoc = snapshot.getValue(DataLocation.class);
                    if (fromLoc != null) {


                        if (BuildLocationClass.currentLocation != null) {

                            //фильтр через дистанцию( между водителем и адресатом)
                            if (calculationDistance(fromLoc.getLatitude(), fromLoc.getLongitude(), BuildLocationClass.currentLocation.getLatitude(), BuildLocationClass.currentLocation.getLongitude()) <= settingDrv.getRadius()) {

                                getOrder(orderRef);

                            }
                        }else {
                            getOrder(orderRef);
                        }

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    public void resetFreeOrders() {
        Util.removeByRefChildListener(srvRef.child("keysO"), mapChildListeners);
        Util.removeByHostListener(srvRef.child("freeOrders"), mapListeners);
        orderList.clear();

        initFreeOrdersListener();
    }

    private float calculationDistance(double fromLat, double fromLong, Double toLat, Double toLong) {
        float[] results = new float[1];
        if (fromLat != 0 && fromLong != 0 && toLat != null && toLong != null) {
            Location.distanceBetween(fromLat, fromLong, toLat, toLong, results);
            return results[0];
        }
        return -1;
    }

    //вызывается при изменении статуса сервером или водителем из адаптера
    private void handlerCmdStatusLocalProvider(int status) {

        switch (status) {
            // -----------------*- команды от пользователя -*----------------
            case Util.PAUSE_PROVIDER_STATUS:
                executorservice.submit(() -> {
                    saveLocaleStatusProvider(status);

                    if (this.statusDrvOnSrv == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {

                        stoppedProvider();

                    }
                });
                break;
            case Util.RUNNING_PROVIDER_STATUS:
                executorservice.submit(() -> {
                    saveLocaleStatusProvider(status);

                    if (this.statusDrvOnSrv == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {

                        initListeners(status);
                    }

                    onProviderListener.onChangeLocalStatusForProvider();
                });
                break;
        }
    }


    private void stoppedProvider() {
        orderList.clear();


//удаление слушателя заказов
        Util.removeByRefChildListener(srvRef.child("keysO"), mapChildListeners);//--------------пауза

//удаление всех слушателей контролирующие статус свободных заказов
        Util.removeByHostListener(srvRef.child("freeOrders"), mapListeners);

//удаление слушателя "смены" - кторый запускает остальных слушателей
        Util.removeByRefListener(drvOnSrvRef.child("ws"), mapListeners);

//удаляем всю цепочку - от слушателя поля до сопровождение статуса ордера
        Util.removeAllValueListener(mapAssignedOrdersListeners);

        onProviderListener.onChangeLocalStatusForProvider();
    }


    private void saveLocaleStatusProvider(int status) {
        DataOrdersProvider ordersProvider = db.getDataOrdersProviderDAO().getProvider(srvRef.getKey(), currentUserUid);
        if (ordersProvider == null) {
            long id = db.getDataOrdersProviderDAO().saveProvider(new DataOrdersProvider(currentUserUid, srvRef.getKey(), status));

        } else {
            ordersProvider.setStatus(status);
            db.getDataOrdersProviderDAO().updateProvider(ordersProvider);
        }
    }

    @Override
    protected void finalize() throws Throwable {
        try {

            recoverResources();

        } finally {
            super.finalize();
        }
    }

    public void recoverResources() {
        orderList.clear();
        Util.removeAllChildListener(mapChildListeners);
        Util.removeAllValueListener(mapListeners);
        Util.removeAllValueListener(mapAssignedOrdersListeners);
        if (executorservice != null) {

            executorservice.shutdownNow();
        }

    }


    @NonNull
    @Override
    public String toString() {

        if (srvRef.getKey() != null) {
            return srvRef.getKey();
        }

        return super.toString();
    }

    public ShiftModel getShiftModel() {
        return shiftModel;
    }

    public void setShiftModel(ShiftModel shiftModel) {
        this.shiftModel = shiftModel;
    }

    public MsgModel getMsgModel() {
        return msgModel;
    }

    public void setMsgModel(MsgModel msgModel) {
        this.msgModel = msgModel;
    }

    private void initStatusDrvInSrvListener() {
        //слушаем создание/удаление узла на сервере, изменение статуса водителя заблокирован/разблокирован/неизвестный
        if (mapListeners.get(drvOnSrvRef.child("status")) == null) {

            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {//  я подключен

                        Integer status = snapshot.getValue(Integer.class);
                        if (status != null) {

                            if (Objects.equals(srvRef.getKey(), "SHAREDSERVER")) {

                                shiftModel.setStatus(Util.SHIFT_OPEN_DRV_STATUS);//---------всегда открыта смена
                                shiftModel.setTimer(0);//---------всегда открыта

                                handlerStatusDrvInSrv(Util.CONNECTED_TO_SERVER_DRIVER_STATUS);//SHARED всегда подключен к серверу
                            } else {

                                handlerStatusDrvInSrv(status);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            drvOnSrvRef.child("status").addValueEventListener(listener);
            mapListeners.put(drvOnSrvRef.child("status"), listener);
        }
    }


    private void handlerStatusDrvInSrv(int status) {
        //получение, изменение статуса водителя на сервере
        statusDrvOnSrv = status;//сохраняем состояние

        switch (status) {

            case Util.CONNECTED_TO_SERVER_DRIVER_STATUS:

                executorservice.submit(() -> {

                    runServer(status);
                });
                break;
            case Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS:

                stoppedProvider();
                initMsgDrvListener();//сообщение для меня
                break;

            case Util.UNKNOWN_DRIVER_STATUS:

                stoppedProvider();
                break;

        }
        onProviderListener.onChangeStatusDrvOnProvider();
    }

    private void runServer(int status) {

        initListeners(status);

    }


    public int getStatusLocal() {
        return statusLocal;
    }

    public void setStatusLocal(int statusLocal) {
        this.statusLocal = statusLocal;

        handlerCmdStatusLocalProvider(statusLocal);
    }

    public DatabaseReference getSrvRef() {
        return srvRef;
    }

    public void setSrvRef(DatabaseReference srvRef) {
        this.srvRef = srvRef;
    }

    public String getNameSrv() {
        return nameSrv;
    }

    public void setNameSrv(String nameSrv) {
        this.nameSrv = nameSrv;
    }

    public int getStatusDrvOnSrv() {
        return statusDrvOnSrv;
    }

    public void setStatusDrvOnSrv(int statusDrvOnSrv) {
        this.statusDrvOnSrv = statusDrvOnSrv;
    }
}

