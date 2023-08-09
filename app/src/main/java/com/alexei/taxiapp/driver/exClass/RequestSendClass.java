package com.alexei.taxiapp.driver.exClass;

import androidx.annotation.NonNull;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.InfoRequestConnect;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RequestSendClass {


    private DatabaseReference hostDBRef;
    private DatabaseReference srvRef;
    private DatabaseReference requestRef;
    private ExecutorService executorservice;
    private String currentUserUid;
    private InfoRequestConnect request;
    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();

    private boolean bActionSend;

    //------------------------------------Listener
    public OnSuccessfulListener onListener;

    public interface OnSuccessfulListener {
        void onSuccessful(boolean success, int s, DatabaseReference srvRef);

    }

    public void setOnSuccessfulListener(OnSuccessfulListener listener) {
        this.onListener = listener;
    }


    public RequestSendClass(DatabaseReference srvRef, InfoRequestConnect request, String userUid, DatabaseReference hostDBRef) {

        this.hostDBRef = hostDBRef;
        this.srvRef = srvRef;
        this.request = request;
        this.currentUserUid = userUid;
        this.requestRef = srvRef.child("request");

        executorservice = Executors.newFixedThreadPool(2);

        executorservice.submit(() -> isExistsHostRequestSrv());
    }


    private void isExistsHostRequestSrv() {
        requestRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    isExistsMySenderRequest();
                } else {
                    onListener.onSuccessful(false, R.string.server_disable_accept_request, srvRef);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void isExistsMySenderRequest() {

        requestRef.child("data/keyS").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {//----------------запрос уже существует проверяем чей
                    String keySender = snapshot.getValue(String.class);

                    if (Objects.equals(keySender, currentUserUid)) {//---мой
                        onListener.onSuccessful(true, R.string.debug_request_break, srvRef);
                    } else {

                        initRequestListener();//--------------отправка запроса, если будет поле - accept
                    }

                } else {
                    initRequestListener();//--------------отправка запроса, если будет поле - accept
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onListener.onSuccessful(false, R.string.err_cancelled, srvRef);
            }
        });
    }

    public void stop() {
        try {
            Util.removeAllValueListener(mapListeners);
            onListener.onSuccessful(false, 0, srvRef);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private void initRequestListener() {
        if (mapListeners.get(requestRef.child("accept")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {//-*-----------------прием существует
                        if (!bActionSend) {

                            sendRequest();
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Util.removeAllValueListener(mapListeners);
                    onListener.onSuccessful(false, R.string.t_error + error.getCode(), srvRef);
                }
            };

            requestRef.child("accept").addValueEventListener(listener);
            mapListeners.put(requestRef.child("accept"), listener);
        }
    }

    private void sendRequest() {

        Map<String, Object> map = new HashMap<>();
        map.put("serverList/" + srvRef.getKey() + "/request/accept", null);//отключение приема
        map.put("serverList/" + srvRef.getKey() + "/request/data", request);//отправляется запрос

        hostDBRef.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {

                if (error == null) {
                    bActionSend = true;
                    Util.removeAllValueListener(mapListeners);
                    onListener.onSuccessful(true, R.string.request_success, srvRef);
                }
            }
        });

    }

    public void recoverResource() {
        Util.removeAllValueListener(mapListeners);
    }

    @Override
    protected void finalize() throws Throwable {
        recoverResource();

        super.finalize();
    }
}

