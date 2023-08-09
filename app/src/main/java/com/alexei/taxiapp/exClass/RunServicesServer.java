package com.alexei.taxiapp.exClass;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.InfoRequestConnect;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.driver.model.DataResponse;
import com.alexei.taxiapp.driver.model.DeniedDrvModel;
import com.alexei.taxiapp.server.exClass.RequestAcceptClass;
import com.alexei.taxiapp.server.exClass.SrvDriversObservationClass;
import com.alexei.taxiapp.server.model.DataSenderModel;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.db.SettingServer;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RunServicesServer {
    private static RunServicesServer instance;
    private SettingServer setting = ServerInformsAboutEventsClass.setting;

    private ExecutorService executorService;
    private AppDatabase db;
    private DatabaseReference serverRef;
    private DatabaseReference hostDrvRef;
    private FirebaseDatabase database;

    private ServerInformsAboutEventsClass serverEvents;

    private SrvDriversObservationClass driversObservation;
    private RequestAcceptClass reqAcceptClass;
    private AcceptClientOrderClass acceptClientOrderClass;
    private FirebaseUser currentUser;

    private List<InfoDriverReg> driversList;

    public ArrayList<DeniedDrvModel> getDeniedDrvList() {
        return deniedDrvList;
    }

    private ArrayList<DeniedDrvModel> deniedDrvList = new ArrayList<>();

    public static boolean starterServer;

    //------------------------------------------Listener 1
    private OnListener onListener;

    public interface OnListener {

        void showRequest(InfoRequestConnect requestModel, long count);

        void onExitReadRequest(boolean bSuccess);

        void onHasRequest(RequestAcceptClass requestAcceptClass, boolean b, long count);

        void onBreakSaveDriver(String descriptionDeny);

        void onError(String err);
    }

    public void setOnListener(OnListener listener) {
        this.onListener = listener;
    }

    //------------------------------------------Listener 2
    private OnClientOrderListener onClientOrderListener;

    public interface OnClientOrderListener {

        void onPostRequestClientOrder(AcceptClientOrderClass orderClass, String keySender, String name);

        void onPostClientOrder(String keySender);

        void onConfirmation(AcceptClientOrderClass orderClass, String name, DataSenderModel dataSender);

    }

    public void setOnClientOrderListener(OnClientOrderListener listener) {
        this.onClientOrderListener = listener;
    }

    //--------------------------

    public static synchronized RunServicesServer getInstance(DatabaseReference serverRef, FirebaseUser currentUId) {  //If you want your method thread safe...
        if (instance == null) {
            instance = new RunServicesServer(serverRef, currentUId);
        }

        return instance;
    }

    public RunServicesServer(DatabaseReference serverRef, FirebaseUser currentUId) {
        this.starterServer = true;

        this.serverRef = serverRef;
        this.currentUser = currentUId;
        this. database = FirebaseDatabase.getInstance();
        this.hostDrvRef = database.getReference().child("SHAREDSERVER/driversList");
        this.executorService = Executors.newFixedThreadPool(2);
        this.db = Room.databaseBuilder(App.context, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();

        setListeners();

        executorService.submit(() -> runServices());

    }

    private void setListeners() {
        setOnListener(new OnListener() {

            @Override
            public void showRequest(InfoRequestConnect requestModel, long count) {

            }

            @Override
            public void onExitReadRequest(boolean bSuccess) {

            }

            @Override
            public void onHasRequest(RequestAcceptClass requestClass, boolean b, long count) {

            }

            @Override
            public void onBreakSaveDriver(String s) {

            }

            @Override
            public void onError(String err) {

            }

        });

        setOnClientOrderListener(new OnClientOrderListener() {
            @Override
            public void onPostRequestClientOrder(AcceptClientOrderClass orderClass, String keySender, String name) {

            }

            @Override
            public void onPostClientOrder(String keySender) {

            }

            @Override
            public void onConfirmation(AcceptClientOrderClass orderClass, String name, DataSenderModel dataSender) {

            }
        });
    }

    private void runServices() {

        serverEvents = ServerInformsAboutEventsClass.getInstance();

        runRequestAcceptClass();

        driversObservation = SrvDriversObservationClass.getInstance(serverRef, currentUser);

        driversList = SrvDriversObservationClass.allDrivers;//ссылка на массив водителей

        runAcceptClientOrderClass();

        serverEvents.onListener.onLoad();

    }

    private void runAcceptClientOrderClass() {

        acceptClientOrderClass = AcceptClientOrderClass.getInstance(serverRef, setting.isChkAcceptOrder(), currentUser.getUid());
        acceptClientOrderClass.setOnListener(new AcceptClientOrderClass.OnListeners() {

            @Override
            public void onPostClientOrder(String keySender) {
                onClientOrderListener.onPostClientOrder(keySender);
            }

            @Override
            public void onConfirmation(String name, DataSenderModel dataSender) {//нужно подтверждение-инициируется из активити
                onClientOrderListener.onConfirmation(acceptClientOrderClass, name, dataSender);//если слушает сервер - запускается диалог
            }

            @Override
            public void onPostRequestClientOrder(String keySender, String nameUser) {
                onClientOrderListener.onPostRequestClientOrder(acceptClientOrderClass, keySender, nameUser);//передаем в активити которое слушает
            }
        });
        acceptClientOrderClass.startClass();//запуск
    }

    private void runRequestAcceptClass() {

        reqAcceptClass = RequestAcceptClass.getInstance(serverRef, setting.isChkDisableReq());
        reqAcceptClass.setOnListener(new RequestAcceptClass.OnListeners() {
            @Override
            public void acceptRequest(InfoRequestConnect requestModel, long count) {
                if (setting.isChkConnect()) {//automatic

                    saveRequestDataDriver(requestModel);
                    preparingHostRequest();//-------подготовка к следующему приему
                    sendResponse(requestModel.getKeyS());//---ответ(подключен) водителю
                } else {

                    onListener.showRequest(requestModel, count);
                }
            }

            @Override
            public void onExitReadRequest(boolean bSuccess) {
                onListener.onExitReadRequest(bSuccess);
//                flBlock.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onHasRequest(boolean b, long count) {//запрос пришел в узел или обработан
                if (b) {
                    if (setting.isChkConnect()) {//авто!!!!!!!!!!!!!!!

                        reqAcceptClass.readRequest();//получаем

                    } else {
                        onListener.onHasRequest(reqAcceptClass, b, count);//сообщаем в активити
                    }
                } else {

                    onListener.onHasRequest(reqAcceptClass, b, count);//сообщаем в активити
                }

            }

        });
    }

    public void sendResponse(String keyDrv) {
        hostDrvRef.child(keyDrv).child("response").child(currentUser.getUid()).setValue(new DataResponse(Util.CONNECTED_TO_SERVER_DRIVER_STATUS, ServerValue.TIMESTAMP));

    }

    public void changeChkDisableRequest(boolean b) {
        reqAcceptClass.checkRefRequest(b);
    }

    private void preparingHostRequest() {
        Map<String, Object> map = new HashMap<>();
        map.put("accept", "");
        map.put("data", null);


        serverRef.child("request").updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error != null) {
                    onListener.onError(App.context.getString(R.string.debug_break_prepare_request));
                }
            }
        });
    }

    public void checkHasRequestClientOrder() {

        acceptClientOrderClass.startClass();
    }//запуск из активити

    public void checkHasRequestNewDrv() {
        if (reqAcceptClass != null) {

            reqAcceptClass.reGetDataClass();//проверить есть ли запрос и сколько принято сейчас
        }
        if (deniedDrvList.size() > 0) {
            onListener.onBreakSaveDriver("");
        }

    }//запуск из активити

    public void readRequest() {
        if (reqAcceptClass != null) {

            reqAcceptClass.readRequest();//команда из активити -получаем запрос
        }
    }



    public void saveRequestDataDriver(InfoRequestConnect reqModel) {

        executorService.submit(() -> {

            InfoDriverReg driver = db.getDataDriversServerDAO().getDriver(reqModel.getKeyS(), currentUser.getUid());//начало ссылки на объект

            if (driver == null) {//новый
                long newCallSign = db.getDataDriversServerDAO().getMaxCallSign(currentUser.getUid()) + 1;

                driver = new InfoDriverReg(
                        currentUser.getUid(),
                        0,
                        Util.SHIFT_CLOSE_DRV_STATUS,
                        System.currentTimeMillis(),
                        System.currentTimeMillis(),
                        0,
                        "",
                        reqModel.getName(),
                        "",
                        newCallSign,
                        reqModel.getAuto(),
                        reqModel.getTypeTr(),
                        reqModel.getKeyS(),
                        "",
                        reqModel.getPhone(),
                        currentUser.getUid(),
                        Util.CONNECTED_TO_SERVER_DRIVER_STATUS,
                        0,
                        reqModel.getDisl(),
                        (Long) reqModel.getTs());

                long id = db.getDataDriversServerDAO().addDriverServer(driver);

                if (id > 0) {
                    driver.setId(id);

                    driversList.add(driver);//начало ссылки на объект

                    driversObservation.createDrvInHostSrv(driver);//там создается узел нового водителя в узле на сервере со статусом - подключен Util.CONNECTED_TO_SERVER_STATUS
                }else {
                    Toast.makeText(App.context, "Не удается сохранить водителя!", Toast.LENGTH_SHORT).show();
                }

            } else {//данные уже есть

                driversObservation.createDrvInHostSrv(driver);//там создается узел для существующего водителя

            }

            //-------------------подготовка узла request к следующему приему
            Map<String, Object> map = new HashMap<>();
            map.put("accept", "");//индикатор
            map.put("data", null);
            serverRef.child("request").updateChildren(map);
        });
    }
}

