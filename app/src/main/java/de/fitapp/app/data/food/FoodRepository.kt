package de.fitapp.app.data.food

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import de.fitapp.app.data.dao.FoodDao
import de.fitapp.app.data.entity.FoodItemEntity
import de.fitapp.app.data.entity.FoodSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

/** Ergebnis einer Barcode-Abfrage, inklusive Fehlerbehandlung für die UI. */
sealed class BarcodeLookupResult {
    data class Found(val item: FoodItemEntity) : BarcodeLookupResult()
    object NotFound : BarcodeLookupResult()
    object NetworkError : BarcodeLookupResult()
}

private data class LocalFoodJson(
    val name: String,
    val kcal: Float,
    val protein: Float,
    val carbs: Float,
    val sugar: Float,
    val fat: Float,
    val fiber: Float
)

// Minimaler Client für Open Food Facts (kostenlos, keine Anmeldung nötig).
private interface OpenFoodFactsApi {
    @GET("api/v2/product/{barcode}.json")
    suspend fun getProduct(@Path("barcode") barcode: String): OffResponse
}

private data class OffResponse(
    val status: Int,
    val product: OffProduct?
)

private data class OffProduct(
    @SerializedName("product_name") val productName: String?,
    @SerializedName("nutriments") val nutriments: OffNutriments?
)

private data class OffNutriments(
    @SerializedName("energy-kcal_100g") val kcal100g: Float?,
    @SerializedName("proteins_100g") val protein100g: Float?,
    @SerializedName("carbohydrates_100g") val carbs100g: Float?,
    @SerializedName("sugars_100g") val sugar100g: Float?,
    @SerializedName("fat_100g") val fat100g: Float?,
    @SerializedName("fiber_100g") val fiber100g: Float?
)

class FoodRepository(
    private val context: Context,
    private val foodDao: FoodDao
) {
    private val api: OpenFoodFactsApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://world.openfoodfacts.org/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(OpenFoodFactsApi::class.java)
    }

    /** Befüllt die lokale Datenbank beim ersten Start mit den mitgelieferten Grundlebensmitteln. */
    suspend fun ensureSeeded() = withContext(Dispatchers.IO) {
        if (foodDao.count() > 0) return@withContext
        val json = context.assets.open("foods_de.json").bufferedReader().use { it.readText() }
        val items = Gson().fromJson(json, Array<LocalFoodJson>::class.java)
        foodDao.insertAll(items.map {
            FoodItemEntity(
                name = it.name,
                barcode = null,
                source = FoodSource.LOCAL_DB,
                kcalPer100g = it.kcal,
                proteinPer100g = it.protein,
                carbsPer100g = it.carbs,
                sugarPer100g = it.sugar,
                fatPer100g = it.fat,
                fiberPer100g = it.fiber
            )
        })
    }

    suspend fun search(query: String): List<FoodItemEntity> = withContext(Dispatchers.IO) {
        if (query.isBlank()) emptyList() else foodDao.search(query.trim())
    }

    suspend fun saveCustomFood(
        name: String,
        kcal: Float, protein: Float, carbs: Float, sugar: Float, fat: Float, fiber: Float
    ): Long = withContext(Dispatchers.IO) {
        foodDao.insert(
            FoodItemEntity(
                name = name,
                source = FoodSource.CUSTOM,
                kcalPer100g = kcal,
                proteinPer100g = protein,
                carbsPer100g = carbs,
                sugarPer100g = sugar,
                fatPer100g = fat,
                fiberPer100g = fiber,
                isCustom = true
            )
        )
    }

    /**
     * Sucht ein Produkt per Barcode. Prüft zuerst lokal (bereits gescannte Produkte),
     * fragt sonst Open Food Facts an. Netzwerk-/Verbindungsfehler werden abgefangen,
     * damit die UI eine verständliche Fehlermeldung mit manueller Eingabe als
     * Ausweichmöglichkeit anzeigen kann.
     */
    suspend fun lookupBarcode(barcode: String): BarcodeLookupResult = withContext(Dispatchers.IO) {
        foodDao.findByBarcode(barcode)?.let { return@withContext BarcodeLookupResult.Found(it) }

        try {
            val response = api.getProduct(barcode)
            val product = response.product
            if (response.status != 1 || product == null || product.nutriments?.kcal100g == null) {
                return@withContext BarcodeLookupResult.NotFound
            }
            val entity = FoodItemEntity(
                name = product.productName?.takeIf { it.isNotBlank() } ?: "Unbenanntes Produkt",
                barcode = barcode,
                source = FoodSource.BARCODE_ONLINE,
                kcalPer100g = product.nutriments.kcal100g,
                proteinPer100g = product.nutriments.protein100g ?: 0f,
                carbsPer100g = product.nutriments.carbs100g ?: 0f,
                sugarPer100g = product.nutriments.sugar100g ?: 0f,
                fatPer100g = product.nutriments.fat100g ?: 0f,
                fiberPer100g = product.nutriments.fiber100g ?: 0f
            )
            val id = foodDao.insert(entity)
            BarcodeLookupResult.Found(entity.copy(id = id))
        } catch (e: Exception) {
            BarcodeLookupResult.NetworkError
        }
    }
}
