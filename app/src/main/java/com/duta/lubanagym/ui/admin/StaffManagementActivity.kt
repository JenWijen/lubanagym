package com.duta.lubanagym.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.databinding.ActivityStaffManagementBinding
import com.duta.lubanagym.data.model.Staff
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class StaffManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffManagementBinding
    private val viewModel: StaffManagementViewModel by viewModels()
    private lateinit var staffAdapter: StaffAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadStaff()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            try {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Manajemen Staff"
            } catch (e: Exception) {
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                toolbar.setNavigationOnClickListener {
                    finish()
                }
                toolbar.title = "Manajemen Staff"
            }
        }
    }

    private fun setupRecyclerView() {
        staffAdapter = StaffAdapter(
            onStaffUpdate = { staff: Staff, field: String, value: Any ->
                val updates = mapOf(field to value)
                viewModel.updateStaff(staff.id, updates)
            },
            onDeleteStaff = { staff: Staff ->
                showDeleteStaffConfirmation(staff)
            }
        )

        binding.rvStaff.apply {
            adapter = staffAdapter
            layoutManager = LinearLayoutManager(this@StaffManagementActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.staffList.observe(this) { result ->
            result.onSuccess { staffList ->
                staffAdapter.submitList(staffList)
                binding.progressBar.visibility = View.GONE

                if (staffList.isEmpty()) {
                    Toast.makeText(this, "👨‍💼 Belum ada data staff", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Staff berhasil diupdate", Toast.LENGTH_SHORT).show()
                loadStaff()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Staff berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadStaff()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error menghapus: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteStaffConfirmation(staff: Staff) {
        MaterialAlertDialogBuilder(this)
            .setTitle("🗑️ Konfirmasi Hapus Staff")
            .setMessage("""
                Apakah Anda yakin ingin menghapus staff ini?
                
                👨‍💼 Nama: ${staff.name}
                📱 Telepon: ${staff.phone}
                🏷️ Posisi: ${staff.position}
                
                ⚠️ Tindakan ini tidak dapat dibatalkan!
            """.trimIndent())
            .setPositiveButton("🗑️ Ya, Hapus") { _, _ ->
                viewModel.deleteStaff(staff.id)
            }
            .setNegativeButton("❌ Batal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun loadStaff() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadStaff()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}