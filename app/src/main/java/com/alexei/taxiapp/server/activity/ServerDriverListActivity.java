package com.alexei.taxiapp.server.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.InputType;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.server.adapter.AdapterServerDriverList;
import com.alexei.taxiapp.server.exClass.PrintShortReportClass;
import com.alexei.taxiapp.server.exClass.ReportWriteClass;
import com.alexei.taxiapp.server.exClass.SrvDriversObservationClass;
import com.alexei.taxiapp.server.exClass.SrvOrdersObservationClass;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.server.model.MsgModel;
import com.alexei.taxiapp.db.ServerReport;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ServerDriverListActivity extends AppCompatActivity {
    private ExecutorService executorservice;// = Executors.newSingleThreadExecutor();
    private AppDatabase db;
    private PrintShortReportClass shortReportClass;
    private SrvDriversObservationClass driversObservation;

    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference refServer;
    private String currUserUid = "";
    private RecyclerView rvServerDriverList;
    private RecyclerView.LayoutManager driverLayoutManager;
    private AdapterServerDriverList driverAdapter;
    private List<InfoDriverReg> driversList = SrvDriversObservationClass.allDrivers;//ссылка;
    private List<InfoOrder> orderList;

    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();

    private TextView tvReport;
    private ImageButton ibReport;

    private Button btnDisplayToMap;
    private Button btnMenuServerDrivers;


    private String selectPhone = "";
    private int sorted = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_server_driver_list);
        setRequestedOrientation(getResources().getConfiguration().orientation);


        executorservice = Executors.newFixedThreadPool(1);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы


        ibReport = findViewById(R.id.ibReport);
        tvReport = findViewById(R.id.tvDrvReport);
        btnDisplayToMap = findViewById(R.id.btnDisplayToMap);
        btnMenuServerDrivers = findViewById(R.id.btnMenuServerDrivers);

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базы данных


        if (auth.getCurrentUser() != null) {

            currUserUid = auth.getCurrentUser().getUid();
            refServer = database.getReference().child("serverList").child(currUserUid);
            Intent intent = getIntent();
            if (intent != null) {
                sorted = intent.getIntExtra("sorted", -1);
            }


            PopupMenu popupMenu = new PopupMenu(ServerDriverListActivity.this, btnMenuServerDrivers);
            popupMenu.inflate(R.menu.server_driver_list_menu);


            ibReport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayElementReport(tvReport.getHeight());
                }
            });

            btnMenuServerDrivers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popupMenu.show();
                }
            });

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    handlerMenuItemClick(item);
                    return true;
                }
            });

            btnDisplayToMap.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    displayDotDriverToMap("");//all

                }
            });

            driversObservation = SrvDriversObservationClass.getInstance(database.getReference().child("serverList").child(currUserUid), auth.getCurrentUser());
//            driversList = SrvDriversObservationClass.allDrivers;//ссылка

            buildRecyclerView();

            orderList = SrvOrdersObservationClass.ordersList;//ссылка

            handlerItemTouchHelper();

            runShortReport();
        } else {
            finish();
        }
    }

    private void handlerMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menuFindDriver:
                searchDriver();
                break;
            case R.id.menuSortedDriver:
                chooseSortedDriver();
                break;
            case R.id.menuSendMsgAllDrivers:
                dlgInputText();
                break;
        }
    }

    private void dlgInputText() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.t_msg_all_drivers);
        alert.setIcon(R.drawable.ic_baseline_send_24);
        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(20)});
        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if (value.length() > 0) {
                    sendMsgAllDrivers(value);
                }
            }
        });
        alert.show();
    }

    private void sendMsgAllDrivers(String msg) {
        Map<String, Object> mChilds = new HashMap<>();

        driversList.stream().filter(d -> d.getStatusToHostSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS).forEach(d -> {
            mChilds.put("driversList/" + d.getDriverUid() + "/msgD", new MsgModel(msg));
        });

        if (mChilds.size() > 0) {
            refServer.updateChildren(mChilds, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {
                        Toast.makeText(getApplicationContext(), getString(R.string.debug_sending).concat(String.valueOf(mChilds.size())), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } else {
            Toast.makeText(getApplicationContext(), R.string.no_drivers, Toast.LENGTH_SHORT).show();
        }

    }

    private void runShortReport() {
        PrintShortReportClass.stopExecute = true;
        shortReportClass = PrintShortReportClass.getInstance();
        shortReportClass.setOnListeners(new PrintShortReportClass.OnListener() {
            @Override
            public void onChangeReport(String report, int type) {
                if (type == 1) {
                    runOnUiThread(() -> {
                        tvReport.setText(report);
                    });
                }
            }
        });
        shortReportClass.notifyChangeReport(1);
    }

    private void handlerItemTouchHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                InfoDriverReg driver = driversList.get(position); //----------------------действие выполняется после сдвига

                AlertDialog.Builder builder = new AlertDialog.Builder(ServerDriverListActivity.this);
                builder.setMessage(R.string.remove_driver_from_system);
                builder.setCancelable(false);

                builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        deleteDrvRef(driver);
                    }
                });
                builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        driverAdapter.notifyItemChanged(position);

                    }
                });
                builder.create().show();

            }

        }).attachToRecyclerView(rvServerDriverList);//---------------прикрепить это действие к recyclerView

    }


    private void displayElementReport(int dist) {

        if (tvReport.getVisibility() == View.GONE) {

            if (tvReport.getTranslationY() < 0) {
                tvReport.animate().translationYBy(dist).setDuration(500);
            }
            tvReport.setVisibility(View.VISIBLE);
            PrintShortReportClass.stopExecute = false;
            shortReportClass.notifyChangeReport(1);
        } else {

            tvReport.setTranslationY(-dist);
            tvReport.setVisibility(View.GONE);
            PrintShortReportClass.stopExecute = true;
        }
    }

    private void chooseSortedDriver() {
        final String[] rateArray = {getString(R.string.status_busy), getString(R.string.status_free)};//, getString(R.string.status_disabled), getString(R.string.unknown)

        AlertDialog.Builder builder = new AlertDialog.Builder(ServerDriverListActivity.this);
        builder.setIcon(R.drawable.ic_baseline_sort_24);
        builder.setTitle(R.string.sort_by);

        builder.setItems(rateArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int status = -2;
                switch (which) {
                    case 0:
                        status = Util.BUSY_DRIVER_STATUS;

                        break;
                    case 1:
                        status = Util.FREE_DRIVER_STATUS;

                        break;
//                    case 2:
//                        status = Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS;
//
//                        break;
//                    case 3:
//                        status = Util.UNKNOWN_DRIVER_STATUS;
//
//                        break;

                }
                if (!sortedDriver(status)) {
                    Toast.makeText(getApplicationContext(), R.string.not_found, Toast.LENGTH_LONG).show();
                }
            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.getListView().setItemChecked(1, true);
        alertDialog.show();
    }


    private boolean sortedDriver(int status) {
        boolean exists = false;
        int index = 0;

        for (InfoDriverReg model : driversList) {
            if (model.getStatusShared() == status) {
                exists = true;
                Collections.swap(driversList, driversList.indexOf(model), index);
                index++;
            }
        }

        driverAdapter.notifyDataSetChanged();
        return exists;
    }

    private void handlerSorted() {
        if (sorted == Util.SORTED_MSG_SRV) {
            sortedMsg();
        } else if (sorted == Util.SORTED_SOS) {
            sortedSos();
        }
    }

    private void sortedSos() {
        int index = 0;

        for (InfoDriverReg model : driversList) {
            if (model.getSosModel() != null) {
                Collections.swap(driversList, driversList.indexOf(model), index);
                index++;
            }
        }

        driverAdapter.notifyDataSetChanged();
    }

    private void sortedMsg() {
        int index = 0;

        for (InfoDriverReg model : driversList) {
            if (!model.getMessage().getMsg().isEmpty()) {
                Collections.swap(driversList, driversList.indexOf(model), index);
                index++;
            }
        }

        driverAdapter.notifyDataSetChanged();
    }

    private void searchDriver() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.t_callsign);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if (!value.isEmpty()) {
                    if (!findCallSignDriver(Long.parseLong(value))) {
                        Toast.makeText(ServerDriverListActivity.this, R.string.drv_not_found, Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

        alert.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private boolean findCallSignDriver(long callSign) {
        boolean exists = false;
        if (driversList.size() > 0) {
            for (InfoDriverReg model : driversList) {
                if (model.getCallSign() == callSign) {
                    exists = true;
                    Collections.swap(driversList, driversList.indexOf(model), 0);
                }
            }
        }
        driverAdapter.notifyDataSetChanged();
        return exists;
    }


    private void deleteDrvRef(InfoDriverReg driver) {
//удаление с сервера
        Map<String, Object> map = new HashMap<>();
        map.put("/driversList/" + driver.getDriverUid(), null);
        map.put("/keysD/" + driver.getDriverUid(), null);

        refServer.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    //удаление вручную от сервера проходит полностью из arrayList, db и database
                    executorservice.submit(() -> {
                        db.getDataDriversServerDAO().deleteDriverInfo(driver.getDriverUid(), currUserUid);
                        driversObservation.shiftObservation.notifyDeletedDriver(driver);
                        driversList.remove(driver);
                        runOnUiThread(() -> {
                            driverAdapter.notifyDataSetChanged();
                            Toast.makeText(getApplicationContext(), R.string.driver_deleted, Toast.LENGTH_SHORT).show();
                        });

                    });
                }
            }
        });
    }


    private void buildRecyclerView() {
        rvServerDriverList = findViewById(R.id.recyclerViewServerDriverList);
        rvServerDriverList.setHasFixedSize(true);

        driverLayoutManager = new LinearLayoutManager(this);
        rvServerDriverList.setLayoutManager(driverLayoutManager);

        driverAdapter = new AdapterServerDriverList(getApplicationContext(), driversList);
        rvServerDriverList.setAdapter(driverAdapter);

        driverAdapter.setListener(new AdapterServerDriverList.SelectListener() {
            @Override
            public void onReadMsg(InfoDriverReg drv) {
                readMsgSrvAdapter(drv);
            }

            @Override
            public void onMenuItemClick(MenuItem menuItem, InfoDriverReg drv) {
                cmdMenuAdapterSrvDrvList(menuItem, drv);
            }

            @Override
            public void onSendMsgClick(String str, InfoDriverReg drv) {
                sendMsgToDrv(str, drv);
            }
        });
    }


    private void displayDotDriverToMap(String key) {
        Intent intent = new Intent(ServerDriverListActivity.this, ServerDrvDotTheMapActivity.class);
        intent.putExtra("keyDrv", key);
        startActivityForResult(intent, Util.DRIVER_DOT_THE_MAP_RESULT);
    }

    @Override
    protected void onDestroy() {
        removeAllListener();
        super.onDestroy();
    }

    private void removeAllListener() {
        Util.removeAllValueListener(mapListeners);
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case Util.RESULT_EDIT_DRIVER:
                switch (resultCode) {

                    case Util.EDIT_OK_RESULT:
                        if (data != null) {
                            Toast.makeText(getApplicationContext(), R.string.changes_save, Toast.LENGTH_LONG).show();

                            executorservice.submit(() -> {
                                long id2 = data.getLongExtra("id", 0);
                                setChangesDriver(id2);

                            });
                        }
                        break;
                    case Util.EDIT_BREAK_RESULT:

                        Toast.makeText(getApplicationContext(), R.string.break_changes_save, Toast.LENGTH_LONG).show();

                        break;
                }
                break;
            case Util.DRIVER_DOT_THE_MAP_RESULT:
//                switch (resultCode) {
//                    case Activity.RESULT_CANCELED:
//
//                        break;
//                }
                break;
        }
    }

    private void setChangesDriver(long id) {
        InfoDriverReg driver = db.getDataDriversServerDAO().getDriverInfo(id, currUserUid);

        if (driver != null) {

            InfoDriverReg drv = driversList.stream().filter(d -> d.getDriverUid().equals(driver.getDriverUid())).findAny().orElseGet(null);
            if (drv != null) {

                drv.setName(driver.getName());
                drv.setPhone(driver.getPhone());
                drv.setPriority(driver.getPriority());
                drv.setCallSign(driver.getCallSign());
                drv.setAuto(driver.getAuto());
                drv.setDislocation(driver.getDislocation());
                drv.setBalance(driver.getBalance());
                drv.setAutoType(driver.getAutoType());
            }

            runOnUiThread(() -> {
                driverAdapter.notifyDataSetChanged();
            });


        }
    }

    private void getAllDrivers() {

        driversObservation.setOnListener(new SrvDriversObservationClass.OnUpdateListener() {


            @Override
            public void onChangeStatusShift(InfoDriverReg driver) {
                runOnUiThread(() -> {
                    driverAdapter.notifyDataSetChanged();

                });
            }

            @Override
            public void onSOS(InfoDriverReg driver) {
                driverAdapter.notifyDataSetChanged();
                shortReportClass.notifyChangeReport(1);
            }

            @Override
            public void onChangeDrvStatusToSrv(InfoDriverReg driver) {
                runOnUiThread(() -> {
                    driverAdapter.notifyDataSetChanged();
                    shortReportClass.notifyChangeReport(1);
                });
            }

            @Override
            public void onUpdateLocation(InfoDriverReg driver) {
                driverAdapter.notifyDataSetChanged();
            }

            @Override
            public void onMsgForSrv(InfoDriverReg driver) {
                driverAdapter.notifyDataSetChanged();
            }

            @Override
            public void onAssignedOrder(InfoDriverReg driver) {
                driverAdapter.notifyDataSetChanged();
            }

            @Override
            public void onExOrder(InfoDriverReg driver) {
                driverAdapter.notifyDataSetChanged();
            }

            @Override
            public void onUpdateSharedStatus(InfoDriverReg driver) {
                driverAdapter.notifyDataSetChanged();
                shortReportClass.notifyChangeReport(1);
            }


            @Override
            public void onRemoveHost(String key) {
                runOnUiThread(() -> {
                    driverAdapter.notifyDataSetChanged();
                    shortReportClass.notifyChangeReport(1);
                });
            }
        });


        driverAdapter.notifyDataSetChanged();
        shortReportClass.notifyChangeReport(1);

        executorservice.submit(this::handlerSorted);
    }

    @Override
    protected void onStart() {
        getAllDrivers();
        runShortReport();
        super.onStart();
    }

    ////////////////////////////////////////////////////////////////////////////////  MenuAdapter ////////////////////////////////////
    private void cmdMenuAdapterSrvDrvList(MenuItem item, InfoDriverReg driver) {

        switch (item.getItemId()) {

            case R.id.actionAdapterDisplayInMap:

                if (driver.getStatusToHostSrv() != Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS && driver.getStatusToHostSrv() != Util.UNKNOWN_DRIVER_STATUS) {
                    displayDotDriverToMap(driver.getDriverUid());
                }
                break;
            case R.id.actionAdapterInfoDriver:

                editInfoDriver(driver);
                break;
            case R.id.actionAdapterBlockDrv:

                executorservice.submit(() -> {
                    if (driver.getStatusToHostSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {

                        refServer.child("driversList").child(driver.getDriverUid()).child("status").setValue(Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS);
//                        driversObservation.assignedStatusDrvOnSrv(driver, Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS);
                    }
                });
                break;
            case R.id.actionAdapterUnblockDrv:

                executorservice.submit(() -> {
                    if (driver.getStatusToHostSrv() == Util.BLOCKED_TO_SYSTEM_DRIVER_STATUS) {

                        refServer.child("driversList").child(driver.getDriverUid()).child("status").setValue(Util.CONNECTED_TO_SERVER_DRIVER_STATUS);
//                        driversObservation.assignedStatusDrvOnSrv(driver, Util.CONNECTED_TO_SERVER_DRIVER_STATUS);
                    }
                });
                break;
//            case R.id.actionAdapterConnectDrv:
//                if (driver.getStatusToHostSrv() == Util.NOT_REF_DRIVER_STATUS_TMP) {//данные есть - узла нет

//                    try {
//                        InfoDriverReg drv = getDriver(driver.getDriverUid());
//
//                        if (drv != null) {
//                            driversObservation.createDrv(drv);
//                        }
//                    } catch (ExecutionException | InterruptedException e) {
//                        e.printStackTrace();
//                    }

//                }
//                break;
            case R.id.actionAdapterPhone:

                selectPhone = driver.getPhone();
                requestCallPhonePermission();
                break;
            case R.id.actionAdapterRoute:

                showInfoRoute(driver);
                break;
            case R.id.actionAdapterReport:

                showReport(driver.getDriverUid(), (ArrayList<ServerReport>) ReportWriteClass.reports);
                break;
//            case R.id.actionAdapterOpenSwift:
//
//                handlerOpenShiftDrv(driver);
//                break;
        }
    }

//    private InfoDriverReg getDriver(String driverUid) throws ExecutionException, InterruptedException {
//
//        Callable task = () -> {
//            InfoDriverReg driver = db.getDataDriversServerDAO().getDriverInfo(driverUid, currUserUid);
//
//            return driver;
//        };
//
//        FutureTask future = new FutureTask<>(task);
//        new Thread(future).start();
//
//        return (InfoDriverReg) future.get();
//
//    }

//    private void handlerOpenShiftDrv(InfoDriverReg driver) {//ручное открытие
//
//    }

    private void showReport(String driverUid, ArrayList<ServerReport> reports) {

        Intent intent = new Intent(ServerDriverListActivity.this, ServerReportDrvActivity.class);
        intent.putExtra("keyDrv", driverUid);
        Bundle bundle = new Bundle();
        bundle.putParcelableArrayList("reports", reports);
        intent.putExtras(bundle);

        startActivity(intent);
    }

    private void showInfoRoute(InfoDriverReg driver) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(R.drawable.ic_baseline_chat_24);
        alert.setTitle(R.string.t_route);

        if (orderList != null) {
            InfoOrder order = orderList.stream().filter(o -> o.getKeyOrder().equals(driver.getExOrder().getKeyOrder())).findAny().orElse(null);
            if (order != null) {

                String strFrom = Util.getAddress(order.getFrom().getLatitude(), order.getFrom().getLongitude(), Util.TYPE_ADDRESS_LONG);
                String strTo = Util.getAddress(order.getTo().getLatitude(), order.getTo().getLongitude(), Util.TYPE_ADDRESS_LONG);

                alert.setMessage(getString(R.string.t_where_from) + strFrom +
                        "\n" + getString(R.string.where_to) + "\n" + strTo +
                        "\n\n" + getString(R.string.t_rate) + "\n" + order.getRate().toString() +
                        "\n\n" + Util.formatTimeDate.format(order.getTimestamp()));
            } else {
                alert.setMessage(R.string.not_data);
            }
        }


        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        alert.show();
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

                    ActivityCompat.requestPermissions(ServerDriverListActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
                }
            });
        } else {
//            callPhone();
            //запрос на разрешение без объяснения  -интерфейс на получение разрешения
            ActivityCompat.requestPermissions(ServerDriverListActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
//        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CALL_PHONE);

        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Util.REQUEST_CALL_PHONE_PERMISSION) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {   //-------------если разрешил

                callPhone(selectPhone);

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

    private void sendMsgToDrv(String msg, InfoDriverReg driver) {
        if (driver != null) {
            DatabaseReference reference = database.getReference().child("serverList").child(driver.getServerUid()).child("driversList").child(driver.getDriverUid());
            reference.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        if (Objects.equals(snapshot.child("msgD/msg").getValue(String.class), "")) {

                            snapshot.getRef().child("msgD").setValue(new MsgModel(msg), new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                    if (error == null) {
                                        Toast.makeText(getApplicationContext(), R.string.msg_send, Toast.LENGTH_SHORT).show();
                                    }
                                }
                            });
                        } else {
                            Toast.makeText(getApplicationContext(), R.string.debug_not_accept_message, Toast.LENGTH_SHORT).show();
                        }

                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }

    private void editInfoDriver(InfoDriverReg infoDriverReg) {
        if (infoDriverReg != null) {
            Intent intent = new Intent(ServerDriverListActivity.this, EditDriverActivity.class);
            intent.putExtra("mode", Util.EDIT_MODE_DRIVER);
            intent.putExtra("id", infoDriverReg.getId());
            startActivityForResult(intent, Util.RESULT_EDIT_DRIVER);
        }
    }


    private void readMsgSrvAdapter(InfoDriverReg driver) {
        DatabaseReference reference = database.getReference().child("serverList").child(driver.getServerUid()).child("driversList").child(driver.getDriverUid()).child("msgS");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    MsgModel message = snapshot.getValue(MsgModel.class);
                    if (message != null && message.getMsg().length() > 0) {

                        showMessage(driver);
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMessage(InfoDriverReg drv) {

        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_chat, null);
        dialogBuilder.setView(dialogView);

        final TextView textViewTitle = dialogView.findViewById(R.id.tvTimeMsgTitle);
        final TextView textViewMessage = dialogView.findViewById(R.id.tvMessage);
        final EditText editTextAnswerMessage = dialogView.findViewById(R.id.etAnswerMessage);
        final Button buttonAnswer = dialogView.findViewById(R.id.buttonAnswer);
        editTextAnswerMessage.setHint(R.string.hint_answer);
        textViewTitle.setText(Util.formatTimeDate.format(drv.getMessage().getCreateTime()) + "\n" + drv.getName() + "(" + drv.getCallSign() + ")");
        buttonAnswer.setText(R.string.ok);

        textViewMessage.setText(drv.getMessage().getMsg());

        final AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        editTextAnswerMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                buttonAnswer.setText(editable.length() > 0 ? getString(R.string.send) : getString(R.string.close));
            }
        });

        buttonAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                clearMsgSrv(drv);

                if (editTextAnswerMessage.length() > 0) {

                    sendMsgToDrv(editTextAnswerMessage.getText().toString(), drv);

                }
                alertDialog.dismiss();

            }
        });
    }

    private void clearMsgSrv(InfoDriverReg model) {
        DatabaseReference reference = database.getReference().child("serverList").child(model.getServerUid()).child("driversList").child(model.getDriverUid()).child("msgS/msg");
        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    snapshot.getRef().setValue("");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

}