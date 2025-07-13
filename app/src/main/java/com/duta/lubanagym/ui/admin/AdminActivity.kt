package com.duta.lubanagym.ui.admin

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import android.widget.Toast
import com.duta.lubanagym.databinding.ActivityAdminBinding
import com.duta.lubanagym.ui.auth.LoginActivity
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper

class AdminActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminBinding
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        preferenceHelper = PreferenceHelper(this)

        // VERIFY ADMIN/STAFF ACCESS
        if (!isAdminOrStaffUser()) {
            redirectToMainActivity()
            return
        }

        binding = ActivityAdminBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        setupUserInfo()
    }

    private fun isAdminOrStaffUser(): Boolean {
        val isLoggedIn = preferenceHelper.getBoolean(Constants.PREF_IS_LOGGED_IN)
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)
        // Allow both admin and staff
        return isLoggedIn && (userRole == Constants.ROLE_ADMIN || userRole == Constants.ROLE_STAFF)
    }

    private fun redirectToMainActivity() {
        Toast.makeText(this, "Akses ditolak. Anda bukan admin atau staff.", Toast.LENGTH_LONG).show()
        val intent = Intent(this, com.duta.lubanagym.ui.main.MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun setupUserInfo() {
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)
        binding.tvUserRole.text = "Role: ${userRole.uppercase()}"

        // Show/hide features based on role
        if (userRole == Constants.ROLE_STAFF) {
            // Staff can access limited features
            binding.btnUserManagement.visibility = android.view.View.GONE
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

        // NEW: QR Scanner button
        binding.btnQrScanner?.setOnClickListener {
            startActivity(Intent(this, QRScannerActivity::class.java))
        }

        binding.btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun logout() {
        preferenceHelper.clear()
        Toast.makeText(this, "Logout berhasil", Toast.LENGTH_SHORT).show()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        super.onBackPressed()
        // Admin/Staff tidak bisa back ke MainActivity
        // Tampilkan dialog konfirmasi logout
        showLogoutConfirmationDialog()
    }

    private fun showLogoutConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Logout")
            .setMessage("Apakah Anda ingin logout dari Admin Panel?")
            .setPositiveButton("Ya") { _, _ ->
                logout()
            }
            .setNegativeButton("Tidak") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}