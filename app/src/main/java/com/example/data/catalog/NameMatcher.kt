package com.example.data.catalog

import com.example.data.model.IsaacItem

/** Result of resolving a noisy OCR / free-text name against the catalog. */
data class NameMatch(
    val item: IsaacItem,
    val score: Float,
    val exact: Boolean
)

/**
 * Accuracy-focused name resolution for OCR / Gemini output over the full 721-item catalog.
 *
 * Ranking, best first:
 *  1. exact normalized match
 *  2. full token containment (every query token in the name, or vice-versa)
 *  3. token-set / Levenshtein similarity >= [THRESHOLD]
 *  4. short curated alias map
 *
 * Returns `null` when nothing clears the bar — callers must never fall back to `items.first()`.
 */
object NameMatcher {

    const val THRESHOLD = 0.82f

    private val ALIASES: Map<String, String> = mapOf(
        "brim" to "brimstone",
        "sacred" to "sacred heart",
        "csection" to "c section",
        "c-section" to "c section",
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
        "the d six" to "the d6",
        "mantle" to "holy mantle",
        "jacob" to "jacob's ladder",
        "pyro" to "pyromaniac",
        "20 20" to "20/20",
        "2020" to "20/20"
    )

    fun normalize(raw: String): String {
        var s = raw.lowercase().trim()
        // strip a leading article
        if (s.startsWith("the ")) s = s.substring(4)
        // drop punctuation, keep alphanumerics and spaces
        s = buildString(s.length) {
            for (ch in s) append(if (ch.isLetterOrDigit() || ch == ' ') ch else ' ')
        }
        // collapse whitespace
        return s.split(' ').filter { it.isNotEmpty() }.joinToString(" ")
    }

    fun match(query: String, items: List<IsaacItem>): NameMatch? {
        val q = normalize(query)
        if (q.isEmpty() || items.isEmpty()) return null

        // Precompute normalized names once per call.
        val normalized = items.map { it to normalize(it.name) }

        // 1. exact
        normalized.firstOrNull { it.second == q }?.let {
            return NameMatch(it.first, 1f, exact = true)
        }

        val qTokens = q.split(' ').toSet()

        var best: NameMatch? = null
        for ((item, name) in normalized) {
            if (name.isEmpty()) continue
            val nameTokens = name.split(' ').toSet()

            val score: Float = when {
                // 2. full token containment either direction
                nameTokens.containsAll(qTokens) || qTokens.containsAll(nameTokens) -> {
                    val ratio = minOf(qTokens.size, nameTokens.size).toFloat() /
                        maxOf(qTokens.size, nameTokens.size).toFloat()
                    0.90f + 0.09f * ratio
                }
                // 3. fuzzy similarity
                else -> maxOf(tokenSetRatio(qTokens, nameTokens), levenshteinRatio(q, name))
            }

            if (score >= THRESHOLD && (best == null || score > best!!.score)) {
                best = NameMatch(item, score, exact = false)
            }
        }
        if (best != null) return best

        // 4. alias fallback
        val aliasTarget = ALIASES[q] ?: ALIASES.entries.firstOrNull { q.contains(it.key) }?.value
        if (aliasTarget != null) {
            normalized.firstOrNull { it.second == aliasTarget }?.let {
                return NameMatch(it.first, THRESHOLD, exact = false)
            }
        }
        return null
    }

    /** Jaccard-style token overlap, lightly rewarding shared tokens. */
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
