package com.duta.lubanagym.ui.admin

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.Token
import com.duta.lubanagym.databinding.ItemTokenBinding
import java.text.SimpleDateFormat
import java.util.*

class TokenAdapter(
    private val onTokenClick: (Token) -> Unit
) : ListAdapter<Token, TokenAdapter.TokenViewHolder>(TokenDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TokenViewHolder {
        val binding = ItemTokenBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        )
        return TokenViewHolder(binding)
    }

    override fun onBindViewHolder(holder: TokenViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    inner class TokenViewHolder(
        private val binding: ItemTokenBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(token: Token) {
            binding.apply {
                tvToken.text = token.token

                val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                tvCreatedAt.text = "Dibuat: ${dateFormat.format(Date(token.createdAt))}"

                if (token.isUsed) {
                    tvStatus.text = "SUDAH DIGUNAKAN"
                    tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.red))
                    if (token.usedAt > 0) {
                        tvUsedAt.text = "Digunakan: ${dateFormat.format(Date(token.usedAt))}"
                        tvUsedAt.visibility = android.view.View.VISIBLE
                    }
                } else {
                    tvStatus.text = "TERSEDIA"
                    tvStatus.setTextColor(ContextCompat.getColor(binding.root.context, R.color.green))
                    tvUsedAt.visibility = android.view.View.GONE
                }

                root.setOnClickListener {
                    onTokenClick(token)
                }
            }
        }
    }

    private class TokenDiffCallback : DiffUtil.ItemCallback<Token>() {
        override fun areItemsTheSame(oldItem: Token, newItem: Token): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Token, newItem: Token): Boolean {
            return oldItem == newItem
        }
    }
}