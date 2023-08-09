package com.alexei.taxiapp.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "srv_rates")
public class RatesSrv implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    @ColumnInfo(name = "curr_u_id")
    private String currUId;
    private float defWait = 1.0f;
    private float fixedAmount = 0.0f;
    private float hourlyRate = 0.0f;
    private float km = 0.0f;
    private float min = 0.0f;
    @ColumnInfo(name = "title")
    private String title = "New";


    public RatesSrv() {
    }

    public RatesSrv(String currUId2, String title2, float km, float min2, float defWait2, float fixedAmount2, float hourlyRate2) {
        this.currUId = currUId2;
        this.title = title2;
        this.km = km;
        this.min = min2;
        this.defWait = defWait2;
        this.fixedAmount = fixedAmount2;
        this.hourlyRate = hourlyRate2;
    }

    public static final Creator<RatesSrv> CREATOR = new Creator<RatesSrv>() {
        public RatesSrv createFromParcel(Parcel in) {
            return new RatesSrv(in);
        }

        public RatesSrv[] newArray(int size) {
            return new RatesSrv[size];
        }
    };

    protected RatesSrv(Parcel in) {
        this.id = in.readLong();
        this.currUId = in.readString();
        this.title = in.readString();
        this.km = in.readFloat();
        this.min = in.readFloat();
        this.defWait = in.readFloat();
        this.fixedAmount = in.readFloat();
        this.hourlyRate = in.readFloat();
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCurrUId() {
        return this.currUId;
    }

    public void setCurrUId(String currUId2) {
        this.currUId = currUId2;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title2) {
        this.title = title2;
    }

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
        parcel.writeLong(this.id);
        parcel.writeString(this.currUId);
        parcel.writeString(this.title);
        parcel.writeFloat(this.km);
        parcel.writeFloat(this.min);
        parcel.writeFloat(this.defWait);
        parcel.writeFloat(this.fixedAmount);
        parcel.writeFloat(this.hourlyRate);
    }

    public String toString() {
        return this.title;
    }
}
