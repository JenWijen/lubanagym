package com.duta.lubanagym.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.duta.lubanagym.databinding.ActivitySplashBinding
import com.duta.lubanagym.ui.main.MainActivity

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize app with ViewModel
        viewModel.initializeApp {
            navigateToHome()
        }
    }

    private fun navigateToHome() {
        // SELALU ke MainActivity (Home), tidak cek login status
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }
}