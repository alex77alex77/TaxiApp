package com.alexei.taxiapp.server.exClass;


import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.Room;

import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.db.SettingServer;
import com.alexei.taxiapp.server.model.ShiftModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class MonitorShiftDrvClass {
    private static MonitorShiftDrvClass instance;
    private ScheduledExecutorService scheduledExecutorService;//Executors.newScheduledThreadPool(1);
    private ExecutorService executorservice;
    private AppDatabase db;

    private ScheduledFuture<?> future;

    private DatabaseReference refSrv;
    private DatabaseReference driversSrvRef;
    private SettingServer setting = ServerInformsAboutEventsClass.setting;
    private List<InfoDriverReg> drivers = SrvDriversObservationClass.allDrivers;
    private List<InfoDriverReg> workDrivers = new ArrayList<>();


    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();

    //------------------------------------------Listener
    private OnListeners onListener;

    public interface OnListeners {

        void onShiftStatusChange(InfoDriverReg driver);
    }

    public void setOnListener(OnListeners listener) {
        this.onListener = listener;
    }


    public static synchronized MonitorShiftDrvClass getInstance(Context context, DatabaseReference refSrv, String mCurrUserUid) {
        if (instance == null) {
            instance = new MonitorShiftDrvClass(context, refSrv, mCurrUserUid);
        }
        return instance;
    }

    public MonitorShiftDrvClass(Context context, DatabaseReference refSrv, String mCurrUserUid) {
        this.refSrv = refSrv;

        this.driversSrvRef = refSrv.child("driversList");

        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        executorservice = Executors.newFixedThreadPool(2);
        db = Room.databaseBuilder(context, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы


        checkEndShift(System.currentTimeMillis());
        monitoring();
    }

    //изменении статуса водителя на сервере, добавлении нового водителя - подключаем слушателя состояния смены
    public void notifyStatusDrvChange(InfoDriverReg drv) {

        if (drv.getStatusToHostSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {

            initShiftStatusListener(drv.getDriverUid(), driversSrvRef.child(drv.getDriverUid()).child("shift"));//создаем слушателя для запроса от водителя
        } else {

            Util.removeByRefListener(driversSrvRef.child(drv.getDriverUid()).child("shift"), mapListeners);
        }
    }

    public void notifyDeletedDriver(InfoDriverReg drv) {//при удалении водителя неоходимо удалить слушателя Shift
        Util.removeByRefListener(driversSrvRef.child(drv.getDriverUid()).child("shift"), mapListeners);
    }

    private void initShiftStatusListener(String drvUid, DatabaseReference shiftRef) {

        if (mapListeners.get(shiftRef) == null) {//1 обнаружено! -сылка назначена объект пересоздался когда слушатель узла сработал отправили далее переданный(старый объект)

            notifyDriverStatusShift(drvUid);//при первом обращении к водителю информируем водителя о состоянии смены

            ValueEventListener eventListener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        Integer action = snapshot.getValue(Integer.class);
                        if (action != null) {
                            snapshot.getRef().removeValue();//удаляем, чтобы еще при запросе от водителя слушатель сработал

                            //получаем и передаем текущий объект водителя для этого узла - это решение проблемы 1
                            drivers.stream().filter(d -> d.getDriverUid().equals(drvUid)).findAny().ifPresent(d -> {
                                if (d.getShiftStatus() != action) {//смена меняется
                                    executorservice.submit(() -> handlerActionShiftListener(d, action));
                                }
                            });
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            shiftRef.addValueEventListener(eventListener);
            mapListeners.put(shiftRef, eventListener);
        }
    }

    private void handlerActionShiftListener(InfoDriverReg drv, int action) {//запрос от водителя
        switch (action) {
            case Util.ACTION_OPEN_SHIFT:
                int result = permissionOpenShift(drv);

                if (result == Util.ALLOWED_OPEN_SHIFT) {

                    notifyDrvStatusShiftRef(drv, Util.SHIFT_OPEN_DRV_STATUS);//назначаем статус смены водителя
                } else {

                    returnResultStatusShift(drv, result);//ответ водителю на запрос
                }
                break;
            case Util.ACTION_CLOSE_SHIFT:

                notifyDrvStatusShiftRef(drv, Util.SHIFT_CLOSE_DRV_STATUS);//назначаем статус смены водителя
                break;
            case Util.ACTION_GET_NOTIFY_SHIFT:

                notifyDriverStatusShift(drv.getDriverUid());
                break;
        }
    }

    private void returnResultStatusShift(InfoDriverReg drv, int result) {
        refSrv.child("driversList").child(drv.getDriverUid()).child("ws").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child("timer").exists()) {

                    refSrv.child("driversList").child(drv.getDriverUid()).child("ws/status").setValue(result);//возвращаем результат состояние смены где время остается прежним
                } else {

                    ShiftModel model = new ShiftModel(result);//возвращаем результат состояние смены с новым временем
                    refSrv.child("driversList").child(drv.getDriverUid()).child("ws").setValue(model);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void notifyDriverStatusShift(String drvUid) {
        drivers.stream().filter(d -> d.getDriverUid().equals(drvUid)).findAny().ifPresent(d -> {

            if (d.getShiftStatus() == Util.SHIFT_OPEN_DRV_STATUS) {

                refSrv.child("driversList").child(d.getDriverUid()).child("ws").setValue(new ShiftModel(d.getOpenShiftTime(), d.getShiftStatus()));
            } else {
                refSrv.child("driversList").child(d.getDriverUid()).child("ws").setValue(new ShiftModel(d.getCloseShiftTime(), d.getShiftStatus()));
            }
        });
    }

    private int permissionOpenShift(InfoDriverReg drv) {

        //если смена закрыта
        if (drv.getShiftStatus() != Util.SHIFT_OPEN_DRV_STATUS) {//включаются все состояния кроме -Util.SHIFT_OPEN_DRV_STATUS
            //водитель к системе подключен
            if (drv.getStatusToHostSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {
                //средств достаточно ли проверяем...
                if ((drv.getBalance() - setting.getRateShift()) >= 0) {
                    return Util.ALLOWED_OPEN_SHIFT;
                } else {
                    return Util.SHIFT_INSUFFICIENT_FUNDS_DRV_STATUS;
                }

            } else {
                return Util.SHIFT_THE_SYSTEM_IS_DENIED_STATUS;
            }
        } else {
            return Util.SHIFT_OPEN_DRV_STATUS;
        }

    }

    private void monitoring() {
        Runnable task = () -> {

            checkEndShift(System.currentTimeMillis());

        };
        future = scheduledExecutorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.MINUTES);
    }

    private void checkEndShift(long timer) {
        workDrivers = drivers.stream().filter(d -> d.getShiftStatus() == Util.SHIFT_OPEN_DRV_STATUS && d.getFinishTimeShift() > 0).collect(Collectors.toList());

        workDrivers.forEach(d -> {
            if (timer >= d.getFinishTimeShift()) {
                notifyDrvStatusShiftRef(d, Util.SHIFT_CLOSE_DRV_STATUS);//1

            }
        });
    }


    private void handlerChangeStatusShift(InfoDriverReg d, int status) {//назначение статус смены водителя

        long t = System.currentTimeMillis();

        if (status == Util.SHIFT_OPEN_DRV_STATUS) {
            d.setBalance(d.getBalance() - setting.getRateShift());//----------снимаем ставку
            long ms = TimeUnit.HOURS.toMillis(setting.getHoursCount());

            d.setFinishTimeShift(t + ms);//-----------------------назначаем время отключения
            d.setOpenShiftTime(t);//------------------сохраняем для отчета
        } else {

            d.setCloseShiftTime(t);
        }

        d.setShiftStatus(status);
        onListener.onShiftStatusChange(d);

        int id = db.getDataDriversServerDAO().updateDataDriverServer(d);

    }


    private void notifyDrvStatusShiftRef(InfoDriverReg d, int status) {//информируем водителя

        ShiftModel model = new ShiftModel(status);

        refSrv.child("driversList").child(d.getDriverUid()).child("ws").setValue(model, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    handlerChangeStatusShift(d, status);
                }
            }
        });
    }

    public void recoverResources() {
        Util.removeAllValueListener(mapListeners);
        if (future != null) {
            future.cancel(true);
            scheduledExecutorService.shutdown();
        }
    }

    @Override
    protected void finalize() throws Throwable {
        recoverResources();
        super.finalize();
    }
}
