package com.alexei.taxiapp.server.model;

public class DataOrderStatusAndDrvUidModel {
    String driverUid;
    int status;

    public DataOrderStatusAndDrvUidModel() {
    }

    public DataOrderStatusAndDrvUidModel(String driverUid2, int status2) {
        this.driverUid = driverUid2;
        this.status = status2;
    }

    public String getDriverUid() {
        return this.driverUid;
    }

    public void setDriverUid(String driverUid2) {
        this.driverUid = driverUid2;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status2) {
        this.status = status2;
    }
}
