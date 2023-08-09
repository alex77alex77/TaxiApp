package com.alexei.taxiapp.driver.provider.exClass;

import androidx.annotation.NonNull;

import com.alexei.taxiapp.driver.model.InfoServerModel;
import com.alexei.taxiapp.driver.model.ServerModel;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TrackingHostServers {

    private static TrackingHostServers instance;
    private FirebaseDatabase database;
    private DatabaseReference hostSharedServer;
    private DatabaseReference hostServers;
    private DatabaseReference hostKeysSrvRef;
    private String currentUserUid;
    public static ArrayList<ServerModel> arrServerByHost = new ArrayList<>();

    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private Map<DatabaseReference, ChildEventListener> mapChildListeners = new HashMap<>();
    private ExecutorService executorservice;


    //-------------listeners
    private OnConnectListener onListener;

    public interface OnConnectListener {

        void onUnConnected(ServerModel serverModel);

        void onConnected(ServerModel serverModel);

        void onRemovedServer(ServerModel serverModel);
    }

    public void setListener(OnConnectListener listener) {
        this.onListener = listener;
    }

    //-------------listeners
    private OnChangeListener onChangeListener;

    public interface OnChangeListener {

        void onChange();
    }

    public void setListener(OnChangeListener listener) {
        this.onChangeListener = listener;
    }

    public static synchronized TrackingHostServers getInstance( String currentUserUid) {
        if (instance == null) {
            instance = new TrackingHostServers( currentUserUid);
        }

        return instance;
    }

    public TrackingHostServers( String currentUserUid) {


        this.database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных
        this.hostSharedServer =  database.getReference().child("SHAREDSERVER");// hostSharedServer;
        this.hostKeysSrvRef = database.getReference().child("keysS");//hostKeysSRef;
        this.hostServers = database.getReference().child("serverList");//hostServers;
        this.currentUserUid = currentUserUid;
        this.executorservice = Executors.newFixedThreadPool(2);

        executorservice.submit(this::loadListServers);
    }



    private void loadListServers() {

        if (mapChildListeners.get(hostKeysSrvRef) == null) {//узел всех ключей серверов

            loadSAHREDSERVER();

            loadOtherServers();

        }
    }

    private void loadOtherServers() {
        ChildEventListener listener = new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                if (snapshot.exists()) {

//                        if (!Objects.equals(snapshot.getKey(), currentUserUid)) {//исключаем свой сервер

                    getInfoServer(snapshot.getKey());
//                        }
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

            }

            @Override
            public void onChildRemoved(@NonNull DataSnapshot snapshot) {
                if (snapshot.getKey() != null) {
                    mapListeners.put(hostServers.child(snapshot.getKey()).child("driversList").child(currentUserUid).child("status"), null);//удаляем слушателя
                    onListener.onRemovedServer(arrServerByHost.stream().filter(s->s.getKeySrv().equals(snapshot.getKey())).findAny().orElse(null));
                    arrServerByHost.removeIf(s -> s.getKeySrv().equals(snapshot.getKey()));//сервер удален

                    if(onChangeListener!=null){// если слушают в SettingDriverMaps
                        onChangeListener.onChange();
                    }
                }
            }

            @Override
            public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        hostKeysSrvRef.addChildEventListener(listener);
        mapChildListeners.put(hostKeysSrvRef, listener);
    }


    private void getInfoServer(String keySrv) {
        hostServers.child(keySrv).child("info").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {//получаем сервер только тот у которого есть узел - info.

                    InfoServerModel info = snapshot.getValue(InfoServerModel.class);
                    if (info != null) {
                        ServerModel server = new ServerModel(keySrv, info.getName(), Util.NOT_DEFINED_PROVIDER_STATUS, info.getServices(),info.getPhone());
                        arrServerByHost.add(server);
                        trackingConnectedDrvListener(keySrv, server);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void loadSAHREDSERVER() {
        hostSharedServer.child("driversList").child(currentUserUid).child("status").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    ServerModel serverModel = new ServerModel("SHAREDSERVER", "SHARED", Util.CONNECTED_PROVIDER_STATUS,"","");
                    arrServerByHost.add(serverModel);
                    onListener.onConnected(serverModel);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void trackingConnectedDrvListener(String keySrv, ServerModel server) {
        if (mapListeners.get(hostServers.child(keySrv).child("driversList").child(currentUserUid).child("status")) == null) {
            //определение если в узле данного снрвера есть мой ключ то сервер=подключен
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        server.setStatus(Util.CONNECTED_PROVIDER_STATUS);
                        onListener.onConnected(server);
                    } else {

                        server.setStatus(Util.UNCONNECTED_PROVIDER_STATUS);
                        onListener.onUnConnected(server);
                    }

                    if(onChangeListener!=null){// если слушают в SettingDriverMaps
                        onChangeListener.onChange();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            hostServers.child(keySrv).child("driversList").child(currentUserUid).child("status").addValueEventListener(listener);
            mapListeners.put(hostServers.child(keySrv).child("driversList").child(currentUserUid).child("status"), listener);
        }
    }

    public void recoverResources() {
        Util.removeAllChildListener(mapChildListeners);
        Util.removeAllValueListener(mapListeners);
        arrServerByHost.clear();

        instance = null;
        executorservice.shutdownNow();
    }
}

