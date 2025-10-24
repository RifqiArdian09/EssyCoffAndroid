package com.example.essycoff.api;

import android.content.Context;
import android.util.Log;

import com.example.essycoff.utils.AuthManager;
import com.example.essycoff.utils.Constants;

import java.io.IOException;

import okhttp3.Authenticator;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.Route;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class RetrofitClient {
    private static final String BASE_URL = Constants.SUPABASE_URL + "/";
    private static Retrofit retrofit = null;
    private static OkHttpClient httpClient = null;

    public static Retrofit getClient(Context context) {
        if (retrofit == null) {
            httpClient = buildHttpClient(context.getApplicationContext());
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(httpClient)
                    .build();
        }
        return retrofit;
    }

    public static Retrofit getClient() {
        if (retrofit == null) {
            retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build();
        }
        return retrofit;
    }

    private static OkHttpClient buildHttpClient(Context appContext) {
        AuthManager auth = AuthManager.getInstance(appContext);

        Interceptor headersInterceptor = chain -> {
            Request original = chain.request();
            Request.Builder builder = original.newBuilder();
            if (original.header("apikey") == null) {
                builder.header("apikey", Constants.SUPABASE_ANON_KEY);
            }
            if (original.header("Authorization") == null) {
                String token = auth.getToken();
                if (token != null && !token.isEmpty()) {
                    builder.header("Authorization", "Bearer " + token);
                }
            }
            return chain.proceed(builder.build());
        };

        Authenticator tokenAuthenticator = new Authenticator() {
            @Override
            public Request authenticate(Route route, Response response) throws IOException {
                // Prevent infinite loops
                if (responseCount(response) >= 2) return null;

                String refreshToken = auth.getRefreshToken();
                if (refreshToken == null || refreshToken.isEmpty()) {
                    return null;
                }

                try {
                    Retrofit tmp = new Retrofit.Builder()
                            .baseUrl(BASE_URL)
                            .addConverterFactory(GsonConverterFactory.create())
                            .build();
                    ApiService api = tmp.create(ApiService.class);
                    Call<com.example.essycoff.model.LoginResponse> call = api.refresh(
                            new ApiService.RefreshRequest(refreshToken),
                            Constants.SUPABASE_ANON_KEY
                    );
                    retrofit2.Response<com.example.essycoff.model.LoginResponse> resp = call.execute();
                    if (resp.isSuccessful() && resp.body() != null) {
                        String newAccess = resp.body().getAccessToken();
                        String newRefresh = resp.body().getRefreshToken();
                        if (newAccess != null && !newAccess.isEmpty()) {
                            auth.saveToken(newAccess);
                            if (newRefresh != null && !newRefresh.isEmpty()) auth.saveRefreshToken(newRefresh);
                            return response.request().newBuilder()
                                    .header("Authorization", "Bearer " + newAccess)
                                    .header("apikey", Constants.SUPABASE_ANON_KEY)
                                    .build();
                        }
                    } else {
                    }
                } catch (Exception e) {
                    Log.e("RetrofitClient", "Refresh error", e);
                }
                return null;
            }
            private int responseCount(Response response) {
                int count = 1;
                while ((response = response.priorResponse()) != null) count++;
                return count;
            }
        };
        return new OkHttpClient.Builder()
            .addInterceptor(headersInterceptor)
            .authenticator(tokenAuthenticator)
            .build();
    }
}