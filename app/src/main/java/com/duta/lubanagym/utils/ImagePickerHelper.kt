package com.duta.lubanagym.utils

import android.content.Intent
import android.net.Uri
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class SimpleImagePicker(private val activity: AppCompatActivity) {

    private var onImageSelected: ((Uri?) -> Unit)? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<String>

    fun setup(onImageSelected: (Uri?) -> Unit) {
        this.onImageSelected = onImageSelected

        imagePickerLauncher = activity.registerForActivityResult(
            ActivityResultContracts.GetContent()
        ) { uri ->
            onImageSelected(uri)
        }
    }

    fun pickImage() {
        try {
            imagePickerLauncher.launch("image/*")
        } catch (e: Exception) {
            onImageSelected?.invoke(null)
        }
    }
}