package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DataSettingDrvDAO {

    @Insert
    long saveDataSetting(SettingDrv settingDrv);

    @Update
    int updateDataSetting(SettingDrv settingDrv);

    @Query("select * from setting_drv WHERE key_drv=:keyDrv")
    SettingDrv getSetting(String keyDrv);

    @Query("SELECT sound_notification FROM setting_drv WHERE key_drv=:keyDrv")
    boolean getSound(String keyDrv);

    @Query("SELECT access_sos FROM setting_drv WHERE key_drv=:keyDrv")
    boolean getAccessSOS(String keyDrv);

    @Query("SELECT voicing_amount FROM setting_drv WHERE key_drv=:keyDrv")
    boolean getVoicingAmount(String keyDrv);

    @Query("UPDATE setting_drv SET password = :password WHERE key_drv=:keyDrv")
    void setPassword(String password,String keyDrv);

    @Query("SELECT password FROM setting_drv WHERE key_drv=:keyDrv")
    String getPassword(String keyDrv);

    @Query("SELECT EXISTS (SELECT 1 FROM setting_drv WHERE key_drv=:keyDrv)")
    boolean isExists(String keyDrv);

    @Query("DELETE FROM setting_drv")
    int delete();

}
