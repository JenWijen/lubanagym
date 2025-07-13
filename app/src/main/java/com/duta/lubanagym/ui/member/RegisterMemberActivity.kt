// Updated RegisterMemberActivity.kt - Fixed status check for activated members
package com.duta.lubanagym.ui.member

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.duta.lubanagym.R
import com.duta.lubanagym.databinding.ActivityRegisterMemberBinding
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper
import com.google.zxing.BarcodeFormat
import com.google.zxing.WriterException
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class RegisterMemberActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterMemberBinding
    private val viewModel: RegisterMemberViewModel by viewModels()
    private lateinit var preferenceHelper: PreferenceHelper

    private val membershipPrices = mapOf(
        Constants.MEMBERSHIP_BASIC to 150000,
        Constants.MEMBERSHIP_PREMIUM to 250000,
        Constants.MEMBERSHIP_VIP to 400000
    )

    private val durations = arrayOf(1, 3, 6, 12)
    private val discounts = mapOf(
        1 to 0,
        3 to 5,   // 5% discount
        6 to 10,  // 10% discount
        12 to 15  // 15% discount
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegisterMemberBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper(this)

        setupToolbar()
        setupSpinners()
        setupClickListeners()
        observeViewModel()
        checkExistingRegistration()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Daftar Member"
        }
    }

    private fun setupSpinners() {
        // Membership Type Spinner
        val membershipTypes = arrayOf(
            "🥉 BASIC - Akses Gym Standar (Rp ${formatPrice(membershipPrices[Constants.MEMBERSHIP_BASIC]!!)})",
            "🥈 PREMIUM - Gym + Konsultasi Trainer (Rp ${formatPrice(membershipPrices[Constants.MEMBERSHIP_PREMIUM]!!)})",
            "🥇 VIP - All Access + Personal Trainer (Rp ${formatPrice(membershipPrices[Constants.MEMBERSHIP_VIP]!!)})"
        )
        val membershipAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, membershipTypes)
        membershipAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerMembershipType.adapter = membershipAdapter

        // Duration Spinner
        val durationOptions = durations.map { months ->
            "$months Bulan${if (discounts[months]!! > 0) " (Diskon ${discounts[months]}%)" else ""}"
        }
        val durationAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, durationOptions)
        durationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        binding.spinnerDuration.adapter = durationAdapter

        // Update calculation when selection changes
        binding.spinnerMembershipType.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updatePriceCalculation()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        binding.spinnerDuration.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updatePriceCalculation()
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })

        // Initial calculation
        updatePriceCalculation()
    }

    private fun updatePriceCalculation() {
        val membershipPosition = binding.spinnerMembershipType.selectedItemPosition
        val durationPosition = binding.spinnerDuration.selectedItemPosition

        val membershipType = when (membershipPosition) {
            0 -> Constants.MEMBERSHIP_BASIC
            1 -> Constants.MEMBERSHIP_PREMIUM
            2 -> Constants.MEMBERSHIP_VIP
            else -> Constants.MEMBERSHIP_BASIC
        }

        val duration = durations[durationPosition]
        val basePrice = membershipPrices[membershipType]!!
        val discount = discounts[duration]!!

        val totalBasePrice = basePrice * duration
        val discountAmount = (totalBasePrice * discount) / 100
        val finalPrice = totalBasePrice - discountAmount

        // Update UI
        binding.apply {
            tvSelectedMembership.text = membershipType.uppercase()
            tvSelectedDuration.text = "$duration Bulan"
            tvBasePrice.text = "Rp ${formatPrice(totalBasePrice)}"

            if (discount > 0) {
                tvDiscount.text = "- Rp ${formatPrice(discountAmount)} ($discount%)"
                tvDiscount.visibility = View.VISIBLE
                tvDiscountLabel.visibility = View.VISIBLE
            } else {
                tvDiscount.visibility = View.GONE
                tvDiscountLabel.visibility = View.GONE
            }

            tvFinalPrice.text = "Rp ${formatPrice(finalPrice)}"

            // Show end date
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MONTH, duration)
            val dateFormat = SimpleDateFormat("dd MMMM yyyy", Locale("id", "ID"))
            tvMembershipEndDate.text = "Berakhir: ${dateFormat.format(calendar.time)}"
        }
    }

    private fun formatPrice(price: Int): String {
        return NumberFormat.getNumberInstance(Locale("id", "ID")).format(price)
    }

    private fun setupClickListeners() {
        binding.btnRegisterMember.setOnClickListener {
            registerMember()
        }
    }

    private fun registerMember() {
        val userId = preferenceHelper.getString(Constants.PREF_USER_ID)
        if (userId.isEmpty()) {
            Toast.makeText(this, "Error: User tidak ditemukan", Toast.LENGTH_SHORT).show()
            return
        }

        val membershipPosition = binding.spinnerMembershipType.selectedItemPosition
        val durationPosition = binding.spinnerDuration.selectedItemPosition

        val membershipType = when (membershipPosition) {
            0 -> Constants.MEMBERSHIP_BASIC
            1 -> Constants.MEMBERSHIP_PREMIUM
            2 -> Constants.MEMBERSHIP_VIP
            else -> Constants.MEMBERSHIP_BASIC
        }

        val duration = durations[durationPosition]
        val basePrice = membershipPrices[membershipType]!!
        val discount = discounts[duration]!!
        val totalBasePrice = basePrice * duration
        val discountAmount = (totalBasePrice * discount) / 100
        val finalPrice = totalBasePrice - discountAmount

        lifecycleScope.launch {
            viewModel.registerMember(userId, membershipType, duration, finalPrice)
        }
    }

    private fun observeViewModel() {
        viewModel.registrationResult.observe(this) { result ->
            result.onSuccess { qrCode ->
                showSuccessDialog(qrCode)
            }.onFailure { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_LONG).show()
            }
        }

        viewModel.existingRegistration.observe(this) { result ->
            result.onSuccess { registration ->
                if (registration != null) {
                    showExistingRegistration(registration)
                } else {
                    showRegistrationForm()
                }
            }.onFailure {
                showRegistrationForm()
            }
        }

        viewModel.isLoading.observe(this) { isLoading ->
            binding.progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            binding.btnRegisterMember.isEnabled = !isLoading
            binding.btnRegisterMember.text = if (isLoading) "Memproses..." else "💳 Daftar Member"
        }
    }

    private fun checkExistingRegistration() {
        val userId = preferenceHelper.getString(Constants.PREF_USER_ID)
        if (userId.isNotEmpty()) {
            viewModel.checkExistingRegistration(userId)
        }
    }

    private fun showRegistrationForm() {
        binding.layoutRegistrationForm.visibility = View.VISIBLE
        binding.layoutExistingRegistration.visibility = View.GONE
    }

    // FIXED: Show existing registration with proper status handling
    private fun showExistingRegistration(registration: com.duta.lubanagym.data.model.MemberRegistration) {
        binding.layoutRegistrationForm.visibility = View.GONE
        binding.layoutExistingRegistration.visibility = View.VISIBLE

        val currentTime = System.currentTimeMillis()
        val isExpired = currentTime > registration.expiryDate

        binding.apply {
            tvExistingMembership.text = registration.membershipType.uppercase()
            tvExistingDuration.text = "${registration.duration} Bulan"
            tvExistingPrice.text = "Rp ${formatPrice(registration.price)}"

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))
            tvRegistrationDate.text = "Tanggal Daftar: ${dateFormat.format(Date(registration.registrationDate))}"
            tvExpiryDate.text = "QR Code Valid Until: ${dateFormat.format(Date(registration.expiryDate))}"

            when {
                registration.status == "activated" -> {
                    // FIXED: Enhanced UI for activated status
                    tvRegistrationStatus.text = "✅ Sudah Diaktivasi"
                    tvRegistrationStatus.setTextColor(getColor(android.R.color.holo_green_dark))

                    val activationDate = if (registration.activationDate > 0) {
                        "pada ${dateFormat.format(Date(registration.activationDate))}"
                    } else {
                        ""
                    }

                    tvStatusDescription.text = "🎉 Selamat! Anda sudah menjadi member aktif Lubana Gym $activationDate\n\n" +
                            "✅ Status: Member Aktif\n" +
                            "💳 Membership: ${registration.membershipType.uppercase()}\n" +
                            "🔄 Profil Anda telah diupdate ke status Member"

                    btnNewRegistration.visibility = View.GONE
                    layoutQrCode.visibility = View.GONE // Hide QR code as it's no longer needed
                }
                isExpired -> {
                    tvRegistrationStatus.text = "❌ QR Code Expired"
                    tvRegistrationStatus.setTextColor(getColor(android.R.color.holo_red_dark))
                    tvStatusDescription.text = "QR Code sudah expired. Silakan daftar ulang untuk mendapatkan QR Code baru"
                    btnNewRegistration.visibility = View.VISIBLE
                    btnNewRegistration.text = "📝 Daftar Ulang"
                    layoutQrCode.visibility = View.GONE
                }
                else -> {
                    tvRegistrationStatus.text = "⏳ Menunggu Aktivasi"
                    tvRegistrationStatus.setTextColor(getColor(android.R.color.holo_orange_dark))
                    val remainingDays = ((registration.expiryDate - currentTime) / (1000 * 60 * 60 * 24)).toInt()
                    tvStatusDescription.text = "QR Code masih valid $remainingDays hari lagi. Silakan datang ke gym untuk aktivasi"
                    btnNewRegistration.visibility = View.GONE

                    // Generate and show QR Code
                    generateQRCode(registration.qrCode)
                }
            }
        }

        binding.btnNewRegistration.setOnClickListener {
            showRegistrationForm()
        }
    }

    private fun showSuccessDialog(qrCode: String) {
        val dialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("🎉 Pendaftaran Berhasil!")
            .setMessage("""
                Selamat! Pendaftaran member Anda berhasil.
                
                📱 QR Code telah dibuat dan berlaku selama 5 hari
                🏃‍♂️ Datang ke gym dan tunjukkan QR Code ini ke admin/staff untuk aktivasi
                ⏰ Jangan lupa aktivasi sebelum QR Code expired!
                
                Terima kasih telah bergabung dengan Lubana Gym! 💪
            """.trimIndent())
            .setPositiveButton("✅ Lihat QR Code") { _, _ ->
                checkExistingRegistration() // Refresh to show QR code
            }
            .setCancelable(false)
            .create()

        dialog.show()
    }

    private fun generateQRCode(qrCodeText: String) {
        try {
            val writer = QRCodeWriter()
            val bitMatrix: BitMatrix = writer.encode(qrCodeText, BarcodeFormat.QR_CODE, 300, 300)
            val width = bitMatrix.width
            val height = bitMatrix.height
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)

            for (x in 0 until width) {
                for (y in 0 until height) {
                    bitmap.setPixel(x, y, if (bitMatrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
                }
            }

            binding.ivQrCode.setImageBitmap(bitmap)
            binding.tvQrCodeText.text = qrCodeText
            binding.layoutQrCode.visibility = View.VISIBLE

        } catch (e: WriterException) {
            Toast.makeText(this, "Error generating QR Code", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}