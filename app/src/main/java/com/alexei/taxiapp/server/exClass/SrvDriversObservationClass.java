package com.alexei.taxiapp.server.exClass;

import android.content.Context;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.driver.model.ExOrderModel;
import com.alexei.taxiapp.driver.model.RedirectOrderModel;
import com.alexei.taxiapp.driver.model.SOSModel;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.server.model.MsgModel;
import com.alexei.taxiapp.server.model.ShiftModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class SrvDriversObservationClass {
    private Context context = App.context;
    private static SrvDriversObservationClass instance;

    private FirebaseDatabase database;
    private FirebaseUser currentUser;
    private AppDatabase db;
    private ExecutorService executorservice;
    private ServerInformsAboutEventsClass eventsClass;
    public MonitorShiftDrvClass shiftObservation;


    private boolean existsDataClass = false;
    private Map<DatabaseReference, ValueEventListener> mapListeners;
    private Map<DatabaseReference, ChildEventListener> mapChildListeners;

    public static List<InfoDriverReg> allDrivers = new ArrayList<>();

    private DatabaseReference refServer;
    private DatabaseReference hostSharedDriversRef;
    private DatabaseReference hostSosRef;


    //------------------------------------------Listener
    private OnUpdateListener onListener;


    public interface OnUpdateListener {

        void onChangeStatusShift(InfoDriverReg driver);

        void onSOS(InfoDriverReg driver);

        void onChangeDrvStatusToSrv(InfoDriverReg driver);

        void onUpdateLocation(InfoDriverReg driver);

        void onMsgForSrv(InfoDriverReg driver);

        void onAssignedOrder(InfoDriverReg driver);

        void onExOrder(InfoDriverReg driver);

        void onUpdateSharedStatus(InfoDriverReg driver);

        void onRemoveHost(String key);
    }

    public void setOnListener(OnUpdateListener listener) {
        this.onListener = listener;
    }


    public SrvDriversObservationClass(DatabaseReference refServer, FirebaseUser currentUser) {
        this.refServer = refServer;
        this.currentUser = currentUser;

        db = Room.databaseBuilder(context, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы
        executorservice = Executors.newFixedThreadPool(2);
        eventsClass = ServerInformsAboutEventsClass.getInstance();

        mapListeners = new HashMap<>();
        mapChildListeners = new HashMap<>();

        database = FirebaseDatabase.getInstance();
        hostSharedDriversRef = database.getReference().child("SHAREDSERVER/driversList");
        hostSosRef = database.getReference().child("sos");

        setListener();

        try {

            allDrivers.addAll(getDrivers());

            //проверяем существование водителей в хосте с водителями которые есть в базе. Если нет то создается узел с данными водителя на сервере
//            checkExistKeyDrvByHostSrv(allDrivers);
            allDrivers.forEach(d -> d.setStatusToHostSrv(Util.NOT_REF_DRIVER_STATUS_TMP));//назначаем статус по умолчанию далее (getDrvInHost) - определится реальный
            getDrvInHostSrv();//получаем всех подключеных водителей к хосту
            runMonitorShiftDrv();

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(context, "Error: SrvDriversObservationClass - "+e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setListener() {
        setOnListener(new OnUpdateListener() {
            @Override
            public void onChangeStatusShift(InfoDriverReg driver) {

            }

            @Override
            public void onSOS(InfoDriverReg driver) {
                if (driver.getSosModel() != null) {

                    eventsClass.onListener.onEvents(1);
                }
            }

            @Override
            public void onChangeDrvStatusToSrv(InfoDriverReg driver) {

            }

            @Override
            public void onUpdateLocation(InfoDriverReg driver) {

            }

            @Override
            public void onMsgForSrv(InfoDriverReg driver) {
                if (driver.getMessage().getMsg().length() > 0) {

                    eventsClass.onListener.onEvents(1);
                }
            }

            @Override
            public void onAssignedOrder(InfoDriverReg driver) {

            }

            @Override
            public void onExOrder(InfoDriverReg driver) {

            }

            @Override
            public void onUpdateSharedStatus(InfoDriverReg driver) {

            }

            @Override
            public void onRemoveHost(String key) {

            }
        });
    }


    public static synchronized SrvDriversObservationClass getInstance(DatabaseReference refServer, FirebaseUser currentUser) {
        if (instance == null) {
            instance = new SrvDriversObservationClass(refServer, currentUser);

        }
        return instance;
    }

    private List<InfoDriverReg> getDrivers() throws ExecutionException, InterruptedException {
        Callable task = () -> {
            List<InfoDriverReg> drivers = db.getDataDriversServerDAO().getAllDrivers(currentUser.getUid());

            return drivers;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (List<InfoDriverReg>) future.get();
    }

    private void runMonitorShiftDrv() {
        shiftObservation = MonitorShiftDrvClass.getInstance(context, refServer, currentUser.getUid());

        shiftObservation.setOnListener(new MonitorShiftDrvClass.OnListeners() {

            @Override
            public void onShiftStatusChange(InfoDriverReg driver) {
                handlerShiftStatus(driver);
                onListener.onChangeStatusShift(driver);
            }
        });
    }

    private void handlerShiftStatus(InfoDriverReg driver) {

    }

    private void getDrvInHostSrv() {

        if (mapChildListeners.get(refServer.child("keysD")) == null) {//сбор/подключение водителей
            ChildEventListener listener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                    executorservice.submit(() -> {
                        if (snapshot.exists()) {

                            String sKey = snapshot.getKey();
                            if (sKey != null) {

                                handlerChildAddedDriver( sKey);

                                existsDataClass = true;
                            }
                        }
                    });
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                    String key = snapshot.getKey();
                    if (key != null) {
                        executorservice.submit(() -> {
                            removeAllListenerByKey(key, mapListeners);
                            //водитель остается в списке если было удаление только узла
                            allDrivers.stream().filter(d -> d.getDriverUid().equals(key)).findAny().ifPresent(d -> {
                                d.setStatusToHostSrv(Util.NOT_REF_DRIVER_STATUS_TMP);//внутренний статус
                                shiftObservation.notifyStatusDrvChange(d);

                            });

                            onListener.onRemoveHost(key);
                        });
                    }
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            refServer.child("keysD").addChildEventListener(listener);
            mapChildListeners.put(refServer.child("keysD"), listener);
        }
    }

    private void handlerChildAddedDriver( String keyDrv) {
        executorservice.submit(() -> {

            InfoDriverReg drvInDb = db.getDataDriversServerDAO().getDriver(keyDrv, currentUser.getUid());//получаем информацыю об этом водителе

            if (drvInDb == null) {// неизвестный    (ссылка есть- данных нет)

                deleteDrvRef(refServer);

            } else {

                handlerExistsDrv(drvInDb);
            }
        });
    }

    private void handlerExistsDrv(InfoDriverReg drvInDb) {
        allDrivers.stream().filter(d -> d.getDriverUid().equals(drvInDb.getDriverUid())).findAny().ifPresent(d -> {

            refServer.child("driversList").child(d.getDriverUid()).child("status").setValue(drvInDb.getStatusToHostSrv(), new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if(error==null){

                        statusDrvOnSrvListener(d, refServer.child("driversList").child(d.getDriverUid()));//после получения статуса запускаем другие слушатели
                        msgSrvDrvOnSrvListener(d, refServer.child("driversList").child(d.getDriverUid()));//постоянно только смс
                    }
                }
            });
        });
    }

    private void deleteDrvRef(DatabaseReference ref) {
        Map<String, Object> map = new HashMap<>();
        map.put("/driversList/"+currentUser.getUid(), null);
        map.put("/keysD/"+currentUser.getUid(), null);
        ref.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error != null) {
                    Toast.makeText(context, "Error: " + error.getCode(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void initListenersDrv(InfoDriverReg driver) {

                    assignedDrvOnSrvListener(driver, refServer.child("driversList").child(driver.getDriverUid()));
                    hostDrvStatusListener(driver, hostSharedDriversRef.child(driver.getDriverUid()));
                    hostDrvLocationListener(driver, hostSharedDriversRef.child(driver.getDriverUid()));
                    hostDrvSOSListener(driver, hostSosRef.child(driver.getDriverUid()));
                    hostDrvExOrderListener(driver, hostSharedDriversRef.child(driver.getDriverUid()));


    }


    private void statusDrvOnSrvListener(InfoDriverReg driver, DatabaseReference drvOnSrvRef) {

        if (mapListeners.get(drvOnSrvRef.child("status")) == null) {
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Integer status = snapshot.getValue(Integer.class);
                        if (status != null) {
                            handlerStatusDrvOnSrv(driver, status);
                        }
                    }else {
                        handlerKillOrder(driver);
//                        handlerStatusDrvOnSrv(driver, Util.NOT_REF_DRIVER_STATUS_TMP);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            drvOnSrvRef.child("status").addValueEventListener(eventListener);
            mapListeners.put(drvOnSrvRef.child("status"), eventListener);
        }
    }

    private void handlerKillOrder(InfoDriverReg driver) {

        removeAllListenerByKey(driver.getDriverUid(), mapListeners);

        Map<String, Object> map = new HashMap<>();
        map.put("/driversList/" + driver.getDriverUid(), null);
        map.put("/keysD/" + driver.getDriverUid(), null);
        refServer.updateChildren(map);

    }

    private void handlerStatusDrvOnSrv(InfoDriverReg driver, int status) {

        driver.setStatusToHostSrv(status);

        switch (status) {
            case Util.CONNECTED_TO_SERVER_DRIVER_STATUS:

//                db.getDataDriversServerDAO().updateStatus(status, driver.getDriverUid(), currentUser.getUid());//save
                initListenersDrv(driver);
                break;
            case Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS:

//                db.getDataDriversServerDAO().updateStatus(status, driver.getDriverUid(), currentUser.getUid());//save
//                removeAllListenerByKey(driver.getDriverUid(), mapListeners);
                removeListenersDrv(driver);
                break;
            case Util.UNKNOWN_DRIVER_STATUS:

                db.getDataDriversServerDAO().deleteDriverInfo(driver.getDriverUid(), currentUser.getUid());////save - объект удаляется, с таким статусом будет присылаться запрос и объект будет создаваться заново
                removeAllListenerByKey(driver.getDriverUid(), mapListeners);
                driver = new InfoDriverReg(currentUser.getUid(), driver.getDriverUid(), Util.UNKNOWN_DRIVER_STATUS);//заглушка
                break;
//            case Util.NOT_REF_DRIVER_STATUS_TMP:
//
////                db.getDataDriversServerDAO().deleteDriverInfo(driver.getDriverUid(), currentUser.getUid());////save - объект удаляется, с таким статусом будет присылаться запрос и объект будет создаваться заново
//                removeAllListenerByKey(driver.getDriverUid(), mapListeners);
//
////                driver = new InfoDriverReg(currentUser.getUid(), driver.getDriverUid(), Util.UNKNOWN_DRIVER_STATUS);//заглушка
//                break;

        }

        db.getDataDriversServerDAO().updateStatus(status, driver.getDriverUid(), currentUser.getUid());//save

        shiftObservation.notifyStatusDrvChange(driver);
        onListener.onChangeDrvStatusToSrv(driver);
    }

    private void removeListenersDrv(InfoDriverReg driver) {

        Util.removeByRefListener(refServer.child("driversList").child(driver.getDriverUid()).child("assignedOrder"),mapListeners);
        Util.removeByRefListener(hostSharedDriversRef.child(driver.getDriverUid()).child("status"),mapListeners);
        Util.removeByRefListener(hostSharedDriversRef.child(driver.getDriverUid()).child("location"),mapListeners);
        Util.removeByRefListener(hostSharedDriversRef.child(driver.getDriverUid()).child("exOrder"),mapListeners);
        Util.removeByRefListener(hostSosRef.child(driver.getDriverUid()),mapListeners);

    }


//    private InfoDriverReg getDriverFromDB(String key) throws ExecutionException, InterruptedException {
//
//        Callable task = () -> db.getDataDriversServerDAO().getDriver(key, currentUser.getUid());
//
//        FutureTask future = new FutureTask<>(task);
//        new Thread(future).start();
//
//        return (InfoDriverReg) future.get();
//    }

    private void hostDrvSOSListener(InfoDriverReg drvInDb, DatabaseReference refDrv) {
        if (mapListeners.get(refDrv) == null) {//если есть ключ есть сигнал
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        SOSModel sosModel = snapshot.getValue(SOSModel.class);
                        if (sosModel != null) {

                            handlerSOS(drvInDb, sosModel);
                        }
                    } else {
                        handlerSOS(drvInDb, null);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            refDrv.addValueEventListener(listener);
            mapListeners.put(refDrv, listener);

        }
    }

    private void handlerSOS(InfoDriverReg drvInDb, SOSModel value) {
        drvInDb.setSosModel(value);
        onListener.onSOS(drvInDb);
    }

    private void hostDrvExOrderListener(InfoDriverReg driver, DatabaseReference hostDrvRef) {
        if (mapListeners.get(hostDrvRef.child("exOrder")) == null) {
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        ExOrderModel exOrder = snapshot.getValue(ExOrderModel.class);
                        if (exOrder != null && exOrder.getKeyOrder().length() > 0) {
                            driver.setExOrder(exOrder);
                            onListener.onExOrder(driver);
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            hostDrvRef.child("exOrder").addValueEventListener(eventListener);
            mapListeners.put(hostDrvRef.child("exOrder"), eventListener);

        }
    }

    private void assignedDrvOnSrvListener(InfoDriverReg driver, DatabaseReference refDrvInSrv) {
        if (mapListeners.get(refDrvInSrv.child("assignedOrder")) == null) {
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        RedirectOrderModel model = snapshot.getValue(RedirectOrderModel.class);
                        if (model != null) {
                            if (!model.getKeyOrder().equals("") && !model.getKeyProvider().equals("")) {

                                driver.setAssignedOrder(model);
                                onListener.onAssignedOrder(driver);
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };

            refDrvInSrv.child("assignedOrder").addValueEventListener(eventListener);
            mapListeners.put(refDrvInSrv.child("assignedOrder"), eventListener);
        }
    }


    private void msgSrvDrvOnSrvListener(InfoDriverReg driver, DatabaseReference refDrvInSrv) {
        if (mapListeners.get(refDrvInSrv.child("msgS/msg")) == null) {
            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        handlerMsgSrv(driver, refDrvInSrv.child("msgS"));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            refDrvInSrv.child("msgS/msg").addValueEventListener(eventListener);
            mapListeners.put(refDrvInSrv.child("msgS/msg"), eventListener);

        }
    }

    private void handlerMsgSrv(InfoDriverReg driver, DatabaseReference msgRef) {

        msgRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    MsgModel msg = snapshot.getValue(MsgModel.class);

                    if (msg != null) {
                        driver.setMessage(msg);
                        onListener.onMsgForSrv(driver);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void hostDrvLocationListener(InfoDriverReg driver, DatabaseReference refDrv) {

        if (mapListeners.get(refDrv.child("location")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        handlerLocation(driver, snapshot.getValue(DataLocation.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            refDrv.child("location").addValueEventListener(listener);
            mapListeners.put(refDrv.child("location"), listener);

        }
    }

    private void hostDrvStatusListener(InfoDriverReg driver, DatabaseReference refDrv) {

        if (mapListeners.get(refDrv.child("status")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        handlerStatusDrvHost(driver, snapshot.getValue(Integer.class));
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            refDrv.child("status").addValueEventListener(listener);
            mapListeners.put(refDrv.child("status"), listener);

        }
    }

    private void handlerLocation(InfoDriverReg driver, DataLocation location) {

        if (location != null) {
            driver.setLocation(location);
            onListener.onUpdateLocation(driver);
        }
    }

    private void handlerStatusDrvHost(InfoDriverReg driver, Integer status) {

        if (status != null) {
            driver.setStatusShared(status);

            onListener.onUpdateSharedStatus(driver);
        }
    }


    public boolean isExistsDataClass() {
        return existsDataClass;
    }


    public void createDrvInHostSrv(InfoDriverReg driver) {

        refServer.child("driversList").child(driver.getDriverUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (!snapshot.exists()) {//нет узла водителя, то  - создаем {status, msgS,msgD,assignedOrder,ws}

                    //слушатели на объект этого ключа назначаться после добавлении новой ссылки в SrvDriversObservation
                    Map<String, Object> map = new HashMap<>();
                    map.put("/driversList/" + driver.getDriverUid() + "/status", driver.getStatusToHostSrv());//сохраненый ранее, либо назначенный новый
                    map.put("/driversList/" + driver.getDriverUid() + "/msgS", new MsgModel());
                    map.put("/driversList/" + driver.getDriverUid() + "/msgD", new MsgModel());
                    map.put("/driversList/" + driver.getDriverUid() + "/ws", new ShiftModel(Util.SHIFT_CLOSE_DRV_STATUS));//смена закрыта
                    map.put("/keysD/" + driver.getDriverUid(), "");

                    refServer.updateChildren(map);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
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
