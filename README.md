# EssyCoff - Coffee Shop Management Android App

![EssyCoff Logo](app/src/main/res/drawable/logo3.png)

EssyCoff adalah aplikasi manajemen kedai kopi berbasis Android yang memungkinkan pengelolaan produk, transaksi, dan riwayat penjualan dengan mudah dan efisien. Aplikasi ini menggunakan Supabase sebagai backend untuk autentikasi dan penyimpanan data.

## 📱 Fitur Utama

### 🔐 **Autentikasi**
- Login dengan email dan password
- Sistem token-based authentication
- Auto refresh token
- Logout dengan konfirmasi

### 📊 **Dashboard**
- Overview statistik penjualan
- Ringkasan data produk dan transaksi
- Navigasi cepat ke fitur utama

### 🛒 **Manajemen Produk**
- Tambah, edit, dan hapus produk
- Upload gambar produk
- Kelola stok produk
- Pencarian dan filter produk

### 💰 **Sistem Transaksi**
- Buat transaksi penjualan baru
- Keranjang belanja interaktif
- Kalkulasi otomatis subtotal dan total
- Manajemen pelanggan

### 📜 **Riwayat Transaksi**
- Lihat semua transaksi sebelumnya
- Filter berdasarkan tanggal
- Detail item per transaksi
- Export data ke Excel

## 🏗️ Arsitektur Aplikasi

### **Tech Stack**
- **Language**: Java
- **Framework**: Android Native
- **Backend**: Supabase (PostgreSQL)
- **HTTP Client**: Retrofit2
- **Image Loading**: Glide
- **UI Components**: Material Design
- **Navigation**: ViewPager2 + Bottom Navigation

### **Struktur Project**
```
app/src/main/java/com/example/essycoff/
├── MainActivity.java                 # Activity utama dengan bottom navigation
├── auth/
│   └── LoginActivity.java           # Halaman login
├── ui/
│   ├── DashboardFragment.java       # Fragment dashboard
│   ├── ProductsFragment.java        # Fragment manajemen produk
│   ├── TransactionsFragment.java    # Fragment transaksi
│   └── HistoryFragment.java         # Fragment riwayat
├── model/
│   ├── Product.java                 # Model produk
│   ├── Order.java                   # Model pesanan
│   ├── OrderItem.java               # Model item pesanan
│   ├── CartItem.java                # Model item keranjang
│   └── LoginResponse.java           # Model response login
├── adapter/
│   ├── ProductAdapter.java          # Adapter untuk list produk
│   ├── CartAdapter.java             # Adapter untuk keranjang
│   ├── TransactionAdapter.java      # Adapter untuk transaksi
│   └── OrderItemAdapter.java        # Adapter untuk item pesanan
├── api/
│   ├── ApiService.java              # Interface API endpoints
│   └── RetrofitClient.java          # Konfigurasi Retrofit
└── utils/
    ├── AuthManager.java             # Manajemen autentikasi
    ├── Constants.java               # Konstanta aplikasi
    └── ImageUrlHelper.java          # Helper untuk URL gambar
```

## 🗄️ Database Schema

![Database Schema](doc/db.png)

### **Tabel Database:**

#### **products**
- `id` (uuid) - Primary Key
- `name` (text) - Nama produk
- `price` (numeric) - Harga produk
- `stock` (int4) - Jumlah stok
- `image_url` (text) - URL gambar produk
- `created_at` (timestamptz) - Waktu dibuat

#### **orders**
- `id` (uuid) - Primary Key
- `order_number` (text) - Nomor pesanan
- `customer_name` (text) - Nama pelanggan
- `subtotal` (numeric) - Subtotal sebelum pajak
- `tax` (numeric) - Pajak
- `change` (numeric) - Kembalian
- `user_id` (uuid) - ID user yang membuat pesanan
- `created_at` (timestamptz) - Waktu dibuat

#### **order_items**
- `id` (uuid) - Primary Key
- `order_id` (uuid) - Foreign Key ke orders
- `product_id` (uuid) - Foreign Key ke products
- `qty` (int4) - Jumlah item
- `price` (numeric) - Harga per item
- `product_name` (text) - Nama produk (snapshot)

## 🔄 Application Flow

![Application Flowchart](doc/flowchart-essycoff.png)

### **Alur Aplikasi:**

1. **Login Process**
   - User memasukkan email dan password
   - Validasi kredensial melalui Supabase Auth
   - Simpan token dan user data
   - Redirect ke MainActivity

2. **Main Navigation**
   - Bottom Navigation dengan 4 tab utama
   - ViewPager2 untuk smooth navigation
   - Auto refresh data saat berpindah tab

3. **Product Management**
   - CRUD operations untuk produk
   - Upload gambar ke Supabase Storage
   - Real-time stock management

4. **Transaction Process**
   - Pilih produk dan masukkan ke keranjang
   - Input data pelanggan
   - Kalkulasi otomatis pajak dan total
   - Simpan order dan order items
   - Update stok produk

5. **History & Reporting**
   - Tampilkan riwayat transaksi
   - Filter dan pencarian
   - Export ke Excel

## 🚀 Instalasi dan Setup

### **Prerequisites**
- Android Studio Arctic Fox atau lebih baru
- Android SDK API Level 24+
- Java 8+
- Koneksi internet untuk akses Supabase

### **Setup Project**

1. **Clone Repository**
   ```bash
   git clone <repository-url>
   cd essycoff3
   ```

2. **Open in Android Studio**
   - Buka Android Studio
   - File → Open → Pilih folder project
   - Tunggu Gradle sync selesai

3. **Konfigurasi Backend**
   - Pastikan Supabase project sudah setup
   - Update `Constants.java` jika diperlukan:
     ```java
     public static final String SUPABASE_URL = "your-supabase-url";
     public static final String SUPABASE_ANON_KEY = "your-anon-key";
     ```

4. **Build dan Run**
   ```bash
   ./gradlew assembleDebug
   ```

### **Database Setup**
1. Buat project baru di [Supabase](https://supabase.com)
2. Jalankan SQL berikut untuk membuat tabel:
   ```sql
   -- Tabel products
   CREATE TABLE products (
     id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
     name TEXT NOT NULL,
     price NUMERIC NOT NULL,
     stock INTEGER NOT NULL DEFAULT 0,
     image_url TEXT,
     created_at TIMESTAMPTZ DEFAULT NOW()
   );

   -- Tabel orders
   CREATE TABLE orders (
     id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
     order_number TEXT NOT NULL,
     customer_name TEXT NOT NULL,
     subtotal NUMERIC NOT NULL,
     tax NUMERIC NOT NULL DEFAULT 0,
     change NUMERIC NOT NULL DEFAULT 0,
     user_id UUID NOT NULL,
     created_at TIMESTAMPTZ DEFAULT NOW()
   );

   -- Tabel order_items
   CREATE TABLE order_items (
     id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
     order_id UUID REFERENCES orders(id) ON DELETE CASCADE,
     product_id UUID REFERENCES products(id),
     qty INTEGER NOT NULL,
     price NUMERIC NOT NULL,
     product_name TEXT NOT NULL
   );
   ```

3. Setup Row Level Security (RLS) sesuai kebutuhan
4. Konfigurasi Storage bucket untuk gambar produk

## 📦 Dependencies

### **Core Dependencies**
```gradle
implementation 'androidx.appcompat:appcompat:1.6.1'
implementation 'com.google.android.material:material:1.11.0'
implementation 'androidx.constraintlayout:constraintlayout:2.1.4'
implementation 'androidx.viewpager2:viewpager2:1.1.0'
```

### **Networking**
```gradle
implementation 'com.squareup.retrofit2:retrofit:2.9.0'
implementation 'com.squareup.retrofit2:converter-gson:2.9.0'
implementation 'com.squareup.okhttp3:logging-interceptor:4.12.0'
```

### **Image Loading**
```gradle
implementation 'com.github.bumptech.glide:glide:4.16.0'
annotationProcessor 'com.github.bumptech.glide:compiler:4.16.0'
```

### **Utilities**
```gradle
implementation 'com.google.code.gson:gson:2.10.1'
implementation 'org.dhatim:fastexcel:0.19.0'
```

## 🔧 Konfigurasi

### **Network Security**
Aplikasi menggunakan network security config untuk HTTPS:
```xml
<!-- res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="false">
        <domain includeSubdomains="true">supabase.co</domain>
    </domain-config>
</network-security-config>
```

### **Permissions**
```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" />
```

## 🎨 UI/UX Features

- **Material Design 3** components
- **Dark/Light theme** support
- **Responsive layout** untuk berbagai ukuran layar
- **Smooth animations** dan transitions
- **Loading states** dan error handling
- **Intuitive navigation** dengan bottom navigation

## 🔒 Security

- **Token-based authentication** dengan Supabase
- **Automatic token refresh**
- **Secure API key management**
- **Input validation** dan sanitization
- **HTTPS-only** communication

## 📊 Performance

- **Lazy loading** untuk list data
- **Image caching** dengan Glide
- **Efficient database queries**
- **Memory management** optimization
- **Background thread** untuk network calls

## 🐛 Troubleshooting

### **Common Issues**

1. **Build Error: "Duplicate class"**
   ```bash
   ./gradlew clean
   ./gradlew build
   ```

2. **Network Error: "Unable to resolve host"**
   - Periksa koneksi internet
   - Pastikan Supabase URL dan API key benar

3. **Authentication Error**
   - Periksa kredensial login
   - Pastikan user sudah terdaftar di Supabase

4. **Image Upload Failed**
   - Periksa storage bucket configuration
   - Pastikan file permissions benar

## 📝 Contributing

1. Fork repository
2. Create feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to branch (`git push origin feature/AmazingFeature`)
5. Open Pull Request

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

## 👥 Team

- **Developer**: [Your Name]
- **Designer**: [Designer Name]
- **Project Manager**: [PM Name]

## 📞 Support

Untuk bantuan dan support:
- Email: support@essycoff.com
- GitHub Issues: [Create Issue](https://github.com/your-repo/issues)
- Documentation: [Wiki](https://github.com/your-repo/wiki)

---

**EssyCoff** - Simplifying Coffee Shop Management ☕️