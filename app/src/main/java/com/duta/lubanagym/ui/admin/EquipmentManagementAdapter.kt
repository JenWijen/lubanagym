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
    private val onViewDetail: (Equipment) -> Unit, // NEW: Detail callback
    private val onEditEquipment: (Equipment) -> Unit // NEW: Edit callback
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
                switchAvailable.isChecked = equipment.isAvailable
                switchAvailable.setOnCheckedChangeListener { _: CompoundButton, isChecked: Boolean ->
                    if (isChecked != equipment.isAvailable) {
                        onEdit(equipment, "isAvailable", isChecked)
                    }
                }

                // NEW: Setup detail button (card click)
                root.setOnClickListener {
                    onViewDetail(equipment)
                }

                // NEW: Setup edit button
                btnEditEquipment.setOnClickListener { _: View ->
                    onEditEquipment(equipment)
                }

                // Setup upload image button
                btnUploadImage.setOnClickListener { _: View ->
                    onUploadImage(equipment)
                }

                // Setup delete button
                btnDelete.setOnClickListener { _: View ->
                    onDelete(equipment)
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