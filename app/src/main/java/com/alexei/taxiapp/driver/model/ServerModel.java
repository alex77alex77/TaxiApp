package com.alexei.taxiapp.driver.model;

import androidx.annotation.NonNull;

public class ServerModel {

    private String keySrv;
    private String name = "";
    private int status = 0;
    private String services = "";
    private String phone = "";

    public ServerModel( String keySrv, String name, int status, String services, String phone) {

        this.keySrv = keySrv;
        this.name = name;
        this.status = status;
        this.services = services;
        this.phone = phone;
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

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    @NonNull
    @Override
    public String toString() {
        return name;
    }
}
