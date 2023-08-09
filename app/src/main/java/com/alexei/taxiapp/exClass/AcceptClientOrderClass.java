package com.alexei.taxiapp.exClass;

import androidx.annotation.NonNull;
import androidx.room.Room;

import com.alexei.taxiapp.App;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.DataBlockClient;
import com.alexei.taxiapp.server.model.DataSenderModel;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class AcceptClientOrderClass {
    private static AcceptClientOrderClass instance;
    private boolean bChkAutoAcceptOrder;
    private String currentUserUid;



    private final AppDatabase db;
    private final ExecutorService executorservice = Executors.newFixedThreadPool(2);

    private String hasKeySender;
    private OnListeners onListener;
    private DatabaseReference serverRef;

    public interface OnListeners {
        void onConfirmation(String str, DataSenderModel dataSenderModel);

        void onPostClientOrder(String str);

        void onPostRequestClientOrder(String str, String str2);
    }

    public void setOnListener(OnListeners listener) {
        onListener = listener;
    }

    public static synchronized AcceptClientOrderClass getInstance(DatabaseReference serverRef2, boolean bChkAutoAcceptOrder2, String uid) {
        if (instance == null) {
            instance = new AcceptClientOrderClass(serverRef2, bChkAutoAcceptOrder2, uid);
        }
        return instance;
    }

    private AcceptClientOrderClass(DatabaseReference serverRef2, boolean bChkAutoAcceptOrder2, String currentUserUid2) {
        currentUserUid = currentUserUid2;
        bChkAutoAcceptOrder = bChkAutoAcceptOrder2;
        serverRef = serverRef2;
        db = Room.databaseBuilder(App.context, AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();
    }


    private void checkHasKeySenderRef() {
        serverRef.child("access/keySender").addValueEventListener(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String sender;
                if (snapshot.exists() && (sender = (String) snapshot.getValue(String.class)) != null
                        && sender.trim().length() > 0) {

                    checkBlockingSender(sender);
                }
            }

            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void getDataRequestSender(String keySender, final String name) {
        serverRef.child("access").addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    DataSenderModel dataLastSender = (DataSenderModel) snapshot.getValue(DataSenderModel.class);
                    if (dataLastSender != null) {
                        checkConfirmSender(dataLastSender, name);
                    }
                }
            }

            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    public void startClass() {
        checkHasKeySenderRef();
    }


    private void checkBlockingSender(String sender) {
        DataBlockClient client = db.getBlockClientDAO().getKeyBlock(sender,currentUserUid);
        if (client != null) {
            String name = client.getName();
            if (client.getBlock() == 3) {
                Map<String, Object> map = new HashMap<>();
                map.put("/access/timer", 0);
                map.put("/access/keySender", "");
                map.put("/access/push", "");
                serverRef.updateChildren(map);
                return;
            }
            hasKeySender = sender;
            onListener.onPostRequestClientOrder(sender, name);
            return;
        }
        hasKeySender = sender;
        onListener.onPostRequestClientOrder(sender, "");
    }


    private void checkConfirmSender(DataSenderModel dataSender, String name) {
        if (bChkAutoAcceptOrder) {
            updateHost(dataSender);
        } else {
            onListener.onConfirmation(name, dataSender);
        }
    }

    public void confirmation(DataSenderModel dataSender, String name, int iCmd) {
        switch (iCmd) {
            case Util.ACCEPT_CLIENT:
                if (!name.equals("")) {
                    executorservice.submit(() -> save(dataSender, name, iCmd));
                }
                updateHost(dataSender);
                break;
            case Util.DENY_CLIENT:
                if (!name.equals("")) {
                    executorservice.submit(() -> save(dataSender, name, iCmd));
                }
                resetDataAccessHost(dataSender, iCmd);
                break;
            case Util.BLOCK_CLIENT:
                executorservice.submit(() -> save(dataSender, name, iCmd));
                resetDataAccessHost(dataSender, iCmd);
                break;
            default:
        }
    }


    private void save(DataSenderModel dataSender, String name, int iCmd) {
        saveDataAcceptClientDB(dataSender.getKeySender(), name, iCmd);
    }


    private void resetDataAccessHost(DataSenderModel dataSender, int iCmd) {
        Map<String, Object> map = new HashMap<>();
        map.put("/access/timer", 0);
        map.put("/access/push", "");
        map.put("/access/keySender", "");
        map.put("/freeOrders/push", iCmd + "/" + dataSender.getPush());
        map.put("/freeOrders/keySender", "");
        serverRef.updateChildren(map, new DatabaseReference.CompletionListener() {
            public void onComplete(DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    hasKeySender = "";
                }
            }
        });
    }

    private void saveDataAcceptClientDB(String keySender, String name, int iCmd) {
        DataBlockClient client = db.getBlockClientDAO().getKeyBlock(keySender,currentUserUid);
        if (client == null) {
            db.getBlockClientDAO().save(new DataBlockClient(currentUserUid, name, keySender, iCmd));
            return;
        }
        client.setName(name);
        client.setBlock(iCmd);
        db.getBlockClientDAO().update(client);
    }

    private void updateHost(DataSenderModel dataSender) {
        Map<String, Object> map = new HashMap<>();
        map.put("/access/timer", 0);
        map.put("/access/push", "");
        map.put("/access/keySender", "");
        map.put("/freeOrders/keySender/", dataSender.getKeySender());
        map.put("/freeOrders/push/", dataSender.getPush());
        serverRef.updateChildren(map, new DatabaseReference.CompletionListener() {
            public void onComplete(DatabaseError error, @NonNull DatabaseReference ref) {
                if (error == null) {
                    hasKeySender = "";
                }
            }
        });
    }
}

