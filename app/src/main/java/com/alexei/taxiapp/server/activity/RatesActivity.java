package com.alexei.taxiapp.server.activity;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.InputFilter;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.room.Room;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityRatesBinding;
import com.alexei.taxiapp.db.AppDatabase;
import com.alexei.taxiapp.db.RatesSrv;
import com.alexei.taxiapp.server.adapter.AdapterRatesList;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

public class RatesActivity extends AppCompatActivity {
    private ExecutorService executorservice;
    private AppDatabase db;
    private FirebaseAuth auth;
    private String currentUserUid;
    private ArrayList<RatesSrv> ratesList = new ArrayList<>();
    private AdapterRatesList adapterRatesList;

    private RecyclerView.LayoutManager layoutManager;
    private ActivityRatesBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(getResources().getConfiguration().orientation);
        binding = ActivityRatesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
//        setContentView(R.layout.activity_rates);

        executorservice = Executors.newFixedThreadPool(2);

        db = Room.databaseBuilder(getApplicationContext(), AppDatabase.class, Util.DATABASE_NAME).allowMainThreadQueries().build();// создание базы

        auth = FirebaseAuth.getInstance();
        if (auth.getCurrentUser() != null) {
            currentUserUid = auth.getCurrentUser().getUid();
            binding.btnCreateRate.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    defTitleRate();
                }
            });

            buildRecyclerView();

            handlerItemTouchHelper();

            adapterRatesList.setSelectListener(new AdapterRatesList.OnSelectListener() {
                @Override
                public void onSelectItem(RatesSrv rate, int position) {
                    initFieldRate(ratesList.get(position), true);

                }
            });

            executorservice.submit(this::loadRates);
        } else {
            finish();
        }

    }

    private void handlerItemTouchHelper() {
        new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public int getSwipeDirs(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {

                return super.getSwipeDirs(recyclerView, viewHolder);
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                executorservice.submit(() -> {
                    RatesSrv rate = ratesList.get(position); //----------------------действие выполняется после сдвига
                    deleteRate(rate);
                    ratesList.remove(position);

                    runOnUiThread(() -> {
                        adapterRatesList.notifyItemRemoved(position);
                    });
                });

            }

        }).attachToRecyclerView(binding.rvRates);//---------------прикрепить это действие к recyclerView

    }

    private void initFieldRate(RatesSrv rate, boolean isUpdate) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        LayoutInflater inflater = this.getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_rate, null);
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


        builder.setPositiveButton(isUpdate ? getString(R.string.update) : getString(R.string.create), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialogBox, int id) {

            }
        });

        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isValidate(etCityKm) | !isValidate(etCityMin) |
                        !isValidate(etDefWait) | !isValidate(etFixedAmount) | !isValidate(etHourlyRate)) {

                } else {
                    rate.setTitle(tvTitle.getText().toString());
                    rate.setKm(Float.parseFloat(etCityKm.getText().toString()));
                    rate.setMin(Float.parseFloat(etCityMin.getText().toString()));
                    rate.setDefWait(Float.parseFloat(etDefWait.getText().toString()));
                    rate.setFixedAmount(Float.parseFloat(etFixedAmount.getText().toString()));
                    rate.setHourlyRate(Float.parseFloat(etHourlyRate.getText().toString()));

                    executorservice.submit(() -> {
                        saveRate(rate, isUpdate);
                    });

                    alertDialog.dismiss();
                }
            }
        });

    }

    private void deleteRate(RatesSrv rate) {
        db.getRatesDAO().deleteRateSrv(rate.getTitle(), currentUserUid);
    }

    private void loadRates() {
        ratesList.addAll(db.getRatesDAO().getRates(currentUserUid));

        runOnUiThread(() -> {
            adapterRatesList.notifyDataSetChanged();
        });
    }

    private boolean isValidate(EditText etCityKm) {
        String value = etCityKm.getText().toString();
        if (value.isEmpty()) {
            etCityKm.setError(getString(R.string.validate_not_empty));
            return false;
        }

        return true;
    }

    private void buildRecyclerView() {


        binding.rvRates.setHasFixedSize(true);
        layoutManager = new LinearLayoutManager(this);
        binding.rvRates.setLayoutManager(layoutManager);

        adapterRatesList = new AdapterRatesList(ratesList);
        binding.rvRates.setAdapter(adapterRatesList);


    }

    private void defTitleRate() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.type_rate_);

        final EditText input = new EditText(this);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(10)});
        input.setGravity(Gravity.CENTER_HORIZONTAL);
        input.setHint(R.string.hint_gruz);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });


        final AlertDialog dialog = builder.create();
        dialog.show();
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = input.getText().toString();

                if (value.trim().length() > 0) {

                    try {
                        if (existsRate(value)) {

                            Toast.makeText(RatesActivity.this, R.string.break_type_exists, Toast.LENGTH_LONG).show();
                        } else {

                            RatesSrv rate = new RatesSrv();
                            rate.setTitle(value);
                            rate.setCurrUId(currentUserUid);
                            initFieldRate(rate, false);
                        }
                    } catch (ExecutionException | InterruptedException e) {
                        e.printStackTrace();
                    }
                }
                dialog.dismiss();
            }
        });

    }

    private void saveRate(RatesSrv rate, boolean isUpdate) {
        if (isUpdate) {
            long id = db.getRatesDAO().updateRateSrv(rate);

            runOnUiThread(() -> {
                if (id > 0) {
                    adapterRatesList.notifyDataSetChanged();

                } else {
                    Toast.makeText(getApplicationContext(), R.string.break_save_change, Toast.LENGTH_LONG).show();
                }
            });

        } else {
            long id = db.getRatesDAO().addRateSrv(rate);

            runOnUiThread(() -> {
                if (id > 0) {
                    rate.setId(id);
                    ratesList.add(0, rate);
                    adapterRatesList.notifyDataSetChanged();

                } else {
                    Toast.makeText(getApplicationContext(), R.string.break_add_data, Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private boolean existsRate(String title) throws ExecutionException, InterruptedException {
        Callable task = () -> db.getRatesDAO().isExistsRate(title, currentUserUid);

        FutureTask future = new FutureTask<>(task);
        new Thread(future).start();

        return (boolean) future.get();
    }

}