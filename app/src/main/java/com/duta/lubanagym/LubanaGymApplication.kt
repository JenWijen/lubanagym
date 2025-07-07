package com.duta.lubanagym

import android.app.Application
import com.duta.lubanagym.utils.CloudinaryService
import com.google.firebase.FirebaseApp

class LubanaGymApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
        CloudinaryService.initialize(this) // Initialize Cloudinary
    }
}