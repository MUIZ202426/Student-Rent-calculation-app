package com.example.data.repository

import com.example.data.local.StudySplitDao
import com.example.data.model.Expense
import com.example.data.model.ExpenseSplit
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.Profile
import com.example.data.model.Settlement
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlin.math.abs
import kotlin.math.min

class StudySplitRepository(private val dao: StudySplitDao) {

    // --- Profiles ---
    val allProfiles: Flow<List<Profile>> = dao.getAllProfiles()

    suspend fun getProfileById(id: Long): Profile? = dao.getProfileById(id)

    suspend fun createProfile(name: String, avatarUrl: String, hostel: String = ""): Long {
        return dao.insertProfile(Profile(name = name, avatarUrl = avatarUrl, universityHostel = hostel))
    }

    // --- Groups ---
    val allGroups: Flow<List<Group>> = dao.getAllGroups()

    suspend fun getGroupById(id: Long): Group? = dao.getGroupById(id)

    suspend fun createGroup(name: String, creatorProfileId: Long): Long {
        // Generate a simple 6-character uppercase invite code
        val allowedChars = ('A'..'Z') + ('0'..'9')
        val inviteCode = (1..6)
            .map { allowedChars.random() }
            .joinToString("")

        val group = Group(name = name, inviteCode = inviteCode, createdBy = creatorProfileId)
        val groupId = dao.insertGroup(group)

        // Automatically join the creator
        dao.insertGroupMember(GroupMember(groupId = groupId, profileId = creatorProfileId, role = "Admin"))
        return groupId
    }

    suspend fun joinGroupByInviteCode(inviteCode: String, profileId: Long): Group? {
        val group = dao.getGroupByInviteCode(inviteCode.uppercase()) ?: return null
        // Check if already a member
        val existing = dao.getGroupMember(group.id, profileId)
        if (existing == null) {
            dao.insertGroupMember(GroupMember(groupId = group.id, profileId = profileId))
        }
        return group
    }

    fun getGroupProfiles(groupId: Long): Flow<List<Profile>> = dao.getProfilesByGroupId(groupId)

    // --- Expenses ---
    fun getGroupExpenses(groupId: Long): Flow<List<Expense>> = dao.getExpensesByGroupId(groupId)

    fun getSplitsForExpense(expenseId: Long): Flow<List<ExpenseSplit>> = dao.getSplitsByExpenseId(expenseId)

    suspend fun getExpenseById(id: Long): Expense? = dao.getExpenseById(id)

    suspend fun addExpenseWithSplits(expense: Expense, splits: List<ExpenseSplit>): Long {
        val expenseId = dao.insertExpense(expense)
        val splitsWithId = splits.map { it.copy(expenseId = expenseId) }
        dao.insertExpenseSplits(splitsWithId)
        return expenseId
    }

    suspend fun updateExpenseWithSplits(expense: Expense, splits: List<ExpenseSplit>) {
        dao.updateExpense(expense)
        dao.deleteSplitsByExpenseId(expense.id)
        val splitsWithId = splits.map { it.copy(expenseId = expense.id) }
        dao.insertExpenseSplits(splitsWithId)
    }

    suspend fun deleteExpense(expense: Expense) {
        dao.deleteSplitsByExpenseId(expense.id)
        dao.deleteExpense(expense)
    }

    // --- Settlements ---
    fun getGroupSettlements(groupId: Long): Flow<List<Settlement>> = dao.getSettlementsByGroupId(groupId)

    suspend fun addSettlement(settlement: Settlement): Long {
        return dao.insertSettlement(settlement)
    }

    suspend fun deleteSettlement(settlement: Settlement) {
        dao.deleteSettlement(settlement)
    }

    // --- Balances and Optimization ---

    suspend fun calculateBalances(groupId: Long): List<MemberBalance> {
        val profiles = dao.getProfilesByGroupId(groupId).first()
        val expenses = dao.getExpensesByGroupId(groupId).first()
        val settlements = dao.getSettlementsByGroupId(groupId).first()

        val totalPaidMap = mutableMapOf<Long, Double>()
        val totalOwedMap = mutableMapOf<Long, Double>()

        // Initialize maps
        profiles.forEach {
            totalPaidMap[it.id] = 0.0
            totalOwedMap[it.id] = 0.0
        }

        // Expenses paid details
        expenses.forEach { expense ->
            val paidBy = expense.paidBy
            totalPaidMap[paidBy] = (totalPaidMap[paidBy] ?: 0.0) + expense.amount

            val splits = dao.getSplitsByExpenseIdSync(expense.id)
            splits.forEach { split ->
                totalOwedMap[split.profileId] = (totalOwedMap[split.profileId] ?: 0.0) + split.amountOwed
            }
        }

        // Settlements adjusted
        settlements.forEach { settlement ->
            val payer = settlement.paidBy
            val receiver = settlement.paidTo
            // Payer paid money, so they reduce their debt / increase their balance
            totalPaidMap[payer] = (totalPaidMap[payer] ?: 0.0) + settlement.amount
            // Receiver got money, so they increase their "owed" or reduce their credit
            totalOwedMap[receiver] = (totalOwedMap[receiver] ?: 0.0) + settlement.amount
        }

        return profiles.map { profile ->
            val paid = totalPaidMap[profile.id] ?: 0.0
            val owed = totalOwedMap[profile.id] ?: 0.0
            MemberBalance(
                profile = profile,
                netBalance = paid - owed,
                totalPaid = paid,
                totalOwed = owed
            )
        }
    }

    suspend fun calculateOptimizedSettlements(groupId: Long): List<SettlementSuggestion> {
        val balances = calculateBalances(groupId)
        val suggestions = mutableListOf<SettlementSuggestion>()

        // Create lists of debtors and creditors
        val debtors = balances.filter { it.netBalance < -0.01 }
            .map { it.profile to abs(it.netBalance) }
            .toMutableList()

        val creditors = balances.filter { it.netBalance > 0.01 }
            .map { it.profile to it.netBalance }
            .toMutableList()

        var dIndex = 0
        var cIndex = 0

        while (dIndex < debtors.size && cIndex < creditors.size) {
            val debtor = debtors[dIndex]
            val creditor = creditors[cIndex]

            val dId = debtor.first
            val dAmount = debtor.second

            val cId = creditor.first
            val cAmount = creditor.second

            val settleAmount = min(dAmount, cAmount)
            if (settleAmount > 0.01) {
                suggestions.add(
                    SettlementSuggestion(
                        fromProfile = dId,
                        toProfile = cId,
                        amount = settleAmount
                    )
                )
            }

            // Update remaining balances
            debtors[dIndex] = dId to (dAmount - settleAmount)
            creditors[cIndex] = cId to (cAmount - settleAmount)

            if (debtors[dIndex].second <= 0.01) {
                dIndex++
            }
            if (creditors[cIndex].second <= 0.01) {
                cIndex++
            }
        }

        return suggestions
    }
}

data class MemberBalance(
    val profile: Profile,
    val netBalance: Double,
    val totalPaid: Double,
    val totalOwed: Double
)

data class SettlementSuggestion(
    val fromProfile: Profile,
    val toProfile: Profile,
    val amount: Double
)

