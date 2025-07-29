package com.duta.lubanagym.ui.admin

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.duta.lubanagym.data.firebase.FirebaseService
import com.duta.lubanagym.data.model.Trainer
import com.duta.lubanagym.data.repository.TrainerRepository
import kotlinx.coroutines.launch

class TrainerManagementViewModel : ViewModel() {

    private val firebaseService = FirebaseService()
    private val trainerRepository = TrainerRepository(firebaseService)

    enum class SortType {
        NEWEST_FIRST,
        OLDEST_FIRST,
        NAME_A_Z,
        NAME_Z_A,
        SPECIALIZATION_A_Z
    }

    private val _trainerList = MutableLiveData<Result<List<Trainer>>>()
    val trainerList: LiveData<Result<List<Trainer>>> = _trainerList

    private val _filteredTrainerList = MutableLiveData<Result<List<Trainer>>>()
    val filteredTrainerList: LiveData<Result<List<Trainer>>> = _filteredTrainerList

    private val _createResult = MutableLiveData<Result<String>>()
    val createResult: LiveData<Result<String>> = _createResult

    private val _updateResult = MutableLiveData<Result<Unit>>()
    val updateResult: LiveData<Result<Unit>> = _updateResult

    private val _deleteResult = MutableLiveData<Result<Unit>>()
    val deleteResult: LiveData<Result<Unit>> = _deleteResult

    private var allTrainers = listOf<Trainer>()
    private var currentSearchQuery = ""
    private var currentSpecializationFilter: String? = null
    private var currentStatusFilter: Boolean? = null
    private var currentSortType = SortType.NEWEST_FIRST

    fun loadTrainers() {
        viewModelScope.launch {
            try {
                val result = trainerRepository.getAllTrainers()
                result.onSuccess { trainers ->
                    allTrainers = trainers
                    applyFiltersAndSort()
                }
                _trainerList.postValue(result)
            } catch (e: Exception) {
                _trainerList.postValue(Result.failure(e))
                _filteredTrainerList.postValue(Result.failure(e))
            }
        }
    }

    fun searchTrainers(query: String) {
        currentSearchQuery = query
        applyFiltersAndSort()
    }

    fun filterBySpecialization(specialization: String?) {
        currentSpecializationFilter = if (specialization == "Semua Spesialisasi") null else specialization
        applyFiltersAndSort()
    }

    fun filterByStatus(isActive: Boolean?) {
        currentStatusFilter = isActive
        applyFiltersAndSort()
    }

    fun sortTrainers(sortType: SortType) {
        currentSortType = sortType
        applyFiltersAndSort()
    }

    fun resetFilters() {
        currentSearchQuery = ""
        currentSpecializationFilter = null
        currentStatusFilter = null
        currentSortType = SortType.NEWEST_FIRST
        applyFiltersAndSort()
    }

    private fun applyFiltersAndSort() {
        var filteredTrainers = allTrainers

        // Apply search filter
        if (currentSearchQuery.isNotEmpty()) {
            filteredTrainers = filteredTrainers.filter { trainer ->
                trainer.name.contains(currentSearchQuery, ignoreCase = true) ||
                        trainer.specialization.contains(currentSearchQuery, ignoreCase = true) ||
                        trainer.experience.contains(currentSearchQuery, ignoreCase = true) ||
                        trainer.phone.contains(currentSearchQuery, ignoreCase = true) ||
                        trainer.bio.contains(currentSearchQuery, ignoreCase = true)
            }
        }

        // Apply specialization filter
        currentSpecializationFilter?.let { specialization ->
            filteredTrainers = filteredTrainers.filter { trainer ->
                trainer.specialization.equals(specialization, ignoreCase = true)
            }
        }

        // Apply status filter
        currentStatusFilter?.let { isActive ->
            filteredTrainers = filteredTrainers.filter { trainer ->
                trainer.isActive == isActive
            }
        }

        // Apply sorting
        filteredTrainers = when (currentSortType) {
            SortType.NEWEST_FIRST -> filteredTrainers.sortedByDescending { it.createdAt }
            SortType.OLDEST_FIRST -> filteredTrainers.sortedBy { it.createdAt }
            SortType.NAME_A_Z -> filteredTrainers.sortedBy { it.name }
            SortType.NAME_Z_A -> filteredTrainers.sortedByDescending { it.name }
            SortType.SPECIALIZATION_A_Z -> filteredTrainers.sortedBy { it.specialization }
        }

        _filteredTrainerList.postValue(Result.success(filteredTrainers))
    }

    fun createTrainer(trainer: Trainer) {
        viewModelScope.launch {
            try {
                val result = trainerRepository.createTrainer(trainer)
                _createResult.postValue(result)
            } catch (e: Exception) {
                _createResult.postValue(Result.failure(e))
            }
        }
    }

    fun updateTrainer(trainerId: String, updates: Map<String, Any>) {
        viewModelScope.launch {
            try {
                val result = trainerRepository.updateTrainer(trainerId, updates)
                _updateResult.postValue(result)
            } catch (e: Exception) {
                _updateResult.postValue(Result.failure(e))
            }
        }
    }

    fun deleteTrainer(trainerId: String) {
        viewModelScope.launch {
            try {
                val result = trainerRepository.deleteTrainer(trainerId)
                _deleteResult.postValue(result)
            } catch (e: Exception) {
                _deleteResult.postValue(Result.failure(e))
            }
        }
    }
}