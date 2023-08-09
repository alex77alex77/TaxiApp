package com.alexei.taxiapp.db;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface DataOrdersProviderDAO {
    @Insert
    long saveProvider(DataOrdersProvider provider);

    @Update
    void updateProvider(DataOrdersProvider provider);

    @Query("select * from orders_providers WHERE provider_key ==:key AND curr_u_id=:currUid")
    DataOrdersProvider getProvider(String key,String currUid);

    @Query("DELETE FROM orders_providers WHERE provider_key ==:key AND curr_u_id=:currUid")
    void deleteDataProvider(String key,String currUid);

}
