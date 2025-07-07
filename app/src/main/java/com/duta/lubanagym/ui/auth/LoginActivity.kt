package com.duta.lubanagym.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.duta.lubanagym.databinding.ActivityLoginBinding
import com.duta.lubanagym.ui.admin.AdminActivity
import com.duta.lubanagym.ui.main.MainActivity
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper(this)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnLogin.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                lifecycleScope.launch {
                    viewModel.login(email, password)
                }
            }
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Back button - kembali ke MainActivity
        binding.btnBack?.setOnClickListener {
            finish() // Kembali ke MainActivity
        }
    }

    private fun validateInput(email: String, password: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email tidak boleh kosong"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password tidak boleh kosong"
            return false
        }
        return true
    }

    private fun observeViewModel() {
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                // Save user data
                preferenceHelper.saveString(Constants.PREF_USER_ID, user.id)
                preferenceHelper.saveString(Constants.PREF_USER_ROLE, user.role)
                preferenceHelper.saveBoolean(Constants.PREF_IS_LOGGED_IN, true)

                Toast.makeText(this, "Login berhasil! Selamat datang ${user.username}", Toast.LENGTH_SHORT).show()

                // Navigate based on role
                if (user.role == Constants.ROLE_ADMIN || user.role == Constants.ROLE_STAFF) {
                    // Admin/Staff langsung ke AdminActivity
                    val intent = Intent(this, AdminActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    // Member kembali ke MainActivity (akan otomatis show profile)
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                    startActivity(intent)
                }

                finish()
            }.onFailure { error ->
                Toast.makeText(this, "Login gagal: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.btnLogin.text = if (isLoading) "Loading..." else "Login"
        }
    }
}