package com.duta.lubanagym.ui.main

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.duta.lubanagym.data.model.User
import com.duta.lubanagym.databinding.FragmentProfileBinding
import com.duta.lubanagym.ui.admin.AdminActivity
import com.duta.lubanagym.ui.auth.LoginActivity
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper
import java.text.SimpleDateFormat
import java.util.*

class ProfileFragment : Fragment() {

    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val viewModel: ProfileViewModel by viewModels()
    private lateinit var preferenceHelper: PreferenceHelper
    private var currentUser: User? = null
    private var isEditMode = false

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
        setupClickListeners()
        observeViewModel()
    }

    override fun onResume() {
        super.onResume()
        checkLoginStatus()
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
        binding.layoutLoggedIn.visibility = View.VISIBLE
        binding.layoutEditProfile.visibility = View.GONE

        val userId = preferenceHelper.getString(Constants.PREF_USER_ID)
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)

        // Show admin panel button if user is admin/staff
        if (userRole == Constants.ROLE_ADMIN || userRole == Constants.ROLE_STAFF) {
            binding.btnAdminPanel.visibility = View.VISIBLE
        } else {
            binding.btnAdminPanel.visibility = View.GONE
        }

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

        // Admin panel button
        binding.btnAdminPanel.setOnClickListener {
            val intent = Intent(requireContext(), AdminActivity::class.java)
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
            toggleEditMode(false)
            currentUser?.let { user ->
                displayUserProfile(user)
            }
        }

        // Logout button
        binding.btnLogout.setOnClickListener {
            logout()
        }

        // NEW: Date picker for date of birth
        binding.etDateOfBirth.setOnClickListener {
            showDatePickerDialog()
        }

        // Make the EditText non-focusable to prevent keyboard
        binding.etDateOfBirth.isFocusable = false
        binding.etDateOfBirth.isClickable = true
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

                // Show date picker hint
                Toast.makeText(requireContext(), "💡 Tap pada tanggal lahir untuk membuka date picker", Toast.LENGTH_LONG).show()
            } else {
                layoutLoggedIn.visibility = View.VISIBLE
                layoutEditProfile.visibility = View.GONE
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

        val profileData = mapOf(
            "fullName" to fullName,
            "phone" to phone,
            "dateOfBirth" to dateOfBirth,
            "gender" to gender,
            "address" to address,
            "emergencyContact" to emergencyContact,
            "emergencyPhone" to emergencyPhone,
            "bloodType" to bloodType,
            "allergies" to allergies
        )

        val userId = preferenceHelper.getString(Constants.PREF_USER_ID)
        viewModel.updateProfile(userId, profileData)
    }

    private fun logout() {
        preferenceHelper.clear()
        checkLoginStatus()
        Toast.makeText(context, "Logout berhasil", Toast.LENGTH_SHORT).show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}