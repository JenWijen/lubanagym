package com.duta.lubanagym.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.duta.lubanagym.databinding.ActivityAdminBinding
import com.duta.lubanagym.ui.auth.LoginActivity
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper(this)

        setupClickListeners()
        setupUserInfo()
    }

    private fun setupUserInfo() {
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)
        binding.tvUserRole.text = "Role: ${userRole.uppercase()}"

        // Hide some features for staff
        if (userRole == Constants.ROLE_STAFF) {
            binding.btnUserManagement.visibility = android.view.View.GONE
            binding.btnStaffManagement.visibility = android.view.View.GONE
        }
    }

    private fun setupClickListeners() {
        binding.btnUserManagement.setOnClickListener {
            startActivity(Intent(this, UserManagementActivity::class.java))
        }

        binding.btnMemberManagement.setOnClickListener {
            startActivity(Intent(this, MemberManagementActivity::class.java))
        }

        binding.btnStaffManagement.setOnClickListener {
            startActivity(Intent(this, StaffManagementActivity::class.java))
        }

        binding.btnTrainerManagement.setOnClickListener {
            startActivity(Intent(this, TrainerManagementActivity::class.java))
        }

        binding.btnEquipmentManagement.setOnClickListener {
            startActivity(Intent(this, EquipmentManagementActivity::class.java))
        }

        binding.btnTokenManagement.setOnClickListener {
            startActivity(Intent(this, TokenManagementActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        preferenceHelper.clear()
        startActivity(Intent(this, LoginActivity::class.java))
        finish()
    }
}

