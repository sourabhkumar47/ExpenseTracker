package com.example.data.api

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Standard Plaid request / response objects

@JsonClass(generateAdapter = true)
data class PlaidUser(
    @Json(name = "client_user_id") val clientUserId: String
)

@JsonClass(generateAdapter = true)
data class LinkTokenCreateRequest(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "client_name") val clientName: String = "Budget Tracker",
    @Json(name = "user") val user: PlaidUser,
    @Json(name = "products") val products: List<String> = listOf("transactions"),
    @Json(name = "country_codes") val countryCodes: List<String> = listOf("US"),
    @Json(name = "language") val language: String = "en"
)

@JsonClass(generateAdapter = true)
data class LinkTokenCreateResponse(
    @Json(name = "link_token") val linkToken: String,
    @Json(name = "expiration") val expiration: String?,
    @Json(name = "request_id") val requestId: String?
)

@JsonClass(generateAdapter = true)
data class PublicTokenExchangeRequest(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "public_token") val publicToken: String
)

@JsonClass(generateAdapter = true)
data class PublicTokenExchangeResponse(
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "item_id") val itemId: String,
    @Json(name = "request_id") val requestId: String?
)

@JsonClass(generateAdapter = true)
data class BalanceGetRequest(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "access_token") val accessToken: String
)

@JsonClass(generateAdapter = true)
data class AccountBalances(
    @Json(name = "available") val available: Double?,
    @Json(name = "current") val current: Double,
    @Json(name = "iso_currency_code") val isoCurrencyCode: String?
)

@JsonClass(generateAdapter = true)
data class PlaidAccount(
    @Json(name = "account_id") val accountId: String,
    @Json(name = "name") val name: String,
    @Json(name = "mask") val mask: String?,
    @Json(name = "type") val type: String,
    @Json(name = "subtype") val subtype: String?,
    @Json(name = "balances") val balances: AccountBalances
)

@JsonClass(generateAdapter = true)
data class BalanceGetResponse(
    @Json(name = "accounts") val accounts: List<PlaidAccount>,
    @Json(name = "request_id") val requestId: String?
)

@JsonClass(generateAdapter = true)
data class TransactionsGetRequest(
    @Json(name = "client_id") val clientId: String,
    @Json(name = "secret") val secret: String,
    @Json(name = "access_token") val accessToken: String,
    @Json(name = "start_date") val startDate: String, // YYYY-MM-DD
    @Json(name = "end_date") val endDate: String, // YYYY-MM-DD
    @Json(name = "options") val options: TransactionsGetOptions? = null
)

@JsonClass(generateAdapter = true)
data class TransactionsGetOptions(
    @Json(name = "count") val count: Int = 100,
    @Json(name = "offset") val offset: Int = 0
)

@JsonClass(generateAdapter = true)
data class PlaidTransaction(
    @Json(name = "transaction_id") val transactionId: String,
    @Json(name = "account_id") val accountId: String,
    @Json(name = "amount") val amount: Double, // positive is debit (expense), negative is credit (income)
    @Json(name = "date") val date: String,
    @Json(name = "name") val name: String,
    @Json(name = "category") val category: List<String>?,
    @Json(name = "pending") val pending: Boolean
)

@JsonClass(generateAdapter = true)
data class TransactionsGetResponse(
    @Json(name = "accounts") val accounts: List<PlaidAccount>,
    @Json(name = "transactions") val transactions: List<PlaidTransaction>,
    @Json(name = "total_transactions") val totalTransactions: Int?,
    @Json(name = "request_id") val requestId: String?
)
