package com.alexei.taxiapp.driver.model;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;

public class DataAuto implements Parcelable {
    public static final Creator<DataAuto> CREATOR = new Creator<DataAuto>() {
        public DataAuto createFromParcel(Parcel in) {
            return new DataAuto(in);
        }

        public DataAuto[] newArray(int size) {
            return new DataAuto[size];
        }
    };
    private String autoColor = "";
    private String autoModel = "";
    private String autoNumber = "";

    public DataAuto() {
    }

    public DataAuto(String autoModel2, String autoColor2, String autoNumber2) {
        this.autoModel = autoModel2;
        this.autoColor = autoColor2;
        this.autoNumber = autoNumber2;
    }

    protected DataAuto(Parcel in) {
        this.autoModel = in.readString();
        this.autoColor = in.readString();
        this.autoNumber = in.readString();
    }

    public String getAutoModel() {
        return this.autoModel;
    }

    public void setAutoModel(String autoModel2) {
        this.autoModel = autoModel2;
    }

    public String getAutoColor() {
        return this.autoColor;
    }

    public void setAutoColor(String autoColor2) {
        this.autoColor = autoColor2;
    }

    public String getAutoNumber() {
        return this.autoNumber;
    }

    public void setAutoNumber(String autoNumber2) {
        this.autoNumber = autoNumber2;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.autoModel);
        parcel.writeString(this.autoColor);
        parcel.writeString(this.autoNumber);
    }

    public String toString() {
        Context context = App.context;
        return context.getString(R.string.t_model) + this.autoModel + "\n" + context.getString(R.string.t_color) + this.autoColor + "\n" + context.getString(R.string.t_reg_num) + this.autoNumber;
    }
}
