package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DataBlockClientDAO {
    @Query("DELETE FROM block_clients WHERE currUid=:keyClient AND currUid=:currUid")
    void delete(String keyClient, String currUid);

    @Query("SELECT * FROM block_clients WHERE keyClient=:keyClient AND currUid=:currUid")
    DataBlockClient getKeyBlock(String keyClient, String currUid);

    @Insert
    long save(DataBlockClient dataAcceptClient);

    @Update
    int update(DataBlockClient dataAcceptClient);
}
