package com.alexei.taxiapp.server.model;

import com.alexei.taxiapp.util.Util;

public class DriverCreateToServerModel {
    private int status = 0;
    private MsgModel msgS = new MsgModel();
    private MsgModel msgD = new MsgModel();
    private ShiftModel ws = new ShiftModel(Util.SHIFT_CLOSE_DRV_STATUS);


    public DriverCreateToServerModel() {

    }

    public DriverCreateToServerModel(int status) {
        this.status = status;
    }

    public DriverCreateToServerModel(int status, MsgModel msgForSrv, MsgModel msgForDrv, ShiftModel ws) {
        this.status = status;
        this.msgS = msgForSrv;
        this.msgD = msgForDrv;

        this.ws = ws;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public MsgModel getMsgS() {
        return msgS;
    }

    public void setMsgS(MsgModel msgS) {
        this.msgS = msgS;
    }

    public MsgModel getMsgD() {
        return msgD;
    }

    public void setMsgD(MsgModel msgD) {
        this.msgD = msgD;
    }

    public ShiftModel getWs() {
        return ws;
    }

    public void setWs(ShiftModel ws) {
        this.ws = ws;
    }
}
