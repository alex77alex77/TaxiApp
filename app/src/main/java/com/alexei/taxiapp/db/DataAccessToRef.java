package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "data_access_ref")
public class DataAccessToRef {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "ref_u_id")
    private String refUid;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "password")
    private String password;

    @ColumnInfo(name = "mode")
    private int mode;

    @Ignore
    public DataAccessToRef() {
    }

    public DataAccessToRef( String refUid, String name, String password, int mode) {

        this.refUid = refUid;
        this.name = name;
        this.password = password;
        this.mode = mode;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getRefUid() {
        return refUid;
    }

    public void setRefUid(String refUid) {
        this.refUid = refUid;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }
}
