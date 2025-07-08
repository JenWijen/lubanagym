package com.duta.lubanagym.utils

import android.app.DatePickerDialog
import android.content.Context
import android.widget.EditText
import android.widget.Toast
import java.text.SimpleDateFormat
import java.util.*

class DatePickerHelper(private val context: Context) {

    private val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
    private var selectedCalendar = Calendar.getInstance()

    /**
     * Show date picker dialog for birth date
     * @param editText EditText to update with selected date
     * @param currentDate Current date string (optional)
     * @param onDateSelected Callback when date is selected
     */
    fun showBirthDatePicker(
        editText: EditText,
        currentDate: String? = null,
        onDateSelected: ((String) -> Unit)? = null
    ) {
        // Parse current date if exists
        if (!currentDate.isNullOrEmpty() && isValidDate(currentDate)) {
            try {
                val parsedDate = dateFormat.parse(currentDate)
                parsedDate?.let {
                    selectedCalendar.time = it
                }
            } catch (e: Exception) {
                setDefaultDate()
            }
        } else {
            setDefaultDate()
        }

        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                // Update calendar with selected date
                selectedCalendar.set(year, month, dayOfMonth)

                // Format and set the date
                val formattedDate = dateFormat.format(selectedCalendar.time)
                editText.setText(formattedDate)

                // Show confirmation
                Toast.makeText(context, "📅 Tanggal lahir: $formattedDate", Toast.LENGTH_SHORT).show()

                // Callback
                onDateSelected?.invoke(formattedDate)
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )

        setupDatePickerConstraints(datePickerDialog)
        customizeDialog(datePickerDialog)
        datePickerDialog.show()
    }

    /**
     * Show date picker for general use (with custom constraints)
     */
    fun showDatePicker(
        editText: EditText,
        title: String = "📅 Pilih Tanggal",
        minYearsAgo: Int = 100,
        maxYearsFromNow: Int = 0,
        onDateSelected: ((String) -> Unit)? = null
    ) {
        val datePickerDialog = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                selectedCalendar.set(year, month, dayOfMonth)
                val formattedDate = dateFormat.format(selectedCalendar.time)
                editText.setText(formattedDate)
                Toast.makeText(context, "$title: $formattedDate", Toast.LENGTH_SHORT).show()
                onDateSelected?.invoke(formattedDate)
            },
            selectedCalendar.get(Calendar.YEAR),
            selectedCalendar.get(Calendar.MONTH),
            selectedCalendar.get(Calendar.DAY_OF_MONTH)
        )

        // Set custom constraints
        datePickerDialog.datePicker.apply {
            // Set minimum date
            val minCalendar = Calendar.getInstance()
            minCalendar.add(Calendar.YEAR, -minYearsAgo)
            minDate = minCalendar.timeInMillis

            // Set maximum date
            val maxCalendar = Calendar.getInstance()
            maxCalendar.add(Calendar.YEAR, maxYearsFromNow)
            maxDate = maxCalendar.timeInMillis
        }

        datePickerDialog.setTitle(title)
        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "✅ Pilih", datePickerDialog)
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "❌ Batal", datePickerDialog)
        datePickerDialog.show()
    }

    private fun setDefaultDate() {
        selectedCalendar = Calendar.getInstance()
        selectedCalendar.set(1990, 0, 1) // Default to 1990-01-01
    }

    private fun setupDatePickerConstraints(datePickerDialog: DatePickerDialog) {
        datePickerDialog.datePicker.apply {
            // Maximum date is today (can't be born in the future)
            maxDate = System.currentTimeMillis()

            // Minimum date is 100 years ago
            val minCalendar = Calendar.getInstance()
            minCalendar.add(Calendar.YEAR, -100)
            minDate = minCalendar.timeInMillis
        }
    }

    private fun customizeDialog(datePickerDialog: DatePickerDialog) {
        datePickerDialog.setTitle("📅 Pilih Tanggal Lahir")
        datePickerDialog.setButton(DatePickerDialog.BUTTON_POSITIVE, "✅ Pilih", datePickerDialog)
        datePickerDialog.setButton(DatePickerDialog.BUTTON_NEGATIVE, "❌ Batal", datePickerDialog)
    }

    /**
     * Validate date string format
     */
    fun isValidDate(dateString: String): Boolean {
        return try {
            dateFormat.parse(dateString)
            true
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Get formatted date string from Calendar
     */
    fun getFormattedDate(calendar: Calendar): String {
        return dateFormat.format(calendar.time)
    }

    /**
     * Parse date string to Calendar
     */
    fun parseDate(dateString: String): Calendar? {
        return try {
            val date = dateFormat.parse(dateString)
            val calendar = Calendar.getInstance()
            date?.let {
                calendar.time = it
                calendar
            }
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Calculate age from birth date
     */
    fun calculateAge(birthDateString: String): Int? {
        return try {
            val birthDate = dateFormat.parse(birthDateString)
            val birth = Calendar.getInstance()
            val today = Calendar.getInstance()

            birthDate?.let {
                birth.time = it
                var age = today.get(Calendar.YEAR) - birth.get(Calendar.YEAR)

                if (today.get(Calendar.DAY_OF_YEAR) < birth.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }

                age
            }
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        const val DATE_FORMAT = "dd/MM/yyyy"

        /**
         * Quick setup for EditText to use date picker
         */
        fun setupDatePickerEditText(editText: EditText, context: Context) {
            editText.apply {
                isFocusable = false
                isClickable = true
                isCursorVisible = false
                inputType = android.text.InputType.TYPE_NULL

                setOnClickListener {
                    val helper = DatePickerHelper(context)
                    helper.showBirthDatePicker(this, text.toString())
                }
            }
        }
    }
}