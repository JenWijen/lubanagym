package com.duta.lubanagym.ui.main

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.duta.lubanagym.databinding.FragmentHomeBinding
import com.duta.lubanagym.utils.PreferenceHelper
import com.duta.lubanagym.utils.Constants
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

        setupWelcomeMessage()
        setupUserSpecificContent()
        setupClickListeners()
    }

    private fun setupWelcomeMessage() {
        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when (currentHour) {
            in 5..11 -> "Good Morning"
            in 12..14 -> "Good Afternoon"
            in 15..17 -> "Good Evening"
            else -> "Good Night"
        }

        val isLoggedIn = preferenceHelper.getBoolean(Constants.PREF_IS_LOGGED_IN)
        val welcomeMessage = if (isLoggedIn) {
            val userName = preferenceHelper.getString("user_name", "Member")
            "$greeting, $userName! 💪\nReady for your workout today?"
        } else {
            "$greeting! Welcome to Lubana Gym 🏋️‍♂️\nYour fitness journey starts here!"
        }

        binding.tvWelcome.text = welcomeMessage
    }

    private fun setupUserSpecificContent() {
        val isLoggedIn = preferenceHelper.getBoolean(Constants.PREF_IS_LOGGED_IN)
        val userRole = preferenceHelper.getString(Constants.PREF_USER_ROLE)

        when {
            !isLoggedIn -> setupGuestContent()
            userRole == Constants.ROLE_MEMBER -> setupMemberContent()
            userRole == Constants.ROLE_ADMIN || userRole == Constants.ROLE_STAFF -> setupStaffAdminContent()
            else -> setupGuestContent()
        }
    }

    private fun setupGuestContent() {
        binding.tvMembershipInfo.text = """
            🎯 Why Choose Lubana Gym?
            
            ✅ Modern equipment & clean facilities
            ✅ Professional certified trainers
            ✅ Flexible membership options
            ✅ Free trial for new members
            ✅ Community of fitness enthusiasts
            
            📱 Contact us:
            WhatsApp: 0812-3456-7890
            Email: info@lubanagym.com
        """.trimIndent()

        binding.tvMembershipInfo.visibility = View.VISIBLE
        binding.btnBecomeMember.visibility = View.VISIBLE
    }

    private fun setupMemberContent() {
        binding.tvMembershipInfo.text = """
            🔥 Today's Workout Tips:
            
            💪 Warm up for 10-15 minutes before training
            🎯 Focus on proper form over heavy weight
            💧 Stay hydrated - drink 2-3 liters daily
            ⚡ Rest 48-72 hours between muscle groups
            🧘 Don't forget cool down & stretching
            
            🏋️ Personal trainers are available for consultation!
        """.trimIndent()

        binding.tvMembershipInfo.visibility = View.VISIBLE
        binding.btnBecomeMember.visibility = View.GONE
    }

    private fun setupStaffAdminContent() {
        binding.tvMembershipInfo.text = """
            👨‍💼 Staff Dashboard Overview:
            
            ✅ Monitor daily gym operations
            ✅ Ensure cleanliness and safety
            ✅ Assist members professionally  
            ✅ Update member & equipment data
            ✅ Coordinate with trainers
            
            📊 Focus today: Member service excellence!
        """.trimIndent()

        binding.tvMembershipInfo.visibility = View.VISIBLE
        binding.btnBecomeMember.visibility = View.GONE
    }

    private fun setupClickListeners() {
        binding.btnBecomeMember.setOnClickListener {
            val intent = Intent(requireContext(), com.duta.lubanagym.ui.member.RegisterMemberActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        // Refresh content when user returns
        setupWelcomeMessage()
        setupUserSpecificContent()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}