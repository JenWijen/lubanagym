package com.duta.lubanagym.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.Equipment
import com.duta.lubanagym.databinding.FragmentEquipmentBinding
import com.duta.lubanagym.databinding.DialogEquipmentDetailBinding
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class EquipmentFragment : Fragment() {

    private var _binding: FragmentEquipmentBinding? = null
    private val binding get() = _binding!!
    private val viewModel: EquipmentViewModel by viewModels()
    private lateinit var equipmentAdapter: EquipmentAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEquipmentBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupRecyclerView()
        observeViewModel()
        loadEquipment()
    }

    private fun setupRecyclerView() {
        equipmentAdapter = EquipmentAdapter { equipment ->
            showEquipmentDetail(equipment)
        }

        binding.rvEquipment.apply {
            adapter = equipmentAdapter
            layoutManager = GridLayoutManager(context, getSpanCount())
            // Smooth scrolling
            setHasFixedSize(true)
            isNestedScrollingEnabled = false
        }
    }

    private fun getSpanCount(): Int {
        // Responsive grid based on screen width
        val displayMetrics = resources.displayMetrics
        val dpWidth = displayMetrics.widthPixels / displayMetrics.density
        return when {
            dpWidth >= 600 -> 3 // Tablet
            dpWidth >= 480 -> 2 // Large phone
            else -> 2 // Phone
        }
    }

    private fun showEquipmentDetail(equipment: Equipment) {
        val dialogBinding = DialogEquipmentDetailBinding.inflate(layoutInflater)

        dialogBinding.apply {
            // Equipment info
            tvEquipmentName.text = equipment.name
            tvEquipmentCategory.text = equipment.category
            tvEquipmentDescription.text = equipment.description
            tvEquipmentInstructions.text = equipment.instructions

            // Availability status with better styling
            if (equipment.isAvailable) {
                tvAvailabilityStatus.text = "✅ Tersedia"
                tvAvailabilityStatus.setTextColor(
                    requireContext().getColor(R.color.success_color)
                )
            } else {
                tvAvailabilityStatus.text = "⚠️ Maintenance"
                tvAvailabilityStatus.setTextColor(
                    requireContext().getColor(R.color.warning_color)
                )
            }

            // Load image with better error handling
            if (equipment.imageUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(equipment.imageUrl)
                    .placeholder(R.drawable.ic_equipment_placeholder)
                    .error(R.drawable.ic_equipment_placeholder)
                    .centerCrop()
                    .into(ivEquipmentImage)
            } else {
                ivEquipmentImage.setImageResource(R.drawable.ic_equipment_placeholder)
            }

            // Hide edit button for members
            btnEditEquipment.visibility = View.GONE
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewModel.equipmentList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { equipmentList ->
                // Filter available equipment for better user experience
                val availableEquipment = equipmentList.filter { it.isAvailable }

                equipmentAdapter.submitList(availableEquipment)
                binding.progressBar.visibility = View.GONE

                if (availableEquipment.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                showEmptyState()
                Toast.makeText(
                    context,
                    "Gagal memuat equipment: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    private fun showEmptyState() {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.rvEquipment.visibility = View.GONE
    }

    private fun hideEmptyState() {
        binding.layoutEmptyState.visibility = View.GONE
        binding.rvEquipment.visibility = View.VISIBLE
    }

    private fun loadEquipment() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadEquipment()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}