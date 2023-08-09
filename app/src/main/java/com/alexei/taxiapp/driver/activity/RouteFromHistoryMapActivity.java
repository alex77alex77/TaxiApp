package com.alexei.taxiapp.driver.activity;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.view.View;

import androidx.fragment.app.FragmentActivity;
import androidx.room.Room;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityRouteFromHistoryMapBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.DataRoute;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.alexei.taxiapp.util.Util;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CustomCap;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.RoundCap;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class RouteFromHistoryMapActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityRouteFromHistoryMapBinding binding;
    private BuildLocationClass locationClass;

    private AppDatabase db;

    private String currUId;
    private String keyOrder;
    private Location location = new Location("");
    private Marker userMarker;
    private boolean bEye = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityRouteFromHistoryMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.mapRoute);
        mapFragment.getMapAsync(this);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы

        Intent intent = getIntent();
        if (intent != null) {
            currUId = intent.getStringExtra("user_id");
            keyOrder = intent.getStringExtra("keyOrder");

        }

        binding.tvEye.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                rollUp();
            }
        });

    }

    private void rollUp() {
        float dis = binding.rlRouteBlock.getWidth();
        if (bEye) {

            binding.tvInfoRoute.animate().translationXBy(-dis).setDuration(500);
            bEye = false;
        } else {
            binding.tvInfoRoute.animate().translationXBy(dis).setDuration(500);
            bEye = true;
        }
    }

    private void updateLocation() {
        LatLng pos = new LatLng(location.getLatitude(), location.getLongitude());
        if (userMarker == null) {

            userMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(location.getLatitude(), location.getLongitude())).title(getString(R.string.I)));
        } else {

            userMarker.setPosition(pos);
        }
        mMap.moveCamera(CameraUpdateFactory.newLatLng(pos));
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

        mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
            @Override
            public void onMapLoaded() {
                loadRoute();
                runBuildLocation();

            }
        });
    }

    private void runBuildLocation() {
        locationClass = new BuildLocationClass(RouteFromHistoryMapActivity.this, null);

        locationClass.setOnUpdateListener(new BuildLocationClass.OnUpdateLocationListener() {
            @Override
            public void onUpdateLocation(Location loc, int satellites) {
                location.set(loc);
                updateLocation();
//                Log.d("key1", "onUpdateLocation: " + locationClass);
            }
        });
        locationClass.getCurrentLocation();
    }


    @Override
    protected void onDestroy() {
        locationClass.stopLocationUpdate();
        locationClass.recoveryResources();
        super.onDestroy();
    }

    private void loadRoute() {

        try {

            DataRoute route = getRoute();
            if (route != null) {
                StringBuffer info = new StringBuffer();
                info.append(getString(R.string.where_from));
                info.append("\n");
                info.append(Util.getAddress(route.getFrom().getLatitude(), route.getFrom().getLongitude(), Util.TYPE_ADDRESS_LONG));
                info.append("\n");
                info.append(getString(R.string.where_to));
                info.append("\n");
                info.append(Util.getAddress(route.getTo().getLatitude(), route.getTo().getLongitude(), Util.TYPE_ADDRESS_LONG));
                binding.tvInfoRoute.setText(info);

                if (route != null) {
                    PolylineOptions options = new PolylineOptions().
                            width(4).
                            color(Color.RED).
                            startCap(new RoundCap());

                    for (int z = 0; z < route.getDots().size(); z++) {
                        LatLng point = route.getDots().get(z);
                        options.add(point);
                        options.endCap(new CustomCap(BitmapDescriptorFactory.fromResource(R.drawable.arrow24), 10));

                        mMap.addPolyline(options);
                    }
                    mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(route.getDots().get(0), 16));

                }
            }
            binding.flBlockRoute.setVisibility(View.GONE);
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    private DataRoute getRoute() throws ExecutionException, InterruptedException {
        Callable task = () -> {
            DataRoute route = db.getDataRouteDAO().getDataRoute(currUId, keyOrder);

            return route;
        };

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (DataRoute) future.get();
    }

}