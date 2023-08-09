package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "orders_providers")
public class DataOrdersProvider {
  @PrimaryKey(autoGenerate = true)
  @ColumnInfo(name = "id")
  private long id;
  @ColumnInfo(name = "curr_u_id")
  private String currUid;
  @ColumnInfo(name = "provider_key")
  private String providerKey;
  @ColumnInfo(name = "status")
  private int status;

  public DataOrdersProvider() {
  }

  public DataOrdersProvider(String currUid2, String providerKey2, int status2) {
    this.currUid = currUid2;
    this.providerKey = providerKey2;
    this.status = status2;
  }

  public long getId() {
    return this.id;
  }

  public void setId(long id) {
    this.id = id;
  }

  public String getCurrUid() {
    return this.currUid;
  }

  public void setCurrUid(String currUid2) {
    this.currUid = currUid2;
  }

  public String getProviderKey() {
    return this.providerKey;
  }

  public void setProviderKey(String providerKey2) {
    this.providerKey = providerKey2;
  }

  public int getStatus() {
    return this.status;
  }

  public void setStatus(int status2) {
    this.status = status2;
  }
}
