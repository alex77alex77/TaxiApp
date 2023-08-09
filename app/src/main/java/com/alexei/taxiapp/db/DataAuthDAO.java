package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DataAuthDAO {
    @Query("DELETE FROM data_auth WHERE userId=:currUid")
    int delete(String currUid);
//    @Query("select * from data_auth WHERE userId=:currUid")
//    DataAuthModel getDataAuth();
    @Insert
    long saveDataAuth(DataAuthModel dataAuthModel);
    @Update
    void updateDataAuth(DataAuthModel dataAuthModel);
}
