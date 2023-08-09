package com.alexei.taxiapp.driver.exClass;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.SettingDrv;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.driver.model.DataAuto;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.driver.model.ServerModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class LoginInAppClass {
    private ExecutorService executorservice;// = Executors.newSingleThreadExecutor();
    private AppDatabase db;
    private DatabaseReference loginKRef;
    private SettingDrv settingDrv;
    private FirebaseUser user;
    private int mode;

    private OnListeners onListener;

    public interface OnListeners {
        void onSuccessful(boolean success, int mode, String res);

        void onError(int mode, String err);
    }

    public void setOnListeners(OnListeners listener) {
        this.onListener = listener;
    }


    public LoginInAppClass(DatabaseReference loginKRef, int mode, FirebaseUser user) {

        this.loginKRef = loginKRef;
        this.mode = mode;
        this.executorservice = Executors.newFixedThreadPool(2);
        this.db = Room.databaseBuilder(App.context, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();
        this.user = user;

        try {

            this.settingDrv = getSetting(user.getUid());
            executorservice.submit(() -> actionChooseMode(mode));

        } catch (Exception e) {
            e.printStackTrace();
            onListener.onError(mode, e.getMessage());
        }
    }

    private void actionChooseMode(int mode) {

        switch (mode) {
            case Util.DRIVER_MODE:
                checkLoginToSystem();//это для учета в систему с одного устройства
                break;
            case Util.PASSENGER_MODE:
                onListener.onSuccessful(true, mode, "");
                break;
        }
    }

    private void checkLoginToSystem() {

        loginKRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    executorservice.execute(() -> {
                        checkKeyDevice(snapshot.getValue(String.class));
                    });
                } else {

                    executorservice.execute(() -> {
                        createKeyDevice();
                    });
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void createKeyDevice() {

        final String key = loginKRef.push().getKey();//ключ

        if (key != null) {

            settingDrv.setLoginK(key);
            int id = db.getSettingAppDAO().updateDataSetting(settingDrv);

            if (id > 0) {
                loginKRef.setValue(key, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if (error == null) {

                            onListener.onSuccessful(true, mode, "");
                        } else {

                            onListener.onError(mode, App.context.getString(R.string.break_save_database));
                        }
                    }
                });
            } else {

                onListener.onError(mode, App.context.getString(R.string.debug_break_save_drv));
            }

        } else {
            onListener.onError(mode, App.context.getString(R.string.break_push));
        }
    }

    private void checkKeyDevice(@Nullable String val) {
        if (val != null) {//------------------------вход в firebase уже был
            if (settingDrv.getLoginK().isEmpty()) {//ключа в устройстве нет (новое устройство)

                createKeyDevice();//создаем ключ, сохраняем в firebase и в устройстве (т.е теперь вход в firebase для тек. аккаунта только для этого устройства)
            } else {

                if (val.equals(settingDrv.getLoginK())) {//сравнение ключа в устройстве с ключом в хосте водителя
                    onListener.onSuccessful(true, mode, "");
                } else {
                    onListener.onSuccessful(false, mode, App.context.getString(R.string.debug_break_login));
                }
            }

        }
    }

    public void recoveryResources() {
        executorservice.shutdown();
    }

    private SettingDrv getSetting(String uId) throws ExecutionException, InterruptedException {
        Callable task = () -> {
            SettingDrv settingDrv = db.getSettingAppDAO().getSetting(uId);
            if (settingDrv == null) {

                settingDrv = new SettingDrv(
                        user.getUid(),
                        new ArrayList<ServerModel>(Collections.singleton(new ServerModel("SHAREDSERVER", "SHARED", Util.CONNECTED_PROVIDER_STATUS, "", ""))),
                        false,
                        "",
                        "",
                        "",
                        "",
                        false,
                        false,
                        false,
                        user.getUid(),
                        new DataAuto("", "", ""),
                        "",
                        new DataLocation(0, 0),
                        10000,
                        "",
                        "");

                long id = db.getSettingAppDAO().saveDataSetting(settingDrv);
                if (id > 0) {
                    settingDrv.setId(id);
                }
            }

            return settingDrv;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (SettingDrv) future.get();
    }
}

