package com.alexei.taxiapp.driver.activity;

import android.content.Intent;
import android.location.Location;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.SelectPointInMapActivity;
import com.alexei.taxiapp.databinding.ActivityListFreeOrdersBinding;
import com.alexei.taxiapp.databinding.ActivityOrderDisplayBinding;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class OrderDisplayActivity extends AppCompatActivity {
    private MediaPlayer mp;
    private BuildLocationClass locationClass;

    private DatabaseReference refLocation;
    private FirebaseAuth auth;
    private FirebaseDatabase database;

    private String keyOrder;
    private String currentUserUid;

    private DataLocation mFrom;
    private DataLocation mTo;

    private Location currLocation;

    private String senderUid;
    private InfoOrder infoOrder;
    private boolean isSound;
    private ActivityOrderDisplayBinding binding;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityOrderDisplayBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        setContentView(R.layout.activity_order_display);
        setRequestedOrientation(getResources().getConfiguration().orientation);


        mFrom = new DataLocation();
        mTo = new DataLocation();

        auth = FirebaseAuth.getInstance();
        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базыданных
        if (auth.getCurrentUser() != null) {
            currentUserUid = auth.getCurrentUser().getUid();
            refLocation = database.getReference().child("SHAREDSERVER/driversList").child(currentUserUid).child("location");
        } else {
            finish();
        }

        Intent intentData = getIntent();
        if (intentData != null) {
            infoOrder = (InfoOrder) intentData.getParcelableExtra("data_order");
            isSound = intentData.getBooleanExtra("isSound", false);
            displayInfoOrder(infoOrder);
        } else {
            finish();
        }

        locationClass = BuildLocationClass.getInstance(this, refLocation);
        locationClass.setOnUpdateListener(new BuildLocationClass.OnUpdateLocationListener() {
            @Override
            public void onUpdateLocation(Location location, int satellites) {
                currLocation = location;
                defDistanceToClient();
            }
        });
        locationClass.getCurrentLocation();


        binding.ibDisplayMapFrom.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mFrom.getLatitude() != 0 && mFrom.getLongitude() != 0) {
                    Intent intent = new Intent(OrderDisplayActivity.this, SelectPointInMapActivity.class);
                    intent.putExtra("location", mFrom);

                    intent.putExtra("titleMarker", getString(R.string.where_from2));
                    startActivity(intent);

                } else {
                    Toast.makeText(OrderDisplayActivity.this, R.string.location_not_defined, Toast.LENGTH_LONG).show();
                }
            }
        });

        binding.displayMapToImageButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTo.getLatitude() != 0 && mTo.getLongitude() != 0) {
                    Intent intent = new Intent(OrderDisplayActivity.this, SelectPointInMapActivity.class);
                    intent.putExtra("location", mTo);

                    intent.putExtra("titleMarker", getString(R.string.where_to2));
                    startActivity(intent);

                } else {
                    Toast.makeText(OrderDisplayActivity.this, R.string.location_not_defined, Toast.LENGTH_LONG).show();
                }
            }
        });

        binding.acceptOrderDisplayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                acceptOrder(Util.ACCEPT_OK);
            }
        });

        binding.notAcceptOrderDisplayButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                acceptOrder(Util.ACCEPT_CANCEL);
            }
        });

        playSignal();
    }

    private void defDistanceToClient() {

        binding.distanceToOrderTextView.setText(getString(R.string.t_where_to_client) + Util.defineDistance(mFrom.getLatitude(), mFrom.getLongitude(), currLocation.getLatitude(), currLocation.getLongitude()));
    }

    private void playSignal() {
        if (isSound) {
            mp = MediaPlayer.create(getApplicationContext(), R.raw.bell_sound);
            mp.setLooping(true);
            mp.start();
        }
    }


    @Override
    protected void onDestroy() {
        try {
            if (mp != null) {
                mp.release();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            super.onDestroy();
        }
    }


    private void displayInfoOrder(InfoOrder order) {
        binding.tvProvider.setText(order.getProviderName() + "\n" + Util.formatTimeDate.format(order.getTimestamp()));

        binding.tvAddressDisplayOrderFrom.setText(Util.getAddress(order.getFrom().getLatitude(), order.getFrom().getLongitude(), Util.TYPE_ADDRESS_LONG));
        binding.tvAddressDisplayTo.setText(Util.getAddress(order.getTo().getLatitude(), order.getTo().getLongitude(), Util.TYPE_ADDRESS_LONG));

        binding.tvNoteDisplayOrder.setText(order.getNote());
        binding.tvTypeTr.setText(R.string.t_type);
        binding.tvTypeTr.append(order.getTypeTr());

        mTo = order.getTo();
        mFrom = order.getFrom();

        keyOrder = order.getKeyOrder();
        senderUid = order.getClientUid();

        binding.tvDisplayRate.setText(R.string.t_rate);
        binding.tvDisplayRate.append(order.getRate().toString());
    }


    private void acceptOrder(int status) {

        Intent intent = new Intent();
        intent.putExtra("senderUid", senderUid);
        intent.putExtra("keyOrder", keyOrder);
        intent.putExtra("data_order", infoOrder);

        setResult(status, intent);
        finish();
    }


    @Override
    public void onBackPressed() {
        Intent intent = new Intent();
        intent.putExtra("keyOrder", keyOrder);
        setResult(Util.ACCEPT_CANCEL, intent);
        finish();
    }
}