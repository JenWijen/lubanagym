package com.duta.lubanagym.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duta.lubanagym.data.model.Staff
import com.duta.lubanagym.databinding.ItemStaffBinding
import java.text.SimpleDateFormat
import java.util.*

class StaffAdapter(
    private val onStaffUpdate: (Staff, String, Any) -> Unit,
    private val onDeleteStaff: (Staff) -> Unit
) : ListAdapter<Staff, StaffAdapter.StaffViewHolder>(StaffDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): StaffViewHolder {
        val binding = ItemStaffBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return StaffViewHolder(binding)
    }

    override fun onBindViewHolder(holder: StaffViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class StaffViewHolder(
        private val binding: ItemStaffBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(staff: Staff) {
            binding.apply {
                tvStaffName.text = staff.name
                tvStaffPhone.text = staff.phone
                tvStaffPosition.text = staff.position

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                tvJoinDate.text = "Bergabung: ${dateFormat.format(Date(staff.joinDate))}"

                // Setup status switch
                switchStatus.setOnCheckedChangeListener(null) // Clear previous listener
                switchStatus.isChecked = staff.isActive
                switchStatus.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != staff.isActive) {
                        onStaffUpdate(staff, "isActive", isChecked)
                    }
                }

                // Setup delete button
                btnDeleteStaff.setOnClickListener {
                    onDeleteStaff(staff)
                }
            }
        }
    }

    private class StaffDiffCallback : DiffUtil.ItemCallback<Staff>() {
        override fun areItemsTheSame(oldItem: Staff, newItem: Staff): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Staff, newItem: Staff): Boolean {
            return oldItem == newItem
        }
    }
}