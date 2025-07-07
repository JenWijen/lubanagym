package com.duta.lubanagym.ui.admin

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.databinding.ActivityTokenManagementBinding
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper

class TokenManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTokenManagementBinding
    private val viewModel: TokenManagementViewModel by viewModels()
    private lateinit var tokenAdapter: TokenAdapter
    private lateinit var preferenceHelper: PreferenceHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTokenManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper(this)

        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadTokens()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Manajemen Token"
        }
    }

    private fun setupClickListeners() {
        binding.btnGenerateToken.setOnClickListener {
            val userId = preferenceHelper.getString(Constants.PREF_USER_ID)
            viewModel.generateToken(userId)
        }
    }

    private fun setupRecyclerView() {
        tokenAdapter = TokenAdapter { token ->
            // Copy token to clipboard
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("Token", token.token)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "Token disalin ke clipboard", Toast.LENGTH_SHORT).show()
        }

        binding.rvTokens.apply {
            adapter = tokenAdapter
            layoutManager = LinearLayoutManager(this@TokenManagementActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.tokenList.observe(this) { result ->
            result.onSuccess { tokens ->
                tokenAdapter.submitList(tokens)
                binding.progressBar.visibility = View.GONE
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.generateResult.observe(this) { result ->
            result.onSuccess { token ->
                Toast.makeText(this, "Token berhasil dibuat: $token", Toast.LENGTH_LONG).show()
                loadTokens() // Refresh list
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTokens() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadTokens()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}