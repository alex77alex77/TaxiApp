package com.alexei.taxiapp.server.exClass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.InfoRequestConnect;
import com.alexei.taxiapp.driver.model.DataAuto;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestAcceptClass {
    private static RequestAcceptClass instance;
    private ExecutorService executorservice;
    private DatabaseReference refServer;
    private FirebaseDatabase database;

    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();

    private long countAddDrv;
    private boolean bHasRequest;



    //------------------------------------Listener
    public OnListeners onListener;

    public interface OnListeners {
        void acceptRequest(InfoRequestConnect requestModel, long countAddDrv);

        void onExitReadRequest(boolean bSuccess);

        void onHasRequest(boolean b, long countAddDrv);

    }

    public void setOnListener(OnListeners listener) {
        this.onListener = listener;
    }

    public static synchronized RequestAcceptClass getInstance(DatabaseReference refServer, boolean bDisableRequest) {
        if (instance == null) {
            instance = new RequestAcceptClass(refServer, bDisableRequest);
        }

        return instance;
    }

    public RequestAcceptClass(DatabaseReference refServer, boolean bDisableRequest) {
        this.database = FirebaseDatabase.getInstance();
        this.refServer = refServer;
        this.executorservice = Executors.newFixedThreadPool(2);
        setListeners();

        executorservice.submit(() -> {
            checkRefRequest(bDisableRequest);//проверка узла для приема запроса
        });
    }

    private void setListeners() {
        setOnListener(new OnListeners() {
            @Override
            public void acceptRequest(InfoRequestConnect requestModel, long countAddDrv) {

            }

            @Override
            public void onExitReadRequest(boolean bSuccess) {

            }

            @Override
            public void onHasRequest(boolean b, long countAddDrv) {

            }
        });
    }

    public void checkRefRequest(boolean bDisableReq) {//---------------------------------   проверка узла для приема запроса

        if (!bDisableReq) {// нет запрета для приема запросов

            refServer.child("request").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!snapshot.exists()) {//--------------узла для запроса нет

                        //-------------создается узел с флагом(accept) для приема -----""для фиксирования ссылки
                        refServer.child("request/accept").setValue("", new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                if (error == null) {
                                    requestListener();//слушаем запрос
                                }
                            }
                        });

                    } else {
                        requestListener();//слушаем запрос
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else {

            refServer.child("request").removeValue();
            Util.removeByRefListener(refServer.child("request/data"),mapListeners);
        }
    }

    private void requestListener() {

        if (mapListeners.get(refServer.child("request/data")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {

                    //пришел запрос
                    if (snapshot.exists()) {//запрос пришел
                        countAddDrv++;
                        bHasRequest = true;
                        onListener.onHasRequest(true, countAddDrv);
                    } else {
                        bHasRequest = false;
                        onListener.onHasRequest(false, countAddDrv);//запроса нет либо обработан
                    }

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            refServer.child("request/data").addValueEventListener(listener);
            mapListeners.put(refServer.child("request/data"), listener);
        }
    }

    public void readRequest() {
        if (bHasRequest) {

            refServer.child("request/data").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        InfoRequestConnect requestModel = snapshot.getValue(InfoRequestConnect.class);
                        if (requestModel != null) {

                            getDataTransportRef(requestModel);//получает транспорт отправителя запроса

                        } else {
                            onListener.onExitReadRequest(false);
                        }
                    } else {
                        bHasRequest = false;
                        onListener.onHasRequest(false, countAddDrv);//запроса нет либо обработан
                        onListener.onExitReadRequest(false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    onListener.onExitReadRequest(false);
                }
            });
        } else {
            onListener.onExitReadRequest(false);
        }
    }

    private void getDataTransportRef(InfoRequestConnect reqModel) {

        database.getReference().child("transport").child(reqModel.getKeyS()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    DataAuto dataAuto = snapshot.getValue(DataAuto.class);
                    if (dataAuto != null) {
                        reqModel.setAuto(dataAuto);
                        onListener.acceptRequest(reqModel, countAddDrv);//возвращаем в класс сервис (там сохранение)
                        onListener.onExitReadRequest(true);
                    }
                } else {

                    reqModel.setAuto(new DataAuto("-", "-", "-"));
                    onListener.acceptRequest(reqModel, countAddDrv);//возвращаем в класс сервис (там сохранение)
                    onListener.onExitReadRequest(true);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                reqModel.setAuto(new DataAuto(App.context.getString(R.string.unavailable),
                        App.context.getString(R.string.unavailable),
                        App.context.getString(R.string.unavailable)));

                onListener.acceptRequest(reqModel, countAddDrv);//возвращаем в класс сервис (там сохранение)
                onListener.onExitReadRequest(true);
            }
        });
    }


    @Override
    protected void finalize() throws Throwable {
        recoverRes();
        super.finalize();

    }

    public void recoverRes() {
        Util.removeAllValueListener(mapListeners);
        instance = null;
    }

    public void reGetDataClass() {

        onListener.onHasRequest(bHasRequest, countAddDrv);

    }

}
