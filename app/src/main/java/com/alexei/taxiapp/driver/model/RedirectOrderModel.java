package com.alexei.taxiapp.driver.model;

public class RedirectOrderModel {

    String keyProvider;

    String keyOrder;

    public RedirectOrderModel() {
    }

    public RedirectOrderModel(String keyProvider, String keyOrder) {
        this.keyProvider = keyProvider;
        this.keyOrder = keyOrder;
    }

    public String getKeyProvider() {
        return keyProvider;
    }

    public void setKeyProvider(String keyProvider) {
        this.keyProvider = keyProvider;
    }

    public String getKeyOrder() {
        return keyOrder;
    }

    public void setKeyOrder(String keyOrder) {
        this.keyOrder = keyOrder;
    }
}
