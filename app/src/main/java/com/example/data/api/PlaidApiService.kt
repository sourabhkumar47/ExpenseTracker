package com.example.data.api

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Url

interface PlaidApiService {

    @POST
    suspend fun createLinkToken(
        @Url url: String,
        @Body request: LinkTokenCreateRequest
    ): LinkTokenCreateResponse

    @POST
    suspend fun exchangePublicToken(
        @Url url: String,
        @Body request: PublicTokenExchangeRequest
    ): PublicTokenExchangeResponse

    @POST
    suspend fun getBalances(
        @Url url: String,
        @Body request: BalanceGetRequest
    ): BalanceGetResponse

    @POST
    suspend fun getTransactions(
        @Url url: String,
        @Body request: TransactionsGetRequest
    ): TransactionsGetResponse

    companion object {
        private val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        private val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        private val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            .build()

        fun create(): PlaidApiService {
            return Retrofit.Builder()
                .baseUrl("https://sandbox.plaid.com/") // default backup fallback base url
                .client(okHttpClient)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(PlaidApiService::class.java)
        }
    }
}
