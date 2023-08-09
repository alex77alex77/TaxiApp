package com.alexei.taxiapp.db;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {

        DataBlockClient.class,
        DataAvailableServer.class,
        DataRoute.class,
        InfoOrder.class,
        ServerReport.class,
        InfoRequestConnect.class,
        DataTaximeter.class,
        SettingDrv.class,
        InfoDriverReg.class,

        SettingServer.class,

        DataOrdersProvider.class,
        RatesSrv.class,
        DataExecutableOrder.class}, version = 1, exportSchema = false)

public abstract class AppDatabase extends RoomDatabase {

    public abstract DataBlockClientDAO getBlockClientDAO();

    public abstract DataAvailableServerDAO getAvailableServerDAO();

    public abstract DataRouteDAO getDataRouteDAO();

    public abstract FillingOrderDAO getInfoOrderDAO();

    public abstract ReportOnDriversForServerDAO getReportServerDAO();

    public abstract DataTaximeterDAO getTaximeterDAO();

    public abstract DataSettingDrvDAO getSettingAppDAO();

    public abstract DataDriversServerDAO getDataDriversServerDAO();


    public abstract DataOrdersProviderDAO getDataOrdersProviderDAO();

    public abstract DataSettingServerDAO getSettingServerDAO();

    public abstract DataRatesSrvDAO getRatesDAO();

    public abstract DataExecutableOrderDAO getExecutableOrderDAO();

}
