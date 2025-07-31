package com.duta.lubanagym.ui.auth

import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.duta.lubanagym.databinding.ActivityForgotPasswordBinding
import kotlinx.coroutines.launch

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivityForgotPasswordBinding
    private val viewModel: ForgotPasswordViewModel by viewModels()
    private var currentStep = 1 // 1: Enter Email, 2: Enter Token & New Password

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityForgotPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupClickListeners()
        observeViewModel()
        showStep1()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Lupa Password"
        }
    }

    private fun setupClickListeners() {
        binding.btnSendToken.setOnClickListener {
            if (currentStep == 1) {
                sendResetToken()
            } else {
                resetPassword()
            }
        }

        binding.btnBack.setOnClickListener {
            if (currentStep == 2) {
                showStep1()
            } else {
                finish()
            }
        }
    }

    private fun showStep1() {
        currentStep = 1
        binding.layoutStep1.visibility = android.view.View.VISIBLE
        binding.layoutStep2.visibility = android.view.View.GONE
        binding.btnSendToken.text = "📧 Kirim Token Reset"
        supportActionBar?.subtitle = "Masukkan email Anda"
    }

    private fun showStep2(email: String) {
        currentStep = 2
        binding.layoutStep1.visibility = android.view.View.GONE
        binding.layoutStep2.visibility = android.view.View.VISIBLE
        binding.btnSendToken.text = "🔄 Reset Password"
        binding.tvEmailInfo.text = "Token telah dikirim ke: $email"
        supportActionBar?.subtitle = "Masukkan token dan password baru"
    }

    private fun sendResetToken() {
        val email = binding.etEmail.text.toString().trim()

        if (!validateEmail(email)) return

        lifecycleScope.launch {
            viewModel.sendPasswordResetToken(email)
        }
    }

    private fun resetPassword() {
        val email = binding.etEmail.text.toString().trim()
        val token = binding.etResetToken.text.toString().trim()
        val newPassword = binding.etNewPassword.text.toString().trim()
        val confirmPassword = binding.etConfirmPassword.text.toString().trim()

        if (!validateResetInput(token, newPassword, confirmPassword)) return

        lifecycleScope.launch {
            viewModel.resetPassword(email, token, newPassword)
        }
    }

    private fun validateEmail(email: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.etEmail.error = "Email tidak boleh kosong"
                return false
            }
            !isValidEmail(email) -> {
                binding.etEmail.error = "Format email tidak valid"
                return false
            }
            else -> return true
        }
    }

    private fun validateResetInput(token: String, password: String, confirmPassword: String): Boolean {
        when {
            token.isEmpty() -> {
                binding.etResetToken.error = "Token tidak boleh kosong"
                return false
            }
            token.length != 32 -> {
                binding.etResetToken.error = "Token reset tidak valid"
                return false
            }
            password.isEmpty() -> {
                binding.etNewPassword.error = "Password baru tidak boleh kosong"
                return false
            }
            password.length < 8 -> {
                binding.etNewPassword.error = "Password minimal 8 karakter"
                return false
            }
            !isValidPassword(password) -> {
                binding.etNewPassword.error = "Password harus mengandung kombinasi huruf dan angka"
                return false
            }
            password != confirmPassword -> {
                binding.etConfirmPassword.error = "Password tidak sama"
                return false
            }
            else -> return true
        }
    }

    private fun isValidEmail(email: String): Boolean {
        val emailPattern = "^[a-zA-Z0-9._-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$"
        val regex = Regex(emailPattern)

        // Email harus mengandung kombinasi huruf dan angka
        val hasLetter = email.any { it.isLetter() }
        val hasDigit = email.any { it.isDigit() }

        return regex.matches(email) && hasLetter && hasDigit && email.length >= 8
    }

    private fun isValidPassword(password: String): Boolean {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    private fun observeViewModel() {
        viewModel.sendTokenResult.observe(this) { result ->
            result.onSuccess { token ->
                val email = binding.etEmail.text.toString().trim()

                // In real app, token would be sent via email
                // For demo purposes, we show it to the user
                Toast.makeText(
                    this,
                    "Token reset: $token\n(Dalam aplikasi nyata, token akan dikirim via email)",
                    Toast.LENGTH_LONG
                ).show()

                showStep2(email)
            }.onFailure { error ->
                Toast.makeText(this, "❌ ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.resetPasswordResult.observe(this) { result ->
            result.onSuccess { message ->
                Toast.makeText(this, "✅ $message", Toast.LENGTH_LONG).show()

                // Navigate back to login
                finish()
            }.onFailure { error ->
                Toast.makeText(this, "❌ ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnSendToken.isEnabled = !isLoading

            if (isLoading) {
                if (currentStep == 1) {
                    binding.btnSendToken.text = "🔄 Mengirim..."
                } else {
                    binding.btnSendToken.text = "🔄 Mereset..."
                }
            } else {
                if (currentStep == 1) {
                    binding.btnSendToken.text = "📧 Kirim Token Reset"
                } else {
                    binding.btnSendToken.text = "🔄 Reset Password"
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        if (currentStep == 2) {
            showStep1()
            return true
        }
        return super.onSupportNavigateUp()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (currentStep == 2) {
            showStep1()
        } else {
            super.onBackPressed()
        }
    }
}