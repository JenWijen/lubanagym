package com.duta.lubanagym.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.databinding.ActivityStaffManagementBinding

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
                toolbar.setNavigationOnClickListener { onBackPressed() }
                toolbar.title = "Manajemen Staff"
            }
        }
    }

    private fun setupRecyclerView() {
        staffAdapter = StaffAdapter { staff, field, value ->
            val updates = mapOf(field to value)
            viewModel.updateStaff(staff.id, updates)
        }

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
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Staff berhasil diupdate", Toast.LENGTH_SHORT).show()
                loadStaff()
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadStaff() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadStaff()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}
