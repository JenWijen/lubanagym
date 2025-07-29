package com.duta.lubanagym.ui.admin

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.databinding.ActivityMemberManagementBinding
import com.duta.lubanagym.databinding.DialogMemberDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class MemberManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberManagementBinding
    private val viewModel: MemberManagementViewModel by viewModels()
    private lateinit var memberAdapter: MemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        setupSearchAndFilter()
        observeViewModel()
        loadMembers()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            try {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Manajemen Member"
            } catch (e: Exception) {
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                toolbar.setNavigationOnClickListener { onBackPressed() }
                toolbar.title = "Manajemen Member"
            }
        }
    }

    private fun setupRecyclerView() {
        memberAdapter = MemberAdapter(
            onMembershipChange = { member, newMembershipType ->
                val updates = mapOf("membershipType" to newMembershipType)
                viewModel.updateMember(member.id, updates)
            },
            onStatusChange = { member, isActive ->
                val updates = mapOf("isActive" to isActive)
                viewModel.updateMember(member.id, updates)
            },
            onViewDetail = { member ->
                showMemberDetail(member)
            }
        )

        binding.rvMembers.apply {
            adapter = memberAdapter
            layoutManager = LinearLayoutManager(this@MemberManagementActivity)
        }
    }

    private fun setupSearchAndFilter() {
        // Toggle filter visibility
        binding.btnToggleFilters.setOnClickListener {
            val isVisible = binding.layoutFilters.visibility == View.VISIBLE
            binding.layoutFilters.visibility = if (isVisible) View.GONE else View.VISIBLE
            binding.btnToggleFilters.text = if (isVisible) "🔽Filter" else "🔼Filter"
        }

        // Search functionality
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                viewModel.searchMembers(query)
            }
        })

        // Membership filter
        binding.spinnerMembershipFilter.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedMembership = when (position) {
                    0 -> null // Semua Membership
                    1 -> "basic"
                    2 -> "premium"
                    3 -> "vip"
                    else -> null
                }
                viewModel.filterByMembership(selectedMembership)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Status filter
        binding.spinnerStatusFilter.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val selectedStatus = when (position) {
                    0 -> null // Semua Status
                    1 -> true // Aktif
                    2 -> false // Tidak Aktif
                    else -> null
                }
                viewModel.filterByStatus(selectedStatus)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Sort options
        binding.spinnerSort.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                val sortType = when (position) {
                    0 -> MemberManagementViewModel.SortType.NEWEST_FIRST
                    1 -> MemberManagementViewModel.SortType.OLDEST_FIRST
                    2 -> MemberManagementViewModel.SortType.NAME_A_Z
                    3 -> MemberManagementViewModel.SortType.NAME_Z_A
                    4 -> MemberManagementViewModel.SortType.EXPIRY_SOON
                    5 -> MemberManagementViewModel.SortType.EXPIRY_LATEST
                    else -> MemberManagementViewModel.SortType.NEWEST_FIRST
                }
                viewModel.sortMembers(sortType)
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Clear filters button
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.spinnerMembershipFilter.setSelection(0)
            binding.spinnerStatusFilter.setSelection(0)
            binding.spinnerSort.setSelection(0)
            viewModel.resetFilters()
        }
    }

    private fun observeViewModel() {
        viewModel.filteredMemberList.observe(this) { result ->
            result.onSuccess { members ->
                memberAdapter.submitList(members)
                binding.progressBar.visibility = View.GONE

                // Update result count
                binding.tvResultCount.text = "Menampilkan ${members.size} member"

                if (members.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "👥 Tidak ada member yang sesuai dengan pencarian"
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Member berhasil diupdate", Toast.LENGTH_SHORT).show()
                loadMembers()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showMemberDetail(member: Member) {
        val dialogBinding = DialogMemberDetailBinding.inflate(layoutInflater)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        dialogBinding.apply {
            // Load Profile Image
            if (member.profileImageUrl.isNotEmpty()) {
                Glide.with(this@MemberManagementActivity)
                    .load(member.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(ivMemberDetailProfile)
            } else {
                ivMemberDetailProfile.setImageResource(R.drawable.ic_profile_placeholder)
            }

            // Basic Info
            tvDetailName.text = member.name.ifEmpty { "Nama belum diatur" }
            tvDetailPhone.text = member.phone.ifEmpty { "No. telepon belum diatur" }
            tvDetailMembership.text = member.membershipType.uppercase()
            tvDetailId.text = member.id.take(8)
            tvDetailQrCode.text = member.qrCode

            // Dates
            tvDetailJoinDate.text = dateFormat.format(Date(member.joinDate))
            tvDetailExpiryDate.text = dateFormat.format(Date(member.expiryDate))

            // Status
            val statusText = if (member.isActive) "✅ Aktif" else "❌ Tidak Aktif"
            val statusColor = if (member.isActive) android.R.color.holo_green_dark else android.R.color.holo_red_dark
            tvDetailStatus.text = statusText
            tvDetailStatus.setTextColor(getColor(statusColor))

            // Expiry warning
            val daysUntilExpiry = ((member.expiryDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
            when {
                daysUntilExpiry < 0 -> {
                    tvDetailExpiryWarning.text = "⚠️ Membership sudah expired ${Math.abs(daysUntilExpiry)} hari lalu"
                    tvDetailExpiryWarning.setTextColor(getColor(android.R.color.holo_red_dark))
                    tvDetailExpiryWarning.visibility = View.VISIBLE
                }
                daysUntilExpiry < 30 -> {
                    tvDetailExpiryWarning.text = "⚠️ Membership akan expired dalam $daysUntilExpiry hari"
                    tvDetailExpiryWarning.setTextColor(getColor(android.R.color.holo_orange_dark))
                    tvDetailExpiryWarning.visibility = View.VISIBLE
                }
                else -> {
                    tvDetailExpiryWarning.visibility = View.GONE
                }
            }

            // Membership badge
            val badgeColor = when (member.membershipType.lowercase()) {
                "vip" -> android.R.color.holo_orange_dark
                "premium" -> android.R.color.holo_blue_dark
                else -> android.R.color.holo_green_dark
            }
            tvDetailMembershipBadge.text = member.membershipType.uppercase()
            tvDetailMembershipBadge.setBackgroundColor(getColor(badgeColor))
        }

        MaterialAlertDialogBuilder(this)
            .setTitle("👤 Detail Member")
            .setView(dialogBinding.root)
            .setPositiveButton("✅ Tutup", null)
            .setNeutralButton("📝 Edit User", { _, _ ->
                Toast.makeText(this, "Edit user melalui User Management", Toast.LENGTH_SHORT).show()
            })
            .show()
    }

    private fun loadMembers() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadMembers()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}