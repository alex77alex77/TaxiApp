package com.alexei.taxiapp.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.alexei.taxiapp.driver.model.DataAuto;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.server.model.MsgModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Entity(tableName = "connect_requests")
public class InfoRequestConnect implements Parcelable {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @Ignore
    private DataAuto auto;

    @ColumnInfo(name = "key")
    private String keySrv = "";
    @ColumnInfo(name = "name")
    private String name = "";
    @ColumnInfo(name = "title_host")
    private String title = "";
    @ColumnInfo(name = "phone")
    private String phone = "";
    @ColumnInfo(name = "type_transport")
    private String typeTr = "";
    @ColumnInfo(name = "key_drv")
    private String keyS = "";

    @Embedded
    private DataLocation disl = new DataLocation();
    @TypeConverters({converter.class})
    private Object ts = 0;


    public InfoRequestConnect() {
    }

    public InfoRequestConnect(String keySrv, String name, String title, String phone, String typeTr,
                              DataLocation disl, Object timestamp, String keyS) {

        this.keySrv = keySrv;
        this.name = name;
        this.title = title;
        this.phone = phone;
        this.typeTr = typeTr;
        this.disl = disl;

        this.ts = timestamp;
        this.keyS = keyS;
    }

    public InfoRequestConnect(DataAuto auto, String keySrv, String name, String title, String phone, String typeTr,
                              DataLocation disl, Object timestamp, String keyS) {

        this.auto = auto;
        this.keySrv = keySrv;
        this.name = name;
        this.title = title;
        this.phone = phone;
        this.typeTr = typeTr;
        this.disl = disl;

        this.ts = timestamp;
        this.keyS = keyS;
    }

    protected InfoRequestConnect(Parcel in) {
        auto = in.readParcelable(DataAuto.class.getClassLoader());
        keySrv = in.readString();
        name = in.readString();
        title = in.readString();
        phone = in.readString();
        typeTr = in.readString();
        disl = in.readParcelable(DataLocation.class.getClassLoader());

        ts = in.readValue(Object.class.getClassLoader());
        keyS = in.readString();
    }

    public static final Creator<InfoRequestConnect> CREATOR = new Creator<InfoRequestConnect>() {
        @Override
        public InfoRequestConnect createFromParcel(Parcel in) {
            return new InfoRequestConnect(in);
        }

        @Override
        public InfoRequestConnect[] newArray(int size) {
            return new InfoRequestConnect[size];
        }
    };

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DataAuto getAuto() {
        return auto;
    }

    public void setAuto(DataAuto auto) {
        this.auto = auto;
    }

    public String getKeySrv() {
        return keySrv;
    }

    public void setKeySrv(String keySrv) {
        this.keySrv = keySrv;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getTypeTr() {
        return typeTr;
    }

    public void setTypeTr(String typeTr) {
        this.typeTr = typeTr;
    }

    public String getKeyS() {
        return keyS;
    }

    public void setKeyS(String keyS) {
        this.keyS = keyS;
    }

    public DataLocation getDisl() {
        return disl;
    }

    public void setDisl(DataLocation disl) {
        this.disl = disl;
    }

    public Object getTs() {
        return ts;
    }

    public void setTs(Object ts) {
        this.ts = ts;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeParcelable(auto, i);
        parcel.writeString(keySrv);
        parcel.writeString(name);
        parcel.writeString(title);
        parcel.writeString(phone);
        parcel.writeString(typeTr);
        parcel.writeParcelable(disl, i);
        parcel.writeValue(ts);
        parcel.writeString(keyS);
    }
    public static class converter {
        @TypeConverter
        public String timestamp(Object object) {
            return new GsonBuilder().create().toJson(object);
        }
        @TypeConverter
        public Object timestamp(String json) {
            return new Gson().fromJson(json, Object.class);
        }
    }
}
