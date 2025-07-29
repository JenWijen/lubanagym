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
import com.duta.lubanagym.data.model.Equipment
import com.duta.lubanagym.databinding.ActivityEquipmentManagementBinding
import com.duta.lubanagym.databinding.DialogAddEquipmentBinding
import com.duta.lubanagym.databinding.DialogEquipmentDetailBinding
import com.duta.lubanagym.databinding.DialogEditEquipmentBinding
import com.duta.lubanagym.utils.CloudinaryService
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import kotlinx.coroutines.launch

class EquipmentManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEquipmentManagementBinding
    private val viewModel: EquipmentManagementViewModel by viewModels()
    private lateinit var equipmentAdapter: EquipmentManagementAdapter
    private lateinit var cloudinaryService: CloudinaryService

    // Image picker variables
    private var selectedImageUri: Uri? = null
    private var currentEquipmentId: String? = null
    private var currentDialogBinding: DialogAddEquipmentBinding? = null
    private var currentEditDialogBinding: DialogEditEquipmentBinding? = null
    private var isEditMode = false

    // Activity result launchers
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityEquipmentManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cloudinaryService = CloudinaryService()
        setupImagePicker()
        setupToolbar()
        setupRecyclerView()
        setupClickListeners()
        observeViewModel()
        loadEquipment()
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
                } else {
                    Toast.makeText(this, "❌ Gagal memilih foto", Toast.LENGTH_SHORT).show()
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
                showPermissionDeniedDialog()
            }
        }
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            try {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Manajemen Alat Gym"
            } catch (e: Exception) {
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                toolbar.setNavigationOnClickListener { onBackPressed() }
                toolbar.title = "Manajemen Alat Gym"
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabAddEquipment.setOnClickListener {
            showAddEquipmentDialog()
        }
    }

    private fun setupRecyclerView() {
        equipmentAdapter = EquipmentManagementAdapter(
            onEdit = { equipment, field, value ->
                // FIXED: Validate equipment ID before update
                if (equipment.id.isEmpty()) {
                    Toast.makeText(this, "❌ Error: Equipment ID tidak valid", Toast.LENGTH_SHORT).show()
                    android.util.Log.e("EquipmentUpdate", "Cannot update: Equipment ID is empty")
                    // Reload data to refresh adapter
                    loadEquipment()
                    return@EquipmentManagementAdapter
                }

                val updates = mapOf(field to value, "updatedAt" to System.currentTimeMillis())
                android.util.Log.d("EquipmentUpdate", "Updating equipment ${equipment.id}: $field = $value")
                viewModel.updateEquipment(equipment.id, updates)
            },
            onDelete = { equipment ->
                if (equipment.id.isEmpty()) {
                    Toast.makeText(this, "❌ Error: Equipment ID tidak valid", Toast.LENGTH_SHORT).show()
                    return@EquipmentManagementAdapter
                }
                showDeleteConfirmation(equipment)
            },
            onUploadImage = { equipment ->
                if (equipment.id.isEmpty()) {
                    Toast.makeText(this, "❌ Error: Equipment ID tidak valid", Toast.LENGTH_SHORT).show()
                    return@EquipmentManagementAdapter
                }
                currentEquipmentId = equipment.id
                isEditMode = false
                pickImage()
            },
            onViewDetail = { equipment ->
                if (equipment.id.isEmpty()) {
                    Toast.makeText(this, "❌ Error: Equipment ID tidak valid", Toast.LENGTH_SHORT).show()
                    return@EquipmentManagementAdapter
                }
                showEquipmentDetail(equipment)
            },
            onEditEquipment = { equipment ->
                if (equipment.id.isEmpty()) {
                    Toast.makeText(this, "❌ Error: Equipment ID tidak valid", Toast.LENGTH_SHORT).show()
                    return@EquipmentManagementAdapter
                }
                showEditEquipmentDialog(equipment)
            }
        )

        binding.rvEquipment.apply {
            adapter = equipmentAdapter
            layoutManager = LinearLayoutManager(this@EquipmentManagementActivity)
        }
    }

    // NEW: Show equipment detail dialog
    private fun showEquipmentDetail(equipment: Equipment) {
        val dialogBinding = DialogEquipmentDetailBinding.inflate(layoutInflater)

        dialogBinding.apply {
            tvEquipmentName.text = equipment.name
            tvEquipmentCategory.text = equipment.category
            tvEquipmentDescription.text = equipment.description
            tvEquipmentInstructions.text = equipment.instructions

            // Set availability status
            tvAvailabilityStatus.text = if (equipment.isAvailable) "✅ Tersedia" else "❌ Tidak Tersedia"
            tvAvailabilityStatus.setTextColor(
                getColor(if (equipment.isAvailable) android.R.color.holo_green_dark else android.R.color.holo_red_dark)
            )

            // Load image
            if (equipment.imageUrl.isNotEmpty()) {
                Glide.with(this@EquipmentManagementActivity)
                    .load(equipment.imageUrl)
                    .placeholder(R.drawable.ic_equipment_placeholder)
                    .error(R.drawable.ic_equipment_placeholder)
                    .into(ivEquipmentImage)
            } else {
                ivEquipmentImage.setImageResource(R.drawable.ic_equipment_placeholder)
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnEditEquipment.setOnClickListener {
            dialog.dismiss()
            showEditEquipmentDialog(equipment)
        }

        dialogBinding.btnCloseDetail.setOnClickListener {
            dialog.dismiss()
        }

        dialog.show()
    }

    // NEW: Show edit equipment dialog
    // FIXED: Improved showEditEquipmentDialog with validation
    private fun showEditEquipmentDialog(equipment: Equipment) {
        // FIXED: Validate equipment ID first
        if (equipment.id.isEmpty()) {
            Toast.makeText(this, "❌ Error: Equipment ID tidak valid", Toast.LENGTH_SHORT).show()
            android.util.Log.e("EquipmentEdit", "Equipment ID is empty: $equipment")
            return
        }

        android.util.Log.d("EquipmentEdit", "Editing equipment with ID: ${equipment.id}")

        val dialogBinding = DialogEditEquipmentBinding.inflate(layoutInflater)
        currentEditDialogBinding = dialogBinding
        selectedImageUri = null
        isEditMode = true

        dialogBinding.apply {
            // Fill current data
            etEditName.setText(equipment.name)
            etEditDescription.setText(equipment.description)
            etEditInstructions.setText(equipment.instructions)
            switchEditAvailable.isChecked = equipment.isAvailable

            // Set category spinner selection
            val categories = resources.getStringArray(R.array.equipment_categories)
            val categoryIndex = categories.indexOf(equipment.category)
            if (categoryIndex >= 0) {
                spinnerEditCategory.setSelection(categoryIndex)
            } else {
                // If category not found, select first item (usually a default option)
                spinnerEditCategory.setSelection(0)
            }

            // Load current image
            if (equipment.imageUrl.isNotEmpty()) {
                Glide.with(this@EquipmentManagementActivity)
                    .load(equipment.imageUrl)
                    .placeholder(R.drawable.ic_equipment_placeholder)
                    .error(R.drawable.ic_equipment_placeholder)
                    .into(ivEditPreview)
            }
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Edit Equipment")
            .setView(dialogBinding.root)
            .create()

        dialogBinding.btnChangeImage.setOnClickListener {
            currentEquipmentId = equipment.id
            pickImage()
        }

        dialogBinding.btnSaveEdit.setOnClickListener {
            saveEditEquipment(equipment, dialogBinding, dialog)
        }

        dialogBinding.btnCancelEdit.setOnClickListener {
            dialog.dismiss()
            currentEditDialogBinding = null
        }

        dialog.setOnDismissListener {
            currentEditDialogBinding = null
        }

        dialog.show()
    }

    // FIXED: Improved saveEditEquipment with better error handling
    // UPDATED: saveEditEquipment method dengan compression
    private fun saveEditEquipment(equipment: Equipment, dialogBinding: DialogEditEquipmentBinding, dialog: AlertDialog) {
        // Validation
        if (equipment.id.isEmpty()) {
            Toast.makeText(this, "❌ Error: Equipment ID tidak valid", Toast.LENGTH_SHORT).show()
            return
        }

        val name = dialogBinding.etEditName.text.toString().trim()
        val description = dialogBinding.etEditDescription.text.toString().trim()
        val instructions = dialogBinding.etEditInstructions.text.toString().trim()
        val isAvailable = dialogBinding.switchEditAvailable.isChecked

        // Get category from spinner
        val categoryPosition = dialogBinding.spinnerEditCategory.selectedItemPosition
        val category = if (categoryPosition >= 0) {
            val categories = resources.getStringArray(R.array.equipment_categories)
            if (categoryPosition < categories.size) {
                categories[categoryPosition]
            } else {
                equipment.category
            }
        } else {
            equipment.category
        }

        if (validateEquipmentInput(name, description, category, instructions)) {
            lifecycleScope.launch {
                try {
                    binding.progressBar.visibility = View.VISIBLE

                    var imageUrl = equipment.imageUrl // Keep existing image by default

                    // Upload new image if selected with compression
                    selectedImageUri?.let { uri ->
                        // NEW: Show compression info
                        val originalSizeKB = cloudinaryService.getImageSizeKB(this@EquipmentManagementActivity, uri)
                        Toast.makeText(this@EquipmentManagementActivity,
                            "📸 Foto asli: ${originalSizeKB}KB - Mengkompress...",
                            Toast.LENGTH_SHORT).show()

                        // UPDATED: Pass context for compression
                        val uploadResult = cloudinaryService.uploadImage(uri, "equipment", this@EquipmentManagementActivity)
                        uploadResult.onSuccess { url ->
                            imageUrl = url
                            Toast.makeText(this@EquipmentManagementActivity,
                                "✅ Foto berhasil diupload (compressed to ~1MB)",
                                Toast.LENGTH_SHORT).show()
                        }.onFailure { error ->
                            Toast.makeText(this@EquipmentManagementActivity,
                                "⚠️ Upload foto gagal: ${error.message}",
                                Toast.LENGTH_LONG).show()
                        }
                    }

                    val updates = mapOf(
                        "name" to name,
                        "description" to description,
                        "category" to category,
                        "instructions" to instructions,
                        "imageUrl" to imageUrl,
                        "isAvailable" to isAvailable,
                        "updatedAt" to System.currentTimeMillis()
                    )

                    viewModel.updateEquipment(equipment.id, updates)
                    dialog.dismiss()

                } catch (e: Exception) {
                    Toast.makeText(this@EquipmentManagementActivity, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    binding.progressBar.visibility = View.GONE
                }
            }
        }
    }

    private fun getCategoryFromSpinner(spinner: android.widget.Spinner): String {
        val position = spinner.selectedItemPosition
        return if (position >= 0) {
            val categories = resources.getStringArray(R.array.equipment_categories)
            categories[position]
        } else {
            ""
        }
    }

    private fun setCategorySpinnerSelection(spinner: android.widget.Spinner, category: String) {
        val categories = resources.getStringArray(R.array.equipment_categories)
        val index = categories.indexOf(category)
        if (index >= 0) {
            spinner.setSelection(index)
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
            shouldShowRequestPermissionRationale(permission) -> {
                showPermissionRationaleDialog()
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

    private fun showPermissionRationaleDialog() {
        AlertDialog.Builder(this)
            .setTitle("🔐 Permission Diperlukan")
            .setMessage("Aplikasi memerlukan akses ke galeri untuk memilih foto alat gym.")
            .setPositiveButton("✅ Berikan Permission") { _, _ ->
                requestPermission()
            }
            .setNegativeButton("❌ Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Permission Ditolak")
            .setMessage("Permission galeri ditolak. Anda masih dapat menambah alat gym tanpa foto.")
            .setPositiveButton("⚙️ Buka Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("📝 Lanjut", null)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:$packageName")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Tidak dapat membuka settings", Toast.LENGTH_SHORT).show()
        }
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
                    .placeholder(R.drawable.ic_equipment_placeholder)
                    .into(dialogBinding.ivEditPreview)
            }
        } else {
            currentDialogBinding?.let { dialogBinding ->
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_equipment_placeholder)
                    .into(dialogBinding.ivPreview)
                dialogBinding.ivPreview.visibility = View.VISIBLE
                dialogBinding.btnSelectImage.text = "🔄 Ganti Foto"
            }
        }
    }

    private fun observeViewModel() {
        viewModel.equipmentList.observe(this) { result ->
            result.onSuccess { equipmentList ->
                // FIXED: Log equipment IDs for debugging
                equipmentList.forEach { equipment ->
                    android.util.Log.d("EquipmentList", "Equipment: ${equipment.name}, ID: ${equipment.id}")
                    if (equipment.id.isEmpty()) {
                        android.util.Log.e("EquipmentList", "Warning: Equipment '${equipment.name}' has empty ID!")
                    }
                }

                equipmentAdapter.submitList(equipmentList)
                binding.progressBar.visibility = View.GONE

                if (equipmentList.isEmpty()) {
                    binding.tvEmptyState.visibility = View.VISIBLE
                    binding.tvEmptyState.text = "🏋️ Belum ada alat gym\n\nTambah alat gym pertama dengan menekan tombol +"
                } else {
                    binding.tvEmptyState.visibility = View.GONE
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                android.util.Log.e("EquipmentList", "Error loading equipment: ${error.message}", error)
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.createResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Equipment berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                loadEquipment()
            }.onFailure { error ->
                android.util.Log.e("EquipmentCreate", "Error creating equipment: ${error.message}", error)
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                if (currentEquipmentId != null) {
                    Toast.makeText(this, "✅ Equipment berhasil diupdate", Toast.LENGTH_SHORT).show()
                    currentEquipmentId = null
                } else {
                    Toast.makeText(this, "✅ Equipment berhasil diupdate", Toast.LENGTH_SHORT).show()
                }
                loadEquipment()
            }.onFailure { error ->
                android.util.Log.e("EquipmentUpdate", "Error updating equipment: ${error.message}", error)
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Equipment berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadEquipment()
            }.onFailure { error ->
                android.util.Log.e("EquipmentDelete", "Error deleting equipment: ${error.message}", error)
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddEquipmentDialog() {
        val dialogBinding = DialogAddEquipmentBinding.inflate(layoutInflater)
        currentDialogBinding = dialogBinding
        selectedImageUri = null
        isEditMode = false

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Tambah Alat Gym")
            .setView(dialogBinding.root)
            .setPositiveButton("💾 Simpan", null)
            .setNegativeButton("❌ Batal") { _, _ ->
                currentDialogBinding = null
            }
            .create()

        dialogBinding.btnSelectImage.setOnClickListener {
            pickImage()
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etName.text.toString().trim()
                val description = dialogBinding.etDescription.text.toString().trim()
                val instructions = dialogBinding.etInstructions.text.toString().trim()

                // UPDATED: Get category from spinner
                val categoryPosition = dialogBinding.spinnerCategory.selectedItemPosition
                val category = if (categoryPosition > 0) {
                    // Get categories array and select the chosen one
                    val categories = resources.getStringArray(R.array.equipment_categories)
                    categories[categoryPosition]
                } else {
                    ""
                }

                if (validateEquipmentInput(name, description, category, instructions)) {
                    createEquipment(name, description, category, instructions, dialog)
                }
            }
        }

        dialog.setOnDismissListener {
            currentDialogBinding = null
        }

        dialog.show()
    }

    private fun validateEquipmentInput(name: String, description: String, category: String, instructions: String): Boolean {
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "❌ Nama alat tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            name.length < 3 -> {
                Toast.makeText(this, "❌ Nama alat minimal 3 karakter", Toast.LENGTH_SHORT).show()
                return false
            }
            category.isEmpty() -> {
                Toast.makeText(this, "❌ Pilih kategori alat gym", Toast.LENGTH_SHORT).show()
                return false
            }
            description.isEmpty() -> {
                Toast.makeText(this, "❌ Deskripsi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            description.length < 10 -> {
                Toast.makeText(this, "❌ Deskripsi minimal 10 karakter", Toast.LENGTH_SHORT).show()
                return false
            }
            instructions.isEmpty() -> {
                Toast.makeText(this, "❌ Instruksi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            instructions.length < 15 -> {
                Toast.makeText(this, "❌ Instruksi minimal 15 karakter untuk keamanan pengguna", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private fun createEquipment(name: String, description: String, category: String, instructions: String, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                var imageUrl = ""
                selectedImageUri?.let { uri ->
                    // NEW: Show image size before compression
                    val originalSizeKB = cloudinaryService.getImageSizeKB(this@EquipmentManagementActivity, uri)
                    Toast.makeText(this@EquipmentManagementActivity,
                        "📸 Foto asli: ${originalSizeKB}KB - Mengkompress...",
                        Toast.LENGTH_SHORT).show()

                    // UPDATED: Pass context for compression
                    val uploadResult = cloudinaryService.uploadImage(uri, "equipment", this@EquipmentManagementActivity)
                    uploadResult.onSuccess { url ->
                        imageUrl = url
                        Toast.makeText(this@EquipmentManagementActivity,
                            "✅ Foto berhasil diupload (compressed to ~1MB)",
                            Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(this@EquipmentManagementActivity,
                            "⚠️ Upload foto gagal, equipment akan disimpan tanpa foto",
                            Toast.LENGTH_LONG).show()
                    }
                }

                val equipment = Equipment(
                    name = name,
                    description = description,
                    category = category,
                    imageUrl = imageUrl,
                    instructions = instructions,
                    isAvailable = true,
                    createdAt = System.currentTimeMillis()
                )

                viewModel.createEquipment(equipment)
                dialog.dismiss()

            } catch (e: Exception) {
                Toast.makeText(this@EquipmentManagementActivity, "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
            } finally {
                binding.progressBar.visibility = View.GONE
            }
        }
    }


    private fun showDeleteConfirmation(equipment: Equipment) {
        MaterialAlertDialogBuilder(this)
            .setTitle("🗑️ Hapus Equipment")
            .setMessage("Apakah Anda yakin ingin menghapus:\n\n📋 ${equipment.name}\n🏷️ ${equipment.category}")
            .setPositiveButton("🗑️ Hapus") { _, _ ->
                viewModel.deleteEquipment(equipment.id)
            }
            .setNegativeButton("❌ Batal", null)
            .show()
    }

    private fun loadEquipment() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadEquipment()
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}