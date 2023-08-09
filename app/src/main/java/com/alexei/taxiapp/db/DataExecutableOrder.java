package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.alexei.taxiapp.driver.model.DataLocale;

@Entity(tableName = "executable_order")
public class DataExecutableOrder {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "key_order")
    private String keyOrder;
    @ColumnInfo(name = "key_srv")
    private String keySrv;
    @Embedded
    private DataLocale locale;
    @ColumnInfo(name = "name_srv")
    private String nameSrv;
    @ColumnInfo(name = "curr_u_id")
    private String currUid;
    @Ignore
    public DataExecutableOrder() {
    }

    public DataExecutableOrder(long id, String keyOrder, String keySrv, DataLocale locale,String nameSrv,String currUid) {
        this.id = id;
        this.keyOrder = keyOrder;
        this.keySrv = keySrv;
        this.locale = locale;
        this.nameSrv=nameSrv;
        this.currUid=currUid;

    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getKeyOrder() {
        return keyOrder;
    }

    public void setKeyOrder(String keyOrder) {
        this.keyOrder = keyOrder;
    }

    public String getKeySrv() {
        return keySrv;
    }

    public void setKeySrv(String keySrv) {
        this.keySrv = keySrv;
    }

    public DataLocale getLocale() {
        return locale;
    }

    public void setLocale(DataLocale locale) {
        this.locale = locale;
    }

    public String getNameSrv() {
        return nameSrv;
    }

    public void setNameSrv(String nameSrv) {
        this.nameSrv = nameSrv;
    }

    public String getCurrUid() {
        return currUid;
    }

    public void setCurrUid(String currUid) {
        this.currUid = currUid;
    }
}
