package com.duta.lubanagym.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.databinding.ActivityUserManagementBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

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
        userAdapter = UserAdapter(
            onRoleChange = { user, newRole ->
                showRoleChangeConfirmation(user.username, user.role, newRole) {
                    viewModel.updateUserRole(user.id, user.role, newRole)
                }
            },
            onDeleteUser = { user -> // NEW: Delete user callback
                showDeleteUserConfirmation(user)
            }
        )

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
                loadUsers()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        // NEW: Observer for delete result
        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                loadUsers()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error menghapus: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // NEW: Show delete confirmation dialog
    private fun showDeleteUserConfirmation(user: com.duta.lubanagym.data.model.User) {
        MaterialAlertDialogBuilder(this)
            .setTitle("🗑️ Konfirmasi Hapus User")
            .setMessage("""
                ⚠️ PERINGATAN: Ini akan menghapus secara permanen!
                
                👤 User: ${user.username}
                📧 Email: ${user.email}
                🏷️ Role: ${user.role.uppercase()}
                
                🗑️ Yang akan dihapus:
                • Data user dari sistem
                • Data profil ${user.role} (jika ada)
                • Semua data terkait
                
                ❌ Tindakan ini TIDAK DAPAT dibatalkan!
            """.trimIndent())
            .setPositiveButton("🗑️ Ya, Hapus Permanen") { _, _ ->
                viewModel.deleteUser(user.id, user.role)
            }
            .setNegativeButton("❌ Batal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setCancelable(false)
            .show()
    }

    private fun showRoleChangeConfirmation(username: String, oldRole: String, newRole: String, onConfirm: () -> Unit) {
        val roleInfo = mapOf(
            "member" to Pair("👥 Member", "Akses gym standar, data member"),
            "staff" to Pair("👨‍💼 Staff", "Bantuan operasional, data staff"),
            "trainer" to Pair("🏋️ Trainer", "Pelatih fitness, data trainer"),
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

        MaterialAlertDialogBuilder(this)
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
                loadUsers()
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