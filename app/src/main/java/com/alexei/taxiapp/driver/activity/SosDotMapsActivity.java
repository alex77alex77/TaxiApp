package com.alexei.taxiapp.driver.activity;

import android.content.Intent;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivitySosDotMapsBinding;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class SosDotMapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivitySosDotMapsBinding binding;

    private Marker myMarker;
    private float mZoom = 14;

    private FirebaseDatabase database;
    private BuildLocationClass locationClass;
    private ArrayList<String> keyList;
    private DataLocation loc;
    private String currUserUid;
    private Location location = new Location("");

    private Map<String, Marker> mapMarkers = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivitySosDotMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapDots);

        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        Intent intent = getIntent();
        if (intent != null) {

            database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных

            loc = intent.getParcelableExtra("location");
            keyList = intent.getStringArrayListExtra("keys");
            currUserUid = intent.getStringExtra("userUid");

        } else {
            finish();
        }
    }

    private void showDotKeys() {
        DatabaseReference ref = database.getReference().child("SHAREDSERVER/driversList");

        keyList.stream().filter(k -> !k.equals(currUserUid)).forEach(k -> ref.child(k).child("location").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataLocation pos = snapshot.getValue(DataLocation.class);
                    if (pos != null) {
                        Marker marker = mMap.addMarker(new MarkerOptions().position(new LatLng(pos.getLatitude(), pos.getLongitude())).title(getString(R.string.help_me)));
                        if (marker != null) {
                            mapMarkers.put(k, marker);
                            displayAllMarkersByCenter();
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        }));
    }

    private void displayAllMarkersByCenter() {

        LatLngBounds.Builder builder = new LatLngBounds.Builder();
        builder.include(myMarker.getPosition());
        mapMarkers.forEach((k, v) -> {
            if (v != null) {

                builder.include(v.getPosition());
            }
        });

        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), width, height, 100));
        mMap.setMaxZoomPreference(18);
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
        mMap.setMaxZoomPreference(18);
        LatLng latLng = new LatLng(loc.getLatitude(), loc.getLongitude());
        myMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(getString(R.string.I)));

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mZoom = mMap.getCameraPosition().zoom;

            }
        });

        if (myMarker != null) {
            myMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.taxi_logos));
        }

        moveCamera(new LatLng(loc.getLatitude(), loc.getLongitude()));

        if (keyList != null) {
            showDotKeys();
        }
    }

    private void moveCamera(LatLng position) {
        CameraPosition cameraPosition = new CameraPosition.Builder()
                .target(position)
                .zoom(mZoom)
                .build();

        mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
    }

    @Override
    protected void onStart() {
        super.onStart();

        locationClass = BuildLocationClass.getInstance(this, null);
        locationClass.setOnUpdateListener(new BuildLocationClass.OnUpdateLocationListener() {
            @Override
            public void onUpdateLocation(Location loc, int satellites) {
                location = loc;
                if (mMap != null) {

                    updateMarkerPos();
                }
            }
        });
        locationClass.getCurrentLocation();

    }

    private void updateMarkerPos() {

        myMarker.setPosition(new LatLng(location.getLatitude(), location.getLongitude()));

    }
}