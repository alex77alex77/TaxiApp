package com.alexei.taxiapp.driver.model;

public class ExOrderModel {
  String keyOrder = "";
  String keySrv = "";

  public ExOrderModel() {
  }

  public ExOrderModel(String keySrv2, String keyOrder2) {
    this.keySrv = keySrv2;
    this.keyOrder = keyOrder2;
  }

  public String getKeySrv() {
    return this.keySrv;
  }

  public void setKeySrv(String keySrv2) {
    this.keySrv = keySrv2;
  }

  public String getKeyOrder() {
    return this.keyOrder;
  }

  public void setKeyOrder(String keyOrder2) {
    this.keyOrder = keyOrder2;
  }
}
