package com.alexei.taxiapp.driver.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityOrderGuideBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.DataTaximeter;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.db.RatesSrv;
import com.alexei.taxiapp.driver.adapter.AdapterMsgListSrv;
import com.alexei.taxiapp.driver.exClass.DataRouteClass;
import com.alexei.taxiapp.driver.exClass.RedirectOrderForDrvClass;
import com.alexei.taxiapp.driver.exClass.SOSClass;
import com.alexei.taxiapp.driver.exClass.TaximeterExecutorClass;
import com.alexei.taxiapp.driver.model.DataLocale;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.driver.model.RateModel;
import com.alexei.taxiapp.driver.model.SOSModel;
import com.alexei.taxiapp.driver.provider.exClass.GetProvidersClass;
import com.alexei.taxiapp.driver.provider.exClass.ProviderClass;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.alexei.taxiapp.server.model.MsgModel;
import com.alexei.taxiapp.server.model.RouteInfoModel;
import com.alexei.taxiapp.util.Util;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
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
import java.util.concurrent.ScheduledExecutorService;
import java.util.stream.Collectors;

public class OrderGuideActivity extends FragmentActivity implements OnMapReadyCallback, TextToSpeech.OnInitListener {

    private GoogleMap mMap;
    private ActivityOrderGuideBinding binding;
    private AppDatabase db;
    private DataRouteClass routeClass;

    private Marker driverMarker;
    private Marker passengerMarker;
    private Polyline line;
    private float mZoom = 16;
    private MediaPlayer mp;

    private FirebaseAuth auth;
    private FirebaseDatabase database;

    private DatabaseReference hostSharedSrvRef;
    private DatabaseReference hostServersRef;
    private DatabaseReference locationRef;
    private DatabaseReference senderSrvRef;
    private DatabaseReference orderRef;
    private DatabaseReference sosHostRef;
    private String keyOrder;
    private DataLocation mFrom;
    private DataLocation mTo;
    private DataLocation mLocationNavigator;
    private InfoOrder infoOrder;

    private final Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private List<RedirectOrderForDrvClass> redirectList = new ArrayList<>();
    private Map<String, String> mapMenuLang = new HashMap<>();
    private Location currLocation = new Location("");
    private TaximeterExecutorClass executor;

    private int statusSOS = Util.SOS_OFF;
    private int buttonAction = Util.CAR_PULLED_UP;
    private String currentUserUid;
    private int typeDisplay = 0;

    private ExecutorService executorservice;
    private ScheduledExecutorService service;
    private BuildLocationClass locationClass;
    private SOSClass sosClass;
    private GetProvidersClass providersClass;

    private DataTaximeter dataExTaximeter;
    private DataLocale intentLocale;
    private Locale menuLocale = Locale.getDefault();
    private String mLanguage = Locale.getDefault().getLanguage();
    private boolean mIsInit;
    private PopupMenu popupMenu;
    private TextToSpeech mTextToSpeech;
    private float mAmountMoney = 0;
    private Location oldLoc = new Location("");//для навигации

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityOrderGuideBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        setContentView(R.layout.activity_order_guide);

        setRequestedOrientation(getResources().getConfiguration().orientation);


        executorservice = Executors.newFixedThreadPool(2);
        service = Executors.newScheduledThreadPool(1);


        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapGuide);
        mapFragment.getMapAsync(this);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы

        mFrom = new DataLocation();
        mTo = new DataLocation();
        mLocationNavigator = new DataLocation();

        popupMenu = new PopupMenu(OrderGuideActivity.this, binding.buttonMenu);
        popupMenu.inflate(R.menu.order_guide_menu);


        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных

        if (auth.getCurrentUser() == null) {
            finish();
        } else {
            currentUserUid = auth.getCurrentUser().getUid();
            hostSharedSrvRef = database.getReference().child("SHAREDSERVER");
            hostServersRef = database.getReference().child("serverList");
            locationRef = database.getReference().child("SHAREDSERVER/driversList").child(currentUserUid).child("location");
            sosHostRef = database.getReference().child("sos");
        }

        Intent intentData = getIntent();
        if (intentData != null) {

            infoOrder = (InfoOrder) intentData.getParcelableExtra("data_order");
            intentLocale = intentData.getParcelableExtra("locale");


            if (infoOrder.getProviderKey().equals("SHAREDSERVER")) {
                orderRef = hostSharedSrvRef.child("freeOrders").child(infoOrder.getKeyOrder());//нахождение заказа

            } else {
                orderRef = hostServersRef.child(infoOrder.getProviderKey()).child("freeOrders").child(infoOrder.getKeyOrder());//нахождение заказа
            }

            senderSrvRef = hostServersRef.child(infoOrder.getSenderKey().equals("") ? infoOrder.getProviderKey() : infoOrder.getSenderKey());//нахождение сервера отправителя


            runClassLoc();

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    handlerFirstMenuItemClick(item);
                    return true;
                }
            });

            binding.tvMailFromSrv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showListMsg();
                }
            });

            binding.ibMailFromClient.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showMsgMail();
                }
            });

//            buttonClose.setOnClickListener(new View.OnClickListener() {
//                @Override
//                public void onClick(View view) {
//                    close();
//                }
//            });

            binding.tvCurrentRate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    executorservice.submit(() -> {
                        selectedRate();
                    });
                }
            });

            binding.buttonMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popupMenu.getMenu().findItem(R.id.menuCmd5).setEnabled(!Objects.equals(infoOrder.getProviderKey(), "SHAREDSERVER"));
                    popupMenu.getMenu().findItem(R.id.menuCmd2).setEnabled(infoOrder.getClientUid().length() > 1);

                    popupMenu.show();

                }
            });

            binding.ibSpeak.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    voiceAmount();

                }
            });

            binding.ibGuidePhoneCall.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    requestCallPhonePermission();
                }
            });

            binding.ibSOS.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    executorservice.submit(() -> {
                        handlerClickSOS();
                    });
                }
            });

            binding.actionOrderGuideButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handlerActionOrderGuideClick();
                }
            });

            binding.titleOrder.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayElementAddress(binding.blockAddressElements.getWidth());
                }
            });

            mTextToSpeech = new TextToSpeech(this, this);

            loadDataOrder(infoOrder);

            initOrderListeners();

        } else {
            finish();
        }

    }

    public Location getCurrLocation() {
        return this.currLocation;
    }


    @Override
    protected void onDestroy() {
        try {

            if (routeClass != null) {
                routeClass.recoveryResources();
            }

            if (executor != null) {
                executor.recoveryResources();
            }

            Util.removeAllValueListener(mapListeners);
            if (executorservice != null) executorservice.shutdown();
            if (service != null) service.shutdown();
            if (mp != null) mp.release();

            if (mTextToSpeech != null) {
                mTextToSpeech.stop();
                mTextToSpeech.shutdown();
                mTextToSpeech = null;
            }

        } finally {
            super.onDestroy();
        }
    }

    private void showListMsg() {

        AlertDialog.Builder builder = new AlertDialog.Builder(OrderGuideActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_list_message, null);

        builder.setIcon(R.drawable.ic_baseline_chat_24);
        builder.setTitle(R.string.t_unread_msg);
        builder.setView(dialogView);


        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvListMessage);
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);

        List<ProviderClass> filterList = GetProvidersClass.mapProviders.stream().filter(p -> !p.getMsgModel().getMsg().isEmpty()).collect(Collectors.toList());
        AdapterMsgListSrv adapter = new AdapterMsgListSrv(filterList);
        rv.setAdapter(adapter);


        adapter.setSelectListener(new AdapterMsgListSrv.OnSelectListener() {
            @Override
            public void onSelectItem(ProviderClass provider, int position) {
                if (provider != null) {
                    String title = provider.getNameSrv();

                    dlgMessage(title, provider.getMsgModel().getMsg(), "", provider.getSrvRef().child("driversList").child(currentUserUid).child("msgS"));
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

                    if (GetProvidersClass.mapProviders.stream().allMatch(p -> p.getMsgModel().getMsg().isEmpty())) {
                        binding.tvMailFromSrv.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void handlerActionOrderGuideClick() {
        switch (buttonAction) {
            case Util.START_ROUTE://-----------------------------Начать маршрут
                setViewElementsRoute();
                setStatusOrder(Util.EXECUTION_ORDER_STATUS);

                loadDataTaximeter(keyOrder, infoOrder.getRate());

                break;
            case Util.CAR_PULLED_UP://-----------------------подъехал
                checkDistance();

                break;
            case Util.TAXIMETER://-----------------------таксометр
                displayElementTaximeter(binding.blockTaximeter.getWidth());
                break;

        }
    }

    private void handlerFirstMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menuCmd1:

                setStatusOrder(Util.ROUTE_FINISHED_ORDER_STATUS);//------------закончен
                break;
            case R.id.menuCmd2:

                dlgMessage(getString(R.string.t_msg_to_client), "", getString(R.string.t_msg_from_drv_), orderRef.child("msgC"));
                break;
            case R.id.menuCmd3:

                dropOrder();//----------------сброшен
                break;
            case R.id.menuCmd4:

                showNavigator();
                break;
            case R.id.menuCmd5:

                dlgMessage(getString(R.string.t_msg_to_srv), "", "", senderSrvRef.child("driversList").child(currentUserUid).child("msgS"));
                break;
            case R.id.menuCmd6:

                prepOrderForRedirect();
                break;
            case R.id.menuCmd7:

                showRouteFromHistory();
                break;
            case R.id.menuCmd8:

                close();
                break;

        }
    }

    private void showRouteFromHistory() {
        Intent intent = new Intent(OrderGuideActivity.this, RouteFromHistoryMapActivity.class);
        intent.putExtra("user_id", currentUserUid);
        intent.putExtra("keyOrder", infoOrder.getKeyOrder());

        startActivity(intent);
    }

    private void prepOrderForRedirect() {

        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(R.drawable.ic_baseline_send_24);
        alert.setTitle(R.string.redirection_order);
        alert.setMessage(R.string.t_callsign);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setGravity(Gravity.CENTER);
        InputFilter[] filterArray = new InputFilter[1];
        filterArray[0] = new InputFilter.LengthFilter(10);
        input.setFilters(filterArray);

        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if (value.length() > 0) {

                    initRedirectOrder(Integer.parseInt(value));
                }
            }
        });
        alert.show();
    }


    private void initRedirectOrder(int callSign) {

        InfoDriverReg driver = db.getDataDriversServerDAO().getDriverInfo(callSign, currentUserUid);
        if (driver != null) {


            Map<String, Object> map = new HashMap<>();
            map.put("senderKey", currentUserUid);//сохраняем отправителя меня
            map.put("driverUid", "");
            map.put("status", Util.SEND_TO_DRV_ORDER_STATUS);

            orderRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {
                        redirectOrderForDrv(orderRef, driver);
                    } else {
                        Toast.makeText(getApplicationContext(), getString(R.string.debug_redirect_break) + error.getCode(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {

            dlgMessage(getString(R.string.redirection_order), getString(R.string.debug_break_search_driver), "", null);
        }
    }


    private void redirectOrderForDrv(DatabaseReference orderRef, InfoDriverReg driver) {
        RedirectOrderForDrvClass redirectClass = new RedirectOrderForDrvClass(hostServersRef.child(currentUserUid), orderRef, driver, infoOrder);
        redirectList.add(redirectClass);
        redirectClass.setRedirectListeners(new RedirectOrderForDrvClass.OnRedirectListener() {
            @Override
            public void onCompleted(boolean success) {

                if (success) {

                    Toast.makeText(getApplicationContext(), R.string.order_redirect_success, Toast.LENGTH_SHORT).show();

                    Intent intent = new Intent();
                    setResult(Util.REDIRECT_OK, intent);
                    finish();

                } else {

                    Toast.makeText(getApplicationContext(), R.string.break_redirect, Toast.LENGTH_LONG).show();
                }

                redirectClass.recoverResources();
                redirectList.remove(redirectClass);
            }
        });
    }

    private void showMsgMail() {
        MsgModel msg = (MsgModel) binding.ibMailFromClient.getTag();
        if (msg != null) {
            long t = (long) msg.getCreateTime();
            dlgMessage("" + Util.formatTimeDate.format(t) + getString(R.string.msg_from_client), msg.getMsg(), getString(R.string.t_msg_from_drv_), orderRef.child("msgC"));
            orderRef.child("msgD/msg").setValue("");
        }

    }

    @Override
    protected void onStart() {
        super.onStart();
        runClassSOS();
        runProvidersClassMsgListener();
    }

    private void runClassLoc() {

        locationClass = BuildLocationClass.getInstance(OrderGuideActivity.this, locationRef);
        locationClass.setOnUpdateListener(new BuildLocationClass.OnUpdateLocationListener() {
            @Override
            public void onUpdateLocation(Location loc, int satellites) {
                currLocation.set(loc);
                updateData();
            }
        });
        locationClass.getCurrentLocation();

    }

    private void runClassSOS() {
        sosClass = SOSClass.getInstance(auth.getCurrentUser(), sosHostRef);

        sosClass.setOnUpdateListener(new SOSClass.OnUpdateListener() {
            @Override
            public void onOnSOS() {
                binding.ibSOS.setColorFilter(Color.RED);
                statusSOS = Util.SOS_ON;
            }

            @Override
            public void onCount(int size) {

            }

            @Override
            public void onOffSOS() {
                binding.ibSOS.setColorFilter(Color.GRAY);
                statusSOS = Util.SOS_OFF;
            }
        });
        sosClass.initStatus();
    }

    private void runProvidersClassMsgListener() {

        providersClass = GetProvidersClass.getInstance(currentUserUid);//, TrackingHostServers.arrServerByHost
        providersClass.setMsgListener(new GetProvidersClass.OnMsgListener() {
            @Override
            public void onMsgDrv2(ProviderClass providerClass) {
                handlerMsgForDrvFromSrv();
            }
        });
        //если есть сообщения то сообщить сигналом и ib
        if (GetProvidersClass.mapProviders.stream().anyMatch(p -> !p.getMsgModel().getMsg().isEmpty())) {
            handlerMsgForDrvFromSrv();
        } else {
            binding.tvMailFromSrv.setVisibility(View.GONE);
        }
    }

    private void handlerMsgForDrvFromSrv() {
        mp = MediaPlayer.create(getApplicationContext(), R.raw.sms);
        mp.start();

        binding.tvMailFromSrv.setVisibility(View.VISIBLE);
        binding.tvMailFromSrv.setText("" + GetProvidersClass.mapProviders.stream().filter(p -> !p.getMsgModel().getMsg().isEmpty()).count());
    }

    private void updateData() {
        if (currLocation != null && currLocation.getLatitude() != 0 && currLocation.getLongitude() != 0) {

            setDriverMarker();

            if (typeDisplay == 1) {
                drawLineRoute();
            } else {
                displayMapByCenter();
            }

        }
    }


    private void handlerClickSOS() {
        if (statusSOS == Util.SOS_OFF) {
            sosHostRef.child(currentUserUid).setValue(new SOSModel());
        } else {
            boolean accessToSOS = db.getSettingAppDAO().getAccessSOS(currentUserUid);
            if (accessToSOS) {
                String password = db.getSettingAppDAO().getPassword(currentUserUid);

                if (password != null) {

                    AlertDialog.Builder alert = new AlertDialog.Builder(this);
                    alert.setMessage(R.string.input_passowrd);
                    final EditText input = new EditText(this);
                    alert.setView(input);

                    alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String value = input.getText().toString();
                            if (value.equals(password)) {
                                sosHostRef.child(currentUserUid).removeValue();

                            } else {
                                Toast.makeText(OrderGuideActivity.this, R.string.invalid_password2, Toast.LENGTH_LONG).show();

                            }
                        }
                    });
                    runOnUiThread(alert::show);
                }
            } else {

                sosHostRef.child(currentUserUid).removeValue();
            }
        }
    }

    private void dropOrder() {
        executorservice.submit(() -> {

            if (dataExTaximeter != null) {
                db.getTaximeterDAO().deleteDataTaximeter(dataExTaximeter.getOrderKey(), currentUserUid);
                dataExTaximeter = null;
            }

            if (infoOrder.getStatus() != Util.CANCEL_ORDER_STATUS) {

                Map<String, Object> map = new HashMap<>();
                map.put("distanceToClient", "");//----запрашиваем разрешение разместить этот заказ
                map.put("driverUid", "");//----запрашиваем разрешение разместить этот заказ
                map.put("msgC", new MsgModel());
                map.put("msgD", new MsgModel());
                map.put("msgS", new MsgModel());
                map.put("status", Util.FREE_ORDER_STATUS);

                orderRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                    @Override
                    public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                        if (error == null) {

                            Intent intent = new Intent();
                            intent.putExtra("keyOrder", keyOrder);
                            intent.putExtra("data_order", infoOrder);

                            setResult(Util.RESULT_DROP_ORDER, intent);
                            finish();
                        }
                    }
                });
            }
        });
    }

    private void handlerDriverUid(int resultCode) {
        executorservice.submit(() -> {

            if (dataExTaximeter != null) {
                db.getTaximeterDAO().deleteDataTaximeter(dataExTaximeter.getOrderKey(), currentUserUid);
                dataExTaximeter = null;
            }

            Map<String, Object> map = new HashMap<>();
//            map.put("dataAuto", "");
            map.put("distanceToClient", "");

            map.put("msgC", new MsgModel());
            map.put("msgD", new MsgModel());
            map.put("msgS", new MsgModel());

            orderRef.updateChildren(map);

            Intent intent = new Intent();
            intent.putExtra("keyOrder", keyOrder);
            intent.putExtra("data_order", infoOrder);

            setResult(resultCode, intent);
            finish();

        });

    }

    private void showNavigator() {
        Uri gmmIntentUri = Uri.parse("google.navigation:q=" + mLocationNavigator.getLatitude() + "," + mLocationNavigator.getLongitude());
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setPackage("com.google.android.apps.maps");
        startActivity(mapIntent);
    }

    private void close() {
        Intent intent = new Intent();
        intent.putExtra("keyOrder", keyOrder);
        intent.putExtra("data_order", infoOrder);
        setResult(Util.ACCEPT_OK, intent);
        finish();
    }

    private void voiceAmount() {
        String s2;
        String s1;

        if (intentLocale.getCurrency().equals("₽")) {
            s2 = " руб.";//------------звук-> рублей
        } else {
            s2 = " " + intentLocale.getCurrency();
        }
        s1 = String.format(menuLocale, "%.2f", mAmountMoney);
        String textToSpeech = getString(R.string.amount_of_the_trip) + s1 + s2;//+ "100,50 руб.";//
//                mTextToSpeech.setSpeechRate(0.6f);//скорость
        if (mTextToSpeech != null) {
            mTextToSpeech.speak(textToSpeech, TextToSpeech.QUEUE_FLUSH, null, "id1");
        }
    }


    @Override
    public void onInit(int status) {

        if (status == TextToSpeech.SUCCESS) {
            menuLocale = new Locale(mLanguage);//Locale.getDefault().getLocale()
            int result = mTextToSpeech.setLanguage(menuLocale);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                mIsInit = false;
            } else {
                mIsInit = true;
            }
        } else {
            mIsInit = false;
        }
    }


    private void checkDistance() {
        float distance = Util.calculationDistance(mFrom.getLatitude(), mFrom.getLongitude(), currLocation.getLatitude(), currLocation.getLongitude());
        if (distance > 100) {
            AlertDialog.Builder builder = new AlertDialog.Builder(OrderGuideActivity.this);
            builder.setMessage(getString(R.string.t_where_to_client) + (int) distance + getString(R.string.meters));
            builder.setCancelable(true);
            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                public void onClick(DialogInterface dialog, int which) {
                    setStatusOrder(Util.ARRIVE_ORDER_STATUS);
                }
            });

            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            builder.create().show();
        } else {

            setStatusOrder(Util.ARRIVE_ORDER_STATUS);
        }
    }


    private void initOrderListeners() {
        ValueEventListener driverUidListener = new ValueEventListener() {// слушатель смена назначенного водителя
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String drvUid = snapshot.getValue(String.class);
                    if (drvUid != null) {
                        if (!drvUid.equals("")) {
                            if (!drvUid.equals(currentUserUid)) {

                                handlerDriverUid(Util.RESULT_CHANGE_DRIVER);
                            }
                        } else {

                            handlerDriverUid(Util.RESULT_CANCELED_DRIVER);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        orderRef.child("driverUid").addValueEventListener(driverUidListener);
        mapListeners.put(orderRef.child("driverUid"), driverUidListener);

        ValueEventListener msgDrvListener = new ValueEventListener() {// слушатель сообщения от пассажира
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    handlerMsgForDrv(orderRef.child("msgD"));
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        orderRef.child("msgD/msg").addValueEventListener(msgDrvListener);
        mapListeners.put(orderRef.child("msgD/msg"), msgDrvListener);

        ValueEventListener statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer status = snapshot.getValue(Integer.class);
                    if (status != null) {

                        infoOrder.setStatus(status);
                        if (status == Util.ROUTE_FINISHED_ORDER_STATUS) {//--------------------маршрут закончен

                            deleteOrderRef(infoOrder, status);
                        } else {

                            setViewByStatus(status);
                        }
                    }
                } else if (infoOrder.getStatus() != Util.ROUTE_FINISHED_ORDER_STATUS) {

                    notifyEventKillOrder();//информируем если ордера нет и статус не закончен
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        orderRef.child("status").addValueEventListener(statusListener);
        mapListeners.put(orderRef.child("status"), statusListener);
    }

    private void notifyEventKillOrder() {
        Intent intent = new Intent();
        intent.putExtra("keyOrder", keyOrder);
        intent.putExtra("data_order", infoOrder);
        setResult(Util.RESULT_KILL_ORDER, intent);
        finish();
    }

    private void deleteOrderRef(InfoOrder order, int status) {

        DatabaseReference refSrv;

        Map<String, Object> map = new HashMap<>();
        map.put("/freeOrders/" + order.getKeyOrder(), null);
        map.put("/keysO/" + order.getKeyOrder() + "/dial", null);

        if (order.getProviderKey().equals("SHAREDSERVER")) {
            refSrv = hostSharedSrvRef;
        } else {
            refSrv = hostServersRef.child(order.getProviderKey());
        }

        refSrv.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    Util.removeByHostListener(orderRef, mapListeners);
                    if (db.getSettingAppDAO().getVoicingAmount(currentUserUid)) {
                        voiceAmount();
                    }

                    setViewByStatus(status);
                }
            }
        });
    }


    private void handlerMsgForDrv(DatabaseReference msgForDrv) {
        msgForDrv.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    MsgModel msg = snapshot.getValue(MsgModel.class);
                    if (msg != null && msg.getMsg().length() > 0) {

                        binding.ibMailFromClient.setVisibility(View.VISIBLE);
                        binding.ibMailFromClient.setTag(msg);

                    } else {
                        binding.ibMailFromClient.setTag(null);
                        binding.ibMailFromClient.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void selectedRate() {
//        при выборе тарифа время allAmoutSecond должно совпадать с таймером счета прошедшего времени
        List<RatesSrv> rates = db.getRatesDAO().getRates(currentUserUid);

        AlertDialog.Builder builder = new AlertDialog.Builder(OrderGuideActivity.this);
        builder.setTitle(R.string.t_select_rate);
        final ArrayAdapter<RatesSrv> adapter = new ArrayAdapter<RatesSrv>(getApplicationContext(), android.R.layout.simple_selectable_list_item);
        adapter.addAll(rates);

        builder.setAdapter(adapter, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                RatesSrv rate = adapter.getItem(which);
                loadDataTaximeter(keyOrder, new RateModel(rate.getKm(), rate.getMin(), rate.getDefWait(), rate.getFixedAmount(), rate.getHourlyRate()));
            }
        });

        builder.create().show();

    }

    //--------------------------проверка есть ли разрешения
    private void requestCallPhonePermission() {
        // ----ActivityCompat   Помощник для доступа к функциям в Activity.
        //--ActivityCompat.shouldShowRequestPermissionRationale - Передается название разрешения, а он вам в виде boolean ответит, надо ли показывать объяснение для пользователя.
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE);

        if (shouldProvideRationale) {//------------запрос на разрешение с объяснением

            showSnackBar(getString(R.string.debug_permission_phone), getString(R.string.ok), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //запрос на разрешение  (обработчик-onRequestPermissionsResult по ключу REQUEST_LOCATION_PERMISSION)
                    //*Этот интерфейс на получение результатов для запросов разрешений. ActivityCompat.requestPermissions

                    ActivityCompat.requestPermissions(OrderGuideActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
                }
            });
        } else {

            //запрос на разрешение без объяснения  -интерфейс на получение разрешения
            ActivityCompat.requestPermissions(OrderGuideActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE);

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Util.REQUEST_CALL_PHONE_PERMISSION) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {   //-------------если разрешил

                callPhone();

            }

        }
    }

    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {
        //Snackbar содержит действие, которое устанавливается через- setAction(action,listener)
        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE).setAction(action, listener).show();
    }

    private void callPhone() {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + infoOrder.getPhone()));
        startActivity(intent);
    }


    private void setViewElementsRoute() {
        int i = binding.blockAddressElements.getWidth();
        binding.tvCurrentRate.setTranslationX(i);
        binding.ibSpeak.setTranslationX(-i);
        binding.coveredDistanceTextView.setTranslationX(-i);
//        ibLanguage.setTranslationX(i);
        binding.timeSpentTextView.setTranslationX(i);
        binding.tripAmountTextView.setTranslationX(-i);

        displayElementTaximeter(i);

        LatLng latLng = new LatLng(mTo.getLatitude(), mTo.getLongitude());//----------получение координат
        passengerMarker.setPosition(latLng);
        passengerMarker.setTitle(getString(R.string.addressee));

        typeDisplay = 1;//---------------------------------прокладывать маршрут
        drawLineRoute();

    }


    private void loadDataTaximeter(String key, RateModel rate) {

        //----------------запуск сохрание маршрута
        if (routeClass == null) {
            routeClass = new DataRouteClass(currentUserUid, infoOrder, currLocation);
        }


        if (dataExTaximeter == null) {//--------------не создавался

            try {
                dataExTaximeter = getDataTaximeter(key, rate);//NEW

                executor = new TaximeterExecutorClass(dataExTaximeter, database.getReference().child("timer"), locationRef, currLocation);

                executor.setOnListener(new TaximeterExecutorClass.OnTaximeterListener() {
                    @Override
                    public void onUpdateTaximeter(long secondExecute, long secondWait, float allMeter, float amountMoney) {
                        mAmountMoney = amountMoney;
                        runOnUiThread(() -> {

                            displayExecuteTimeTaximeter(secondExecute, secondWait);
                            displayDistanceTaximeter(allMeter);
                        });
                    }

                    @Override
                    public void onSetSaveTaximeter(DataTaximeter taximeter) {
                        saveTaximeter(taximeter);
                    }

                });

            } catch (ExecutionException | InterruptedException e) {

                e.printStackTrace();
            }
        } else {

            executor.setDataRate(rate);//смена тарифа
        }

        //display
        binding.tvCurrentRate.setText(dataExTaximeter.getRate().toString());
    }

    private void saveTaximeter(DataTaximeter taximeter) {
        if (taximeter != null) {
            db.getTaximeterDAO().updateDataTaximeter(taximeter);
        }
    }

    private DataTaximeter getDataTaximeter(String key, RateModel rate) throws ExecutionException, InterruptedException {
        Callable task = () -> {
            DataTaximeter dataTaximeter = db.getTaximeterDAO().getTaximeter(key, currentUserUid);
            if (dataTaximeter == null) {
                dataTaximeter = new DataTaximeter(currentUserUid,
                        key,
                        0,
                        0,
                        rate,
                        0,
                        0,
                        0,
                        0,
                        0,
                        0,
                        intentLocale.getCurrency(),
                        new RouteInfoModel(infoOrder.getFrom().getLatitude(), infoOrder.getFrom().getLongitude(), infoOrder.getTo().getLatitude(), infoOrder.getTo().getLongitude(), getString(R.string.t_from), getString(R.string.t_to)));

                long id = db.getTaximeterDAO().addDataTaximeter(dataTaximeter);
                if (id > 0) {

                    dataTaximeter.setId(id);
                }
            }

            return dataTaximeter;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (DataTaximeter) future.get();
    }

    private void displayDistanceTaximeter(float distanceMeter) {

        binding.coveredDistanceTextView.setText(String.format(getString(R.string.format_distance), distanceMeter / 1000));//+ " км."
        if (intentLocale.getLanguage().equals("ru")) {//locale.getCurrency()
            binding.tripAmountTextView.setText(String.format("%d " + intentLocale.getCurrency(), (long) mAmountMoney));//удаление копеек

        } else {

            binding.tripAmountTextView.setText(String.format("%.2f " + intentLocale.getCurrency(), mAmountMoney));
        }

    }


    private void displayExecuteTimeTaximeter(long executeSec, long secondWait) {
        long executeMin = executeSec / 60;//перевод в minute
        long lHour = executeMin / 60;
        long lMinute = executeMin % 60;

        String strTimeExecute = String.format(getString(R.string.format_h), lHour) + String.format(getString(R.string.format_min), lMinute);//  (minuteExecuteTaximeter/60) ? ("" + (int) distance + " м.") : (String.format("%.3f", distance / 1000) + " км.");

        binding.timeSpentTextView.setText(strTimeExecute + " (" + (secondWait / 60) + ")");

    }


    private void displayElementTaximeter(int distance) {
        if (binding.coveredDistanceTextView.getTranslationX() < 0) {
            binding.tvCurrentRate.animate().translationXBy(-distance).setDuration(500);
            binding.ibSpeak.animate().translationXBy(distance).setDuration(500);
            binding.coveredDistanceTextView.animate().translationXBy(distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)
//            ibLanguage.animate().translationXBy(-distance).setDuration(500);
            binding.timeSpentTextView.animate().translationXBy(-distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)
            binding.tripAmountTextView.animate().translationXBy(distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)

        } else {

            binding.tvCurrentRate.animate().translationXBy(distance).setDuration(500);
            binding.ibSpeak.animate().translationXBy(-distance).setDuration(500);
            binding.coveredDistanceTextView.animate().translationXBy(-distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)
//            ibLanguage.animate().translationXBy(distance).setDuration(500);
            binding.timeSpentTextView.animate().translationXBy(distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)
            binding.tripAmountTextView.animate().translationXBy(-distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)

        }

    }

    private void setStatusOrder(int status) {

        if (status == Util.ROUTE_FINISHED_ORDER_STATUS) {
            Map<String, Object> map = new HashMap<>();
            map.put("timeF", ServerValue.TIMESTAMP);
            map.put("status", status);

            orderRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {
//                        infoOrder.setStatus(status);
                    }
                }
            });

        } else {

            orderRef.child("status").setValue(status, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {
//                        infoOrder.setStatus(status);
                    }
                }
            });
        }

    }

    private void displayElementAddress(int distance) {
        if (binding.tvAddressGuideOrderFrom.getTranslationX() < 0) {

            binding.tvAddressGuideOrderFrom.animate().translationXBy(distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)
            binding.tvNoteGuideOrder.animate().translationXBy(distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)
            binding.tvAddressGuideOrderTo.animate().translationXBy(distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)

        } else {
            binding.tvAddressGuideOrderFrom.animate().translationXBy(-distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)
            binding.tvNoteGuideOrder.animate().translationXBy(-distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)
            binding.tvAddressGuideOrderTo.animate().translationXBy(-distance).setDuration(500);//----------------- перемещение на 100 пикселей вправо(-100 влево)

        }
    }

    private void loadDataOrder(InfoOrder order) {

        binding.titleOrder.setText(order.getProviderName());
        binding.titleOrder.append(" - " + Util.formatTimeDate.format(order.getTimestamp()));

        binding.tvAddressGuideOrderFrom.setText(R.string.t_from);
        binding.tvAddressGuideOrderFrom.append("\n" + Util.getAddress(order.getFrom().getLatitude(), order.getFrom().getLongitude(), Util.TYPE_ADDRESS_LONG));

        binding.tvAddressGuideOrderTo.setText(R.string.t_to);
        binding.tvAddressGuideOrderTo.append("\n" + Util.getAddress(order.getTo().getLatitude(), order.getTo().getLongitude(), Util.TYPE_ADDRESS_LONG));

        if (order.getNote().length() > 1) {
            binding.tvNoteGuideOrder.setText(R.string.t_note);
            binding.tvNoteGuideOrder.append("\n" + order.getNote());
            binding.tvNoteGuideOrder.setVisibility(View.VISIBLE);
        }

        if (order.getPhone().length() > 1) {
            binding.ibGuidePhoneCall.setVisibility(View.VISIBLE);
        }

        mTo = order.getTo();
        mFrom = order.getFrom();
        mLocationNavigator = mFrom;
        keyOrder = order.getKeyOrder();

        if (order.getStatus() == Util.EXECUTION_ORDER_STATUS) {//---------заказ выполняется
            mLocationNavigator = mTo;

            loadDataTaximeter(keyOrder, order.getRate());
        }

    }

    private void setViewByStatus(int status) {
        switch (status) {
            case Util.EXECUTION_ORDER_STATUS:

                buttonAction = Util.TAXIMETER;
                binding.actionOrderGuideButton.setText(R.string.taximeter);
                typeDisplay = 1;//прокладывать маршрут
                binding.blockTaximeter.setVisibility(View.VISIBLE);

                break;
            case Util.ARRIVE_ORDER_STATUS:
                buttonAction = Util.START_ROUTE;
                mLocationNavigator = mTo;

                binding.actionOrderGuideButton.setText(R.string.to_begin);

                break;
            case Util.ASSIGN_ORDER_STATUS:
                buttonAction = Util.CAR_PULLED_UP;
                binding.actionOrderGuideButton.setText(R.string.arrive);

                break;
            case Util.ROUTE_FINISHED_ORDER_STATUS:

                binding.titleOrder.append(getString(R.string._finished_h));
                buttonAction = Util.TAXIMETER;
                binding.actionOrderGuideButton.setText(R.string.taximeter);

                binding.tvCurrentRate.setEnabled(false);
                binding.ibGuidePhoneCall.setVisibility(View.GONE);
//                buttonClose.setVisibility(View.VISIBLE);


                if (executor != null) {
                    executor.recoveryResources();
                    executor = null;
                }

                if (executorservice != null) executorservice.shutdown();
                if (service != null) service.shutdown();
                binding.blockTaximeter.setVisibility(View.VISIBLE);

//                buttonMenu.setVisibility(View.GONE);
                popupMenu.getMenu().findItem(R.id.menuCmd8).setVisible(true);
                popupMenu.getMenu().findItem(R.id.menuCmd6).setVisible(false);
                popupMenu.getMenu().findItem(R.id.menuCmd5).setVisible(false);
                popupMenu.getMenu().findItem(R.id.menuCmd4).setVisible(false);
                popupMenu.getMenu().findItem(R.id.menuCmd3).setVisible(false);
                popupMenu.getMenu().findItem(R.id.menuCmd2).setVisible(false);
                popupMenu.getMenu().findItem(R.id.menuCmd1).setVisible(false);

                break;
        }

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
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mZoom = mMap.getCameraPosition().zoom;
            }
        });

        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                hideRouteData();
            }
        });
        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                hideRouteData();
            }
        });


        passengerMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(mFrom.getLatitude(), mFrom.getLongitude())).title(getString(R.string.client)));
        setDriverMarker();

        displayMapByCenter();
        mMap.setMaxZoomPreference(19);


    }

    private void setDriverMarker() {
        if (mMap != null) {
            if (currLocation != null) {
                if (driverMarker != null) {
                    driverMarker.setPosition(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()));
                } else {
                    driverMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(currLocation.getLatitude(), currLocation.getLongitude())).title(getString(R.string.I)).icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_logos)));//----------создание маркера
                }
            }
        }
    }

    private void hideRouteData() {

        if (binding.tvAddressGuideOrderFrom.getTranslationX() == 0) {
            displayElementAddress(binding.blockAddressElements.getWidth());
        }

        if (binding.coveredDistanceTextView.getTranslationX() == 0) {
            displayElementTaximeter(binding.blockAddressElements.getWidth());
        }

    }

    //***************************************************************************************************по центру
    private void displayMapByCenter() {

        if (currLocation != null && driverMarker != null) {
            setDriverMarker();

            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(driverMarker.getPosition());
            builder.include(passengerMarker.getPosition());
            LatLngBounds bounds = builder.build();
            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.20); // смещение от краев карты 20% экрана

            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mMap.moveCamera(cu);
        }
    }


    private void drawLineRoute() {

        if (currLocation != null) {

            if (line != null) line.remove();
            if (mTo.getLatitude() != 0 && mTo.getLongitude() != 0) {
                line = mMap.addPolyline(new PolylineOptions()
                        .add(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()), new LatLng(mTo.getLatitude(), mTo.getLongitude()))
                        .color(Color.RED)
                        .width(2));
            }

            setPosCamera(currLocation);
        }
    }


    private float bearingBetweenLocations(LatLng latLng1, LatLng latLng2) {

        double pi = Math.PI; //3.141592653;
        double lat1 = latLng1.latitude * pi / 180;
        double long1 = latLng1.longitude * pi / 180;
        double lat2 = latLng2.latitude * pi / 180;
        double long2 = latLng2.longitude * pi / 180;
        double dLon = (long2 - long1);
        double y = Math.sin(dLon) * Math.cos(lat2);
        double x = Math.cos(lat1) * Math.sin(lat2) - Math.sin(lat1) * Math.cos(lat2) * Math.cos(dLon);
        double brng = Math.atan2(y, x);
        brng = Math.toDegrees(brng);
        brng = (brng + 360) % 360;

        return (float) brng;
    }


    private void setPosCamera(Location currLocation) {
        float bearing = bearingBetweenLocations(new LatLng(oldLoc.getLatitude(), oldLoc.getLongitude()), new LatLng(currLocation.getLatitude(), currLocation.getLongitude()));
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()))
                .zoom(mZoom)
                .bearing(bearing)
                .build();

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        oldLoc = currLocation;

    }

    private void dlgMessage(String title, String msg, String from, DatabaseReference answerRef) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_chat, null);
        dialogBuilder.setView(dialogView);

        final TextView tvTitle = dialogView.findViewById(R.id.tvTimeMsgTitle);
        final TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        final EditText etAnswerMessage = dialogView.findViewById(R.id.etAnswerMessage);
        final Button buttonAnswer = dialogView.findViewById(R.id.buttonAnswer);

        if (!title.isEmpty()) {
            tvTitle.setText(title);
        } else {
            tvTitle.setVisibility(View.GONE);
        }

        if (msg.isEmpty()) {
            tvMessage.setVisibility(View.GONE);
        } else {
            tvMessage.setText(msg);
        }

        if (answerRef == null) {
            etAnswerMessage.setVisibility(View.GONE);
        }

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        etAnswerMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                buttonAnswer.setText(editable.length() > 0 ? R.string.send : R.string.close_m);
            }
        });

        buttonAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etAnswerMessage.length() > 0) {
                    sendMsgByRef(from + etAnswerMessage.getText().toString(), answerRef);
                }
                alertDialog.dismiss();
            }
        });
    }

    private void sendMsgByRef(String msg, DatabaseReference ref) {

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {//msgFor... есть

                    if (snapshot.child("msg").exists()) {

                        if (Objects.equals(snapshot.child("msg").getValue(), "")) {

                            snapshot.getRef().setValue(new MsgModel(msg));
                            Toast.makeText(getApplicationContext(), R.string.msg_send, Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.break_send_msg, Toast.LENGTH_LONG).show();
                        }

                    } else {//ссылка поврежденная

                        snapshot.getRef().setValue(new MsgModel(msg));//- исправляем
                        Toast.makeText(getApplicationContext(), R.string.msg_send, Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getApplicationContext(), R.string.addressee_not_exsists, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }
}