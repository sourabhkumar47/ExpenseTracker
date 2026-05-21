package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "asset_accounts")
data class AssetAccount(
    @PrimaryKey val id: String,
    val name: String,
    val type: String, // "Wallet", "Credit Card", "Checking/Debit", "Savings"
    val balance: Double,
    val billingDate: Int = 15, // Settlement day for credit cards
    val colorHex: String = "#4A2E80"
)

@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey val id: String,
    val accountId: String,
    val destAccountId: String? = null, // In case of TRANSFER type
    val type: String, // "EXPENSE", "INCOME", "TRANSFER"
    val amount: Double,
    val date: String, // YYYY-MM-DD
    val name: String,
    val category: String,
    val subCategory: String = "General",
    val notes: String = "",
    val isRecurring: Boolean = false,
    val recurrenceInterval: String = "None" // "Daily", "Weekly", "Monthly", "None"
)

@Entity(tableName = "budgets")
data class Budget(
    @PrimaryKey val category: String,
    val limitAmount: Double
)

@Entity(tableName = "frequent_templates")
data class FrequentTemplate(
    @PrimaryKey val id: String,
    val templateTitle: String,
    val accountId: String,
    val destAccountId: String? = null,
    val type: String,
    val amount: Double,
    val category: String,
    val name: String
)

@Entity(tableName = "app_preferences")
data class AppPreference(
    @PrimaryKey val id: Int = 1,
    val passcode: String = "", // passcode lock (empty = disabled)
    val currencySymbol: String = "$",
    val startOfMonthDay: Int = 1, // Custom start of month
    val subCategoriesEnabled: Boolean = false,
    val darkModeSetting: Int = 0 // 0 = System, 1 = Light, 2 = Dark
)
