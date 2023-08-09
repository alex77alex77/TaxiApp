package com.alexei.taxiapp.server.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.room.Room;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.SelectPointInMapActivity;
import com.alexei.taxiapp.databinding.ActivityEditDriverBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class EditDriverActivity extends AppCompatActivity {
    private ExecutorService executorservice;// = Executors.newSingleThreadExecutor();

    private AppDatabase db;
    private FirebaseAuth auth;
    private String currentUserId;
    private InfoDriverReg infoDriver;
    private long longCallSign;
    private long idDrv;
    private DataLocation loc;
    private ActivityEditDriverBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditDriverBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_edit_driver);

        setRequestedOrientation(getResources().getConfiguration().orientation);

        loc = new DataLocation();
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы
        executorservice = Executors.newSingleThreadExecutor();
        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserId = auth.getCurrentUser().getUid();

            Intent intent = getIntent();
            if (intent != null) {
                idDrv = intent.getLongExtra("id", 0);
            } else {
                finish();
            }

            binding.ibSelDislocReg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent intent = new Intent(EditDriverActivity.this, SelectPointInMapActivity.class);
                    intent.putExtra("location", loc);

                    startActivityForResult(intent, Util.GET_LOCATION_FROM);
                }
            });

            binding.buttonSaveReg.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    edit();
                }
            });

            executorservice.submit(this::loadDataDriver);
        } else {
            finish();
        }
    }

    private void loadDataDriver() {

        infoDriver = db.getDataDriversServerDAO().getDriverInfo(idDrv, currentUserId);

        if (infoDriver != null) {

            runOnUiThread(() -> {
                binding.tvTimeCreate.setText(R.string.date_create_);
                binding.tvTimeCreate.append(Util.formatTimeDate2.format(infoDriver.getTimeCreate()));

                binding.etDeposit.setText(String.valueOf(infoDriver.getBalance()));

                binding.etAdditionData.setText(infoDriver.getAdditionData());
                binding.etNameReg.setText(infoDriver.getName());

                binding.etModelReg.setText(infoDriver.getAuto().getAutoModel());
                binding.etColorReg.setText(infoDriver.getAuto().getAutoColor());
                binding.etNumberReg.setText(infoDriver.getAuto().getAutoNumber());

                binding.mSpinnerTypeTrans.setTextSelectedItems(infoDriver.getAutoType());
                binding.etPhoneReg.setText(infoDriver.getPhone());
                binding.etPriorityReg.setText(String.valueOf(infoDriver.getPriority()));
                longCallSign = infoDriver.getCallSign();
                binding.tvCallSignReg.setText(String.valueOf(longCallSign));
                loc = infoDriver.getDislocation();
                binding.tvDislocationRead.setText(Util.getAddress(loc.getLatitude(), loc.getLongitude(), Util.TYPE_ADDRESS_SHORT));
            });
        } else {
            finish();
        }
    }

    private void edit() {
        if (!validateColor() | !validateRegNumber() | !validateModel() | !validateDeposit() | !validateName() | !validateType() | !validateDislocation()) {//
            return;
        }

        editDriver();
    }


    private void editDriver() {

        executorservice.submit(() -> {

            infoDriver.setBalance(Float.parseFloat(binding.etDeposit.getText().toString()));
            infoDriver.setAdditionData(binding.etAdditionData.getText().toString());
            infoDriver.setName(binding.etNameReg.getText().toString());
            infoDriver.setPhone(binding.etPhoneReg.getText().toString());
            infoDriver.setPriority(Integer.parseInt(binding.etPriorityReg.getText().toString()));

            infoDriver.getAuto().setAutoModel(binding.etModelReg.getText().toString());
            infoDriver.getAuto().setAutoColor(binding.etColorReg.getText().toString());
            infoDriver.getAuto().setAutoNumber(binding.etNumberReg.getText().toString());

            infoDriver.setDislocation(loc);
            infoDriver.setAutoType(binding.mSpinnerTypeTrans.getTextSelectedItems());

            int id = db.getDataDriversServerDAO().updateDataDriverServer(infoDriver);
            if (id > 0) {
                close(infoDriver.getId(), longCallSign, Util.EDIT_OK_RESULT);
            } else {
                close(infoDriver.getId(), longCallSign, Util.EDIT_BREAK_RESULT);
            }
        });
    }


    private void close(long id, long callSign, int result) {

        Intent intent = new Intent();
        intent.putExtra("id", id);
        intent.putExtra("callSign", callSign);

        setResult(result, intent);
        finish();
    }


    private boolean validateName() {
        String input = binding.etNameReg.getText().toString().trim();
        if (input.isEmpty()) {
            binding.etNameReg.setError(getString(R.string.validate_input_name));
            return (false);
        }

        return true;
    }

    private boolean validateModel() {
        String input = binding.etModelReg.getText().toString().trim();
        if (input.isEmpty()) {
            binding.etModelReg.setError(getString(R.string.validate_model));
            return (false);
        }

        return true;
    }

    private boolean validateColor() {
        String input = binding.etColorReg.getText().toString().trim();
        if (input.isEmpty()) {
            binding.etColorReg.setError(getString(R.string.validate_color));
            return (false);
        }

        return true;
    }

    private boolean validateDeposit() {
        String input = binding.etDeposit.getText().toString().trim();
        if (input.isEmpty()) {
            binding.etDeposit.setError(getString(R.string.validate_deposit_amount));
            return (false);
        }

        return true;
    }

    private boolean validateRegNumber() {
        String input = binding.etNumberReg.getText().toString().trim();
        if (input.isEmpty()) {
            binding.etNumberReg.setError(getString(R.string.validate_reg_num));
            return (false);
        }

        return true;
    }


    private boolean validateType() {
        String input = binding.mSpinnerTypeTrans.getTextSelectedItems();

        if (input.isEmpty()) {
            View view = binding.mSpinnerTypeTrans.getSelectedView();
            binding.mSpinnerTypeTrans.setError(view, getString(R.string.validate_criteria));
            return (false);
        }
        return true;
    }

    private boolean validateDislocation() {

        if (loc.getLatitude() == 0 || loc.getLongitude() == 0) {
            binding.tvDislocationRead.setError(getString(R.string.validate_dislocation));
            return (false);
        }

        return true;
    }


    @Override
    protected void onDestroy() {
        executorservice.shutdown();
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {

        super.onBackPressed();

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable @org.jetbrains.annotations.Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Util.GET_LOCATION_FROM) {
            if (resultCode == Util.RESULT_OK) {
                if (data != null) {
                    loc = data.getParcelableExtra("location");

                    String address = Util.getAddress(loc.getLatitude(), loc.getLongitude(), Util.TYPE_ADDRESS_SHORT);

                    binding.tvDislocationRead.setText(address);
                    binding.tvDislocationRead.setError(null);
                }
            }
        }
    }


}