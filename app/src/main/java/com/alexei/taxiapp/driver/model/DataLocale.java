package com.alexei.taxiapp.driver.model;

import android.os.Parcel;
import android.os.Parcelable;

public class DataLocale implements Parcelable {
  public static final Creator<DataLocale> CREATOR = new Creator<DataLocale>() {
    public DataLocale createFromParcel(Parcel in) {
      return new DataLocale(in);
    }

    public DataLocale[] newArray(int size) {
      return new DataLocale[size];
    }
  };
  String currency = "₽";
  String language = "ru";

  public DataLocale() {
  }

  public DataLocale(String language2, String currency2) {
    this.language = language2;
    this.currency = currency2;
  }

  protected DataLocale(Parcel in) {
    this.language = in.readString();
    this.currency = in.readString();
  }

  public void writeToParcel(Parcel dest, int flags) {
    dest.writeString(this.language);
    dest.writeString(this.currency);
  }

  public int describeContents() {
    return 0;
  }

  public String getLanguage() {
    return this.language;
  }

  public void setLanguage(String language2) {
    this.language = language2;
  }

  public String getCurrency() {
    return this.currency;
  }

  public void setCurrency(String currency2) {
    this.currency = currency2;
  }
}
