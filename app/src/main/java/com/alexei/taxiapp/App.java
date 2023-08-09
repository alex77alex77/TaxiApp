package com.alexei.taxiapp;

import android.app.Application;
import android.content.Context;

public class App extends Application {
    public static Context context;

    public App() {

    }

    @Override
    public void onCreate() {
        super.onCreate();

        App.context = getApplicationContext();
    }
}
