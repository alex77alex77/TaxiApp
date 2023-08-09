package com.alexei.taxiapp.server.exClass;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.driver.model.RedirectOrderModel;
import com.alexei.taxiapp.db.InfoDriverReg;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

public class SrvAssignOrderClass {

    private OnCompletedListener onCompletedListener;

    public interface OnCompletedListener {
        void onCompleted(boolean success);
    }

    public void setCompletedListener(OnCompletedListener listener) {
        this.onCompletedListener = listener;
    }

    public SrvAssignOrderClass(@NonNull DatabaseReference serverRef, @NonNull InfoOrder order, @NonNull InfoDriverReg drv) {

        prepareForSenderOrderToDrv(serverRef, drv, order);
    }


    private void prepareForSenderOrderToDrv(DatabaseReference serverRef, InfoDriverReg driver, InfoOrder order) {

        final String key = serverRef.child("freeOrders").push().getKey();
        if (key != null) {
            order.setKeyOrder(key);
            order.setDriverUid("");
            order.setProviderKey(serverRef.getKey());
            order.getMsgD().setMsg("");
            order.getMsgC().setMsg("");
            order.setSenderKey("");
            order.setTimestamp(System.currentTimeMillis());
            order.setStatus(Util.SEND_TO_DRV_ORDER_STATUS);

            serverRef.child("freeOrders").child(order.getKeyOrder()).setValue(order, new DatabaseReference.CompletionListener() {//создаем новый обновленый ордер
                @Override
                public void onComplete(@Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                    if (error == null) {

                        notifyAssignedOrderToDrv(order, serverRef.child("driversList").child(driver.getDriverUid()));
                    } else {

                        onCompletedListener.onCompleted(false);
                    }
                }
            });
        }
    }


    private void notifyAssignedOrderToDrv(InfoOrder order, DatabaseReference drvRef) {

        drvRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    snapshot.getRef().child("assignedOrder").setValue(new RedirectOrderModel(order.getKeyOrder(),order.getProviderKey()), new DatabaseReference.CompletionListener() {
                        @Override
                        public void onComplete(@Nullable @org.jetbrains.annotations.Nullable DatabaseError error, @NonNull DatabaseReference ref) {
                            if (error == null) {

                                onCompletedListener.onCompleted(true);
                            } else {

                                onCompletedListener.onCompleted(false);
                            }
                        }
                    });
                } else {

                    onCompletedListener.onCompleted(false);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                onCompletedListener.onCompleted(false);
            }
        });
    }

    @Override
    protected void finalize() throws Throwable {

        super.finalize();
    }
}
