package com.alexei.taxiapp.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

import com.alexei.taxiapp.server.model.RouteInfoModel;

@Entity(tableName = "server_reports")
public class ServerReport implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    private String currUId;
    private String keyDrv;
    private String keyOrder;
    @Embedded
    private RouteInfoModel route;
    private int statusOrder;
    private long timerCloseShift;
    private long timerFinish;
    private long timerOpenShift;
    private long timerStart;

    public ServerReport() {
    }

    public ServerReport(String currUId2, String keyDrv2, String keyOrder2, int statusOrder2, long timerFinish2, long timerOpenShift2, long timerCloseShift2) {
        this.currUId = currUId2;
        this.keyDrv = keyDrv2;
        this.keyOrder = keyOrder2;
        this.statusOrder = statusOrder2;
        this.timerFinish = timerFinish2;
        this.timerOpenShift = timerOpenShift2;
        this.timerCloseShift = timerCloseShift2;
    }

    public ServerReport(String currUId2, String keyDrv2, String keyOrder2, RouteInfoModel route2, int statusOrder2, long timerStart2, long timerFinish2, long timerOpenShift2, long timerCloseShift2) {
        this.currUId = currUId2;
        this.keyDrv = keyDrv2;
        this.keyOrder = keyOrder2;
        this.route = route2;
        this.statusOrder = statusOrder2;
        this.timerStart = timerStart2;
        this.timerFinish = timerFinish2;
        this.timerOpenShift = timerOpenShift2;
        this.timerCloseShift = timerCloseShift2;
    }

    protected ServerReport(Parcel in) {
        this.currUId = in.readString();
        this.keyDrv = in.readString();
        this.keyOrder = in.readString();
        this.route = (RouteInfoModel) in.readParcelable(RouteInfoModel.class.getClassLoader());
        this.statusOrder = in.readInt();
        this.timerStart = in.readLong();
        this.timerFinish = in.readLong();
        this.timerOpenShift = in.readLong();
        this.timerCloseShift = in.readLong();
    }

    public static final Creator<ServerReport> CREATOR = new Creator<ServerReport>() {
        public ServerReport createFromParcel(Parcel in) {
            return new ServerReport(in);
        }

        public ServerReport[] newArray(int size) {
            return new ServerReport[size];
        }
    };

    public String getCurrUId() {
        return this.currUId;
    }

    public void setCurrUId(String currUId2) {
        this.currUId = currUId2;
    }

    public String getKeyDrv() {
        return this.keyDrv;
    }

    public void setKeyDrv(String keyDrv2) {
        this.keyDrv = keyDrv2;
    }

    public String getKeyOrder() {
        return this.keyOrder;
    }

    public void setKeyOrder(String keyOrder2) {
        this.keyOrder = keyOrder2;
    }

    public RouteInfoModel getRoute() {
        return this.route;
    }

    public void setRoute(RouteInfoModel route2) {
        this.route = route2;
    }

    public int getStatusOrder() {
        return this.statusOrder;
    }

    public void setStatusOrder(int statusOrder2) {
        this.statusOrder = statusOrder2;
    }

    public long getTimerStart() {
        return this.timerStart;
    }

    public void setTimerStart(long timerStart2) {
        this.timerStart = timerStart2;
    }

    public long getTimerFinish() {
        return this.timerFinish;
    }

    public void setTimerFinish(long timerFinish2) {
        this.timerFinish = timerFinish2;
    }

    public long getTimerOpenShift() {
        return this.timerOpenShift;
    }

    public void setTimerOpenShift(long timerOpenShift2) {
        this.timerOpenShift = timerOpenShift2;
    }

    public long getTimerCloseShift() {
        return this.timerCloseShift;
    }

    public void setTimerCloseShift(long timerCloseShift2) {
        this.timerCloseShift = timerCloseShift2;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeString(this.currUId);
        parcel.writeString(this.keyDrv);
        parcel.writeString(this.keyOrder);
        parcel.writeParcelable(this.route, i);
        parcel.writeInt(this.statusOrder);
        parcel.writeLong(this.timerStart);
        parcel.writeLong(this.timerFinish);
        parcel.writeLong(this.timerOpenShift);
        parcel.writeLong(this.timerCloseShift);
    }
}
