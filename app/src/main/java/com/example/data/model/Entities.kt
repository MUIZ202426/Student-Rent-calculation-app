package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "profiles")
data class Profile(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarUrl: String = "", // Can be emoji or drawable string
    val universityHostel: String = "",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "groups")
data class Group(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val inviteCode: String,
    val createdBy: Long, // profileId
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "group_members")
data class GroupMember(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val profileId: Long,
    val role: String = "Member", // "Admin" or "Member"
    val joinedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val paidBy: Long, // profileId
    val amount: Double,
    val title: String,
    val category: String, // e.g. "Food", "Utility", "Internet", "Cleaning", "Rent", "Groceries", "Others"
    val date: Long = System.currentTimeMillis(),
    val notes: String = "",
    val receiptUrl: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val emojiReaction: String = "" // "❤️", "😂", "👍"
)

@Entity(tableName = "expense_splits")
data class ExpenseSplit(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val expenseId: Long,
    val profileId: Long, // user_id who owes
    val amountOwed: Double
)

@Entity(tableName = "settlements")
data class Settlement(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val groupId: Long,
    val paidBy: Long, // profileId who paid
    val paidTo: Long, // profileId who received
    val amount: Double,
    val date: Long = System.currentTimeMillis()
)
