package com.bhumivera.scanner.data

import com.bhumivera.scanner.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface BhumiveraService {
    @GET("products/active")
    suspend fun activeProducts(@Query("search") search: String? = null): ApiEnvelope<List<Product>>

    @GET("products/slug/{slug}")
    suspend fun productBySlug(@Path("slug") slug: String): ApiEnvelope<Product>

    @GET("products/{identifier}")
    suspend fun productById(@Path("identifier") identifier: Int): ApiEnvelope<Product>

    @POST("auth/login")
    suspend fun login(@Body request: LoginRequest): LoginResponse

    @POST("auth/google")
    suspend fun googleLogin(@Body request: GoogleLoginRequest): LoginResponse

    @POST("auth/2fa/verify")
    suspend fun verifyTwoFactor(@Body request: TwoFactorRequest): LoginResponse

    @POST("auth/logout")
    suspend fun logout()

    @GET("cart")
    suspend fun cart(): CartResponse

    @POST("cart")
    suspend fun addToCart(@Body request: AddCartRequest): CartResponse

    @PUT("cart/{productId}")
    suspend fun updateCartItem(
        @Path("productId") productId: Int,
        @Body request: UpdateCartRequest,
    ): CartResponse

    @DELETE("cart/{productId}")
    suspend fun removeCartItem(@Path("productId") productId: Int): CartResponse

    // ==========================================
    // 10x ACCELERATED SCANNER & E-BILL POS APIS
    // ==========================================

    @GET("inventory-software/scanner/ping")
    suspend fun scannerPing(@Query("scannerApiKey") apiKey: String? = null): ApiEnvelope<Map<String, Any>>

    @GET("inventory-software/scanner/catalog")
    suspend fun scannerCatalog(@Query("scannerApiKey") apiKey: String? = null): ScannerCatalogResponse

    @POST("inventory-software/scanner/lookup")
    suspend fun scannerLookup(@Body request: ScannerLookupRequest): ScannerLookupResponse

    @POST("inventory-software/scanner/scan")
    suspend fun scannerScan(@Body request: ScannerScanRequest): ScannerScanResponse

    @POST("inventory-software/pos/checkout")
    suspend fun posCheckout(@Body request: PosCheckoutRequest): PosCheckoutResponse

    companion object {
        fun create(sessionStore: SessionStore): BhumiveraService {
            val authInterceptor = Interceptor { chain ->
                val orig = chain.request()
                val builder = orig.newBuilder()
                sessionStore.token()?.let { builder.header("Authorization", "Bearer $it") }
                val scannerKey = sessionStore.scannerApiKey()
                if (scannerKey.isNotBlank()) {
                    builder.header("x-scanner-api-key", scannerKey)
                }
                chain.proceed(builder.build())
            }

            val connectionPool = okhttp3.ConnectionPool(10, 5, TimeUnit.MINUTES)
            val client = OkHttpClient.Builder()
                .connectionPool(connectionPool)
                .addInterceptor(authInterceptor)
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .retryOnConnectionFailure(true)
                .build()

            val base = sessionStore.backendUrl().trimEnd('/') + "/api/"
            return Retrofit.Builder()
                .baseUrl(base)
                .client(client)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(BhumiveraService::class.java)
        }
    }
}
