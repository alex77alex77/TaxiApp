package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface ReportOnDriversForServerDAO {
    @Insert
    long addReport(ServerReport serverReport);

    @Update
    void updateReport(ServerReport serverReport);

    @Query("DELETE FROM server_reports WHERE currUId=:currUid")
    void clearReport(String currUid);

    @Query("SELECT * FROM server_reports WHERE currUId==:currUId")
    List<ServerReport> getAllReport(String currUId);

    @Query("SELECT COUNT(id) FROM server_reports WHERE currUId=:currUId")
    long getSize(String currUId);
}
