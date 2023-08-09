package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "available_servers")
public class DataAvailableServer {
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  private long id;
  private String currUid;
  private String keySrv;
  private int status;

  public DataAvailableServer() {
  }

  public DataAvailableServer(String keySrv2, int status2, String currUid2) {
    this.keySrv = keySrv2;
    this.status = status2;
    this.currUid = currUid2;
  }

  public long getId() {
    return this.id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getKeySrv() {
    return this.keySrv;
  }

  public void setKeySrv(String keySrv2) {
    this.keySrv = keySrv2;
  }

  public int getStatus() {
    return this.status;
  }

  public void setStatus(int status2) {
    this.status = status2;
  }

  public String getCurrUid() {
    return this.currUid;
  }

  public void setCurrUid(String currUid2) {
    this.currUid = currUid2;
  }
}
