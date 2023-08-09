package com.alexei.taxiapp.driver.exClass;

import android.media.MediaPlayer;

import androidx.annotation.NonNull;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.driver.model.SOSModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class SOSClass {

    private MediaPlayer mp;
    private int status = Util.SOS_OFF;

    private static SOSClass instance;
    private DatabaseReference sosHostRef;
    private FirebaseUser currentUser;
    private Map<DatabaseReference, ChildEventListener> mapChildListeners = new HashMap<>();

    public ArrayList<String> drvSOSList = new ArrayList<>();

    //------------------------------------Listener
    public OnUpdateListener onListener;

    public interface OnUpdateListener {
        void onOnSOS();

        void onCount(int size);

        void onOffSOS();
    }

    public void setOnUpdateListener(OnUpdateListener listener) {
        this.onListener = listener;
    }


    public static synchronized SOSClass getInstance(FirebaseUser currentUser, DatabaseReference refSOS) {  //If you want your method thread safe...
        if (instance == null) {
            instance = new SOSClass(currentUser, refSOS);
        }

        return instance;
    }

    public SOSClass(FirebaseUser currentUser, DatabaseReference sosHostRef) {

        this.sosHostRef = sosHostRef;
        this.currentUser = currentUser;

        initListenerSOS();
    }

    private void initListenerSOS() {
        if (mapChildListeners.get(sosHostRef) == null) {
            ChildEventListener listener = new ChildEventListener() {
                @Override
                public void onChildAdded(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {
                    if (snapshot.exists()) {
                        SOSModel sosModel = snapshot.getValue(SOSModel.class);
                        if (sosModel != null) {
                            String key = snapshot.getKey();
                            drvSOSList.add(key);
                            if (Objects.equals(key, currentUser.getUid())) {
                                status = Util.SOS_ON;//сохраняем состояние
                                if (mp == null) {
                                    mp = MediaPlayer.create(App.context, R.raw.sos);
                                    mp.setLooping(true);
                                    mp.start();
                                }
                                onListener.onOnSOS();
                            }
                        }

                        onListener.onCount(drvSOSList.size());
                    }
                }

                @Override
                public void onChildChanged(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                }

                @Override
                public void onChildRemoved(@NonNull DataSnapshot snapshot) {

                    String key = snapshot.getKey();
                    drvSOSList.remove(key);
                    if (Objects.equals(key, currentUser.getUid())) {

                        status = Util.SOS_OFF;//сохраняем состояние
                        if (mp != null) {
                            mp.release();
                            mp = null;
                        }
                        onListener.onOffSOS();
                    }

                    onListener.onCount(drvSOSList.size());
                }

                @Override
                public void onChildMoved(@NonNull DataSnapshot snapshot, @Nullable @org.jetbrains.annotations.Nullable String previousChildName) {

                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            sosHostRef.addChildEventListener(listener);
            mapChildListeners.put(sosHostRef, listener);
        }
    }

    public void initStatus() {
        if (onListener != null) {
            if (status == Util.SOS_ON) {
                onListener.onOnSOS();
            }
            if (status == Util.SOS_OFF) {
                onListener.onOffSOS();
            }

            onListener.onCount(drvSOSList.size());
        }
    }


    public void recoverRes() {
        Util.removeAllChildListener(mapChildListeners);
        drvSOSList.clear();
    }


    @Override
    protected void finalize() throws Throwable {
        recoverRes();
        super.finalize();
    }
}

