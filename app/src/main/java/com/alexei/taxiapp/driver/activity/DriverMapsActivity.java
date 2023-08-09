package com.alexei.taxiapp.driver.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.ChooseModeActivity;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityDriverMapsBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.DataExecutableOrder;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.db.InfoRequestConnect;
import com.alexei.taxiapp.db.SettingDrv;
import com.alexei.taxiapp.db.SettingServer;

import com.alexei.taxiapp.driver.adapter.AdapterMsgListSrv;
import com.alexei.taxiapp.driver.adapter.AdapterProblemInfoProvider;
import com.alexei.taxiapp.driver.adapter.AdapterProviders;
import com.alexei.taxiapp.driver.exClass.AssignedToFreeOrderClass;
import com.alexei.taxiapp.driver.exClass.AssignedToPersonalOrderClass;
import com.alexei.taxiapp.driver.exClass.RequestSendClass;
import com.alexei.taxiapp.driver.exClass.SOSClass;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.driver.model.DataAuto;
import com.alexei.taxiapp.driver.model.DataLocale;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.driver.model.ExOrderModel;
import com.alexei.taxiapp.driver.model.InfoProviderModel;
import com.alexei.taxiapp.driver.model.RedirectOrderModel;
import com.alexei.taxiapp.driver.model.SOSModel;
import com.alexei.taxiapp.driver.model.ServerModel;
import com.alexei.taxiapp.driver.provider.exClass.GetProvidersClass;
import com.alexei.taxiapp.driver.provider.exClass.ProviderClass;
import com.alexei.taxiapp.driver.provider.exClass.TrackingHostServers;
import com.alexei.taxiapp.exClass.AcceptClientOrderClass;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.alexei.taxiapp.exClass.RunServicesServer;
import com.alexei.taxiapp.server.activity.ServerActivity;
import com.alexei.taxiapp.server.exClass.RequestAcceptClass;
import com.alexei.taxiapp.server.model.DataSenderModel;
import com.alexei.taxiapp.server.model.ShiftModel;
import com.alexei.taxiapp.util.Util;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class DriverMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityDriverMapsBinding binding;
    private final String GET_ON_THE_LINE = App.context.getString(R.string.stand_up);
    private final String GET_OFF_THE_LINE = App.context.getString(R.string.go_out);

    private AppDatabase db;

    private ExecutorService executorService;
    private Location currLocation = new Location("");
    private Locale currLocale;

    private RunServicesServer services;

    private ServerInformsAboutEventsClass serverEvents;

    private float mZoom = 16;

    private Marker driverMarker;

    private BuildLocationClass locationClass;
    private SOSClass sosClass;
    private GetProvidersClass getAllProvidersClass;
    private TrackingHostServers trackingHostServers;


    public static SettingDrv settingDrv;
    private DataExecutableOrder exOrder;

    private FirebaseAuth auth;
    private FirebaseDatabase database;

    private DatabaseReference locationRef;
    private DatabaseReference sharedServerRef;
    private DatabaseReference sharedDrvRef;
    private DatabaseReference hostServersRef;

    private DatabaseReference sosHostRef;

    private String currentUserUid;
    private ArrayList<String> arrayListRejectedOrders;

    private ArrayList<InfoOrder> arrayListFreeOrders;
    private ArrayList<InfoOrder> arrayListPersonalOrders;
    private ArrayList<AssignedToFreeOrderClass> arrayListRegToOrder;

    private int statusSOS = Util.SOS_OFF;
    private MediaPlayer mp;
    private PopupMenu popupMenu;

    private Map<DatabaseReference, ValueEventListener> mapRegStreamListeners = new HashMap<>();
    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private Map<DatabaseReference, ChildEventListener> mapChildListeners = new HashMap<>();
    private Map<DatabaseReference, RequestSendClass> mapRequests = new HashMap<>();

    private List<AssignedToPersonalOrderClass> assignedClassList = new ArrayList<>();

    private ArrayList<InfoProviderModel> arrProblemInfoProviders;
    private final List<ProviderClass> mapListProvider = GetProvidersClass.mapProviders;// ссылка на массив Providers;

    private AdapterProviders adapterProviders;
    private AdapterProblemInfoProvider adapterInfoProviders;

    private boolean onTheLine = false;
    private boolean driverBusy = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setRequestedOrientation(getResources().getConfiguration().orientation);
        currLocale = Locale.getDefault();

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы
        executorService = Executors.newFixedThreadPool(2);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        arrayListPersonalOrders = new ArrayList<>();
        arrayListRejectedOrders = new ArrayList<>();
        arrayListFreeOrders = new ArrayList<>();
        arrayListRegToOrder = new ArrayList<>();
        arrProblemInfoProviders = new ArrayList<>();

        popupMenu = new PopupMenu(DriverMapsActivity.this, binding.btnMenuOne);
        popupMenu.inflate(R.menu.primary_menu);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных


        if (auth.getCurrentUser() != null) {

            setViewsListener();

            currentUserUid = auth.getCurrentUser().getUid();

            hostServersRef = database.getReference().child("serverList");

            sharedServerRef = database.getReference().child("SHAREDSERVER");

            sosHostRef = database.getReference().child("sos");

            sharedDrvRef = sharedServerRef.child("driversList").child(currentUserUid);

            binding.getOnTheLineButton.setText(GET_ON_THE_LINE);

            loadData();//<-checkExecutableOrder();//<- mapListProvider = GetProvidersClass.mapProviders;// ссылка на массив Providers

        } else {
            finish();
        }
    }

    private void setViewsListener() {
        binding.tvProviders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayConnProviders();
            }
        });

        binding.btnMenuOne.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                commandOneMenu(item);
                return true;
            }
        });

        binding.tvInfoProviderImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showInfoProvider();
            }
        });

        binding.ibMyServer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                gotoMySrv();
            }
        });

        binding.tvMailSrv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showListMsg();
            }
        });

        binding.btnFreeOrders.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.flBlock.setVisibility(View.VISIBLE);
                showOrders(arrayListFreeOrders, Util.REQUEST_SELECT_FREE_ORDER);
            }
        });

        binding.btnPersonOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.flBlock.setVisibility(View.VISIBLE);
                showOrders(arrayListPersonalOrders, Util.REQUEST_SELECT_PERSONAL_ORDER);
            }
        });

        binding.tvSOS1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                handlerClickSOS();
            }
        });

        binding.tvSOS1.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                handlerSosLongClick();
                return true;
            }
        });

        binding.ibGPS.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (currLocation != null) {
                    setCameraInTheDirection(currLocation);
                } else {
                    Toast.makeText(DriverMapsActivity.this, R.string.not_gps, Toast.LENGTH_SHORT).show();
                }
            }
        });

        binding.getOnTheLineButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (binding.getOnTheLineButton.getText().equals(GET_ON_THE_LINE)) {

                    setViewOnTheLineButton(Util.SELECT_BUTTON);
                } else {

                    setViewOnTheLineButton(Util.NOT_SELECT_BUTTON);
                }
            }
        });
    }

//****************************      TrackingConnectedServers    Отслеживаем статус серверов     TrackingConnectedServers    ************************

    private void handlerTrackingConnectedServersListener() {
        trackingHostServers = TrackingHostServers.getInstance(currentUserUid);//собираем все сервера где есть мой ключ

        trackingHostServers.setListener(new TrackingHostServers.OnConnectListener() {
            @Override
            public void onUnConnected(ServerModel serverModel) {
                if (getAllProvidersClass != null) {
                    getAllProvidersClass.stopProvider(serverModel);
                }
            }

            @Override
            public void onConnected(ServerModel serverModel) {//у данного сервера есть мой(водителя) ключ
                if (getAllProvidersClass != null) {
                    getAllProvidersClass.runProvider(serverModel);//запуск из стока провайдеров
                }
            }

            @Override
            public void onRemovedServer(ServerModel serverModel) {
                if (getAllProvidersClass != null) {
                    if (serverModel != null) {
                        getAllProvidersClass.stopProvider(serverModel);
                    }
                }
            }
        });

    }

    private void showInfoProvider() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DriverMapsActivity.this);
        builder.setCancelable(false);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_list_message, null);

        builder.setView(dialogView);


        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvListMessage);
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);

        adapterInfoProviders = new AdapterProblemInfoProvider(arrProblemInfoProviders);
        rv.setAdapter(adapterInfoProviders);

        handlerAdapterListener(adapterInfoProviders);

        builder.setPositiveButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handlerAdapterListener(AdapterProblemInfoProvider adapterInfoProviders) {
        adapterInfoProviders.setOnListener(new AdapterProblemInfoProvider.OnSelectListener() {
            @Override
            public void onSelectItem(InfoProviderModel info) {

            }

            @Override
            public void onFix(InfoProviderModel info) {
                handlerFixClick(info);

            }
        });
    }

    private void handlerFixClick(InfoProviderModel info) {
        switch (info.getStatus()) {
            case Util.UNKNOWN_DRIVER_STATUS:
                setUserDataForRequest(info);
                break;

        }
    }

    private void handlerSosLongClick() {
        Intent intent = new Intent(DriverMapsActivity.this, SosDotMapsActivity.class);
        ArrayList<String> arrayList = sosClass.drvSOSList;
        intent.putExtra("location", new DataLocation(currLocation));//.getLatitude(), currLocation.getLongitude()
        intent.putExtra("userUid", currentUserUid);
        intent.putStringArrayListExtra("keys", arrayList);

        startActivity(intent);
    }

    private void showOrders(ArrayList<InfoOrder> arrayOrders, int resultCode) {
        Intent intent = new Intent(DriverMapsActivity.this, ListFreeOrdersActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("listOrder", arrayOrders);
        intent.putExtras(bundle);
        startActivityForResult(intent, resultCode);
    }

    private void checkExecutableOrder() {

        DataExecutableOrder exOrderDb = db.getExecutableOrderDAO().getExecutableOrder(currentUserUid);

//получаем назначенный данные ордера из базы
//получаем ордер по ссылке в свободных опр. сервера
//показываем продолжение маршрута
//подключаем все провайдеры

        if (exOrderDb != null) {
            DatabaseReference orderRef;

            if (exOrderDb.getKeySrv().equals("SHAREDSERVER")) {
                orderRef = sharedServerRef.child("freeOrders").child(exOrderDb.getKeyOrder());
            } else {
                orderRef = hostServersRef.child(exOrderDb.getKeySrv()).child("freeOrders").child(exOrderDb.getKeyOrder());
            }

            orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        InfoOrder order = snapshot.getValue(InfoOrder.class);
                        if (order != null) {

                            handlerOldExOrder(order, exOrderDb);

                        } else {//нет данных ордера по ссылке

                            Toast.makeText(getApplicationContext(), R.string.debug_false_get_data_order, Toast.LENGTH_SHORT).show();
                            executorService.submit(() -> deleteDataExOrder());
                        }

                    } else {//заказа по ссылке нет

                        Toast.makeText(getApplicationContext(), R.string.order_del, Toast.LENGTH_SHORT).show();
                        // запуск Providers если нет выполняемого заказа или назначеный заказ не получен и не заказ занят другим
                        executorService.submit(() -> deleteDataExOrder());
                        runProvidersModeFirst();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        } else {

//            sharedServerRef.child("driversList").setValue(new  Util.FREE_DRIVER_STATUS);//создание водителя
            sharedServerRef.child("driversList").child(currentUserUid).child("status").setValue(Util.FREE_DRIVER_STATUS, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {
                        runProvidersModeFirst();
                    }
                }
            });
        }
    }

    private void handlerOldExOrder(InfoOrder order, DataExecutableOrder exOrder) {
        if (order.getDriverUid().equals(currentUserUid)) {//если назначен я

            order.setProviderName(exOrder.getNameSrv());

            sharedDrvRef.child("status").setValue(Util.BUSY_DRIVER_STATUS, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {

                        displayGuideOrder(order, exOrder.getLocale());

                        runProvidersModeFirst();
                    }
                }
            });

        } else {//назначен уже другой

            executorService.submit(() -> deleteDataExOrder());
            Toast.makeText(getApplicationContext(), R.string.another_driver_assigned, Toast.LENGTH_SHORT).show();
        }
    }


    private void gotoMySrv() {
        binding.flBlock.setVisibility(View.VISIBLE);
        Intent intent = new Intent(DriverMapsActivity.this, ServerActivity.class);
        startActivity(intent);

    }

    private void handlerClickSOS() {
        if (statusSOS == Util.SOS_OFF) {
            sosHostRef.child(currentUserUid).setValue(new SOSModel());
        } else {

            if (settingDrv.isAccessPassSOS()) {
                //   String password = settingDrv.getPassword();// db.getSettingAppDAO().getPassword(currentUserUid);

                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setMessage(R.string.input_passowrd);
                final EditText input = new EditText(this);
                input.setGravity(Gravity.CENTER);
                alert.setView(input);

                alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        if (value.equals(settingDrv.getPassword())) {

                            sosHostRef.child(currentUserUid).removeValue();
                        } else {

                            Toast.makeText(DriverMapsActivity.this, R.string.invalid_password2, Toast.LENGTH_LONG).show();
                        }
                    }
                });
                alert.show();

            } else {

                sosHostRef.child(currentUserUid).removeValue();
            }
        }
    }


    private SettingDrv getSetting() throws ExecutionException, InterruptedException {
        Callable task = () -> {
            SettingDrv settingDrv = db.getSettingAppDAO().getSetting(currentUserUid);
            if (settingDrv == null) {
                notifyNotDataSettingSnackBar();
                settingDrv = new SettingDrv(
                        currentUserUid,
                        new ArrayList<ServerModel>(Collections.singleton(new ServerModel("SHAREDSERVER", "SHARED", Util.CONNECTED_PROVIDER_STATUS, "", ""))),
                        false,
                        "",
                        "",
                        "",
                        "",
                        false,
                        false,
                        false,
                        currentUserUid,
                        new DataAuto("", "", ""),
                        "",
                        new DataLocation(0, 0),
                        10000,
                        "",
                        "");
            }

            return settingDrv;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (SettingDrv) future.get();
    }

    private void loadData() {
        try {
            runClassLocation();//активизируется, но начинает писать локацию после инициализации меня в SHAREDSERVER -> activeStatusDrvSharedServerHost()

            settingDrv = getSetting();

            //фиксируем время входа или создаем меня->наблюдение за моим статусом на всех серверах
            activateStateDriverOnSHAREDSERVER();//обновляем время/создаем меня на SHAREDSERVER-> там подключается TrackingHostServers

            runEventsServerClass(getSettingSrv());


            if (settingDrv.isRunServer()) {
                handlerStartServer();
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(this, "error - " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void activateStateDriverOnSHAREDSERVER() {
        sharedDrvRef.child("ts").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    sharedDrvRef.child("ts").setValue(ServerValue.TIMESTAMP, new DatabaseReference.CompletionListener() {//фиксируем время входа
                        @Override
                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error == null) {
                                nextLoadData();
                            } else {
                                Toast.makeText(DriverMapsActivity.this, "error update timestamp- " + error.getCode(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });
                } else {//********************   создание водителя

                    createDriverOnSharedServer();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void nextLoadData() {
        if (locationClass != null) {
            locationRef = sharedDrvRef.child("location");//запускаем запись локации
            locationClass.setRefLoc(locationRef);//запускается

            handlerTrackingConnectedServersListener();//после создания меня на SHAREDSERVER наблюдение за моим подключением на всех серверах включая этот
            checkExecutableOrder();
        }
    }

    private void createDriverOnSharedServer() {
        Map<String, Object> map = new HashMap<>();

        map.put("/keysD/" + currentUserUid, "");
        map.put("/driversList/" + currentUserUid + "/location", new DataLocation(currLocation));
        map.put("/driversList/" + currentUserUid + "/status", Util.FREE_DRIVER_STATUS);
        map.put("/driversList/" + currentUserUid + "/ts", ServerValue.TIMESTAMP);


        sharedServerRef.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    nextLoadData();
                } else {
                    Toast.makeText(DriverMapsActivity.this, R.string.err_create_drv, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private SettingServer getSettingSrv() throws ExecutionException, InterruptedException {
        Callable task = () -> {
            SettingServer server = db.getSettingServerDAO().getSetting(currentUserUid);
            if (server == null) {
                server = new SettingServer(0,
                        currentUserUid,
                        "",
                        false,
                        false,
                        24,
                        4,
                        10,
                        "",
                        false,
                        false,
                        "");
            }

            return server;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (SettingServer) future.get();
    }


    private void runProvidersModeTwo() {

        getAllProvidersClass = GetProvidersClass.getInstance(currentUserUid);//, TrackingHostServers.arrServerByHost
        getAllProvidersClass.setMsgListener(new GetProvidersClass.OnMsgListener() {
            @Override
            public void onMsgDrv2(ProviderClass providerClass) {

                handlerMsgForDrv();
            }
        });
        //если есть сообщения то сообщить сигналом и ib
        if (mapListProvider.stream().anyMatch(p -> !p.getMsgModel().getMsg().isEmpty())) {

            handlerMsgForDrv();
        } else {
            binding.tvMailSrv.setVisibility(View.GONE);
        }
    }

//*************** runProviders  ---------  Сообщения от Провайдера  --------  runProviders***************

    private void runProvidersModeFirst() {

        getAllProvidersClass = GetProvidersClass.getInstance(currentUserUid);
        getAllProvidersClass.setListener(new GetProvidersClass.OnConnectListener() {

            @Override
            public void onNewOrder(ProviderClass providerClass, InfoOrder order) {

                handlerNewFreeOrders(order);

            }

            @Override
            public void onRemoveOrder(ProviderClass providerClass, String key) {

                removeFreeOrderInList(key);
                removePersonalOrderInList(key);
            }


            @Override
            public void onChangeFieldAssignedOrder(ProviderClass providerClass, RedirectOrderModel model, DatabaseReference orderRef) {
//------этот заказ помещаем в список
                getAssignedOrder(providerClass.getSrvRef(), orderRef, providerClass.getNameSrv(), getString(R.string.order_personal), model);
            }

            @Override
            public void onChangeStatusDrvOnProvider(ProviderClass providerClass) {

                handlerStatusDrvOnSrv(providerClass);
                displayStateProviders();
            }

            @Override
            public void onUnConnected(ProviderClass provider) {

                reloadAllFreeOrders();

                if (adapterProviders != null) {
                    adapterProviders.notifyDataSetChanged();
                }

                displayStateProviders();
            }

            //получаем статус СМЕНЫ от сервера
            @Override
            public void onShiftStatus(ProviderClass providerClass) {//DatabaseReference srvRef, ShiftModel shiftModel, String nameSrv

                handlerShiftStatus(providerClass.getSrvRef(), providerClass.getShiftModel(), providerClass.getNameSrv());
                displayStateProviders();
            }

            @Override
            public void onExecutedAssignedOrder(ProviderClass providerClass, String keyOrder) {

                removePersonalOrderInList(keyOrder);
                providerClass.getSrvRef().child("driversList").child(currentUserUid).child("assignedOrder").removeValue();
            }

            @Override
            public void onChangeStatusOrder(String key, int status) {//изменяется когда ордер не свободен иначе новый ордер

                removeFreeOrderInList(key);
                if (status != Util.SEND_TO_DRV_ORDER_STATUS) {
                    removePersonalOrderInList(key);
                }
            }

            @Override
            public void onExecutedRedirectOrder(ProviderClass providerClass, String keyOrder) {
                removePersonalOrderInList(keyOrder);
                providerClass.getSrvRef().child("driversList").child(currentUserUid).child("rOrder").removeValue();
            }

            @Override
            public void onKillRedirectOrder(ProviderClass providerClass, String keyOrder) {
                removePersonalOrderInList(keyOrder);
                providerClass.getSrvRef().child("driversList").child(currentUserUid).child("rOrder").removeValue();
            }

            @Override
            public void onChangeFieldRedirectOrder(ProviderClass providerClass, RedirectOrderModel model, DatabaseReference orderRef) {

                //этот заказ помещаем в список
                getAssignedOrder(providerClass.getSrvRef(), orderRef, providerClass.getNameSrv(), getString(R.string.redirected_order), model);
            }

            @Override
            public void onChangeLocalStatusForProvider(ProviderClass providerClass) {

                handlerLocalStatusProvider(providerClass);
            }
        });
    }

    private void handlerNewFreeOrders(InfoOrder order) {

        reloadAllFreeOrders();

        if (initSuitableAccordingCriteria(order)) {//подходит ли заказ по кретериям
            if (!arrayListRejectedOrders.contains(order.getKeyOrder())) {//--------------------------filter
                if (!driverBusy && onTheLine) {

                    driverBusy = true;//----------------------водитель в процессе заказа

                    displayOrder(order);//--------------окно c поступившим заказом для принятия
                }
            }
        }
    }

    private void displayStateProviders() {

        long count = mapListProvider.stream().filter(p -> p.getStatusDrvOnSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS).count();
        long used = mapListProvider.stream().filter(p -> p.getStatusDrvOnSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS &&
                p.getShiftModel().getStatus() == Util.SHIFT_OPEN_DRV_STATUS).count();

        binding.tvProviders.setText(R.string.providers);
        binding.tvProviders.append(" " + count + "(" + used + ")");
    }


    private void handlerLocalStatusProvider(ProviderClass providerClass) {

        if (providerClass.getStatusLocal() == Util.PAUSE_PROVIDER_STATUS) {
            reloadAllFreeOrders();

        }
        runOnUiThread(() -> {
            if (adapterProviders != null) {
                adapterProviders.notifyDataSetChanged();
            }
            printCountFreeOrders();
        });
    }

    private synchronized void reloadAllFreeOrders() {
        //ордера никапливаются у каждого провайдера свои здель перезаписываем общий массив
        arrayListFreeOrders.clear();

        mapListProvider.forEach(p -> arrayListFreeOrders.addAll(p.orderList));
        printCountFreeOrders();
    }


    private void handlerStatusDrvOnSrv(ProviderClass providerClass) {
        switch (providerClass.getStatusDrvOnSrv()) {
            case Util.PAUSE_PROVIDER_STATUS:
                reloadAllFreeOrders();
//                delAllOrdersByProvider(providerClass.getSrvRef());
                break;
            case Util.UNCONNECTED_PROVIDER_STATUS:

                mapListProvider.remove(providerClass.getSrvRef());
                reloadAllFreeOrders();
//                delAllOrdersByProvider(providerClass.getSrvRef());
                break;
            case Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS:
                reloadAllFreeOrders();
//                delAllOrdersByProvider(providerClass.getSrvRef());
                notifyDisconnectDialog(providerClass.getSrvRef(), providerClass.getNameSrv());

                break;
            case Util.UNKNOWN_DRIVER_STATUS:
                reloadAllFreeOrders();
//                delAllOrdersByProvider(providerClass.getSrvRef());
                notifyAdapterInfoProvider(providerClass.getNameSrv(), providerClass.getSrvRef(), providerClass.getStatusDrvOnSrv(), getString(R.string.data_connected_drv_required));
                break;

            case Util.CONNECTED_TO_SERVER_DRIVER_STATUS:

                notifyChangeInfoProvider(providerClass.getSrvRef());
                break;
        }

        if (adapterProviders != null) {
            adapterProviders.notifyDataSetChanged();
        }
    }

    private void notifyChangeInfoProvider(DatabaseReference srvRef) {
        arrProblemInfoProviders.removeIf(i -> i.getRefSrv().equals(srvRef));
        if (arrProblemInfoProviders.size() == 0) {
            binding.tvInfoProviderImg.setVisibility(View.GONE);
        }
        if (adapterInfoProviders != null) {

            adapterInfoProviders.notifyDataSetChanged();
        }
    }

    private void notifyAdapterInfoProvider(String name, DatabaseReference srvRef, int status, String desc) {
        arrProblemInfoProviders.add(new InfoProviderModel(srvRef, name, status, desc, Util.NOT_FIX_STATE));
        if (adapterInfoProviders != null) {
            adapterInfoProviders.notifyDataSetChanged();
        }
        binding.tvInfoProviderImg.setVisibility(View.VISIBLE);
        binding.tvInfoProviderImg.setText("" + arrProblemInfoProviders.size());
    }

    private void handlerShiftStatus(DatabaseReference srvRef, ShiftModel shiftModel, String nameSrv) {
        switch (shiftModel.getStatus()) {

            case Util.SHIFT_CLOSE_DRV_STATUS:

//                delFreeOrdersByProvider(srvRef);
                reloadAllFreeOrders();
                showMessage(Util.formatTimeDate.format(shiftModel.getTimer()) + "\n" + nameSrv + getString(R.string._shift_closed));
                break;
            case Util.SHIFT_OPEN_DRV_STATUS:

//                showMessage(Util.formatTimeDate.format(shiftModel.getTimer()) + "\n" + nameSrv + getString(R.string._shift_opened));
                break;
            case Util.SHIFT_THE_SYSTEM_IS_DENIED_STATUS:

                showMessage(Util.formatTimeDate.format(shiftModel.getTimer()) + "\n" + nameSrv + " - " + getString(R.string.access_disable));
                break;
            case Util.SHIFT_INSUFFICIENT_FUNDS_DRV_STATUS:

                showMessage(Util.formatTimeDate.format(shiftModel.getTimer()) + "\n" + nameSrv + getString(R.string._insufficient_funds));
                break;
        }

        if (adapterProviders != null) {
            adapterProviders.notifyDataSetChanged();
        }
    }

    private void notifyDisconnectDialog(DatabaseReference srvRef, String name) {
        showMessage(Util.formatTimeDate.format(System.currentTimeMillis()) + "\n" + name + getString(R.string._blocked));
    }

    private void notifyNotDataSettingSnackBar() {
        Snackbar.make(findViewById(android.R.id.content), getString(R.string.not_defined_setting), Snackbar.LENGTH_INDEFINITE).setAction(R.string.ok, new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                Intent intent = new Intent(DriverMapsActivity.this, DriverSettingActivity.class);
                intent.putExtra("location", new DataLocation(currLocation.getLatitude(), currLocation.getLongitude()));
                startActivityForResult(intent, Util.REQUEST_SETTING_DRV);
            }
        }).show();
    }

    private void runRequestClass(InfoRequestConnect reqData, InfoProviderModel infoProvider) {

        if (mapRequests.get(infoProvider.getRefSrv()) == null) {

            RequestSendClass request = new RequestSendClass(infoProvider.getRefSrv(), reqData, currentUserUid, database.getReference());

            request.setOnSuccessfulListener(new RequestSendClass.OnSuccessfulListener() {
                @Override
                public void onSuccessful(boolean success, int s, DatabaseReference srvRef) {

                    request.recoverResource();//-----------------освобождаем ресурсы
                    infoProvider.setState(Util.FIX_STATE);//-------------сообщаем адаптеру
                    adapterInfoProviders.notifyDataSetChanged();

                    if (s > 0) {
                        Toast.makeText(getApplicationContext(), getString(s), Toast.LENGTH_LONG).show();
                    }
                    if (!success) {//что-то не получился запрос
                        mapRequests.remove(srvRef);//-------------удаляем чтобы иметь возможность еще раз запустить
                    }
                }
            });
            mapRequests.put(infoProvider.getRefSrv(), request);
        } else {

            Toast.makeText(getApplicationContext(), R.string.break_send_request, Toast.LENGTH_LONG).show();
        }
    }


    private void setUserDataForRequest(InfoProviderModel info) {//вызывается из активити водителя при фиксировании проблемы(unknown) водителя

        if (validateDataRequest()) {

            executorService.submit(() -> {
                InfoRequestConnect reqData = new InfoRequestConnect(info.getRefSrv().getKey(), settingDrv.getName(), settingDrv.getNameHost(),
                        settingDrv.getPhone(), settingDrv.getOrderCriteria(), settingDrv.getDislocation(), ServerValue.TIMESTAMP, currentUserUid);

                runRequestClass(reqData, info);
            });

        } else {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle(R.string.title_app);
            builder.setIcon(R.drawable.ic_baseline_report_problem_24);
            builder.setMessage(R.string.debug_break_send_request);

            builder.setPositiveButton(getString(R.string.ok), new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    Intent intent = new Intent(DriverMapsActivity.this, DriverSettingActivity.class);
                    intent.putExtra("location", new DataLocation(currLocation.getLatitude(), currLocation.getLongitude()));
                    startActivityForResult(intent, Util.REQUEST_SETTING_DRV);
                }
            });

            AlertDialog dialog = builder.show();
            TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
            messageText.setGravity(Gravity.CENTER);
            dialog.show();
        }

    }

    private boolean validateDataRequest() {
        return (!settingDrv.getName().isEmpty() && settingDrv.getName().length() > 1) &&
                !settingDrv.getPhone().isEmpty() &&
                settingDrv.getAuto().getAutoModel().length() > 1 &&
                settingDrv.getAuto().getAutoColor().length() > 1 &&
                settingDrv.getAuto().getAutoNumber().length() > 1 &&
                (!settingDrv.getOrderCriteria().isEmpty() && settingDrv.getOrderCriteria().length() > 1) &&
                settingDrv.getDislocation().getLatitude() != 0 &&
                settingDrv.getDislocation().getLongitude() != 0;
    }


    private synchronized void getAssignedOrder(DatabaseReference serverRef, DatabaseReference orderRef, String nameSrv, String title, RedirectOrderModel model) {

        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    InfoOrder order = snapshot.getValue(InfoOrder.class);
                    if (order != null) {
                        if (Objects.equals(order.getDriverUid(), "")) {//никто не назначен

                            if (order.getStatus() == Util.SEND_TO_DRV_ORDER_STATUS) {//заказ направлен
                                order.setProviderName(title + nameSrv);

                                removePersonalOrderInList(order.getKeyOrder());
                                arrayListPersonalOrders.add(order);
                                printCountPersonalOrders();

                                if (!driverBusy && onTheLine) {//если не занят И на линии то показывается заказ на выполнение

                                    driverBusy = true;
                                    orderRef.child("status").setValue(Util.ASSIGN_ORDER_STATUS, new DatabaseReference.CompletionListener() {
                                        @Override
                                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                            if (error == null) {
//                                                setStatusDrvRef(Util.BUSY_DRIVER_STATUS);//----------занят
                                                sharedDrvRef.child("status").setValue(Util.BUSY_DRIVER_STATUS, new DatabaseReference.CompletionListener() {
                                                    @Override
                                                    public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                                        if (error == null) {
                                                            regExOrderRef(serverRef.getKey(), order.getKeyOrder());//------сохраняем ключ ордера и ключ поставщика выполняемого заказа
                                                            displayGuideOrder(order, new DataLocale(currLocale.getLanguage(), Currency.getInstance(Locale.getDefault()).getSymbol()));//-------------------------------продолжение маршрута

                                                        }
                                                    }
                                                });
                                            }
                                        }
                                    });
                                }
                            } else {

                                Toast.makeText(getApplicationContext(), R.string.assigned_order_break, Toast.LENGTH_SHORT).show();
                                resetAssignedOrder(order.getKeyOrder(), serverRef);
                            }
                        } else {//не свободен - другой водитель

                            if (!order.getDriverUid().equals(currentUserUid)) {
                                Toast.makeText(getApplicationContext(), R.string.order_execute, Toast.LENGTH_SHORT).show();
                            }
                            resetAssignedOrder(order.getKeyOrder(), serverRef);
                        }
                    } else {//не получен

                        Toast.makeText(getApplicationContext(), R.string.order_receive_break, Toast.LENGTH_SHORT).show();
                        resetAssignedOrder(orderRef.getKey(), serverRef);
                    }
                } else {//нет заказа

                    resetAssignedOrder(orderRef.getKey(), serverRef);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                resetAssignedOrder(orderRef.getKey(), serverRef);
            }
        });
    }

    private void resetAssignedOrder(String keyOrder, DatabaseReference serverRef) {
        driverBusy = false;
        removePersonalOrderInList(keyOrder);
        serverRef.child("driversList").child(currentUserUid).child("assignedOrder").removeValue();
//        setStatusDrvRef(Util.FREE_DRIVER_STATUS);//----------свободен

        sharedDrvRef.child("status").setValue(Util.FREE_DRIVER_STATUS);//----------свободен
    }

    private void regExOrderRef(String keySrv, String keyOrder) {
//host используется для получении информации о водителе,заказе и т.д - сервером
        ExOrderModel exOrder = new ExOrderModel(keySrv, keyOrder);
        sharedDrvRef.child("exOrder").setValue(exOrder);

    }

    private void handlerMsgForDrv() {

        mp = MediaPlayer.create(getApplicationContext(), R.raw.sms);
        mp.start();
        binding.tvMailSrv.setVisibility(View.VISIBLE);
        binding.tvMailSrv.setText("" + mapListProvider.stream().filter(p -> !p.getMsgModel().getMsg().isEmpty()).count());


    }

    private void showListMsg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(DriverMapsActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_list_message, null);

        builder.setIcon(R.drawable.ic_baseline_chat_24);
        builder.setTitle(R.string.t_unread_msg);
        builder.setView(dialogView);

        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvListMessage);
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);

        List<ProviderClass> filterList = mapListProvider.stream().filter(p -> !p.getMsgModel().getMsg().isEmpty()).collect(Collectors.toList());
        AdapterMsgListSrv adapter = new AdapterMsgListSrv(filterList);
        rv.setAdapter(adapter);


        adapter.setSelectListener(new AdapterMsgListSrv.OnSelectListener() {
            @Override
            public void onSelectItem(ProviderClass provider, int position) {
                if (provider != null) {
                    String title = provider.getNameSrv();

                    Util.dlgMessage(DriverMapsActivity.this, title, provider.getMsgModel().getMsg(), "", provider.getSrvRef().child("driversList").child(currentUserUid).child("msgS"));
                }
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                removeMessageInList(filterList);
            }
        });

        builder.create().show();
    }

    private void removeMessageInList(List<ProviderClass> list) {

        list.forEach(this::removeMsgByRef);

    }

    private void removeMsgByRef(ProviderClass provider) {

        DatabaseReference reference = provider.getSrvRef().child("driversList").child(currentUserUid).child("msgD/msg");

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    provider.getMsgModel().setMsg("");
                    reference.setValue("");

                    if (mapListProvider.stream().allMatch(p -> p.getMsgModel().getMsg().isEmpty())) {

                        binding.tvMailSrv.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private boolean initSuitableAccordingCriteria(InfoOrder order) {
        return isOrderSuitableAccordingCriteria(order) && isOrderSuitableAccordingDistance(order);
    }

    private boolean isOrderSuitableAccordingCriteria(InfoOrder order) {

        String[] words = settingDrv.getOrderCriteria().trim().split("\\s*,\\s*");//критерий у водителя
        String[] words2 = order.getTypeTr().trim().split("\\s*,\\s*");//критерий у заказа
        if (Arrays.asList(words).containsAll(Arrays.asList(words2))) {
            return true;
        }

        return false;
    }

    private boolean isOrderSuitableAccordingDistance(InfoOrder order) {

        float distance = calculationDistance(order.getFrom().getLatitude(), order.getFrom().getLongitude(), currLocation.getLatitude(), currLocation.getLongitude());
        if (distance > -1 && distance <= settingDrv.getRadius()) {
            return true;
        }
        return false;
    }

    private float calculationDistance(double fromLat, double fromLong, double toLat, double toLong) {
        float[] results = new float[1];
        if (fromLat != 0 && fromLong != 0 && toLat != 0 && toLong != 0) {
            Location.distanceBetween(fromLat, fromLong, toLat, toLong, results);
            return results[0];
        }
        return -1;
    }

    private void removeFreeOrderInList(String key) {
        arrayListFreeOrders.removeIf(s -> s.getKeyOrder().equals(key));
        printCountFreeOrders();
    }

    private void removePersonalOrderInList(String key) {
        arrayListPersonalOrders.removeIf(s -> s.getKeyOrder().equals(key));
        printCountPersonalOrders();
    }


    private void commandOneMenu(MenuItem item) {
        Intent intent;
        switch (item.getItemId()) {
            case R.id.actionShowReport:

                intent = new Intent(DriverMapsActivity.this, ReportDrvActivity.class);
                startActivity(intent);
                break;
            case R.id.actionSetting:

                intent = new Intent(DriverMapsActivity.this, DriverSettingActivity.class);
                intent.putExtra("location", new DataLocation(currLocation.getLatitude(), currLocation.getLongitude()));
                startActivityForResult(intent, Util.REQUEST_SETTING_DRV);
                break;
            case R.id.actionProviders:

                displayConnProviders();
                break;
            case R.id.actionStartServer:

                handlerStartServer();
                break;
        }
    }

    private void handlerStartServer() {
        if (services == null) {

            hostServersRef.child(currentUserUid).child("info/name").addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {//сервер есть
                        String name = snapshot.getValue(String.class);
                        if (name != null) {
                            ServerInformsAboutEventsClass.setting.setServerName(name);
                        }
                        RunServicesServer.starterServer = true;
                        runServicesClassListener();
                    } else {
                        Toast.makeText(getApplicationContext(), R.string.server_not_found, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void runEventsServerClass(SettingServer settingSrv) {
        ServerInformsAboutEventsClass.setting = settingSrv;

        handlerEventClassListeners();
    }

    private void handlerEventClassListeners() {
        serverEvents = ServerInformsAboutEventsClass.getInstance();//содержит общую ссылку на настройка сервера, уведомляет о события на сервере в акт.водителю
        serverEvents.setListener(new ServerInformsAboutEventsClass.OnListener() {
            @Override
            public void onEvents(long countEvents) {
                runOnUiThread(() -> {

                    if (countEvents > 0) {

                        binding.ibMyServer.setImageResource(R.drawable.ic_baseline_notifications_24);
                    } else {

                        binding.ibMyServer.setImageResource(R.drawable.ic_baseline_storage_24);
                    }
                });
            }

            @Override
            public void onLoad() {
                runOnUiThread(() -> {

                    binding.ibMyServer.setVisibility(View.VISIBLE);
                    Toast.makeText(getApplicationContext(), R.string.server_run, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }


    private void runActionShift(ProviderClass provider, int action) {

        provider.getSrvRef().child("driversList").child(currentUserUid).child("shift").setValue(action, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {

                } else {

                }
            }
        });

    }

    //****//////////////////////////////////////   Providers  Providers  Providers  Providers  Providers

    private void displayConnProviders() {

        AlertDialog.Builder builder = new AlertDialog.Builder(DriverMapsActivity.this);
        builder.setCancelable(false);
        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_available_list, null);
        builder.setTitle(R.string.connected_providers);
        builder.setView(dialogView);


        RecyclerView rvProviders = (RecyclerView) dialogView.findViewById(R.id.rvAvailableList);
        rvProviders.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rvProviders.setLayoutManager(layoutManager);

        //в этом списке можно остановить/продолжить работу провайдера
//        adapterProviders = new AdapterProviders(mapListProvider.values().stream().filter(p -> p.getStatusDrvOnSrv() != Util.UNCONNECTED_PROVIDER_STATUS &&
//                p.getStatusDrvOnSrv() != Util.UNKNOWN_DRIVER_STATUS).collect(Collectors.toList()));

        adapterProviders = new AdapterProviders(mapListProvider);

        rvProviders.setAdapter(adapterProviders);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                adapterProviders = null;
            }
        });

        handlerAdapterProviderListeners();

        AlertDialog dialog = builder.create();
        dialog.show();
    }


    private void handlerAdapterProviderListeners() {
        adapterProviders.setListeners(new AdapterProviders.OnListeners() {

            @Override
            public void onSendMsgSrv(DatabaseReference ref, String s) {
                Util.sendMsgByRef(s, ref.child("driversList").child(currentUserUid).child("msgS"));
            }

            @Override
            public void onMenuItemClick(MenuItem item, ProviderClass provider) {
                handlerAdapterProviderMenuClick(item, provider);
            }
        });
    }

/////////////////////////////////////////// adapterProvider  adapterProvider  adapterProvider/////////////////////////

    private void handlerAdapterProviderMenuClick(MenuItem item, ProviderClass provider) {
        switch (item.getItemId()) {

            case R.id.actionStopProvList:

                provider.setStatusLocal(Util.PAUSE_PROVIDER_STATUS);
                break;
            case R.id.actionContinueProvList:

                provider.setStatusLocal(Util.RUNNING_PROVIDER_STATUS);
                break;
            case R.id.actionShift:

                handlerMenuActionShift(provider);
                break;
        }
    }

    private void handlerMenuActionShift(ProviderClass provider) {
        if (provider.getStatusDrvOnSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {

            if (provider.getShiftModel().getStatus() != Util.SHIFT_OPEN_DRV_STATUS) {//смена не открыта
                //open
                handlerActionShift(provider, Util.ACTION_OPEN_SHIFT, getString(R.string.open_shift_));

            } else if (provider.getShiftModel().getStatus() != Util.SHIFT_CLOSE_DRV_STATUS) {//смена не закрыта
                //close
                handlerActionShift(provider, Util.ACTION_CLOSE_SHIFT, getString(R.string.close_shift_));
            }

        } else {

            Toast.makeText(getApplicationContext(), getString(R.string.provider_2) + provider.getNameSrv() + "\n" + getString(R.string.debug_open_shift_disabled), Toast.LENGTH_SHORT).show();
        }
    }

    private void handlerActionShift(ProviderClass provider, int actionShift, String s) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_app);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setMessage(s);

        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
                if (provider != null) {
//                    notifyShiftStatus = true;
                    runActionShift(provider, actionShift);
                    Toast.makeText(getApplicationContext(), R.string.request_sender, Toast.LENGTH_SHORT).show();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });

        AlertDialog dialog = builder.show();
        TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
        dialog.show();
    }


    private void setViewOnTheLineButton(int select) {
        if (select == Util.SELECT_BUTTON) {

            onTheLine = true;
            binding.getOnTheLineButton.setActivated(true);
            binding.getOnTheLineButton.setText(GET_OFF_THE_LINE);//уйти с линии

            getFreeOrder();
        } else {

            onTheLine = false;
            binding.getOnTheLineButton.setActivated(false);
            binding.getOnTheLineButton.setText(GET_ON_THE_LINE);//встать на линию
        }
    }

    private void getFreeOrder() {
        if (!driverBusy) {//свободен
            for (InfoOrder order : arrayListFreeOrders) {
                if (initSuitableAccordingCriteria(order)) {//подходит ли заказ по кретериям
                    if (!arrayListRejectedOrders.contains(order.getKeyOrder())) {//--------------------------filter

                        if (!driverBusy) {
                            driverBusy = true;
                            displayOrder(order);//--------------окно c поступившим заказом для принятия
                        }
                    }
                }
            }
        }
    }

    private void printCountFreeOrders() {
        int count = arrayListFreeOrders.size();
        if (count > 0) {
            binding.btnFreeOrders.setEnabled(true);
            binding.btnFreeOrders.setActivated(true);
            binding.btnFreeOrders.setText(getString(R.string.free));
            binding.btnFreeOrders.append(" (" + count + ")");
            System.out.println(Thread.currentThread().getName());
        } else {
            binding.btnFreeOrders.setActivated(false);
            binding.btnFreeOrders.setEnabled(false);
            binding.btnFreeOrders.setText(R.string.drv_map_free);
        }
    }

    private void printCountPersonalOrders() {
        int count = arrayListPersonalOrders.size();
        if (count > 0) {
            binding.btnPersonOrder.setEnabled(true);
            binding.btnPersonOrder.setActivated(true);
            binding.btnPersonOrder.setText(getString(R.string.assigned));
            binding.btnPersonOrder.append(" (" + count + ")");
        } else {
            binding.btnPersonOrder.setActivated(false);
            binding.btnPersonOrder.setEnabled(false);
            binding.btnPersonOrder.setText(R.string.drv_map_assigned);
        }
    }

    private void displayGuideOrder(InfoOrder infoOrder, DataLocale dataLocale) {

        saveDataExecutableOrder(infoOrder, dataLocale);

        Intent intent = new Intent(DriverMapsActivity.this, OrderGuideActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("data_order", infoOrder);
        bundle.putParcelable("locale", dataLocale);

        intent.putExtras(bundle);

        startActivityForResult(intent, Util.REQUEST_GUIDE_ORDER);

    }

    private void saveDataExecutableOrder(InfoOrder infoOrder, DataLocale dataLocale) {
        exOrder = db.getExecutableOrderDAO().getExecutableOrder(currentUserUid);
        if (exOrder == null) {

            exOrder = new DataExecutableOrder(1,
                    infoOrder.getKeyOrder(),
                    infoOrder.getProviderKey(),
                    dataLocale,
                    infoOrder.getProviderName(),
                    currentUserUid);

            db.getExecutableOrderDAO().add(exOrder);
        } else {

            exOrder.setKeyOrder(infoOrder.getKeyOrder());
            exOrder.setKeySrv(infoOrder.getProviderKey());
            exOrder.setLocale(dataLocale);
            exOrder.setNameSrv(infoOrder.getProviderName());

            db.getExecutableOrderDAO().update(exOrder);
        }
    }

    private void displayOrder(InfoOrder infoOrder) {

        Intent intent = new Intent(DriverMapsActivity.this, OrderDisplayActivity.class);

        Bundle bundle = new Bundle();
        bundle.putParcelable("data_order", infoOrder);


        intent.putExtras(bundle);
        intent.putExtra("isSound", settingDrv.isSoundNotification());
        startActivityForResult(intent, Util.REQUEST_ACCEPT_ORDER);

    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMaxZoomPreference(18);
        mMap.moveCamera(CameraUpdateFactory.zoomBy(mZoom));
        driverMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(0, 0)).title(getString(R.string.I)));

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mZoom = mMap.getCameraPosition().zoom;

            }
        });
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case Util.REQUEST_ACCEPT_ORDER:

                handlerResultAcceptOrder(resultCode, data);
                break;
            case Util.REQUEST_GUIDE_ORDER:

                handlerResultGuideOrder(resultCode);
                break;

            case Util.REQUEST_SELECT_FREE_ORDER:

                handlerResultSelectFreeOrder(resultCode, data);
                break;
            case Util.REQUEST_SELECT_PERSONAL_ORDER:

                handlerResultSelectPersonalOrder(resultCode, data);
                break;
            case Util.REQUEST_SETTING_DRV:

                handlerResultRequestSetting(resultCode);
                break;
        }
    }


    private void handlerResultRequestSetting(int resultCode) {
        switch (resultCode) {
            case Util.RESULT_OK:
                try {

                    updateSettingDrv(getSetting());//обновление
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
                break;
            case Util.SIGN_OUT:

                signOut();
                break;
        }
    }

    private void updateSettingDrv(SettingDrv settingDB) {
        //currUid
        settingDrv.setUsedProviders(settingDB.getUsedProviders());
        settingDrv.setSoundNotification(settingDB.isSoundNotification());
        settingDrv.setPassword(settingDB.getPassword());
        settingDrv.setName(settingDB.getName());
        settingDrv.setNameHost(settingDB.getNameHost());
        settingDrv.setPhone(settingDB.getPhone());
        settingDrv.setAccessPassSOS(settingDB.isAccessPassSOS());
        settingDrv.setVoicingAmount(settingDB.isVoicingAmount());
        settingDrv.setRunServer(settingDB.isRunServer());
        //serverUid
        settingDrv.setAuto(settingDB.getAuto());
        settingDrv.setOrderCriteria(settingDB.getOrderCriteria());
        settingDrv.setDislocation(settingDB.getDislocation());

        //loginK
        settingDrv.setSrvName(settingDB.getSrvName());

        if (settingDrv.getRadius() != settingDB.getRadius()) {
            settingDrv.setRadius(settingDB.getRadius());
            resetFreeOrders();
        }
    }

    private void resetFreeOrders() {

        getAllProvidersClass.resetFreeOrders();
    }

    private void signOut() {
        Intent intent = new Intent(DriverMapsActivity.this, ChooseModeActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

//        auth.signOut();

        startActivity(intent);
    }

    private void handlerResultSelectPersonalOrder(int resultCode, Intent data) {
        if (resultCode == Util.ACCEPT_OK) {
            if (data != null) {

                String keyOrder = data.getStringExtra("key_order");
                if (keyOrder != null && !keyOrder.isEmpty()) {

                    InfoOrder order = arrayListPersonalOrders.stream().filter(o -> o.getKeyOrder().equals(keyOrder)).findAny().orElse(null);
                    if (order != null) {

                        driverBusy = true;
                        assignDrvToPersonalOrder(order);//прямое назначение
                    }
                }
            }
        }
    }

    private void handlerResultSelectFreeOrder(int resultCode, Intent data) {
        if (resultCode == Util.ACCEPT_OK) {
            if (data != null) {

                String keyOrder = data.getStringExtra("key_order");
                if (keyOrder != null && !keyOrder.isEmpty()) {

                    InfoOrder order = arrayListFreeOrders.stream().filter(o -> o.getKeyOrder().equals(keyOrder)).findAny().orElse(null);
                    if (order != null) {

                        driverBusy = true;
                        assignDrvToFreeOrder(order);//назначение с дозвоном
                    }
                }
            }
        }
    }


    private void handlerResultAcceptOrder(int resultCode, Intent data) {
        switch (resultCode) {
            case Util.ACCEPT_OK://---------- принял заказ

                if (data != null) {
                    InfoOrder infoOrder = (InfoOrder) data.getParcelableExtra("data_order");
                    if (infoOrder != null) {
                        driverBusy = true;// водитель занят

                        assignDrvToFreeOrder(infoOrder);
                    }
                }
                break;
            case Util.ACCEPT_CANCEL://---------- не принял, отказался от заказа
                driverBusy = false;// водитель не занят

                if (data != null) {
                    if (!arrayListRejectedOrders.contains(data.getStringExtra("keyOrder"))) {
                        arrayListRejectedOrders.add(data.getStringExtra("keyOrder"));
                    }
                }
                setViewOnTheLineButton(Util.SELECT_BUTTON);// на линии
                if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomBy(mZoom));
                break;
        }
    }

    private void handlerResultGuideOrder(int resultCode) {
        driverBusy = false;//не занят
//        setStatusFreeDrvRef();//свободен

        sharedDrvRef.child("status").setValue(Util.FREE_DRIVER_STATUS, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    executorService.submit(() -> {
                        deleteDataExOrder();
                        clearExOrderRef();
                    });

                    switch (resultCode) {
                        case Util.RESULT_CANCELED_DRIVER:

                            showMessage(getString(R.string.canceled_driver));
                            setViewOnTheLineButton(Util.SELECT_BUTTON);// на линии
                            break;
                        case Util.RESULT_CHANGE_DRIVER:

                            showMessage(getString(R.string.another_driver_assigned));
                            setViewOnTheLineButton(Util.SELECT_BUTTON);// на линии
                            break;
                        case Util.ACCEPT_OK://-----------------заказ завершен водителем,перенаправлен - не чего не делается!
                        case Util.REDIRECT_OK://-----------------заказ перенаправлен - не чего не делается!
                        case Util.RESULT_DROP_ORDER://-----------сброс заказа

                            setViewOnTheLineButton(Util.NOT_SELECT_BUTTON);//не на линии
                            break;
                        case Util.RESULT_KILL_ORDER://-----------заказ удален из системы

                            showMessage(getString(R.string.order_del));
                            setViewOnTheLineButton(Util.SELECT_BUTTON);// на линии
                            break;
                    }

                    if (mMap != null) mMap.animateCamera(CameraUpdateFactory.zoomBy(mZoom));
                }
            }
        });


    }

    private void clearExOrderRef() {
        Map<String, Object> map = new HashMap<>();
        map.put("keyOrder", "");
        map.put("keySrv", "");

        sharedServerRef.child("driversList").child(currentUserUid).child("exOrder").updateChildren(map);
    }

    private void assignDrvToPersonalOrder(InfoOrder order) {
        DatabaseReference orderRef;
        if (order.getProviderKey().equals("SHAREDSERVER")) {
            orderRef = sharedServerRef.child("freeOrders").child(order.getKeyOrder());
        } else {
            orderRef = hostServersRef.child(order.getProviderKey()).child("freeOrders").child(order.getKeyOrder());
        }


        AssignedToPersonalOrderClass assigned = new AssignedToPersonalOrderClass(orderRef, currentUserUid);

        assignedClassList.add(assigned);

        assigned.setOnRegistrationListener(new AssignedToPersonalOrderClass.OnRegistrationListener() {
            @Override
            public void onRegistrationOrder(boolean success) {
                if (success) {

                    orderRef.child("status").setValue(Util.ASSIGN_ORDER_STATUS, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error == null) {
                                driverBusy = true;

//                                setStatusDrvRef(Util.BUSY_DRIVER_STATUS);//----------занят
                                sharedDrvRef.child("status").setValue(Util.BUSY_DRIVER_STATUS, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                        if (error == null) {

                                            setExOrderDrv(order);
                                            displayGuideOrder(order, new DataLocale(currLocale.getLanguage(), Currency.getInstance(Locale.getDefault()).getSymbol()));
                                        }
                                    }
                                });
                            }
                        }
                    });


                } else {

                    driverBusy = false;//свободен
                    Toast.makeText(getApplicationContext(), R.string.receive_order_break, Toast.LENGTH_SHORT).show();
                }

                removePersonalOrderInList(order.getKeyOrder());

                assignedClassList.remove(assigned);
            }
        });
    }

//    private void setStatusFreeDrvRef() {
//        sharedDrvRef.child("status").setValue(Util.FREE_DRIVER_STATUS);
//
//        executorService.submit(this::deleteDataExOrder);
//    }

    private void deleteDataExOrder() {
        db.getExecutableOrderDAO().deleteDataExOrder(currentUserUid);
    }


    //регистрация На Заказ - звоним и назначил меня на выполнение заказа
    private void assignDrvToFreeOrder(@NonNull InfoOrder order) {
        DatabaseReference srvRef;
        if (order.getProviderKey().equals("SHAREDSERVER")) {
            srvRef = sharedServerRef;
        } else {
            srvRef = hostServersRef.child(order.getProviderKey());
        }

        AssignedToFreeOrderClass regOrder = new AssignedToFreeOrderClass(DriverMapsActivity.this, order, 15, srvRef, currentUserUid);
        arrayListRegToOrder.add(regOrder);

        regOrder.setOnRegistrationListener(new AssignedToFreeOrderClass.OnRegistrationListener() {
            @Override
            public void onRegistrationOrder(InfoOrder order, boolean successful) {

                if (successful) {
                    driverBusy = true;//занят
//                    setStatusDrvRef(Util.BUSY_DRIVER_STATUS);//----------занят
                    sharedDrvRef.child("status").setValue(Util.BUSY_DRIVER_STATUS, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error == null) {
                                setExOrderDrv(order);
                                displayGuideOrder(order, new DataLocale(currLocale.getLanguage(), Currency.getInstance(Locale.getDefault()).getSymbol()));
                            }
                        }
                    });

                } else {

                    driverBusy = false;//свободен
                    Toast.makeText(getApplicationContext(), R.string.receive_order_break, Toast.LENGTH_SHORT).show();
                }

                removeFreeOrderInList(order.getKeyOrder());

                regOrder.recoverResources();
                arrayListRegToOrder.remove(regOrder);
            }

            @Override
            public void onAnotherDrvAssigned() {

                driverBusy = false;//свободен
                showMessage(getString(R.string.another_driver_assigned));

                regOrder.recoverResources();
                arrayListRegToOrder.remove(regOrder);

//----------------------------------------- arrayListFreeOrders
                removeFreeOrderInList(order.getKeyOrder());
                removePersonalOrderInList(order.getKeyOrder());
            }
        });
    }

    private void setExOrderDrv(InfoOrder order) {//эти данные для получения информации сервером
        ExOrderModel exOrder = new ExOrderModel(order.getProviderKey(), order.getKeyOrder());
        sharedDrvRef.child("exOrder").setValue(exOrder);
    }

    private void setStatusDrvRef(int busyDrvStatus) {
        sharedDrvRef.child("status").setValue(busyDrvStatus);

    }


    private void showMessage(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_app);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setMessage(message);


        builder.setPositiveButton("ok", new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

            }
        });

        AlertDialog dialog = builder.show();
        TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
        dialog.show();

    }

    @Override
    protected void onStart() {
        startClasses();
        binding.flBlock.setVisibility(View.INVISIBLE);
        super.onStart();
    }

    private void startClasses() {


        runServicesClassListener();//если сервис запущен то перегрузка слушателей для обработки этой активити

        runClassLocation();
        runClassSOS();
        runProvidersModeFirst();
        runProvidersModeTwo();
//        handlerTrackingConnectedServersListener();
        handlerEventClassListeners();

    }

    private void runServicesClassListener() {
        if (RunServicesServer.starterServer) {

            services = RunServicesServer.getInstance(hostServersRef.child(currentUserUid), auth.getCurrentUser());

            services.setOnListener(new RunServicesServer.OnListener() {
                @Override
                public void showRequest(InfoRequestConnect requestModel, long count) {

                }

                @Override
                public void onExitReadRequest(boolean bSuccess) {

                }

                @Override
                public void onHasRequest(RequestAcceptClass requestClass, boolean b, long count) {
                    if (b) {
                        serverEvents.onListener.onEvents(1);
                    }
                }

                @Override
                public void onBreakSaveDriver(String descriptionDeny) {

                }

                @Override
                public void onError(String err) {
                    Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
                }

            });

            services.setOnClientOrderListener(new RunServicesServer.OnClientOrderListener() {
                @Override
                public void onPostRequestClientOrder(AcceptClientOrderClass orderClass, String keySender, String name) {
                    serverEvents.onListener.onEvents(1);//serverEvents постоянно слушает, при запуске в активити события этого класса перегружиется где чего не делается

                }

                @Override
                public void onPostClientOrder(String keySender) {

                }

                @Override
                public void onConfirmation(AcceptClientOrderClass orderClass, String name, DataSenderModel dataSender) {

                }
            });
        }

    }


    private void runClassLocation() {
        locationClass = BuildLocationClass.getInstance(DriverMapsActivity.this, locationRef);
        locationClass.setOnUpdateListener(new BuildLocationClass.OnUpdateLocationListener() {
            @Override
            public void onUpdateLocation(Location location, int satellites) {

                currLocation = location;

                updateLocationUi(satellites);
            }

        });
        locationClass.getCurrentLocation();

    }

    private void runClassSOS() {
        sosClass = SOSClass.getInstance(auth.getCurrentUser(), sosHostRef);

        sosClass.setOnUpdateListener(new SOSClass.OnUpdateListener() {
            @Override
            public void onOnSOS() {
                binding.tvSOS1.setBackgroundTintList(ColorStateList.valueOf(Color.RED));
                statusSOS = Util.SOS_ON;
            }

            @Override
            public void onCount(int size) {
                binding.tvSOS1.setText(size > 0 ? (getString(R.string.sos) + " " + size) : getString(R.string.sos));
            }

            @Override
            public void onOffSOS() {
                binding.tvSOS1.setBackgroundTintList(ColorStateList.valueOf(getColor(R.color.greyLight)));
                statusSOS = Util.SOS_OFF;
            }
        });
        sosClass.initStatus();
    }

    @Override
    protected void onPause() {
        binding.flBlock.setVisibility(View.GONE);
        super.onPause();
    }


    private void updateLocationUi(int satellites) {

        if (currLocation != null) {

            if (mMap != null) {
                LatLng drvLoc = new LatLng(currLocation.getLatitude(), currLocation.getLongitude());//----------получение координат

                if (driverMarker == null) {
                    driverMarker = mMap.addMarker(new MarkerOptions().position(drvLoc).title(getString(R.string.I)));
                } else {
                    driverMarker.setPosition(drvLoc);
                }

                setCameraInTheDirection(currLocation);
            }
            if (satellites > 3) {

                binding.ibGPS.setColorFilter(Color.GREEN);
            } else {
                binding.ibGPS.setColorFilter(Color.rgb(255, 200, 0));

            }

        } else {

            binding.ibGPS.setColorFilter(Color.RED);
        }
    }

    private void setCameraInTheDirection(Location location) {

        CameraPosition position = CameraPosition.builder()
                .bearing(location.getBearing())
                .target(new LatLng(driverMarker.getPosition().latitude, driverMarker.getPosition().longitude))
                .zoom(mZoom)
                .tilt(mMap.getCameraPosition().tilt)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
    }

    @Override
    protected void onDestroy() {

//        try {
        getAllProvidersClass.recoveryResources();
        trackingHostServers.recoverResources();
        mapListProvider.clear();

        Util.removeAllValueListener(mapRegStreamListeners);
        Util.removeAllValueListener(mapListeners);
        Util.removeAllChildListener(mapChildListeners);

        if (mp != null) mp.release();
        locationClass.recoveryResources();

        mapRequests.forEach((k, r) -> r.stop());

//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
        super.onDestroy();
//        }
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }
}