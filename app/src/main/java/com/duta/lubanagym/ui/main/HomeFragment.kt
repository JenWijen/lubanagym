package com.duta.lubanagym.ui.main

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.duta.lubanagym.databinding.FragmentHomeBinding
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.utils.DummyDataHelper
import kotlinx.coroutines.launch

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupViews()
    }

    private fun setupViews() {
        // Setup gym info, announcements, etc.
        binding.tvGymName.text = "Lubana Gym"
        binding.tvWelcome.text = "Selamat datang di Lubana Gym! Tempat terbaik untuk mencapai target fitness Anda."

        // Add more gym information
        binding.tvGymDescription.text = """
            Lubana Gym hadir untuk membantu Anda mencapai tubuh ideal dengan:
            • Peralatan fitness modern dan lengkap
            • Trainer profesional dan berpengalaman  
            • Lingkungan yang nyaman dan bersih
            • Program latihan yang disesuaikan dengan kebutuhan
            
            Jam Operasional:
            Senin - Jumat: 06.00 - 22.00
            Sabtu - Minggu: 07.00 - 21.00
        """.trimIndent()

        // TAMBAHAN: Button untuk setup dummy data (DEVELOPMENT ONLY)
        setupDevelopmentButton()
    }

    private fun setupDevelopmentButton() {
        // Tambah listener ke gym name - tap 5x untuk show setup
        var tapCount = 0
        binding.tvGymName.setOnClickListener {
            tapCount++
            if (tapCount >= 5) {
                showDummyDataDialog()
                tapCount = 0
            }
        }

        // ATAU bisa tambah button tersembunyi
        binding.btnHiddenSetup?.visibility = View.VISIBLE
        binding.btnHiddenSetup?.setOnClickListener {
            showDummyDataDialog()
        }
    }

    private fun showDummyDataDialog() {
        AlertDialog.Builder(requireContext())
            .setTitle("🔧 Setup Development Data")
            .setMessage("Buat data dummy untuk testing aplikasi?\n\n📱 Akun yang akan dibuat:\n• Admin: admin@lubanagym.com / admin123\n• Staff: staff@lubanagym.com / staff123\n\n🎫 Token Registration:\n• GYM001, GYM002, GYM003, TEST123, DEMO456\n\n🏋️ Sample Data:\n• 5 Equipment gym\n• 3 Trainers\n\n⚠️ Proses ini butuh koneksi internet yang stabil!")
            .setPositiveButton("✅ Buat Sekarang") { _, _ ->
                createDummyData()
            }
            .setNeutralButton("🔑 Buat Admin Saja") { _, _ ->
                createAdminOnly()
            }
            .setNegativeButton("❌ Batal", null)
            .show()
    }

    private fun createDummyData() {
        lifecycleScope.launch {
            try {
                val firebaseService = FirebaseService()
                val dummyDataHelper = DummyDataHelper(firebaseService)

                // Show progress
                binding.tvWelcome.text = "🔄 Sedang membuat data dummy... Mohon tunggu..."

                val result = dummyDataHelper.createAllDummyData()

                result.onSuccess { message ->
                    binding.tvWelcome.text = "✅ Data dummy berhasil dibuat! Silakan login dengan akun admin."

                    AlertDialog.Builder(requireContext())
                        .setTitle("🎉 Berhasil!")
                        .setMessage("Data dummy berhasil dibuat!\n\n🔑 Login dengan:\nEmail: admin@lubanagym.com\nPassword: admin123\n\n📱 Buka tab Profile → Login untuk memulai!")
                        .setPositiveButton("OK", null)
                        .show()
                }.onFailure { error ->
                    binding.tvWelcome.text = "❌ Gagal membuat data dummy. Coba lagi."
                    Toast.makeText(requireContext(), "Error: ${error.message}", Toast.LENGTH_LONG).show()
                }

            } catch (e: Exception) {
                binding.tvWelcome.text = "❌ Terjadi kesalahan. Periksa koneksi internet."
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun createAdminOnly() {
        lifecycleScope.launch {
            try {
                val firebaseService = FirebaseService()
                val dummyDataHelper = DummyDataHelper(firebaseService)

                binding.tvWelcome.text = "🔄 Membuat akun admin..."

                // Create admin
                val adminResult = dummyDataHelper.createDummyAdmin()

                // Create some tokens
                val tokenResult = dummyDataHelper.createDummyTokens()

                if (adminResult.isSuccess && tokenResult.isSuccess) {
                    binding.tvWelcome.text = "✅ Admin berhasil dibuat! Silakan login."

                    AlertDialog.Builder(requireContext())
                        .setTitle("🎉 Admin Berhasil Dibuat!")
                        .setMessage("🔑 Login dengan:\nEmail: admin@lubanagym.com\nPassword: admin123\n\n🎫 Token tersedia: GYM001, GYM002, GYM003\n\n📱 Buka tab Profile → Login!")
                        .setPositiveButton("OK", null)
                        .show()
                } else {
                    binding.tvWelcome.text = "❌ Gagal membuat admin. Coba lagi."
                    Toast.makeText(requireContext(), "Gagal membuat admin", Toast.LENGTH_SHORT).show()
                }

            } catch (e: Exception) {
                binding.tvWelcome.text = "❌ Terjadi kesalahan. Periksa koneksi internet."
                Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}