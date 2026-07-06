package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.StudySplitDatabase
import com.example.data.repository.StudySplitRepository
import com.example.data.repository.MemberBalance
import com.example.data.repository.SettlementSuggestion
import com.example.data.model.Expense
import com.example.data.model.ExpenseSplit
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.Profile
import com.example.data.model.Settlement
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class StudySplitViewModel(
    application: Application,
    private val repository: StudySplitRepository
) : AndroidViewModel(application) {

    // All available profiles in the app
    val allProfiles: StateFlow<List<Profile>> = repository.allProfiles
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // All groups in the app
    val allGroups: StateFlow<List<Group>> = repository.allGroups
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Currently logged-in profile ("Me")
    private val _currentProfile = MutableStateFlow<Profile?>(null)
    val currentProfile: StateFlow<Profile?> = _currentProfile.asStateFlow()

    // Currently selected group
    private val _currentGroup = MutableStateFlow<Group?>(null)
    val currentGroup: StateFlow<Group?> = _currentGroup.asStateFlow()

    // Current group state
    private val _groupProfiles = MutableStateFlow<List<Profile>>(emptyList())
    val groupProfiles: StateFlow<List<Profile>> = _groupProfiles.asStateFlow()

    private val _groupExpenses = MutableStateFlow<List<Expense>>(emptyList())
    val groupExpenses: StateFlow<List<Expense>> = _groupExpenses.asStateFlow()

    private val _groupSettlements = MutableStateFlow<List<Settlement>>(emptyList())
    val groupSettlements: StateFlow<List<Settlement>> = _groupSettlements.asStateFlow()

    private val _groupBalances = MutableStateFlow<List<MemberBalance>>(emptyList())
    val groupBalances: StateFlow<List<MemberBalance>> = _groupBalances.asStateFlow()

    private val _groupSuggestions = MutableStateFlow<List<SettlementSuggestion>>(emptyList())
    val groupSuggestions: StateFlow<List<SettlementSuggestion>> = _groupSuggestions.asStateFlow()

    init {
        // Initialize current active user on launch
        viewModelScope.launch {
            val profiles = repository.allProfiles.first()
            if (profiles.isNotEmpty()) {
                // Default to the first profile as active user
                _currentProfile.value = profiles.first()
            }
        }
    }

    fun selectProfile(profile: Profile) {
        _currentProfile.value = profile
    }

    fun selectGroup(group: Group?) {
        _currentGroup.value = group
        if (group != null) {
            refreshGroupData(group.id)
        } else {
            _groupProfiles.value = emptyList()
            _groupExpenses.value = emptyList()
            _groupSettlements.value = emptyList()
            _groupBalances.value = emptyList()
            _groupSuggestions.value = emptyList()
        }
    }

    fun refreshGroupData(groupId: Long) {
        viewModelScope.launch {
            // Load and cache
            repository.getGroupProfiles(groupId).collect { profiles ->
                _groupProfiles.value = profiles
                updateBalancesAndSuggestions(groupId)
            }
        }
        viewModelScope.launch {
            repository.getGroupExpenses(groupId).collect { expenses ->
                _groupExpenses.value = expenses
                updateBalancesAndSuggestions(groupId)
            }
        }
        viewModelScope.launch {
            repository.getGroupSettlements(groupId).collect { settlements ->
                _groupSettlements.value = settlements
                updateBalancesAndSuggestions(groupId)
            }
        }
    }

    private suspend fun updateBalancesAndSuggestions(groupId: Long) {
        _groupBalances.value = repository.calculateBalances(groupId)
        _groupSuggestions.value = repository.calculateOptimizedSettlements(groupId)
    }

    // --- Actions ---

    fun createProfile(name: String, avatarUrl: String, hostel: String, onComplete: (Profile) -> Unit = {}) {
        viewModelScope.launch {
            val id = repository.createProfile(name, avatarUrl, hostel)
            val newProfile = repository.getProfileById(id)
            if (newProfile != null) {
                if (_currentProfile.value == null) {
                    _currentProfile.value = newProfile
                }
                onComplete(newProfile)
            }
        }
    }

    fun createGroup(name: String, onComplete: (Group) -> Unit) {
        viewModelScope.launch {
            val me = _currentProfile.value ?: return@launch
            val groupId = repository.createGroup(name, me.id)
            val group = repository.getGroupById(groupId)
            if (group != null) {
                selectGroup(group)
                onComplete(group)
            }
        }
    }

    fun joinGroup(inviteCode: String, onResult: (Boolean, Group?) -> Unit) {
        viewModelScope.launch {
            val me = _currentProfile.value ?: return@launch
            val group = repository.joinGroupByInviteCode(inviteCode, me.id)
            if (group != null) {
                selectGroup(group)
                onResult(true, group)
            } else {
                onResult(false, null)
            }
        }
    }

    fun addExpense(
        title: String,
        amount: Double,
        paidByProfileId: Long,
        category: String,
        notes: String,
        date: Long,
        splits: List<ExpenseSplit>
    ) {
        viewModelScope.launch {
            val group = _currentGroup.value ?: return@launch
            val expense = Expense(
                groupId = group.id,
                paidBy = paidByProfileId,
                amount = amount,
                title = title,
                category = category,
                date = date,
                notes = notes
            )
            repository.addExpenseWithSplits(expense, splits)
            refreshGroupData(group.id)
        }
    }

    fun updateExpense(
        expenseId: Long,
        title: String,
        amount: Double,
        paidByProfileId: Long,
        category: String,
        notes: String,
        date: Long,
        splits: List<ExpenseSplit>
    ) {
        viewModelScope.launch {
            val group = _currentGroup.value ?: return@launch
            val existing = repository.getExpenseById(expenseId) ?: return@launch
            val updated = existing.copy(
                paidBy = paidByProfileId,
                amount = amount,
                title = title,
                category = category,
                date = date,
                notes = notes
            )
            repository.updateExpenseWithSplits(updated, splits)
            refreshGroupData(group.id)
        }
    }

    fun deleteExpense(expense: Expense) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _currentGroup.value?.id?.let { refreshGroupData(it) }
        }
    }

    fun addSettlement(paidBy: Long, paidTo: Long, amount: Double) {
        viewModelScope.launch {
            val group = _currentGroup.value ?: return@launch
            val settlement = Settlement(
                groupId = group.id,
                paidBy = paidBy,
                paidTo = paidTo,
                amount = amount
            )
            repository.addSettlement(settlement)
            refreshGroupData(group.id)
        }
    }

    fun deleteSettlement(settlement: Settlement) {
        viewModelScope.launch {
            repository.deleteSettlement(settlement)
            _currentGroup.value?.id?.let { refreshGroupData(it) }
        }
    }

    fun reactToExpense(expense: Expense, emoji: String) {
        viewModelScope.launch {
            val updated = expense.copy(emojiReaction = emoji)
            repository.updateExpenseWithSplits(updated, emptyList()) // update only general field
            _currentGroup.value?.id?.let { refreshGroupData(it) }
        }
    }

    // --- Helper for view creation ---
    class Factory(
        private val application: Application,
        private val repository: StudySplitRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(StudySplitViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return StudySplitViewModel(application, repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
