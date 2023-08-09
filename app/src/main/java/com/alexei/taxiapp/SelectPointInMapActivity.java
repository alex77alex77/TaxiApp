package com.alexei.taxiapp;

import android.content.Intent;
import android.graphics.Color;
import android.location.Address;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alexei.taxiapp.databinding.ActivitySelectPointInMapBinding;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.server.model.RouteInfoModel;
import com.alexei.taxiapp.util.Util;
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
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutionException;

public class SelectPointInMapActivity extends AppCompatActivity implements OnMapReadyCallback {
    private Polyline line;
    private DataLocation location;
    private RouteInfoModel route;

    private String titleMarker;

    private GoogleMap mMap;
    private Marker dotToMarker;
    private Marker dotFromMarker;
    private float mZoom = 16;
    private ActivitySelectPointInMapBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySelectPointInMapBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_select_point_in_map);
        setRequestedOrientation(getResources().getConfiguration().orientation);

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.mapSelDot);
        mapFragment.getMapAsync(this);

        Intent intent = getIntent();
        if (intent != null) {
            location = intent.getParcelableExtra("location");
            route = intent.getParcelableExtra("route");
            titleMarker = intent.getStringExtra("titleMarker");

            loadView();
        }

        binding.butOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                selectedMarker();
            }
        });

        binding.etAddress.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                if (editable.length() > 0) {
                    binding.ibSpeakAddress.setVisibility(View.GONE);

                } else {
                    binding.ibSpeakAddress.setVisibility(View.VISIBLE);
                }
            }
        });

        binding.ibSpeakAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
                Locale current = getResources().getConfiguration().locale;
                intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, current);//Locale устанавливается в настроиках!!!!!!!!!!!!!!!!!!!!!

                startActivityForResult(intent, Util.VOICE_REQUEST_CODE);
            }
        });

        binding.ibFindAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                findAddress(binding.etAddress.getText().toString());

            }
        });
    }

    private void loadView() {
        if (route != null) {
            binding.tvRouteReport.setText(R.string.t_where_from);
            binding.tvRouteReport.append(Util.getAddress(route.getDotFromLat(), route.getDotFromLon(), Util.TYPE_ADDRESS_LONG));
            binding.tvRouteReport.append("\n");
            binding.tvRouteReport.append(getString(R.string.where_to));
            binding.tvRouteReport.append("\n");
            binding.tvRouteReport.append(Util.getAddress(route.getDotToLat(), route.getDotToLon(), Util.TYPE_ADDRESS_LONG));
            binding.tvRouteReport.setVisibility(View.VISIBLE);

            binding.blockFindLL.setVisibility(View.GONE);
        }

    }

    private void drawLineRoute() {

        if (line != null) line.remove();
        if (route.getDotFromLat() != 0 && route.getDotFromLon() != 0 && route.getDotToLat() != 0 && route.getDotToLon() != 0) {
            line = mMap.addPolyline(new PolylineOptions().add(new LatLng(route.getDotFromLat(), route.getDotFromLon()),
                            new LatLng(route.getDotToLat(), route.getDotToLon()))
                    .color(Color.RED)
                    .width(2));

            binding.tvDistanceToDot.setText(Util.defineDistance(route.getDotFromLat(), route.getDotFromLon(),
                    route.getDotToLat(), route.getDotToLon()));
        }

    }

    private void findAddress(String locationString) {

        try {
            List<Address> addressList = Util.getAddress(locationString);

            if (addressList.size() > 0) {
                Address address = addressList.get(0);
                setPositionMarker(new LatLng(address.getLatitude(), address.getLongitude()));
            } else {
                Toast.makeText(getApplicationContext(), R.string.not_found, Toast.LENGTH_SHORT).show();
            }
        } catch (ExecutionException | InterruptedException e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    private void setPositionMarker(LatLng latLng) {

        if (dotToMarker == null) {
            dotToMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(titleMarker));

        } else {
            dotToMarker.setPosition(latLng);
        }

        if (mMap != null) {
            CameraPosition cameraPosition = new CameraPosition.Builder()
                    .target(latLng)
                    .zoom(mZoom)
                    .build();
            mMap.moveCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
        }
        location = new DataLocation(latLng);//.latitude, latLng.longitude

    }

    private void displayAllMarkersByCenter() {
        LatLngBounds.Builder builder = new LatLngBounds.Builder();

        if (dotToMarker != null) {
            builder.include(dotToMarker.getPosition());
        }
        if (dotFromMarker != null) {
            builder.include(dotFromMarker.getPosition());
        }

        mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 100));
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

        mMap.setOnCameraIdleListener(new GoogleMap.OnCameraIdleListener() {
            @Override
            public void onCameraIdle() {
                mZoom = mMap.getCameraPosition().zoom;
            }
        });

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(@NonNull LatLng latLng) {
                if (route == null) {

                    if (dotToMarker == null) {
                        dotToMarker = mMap.addMarker(new MarkerOptions().position(latLng).title(titleMarker));

                    } else {
                        dotToMarker.setPosition(latLng);
                    }

                    location = new DataLocation(latLng);//.latitude, latLng.longitude
                }
            }
        });


        if (route != null) {

            if (route.getDotFromLat() != 0 && route.getDotFromLon() != 0) {
                dotFromMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(route.getDotFromLat(), route.getDotFromLon())).title(route.getTitleFrom()));
                if (dotFromMarker != null) {
                    dotFromMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.yellow_dot));
                }
            }

            if (route.getDotToLat() != 0 && route.getDotToLon() != 0) {
                dotToMarker = mMap.addMarker(new MarkerOptions().position(new LatLng(route.getDotToLat(), route.getDotToLon())).title(route.getTitleTo()));
                if (dotToMarker != null) {
                    dotToMarker.setIcon(BitmapDescriptorFactory.fromResource(R.drawable.red_dot));
                }
            }

            mMap.setOnMapLoadedCallback(new GoogleMap.OnMapLoadedCallback() {
                @Override
                public void onMapLoaded() {
                    displayAllMarkersByCenter();
                    drawLineRoute();
                }
            });

        } else {

            LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
            setPositionMarker(latLng);

        }
    }


    public void selectedMarker() {
        Intent intent = new Intent();
        intent.putExtra("location", location);

        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        setResult(Util.RESULT_OK, intent);

        finish();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case Util.VOICE_REQUEST_CODE:
                if (resultCode == RESULT_OK) {
                    voiceMessageInText(data);
                }
                break;
        }
    }

    private void voiceMessageInText(Intent data) {
        ArrayList<String> text = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
        findAddress(text.get(0));
    }
}