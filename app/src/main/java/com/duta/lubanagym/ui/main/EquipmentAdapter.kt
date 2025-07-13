package com.duta.lubanagym.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.Equipment
import com.duta.lubanagym.databinding.ItemEquipmentBinding

class EquipmentAdapter(
    private val onItemClick: (Equipment) -> Unit
) : ListAdapter<Equipment, EquipmentAdapter.EquipmentViewHolder>(EquipmentDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EquipmentViewHolder {
        val binding = ItemEquipmentBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return EquipmentViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EquipmentViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class EquipmentViewHolder(
        private val binding: ItemEquipmentBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(equipment: Equipment) {
            binding.apply {
                // Equipment info
                tvEquipmentName.text = equipment.name
                tvEquipmentCategory.text = equipment.category
                tvEquipmentDescription.text = equipment.description

                // Status badge with emoji
                tvStatusBadge.text = if (equipment.isAvailable) "✅" else "⚠️"

                // Load image with smooth animation
                if (equipment.imageUrl.isNotEmpty()) {
                    Glide.with(root.context)
                        .load(equipment.imageUrl)
                        .placeholder(R.drawable.ic_equipment_placeholder)
                        .error(R.drawable.ic_equipment_placeholder)
                        .centerCrop()
                        .transform(RoundedCorners(12))
                        .into(ivEquipment)
                } else {
                    ivEquipment.setImageResource(R.drawable.ic_equipment_placeholder)
                }

                // Card click animation
                root.setOnClickListener {
                    // Simple scale animation
                    root.animate()
                        .scaleX(0.95f)
                        .scaleY(0.95f)
                        .setDuration(100)
                        .withEndAction {
                            root.animate()
                                .scaleX(1.0f)
                                .scaleY(1.0f)
                                .setDuration(100)
                                .start()
                            onItemClick(equipment)
                        }
                        .start()
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