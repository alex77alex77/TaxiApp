package com.alexei.taxiapp.client.activity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityClientStatusOrderBinding;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.util.Util;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;

public class ClientStatusOrderActivity extends AppCompatActivity implements OnMapReadyCallback {
    private GoogleMap mMap;
    private Marker driverMarker;
    private Marker passengerMarker;
    private float mZoom = 16;
    private DatabaseReference locationDrvRef;
    private DatabaseReference hostFreeOrdersRef;
    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DataLocation location;
    private DataLocation mFrom;
    private InfoOrder infoOrder;
    private boolean bShowLocEndRoute = false;
    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private ActivityClientStatusOrderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityClientStatusOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        setContentView(R.layout.activity_client_status_order);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapClient);
        mapFragment.getMapAsync(this);


        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных

        if (auth.getCurrentUser() == null) {

            finish();
        } else {

            Intent intent = getIntent();
            if (intent != null) {

                mFrom = new DataLocation();
                location = intent.getParcelableExtra("location");
                infoOrder = (InfoOrder) intent.getParcelableExtra("data_order");
                if (infoOrder != null) {

                    loadDataOrder(infoOrder);
                } else {

                    Toast.makeText(getApplicationContext(), "data_order null", Toast.LENGTH_LONG).show();
                    finish();
                }

            } else {

                Toast.makeText(getApplicationContext(), "Intent null", Toast.LENGTH_LONG).show();
                finish();
            }
        }
    }

    private void loadDataOrder(InfoOrder infoOrder) {

        mFrom = infoOrder.getFrom();

        locationDrvRef = database.getReference().child("SHAREDSERVER/driversList").child(infoOrder.getDriverUid()).child("location");

        if (infoOrder.getProviderKey().equals("SHAREDSERVER")) {
            hostFreeOrdersRef = database.getReference().child("SHAREDSERVER/freeOrders");
        } else {
            hostFreeOrdersRef = database.getReference().child("serverList").child(infoOrder.getProviderKey()).child("freeOrders");
        }

        if (infoOrder.getStatus() != Util.ROUTE_FINISHED_ORDER_STATUS) {

            initStatusListener();

        } else if (location.getLatitude() != 0 && location.getLongitude() != 0) {
            bShowLocEndRoute = true;
        }
    }

    private void showLocationEndRouteDriver(DataLocation coordinateEndRoute) {
        binding.textViewDistanceToDriver.setText(R.string.route_finished);
        LatLng latLngDriver = new LatLng(coordinateEndRoute.getLatitude(), coordinateEndRoute.getLongitude());

        if (driverMarker == null) {
            driverMarker = mMap.addMarker(new MarkerOptions().position(latLngDriver).icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_logos)));//----------создание маркера
        } else {
            driverMarker.setPosition(latLngDriver);
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngDriver, mZoom));
    }

    private void initStatusListener() {

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    Integer status = snapshot.getValue(Integer.class);
                    if (status != null) {
                        switch (status) {

                            case Util.ASSIGN_ORDER_STATUS://--------------------водитель назначен
                            case Util.ARRIVE_ORDER_STATUS://--------------------водитель ожидает
                            case Util.EXECUTION_ORDER_STATUS://--------------------водитель на маршруте

                                setLocationDrvListener();
                                break;
                            case Util.FREE_ORDER_STATUS:

                                Util.removeByRefListener(locationDrvRef, mapListeners);

                                if (driverMarker != null) {
                                    driverMarker.remove();
                                }
                                driverMarker = null;
                                binding.textViewDistanceToDriver.setText(R.string.t_find);
                                break;
                            case Util.ROUTE_FINISHED_ORDER_STATUS:

                                Util.removeByRefListener(locationDrvRef, mapListeners);
                                binding.textViewDistanceToDriver.setText(R.string.route_finished);
                                break;
                        }
                    }

                } else {
                    finish();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        hostFreeOrdersRef.child(infoOrder.getKeyOrder()).child("status").addValueEventListener(listener);
        mapListeners.put(hostFreeOrdersRef.child(infoOrder.getKeyOrder()).child("status"), listener);
    }


    private void setLocationDrvListener() {

        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.getValue() != null) {
                    DataLocation location = snapshot.getValue(DataLocation.class);
                    if (location != null) {

                        if (driverMarker == null) {
                            driverMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).icon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_logos)));//----------создание маркера
                        } else {
                            driverMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));
                        }

                        displayMapByCenter();
                        String strDistance = getString(R.string.distance_) + Util.defineDistance(mFrom.getLatitude(), mFrom.getLongitude(), location.getLatitude(), location.getLongitude());
                        binding.textViewDistanceToDriver.setText(strDistance);

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        };
        locationDrvRef.addValueEventListener(listener);
        mapListeners.put(locationDrvRef, listener);
    }

    private void displayMapByCenter() {
        if (passengerMarker != null) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            builder.include(driverMarker.getPosition());
            builder.include(passengerMarker.getPosition());

            LatLngBounds bounds = builder.build();

            int width = getResources().getDisplayMetrics().widthPixels;
            int height = getResources().getDisplayMetrics().heightPixels;
            int padding = (int) (width * 0.15); // смещение от краев карты 10% экрана

            CameraUpdate cu = CameraUpdateFactory.newLatLngBounds(bounds, width, height, padding);
            mMap.animateCamera(cu);
        }
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
        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mZoom = mMap.getCameraPosition().zoom;
            }
        });
        if (!bShowLocEndRoute) {
            LatLng latLngPassenger = new LatLng(mFrom.getLatitude(), mFrom.getLongitude());
            if (passengerMarker != null) {
                passengerMarker.setPosition(latLngPassenger);
            } else {
                passengerMarker = mMap.addMarker(new MarkerOptions().position(latLngPassenger));
            }
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLngPassenger, mZoom));
        } else {
            showLocationEndRouteDriver(location);//последние координаты водителя
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Util.removeAllValueListener(mapListeners);
    }


}