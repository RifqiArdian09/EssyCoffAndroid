package com.example.essycoff.api;

import com.example.essycoff.model.LoginResponse;
import com.example.essycoff.model.Order;
import com.example.essycoff.model.OrderItem;
import com.example.essycoff.model.Product;

import java.util.List;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Headers;
import retrofit2.http.PATCH;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface ApiService {

    // 🔐 Auth: Login
    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=password")
    Call<LoginResponse> login(@Body LoginRequest body, @Header("apikey") String apiKey);

    // 🔐 Auth: Refresh Token
    @Headers("Content-Type: application/json")
    @POST("auth/v1/token?grant_type=refresh_token")
    Call<LoginResponse> refresh(@Body RefreshRequest body, @Header("apikey") String apiKey);

    // 🛒 Products - CRUD operations
    @GET("rest/v1/products")
    Call<List<Product>> getProducts(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @POST("rest/v1/products")
    Call<List<Product>> createProduct(
            @Body Product product,
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/products")
    Call<List<Product>> updateProduct(
            @Query("id") String productId,
            @Body Product product,
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );

    @DELETE("rest/v1/products")
    Call<ResponseBody> deleteProduct(
            @Query("id") String productId,
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );

    @POST("storage/v1/object/{bucket}/{fileName}")
    @Headers({
            "Content-Type: application/octet-stream",
            "x-upsert: true"
    })
    Call<ResponseBody> uploadFileToStorage(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Path("bucket") String bucketName,
            @Path("fileName") String fileName,
            @Body RequestBody file
    );

    // 💵 Transaksi: Orders
    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @POST("rest/v1/orders")
    Call<List<Order>> createOrder(
            @Body Order order,
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );

    // ✅ FIX: Tetap menggunakan List<OrderItem> karena memang insert multiple items
    @Headers({"Content-Type: application/json", "Prefer: return=representation"})
    @POST("rest/v1/order_items")
    Call<List<OrderItem>> insertOrderItems(
            @Body List<OrderItem> items,
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );

    @Headers({
            "Content-Type: application/json",
            "Prefer: return=representation"
    })
    @PATCH("rest/v1/products")
    Call<List<Product>> updateProductStock(
            @Query("id") String id,
            @Body Product product,
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth
    );

    // 📜 Riwayat
    @GET("rest/v1/orders")
    Call<List<Order>> getOrders(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("user_id") String user_id,
            @Query("order") String order
    );

    // 📜 Riwayat dengan filter tambahan menggunakan PostgREST and-clause
    @GET("rest/v1/orders")
    Call<List<Order>> getOrders(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("user_id") String user_id,
            @Query("order") String order,
            @Query("and") String andClause
    );

    // ApiService.java
    @GET("rest/v1/order_items")
    Call<List<OrderItem>> getOrderItems(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("order_id") String order_id  // eq.orderId
    );

    // Bulk fetch order items by multiple order IDs using PostgREST IN filter
    @GET("rest/v1/order_items")
    Call<List<OrderItem>> getOrderItemsByOrderIds(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("order_id") String inOrderIds // in.(id1,id2,...)
    );

    @DELETE("rest/v1/order_items")
    Call<ResponseBody> deleteOrderItems(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("order_id") String orderId  // eq.orderId
    );

    @DELETE("rest/v1/orders")
    Call<ResponseBody> deleteOrder(
            @Header("apikey") String apiKey,
            @Header("Authorization") String auth,
            @Query("id") String orderId  // eq.orderId
    );


    // Login Request Class
    class LoginRequest {
        public String email;
        public String password;

        public LoginRequest(String email, String password) {
            this.email = email;
            this.password = password;
        }
    }

    // Refresh Request Class
    class RefreshRequest {
        public String refresh_token;

        public RefreshRequest(String refreshToken) {
            this.refresh_token = refreshToken;
        }
    }
}