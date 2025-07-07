package com.duta.lubanagym.ui.main

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
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
                tvEquipmentName.text = equipment.name
                tvEquipmentDescription.text = equipment.description
                tvEquipmentCategory.text = equipment.category

                // Load image with Glide
                if (equipment.imageUrl.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(equipment.imageUrl)
                        .placeholder(R.drawable.ic_equipment_placeholder)
                        .into(ivEquipment)
                } else {
                    ivEquipment.setImageResource(R.drawable.ic_equipment_placeholder)
                }

                root.setOnClickListener {
                    onItemClick(equipment)
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