package com.example.macrotrack

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Data classes matching Open Food Facts JSON structure
data class OffSearchResponse(
    val products: List<OffProduct>?
)

data class OffProduct(
    val product_name: String?,
    val brands: String?,
    val serving_size: String?,
    val nutriments: OffNutriments?
)

data class OffNutriments(
    val energy_kcal_100g: Float?,
    val proteins_100g: Float?,
    val carbohydrates_100g: Float?,
    val fat_100g: Float?,
    val sugars_100g: Float?
)

interface OpenFoodApiService {
    @GET("cgi/search.pl?search_simple=1&action=process&json=1")
    suspend fun searchProducts(@Query("search_terms") query: String): OffSearchResponse

    @GET("cgi/search.pl?action=process&json=1")
    suspend fun searchByBrand(@Query("brands") brand: String): OffSearchResponse

    companion object {
        private const val BASE_URL = "https://world.openfoodfacts.org/"

        val instance: OpenFoodApiService by lazy {
            Retrofit.Builder()
                .baseUrl(BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(OpenFoodApiService::class.java)
        }
    }
}