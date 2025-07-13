package com.duta.lubanagym.ui.main

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.duta.lubanagym.R
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.databinding.FragmentProfileBinding
import com.duta.lubanagym.ui.admin.AdminActivity
import com.duta.lubanagym.ui.auth.LoginActivity
import com.duta.lubanagym.utils.CloudinaryService
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var cloudinaryService: CloudinaryService
    private var currentUser: User? = null
    private var isEditMode = false

    // Image picker variables
    private var selectedImageUri: Uri? = null
    private lateinit var imagePickerLauncher: ActivityResultLauncher<Intent>
    private lateinit var permissionLauncher: ActivityResultLauncher<Array<String>>

    // Date picker variables
    private var selectedDateCalendar = Calendar.getInstance()
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        preferenceHelper = PreferenceHelper(requireContext())
        cloudinaryService = CloudinaryService()

        setupImagePicker()
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
    }

    private fun setupImagePicker() {
        // FIXED: Better image picker setup
        imagePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == android.app.Activity.RESULT_OK) {
                val imageUri = result.data?.data
                if (imageUri != null) {
                    selectedImageUri = imageUri
                    updateImagePreview(imageUri)
                    Toast.makeText(requireContext(), "✅ Foto berhasil dipilih", Toast.LENGTH_SHORT).show()

                    // FIXED: Jangan kembali ke display mode setelah pilih foto
                    // Biarkan user tetap di edit mode untuk save changes
                } else {
                    Toast.makeText(requireContext(), "❌ Gagal memilih foto", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(requireContext(), "❌ Pemilihan foto dibatalkan", Toast.LENGTH_SHORT).show()
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

    private fun checkLoginStatus() {
        val isLoggedIn = preferenceHelper.getBoolean(Constants.PREF_IS_LOGGED_IN)

        if (!isLoggedIn) {
            showLoginPrompt()
        } else {
            loadUserProfile()
        }
    }

    private fun showLoginPrompt() {
        binding.layoutNotLoggedIn.visibility = View.VISIBLE
        binding.layoutLoggedIn.visibility = View.GONE
        binding.layoutEditProfile.visibility = View.GONE
    }

    private fun loadUserProfile() {
        binding.layoutNotLoggedIn.visibility = View.GONE

        // FIXED: Pastikan tetap di edit mode jika sedang edit
        if (isEditMode) {
            binding.layoutLoggedIn.visibility = View.GONE
            binding.layoutEditProfile.visibility = View.VISIBLE
        } else {
            binding.layoutLoggedIn.visibility = View.VISIBLE
            binding.layoutEditProfile.visibility = View.GONE
        }

        val userId = preferenceHelper.getString(Constants.PREF_USER_ID)
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)

        // Show admin panel button if user is admin/staff
        binding.btnAdminPanel.visibility = View.GONE

        // Load user profile
        viewModel.loadUserProfile(userId)

        // Load member data if user is member
        if (userRole == Constants.ROLE_MEMBER) {
            viewModel.loadMemberProfile(userId)
        }
    }

    private fun setupClickListeners() {
        // Login button
        binding.btnLogin.setOnClickListener {
            val intent = Intent(requireContext(), LoginActivity::class.java)
            startActivity(intent)
        }

        // Edit profile button
        binding.btnEditProfile.setOnClickListener {
            toggleEditMode(true)
        }

        // Save profile button
        binding.btnSaveProfile.setOnClickListener {
            saveProfile()
        }

        // Cancel edit button
        binding.btnCancelEdit.setOnClickListener {
            // FIXED: Reset selected image when cancel
            selectedImageUri = null
            toggleEditMode(false)
            currentUser?.let { user ->
                displayUserProfile(user)
            }
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            logout()
        }

        // Date picker for date of birth
        binding.etDateOfBirth.setOnClickListener {
            showDatePickerDialog()
        }

        // Make the EditText non-focusable to prevent keyboard
        binding.etDateOfBirth.isFocusable = false
        binding.etDateOfBirth.isClickable = true

        // Profile photo edit click listeners
        binding.layoutEditPhoto.setOnClickListener {
            if (isEditMode) {
                pickImage()
            }
        }

        binding.btnChangeProfilePhoto.setOnClickListener {
            pickImage()
        }
        // NEW: Button daftar member untuk guest
        binding.btnBecomeMember?.setOnClickListener {
            val intent = Intent(requireContext(), com.duta.lubanagym.ui.member.RegisterMemberActivity::class.java)
            startActivity(intent)
        }
    }

    private fun pickImage() {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            Manifest.permission.READ_EXTERNAL_STORAGE
        }

        when {
            ContextCompat.checkSelfPermission(requireContext(), permission) == PackageManager.PERMISSION_GRANTED -> {
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
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("🔐 Permission Diperlukan")
            .setMessage("Aplikasi memerlukan akses ke galeri untuk memilih foto profil.")
            .setPositiveButton("✅ Berikan Permission") { _, _ ->
                requestPermission()
            }
            .setNegativeButton("❌ Batal") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showPermissionDeniedDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("⚠️ Permission Ditolak")
            .setMessage("Permission galeri ditolak. Anda masih dapat mengedit profil tanpa foto.")
            .setPositiveButton("⚙️ Buka Settings") { _, _ ->
                openAppSettings()
            }
            .setNegativeButton("📝 Lanjut", null)
            .show()
    }

    private fun openAppSettings() {
        try {
            val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
            intent.data = Uri.parse("package:${requireContext().packageName}")
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Tidak dapat membuka settings", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openImagePicker() {
        try {
            // FIXED: Improved image picker intent
            val intent = Intent().apply {
                action = Intent.ACTION_GET_CONTENT
                type = "image/*"
                addCategory(Intent.CATEGORY_OPENABLE)
                // Add extra options for better compatibility
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, false)
                putExtra(Intent.EXTRA_LOCAL_ONLY, true)
            }

            // Create chooser for better compatibility
            val chooser = Intent.createChooser(intent, "Pilih Foto Profil")

            // Check if gallery app is available
            if (chooser.resolveActivity(requireContext().packageManager) != null) {
                imagePickerLauncher.launch(chooser)
            } else {
                // Fallback to simple intent
                imagePickerLauncher.launch(intent)
            }
        } catch (e: Exception) {
            Toast.makeText(requireContext(), "Error membuka galeri: ${e.message}", Toast.LENGTH_SHORT).show()
            android.util.Log.e("ProfileFragment", "Error opening image picker", e)
        }
    }

    private fun updateImagePreview(uri: Uri) {
        try {
            if (isEditMode) {
                // Update edit profile image
                Glide.with(this)
                    .load(uri)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.ivEditProfilePicture)
            }

            // Also update main profile image
            Glide.with(this)
                .load(uri)
                .placeholder(R.drawable.ic_profile_placeholder)
                .error(R.drawable.ic_profile_placeholder)
                .into(binding.ivProfilePicture)

            android.util.Log.d("ProfileFragment", "Image preview updated successfully")
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error updating image preview", e)
            Toast.makeText(requireContext(), "Error memuat preview foto", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showDatePickerDialog() {
        // Parse current date if exists
        val currentDateText = binding.etDateOfBirth.text.toString()
        if (currentDateText.isNotEmpty() && isValidDate(currentDateText)) {
            try {
                val currentDate = dateFormat.parse(currentDateText)
                currentDate?.let {
                    selectedDateCalendar.time = it
                }
            } catch (e: Exception) {
                // Use default date if parsing fails
                selectedDateCalendar = Calendar.getInstance()
                selectedDateCalendar.set(1990, 0, 1) // Default to 1990-01-01
            }
        } else {
            // Set default date to 1990-01-01
            selectedDateCalendar = Calendar.getInstance()
            selectedDateCalendar.set(1990, 0, 1)
        }

        val datePickerDialog = DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                // Update calendar with selected date
                selectedDateCalendar.set(year, month, dayOfMonth)

                // Format and set the date
                val formattedDate = dateFormat.format(selectedDateCalendar.time)
                binding.etDateOfBirth.setText(formattedDate)

                // Show confirmation toast
                Toast.makeText(requireContext(), "📅 Tanggal lahir: $formattedDate", Toast.LENGTH_SHORT).show()
            },
            selectedDateCalendar.get(Calendar.YEAR),
            selectedDateCalendar.get(Calendar.MONTH),
            selectedDateCalendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set date picker constraints
        datePickerDialog.datePicker.apply {
            // Set maximum date to today (can't be born in the future)
            maxDate = System.currentTimeMillis()

            // Set minimum date to 100 years ago
            val minCalendar = Calendar.getInstance()
            minCalendar.add(Calendar.YEAR, -100)
            minDate = minCalendar.timeInMillis
        }

        // Customize dialog
        datePickerDialog.setTitle("📅 Pilih Tanggal Lahir")
        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "✅ Pilih", datePickerDialog)
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "❌ Batal", datePickerDialog)

        datePickerDialog.show()
    }

    private fun isValidDate(dateString: String): Boolean {
        return try {
            dateFormat.parse(dateString)
            true
        } catch (e: Exception) {
            false
        }
    }

    private fun observeViewModel() {
        viewModel.userProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { user ->
                if (user != null) {
                    currentUser = user
                    displayUserProfile(user)
                } else {
                    showProfileNotFound()
                }
            }.onFailure { error ->
                Toast.makeText(context, "Error loading profile: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.memberProfile.observe(viewLifecycleOwner) { result ->
            result.onSuccess { member ->
                member?.let {
                    binding.tvMembershipType.text = it.membershipType.uppercase()
                    binding.tvMemberId.text = "ID: ${it.id.take(8)}"
                } ?: run {
                    binding.tvMembershipType.text = "BELUM DIATUR"
                    binding.tvMemberId.text = "Hubungi admin"
                }
            }
        }

        viewModel.updateResult.observe(viewLifecycleOwner) { result ->
            result.onSuccess { message ->
                Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
                // FIXED: Reset selected image after successful save
                selectedImageUri = null
                toggleEditMode(false)
            }.onFailure { error ->
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnSaveProfile.isEnabled = !isLoading
            binding.btnSaveProfile.text = if (isLoading) "Menyimpan..." else "💾 Simpan"
        }
    }

    private fun displayUserProfile(user: User) {
        binding.apply {
            // Display mode
            tvUserName.text = if (user.fullName.isNotEmpty()) user.fullName else user.username
            tvEmail.text = user.email
            tvPhone.text = if (user.phone.isNotEmpty()) user.phone else "Belum diisi"
            tvDateOfBirth.text = if (user.dateOfBirth.isNotEmpty()) user.dateOfBirth else "Belum diisi"
            tvGender.text = if (user.gender.isNotEmpty()) user.gender.uppercase() else "Belum diisi"
            tvAddress.text = if (user.address.isNotEmpty()) user.address else "Belum diisi"
            tvEmergencyContact.text = if (user.emergencyContact.isNotEmpty()) user.emergencyContact else "Belum diisi"
            tvEmergencyPhone.text = if (user.emergencyPhone.isNotEmpty()) user.emergencyPhone else "Belum diisi"
            tvBloodType.text = if (user.bloodType.isNotEmpty()) user.bloodType else "Belum diisi"
            tvAllergies.text = if (user.allergies.isNotEmpty()) user.allergies else "Tidak ada"

            // Edit mode
            etFullName.setText(user.fullName)
            etPhone.setText(user.phone)
            etDateOfBirth.setText(user.dateOfBirth)
            etAddress.setText(user.address)
            etEmergencyContact.setText(user.emergencyContact)
            etEmergencyPhone.setText(user.emergencyPhone)
            etBloodType.setText(user.bloodType)
            etAllergies.setText(user.allergies)

            // Set gender spinner
            when (user.gender.lowercase()) {
                "male" -> spinnerGender.setSelection(1)
                "female" -> spinnerGender.setSelection(2)
                else -> spinnerGender.setSelection(0)
            }

            // Load profile image - FIXED: Better image loading
            loadProfileImage(user.profileImageUrl)

            // Show profile completion status
            if (user.isProfileComplete) {
                tvProfileStatus.text = "✅ Profil Lengkap"
                tvProfileStatus.setTextColor(resources.getColor(android.R.color.holo_green_dark, null))
            } else {
                tvProfileStatus.text = "⚠️ Profil Belum Lengkap"
                tvProfileStatus.setTextColor(resources.getColor(android.R.color.holo_orange_dark, null))
            }
        }
    }

    // FIXED: Separate method for loading profile image
    private fun loadProfileImage(imageUrl: String) {
        try {
            if (imageUrl.isNotEmpty()) {
                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.ivProfilePicture)

                Glide.with(this)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_profile_placeholder)
                    .error(R.drawable.ic_profile_placeholder)
                    .into(binding.ivEditProfilePicture)
            } else {
                binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
                binding.ivEditProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
            }
        } catch (e: Exception) {
            android.util.Log.e("ProfileFragment", "Error loading profile image", e)
            binding.ivProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
            binding.ivEditProfilePicture.setImageResource(R.drawable.ic_profile_placeholder)
        }
    }

    private fun showProfileNotFound() {
        binding.apply {
            tvUserName.text = "Profil Tidak Ditemukan"
            tvEmail.text = "Error loading user data"
            tvProfileStatus.text = "❌ Error"
            tvProfileStatus.setTextColor(resources.getColor(android.R.color.holo_red_dark, null))
        }
        Toast.makeText(context, "Profil tidak ditemukan. Silakan login ulang.", Toast.LENGTH_LONG).show()
    }

    private fun toggleEditMode(editMode: Boolean) {
        isEditMode = editMode

        binding.apply {
            if (editMode) {
                layoutLoggedIn.visibility = View.GONE
                layoutEditProfile.visibility = View.VISIBLE
                layoutEditPhoto.visibility = View.VISIBLE

                // Show tips
                Toast.makeText(requireContext(), "💡 Tap pada foto profil untuk mengganti foto", Toast.LENGTH_LONG).show()
            } else {
                layoutLoggedIn.visibility = View.VISIBLE
                layoutEditProfile.visibility = View.GONE
                layoutEditPhoto.visibility = View.GONE
                // FIXED: Reset selected image when exit edit mode
                selectedImageUri = null
            }
        }
    }

    private fun saveProfile() {
        val fullName = binding.etFullName.text.toString().trim()
        val phone = binding.etPhone.text.toString().trim()
        val dateOfBirth = binding.etDateOfBirth.text.toString().trim()
        val address = binding.etAddress.text.toString().trim()
        val emergencyContact = binding.etEmergencyContact.text.toString().trim()
        val emergencyPhone = binding.etEmergencyPhone.text.toString().trim()
        val bloodType = binding.etBloodType.text.toString().trim()
        val allergies = binding.etAllergies.text.toString().trim()

        val gender = when (binding.spinnerGender.selectedItemPosition) {
            1 -> "male"
            2 -> "female"
            else -> ""
        }

        // Validation
        if (fullName.isEmpty()) {
            binding.etFullName.error = "Nama lengkap wajib diisi"
            return
        }
        if (phone.isEmpty()) {
            binding.etPhone.error = "No. telepon wajib diisi"
            return
        }
        if (phone.length < 10) {
            binding.etPhone.error = "No. telepon minimal 10 digit"
            return
        }

        // Validate date format if filled
        if (dateOfBirth.isNotEmpty() && !isValidDate(dateOfBirth)) {
            binding.etDateOfBirth.error = "Format tanggal tidak valid. Gunakan date picker."
            Toast.makeText(requireContext(), "❌ Gunakan date picker untuk memilih tanggal lahir", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch {
            try {
                var profileImageUrl = currentUser?.profileImageUrl ?: ""

                // Upload new profile image if selected
                selectedImageUri?.let { uri ->
                    binding.progressBar.visibility = View.VISIBLE
                    Toast.makeText(requireContext(), "📤 Mengupload foto profil...", Toast.LENGTH_SHORT).show()

                    val uploadResult = cloudinaryService.uploadImage(uri, "profiles")
                    uploadResult.onSuccess { url ->
                        profileImageUrl = url
                        Toast.makeText(requireContext(), "✅ Foto berhasil diupload", Toast.LENGTH_SHORT).show()
                    }.onFailure { error ->
                        Toast.makeText(requireContext(), "⚠️ Upload foto gagal: ${error.message}", Toast.LENGTH_LONG).show()
                        // Continue with profile update even if image upload fails
                    }
                }

                val profileData = mapOf(
                    "fullName" to fullName,
                    "phone" to phone,
                    "dateOfBirth" to dateOfBirth,
                    "gender" to gender,
                    "address" to address,
                    "emergencyContact" to emergencyContact,
                    "emergencyPhone" to emergencyPhone,
                    "bloodType" to bloodType,
                    "allergies" to allergies,
                    "profileImageUrl" to profileImageUrl
                )

                val userId = preferenceHelper.getString(Constants.PREF_USER_ID)
                viewModel.updateProfile(userId, profileData)

            } catch (e: Exception) {
                binding.progressBar.visibility = View.GONE
                Toast.makeText(requireContext(), "❌ Error: ${e.message}", Toast.LENGTH_SHORT).show()
                android.util.Log.e("ProfileFragment", "Error saving profile", e)
            }
        }
    }

    private fun logout() {
        preferenceHelper.clear()
        selectedImageUri = null // Reset selected image
        isEditMode = false // Reset edit mode
        checkLoginStatus()
        Toast.makeText(context, "Logout berhasil", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}