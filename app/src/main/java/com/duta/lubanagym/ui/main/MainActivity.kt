package com.duta.lubanagym.ui.main

import android.content.Intent
import android.os.Bundle
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.duta.lubanagym.R
import com.duta.lubanagym.databinding.ActivityMainBinding
import com.duta.lubanagym.ui.admin.AdminActivity
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferenceHelper = PreferenceHelper(this)

        // CHECK ADMIN ACCESS - REDIRECT JIKA ADMIN/STAFF
        checkAdminAccess()

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupBottomNavigation()

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(HomeFragment())
        }
    }

    private fun checkAdminAccess() {
        val isLoggedIn = preferenceHelper.getBoolean(Constants.PREF_IS_LOGGED_IN)
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)

        // UPDATED: Hanya admin dan staff yang redirect, HAPUS trainer dari sini
        if (isLoggedIn && (userRole == Constants.ROLE_ADMIN || userRole == Constants.ROLE_STAFF)) {
            // Admin & Staff tidak boleh mengakses MainActivity, redirect ke AdminActivity
            val intent = Intent(this, AdminActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
            return
        }
    }

    override fun onResume() {
        super.onResume()
        // Check lagi saat resume untuk memastikan admin/staff tidak bisa bypass
        checkAdminAccess()
    }

    private fun setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_home -> {
                    loadFragment(HomeFragment())
                    true
                }
                R.id.nav_equipment -> {
                    loadFragment(EquipmentFragment())
                    true
                }
                R.id.nav_profile -> {
                    loadFragment(ProfileFragment())
                    true
                }
                else -> false
            }
        }
    }

    private fun loadFragment(fragment: Fragment) {
        supportFragmentManager.beginTransaction()
            .replace(R.id.container, fragment)
            .commit()
    }
}