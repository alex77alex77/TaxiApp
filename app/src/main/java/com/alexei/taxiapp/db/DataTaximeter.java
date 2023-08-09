package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.alexei.taxiapp.driver.model.RateModel;
import com.alexei.taxiapp.server.model.RouteInfoModel;
@Entity(tableName = "taximeters")
public class DataTaximeter {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    private float amount;
    private String currency;
    private float distanceAllMeter;
    private long executeSecond;

    private String keyDrv;
    private double latitudeOld;
    private double longitudeOld;
    private String orderKey;
    @Embedded
    private RateModel rate;
    @Embedded
    private RouteInfoModel route;
    private long secondWaitTaximeter;
    private long startTime;
    private long timeUpdate;

    public DataTaximeter() {
    }

    public DataTaximeter(String keyDrv2, String orderKey2, float distanceAllMeter2, long secondWaitTaximeter2, RateModel rate2, long startTime2, double latitudeOld2, double longitudeOld2, long timeUpdate2, long executeSecond2, float amount2, String currency2, RouteInfoModel route2) {
        this.keyDrv = keyDrv2;
        this.orderKey = orderKey2;
        this.distanceAllMeter = distanceAllMeter2;
        this.secondWaitTaximeter = secondWaitTaximeter2;
        this.rate = rate2;
        this.startTime = startTime2;
        this.latitudeOld = latitudeOld2;
        this.longitudeOld = longitudeOld2;
        this.timeUpdate = timeUpdate2;
        this.executeSecond = executeSecond2;
        this.amount = amount2;
        this.currency = currency2;
        this.route = route2;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKeyDrv() {
        return this.keyDrv;
    }

    public void setKeyDrv(String keyDrv2) {
        this.keyDrv = keyDrv2;
    }

    public String getOrderKey() {
        return this.orderKey;
    }

    public void setOrderKey(String orderKey2) {
        this.orderKey = orderKey2;
    }

    public float getDistanceAllMeter() {
        return this.distanceAllMeter;
    }

    public void setDistanceAllMeter(float distanceAllMeter2) {
        this.distanceAllMeter = distanceAllMeter2;
    }

    public long getSecondWaitTaximeter() {
        return this.secondWaitTaximeter;
    }

    public void setSecondWaitTaximeter(long secondWaitTaximeter2) {
        this.secondWaitTaximeter = secondWaitTaximeter2;
    }

    public RateModel getRate() {
        return this.rate;
    }

    public void setRate(RateModel rate2) {
        this.rate = rate2;
    }

    public long getStartTime() {
        return this.startTime;
    }

    public void setStartTime(long startTime2) {
        this.startTime = startTime2;
    }

    public double getLatitudeOld() {
        return this.latitudeOld;
    }

    public void setLatitudeOld(double latitudeOld2) {
        this.latitudeOld = latitudeOld2;
    }

    public double getLongitudeOld() {
        return this.longitudeOld;
    }

    public void setLongitudeOld(double longitudeOld2) {
        this.longitudeOld = longitudeOld2;
    }

    public long getTimeUpdate() {
        return this.timeUpdate;
    }

    public void setTimeUpdate(long timeUpdate2) {
        this.timeUpdate = timeUpdate2;
    }

    public long getExecuteSecond() {
        return this.executeSecond;
    }

    public void setExecuteSecond(long executeSecond2) {
        this.executeSecond = executeSecond2;
    }

    public float getAmount() {
        return this.amount;
    }

    public void setAmount(float amount2) {
        this.amount = amount2;
    }

    public String getCurrency() {
        return this.currency;
    }

    public void setCurrency(String currency2) {
        this.currency = currency2;
    }

    public RouteInfoModel getRoute() {
        return this.route;
    }

    public void setRoute(RouteInfoModel route2) {
        this.route = route2;
    }
}
