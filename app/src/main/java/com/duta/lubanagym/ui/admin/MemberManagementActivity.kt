package com.duta.lubanagym.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.databinding.ActivityMemberManagementSimplifiedBinding
import com.duta.lubanagym.databinding.DialogMemberDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.text.SimpleDateFormat
import java.util.*

class MemberManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMemberManagementSimplifiedBinding
    private val viewModel: MemberManagementViewModel by viewModels()
    private lateinit var memberAdapter: MemberAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMemberManagementSimplifiedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
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

    private fun observeViewModel() {
        viewModel.memberList.observe(this) { result ->
            result.onSuccess { members ->
                memberAdapter.submitList(members)
                binding.progressBar.visibility = View.GONE

                if (members.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "👥 Belum ada member\n\nMember akan otomatis muncul ketika user role diubah ke 'member' di User Management"
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

    // NEW: Show member detail dialog
    private fun showMemberDetail(member: Member) {
        val dialogBinding = DialogMemberDetailBinding.inflate(layoutInflater)

        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        dialogBinding.apply {
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