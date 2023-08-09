package com.alexei.taxiapp.server.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.Ignore;

public class RouteInfoModel implements Parcelable {
    private double dotFromLat;
    private double dotFromLon;
    private double dotToLat;
    private double dotToLon;

    private String titleFrom = "";
    private String titleTo = "";

    @Ignore
    public RouteInfoModel() {
    }

    @Ignore
    public RouteInfoModel(double dotFromLat, double dotFromLon, double dotToLat, double dotToLon, String titleFrom, String titleTo) {

        this.dotFromLat = dotFromLat;
        this.dotFromLon = dotFromLon;
        this.dotToLat = dotToLat;
        this.dotToLon = dotToLon;
        this.titleFrom = titleFrom;
        this.titleTo = titleTo;
    }

    public RouteInfoModel(double dotFromLat, double dotFromLon, double dotToLat, double dotToLon) {
        this.dotFromLat = dotFromLat;
        this.dotFromLon = dotFromLon;
        this.dotToLat = dotToLat;
        this.dotToLon = dotToLon;
    }

    protected RouteInfoModel(Parcel in) {

        dotFromLat = in.readDouble();
        dotFromLon = in.readDouble();
        dotToLat = in.readDouble();
        dotToLon = in.readDouble();
        titleFrom = in.readString();
        titleTo = in.readString();
    }

    public static final Creator<RouteInfoModel> CREATOR = new Creator<RouteInfoModel>() {
        @Override
        public RouteInfoModel createFromParcel(Parcel in) {
            return new RouteInfoModel(in);
        }

        @Override
        public RouteInfoModel[] newArray(int size) {
            return new RouteInfoModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {

        parcel.writeDouble(dotFromLat);
        parcel.writeDouble(dotFromLon);
        parcel.writeDouble(dotToLat);
        parcel.writeDouble(dotToLon);
        parcel.writeString(titleFrom);
        parcel.writeString(titleTo);
    }


    public double getDotToLat() {
        return dotToLat;
    }

    public void setDotToLat(double dotToLat) {
        this.dotToLat = dotToLat;
    }

    public double getDotToLon() {
        return dotToLon;
    }

    public void setDotToLon(double dotToLon) {
        this.dotToLon = dotToLon;
    }

    public String getTitleFrom() {
        return titleFrom;
    }

    public void setTitleFrom(String titleFrom) {
        this.titleFrom = titleFrom;
    }

    public String getTitleTo() {
        return titleTo;
    }

    public void setTitleTo(String titleTo) {
        this.titleTo = titleTo;
    }

    public double getDotFromLat() {
        return dotFromLat;
    }

    public void setDotFromLat(double dotFromLat) {
        this.dotFromLat = dotFromLat;
    }

    public double getDotFromLon() {
        return dotFromLon;
    }

    public void setDotFromLon(double dotFromLon) {
        this.dotFromLon = dotFromLon;
    }
}
