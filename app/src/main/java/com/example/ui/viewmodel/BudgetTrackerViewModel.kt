package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.AssetAccount
import com.example.data.model.Budget
import com.example.data.model.FrequentTemplate
import com.example.data.model.Transaction
import com.example.data.model.AppPreference
import com.example.data.repository.FinanceRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class BudgetTrackerViewModel(
    application: Application,
    private val repository: FinanceRepository
) : AndroidViewModel(application) {

    // Main local data streams
    val accounts: StateFlow<List<AssetAccount>> = repository.allAssetAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val transactions: StateFlow<List<Transaction>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<Budget>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val templates: StateFlow<List<FrequentTemplate>> = repository.allTemplates
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val preferences: StateFlow<AppPreference> = repository.preferences
        .map { it ?: AppPreference() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppPreference())

    // UI/Recurrence states
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        viewModelScope.launch {
            // First time run, fill with gorgeous demo assets to instantly show double-entry accounting
            repository.allAssetAccounts.first().let { currentList ->
                if (currentList.isEmpty()) {
                    populateInitialDemoData()
                }
            }
            // Execute automatic recurrence checks
            repository.checkAndApplyRecurrence()
        }
    }

    private suspend fun populateInitialDemoData() {
        _isSyncing.value = true
        _syncMessage.value = "Creating default wallets & accounts..."

        // Default Assets
        val wallet = AssetAccount("asset_wallet", "Cash Wallet", "Wallet", 120.0, 1, "#4CAF50")
        val checking = AssetAccount("asset_checking", "Chase Checking", "Checking/Debit", 3450.0, 1, "#1E88E5")
        val credit = AssetAccount("asset_credit", "Premium Credit Card", "Credit Card", 480.0, 25, "#E53935")
        val savings = AssetAccount("asset_savings", "High Yield Savings", "Savings", 12000.0, 1, "#8E24AA")

        repository.saveAssetAccount(wallet)
        repository.saveAssetAccount(checking)
        repository.saveAssetAccount(credit)
        repository.saveAssetAccount(savings)

        // Default budgets
        repository.saveBudget(Budget("Food & Dining", 400.0))
        repository.saveBudget(Budget("Shopping/Groceries", 300.0))
        repository.saveBudget(Budget("Entertainment", 150.0))
        repository.saveBudget(Budget("Utilities", 250.0))

        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cal = Calendar.getInstance()

        // Dummy transactions
        cal.add(Calendar.DAY_OF_YEAR, -5)
        repository.addTransaction(
            Transaction(
                id = "demo_tx_1",
                accountId = "asset_checking",
                type = "EXPENSE",
                amount = 45.20,
                date = sdf.format(cal.time),
                name = "Target Supercenter",
                category = "Shopping/Groceries",
                notes = "Weekly groceries reset"
            )
        )

        cal.add(Calendar.DAY_OF_YEAR, 2)
        repository.addTransaction(
            Transaction(
                id = "demo_tx_2",
                accountId = "asset_wallet",
                type = "EXPENSE",
                amount = 12.50,
                date = sdf.format(cal.time),
                name = "Starbucks Coffee",
                category = "Food & Dining",
                notes = "Latte & pastry"
            )
        )

        cal.add(Calendar.DAY_OF_YEAR, 1)
        repository.addTransaction(
            Transaction(
                id = "demo_tx_3",
                accountId = "asset_credit",
                type = "EXPENSE",
                amount = 89.00,
                date = sdf.format(cal.time),
                name = "Netflix & Internet Bills",
                category = "Utilities",
                notes = "Auto subscription standard"
            )
        )

        // Monthly salary income deposit (Double-entry: increases asset)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        repository.addTransaction(
            Transaction(
                id = "demo_tx_income",
                accountId = "asset_checking",
                type = "INCOME",
                amount = 2600.0,
                date = sdf.format(cal.time),
                name = "Direct Deposit Paycheck",
                category = "Salary / Income",
                notes = "Bi-weekly paycheck salary"
            )
        )

        // Asset transfer (Double-entry: decreases dynamic savings, increases checking)
        cal.add(Calendar.DAY_OF_YEAR, 1)
        repository.addTransaction(
            Transaction(
                id = "demo_tx_transfer",
                accountId = "asset_savings",
                destAccountId = "asset_checking",
                type = "TRANSFER",
                amount = 500.0,
                date = sdf.format(cal.time),
                name = "Transfer: Emergency Fund",
                category = "Transfer"
            )
        )

        // Frequent template bookmarks for 1-click lightning fast entry
        repository.saveTemplate(
            FrequentTemplate("t_coffee", "☕ Coffee Ritual", "asset_wallet", null, "EXPENSE", 4.50, "Food & Dining", "Local Roasted Coffee Shop")
        )
        repository.saveTemplate(
            FrequentTemplate("t_gas", "⛽ Gas Refill", "asset_credit", null, "EXPENSE", 45.00, "Travel & Transport", "Shell Station")
        )
        repository.saveTemplate(
            FrequentTemplate("t_grocery", "🛒 Groceries Shop", "asset_checking", null, "EXPENSE", 75.00, "Shopping/Groceries", "Whole Foods")
        )

        _syncMessage.value = "Welcome to your MyMoney Finance checkbook!"
        _isSyncing.value = false
    }

    // --- FINANCIAL CALCULATED STATEFLOW METRICS ---

    val netWorth: StateFlow<Double> = accounts.map { list ->
        list.sumOf { acc ->
            if (acc.type.equals("Credit Card", ignoreCase = true)) {
                -acc.balance // Reduce net worth by credit card debt balance
            } else {
                acc.balance // Increase net worth by assets
            }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalAssets: StateFlow<Double> = accounts.map { list ->
        list.filterNot { it.type.equals("Credit Card", ignoreCase = true) }
            .sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val totalLiabilities: StateFlow<Double> = accounts.map { list ->
        list.filter { it.type.equals("Credit Card", ignoreCase = true) }
            .sumOf { it.balance }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Current Monthly Spending vs Income (computed dynamically using user startOfMonthDay preference!)
    val currentMonthSpending: StateFlow<Double> = combine(transactions, preferences) { list, pref ->
        val cutoffCalendar = getStartOfMonthCutoffCalendar(pref.startOfMonthDay)
        list.filter { tx ->
            val txDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(tx.date) ?: Date()
            txDate.after(cutoffCalendar.time) && tx.type == "EXPENSE"
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val currentMonthIncome: StateFlow<Double> = combine(transactions, preferences) { list, pref ->
        val cutoffCalendar = getStartOfMonthCutoffCalendar(pref.startOfMonthDay)
        list.filter { tx ->
            val txDate = SimpleDateFormat("yyyy-MM-dd", Locale.US).parse(tx.date) ?: Date()
            txDate.after(cutoffCalendar.time) && tx.type == "INCOME"
        }.sumOf { it.amount }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    fun getStartOfMonthCutoffCalendar(startDay: Int): Calendar {
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        val currentDay = cal.get(Calendar.DAY_OF_MONTH)
        if (currentDay < startDay) {
            // Cutoff is in previous month
            cal.add(Calendar.MONTH, -1)
        }
        cal.set(Calendar.DAY_OF_MONTH, startDay.coerceIn(1, 28)) // avoid February invalid days
        return cal
    }

    // --- ACTIONS ---

    fun triggerQuickTemplate(template: FrequentTemplate) {
        viewModelScope.launch {
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val tx = Transaction(
                id = "tx_tpl_" + UUID.randomUUID().toString().take(10),
                accountId = template.accountId,
                destAccountId = template.destAccountId,
                type = template.type,
                amount = template.amount,
                date = sdf.format(Date()),
                name = template.name,
                category = template.category,
                notes = "Quick entry from bookmark"
            )
            repository.addTransaction(tx)
            _syncMessage.value = "Created transaction '${template.templateTitle}'!"
        }
    }

    fun saveBookmarkTemplate(title: String, accountId: String, destAccountId: String?, type: String, amount: Double, category: String, name: String) {
        viewModelScope.launch {
            repository.saveTemplate(
                FrequentTemplate(
                    id = "template_" + UUID.randomUUID().toString().take(10),
                    templateTitle = title.trim(),
                    accountId = accountId,
                    destAccountId = destAccountId,
                    type = type,
                    amount = amount,
                    category = category,
                    name = name.trim()
                )
            )
            _syncMessage.value = "Bookmark template saved!"
        }
    }

    fun deleteBookmarkTemplate(id: String) {
        viewModelScope.launch {
            repository.deleteTemplate(id)
            _syncMessage.value = "Bookmark template removed."
        }
    }

    fun addNewAssetAccount(name: String, type: String, balance: Double, billingDate: Int, colorHex: String) {
        viewModelScope.launch {
            val id = "asset_" + UUID.randomUUID().toString().take(10)
            val acc = AssetAccount(
                id = id,
                name = name.trim(),
                type = type,
                balance = balance,
                billingDate = billingDate,
                colorHex = colorHex
            )
            repository.saveAssetAccount(acc)
            _syncMessage.value = "New Asset Account '$name' connected offline!"
        }
    }

    fun editAssetAccountBalance(id: String, newBalance: Double) {
        viewModelScope.launch {
            accounts.value.firstOrNull { it.id == id }?.let { original ->
                repository.saveAssetAccount(original.copy(balance = newBalance))
                _syncMessage.value = "Asset Balance adjusted."
            }
        }
    }

    fun removeAssetAccount(accountId: String) {
        viewModelScope.launch {
            repository.deleteAssetAccount(accountId)
            _syncMessage.value = "Asset Account disconnected and logs purged."
        }
    }

    fun addNewLocalTransaction(
        accountId: String,
        destAccountId: String?,
        type: String,
        amount: Double,
        date: String,
        name: String,
        category: String,
        notes: String,
        isRecurring: Boolean,
        recurrenceInterval: String
    ) {
        viewModelScope.launch {
            val tx = Transaction(
                id = "tx_" + UUID.randomUUID().toString().take(12),
                accountId = accountId,
                destAccountId = destAccountId,
                type = type,
                amount = amount,
                date = date,
                name = name.trim().ifBlank { "Uncategorized spending" },
                category = category,
                notes = notes.trim(),
                isRecurring = isRecurring,
                recurrenceInterval = recurrenceInterval
            )
            repository.addTransaction(tx)
            _syncMessage.value = "Transaction recorded successfully!"
            
            // Re-trigger recurrence engine if registered
            if (isRecurring && recurrenceInterval != "None") {
                repository.checkAndApplyRecurrence()
            }
        }
    }

    fun removeTransaction(tx: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(tx)
            _syncMessage.value = "Transaction entry removed. Balancing adjusted."
        }
    }

    fun saveBudgetLimit(category: String, limit: Double) {
        viewModelScope.launch {
            repository.saveBudget(Budget(category, limit))
            _syncMessage.value = "Budget rule updated."
        }
    }

    fun removeBudgetLimit(category: String) {
        viewModelScope.launch {
            repository.removeBudget(category)
            _syncMessage.value = "Budget rule deleted."
        }
    }

    fun updateSecurityPreferences(passcode: String, currencySymbol: String, startOfMonthDay: Int, subCategoriesEnabled: Boolean) {
        viewModelScope.launch {
            repository.savePreferences(
                AppPreference(
                    id = 1,
                    passcode = passcode.trim(),
                    currencySymbol = currencySymbol.trim(),
                    startOfMonthDay = startOfMonthDay,
                    subCategoriesEnabled = subCategoriesEnabled
                )
            )
            _syncMessage.value = "Accounting variables updated!"
        }
    }

    fun triggerLocalBackup(onGenerated: (String) -> Unit) {
        viewModelScope.launch {
            val backup = repository.exportBackupString()
            onGenerated(backup)
        }
    }

    fun restoreLocalBackup(backupStr: String, onCompleted: (Boolean) -> Unit) {
        viewModelScope.launch {
            _isSyncing.value = true
            val success = repository.restoreBackupString(backupStr)
            _isSyncing.value = false
            if (success) {
                _syncMessage.value = "Database backup restored successfully!"
                onCompleted(true)
            } else {
                _syncMessage.value = "Failed to restore backup. Structural integrity mismatch."
                onCompleted(false)
            }
        }
    }

    fun clearLocalData() {
        viewModelScope.launch {
            repository.resetAllAppState()
            _syncMessage.value = "Local database reset successfully."
            populateInitialDemoData()
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    // Factory template setup
    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val database = AppDatabase.getDatabase(application)
            val repository = FinanceRepository(database.financeDao())
            return BudgetTrackerViewModel(application, repository) as T
        }
    }
}
