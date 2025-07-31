package com.duta.lubanagym.ui.auth

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.duta.lubanagym.R
import com.duta.lubanagym.databinding.ActivityRegisterBinding
import com.duta.lubanagym.ui.main.MainActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding
    private val viewModel: RegisterViewModel by viewModels()
    private lateinit var googleSignInClient: GoogleSignInClient

    // Google Sign-In launcher
    private val googleSignInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)
            account.idToken?.let { token ->
                lifecycleScope.launch {
                    viewModel.signInWithGoogle(token)
                }
            } ?: run {
                Toast.makeText(this, "Gagal mendapatkan token Google", Toast.LENGTH_SHORT).show()
            }
        } catch (e: ApiException) {
            Log.w("RegisterActivity", "Google sign in failed", e)
            Toast.makeText(this, "Google Sign-In gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupGoogleSignIn()
        setupClickListeners()
        observeViewModel()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun setupClickListeners() {
        // Register dengan email/password
        binding.btnRegister.setOnClickListener {
            val email = binding.etEmail.text.toString().trim()
            val username = binding.etUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()
            val confirmPassword = binding.etConfirmPassword.text.toString().trim()

            if (validateInput(email, username, password, confirmPassword)) {
                lifecycleScope.launch {
                    viewModel.register(email, password, username)
                }
            }
        }

        // Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        binding.tvLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun signInWithGoogle() {
        // Sign out terlebih dahulu untuk memastikan account picker muncul
        googleSignInClient.signOut().addOnCompleteListener {
            val signInIntent = googleSignInClient.signInIntent
            googleSignInLauncher.launch(signInIntent)
        }
    }

    private fun validateInput(email: String, username: String, password: String, confirmPassword: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.etEmail.error = "Email tidak boleh kosong"
                return false
            }
            !isValidEmail(email) -> {
                binding.etEmail.error = "Format email tidak valid. Email harus mengandung kombinasi huruf dan angka, minimal 8 karakter"
                return false
            }
            username.isEmpty() -> {
                binding.etUsername.error = "Username tidak boleh kosong"
                return false
            }
            !isValidUsername(username) -> {
                binding.etUsername.error = "Username harus terdiri dari huruf saja (a-z, A-Z) minimal 3 karakter"
                return false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password tidak boleh kosong"
                return false
            }
            password.length < 8 -> {
                binding.etPassword.error = "Password minimal 8 karakter"
                return false
            }
            !isValidPassword(password) -> {
                binding.etPassword.error = "Password harus mengandung kombinasi huruf dan angka"
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

    private fun isValidUsername(username: String): Boolean {
        // Username hanya boleh huruf (a-z, A-Z)
        val regex = Regex("^[a-zA-Z]+$")
        return regex.matches(username) && username.length >= 3
    }

    private fun isValidPassword(password: String): Boolean {
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        return hasLetter && hasDigit
    }

    private fun observeViewModel() {
        viewModel.registerResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Registrasi berhasil! Selamat datang di Lubana Gym!", Toast.LENGTH_LONG).show()
                navigateToMain()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Registrasi gagal: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.googleSignInResult.observe(this) { result ->
            result.onSuccess { user ->
                Toast.makeText(this, "✅ Login Google berhasil! Selamat datang ${user.fullName.ifEmpty { user.username }}!", Toast.LENGTH_LONG).show()
                navigateToMain()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Google Sign-In gagal: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnRegister.isEnabled = !isLoading
            binding.btnGoogleSignIn.isEnabled = !isLoading

            if (isLoading) {
                binding.btnRegister.text = "🔄 Mendaftar..."
                binding.btnGoogleSignIn.text = "🔄 Masuk dengan Google..."
            } else {
                binding.btnRegister.text = "📝 Daftar Sekarang"
                binding.btnGoogleSignIn.text = "🔍 Masuk dengan Google"
            }
        }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}