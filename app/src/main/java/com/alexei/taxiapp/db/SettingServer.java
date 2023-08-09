package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "setting_server")
public class SettingServer {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    private boolean chkAcceptOrder;
    private boolean chkAvailable;
    private boolean chkConnect;
    private boolean chkDisableReq;
    private String contactPhone;
    private int hoursCount;

    private float rateShift;
    private String serverName;
    private String serverUid;
    private String typesServices;
    private int waitAccept = 12;
    private int waitAccumulation = 4;

    public SettingServer() {
    }

    public SettingServer(SettingServer settingSrv) {
        this.rateShift = settingSrv.rateShift;
        this.serverUid = settingSrv.serverUid;
        this.serverName = settingSrv.serverName;
        this.chkConnect = settingSrv.chkConnect;
        this.chkDisableReq = settingSrv.chkDisableReq;
        this.hoursCount = settingSrv.hoursCount;
        this.waitAccumulation = settingSrv.waitAccumulation;
        this.waitAccept = settingSrv.waitAccept;
        this.typesServices = settingSrv.typesServices;
        this.chkAvailable = settingSrv.chkAvailable;
        this.chkAcceptOrder = settingSrv.chkAcceptOrder;
        this.contactPhone = settingSrv.contactPhone;
    }

    public SettingServer(float rateShift2, String serverUid2, String serverName2, boolean chkConnect2, boolean chkDisableReq2, int hoursCount2, int waitAccumulation2, int waitAccept2, String typesServices2, boolean chkAvailable2, boolean chkAcceptOrder2, String contactPhone2) {
        this.rateShift = rateShift2;
        this.serverUid = serverUid2;
        this.serverName = serverName2;
        this.chkConnect = chkConnect2;
        this.chkDisableReq = chkDisableReq2;
        this.hoursCount = hoursCount2;
        this.waitAccumulation = waitAccumulation2;
        this.waitAccept = waitAccept2;
        this.typesServices = typesServices2;
        this.chkAvailable = chkAvailable2;
        this.chkAcceptOrder = chkAcceptOrder2;
        this.contactPhone = contactPhone2;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public float getRateShift() {
        return this.rateShift;
    }

    public void setRateShift(float rateShift2) {
        this.rateShift = rateShift2;
    }

    public String getServerUid() {
        return this.serverUid;
    }

    public void setServerUid(String serverUid2) {
        this.serverUid = serverUid2;
    }

    public boolean isChkConnect() {
        return this.chkConnect;
    }

    public void setChkConnect(boolean chkConnect2) {
        this.chkConnect = chkConnect2;
    }

    public boolean isChkDisableReq() {
        return this.chkDisableReq;
    }

    public void setChkDisableReq(boolean chkDisableReq2) {
        this.chkDisableReq = chkDisableReq2;
    }

    public int getHoursCount() {
        return this.hoursCount;
    }

    public void setHoursCount(int hoursCount2) {
        this.hoursCount = hoursCount2;
    }

    public int getWaitAccumulation() {
        return this.waitAccumulation;
    }

    public void setWaitAccumulation(int waitAccumulation2) {
        this.waitAccumulation = waitAccumulation2;
    }

    public String getTypesServices() {
        return this.typesServices;
    }

    public void setTypesServices(String typesServices2) {
        this.typesServices = typesServices2;
    }

    public boolean isChkAvailable() {
        return this.chkAvailable;
    }

    public void setChkAvailable(boolean chkAvailable2) {
        this.chkAvailable = chkAvailable2;
    }

    public String getServerName() {
        return this.serverName;
    }

    public void setServerName(String serverName2) {
        this.serverName = serverName2;
    }

    public int getWaitAccept() {
        return this.waitAccept;
    }

    public void setWaitAccept(int waitAccept2) {
        this.waitAccept = waitAccept2;
    }

    public boolean isChkAcceptOrder() {
        return this.chkAcceptOrder;
    }

    public void setChkAcceptOrder(boolean chkAcceptOrder2) {
        this.chkAcceptOrder = chkAcceptOrder2;
    }

    public String getContactPhone() {
        return this.contactPhone;
    }

    public void setContactPhone(String contactPhone2) {
        this.contactPhone = contactPhone2;
    }
}
