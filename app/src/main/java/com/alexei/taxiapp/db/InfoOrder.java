package com.alexei.taxiapp.db;

import android.os.Parcel;
import android.os.Parcelable;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.driver.model.RateModel;
import com.alexei.taxiapp.server.model.MsgModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
@Entity(tableName = "orders")
public class InfoOrder implements Parcelable {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    @TypeConverters({converter.class})
    private Object timestamp = 0;

    private long timeF = 0;
    @Embedded
    private RateModel rate;

    @TypeConverters({converter.class})
    private DataLocation from = new DataLocation();
    @TypeConverters({converter.class})
    private DataLocation to = new DataLocation();

    private String note = "";
    private String phone = "";
    private String clientUid = "";
    private String driverUid = "";
    private String senderKey = "";

    @TypeConverters({converter.class})
    private MsgModel msgC = new MsgModel();
    @TypeConverters({converter.class})
    private MsgModel msgD = new MsgModel();
    @TypeConverters({converter.class})
    private MsgModel msgS = new MsgModel();

    private String keyOrder = "";
    private int status = 5;
    private String typeTr = "";

    private String clientName = "";
    private String distanceToClient = "";

    @TypeConverters({converter.class})
    private DataLocation endRoute = new DataLocation();

    private String providerKey = "";
    private String providerName = "";
    private String dataAuto = "";

    public InfoOrder() {
    }

    public InfoOrder(Object timestamp2,
                     long timeF2,
                     RateModel rate2,
                     DataLocation from2,
                     DataLocation to,
                     String note2,
                     String phone2,
                     String clientUid2,
                     String driverUid2,
                     String senderKey2,
                     MsgModel msgForDrv,
                     MsgModel msgForClient,
                     MsgModel msgForSrv,
                     String keyOrder2,
                     int status2,
                     String typeTr2) {
        this.timestamp = timestamp2;
        this.timeF = timeF2;
        this.rate = rate2;
        this.from = from2;
        this.to = to;
        this.note = note2;
        this.phone = phone2;
        this.clientUid = clientUid2;
        this.driverUid = driverUid2;
        this.senderKey = senderKey2;
        this.msgD = msgForDrv;
        this.msgC = msgForClient;
        this.msgS = msgForSrv;
        this.keyOrder = keyOrder2;
        this.status = status2;
        this.typeTr = typeTr2;
    }

    protected InfoOrder(Parcel in) {
        this.timestamp = in.readValue(Object.class.getClassLoader());
        this.timeF = in.readLong();
        this.rate = (RateModel) in.readParcelable(RateModel.class.getClassLoader());
        this.from = (DataLocation) in.readParcelable(DataLocation.class.getClassLoader());
        this.to = (DataLocation) in.readParcelable(DataLocation.class.getClassLoader());
        this.note = in.readString();
        this.phone = in.readString();
        this.clientUid = in.readString();
        this.driverUid = in.readString();
        this.senderKey = in.readString();
        this.msgD = (MsgModel) in.readParcelable(MsgModel.class.getClassLoader());
        this.msgC = (MsgModel) in.readParcelable(MsgModel.class.getClassLoader());
        this.msgS = (MsgModel) in.readParcelable(MsgModel.class.getClassLoader());
        this.keyOrder = in.readString();
        this.status = in.readInt();
        this.typeTr = in.readString();
        this.providerName = in.readString();
        this.providerKey = in.readString();
        this.distanceToClient = in.readString();
        this.dataAuto = in.readString();
    }

    public int describeContents() {
        return 0;
    }

    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeValue(this.timestamp);
        parcel.writeLong(this.timeF);
        parcel.writeParcelable(this.rate, i);
        parcel.writeParcelable(this.from, i);
        parcel.writeParcelable(this.to, i);
        parcel.writeString(this.note);
        parcel.writeString(this.phone);
        parcel.writeString(this.clientUid);
        parcel.writeString(this.driverUid);
        parcel.writeString(this.senderKey);
        parcel.writeParcelable(this.msgD, i);
        parcel.writeParcelable(this.msgC, i);
        parcel.writeParcelable(this.msgS, i);
        parcel.writeString(this.keyOrder);
        parcel.writeInt(this.status);
        parcel.writeString(this.typeTr);
        parcel.writeString(this.providerName);
        parcel.writeString(this.providerKey);
        parcel.writeString(this.distanceToClient);
        parcel.writeString(this.dataAuto);
    }

    public static final Creator<InfoOrder> CREATOR = new Creator<InfoOrder>() {
        public InfoOrder createFromParcel(Parcel in) {
            return new InfoOrder(in);
        }

        public InfoOrder[] newArray(int size) {
            return new InfoOrder[size];
        }
    };

    public DataLocation getFrom() {
        return this.from;
    }

    public void setFrom(DataLocation from2) {
        this.from = from2;
    }

    public DataLocation getTo() {
        return this.to;
    }

    public void setTo(DataLocation to) {
        this.to = to;
    }

    public DataLocation getEndRoute() {
        return this.endRoute;
    }

    public void setEndRoute(DataLocation coordinatesEndRoute) {
        this.endRoute = coordinatesEndRoute;
    }

    public String getProviderKey() {
        return this.providerKey;
    }

    public void setProviderKey(String providerKey2) {
        this.providerKey = providerKey2;
    }

    public String getProviderName() {
        return this.providerName;
    }

    public void setProviderName(String providerName2) {
        this.providerName = providerName2;
    }

    public String getDataAuto() {
        return this.dataAuto;
    }

    public void setDataAuto(String dataAuto2) {
        this.dataAuto = dataAuto2;
    }

    public String getDistanceToClient() {
        return this.distanceToClient;
    }

    public void setDistanceToClient(String distanceToClient2) {
        this.distanceToClient = distanceToClient2;
    }

    public Object getTimestamp() {
        return this.timestamp;
    }

    public void setTimestamp(Object timestamp2) {
        this.timestamp = timestamp2;
    }

    public long getTimeF() {
        return this.timeF;
    }

    public void setTimeF(long timeF2) {
        this.timeF = timeF2;
    }

    public RateModel getRate() {
        return this.rate;
    }

    public void setRate(RateModel rate2) {
        this.rate = rate2;
    }

    public String getKeyOrder() {
        return this.keyOrder;
    }

    public void setKeyOrder(String keyOrder2) {
        this.keyOrder = keyOrder2;
    }

    public MsgModel getMsgD() {
        return this.msgD;
    }

    public void setMsgD(MsgModel msgForDrv) {
        this.msgD = msgForDrv;
    }

    public MsgModel getMsgC() {
        return this.msgC;
    }

    public void setMsgC(MsgModel msgForClient) {
        this.msgC = msgForClient;
    }

    public MsgModel getMsgS() {
        return this.msgS;
    }

    public void setMsgS(MsgModel msgForSrv) {
        this.msgS = msgForSrv;
    }

    public String getClientUid() {
        return this.clientUid;
    }

    public void setClientUid(String clientUid2) {
        this.clientUid = clientUid2;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String note2) {
        this.note = note2;
    }

    public String getDriverUid() {
        return this.driverUid;
    }

    public void setDriverUid(String driverUid2) {
        this.driverUid = driverUid2;
    }

    public String getSenderKey() {
        return this.senderKey;
    }

    public void setSenderKey(String senderKey2) {
        this.senderKey = senderKey2;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone2) {
        this.phone = phone2;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status2) {
        this.status = status2;
    }

    public String getTypeTr() {
        return this.typeTr;
    }

    public void setTypeTr(String typeTr2) {
        this.typeTr = typeTr2;
    }

    public String getClientName() {
        return this.clientName;
    }

    public void setClientName(String clientName2) {
        this.clientName = clientName2;
    }

    public String toString() {
        return this.keyOrder;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public static class converter {
        @TypeConverter
        public String location(DataLocation location) {
            return new GsonBuilder().create().toJson((Object) location);
        }
        @TypeConverter
        public DataLocation location(String json) {
            return (DataLocation) new Gson().fromJson(json, DataLocation.class);
        }

        @TypeConverter
        public String msg(MsgModel msgModel) {
            return new GsonBuilder().create().toJson((Object) msgModel);
        }
        @TypeConverter
        public MsgModel msg(String json) {
            return (MsgModel) new Gson().fromJson(json, MsgModel.class);
        }

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
