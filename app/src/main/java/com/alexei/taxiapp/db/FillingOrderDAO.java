package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface FillingOrderDAO {
    @Insert
    long add(InfoOrder fillingOrder);

    @Update
    int update(InfoOrder fillingOrder);

    @Query("DELETE FROM orders WHERE keyOrder=:keyOrder AND ( providerKey=:currUid OR senderKey=:currUid OR clientUid=:currUid)")
    int delFillingOrder(String keyOrder,String currUid);

    @Query("select * from orders WHERE ( providerKey=:currUid OR senderKey=:currUid OR clientUid=:currUid)")
    List<InfoOrder> getAllFillingOrders(String currUid);

    @Query("SELECT * from orders WHERE keyOrder==:keyOrder AND ( providerKey=:currUid OR senderKey=:currUid OR clientUid=:currUid)")
    InfoOrder getFillingOrder(String keyOrder,String currUid);

    @Query("SELECT * from orders WHERE ( providerKey=:currUid OR senderKey=:currUid OR clientUid=:currUid) ORDER BY ID DESC LIMIT 1")
    InfoOrder getLastFillingOrder(String currUid);
}
