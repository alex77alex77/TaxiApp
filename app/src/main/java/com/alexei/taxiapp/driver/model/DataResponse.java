package com.alexei.taxiapp.driver.model;

public class DataResponse {
    private int response;

    /* renamed from: ts */
    private Object f6ts;

    public DataResponse() {
    }

    public DataResponse(int response2, Object ts) {
        this.response = response2;
        this.f6ts = ts;
    }

    public int getResponse() {
        return this.response;
    }

    public void setResponse(int response2) {
        this.response = response2;
    }

    public Object getTs() {
        return this.f6ts;
    }

    public void setTs(Object ts) {
        this.f6ts = ts;
    }
}
