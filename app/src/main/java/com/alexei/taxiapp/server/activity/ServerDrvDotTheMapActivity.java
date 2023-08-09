package com.alexei.taxiapp.server.activity;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityServerDrvDotTheMapBinding;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.server.adapter.AdapterMsgListDrv;
import com.alexei.taxiapp.server.exClass.SrvDriversObservationClass;
import com.alexei.taxiapp.server.exClass.SrvOrdersObservationClass;
import com.alexei.taxiapp.util.Util;
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
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class ServerDrvDotTheMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityServerDrvDotTheMapBinding binding;

    private ScheduledFuture<?> scheduledFutureTimer2;
    private ExecutorService executorservice;
    private ScheduledExecutorService scheduledExecutorService;
    private FirebaseAuth auth;
    private FirebaseDatabase database;

    private String currUserUid = "";
    private boolean bBounds = false;
    private String idDrv = "";

    private float mZoom = 16;

    private final Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private final Map<DatabaseReference, ChildEventListener> mapChildListeners = new HashMap<>();
    private final Map<String, Marker> mapMarkers = new HashMap<>();

    private Marker selMarker;
    private boolean isDisplayByCenter = true;

    private SrvDriversObservationClass driversObservation;

    private List<InfoDriverReg> driversList;
    private List<InfoOrder> orderList;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityServerDrvDotTheMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setRequestedOrientation(getResources().getConfiguration().orientation);

        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        executorservice = Executors.newFixedThreadPool(2);

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currUserUid = auth.getCurrentUser().getUid();
        } else {
            finish();
        }


        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базы данных


        PopupMenu popupMenu = new PopupMenu(ServerDrvDotTheMapActivity.this, binding.ibMenuDrvDot);
        popupMenu.inflate(R.menu.server_driver_point_map_menu);


        Intent intent = getIntent();
        if (intent != null) {
            idDrv = intent.getStringExtra("keyDrv");

            loadData();

        }

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDotDrivers);
        if (mapFragment != null) {

            mapFragment.getMapAsync(this);
        }

        binding.ibNewMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                showListMsg();

            }
        });

        binding.ibSearchDrv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                searchDriver();
            }
        });

        binding.ibByCenter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                isDisplayByCenter = !isDisplayByCenter;
                if (isDisplayByCenter) {

                    binding.ibByCenter.setColorFilter(Color.BLUE);
                    if (mMap != null) {

                        displayAllMarkersByCenter();

                    }
                } else {
                    binding.ibByCenter.setColorFilter(Color.GRAY);
                }

            }
        });

        binding.ibMenuDrvDot.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if (selMarker != null) {
                    popupMenu.show();
                    mMap.moveCamera(CameraUpdateFactory.newLatLng(selMarker.getPosition()));
                } else {
                    binding.ibMenuDrvDot.setVisibility(View.GONE);
                }
            }
        });

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                handlerMenuItemClick(item);
                return true;
            }
        });


        orderList = SrvOrdersObservationClass.ordersList;//ссылка
        driversList = SrvDriversObservationClass.allDrivers;//ссылка на массив водителей
    }

    private void handlerMenuItemClick(MenuItem item) {
        InfoDriverReg driver = (InfoDriverReg) selMarker.getTag();
        if (driver != null) {
            switch (item.getItemId()) {
                case R.id.actionDrvDotInfo:

                    getDrvInfo(driver);
                    break;
                case R.id.actionDrvDotPhone:

                    drvPhone(driver);
                    break;
                case R.id.actionDrvDotSendMsg:

                    DatabaseReference ref = database.getReference().child("serverList").child(driver.getServerUid()).child("driversList").child(driver.getDriverUid()).child("msgD");
                    String title = getString(R.string.t_whom) + driver.getName() + " (" + driver.getCallSign() + ")";
                    Util.dlgMessage(ServerDrvDotTheMapActivity.this, title, "", "", ref);
                    break;
            }
        }
    }

    private void runDriversObservationClass() {
        driversObservation = SrvDriversObservationClass.getInstance(database.getReference().child("serverList").child(currUserUid), auth.getCurrentUser());


        driversObservation.setOnListener(new SrvDriversObservationClass.OnUpdateListener() {

            @Override
            public void onChangeStatusShift(InfoDriverReg driver) {
                handlerStatus(driver);
            }

            @Override
            public void onSOS(InfoDriverReg driver) {

                handlerStatus(driver);
            }

            @Override
            public void onChangeDrvStatusToSrv(InfoDriverReg driver) {

//                handlerStatusToSrv(driver);
            }

            @Override
            public void onUpdateSharedStatus(InfoDriverReg driver) {
                handlerStatus(driver);
            }

            @Override
            public void onUpdateLocation(InfoDriverReg driver) {
                handlerLocation(driver);
            }

            @Override
            public void onMsgForSrv(InfoDriverReg driver) {
                handlerPostMsg(driver);
            }

            @Override
            public void onAssignedOrder(InfoDriverReg driver) {
                handlerAssignOrder(driver);
            }

            @Override
            public void onExOrder(InfoDriverReg driver) {
                handlerExOrder(driver);
            }


            @Override
            public void onRemoveHost(String key) {
                Marker marker = mapMarkers.get(key);
                if (marker != null) {
                    marker.remove();
                    mapMarkers.remove(key);
                }
            }
        });

        if (driversObservation.isExistsDataClass()) {//для - при возврате из другой активити обновить вид согласно статусу включая SOS
            redrawDotMap();
        }

        if (idDrv.length() > 0) {

            drawDotToCenter();
        }
    }


    private void drawDotToCenter() {
        Marker marker = mapMarkers.get(idDrv);
        if (marker != null) {
            enabledDisplayByCenter(false);
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(marker.getPosition())
                    .zoom(18)
                    .build();

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));

            marker.showInfoWindow();
        }
    }

    private void redrawDotMap() {
        driversList.stream().filter(d -> d.getStatusToHostSrv() != Util.UNKNOWN_DRIVER_STATUS).forEach(d -> {
            handlerLocation(d);
            handlerStatus(d);
            handlerPostMsg(d);
        });
    }

    private void handlerStatus(InfoDriverReg driver) {
        Marker marker = setMarker(driver);
        if (driver.getShiftStatus() == Util.SHIFT_OPEN_DRV_STATUS) {

            switch (driver.getStatusShared()) {
                case Util.FREE_DRIVER_STATUS:

                    if (driver.getSosModel() != null) {
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.y_sos));
                    } else {
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot));
                    }

                    break;
                case Util.BUSY_DRIVER_STATUS:

                    if (driver.getSosModel() != null) {
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.g_sos));
                    } else {
                        marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.green_dot));
                    }

                    break;
                default:
                    marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.dot_silver));
                    marker.setTitle(driver.getName() + R.string.unknown);
                    break;
            }
        } else {
            marker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.blue_dot));
        }
    }

    private void handlerAssignOrder(InfoDriverReg driver) {

    }

    private void handlerEditDrv(InfoDriverReg driver) {

    }

    private void handlerExOrder(InfoDriverReg driver) {

    }

    private void handlerLocation(InfoDriverReg driver) {

        if (driver != null) {
            Marker marker = setMarker(driver);
//            displayAllMarkersByCenter();
        }

    }


    private Marker setMarker(InfoDriverReg driver) {
        Marker marker = mapMarkers.get(driver.getDriverUid());
        if (marker == null) {
            marker = mMap.addMarker(new MarkerOptions().position(new LatLng(driver.getLocation().getLatitude(), driver.getLocation().getLongitude()))
                    .title(driver.getName() + "(" + driver.getCallSign() + ")"));

            if (marker != null) {
                marker.setTag(driver);
                mapMarkers.put(driver.getDriverUid(), marker);
            }
        }
        return marker;
    }


    private void findDrv(String search) {
        boolean isDigitsOnly = TextUtils.isDigitsOnly(search);//Возвращает, содержит ли данная CharSequence только цифры.
        final boolean[] bFind = {false};
        driversList.forEach(d -> {
            if (isDigitsOnly) {
                if (d.getCallSign() == Long.parseLong(search) || d.getName().equalsIgnoreCase(search)) {
                    bFind[0] = true;
                    enabledDisplayByCenter(false);
                    drawMarker(mapMarkers.get(d.getDriverUid()));
                }
            } else {
                if (d.getName().equalsIgnoreCase(search)) {
                    bFind[0] = true;
                    enabledDisplayByCenter(false);
                    drawMarker(mapMarkers.get(d.getDriverUid()));
                }
            }
        });

        if (!bFind[0]) {
            Toast.makeText(this, R.string.drv_not_found, Toast.LENGTH_SHORT).show();
        }

    }

    private void enabledDisplayByCenter(boolean b) {
        isDisplayByCenter = b;
        if (!b) {

            binding.ibByCenter.setColorFilter(Color.GRAY);
            binding.ibMenuDrvDot.setVisibility(View.GONE);
        } else {
            binding.ibByCenter.setColorFilter(Color.BLUE);

        }
    }

    private void drawMarker(Marker marker) {

        if (marker != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(marker.getPosition())
                    .zoom(18)
                    .build();

            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
            marker.showInfoWindow();
        }
    }

    private void searchDriver() {

        String value = binding.etSearchDrv.getText().toString();

        if (value.length() > 0) {
            findDrv(value);
        }

    }

    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        setResult(Activity.RESULT_CANCELED, intent);
        finish();
//        super.onBackPressed();
    }

    private void showListMsg() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_list_message, null);
        builder.setIcon(R.drawable.ic_baseline_chat_24);
        builder.setTitle(R.string.t_message);
        builder.setView(dialogView);

        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvListMessage);
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);//connectedDrvList.stream().filter(d -> !d.getMessage().isEmpty()).collect(Collectors.toList())

        List<InfoDriverReg> drvWithLetterList = driversList.stream().filter(d -> !d.getMessage().getMsg().isEmpty()).collect(Collectors.toList());
        AdapterMsgListDrv adapter = new AdapterMsgListDrv(drvWithLetterList);
        rv.setAdapter(adapter);

        adapter.setSelectListener(new AdapterMsgListDrv.OnSelectListener() {
            @Override
            public void onSelectItem(InfoDriverReg driver, int position) {
                if (driver != null) {

                    String title = R.string.t_msg_from_drv_ + driver.getName() + " (" + driver.getCallSign() + ")";
                    DatabaseReference ref = database.getReference().child("serverList").child(driver.getServerUid()).child("driversList").child(driver.getDriverUid()).child("msgD");
                    Util.dlgMessage(ServerDrvDotTheMapActivity.this, title, driver.getMessage().getMsg(), "", ref);

                }
            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                removeReadMsg(drvWithLetterList);
            }
        });

        builder.create().show();
    }

    private void removeReadMsg(List<InfoDriverReg> drvWithLetterList) {
        drvWithLetterList.forEach(this::removeMsgByRef);

    }

    private void removeMsgByRef(InfoDriverReg driver) {
        DatabaseReference reference = database.getReference().child("serverList").child(driver.getServerUid()).child("driversList").child(driver.getDriverUid()).child("msgS/msg");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    reference.setValue("");
                    driver.getMessage().setMsg("");

                    if (driversList.stream().allMatch(d -> d.getMessage().getMsg().isEmpty())) {
                        binding.ibNewMessage.setVisibility(View.GONE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void drvPhone(InfoDriverReg model) {
        requestCallPhonePermission();
    }

    private void getDrvInfo(InfoDriverReg drv) {

        StringBuilder str = new StringBuilder();

        str.append(getString(R.string.t_name))
                .append(drv.getName()).append("\n\n")
                .append(getString(R.string.t_callsign))
                .append(drv.getCallSign()).append("\n\n")
                .append(getString(R.string.t_priority))
                .append(drv.getPriority()).append("\n\n")
                .append(getString(R.string.t_phone))
                .append(drv.getPhone()).append("\n\n")
                .append(getString(R.string.t_transport)).append("\n")
                .append(drv.getAuto().toString()).append("\n\n")
                .append(getString(R.string.t_type)).append(drv.getAutoType()).append("\n\n")
                .append(getStatusDrv(drv)).append("\n")
                .append(getStatusShiftDrv(drv));

        if (drv.getStatusShared() == Util.BUSY_DRIVER_STATUS) {
            getRoute(drv, str);
        } else {
            showInfo(getString(R.string.t_debug_drv), str);
        }
    }

    private String getStatusShiftDrv(InfoDriverReg drv) {
        String status = getString(R.string.t_status);

        switch (drv.getShiftStatus()) {
            case Util.SHIFT_OPEN_DRV_STATUS:

                status = "\n" + getString(R.string.shift_open) + Util.formatTimeDate.format(drv.getOpenShiftTime()) +
                        getString(R.string.end_shift) + " - " + Util.formatTimeDate.format(drv.getFinishTimeShift());
                break;
            case Util.SHIFT_CLOSE_DRV_STATUS:

                status = "\n" + getString(R.string.shift_close_) + Util.formatTimeDate.format(drv.getCloseShiftTime()) +
                        getString(R.string.end_shift) + " - " + Util.formatTimeDate.format(drv.getFinishTimeShift());
                break;
        }

        return status;
    }

    private void getRoute(InfoDriverReg driver, StringBuilder str) {

        if (orderList != null) {
            InfoOrder order = orderList.stream().filter(o -> o.getKeyOrder().equals(driver.getExOrder().getKeyOrder())).findAny().orElse(null);
            if (order != null) {

                String strFrom = Util.getAddress(order.getFrom().getLatitude(), order.getFrom().getLongitude(), Util.TYPE_ADDRESS_LONG);
                String strTo = Util.getAddress(order.getTo().getLatitude(), order.getTo().getLongitude(), Util.TYPE_ADDRESS_LONG);

                str.append(getString(R.string.info_route)).append(strFrom).append(getString(R.string.t_there_to)).append(strTo);
                str.append(getString(R.string._t_rate_)).append(order.getRate().toString());
            } else {
                str.append(getString(R.string.info_route_disabled));
            }
        } else {
            str.append(getString(R.string.info_route_disabled));
        }

        showInfo(getString(R.string.t_debug_drv), str);
    }

    private String getStatusDrv(InfoDriverReg drv) {
        String status = getString(R.string.t_status);

        switch (drv.getStatusShared()) {
            case Util.BUSY_DRIVER_STATUS:
                status += getString(R.string.drv_status_busy);
                break;
            case Util.FREE_DRIVER_STATUS:
                status += getString(R.string.drv_status_free);
                break;

            default:
                status += getString(R.string.drv_status_unknown);
                break;
        }

        return status;
    }

    private void showInfo(String title, StringBuilder str) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(R.drawable.ic_baseline_chat_24);
        alert.setTitle(title);
        alert.setMessage(str);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        alert.show();
    }


    //--------------------------проверка есть ли разрешения/**************************************************
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

                    ActivityCompat.requestPermissions(ServerDrvDotTheMapActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
                }
            });
        } else {
//            callPhone();
            //запрос на разрешение без объяснения  -интерфейс на получение разрешения
            ActivityCompat.requestPermissions(ServerDrvDotTheMapActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE);

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Util.REQUEST_CALL_PHONE_PERMISSION) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {   //-------------если разрешил
                InfoDriverReg model = (InfoDriverReg) selMarker.getTag();
                if (model != null) {
                    callPhone(model.getPhone());
                }
            }
        }
    }

    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {
        //Snackbar содержит действие, которое устанавливается через- setAction(action,listener)

        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE).setAction(action, listener).show();
    }

    private void callPhone(String phone) {
        Intent intent = new Intent(Intent.ACTION_CALL);
        intent.setData(Uri.parse("tel:" + phone));
        startActivity(intent);
    }

    @Override
    protected void onDestroy() {
        try {

            Util.removeAllValueListener(mapListeners);
            Util.removeAllChildListener(mapChildListeners);

            if (scheduledFutureTimer2 != null) {
                scheduledFutureTimer2.cancel(true);
            }

            executorservice.shutdownNow();
            scheduledExecutorService.shutdownNow();
        } finally {
            super.onDestroy();
        }

    }

    //СТАРТ****СТАРТ****СТАРТ****СТАРТ****СТАРТ****СТАРТ****СТАРТ****СТАРТ****СТАРТ****СТАРТ****СТАРТ****

    private void loadData() {

        executorservice.submit(() -> {
            //по центру
            Runnable task2 = () -> {
                if (mMap != null) {
                    displayAllMarkersByCenter();
                }
            };
            scheduledFutureTimer2 = scheduledExecutorService.scheduleAtFixedRate(task2, 0, 5, TimeUnit.SECONDS);
        });
    }

    private void handlerPostMsg(InfoDriverReg driver) {

        if (driver.getMessage().getMsg().length() > 0) {
            binding.ibNewMessage.setVisibility(View.VISIBLE);
        }
    }


    private void displayAllMarkersByCenter() {
        runOnUiThread(() -> {
            bBounds = false;
            if (isDisplayByCenter) {

                LatLngBounds.Builder builder = new LatLngBounds.Builder();

                mapMarkers.forEach((k, v) -> {
                    if (v != null) {
                        if (v.getPosition().latitude != 0 && v.getPosition().longitude != 0) {
                            builder.include(v.getPosition());
                            bBounds = true;
                        }
                    }
                });

                if (bBounds) {
                    int width = getResources().getDisplayMetrics().widthPixels;
                    int height = getResources().getDisplayMetrics().heightPixels;
                    mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 100));

                    mMap.setMaxZoomPreference(18);
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();

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


        mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {
            @Override
            public void onMapLongClick(LatLng latLng) {
                enabledDisplayByCenter(false);
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                enabledDisplayByCenter(false);
            }
        });


        mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {
            @Override
            public boolean onMarkerClick(Marker marker) {
                isDisplayByCenter = false;
                binding.ibByCenter.setColorFilter(Color.GRAY);
                selMarker = marker;
                InfoDriverReg drv = (InfoDriverReg) marker.getTag();
                if (drv != null && drv.getStatusToHostSrv() != Util.UNKNOWN_DRIVER_STATUS) {

                    binding.ibMenuDrvDot.setVisibility(View.VISIBLE);
                }

                CameraPosition cameraPosition = new CameraPosition.Builder()
                        .target(marker.getPosition())
                        .zoom(18)
                        .build();

                mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                return false;
            }
        });


        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mZoom = mMap.getCameraPosition().zoom;
            }
        });

        runDriversObservationClass();
        displayAllMarkersByCenter();
    }


}