package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;


@Dao
public interface DataDriversServerDAO {

    @Insert
    long addDriverServer(InfoDriverReg infoDriverReg);

    @Update
    int updateDataDriverServer(InfoDriverReg infoDriverReg);

    @Query("DELETE FROM drivers_in_server WHERE driver_u_id=:keyDrv AND u_id=:currUid")
    void deleteDriverInfo(String keyDrv, String currUid);

    @Query("UPDATE drivers_in_server SET status_to_host_srv = :status WHERE driver_u_id =:keyDrv AND u_id=:currUid")
    int updateStatus(int status, String keyDrv, String currUid);

    @Query("select * from drivers_in_server WHERE server_u_id==:currUid")
    List<InfoDriverReg> getAllDrivers(String currUid);

    @Query("select * from drivers_in_server WHERE call_sign==:callSign AND u_id=:currUid")
    InfoDriverReg getDriverInfo(int callSign, String currUid);

    @Query("select * from drivers_in_server WHERE driver_u_id==:uId  AND u_id=:currUid")
    InfoDriverReg getDriverInfo(String uId, String currUid);

    @Query("select * from drivers_in_server WHERE id==:id AND u_id=:currUid")
    InfoDriverReg getDriverInfo(long id, String currUid);

    @Query("SELECT MAX(call_sign) FROM drivers_in_server WHERE u_id=:currUid")
    long getMaxCallSign(String currUid);

    @Query("SELECT * FROM drivers_in_server WHERE driver_u_id ==:driverUid AND u_id=:currUid")
    InfoDriverReg getDriver(String driverUid, String currUid);

    @Query("DELETE FROM drivers_in_server WHERE u_id=:currUid")
    int delAllDrivers(String currUid);

}
