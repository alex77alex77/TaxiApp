package com.alexei.taxiapp.driver.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.alexei.taxiapp.util.Util;


public class RateModel implements Parcelable {
    private float defWait = 0.0f;
    private float fixedAmount = 0.0f;
    private float hourlyRate = 0.0f;
    private float km = 0.0f;
    private float min = 0.0f;

    public RateModel() {
    }

    public RateModel(float fixedAmount2) {
        this.fixedAmount = fixedAmount2;
    }

    public RateModel(float km, float min2, float defWait2, float fixedAmount2, float hourlyRate2) {
        this.km = km;
        this.min = min2;
        this.defWait = defWait2;
        this.fixedAmount = fixedAmount2;
        this.hourlyRate = hourlyRate2;
    }

    protected RateModel(Parcel in) {
        this.km = in.readFloat();
        this.min = in.readFloat();
        this.defWait = in.readFloat();
        this.fixedAmount = in.readFloat();
        this.hourlyRate = in.readFloat();
    }

    public static final Creator<RateModel> CREATOR = new Creator<RateModel>() {
        public RateModel createFromParcel(Parcel in) {
            return new RateModel(in);
        }

        public RateModel[] newArray(int size) {
            return new RateModel[size];
        }
    };

    public float getKm() {
        return this.km;
    }

    public void setKm(float km) {
        this.km = km;
    }

    public float getMin() {
        return this.min;
    }

    public void setMin(float min2) {
        this.min = min2;
    }

    public float getDefWait() {
        return this.defWait;
    }

    public void setDefWait(float defWait2) {
        this.defWait = defWait2;
    }

    public float getFixedAmount() {
        return this.fixedAmount;
    }

    public void setFixedAmount(float fixedAmount2) {
        this.fixedAmount = fixedAmount2;
    }

    public float getHourlyRate() {
        return this.hourlyRate;
    }

    public void setHourlyRate(float hourlyRate2) {
        this.hourlyRate = hourlyRate2;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeFloat(this.km);
        parcel.writeFloat(this.min);
        parcel.writeFloat(this.defWait);
        parcel.writeFloat(this.fixedAmount);
        parcel.writeFloat(this.hourlyRate);
    }

    public String toString() {
        return Util.decimalFormat.format((double) getKm()) + "/" + Util.decimalFormat.format((double) getMin()) + "/" + Util.decimalFormat.format((double) getFixedAmount()) + "/" + Util.decimalFormat.format((double) getHourlyRate());
    }
}
