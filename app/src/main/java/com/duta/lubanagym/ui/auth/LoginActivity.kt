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
            val email = binding.etEmail.text.toString().trim()
            val password = binding.etPassword.text.toString().trim()

            if (validateInput(email, password)) {
                lifecycleScope.launch {
                    viewModel.login(email, password)
                }
            }
        }

        // Google Sign-In
        binding.btnGoogleSignIn.setOnClickListener {
            signInWithGoogle()
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

    private fun validateInput(email: String, password: String): Boolean {
        when {
            email.isEmpty() -> {
                binding.etEmail.error = "Email tidak boleh kosong"
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                binding.etEmail.error = "Format email tidak valid"
                return false
            }
            password.isEmpty() -> {
                binding.etPassword.error = "Password tidak boleh kosong"
                return false
            }
            password.length < 6 -> {
                binding.etPassword.error = "Password minimal 6 karakter"
                return false
            }
            else -> return true
        }
    }

    private fun observeViewModel() {
        // Email/Password Login Result
        viewModel.loginResult.observe(this) { result ->
            result.onSuccess { user ->
                saveUserSession(user.id, user.role)
                Toast.makeText(this, "✅ Login berhasil! Selamat datang ${user.username}", Toast.LENGTH_SHORT).show()
                navigateBasedOnRole(user.role)
            }.onFailure { error ->
                Toast.makeText(this, "❌ Login gagal: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        // Google Sign-In Result
        viewModel.googleSignInResult.observe(this) { result ->
            result.onSuccess { user ->
                saveUserSession(user.id, user.role)
                Toast.makeText(this, "✅ Google Sign-In berhasil! Selamat datang ${user.fullName.ifEmpty { user.username }}", Toast.LENGTH_SHORT).show()
                navigateBasedOnRole(user.role)
            }.onFailure { error ->
                Toast.makeText(this, "❌ Google Sign-In gagal: ${error.message}", Toast.LENGTH_SHORT).show()
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