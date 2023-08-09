package com.alexei.taxiapp.exClass;

import com.google.firebase.auth.FirebaseAuth;

public class AuthenticationClass {

    private FirebaseAuth auth;

    int mode;

    private OnSuccessfulListener onSuccessfulListener;

    public interface OnSuccessfulListener {
        void onSuccessful(int mode);

        void onNoAuthentication(int mode);
    }

    public void setOnSuccessfulListener(OnSuccessfulListener listener) {
        this.onSuccessfulListener = listener;
    }


    public AuthenticationClass(int mode) {

        this.mode = mode;

        this.auth = FirebaseAuth.getInstance();


    }


    public void authentication() {

        if (auth.getCurrentUser() == null) {

            onSuccessfulListener.onNoAuthentication(mode);

        } else {

            onSuccessfulListener.onSuccessful(mode);

        }
    }
}

