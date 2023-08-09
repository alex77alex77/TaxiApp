package com.alexei.taxiapp;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;

import com.alexei.taxiapp.client.adapter.ProviderAccessForClientAdapter;
import com.alexei.taxiapp.client.model.AvailableProviderModel;
import com.alexei.taxiapp.databinding.ActivityChooseModeBinding;
import com.alexei.taxiapp.databinding.ActivityFillingOrderBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.db.InfoOrder;
import com.alexei.taxiapp.driver.model.InfoServerModel;
import com.alexei.taxiapp.driver.model.RateModel;
import com.alexei.taxiapp.db.RatesSrv;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FillingOrderActivity extends AppCompatActivity {

    public ProviderAccessForClientAdapter availableProvidersAdapter;
    public List<AvailableProviderModel> availableProvidersList = new ArrayList();

    private String currUserUId;
    private FirebaseDatabase database;
    private FirebaseAuth auth;
    private AppDatabase db;
    private View dialogView;

    private AlertDialog dlgAvaiProviders;
    private ExecutorService executorservice;
    private DatabaseReference hostKeysSRef;
    private DatabaseReference hostServersRef;
    private DataLocation mFrom;
    private DataLocation mLocation;
    private DataLocation mTo;
    private Map<DatabaseReference, ValueEventListener> mapDlgGetProvidersListener = new HashMap();

    private ArrayAdapter<RatesSrv> ratesAdapter;
    private AvailableProviderModel recipientSrv = new AvailableProviderModel("SHAREDSERVER", new InfoServerModel("SHARED", "", ""));
    private RatesSrv selRate;
    private ActivityFillingOrderBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityFillingOrderBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_filling_order);
        setRequestedOrientation(getResources().getConfiguration().orientation);

        executorservice = Executors.newFixedThreadPool(2);
        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();
        database = FirebaseDatabase.getInstance();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {
            currUserUId = auth.getCurrentUser().getUid();
            hostKeysSRef = database.getReference().child("keysS");
            hostServersRef = database.getReference().child("serverList");
            mTo = new DataLocation();
            mFrom = new DataLocation();
            mLocation = new DataLocation();
            Intent intentData = getIntent();
            if (intentData != null) {
                mLocation = (DataLocation) intentData.getParcelableExtra("location");
                int intExtra = intentData.getIntExtra("employer", -1);

                mFrom = mLocation;

                if (intExtra == 4) {
                    binding.blockSelectProvider.setVisibility(View.VISIBLE);
                    binding.tvDebugCurrency.setVisibility(View.VISIBLE);
                    autoFillingFields();
                } else {
                    handlerExistsRates(intentData);
                    binding.tvAddressFrom.setText(Util.getAddress(mLocation.getLatitude(), mLocation.getLongitude(), 0));
                }
            } else {
                finish();
            }
            binding.tvSelectProvider.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    FillingOrderActivity.this.displayProvider();
                }
            });
            binding.tvRateInfo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    if (ratesAdapter != null) {

                        initFieldRate((RatesSrv) binding.spinnerRates.getSelectedItem());
                    }
                }
            });
            binding.btnCancelFilling.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    setResult(0, new Intent());
                    finish();
                }
            });
            binding.ibSelectAddressInMapFrom.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(FillingOrderActivity.this, SelectPointInMapActivity.class);
                    intent.putExtra("location", mLocation);
                    intent.putExtra("titleMarker", getString(R.string.where_from2));
                    startActivityForResult(intent, 11);
                }
            });
            binding.ibSelectAddressInMapTo.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    Intent intent = new Intent(FillingOrderActivity.this, SelectPointInMapActivity.class);
                    intent.putExtra("location", mLocation);
                    intent.putExtra("titleMarker", getString(R.string.where_to2));
                    startActivityForResult(intent, 10);
                }
            });
            binding.btnNextFillingOrder.setOnClickListener(new View.OnClickListener() {
                public void onClick(View view) {
                    endFillingOrder();
                }
            });
            return;
        }
        finish();
    }

    private void handlerExistsRates(Intent intentData) {
        final ArrayList<RatesSrv> rates = intentData.getParcelableArrayListExtra("rates");
        if (rates == null || rates.size() <= 0) {
            binding.blockWTP.setVisibility(View.VISIBLE);
        } else {
            ArrayAdapter<RatesSrv> arrayAdapter = new ArrayAdapter<>(getApplicationContext(), R.layout.rate_item, rates);
            ratesAdapter = arrayAdapter;

            binding.spinnerRates.setAdapter(ratesAdapter);
            binding.spinnerRates.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                    selRate = (RatesSrv) rates.get(pos);
                }

                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            binding.blockRates.setVisibility(View.VISIBLE);
            binding.blockWTP.setVisibility(View.GONE);
        }
        binding.tvAddressFrom.setText(Util.getAddress(this.mLocation.getLatitude(), this.mLocation.getLongitude(), 0));
    }

    private void autoFillingFields() {
        InfoOrder lastOrder = db.getInfoOrderDAO().getLastFillingOrder(this.currUserUId);
        if (lastOrder != null) {
            mFrom = lastOrder.getFrom();
            mTo = lastOrder.getTo();
            binding.tvAddressFrom.setText(Util.getAddress(mFrom.getLatitude(), mFrom.getLongitude(), 0));
            binding.tvAddressTo.setText(Util.getAddress(mTo.getLatitude(), mTo.getLongitude(), 0));
            binding.addressNoteEditText.setText(lastOrder.getNote());
            binding.etPhone.setText(lastOrder.getPhone());
            binding.spinnerTypeTransport.setTextSelectedItems(lastOrder.getTypeTr());
            return;
        }
        binding.tvAddressFrom.setText(Util.getAddress(mLocation.getLatitude(), mLocation.getLongitude(), 0));
    }

    /* access modifiers changed from: private */
    public void displayProvider() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        dialogView = getLayoutInflater().inflate(R.layout.dialog_available_list, (ViewGroup) null);
        builder.setIcon(R.drawable.ic_baseline_list_24);
        builder.setTitle(R.string.t_select_company);
        builder.setView(dialogView);
        builder.setCancelable(false);
        RecyclerView rv = (RecyclerView) dialogView.findViewById(R.id.rvAvailableList);
        rv.setHasFixedSize(true);
        rv.setLayoutManager(new LinearLayoutManager(this));
        ProviderAccessForClientAdapter providerAccessForClientAdapter = new ProviderAccessForClientAdapter(availableProvidersList);
        availableProvidersAdapter = providerAccessForClientAdapter;
        rv.setAdapter(providerAccessForClientAdapter);
        getAvailableProviders();
        availableProvidersAdapter.setListener(new ProviderAccessForClientAdapter.SelectItemListener() {
            public void onSelectItem(AvailableProviderModel model) {
                handlerSelectItemProviderAdapter(dlgAvaiProviders, model);
            }
        });
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
            }
        }).setPositiveButton(R.string.default_provider, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogInterface, int i) {
                handlerSelectItemProviderAdapter(dlgAvaiProviders, new AvailableProviderModel("SHAREDSERVER", new InfoServerModel("SHARED", "", "")));
            }
        });
        AlertDialog create = builder.create();
        this.dlgAvaiProviders = create;
        create.show();
    }

    /* access modifiers changed from: private */
    public void handlerSelectItemProviderAdapter(AlertDialog alert, AvailableProviderModel model) {
        this.recipientSrv = model;
        binding.tvSelectProvider.setText(model.getInfoServer().getName().equals("SHARED") ? getString(R.string.default_provider) : model.getInfoServer().getName());
        alert.dismiss();
    }

    /* access modifiers changed from: private */
    public void endFillingOrder() {
        if (isValidateFix(binding.etPriceWTP) && isValidateType()) {
            InfoOrder infoOrder = new InfoOrder();
            infoOrder.setFrom(mFrom);
            infoOrder.setTo(mTo);
            infoOrder.setNote(binding.addressNoteEditText.getText().toString());
            infoOrder.setPhone(binding.etPhone.getText().toString().trim());
            if (this.selRate != null) {
                infoOrder.setRate(new RateModel(selRate.getKm(), selRate.getMin(), selRate.getDefWait(), this.selRate.getFixedAmount(), this.selRate.getHourlyRate()));
            } else {
                infoOrder.setRate(new RateModel(Float.parseFloat(binding.etPriceWTP.getText().toString())));
            }
            infoOrder.setTypeTr(binding.spinnerTypeTransport.getTextSelectedItems());
            AvailableProviderModel availableProviderModel = recipientSrv;
            if (availableProviderModel != null) {
                infoOrder.setProviderKey(availableProviderModel.getKeySrv());
                infoOrder.setProviderName(recipientSrv.getInfoServer().getName());
            }
            Intent intent = new Intent();
            intent.putExtra("data_order", infoOrder);
            setResult(5, intent);
            finish();
        }
    }

    /* access modifiers changed from: protected */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 10 && resultCode == 5 && data != null) {
            DataLocation dataLocation = (DataLocation) data.getParcelableExtra("location");
            mTo = dataLocation;
            binding.tvAddressTo.setText(Util.getAddress(dataLocation.getLatitude(), mTo.getLongitude(), 0));
        }
        if (requestCode == 11 && resultCode == 5 && data != null) {
            DataLocation dataLocation2 = (DataLocation) data.getParcelableExtra("location");
            mFrom = dataLocation2;
            binding.tvAddressFrom.setText(Util.getAddress(dataLocation2.getLatitude(), mFrom.getLongitude(), 0));
        }
    }

    /* access modifiers changed from: private */
    public boolean isValidateFieldRate(EditText et) {
        try {
            if (et.getText().toString().isEmpty()) {
                et.setError(getString(R.string.validate_price));
                return false;
            } else if (Float.parseFloat(et.getText().toString()) < 0.0f) {
                return false;
            } else {
                return true;
            }
        } catch (Exception e) {
            et.setError(getString(R.string.invalid_value));
            return false;
        }
    }

    private boolean isValidateFix(EditText et) {
        try {
            if (!et.getText().toString().isEmpty()) {
                return true;
            }
            et.setError(getString(R.string.validate_amount));
            return false;
        } catch (Exception e) {
            et.setError(getString(R.string.invalid_value));
            return false;
        }
    }

    private boolean isValidateType() {
        if (!binding.spinnerTypeTransport.getTextSelectedItems().isEmpty()) {
            return true;
        }
        binding.spinnerTypeTransport.setError(binding.spinnerTypeTransport.getSelectedView(), getString(R.string.validate_type_tr));
        return false;
    }


    public void initFieldRate(RatesSrv rate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        View view = getLayoutInflater().inflate(R.layout.dialog_rate, (ViewGroup) null);
        builder.setView(view);
        TextView tvTitle = view.findViewById(R.id.tvDlgTitle);
        tvTitle.setText(rate.getTitle());

        EditText etCityKm = view.findViewById(R.id.etDlgKm);
        etCityKm.setText(String.valueOf(rate.getKm()));

        EditText etCityMin = view.findViewById(R.id.etDlgMin);
        etCityMin.setText(String.valueOf(rate.getMin()));

        EditText etDefWait = view.findViewById(R.id.etDlgDefWait);
        etDefWait.setText(String.valueOf(rate.getDefWait()));

        EditText etFixedAmount = view.findViewById(R.id.etDlgFixedAmount);
        etFixedAmount.setText(String.valueOf(rate.getFixedAmount()));

        EditText etHourlyRate = view.findViewById(R.id.etDlgHourlyRate);
        etHourlyRate.setText(String.valueOf(rate.getHourlyRate()));
        AlertDialog alertDialog = builder.create();

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogBox, int id) {

            }
        });
        builder.setNegativeButton(R.string.close, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {

            }
        });
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isValidateFieldRate(etCityKm) |
                        !isValidateFieldRate(etCityMin) |
                        !isValidateFieldRate(etDefWait) |
                        !isValidateFieldRate(etFixedAmount) |
                        !isValidateFieldRate(etHourlyRate)) {

                } else {
                    executorservice.submit(() -> saveRate(rate,
                            tvTitle,
                            etCityKm,
                            etCityMin,
                            etDefWait,
                            etFixedAmount,
                            etHourlyRate));
                    alertDialog.dismiss();
                }
            }
        });

        alertDialog.show();
    }

    private void saveRate(RatesSrv rate,
                          TextView tvTitle,
                          EditText etCityKm,
                          EditText etCityMin,
                          EditText etDefWait,
                          EditText etFixedAmount,
                          EditText etHourlyRate) {
        rate.setTitle(tvTitle.getText().toString());
        rate.setKm(Float.parseFloat(etCityKm.getText().toString()));
        rate.setMin(Float.parseFloat(etCityMin.getText().toString()));
        rate.setDefWait(Float.parseFloat(etDefWait.getText().toString()));
        rate.setFixedAmount(Float.parseFloat(etFixedAmount.getText().toString()));
        rate.setHourlyRate(Float.parseFloat(etHourlyRate.getText().toString()));

        db.getRatesDAO().updateRateSrv(rate);

        runOnUiThread(() -> printRate(rate));
    }


    private void printRate(RatesSrv selRate2) {
        binding.tvRateInfo.setText(R.string.t_km);
        binding.tvRateInfo.append(selRate2.getKm() + "\n");

        binding.tvRateInfo.append("" + R.string.t_min);
        binding.tvRateInfo.append(selRate2.getMin() + "\n");

        binding.tvRateInfo.append("" + R.string.start_amount);
        binding.tvRateInfo.append(selRate2.getFixedAmount() + "\n");

        binding.tvRateInfo.append("" + R.string.hour_payment);
        binding.tvRateInfo.append("" + selRate2.getHourlyRate());
    }

    private void getAvailableProviders() {
        availableProvidersList.clear();
        Util.removeAllValueListener(mapDlgGetProvidersListener);

        hostKeysSRef.addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {

                    for (DataSnapshot keyS : snapshot.getChildren()) {
                        String key = keyS.getKey();
                        if (key != null) {
                            getAccessServer(hostServersRef.child(key), key);
                        }
                    }
                }
            }

            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }


    public void getAccessServer(final DatabaseReference srvRef, final String key) {
        if (mapDlgGetProvidersListener.get(srvRef.child("access")) == null) {
            ValueEventListener listener = new ValueEventListener() {
                public void onDataChange(DataSnapshot snapshot) {
                    if (snapshot.exists()) {
                        getInfoServerRef(srvRef, key);

                    } else {
                        availableProvidersList.removeIf(p -> p.getKeySrv().equals(key));
                        if (availableProvidersAdapter != null) {
                            availableProvidersAdapter.notifyDataSetChanged();
                        }
                        responseResult();
                    }

                }

                public void onCancelled(DatabaseError error) {
                    responseResult();
                }
            };
            this.mapDlgGetProvidersListener.put(srvRef.child("access"), listener);
            srvRef.child("access").addValueEventListener(listener);
        }
    }


    public void getInfoServerRef(DatabaseReference srvRef, final String keySrv) {
        srvRef.child("info").addListenerForSingleValueEvent(new ValueEventListener() {
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                InfoServerModel dataServer;
                if (snapshot.exists() && (dataServer = (InfoServerModel) snapshot.getValue(InfoServerModel.class)) != null) {
                    availableProvidersList.add(new AvailableProviderModel(keySrv, dataServer));
                    if (availableProvidersAdapter != null) {
                        availableProvidersAdapter.notifyDataSetChanged();
                    }
                }
                responseResult();
            }

            public void onCancelled(DatabaseError error) {
                responseResult();
            }
        });
    }


    public synchronized void responseResult() {

        if (availableProvidersList.size() == 0) {
            this.dialogView.findViewById(R.id.tvResultLoadAvailableSrv).setVisibility(View.VISIBLE);
            this.dialogView.findViewById(R.id.load_available_prog_bar).setVisibility(View.GONE);
        }
    }

    public void onDestroy() {
        Util.removeAllValueListener(mapDlgGetProvidersListener);
        super.onDestroy();
    }
}
