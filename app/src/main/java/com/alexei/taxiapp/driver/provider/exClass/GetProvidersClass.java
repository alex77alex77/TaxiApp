package com.alexei.taxiapp.driver.provider.exClass;

import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.driver.model.RedirectOrderModel;
import com.alexei.taxiapp.driver.model.ServerModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GetProvidersClass {

    private static GetProvidersClass instance;

    public static List<ProviderClass> mapProviders = new ArrayList<>();
    private FirebaseDatabase database;

    private String currentUserUid;

    private ArrayList<ServerModel> runServerModels = new ArrayList<>();
    private ExecutorService executorservice;

    private Map<DatabaseReference, ChildEventListener> mapChildListeners = new HashMap<>();
    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();


    //-------------listeners
    private OnConnectListener onGetProvidersListener;


    public interface OnConnectListener {

        void onShiftStatus(ProviderClass providerClass);//DatabaseReference serverRef, ShiftModel statusModel, String nameSrv

        void onExecutedAssignedOrder(ProviderClass providerClass, String keyOrder);//, DatabaseReference serverRef, String name

        void onChangeStatusOrder(String keyOrder, int status);

        void onExecutedRedirectOrder(ProviderClass providerClass, String keyOrder);

        void onKillRedirectOrder(ProviderClass providerClass, String keyOrder);

        void onChangeFieldRedirectOrder(ProviderClass providerClass, RedirectOrderModel model, DatabaseReference orderRef);

        void onNewOrder(ProviderClass providerClass, InfoOrder order);//DataSnapshot snapshot, DatabaseReference serverRef, String name

        void onRemoveOrder(ProviderClass providerClass, String key);//String snapshot, DatabaseReference serverRef, String name

        void onChangeFieldAssignedOrder(ProviderClass providerClass, RedirectOrderModel model, DatabaseReference orderRef);//String orderKey, DatabaseReference serverRef, String name

        void onChangeStatusDrvOnProvider(ProviderClass providerClass);

        void onUnConnected(ProviderClass providerClass);

        void onChangeLocalStatusForProvider(ProviderClass providerClass);

    }

    public void setListener(OnConnectListener listener) {
        this.onGetProvidersListener = listener;
    }

    private OnMsgListener onGetMsgProvidersListener;

    public interface OnMsgListener {
        void onMsgDrv2(ProviderClass providerClass);//MsgModel msg, DatabaseReference serverRef, String name
    }

    public void setMsgListener(OnMsgListener listener) {
        this.onGetMsgProvidersListener = listener;
    }


    public static synchronized GetProvidersClass getInstance(String currentUserUid) {//, ArrayList<ServerModel> servers
        if (instance == null) {
            instance = new GetProvidersClass(currentUserUid);//, servers
        }

        return instance;
    }

    public GetProvidersClass(String currentUserUid) {//, ArrayList<ServerModel> servers

        this.currentUserUid = currentUserUid;
        this.database = FirebaseDatabase.getInstance();
        this.executorservice = Executors.newFixedThreadPool(2);

    }


    @Override
    protected void finalize() throws Throwable {
        try {

            recoveryResources();
        } finally {
            super.finalize();
        }
    }

    public void recoveryResources() {
        Util.removeAllChildListener(mapChildListeners);
        Util.removeAllValueListener(mapListeners);
        mapProviders.stream().filter(Objects::nonNull).forEach(ProviderClass::recoverResources);
        mapProviders.clear();
        instance = null;
        executorservice.shutdownNow();
    }



    public void runProvider(ServerModel serverModel) {

        if (serverModel.getKeySrv().equals("SHAREDSERVER")) {

            runProviderClass(database.getReference().child("SHAREDSERVER"), serverModel.getName());
        } else {
            runProviderClass(database.getReference().child("serverList").child(serverModel.getKeySrv()), serverModel.getName());
        }

    }


    private synchronized void runProviderClass(DatabaseReference ref, String name) {
        if (mapProviders.stream().noneMatch(p -> p.getSrvRef().equals(ref))) {//если нет провайдера по такому адресу в списке запущенных


            ProviderClass providerClass = new ProviderClass(currentUserUid, ref, name);
            providerClass.setListener(new ProviderClass.OnConnectListener() {
                @Override
                public void onShiftStatus() {//DatabaseReference serverRef, ShiftModel statusModel, String mNameSrv
                    onGetProvidersListener.onShiftStatus(providerClass);
                }

                @Override
                public void onExecutedAssignedOrder(String keyOrder) {//, DatabaseReference serverRef, String name
                    onGetProvidersListener.onExecutedAssignedOrder(providerClass, keyOrder);
                }


                @Override
                public void onExecutedRedirectOrder(String keyOrder) {
                    onGetProvidersListener.onExecutedRedirectOrder(providerClass, keyOrder);
                }

                @Override
                public void onKillRedirectOrder(String keyOrder) {
                    onGetProvidersListener.onKillRedirectOrder(providerClass, keyOrder);
                }

                @Override
                public void onChangeFieldRedirectOrder(RedirectOrderModel model, DatabaseReference orderRef) {
                    onGetProvidersListener.onChangeFieldRedirectOrder(providerClass, model, orderRef);
                }

                @Override
                public void onChangeStatusOrder(String keyOrder, int status) {
                    onGetProvidersListener.onChangeStatusOrder(keyOrder, status);
                }

                @Override
                public void onNewOrder(InfoOrder order) {//DataSnapshot snapshot, DatabaseReference serverRef, String name

                    onGetProvidersListener.onNewOrder(providerClass, order);
                }

                @Override
                public void onRemoveOrder(String key) {//String snapshot, DatabaseReference serverRef, String name
                    onGetProvidersListener.onRemoveOrder(providerClass, key);
                }

                @Override
                public void onMsgDrv() {
                    if (onGetMsgProvidersListener != null) {

                        onGetMsgProvidersListener.onMsgDrv2(providerClass);
                    }
                }

                @Override
                public void onChangeLocalStatusForProvider() {
                    onGetProvidersListener.onChangeLocalStatusForProvider(providerClass);
                }

                @Override
                public void onChangeFieldAssignedOrder(RedirectOrderModel model, DatabaseReference orderRef) {//, DatabaseReference serverRef, String name
                    onGetProvidersListener.onChangeFieldAssignedOrder(providerClass, model, orderRef);
                }

                @Override
                public void onChangeStatusDrvOnProvider() {
                    onGetProvidersListener.onChangeStatusDrvOnProvider(providerClass);
                }
            });


            mapProviders.add(providerClass);

        }
    }

    public void stopProvider(ServerModel serverModel) {
        mapProviders.stream().filter(p -> Objects.equals(p.getSrvRef().getKey(), serverModel.getKeySrv())).findAny().ifPresent(p -> {
            p.recoverResources();
            mapProviders.remove(p);
            onGetProvidersListener.onUnConnected(p);
        });

        runServerModels.remove(serverModel);
    }

    public void resetFreeOrders() {
        mapProviders.stream().filter(p -> p.getStatusDrvOnSrv()==Util.CONNECTED_TO_SERVER_DRIVER_STATUS).forEach(p->{
            p.resetFreeOrders();
        });
    }

}

