package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DataAvailableServerDAO {
    @Query("DELETE FROM available_servers WHERE keySrv=:keySrv ")
    void delete( String keySrv);

    @Query("SELECT * FROM available_servers WHERE keySrv=:keySrv AND currUid=:currUid")
    DataAvailableServer getAvailableServer(String currUid, String keySrv);

    @Query("select * from available_servers WHERE currUid==:currUid")
    List<DataAvailableServer> getAvailableServers(String currUid);

    @Insert
    long save(DataAvailableServer dataAvailableServer);

    @Update
    int update(DataAvailableServer dataAvailableServer);
}
