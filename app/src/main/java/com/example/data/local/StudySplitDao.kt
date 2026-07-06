package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Expense
import com.example.data.model.ExpenseSplit
import com.example.data.model.Group
import com.example.data.model.GroupMember
import com.example.data.model.Profile
import com.example.data.model.Settlement
import kotlinx.coroutines.flow.Flow

@Dao
interface StudySplitDao {

    // --- Profile Queries ---
    @Query("SELECT * FROM profiles ORDER BY name ASC")
    fun getAllProfiles(): Flow<List<Profile>>

    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: Long): Profile?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: Profile): Long

    // --- Group Queries ---
    @Query("SELECT * FROM groups")
    fun getAllGroups(): Flow<List<Group>>

    @Query("SELECT * FROM groups WHERE id = :id")
    suspend fun getGroupById(id: Long): Group?

    @Query("SELECT * FROM groups WHERE inviteCode = :code")
    suspend fun getGroupByInviteCode(code: String): Group?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroup(group: Group): Long

    @Delete
    suspend fun deleteGroup(group: Group)

    // --- GroupMember Queries ---
    @Query("SELECT * FROM group_members WHERE groupId = :groupId")
    fun getMembersByGroupId(groupId: Long): Flow<List<GroupMember>>

    @Query("SELECT * FROM group_members WHERE groupId = :groupId AND profileId = :profileId")
    suspend fun getGroupMember(groupId: Long, profileId: Long): GroupMember?

    @Query("""
        SELECT p.* FROM profiles p 
        INNER JOIN group_members gm ON p.id = gm.profileId 
        WHERE gm.groupId = :groupId
    """)
    fun getProfilesByGroupId(groupId: Long): Flow<List<Profile>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGroupMember(member: GroupMember): Long

    @Query("DELETE FROM group_members WHERE groupId = :groupId AND profileId = :profileId")
    suspend fun removeGroupMember(groupId: Long, profileId: Long)

    // --- Expense Queries ---
    @Query("SELECT * FROM expenses WHERE groupId = :groupId ORDER BY date DESC")
    fun getExpensesByGroupId(groupId: Long): Flow<List<Expense>>

    @Query("SELECT * FROM expenses WHERE id = :id")
    suspend fun getExpenseById(id: Long): Expense?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense): Long

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    // --- ExpenseSplit Queries ---
    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    fun getSplitsByExpenseId(expenseId: Long): Flow<List<ExpenseSplit>>

    @Query("SELECT * FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun getSplitsByExpenseIdSync(expenseId: Long): List<ExpenseSplit>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpenseSplits(splits: List<ExpenseSplit>)

    @Query("DELETE FROM expense_splits WHERE expenseId = :expenseId")
    suspend fun deleteSplitsByExpenseId(expenseId: Long)

    // --- Settlement Queries ---
    @Query("SELECT * FROM settlements WHERE groupId = :groupId ORDER BY date DESC")
    fun getSettlementsByGroupId(groupId: Long): Flow<List<Settlement>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSettlement(settlement: Settlement): Long

    @Delete
    suspend fun deleteSettlement(settlement: Settlement)
}
