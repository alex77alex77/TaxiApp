package com.alexei.taxiapp;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.util.Patterns;
import android.view.Gravity;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.alexei.taxiapp.client.activity.ClientMapActivity;
import com.alexei.taxiapp.databinding.ActivitySignInBinding;
import com.alexei.taxiapp.driver.activity.DriverMapsActivity;
import com.alexei.taxiapp.driver.exClass.LoginInAppClass;
import com.alexei.taxiapp.util.Util;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

import javax.mail.Authenticator;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

public class SignInActivity extends AppCompatActivity {
    private ExecutorService executorservice;// = Executors.newSingleThreadExecutor();
    //    private AppDatabase db;
    private FirebaseDatabase database;
    private FirebaseAuth auth;


    private boolean isLogin;
    private String email;
    private String name;
    private String password;

    private String codeVerificationEmail;

    private int mode;
    private int invalidCodeEntered = 0;

    private ArrayList<LoginInAppClass> loginList = new ArrayList<>();
    private ActivitySignInBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setRequestedOrientation(getResources().getConfiguration().orientation);
        Intent intent = getIntent();
        if (intent != null)
            mode = intent.getIntExtra("mode", 0);

        executorservice = Executors.newSingleThreadExecutor();
        database = FirebaseDatabase.getInstance(); //доступ к корневой папке базы данных
        auth = FirebaseAuth.getInstance();
        initView(false);

        viewListener();
    }

    private void viewListener() {
        binding.tvTextPolitic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(getString(R.string.politic_uri)));
                startActivity(browserIntent);

            }
        });

        binding.chkPolitic.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                binding.loginSignUpButton.setEnabled(binding.chkPolitic.isChecked());
            }
        });

        binding.loginSignUpButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                loginSignUpUser();
            }
        });
    }

    private boolean validateEmail() {
        if (binding.etEmail.getText() != null) {
            String inputEmail = binding.etEmail.getText().toString().trim();
            if (inputEmail.isEmpty()) {
                binding.etEmail.setError(getString(R.string.validate_email));
                return (false);
            } else if (!isValidEmail(inputEmail)) {
                binding.etEmail.setError(getString(R.string.validate_email2));
                return (false);
            }
//            if (binding.etEmail.getError().length() > 0)
            binding.etEmail.setError(null);

            email = inputEmail;
            return true;
        } else {
            return false;
        }
    }

    private boolean validateName() {
        if (binding.etName.getText() != null) {
            String inputName = binding.etName.getText().toString().trim();

            if (inputName.isEmpty()) {
                binding.etName.setError(getString(R.string.validate_input_name));
                return (false);
            }

//            if (binding.etName.getError()!=null && binding.etName.getError().length() > 0)
            binding.etName.setError(null);

            name = inputName;
            return true;
        } else {
            return false;
        }
    }

    private boolean validatePassword() {
        if (binding.etPass.getText() != null) {
            String inputPassword = binding.etPass.getText().toString().trim();

            if (inputPassword.isEmpty()) {
                binding.etPass.setError(getString(R.string.input_passowrd));
                return (false);
            } else if (inputPassword.length() < 6) {
                binding.etPass.setError(getString(R.string.validate_password_char));
                return (false);
            }

//            if (binding.etPass.getError().length() > 0)
            binding.etPass.setError(null);

            password = inputPassword;
            return true;
        } else {
            return false;
        }

    }

    private boolean validatePassword2() {
        if (binding.etPass.getText() != null && binding.etConfirmPass.getText() != null) {
            String inputPassword = binding.etPass.getText().toString().trim();
            String inputConfirmPassword = binding.etConfirmPass.getText().toString().trim();
            if (!inputPassword.equals(inputConfirmPassword)) {
                binding.etConfirmPass.setError(getString(R.string.validate_match_password));
                return (false);
            }
//            if (binding.etConfirmPass.getError().length() > 0)
            binding.etConfirmPass.setError(null);

            return true;
        } else {
            return false;
        }

    }

    private boolean isValidEmail(String email) {
        Pattern pattern = Patterns.EMAIL_ADDRESS;
        return pattern.matcher(email).matches();
    }

    public void loginSignUpUser() {

        if (isLogin) {
            signInUser();
        } else {
            sendCodeVerificationEmail();
            if (!(!validateEmail() | !validateName() | !validatePassword() | !validatePassword2())) {
                inputCodeVerificationEmail();
            }
        }
    }

    private void inputCodeVerificationEmail() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);

        builder.setTitle(R.string.confirm_email);
        builder.setMessage(R.string.input_code);
        builder.setIcon(R.drawable.ic_baseline_verified_24);
        final EditText input = new EditText(this);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        input.setGravity(Gravity.CENTER);
        builder.setView(input);

        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });

        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {

            }
        });
        builder.create();
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String value = input.getText().toString().trim();
                if (value.length() > 0 && value.equals(codeVerificationEmail)) {
                    signUpUser();
                    alertDialog.dismiss();
                } else {

                    invalidCodeEntered++;
                    if (invalidCodeEntered >= 4) {
                        alertDialog.dismiss();
                    } else {
                        Toast.makeText(SignInActivity.this,
                                String.format(getString(R.string.debug_invalid_code_entered),
                                        (4 - invalidCodeEntered)), Toast.LENGTH_LONG).show();
                    }
                }
            }
        });

    }

    private void sendCodeVerificationEmail() {

        executorservice.submit(() -> {
            try {
                codeVerificationEmail = new Random().ints(4,
                        33,
                        122).collect(StringBuilder::new,
                        StringBuilder::appendCodePoint, StringBuilder::append).toString();

                sendEmailVerificationCode(codeVerificationEmail);
                invalidCodeEntered = 0;
            } catch (MessagingException e) {
                e.printStackTrace();
                Toast.makeText(SignInActivity.this, getString(R.string.failed_attempt) + e.getMessage(), Toast.LENGTH_SHORT).show();
                System.out.println(e.getMessage());
            }
        });
    }

    private void sendEmailVerificationCode(String codeConfirm) throws MessagingException {

        //Конкретно для Yandex параметры соединения можно подсмотреть тут:
        //https://yandex.ru/support/mail/mail-clients.html (раздел "Исходящая почта")
        Properties properties = new Properties();
        //Хост или IP-адрес почтового сервера
        properties.put("mail.smtp.host", "smtp.yandex.ru");
        //Требуется ли аутентификация для отправки сообщения
        properties.put("mail.smtp.auth", "true");
//----------------
        properties.setProperty("mail.transport.protocol", "smtp");
        properties.put("mail.smtp.port", "465");
// --------------
        //Порт для установки соединения
        properties.put("mail.smtp.socketFactory.port", "465");
        //Фабрика сокетов, так как при отправке сообщения Yandex требует SSL-соединения
        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        //Создаем соединение для отправки почтового сообщения
        Session session = Session.getDefaultInstance(properties,
                //Аутентификатор - объект, который передает логин и пароль
                new Authenticator() {
                    @Override
                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("gribal3x@yandex.ru", "dleamxxjopxwbwyc");
                    }
                });

        //Создаем новое почтовое сообщение
        Message message = new MimeMessage(session);
        //От кого
        message.setFrom(new InternetAddress("gribal3x@yandex.ru"));
        //Кому
        message.setRecipient(Message.RecipientType.TO, new InternetAddress(email));
        //Тема письма
        message.setSubject("Подтверждение email");
        //Текст письма
        message.setText("Код подтверждения: " + codeConfirm);
        //Поехали!!!
        Transport.send(message);

    }

    private void signUpUser() {

        binding.flBlockSignIn.setVisibility(View.VISIBLE);

        FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    updateProfile(name, FirebaseAuth.getInstance().getCurrentUser());

                    switchIntent(mode);
                } else {
                    binding.flBlockSignIn.setVisibility(View.GONE);
                    Toast.makeText(getApplicationContext(), R.string.authentication_failed, Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void signInUser() {
        if (!validateEmail() | !validateName() | !validatePassword()) {
            return;
        }
        binding.flBlockSignIn.setVisibility(View.VISIBLE);

        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(@NonNull Task<AuthResult> task) {
                if (task.isSuccessful()) {

                    checkLogin(mode, auth.getCurrentUser());
                } else {

                    Toast.makeText(getApplicationContext(), R.string.login_failed2, Toast.LENGTH_LONG).show();
                }
                binding.flBlockSignIn.setVisibility(View.GONE);

            }
        });
    }


    @Override
    protected void onStart() {
        super.onStart();
        binding.flBlockSignIn.setVisibility(View.GONE);
    }


    private void checkLogin(int mode, FirebaseUser user) {

        LoginInAppClass login = new LoginInAppClass(database.getReference().getRef().child("SHAREDSERVER/driversList").child(user.getUid()).child("loginK"), mode, user);
        login.setOnListeners(new LoginInAppClass.OnListeners() {
            @Override
            public void onSuccessful(boolean success, int mode, String res) {
                if (success) {
                    switchIntent(mode);
                } else {
                    auth.signOut();//выход, так как  checkLogin запускается после входа в систему для того чтобы была возможность получить из базы ключ

                    runOnUiThread(() -> {
                        Toast.makeText(getApplicationContext(), res, Toast.LENGTH_LONG).show();
                        binding.flBlockSignIn.setVisibility(View.INVISIBLE);

                        initView(false);//регистрация
                    });
                }
                login.recoveryResources();
                loginList.remove(login);
            }

            @Override
            public void onError(int mode, String err) {
                auth.signOut();//выход, так как  checkLogin запускается после входа в систему для того чтобы была возможность получить из базы ключ

                runOnUiThread(() -> {
                    Toast.makeText(getApplicationContext(), err, Toast.LENGTH_LONG).show();
                    binding.flBlockSignIn.setVisibility(View.GONE);
                });
                login.recoveryResources();
                loginList.remove(login);
            }
        });
        loginList.add(login);

    }

    private void switchIntent(int mode) {
        Intent intent;
        switch (mode) {

            case Util.PASSENGER_MODE:

                intent = new Intent(SignInActivity.this, ClientMapActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                break;
            case Util.DRIVER_MODE:

                intent = new Intent(SignInActivity.this, DriverMapsActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                break;

        }
    }

    public void updateProfile(String name, FirebaseUser user) {
        if (user != null) {
            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                    .setDisplayName(name)
                    .build();
            user.updateProfile(profileUpdates).addOnCompleteListener(new OnCompleteListener<Void>() {
                @Override
                public void onComplete(@NonNull Task<Void> task) {
                    if (task.isSuccessful()) {

                    }
                }
            });
        }

    }


    public void toggleLoginSignUp(View view) {
        isLogin = !isLogin;

        initView(isLogin);
    }

    private void initView(boolean signIn) {
        isLogin = signIn;
        if (signIn) {
            binding.etConfirmPass.setVisibility(View.GONE);
            binding.ilConfirmPassword.setVisibility(View.GONE);
            binding.loginSignUpButton.setText(R.string.input);
            binding.loginSignUpButton.setEnabled(true);
            binding.chkPolitic.setVisibility(View.GONE);
            binding.tvTextPolitic.setVisibility(View.GONE);
        } else {
            binding.etConfirmPass.setVisibility(View.VISIBLE);
            binding.ilConfirmPassword.setVisibility(View.VISIBLE);
            binding.chkPolitic.setChecked(false);
            binding.chkPolitic.setVisibility(View.VISIBLE);
            binding.tvTextPolitic.setVisibility(View.VISIBLE);
            binding.loginSignUpButton.setEnabled(false);
            binding.loginSignUpButton.setText(R.string.signup);
        }
    }


    @Override
    protected void onDestroy() {
        executorservice.shutdown();
        super.onDestroy();
    }
}
