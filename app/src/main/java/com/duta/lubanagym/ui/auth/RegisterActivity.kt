package com.duta.lubanagym.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.duta.lubanagym.databinding.ActivityRegisterBinding
import com.duta.lubanagym.ui.main.MainActivity
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
        observeViewModel()
    }

    private fun setupClickListeners() {
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()
            val token = binding.etToken.text.toString().trim()

            if (validateInput(email, username, password, confirmPassword, token)) {
                lifecycleScope.launch {
                    viewModel.register(email, password, username, token)
                }
            }
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun validateInput(email: String, username: String, password: String, confirmPassword: String, token: String): Boolean {
        if (email.isEmpty()) {
            binding.etEmail.error = "Email tidak boleh kosong"
            return false
        }
        if (username.isEmpty()) {
            binding.etUsername.error = "Username tidak boleh kosong"
            return false
        }
        if (password.isEmpty()) {
            binding.etPassword.error = "Password tidak boleh kosong"
            return false
        }
        if (password != confirmPassword) {
            binding.etConfirmPassword.error = "Password tidak sama"
            return false
        }
        if (token.isEmpty()) {
            binding.etToken.error = "Token tidak boleh kosong"
            return false
        }
        return true
    }

    private fun observeViewModel() {
        viewModel.registerResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Registrasi berhasil! Silakan login untuk melanjutkan", Toast.LENGTH_LONG).show()

                // Langsung ke LoginActivity
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }.onFailure { error ->
                Toast.makeText(this, "Registrasi gagal: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnRegister.isEnabled = !isLoading
            binding.btnRegister.text = if (isLoading) "Loading..." else "Daftar"
        }
    }
}