package com.alexei.taxiapp.db;

import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Embedded;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;

import com.alexei.taxiapp.driver.model.DataAuto;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.driver.model.ServerModel;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

@Entity(tableName = "setting_drv")
public class SettingDrv {

    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    private long id;

    @ColumnInfo(name = "key_drv")
    private String keyDrv;

    @TypeConverters({converter.class})
    private List<ServerModel> usedProviders;

    @ColumnInfo(name = "sound_notification")
    private boolean soundNotification;

    @ColumnInfo(name = "password")
    private String password;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "name_host")
    private String nameHost;

    @ColumnInfo(name = "phone")
    private String phone;

    @ColumnInfo(name = "access_sos")
    private boolean accessPassSOS;

    @ColumnInfo(name = "voicing_amount")
    private boolean voicingAmount;

    @ColumnInfo(name = "autorun_server")
    private boolean runServer;

    @ColumnInfo(name = "server_use")
    private String server;

    @Embedded
    private DataAuto auto;

    @ColumnInfo(name = "order_criteria")
    private String orderCriteria;

    @Embedded
    private DataLocation dislocation = new DataLocation();

    @ColumnInfo(name = "radius")
    private int radius;

    @ColumnInfo(name = "login_k")
    private String loginK;

    @ColumnInfo(name = "srv_name")
    private String srvName;

    @Ignore
    public SettingDrv() {
    }


    public SettingDrv(String keyDrv, List<ServerModel> usedProviders, boolean soundNotification, String password, String name, String nameHost, String phone,
                      boolean accessPassSOS, boolean voicingAmount, boolean runServer, String server, DataAuto auto, String orderCriteria,
                      DataLocation dislocation, int radius, String loginK, String srvName) {

        this.keyDrv = keyDrv;
        this.usedProviders = usedProviders;
        this.soundNotification = soundNotification;
        this.password = password;
        this.name = name;
        this.nameHost = nameHost;
        this.phone = phone;
        this.accessPassSOS = accessPassSOS;
        this.voicingAmount = voicingAmount;
        this.runServer = runServer;
        this.server = server;
        this.auto = auto;
        this.orderCriteria = orderCriteria;
        this.dislocation = dislocation;
        this.radius = radius;
        this.loginK = loginK;
        this.srvName = srvName;
    }


    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public DataLocation getDislocation() {
        return dislocation;
    }

    public void setDislocation(DataLocation dislocation) {
        this.dislocation = dislocation;
    }

    public String getOrderCriteria() {
        return orderCriteria;
    }

    public void setOrderCriteria(String orderCriteria) {
        this.orderCriteria = orderCriteria;
    }

    public boolean isSoundNotification() {
        return soundNotification;
    }

    public void setSoundNotification(boolean soundNotification) {
        this.soundNotification = soundNotification;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public boolean isAccessPassSOS() {
        return accessPassSOS;
    }

    public void setAccessPassSOS(boolean accessPassSOS) {
        this.accessPassSOS = accessPassSOS;
    }

    public boolean isVoicingAmount() {
        return voicingAmount;
    }

    public void setVoicingAmount(boolean voicingAmount) {
        this.voicingAmount = voicingAmount;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public DataAuto getAuto() {
        return auto;
    }

    public void setAuto(DataAuto auto) {
        this.auto = auto;
    }

    public boolean isRunServer() {
        return runServer;
    }

    public void setRunServer(boolean runServer) {
        this.runServer = runServer;
    }

    public String getNameHost() {
        return nameHost;
    }

    public void setNameHost(String nameHost) {
        this.nameHost = nameHost;
    }

    public int getRadius() {
        return radius;
    }

    public void setRadius(int radius) {
        this.radius = radius;
    }

    public List<ServerModel> getUsedProviders() {
        return usedProviders;
    }

    public void setUsedProviders(List<ServerModel> usedProviders) {
        this.usedProviders = usedProviders;
    }

    public String getKeyDrv() {
        return keyDrv;
    }

    public void setKeyDrv(String keyDrv) {
        this.keyDrv = keyDrv;
    }

    public String getLoginK() {
        return loginK;
    }

    public void setLoginK(String loginK) {
        this.loginK = loginK;
    }

    public String getSrvName() {
        return srvName;
    }

    public void setSrvName(String srvName) {
        this.srvName = srvName;
    }

    //---------------------------------------------

    public static class converter {

        @TypeConverter
        public static String usedProviders(List<ServerModel> list) {
            Gson gson = new GsonBuilder().create();
            return gson.toJson(list);
        }

        @TypeConverter
        public static List<ServerModel> usedProviders(@Nullable String json) {
            if (json == null) {
                return Collections.emptyList();
            }

            Type listType = new TypeToken<List<ServerModel>>() {
            }.getType();

            Gson gson = new GsonBuilder().create();
            return gson.fromJson(json, listType);
        }
    }
}
