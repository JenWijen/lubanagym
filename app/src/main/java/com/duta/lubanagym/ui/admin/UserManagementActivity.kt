package com.duta.lubanagym.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.databinding.ActivityUserManagementBinding

class UserManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityUserManagementBinding
    private val viewModel: UserManagementViewModel by viewModels()
    private lateinit var userAdapter: UserAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadUsers()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            try {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Manajemen User"
            } catch (e: Exception) {
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                toolbar.setNavigationOnClickListener { onBackPressed() }
                toolbar.title = "Manajemen User"
            }
        }
    }

    private fun setupRecyclerView() {
        userAdapter = UserAdapter { user, newRole ->
            // Konfirmasi perubahan role dengan info cleanup
            showRoleChangeConfirmation(user.username, user.role, newRole) {
                viewModel.updateUserRole(user.id, user.role, newRole)
            }
        }

        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.userList.observe(this) { result ->
            result.onSuccess { users ->
                userAdapter.submitList(users)
                binding.progressBar.visibility = View.GONE

                if (users.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                loadUsers() // Refresh data
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showRoleChangeConfirmation(username: String, oldRole: String, newRole: String, onConfirm: () -> Unit) {
        val roleInfo = mapOf(
            "member" to Pair("👥 Member", "Akses gym standar, data member"),
            "staff" to Pair("👨‍💼 Staff", "Bantuan operasional, data staff"),
            "trainer" to Pair("🏋️ Trainer", "Pelatih fitness, data trainer"), // NEW
            "admin" to Pair("👨‍💻 Admin", "Akses penuh sistem")
        )

        val oldRoleInfo = roleInfo[oldRole] ?: Pair("❓ Unknown", "Data tidak dikenal")
        val newRoleInfo = roleInfo[newRole] ?: Pair("❓ Unknown", "Data tidak dikenal")

        val cleanupMessage = when (oldRole) {
            newRole -> "⚠️ Tidak ada perubahan role"
            "admin" -> "ℹ️ Admin tidak memiliki data terpisah"
            else -> "🗑️ ${oldRoleInfo.second} akan dihapus otomatis"
        }

        val createMessage = when (newRole) {
            "admin" -> "ℹ️ Admin tidak memerlukan profil terpisah"
            else -> "✨ ${newRoleInfo.second} akan dibuat otomatis"
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🔄 Konfirmasi Perubahan Role")
            .setMessage("""
                👤 User: $username
                
                📝 Role Lama: ${oldRoleInfo.first}
                ➡️ Role Baru: ${newRoleInfo.first}
                
                🔄 Yang akan terjadi:
                $cleanupMessage
                $createMessage
                
                ⚠️ Perubahan ini tidak dapat dibatalkan!
            """.trimIndent())
            .setPositiveButton("✅ Ya, Ubah Role") { _, _ ->
                onConfirm()
            }
            .setNegativeButton("❌ Batal") { dialog, _ ->
                dialog.dismiss()
                loadUsers() // Refresh to reset spinner
            }
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
    }

    private fun showEmptyState() {
        Toast.makeText(this, "👤 Belum ada data user", Toast.LENGTH_SHORT).show()
    }

    private fun hideEmptyState() {
        // Nothing to hide since no empty state view
    }

    private fun loadUsers() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadUsers()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}