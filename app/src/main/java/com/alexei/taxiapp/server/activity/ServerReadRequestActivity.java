package com.alexei.taxiapp.server.activity;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.SelectPointInMapActivity;
import com.alexei.taxiapp.databinding.ActivityServerReadRequestBinding;
import com.alexei.taxiapp.db.InfoRequestConnect;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.snackbar.Snackbar;

public class ServerReadRequestActivity extends AppCompatActivity {
    private InfoRequestConnect requestModel;
    private ActivityServerReadRequestBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(getResources().getConfiguration().orientation);
        binding = ActivityServerReadRequestBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_server_read_request);

        binding.ibSelDislocRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showDotDisLocation();
            }
        });

        binding.ibCallPhoneReqRead.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                requestCallPhonePermission();
            }
        });

        binding.btnAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("data_request", requestModel);
                setResult(Util.RESULT_OK, intent);
                finish();
            }
        });

        binding.btnNotAccept.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent();
                intent.putExtra("key", requestModel.getKeyS());
                setResult(Util.RESULT_CANCEL, intent);
                finish();
            }
        });


        Intent intent = getIntent();
        if (intent != null) {
            requestModel = (InfoRequestConnect) intent.getParcelableExtra("data_request");
            if (requestModel != null) {

                loadData();

            }
        }
    }


    private void showDotDisLocation() {
        Intent intent = new Intent(ServerReadRequestActivity.this, SelectPointInMapActivity.class);
        intent.putExtra("location", requestModel.getDisl());
        startActivity(intent);
    }

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

                    ActivityCompat.requestPermissions(ServerReadRequestActivity.this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
                }
            });
        } else {
//            callPhone();
            //запрос на разрешение без объяснения  -интерфейс на получение разрешения
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CALL_PHONE}, Util.REQUEST_CALL_PHONE_PERMISSION);
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
        intent.setData(Uri.parse("tel:" + binding.tvPhoneReqRead.getText().toString()));
        startActivity(intent);
    }

    private void loadData() {
        binding.tvNameReqRead.setText(requestModel.getName());
        binding.tvPhoneReqRead.setText(requestModel.getPhone());

        binding.tvModelRead.setText(requestModel.getAuto().getAutoModel());
        binding.tvColorRead.setText(requestModel.getAuto().getAutoColor());
        binding.tvRegNumRead.setText(requestModel.getAuto().getAutoNumber());

        binding.tvTypeTrRead.setText(requestModel.getTypeTr());
        binding.tvDislocationRead.setText(Util.getAddress(
                requestModel.getDisl().getLatitude(),
                requestModel.getDisl().getLongitude(),
                Util.TYPE_ADDRESS_SHORT));

    }
}