package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DataTaximeterDAO {
    @Insert
    long addDataTaximeter(DataTaximeter dataTaximeter);

    @Update
    void updateDataTaximeter(DataTaximeter dataTaximeter);

    @Query("DELETE FROM taximeters WHERE orderKey=:keyOrder AND keyDrv=:currUid")
    void deleteDataTaximeter(String keyOrder,String currUid);

    @Query("select * from taximeters where startTime >=:timerFrom AND startTime <:timerTo AND keyDrv=:keyDrv")
    List<DataTaximeter> getByDateTaximeter(long timerFrom, long timerTo,String keyDrv);

    @Query("select * from taximeters where keyDrv=:currUid")
    List<DataTaximeter> getAllTaximeter(String currUid);

    @Query("select * from taximeters where orderKey ==:orderKey AND keyDrv=:keyDrv")
    DataTaximeter getTaximeter(String orderKey,String keyDrv);
}
