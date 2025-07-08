package com.duta.lubanagym.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
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
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun showEquipmentDetail(equipment: Equipment) {
        val dialogBinding = DialogEquipmentDetailBinding.inflate(layoutInflater)

        dialogBinding.apply {
            // Set equipment data
            tvEquipmentName.text = equipment.name
            tvEquipmentCategory.text = equipment.category
            tvEquipmentDescription.text = equipment.description
            tvEquipmentInstructions.text = equipment.instructions

            // Set availability status
            if (equipment.isAvailable) {
                tvAvailabilityStatus.text = "✅ Tersedia"
                tvAvailabilityStatus.setTextColor(requireContext().getColor(android.R.color.holo_green_dark))
            } else {
                tvAvailabilityStatus.text = "❌ Tidak Tersedia"
                tvAvailabilityStatus.setTextColor(requireContext().getColor(android.R.color.holo_red_dark))
            }

            // Load equipment image
            if (equipment.imageUrl.isNotEmpty()) {
                Glide.with(requireContext())
                    .load(equipment.imageUrl)
                    .placeholder(R.drawable.ic_equipment_placeholder)
                    .error(R.drawable.ic_equipment_placeholder)
                    .into(ivEquipmentImage)
            } else {
                ivEquipmentImage.setImageResource(R.drawable.ic_equipment_placeholder)
            }
        }

        val dialog = MaterialAlertDialogBuilder(requireContext())
            .setView(dialogBinding.root)
            .create()

        // Setup buttons
        dialogBinding.btnEditEquipment.visibility = View.GONE // Hide edit for members

        dialogBinding.btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun observeViewModel() {
        viewModel.equipmentList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { equipmentList ->
                // Filter only available equipment for members
                val availableEquipment = equipmentList.filter { it.isAvailable }
                equipmentAdapter.submitList(availableEquipment)
                binding.progressBar.visibility = View.GONE

                if (availableEquipment.isEmpty()) {
                    showEmptyState("🏋️‍♂️ Equipment sedang dalam maintenance\n\nSilakan coba lagi nanti atau hubungi staff gym")
                } else {
                    hideEmptyState()
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                showEmptyState("❌ Gagal memuat data equipment\n\nPeriksa koneksi internet Anda")
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showEmptyState(message: String) {
        binding.layoutEmptyState.visibility = View.VISIBLE
        binding.tvEmptyState.text = message
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