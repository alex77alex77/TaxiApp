package com.alexei.taxiapp.db;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.alexei.taxiapp.driver.model.DataLocation;
import com.google.android.gms.maps.model.LatLng;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Entity(tableName = "routes")
public class DataRoute {
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "u_id")
    private String userId;

    @ColumnInfo(name = "key_order")
    private String keyOrder;

    @ColumnInfo(name = "time")
    private long time;

    @TypeConverters({converter.class})
    private List<LatLng> dots;

    @TypeConverters({converter.class})
    private DataLocation from = new DataLocation();
    @TypeConverters({converter.class})
    private DataLocation to = new DataLocation();


    public DataRoute() {
    }

    public DataRoute(String userId, String keyOrder, long time, List<LatLng> dots,DataLocation from,DataLocation to) {
        this.userId = userId;
        this.keyOrder = keyOrder;
        this.time = time;
        this.dots = dots;
        this.from=from;
        this.to=to;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getKeyOrder() {
        return keyOrder;
    }

    public void setKeyOrder(String keyOrder) {
        this.keyOrder = keyOrder;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public List<LatLng> getDots() {
        return dots;
    }

    public void setDots(List<LatLng> dots) {
        this.dots = dots;
    }

    public DataLocation getFrom() {
        return from;
    }

    public void setFrom(DataLocation from) {
        this.from = from;
    }

    public DataLocation getTo() {
        return to;
    }

    public void setTo(DataLocation to) {
        this.to = to;
    }

    //-------------------------------------------------------
    public static class converter {

        @TypeConverter
        public static String dots(List<LatLng> list) {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(list);
        }

        @TypeConverter
        public static List<LatLng> dots(@Nullable String json) {
            if (json == null) {
                return Collections.emptyList();
            }

            Type listType = new TypeToken<List<LatLng>>() {
            }.getType();

            Gson gson = new GsonBuilder().create();
            return gson.fromJson(json, listType);
        }

        @TypeConverter
        public String location(DataLocation location) {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(location);
        }

        @TypeConverter
        public DataLocation location(String json) {
            return new Gson().fromJson(json, DataLocation.class);
        }
    }
}
