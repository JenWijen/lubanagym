package com.duta.lubanagym.utils

object Constants {
    const val USERS_COLLECTION = "users"
    const val MEMBERS_COLLECTION = "members"
    const val STAFF_COLLECTION = "staff"
    const val TRAINERS_COLLECTION = "trainers" // Tetap ada tapi hanya sebagai data
    const val EQUIPMENT_COLLECTION = "equipment"
    const val TOKENS_COLLECTION = "tokens"

    // HAPUS ROLE_TRAINER dari role user
    const val ROLE_GUEST = "guest"
    const val ROLE_MEMBER = "member"
    const val ROLE_STAFF = "staff"
    const val ROLE_ADMIN = "admin"
    // const val ROLE_TRAINER = "trainer" // DIHAPUS

    const val MEMBERSHIP_BASIC = "basic"
    const val MEMBERSHIP_PREMIUM = "premium"
    const val MEMBERSHIP_VIP = "vip"

    // Profile completion - FIXED: Use val instead of const val for array
    val PROFILE_REQUIRED_FIELDS = arrayOf("fullName", "phone", "dateOfBirth", "gender")

    const val PREF_USER_ID = "user_id"
    const val PREF_USER_ROLE = "user_role"
    const val PREF_IS_LOGGED_IN = "is_logged_in"
    const val PREF_PROFILE_COMPLETE = "profile_complete"

    // Equipment status constants
    const val EQUIPMENT_AVAILABLE = "available"
    const val EQUIPMENT_MAINTENANCE = "maintenance"
    const val EQUIPMENT_BROKEN = "broken"

    // Profile field names (for validation)
    const val FIELD_FULL_NAME = "fullName"
    const val FIELD_PHONE = "phone"
    const val FIELD_DATE_OF_BIRTH = "dateOfBirth"
    const val FIELD_GENDER = "gender"
    const val FIELD_ADDRESS = "address"
    const val FIELD_EMERGENCY_CONTACT = "emergencyContact"
    const val FIELD_EMERGENCY_PHONE = "emergencyPhone"
    const val FIELD_BLOOD_TYPE = "bloodType"
    const val FIELD_ALLERGIES = "allergies"

    // Gender options
    const val GENDER_MALE = "male"
    const val GENDER_FEMALE = "female"

    // Validation constants
    const val MIN_NAME_LENGTH = 3
    const val MIN_PHONE_LENGTH = 10
    const val MIN_PASSWORD_LENGTH = 6

    // Helper functions for profile completion
    fun getRequiredFields(): Array<String> {
        return PROFILE_REQUIRED_FIELDS
    }

    fun isValidRole(role: String): Boolean {
        // HAPUS ROLE_TRAINER dari validasi
        return role in arrayOf(ROLE_GUEST, ROLE_MEMBER, ROLE_STAFF, ROLE_ADMIN)
    }

    fun isValidMembershipType(type: String): Boolean {
        return type in arrayOf(MEMBERSHIP_BASIC, MEMBERSHIP_PREMIUM, MEMBERSHIP_VIP)
    }

    fun isValidEquipmentStatus(status: String): Boolean {
        return status in arrayOf(EQUIPMENT_AVAILABLE, EQUIPMENT_MAINTENANCE, EQUIPMENT_BROKEN)
    }

    fun getGenderOptions(): Array<String> {
        return arrayOf(GENDER_MALE, GENDER_FEMALE)
    }
}