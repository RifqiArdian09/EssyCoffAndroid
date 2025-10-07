package com.example.essycoff.utils;

import android.util.Log;

public class ImageUrlHelper {
    private static final String TAG = "ImageUrlHelper";
    
    /**
     * Fixes malformed Supabase storage URLs that are missing the slash
     * between the domain and storage path.
     * 
     * @param imageUrl The original image URL
     * @return The corrected image URL
     */
    public static String fixSupabaseUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return imageUrl;
        }
        
        // Fix the common malformed URL pattern
        if (imageUrl.contains("supabase.costorage")) {
            String correctedUrl = imageUrl.replace("supabase.costorage", "supabase.co/storage");
            Log.d(TAG, "Fixed malformed URL: " + imageUrl + " -> " + correctedUrl);
            return correctedUrl;
        }
        
        return imageUrl;
    }
    
    /**
     * Validates if a URL is a properly formatted Supabase storage URL
     * 
     * @param imageUrl The image URL to validate
     * @return true if the URL appears to be valid, false otherwise
     */
    public static boolean isValidSupabaseUrl(String imageUrl) {
        if (imageUrl == null || imageUrl.isEmpty()) {
            return false;
        }
        
        return imageUrl.startsWith("https://") && 
               imageUrl.contains("supabase.co/storage/v1/object/public/");
    }
}
