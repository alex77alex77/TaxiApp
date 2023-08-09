package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DataRatesSrvDAO {
    @Insert
    long addRateSrv(RatesSrv rate);

    @Update
    int updateRateSrv(RatesSrv rate);

    @Query("DELETE FROM srv_rates WHERE title=:sTitle AND curr_u_id=:currUid")
    int deleteRateSrv(String sTitle,String currUid);

    @Query("SELECT * from srv_rates WHERE curr_u_id=:currUId")
    List<RatesSrv> getRates(String currUId);

    @Query("SELECT * from srv_rates where title==:title AND curr_u_id=:currUId")
    RatesSrv getRate(String title,String currUId);

    @Query("SELECT EXISTS(SELECT * FROM srv_rates WHERE title ==:title AND curr_u_id=:currUId)")
    boolean isExistsRate(String title,String currUId);

    @Query("SELECT COUNT(id) FROM srv_rates WHERE curr_u_id=:currUId")
    long getCount(String currUId);
}
