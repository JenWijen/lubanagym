package com.duta.lubanagym.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.GridLayoutManager
import com.duta.lubanagym.databinding.FragmentEquipmentBinding

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
            // Handle equipment item click
            Toast.makeText(context, "Equipment: ${equipment.name}", Toast.LENGTH_SHORT).show()
        }

        binding.rvEquipment.apply {
            adapter = equipmentAdapter
            layoutManager = GridLayoutManager(context, 2)
        }
    }

    private fun observeViewModel() {
        viewModel.equipmentList.observe(viewLifecycleOwner) { result ->
            result.onSuccess { equipmentList ->
                equipmentAdapter.submitList(equipmentList)
                binding.progressBar.visibility = View.GONE
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
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