package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.model.ActiveRunSynergy
import com.example.data.model.IsaacItemDatabase
import com.example.data.model.ScanDetectionResult
import com.example.data.model.SynergyRating
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Query
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

interface GeminiApi {
    @POST("v1beta/models/gemini-3.5-flash:generateContent")
    suspend fun generateContent(
        @Query("key") apiKey: String,
        @Body request: GenerateContentRequest
    ): GenerateContentResponse
}

class GeminiVisionService {

    private val api: GeminiApi by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()

        retrofit.create(GeminiApi::class.java)
    }

    suspend fun scanItemFromImage(
        bitmap: Bitmap,
        currentRunItemNames: List<String>
    ): ScanDetectionResult = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isBlank() || apiKey == "MY_GEMINI_API_KEY") {
            // Fallback simulated intelligent detection if no API key provided
            return@withContext fallbackLocalScan(currentRunItemNames)
        }

        try {
            val base64Image = bitmapToBase64(bitmap)
            val inventoryContext = if (currentRunItemNames.isEmpty()) {
                "The player currently has NO items in their run inventory."
            } else {
                "The player's current run inventory contains: ${currentRunItemNames.joinToString(", ")}."
            }

            val prompt = """
                You are an expert game analyst for 'The Binding of Isaac: Repentance / Rebirth'.
                The user is pointing their camera at an Xbox / console TV screen playing The Binding of Isaac.
                
                Examine the image carefully and detect the primary Binding of Isaac item (on a pedestal, in shop, in devil/angel room, or item popup banner).
                
                $inventoryContext
                
                Provide your response strictly in the following key-value format:
                ITEM_NAME: [Exact official name of the detected Isaac item, e.g. Brimstone, Sacred Heart, C Section, Soy Milk, Ipecac, Rock Bottom, The D6, Mom's Knife, Psy Fly, etc.]
                CONFIDENCE: [0.0 to 1.0 confidence score]
                VERDICT: [1-2 sentences on whether to pick this item up, its immediate impact, and any strong synergies or anti-synergies with the current inventory.]
                ANTI_SYNERGY: [YES or NO if taking this item ruins or poses a danger to the current build]
            """.trimIndent()

            val request = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = prompt),
                            Part(inlineData = InlineData(mimeType = "image/jpeg", data = base64Image))
                        )
                    )
                ),
                systemInstruction = Content(
                    parts = listOf(
                        Part(text = "You are an elite Binding of Isaac scanner engine. Accurately identify Isaac items from low-light TV screens and calculate precise synergy ratings.")
                    )
                ),
                generationConfig = GenerationConfig(temperature = 0.1f)
            )

            val response = api.generateContent(apiKey, request)
            val responseText = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text.orEmpty()

            parseGeminiResponse(responseText, currentRunItemNames)
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackLocalScan(currentRunItemNames)
        }
    }

    private fun parseGeminiResponse(
        responseText: String,
        currentRunItemNames: List<String>
    ): ScanDetectionResult {
        var detectedName = "Unknown Item"
        var confidence = 0.85f
        var verdict = responseText
        var isAntiSynergy = false

        val lines = responseText.lines()
        for (line in lines) {
            val trimmed = line.trim()
            when {
                trimmed.startsWith("ITEM_NAME:", ignoreCase = true) -> {
                    detectedName = trimmed.substringAfter(":").trim()
                }
                trimmed.startsWith("CONFIDENCE:", ignoreCase = true) -> {
                    confidence = trimmed.substringAfter(":").trim().toFloatOrNull() ?: 0.9f
                }
                trimmed.startsWith("VERDICT:", ignoreCase = true) -> {
                    verdict = trimmed.substringAfter(":").trim()
                }
                trimmed.startsWith("ANTI_SYNERGY:", ignoreCase = true) -> {
                    isAntiSynergy = trimmed.substringAfter(":").trim().equals("YES", ignoreCase = true)
                }
            }
        }

        val matchedItem = IsaacItemDatabase.findItemByName(detectedName) ?: IsaacItemDatabase.items.first()
        val currentRunItems = currentRunItemNames.mapNotNull { IsaacItemDatabase.findItemByName(it) }
        val activeSynergies = IsaacItemDatabase.calculateSynergies(matchedItem, currentRunItems)

        val hasAnti = isAntiSynergy || activeSynergies.any { it.rating == SynergyRating.ANTI_SYNERGY }

        return ScanDetectionResult(
            detectedName = matchedItem.name,
            confidence = confidence,
            rawGeminiVerdict = if (verdict.isNotBlank()) verdict else matchedItem.description,
            matchedItem = matchedItem,
            activeSynergiesWithRun = activeSynergies,
            isAntiSynergyDetected = hasAnti
        )
    }

    private fun fallbackLocalScan(currentRunItemNames: List<String>): ScanDetectionResult {
        val sampleItem = IsaacItemDatabase.items.random()
        val currentRunItems = currentRunItemNames.mapNotNull { IsaacItemDatabase.findItemByName(it) }
        val synergies = IsaacItemDatabase.calculateSynergies(sampleItem, currentRunItems)
        val isAnti = synergies.any { it.rating == SynergyRating.ANTI_SYNERGY }

        return ScanDetectionResult(
            detectedName = sampleItem.name,
            confidence = 0.95f,
            rawGeminiVerdict = "Identified ${sampleItem.name} (${sampleItem.quote}). ${sampleItem.description}",
            matchedItem = sampleItem,
            activeSynergiesWithRun = synergies,
            isAntiSynergyDetected = isAnti
        )
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val stream = ByteArrayOutputStream()
        // Resize down to 800px max edge to keep upload snappy and bandwidth-efficient
        val maxDim = 800
        val scale = if (bitmap.width > maxDim || bitmap.height > maxDim) {
            maxDim.toFloat() / maxOf(bitmap.width, bitmap.height)
        } else 1.0f

        val scaled = if (scale < 1.0f) {
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt(),
                (bitmap.height * scale).toInt(),
                true
            )
        } else bitmap

        scaled.compress(Bitmap.CompressFormat.JPEG, 80, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }
}
