package com.tms.os.launcher.domain

import com.tms.os.launcher.model.AppInfo
import java.text.Normalizer
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

object SpotlightSearchEngine {

    data class SearchResult(
        val app: AppInfo,
        val score: Int
    )

    fun searchApps(query: String, allApps: List<AppInfo>): List<AppInfo> {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            // Return top used / suggested apps when query is empty
            return allApps
                .filter { !it.isHidden }
                .sortedWith(
                    compareByDescending<AppInfo> { it.launchCount }
                        .thenByDescending { it.lastLaunchedTime }
                        .thenBy { it.displayLabel.lowercase(Locale.ROOT) }
                )
                .take(12)
        }

        val normalizedQuery = normalize(trimmed)
        val results = mutableListOf<SearchResult>()

        for (app in allApps) {
            if (app.isHidden) continue
            val score = scoreApp(normalizedQuery, app)
            if (score > 0) {
                results.add(SearchResult(app, score))
            }
        }

        return results
            .sortedWith(
                compareByDescending<SearchResult> { it.score }
                    .thenByDescending { it.app.launchCount }
                    .thenBy { it.app.displayLabel.lowercase(Locale.ROOT) }
            )
            .map { it.app }
    }

    private fun scoreApp(query: String, app: AppInfo): Int {
        val labelNorm = normalize(app.displayLabel)
        val pkgNorm = app.packageName.lowercase(Locale.ROOT)
        val catNorm = normalize(app.category.titleDe)

        // 1. Exact full match
        if (labelNorm == query) return 1000 + min(app.launchCount * 5, 100)

        // 2. Exact prefix match ("wha" -> "whatsapp")
        if (labelNorm.startsWith(query)) {
            return 850 + (50 - min(labelNorm.length, 50)) + min(app.launchCount * 5, 100)
        }

        // 3. Word boundary / CamelCase / Initials Acronym match ("wa" -> "WhatsApp", "db" -> "DB Navigator")
        val acronym = buildAcronym(app.displayLabel)
        if (acronym.startsWith(query)) {
            return 780 + min(app.launchCount * 5, 100)
        }

        // 4. Word start match ("nav" -> "db navigator")
        val words = labelNorm.split(" ", "-", "_", ".")
        if (words.any { it.startsWith(query) }) {
            return 720 + min(app.launchCount * 5, 100)
        }

        // 5. Substring match in label
        val indexInLabel = labelNorm.indexOf(query)
        if (indexInLabel >= 0) {
            return 600 - (indexInLabel * 5) + min(app.launchCount * 3, 60)
        }

        // 6. Subsequence fuzzy match (e.g. "whtsp" -> "whatsapp")
        if (query.length >= 2 && isSubsequence(query, labelNorm)) {
            return 450 + min(app.launchCount * 3, 50)
        }

        // 7. Typo tolerance (Levenshtein distance <= 1 or 2 for longer queries)
        if (query.length >= 3) {
            for (word in words) {
                if (word.isEmpty()) continue
                val prefixTarget = word.take(max(query.length, min(word.length, query.length + 1)))
                val dist = levenshtein(query, prefixTarget)
                val maxAllowed = if (query.length >= 5) 2 else 1
                if (dist <= maxAllowed) {
                    return 380 - (dist * 80) + min(app.launchCount * 2, 40)
                }
            }
        }

        // 8. Category or package match
        if (catNorm.contains(query)) {
            return 220 + min(app.launchCount * 2, 40)
        }
        if (pkgNorm.contains(query)) {
            return 180
        }

        return 0
    }

    private fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input.lowercase(Locale.ROOT), Normalizer.Form.NFD)
        return decomposed.replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
    }

    private fun buildAcronym(label: String): String {
        val sb = StringBuilder()
        var prevWasSep = true
        for (i in label.indices) {
            val c = label[i]
            if (!c.isLetterOrDigit()) {
                prevWasSep = true
            } else {
                val isUpperTransition = i > 0 && c.isUpperCase() && label[i - 1].isLowerCase()
                if (prevWasSep || isUpperTransition) {
                    sb.append(c.lowercaseChar())
                }
                prevWasSep = false
            }
        }
        return sb.toString()
    }

    private fun isSubsequence(sub: String, target: String): Boolean {
        var sIdx = 0
        var tIdx = 0
        while (sIdx < sub.length && tIdx < target.length) {
            if (sub[sIdx] == target[tIdx]) {
                sIdx++
            }
            tIdx++
        }
        return sIdx == sub.length
    }

    private fun levenshtein(a: String, b: String): Int {
        val dp = IntArray(b.length + 1) { it }
        for (i in 1..a.length) {
            var prev = dp[0]
            dp[0] = i
            for (j in 1..b.length) {
                val temp = dp[j]
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + cost)
                prev = temp
            }
        }
        return dp[b.length]
    }

    /**
     * Evaluates simple arithmetic expressions directly in Spotlight (e.g. "45*12", "120/4 + 15", "19% von 250")
     */
    fun evaluateMathOrConversion(query: String): String? {
        val cleaned = query
            .replace(",", ".")
            .replace("×", "*")
            .replace("÷", "/")
            .trim()

        // Only evaluate if it looks like a math expression with at least one operator
        if (!cleaned.matches(Regex("^[0-9.+\\-*/()%\\s]+$"))) return null
        if (!cleaned.any { it in charArrayOf('+', '-', '*', '/', '%') }) return null
        if (cleaned.all { it.isDigit() || it == '.' || it == '-' }) return null

        return try {
            val result = SimpleMathParser(cleaned).parse()
            if (result.isNaN() || result.isInfinite()) null
            else if (result % 1.0 == 0.0) result.toLong().toString()
            else String.format(Locale.GERMANY, "%.4f", result).trimEnd('0').trimEnd(',')
        } catch (_: Exception) {
            null
        }
    }

    private class SimpleMathParser(private val str: String) {
        private var pos = -1
        private var ch = 0

        private fun nextChar() {
            ch = if (++pos < str.length) str[pos].code else -1
        }

        private fun eat(charToEat: Int): Boolean {
            while (ch == ' '.code) nextChar()
            if (ch == charToEat) {
                nextChar()
                return true
            }
            return false
        }

        fun parse(): Double {
            nextChar()
            val x = parseExpression()
            if (pos < str.length) throw RuntimeException("Unexpected")
            return x
        }

        private fun parseExpression(): Double {
            var x = parseTerm()
            while (true) {
                when {
                    eat('+'.code) -> x += parseTerm()
                    eat('-'.code) -> x -= parseTerm()
                    else -> return x
                }
            }
        }

        private fun parseTerm(): Double {
            var x = parseFactor()
            while (true) {
                when {
                    eat('*'.code) -> x *= parseFactor()
                    eat('/'.code) -> x /= parseFactor()
                    eat('%'.code) -> x %= parseFactor()
                    else -> return x
                }
            }
        }

        private fun parseFactor(): Double {
            if (eat('+'.code)) return parseFactor()
            if (eat('-'.code)) return -parseFactor()
            var x: Double
            val startPos = pos
            if (eat('('.code)) {
                x = parseExpression()
                eat(')'.code)
            } else if ((ch in '0'.code..'9'.code) || ch == '.'.code) {
                while ((ch in '0'.code..'9'.code) || ch == '.'.code) nextChar()
                x = str.substring(startPos, pos).toDouble()
            } else {
                throw RuntimeException("Unexpected")
            }
            return x
        }
    }
}
