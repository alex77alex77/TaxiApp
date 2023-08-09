package com.alexei.taxiapp.driver.activity;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.PopupMenu;
import android.widget.TextView;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.SelectPointInMapActivity;
import com.alexei.taxiapp.databinding.ActivityDriverSettingBinding;
import com.alexei.taxiapp.databinding.ActivityListFreeOrdersBinding;
import com.alexei.taxiapp.driver.adapter.AdapterFreeOrders;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.exClass.BuildLocationClass;
import com.alexei.taxiapp.server.model.RouteInfoModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class ListFreeOrdersActivity extends AppCompatActivity {
    private static final String APP_PREFERENCES = "mySetting";


    private FirebaseAuth auth;
    private FirebaseDatabase database;
    private DatabaseReference refLocation;


    private RecyclerView.LayoutManager orderLayoutManager;
    private AdapterFreeOrders adapterFreeOrders;
    private ArrayList<InfoOrder> orderArrayList = new ArrayList<>();

    private BuildLocationClass locationClass;
    private Location currentLocation;

    private int idSorted;
    private boolean bSortByDate;
    private ActivityListFreeOrdersBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityListFreeOrdersBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setRequestedOrientation(getResources().getConfiguration().orientation);

        SharedPreferences sharedPreferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);

        idSorted = sharedPreferences.getInt("CHECKED_MENU", 0);

        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базы данных
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            refLocation = database.getReference().child("SHAREDSERVER/driversList").child(auth.getCurrentUser().getUid()).child("location");

            binding.tvMenuFreeOrders.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    PopupMenu popupMenu = new PopupMenu(ListFreeOrdersActivity.this, binding.tvMenuFreeOrders);
                    popupMenu.inflate(R.menu.driver_free_orders_menu);
                    popupMenu.show();
                    if (popupMenu.getMenu().findItem(idSorted) != null) {
                        popupMenu.getMenu().findItem(idSorted).setCheckable(true).setChecked(true);
                    }
                    popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                        @Override
                        public boolean onMenuItemClick(MenuItem menuItem) {

                            handlerClickItemMenu(menuItem);
                            return true;
                        }
                    });
                }
            });


            Intent intent = getIntent();
            if (intent != null) {
                orderArrayList = intent.getParcelableArrayListExtra("listOrder");
                if (orderArrayList == null) {
                    finish();
                } else {

                    buildRecyclerView();
                    chooseSorted(idSorted);
                    locationClass = BuildLocationClass.getInstance(ListFreeOrdersActivity.this, refLocation);
                    locationClass.setOnUpdateListener(new BuildLocationClass.OnUpdateLocationListener() {
                        @Override
                        public void onUpdateLocation(Location location, int satellites) {
                            currentLocation = location;
                            getDistanceToClient(location);
                        }
                    });

                    locationClass.getCurrentLocation();
                }
            } else {
                finish();
            }
        } else {
            finish();
        }
    }

    private void savePreferences(String key, int value) {
        SharedPreferences sharedPreferences = getSharedPreferences(APP_PREFERENCES, MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putInt(key, value);
        editor.apply();
        idSorted = sharedPreferences.getInt("CHECKED_MENU", 0);
    }


    private void handlerClickItemMenu(MenuItem item) {
        savePreferences("CHECKED_MENU", item.getItemId());
        chooseSorted(item.getItemId());

    }

    private void chooseSorted(int typeSorted) {
        switch (typeSorted) {
            case R.id.sortedByDate:

                sortedByDate();
                break;
            case R.id.sortedByServer:

                sortedByServer();
                break;
        }
    }

    private void sortedByDate() {
        if (!bSortByDate) {
            bSortByDate = true;
            Collections.sort(orderArrayList, new Comparator<InfoOrder>() {
                public int compare(InfoOrder p1, InfoOrder p2) {
                    return ((Long) p1.getTimestamp()).compareTo((Long) p2.getTimestamp());
                }
            });
        } else {
            Collections.reverse(orderArrayList);
        }

        adapterFreeOrders.notifyDataSetChanged();
    }

    private void sortedByServer() {
        Collections.sort(orderArrayList, new Comparator<InfoOrder>() {
            public int compare(InfoOrder p1, InfoOrder p2) {
                return (p1.getProviderName()).compareTo(p2.getProviderName());
            }
        });
        adapterFreeOrders.notifyDataSetChanged();
//        boolean exists = false;
//        int index = 0;
//        orderArrayList = (ArrayList<InfoOrder>) orderArrayList.stream().sorted(Comparator.comparing(InfoOrder::getProviderName)).collect(Collectors.toList());
//
//        adapterFreeOrders.notifyDataSetChanged();
//        List<String> names=orderArrayList.stream().sorted()
//        if (orderArrayList.size() > 0) {
//
//            for (InfoOrder model : orderArrayList) {
//
//
//                    if (model.getProviderName().equals(name)) {
//                        exists = true;
//                        Collections.swap(orderArrayList, orderArrayList.indexOf(model), index);
//                        index++;
//                    }
//            }
//        }
//        adapterFreeOrders.notifyDataSetChanged();
//        if (!exists) {
//            Toast.makeText(getApplicationContext(), R.string.not_found, Toast.LENGTH_LONG).show();
//        }
    }

    private void getDistanceToClient(Location location) {
        orderArrayList.forEach(o -> {
            o.setDistanceToClient(Util.defineDistance(o.getFrom().getLatitude(), o.getFrom().getLongitude(), location.getLatitude(), location.getLongitude()));
        });
        adapterFreeOrders.notifyDataSetChanged();
    }


    public void selectedItem(InfoOrder infoOrder, int position) {
        Intent intent = new Intent();

        intent.putExtra("key_order", infoOrder.getKeyOrder());
        setResult(Util.ACCEPT_OK, intent);
        finish();
    }

    private void buildRecyclerView() {

        binding.recyclerViewFreeOrders.setHasFixedSize(true);
        orderLayoutManager = new LinearLayoutManager(this);
        binding.recyclerViewFreeOrders.setLayoutManager(orderLayoutManager);

        adapterFreeOrders = new AdapterFreeOrders(this, orderArrayList);
        binding.recyclerViewFreeOrders.setAdapter(adapterFreeOrders);

    }


    public void showDotFrom(InfoOrder infoOrder) {
        Intent intent = new Intent(ListFreeOrdersActivity.this, SelectPointInMapActivity.class);
        intent.putExtra("location", infoOrder.getFrom());
        intent.putExtra("route", new RouteInfoModel(infoOrder.getFrom().getLatitude(), infoOrder.getFrom().getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude(), getString(R.string.t_from), getString(R.string.I)));

        startActivity(intent);
    }

    public void showDotTo(InfoOrder infoOrder) {
        Intent intent = new Intent(ListFreeOrdersActivity.this, SelectPointInMapActivity.class);
        intent.putExtra("location", infoOrder.getTo());
        intent.putExtra("route", new RouteInfoModel(infoOrder.getTo().getLatitude(), infoOrder.getTo().getLongitude(), currentLocation.getLatitude(), currentLocation.getLongitude(), getString(R.string.t_to), getString(R.string.I)));
        startActivity(intent);
    }
}