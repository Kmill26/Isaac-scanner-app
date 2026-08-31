package com.example.data.catalog

import com.example.data.model.IsaacItem

/** Result of resolving a noisy OCR / free-text name against the catalog. */
data class NameMatch(
    val item: IsaacItem,
    val score: Float,
    val exact: Boolean
)

/**
 * Accuracy-focused name resolution for OCR / Gemini output over the full catalog.
 *
 * Ranking, best first:
 *  1. exact normalized match (score 1.0)
 *  2. exact alias match (score 0.98)
 *  3. high token-containment when multi-token name is matched with noise (e.g. "SACRED HEART L")
 *  4. fuzzy string similarity (Levenshtein / Jaccard) >= [THRESHOLD]
 *
 * Returns `null` when nothing clears the bar — callers must never fall back to an arbitrary item.
 */
object NameMatcher {

    const val THRESHOLD = 0.82f

    private val ALIASES: Map<String, String> = mapOf(
        "brim" to "brimstone",
        "sacred" to "sacred heart",
        "csection" to "c section",
        "c-section" to "c section",
        "c section" to "c section",
        "techx" to "tech x",
        "tech x" to "tech x",
        "godhead" to "godhead",
        "spindown" to "spindown dice",
        "spin down" to "spindown dice",
        "moms knife" to "mom's knife",
        "mom knife" to "mom's knife",
        "psyfly" to "psy fly",
        "mega mush" to "mega mushroom",
        "magic mush" to "magic mushroom",
        "poly" to "polyphemus",
        "soy" to "soy milk",
        "soymilk" to "soy milk",
        "d6" to "the d6",
        "the d6" to "the d6",
        "the d six" to "the d6",
        "d-6" to "the d6",
        "d100" to "d100",
        "d20" to "d20",
        "d4" to "d4",
        "d8" to "d8",
        "d7" to "d7",
        "d10" to "d10",
        "d12" to "d12",
        "dinfinity" to "d-infinity",
        "d-infinity" to "d-infinity",
        "d infinity" to "d-infinity",
        "mantle" to "holy mantle",
        "jacob" to "jacob's ladder",
        "pyro" to "pyromaniac",
        "20 20" to "20/20",
        "2020" to "20/20",
        "20/20" to "20/20",
        "1-up" to "1up!",
        "1up" to "1up!",
        "1 up" to "1up!",
        "1up!" to "1up!",
        "less than three" to "<3",
        "<3" to "<3",
        "cat o nine tails" to "cat-o-nine-tails",
        "cat-o-nine-tails" to "cat-o-nine-tails",
        "cat o' nine tails" to "cat-o-nine-tails",
        "forget me not" to "forget me now",
        "forget me now" to "forget me now",
        "spider mod" to "spidermod",
        "spidermod" to "spidermod"
    )

    fun normalize(raw: String): String {
        var s = raw.lowercase().trim()
        if (s.startsWith("the ")) s = s.substring(4)
        s = buildString(s.length) {
            for (ch in s) append(if (ch.isLetterOrDigit() || ch == ' ') ch else ' ')
        }
        return s.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun match(query: String, items: List<IsaacItem>): NameMatch? {
        val q = normalize(query)
        if (q.isEmpty() || items.isEmpty()) return null

        // Precompute normalized names once per call.
        val normalized = items.map { it to normalize(it.name) }

        // 1. exact normalized match
        normalized.firstOrNull { it.second == q }?.let {
            return NameMatch(it.first, 1f, exact = true)
        }

        // 2. exact alias lookup
        val rawAlias = ALIASES[query.trim().lowercase()] ?: ALIASES[q]
        if (rawAlias != null) {
            val normalizedAlias = normalize(rawAlias)
            normalized.firstOrNull { it.second == normalizedAlias || it.first.name.equals(rawAlias, ignoreCase = true) }?.let {
                return NameMatch(it.first, 0.98f, exact = false)
            }
        }

        val qTokens = q.split(' ').filter { it.isNotEmpty() }.toSet()
        if (qTokens.isEmpty()) return null

        var best: NameMatch? = null
        for ((item, name) in normalized) {
            if (name.isEmpty()) continue
            val nameTokens = name.split(' ').filter { it.isNotEmpty() }.toSet()
            if (nameTokens.isEmpty()) continue

            val score: Float = when {
                // Multi-token containment with noise:
                // e.g. query "SACRED HEART L" contains all name tokens "sacred", "heart"
                nameTokens.size >= 2 && qTokens.containsAll(nameTokens) -> {
                    val overlapRatio = nameTokens.size.toFloat() / qTokens.size.toFloat()
                    0.86f + 0.10f * overlapRatio
                }
                // query tokens contain all name tokens when both are multi-token
                nameTokens.size >= 2 && qTokens.size >= 2 && nameTokens.containsAll(qTokens) -> {
                    val overlapRatio = qTokens.size.toFloat() / nameTokens.size.toFloat()
                    0.86f + 0.10f * overlapRatio
                }
                // Fuzzy string similarity
                else -> {
                    if (q.length < 4 || name.length < 4) {
                        0f
                    } else {
                        maxOf(tokenSetRatio(qTokens, nameTokens), levenshteinRatio(q, name))
                    }
                }
            }

            if (score >= THRESHOLD && (best == null || score > best!!.score)) {
                best = NameMatch(item, score, exact = false)
            }
        }
        return best
    }

    /** Jaccard-style token overlap. */
    private fun tokenSetRatio(a: Set<String>, b: Set<String>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val inter = a.count { it in b }
        val union = (a + b).size
        return inter.toFloat() / union.toFloat()
    }

    private fun levenshteinRatio(a: String, b: String): Float {
        if (a.isEmpty() && b.isEmpty()) return 1f
        val dist = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length)
        return 1f - dist.toFloat() / maxLen.toFloat()
    }

    private fun levenshtein(a: String, b: String): Int {
        val m = a.length
        val n = b.length
        if (m == 0) return n
        if (n == 0) return m
        var prev = IntArray(n + 1) { it }
        var curr = IntArray(n + 1)
        for (i in 1..m) {
            curr[0] = i
            for (j in 1..n) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    curr[j - 1] + 1,
                    prev[j] + 1,
                    prev[j - 1] + cost
                )
            }
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[n]
    }
}
