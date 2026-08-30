package com.example.data.gemini

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// ----------------------------------------------------------------------------------------------
// Request
// ----------------------------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class GenerateContentRequest(
    @Json(name = "contents") val contents: List<Content>,
    @Json(name = "generationConfig") val generationConfig: GenerationConfig? = null,
    @Json(name = "systemInstruction") val systemInstruction: Content? = null
)

@JsonClass(generateAdapter = true)
data class Content(
    @Json(name = "parts") val parts: List<Part>,
    @Json(name = "role") val role: String? = null
)

@JsonClass(generateAdapter = true)
data class Part(
    @Json(name = "text") val text: String? = null,
    @Json(name = "inlineData") val inlineData: InlineData? = null
)

@JsonClass(generateAdapter = true)
data class InlineData(
    @Json(name = "mimeType") val mimeType: String,
    @Json(name = "data") val data: String
)

@JsonClass(generateAdapter = true)
data class GenerationConfig(
    @Json(name = "temperature") val temperature: Float? = null,
    @Json(name = "responseMimeType") val responseMimeType: String? = null,
    @Json(name = "responseSchema") val responseSchema: Schema? = null,
    @Json(name = "thinkingConfig") val thinkingConfig: ThinkingConfig? = null
)

@JsonClass(generateAdapter = true)
data class ThinkingConfig(
    @Json(name = "thinkingBudget") val thinkingBudget: Int = 0
)

/** Minimal subset of the OpenAPI-ish schema shape the Gemini API accepts for structured output. */
@JsonClass(generateAdapter = true)
data class Schema(
    @Json(name = "type") val type: String,
    @Json(name = "properties") val properties: Map<String, Schema>? = null,
    @Json(name = "required") val required: List<String>? = null,
    @Json(name = "items") val items: Schema? = null
)

// ----------------------------------------------------------------------------------------------
// Response
// ----------------------------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class GenerateContentResponse(
    @Json(name = "candidates") val candidates: List<Candidate>? = null,
    @Json(name = "promptFeedback") val promptFeedback: PromptFeedback? = null,
    @Json(name = "error") val error: ApiError? = null
)

@JsonClass(generateAdapter = true)
data class Candidate(
    @Json(name = "content") val content: Content? = null,
    @Json(name = "finishReason") val finishReason: String? = null
)

@JsonClass(generateAdapter = true)
data class PromptFeedback(
    @Json(name = "blockReason") val blockReason: String? = null
)

@JsonClass(generateAdapter = true)
data class ApiError(
    @Json(name = "code") val code: Int? = null,
    @Json(name = "message") val message: String? = null,
    @Json(name = "status") val status: String? = null,
    @Json(name = "details") val details: List<ErrorDetail>? = null
)

@JsonClass(generateAdapter = true)
data class ErrorDetail(
    @Json(name = "@type") val type: String? = null,
    /** e.g. "5s" — present on google.rpc.RetryInfo details for 429/503. */
    @Json(name = "retryDelay") val retryDelay: String? = null
)

// ----------------------------------------------------------------------------------------------
// Structured identification payload (parsed out of the model's JSON text part)
// ----------------------------------------------------------------------------------------------

@JsonClass(generateAdapter = true)
data class ScanPayload(
    @Json(name = "itemName") val itemName: String = "",
    @Json(name = "itemDetected") val itemDetected: Boolean = false,
    @Json(name = "confidence") val confidence: Double = 0.0,
    @Json(name = "verdict") val verdict: String = "",
    @Json(name = "antiSynergy") val antiSynergy: Boolean = false
)

/** What [GeminiVisionService.identify] hands back to [com.example.data.scan.ScanEngine]. */
data class GeminiIdentification(
    val itemName: String,
    val confidence: Float,
    val verdict: String,
    val antiSynergy: Boolean
)
