package com.alexei.taxiapp.db;

import androidx.room.Dao;

@Dao
public interface DataAccessRefDAO {
    DataAccessToRef getDataAccess(String str, int i);

    long saveDataAccess(DataAccessToRef dataAccessToRef);

    int updateDataAccess(DataAccessToRef dataAccessToRef);
}
