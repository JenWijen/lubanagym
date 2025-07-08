package com.duta.lubanagym.ui.admin

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.databinding.ItemUserBinding
import com.duta.lubanagym.utils.Constants

class UserAdapter(
    private val onRoleChange: (User, String) -> Unit,
    private val onDeleteUser: (User) -> Unit // NEW: Delete callback
) : ListAdapter<User, UserAdapter.UserViewHolder>(UserDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val binding = ItemUserBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return UserViewHolder(binding)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class UserViewHolder(
        private val binding: ItemUserBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(user: User) {
            binding.apply {
                tvUsername.text = user.username
                tvEmail.text = user.email

                // Setup role badge
                setupRoleBadge(user.role)

                // Setup role spinner - TAMBAH TRAINER
                val roles = arrayOf(
                    Constants.ROLE_MEMBER,
                    Constants.ROLE_STAFF,
                    Constants.ROLE_TRAINER,
                    Constants.ROLE_ADMIN
                )
                val roleDisplayNames = arrayOf(
                    "👥 Member",
                    "👨‍💼 Staff",
                    "🏋️ Trainer",
                    "👨‍💻 Admin"
                )

                val adapter = ArrayAdapter(
                    binding.root.context,
                    android.R.layout.simple_spinner_item,
                    roleDisplayNames
                )
                adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

                spinnerRole.adapter = adapter
                val currentPosition = roles.indexOf(user.role)
                spinnerRole.setSelection(if (currentPosition >= 0) currentPosition else 0)

                spinnerRole.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        val newRole = roles[position]
                        if (newRole != user.role) {
                            onRoleChange(user, newRole)
                        }
                    }
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }

                // NEW: Setup delete button
                btnDeleteUser.setOnClickListener {
                    onDeleteUser(user)
                }
            }
        }

        private fun setupRoleBadge(role: String) {
            binding.tvRoleBadge.text = role.uppercase()

            val (backgroundRes, textColorRes) = when (role) {
                Constants.ROLE_ADMIN -> Pair(R.drawable.role_badge_admin, R.color.white)
                Constants.ROLE_STAFF -> Pair(R.drawable.role_badge_staff, R.color.white)
                Constants.ROLE_TRAINER -> Pair(R.drawable.role_badge_trainer, R.color.white)
                Constants.ROLE_MEMBER -> Pair(R.drawable.role_badge_member, R.color.white)
                else -> Pair(R.drawable.role_badge_member, R.color.white)
            }

            binding.tvRoleBadge.setBackgroundResource(backgroundRes)
            binding.tvRoleBadge.setTextColor(
                ContextCompat.getColor(binding.root.context, textColorRes)
            )
        }
    }

    private class UserDiffCallback : DiffUtil.ItemCallback<User>() {
        override fun areItemsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: User, newItem: User): Boolean {
            return oldItem == newItem
        }
    }
}