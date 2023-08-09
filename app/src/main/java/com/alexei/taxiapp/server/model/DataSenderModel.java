package com.alexei.taxiapp.server.model;

public class DataSenderModel {
  private String keySender;
  private String push;
  private long timer;

  public DataSenderModel() {
  }

  public DataSenderModel(String keySender2, String push2, long timer2) {
    this.keySender = keySender2;
    this.push = push2;
    this.timer = timer2;
  }

  public String getKeySender() {
    return this.keySender;
  }

  public void setKeySender(String keySender2) {
    this.keySender = keySender2;
  }

  public String getPush() {
    return this.push;
  }

  public void setPush(String push2) {
    this.push = push2;
  }

  public long getTimer() {
    return this.timer;
  }

  public void setTimer(long timer2) {
    this.timer = timer2;
  }
}
