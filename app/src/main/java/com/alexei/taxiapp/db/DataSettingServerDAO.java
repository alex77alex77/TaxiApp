package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DataSettingServerDAO {
    @Insert
    long addSettingServer(SettingServer settingServer);

    @Query("select * from setting_server WHERE serverUid=:userUid")
    SettingServer getSetting(String userUid);

    @Update
    void updateSettingServer(SettingServer settingServer);
}
