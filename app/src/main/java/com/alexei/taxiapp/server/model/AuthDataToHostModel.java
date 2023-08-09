package com.alexei.taxiapp.server.model;

public class AuthDataToHostModel {
    String name = "";
    String password = "";

    public AuthDataToHostModel() {
    }

    public AuthDataToHostModel(String name2, String password2) {
        this.name = name2;
        this.password = password2;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name2) {
        this.name = name2;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String password2) {
        this.password = password2;
    }
}
