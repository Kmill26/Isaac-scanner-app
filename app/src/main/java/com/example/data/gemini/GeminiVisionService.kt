package com.example.data.gemini

import android.graphics.Bitmap
import android.util.Base64
import com.example.BuildConfig
import com.example.data.scan.ScanException
import com.squareup.moshi.Moshi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit

interface GeminiApi {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Body request: GenerateContentRequest
    ): Response<GenerateContentResponse>
}

/**
 * Optional AI vision layer. Only ever invoked by [com.example.data.scan.ScanEngine] when the
 * offline OCR path found no usable text AND [isConfigured] is true.
 *
 * Two entry points:
 *  - [identify] — sprite-only identification when OCR failed.
 *  - [verdictFor] — an on-demand, run-aware "should I take this?" note after a successful match.
 *
 * Every failure surfaces as a typed [ScanException]. There is no local fallback and no
 * "pick a random item" coalesce — a bad response throws.
 */
class GeminiVisionService {

    private val moshi: Moshi by lazy { Moshi.Builder().build() }
    private val payloadAdapter by lazy { moshi.adapter(ScanPayload::class.java) }
    private val errorAdapter by lazy { moshi.adapter(GenerateContentResponse::class.java) }

    private val rawKey: String get() = BuildConfig.GEMINI_API_KEY

    /** True only when a real key is present — not blank, not the placeholders, not "NONE". */
    fun isConfigured(): Boolean {
        val k = rawKey.trim()
        return k.isNotEmpty() &&
            !k.equals("NONE", ignoreCase = true) &&
            k != "MY_GEMINI_API_KEY" &&
            k != "YOUR_API_KEY"
    }

    private val api: GeminiApi by lazy {
        val builder = OkHttpClient.Builder()
            .callTimeout(25, TimeUnit.SECONDS)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val req = chain.request().newBuilder()
                    .addHeader("x-goog-api-key", rawKey.trim())
                    .build()
                chain.proceed(req)
            }

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                    redactHeader("x-goog-api-key")
                }
            )
        }

        Retrofit.Builder()
            .baseUrl("https://generativelanguage.googleapis.com/")
            .client(builder.build())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(GeminiApi::class.java)
    }

    // ------------------------------------------------------------------------------------------
    // Entry points
    // ------------------------------------------------------------------------------------------

    /** Identify the pedestal item from the sprite alone. Throws [ScanException] on any failure. */
    suspend fun identify(bitmap: Bitmap): GeminiIdentification = withContext(Dispatchers.IO) {
        if (!isConfigured()) throw ScanException.NoApiKey

        val prompt = """
            You are an expert on The Binding of Isaac: Repentance.
            This photo is of a TV/console screen. Identify the single most prominent Isaac
            collectible shown (on a pedestal, shop slot, devil/angel reward, or item pickup banner).
            Respond with JSON only, matching the provided schema.
            - itemName: the exact official English item name (e.g. "Brimstone", "Sacred Heart", "The D6").
            - itemDetected: false if you cannot see an Isaac item at all.
            - confidence: 0..1, your certainty in itemName.
            - verdict: one or two sentences on the item's immediate impact.
            - antiSynergy: true only if this item is broadly build-ruining on its own.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(
                Content(
                    parts = listOf(
                        Part(text = prompt),
                        Part(inlineData = InlineData(mimeType = "image/jpeg", data = bitmapToBase64(bitmap)))
                    )
                )
            ),
            generationConfig = GenerationConfig(
                temperature = 0.1f,
                responseMimeType = "application/json",
                responseSchema = SCAN_SCHEMA,
                thinkingConfig = ThinkingConfig(thinkingBudget = 0)
            )
        )

        val text = executeForText(request)
        val payload = payloadAdapter.fromJson(extractJson(text))
            ?: throw ScanException.BadResponse("Could not parse identification JSON.")

        if (!payload.itemDetected || payload.confidence < MIN_CONFIDENCE) {
            throw ScanException.NoItemDetected
        }

        GeminiIdentification(
            itemName = payload.itemName.trim(),
            confidence = payload.confidence.toFloat().coerceIn(0f, 1f),
            verdict = payload.verdict.trim(),
            antiSynergy = payload.antiSynergy
        )
    }

    /**
     * A run-aware verdict for an item the OCR path already matched. Text only, no image.
     * Throws [ScanException] on failure so the caller can keep showing the offline result.
     */
    suspend fun verdictFor(itemName: String, runItems: List<String>): String = withContext(Dispatchers.IO) {
        if (!isConfigured()) throw ScanException.NoApiKey

        val inventory = if (runItems.isEmpty()) {
            "The player has no items yet this run."
        } else {
            "The player's current run items: ${runItems.joinToString(", ")}."
        }
        val prompt = """
            The Binding of Isaac: Repentance. The player is deciding whether to take "$itemName".
            $inventory
            In two or three sentences: should they take it? Mention the biggest synergy or
            anti-synergy with what they already have. Plain text only.
        """.trimIndent()

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt)))),
            generationConfig = GenerationConfig(
                temperature = 0.3f,
                thinkingConfig = ThinkingConfig(thinkingBudget = 0)
            )
        )

        executeForText(request).trim().ifEmpty {
            throw ScanException.BadResponse("Empty verdict text.")
        }
    }

    // ------------------------------------------------------------------------------------------
    // HTTP
    // ------------------------------------------------------------------------------------------

    /** Runs the call (one retry on 429/503), validates the envelope, returns concatenated text parts. */
    private suspend fun executeForText(request: GenerateContentRequest): String {
        var attempt = 0
        while (true) {
            val response: Response<GenerateContentResponse> = try {
                api.generateContent(MODEL, request)
            } catch (e: IOException) {
                throw ScanException.Network(e)
            }

            if (response.isSuccessful) {
                return validateAndExtract(response.body())
            }

            val code = response.code()
            val errBody = runCatching { response.errorBody()?.string() }.getOrNull()
            val apiError = parseError(errBody)

            if ((code == 429 || code == 503) && attempt == 0) {
                attempt++
                delay(retryDelayMs(apiError))
                continue
            }

            throw when (code) {
                429 -> ScanException.RateLimited(retrySeconds(apiError))
                in 500..599 -> ScanException.ServerError(code)
                else -> ScanException.BadResponse(
                    "HTTP $code: ${apiError?.message ?: errBody?.take(180) ?: "no body"}"
                )
            }
        }
    }

    private fun validateAndExtract(body: GenerateContentResponse?): String {
        if (body == null) throw ScanException.BadResponse("Empty response body.")
        body.error?.let { throw ScanException.BadResponse("API error: ${it.message ?: it.status ?: it.code}") }
        body.promptFeedback?.blockReason?.let { throw ScanException.BadResponse("Prompt blocked: $it") }

        val candidate = body.candidates?.firstOrNull()
            ?: throw ScanException.BadResponse("No candidates in response.")
        val finish = candidate.finishReason
        if (finish != null && !finish.equals("STOP", ignoreCase = true)) {
            throw ScanException.BadResponse("Generation did not finish cleanly: $finish")
        }

        val text = candidate.content?.parts.orEmpty()
            .mapNotNull { it.text }
            .joinToString("")
            .trim()
        if (text.isEmpty()) throw ScanException.BadResponse("No text parts in response.")
        return text
    }

    private fun parseError(body: String?): ApiError? {
        if (body.isNullOrBlank()) return null
        return runCatching { errorAdapter.fromJson(body)?.error }.getOrNull()
    }

    private fun retrySeconds(error: ApiError?): Int? {
        val delayStr = error?.details
            ?.firstOrNull { it.retryDelay != null }
            ?.retryDelay ?: return null
        // "5s" / "5.0s" -> 5, rounded up
        val secs = delayStr.removeSuffix("s").toDoubleOrNull() ?: return null
        return kotlin.math.ceil(secs).toInt().coerceIn(0, RETRY_CAP_SEC)
    }

    private fun retryDelayMs(error: ApiError?): Long {
        val secs = retrySeconds(error) ?: DEFAULT_RETRY_SEC
        return secs.coerceIn(1, RETRY_CAP_SEC) * 1000L
    }

    /** Pulls the first {...} block out of a possibly-fenced text blob. */
    private fun extractJson(text: String): String {
        val trimmed = text.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        return if (start >= 0 && end > start) trimmed.substring(start, end + 1) else trimmed
    }

    private fun bitmapToBase64(bitmap: Bitmap): String {
        val maxDim = 1024
        val longest = maxOf(bitmap.width, bitmap.height)
        val scaled = if (longest > maxDim) {
            val scale = maxDim.toFloat() / longest
            Bitmap.createScaledBitmap(
                bitmap,
                (bitmap.width * scale).toInt().coerceAtLeast(1),
                (bitmap.height * scale).toInt().coerceAtLeast(1),
                true
            )
        } else {
            bitmap
        }
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 90, stream)
        return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    }

    companion object {
        private const val MODEL = "gemini-2.5-flash"
        private const val MIN_CONFIDENCE = 0.35
        private const val DEFAULT_RETRY_SEC = 2
        private const val RETRY_CAP_SEC = 4

        private val SCAN_SCHEMA = Schema(
            type = "object",
            properties = mapOf(
                "itemName" to Schema(type = "string"),
                "itemDetected" to Schema(type = "boolean"),
                "confidence" to Schema(type = "number"),
                "verdict" to Schema(type = "string"),
                "antiSynergy" to Schema(type = "boolean")
            ),
            required = listOf("itemName", "itemDetected", "confidence", "verdict", "antiSynergy")
        )
    }
}
