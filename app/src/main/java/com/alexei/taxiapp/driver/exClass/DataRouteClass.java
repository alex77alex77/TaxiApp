package com.alexei.taxiapp.driver.exClass;

import android.location.Location;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.db.DataRoute;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.util.Util;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DataRouteClass {
    private ScheduledExecutorService serviceScheduled;
    private ScheduledFuture<?> futureSaveTimer;
    private ExecutorService executorservice;
    private AppDatabase db;
    private DataRoute dataRoute;
    private Location currLocation;
    private Location oldLocation;


    public DataRouteClass(@NonNull String currUId, @NonNull InfoOrder order, Location location) {
        this.serviceScheduled = Executors.newScheduledThreadPool(2);
        this.executorservice = Executors.newFixedThreadPool(2);
        db = Room.databaseBuilder(App.context, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы
        this.currLocation = location;
        this.oldLocation = new Location("");

        executorservice.submit(() -> initDataRoure(currUId, order));
    }

    private void initDataRoure(String currUId, InfoOrder order) {
        dataRoute = db.getDataRouteDAO().getDataRoute(currUId, order.getKeyOrder());

        if (dataRoute == null) {
            dataRoute = new DataRoute(currUId, order.getKeyOrder(), System.currentTimeMillis(), new ArrayList<>(), order.getFrom(), order.getTo());
            dataRoute.getDots().add(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()));
            long id = db.getDataRouteDAO().add(dataRoute);

            if (id >= 0) {
                dataRoute.setId(id);
            }
        }
        runSaveCoordinates();
    }

    private void runSaveCoordinates() {
        futureSaveTimer = serviceScheduled.scheduleAtFixedRate(() -> {

            if (dataRoute != null && (currLocation.getLatitude() != oldLocation.getLatitude() || currLocation.getLongitude() != oldLocation.getLongitude())) {
                dataRoute.getDots().add(new LatLng(currLocation.getLatitude(), currLocation.getLongitude()));

                db.getDataRouteDAO().update(dataRoute);

                oldLocation.set(currLocation);
            }
        }, 0, 1, TimeUnit.SECONDS);
    }

    public void recoveryResources() {
        if (futureSaveTimer != null) futureSaveTimer.cancel(true);
        executorservice.shutdownNow();
        serviceScheduled.shutdownNow();
    }

    @Override
    protected void finalize() throws Throwable {
        recoveryResources();
        super.finalize();
    }
}
