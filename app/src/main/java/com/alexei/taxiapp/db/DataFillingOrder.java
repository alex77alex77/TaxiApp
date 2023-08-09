package com.alexei.taxiapp.db;

import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.server.model.MsgModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Entity(tableName = "filling_order")
public class DataFillingOrder {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;
    @TypeConverters({converter.class})
    private InfoOrder fillingOrder;
    private String keyProvider;


    public DataFillingOrder() {
    }

    public DataFillingOrder(InfoOrder fillingOrder, String keyProvider) {
        this.fillingOrder = fillingOrder;
        this.keyProvider = keyProvider;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public InfoOrder getFillingOrder() {
        return fillingOrder;
    }

    public void setFillingOrder(InfoOrder fillingOrder) {
        this.fillingOrder = fillingOrder;
    }

    public String getKeyProvider() {
        return keyProvider;
    }

    public void setKeyProvider(String keyProvider) {
        this.keyProvider = keyProvider;
    }

    public static class converter {

        @TypeConverter
        public String infoOrder(InfoOrder infoOrder) {
            return new GsonBuilder().create().toJson(infoOrder);
        }

        @TypeConverter
        public InfoOrder infoOrder(String json) {
            return new Gson().fromJson(json, InfoOrder.class);
        }
    }
}
