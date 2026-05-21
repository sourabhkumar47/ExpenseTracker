package com.example.data.repository

import com.example.data.local.FinanceDao
import com.example.data.model.AssetAccount
import com.example.data.model.Budget
import com.example.data.model.FrequentTemplate
import com.example.data.model.Transaction
import com.example.data.model.AppPreference
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class FinanceRepository(private val dao: FinanceDao) {

    // Streams for reactive UI updates
    val allAssetAccounts: Flow<List<AssetAccount>> = dao.getAllAssetAccountsFlow()
    val allTransactions: Flow<List<Transaction>> = dao.getAllTransactionsFlow()
    val allBudgets: Flow<List<Budget>> = dao.getAllBudgetsFlow()
    val allTemplates: Flow<List<FrequentTemplate>> = dao.getAllTemplatesFlow()
    val preferences: Flow<AppPreference?> = dao.getPreferencesFlow()

    // --- ASSETS MANAGEMENT ---

    suspend fun saveAssetAccount(account: AssetAccount) {
        dao.insertAssetAccount(account)
    }

    suspend fun deleteAssetAccount(accountId: String) {
        dao.deleteAssetAccountById(accountId)
        dao.deleteTransactionsByAccountId(accountId)
    }

    // --- TRANSACTION & BOOKKEEPING DOUBLE-ENTRY SYSTEM ---

    suspend fun addTransaction(tx: Transaction) {
        dao.insertTransaction(tx)
        applyDoubleEntryBalanceChange(tx)
    }

    suspend fun deleteTransaction(tx: Transaction) {
        dao.deleteTransactionById(tx.id)
        // Reverse bookkeeping effect!
        val reversedTx = tx.copy(
            amount = -tx.amount // Reverse the direction
        )
        // To reverse a transfer, we swap source and dest
        if (tx.type == "TRANSFER" && tx.destAccountId != null) {
            val swappedReversed = tx.copy(
                accountId = tx.destAccountId,
                destAccountId = tx.accountId,
                amount = tx.amount // Moving back original positive amount
            )
            applyDoubleEntryBalanceChange(swappedReversed)
        } else {
            applyDoubleEntryBalanceChange(reversedTx)
        }
    }

    private suspend fun applyDoubleEntryBalanceChange(tx: Transaction) {
        // Adjust source account
        val source = dao.getAssetAccountById(tx.accountId)
        if (source != null) {
            val isCreditCard = source.type.equals("Credit Card", ignoreCase = true)
            val updatedBalance = when (tx.type) {
                "EXPENSE" -> {
                    if (isCreditCard) source.balance + tx.amount // Expenses increase credit card liabilities
                    else source.balance - tx.amount // Expenses decrease assets
                }
                "INCOME" -> {
                    if (isCreditCard) source.balance - tx.amount // Income reduces credit card balance
                    else source.balance + tx.amount // Income increases assets
                }
                "TRANSFER" -> {
                    if (isCreditCard) source.balance + tx.amount // Transferring out of credit card increases liability
                    else source.balance - tx.amount // Normal transfer reduces source balance
                }
                else -> source.balance
            }
            dao.insertAssetAccount(source.copy(balance = updatedBalance))
        }

        // Adjust destination account if TRANSFER type
        if (tx.type == "TRANSFER" && tx.destAccountId != null) {
            val dest = dao.getAssetAccountById(tx.destAccountId)
            if (dest != null) {
                val isDestCredit = dest.type.equals("Credit Card", ignoreCase = true)
                val updatedDestBalance = if (isDestCredit) {
                    dest.balance - tx.amount // Transferring into credit card reduces liability (repayment)
                } else {
                    dest.balance + tx.amount // Normal asset increases
                }
                dao.insertAssetAccount(dest.copy(balance = updatedDestBalance))
            }
        }
    }

    // --- SYSTEM RECURRENCE ENGINE ---

    suspend fun checkAndApplyRecurrence() {
        val allTx = dao.getAllTransactions()
        val recurringTxs = allTx.filter { it.isRecurring && it.recurrenceInterval != "None" }
        if (recurringTxs.isEmpty()) return

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val today = Date()
        val todayStr = sdf.format(today)

        for (tx in recurringTxs) {
            // Check if we need to insert a duplicate recurring tx based on interval
            val lastDateStr = tx.date
            val lastDate = sdf.parse(lastDateStr) ?: continue
            val cal = Calendar.getInstance()
            cal.time = lastDate

            // Check if current calendar progress mandates another transaction
            var shouldAdd = false
            when (tx.recurrenceInterval) {
                "Daily" -> {
                    cal.add(Calendar.DAY_OF_YEAR, 1)
                    if (cal.time.before(today) || sdf.format(cal.time) == todayStr) shouldAdd = true
                }
                "Weekly" -> {
                    cal.add(Calendar.WEEK_OF_YEAR, 1)
                    if (cal.time.before(today) || sdf.format(cal.time) == todayStr) shouldAdd = true
                }
                "Monthly" -> {
                    cal.add(Calendar.MONTH, 1)
                    if (cal.time.before(today) || sdf.format(cal.time) == todayStr) shouldAdd = true
                }
            }

            if (shouldAdd) {
                // Add new transaction log for today
                val newTx = tx.copy(
                    id = "recurring_gen_" + UUID.randomUUID().toString().take(10),
                    date = todayStr,
                    isRecurring = true // maintains recurrence stream for future
                )
                
                // Set the old transaction to non-recurring so we don't double count it next time
                dao.insertTransaction(tx.copy(isRecurring = false))
                // Write the new transaction
                addTransaction(newTx)
            }
        }
    }

    // --- FREQUENT BOOKMARKS/TEMPLATES ---

    suspend fun saveTemplate(template: FrequentTemplate) {
        dao.insertTemplate(template)
    }

    suspend fun deleteTemplate(id: String) {
        dao.deleteTemplateById(id)
    }

    // --- CATEGORICAL BUDGET RULES ---

    suspend fun saveBudget(budget: Budget) {
        dao.insertBudget(budget)
    }

    suspend fun removeBudget(category: String) {
        dao.deleteBudgetByCategory(category)
    }

    // --- SYSTEM PREFERENCES ---

    suspend fun savePreferences(pref: AppPreference) {
        dao.updatePreferences(pref)
    }

    suspend fun getPreferences(): AppPreference {
        return dao.getPreferences() ?: AppPreference().also { dao.updatePreferences(it) }
    }

    // --- BACKUP & RESTORE SERIALIZATION ---

    suspend fun exportBackupString(): String {
        val accounts = dao.getAllAssetAccounts()
        val transactions = dao.getAllTransactions()
        val budgets = dao.getAllBudgets()
        val prefs = dao.getPreferences() ?: AppPreference()

        val builder = StringBuilder()
        builder.append("### MYMONEY BACKUP FILE v2 ###\n")
        builder.append("PREFS|${prefs.passcode}|${prefs.currencySymbol}|${prefs.startOfMonthDay}|${prefs.subCategoriesEnabled}\n")
        
        accounts.forEach { acc ->
            builder.append("ACCOUNT|${acc.id}|${acc.name}|${acc.type}|${acc.balance}|${acc.billingDate}|${acc.colorHex}\n")
        }
        
        budgets.forEach { b ->
            builder.append("BUDGET|${b.category}|${b.limitAmount}\n")
        }

        transactions.forEach { tx ->
            builder.append("TX|${tx.id}|${tx.accountId}|${tx.destAccountId ?: ""}|${tx.type}|${tx.amount}|${tx.date}|${tx.name}|${tx.category}|${tx.subCategory}|${tx.notes}|${tx.isRecurring}|${tx.recurrenceInterval}\n")
        }

        return builder.toString()
    }

    suspend fun restoreBackupString(backup: String): Boolean {
        if (!backup.startsWith("### MYMONEY BACKUP FILE")) return false
        try {
            val lines = backup.split("\n")
            val accountsToInsert = mutableListOf<AssetAccount>()
            val txsToInsert = mutableListOf<Transaction>()
            val budgetsToInsert = mutableListOf<Budget>()
            var prefToInsert: AppPreference? = null

            for (line in lines) {
                if (line.isBlank() || line.startsWith("###")) continue
                val parts = line.split("|")
                when (parts.firstOrNull()) {
                    "PREFS" -> {
                        prefToInsert = AppPreference(
                            id = 1,
                            passcode = parts.getOrNull(1) ?: "",
                            currencySymbol = parts.getOrNull(2) ?: "$",
                            startOfMonthDay = parts.getOrNull(3)?.toIntOrNull() ?: 1,
                            subCategoriesEnabled = parts.getOrNull(4)?.toBoolean() ?: false
                        )
                    }
                    "ACCOUNT" -> {
                        accountsToInsert.add(
                            AssetAccount(
                                id = parts[1],
                                name = parts[2],
                                type = parts[3],
                                balance = parts[4].toDoubleOrNull() ?: 0.0,
                                billingDate = parts.getOrNull(5)?.toIntOrNull() ?: 15,
                                colorHex = parts.getOrNull(6) ?: "#4A2E80"
                            )
                        )
                    }
                    "BUDGET" -> {
                        budgetsToInsert.add(
                            Budget(
                                category = parts[1],
                                limitAmount = parts[2].toDoubleOrNull() ?: 0.0
                            )
                        )
                    }
                    "TX" -> {
                        txsToInsert.add(
                            Transaction(
                                id = parts[1],
                                accountId = parts[2],
                                destAccountId = parts.getOrNull(3).takeIf { !it.isNullOrBlank() },
                                type = parts[4],
                                amount = parts[5].toDoubleOrNull() ?: 0.0,
                                date = parts[6],
                                name = parts[7],
                                category = parts[8],
                                subCategory = parts.getOrNull(9) ?: "General",
                                notes = parts.getOrNull(10) ?: "",
                                isRecurring = parts.getOrNull(11)?.toBoolean() ?: false,
                                recurrenceInterval = parts.getOrNull(12) ?: "None"
                            )
                        )
                    }
                }
            }

            // Clear old local state entirely before inserting restoration
            dao.clearAllAssetAccounts()
            dao.clearAllTransactions()
            
            prefToInsert?.let { dao.updatePreferences(it) }
            if (accountsToInsert.isNotEmpty()) dao.insertAssetAccounts(accountsToInsert)
            if (txsToInsert.isNotEmpty()) dao.insertTransactions(txsToInsert)
            for (b in budgetsToInsert) {
                dao.insertBudget(b)
            }

            return true
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }
    }

    suspend fun resetAllAppState() {
        dao.clearAllAssetAccounts()
        dao.clearAllTransactions()
        dao.updatePreferences(AppPreference())
    }
}
