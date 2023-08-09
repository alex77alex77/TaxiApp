package com.alexei.taxiapp.driver.model;

import com.alexei.taxiapp.db.InfoRequestConnect;

public class DeniedDrvModel {
  private InfoRequestConnect dataReq;
  private String errDescription;
  private long exCallSign;

  public DeniedDrvModel(InfoRequestConnect dataReq2, long exCallSign2, String errDescription2) {
    this.exCallSign = exCallSign2;
    this.errDescription = errDescription2;
  }

  public InfoRequestConnect getDataReq() {
    return this.dataReq;
  }

  public void setDataReq(InfoRequestConnect dataReq2) {
    this.dataReq = dataReq2;
  }

  public long getExCallSign() {
    return this.exCallSign;
  }

  public void setExCallSign(long exCallSign2) {
    this.exCallSign = exCallSign2;
  }

  public String getErrDescription() {
    return this.errDescription;
  }

  public void setErrDescription(String errDescription2) {
    this.errDescription = errDescription2;
  }
}
