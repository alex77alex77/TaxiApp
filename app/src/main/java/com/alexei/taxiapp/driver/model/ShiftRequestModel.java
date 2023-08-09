package com.alexei.taxiapp.driver.model;

public class ShiftRequestModel {
    int action;

    /* renamed from: kD */
    String f19kD;

    public ShiftRequestModel() {
    }

    public ShiftRequestModel(String kD, int action2) {
        this.f19kD = kD;
        this.action = action2;
    }

    public String getkD() {
        return this.f19kD;
    }

    public void setkD(String kD) {
        this.f19kD = kD;
    }

    public int getAction() {
        return this.action;
    }

    public void setAction(int action2) {
        this.action = action2;
    }
}
