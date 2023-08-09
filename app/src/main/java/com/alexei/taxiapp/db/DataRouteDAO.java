package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DataRouteDAO {
    @Insert
    long add(DataRoute routeModel);

    @Update
    int update(DataRoute routeModel);

    @Query("DELETE FROM routes WHERE u_id=:currUid AND key_order=:keyOrder")
    void deleteRoute(String currUid,String keyOrder);

    @Query("select * from routes WHERE key_order=:keyOrder AND u_id =:currUid")
    DataRoute getDataRoute(String currUid, String keyOrder);

    @Query("SELECT * from routes WHERE u_id=:currUId")
    List<DataRoute> getRoutes(String currUId);

}
