package com.alexei.taxiapp.driver.exClass;

import com.alexei.taxiapp.db.SettingServer;

public class ServerInformsAboutEventsClass {
    private static ServerInformsAboutEventsClass instance;
    public static SettingServer setting;
    public OnListener onListener;

    public interface OnListener {
        void onEvents(long j);

        void onLoad();
    }

    public void setListener(OnListener listener) {
        this.onListener = listener;
    }

    public static synchronized ServerInformsAboutEventsClass getInstance() {
        ServerInformsAboutEventsClass serverInformsAboutEventsClass;
        synchronized (ServerInformsAboutEventsClass.class) {
            if (instance == null) {
                instance = new ServerInformsAboutEventsClass();
            }
            serverInformsAboutEventsClass = instance;
        }
        return serverInformsAboutEventsClass;
    }

    public ServerInformsAboutEventsClass() {
        setListeners();
    }

    private void setListeners() {
        setListener(new OnListener() {
            public void onEvents(long countEvents) {
            }

            public void onLoad() {
            }
        });
    }

    public void updateSettingSrv(SettingServer sett) {
        setting.setRateShift(sett.getRateShift());
        setting.setServerUid(sett.getServerUid());
        setting.setServerName(sett.getServerName());
        setting.setChkConnect(sett.isChkConnect());
        setting.setChkDisableReq(sett.isChkDisableReq());
        setting.setHoursCount(sett.getHoursCount());
        setting.setWaitAccumulation(sett.getWaitAccumulation());
        setting.setWaitAccept(sett.getWaitAccept());
        setting.setTypesServices(sett.getTypesServices());
        setting.setChkAvailable(sett.isChkAvailable());
        setting.setChkAcceptOrder(sett.isChkAcceptOrder());
        setting.setContactPhone(sett.getContactPhone());
    }
}

