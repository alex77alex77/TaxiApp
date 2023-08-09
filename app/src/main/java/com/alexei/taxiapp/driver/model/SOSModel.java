package com.alexei.taxiapp.driver.model;

import com.google.firebase.database.ServerValue;

public class SOSModel {
    private Object time = ServerValue.TIMESTAMP;

    public SOSModel() {
    }

    public Object getTime() {
        return time;
    }

    public void setTime(Object time) {
        this.time = time;
    }

}
