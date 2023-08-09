package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DataExecutableOrderDAO {
    @Insert
    long add(DataExecutableOrder dataExecutableOrder);

    @Update
    int update(DataExecutableOrder dataExecutableOrder);

    @Query("DELETE FROM executable_order WHERE curr_u_id=:currUid")
    void deleteDataExOrder(String currUid);

    @Query("select * from executable_order WHERE curr_u_id =:currUid")
    DataExecutableOrder getExecutableOrder(String currUid);
}
