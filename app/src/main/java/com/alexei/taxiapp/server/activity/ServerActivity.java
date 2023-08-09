package com.alexei.taxiapp.server.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityRatesBinding;
import com.alexei.taxiapp.databinding.ActivityServerBinding;
import com.alexei.taxiapp.exClass.AcceptClientOrderClass;
import com.alexei.taxiapp.db.InfoRequestConnect;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.driver.model.DataResponse;
import com.alexei.taxiapp.driver.model.DeniedDrvModel;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.exClass.RunServicesServer;
import com.alexei.taxiapp.server.adapter.AdapterDeniedList;
import com.alexei.taxiapp.server.exClass.PrintShortReportClass;
import com.alexei.taxiapp.server.exClass.RequestAcceptClass;
import com.alexei.taxiapp.server.exClass.SrvDriversObservationClass;
import com.alexei.taxiapp.server.exClass.SrvOrdersObservationClass;
import com.alexei.taxiapp.server.model.DataSenderModel;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.db.SettingServer;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class ServerActivity extends AppCompatActivity {
    private SettingServer setting = ServerInformsAboutEventsClass.setting;
    private AppDatabase db;

    private RunServicesServer servicesServer;
    private SrvOrdersObservationClass ordersObservation;
    private SrvDriversObservationClass driversObservation;
    private PrintShortReportClass shortReportClass;
    private ServerInformsAboutEventsClass serverEvents;


    private DatabaseReference hostDrvRef;
    private DatabaseReference refServer;
    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private String mCurrUserUid;

    private List<InfoOrder> orderList = new ArrayList<>();
    private List<InfoDriverReg> driversList = new ArrayList<>();
    private ActivityServerBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(getResources().getConfiguration().orientation);
        binding = ActivityServerBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы

        database = FirebaseDatabase.getInstance();
        hostDrvRef = database.getReference().child("SHAREDSERVER/driversList");

        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            currentUser = auth.getCurrentUser();
            mCurrUserUid = currentUser.getUid();

            refServer = database.getReference().child("serverList").child(mCurrUserUid);
            binding.tvSrvNameAccount.setText(currentUser.getDisplayName());

            binding.tvSOSDrvInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handlerClickDrvInfo(Util.SORTED_SOS);

                }
            });

            binding.tvOrderMsgInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ServerActivity.this, ServerOrderListActivity.class);
                    intent.putExtra("sorted", Util.SORTED_MSG_SRV);
                    startActivity(intent);
                }
            });

            binding.tvDrvMsgInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    handlerClickDrvInfo(Util.SORTED_MSG_SRV);

                }
            });

            binding.tvBreakInfo.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showInfoBreak(servicesServer.getDeniedDrvList());
                }
            });

            binding.tvRequest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    binding.flBlockSrv.setVisibility(View.VISIBLE);
                    servicesServer.readRequest();
                }
            });

            binding.ibSrvSettingMenu.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ServerActivity.this, ServerSettingActivity.class);
                    startActivityForResult(intent, Util.RESULT_SETTING_SERVER);
                }
            });

            binding.btnSrvDrivers.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ServerActivity.this, ServerDriverListActivity.class);
                    startActivity(intent);
                }
            });

            binding.btnSrvOrders.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(ServerActivity.this, ServerOrderListActivity.class);
                    startActivity(intent);
                }
            });


            runAllClasses();
        } else {
            finish();
        }
    }

    private void handlerClickDrvInfo(int sorted) {
        Intent intent = new Intent(ServerActivity.this, ServerDriverListActivity.class);
        intent.putExtra("sorted", sorted);
        startActivity(intent);
    }

    private void runAllClasses() {//запусе при старте классы перегружаются
        try {
            runServerInformsAboutEventsClass();

            runServicesClass();
            servicesServer.checkHasRequestNewDrv();//класс уже запущен и спрашиваем есть ли что для обработки
            servicesServer.checkHasRequestClientOrder();

            ordersObservation = SrvOrdersObservationClass.getInstance(refServer, currentUser);
            orderList = SrvOrdersObservationClass.ordersList;//ссылка на массив заказов

            driversObservation = SrvDriversObservationClass.getInstance(refServer, currentUser);
            driversList = SrvDriversObservationClass.allDrivers;//ссылка на массив водителей

            runShortReportClass();

            setViewRequest();

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), getString(R.string.break_load_services), Toast.LENGTH_LONG).show();
        }

    }

    private void runServerInformsAboutEventsClass() {
        serverEvents = ServerInformsAboutEventsClass.getInstance();
        serverEvents.onListener.onEvents(0);//удаляем иконку уведомления в активити водителя
    }

    private void runServicesClass() {
        servicesServer = RunServicesServer.getInstance(refServer, currentUser);

        servicesServer.setOnListener(new RunServicesServer.OnListener() {

            @Override
            public void showRequest(InfoRequestConnect requestModel, long count) {
                displayRequest(requestModel);

            }

            @Override
            public void onExitReadRequest(boolean bSuccess) {
//                flBlock.setVisibility(View.INVISIBLE);
            }

            @Override
            public void onHasRequest(RequestAcceptClass requestClass, boolean b, long count) {
                handlerHasRequest(b, count);
            }

            @Override
            public void onBreakSaveDriver(String descDeny) {//DeniedDrvModel model
                handlerSaveDriverBreak(descDeny);
                serverEvents.onListener.onEvents(1);//сигнал водителю
            }

            @Override
            public void onError(String err) {
                binding.flBlockSrv.setVisibility(View.INVISIBLE);
                showInfo(err);
            }

        });

        servicesServer.setOnClientOrderListener(new RunServicesServer.OnClientOrderListener() {
            @Override
            public void onPostClientOrder(String keySender) {

            }

            @Override
            public void onConfirmation(AcceptClientOrderClass acceptClientOrderClass, String name, DataSenderModel dataSender) {

                setConfirmationAcceptOrderClient(acceptClientOrderClass, name, dataSender);//показать диалог для подтверждения
            }

            @Override
            public void onPostRequestClientOrder(AcceptClientOrderClass acceptClientOrderClass, String keySender, String name) {
                acceptClientOrderClass.getDataRequestSender(keySender, name);//продолжение обработки запроса классом
            }
        });

    }

    private void setConfirmationAcceptOrderClient(AcceptClientOrderClass acceptClientOrderClass, String name, DataSenderModel dataSender) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View view = (View) inflater.inflate(R.layout.dialog_confirmation_accept_order_client, null);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setTitle(R.string.confirmation);
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

    private void handlerHasRequest(boolean b, long count) {
        if (!setting.isChkConnect()) {//вручную
            if (b) {
                binding.tvRequest.setVisibility(View.VISIBLE);
            } else {
                binding.tvRequest.setVisibility(View.GONE);
            }
        }

        binding.tvRequest.setText("(" + count + ")");
    }

    private void handlerSaveDriverBreak(String descDeny) {//DeniedDrvModel model
        runOnUiThread(() -> {
            if (setting.isChkConnect()) {
//                deniedDrvList.add(model);
                binding.tvBreakInfo.setVisibility(View.VISIBLE);

            } else {
                if (!descDeny.isEmpty()) {

                    Util.dlgMessage(ServerActivity.this, "", descDeny, "", null);
                }
            }
        });
    }

    private void runShortReportClass() {
        PrintShortReportClass.stopExecute = false;
        shortReportClass = PrintShortReportClass.getInstance();
        shortReportClass.setOnListeners(new PrintShortReportClass.OnListener() {
            @Override
            public void onChangeReport(String report, int type) {
                runOnUiThread(() -> {
                    if (type == 1) {

                        binding.tvReportDrv.setText(report);
                    } else {

                        binding.tvReportOrder.setText(report);
                    }
                });
            }
        });
        shortReportClass.notifyChangeReport(1);
        shortReportClass.notifyChangeReport(2);
    }

    private SettingServer getSetting() throws ExecutionException, InterruptedException {
        Callable task = () -> {
            SettingServer server = db.getSettingServerDAO().getSetting(mCurrUserUid);
            if (server == null) {
                server = new SettingServer(
                        0,
                        mCurrUserUid,
                        getString(R.string.unknown),
                        false,
                        false,
                        24,
                        4,
                        12,
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

    private void showInfoBreak(ArrayList<DeniedDrvModel> deniedDrvList) {
        ArrayList<DeniedDrvModel> copyDeniedList = new ArrayList<>(deniedDrvList);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_list_message, null);
        builder.setIcon(R.drawable.ic_baseline_info_24);
        builder.setTitle(getString(R.string.refusal_) + copyDeniedList.size() + "):");
        builder.setView(dialogView);

        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvListMessage);
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);

        AdapterDeniedList adapter = new AdapterDeniedList(copyDeniedList);
        rv.setAdapter(adapter);

        adapter.setSelectListener(new AdapterDeniedList.OnSelectListener() {
            @Override
            public void onSelectItem(DeniedDrvModel deniedDrvModel, int position) {

            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                binding.tvBreakInfo.setVisibility(View.GONE);
                deniedDrvList.removeAll(copyDeniedList);
            }
        });

        builder.create().show();
    }

    private void displayRequest(InfoRequestConnect requestModel) {
        Intent intent = new Intent(ServerActivity.this, ServerReadRequestActivity.class);
        Bundle bundle = new Bundle();
        bundle.putParcelable("data_request", requestModel);
        intent.putExtras(bundle);

        startActivityForResult(intent, Util.RESULT_REQUEST_CONNECT);
    }


    private void setViewRequest() {

        if (setting.isChkConnect()) {

            binding.tvRequest.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_person_add_alt_1_24, 0, 0, 0);
            binding.tvRequest.setVisibility(View.VISIBLE);
            binding.tvRequest.setEnabled(false);

        } else {

            binding.tvRequest.setEnabled(true);
            binding.tvRequest.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_baseline_assignment_ind_24, 0, 0, 0);
        }

    }


    @Override
    protected void onStart() {

        try {
            binding.flBlockSrv.setVisibility(View.INVISIBLE);

            ordersObservation.setOnUpdateListener(new SrvOrdersObservationClass.OnUpdateListener() {
                @Override
                public void onWaitTimeOut(InfoOrder order, DatabaseReference orderRef, String driverUid) {

                }

                @Override
                public void onMsgForSrvChange(InfoOrder order, DatabaseReference orderRef) {
                    if (!order.getMsgS().getMsg().isEmpty()) {
                        binding.tvOrderMsgInfo.setVisibility(View.VISIBLE);
                        serverEvents.onListener.onEvents(1);
                    } else {
                        checkAllInfo();
                    }
                }

                @Override
                public void onChangeStatus(InfoOrder order) {
                    shortReportClass.notifyChangeReport(2);
                }

                @Override
                public void onUpdateDriverUid(InfoOrder order, DatabaseReference orderRef) {

                }

                @Override
                public void onTimerRouteFinish(InfoOrder order, DatabaseReference orderRef, long timer) {

                }

                @Override
                public void onRemove() {
                    shortReportClass.notifyChangeReport(2);
                }
            });

            driversObservation.setOnListener(new SrvDriversObservationClass.OnUpdateListener() {

                @Override
                public void onChangeStatusShift(InfoDriverReg driver) {

                }

                @Override
                public void onSOS(InfoDriverReg driver) {
                    if (driver.getSosModel() != null) {
                        binding.tvSOSDrvInfo.setVisibility(View.VISIBLE);
                        serverEvents.onListener.onEvents(1);
                    } else {
                        checkAllInfo();//проверим все у всех
                    }
                    shortReportClass.notifyChangeReport(1);
                }

                @Override
                public void onChangeDrvStatusToSrv(InfoDriverReg driver) {

                    shortReportClass.notifyChangeReport(1);
                }

                @Override
                public void onUpdateLocation(InfoDriverReg driver) {

                }

                @Override
                public void onMsgForSrv(InfoDriverReg driver) {
                    if (!driver.getMessage().getMsg().isEmpty()) {
                        binding.tvDrvMsgInfo.setVisibility(View.VISIBLE);
                        serverEvents.onListener.onEvents(1);
                    } else {
                        checkAllInfo();
                    }
                }

                @Override
                public void onAssignedOrder(InfoDriverReg driver) {

                }

                @Override
                public void onExOrder(InfoDriverReg driver) {

                }

                @Override
                public void onUpdateSharedStatus(InfoDriverReg driver) {
                    shortReportClass.notifyChangeReport(1);
                }

                @Override
                public void onRemoveHost(String key) {
                    shortReportClass.notifyChangeReport(1);
                }
            });

            runShortReportClass();

            runServicesClass();

            checkAllInfo();
        } finally {
            super.onStart();
        }
    }

    private void checkAllInfo() {
        long countSosDrv = driversList.stream().filter(d -> d.getSosModel() != null).count();
        if (countSosDrv > 0) {
            binding.tvSOSDrvInfo.setVisibility(View.VISIBLE);
            binding.tvSOSDrvInfo.setText("" + countSosDrv);
        } else {

            binding.tvSOSDrvInfo.setVisibility(View.GONE);
        }
        //--------------
        long countMsgOrder = orderList.stream().filter(o -> !o.getMsgS().getMsg().isEmpty()).count();
        if (countMsgOrder > 0) {

            binding.tvOrderMsgInfo.setVisibility(View.VISIBLE);
            binding.tvOrderMsgInfo.setText("" + countMsgOrder);

        } else {

            binding.tvOrderMsgInfo.setVisibility(View.GONE);
        }
        //--------------------------
        long countMsgDrv = driversList.stream().filter(d -> !d.getMessage().getMsg().isEmpty()).count();
        if (countMsgDrv > 0) {

            binding.tvDrvMsgInfo.setVisibility(View.VISIBLE);
            binding.tvDrvMsgInfo.setText("" + countMsgDrv);
        } else {

            binding.tvDrvMsgInfo.setVisibility(View.GONE);
        }


        serverEvents.onListener.onEvents(countMsgDrv + countMsgOrder + countSosDrv);
    }


    //***********************************  onActivityResult ****************************************
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Util.RESULT_SETTING_SERVER:
                if (resultCode == Util.RESULT_OK) {
                    try {

                        updateDisplayElementSetting(getSetting());

                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                break;
            case Util.RESULT_REQUEST_CONNECT:
                if (resultCode == Util.RESULT_OK) {
                    if (data != null) {
                        InfoRequestConnect request = (InfoRequestConnect) data.getParcelableExtra("data_request");
                        servicesServer.saveRequestDataDriver(request);

                        sendResponse(request.getKeyS(), Util.CONNECTED_TO_SERVER_DRIVER_STATUS);//отправка ответа водителю
                    }
                    preparingHostRequest();//подготовка узла с флагом приема
                }
                if (resultCode == Util.RESULT_CANCEL) {
                    if (data != null) {
                        String keyDrv = data.getStringExtra("key");
                        if (keyDrv.trim().length() > 0) {
                            refServer.child("driversList").child(keyDrv).removeValue();//удаление из хоста сервера(если есть)
                            sendResponse(keyDrv, Util.RESPONSE_DENY);//отправка ответа водителю
                        }
                    }
                    preparingHostRequest();//подготовка узла с флагом приема
                }

                break;
        }
    }

    private void preparingHostRequest() {

        if (!setting.isChkDisableReq()) {

            Map<String, Object> map = new HashMap<>();
            map.put("accept", "");
            map.put("data", null);

            refServer.child("request").updateChildren(map, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error != null) {

                        showInfo(getString(R.string.debug_break_prepare_request));
                    }
                }
            });
        }

    }

    private void showInfo(String msg) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(msg);
        builder.setCancelable(true);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {

            }
        });
        builder.create();
        builder.show();
    }

    private void sendResponse(String keyDrv, int responseCode) {
        hostDrvRef.child(keyDrv).child("response").child(currentUser.getUid()).setValue(new DataResponse(responseCode, ServerValue.TIMESTAMP));

    }


    private void updateDisplayElementSetting(SettingServer sett) {

        setViewRequest();
        servicesServer.changeChkDisableRequest(sett.isChkDisableReq());
    }
}