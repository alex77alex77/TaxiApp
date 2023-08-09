package com.alexei.taxiapp.driver.activity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;

import com.alexei.taxiapp.R;
import com.alexei.taxiapp.databinding.ActivityRequestConnectBinding;
import com.alexei.taxiapp.db.InfoRequestConnect;
import com.alexei.taxiapp.driver.model.DataLocation;
import com.alexei.taxiapp.util.Util;
import com.google.firebase.auth.FirebaseAuth;

public class RequestConnectActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private String mCurrUserUid;

    private InfoRequestConnect infoReqConn;
    private DataLocation dislocation;

    private String keySrv = "";
    private String nameSrv = "";
    private ActivityRequestConnectBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityRequestConnectBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

//        setContentView(R.layout.activity_request_connect);
        setRequestedOrientation(getResources().getConfiguration().orientation);

        dislocation = new DataLocation();
        auth = FirebaseAuth.getInstance();

        if (auth.getCurrentUser() != null) {

            mCurrUserUid = auth.getCurrentUser().getUid();
        } else {
            finish();
        }

        Intent intent = getIntent();
        if (intent != null) {

            keySrv = intent.getStringExtra("keySrv");
            nameSrv = intent.getStringExtra("nameSrv");
            infoReqConn = intent.getParcelableExtra("data_req");


            if (infoReqConn != null) {
                initFieldRequest();//чтение запроса
            } else {
                finish();
            }

        }

        binding.btnCloseReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });


        binding.btnSendReq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!validateColor() | !validateModel() | !validateName() | !validateNameHost() | !validateRegNumber() | !validateType() | !validatePhone() | !validateDislocation()) {//

                    return;
                } else {
                    Intent intent = new Intent();
                    intent.putExtra("data_req", loadDataRequest());
                    intent.putExtra("nameSrv", nameSrv);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    setResult(Util.RESULT_OK, intent);

                    finish();
                }
            }
        });


    }

    private void initFieldRequest() {
        keySrv = infoReqConn.getKeySrv();
        binding.tvNameHostReq.setText(infoReqConn.getTitle());
        binding.tvNameReq.setText(infoReqConn.getName());
        binding.tvPhoneReq.setText(infoReqConn.getPhone());

//        tvModelReq.setText(infoReqConn.getAuto().getAutoModel());
//        tvColorReq.setText(infoReqConn.getAuto().getAutoColor());
//        tvRegNumReq.setText(infoReqConn.getAuto().getAutoNumber());

        binding.tvTypeTransport.setText(infoReqConn.getTypeTr());
        binding.tvDislocationReq.setText(Util.getAddress(infoReqConn.getDisl().getLatitude(), infoReqConn.getDisl().getLongitude(), Util.TYPE_ADDRESS_SHORT));
        dislocation = infoReqConn.getDisl();

    }


    private InfoRequestConnect loadDataRequest() {
        return new InfoRequestConnect(
                keySrv,
                binding.tvNameReq.getText().toString(),
                binding.tvNameHostReq.getText().toString(),
                binding.tvPhoneReq.getText().toString(),
                binding.tvTypeTransport.getText().toString(),
                dislocation,
                System.currentTimeMillis(),
                mCurrUserUid);
    }


    private boolean validateName() {
        String input = binding.tvNameReq.getText().toString().trim();
        if (input.isEmpty()) {
            binding.tvNameReq.setError(getString(R.string.not_defined_name));
            return (false);
        }

        return true;
    }

    private boolean validateNameHost() {
        String input = binding.tvNameHostReq.getText().toString().trim();
        if (input.isEmpty()) {
            binding.tvNameHostReq.setError(getString(R.string.no_defined_title_host));
            return (false);
        }

        return true;
    }

    private boolean validateModel() {
        String input = binding.tvModelReq.getText().toString().trim();
        if (input.length() < 3) {
            binding.tvModelReq.setError(getString(R.string.not_model));
            return (false);
        }

        return true;
    }

    private boolean validateColor() {
        String input = binding.tvColorReq.getText().toString().trim();
        if (input.length() < 3) {
            binding.tvColorReq.setError(getString(R.string.not_color));
            return (false);
        }

        return true;
    }

    private boolean validateRegNumber() {
        String input = binding.tvRegNumReq.getText().toString().trim();
        if (input.length() < 3) {
            binding.tvRegNumReq.setError(getString(R.string.not_number));
            return (false);
        }

        return true;
    }

    private boolean validatePhone() {
        String input = binding.tvPhoneReq.getText().toString().trim();
        if (input.length() < 2) {
            binding.tvPhoneReq.setError(getString(R.string.not_define_phone));
            return (false);
        }

        return true;
    }


    private boolean validateType() {
        String input = binding.tvTypeTransport.getText().toString();

        if (input.isEmpty()) {
            binding.tvTypeTransport.setError(getString(R.string.not_define_type));

            return (false);
        }
        return true;
    }

    private boolean validateDislocation() {
        String input = binding.tvDislocationReq.getText().toString().trim();

        if (input.isEmpty() || dislocation.getLatitude() == 0 || dislocation.getLongitude() == 0) {
            binding.tvDislocationReq.setError(getString(R.string.not_dislocation));

            return (false);
        }

        return true;
    }


}