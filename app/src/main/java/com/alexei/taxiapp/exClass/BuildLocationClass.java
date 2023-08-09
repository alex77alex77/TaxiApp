package com.alexei.taxiapp.exClass;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.IntentSender;
import android.content.pm.PackageManager;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.util.Util;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsResponse;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.database.DatabaseReference;

public class BuildLocationClass {

    private static BuildLocationClass instance;

    private DatabaseReference refLoc;
    private Activity activity;

    private FusedLocationProviderClient fusedLocationClient;//   класс для определения местоположения
    private SettingsClient settingsClient;//настройки клиента
    private LocationRequest locationRequest;//сохраненние данных запроса -FusedLocationProviderClient Api
    private LocationSettingsRequest locationSettingsRequest; //определение настроек пользователя на данный момент
    private LocationCallback locationCallback;//определение событий в определении местоположения

    public static Location currentLocation;


    private DataLocation dataLocation = new DataLocation();
    private boolean isLocationUpdatesActive = true;//системная

    private int inUse = 0;

    private LocationManager locationManager ;

    public OnUpdateLocationListener onUpdateLocationListener;


    public interface OnUpdateLocationListener {
        void onUpdateLocation(Location location, int satellites);
    }

    public void setOnUpdateListener(OnUpdateLocationListener listener) {
        this.onUpdateLocationListener = listener;
    }


    public static synchronized BuildLocationClass getInstance(Activity activity, DatabaseReference refLoc) {
        if (instance == null) {
            instance = new BuildLocationClass(activity, refLoc);
        }
        return instance;
    }

    public BuildLocationClass(Activity activity, DatabaseReference refLoc) {
        this.refLoc = refLoc;
        this.activity = activity;

        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity);
        this.settingsClient = LocationServices.getSettingsClient(activity);

        this.buildLocationRequest();
        this.buildLocationCallBack();
        this.buildLocationSettingsRequest();

        startLocationUpdate();

        getSatellite(activity);
    }

    private void getSatellite(Activity activity) {
        locationManager = (LocationManager) activity.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

            return;
        }
        locationManager.registerGnssStatusCallback(new GnssStatus.Callback() {
            @Override
            public void onStarted() {
                super.onStarted();

            }

            @Override
            public void onStopped() {
                super.onStopped();

            }

            @Override
            public void onFirstFix(int ttffMillis) {
                super.onFirstFix(ttffMillis);

            }

            @Override
            public void onSatelliteStatusChanged(@NonNull GnssStatus status) {
                super.onSatelliteStatusChanged(status);

                inUse = 0;
                for (int i = 0; i < status.getSatelliteCount(); i++) {
                    if (status.usedInFix(i)) {
                        inUse++;
                    }
                }
            }
        });
    }

    private void buildLocationRequest() {
        locationRequest = new LocationRequest();
        locationRequest.setInterval(1000);
        locationRequest.setFastestInterval(1000);
        locationRequest.setSmallestDisplacement(5);//5meter
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);//высокая точность
    }

    private void buildLocationCallBack() {
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                super.onLocationResult(locationResult);


                currentLocation = locationResult.getLastLocation();//---Возвращает самое новые значения последнее доступное местоположение в этом результате или null, если местоположения недоступны.

                updateLocationRef();

                onUpdateLocationListener.onUpdateLocation(currentLocation, inUse);

            }
        };
    }


    private void updateLocationRef() {

        if (currentLocation != null) {
            dataLocation.setLatitude((double) Math.round(currentLocation.getLatitude() * 100000d) / 100000d);
            dataLocation.setLongitude((double) Math.round(currentLocation.getLongitude() * 100000d) / 100000d);

            if (refLoc != null) {
                refLoc.setValue(dataLocation);
            }
        }
    }

    private void buildLocationSettingsRequest() {
        LocationSettingsRequest.Builder builder = new LocationSettingsRequest.Builder();
        builder.addLocationRequest(locationRequest);//-----------------Добавляет коллекцию, LocationRequests которая созданна в методе buildLocationRequest().
        locationSettingsRequest = builder.build();//-------------------Создает LocationSettingsRequest, который можно использовать с SettingsApi.

    }

    public void stopLocationUpdate() {
        if (isLocationUpdatesActive) {
            fusedLocationClient.removeLocationUpdates(locationCallback).addOnCompleteListener(activity, new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    isLocationUpdatesActive = false;

                }
            });
        }
    }

    public void startLocationUpdate() {
        isLocationUpdatesActive = true;

        //проверка установлены ли соответствующие настройки
        settingsClient.checkLocationSettings(locationSettingsRequest).addOnSuccessListener(activity, new OnSuccessListener<LocationSettingsResponse>() {
            @Override
            public void onSuccess(LocationSettingsResponse locationSettingsResponse) {

                //если отсутствуют какие-то разрешения
                if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                        ActivityCompat.checkSelfPermission(activity, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {

                    return;
                }
                //запрос местоположения в отдельном потоке ,результат  методе onLocationResult в созданном объекте callback: locationCallback = new LocationCallback() {
                fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());//лупер текущего потока работающее непрерывно(c настройками locationRequest) (задача в отдельном потоке)


            }
        }).addOnFailureListener(activity, new OnFailureListener() {// ----------произошла неудача настройки
            @Override
            public void onFailure(@NonNull Exception e) {
                //   определение причины неудачи
                int statusCode = ((ApiException) e).getStatusCode();
                switch (statusCode) {

                    case LocationSettingsStatusCodes.RESOLUTION_REQUIRED://-------------------требуется разрешение от пользователя
                        try {
                            ResolvableApiException resolvableApiException = (ResolvableApiException) e;
                            resolvableApiException.startResolutionForResult(activity, Util.CHECK_SETTINGS_CODE);//----показать окно этой настройки
                        } catch (IntentSender.SendIntentException sie) {
                            // Log the error
                            sie.printStackTrace();
                        }
                        break;
                    case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE://-----------невозможно изменить из приложения эти настройки(нужно установить в ручную)

                        Toast.makeText(activity, R.string.inf_location_setting, Toast.LENGTH_SHORT).show();

                        isLocationUpdatesActive = false;
                        break;
                }
            }
        });
    }


    @Override
    protected void finalize() throws Throwable {
        try {
            recoveryResources();
        } finally {
            super.finalize();
        }

    }

    public void recoveryResources() {
        if (fusedLocationClient != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            instance = null;
        }
    }


    public Location getCurrentLocation() {
        if (currentLocation != null) {
            onUpdateLocationListener.onUpdateLocation(currentLocation, inUse);
        }
        return currentLocation;
    }

    public DatabaseReference getRefLoc() {
        return refLoc;
    }

    public void setRefLoc(DatabaseReference refLoc) {
        this.refLoc = refLoc;
        getCurrentLocation();
    }
}

