package com.duta.lubanagym.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.Trainer
import com.duta.lubanagym.databinding.ItemTrainerBinding

class TrainerAdapter(
    private val onEdit: (Trainer, String, Any) -> Unit,
    private val onDelete: (Trainer) -> Unit
) : ListAdapter<Trainer, TrainerAdapter.TrainerViewHolder>(TrainerDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TrainerViewHolder {
        val binding = ItemTrainerBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TrainerViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TrainerViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TrainerViewHolder(
        private val binding: ItemTrainerBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(trainer: Trainer) {
            binding.apply {
                tvTrainerName.text = trainer.name
                tvSpecialization.text = trainer.specialization
                tvExperience.text = "Pengalaman: ${trainer.experience}"
                tvPhone.text = trainer.phone
                tvBio.text = trainer.bio

                // Load profile image
                if (trainer.profileImageUrl.isNotEmpty()) {
                    Glide.with(binding.root.context)
                        .load(trainer.profileImageUrl)
                        .placeholder(R.drawable.ic_profile_placeholder)
                        .into(ivTrainerPhoto)
                } else {
                    ivTrainerPhoto.setImageResource(R.drawable.ic_profile_placeholder)
                }

                // Setup status switch
                switchStatus.isChecked = trainer.isActive
                switchStatus.setOnCheckedChangeListener { _, isChecked ->
                    if (isChecked != trainer.isActive) {
                        onEdit(trainer, "isActive", isChecked)
                    }
                }

                // Setup delete button
                btnDelete.setOnClickListener {
                    onDelete(trainer)
                }
            }
        }
    }

    private class TrainerDiffCallback : DiffUtil.ItemCallback<Trainer>() {
        override fun areItemsTheSame(oldItem: Trainer, newItem: Trainer): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Trainer, newItem: Trainer): Boolean {
            return oldItem == newItem
        }
    }
}