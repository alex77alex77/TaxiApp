package com.alexei.taxiapp.driver.exClass;

import android.location.Location;

import androidx.annotation.NonNull;

import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.db.DataTaximeter;
import com.alexei.taxiapp.driver.model.RateModel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class TaximeterExecutorClass {

    private DatabaseReference refLocation;
    private DatabaseReference refTimer;

    private ExecutorService executorService;
    private ScheduledExecutorService serviceScheduled;
    private ScheduledFuture<?> futureDriveTimer;
    private ScheduledFuture<?> futureSaveTimer;


    private RateModel rate;
    private DataTaximeter taximeter;//----------


    private Location currLocation;


    private float amountAllMeter = 0;//для суммирования
    private float distanceMeterPart = 0;//для суммирования


    private long taximeterExeSeconds = 0;
    private long allSecondWait = 0;
    private int iPeriodSec;


    private OnTaximeterListener onTaximeterListener;

    public interface OnTaximeterListener {
        void onUpdateTaximeter(long secondExecuteTimer, long secondWait, float amountAllMeter, float amountMoney);

        void onSetSaveTaximeter(DataTaximeter taximeter);
    }

    public void setOnListener(OnTaximeterListener listener) {
        this.onTaximeterListener = listener;
    }


    public TaximeterExecutorClass(@NonNull DataTaximeter taximeter, DatabaseReference refTimer, DatabaseReference refLocation, Location currLoc) {
        this.taximeter = taximeter;
        this.refTimer = refTimer;
        this.refLocation = refLocation;
        this.currLocation = currLoc;//ссылка

        executorService = Executors.newFixedThreadPool(2);
        serviceScheduled = Executors.newScheduledThreadPool(2);

        setListeners();

        initVariableFieldTaximeter();

        executorService.submit(this::startTaximeter);

        if (taximeter.getStartTime() == 0) {
            setTaximeterStartTimer();
        } else {
            correctWaitSecond();
        }
    }

    private void setListeners() {
        setOnListener(new OnTaximeterListener() {
            @Override
            public void onUpdateTaximeter(long secondExecuteTimer, long secondWait, float amountAllMeter, float amountMoney) {

            }

            @Override
            public void onSetSaveTaximeter(DataTaximeter taximeter) {

            }
        });
    }


    private void startTaximeter() {

        futureDriveTimer = serviceScheduled.scheduleAtFixedRate(() -> {

            distanceMeterPart = 0;
            taximeterExeSeconds += iPeriodSec;//общее время
            taximeter.setExecuteSecond(taximeterExeSeconds);

            //   движение
            if (currLocation != null && currLocation.getLatitude() != 0 && currLocation.getLongitude() != 0) {
                if (taximeter.getLatitudeOld() != 0 && taximeter.getLongitudeOld() != 0) {

//                    if (rateKm > 0) {
                    distanceMeterPart = calculationDistance(taximeter.getLatitudeOld(), taximeter.getLongitudeOld(), currLocation.getLatitude(), currLocation.getLongitude());
                    if (distanceMeterPart > (taximeter.getRate().getDefWait() * iPeriodSec)) {//движение > 1м в 1сек = 3,600км/ч

                        amountAllMeter += distanceMeterPart;
                        taximeter.setDistanceAllMeter(amountAllMeter);//   все дистанция

                        taximeter.setLatitudeOld(currLocation.getLatitude());// ---------сброс
                        taximeter.setLongitudeOld(currLocation.getLongitude());

                    } else {
                        distanceMeterPart = 0;//будет оцениваться как простой
                    }
//                    }

                } else {

                    taximeter.setLatitudeOld(currLocation.getLatitude());// -------------init
                    taximeter.setLongitudeOld(currLocation.getLongitude());

                }
            }

            //   простой
            if (taximeter.getRate().getMin() > 0) {
                if (distanceMeterPart == 0) {//distanceMeterPart <= (defineWait * iPeriod)простой < 1м в 1сек = 3,600км/ч

                    allSecondWait += iPeriodSec;
                    taximeter.setSecondWaitTaximeter(allSecondWait);//   все время простоя

                    if (currLocation != null && currLocation.getLatitude() != 0 && currLocation.getLongitude() != 0) {
                        taximeter.setLatitudeOld(currLocation.getLatitude());// ---------сброс - оценен как простой
                        taximeter.setLongitudeOld(currLocation.getLongitude());
                    }
                }
            }


            //--------------------------save
            float countHourly = (float) (Math.ceil((double) taximeter.getExecuteSecond() / 3600));//количество часов(+1)            //   почасовая
            float allAmount = (taximeter.getRate().getFixedAmount()) + ((amountAllMeter / 1000) * taximeter.getRate().getKm()) + (((float) allSecondWait / 60) * taximeter.getRate().getMin()) + (countHourly * taximeter.getRate().getHourlyRate());
            taximeter.setAmount(allAmount);
            onTaximeterListener.onUpdateTaximeter(taximeterExeSeconds, allSecondWait, amountAllMeter, allAmount);
//            onUpdateTaximeterListener.onSetSaveTaximeter(taximeter);


        }, 0, iPeriodSec, TimeUnit.SECONDS);
//------------------------------------------------------------------------------------

        futureSaveTimer = serviceScheduled.scheduleAtFixedRate(() -> {
            onTaximeterListener.onSetSaveTaximeter(taximeter);

        }, 0, 5, TimeUnit.SECONDS);
    }


    private void initVariableFieldTaximeter() {
        iPeriodSec = 2;
        taximeterExeSeconds = taximeter.getExecuteSecond();
        allSecondWait = taximeter.getSecondWaitTaximeter();
        amountAllMeter = taximeter.getDistanceAllMeter();

    }

    private void setTaximeterStartTimer() {

        refTimer.setValue(ServerValue.TIMESTAMP, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Long timer = snapshot.getValue(Long.class);
                            if (timer != null) {
                                taximeter.setStartTime(timer);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    private void correctWaitSecond() {
        refTimer.setValue(ServerValue.TIMESTAMP, new DatabaseReference.CompletionListener() {
            @Override
            public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                ref.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            Long timer = snapshot.getValue(Long.class);
                            if (timer != null) {
                                getLastLocation(timer);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });
    }

    private void getLastLocation(Long timer) {
        refLocation.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    executorService.submit(() -> {
                        DataLocation dataLocation = snapshot.getValue(DataLocation.class);
                        if (dataLocation != null && dataLocation.getLatitude() != 0 && dataLocation.getLongitude() != 0) {
                            calcNotAccountedSeconds(timer, dataLocation.getLatitude(), dataLocation.getLongitude());
                        }
                    });

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void calcNotAccountedSeconds(Long currTimer, double lastLat, double lastLon) {
        long startTimer = taximeter.getStartTime();
        long allMillSec = currTimer - startTimer;//общее время
        long sec = allMillSec / 1000;
        long notAccountedSec = sec - (taximeter.getExecuteSecond());//не учтенное время
        float passedMeters = calculationDistance(taximeter.getLatitudeOld(), taximeter.getLongitudeOld(), lastLat, lastLon);

        if (notAccountedSec > 0) {
            taximeterExeSeconds += notAccountedSec;//убираем не учтенное время

            if ((passedMeters / notAccountedSec) > taximeter.getRate().getDefWait()) {//движение
                amountAllMeter += passedMeters;//увеличиваем пройденную дистанцию(оплачеваемое)
            } else {//простой
                if (taximeter.getRate().getMin() > 0) {
                    allSecondWait += notAccountedSec;//увеличиваем время ожидания(оплачеваемое)
                }
            }
        }
    }


    private static float calculationDistance(double oldLatitude, double oldLongitude, double curLatitude, double curLongitude) {
        float[] results = new float[1];
        if (oldLatitude != 0 && oldLongitude != 0 && curLatitude != 0 && curLongitude != 0) {
            Location.distanceBetween(oldLatitude, oldLongitude, curLatitude, curLongitude, results);
        }
        return results[0];
    }


    public RateModel getRate() {
        return rate;
    }


    public void setDataRate(RateModel rate) {
        this.rate = rate;

        updateRate(rate);
    }

    private void updateRate(RateModel rate) {
        allSecondWait = 0;//сброс оплачеваемого
        taximeterExeSeconds = 0;//сброс общего

        taximeter.setStartTime(0);
        taximeter.setRate(rate);

        onTaximeterListener.onSetSaveTaximeter(taximeter);
    }

    public void recoveryResources() {
        if (futureDriveTimer != null) futureDriveTimer.cancel(true);
        if (futureSaveTimer != null) futureSaveTimer.cancel(true);

        executorService.shutdown();
        serviceScheduled.shutdown();

    }

    @Override
    protected void finalize() throws Throwable {
        try {

            recoveryResources();

        } finally {
            super.finalize();
        }
    }

}

