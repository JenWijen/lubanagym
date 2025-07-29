package com.duta.lubanagym.ui.admin

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.R
import com.duta.lubanagym.databinding.ActivityStaffManagementBinding
import com.duta.lubanagym.data.model.Staff
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import android.widget.Spinner
import android.widget.Switch
import android.widget.LinearLayout
import java.text.SimpleDateFormat
import java.util.*

class StaffManagementActivity : AppCompatActivity() {

    private lateinit var binding: ActivityStaffManagementBinding
    private val viewModel: StaffManagementViewModel by viewModels()
    private lateinit var staffAdapter: StaffAdapter
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityStaffManagementBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupToolbar()
        setupRecyclerView()
        observeViewModel()
        loadStaff()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            try {
                setSupportActionBar(toolbar)
                supportActionBar?.setDisplayHomeAsUpEnabled(true)
                supportActionBar?.title = "Manajemen Staff"
            } catch (e: Exception) {
                toolbar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material)
                toolbar.setNavigationOnClickListener {
                    finish()
                }
                toolbar.title = "Manajemen Staff"
            }
        }
    }

    private fun setupRecyclerView() {
        staffAdapter = StaffAdapter(
            onStaffUpdate = { staff: Staff, field: String, value: Any ->
                val updates = mapOf(field to value)
                viewModel.updateStaff(staff.id, updates)
            },
            onDeleteStaff = { staff: Staff ->
                showDeleteStaffConfirmation(staff)
            },
            onEditStaff = { staff: Staff -> // ADD: Handle edit staff
                showEditStaffDialog(staff)
            }
        )

        binding.rvStaff.apply {
            adapter = staffAdapter
            layoutManager = LinearLayoutManager(this@StaffManagementActivity)
        }
    }

    // ADD: Show edit staff dialog
    private fun showEditStaffDialog(staff: Staff) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_edit_staff, null)

        // Find views
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etStaffName)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.etStaffPhone)
        val etAddress = dialogView.findViewById<TextInputEditText>(R.id.etAddress)
        val etDateOfBirth = dialogView.findViewById<TextInputEditText>(R.id.etDateOfBirth)
        val etEmergencyContact = dialogView.findViewById<TextInputEditText>(R.id.etEmergencyContact)
        val etEmergencyPhone = dialogView.findViewById<TextInputEditText>(R.id.etEmergencyPhone)
        val spinnerPosition = dialogView.findViewById<Spinner>(R.id.spinnerPosition)
        val spinnerGender = dialogView.findViewById<Spinner>(R.id.spinnerGender)
        val switchActive = dialogView.findViewById<Switch>(R.id.switchStaffActive)

        // Fill current data
        etName.setText(staff.name)
        etPhone.setText(staff.phone)
        etAddress.setText(staff.address)
        etDateOfBirth.setText(staff.dateOfBirth)
        etEmergencyContact.setText(staff.emergencyContact)
        etEmergencyPhone.setText(staff.emergencyPhone)
        switchActive.isChecked = staff.isActive

        // Setup position spinner
        val positions = resources.getStringArray(R.array.staff_positions)
        val positionAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, positions)
        positionAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerPosition.adapter = positionAdapter

        val positionIndex = positions.indexOf(staff.position)
        if (positionIndex >= 0) {
            spinnerPosition.setSelection(positionIndex)
        }

        // Setup gender spinner
        val genders = resources.getStringArray(R.array.gender_options)
        val genderAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, genders)
        genderAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerGender.adapter = genderAdapter

        when (staff.gender.lowercase()) {
            "male", "laki-laki" -> spinnerGender.setSelection(1)
            "female", "perempuan" -> spinnerGender.setSelection(2)
            else -> spinnerGender.setSelection(0)
        }

        // Setup date picker for date of birth
        etDateOfBirth.setOnClickListener {
            showDatePickerDialog(etDateOfBirth, staff.dateOfBirth)
        }

        val dialog = MaterialAlertDialogBuilder(this)
            .setTitle("✏️ Edit Staff")
            .setView(dialogView)
            .setPositiveButton("💾 Simpan") { _, _ ->
                saveStaffChanges(staff, dialogView)
            }
            .setNegativeButton("❌ Batal", null)
            .create()

        dialog.show()
    }

    private fun showDatePickerDialog(editText: TextInputEditText, currentDate: String) {
        val calendar = Calendar.getInstance()

        // Parse current date if exists
        if (currentDate.isNotEmpty()) {
            try {
                val date = dateFormat.parse(currentDate)
                date?.let { calendar.time = it }
            } catch (e: Exception) {
                // Use default date if parsing fails
                calendar.set(1990, 0, 1)
            }
        } else {
            calendar.set(1990, 0, 1)
        }

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                calendar.set(year, month, dayOfMonth)
                val formattedDate = dateFormat.format(calendar.time)
                editText.setText(formattedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set constraints
        datePickerDialog.datePicker.apply {
            maxDate = System.currentTimeMillis()
            val minCalendar = Calendar.getInstance()
            minCalendar.add(Calendar.YEAR, -100)
            minDate = minCalendar.timeInMillis
        }

        datePickerDialog.show()
    }

    private fun saveStaffChanges(staff: Staff, dialogView: View) {
        val etName = dialogView.findViewById<TextInputEditText>(R.id.etStaffName)
        val etPhone = dialogView.findViewById<TextInputEditText>(R.id.etStaffPhone)
        val etAddress = dialogView.findViewById<TextInputEditText>(R.id.etAddress)
        val etDateOfBirth = dialogView.findViewById<TextInputEditText>(R.id.etDateOfBirth)
        val etEmergencyContact = dialogView.findViewById<TextInputEditText>(R.id.etEmergencyContact)
        val etEmergencyPhone = dialogView.findViewById<TextInputEditText>(R.id.etEmergencyPhone)
        val spinnerPosition = dialogView.findViewById<Spinner>(R.id.spinnerPosition)
        val spinnerGender = dialogView.findViewById<Spinner>(R.id.spinnerGender)
        val switchActive = dialogView.findViewById<Switch>(R.id.switchStaffActive)

        val name = etName.text.toString().trim()
        val phone = etPhone.text.toString().trim()
        val position = spinnerPosition.selectedItem.toString()
        val address = etAddress.text.toString().trim()
        val dateOfBirth = etDateOfBirth.text.toString().trim()
        val emergencyContact = etEmergencyContact.text.toString().trim()
        val emergencyPhone = etEmergencyPhone.text.toString().trim()
        val isActive = switchActive.isChecked

        val gender = when (spinnerGender.selectedItemPosition) {
            1 -> "male"
            2 -> "female"
            else -> ""
        }

        // Validation
        if (name.isEmpty()) {
            Toast.makeText(this, "❌ Nama staff tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }
        if (phone.isEmpty()) {
            Toast.makeText(this, "❌ No. telepon tidak boleh kosong", Toast.LENGTH_SHORT).show()
            return
        }

        // Prepare updates
        val updates = mutableMapOf<String, Any>(
            "name" to name,
            "phone" to phone,
            "position" to position,
            "isActive" to isActive,
            "updatedAt" to System.currentTimeMillis()
        )

        // Add optional fields if not empty
        if (address.isNotEmpty()) updates["address"] = address
        if (dateOfBirth.isNotEmpty()) updates["dateOfBirth"] = dateOfBirth
        if (emergencyContact.isNotEmpty()) updates["emergencyContact"] = emergencyContact
        if (emergencyPhone.isNotEmpty()) updates["emergencyPhone"] = emergencyPhone
        if (gender.isNotEmpty()) updates["gender"] = gender

        viewModel.updateStaff(staff.id, updates)
    }

    private fun observeViewModel() {
        viewModel.staffList.observe(this) { result ->
            result.onSuccess { staffList ->
                staffAdapter.submitList(staffList)
                binding.progressBar.visibility = View.GONE

                if (staffList.isEmpty()) {
                    Toast.makeText(this, "👨‍💼 Belum ada data staff", Toast.LENGTH_SHORT).show()
                }
            }.onFailure { error ->
                binding.progressBar.visibility = View.GONE
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.updateResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Staff berhasil diupdate", Toast.LENGTH_SHORT).show()
                loadStaff()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        viewModel.deleteResult.observe(this) { result ->
            result.onSuccess {
                Toast.makeText(this, "✅ Staff berhasil dihapus", Toast.LENGTH_SHORT).show()
                loadStaff()
            }.onFailure { error ->
                Toast.makeText(this, "❌ Error menghapus: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showDeleteStaffConfirmation(staff: Staff) {
        MaterialAlertDialogBuilder(this)
            .setTitle("🗑️ Konfirmasi Hapus Staff")
            .setMessage("""
                Apakah Anda yakin ingin menghapus staff ini?
                
                👨‍💼 Nama: ${staff.name}
                📱 Telepon: ${staff.phone}
                🏷️ Posisi: ${staff.position}
                
                ⚠️ Tindakan ini tidak dapat dibatalkan!
            """.trimIndent())
            .setPositiveButton("🗑️ Ya, Hapus") { _, _ ->
                viewModel.deleteStaff(staff.id)
            }
            .setNegativeButton("❌ Batal", null)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .show()
    }

    private fun loadStaff() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.loadStaff()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}