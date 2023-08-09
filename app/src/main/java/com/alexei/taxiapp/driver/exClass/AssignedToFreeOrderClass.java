package com.alexei.taxiapp.driver.exClass;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.driver.model.GetStDrvUidModel;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class AssignedToFreeOrderClass extends FragmentActivity {
    private DatabaseReference orderRef;
    private DatabaseReference srvRef;
    private DatabaseReference dialRef;

    private ScheduledExecutorService scheduledExecutorService;//Executors.newScheduledThreadPool(1);
    private ExecutorService executorservice;// = Executors.newSingleThreadExecutor();
    private ScheduledFuture<?> scheduledFutureDlg;
    private ScheduledFuture<?> scheduledFutureTimer;
    private Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();
    private InfoOrder infoOrder;
    private String currUserId;
    private Activity activity;
    private AlertDialog dialogNotification;
    private int period;
    private int passSec;
    private boolean execution = true;


    private OnRegistrationListener onRegistrationListener;

    public interface OnRegistrationListener {
        void onRegistrationOrder(InfoOrder order, boolean successful);

        void onAnotherDrvAssigned();
    }

    public void setOnRegistrationListener(OnRegistrationListener listener) {
        this.onRegistrationListener = listener;
    }


    public AssignedToFreeOrderClass(Activity activity, InfoOrder infoOrder, int period, DatabaseReference srvRef, String currUserId) {
        this.infoOrder = infoOrder;
        this.period = period;
        this.activity = activity;
        this.srvRef = srvRef;
        this.dialRef = srvRef.child("keysO").child(infoOrder.getKeyOrder()).child("dial");
        this.orderRef = srvRef.child("freeOrders").child(infoOrder.getKeyOrder());
        this.passSec = period;
        this.currUserId = currUserId;

        scheduledExecutorService = Executors.newScheduledThreadPool(1);
        executorservice = Executors.newFixedThreadPool(2);

//        monitoringOrderRef(orderRef);

//        startTimer();
        executorservice.submit(() -> startRegistration());
    }


    private void startRegistration() {

        setDrvUidListener(orderRef.child("driverUid"));
        setDialListener(dialRef);
    }


    private void setDrvUidListener(DatabaseReference driverUidRef) {
        if (mapListeners.get(driverUidRef) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String valDrvUid = snapshot.getValue(String.class);
                        if (valDrvUid != null) {
                            handlerDriverUidListener(valDrvUid);
                        }
                    } else {
                        recoverResources();
                        onRegistrationListener.onRegistrationOrder(infoOrder, false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            driverUidRef.addValueEventListener(listener);
            mapListeners.put(driverUidRef, listener);
        }
    }


    private void setDialListener(DatabaseReference dialRef) {
        if (mapListeners.get(dialRef) == null) {
            ValueEventListener listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        String valDial = snapshot.getValue(String.class);
                        if (valDial != null) {
                            if (valDial.equals("")) {
                                //звоним предлагаем себя
                                snapshot.getRef().setValue(currUserId, new DatabaseReference.CompletionListener() {
                                    @Override
                                    public void onComplete(@androidx.annotation.Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                        if (error == null) {
                                            startTimer();//если через период я не назначен, то показ диалога где можно удалить заказ
                                            removeRefListener(dialRef, mapListeners);
                                        }
                                    }
                                });
                            }
                        }
//                        handlerDialListener(snapshot);
                    } else {
                        recoverResources();
                        onRegistrationListener.onRegistrationOrder(infoOrder, false);
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            };
            dialRef.addValueEventListener(listener);
            mapListeners.put(dialRef, listener);
        }
    }


    private void handlerDriverUidListener(String keyDrvUid) {

        if (keyDrvUid.equals(currUserId)) {//ответ от сервера- назначен я
            if (scheduledFutureDlg != null) scheduledFutureDlg.cancel(true);//отменяем диалог
            if (scheduledFutureTimer != null) scheduledFutureTimer.cancel(true);
            if (dialogNotification != null)
                dialogNotification.dismiss();//если диалог показан то закрываем

            orderRef.child("status").setValue(Util.ASSIGN_ORDER_STATUS, new DatabaseReference.CompletionListener() {
                @Override
                public void onComplete(@androidx.annotation.Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {
                        recoverResources();
                        onRegistrationListener.onRegistrationOrder(infoOrder, true);
                    }
                }
            });

        } else if (keyDrvUid.length() > 0) {//ктото назначен(если статус не успел записаться, то то клиент назначит другого и здесь мы выдем)
            recoverResources();
            onRegistrationListener.onAnotherDrvAssigned();
        }
    }

    public void recoverResources() {
        try {
            execution = false;

            if (scheduledFutureDlg != null) scheduledFutureDlg.cancel(true);
            if (scheduledFutureTimer != null) scheduledFutureTimer.cancel(true);
            if (dialogNotification != null) dialogNotification.dismiss();

            Util.removeAllValueListener(mapListeners);

            scheduledExecutorService.shutdown();
            executorservice.shutdown();

        } catch (Exception e) {
            e.printStackTrace();
        }

    }


    private void startTimer() {
        if (execution) {

            Runnable task = this::showTimeProcess;
            scheduledFutureDlg = scheduledExecutorService.schedule(task, this.period, TimeUnit.SECONDS);
        }
    }

    private void showTimeProcess() {
        AlertDialog.Builder builder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = (View) inflater.inflate(R.layout.dialog_notification, null);
        builder.setCancelable(false);
        TextView tvTimer = dialogView.findViewById(R.id.textViewTimePassed);
        builder.setView(dialogView);

        startPassedTime(tvTimer);

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

                deleteOrderRef();

                recoverResources();
                onRegistrationListener.onRegistrationOrder(infoOrder, false);

            }
        });
        runOnUiThread(() -> {
            dialogNotification = builder.create();
            dialogNotification.show();
        });
    }

    private void deleteOrderRef() {
        orderRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    GetStDrvUidModel model = snapshot.getValue(GetStDrvUidModel.class);
                    if (model != null) {
                        if ((model.getDriverUid().equals("") && model.getStatus() == Util.FREE_ORDER_STATUS)) {
                            Map<String, Object> map = new HashMap<>();
                            map.put("/freeOrders/" + infoOrder.getKeyOrder(), null);
                            map.put("/keysO/" + infoOrder.getKeyOrder() + "/dial", null);

                            srvRef.updateChildren(map, new DatabaseReference.CompletionListener() {
                                @Override
                                public void onComplete(@androidx.annotation.Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                                    if (error == null) {

                                    }
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    private void startPassedTime(TextView tvTimer) {

        Runnable task = () -> {

            passSec++;
            int minute = passSec / 60;
            String sMinute = (minute > 9) ? ("" + minute) : ("0" + minute);
            int second = passSec % 60;
            String sSecond = (second > 9) ? ("" + second) : ("0" + second);
            runOnUiThread(() -> {
                tvTimer.setText(activity.getString(R.string.passed_) + sMinute + ":" + sSecond);
            });

        };
        scheduledFutureTimer = scheduledExecutorService.scheduleAtFixedRate(task, 0, 1, TimeUnit.SECONDS);
    }


    private void removeRefListener(DatabaseReference ref, Map<DatabaseReference, ValueEventListener> listeners) {
        Util.removeByRefListener(ref, listeners);
    }

    @Override
    protected void finalize() throws Throwable {

        recoverResources();
        super.finalize();
    }

}

