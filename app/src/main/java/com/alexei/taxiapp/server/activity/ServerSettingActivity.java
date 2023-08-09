package com.alexei.taxiapp.server.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityServerSettingBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.SettingServer;
import com.alexei.taxiapp.driver.exClass.ServerInformsAboutEventsClass;
import com.alexei.taxiapp.driver.model.InfoServerModel;
import com.alexei.taxiapp.server.exClass.ReportWriteClass;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class ServerSettingActivity extends AppCompatActivity {
    private ExecutorService executorservice;
    private AppDatabase db;

    private FirebaseAuth auth;
    private FirebaseUser currentUser;
    private FirebaseDatabase database;
    private DatabaseReference srvRef;

    private ActivityServerSettingBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityServerSettingBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_server_setting);
        setRequestedOrientation(getResources().getConfiguration().orientation);
        executorservice = Executors.newFixedThreadPool(2);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы

        auth = FirebaseAuth.getInstance();
        currentUser = auth.getCurrentUser();
        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных

        if (currentUser != null) {

            srvRef = database.getReference().child("serverList").child(currentUser.getUid());
            binding.btnClearReport.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    deleteReport();
                }
            });

            binding.dtnSettingCancel.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    finish();
                }
            });

            binding.btnRates.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    updateRates();
                }
            });

            binding.dtnSave.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    cmdSave();
                }
            });

            executorservice.submit(this::loadSetting);

        } else {
            finish();
        }
    }

    ActivityResultLauncher<Intent> launchSomeActivity = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Util.RESULT_OK) {
                        executorservice.submit(() -> {
                            updateCountRates();
                        });
                    }
                }
            });

    public void updateRates() {
        Intent intent = new Intent(this, RatesActivity.class);
        launchSomeActivity.launch(intent);
    }


    private void deleteReport() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.title_app);
        builder.setIcon(R.drawable.ic_baseline_help_24);
        builder.setMessage(R.string.debug_del_records);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                try {
                    displayCountRowReport(getResDeleteReport());
                } catch (ExecutionException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });

        AlertDialog dialog = builder.show();
        TextView messageText = (TextView) dialog.findViewById(android.R.id.message);
        messageText.setGravity(Gravity.CENTER);
        dialog.show();
    }

    private long getResDeleteReport() throws ExecutionException, InterruptedException {

        Callable task = () -> {
            db.getReportServerDAO().clearReport(currentUser.getUid());
            ReportWriteClass.reports.clear();
            return db.getReportServerDAO().getSize(currentUser.getUid());
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (long) future.get();
    }

    private boolean saveSetting() {
        SettingServer oldSettingServer = db.getSettingServerDAO().getSetting(currentUser.getUid());

        SettingServer setting = new SettingServer(Float.parseFloat(
                binding.etRateShift.getText().toString()),
                currentUser.getUid(),
                binding.tvNameSrv.getText().toString(),
                binding.chkAutoConnect.isChecked(),
                binding.chkDisableRequest.isChecked(),
                Integer.parseInt(binding.etShiftHoursCount.getText().toString()),
                Integer.parseInt(binding.etWaitAccumulation.getText().toString()),
                Integer.parseInt(binding.etWaitAcceptOrder.getText().toString()),
                binding.msTypesServices.getTextSelectedItems(),
                binding.chkAvailableForClient.isChecked(),
                binding.chkAutoAcceptOrder.isChecked(),
                binding.etPhoneSrvSetting.getText().toString());

        if (oldSettingServer == null) {
            db.getSettingServerDAO().addSettingServer(setting);
        } else {
            setting.setId(oldSettingServer.getId());
            db.getSettingServerDAO().updateSettingServer(setting);
        }

        ServerInformsAboutEventsClass.getInstance().updateSettingSrv(setting);
        return true;
    }

    private void cmdSave() {

        if (!validateHours() | !validateRateShift() | !validateWaitAccumulation() | !isValidateAvailableForClient() | !validateWaitResponseDrv()) {
            Toast.makeText(this, R.string.debug_break_save_setting, Toast.LENGTH_SHORT).show();
        } else {

            executorservice.submit(() -> updateDataServerRef());
        }
    }


    private boolean validateWaitAccumulation() {
        String f = binding.etWaitAccumulation.getText().toString().trim();
        if (f.isEmpty()) {
            binding.etWaitAccumulation.setError(getString(R.string.validate_period_assigned_drivers_to_order));
            return false;
        }

        return true;
    }

    private boolean validateWaitResponseDrv() {
        String f = binding.etWaitAcceptOrder.getText().toString().trim();
        if (f.isEmpty()) {
            binding.etWaitAcceptOrder.setError(getString(R.string.validate_period_accept_order));
            return false;
        } else {
            if (Integer.parseInt(f) > 15) {
                binding.etWaitAcceptOrder.setError(getString(R.string.debug_period_accept));
                Toast.makeText(this, R.string.debug_period_accept, Toast.LENGTH_LONG).show();
            }
        }

        return true;
    }

    private void updateDataServerRef() {
        Map<String, Object> map = new HashMap<>();

        if (binding.chkAvailableForClient.isChecked()) {

            map.put("/access/keySender", "");//подготовка
            map.put("/access/push", "");
            map.put("/access/timer", 0);

        } else {

            map.put("/access/keySender", null);//удаляем хост(для клиента не доступен)
            map.put("/access/push", null);
            map.put("/access/timer", null);
        }

        map.put("/info/services", binding.msTypesServices.getTextSelectedItems());
        map.put("/info/phone", binding.etPhoneSrvSetting.getText().toString());

        srvRef.updateChildren(map, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {

                    nextSaveData();
                } else {
                    Toast.makeText(getApplicationContext(), R.string.break_save_database, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void nextSaveData() {
        executorservice.submit(() -> {
            try {
                if (saveSetting()) {
                    Intent intent = new Intent();
                    setResult(Util.RESULT_OK, intent);
                    finish();
                } else {
                    runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.break_save_setting, Toast.LENGTH_SHORT).show());
                }
            } catch (Exception e) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), R.string.break_save_setting, Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void loadSetting() {

        SettingServer setting = db.getSettingServerDAO().getSetting(currentUser.getUid());

        if (setting != null) {
            runOnUiThread(() -> {

                binding.chkAutoConnect.setChecked(setting.isChkConnect());
                binding.chkDisableRequest.setChecked(setting.isChkDisableReq());
                binding.etShiftHoursCount.setText("" + setting.getHoursCount());
                binding.etRateShift.setText("" + setting.getRateShift());

                binding.etWaitAccumulation.setText("" + setting.getWaitAccumulation());
                binding.etWaitAcceptOrder.setText("" + setting.getWaitAccept());
            });
        }
        getInfoServerRef();

        updateCountRates();

        displayCountRowReport(getCountRowsReport());

        checkAccessRef();
    }

    private void getInfoServerRef() {
        srvRef.child("info").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    InfoServerModel servicesModel = snapshot.getValue(InfoServerModel.class);
                    if (servicesModel != null) {
                        binding.msTypesServices.setTextSelectedItems(servicesModel.getServices());
                        binding.tvNameSrv.setText(servicesModel.getName());
                        binding.etPhoneSrvSetting.setText(servicesModel.getPhone());
                    }
                } else {
                    binding.tvNameSrv.setText("-");

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkAccessRef() {
        srvRef.child("access").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    binding.chkAvailableForClient.setChecked(true);
                } else {
                    binding.chkAvailableForClient.setChecked(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void displayCountRowReport(long count) {
        runOnUiThread(() -> {
            binding.tvSizeReport.setText(getString(R.string.all_records_) + count);
        });
    }

    private void updateCountRates() {
        long size = db.getRatesDAO().getCount(currentUser.getUid());
        binding.tvCountRates.setText(getString(R.string.all_));
        binding.tvCountRates.append("" + size);
    }

    private long getCountRowsReport() {
        return db.getReportServerDAO().getSize(currentUser.getUid());
    }


    private boolean validateHours() {

        String input = binding.etShiftHoursCount.getText().toString().trim();
        if (input.isEmpty()) {
            binding.etShiftHoursCount.setError(getString(R.string.validate_period_shift));
            return false;
        } else if (Integer.parseInt(binding.etShiftHoursCount.getText().toString()) <= 0) {
            binding.etShiftHoursCount.setError(getString(R.string.validate_count_hour));
            return false;
        }

        return true;

    }


    private boolean validateRateShift() {
        String f = binding.etRateShift.getText().toString().trim();
        if (f.isEmpty()) {
            binding.etRateShift.setError(getString(R.string.set_bid));
            return false;
        }

        return true;
    }

    private boolean isValidateAvailableForClient() {
        if (binding.chkAvailableForClient.isChecked()) {
            if (binding.msTypesServices.getTextSelectedItems().isEmpty() || binding.msTypesServices.getTextSelectedItems().equals("-")) {

                View view = binding.msTypesServices.getSelectedView();
                binding.msTypesServices.setError(view, getString(R.string.debug_visibility_server));
                return false;
            }
            return true;
        }
        return true;
    }

}
