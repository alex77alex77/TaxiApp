package com.alexei.taxiapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.alexei.taxiapp.client.activity.ClientMapActivity;
import com.alexei.taxiapp.databinding.ActivityChooseModeBinding;
import com.alexei.taxiapp.driver.activity.DriverMapsActivity;
import com.alexei.taxiapp.driver.exClass.LoginInAppClass;
import com.alexei.taxiapp.util.Util;
import com.google.android.material.snackbar.Snackbar;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;

public class ChooseModeActivity extends AppCompatActivity {
    private FirebaseDatabase database;
    private FirebaseUser user;
    private ArrayList<LoginInAppClass> loginList = new ArrayList<>();
    private ActivityChooseModeBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChooseModeBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных
        user = FirebaseAuth.getInstance().getCurrentUser();

        binding.passengerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.flBlock.setVisibility(View.VISIBLE);
                chooseMode(Util.PASSENGER_MODE);
            }
        });

        binding.driverButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.flBlock.setVisibility(View.VISIBLE);
                chooseMode(Util.DRIVER_MODE);

            }
        });
    }


    private void handlerAuth(int mode) {
        if (user == null) {
            binding.flBlock.setVisibility(View.GONE);

            Intent intent = new Intent(ChooseModeActivity.this, SignInActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            intent.putExtra("mode", mode);
            startActivity(intent);
        } else {
            runLoginAppClass(mode);//вход в программу
        }
    }

    private void runLoginAppClass(int mode) {

        LoginInAppClass login = new LoginInAppClass(database.getReference().getRef().child("SHAREDSERVER/driversList").child(user.getUid()).child("loginK"), mode, user);

        login.setOnListeners(new LoginInAppClass.OnListeners() {
            @Override
            public void onSuccessful(boolean success, int mode, String res) {
                if (success) {
                    switchIntent(mode);
                } else {
                    runOnUiThread(() -> {

                        binding.flBlock.setVisibility(View.GONE);

                        Intent intent = new Intent(ChooseModeActivity.this, SignInActivity.class);
                        intent.putExtra("isLogin", false);
                        intent.putExtra("mode", mode);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                    });
                }
                login.recoveryResources();
                loginList.remove(login);

            }

            @Override
            public void onError(int mode, String err) {
                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
                    binding.flBlock.setVisibility(View.GONE);
                });
                login.recoveryResources();
                loginList.remove(login);
            }
        });
        loginList.add(login);
    }


    private void chooseMode(int mode) {

        switch (mode) {
            case Util.DRIVER_MODE:
            case Util.PASSENGER_MODE:
                handlerAuth(mode);
                break;

        }
    }


    private void switchIntent(int mode) {
        Intent intent;
        switch (mode) {
            case Util.DRIVER_MODE:
                intent = new Intent(ChooseModeActivity.this, DriverMapsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                break;
            case Util.PASSENGER_MODE:

                intent = new Intent(ChooseModeActivity.this, ClientMapActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);

                break;

        }
    }

    @Override
    protected void onStart() {
//        mCurrentLocale = getResources().getConfiguration().locale;

        binding.flBlock.setVisibility(View.GONE);

        if (!checkLocationPermission()) {
            requestLocationPermission();
        } else {
            binding.passengerButton.setEnabled(true);
            binding.driverButton.setEnabled(true);

        }
        super.onStart();
    }

    //--------------------------проверка есть ли разрешения
    private void requestLocationPermission() {
        // ----ActivityCompat   Помощник для доступа к функциям в Activity.
        //--ActivityCompat.shouldShowRequestPermissionRationale - Передается название разрешения, а он вам в виде boolean ответит, надо ли показывать объяснение для пользователя.
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);

        if (shouldProvideRationale) {//------------запрос на разрешение с объяснением

            showSnackBar(getString(R.string.debug_permission_location), getString(R.string.ok), new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    //запрос на разрешение  (обработчик-onRequestPermissionsResult по ключу REQUEST_LOCATION_PERMISSION)
                    //*Этот интерфейс на получение результатов для запросов разрешений. ActivityCompat.requestPermissions

                    ActivityCompat.requestPermissions(ChooseModeActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Util.REQUEST_LOCATION_PERMISSION);
                }
            });
        } else {
            //запрос на разрешение без объяснения  -интерфейс на получение разрешения
            ActivityCompat.requestPermissions(ChooseModeActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, Util.REQUEST_LOCATION_PERMISSION);
        }
    }

    //---------------*обработчик результата ActivityCompat.requestPermissions по ключу REQUEST_LOCATION_PERMISSION
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        boolean shouldProvideRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == Util.REQUEST_LOCATION_PERMISSION) {

            if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {   //-------------если разрешил

                binding.passengerButton.setEnabled(true);
                binding.driverButton.setEnabled(true);
//                buttonServer.setEnabled(true);
            } else if (!shouldProvideRationale) {//---объяснения не нужно нажата "больше не беспокоить"

                showSnackBar(getString(R.string.go_over_setting), getString(R.string.settings), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        //----------------------переход в настройки телефона
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);//-------------действие станет началом новой задачи

                        startActivity(intent);
                    }
                });

            } else { //-----объяснения нужны, в окне настоек нажата "deny"
                //зацикливание (окно с объяснениями)
                requestLocationPermission();
            }
        }
    }

    private void showSnackBar(final String mainText, final String action, View.OnClickListener listener) {
        //Snackbar содержит действие, которое устанавливается через- setAction(action,listener)

        Snackbar.make(findViewById(android.R.id.content), mainText, Snackbar.LENGTH_INDEFINITE).setAction(action, listener).show();
    }

    private boolean checkLocationPermission() {
        int permissionState = ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION);// ---есть ли разрешение -<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />

        return (permissionState == PackageManager.PERMISSION_GRANTED);//---------Если пакет имеет разрешение, возвращается PERMISSION_GRANTED
    }
}