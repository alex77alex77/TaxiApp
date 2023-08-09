package com.alexei.taxiapp.client.model;

import com.alexei.taxiapp.driver.model.InfoServerModel;

public class AvailableProviderModel {
    private String keySrv;
    private InfoServerModel infoServer;

    public AvailableProviderModel() {
    }

    public AvailableProviderModel(String keySrv, InfoServerModel infoServer) {
        this.keySrv = keySrv;
        this.infoServer = infoServer;
    }

    public String getKeySrv() {
        return keySrv;
    }

    public void setKeySrv(String keySrv) {
        this.keySrv = keySrv;
    }

    public InfoServerModel getInfoServer() {
        return infoServer;
    }

    public void setInfoServer(InfoServerModel infoServer) {
        this.infoServer = infoServer;
    }
}

