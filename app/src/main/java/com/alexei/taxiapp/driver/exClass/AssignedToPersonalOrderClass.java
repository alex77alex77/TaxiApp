package com.alexei.taxiapp.driver.exClass;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.MutableData;
import com.google.firebase.database.Transaction;

public class AssignedToPersonalOrderClass {

    private DatabaseReference orderRef;

    private OnRegistrationListener onRegistrationListener;

    public interface OnRegistrationListener {
        void onRegistrationOrder(boolean successful);

    }

    public void setOnRegistrationListener(OnRegistrationListener listener) {
        this.onRegistrationListener = listener;
    }


    public AssignedToPersonalOrderClass(DatabaseReference orderRef, String currUserUid) {

        this.orderRef = orderRef;

        transactionAssigned(currUserUid);
    }


    private void transactionAssigned(String currUserUId) {
        orderRef.child("driverUid").runTransaction(new Transaction.Handler() {
            @Override
            public Transaction.Result doTransaction(MutableData mutableData) {

                String s = mutableData.getValue(String.class);
                if (s != null) {
                    if (s.equals("")) {

                        mutableData.setValue(currUserUId);
                        return Transaction.success(mutableData);
                    }
                } else {

                    return Transaction.success(mutableData);
                }

                return Transaction.abort();
            }

            @Override
            public void onComplete(@com.google.firebase.database.annotations.Nullable DatabaseError error, boolean committed, @com.google.firebase.database.annotations.Nullable DataSnapshot dataSnapshot) {
                if(dataSnapshot.exists()){
                    if (committed) {

//                    recoverResources();
                        onRegistrationListener.onRegistrationOrder(true);
                    } else {
//кто то назначен
                        onRegistrationListener.onRegistrationOrder(false);
                    }
                }else {
//error
                    onRegistrationListener.onRegistrationOrder(false);
                }
            }
        });
    }


    @Override
    protected void finalize() throws Throwable {

        super.finalize();
    }

}

