package com.example.data.local

import androidx.room.*
import com.example.data.model.AssetAccount
import com.example.data.model.Budget
import com.example.data.model.FrequentTemplate
import com.example.data.model.Transaction
import com.example.data.model.AppPreference
import kotlinx.coroutines.flow.Flow

@Dao
interface FinanceDao {
    // Asset Accounts
    @Query("SELECT * FROM asset_accounts ORDER BY name ASC")
    fun getAllAssetAccountsFlow(): Flow<List<AssetAccount>>

    @Query("SELECT * FROM asset_accounts ORDER BY name ASC")
    suspend fun getAllAssetAccounts(): List<AssetAccount>

    @Query("SELECT * FROM asset_accounts WHERE id = :id")
    suspend fun getAssetAccountById(id: String): AssetAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetAccount(account: AssetAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAssetAccounts(accounts: List<AssetAccount>)

    @Query("DELETE FROM asset_accounts WHERE id = :id")
    suspend fun deleteAssetAccountById(id: String)

    @Query("DELETE FROM asset_accounts")
    suspend fun clearAllAssetAccounts()

    // Transactions
    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    fun getAllTransactionsFlow(): Flow<List<Transaction>>

    @Query("SELECT * FROM transactions ORDER BY date DESC, id DESC")
    suspend fun getAllTransactions(): List<Transaction>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: Transaction)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<Transaction>)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: String)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()

    @Query("DELETE FROM transactions WHERE accountId = :accountId OR destAccountId = :accountId")
    suspend fun deleteTransactionsByAccountId(accountId: String)

    // Budgets
    @Query("SELECT * FROM budgets ORDER BY category ASC")
    fun getAllBudgetsFlow(): Flow<List<Budget>>

    @Query("SELECT * FROM budgets ORDER BY category ASC")
    suspend fun getAllBudgets(): List<Budget>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget)

    @Query("DELETE FROM budgets WHERE category = :category")
    suspend fun deleteBudgetByCategory(category: String)

    // Frequent Templates (Bookmarks)
    @Query("SELECT * FROM frequent_templates ORDER BY templateTitle ASC")
    fun getAllTemplatesFlow(): Flow<List<FrequentTemplate>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: FrequentTemplate)

    @Query("DELETE FROM frequent_templates WHERE id = :id")
    suspend fun deleteTemplateById(id: String)

    // App Preferences
    @Query("SELECT * FROM app_preferences WHERE id = 1")
    fun getPreferencesFlow(): Flow<AppPreference?>

    @Query("SELECT * FROM app_preferences WHERE id = 1")
    suspend fun getPreferences(): AppPreference?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun updatePreferences(pref: AppPreference)
}
