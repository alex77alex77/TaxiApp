package com.alexei.taxiapp.driver.model;

public class InfoServerModel {
    String name;
    String phone;
    String services;

    public InfoServerModel() {
    }

    public InfoServerModel(String name2, String services2, String phone2) {
        this.name = name2;
        this.services = services2;
        this.phone = phone2;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name2) {
        this.name = name2;
    }

    public String getServices() {
        return this.services;
    }

    public void setServices(String services2) {
        this.services = services2;
    }

    public String getPhone() {
        return this.phone;
    }

    public void setPhone(String phone2) {
        this.phone = phone2;
    }
}
