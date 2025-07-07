package com.duta.lubanagym.ui.admin

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.databinding.ActivityTrainerManagementBinding

class TrainerManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrainerManagementBinding
    private val viewModel: TrainerManagementViewModel by viewModels()
    private lateinit var trainerAdapter: TrainerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainerManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadTrainers()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            try {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Manajemen Trainer"
            } catch (e: Exception) {
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                toolbar.setNavigationOnClickListener { onBackPressed() }
                toolbar.title = "Manajemen Trainer"
            }
        }
    }

    private fun setupRecyclerView() {
        trainerAdapter = TrainerAdapter(
            onEdit = { trainer, field, value ->
                val updates = mapOf(field to value)
                viewModel.updateTrainer(trainer.id, updates)
            },
            onDelete = { trainer ->
                viewModel.deleteTrainer(trainer.id)
            }
        )

        binding.rvTrainers.apply {
            adapter = trainerAdapter
            layoutManager = LinearLayoutManager(this@TrainerManagementActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.trainerList.observe(this) { result ->
            result.onSuccess { trainers ->
                trainerAdapter.submitList(trainers)
                binding.progressBar.visibility = View.GONE
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Trainer berhasil diupdate", Toast.LENGTH_SHORT).show()
                loadTrainers()
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "Trainer berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadTrainers()
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadTrainers() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadTrainers()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}