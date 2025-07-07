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
        // Setup image picker launcher dengan Intent manual
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

        // Setup permission launcher untuk multiple permissions
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

    private fun pickImage() {
        // Check permission berdasarkan Android version
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
            .setMessage("Aplikasi memerlukan akses ke galeri untuk memilih foto alat gym.\n\nTanpa permission ini, Anda tidak dapat menambahkan foto.")
            .setPositiveButton("✅ Berikan Permission") { _, _ ->
                requestPermission()
            }
            .setNegativeButton("❌ Batal") { dialog, _ ->
                dialog.dismiss()
                Toast.makeText(this, "Anda dapat menambah foto nanti melalui tombol 'Upload Foto'", Toast.LENGTH_LONG).show()
            }
            .setCancelable(false)
            .show()
    }

    private fun showPermissionDeniedDialog() {
        AlertDialog.Builder(this)
            .setTitle("⚠️ Permission Ditolak")
            .setMessage("Permission galeri ditolak. Anda masih dapat:\n\n• Menambah alat gym tanpa foto\n• Menambah foto nanti dari list equipment\n\nUntuk mengaktifkan permission, buka Settings > Apps > Lubana Gym > Permissions")
            .setPositiveButton("⚙️ Buka Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("📝 Lanjut Tanpa Foto") { dialog, _ ->
                dialog.dismiss()
            }
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
                putExtra(Intent.EXTRA_MIME_TYPES, arrayOf("image/jpeg", "image/png", "image/jpg"))
            }

            // Fallback jika ACTION_PICK tidak tersedia
            if (intent.resolveActivity(packageManager) != null) {
                imagePickerLauncher.launch(intent)
            } else {
                val fallbackIntent = Intent(Intent.ACTION_GET_CONTENT).apply {
                    type = "image/*"
                    addCategory(Intent.CATEGORY_OPENABLE)
                }
                imagePickerLauncher.launch(Intent.createChooser(fallbackIntent, "Pilih Foto"))
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error membuka galeri: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateImagePreview(uri: Uri) {
        currentDialogBinding?.let { dialogBinding ->
            try {
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_equipment_placeholder)
                    .error(R.drawable.ic_equipment_placeholder)
                    .centerCrop()
                    .into(dialogBinding.ivPreview)

                dialogBinding.ivPreview.visibility = View.VISIBLE
                dialogBinding.btnSelectImage.text = "🔄 Ganti Foto"

            } catch (e: Exception) {
                Toast.makeText(this, "Error loading preview: ${e.message}", Toast.LENGTH_SHORT).show()
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
                val updates = mapOf(field to value)
                viewModel.updateEquipment(equipment.id, updates)
            },
            onDelete = { equipment ->
                showDeleteConfirmation(equipment)
            },
            onUploadImage = { equipment ->
                currentEquipmentId = equipment.id
                pickImage()
            }
        )

        binding.rvEquipment.apply {
            adapter = equipmentAdapter
            layoutManager = LinearLayoutManager(this@EquipmentManagementActivity)
        }
    }

    private fun observeViewModel() {
        viewModel.equipmentList.observe(this) { result ->
            result.onSuccess { equipmentList ->
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
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.createResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Equipment berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                loadEquipment()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                if (currentEquipmentId != null) {
                    Toast.makeText(this, "✅ Foto berhasil diupdate", Toast.LENGTH_SHORT).show()
                    currentEquipmentId = null
                } else {
                    Toast.makeText(this, "✅ Equipment berhasil diupdate", Toast.LENGTH_SHORT).show()
                }
                loadEquipment()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Equipment berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadEquipment()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAddEquipmentDialog() {
        val dialogBinding = DialogAddEquipmentBinding.inflate(layoutInflater)
        currentDialogBinding = dialogBinding
        selectedImageUri = null

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("Tambah Alat Gym")
            .setView(dialogBinding.root)
            .setPositiveButton("💾 Simpan", null)
            .setNegativeButton("❌ Batal") { _, _ ->
                currentDialogBinding = null
            }
            .create()

        // Setup image picker button in dialog
        dialogBinding.btnSelectImage.setOnClickListener {
            pickImage()
        }

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                val name = dialogBinding.etName.text.toString().trim()
                val description = dialogBinding.etDescription.text.toString().trim()
                val category = dialogBinding.etCategory.text.toString().trim()
                val instructions = dialogBinding.etInstructions.text.toString().trim()

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
            description.isEmpty() -> {
                Toast.makeText(this, "❌ Deskripsi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            category.isEmpty() -> {
                Toast.makeText(this, "❌ Kategori tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            instructions.isEmpty() -> {
                Toast.makeText(this, "❌ Instruksi tidak boleh kosong", Toast.LENGTH_SHORT).show()
                return false
            }
            else -> return true
        }
    }

    private fun createEquipment(name: String, description: String, category: String, instructions: String, dialog: AlertDialog) {
        lifecycleScope.launch {
            try {
                binding.progressBar.visibility = View.VISIBLE

                // Upload image first if selected
                var imageUrl = ""
                selectedImageUri?.let { uri ->
                    Toast.makeText(this@EquipmentManagementActivity, "📤 Mengupload foto...", Toast.LENGTH_SHORT).show()
                    val uploadResult = cloudinaryService.uploadImage(uri, "equipment")
                    uploadResult.onSuccess { url ->
                        imageUrl = url
                        Toast.makeText(this@EquipmentManagementActivity, "✅ Foto berhasil diupload", Toast.LENGTH_SHORT).show()
                    }.onFailure { error ->
                        Toast.makeText(this@EquipmentManagementActivity, "⚠️ Upload foto gagal, equipment akan disimpan tanpa foto", Toast.LENGTH_LONG).show()
                        // Continue without image
                    }
                }

                // Create equipment object
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