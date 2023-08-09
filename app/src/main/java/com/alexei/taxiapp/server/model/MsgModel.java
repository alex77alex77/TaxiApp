package com.alexei.taxiapp.server.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.database.ServerValue;

public class MsgModel implements Parcelable {

    private Object createTime = 0;//ServerValue.TIMESTAMP
    private String msg = "";

    public MsgModel() {
    }

    public MsgModel(Object createTime, String msg) {
        this.createTime = createTime;
        this.msg = msg;
    }

    public MsgModel(String msg) {
        this.createTime=ServerValue.TIMESTAMP;
        this.msg = msg;
    }

    protected MsgModel(Parcel in) {
        msg = in.readString();
    }

    public static final Creator<MsgModel> CREATOR = new Creator<MsgModel>() {
        @Override
        public MsgModel createFromParcel(Parcel in) {
            return new MsgModel(in);
        }

        @Override
        public MsgModel[] newArray(int size) {
            return new MsgModel[size];
        }
    };

    public Object getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Object createTime) {
        this.createTime = createTime;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(msg);
    }
}
