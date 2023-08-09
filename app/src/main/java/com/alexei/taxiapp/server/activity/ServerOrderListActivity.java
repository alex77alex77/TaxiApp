package com.alexei.taxiapp.server.activity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.os.Parcelable;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.alexei.taxiapp.FillingOrderActivity;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.client.model.KeysOFieldsModel;
import com.alexei.taxiapp.databinding.ActivityServerOrderListBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.db.RatesSrv;
import com.alexei.taxiapp.db.SettingServer;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.exClass.AcceptClientOrderClass;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.alexei.taxiapp.exClass.RunServicesServer;
import com.alexei.taxiapp.server.adapter.AdapterServerOrderList;
import com.alexei.taxiapp.server.exClass.PrintShortReportClass;
import com.alexei.taxiapp.server.exClass.SrvAssignOrderClass;
import com.alexei.taxiapp.server.exClass.SrvOrdersObservationClass;
import com.alexei.taxiapp.server.model.DataSenderModel;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ServerOrderListActivity extends AppCompatActivity {
    private SettingServer setting = ServerInformsAboutEventsClass.setting;
    private RunServicesServer servicesServer;
    private ExecutorService executorservice;
    private AppDatabase db;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference sharedRef;
    private DatabaseReference serverRef;

    private DatabaseReference locationDrvRef;
    private String currUserUid = "";

    private RecyclerView.LayoutManager orderLayoutManager;
    private AdapterServerOrderList ordersAdapter;
    private ArrayList<InfoOrder> orderList;

    private BuildLocationClass locationClass;
    private SrvOrdersObservationClass ordersObservation;
    private PrintShortReportClass shortReportClass;

    private Location currLocation = new Location("");

    private boolean bSortByDate;
    private String selectPhone = "";

    private final Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private final List<SrvAssignOrderClass> assignedList = new ArrayList<>();

    private int sorted = -1;
    private ActivityServerOrderListBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityServerOrderListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        setContentView(R.layout.activity_server_order_list);
        setRequestedOrientation(getResources().getConfiguration().orientation);

        executorservice = Executors.newSingleThreadExecutor();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы
        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базы данных

        Intent intent = getIntent();
        if (intent != null) {
            sorted = intent.getIntExtra("sorted", -1);
        }

        if (auth.getCurrentUser() != null) {
            currUserUid = auth.getCurrentUser().getUid();
            serverRef = database.getReference().child("serverList").child(currUserUid);

            sharedRef = database.getReference().child("SHAREDSERVER");
            locationDrvRef = sharedRef.child("driversList").child(currUserUid).child("location");


        } else {
            finish();
        }

        PopupMenu popupMenu = new PopupMenu(ServerOrderListActivity.this, binding.btnMenuSrvOrderList);
        popupMenu.inflate(R.menu.server_order_list_menu);

        binding.ibShowOrderReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                displayElementReport(binding.tvOrderReport.getHeight());
            }
        });

        binding.btnSrvCreateOrder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                createOrder();

            }
        });


        binding.btnMenuSrvOrderList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                popupMenu.show();
            }
        });

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {

                switch (item.getItemId()) {

//                    case R.id.menuFindOrder:
//                        searchOrder();
//                        break;
                    case R.id.menuSortedOrder:
                        chooseSortedOrder();
                        break;
                    case R.id.menuClearCompletedOrders:
                        clearCompletedOrders();
                        break;
                }
                return true;
            }
        });

        locationClass = new BuildLocationClass(ServerOrderListActivity.this, locationDrvRef);
        locationClass.setOnUpdateListener(new BuildLocationClass.OnUpdateLocationListener() {
            @Override
            public void onUpdateLocation(Location location, int satellites) {
                currLocation = location;
            }
        });
        locationClass.getCurrentLocation();


        binding.chkAutoDistribution.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    sendAllWaitOrders();
                }
            }
        });


        handlerItemTouchHelper();

        orderList = SrvOrdersObservationClass.ordersList;//ссылка на массив
        ordersObservation = SrvOrdersObservationClass.getInstance(serverRef, auth.getCurrentUser());

        buildRecyclerView();

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
                InfoOrder order = orderList.get(position); //----------------------действие выполняется после сдвига

                if (order.getStatus() != Util.ROUTE_FINISHED_ORDER_STATUS && order.getStatus() != Util.WAIT_SEND_ORDER_STATUS) {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ServerOrderListActivity.this);
                    builder.setMessage(getString(R.string.remove_order_) + order.getId() + ")?");
                    builder.setCancelable(false);

                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            actionSwiped(order, position);

                        }
                    });
                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {

                            ordersAdapter.notifyItemChanged(position);

                        }
                    });
                    builder.show();
                } else {
                    actionSwiped(order, position);
                }
            }

        }).attachToRecyclerView(binding.recyclerViewServerOrderList);//---------------прикрепить это действие к recyclerView

    }

    private void runShortReportClass() {
        PrintShortReportClass.stopExecute = true;
        shortReportClass = PrintShortReportClass.getInstance();
        shortReportClass.setOnListeners(new PrintShortReportClass.OnListener() {
            @Override
            public void onChangeReport(String report, int type) {
                if (type == 2) {
                    runOnUiThread(() -> {
                        binding.tvOrderReport.setText(report);
                    });
                }
            }
        });
        shortReportClass.notifyChangeReport(2);
    }

    private void actionSwiped(InfoOrder order, int position) {

        ordersObservation.deleteOrder(order);
    }

    private void clearCompletedOrders() {
        ordersObservation.clearCompletedOrders();
    }

    private void displayElementReport(int dist) {

        if (binding.tvOrderReport.getVisibility() == View.GONE) {
            if (binding.tvOrderReport.getTranslationY() < 0) {
                binding.tvOrderReport.animate().translationYBy(dist).setDuration(500);
            }

            binding.tvOrderReport.setVisibility(View.VISIBLE);
            PrintShortReportClass.stopExecute = false;
            shortReportClass.notifyChangeReport(2);
        } else {

            binding.tvOrderReport.setTranslationY(-dist);
            binding.tvOrderReport.setVisibility(View.GONE);
            PrintShortReportClass.stopExecute = true;
        }
    }


    private void chooseSortedOrder() {
        final String[] rateArray = {getString(R.string.sort_order_wait), getString(R.string.sort_order_free),
                getString(R.string.sort_order_finished), getString(R.string.sort_order_executed), getString(R.string.sort_by_date)};

        AlertDialog.Builder builder = new AlertDialog.Builder(ServerOrderListActivity.this);
        builder.setIcon(R.drawable.ic_baseline_sort_24);
        builder.setTitle(R.string.sort_by);

        builder.setItems(rateArray, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                int status = -2;
                switch (which) {
                    case 0:
                        status = Util.WAIT_SEND_ORDER_STATUS;

                        break;
                    case 1:
                        status = Util.FREE_ORDER_STATUS;

                        break;
                    case 2:
                        status = Util.ROUTE_FINISHED_ORDER_STATUS;

                        break;
                    case 3:
                        status = Util.EXECUTION_ORDER_STATUS;

                        break;
                    case 4:

                        sortedByDate();
                        return;
                }
                sortedByStatus(status);
            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.getListView().setItemChecked(1, true);
        alertDialog.show();
    }

    private void sortedByStatus(int status) {
        boolean exists = false;
        int index = 0;
        if (orderList.size() > 0) {

            for (InfoOrder model : orderList) {

                if (status == Util.ASSIGN_ORDER_STATUS) {
                    if (model.getStatus() == Util.EXECUTION_ORDER_STATUS || model.getStatus() == Util.ASSIGN_ORDER_STATUS || model.getStatus() == Util.ARRIVE_ORDER_STATUS) {
                        exists = true;
                        Collections.swap(orderList, orderList.indexOf(model), index);
                        index++;

                    }
                } else {
                    if (model.getStatus() == status) {
                        exists = true;
                        Collections.swap(orderList, orderList.indexOf(model), index);
                        index++;
                    }
                }

            }
        }
        ordersAdapter.notifyDataSetChanged();
        if (!exists) {
            Toast.makeText(getApplicationContext(), R.string.not_found, Toast.LENGTH_LONG).show();
        }
    }

    private void sortedByDate() {
        if (!bSortByDate) {
            bSortByDate = true;
            Collections.sort(orderList, new Comparator<InfoOrder>() {
                public int compare(InfoOrder p1, InfoOrder p2) {
                    return ((Long) p1.getTimestamp()).compareTo((Long) p2.getTimestamp());
                }
            });
        } else {
            Collections.reverse(orderList);
        }

        ordersAdapter.notifyDataSetChanged();
    }

    private void handlerSorted() {
        if (sorted == Util.SORTED_MSG_SRV) {
            sortedMsg();
        }
    }

    private void sortedMsg() {
        int index = 0;

        for (InfoOrder model : orderList) {
            if (!model.getMsgS().getMsg().isEmpty()) {
                Collections.swap(orderList, orderList.indexOf(model), index);
                index++;
            }
        }

        ordersAdapter.notifyDataSetChanged();
    }

    private void createOrder() {

        try {
            List<RatesSrv> rates = getRates();

            Intent intent = new Intent(ServerOrderListActivity.this, FillingOrderActivity.class);
            intent.putParcelableArrayListExtra("rates", (ArrayList<? extends Parcelable>) rates);
            intent.putExtra("location", new DataLocation(currLocation));//.getLatitude(), currLocation.getLongitude()

            startActivityForResult(intent, Util.DATA_FILLING_ORDER);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }


    }

    private List<RatesSrv> getRates() throws ExecutionException, InterruptedException {
        Callable task = () -> {
            List<RatesSrv> rates = db.getRatesDAO().getRates(currUserUid);

            return rates;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (List<RatesSrv>) future.get();
    }


    private void sendAllWaitOrders() {
        for (InfoOrder order : orderList) {
            if (order != null && order.getStatus() == Util.WAIT_SEND_ORDER_STATUS) {
                sendToFreeOrdersHost(order);
            }
        }
    }

    private void buildRecyclerView() {

        binding.recyclerViewServerOrderList.setHasFixedSize(true);

        orderLayoutManager = new LinearLayoutManager(this);
        binding.recyclerViewServerOrderList.setLayoutManager(orderLayoutManager);

        ordersAdapter = new AdapterServerOrderList(ordersObservation.getOrdersList());
        binding.recyclerViewServerOrderList.setAdapter(ordersAdapter);

        ordersAdapter.setOnListener(new AdapterServerOrderList.OnListener() {
            @Override
            public void onMenuItemClick(MenuItem menuItem, InfoOrder order) {
                cmdMenuAdapterSrvOrderList(menuItem, order);
            }

            @Override
            public void onAdapterItemLongClick(InfoOrder order) {
//                dialogEditOrder(order);
            }

            @Override
            public void onMailClick(int pos, InfoOrder order) {
                readMsg(order);
            }
        });
    }


    /////////////////-onActivityResult-------------onActivityResult-onActivityResult-----onActivityResult
    /////////////////-onActivityResult-------------onActivityResult-onActivityResult-----onActivityResult
    /////////////////-onActivityResult-------------onActivityResult-onActivityResult-----onActivityResult


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case Util.DATA_FILLING_ORDER:
                if (resultCode == Util.RESULT_OK) {
                    //-------------------------определил заказ
                    if (data != null) {
                        InfoOrder order = (InfoOrder) data.getParcelableExtra("data_order");
                        if (order != null) {

                            order.setId(getOrderId() + 1);
                            order.setProviderKey(currUserUid);
                            order.setProviderName(setting.getServerName());
                            order.setSenderKey("");
                            order.setStatus(Util.WAIT_SEND_ORDER_STATUS);

                            newOrderToBufferAdapter(order);//------------------------отправка заказа в буфер перед в узел "свободные"
                        }
                    }
                }
                break;
            case Util.EDIT_FILLING_ORDER:
                if (resultCode == Util.RESULT_OK) {
                    if (data != null) {
                        InfoOrder order = (InfoOrder) data.getParcelableExtra("order");
                        if (order != null) {

                        }
                    }
                }
                break;
        }
    }

    private long getOrderId() {
        Comparator<InfoOrder> comparator = (o1, o2) -> Long.compare(o1.getId(), o2.getId());
        InfoOrder order = orderList.stream().max(comparator).orElse(null);
        if (order != null) {
            return order.getId();
        } else {
            return 0;
        }
    }

    private void newOrderToBufferAdapter(InfoOrder order) {

        orderList.add(order);

        if (binding.chkAutoDistribution.isChecked()) {
            sendToFreeOrdersHost(order);
        }
    }

    //-----------НОВЫЙ ОРДЕР
    private void sendToFreeOrdersHost(InfoOrder order) {

        final String key = serverRef.child("freeOrders").push().getKey();
        Map<String, Object> map = new HashMap<>();


        if (key != null) {
            order.setKeyOrder(key);
            order.setStatus(Util.FREE_ORDER_STATUS);
            order.setTimestamp(System.currentTimeMillis());

            map.put("/keysO/" + key, new KeysOFieldsModel(""));
            map.put("/freeOrders/" + key, order);

            serverRef.updateChildren(map);
        }
    }


    @Override
    protected void onDestroy() {

        try {
            removeAllListeners(mapListeners);
        } finally {
            super.onDestroy();
        }
    }

    private void removeAllListeners(Map<DatabaseReference, ValueEventListener> listeners) {
        for (Map.Entry<DatabaseReference, ValueEventListener> entry : listeners.entrySet()) {
            entry.getKey().removeEventListener(entry.getValue());
        }
        listeners.clear();
    }

    @Override
    protected void onStart() {
        runOrdersObservation();
        runShortReportClass();
        runServicesClass();
        super.onStart();
    }

    private void runServicesClass() {
        servicesServer = RunServicesServer.getInstance(serverRef, auth.getCurrentUser());


        servicesServer.setOnClientOrderListener(new RunServicesServer.OnClientOrderListener() {
            @Override
            public void onPostClientOrder(String keySender) {

            }

            @Override
            public void onPostRequestClientOrder(AcceptClientOrderClass acceptClientOrderClass, String keySender, String name) {
                acceptClientOrderClass.getDataRequestSender(keySender, name);
            }

            @Override
            public void onConfirmation(AcceptClientOrderClass acceptClientOrderClass, String name, DataSenderModel dataSender) {
                setConfirmationAcceptOrderClient(acceptClientOrderClass, name, dataSender);
            }
        });
    }

    private void runOrdersObservation() {

        ordersObservation.setOnUpdateListener(new SrvOrdersObservationClass.OnUpdateListener() {
            @Override
            public void onWaitTimeOut(InfoOrder order, DatabaseReference orderRef, String driverUid) {
                ordersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onMsgForSrvChange(InfoOrder order, DatabaseReference orderRef) {
                ordersAdapter.notifyDataSetChanged();
            }

            @Override
            public void onChangeStatus(InfoOrder order) {
                ordersAdapter.notifyDataSetChanged();
                shortReportClass.notifyChangeReport(2);
            }

            @Override
            public void onUpdateDriverUid(InfoOrder order, DatabaseReference orderRef) {
                ordersAdapter.notifyDataSetChanged();
                shortReportClass.notifyChangeReport(2);
            }

            @Override
            public void onTimerRouteFinish(InfoOrder order, DatabaseReference orderRef, long timer) {
//обновляется время финиша
            }

            @Override
            public void onRemove() {
                ordersAdapter.notifyDataSetChanged();
                shortReportClass.notifyChangeReport(2);
            }

        });

        ordersAdapter.notifyDataSetChanged();
        executorservice.submit(this::handlerSorted);
    }

    private void setConfirmationAcceptOrderClient(AcceptClientOrderClass acceptClientOrderClass, String name, DataSenderModel dataSender) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View view = (View) inflater.inflate(R.layout.dialog_confirmation_accept_order_client, null);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setTitle(R.string.confirmation_order);
        builder.setView(view);
        builder.setCancelable(false);
        EditText tvName = view.findViewById(R.id.etClientName);
        tvName.setText(name);
        TextView tvTime = view.findViewById(R.id.tvTimePost);
        tvTime.setText(Util.formatTimeDate.format(dataSender.getTimer()));

        builder.setPositiveButton(R.string.accept, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                acceptClientOrderClass.confirmation(dataSender, name.equals(tvName.getText().toString()) ? "" : tvName.getText().toString(), Util.ACCEPT_CLIENT);
            }
        });
        builder.setNegativeButton(R.string.deny, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                acceptClientOrderClass.confirmation(dataSender, name.equals(tvName.getText().toString()) ? "" : tvName.getText().toString(), Util.DENY_CLIENT);
            }
        });
        builder.setNeutralButton(R.string.cmd_blocked, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
//перегружен
            }
        });


        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                handlerBlockSenderKey(acceptClientOrderClass, dataSender, name.equals(tvName.getText().toString()) ? "" : tvName.getText().toString().toString(), alertDialog);
            }
        });
    }

    private void handlerBlockSenderKey(AcceptClientOrderClass acceptClientOrderClass, DataSenderModel dataSender, String name, AlertDialog alertDialog) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setIcon(R.drawable.ic_baseline_help_24);
        builder.setTitle(R.string.block_key);
        builder.setMessage(R.string.debug_block_key);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                acceptClientOrderClass.confirmation(dataSender, name, Util.BLOCK_CLIENT);
                alertDialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        builder.create().show();
    }


    public void cmdMenuAdapterSrvOrderList(MenuItem item, InfoOrder order) {

        if (order != null) {
            switch (item.getItemId()) {

                case R.id.actionOrderSendToCallSign:

                    if (order.getStatus() == Util.WAIT_SEND_ORDER_STATUS) {
                        sendToCallSign(order);
                    }
                    break;
                case R.id.actionOrderSendToAll:

                    if (order.getStatus() == Util.WAIT_SEND_ORDER_STATUS) {
                        sendToFreeOrdersHost(order);
                    }
                    break;
                case R.id.actionOrderMsgDrv:

                    sendMessage(order);
                    break;
                case R.id.actionOrderPhoneDrv:

                    initDrvCallPhone(order);
                    break;
                case R.id.actionOrderCancel:
                    if (order.getClientUid().length() > 1) {//заказ клиента

                        Toast.makeText(this, R.string.debug_break_cancel_orter, Toast.LENGTH_LONG).show();
                    } else {
                        ordersObservation.cancelOrder(order);
                    }
                    break;
                case R.id.actionOrderInfoAssignDrv:

                    initInfoDriver(order.getDriverUid());
                    break;
                case R.id.actionOrderMsgClient:

                    Util.dlgMessage(this, getString(R.string.msg_for_client), "", getString(R.string.msg_from_company), serverRef.child("freeOrders").child(order.getKeyOrder()).child("msgC"));
                    break;
                case R.id.actionOrderPhoneClient:

                    initClientCallPhone(order);
                    break;

            }
        }
    }


    private void initInfoDriver(String keyDrv) {
        executorservice.submit(() -> {
            InfoDriverReg drv = db.getDataDriversServerDAO().getDriverInfo(keyDrv, currUserUid);
            if (drv != null) {
                getDriverInfo(drv);
            }
        });

    }

    private void getDriverInfo(InfoDriverReg drv) {
        if (drv != null) {
            String str = R.string.t_name + "\n" + drv.getName() + "\n\n" +
                    R.string.t_callsign + "\n" + drv.getCallSign() + "\n\n" +
                    R.string.t_phone + "\n" + drv.getPhone() + "\n\n" +
                    R.string.t_transport + "\n" +
                    drv.getAuto().toString() + "\n\n" +
                    R.string.t_type + "\n" + drv.getAutoType();

            showInfo(getString(R.string.t_debug_drv), str);

        }
    }

    private void initDrvCallPhone(InfoOrder order) {
        executorservice.submit(() -> {
            InfoDriverReg assignDriver = db.getDataDriversServerDAO().getDriverInfo(order.getDriverUid(), currUserUid);
            if (assignDriver != null) {
                selectPhone = assignDriver.getPhone();
                if (selectPhone.length() > 1) {
                    requestCallPhonePermission();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.phone_not, Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void initClientCallPhone(InfoOrder order) {
        selectPhone = order.getPhone();
        if (selectPhone.length() > 1) {
            requestCallPhonePermission();
        } else {
            Toast.makeText(getApplicationContext(), R.string.phone_not, Toast.LENGTH_LONG).show();
        }
    }

    private void sendMessage(InfoOrder order) {
        executorservice.submit(() -> {

            InfoDriverReg assignDriver = db.getDataDriversServerDAO().getDriverInfo(order.getDriverUid(), currUserUid);
            if (assignDriver != null) {

                String title = getString(R.string.t_whom) + assignDriver.getName() + " (" + assignDriver.getCallSign() + ")";

                DatabaseReference ref = database.getReference().child("serverList").child(assignDriver.getServerUid()).child("driversList").child(assignDriver.getDriverUid()).child("msgD");
                runOnUiThread(() -> {

                    Util.dlgMessage(ServerOrderListActivity.this, title, "", "", ref);
                });

            } else {
                Toast.makeText(ServerOrderListActivity.this, R.string.false_def_drv, Toast.LENGTH_LONG).show();
            }

        });
    }

    private void sendToCallSign(InfoOrder order) {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setTitle(R.string.t_send_order_drv);
        alert.setIcon(R.drawable.ic_baseline_send_24);
        alert.setMessage(R.string.t_callsign);

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setGravity(Gravity.CENTER);
        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                if (value.length() > 0) {

                    initAssignedOrderForDrv(Integer.parseInt(value), order);
                }
            }
        });
        alert.show();
    }

    private void initAssignedOrderForDrv(int callSign, InfoOrder order) {

        executorservice.submit(() -> {

            InfoDriverReg driver = db.getDataDriversServerDAO().getDriverInfo(callSign, currUserUid);
            if (driver != null) {
                if (driver.getStatusToHostSrv() == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {
                    if (driver.getShiftStatus() == Util.SHIFT_OPEN_DRV_STATUS) {

                        sendOrderForDrv(order, driver);
                    } else {

                        notifyShiftSTDlg(order, driver);
                    }

                } else {
                    runOnUiThread(() -> {

                        Toast.makeText(ServerOrderListActivity.this, R.string.drv_disconnectd, Toast.LENGTH_LONG).show();
                    });
                }

            } else {
                runOnUiThread(() -> {

                    Toast.makeText(ServerOrderListActivity.this, R.string.driver_not_found, Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void notifyShiftSTDlg(InfoOrder order, InfoDriverReg driver) {
        runOnUiThread(() -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setIcon(R.drawable.ic_baseline_report_24);
            alert.setTitle(R.string.send_order_to_drv);
            alert.setMessage(R.string.debug_shift_close);

            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                    sendOrderForDrv(order, driver);
                }
            }).setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });

            alert.show();
        });

    }

    private void sendOrderForDrv(InfoOrder order, InfoDriverReg driver) {

        SrvAssignOrderClass assignClass = new SrvAssignOrderClass(serverRef, order, driver);
        assignedList.add(assignClass);

        assignClass.setCompletedListener(new SrvAssignOrderClass.OnCompletedListener() {
            @Override
            public void onCompleted(boolean success) {
                if (success) {

                    Toast.makeText(getApplicationContext(), R.string.debug_send_order_success, Toast.LENGTH_LONG).show();
                } else {

                    Toast.makeText(getApplicationContext(), R.string.break_send_order, Toast.LENGTH_LONG).show();
                }
                assignedList.remove(assignClass);
            }
        });
    }


    private void showInfo(String title, String str) {
        runOnUiThread(() -> {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);
            alert.setIcon(R.drawable.ic_baseline_chat_24);
            alert.setTitle(title);
            alert.setMessage(str);

            alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {

                }
            });

            alert.show();
        });

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

                    ActivityCompat.requestPermissions(ServerOrderListActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
                }
            });
        } else {
//            callPhone();
            //запрос на разрешение без объяснения  -интерфейс на получение разрешения
            ActivityCompat.requestPermissions(ServerOrderListActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
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

    public void selItemEditOrder(InfoOrder order) {

    }


    private void readMsg(InfoOrder order) {
        if (order != null) {
            serverRef.child("freeOrders").child(order.getKeyOrder()).child("msgS/msg").setValue("");
            Util.dlgMessage(this, getString(R.string.t_msg_from_client), order.getMsgS().getMsg(), getString(R.string.msg_from_company), serverRef.child("freeOrders").child(order.getKeyOrder()).child("msgC"));
        }
    }
}