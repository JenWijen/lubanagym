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
import com.duta.lubanagym.databinding.ActivityLoginBinding
import com.duta.lubanagym.ui.admin.AdminActivity
import com.duta.lubanagym.ui.main.MainActivity
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private val viewModel: LoginViewModel by viewModels()
    private lateinit var preferenceHelper: PreferenceHelper
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
            Log.w("LoginActivity", "Google sign in failed", e)
            Toast.makeText(this, "Google Sign-In gagal: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper(this)

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
        // Email/Password Login
        binding.btnLogin.setOnClickListener {
            val emailOrUsername = binding.etEmailOrUsername.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(emailOrUsername, password)) {
                lifecycleScope.launch {
                    viewModel.login(emailOrUsername, password)
                }
            }
        }

        // Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
        }

        // Forgot Password
        binding.tvForgotPassword.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }

        binding.tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        binding.btnBack?.setOnClickListener {
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

    private fun validateInput(emailOrUsername: String, password: String): Boolean {
        when {
            emailOrUsername.isEmpty() -> {
                binding.etEmailOrUsername.error = "Email/Username tidak boleh kosong"
                return false
            }
            emailOrUsername.contains("@") && !isValidEmail(emailOrUsername) -> {
                binding.etEmailOrUsername.error = "Format email tidak valid. Email harus mengandung kombinasi huruf dan angka, minimal 8 karakter"
                return false
            }
            !emailOrUsername.contains("@") && !isValidUsername(emailOrUsername) -> {
                binding.etEmailOrUsername.error = "Username harus terdiri dari huruf saja (a-z, A-Z) minimal 3 karakter"
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
        // Email/Password Login Result
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                saveUserSession(user.id, user.role)
                Toast.makeText(this, "✅ Login berhasil! Selamat datang ${user.username}", Toast.LENGTH_SHORT).show()
                navigateBasedOnRole(user.role)
            }.onFailure { error ->
                Toast.makeText(this, "❌ Login gagal: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        // Google Sign-In Result
        viewModel.googleSignInResult.observe(this) { result ->
            result.onSuccess { user ->
                saveUserSession(user.id, user.role)
                Toast.makeText(this, "✅ Google Sign-In berhasil! Selamat datang ${user.fullName.ifEmpty { user.username }}", Toast.LENGTH_SHORT).show()
                navigateBasedOnRole(user.role)
            }.onFailure { error ->
                Toast.makeText(this, "❌ Google Sign-In gagal: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.btnLogin.isEnabled = !isLoading
            binding.btnGoogleSignIn.isEnabled = !isLoading

            if (isLoading) {
                binding.btnLogin.text = "🔄 Masuk..."
                binding.btnGoogleSignIn.text = "🔄 Masuk dengan Google..."
            } else {
                binding.btnLogin.text = "🚪 Login"
                binding.btnGoogleSignIn.text = "🔍 Masuk dengan Google"
            }
        }
    }

    private fun saveUserSession(userId: String, userRole: String) {
        preferenceHelper.saveString(Constants.PREF_USER_ID, userId)
        preferenceHelper.saveString(Constants.PREF_USER_ROLE, userRole)
        preferenceHelper.saveBoolean(Constants.PREF_IS_LOGGED_IN, true)
    }

    private fun navigateBasedOnRole(userRole: String) {
        val intent = when (userRole) {
            Constants.ROLE_ADMIN, Constants.ROLE_STAFF -> {
                Intent(this, AdminActivity::class.java)
            }
            else -> {
                Intent(this, MainActivity::class.java)
            }
        }

        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}