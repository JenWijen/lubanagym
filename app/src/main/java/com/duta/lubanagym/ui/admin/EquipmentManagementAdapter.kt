package com.duta.lubanagym.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CompoundButton
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.Equipment
import com.duta.lubanagym.databinding.ItemEquipmentManagementBinding

class EquipmentManagementAdapter(
    private val onEdit: (Equipment, String, Any) -> Unit,
    private val onDelete: (Equipment) -> Unit,
    private val onUploadImage: (Equipment) -> Unit,
    private val onViewDetail: (Equipment) -> Unit,
    private val onEditEquipment: (Equipment) -> Unit
) : ListAdapter<Equipment, EquipmentManagementAdapter.EquipmentViewHolder>(EquipmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentViewHolder {
        val binding = ItemEquipmentManagementBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EquipmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EquipmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EquipmentViewHolder(
        private val binding: ItemEquipmentManagementBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(equipment: Equipment) {
            binding.apply {
                tvEquipmentName.text = equipment.name
                tvEquipmentDescription.text = equipment.description
                tvEquipmentCategory.text = equipment.category
                tvInstructions.text = equipment.instructions

                // FIXED: Add debug log to check equipment ID
                android.util.Log.d("EquipmentAdapter", "Binding equipment with ID: ${equipment.id}")

                // Load equipment image
                if (equipment.imageUrl.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(equipment.imageUrl)
                        .placeholder(R.drawable.ic_equipment_placeholder)
                        .error(R.drawable.ic_equipment_placeholder)
                        .into(ivEquipment)
                } else {
                    ivEquipment.setImageResource(R.drawable.ic_equipment_placeholder)
                }

                // Setup availability switch
                // FIXED: Clear listener first to prevent unwanted triggers
                switchAvailable.setOnCheckedChangeListener(null)
                switchAvailable.isChecked = equipment.isAvailable
                switchAvailable.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                    if (isChecked != equipment.isAvailable) {
                        // FIXED: Validate equipment ID before calling onEdit
                        if (equipment.id.isNotEmpty()) {
                            android.util.Log.d("EquipmentAdapter", "Updating availability for equipment ID: ${equipment.id}")
                            onEdit(equipment, "isAvailable", isChecked)
                        } else {
                            android.util.Log.e("EquipmentAdapter", "Equipment ID is empty!")
                            // Reset switch to original state
                            switchAvailable.setOnCheckedChangeListener(null)
                            switchAvailable.isChecked = equipment.isAvailable
                            switchAvailable.setOnCheckedChangeListener { _, newChecked ->
                                if (newChecked != equipment.isAvailable && equipment.id.isNotEmpty()) {
                                    onEdit(equipment, "isAvailable", newChecked)
                                }
                            }
                        }
                    }
                }

                // Setup detail button (card click)
                root.setOnClickListener {
                    if (equipment.id.isNotEmpty()) {
                        onViewDetail(equipment)
                    } else {
                        android.util.Log.e("EquipmentAdapter", "Cannot view detail: Equipment ID is empty!")
                    }
                }

                // Setup edit button
                btnEditEquipment.setOnClickListener { _: View ->
                    if (equipment.id.isNotEmpty()) {
                        android.util.Log.d("EquipmentAdapter", "Edit button clicked for equipment ID: ${equipment.id}")
                        onEditEquipment(equipment)
                    } else {
                        android.util.Log.e("EquipmentAdapter", "Cannot edit: Equipment ID is empty!")
                    }
                }

                // Setup upload image button
                btnUploadImage.setOnClickListener { _: View ->
                    if (equipment.id.isNotEmpty()) {
                        onUploadImage(equipment)
                    } else {
                        android.util.Log.e("EquipmentAdapter", "Cannot upload image: Equipment ID is empty!")
                    }
                }

                // Setup delete button
                btnDelete.setOnClickListener { _: View ->
                    if (equipment.id.isNotEmpty()) {
                        onDelete(equipment)
                    } else {
                        android.util.Log.e("EquipmentAdapter", "Cannot delete: Equipment ID is empty!")
                    }
                }
            }
        }
    }

    private class EquipmentDiffCallback : DiffUtil.ItemCallback<Equipment>() {
        override fun areItemsTheSame(oldItem: Equipment, newItem: Equipment): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Equipment, newItem: Equipment): Boolean {
            return oldItem == newItem
        }
    }
}