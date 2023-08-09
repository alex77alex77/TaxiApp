package com.alexei.taxiapp.server.model;

public class DataAccessClientModel {
    private String keySender;
    private String services;
    private long timer;

    public DataAccessClientModel() {
    }

    public DataAccessClientModel(String services2, String keySender2) {
        this.services = services2;
        this.keySender = keySender2;
    }

    public String getKeySender() {
        return this.keySender;
    }

    public void setKeySender(String keySender2) {
        this.keySender = keySender2;
    }

    public long getTimer() {
        return this.timer;
    }

    public void setTime(long timer2) {
        this.timer = timer2;
    }

    public String getServices() {
        return this.services;
    }

    public void setServices(String services2) {
        this.services = services2;
    }
}
