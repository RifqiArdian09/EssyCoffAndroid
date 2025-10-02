# EssyCoff POS (Android)

Aplikasi POS sederhana untuk kedai kopi: kelola produk, transaksi, dan riwayat dengan antarmuka Material 3.

## Ringkasan Proyek
- **Module**: `:app`
- **Application ID**: `com.example.essycoff`
- **Min SDK**: 24
- **Target/Compile SDK**: 34
- **Bahasa**: Java
- **UI**: Material 3 dengan tema siang/malam (DayNight)
- **Launcher**: `com.example.essycoff.auth.LoginActivity`
- **Setelah login**: `com.example.essycoff.MainActivity` (Bottom Navigation)

## Fitur Utama
- **Autentikasi dasar**: Login, simpan token/email di SharedPreferences (`PREF_NAME`, `KEY_TOKEN`, `KEY_EMAIL`, `KEY_USER_UUID`).
- **Navigasi bawah (BottomNavigationView)** di `MainActivity` dengan 3 fragment utama:
  - `ProductsFragment` (daftar produk)
  - `TransactionsFragment` (transaksi berjalan)
  - `HistoryFragment` (riwayat)
- **Logout cepat** dari tab profil (dialog konfirmasi, lalu clear session).
- **Tema Material 3** dengan dukungan light/dark melalui `values/themes.xml` dan `values-night/themes.xml`.

## Struktur Proyek (ringkas)
```
essycoff3/
├─ app/
│  ├─ src/main/
│  │  ├─ java/com/example/essycoff/
│  │  │  ├─ MainActivity.java
│  │  │  ├─ auth/LoginActivity.java
│  │  │  ├─ ui/
│  │  │  │  ├─ ProductsFragment.java
│  │  │  │  ├─ TransactionsFragment.java
│  │  │  │  └─ HistoryFragment.java
│  │  │  └─ utils/
│  │  │     ├─ AuthManager.java
│  │  │     └─ Constants.java
│  │  ├─ res/
│  │  │  ├─ layout/ ...
│  │  │  ├─ drawable/ ...
│  │  │  ├─ values/colors.xml
│  │  │  ├─ values/themes.xml
│  │  │  └─ values-night/themes.xml
│  │  └─ AndroidManifest.xml
│  └─ build.gradle
├─ settings.gradle
├─ build.gradle (root)
└─ README.md
```

Catatan: Beberapa file/folder mungkin berbeda atau tidak ditampilkan seluruhnya di ringkasan ini.

## Konfigurasi & Secrets
- File `Constants.java` berisi konfigurasi Supabase:
  - `SUPABASE_URL`
  - `SUPABASE_ANON_KEY`
- Untuk keamanan produksi, jangan commit kunci sensitif langsung ke repository. Pertimbangkan untuk memindahkan nilai ke solusi yang lebih aman (misalnya: BuildConfig via `local.properties`, atau remote config/secret manager) dan gunakan proguard/obfuscation untuk rilis.

