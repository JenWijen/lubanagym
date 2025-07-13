package com.duta.lubanagym.ui.admin

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.duta.lubanagym.databinding.ActivityQrScannerBinding
import com.duta.lubanagym.utils.Constants
import com.duta.lubanagym.utils.PreferenceHelper
import com.google.zxing.Result
import com.journeyapps.barcodescanner.CaptureManager
import com.journeyapps.barcodescanner.DecoratedBarcodeView
import com.journeyapps.barcodescanner.BarcodeCallback
import com.journeyapps.barcodescanner.BarcodeResult
import java.text.SimpleDateFormat
import java.util.*

class QRScannerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityQrScannerBinding
    private val viewModel: QRScannerViewModel by viewModels()
    private lateinit var preferenceHelper: PreferenceHelper
    private lateinit var barcodeView: DecoratedBarcodeView
    private lateinit var capture: CaptureManager

    companion object {
        private const val CAMERA_PERMISSION_REQUEST = 100
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityQrScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        preferenceHelper = PreferenceHelper(this)

        setupToolbar()
        setupScanner()

        // Initialize CaptureManager with proper savedInstanceState
        capture.initializeFromIntent(intent, savedInstanceState)

        observeViewModel()
        checkPermissionAndStartScanning()
    }

    private fun setupToolbar() {
        binding.toolbar?.let { toolbar ->
            setSupportActionBar(toolbar)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
            supportActionBar?.title = "Scan QR Code Member"
        }
    }

    private fun setupScanner() {
        barcodeView = DecoratedBarcodeView(this)
        binding.scannerContainer.addView(barcodeView)

        val callback = object : BarcodeCallback {
            override fun barcodeResult(result: BarcodeResult) {
                handleResult(result.text)
            }
        }

        barcodeView.decodeContinuous(callback)
        capture = CaptureManager(this, barcodeView)
    }

    private fun checkPermissionAndStartScanning() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
            == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        } else {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CAMERA),
                CAMERA_PERMISSION_REQUEST
            )
        }
    }

    private fun startScanning() {
        capture.onResume()
    }

    private fun stopScanning() {
        capture.onPause()
    }

    private fun handleResult(qrCode: String) {
        // Stop scanning temporarily
        stopScanning()

        // Process QR code
        val adminId = preferenceHelper.getString(Constants.PREF_USER_ID)
        viewModel.processQRCode(qrCode, adminId)
    }

    private fun observeViewModel() {
        viewModel.scanResult.observe(this) { result ->
            result.onSuccess { registration ->
                showActivationDialog(registration)
            }.onFailure { error ->
                showErrorDialog(error.message ?: "QR Code tidak valid")
            }
        }

        viewModel.activationResult.observe(this) { result ->
            result.onSuccess { message ->
                showSuccessDialog(message)
            }.onFailure { error ->
                showErrorDialog(error.message ?: "Gagal aktivasi member")
            }
        }
    }

    private fun showActivationDialog(registration: com.duta.lubanagym.data.model.MemberRegistration) {
        val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("id", "ID"))

        AlertDialog.Builder(this)
            .setTitle("✅ QR Code Valid")
            .setMessage("""
                📋 Detail Pendaftaran:
                
                👤 Nama: ${registration.userFullName.ifEmpty { registration.userName }}
                📧 Email: ${registration.userEmail}
                📱 Phone: ${registration.userPhone}
                
                💳 Membership: ${registration.membershipType.uppercase()}
                ⏰ Durasi: ${registration.duration} Bulan
                💰 Harga: Rp ${formatPrice(registration.price)}
                
                📅 Tanggal Daftar: ${dateFormat.format(Date(registration.registrationDate))}
                ⏳ Valid Until: ${dateFormat.format(Date(registration.expiryDate))}
                
                Aktivasi member sekarang?
            """.trimIndent())
            .setPositiveButton("✅ Aktivasi") { _, _ ->
                val adminId = preferenceHelper.getString(Constants.PREF_USER_ID)
                viewModel.activateMember(registration.id, adminId)
            }
            .setNegativeButton("❌ Batal") { _, _ ->
                // Resume scanning
                startScanning()
            }
            .setCancelable(false)
            .show()
    }

    private fun showSuccessDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("🎉 Berhasil!")
            .setMessage(message)
            .setPositiveButton("✅ OK") { _, _ ->
                finish() // Kembali ke admin panel
            }
            .setCancelable(false)
            .show()
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(this)
            .setTitle("❌ Error")
            .setMessage(message)
            .setPositiveButton("🔄 Scan Lagi") { _, _ ->
                startScanning()
            }
            .setNegativeButton("❌ Batal") { _, _ ->
                finish()
            }
            .show()
    }

    private fun formatPrice(price: Int): String {
        return java.text.NumberFormat.getNumberInstance(Locale("id", "ID")).format(price)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == CAMERA_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startScanning()
            } else {
                Toast.makeText(this, "Permission camera diperlukan untuk scan QR Code", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        if (::capture.isInitialized &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            startScanning()
        }
    }

    override fun onPause() {
        super.onPause()
        if (::capture.isInitialized) {
            stopScanning()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::capture.isInitialized) {
            capture.onDestroy()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        if (::capture.isInitialized) {
            capture.onSaveInstanceState(outState)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}