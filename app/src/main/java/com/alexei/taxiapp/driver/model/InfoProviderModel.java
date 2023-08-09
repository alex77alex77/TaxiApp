package com.alexei.taxiapp.driver.model;

import com.google.firebase.database.DatabaseReference;

public class InfoProviderModel {
    private String desc;
    private String nameSrv;
    private DatabaseReference refSrv;
    private int state;
    private int status;

    public InfoProviderModel() {
    }

    public InfoProviderModel(DatabaseReference refSrv2, String nameSrv2, int status2, String desc2, int state2) {
        this.refSrv = refSrv2;
        this.nameSrv = nameSrv2;
        this.status = status2;
        this.desc = desc2;
        this.state = state2;
    }

    public DatabaseReference getRefSrv() {
        return this.refSrv;
    }

    public void setRefSrv(DatabaseReference refSrv2) {
        this.refSrv = refSrv2;
    }

    public String getNameSrv() {
        return this.nameSrv;
    }

    public void setNameSrv(String nameSrv2) {
        this.nameSrv = nameSrv2;
    }

    public int getStatus() {
        return this.status;
    }

    public void setStatus(int status2) {
        this.status = status2;
    }

    public String getDesc() {
        return this.desc;
    }

    public void setDesc(String desc2) {
        this.desc = desc2;
    }

    public int getState() {
        return this.state;
    }

    public void setState(int state2) {
        this.state = state2;
    }
}
