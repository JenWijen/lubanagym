package com.duta.lubanagym.ui.admin

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
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
import com.duta.lubanagym.databinding.DialogAddTrainerBinding
import com.duta.lubanagym.databinding.DialogEditTrainerBinding
import com.duta.lubanagym.utils.CloudinaryService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class TrainerManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityTrainerManagementBinding
    private val viewModel: TrainerManagementViewModel by viewModels()
    private lateinit var trainerAdapter: TrainerAdapter
    private lateinit var cloudinaryService: CloudinaryService

    // Image picker variables
    private var selectedImageUri: Uri? = null
    private var currentAddDialogBinding: DialogAddTrainerBinding? = null
    private var currentEditDialogBinding: DialogEditTrainerBinding? = null
    private var isEditMode = false

    // Filter state
    private var isFiltersVisible = false

    // Date picker variables
    private var selectedDateCalendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

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
        setupSearchAndFilter()
        setupClickListeners()
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

    private fun setupSearchAndFilter() {
        // Search functionality
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: Editable?) {
                val query = s.toString().trim()
                viewModel.searchTrainers(query)
            }
        })

        // Toggle filters visibility
        binding.btnToggleFilters.setOnClickListener {
            toggleFiltersVisibility()
        }

        // Specialization filter
        binding.spinnerSpecializationFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val specializations = resources.getStringArray(R.array.specialization_filter_options)
                val selectedSpecialization = if (position == 0) null else specializations[position]
                viewModel.filterBySpecialization(selectedSpecialization)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Status filter
        binding.spinnerStatusFilter.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val isActive = when (position) {
                    0 -> null // Semua Status
                    1 -> true // Aktif
                    2 -> false // Tidak Aktif
                    else -> null
                }
                viewModel.filterByStatus(isActive)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Sort options
        binding.spinnerSort.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                val sortType = when (position) {
                    0 -> TrainerManagementViewModel.SortType.NEWEST_FIRST
                    1 -> TrainerManagementViewModel.SortType.OLDEST_FIRST
                    2 -> TrainerManagementViewModel.SortType.NAME_A_Z
                    3 -> TrainerManagementViewModel.SortType.NAME_Z_A
                    4 -> TrainerManagementViewModel.SortType.SPECIALIZATION_A_Z
                    else -> TrainerManagementViewModel.SortType.NEWEST_FIRST
                }
                viewModel.sortTrainers(sortType)
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Clear search button
        binding.btnClearSearch.setOnClickListener {
            binding.etSearch.text?.clear()
            binding.spinnerSpecializationFilter.setSelection(0)
            binding.spinnerStatusFilter.setSelection(0)
            binding.spinnerSort.setSelection(0)
            viewModel.resetFilters()
        }
    }

    private fun toggleFiltersVisibility() {
        isFiltersVisible = !isFiltersVisible
        binding.layoutFilters.visibility = if (isFiltersVisible) View.VISIBLE else View.GONE
        binding.btnToggleFilters.text = if (isFiltersVisible) "🔼 Filter" else "🔽 Filter"
    }

    private fun setupClickListeners() {
        binding.fabAddTrainer?.setOnClickListener {
            showAddTrainerDialog()
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

    private fun showAddTrainerDialog() {
        val dialogBinding = DialogAddTrainerBinding.inflate(layoutInflater)
        currentAddDialogBinding = dialogBinding
        selectedImageUri = null
        isEditMode = false

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("➕ Tambah Trainer Baru")
            .setView(dialogBinding.root)
            .create()

        // Setup image picker
        dialogBinding.btnSelectProfileImage.setOnClickListener {
            pickImage()
        }

        // Setup date picker
        dialogBinding.etDateOfBirth.setOnClickListener {
            showDatePickerDialog()
        }

        // Setup save button
        dialogBinding.btnSaveTrainer.setOnClickListener {
            saveNewTrainer(dialogBinding, dialog)
        }

        // Setup cancel button
        dialogBinding.btnCancelTrainer.setOnClickListener {
            dialog.dismiss()
            currentAddDialogBinding = null
        }

        dialog.setOnDismissListener {
            currentAddDialogBinding = null
        }

        dialog.show()
    }

    // UPDATED: saveNewTrainer method dengan compression
    private fun saveNewTrainer(dialogBinding: DialogAddTrainerBinding, dialog: AlertDialog) {
        val name = dialogBinding.etTrainerName.text.toString().trim()
        val phone = dialogBinding.etTrainerPhone.text.toString().trim()
        val specialization = dialogBinding.etSpecialization.text.toString().trim()
        val experience = dialogBinding.etExperience.text.toString().trim()
        val certification = dialogBinding.etCertification.text.toString().trim()
        val hourlyRate = dialogBinding.etHourlyRate.text.toString().trim()
        val availability = dialogBinding.etAvailability.text.toString().trim()
        val bio = dialogBinding.etBio.text.toString().trim()
        val address = dialogBinding.etAddress.text.toString().trim()
        val dateOfBirth = dialogBinding.etDateOfBirth.text.toString().trim()
        val emergencyContact = dialogBinding.etEmergencyContact.text.toString().trim()
        val emergencyPhone = dialogBinding.etEmergencyPhone.text.toString().trim()
        val bloodType = dialogBinding.etBloodType.text.toString().trim()
        val allergies = dialogBinding.etAllergies.text.toString().trim()
        val languages = dialogBinding.etLanguages.text.toString().trim()
        val isActive = dialogBinding.switchTrainerActive.isChecked

        val gender = when (dialogBinding.spinnerGender.selectedItemPosition) {
            1 -> "male"
            2 -> "female"
            else -> ""
        }

        // Validation
        if (validateTrainerInput(name, phone, specialization, experience)) {
            lifecycleScope.launch {
                try {
                    binding.progressBar.visibility = View.VISIBLE

                    var imageUrl = ""
                    selectedImageUri?.let { uri ->
                        // NEW: Show compression info
                        val originalSizeKB = cloudinaryService.getImageSizeKB(this@TrainerManagementActivity, uri)
                        Toast.makeText(this@TrainerManagementActivity,
                            "📸 Foto trainer: ${originalSizeKB}KB - Mengkompress...",
                            Toast.LENGTH_SHORT).show()

                        // UPDATED: Pass context for compression
                        val uploadResult = cloudinaryService.uploadImage(uri, "trainers", this@TrainerManagementActivity)
                        uploadResult.onSuccess { url ->
                            imageUrl = url
                            Toast.makeText(this@TrainerManagementActivity,
                                "✅ Foto trainer berhasil diupload (compressed to ~1MB)",
                                Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(this@TrainerManagementActivity,
                                "⚠️ Upload foto gagal, trainer akan disimpan tanpa foto",
                                Toast.LENGTH_LONG).show()
                        }
                    }

                    val trainer = Trainer(
                        name = name,
                        phone = phone,
                        specialization = specialization,
                        experience = experience,
                        bio = bio,
                        profileImageUrl = imageUrl,
                        isActive = isActive,
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis(),

                        // Extended fields
                        address = address,
                        emergencyContact = emergencyContact,
                        emergencyPhone = emergencyPhone,
                        dateOfBirth = dateOfBirth,
                        gender = gender,
                        bloodType = bloodType,
                        allergies = allergies,

                        // Additional trainer fields
                        certification = certification,
                        hourlyRate = hourlyRate,
                        availability = availability,
                        languages = languages
                    )

                    viewModel.createTrainer(trainer)
                    dialog.dismiss()

                } catch (e: Exception) {
                    Toast.makeText(this@TrainerManagementActivity, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun showEditTrainerDialog(trainer: Trainer) {
        val dialogBinding = DialogEditTrainerBinding.inflate(layoutInflater)
        currentEditDialogBinding = dialogBinding
        selectedImageUri = null
        isEditMode = true

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

    // UPDATED: saveEditTrainer method dengan compression
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

                    // Upload new image if selected with compression
                    selectedImageUri?.let { uri ->
                        // NEW: Show compression info
                        val originalSizeKB = cloudinaryService.getImageSizeKB(this@TrainerManagementActivity, uri)
                        Toast.makeText(this@TrainerManagementActivity,
                            "📸 Foto trainer: ${originalSizeKB}KB - Mengkompress...",
                            Toast.LENGTH_SHORT).show()

                        // UPDATED: Pass context for compression
                        val uploadResult = cloudinaryService.uploadImage(uri, "trainers", this@TrainerManagementActivity)
                        uploadResult.onSuccess { url ->
                            imageUrl = url
                            Toast.makeText(this@TrainerManagementActivity,
                                "✅ Foto trainer berhasil diupload (compressed)",
                                Toast.LENGTH_SHORT).show()
                        }.onFailure {
                            Toast.makeText(this@TrainerManagementActivity,
                                "⚠️ Upload foto gagal, menggunakan foto lama",
                                Toast.LENGTH_LONG).show()
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
                        "isActive" to isActive,
                        "updatedAt" to System.currentTimeMillis()
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

    private fun showDatePickerDialog() {
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedDateCalendar.set(year, month, dayOfMonth)
                val formattedDate = dateFormat.format(selectedDateCalendar.time)

                if (isEditMode) {
                    // Currently we don't have date field in edit dialog, can be added if needed
                } else {
                    currentAddDialogBinding?.etDateOfBirth?.setText(formattedDate)
                }

                Toast.makeText(this, "📅 Tanggal lahir: $formattedDate", Toast.LENGTH_SHORT).show()
            },
            selectedDateCalendar.get(Calendar.YEAR),
            selectedDateCalendar.get(Calendar.MONTH),
            selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.apply {
            maxDate = System.currentTimeMillis()
            val minCalendar = Calendar.getInstance()
            minCalendar.add(Calendar.YEAR, -100)
            minDate = minCalendar.timeInMillis
        }

        datePickerDialog.setTitle("📅 Pilih Tanggal Lahir")
        datePickerDialog.show()
    }

    private fun validateTrainerInput(name: String, phone: String, specialization: String, experience: String): Boolean {
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "❌ Nama trainer tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            name.length < 3 -> {
                Toast.makeText(this, "❌ Nama trainer minimal 3 karakter", Toast.LENGTH_SHORT).show()
                return false
            }
            phone.isEmpty() -> {
                Toast.makeText(this, "❌ No. telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            phone.length < 10 -> {
                Toast.makeText(this, "❌ No. telepon minimal 10 digit", Toast.LENGTH_SHORT).show()
                return false
            }
            !isValidPhoneNumber(phone) -> {
                Toast.makeText(this, "❌ Format no. telepon tidak valid", Toast.LENGTH_SHORT).show()
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

    private fun isValidPhoneNumber(phone: String): Boolean {
        // Accept formats: 08xxxxxxxxx or +62xxxxxxxxx
        val cleanPhone = phone.replace("[^0-9+]".toRegex(), "")
        return when {
            cleanPhone.startsWith("08") && cleanPhone.length >= 11 -> true
            cleanPhone.startsWith("+628") && cleanPhone.length >= 13 -> true
            cleanPhone.startsWith("628") && cleanPhone.length >= 12 -> true
            else -> false
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
        if (isEditMode) {
            currentEditDialogBinding?.let { dialogBinding ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(dialogBinding.ivTrainerProfile)
            }
        } else {
            currentAddDialogBinding?.let { dialogBinding ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .into(dialogBinding.ivTrainerPreview)
            }
        }
    }

    private fun observeViewModel() {
        // Observe filtered trainer list instead of regular trainer list
        viewModel.filteredTrainerList.observe(this) { result ->
            result.onSuccess { trainers ->
                trainerAdapter.submitList(trainers)
                binding.progressBar.visibility = View.GONE

                // Update result count
                binding.tvResultCount.text = "Menampilkan ${trainers.size} trainer"

                if (trainers.isEmpty()) {
                    showEmptyState()
                } else {
                    hideEmptyState()
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.createResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Trainer berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                loadTrainers()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
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
                💡 Data trainer ini akan dihapus permanen dari sistem.
            """.trimIndent())
            .setPositiveButton("🗑️ Ya, Hapus") { _, _ ->
                viewModel.deleteTrainer(trainer.id)
            }
            .setNegativeButton("❌ Batal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun showEmptyState() {
        binding.tvEmptyState.visibility = View.VISIBLE
        val searchQuery = binding.etSearch.text.toString()
        binding.tvEmptyState.text = if (searchQuery.isNotEmpty()) {
            "🔍 Tidak ada trainer yang sesuai dengan pencarian '$searchQuery'"
        } else {
            "🏋️ Belum ada data trainer\n\nTambah trainer pertama dengan menekan tombol +"
        }
    }

    private fun hideEmptyState() {
        binding.tvEmptyState.visibility = View.GONE
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