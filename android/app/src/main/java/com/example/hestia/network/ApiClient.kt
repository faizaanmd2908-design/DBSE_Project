package com.example.hestia.network

import android.content.Context
import com.example.hestia.BuildConfig
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.PATCH
import retrofit2.http.Path
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

// Retrofit contracts mirror the HESTIA API Gateway.

data class ApiResponse<T>(
    val success: Boolean = false,
    val message: String? = null,
    val data: T? = null
)

data class LoginRequest(val email: String, val password: String)
data class RegisterRequest(val name: String, val email: String, val password: String, val phone: String)
data class FavouriteBody(val property_id: Int)
data class InterestBody(val property_id: Int)
data class ActivityBody(val activity_type: String, val property_id: Int? = null, val activity_details: String? = null)

data class LoginData(val token: String, val user: ApiUser)
data class ApiUser(
    val id: Int,
    val name: String,
    val email: String,
    val phone: String? = null,
    val role: String
)

data class ApiImage(val id: Int? = null, val url: String? = null, val isPrimary: Boolean? = null)
data class ApiProperty(
    val id: Int = 0,
    val property_id: Int = 0,
    val title: String = "",
    val description: String? = null,
    val property_type: String = "APARTMENT",
    val location: String = "",
    val city: String = "",
    val state: String? = null,
    val pincode: String? = null,
    val price: Double = 0.0,
    val area_sqft: Double = 0.0,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val status: String = "AVAILABLE",
    val primary_image: String? = null,
    val images: List<ApiImage>? = null
)

data class ApiInterest(
    val id: Int = 0,
    val property_id: Int = 0,
    val property_title: String = "",
    val location: String = "",
    val status: String = "EXPRESSED",
    val created_at: String? = null
)

data class ApiActivity(
    val id: Int = 0,
    val activity_id: Int = 0,
    val property_id: Int? = null,
    val activity_type: String = "",
    val activity_details: String? = null,
    val created_at: String? = null,
    val property_title: String? = null
)

data class PropertyPayload(
    val title: String,
    val description: String,
    val property_type: String,
    val location: String,
    val city: String,
    val state: String = "Telangana",
    val pincode: String = "500000",
    val price: Double,
    val area_sqft: Double,
    val bedrooms: Int? = null,
    val bathrooms: Int? = null,
    val status: String = "AVAILABLE",
    val images: List<String> = emptyList()
)
data class StatusPayload(val status: String)

interface HestiaApi {
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): ApiResponse<LoginData>
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): ApiResponse<Any>
    @GET("auth/me") suspend fun me(): ApiResponse<ApiUser>

    @GET("properties") suspend fun properties(
        @Query("search") search: String? = null,
        @Query("type") type: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("bedrooms") bedrooms: Int? = null,
        @Query("city") city: String? = null,
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 100
    ): ApiResponse<List<ApiProperty>>

    @GET("properties/{id}") suspend fun property(@Path("id") id: Int): ApiResponse<ApiProperty>
    @POST("properties") suspend fun createProperty(@Body body: PropertyPayload): ApiResponse<Any>
    @PUT("properties/{id}") suspend fun updateProperty(@Path("id") id: Int, @Body body: PropertyPayload): ApiResponse<Any>
    @DELETE("properties/{id}") suspend fun deleteProperty(@Path("id") id: Int): ApiResponse<Any>
    @PATCH("properties/{id}/status") suspend fun updateStatus(@Path("id") id: Int, @Body body: StatusPayload): ApiResponse<Any>

    @GET("favourites") suspend fun favourites(): ApiResponse<List<ApiProperty>>
    @POST("favourites") suspend fun addFavourite(@Body body: FavouriteBody): ApiResponse<Any>
    @DELETE("favourites/{propertyId}") suspend fun removeFavourite(@Path("propertyId") propertyId: Int): ApiResponse<Any>

    @GET("interests") suspend fun interests(): ApiResponse<List<ApiInterest>>
    @POST("interests") suspend fun addInterest(@Body body: InterestBody): ApiResponse<Any>
    @PATCH("interests/{id}/status") suspend fun updateInterestStatus(@Path("id") id: Int, @Body body: StatusPayload): ApiResponse<Any>

    @GET("activity") suspend fun activity(): ApiResponse<List<ApiActivity>>
    @POST("activity") suspend fun logActivity(@Body body: ActivityBody): ApiResponse<Any>
}

object ApiProvider {
    private const val PREFS = "hestia_session"
    private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    val api: HestiaApi by lazy {
        val ctx = requireNotNull(appContext) { "ApiProvider.init() must be called first" }
        val tokenInterceptor = Interceptor { chain: Interceptor.Chain ->
            val token = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString("token", null)
            val request = chain.request().newBuilder().apply {
                if (!token.isNullOrBlank()) addHeader("Authorization", "Bearer $token")
                addHeader("Accept", "application/json")
            }.build()
            chain.proceed(request)
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(tokenInterceptor)
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(20, TimeUnit.SECONDS)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(HestiaApi::class.java)
    }
}
