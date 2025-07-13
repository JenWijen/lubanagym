package com.duta.lubanagym.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.duta.lubanagym.databinding.FragmentHomeBinding
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.utils.DummyDataHelper
import com.duta.lubanagym.utils.PreferenceHelper
import com.duta.lubanagym.utils.Constants
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var preferenceHelper: PreferenceHelper

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
        preferenceHelper = PreferenceHelper(requireContext())
        setupViews()
        loadUserSpecificContent()
    }

    private fun setupViews() {
        // Setup gym info
        binding.tvGymName.text = "LUBANA GYM"

        // Welcome message berdasarkan waktu
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 5..11 -> "Selamat Pagi"
            in 12..14 -> "Selamat Siang"
            in 15..17 -> "Selamat Sore"
            else -> "Selamat Malam"
        }

        val isLoggedIn = preferenceHelper.getBoolean(Constants.PREF_IS_LOGGED_IN)
        if (isLoggedIn) {
            val userName = preferenceHelper.getString("user_name", "Member")
            binding.tvWelcome.text = "$greeting, $userName! 💪\nSiap untuk workout hari ini?"
        } else {
            binding.tvWelcome.text = "$greeting!\nSelamat datang di Lubana Gym - tempat terbaik untuk mencapai target fitness Anda! 🏋️‍♂️"
        }

        // Gym description dengan info lebih lengkap
        binding.tvGymDescription.text = """
            🏋️‍♂️ Lubana Gym - Your Fitness Partner
            
            🎯 Fasilitas Unggulan:
            • Peralatan fitness modern dan lengkap
            • Trainer profesional bersertifikat
            • Area kardio dengan AC dan musik energik
            • Ruang weight training dengan barbel hingga 200kg
            • Locker room bersih dengan shower air hangat
            • Parkir luas dan aman
            
            ⏰ Jam Operasional:
            Senin - Jumat: 06.00 - 22.00
            Sabtu - Minggu: 07.00 - 21.00
            
            💰 Paket Membership:
            🥉 BASIC (Rp 150.000/bulan)
            - Akses gym unlimited
            - Locker pribadi
            
            🥈 PREMIUM (Rp 250.000/bulan)  
            - Semua fasilitas BASIC
            - Konsultasi gratis dengan trainer
            - Akses kelas grup (yoga, aerobik)
            
            🥇 VIP (Rp 400.000/bulan)
            - Semua fasilitas PREMIUM
            - Personal trainer 2x/bulan
            - Priority booking kelas
            - Massage therapy 1x/bulan
            
            📞 Info & Pendaftaran:
            WhatsApp: 0812-3456-7890
            Instagram: @lubanagym
        """.trimIndent()

        // Setup quick stats
        loadQuickStats()
    }

    private fun loadUserSpecificContent() {
        val isLoggedIn = preferenceHelper.getBoolean(Constants.PREF_IS_LOGGED_IN)
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)

        if (isLoggedIn) {
            when (userRole) {
                Constants.ROLE_GUEST -> setupGuestContent()
                Constants.ROLE_MEMBER -> setupMemberContent()
                Constants.ROLE_ADMIN -> setupStaffAdminContent()
                else -> setupGuestContent()
            }
        } else {
            setupGuestContent()
        }
    }

    private fun setupMemberContent() {
        // Show member-specific content
        binding.tvMembershipInfo.text = """
            🎯 Tips Workout Hari Ini:
            • Lakukan pemanasan 10-15 menit sebelum latihan
            • Fokus pada form yang benar, bukan berat yang maksimal
            • Istirahat 48-72 jam antar sesi untuk otot yang sama
            • Jangan lupa minum air 2-3 liter per hari
            • Cool down dan stretching setelah workout
            
            🍎 Nutrisi Penting:
            • Protein: 1.6-2.2g per kg berat badan
            • Makan dalam 30 menit setelah workout
            • Kombinasi protein + karbohidrat untuk recovery
            
            🏋️ Trainer Tersedia:
            • Konsultasi dengan trainer profesional kami
            • Personal training untuk hasil maksimal
            • Program khusus sesuai kebutuhan Anda
        """.trimIndent()

        binding.tvMembershipInfo.visibility = View.VISIBLE
    }

    private fun setupStaffAdminContent() {
        binding.tvMembershipInfo.text = """
            👨‍💼 Dashboard Staff/Admin:
            • Monitoring operasional gym harian
            • Memastikan kebersihan dan keamanan
            • Melayani member dengan ramah dan profesional
            • Update data member, staff, trainer dan equipment
            • Koordinasi dengan trainer untuk program member
            
            📊 Fokus Hari Ini:
            • Cek kondisi semua equipment
            • Update data membership yang akan expired
            • Follow up inquiry calon member baru
            • Pastikan area gym bersih dan nyaman
            • Kelola data trainer dan jadwal mereka
        """.trimIndent()

        binding.tvMembershipInfo.visibility = View.VISIBLE
    }

    private fun setupGuestContent() {
        binding.tvMembershipInfo.text = """
            🎉 Bergabung dengan Lubana Gym!
            
            🔥 Promo Spesial Bulan Ini:
            • Daftar sekarang dapat diskon 20%
            • Free trial 3 hari untuk member baru
            • Gratis konsultasi dengan trainer profesional
            • Tidak ada biaya pendaftaran
            
            💪 Mengapa Memilih Lubana Gym?
            • Lokasi strategis dan mudah diakses
            • Harga terjangkau dengan fasilitas premium
            • Trainer berpengalaman dan bersertifikat
            • Suasana workout yang motivating
            • Community yang supportive
            
            🏋️ Trainer Profesional:
            • 8+ trainer berpengalaman
            • Spesialisasi berbagai bidang fitness
            • Personal training tersedia
            • Konsultasi gratis untuk member
            
            📱 Daftar Sekarang:
            Hubungi admin untuk mendapatkan token registrasi
            dan nikmati semua fasilitas terbaik kami!
        """.trimIndent()

        binding.tvMembershipInfo.visibility = View.VISIBLE

        // NEW: Show member registration button for guests
        showMemberRegistrationButton()
    }

    private fun loadQuickStats() {
        // Tampilkan statistik gym (bisa diambil dari database)
        binding.tvQuickStats.text = """
            📊 Lubana Gym Statistics:
            
            👥 Active Members: 150+
            🏋️‍♂️ Professional Trainers: 8
            💪 Equipment Units: 50+
            ⭐ Member Satisfaction: 4.8/5
            🏆 Years of Experience: 5+
            
            🎯 This Month Achievement:
            • 25 new members joined
            • 120 workout sessions completed
            • 15 fitness goals achieved
            • 95% member retention rate
        """.trimIndent()

        binding.tvQuickStats.visibility = View.VISIBLE
    }

    private fun showMemberRegistrationButton() {
        // Show button for guest to become member
        binding.btnBecomeMember.visibility = View.VISIBLE
        binding.btnBecomeMember.setOnClickListener {
            val intent = Intent(requireContext(), com.duta.lubanagym.ui.member.RegisterMemberActivity::class.java)
            startActivity(intent)
        }
    }




    override fun onResume() {
        super.onResume()
        // Refresh content when user returns to home
        loadUserSpecificContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}