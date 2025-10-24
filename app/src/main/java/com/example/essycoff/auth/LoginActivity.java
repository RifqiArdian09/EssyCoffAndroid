package com.example.essycoff.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.example.essycoff.MainActivity;
import com.example.essycoff.R;
import com.example.essycoff.api.ApiService;
import com.example.essycoff.api.RetrofitClient;
import com.example.essycoff.model.LoginResponse;
import com.example.essycoff.utils.AuthManager;
import com.example.essycoff.utils.Constants;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin;
    private ProgressBar progressBar;
    private AuthManager authManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        authManager = AuthManager.getInstance(this);

        if (authManager.isLoggedIn()) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        etEmail = findViewById(R.id.et_Email);
        etPassword = findViewById(R.id.et_Password);
        btnLogin = findViewById(R.id.btn_Login);
        progressBar = findViewById(R.id.progressBar);
        btnLogin.setOnClickListener(v -> loginUser());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Isi semua field", Toast.LENGTH_SHORT).show();
            return;
        }

        ApiService api = RetrofitClient.getClient(this).create(ApiService.class);
        ApiService.LoginRequest request = new ApiService.LoginRequest(email, password);

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        Call<LoginResponse> call = api.login(request, Constants.SUPABASE_ANON_KEY);
        call.enqueue(new Callback<LoginResponse>() {
            @Override
            public void onResponse(Call<LoginResponse> call, Response<LoginResponse> response) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);

                if (response.isSuccessful() && response.body() != null) {
                    String token = response.body().getAccessToken();
                    String refresh = response.body().getRefreshToken();
                    if (response.body().getUser() != null) {
                        String email = response.body().getUser().getEmail();
                        authManager.saveEmail(email);
                    }
                    authManager.saveToken(token);
                    if (refresh != null && !refresh.isEmpty()) {
                        authManager.saveRefreshToken(refresh);
                    }

                    startActivity(new Intent(LoginActivity.this, MainActivity.class));
                    finish();
                } else {
                    String errorMessage = "Login gagal";
                    try {
                        errorMessage += ": " + response.errorBody().string();
                    } catch (Exception ignored) {}
                    Toast.makeText(LoginActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<LoginResponse> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                Toast.makeText(LoginActivity.this, "Error: " + t.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }
}
