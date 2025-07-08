package com.duta.lubanagym.ui.admin

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.Trainer
import com.duta.lubanagym.databinding.ActivityTrainerManagementBinding
import com.duta.lubanagym.databinding.DialogEditTrainerBinding
import com.duta.lubanagym.utils.CloudinaryService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class TrainerManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrainerManagementBinding
    private val viewModel: TrainerManagementViewModel by viewModels()
    private lateinit var trainerAdapter: TrainerAdapter
    private lateinit var cloudinaryService: CloudinaryService

    // Image picker variables
    private var selectedImageUri: Uri? = null
    private var currentEditDialogBinding: DialogEditTrainerBinding? = null

    // Activity result launchers
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTrainerManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cloudinaryService = CloudinaryService()
        setupImagePicker()
        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadTrainers()
    }

    private fun setupImagePicker() {
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    selectedImageUri = imageUri
                    updateImagePreview(imageUri)
                    Toast.makeText(this, "✅ Foto berhasil dipilih", Toast.LENGTH_SHORT).show()
                }
            }
        }

        permissionLauncher = registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                permissions[Manifest.permission.READ_MEDIA_IMAGES] == true
            } else {
                permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
            }

            if (hasPermission) {
                openImagePicker()
            } else {
                Toast.makeText(this, "Permission ditolak", Toast.LENGTH_SHORT).show()
            }
        }
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
                showDeleteTrainerConfirmation(trainer)
            },
            onEditTrainer = { trainer ->
                showEditTrainerDialog(trainer)
            }
        )

        binding.rvTrainers.apply {
            adapter = trainerAdapter
            layoutManager = LinearLayoutManager(this@TrainerManagementActivity)
        }
    }

    // Show edit trainer dialog
    private fun showEditTrainerDialog(trainer: Trainer) {
        val dialogBinding = DialogEditTrainerBinding.inflate(layoutInflater)
        currentEditDialogBinding = dialogBinding
        selectedImageUri = null

        dialogBinding.apply {
            // Fill current data
            etTrainerName.setText(trainer.name)
            etTrainerPhone.setText(trainer.phone)
            etSpecialization.setText(trainer.specialization)
            etExperience.setText(trainer.experience)
            etBio.setText(trainer.bio)
            switchTrainerActive.isChecked = trainer.isActive

            // Load current profile image
            if (trainer.profileImageUrl.isNotEmpty()) {
                Glide.with(this@TrainerManagementActivity)
                    .load(trainer.profileImageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(ivTrainerProfile)
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Edit Trainer")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnChangeProfileImage.setOnClickListener {
            pickImage()
        }

        dialogBinding.btnSaveTrainer.setOnClickListener {
            saveEditTrainer(trainer, dialogBinding, dialog)
        }

        dialogBinding.btnCancelTrainer.setOnClickListener {
            dialog.dismiss()
            currentEditDialogBinding = null
        }

        dialog.setOnDismissListener {
            currentEditDialogBinding = null
        }

        dialog.show()
    }

    private fun saveEditTrainer(trainer: Trainer, dialogBinding: DialogEditTrainerBinding, dialog: AlertDialog) {
        val name = dialogBinding.etTrainerName.text.toString().trim()
        val phone = dialogBinding.etTrainerPhone.text.toString().trim()
        val specialization = dialogBinding.etSpecialization.text.toString().trim()
        val experience = dialogBinding.etExperience.text.toString().trim()
        val bio = dialogBinding.etBio.text.toString().trim()
        val isActive = dialogBinding.switchTrainerActive.isChecked

        if (validateTrainerInput(name, phone, specialization, experience)) {
            lifecycleScope.launch {
                try {
                    binding.progressBar.visibility = View.VISIBLE

                    var imageUrl = trainer.profileImageUrl // Keep existing image by default

                    // Upload new image if selected
                    selectedImageUri?.let { uri ->
                        Toast.makeText(this@TrainerManagementActivity, "📤 Mengupload foto...", Toast.LENGTH_SHORT).show()
                        val uploadResult = cloudinaryService.uploadImage(uri, "trainers")
                        uploadResult.onSuccess { url ->
                            imageUrl = url
                            Toast.makeText(this@TrainerManagementActivity, "✅ Foto berhasil diupload", Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(this@TrainerManagementActivity, "⚠️ Upload foto gagal, menggunakan foto lama", Toast.LENGTH_LONG).show()
                        }
                    }

                    // Update trainer
                    val updates = mapOf(
                        "name" to name,
                        "phone" to phone,
                        "specialization" to specialization,
                        "experience" to experience,
                        "bio" to bio,
                        "profileImageUrl" to imageUrl,
                        "isActive" to isActive
                    )

                    viewModel.updateTrainer(trainer.id, updates)
                    dialog.dismiss()

                } catch (e: Exception) {
                    Toast.makeText(this@TrainerManagementActivity, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun validateTrainerInput(name: String, phone: String, specialization: String, experience: String): Boolean {
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "❌ Nama trainer tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            phone.isEmpty() -> {
                Toast.makeText(this, "❌ No. telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            specialization.isEmpty() -> {
                Toast.makeText(this, "❌ Spesialisasi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            experience.isEmpty() -> {
                Toast.makeText(this, "❌ Pengalaman tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private fun pickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED -> {
                openImagePicker()
            }
            else -> {
                requestPermission()
            }
        }
    }

    private fun requestPermission() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_IMAGES)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        permissionLauncher.launch(permissions)
    }

    private fun openImagePicker() {
        try {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                type = "image/*"
            }
            imagePickerLauncher.launch(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Error membuka galeri: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImagePreview(uri: Uri) {
        currentEditDialogBinding?.let { dialogBinding ->
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_profile_placeholder)
                .into(dialogBinding.ivTrainerProfile)
        }
    }

    private fun observeViewModel() {
        viewModel.trainerList.observe(this) { result ->
            result.onSuccess { trainers ->
                trainerAdapter.submitList(trainers)
                binding.progressBar.visibility = View.GONE

                if (trainers.isEmpty()) {
                    Toast.makeText(this, "🏋️ Belum ada data trainer", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Trainer berhasil diupdate", Toast.LENGTH_SHORT).show()
                loadTrainers()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Trainer berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadTrainers()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error menghapus: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteTrainerConfirmation(trainer: Trainer) {
        MaterialAlertDialogBuilder(this)
            .setTitle("🗑️ Konfirmasi Hapus Trainer")
            .setMessage("""
                Apakah Anda yakin ingin menghapus trainer ini?
                
                🏋️ Nama: ${trainer.name}
                📱 Telepon: ${trainer.phone}
                🎯 Spesialisasi: ${trainer.specialization}
                
                ⚠️ Tindakan ini tidak dapat dibatalkan!
            """.trimIndent())
            .setPositiveButton("🗑️ Ya, Hapus") { _, _ ->
                viewModel.deleteTrainer(trainer.id)
            }
            .setNegativeButton("❌ Batal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
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