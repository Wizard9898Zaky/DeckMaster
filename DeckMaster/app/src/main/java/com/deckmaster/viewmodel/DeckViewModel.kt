package com.deckmaster.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.deckmaster.engine.SpreadEngine
import com.deckmaster.engine.SpreadEngine.SpreadResult
import com.deckmaster.engine.SpreadEngine.SpreadType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

class DeckViewModel : ViewModel() {

    // ──────────────────────────────────────────────────────────────────────────
    // Pre-computed tablets (expensive — compute once on background thread)
    // ──────────────────────────────────────────────────────────────────────────
    private var tablets: Array<List<String>>? = null

    init {
        viewModelScope.launch(Dispatchers.Default) {
            tablets = SpreadEngine.getTablets()
            _tabletsReady.value = true
        }
    }

    private val _tabletsReady = MutableStateFlow(false)
    val tabletsReady: StateFlow<Boolean> = _tabletsReady.asStateFlow()

    // ──────────────────────────────────────────────────────────────────────────
    // User inputs
    // ──────────────────────────────────────────────────────────────────────────
    data class BirthData(
        val year: Int, val month: Int, val day: Int,
        val bornBeforeSunrise: Boolean = false
    )

    private val _birthData = MutableStateFlow<BirthData?>(null)
    val birthData: StateFlow<BirthData?> = _birthData.asStateFlow()

    private val _targetDate = MutableStateFlow<LocalDate>(LocalDate.now())
    val targetDate: StateFlow<LocalDate> = _targetDate.asStateFlow()

    // ──────────────────────────────────────────────────────────────────────────
    // Computed results
    // ──────────────────────────────────────────────────────────────────────────
    private val _spreads = MutableStateFlow<List<SpreadResult>>(emptyList())
    val spreads: StateFlow<List<SpreadResult>> = _spreads.asStateFlow()

    private val _birthCard = MutableStateFlow<String>("")
    val birthCard: StateFlow<String> = _birthCard.asStateFlow()

    private val _isCalculating = MutableStateFlow(false)
    val isCalculating: StateFlow<Boolean> = _isCalculating.asStateFlow()

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    // ──────────────────────────────────────────────────────────────────────────
    // UI state
    // ──────────────────────────────────────────────────────────────────────────
    private val _selectedSpreadIndex = MutableStateFlow(0)
    val selectedSpreadIndex: StateFlow<Int> = _selectedSpreadIndex.asStateFlow()

    private val _selectedCardIndex = MutableStateFlow<Int?>(null)
    val selectedCardIndex: StateFlow<Int?> = _selectedCardIndex.asStateFlow()

    // ──────────────────────────────────────────────────────────────────────────
    // Actions
    // ──────────────────────────────────────────────────────────────────────────

    fun setBirthData(year: Int, month: Int, day: Int, bornBeforeSunrise: Boolean) {
        var m = month; var d = day; var y = year
        if (bornBeforeSunrise) {
            val (ay, am, ad) = SpreadEngine.adjustForSunrise(y, m, d)
            y = ay; m = am; d = ad
        }
        _birthData.value = BirthData(y, m, d, bornBeforeSunrise)
        recalculate()
    }

    fun setTargetDate(date: LocalDate) {
        _targetDate.value = date
        recalculate()
    }

    fun selectSpread(index: Int) {
        _selectedSpreadIndex.value = index
        _selectedCardIndex.value = null
    }

    fun selectCard(index: Int) {
        _selectedCardIndex.value = if (_selectedCardIndex.value == index) null else index
    }

    fun clearError() { _error.value = null }

    private fun recalculate() {
        val bd = _birthData.value ?: return
        val td = _targetDate.value
        val t = tablets ?: return

        viewModelScope.launch(Dispatchers.Default) {
            _isCalculating.value = true
            _error.value = null
            try {
                val bc = SpreadEngine.getBirthCard(bd.month, bd.day, t)
                val results = SpreadEngine.getSpreads(
                    bd.year, bd.month, bd.day,
                    td.year, td.monthValue, td.dayOfMonth,
                    t
                )
                withContext(Dispatchers.Main) {
                    _birthCard.value = bc
                    _spreads.value = results
                    _selectedCardIndex.value = null
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _error.value = e.message ?: "Calculation error"
                }
            } finally {
                _isCalculating.value = false
            }
        }
    }

    /** Call after tablets are ready to trigger initial calculation if birth data already set. */
    fun onTabletsReady() {
        if (_birthData.value != null) recalculate()
    }
}
