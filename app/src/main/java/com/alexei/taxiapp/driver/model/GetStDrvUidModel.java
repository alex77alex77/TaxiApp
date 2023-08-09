package com.alexei.taxiapp.driver.model;

public class GetStDrvUidModel {
    private String driverUid;
    private int status;

    public GetStDrvUidModel() {
    }

    public GetStDrvUidModel(String driverUid2, int status2) {
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
