package com.duta.lubanagym.utils

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.duta.lubanagym.data.repository.*
import com.duta.lubanagym.ui.auth.LoginViewModel
import com.duta.lubanagym.ui.auth.RegisterViewModel
import com.duta.lubanagym.ui.main.EquipmentViewModel
import com.duta.lubanagym.ui.main.ProfileViewModel
import com.duta.lubanagym.ui.admin.*

class ViewModelFactory(
    private val authRepository: AuthRepository,
    private val userRepository: UserRepository,
    private val memberRepository: MemberRepository,
    private val staffRepository: StaffRepository,
    private val trainerRepository: TrainerRepository,
    private val equipmentRepository: EquipmentRepository,
    private val tokenRepository: TokenRepository
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return when {
            modelClass.isAssignableFrom(LoginViewModel::class.java) -> {
                LoginViewModel() as T
            }
            modelClass.isAssignableFrom(RegisterViewModel::class.java) -> {
                RegisterViewModel() as T
            }
            modelClass.isAssignableFrom(EquipmentViewModel::class.java) -> {
                EquipmentViewModel() as T
            }
            modelClass.isAssignableFrom(ProfileViewModel::class.java) -> {
                ProfileViewModel() as T
            }
            modelClass.isAssignableFrom(UserManagementViewModel::class.java) -> {
                UserManagementViewModel() as T
            }
            modelClass.isAssignableFrom(MemberManagementViewModel::class.java) -> {
                MemberManagementViewModel() as T
            }
            modelClass.isAssignableFrom(StaffManagementViewModel::class.java) -> {
                StaffManagementViewModel() as T
            }
            modelClass.isAssignableFrom(TrainerManagementViewModel::class.java) -> {
                TrainerManagementViewModel() as T
            }
            modelClass.isAssignableFrom(EquipmentManagementViewModel::class.java) -> {
                EquipmentManagementViewModel() as T
            }
            modelClass.isAssignableFrom(TokenManagementViewModel::class.java) -> {
                TokenManagementViewModel() as T
            }
            else -> throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}