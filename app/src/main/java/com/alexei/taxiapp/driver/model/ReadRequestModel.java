package com.alexei.taxiapp.driver.model;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.alexei.taxiapp.db.InfoRequestConnect;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ReadRequestModel implements Parcelable {
    @TypeConverters({converter.class})
    InfoRequestConnect dataReq;
    String kS ="";

    public ReadRequestModel() {
    }

    public ReadRequestModel(InfoRequestConnect dataReq, String kS) {
        this.dataReq = dataReq;
        this.kS = kS;
    }

    protected ReadRequestModel(Parcel in) {
        dataReq = in.readParcelable(InfoRequestConnect.class.getClassLoader());
        kS = in.readString();
    }

    public static final Creator<ReadRequestModel> CREATOR = new Creator<ReadRequestModel>() {
        @Override
        public ReadRequestModel createFromParcel(Parcel in) {
            return new ReadRequestModel(in);
        }

        @Override
        public ReadRequestModel[] newArray(int size) {
            return new ReadRequestModel[size];
        }
    };

    public InfoRequestConnect getDataReq() {
        return dataReq;
    }

    public void setDataReq(InfoRequestConnect dataReq) {
        this.dataReq = dataReq;
    }

    public String getkS() {
        return kS;
    }

    public void setkS(String kS) {
        this.kS = kS;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(dataReq, i);
        parcel.writeString(kS);
    }

    public static class converter {

        @TypeConverter
        public String dataReq(InfoRequestConnect request) {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(request);
        }

        @TypeConverter
        public InfoRequestConnect dataReq(String json) {
            return new Gson().fromJson(json, InfoRequestConnect.class);
        }
    }
}
