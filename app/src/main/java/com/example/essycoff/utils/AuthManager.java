package com.example.essycoff.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.essycoff.utils.Constants;

import java.util.UUID;

public class AuthManager {
    private static final String TAG = "AuthManager";
    private static AuthManager instance;
    private SharedPreferences prefs;

    private AuthManager(Context context) {
        prefs = context.getSharedPreferences(Constants.PREF_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized AuthManager getInstance(Context context) {
        if (instance == null) {
            instance = new AuthManager(context);
        }
        return instance;
    }

    public void saveToken(String token) {
        prefs.edit().putString(Constants.KEY_TOKEN, token).apply();
    }

    public void saveRefreshToken(String refreshToken) {
        prefs.edit().putString(Constants.KEY_REFRESH_TOKEN, refreshToken).apply();
    }

    public void saveEmail(String email) {
        prefs.edit().putString(Constants.KEY_EMAIL, email).apply();

        if (email != null && !email.isEmpty()) {
            String userUuid = UUID.nameUUIDFromBytes(email.getBytes()).toString();
            saveUserUuid(userUuid);
            Log.d(TAG, "Generated UUID for email " + email + ": " + userUuid);
        }
    }

    public String getToken() {
        return prefs.getString(Constants.KEY_TOKEN, null);
    }

    public String getRefreshToken() {
        return prefs.getString(Constants.KEY_REFRESH_TOKEN, null);
    }

    public String getEmail() {
        return prefs.getString(Constants.KEY_EMAIL, null);
    }

    public void saveUserUuid(String userUuid) {
        prefs.edit().putString(Constants.KEY_USER_UUID, userUuid).apply();
    }

    public String getUserId() {
        String uuid = prefs.getString(Constants.KEY_USER_UUID, null);

        // Fallback: If UUID doesn't exist but email does, generate it
        if (uuid == null || uuid.isEmpty()) {
            String email = getEmail();
            if (email != null && !email.isEmpty()) {
                uuid = UUID.nameUUIDFromBytes(email.getBytes()).toString();
                saveUserUuid(uuid);
                Log.d(TAG, "Generated fallback UUID for existing email: " + uuid);
            }
        }

        return uuid;
    }

    public boolean isLoggedIn() {
        return getToken() != null;
    }

    public void logout() {
        prefs.edit().clear().apply();
    }
}