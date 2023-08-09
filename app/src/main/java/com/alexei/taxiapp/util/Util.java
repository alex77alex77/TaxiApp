package com.alexei.taxiapp.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.R;
import com.alexei.taxiapp.server.model.MsgModel;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;

public class Util {
    public static final DecimalFormat decimalFormat = (DecimalFormat) DecimalFormat.getNumberInstance();
    public static final SimpleDateFormat formatTimeDate = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance(SimpleDateFormat.SHORT, SimpleDateFormat.MEDIUM);
    public static final SimpleDateFormat formatTimeDate2 = (SimpleDateFormat) SimpleDateFormat.getDateTimeInstance();
    public static final SimpleDateFormat formatDate = (SimpleDateFormat) SimpleDateFormat.getDateInstance();
    public static final SimpleDateFormat formatTime = (SimpleDateFormat) SimpleDateFormat.getTimeInstance();


    public static final int SELECT_BUTTON = 1;
    public static final int NOT_SELECT_BUTTON = 0;
    public static final int NOT_FIX_STATE = 2;
    public static final int FIX_STATE = 3;
    public static final int SIGN_OUT = 4;

    public static final int RESPONSE_DENY = 0;

    public static final int BLOCK_CLIENT = 3;
    public static final int DENY_CLIENT = 2;
    public static final int ACCEPT_CLIENT = 1;
    public static final String DATABASE_NAME = "TaxiDB";

    private static final String KM = App.context.getString(R.string.km);
    private static final String METER = App.context.getString(R.string.meter);

    public static final int CAR_PULLED_UP = 1;//"Подъехал";

    public static final int START_ROUTE = 2;//"Начать";
    public static final int TAXIMETER = 3;//"Таксометр";


    public static final int KILL_ORDER_STATUS = -2;
    public static final int ARRIVE_ORDER_STATUS = 10;
    public static final int EXECUTION_ORDER_STATUS = 9;
    public static final int ROUTE_FINISHED_ORDER_STATUS = 11;//зарегистрирован в базе данных
    public static final int FREE_ORDER_STATUS = 5;
    public static final int ASSIGN_ORDER_STATUS = 1;
    public static final int CANCEL_ORDER_STATUS = -1;
    public static final int DROP_ORDER_STATUS = 6;
    public static final int WAIT_SEND_ORDER_STATUS = 3;
    public static final int SEND_TO_DRV_ORDER_STATUS = 2;


    public static final int ACCEPT_OK = 1;
    public static final int ACCEPT_CANCEL = 4;
    public static final int REDIRECT_OK = 5;


    public static final int RESULT_OK = 5;
    public static final int RESULT_CANCEL = 4;

    public static final int RESULT_CHANGE_DRIVER = 8;
    public static final int RESULT_DROP_ORDER = 4;
    public static final int REQUEST_ACCEPT_ORDER = 5;
    public static final int REQUEST_GUIDE_ORDER = 6;
    public static final int RESULT_KILL_ORDER = 9;

    public static final int RESULT_SETTING_SERVER = 7;
    public static final int RESULT_CANCELED_DRIVER = 10;

    public static final int RESULT_REQUEST_CONNECT = 4;
    public static final int RESULT_EDIT_DRIVER = 20;

    public static final int REQUEST_SELECT_FREE_ORDER = 23;
    public static final int REQUEST_SELECT_PERSONAL_ORDER = 24;

    public static final int CHECK_SETTINGS_CODE = 100;
    public static final int REQUEST_LOCATION_PERMISSION = 200;
    public static final int REQUEST_CALL_PHONE_PERMISSION = 210;


    public static final int REQUEST_SETTING_DRV = 21;

    public static final int EDIT_FILLING_ORDER = 31;
    public static final int DATA_FILLING_ORDER = 30;


    public static final int GET_LOCATION_TO = 10;
    public static final int GET_LOCATION_FROM = 11;

    public static final int DRIVER_MODE = 1;
    public static final int PASSENGER_MODE = 2;
    public static final int SERVER_MODE = 3;

    public static final int VOICE_REQUEST_CODE = 1;

    public static final int FREE_DRIVER_STATUS = 7;
    public static final int BUSY_DRIVER_STATUS = 6;


    public static final int EDIT_MODE_DRIVER = 1;
    public static final int EDIT_OK_RESULT = 2;
    public static final int EDIT_BREAK_RESULT = 5;

    public static final int DRIVER_DOT_THE_MAP_RESULT = 4;

    public static final int UNCONNECTED_PROVIDER_STATUS = 8;
    public static final int CONNECTED_PROVIDER_STATUS = 9;
    public static final int NOT_DEFINED_PROVIDER_STATUS = 10;
    public static final int SEND_REQUEST_PROVIDER_STATUS = 11;

    public static final int PAUSE_PROVIDER_STATUS = 2;
    public static final int RUNNING_PROVIDER_STATUS = 3;

    public static final int UNKNOWN_DRIVER_STATUS = -1;

    public static final int NOT_REF_DRIVER_STATUS_TMP = 12;
    public static final int CONNECTED_TO_SERVER_DRIVER_STATUS = 5;
    public static final int BLOCKED_TO_SYSTEM_DRIVER_STATUS = 4;




    public static final int TYPE_ADDRESS_SHORT = 1;
    public static final int TYPE_ADDRESS_LONG = 0;

    public static final long MILLISECOND_IN_DAY = 86400000;


    public static final int SOS_ON = 1;
    public static final int SOS_OFF = 0;

    public static final int STATUS_TO_ORDER = 2;

    public static final int ACTION_OPEN_SHIFT = 1;
    public static final int ACTION_CLOSE_SHIFT = 2;
    public static final int ACTION_GET_NOTIFY_SHIFT = 6;

    public static final int SHIFT_CLOSE_DRV_STATUS = 8;
    public static final int SHIFT_OPEN_DRV_STATUS = 9;
    public static final int SHIFT_INSUFFICIENT_FUNDS_DRV_STATUS = 4;
    public static final int SHIFT_THE_SYSTEM_IS_DENIED_STATUS = 5;

    public static final int ALLOWED_OPEN_SHIFT = 3;
    public static final int EMPLOYER_CLIENT = 4;

    public static final int SORTED_SOS = 3;
    public static final int SORTED_MSG_SRV = 0;



    public static synchronized String getAddress(double latitude, double longitude, int typeAddress) {
        String strAddress = "-";

        if (latitude != 0 && longitude != 0) {

            try {
                List<Address> list = getAddress(latitude, longitude);
                Address address = list.get(0);

                switch (typeAddress) {
                    case Util.TYPE_ADDRESS_SHORT:
                        strAddress = "";
//                        strAddress = address.getCountryName() + "\n" + address.getAdminArea() + "\n" + address.getLocality();
                        if (list.get(0).getCountryName() != null) {
                            strAddress += list.get(0).getCountryName() + ",\n ";
                        }

                        if (list.get(0).getAdminArea() != null) {
                            strAddress += list.get(0).getAdminArea() + ",\n ";
                        } else if (list.get(0).getSubAdminArea() != null) {
                            strAddress += list.get(0).getSubAdminArea() + ",\n ";
                        }

                        if (list.get(0).getSubAdminArea() != null) {
                            strAddress += list.get(0).getSubAdminArea() + "\n";
                        } else {
                            if (list.get(0).getLocality() != null) {
                                strAddress += list.get(0).getLocality() + "\n";
                            }
                        }
                        break;
//                    case Util.TYPE_ADDRESS_NORM:
//                        strAddress = address.getCountryName() + "\n" + address.getAdminArea() + "\n" + address.getLocality() + "\n" + address.getThoroughfare() + "\n" + address.getSubThoroughfare();
//                        break;
                    case Util.TYPE_ADDRESS_LONG:

                        strAddress = address.getAddressLine(0);//.replace(",", "\n")

                        break;
                }

                return strAddress;
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }

        }
        return strAddress;
    }

    private static synchronized List<Address> getAddress(double lat, double lon) throws ExecutionException, InterruptedException {

        Callable task = () -> {
            Geocoder gc = new Geocoder(App.context, Locale.getDefault());
            return gc.getFromLocation(lat, lon, 1);
        };

        FutureTask<List<Address>> future = new FutureTask<>(task);
        new Thread(future).start();

        return future.get();
    }

    public static synchronized List<Address> getAddress(String locationString) throws ExecutionException, InterruptedException {

        Callable task = () -> {
            Geocoder gc = new Geocoder(App.context, Locale.getDefault());
            return gc.getFromLocationName(locationString, 1);
        };

        FutureTask<List<Address>> future = new FutureTask<>(task);
        new Thread(future).start();

        return future.get();
    }

    public static synchronized String defineDistance(double latFrom, double longFrom, double latTo, double longTo) {
        if (latFrom != 0 && longFrom != 0 && latTo != 0 && longTo != 0) {

//            float distance = locationFrom.distanceTo(locationTo);//-----------------расчет расстояния от водителя до пользователя
            float distance = calculationDistance(latFrom, longFrom, latTo, longTo);//-----------------расчет расстояния от водителя до пользователя
            return (distance < 1000) ? ("" + (int) distance + METER) : (String.format("%.3f", distance / 1000) + KM);
        } else {
            return App.context.getString(R.string.distance_not_define);
        }
    }

    public static synchronized float calculationDistance(double fromLat, double fromLong, double toLat, double toLong) {
        float[] results = new float[1];
        if (fromLat != 0 && fromLong != 0 && toLat != 0 && toLong != 0) {
            Location.distanceBetween(fromLat, fromLong, toLat, toLong, results);
        }
        return results[0];
    }

    public static void dlgMessage(Activity activity, String title, String msg, String answerFrom, DatabaseReference answerRef) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(activity);

        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_chat, null);
        dialogBuilder.setView(dialogView);

        TextView tvTitle = dialogView.findViewById(R.id.tvTimeMsgTitle);
        TextView tvMessage = dialogView.findViewById(R.id.tvMessage);
        EditText etAnswerMessage = dialogView.findViewById(R.id.etAnswerMessage);
        Button buttonAnswer = dialogView.findViewById(R.id.buttonAnswer);

        if (!title.isEmpty()) {
            tvTitle.setText(title);
        } else {
            tvTitle.setVisibility(View.GONE);
        }

        if (msg.isEmpty()) {
            tvMessage.setVisibility(View.GONE);
        } else {
            tvMessage.setText(msg);
        }

        if (answerRef == null) {
            etAnswerMessage.setVisibility(View.GONE);
        }

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();

        etAnswerMessage.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {

                buttonAnswer.setText(editable.length() > 0 ? activity.getString(R.string.send_m) : activity.getString(R.string.close_m));
            }
        });

        buttonAnswer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (etAnswerMessage.length() > 0) {
                    sendMsgByRef(answerFrom + etAnswerMessage.getText().toString(), answerRef);
                }
                alertDialog.dismiss();
            }
        });
    }

    public static synchronized void sendMsgByRef(String msg, DatabaseReference ref) {

        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {//msgFor... есть

                    if (snapshot.child("msg").exists()) {

                        if (Objects.equals(snapshot.child("msg").getValue(), "")) {

                            snapshot.getRef().setValue(new MsgModel(msg));
                            Toast.makeText(App.context, R.string.msg_send, Toast.LENGTH_SHORT).show();
                        } else {

                            Toast.makeText(App.context, R.string.break_send_msg, Toast.LENGTH_LONG).show();
                        }

                    } else {//ссылка поврежденная

                        snapshot.getRef().setValue(new MsgModel(msg));//- исправляем
                        Toast.makeText(App.context, R.string.msg_send, Toast.LENGTH_SHORT).show();
                    }
                } else {

                    Toast.makeText(App.context, R.string.addressee_not_exsists, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }


    public static synchronized void removeByRefListener(DatabaseReference ref, Map<DatabaseReference, ValueEventListener> listeners) {
        if (ref != null) {
            for (Map.Entry<DatabaseReference, ValueEventListener> entry : listeners.entrySet()) {
                if (entry.getKey().equals(ref)) {
                    if (entry.getValue() != null) {
                        entry.getKey().removeEventListener(entry.getValue());
                        entry.setValue(null);
                    }
                }
            }
        }
    }

    public static synchronized void removeByRefChildListener(DatabaseReference ref, Map<DatabaseReference, ChildEventListener> listeners) {
        if (ref != null) {
            for (Map.Entry<DatabaseReference, ChildEventListener> entry : listeners.entrySet()) {
                if (entry.getKey().equals(ref)) {
                    if (entry.getValue() != null) {
                        entry.getKey().removeEventListener(entry.getValue());
                        entry.setValue(null);
                    }
                }
            }
        }
    }

//удаление слушателя по коренной ссылке(вхождение строки в ссылку)

    public static synchronized void removeByHostListener(DatabaseReference host, Map<DatabaseReference, ValueEventListener> listeners) {
        if (host != null) {
            for (Map.Entry<DatabaseReference, ValueEventListener> entry : listeners.entrySet()) {
                if (entry.getKey().toString().contains(host.toString())) {
                    if (entry.getValue() != null) {

                        entry.getKey().removeEventListener(entry.getValue());
                        entry.setValue(null);
                    }
                }
            }
        }
    }


    public static synchronized void removeAllValueListener(Map<DatabaseReference, ValueEventListener> listeners) {

        for (Map.Entry<DatabaseReference, ValueEventListener> entry : listeners.entrySet()) {
            if (entry.getValue() != null) {

                entry.getKey().removeEventListener(entry.getValue());
                entry.setValue(null);
            }
        }
        listeners.clear();
    }

    public static synchronized void removeAllChildListener(Map<DatabaseReference, ChildEventListener> listeners) {

        for (Map.Entry<DatabaseReference, ChildEventListener> entry : listeners.entrySet()) {
            if (entry.getValue() != null) {

                entry.getKey().removeEventListener(entry.getValue());
                entry.setValue(null);
            }
        }
        listeners.clear();
    }


    public static synchronized String getCountMin(long duration) {

        return String.format("%02d:%02d", duration / 3600, duration / 60 % 60);//duration / 3600, duration / 60 % 60, duration % 60

    }

    public static StringBuffer getEntryTransport(String sPos, String[] source) {
        StringBuffer sBuffer = new StringBuffer();
        String[] pos = sPos.split(",");
        Arrays.stream(pos).forEach(p -> {
            if(Integer.parseInt(p)>-1 && Integer.parseInt(p)< source.length){
                sBuffer.append(source[Integer.parseInt(p)]);
                sBuffer.append(",");
            }else {
                sBuffer.append("?,");
            }
        });
        return sBuffer.deleteCharAt(sBuffer.length() - 1);
    }
}
