package com.duta.lubanagym.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.databinding.ActivityUserManagementBinding
import com.duta.lubanagym.utils.Constants
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
        setupSearchAndFilter()
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
            onDeleteUser = { user ->
                showDeleteUserConfirmation(user)
            }
        )

        binding.rvUsers.apply {
            adapter = userAdapter
            layoutManager = LinearLayoutManager(this@UserManagementActivity)
        }
    }

    private fun setupSearchAndFilter() {
        // Search functionality
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                viewModel.searchUsers(query)
            }
        })

        // Filter by role
        binding.spinnerRoleFilter.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedRole = when (position) {
                    0 -> null // All roles
                    1 -> Constants.ROLE_GUEST
                    2 -> Constants.ROLE_MEMBER
                    3 -> Constants.ROLE_STAFF
                    4 -> Constants.ROLE_ADMIN
                    else -> null
                }
                viewModel.filterByRole(selectedRole)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Sort options
        binding.spinnerSort.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val sortType = when (position) {
                    0 -> UserManagementViewModel.SortType.NEWEST_FIRST
                    1 -> UserManagementViewModel.SortType.OLDEST_FIRST
                    2 -> UserManagementViewModel.SortType.NAME_A_Z
                    3 -> UserManagementViewModel.SortType.NAME_Z_A
                    else -> UserManagementViewModel.SortType.NEWEST_FIRST
                }
                viewModel.sortUsers(sortType)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Clear search button
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.spinnerRoleFilter.setSelection(0)
            binding.spinnerSort.setSelection(0)
            viewModel.resetFilters()
        }
    }

    private fun observeViewModel() {
        viewModel.filteredUserList.observe(this) { result ->
            result.onSuccess { users ->
                userAdapter.submitList(users)
                binding.progressBar.visibility = View.GONE

                // Update result count
                binding.tvResultCount.text = "Menampilkan ${users.size} user"

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

        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess { message ->
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                loadUsers()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error menghapus: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

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
            "guest" to Pair("👤 Guest", "Akses terbatas, belum menjadi member"),
            "member" to Pair("👥 Member", "Akses gym standar, data member"),
            "staff" to Pair("👨‍💼 Staff", "Bantuan operasional, data staff"),
            "admin" to Pair("👨‍💻 Admin", "Akses penuh sistem")
        )

        val oldRoleInfo = roleInfo[oldRole] ?: Pair("❓ Unknown", "Data tidak dikenal")
        val newRoleInfo = roleInfo[newRole] ?: Pair("❓ Unknown", "Data tidak dikenal")

        val cleanupMessage = when (oldRole) {
            newRole -> "⚠️ Tidak ada perubahan role"
            "admin", "guest" -> "ℹ️ ${oldRoleInfo.first} tidak memiliki data terpisah"
            else -> "🗑️ ${oldRoleInfo.second} akan dihapus otomatis"
        }

        val createMessage = when (newRole) {
            "admin", "guest" -> "ℹ️ ${newRoleInfo.first} tidak memerlukan profil terpisah"
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
        binding.tvEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = "👤 Tidak ada user yang sesuai dengan pencarian"
    }

    private fun hideEmptyState() {
        binding.tvEmptyState.visibility = View.GONE
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