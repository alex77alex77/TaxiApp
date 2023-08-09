package com.alexei.taxiapp.driver.activity;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.MultiSpinner;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.SelectPointInMapActivity;
import com.alexei.taxiapp.databinding.ActivityDriverSettingBinding;
import com.alexei.taxiapp.db.DataAvailableServer;
import com.alexei.taxiapp.db.DataRoute;
import com.alexei.taxiapp.db.InfoRequestConnect;
import com.alexei.taxiapp.db.SettingDrv;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.SettingServer;
import com.alexei.taxiapp.driver.adapter.AdapterAvailableSrvList;
import com.alexei.taxiapp.driver.adapter.AdapterRoutes;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.driver.model.DataAuto;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.driver.model.DataResponse;
import com.alexei.taxiapp.driver.model.InfoServerModel;
import com.alexei.taxiapp.driver.model.ServerModel;
import com.alexei.taxiapp.driver.provider.exClass.TrackingHostServers;
import com.alexei.taxiapp.exClass.RunServicesServer;
import com.alexei.taxiapp.server.activity.RatesActivity;
import com.alexei.taxiapp.server.activity.ServerActivity;
import com.alexei.taxiapp.server.model.AuthDataToHostModel;
import com.alexei.taxiapp.util.Util;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collectors;

public class DriverSettingActivity extends AppCompatActivity {
    private ActivityDriverSettingBinding binding;

    private AppDatabase db;
    private ExecutorService executorservice;
    private AuthDataToHostModel authDataOld = new AuthDataToHostModel("", "");
    private DataAuto autoOld = new DataAuto("", "", "");

    private SettingDrv loadSetting;

    private FirebaseAuth auth;
    private String currentUserUid;
    private FirebaseDatabase database;

    private DatabaseReference serversRef;
    private DatabaseReference sharedDrvRef;
    private DatabaseReference transportRef;
    private DatabaseReference authRef;
    private DatabaseReference loginKRef;


    private DataLocation location;
    private DataLocation dislocation;


    private boolean bTask3;

    private TrackingHostServers trackingHostServers;//собираем все аутентифицированные сервера в системе
    private ArrayList<ServerModel> serverModels = TrackingHostServers.arrServerByHost;

    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private Map<DatabaseReference, ChildEventListener> mapChildListeners = new HashMap<>();
    private AdapterAvailableSrvList adapterAvailableServers;
    private AdapterRoutes adapterRoutes;
    private AlertDialog dlgAvaiServers;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityDriverSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        setContentView(R.layout.activity_driver_setting);
        setRequestedOrientation(getResources().getConfiguration().orientation);

        PopupMenu popupMenu = new PopupMenu(DriverSettingActivity.this, binding.tvAccount);
        popupMenu.inflate(R.menu.account_menu);

        executorservice = Executors.newFixedThreadPool(2);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() == null) {

            finish();
        } else {

            trackingHostServers = TrackingHostServers.getInstance(currentUserUid);
            currentUserUid = auth.getCurrentUser().getUid();
            binding.tvAccount.setText(auth.getCurrentUser().getDisplayName());
            database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных

            serversRef = database.getReference().child("serverList");
            sharedDrvRef = database.getReference().child("SHAREDSERVER/driversList").child(currentUserUid);
            transportRef = database.getReference().child("transport").child(currentUserUid);
            authRef = database.getReference().child("auth").child(currentUserUid);
            loginKRef = sharedDrvRef.child("loginK");

            Intent intent = getIntent();
            if (intent != null) {
                location = intent.getParcelableExtra("location");
                dislocation = intent.getParcelableExtra("location");//по умолчанию
            }
            binding.btnDelSrv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dlgDelServer();
                }
            });

            binding.tvAccount.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    popupMenu.show();
                }
            });

            popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {

                    handlerMenuAccount(item);
                    return true;
                }
            });

            binding.chkRunSrv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadSetting.setRunServer(binding.chkRunSrv.isChecked());
                }
            });

            binding.chkOffSound.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadSetting.setSoundNotification(binding.chkOffSound.isChecked());
                }
            });

            binding.chkVoicing.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loadSetting.setVoicingAmount(binding.chkVoicing.isChecked());
                }
            });

            binding.chkAccessToSOS.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!binding.chkAccessToSOS.isChecked()) {
                        try {

                            requestPassword();

                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        loadSetting.setAccessPassSOS(true);
                    }
                }
            });

            binding.btnDisplayRoute.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    try {
                        List<DataRoute> routes = getRoutes();
                        displayAllRoutes(routes);

                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                }
            });

            binding.ibSelDislocation.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(DriverSettingActivity.this, SelectPointInMapActivity.class);
                    intent.putExtra("location", location);

                    startActivityForResult(intent, Util.GET_LOCATION_FROM);
                }
            });

            binding.btnSettingDrvClose.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            binding.btnMySrv.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {

                    handlerMySrv();
                }
            });

            binding.btnDefineRate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(DriverSettingActivity.this, RatesActivity.class);
                    startActivity(intent);
                }
            });

            binding.btnSaveSetting.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    save();
                }
            });

            binding.ibListServer.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    displayAllServers();
                }
            });

            loadSetting();
            handlerChangeStatusServers();
            printUsedServer();
        }
    }

    private void dlgDelServer() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.deleted_);
        builder.setMessage(R.string.delete_server);
        builder.setIcon(R.drawable.ic_baseline_report_problem_24);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                deletedMyServerRef();
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        builder.create().show();

    }

    private void deletedMyServerRef() {

        Map<String, Object> map = new HashMap<>();
        map.put("/serverList/"+currentUserUid , null);
        map.put("/keysS/"+currentUserUid , null);
        map.put("/names/"+loadSetting.getSrvName() , null);

        database.getReference().updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if(error==null){
                    int i =db.getDataDriversServerDAO().delAllDrivers(currentUserUid);

                }else {
                    Toast.makeText(DriverSettingActivity.this, "error: " + error.getCode(), Toast.LENGTH_SHORT).show();
                }
            }
        });


    }

    private void handlerChangeStatusServers() {
        trackingHostServers.setListener(new TrackingHostServers.OnChangeListener() {
            @Override
            public void onChange() {
                printUsedServer();
                if (adapterAvailableServers != null) {
                    adapterAvailableServers.notifyDataSetChanged();
                }
            }
        });
    }

    private void handlerMenuAccount(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.signOut:
                actionSignOut();
                break;
            case R.id.reset:
                actionReset();
                break;
        }
    }

    private void actionReset() {
        Map<String, Object> map = new HashMap<>();
        map.put("SHAREDSERVER/driversList/" + currentUserUid, null);
        map.put("SHAREDSERVER/keysD/" + currentUserUid, null);
        map.put("serverList/" + currentUserUid, null);
        map.put("keysS/" + currentUserUid, null);
        map.put("transport/" + currentUserUid, null);
        map.put("sos/" + currentUserUid, null);
        map.put("auth/" + currentUserUid, null);
        if (!loadSetting.getSrvName().isEmpty()) {
            map.put("names/" + loadSetting.getSrvName(), null);
        }
        if (!loadSetting.getNameHost().isEmpty()) {
            map.put("names/" + loadSetting.getNameHost(), null);
        }
        serverModels.stream().filter(s -> !s.getKeySrv().isEmpty()).forEach(s -> {
            map.put("serverList/" + s.getKeySrv() + "/driversList/" + currentUserUid, null);
            map.put("serverList/" + s.getKeySrv() + "/keysD/" + currentUserUid, null);
        });

        database.getReference().updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    App.context.deleteDatabase(Util.DATABASE_NAME);

                    finishAffinity();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.break_save_database, Toast.LENGTH_LONG).show();
                }
            }
        });


    }

    private void actionSignOut() {
        loginKRef.setValue(null, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    auth.signOut();
                    try {
                        SettingDrv settingDrv = getSettingDrv();
                        settingDrv.setLoginK("");
                        long id = updateSettingDrv(settingDrv);
                        if (id > 0 && auth.getCurrentUser() == null) {
                            finishAffinity();

                        } else {
                            Toast.makeText(getApplicationContext(), R.string.break_save_change, Toast.LENGTH_LONG).show();
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }

                } else {
                    Toast.makeText(getApplicationContext(), R.string.break_save_database, Toast.LENGTH_LONG).show();
                }
            }
        });


    }

    private void requestPassword() throws ExecutionException, InterruptedException {
        SettingDrv setting = getSettingDrv();
        if (setting.isAccessPassSOS()) {
            binding.btnSaveSetting.setEnabled(false);

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setMessage(R.string.input_passowrd);
            final EditText input = new EditText(this);
            input.setGravity(Gravity.CENTER);
            builder.setView(input);

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    String value = input.getText().toString();
                    if (!value.equals(setting.getPassword())) {

                        Toast.makeText(getApplicationContext(), R.string.invalid_password2, Toast.LENGTH_LONG).show();
                        binding.chkAccessToSOS.setChecked(true);
                    }

                    binding.btnSaveSetting.setEnabled(true);

                    loadSetting.setAccessPassSOS(binding.chkAccessToSOS.isChecked());
                }
            });
            builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                    binding.chkAccessToSOS.setChecked(true);
                    binding.btnSaveSetting.setEnabled(true);
                }
            });
            builder.show();

        }

    }


    private void displayAllRoutes(List<DataRoute> routes) {


        AlertDialog.Builder builder = new AlertDialog.Builder(DriverSettingActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_routes, null);
        builder.setTitle(R.string.route);
        builder.setView(dialogView);


        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvRoutes);
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);


        //в этом списке можно остановить/продолжить работу провайдера
        adapterRoutes = new AdapterRoutes(routes);
        rv.setAdapter(adapterRoutes);

        handlerItemTouchHelper(routes, rv);
        handlerAdapterListener(adapterRoutes);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void handlerAdapterListener(AdapterRoutes adapterRoutes) {
        adapterRoutes.setSelectListener(new AdapterRoutes.OnSelectListener() {
            @Override
            public void onSelectItem(DataRoute route) {
                showRouteFromHistory(route);
            }
        });
    }

    private void showRouteFromHistory(DataRoute route) {
        Intent intent = new Intent(DriverSettingActivity.this, RouteFromHistoryMapActivity.class);
        intent.putExtra("user_id", currentUserUid);
        intent.putExtra("keyOrder", route.getKeyOrder());

        startActivity(intent);
    }

    private void handlerItemTouchHelper(List<DataRoute> routes, RecyclerView rvRates) {
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
                int position = viewHolder.getAbsoluteAdapterPosition();
                executorservice.submit(() -> {
                    DataRoute route = routes.get(position); //----------------------действие выполняется после сдвига
                    deleteRoute(route);
                    routes.remove(route);

                    runOnUiThread(() -> {
                        adapterRoutes.notifyDataSetChanged();
                    });
                });

            }

        }).attachToRecyclerView(rvRates);//---------------прикрепить это действие к recyclerView

    }

    private void deleteRoute(DataRoute route) {
        db.getDataRouteDAO().deleteRoute(currentUserUid, route.getKeyOrder());
    }

    private List<DataRoute> getRoutes() throws ExecutionException, InterruptedException {

        Callable task = () -> {
            List<DataRoute> routes = db.getDataRouteDAO().getRoutes(currentUserUid);

            return routes;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (List<DataRoute>) future.get();

    }


    private void handlerMySrv() {

//определение существование по имени сервера так позволяют правила обратится к узлу
        serversRef.child(currentUserUid).child("info/name").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {//есть
                    Intent intent = new Intent(DriverSettingActivity.this, ServerActivity.class);
                    startActivity(intent);
                } else {//нет то создать

                    dialogCreateHostSrv();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //******************************************************************

    private void dialogCreateHostSrv() {

        AlertDialog.Builder builder = new AlertDialog.Builder(DriverSettingActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View view = (View) inflater.inflate(R.layout.dialog_create_server, null);

        builder.setIcon(R.drawable.ic_baseline_create_24);
        builder.setView(view);
        builder.setTitle(R.string.t_create);
        EditText etFieldName = (EditText) view.findViewById(R.id.etNameServer);
        EditText etPhone = (EditText) view.findViewById(R.id.etPhoneServer);
        MultiSpinner msTypeActivity = (MultiSpinner) view.findViewById(R.id.msTypeActivity);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        runOnUiThread(() -> {
            AlertDialog dialog = builder.show();
            dialog.show();
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    if (!validateHostName(etFieldName) | !validateServices(msTypeActivity)) {
                        return;
                    } else {

                        createNewSrvRef(etPhone.getText().toString(), msTypeActivity.getTextSelectedItems(), etFieldName, dialog);

                    }
                }
            });
        });
    }

    private boolean validateServices(MultiSpinner msTypeActivity) {
        if (msTypeActivity.getTextSelectedItems().isEmpty()) {
            View view = msTypeActivity.getSelectedView();
            msTypeActivity.setError(view, getString(R.string.def_type_activity));
            return false;
        }

        loadSetting.setOrderCriteria(msTypeActivity.getTextSelectedItems());
        return true;
    }

    private void createNewSrvRef(String phoneServer, String typeActivity, EditText name, AlertDialog dialog) {
        Map<String, Object> map = new HashMap<>();

        map.put("/names/" + name.getText().toString(), "");
        map.put("/keysS/" + currentUserUid, "");
        map.put("/serverList/" + currentUserUid + "/info", new InfoServerModel(name.getText().toString(), typeActivity, phoneServer));

        database.getReference().updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {

                    dialog.dismiss();
                    Toast.makeText(getApplicationContext(), R.string.server_created, Toast.LENGTH_SHORT).show();
                    executorservice.submit(() -> {
                        saveSettingNewSrv(phoneServer, typeActivity, name.getText().toString());

                        try {
                            SettingDrv settingDrv = getSettingDrv();
                            settingDrv.setSrvName(name.getText().toString());
                            updateSettingDrv(settingDrv);//сохранение одного поля

                        } catch (ExecutionException | InterruptedException e) {
                            e.printStackTrace();
                        }

                        Intent intent = new Intent(DriverSettingActivity.this, ServerActivity.class);
                        startActivity(intent);
                    });

                } else {
                    if (error.getCode() == -3) {
                        name.setError(getString(R.string.is_used_name));

                    }
                }
            }
        });
    }

    private void saveSettingNewSrv(String phoneServer, String typeActivity, String name) {
        SettingServer oldSettingServer = db.getSettingServerDAO().getSetting(currentUserUid);

        SettingServer setting = new SettingServer(0,
                currentUserUid,
                name,
                false,
                false,
                24,
                4,
                10,
                typeActivity,
                false,
                false,
                phoneServer);

        if (oldSettingServer == null) {
            db.getSettingServerDAO().addSettingServer(setting);
        } else {
            setting.setId(oldSettingServer.getId());
            db.getSettingServerDAO().updateSettingServer(setting);
        }

        ServerInformsAboutEventsClass.getInstance().updateSettingSrv(setting);


    }

    private void initDataForConnect(ServerModel server) {
        try {
            SettingDrv settingDrv = getSettingDrv();
            if (validateDataRequest(settingDrv)) {
                InfoRequestConnect request = new InfoRequestConnect(server.getKeySrv(), settingDrv.getName(), settingDrv.getNameHost(),
                        settingDrv.getPhone(), settingDrv.getOrderCriteria(), settingDrv.getDislocation(), ServerValue.TIMESTAMP, currentUserUid);

                if (server.getKeySrv().equals(currentUserUid)) {
                    request.setAuto(settingDrv.getAuto());//инициализируем авто, так как сервер получает эти данные (при запросе) через запрос по autoRef
                    request.setTs(System.currentTimeMillis());//инициализируем время создания, так как сервер получает эти данные (при обработке запросе) через ServerValue.TIMESTAMP

                    addYourselfDriverToYourServer(request);//создаем водителя(себя на своем сервере) напрямую без запроса
                } else {

                    sendRequest(request.getKeySrv(), request, server);//создаем водителя у сервера через запрос/подтверждение от сервера
                }
            } else {

                Toast.makeText(getApplicationContext(), R.string.incorrect_settings_data, Toast.LENGTH_LONG).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private void addYourselfDriverToYourServer(InfoRequestConnect request) {
//запускаем сервер и добавляем в базу водителей себя
        RunServicesServer services = RunServicesServer.getInstance(serversRef.child(currentUserUid), auth.getCurrentUser());
        executorservice.submit(() -> {
            services.saveRequestDataDriver(request);
//            services.sendResponse(currentUserUid);
        });

    }

    private void sendRequest(String keySrv, InfoRequestConnect request, ServerModel server) {

        serversRef.child(keySrv).child("request/accept").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                if (snapshot.exists()) {

                    Map<String, Object> map = new HashMap<>();
                    map.put("serverList/" + keySrv + "/request/accept", null);//отключение приема
                    map.put("serverList/" + keySrv + "/request/data", request);//отправляется запрос


                    database.getReference().updateChildren(map, new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error == null) {

                                server.setStatus(Util.SEND_REQUEST_PROVIDER_STATUS);//статус существует пока работает приложение(не сохраняем)
                                if (adapterAvailableServers != null) {
                                    adapterAvailableServers.notifyDataSetChanged();
                                }

                                responseListener(server);//ждем ответ от сервера в узле response

                                Toast.makeText(getApplicationContext(), R.string.request_sender, Toast.LENGTH_LONG).show();
                            } else {

                                Toast.makeText(getApplicationContext(), R.string.not_send_request, Toast.LENGTH_LONG).show();
                            }
                        }
                    });

                } else {
                    Toast.makeText(DriverSettingActivity.this, R.string.debug_server_not_accept_data, Toast.LENGTH_LONG).show();

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private boolean saveStatusServer(ServerModel server) {
        long id;
        DataAvailableServer srvDb = db.getAvailableServerDAO().getAvailableServer(server.getKeySrv(), currentUserUid);
        if (srvDb == null) {
            id = db.getAvailableServerDAO().save(new DataAvailableServer(server.getKeySrv(), server.getStatus(), currentUserUid));
        } else {
            srvDb.setStatus(server.getStatus());
            id = db.getAvailableServerDAO().update(srvDb);
        }
        return (id > 0);
    }

    private void responseListener(ServerModel server) {

        if (mapListeners.get(sharedDrvRef.child("response").child(server.getKeySrv())) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {

                        DataResponse response = snapshot.getValue(DataResponse.class);
                        if (response != null) {

                            ServerModel srv = serverModels.stream().filter(s -> s.getKeySrv().equals(server.getKeySrv())).findAny().orElse(null);
                            if (srv != null) {

                                srv.setStatus(response.getResponse());
                                if (adapterAvailableServers != null) {
                                    adapterAvailableServers.notifyDataSetChanged();
                                }

                                handlerResponse(srv, response.getResponse(), snapshot.getRef());

                            } else {
                                executorservice.submit(() -> {
                                    clearDb(server.getKeySrv());
                                });
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            sharedDrvRef.child("response").child(server.getKeySrv()).addValueEventListener(listener);
            mapListeners.put(sharedDrvRef.child("response").child(server.getKeySrv()), listener);
        }
    }

    private void handlerResponse(ServerModel srv, int response, DatabaseReference ref) {

        executorservice.submit(() -> {
            if (response == Util.CONNECTED_TO_SERVER_DRIVER_STATUS) {//конечный статус

                clearDb(srv.getKeySrv());
            } else {
                //ответ сохранен
                if (saveStatusServer(srv)) {
                    ref.removeValue();//чистим базу, если сохранить не удается, ссылка будет читаться и пытаться сохранить заново
                }
            }
        });

    }


    private void save() {
//loadSetting обновляется
        if (!validatePassword(binding.etPassword) |
                !validateHostName(binding.etNameHost) |
                !validateUserName(binding.etNameUser) |
                !validateModel() |
                !validateRegNumber() |
                !validateColor() |
                !validateDislocation() |
                !validatePhone() |
                !validateUseServer() |
                !validateRadius() |
                !validateServices(binding.msOrderCriteria)) {

            Toast.makeText(getApplicationContext(), R.string.break_save_change, Toast.LENGTH_LONG).show();
            return;
        } else {
            checkChangeDataRef();
        }
    }

    private boolean validateRadius() {
        String s = binding.etRadius.getText().toString();

        if (s.isEmpty() || s.equals("0")) {
            binding.etRadius.setError(getString(R.string.validate_radius));
            return false;
        }

        loadSetting.setRadius(Integer.parseInt(s));
        return true;
    }

    private boolean validatePhone() {
        String s = binding.etPhoneUser.getText().toString();

        loadSetting.setPhone(s);
        return true;
    }

    private boolean validateUseServer() {
        String str = binding.tvUseServers.getText().toString();

        loadSetting.setUsedProviders(serverModels.stream().filter(s -> s.getStatus() != Util.UNCONNECTED_PROVIDER_STATUS).collect(Collectors.toList()));
        loadSetting.setServer(str);
        return true;
    }

    private void checkChangeDataRef() {
        binding.flBlock.setVisibility(View.VISIBLE);
//auth
        if (!authDataOld.getName().equals(loadSetting.getNameHost()) || !authDataOld.getPassword().equals(loadSetting.getPassword())) {
            if (!authDataOld.getPassword().equals("")) {
                displayConfirm();//есть изменения требуем пароль для подтверждения -> saveAuthDataRef() ->также там сохраняется транспорт(если есть изменения);
            } else {//пароль сохраняется первый раз
                saveDataRef();
            }
//transport
        } else {
            if (!autoOld.getAutoModel().equals(loadSetting.getAuto().getAutoModel()) ||
                    !autoOld.getAutoNumber().equals(loadSetting.getAuto().getAutoNumber()) ||
                    !autoOld.getAutoColor().equals(loadSetting.getAuto().getAutoColor())) {

                saveDataRef();//будет сохранятся только транспорт
            } else {

                saveSettingDrv();
            }
        }
    }

    private void displayConfirm() {
        AlertDialog.Builder alert = new AlertDialog.Builder(this);
        alert.setIcon(R.drawable.ic_baseline_info_24);
        alert.setTitle(R.string.change_access_data);
        alert.setMessage("\n\n" + getString(R.string.t_password));
        final EditText input = new EditText(this);
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        alert.setView(input);

        alert.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                if (authDataOld.getPassword().equals(input.getText().toString())) {

                    saveDataRef();
                } else {
                    binding.flBlock.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), getString(R.string.invalid_password2), Toast.LENGTH_LONG).show();
                }
            }
        });


        alert.create().show();
    }

    private void saveDataRef() {
        Map<String, Object> map = new HashMap<>();
        //изменено имя хоста
        if (!authDataOld.getName().equals(loadSetting.getNameHost())) {
            if (!authDataOld.getName().isEmpty()) {
                map.put("/names/" + authDataOld.getName(), null);//удаляем старое имя
            }
            map.put("/names/" + loadSetting.getNameHost(), "");//сохраняем уникальное имя
            map.put("/auth/" + currentUserUid + "/name", loadSetting.getNameHost());
        }
        //изменен пароль
        if (!authDataOld.getPassword().equals(loadSetting.getPassword())) {
            map.put("/auth/" + currentUserUid + "/password", loadSetting.getPassword());
        }
        //изменены данные в описании транспорта
        if (!autoOld.getAutoModel().equals(loadSetting.getAuto().getAutoModel()) || !autoOld.getAutoNumber().equals(loadSetting.getAuto().getAutoNumber()) ||
                !autoOld.getAutoColor().equals(loadSetting.getAuto().getAutoColor())) {
            map.put("/transport/" + currentUserUid, loadSetting.getAuto());
        }

        database.getReference().updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {

                    saveSettingDrv();

                } else {

                    binding.etNameHost.setError(getString(R.string.is_used_name));
                    Toast.makeText(getApplicationContext(), R.string.is_used_name, Toast.LENGTH_LONG).show();
                    binding.flBlock.setVisibility(View.GONE);
                }
            }
        });
    }


    private boolean validateHostName(EditText editText) {
        String s = editText.getText().toString();

        if (s.isEmpty()) {
            editText.setError(getString(R.string.define_name_host));
            return false;
        } else if (s.equals("SHARED")) {
            editText.setError(getString(R.string.is_used_name));
            return false;
        } else if (serverModels.stream().anyMatch(srv -> srv.getName().equals(s) && !srv.getKeySrv().equals(currentUserUid))) {//уникальность имени
            editText.setError(getString(R.string.is_used_name));
            return false;
        }
        loadSetting.setNameHost(s);
        return true;
    }

    private boolean validateUserName(EditText editText) {
        String s = editText.getText().toString();

        if (s.isEmpty()) {
            editText.setError(getString(R.string.validate_input_name));
            return false;
        }

        loadSetting.setName(s);
        return true;
    }


    private void displayAllServers() {

        AlertDialog.Builder builder = new AlertDialog.Builder(DriverSettingActivity.this);

        LayoutInflater inflater = getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_available_list, null);
        builder.setIcon(R.drawable.ic_baseline_list_24);
        builder.setTitle(R.string.t_available_for_connection);
        builder.setView(dialogView);
        builder.setCancelable(false);

        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvAvailableList);
        rv.setHasFixedSize(true);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        rv.setLayoutManager(layoutManager);


        adapterAvailableServers = new AdapterAvailableSrvList(serverModels);//в этом списке можно зделать запрос подключиться/отключиться
        rv.setAdapter(adapterAvailableServers);
        executorservice.submit(this::initStatusFromDBAvailableSrv);//инициализируем сохраненые состояния

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                saveUsedProviders();
                adapterAvailableServers = null;
            }
        });

        adapterAvailableServers.setListener(new AdapterAvailableSrvList.SelectItemListener() {
            @Override
            public void onSelectItem(ServerModel model, int pos) {

            }

            @Override
            public void onMenuItemClick(MenuItem menuItem, ServerModel server, int position) {
                switch (menuItem.getItemId()) {

                    case R.id.avaiSendDataForConnect:

                        initDataForConnect(server);
                        break;
                    case R.id.avaiDelConnect:

                        handlerDeleteConnected(server.getKeySrv());
                        break;

                }
            }
        });

        dlgAvaiServers = builder.create();
        dlgAvaiServers.show();
    }


    private void handlerDeleteConnected(String keySrv) {
        Map<String, Object> map = new HashMap<>();
        map.put("/serverList/" + keySrv + "/driversList/" + currentUserUid, null);
        map.put("/serverList/" + keySrv + "/keysD/" + currentUserUid, null);
        map.put("/SHAREDSERVER/driversList/" + currentUserUid + "/response/" + keySrv, null);

        database.getReference().updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    executorservice.submit(() -> {

                        db.getAvailableServerDAO().delete( keySrv);
                    });
                    Toast.makeText(getApplicationContext(), R.string.delete_connected, Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.break_save_database, Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    private void initStatusFromDBAvailableSrv() {
        List<DataAvailableServer> dbServers = db.getAvailableServerDAO().getAvailableServers(currentUserUid);
        if (dbServers != null) {

            dbServers.forEach(dbSrv -> {
                serverModels.forEach(server -> {

                    if (dbSrv.getKeySrv().equals(server.getKeySrv())) {
                        server.setStatus(dbSrv.getStatus());//сохраненый статус(по умолчанию)

                        if (dbSrv.getStatus() != Util.RESPONSE_DENY) {

                            clearDb(server.getKeySrv());
                        } else {
                            responseListener(server);//определяем статус по ответу
                        }
                    }
                });
            });
        }
    }

    private void clearDb(String keySrv) {

        db.getAvailableServerDAO().delete( keySrv);//чистим базу db

        sharedDrvRef.child("response").child(keySrv).removeValue();//чистим базу database

    }


    private void saveUsedProviders() {

        try {
            SettingDrv settingDrv = getSettingDrv();
            settingDrv.setUsedProviders(serverModels.stream().filter(s -> s.getStatus() != Util.UNCONNECTED_PROVIDER_STATUS).collect(Collectors.toList()));
            updateSettingDrv(settingDrv);//сохранение одного поля

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }

    }

    private long updateSettingDrv(SettingDrv setting) throws ExecutionException, InterruptedException {

        Callable task = () -> {
            SettingDrv settingDrv = db.getSettingAppDAO().getSetting(currentUserUid);
            long id;
            if (settingDrv == null) {
                id = db.getSettingAppDAO().saveDataSetting(setting);
            } else {
                setting.setId(settingDrv.getId());
                id = db.getSettingAppDAO().updateDataSetting(setting);
            }
            return id;

        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (long) future.get();
    }

    private SettingDrv getSettingDrv() throws ExecutionException, InterruptedException {

        Callable task = () -> {
            SettingDrv settingDrv = db.getSettingAppDAO().getSetting(currentUserUid);
            if (settingDrv == null) {
                settingDrv = new SettingDrv(
                        currentUserUid,
                        serverModels.stream().filter(s -> s.getStatus() != Util.UNCONNECTED_PROVIDER_STATUS).collect(Collectors.toList()),
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

                long id = db.getSettingAppDAO().saveDataSetting(settingDrv);
                if (id > 0) {
                    settingDrv.setId(id);
                }
            }

            return settingDrv;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (SettingDrv) future.get();
    }

    private void loadSetting() {
        try {
            loadSetting = getSettingDrv();
            binding.chkOffSound.setChecked(loadSetting.isSoundNotification());
            binding.chkVoicing.setChecked(loadSetting.isVoicingAmount());
            binding.chkAccessToSOS.setChecked(loadSetting.isAccessPassSOS());
            binding.chkRunSrv.setChecked(loadSetting.isRunServer());
            binding.msOrderCriteria.setTextSelectedItems(loadSetting.getOrderCriteria());
            dislocation = loadSetting.getDislocation();
            binding.tvDislocation.setText(Util.getAddress(dislocation.getLatitude(), dislocation.getLongitude(), Util.TYPE_ADDRESS_SHORT));
            binding.etNameUser.setText(loadSetting.getName());
            binding.etPhoneUser.setText(loadSetting.getPhone());
            binding.etRadius.setText(String.valueOf(loadSetting.getRadius()));

            getTransportRef();
            getDataAuthAccessToHostDrv();

        } catch (Exception e) {
            e.printStackTrace();

            Toast.makeText(getApplicationContext(), R.string.break_load_setting, Toast.LENGTH_LONG).show();
        }

    }

    private void getTransportRef() {
        transportRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataAuto auto = snapshot.getValue(DataAuto.class);
                    if (auto != null) {
                        binding.etModel.setText(auto.getAutoModel());
                        binding.etRegNumber.setText(auto.getAutoNumber());
                        binding.etColorAuto.setText(auto.getAutoColor());

                        loadSetting.getAuto().setAutoModel(auto.getAutoModel());
                        loadSetting.getAuto().setAutoColor(auto.getAutoColor());
                        loadSetting.getAuto().setAutoNumber(auto.getAutoNumber());

                        autoOld = auto;//сохраняем состояние в базе
                    }
                } else {
                    binding.etModel.setText("");
                    binding.etRegNumber.setText("");
                    binding.etColorAuto.setText("");

                    loadSetting.setAuto(new DataAuto());
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private boolean validateDataRequest(SettingDrv setting) {
        try {
            return (!setting.getName().isEmpty() && setting.getName().length() > 1) &&
                    !setting.getPhone().isEmpty() &&
                    setting.getAuto().getAutoModel().length() > 1 &&
                    setting.getAuto().getAutoColor().length() > 1 &&
                    setting.getAuto().getAutoNumber().length() > 1 &&
                    (!setting.getOrderCriteria().isEmpty() && setting.getOrderCriteria().length() > 1) &&
                    setting.getDislocation().getLatitude() != 0 &&
                    setting.getDislocation().getLongitude() != 0;
        } catch (Exception ignored) {
            return false;
        }
    }


    private boolean validateDislocation() {
        String input =  binding.tvDislocation.getText().toString().trim();

        if (input.isEmpty() || dislocation.getLatitude() == 0 || dislocation.getLongitude() == 0) {
            binding.tvDislocation.setError(getString(R.string.not_dislocation));

            return (false);
        }

        loadSetting.setDislocation(dislocation);
        return true;
    }

    private boolean validateModel() {
        String input =  binding.etModel.getText().toString().trim();
        if (input.length() < 2) {
            binding.etModel.setError(getString(R.string.not_model));
            return (false);
        }

        loadSetting.getAuto().setAutoModel(input);
        return true;
    }

    private boolean validateColor() {
        String input =  binding.etColorAuto.getText().toString().trim();
        if (input.length() < 2) {
            binding.etColorAuto.setError(getString(R.string.not_color));
            return (false);
        }

        loadSetting.getAuto().setAutoColor(input);
        return true;
    }

    private boolean validateRegNumber() {
        String input =  binding.etRegNumber.getText().toString().trim();
        if (input.length() == 0) {
            binding.etRegNumber.setError(getString(R.string.not_number));
            return (false);
        }

        loadSetting.getAuto().setAutoNumber(input);
        return true;
    }

    private void saveSettingDrv() {

        try {
            long id = updateSettingDrv(loadSetting);

            if (id > 0) {
                Toast.makeText(getApplicationContext(), R.string.all_change_save, Toast.LENGTH_LONG).show();
                setResult(Util.RESULT_OK, new Intent());
                finish();
            } else {

                Toast.makeText(getApplicationContext(), R.string.not_save, Toast.LENGTH_LONG).show();
                binding.flBlock.setVisibility(View.GONE);
            }

        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onDestroy() {
        try {
            executorservice.shutdown();

            Util.removeAllChildListener(mapChildListeners);
            Util.removeAllValueListener(mapListeners);

        } catch (Exception e) {

        } finally {
            super.onDestroy();
        }

    }


    private boolean validatePassword(EditText editText) {
        String s = editText.getText().toString();
        if (s.isEmpty()) {
            editText.setError(getString(R.string.validate_password_to_host));
            return false;
        } else if (editText.length() < 6) {
            editText.setError(getString(R.string.validate_password_char));
            return false;
        }
        loadSetting.setPassword(s);
        return true;
    }


    private void barrier() {
        if (bTask3) {//bTask2 &&bTask1 &&

            binding.btnSaveSetting.setEnabled(true);
        } else {

            binding.btnSaveSetting.setEnabled(false);
        }
    }

    private void getDataAuthAccessToHostDrv() {

        authRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    AuthDataToHostModel access = snapshot.getValue(AuthDataToHostModel.class);
                    if (access != null) {

                        runOnUiThread(() -> {
                            binding.etPassword.setText(access.getPassword());
                            binding.etNameHost.setText(access.getName());

                            loadSetting.setPassword(access.getPassword());
                            loadSetting.setNameHost(access.getName());
                        });
                        authDataOld = access;//сохраняем состояние в базе
                    }
                } else {
                    loadSetting.setPassword("");
                    loadSetting.setNameHost("");
                }

                bTask3 = true;
                barrier();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {

            case Util.GET_LOCATION_FROM:
                if (resultCode == Util.RESULT_OK) {
                    if (data != null) {
                        dislocation = data.getParcelableExtra("location");

                        String address = Util.getAddress(dislocation.getLatitude(), dislocation.getLongitude(), Util.TYPE_ADDRESS_SHORT);

                        binding.tvDislocation.setText(address);
                        binding.tvDislocation.setError(null);

                    }
                }
                break;
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
//        printUsedServer();

    }

    private void printUsedServer() {
        binding.tvUseServers.setText(serverModels.stream().filter(s -> s.getStatus() != Util.UNCONNECTED_PROVIDER_STATUS &&
                        s.getStatus() != Util.NOT_DEFINED_PROVIDER_STATUS && s.getStatus() != Util.RESPONSE_DENY)
                .collect(Collectors.toList()).toString().replaceAll("[\\[\\]]", ""));
    }

}