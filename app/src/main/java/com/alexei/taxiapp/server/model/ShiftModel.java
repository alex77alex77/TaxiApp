package com.alexei.taxiapp.server.model;

import com.google.firebase.database.ServerValue;

public class ShiftModel {
    private Object timer= ServerValue.TIMESTAMP;
    private int status=-1;

    public ShiftModel() {
    }

    public ShiftModel(int status) {

        this.status = status;
    }

    public ShiftModel(Object timer, int status) {
        this.timer = timer;
        this.status = status;
    }

    public Object getTimer() {
        return timer;
    }

    public void setTimer(Object timer) {
        this.timer = timer;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
