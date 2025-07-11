package com.duta.lubanagym.ui.splash

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import com.duta.lubanagym.databinding.ActivitySplashBinding
import com.duta.lubanagym.ui.admin.AdminActivity
import com.duta.lubanagym.ui.main.MainActivity
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val viewModel: SplashViewModel by viewModels()
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper(this)

        // Initialize app with ViewModel
        viewModel.initializeApp {
            navigateBasedOnUserRole()
        }
    }

    private fun navigateBasedOnUserRole() {
        val isLoggedIn = preferenceHelper.getBoolean(Constants.PREF_IS_LOGGED_IN)
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)

        val intent = when {
            // UPDATE: Both admin and staff go to AdminActivity
            isLoggedIn && (userRole == Constants.ROLE_ADMIN || userRole == Constants.ROLE_STAFF) -> {
                Intent(this, AdminActivity::class.java)
            }
            else -> {
                // Guest, Member, Trainer, atau belum login ke MainActivity
                Intent(this, MainActivity::class.java)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

}