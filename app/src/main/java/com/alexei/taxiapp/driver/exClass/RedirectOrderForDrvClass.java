package com.alexei.taxiapp.driver.exClass;

import androidx.annotation.NonNull;

import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.driver.model.RedirectOrderModel;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class RedirectOrderForDrvClass {

    private final Map<DatabaseReference, ValueEventListener> mapListeners = new HashMap<>();

    private OnRedirectListener onListener;
    private InfoOrder order;

    public interface OnRedirectListener {
        void onCompleted(boolean successfully);
    }

    public void setRedirectListeners(OnRedirectListener listener) {
        this.onListener = listener;
    }

    public RedirectOrderForDrvClass(DatabaseReference srvSenderRef, DatabaseReference orderRef, @NonNull InfoDriverReg driver, InfoOrder order) {
        this.order = order;

        redirectOrderDrvRef(srvSenderRef.child("driversList").child(driver.getDriverUid()),srvSenderRef.getKey());
        monitorDriverUidOrder(orderRef, driver);
    }

    private void redirectOrderDrvRef(DatabaseReference drvRef,String keySender) {

//я(server) водителю из списке водителей в поле rOrder перенаправляю заказ
        drvRef.addListenerForSingleValueEvent(new ValueEventListener() {

            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {//такой водитель на сервере есть

                    drvRef.child("rOrder").setValue(new RedirectOrderModel(order.getKeyOrder(), order.getProviderKey()), new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error==null){

                            }
                        }
                    });
                } else {

                    onListener.onCompleted(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                onListener.onCompleted(false);
            }
        });
    }

    //заказ наблюдается на сервере на котором он находится
    private void monitorDriverUidOrder(DatabaseReference orderRef, InfoDriverReg driver) {
        ValueEventListener listener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    String driverUid = snapshot.getValue(String.class);
                    if (driverUid != null) {

                        if (!driverUid.equals("")) {

                            onListener.onCompleted(driverUid.equals(driver.getDriverUid()));
                        }
                    } else {

                        onListener.onCompleted(false);
                    }
                } else {

                    onListener.onCompleted(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

                onListener.onCompleted(false);
            }
        };
        mapListeners.put(orderRef.child("driverUid"), listener);
        orderRef.child("driverUid").addValueEventListener(listener);
    }

    public void recoverResources() {

        Util.removeAllValueListener(mapListeners);
    }

    @Override
    protected void finalize() throws Throwable {

        super.finalize();
    }
}
