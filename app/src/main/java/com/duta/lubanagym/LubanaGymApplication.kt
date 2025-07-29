package com.duta.lubanagym

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.duta.lubanagym.utils.CloudinaryService
import com.google.firebase.FirebaseApp

class LubanaGymApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        FirebaseApp.initializeApp(this)
        CloudinaryService.initialize(this) // Initialize Cloudinary
    }
}