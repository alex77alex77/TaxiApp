package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "block_clients")
public class DataBlockClient {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    private int block;
    private String currUId;
    private String keyClient;
    private String name;

    public DataBlockClient() {
    }

    public DataBlockClient(String currUId2, String name2, String keyClient2, int block2) {
        this.currUId = currUId2;
        this.name = name2;
        this.keyClient = keyClient2;
        this.block = block2;
    }

    public long getId() {
        return this.id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getCurrUId() {
        return this.currUId;
    }

    public void setCurrUId(String currUId2) {
        this.currUId = currUId2;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name2) {
        this.name = name2;
    }

    public int getBlock() {
        return this.block;
    }

    public void setBlock(int block2) {
        this.block = block2;
    }

    public String getKeyClient() {
        return keyClient;
    }

    public void setKeyClient(String keyClient) {
        this.keyClient = keyClient;
    }
}
