package com.duta.lubanagym.ui.main

import androidx.lifecycle.ViewModel

class MainViewModel : ViewModel() {
    // Simple ViewModel untuk MainActivity
    // Bisa ditambahkan state management jika diperlukan nanti

    private var _currentFragmentTag: String = "home"
    val currentFragmentTag: String get() = _currentFragmentTag

    fun setCurrentFragment(tag: String) {
        _currentFragmentTag = tag
    }
}