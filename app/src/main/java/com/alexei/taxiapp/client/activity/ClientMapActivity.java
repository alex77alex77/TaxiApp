package com.alexei.taxiapp.client.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.alexei.taxiapp.FillingOrderActivity;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.client.adapter.AdapterClientListOrders;
import com.alexei.taxiapp.client.exClass.ClientAssignDriverClass;
import com.alexei.taxiapp.client.model.KeysOFieldsModel;
import com.alexei.taxiapp.databinding.ActivityClientMapBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.driver.model.DataAuto;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.alexei.taxiapp.util.Util;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
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
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityClientMapBinding binding;

    private static final int REQUEST_RESULT_OK = 5;
    private float mZoom = 16;

    private ExecutorService executorservice;
    private AppDatabase db;
    private Marker clientMarker;

    private Location currentLocation;//класс в котором хронятся долгота широта


    private BuildLocationClass locationClass;

    private FirebaseAuth auth;
    private FirebaseDatabase database;

    private DatabaseReference hostSharedSrvRef;
    private DatabaseReference hostSharedFreeOrdersRef;

    private DatabaseReference hostDriversRef;
    private DatabaseReference hostTransportRef;
    private DatabaseReference hostServersRef;
    private String currentUserUid;

    private boolean bNotifyWarning = false;
    private boolean bWaitResponse;


    private RecyclerView.LayoutManager orderLayoutManager;
    private InfoOrder orderRequest;
    private AlertDialog alertRequestInfo;

    private AdapterClientListOrders ordersAdapter;
    private ArrayList<InfoOrder> arrOrders;
    private ArrayList<ClientAssignDriverClass> assignDrvList;

    private final Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(getResources().getConfiguration().orientation);

        binding = ActivityClientMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapClient);
        mapFragment.getMapAsync(this);

        executorservice = Executors.newFixedThreadPool(2);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы

        arrOrders = new ArrayList<>();
        assignDrvList = new ArrayList<>();

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {

            currentUserUid = auth.getCurrentUser().getUid();
            database = FirebaseDatabase.getInstance(); //доступ к корневой папке базы данных

            hostTransportRef = database.getReference().child("transport");
            hostServersRef = database.getReference().child("serverList");
            hostSharedSrvRef = database.getReference().child("SHAREDSERVER");
            hostSharedFreeOrdersRef = database.getReference().child("SHAREDSERVER/freeOrders");

            hostDriversRef = hostSharedSrvRef.child("driversList");


            binding.btnCancelWaitPermission.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
//                    flClientMapBlock.setVisibility(View.GONE);
                }
            });

            binding.fabAdd.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!bWaitResponse) {

                        //-------------------------вызов такси
                        if (currentLocation != null) {
                            Intent intent = new Intent(ClientMapActivity.this, FillingOrderActivity.class);
                            intent.putExtra("location", new DataLocation(currentLocation));
                            intent.putExtra("employer", Util.EMPLOYER_CLIENT);

                            startActivityForResult(intent, Util.DATA_FILLING_ORDER);
                        } else {
                            showMessage(getString(R.string.client_not_gps));
                        }
                    } else {

                        showWaitResponseInfo(getString(R.string.break_req_new_order), 1);
                    }

                }
            });


            buildRecyclerView();
            handlerItemTouchHelper();

            executorservice.submit(this::loadFillingOrders);

        } else {
            finish();
        }
    }

    private void showWaitResponseInfo(String msg, int flg) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        builder.setCancelable(true);

        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        if (flg == 1) {
            builder.setTitle(R.string.title_app);
            builder.setIcon(R.drawable.ic_baseline_info_24);
        }
        builder.setPositiveButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                resetData(orderRequest);
            }
        });
        builder.create();
        alertRequestInfo = builder.show();
    }


    private void loadFillingOrders() {

        arrOrders.addAll(db.getInfoOrderDAO().getAllFillingOrders(currentUserUid));

        arrOrders.forEach(o -> {
            createdListeners(o);
        });
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
                InfoOrder order = arrOrders.get(position); //----------------------действие выполняется после сдвига

                if (order.getStatus() != Util.KILL_ORDER_STATUS) {

                    AlertDialog.Builder builder = new AlertDialog.Builder(ClientMapActivity.this);
                    builder.setMessage(R.string.want_delete_order);
                    builder.setCancelable(false);
                    builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

                        public void onClick(DialogInterface dialog, int which) {

                            removeOrderRef(order);
                        }
                    });

                    builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            ordersAdapter.notifyItemChanged(position);
                        }
                    });

                    builder.create().show();
                } else {

                    removeOrderRef(order);
                }
            }

        }).attachToRecyclerView(binding.rvFillingOrders);//---------------прикрепить это действие к recyclerView

    }


    private void removeOrderRef(InfoOrder order) {
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
                if (error == null || order.getStatus() == Util.KILL_ORDER_STATUS) {
                    Util.removeByHostListener(refSrv.child("freeOrders").child(order.getKeyOrder()), mapListeners);
                    arrOrders.remove(order);
                    ordersAdapter.notifyDataSetChanged();

                    executorservice.submit(() -> deleteOrderInDb(order));
                }
            }
        });
    }

    private void deleteOrderInDb(InfoOrder order) {
        db.getInfoOrderDAO().delFillingOrder(order.getKeyOrder(), currentUserUid);
    }

    private void buildRecyclerView() {

        binding.rvFillingOrders.setHasFixedSize(true);

        orderLayoutManager = new LinearLayoutManager(this);
        binding.rvFillingOrders.setLayoutManager(orderLayoutManager);

        ordersAdapter = new AdapterClientListOrders(this, arrOrders);
        binding.rvFillingOrders.setAdapter(ordersAdapter);

        ordersAdapter.setListener(new AdapterClientListOrders.OnListener() {
            @Override
            public void onMenuItemClick(MenuItem menuItem, InfoOrder order) {
                handlerMenuClickAdapter(menuItem, order);
            }

            @Override
            public void onSelectItemClick(InfoOrder order) {
                handlerSelectItemAdapter(order);
            }
        });
    }

    private void handlerMenuClickAdapter(MenuItem item, InfoOrder order) {

        switch (item.getItemId()) {
            case R.id.actionMsgSrv:
                if (!order.getProviderKey().equals("SHAREDSERVER")) {//если не общий сервер

                    sendMsg(hostServersRef.child(order.getProviderKey()).child("freeOrders").child(order.getKeyOrder()).child("msgS").getRef(), getString(R.string.msg_for_company));
                }
                break;
            case R.id.actionMsgDrv:
                if (order.getProviderKey().equals("SHAREDSERVER")) {//если на общем сервере
                    sendMsg(hostSharedFreeOrdersRef.child(order.getKeyOrder()).child("msgD"), getString(R.string.msg_for_driver));
                } else {
                    sendMsg(hostServersRef.child(order.getProviderKey()).child("freeOrders").child(order.getKeyOrder()).child("msgD"), getString(R.string.msg_for_driver));
                }
                break;
        }
    }


    private void sendMsg(DatabaseReference recipientRef, String title) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(title);
        builder.setIcon(R.drawable.ic_baseline_chat_24);
        final EditText input = new EditText(this);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        input.setGravity(Gravity.CENTER);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString().trim();
                if (value.length() > 0) {

                    Util.sendMsgByRef(value, recipientRef);
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        builder.create().show();


    }

    private void handlerSelectItemAdapter(InfoOrder order) {
        Intent intent = new Intent(ClientMapActivity.this, ClientStatusOrderActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("data_order", order);
        intent.putExtras(bundle);

        if (order.getStatus() != Util.ROUTE_FINISHED_ORDER_STATUS && order.getStatus() != Util.KILL_ORDER_STATUS && order.getStatus() != Util.CANCEL_ORDER_STATUS) {

            intent.putExtra("location", new DataLocation(currentLocation));

        } else {

            intent.putExtra("location", new DataLocation(order.getEndRoute()));
        }

        startActivity(intent);
    }

    private void createdListeners(@NonNull InfoOrder order) {
        DatabaseReference refOrder;
        if (order.getProviderKey().equals("SHAREDSERVER")) {
            refOrder = hostSharedFreeOrdersRef.child(order.getKeyOrder());
        } else {
            refOrder = hostServersRef.child(order.getProviderKey()).child("freeOrders").child(order.getKeyOrder());
        }

        initMsgForClientListener(refOrder, order);//сообщения для меня
        initDriverUidListener(refOrder, order);//назначение водителя
        initStatusListener(refOrder, order);//статус

    }

    private void initDriverUidListener(DatabaseReference orderRef, InfoOrder order) {
        if (mapListeners.get(orderRef.child("driverUid")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        String value = snapshot.getValue(String.class);
                        if (value != null) {

                            order.setDriverUid(value);
                        }
                        ordersAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            orderRef.child("driverUid").addValueEventListener(listener);
            mapListeners.put(orderRef.child("driverUid"), listener);
        }
    }

    private void initStatusListener(DatabaseReference orderRef, InfoOrder order) {
        if (mapListeners.get(orderRef.child("status")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        Integer status = snapshot.getValue(Integer.class);
                        if (status != null) {
                            order.setStatus(status);
                            ordersAdapter.notifyDataSetChanged();

                            handlerStatus(orderRef, status, order);
                        }
                    } else {//заказ удален

                        order.setStatus(Util.KILL_ORDER_STATUS);
                        ordersAdapter.notifyDataSetChanged();
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            orderRef.child("status").addValueEventListener(listener);
            mapListeners.put(orderRef.child("status"), listener);
        }
    }


    private void handlerStatus(DatabaseReference orderRef, Integer status, InfoOrder order) {
        switch (status) {
            case Util.ARRIVE_ORDER_STATUS://------------------------------подъехала

                playSignal(R.raw.bip_sound);
                getDataAuto(order);
                break;
            case Util.ROUTE_FINISHED_ORDER_STATUS://--------------------маршрут закончен

                Util.removeByHostListener(orderRef, mapListeners);
                fixedLocationEndRute(order);
                playSignal(R.raw.sms);
                break;
            case Util.FREE_ORDER_STATUS://---------------------отправлен в свободные
                if (order.getProviderKey().equals("SHAREDSERVER")) {//если находится на общем сервере иначе этот статус обрабатывает сервер
                    Util.removeByHostListener(orderRef, mapListeners);//-----------удаление старых слушателей назначеных на выполнение заказа

                    handlerAssignDrvToOrder(orderRef, order, hostSharedSrvRef);
                }
                break;
            case Util.ASSIGN_ORDER_STATUS://--------------------водитель назначен

                playSignal(R.raw.sms);
                break;
            case Util.KILL_ORDER_STATUS://--------------------deleted

                Util.removeByHostListener(orderRef, mapListeners);
                break;
        }
    }

    private void getDataAuto(InfoOrder order) {

        hostTransportRef.child(order.getDriverUid()).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataAuto auto = snapshot.getValue(DataAuto.class);
                    if (auto != null) {
                        showMsgDataTransport(getString(R.string.meet) + auto.toString(), order.getId());

                        order.setDataAuto(auto.toString());
                        ordersAdapter.notifyDataSetChanged();

                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }


    private void initMsgForClientListener(DatabaseReference orderRef, InfoOrder order) {
        if (mapListeners.get(orderRef.child("msgC/msg")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String msg = snapshot.getValue(String.class);
                        if (msg != null && msg.length() > 0) {

                            Util.dlgMessage(ClientMapActivity.this, getString(R.string.order_) + order.getId(), msg, "", null);
                            snapshot.getRef().setValue("");
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            orderRef.child("msgC/msg").addValueEventListener(listener);
            mapListeners.put(orderRef.child("msgC/msg"), listener);
        }
    }

    private void fixedLocationEndRute(InfoOrder order) {

        hostDriversRef.child(order.getDriverUid()).child("location").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataLocation location = snapshot.getValue(DataLocation.class);
                    order.setEndRoute(location);
                    ordersAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMsgDataTransport(String message, long id) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.order_) + id + "\n" + getString(R.string.t_msg_from_driver));
        builder.setMessage(message);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {
//                if (fieldRef != null) {
//                    fieldRef.addListenerForSingleValueEvent(new ValueEventListener() {
//                        @Override
//                        public void onDataChange(@NonNull DataSnapshot snapshot) {
//                            if (snapshot.exists()) {
//                                snapshot.getRef().setValue("");//прочитанно
//                            }
//                        }
//
//                        @Override
//                        public void onCancelled(@NonNull DatabaseError error) {
//
//                        }
//                    });
//                }
            }
        });

        builder.create().show();
    }

//*************************************************  sendNewOrder  ********************  sendNewOrder  *******************  sendNewOrder  *********************

    private void sendNewOrder(InfoOrder order) {


        if (order.getProviderKey().equals("SHAREDSERVER")) {
            //определяем ключ-push для заказа
            String push = hostSharedFreeOrdersRef.push().getKey();
            if (push != null) {

                order.setKeyOrder(push);
                order.setSenderKey("");
                order.setTimestamp(ServerValue.TIMESTAMP);

                sendToSharedSrvRef(order, hostSharedSrvRef);//отправляется на общий сервер, я назначаю водителя
            }
        } else {
            checkSrvHostAccess(order);//проверяем принимает ли сервер заказы от клиентов
        }
    }

    private void checkSrvHostAccess(InfoOrder order) {

        hostServersRef.child(order.getProviderKey()).child("access/keySender").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    //определяем ключ-push для заказа
                    String push = hostServersRef.child(order.getProviderKey()).child("freeOrders").push().getKey();
                    if (push != null) {

                        order.setKeyOrder(push);
                        order.setSenderKey("");
                        order.setTimestamp(ServerValue.TIMESTAMP);


                        Map<String, Object> map = new HashMap<>();
                        map.put("keySender", currentUserUid);
                        map.put("push", push);//----запрашиваем разрешение разместить этот заказ
                        map.put("timer", ServerValue.TIMESTAMP);

                        hostServersRef.child(order.getProviderKey()).child("access").updateChildren(map, new DatabaseReference.CompletionListener() {
                            @Override
                            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                if (error == null) {
                                    Toast.makeText(getApplicationContext(), R.string.request_sender, Toast.LENGTH_LONG).show();

                                    waitPermissionListener(order);//ждем разрешение

                                } else {
                                    if (error.getCode() == -3) {
                                        Toast.makeText(getApplicationContext(), R.string.debug_server_busy, Toast.LENGTH_LONG).show();
                                    }
                                }
                            }
                        });//спрашиваем у сервера можно ли отправить заказ
                    } else {

                        Toast.makeText(getApplicationContext(), R.string.debug_false_create_order, Toast.LENGTH_LONG).show();
                    }

                } else {

                    Toast.makeText(getApplicationContext(), R.string.not_accept_orders, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void waitPermissionListener(InfoOrder order) {
        bWaitResponse = true;
        orderRequest = order;
        showWaitResponseInfo(getString(R.string.debug_send_req), 0);

        if (mapListeners.get(hostServersRef.child(order.getProviderKey()).child("freeOrders/push")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        String valPush = snapshot.getValue(String.class);//push ордера разрещенного
                        if (valPush != null) {
                            if (valPush.trim().length() > 0) {

                                if (valPush.equals(order.getKeyOrder())) {//----есть разрешение для размещения моего заказа

                                    resetData(order);
                                    sendToSrvRef(order, hostServersRef);//отправляется на сервер (где сервер назначает водителя)
                                } else {//--------------------------------------разшифровываем ответ

                                    String[] responseCode = valPush.split("/");
                                    if (responseCode[0].equals("2") && responseCode[1].equals(order.getKeyOrder())) {//2-deny, 3-block

                                        showMessage(getString(R.string.deny_accept_order));
                                        resetData(order);
                                    } else if (responseCode[0].equals("3") && responseCode[1].equals(order.getKeyOrder())) {//2-deny, 3-block

                                        resetData(order);
                                    }
                                    //else пришел другой ключ
                                }
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            hostServersRef.child(order.getProviderKey()).child("freeOrders/push").addValueEventListener(listener);
            mapListeners.put(hostServersRef.child(order.getProviderKey()).child("freeOrders/push"), listener);
        }
    }

    private void resetData(InfoOrder order) {
        if (order != null) {
            Util.removeByRefListener(hostServersRef.child(order.getProviderKey()).child("freeOrders/push"), mapListeners);//сброс
        }
        bWaitResponse = false;
        if (alertRequestInfo != null) {
            alertRequestInfo.dismiss();
        }
    }

    private void saveFillingOrder(InfoOrder order) {
        InfoOrder fillingOrder = db.getInfoOrderDAO().getFillingOrder(order.getKeyOrder(), currentUserUid);
        if (fillingOrder == null) {
            long id = db.getInfoOrderDAO().add(order);
            order.setId(id);

            runOnUiThread(() -> ordersAdapter.notifyDataSetChanged());
        }
    }

    private void sendToSrvRef(InfoOrder order, DatabaseReference hostSrvRef) {

        Map<String, Object> map = new HashMap<>();
        map.put("/keysO/" + order.getKeyOrder(), new KeysOFieldsModel(""));
        map.put("/freeOrders/" + order.getKeyOrder(), order);

        hostSrvRef.child(order.getProviderKey()).updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {

                if (error == null) {
                    arrOrders.add(order);
                    ordersAdapter.notifyDataSetChanged();

                    createdListeners(order);

                    executorservice.submit(() -> saveFillingOrder(order));
                    Toast.makeText(getApplicationContext(), R.string.debug_send_order_success, Toast.LENGTH_SHORT).show();
                } else {

                    Toast.makeText(getApplicationContext(), R.string.break_send_order, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void sendToSharedSrvRef(InfoOrder order, DatabaseReference hostSrvRef) {
        Map<String, Object> map = new HashMap<>();
        map.put("/keysO/" + order.getKeyOrder(), new KeysOFieldsModel(""));
        map.put("/freeOrders/" + order.getKeyOrder(), order);

        hostSrvRef.updateChildren(map, new DatabaseReference.CompletionListener() {//-----------------------------save  в свободных
            @Override
            public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    arrOrders.add(order);
                    ordersAdapter.notifyDataSetChanged();

//                    createdListeners(order);

                    handlerAssignDrvToOrder(hostSrvRef.child("freeOrders").child(order.getKeyOrder()), order, hostSharedSrvRef);

                    notifyWarningSnackBar();//уделомление: при отсудствии связи заказ будет удален

                    executorservice.submit(() -> saveFillingOrder(order));

                }
            }
        });
    }

    private void showMessage(String message) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_app);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setMessage(message);
        builder.setCancelable(true);


        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {

            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create();
        TextView messageText = (TextView) builder.show().findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);

    }

    private void notifyWarningSnackBar() {
        if (!bNotifyWarning) {

            showMessage(getString(R.string.attention_not_connect));

            bNotifyWarning = true;
        }
    }

    private void handlerAssignDrvToOrder(DatabaseReference orderRef, InfoOrder order, DatabaseReference srvRef) {
        //----------------------------------------обработка назначение звонящего водителя на заказ
        ClientAssignDriverClass clientAssignDriverClass = new ClientAssignDriverClass(orderRef, srvRef, order.getKeyOrder(), order);

        assignDrvList.add(clientAssignDriverClass);

        clientAssignDriverClass.setOnListener(new ClientAssignDriverClass.OnCompleteAssignDriverListener() {
            @Override
            public void onComplete(DatabaseReference orderRef, String drvSelKey, boolean success) {
                if (success) {

                    order.setDriverUid(drvSelKey);
                    createdListeners(order);//новые слушатели, первый удаляются при статусе = свободный
                } else {
                    Toast.makeText(getApplicationContext(), R.string.fails_accept_driver, Toast.LENGTH_LONG).show();
                }
                clientAssignDriverClass.recoverResources();
                assignDrvList.remove(clientAssignDriverClass);
            }

            @Override
            public void onDeletedOrder(DatabaseReference orderRef, InfoOrder order) {
                clientAssignDriverClass.recoverResources();
                assignDrvList.remove(clientAssignDriverClass);

                ordersAdapter.notifyDataSetChanged();

            }
        });
    }


    private void playSignal(int res) {
        MediaPlayer mediaPlayer;

        mediaPlayer = MediaPlayer.create(getApplicationContext(), res);
        mediaPlayer.start();

        mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mediaPlayer) {
                mediaPlayer.release();

            }
        });
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
        if (currentLocation != null) {
            LatLng clientLocation = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());
            googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(clientLocation, 17));
            clientMarker = googleMap.addMarker(new MarkerOptions().position(clientLocation).title(getString(R.string.I)));

        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Util.DATA_FILLING_ORDER) {
            if (resultCode == REQUEST_RESULT_OK) {
                //-------------------------определил заказ
                if (data != null) {

                    InfoOrder order = (InfoOrder) data.getParcelableExtra("data_order");
//                    String nameSrv = data.getStringExtra("server_name");
                    order.setClientUid(currentUserUid);//ключ клиента

                    sendNewOrder(order);//------------------------отправка заказа в узел "свободные"

                } else {

                    Toast.makeText(this, "Error: data=null", Toast.LENGTH_SHORT).show();
                }

            }
        }
    }


    @Override
    protected void onStart() {
        super.onStart();
        runClassLocation();
    }

    private void runClassLocation() {
        locationClass = BuildLocationClass.getInstance(ClientMapActivity.this, null);
        locationClass.setOnUpdateListener((location, satellites) -> {

            currentLocation = location;
            updateLocationUi();

        });
        locationClass.getCurrentLocation();
    }

    private void updateLocationUi() {

        if (currentLocation != null) {

            if (mMap != null) {
                LatLng drvLoc = new LatLng(currentLocation.getLatitude(), currentLocation.getLongitude());//----------получение координат

                if (clientMarker == null) {
                    clientMarker = mMap.addMarker(new MarkerOptions().position(drvLoc).title(getString(R.string.I)));
                } else {
                    clientMarker.setPosition(drvLoc);
                }

                setCameraInTheDirection(currentLocation);
            }
        }
    }

    private void setCameraInTheDirection(Location location) {

        CameraPosition position = CameraPosition.builder()
                .bearing(location.getBearing())
                .target(new LatLng(clientMarker.getPosition().latitude, clientMarker.getPosition().longitude))
                .zoom(mZoom)
                .tilt(mMap.getCameraPosition().tilt)
                .build();
        mMap.animateCamera(CameraUpdateFactory.newCameraPosition(position));
    }


    @Override
    protected void onDestroy() {
        Util.removeAllValueListener(mapListeners);
        locationClass.stopLocationUpdate();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
//        super.onBackPressed();
    }
}