package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.WaterRecord
import com.example.data.WaterRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AppScreen {
    HOME,
    SUCCESS
}

class WaterViewModel(private val repository: WaterRepository) : ViewModel() {

    private fun getTodayDateString(): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(Date())
    }

    private val _currentDate = MutableStateFlow(getTodayDateString())
    val currentDate: StateFlow<String> = _currentDate.asStateFlow()

    private val _currentScreen = MutableStateFlow(AppScreen.HOME)
    val currentScreen: StateFlow<AppScreen> = _currentScreen.asStateFlow()

    val todayRecord: StateFlow<WaterRecord> = _currentDate
        .flatMapLatest { date ->
            repository.getRecord(date).map { record ->
                record ?: WaterRecord(date = date, cups = 0, goal = 8)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = WaterRecord(date = getTodayDateString(), cups = 0, goal = 8)
        )

    val allHistory: StateFlow<List<WaterRecord>> = repository.allRecords
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun drinkCup() {
        viewModelScope.launch {
            val currentRecord = todayRecord.value
            val newCups = currentRecord.cups + 1
            val updated = currentRecord.copy(cups = newCups)
            repository.saveRecord(updated)

            // If we reached or exceeded the goal, transition to success page
            if (newCups == currentRecord.goal) {
                _currentScreen.value = AppScreen.SUCCESS
            }
        }
    }

    fun navigateTo(screen: AppScreen) {
        _currentScreen.value = screen
    }

    fun resetToday() {
        viewModelScope.launch {
            val currentRecord = todayRecord.value
            repository.saveRecord(currentRecord.copy(cups = 0))
            _currentScreen.value = AppScreen.HOME
        }
    }

    fun adjustGoal(newGoal: Int) {
        if (newGoal in 1..20) {
            viewModelScope.launch {
                val currentRecord = todayRecord.value
                repository.saveRecord(currentRecord.copy(goal = newGoal))
            }
        }
    }

    fun refreshDate() {
        _currentDate.value = getTodayDateString()
    }
}

class WaterViewModelFactory(private val repository: WaterRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(WaterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return WaterViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
