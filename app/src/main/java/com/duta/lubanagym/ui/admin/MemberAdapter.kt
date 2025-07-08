package com.duta.lubanagym.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duta.lubanagym.data.model.Member
import com.duta.lubanagym.databinding.ItemMemberSimplifiedBinding
import com.duta.lubanagym.utils.Constants
import java.text.SimpleDateFormat
import java.util.*

class MemberAdapter(
    private val onMembershipChange: (Member, String) -> Unit,
    private val onStatusChange: (Member, Boolean) -> Unit,
    private val onViewDetail: (Member) -> Unit
) : ListAdapter<Member, MemberAdapter.MemberViewHolder>(MemberDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MemberViewHolder {
        val binding = ItemMemberSimplifiedBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return MemberViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MemberViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class MemberViewHolder(
        private val binding: ItemMemberSimplifiedBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(member: Member) {
            binding.apply {
                tvMemberName.text = member.name.ifEmpty { "Nama belum diatur" }
                tvMemberPhone.text = member.phone.ifEmpty { "No. telepon belum diatur" }
                tvMemberId.text = "ID: ${member.id.take(8)}"

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvJoinDate.text = "Bergabung: ${dateFormat.format(Date(member.joinDate))}"

                // Calculate days until expiry
                val daysUntilExpiry = ((member.expiryDate - System.currentTimeMillis()) / (1000 * 60 * 60 * 24)).toInt()
                when {
                    daysUntilExpiry < 0 -> {
                        tvExpiryInfo.text = "⚠️ Expired ${Math.abs(daysUntilExpiry)} hari lalu"
                        tvExpiryInfo.setTextColor(binding.root.context.getColor(android.R.color.holo_red_dark))
                    }
                    daysUntilExpiry < 30 -> {
                        tvExpiryInfo.text = "⚠️ Expired dalam $daysUntilExpiry hari"
                        tvExpiryInfo.setTextColor(binding.root.context.getColor(android.R.color.holo_orange_dark))
                    }
                    else -> {
                        tvExpiryInfo.text = "✅ Expired dalam $daysUntilExpiry hari"
                        tvExpiryInfo.setTextColor(binding.root.context.getColor(android.R.color.holo_green_dark))
                    }
                }

                // Setup membership type spinner
                val membershipTypes = arrayOf(
                    Constants.MEMBERSHIP_BASIC.uppercase(),
                    Constants.MEMBERSHIP_PREMIUM.uppercase(),
                    Constants.MEMBERSHIP_VIP.uppercase()
                )
                val adapter = ArrayAdapter(
                    binding.root.context,
                    android.R.layout.simple_spinner_item,
                    membershipTypes
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                spinnerMembershipType.adapter = adapter
                val currentPosition = when (member.membershipType.lowercase()) {
                    Constants.MEMBERSHIP_PREMIUM -> 1
                    Constants.MEMBERSHIP_VIP -> 2
                    else -> 0
                }
                spinnerMembershipType.setSelection(currentPosition)

                spinnerMembershipType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                        val newType = when (position) {
                            1 -> Constants.MEMBERSHIP_PREMIUM
                            2 -> Constants.MEMBERSHIP_VIP
                            else -> Constants.MEMBERSHIP_BASIC
                        }
                        if (newType != member.membershipType) {
                            onMembershipChange(member, newType)
                        }
                    }
                    override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                })

                // Setup status switch
                switchStatus.isChecked = member.isActive
                switchStatus.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != member.isActive) {
                        onStatusChange(member, isChecked)
                    }
                }

                // Detail button
                btnViewDetail.setOnClickListener {
                    onViewDetail(member)
                }

                // Membership badge color
                val badgeColor = when (member.membershipType.lowercase()) {
                    Constants.MEMBERSHIP_VIP -> android.R.color.holo_orange_dark
                    Constants.MEMBERSHIP_PREMIUM -> android.R.color.holo_blue_dark
                    else -> android.R.color.holo_green_dark
                }
                tvMembershipBadge.text = member.membershipType.uppercase()
                tvMembershipBadge.setBackgroundColor(binding.root.context.getColor(badgeColor))
            }
        }
    }

    private class MemberDiffCallback : DiffUtil.ItemCallback<Member>() {
        override fun areItemsTheSame(oldItem: Member, newItem: Member): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Member, newItem: Member): Boolean {
            return oldItem == newItem
        }
    }
}