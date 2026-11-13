package com.tms.os.launcher.domain

import com.tms.os.launcher.model.AppInfo
import com.tms.os.launcher.model.AppShortcutItem
import com.tms.os.launcher.model.ContactItem
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
            return allApps
                .filter { !it.isHidden }
                .sortedWith(
                    compareByDescending<AppInfo> { it.effectivePriorityScore }
                        .thenByDescending { it.lastLaunchedTime }
                        .thenBy { it.displayLabel.lowercase(Locale.ROOT) }
                )
                .take(8)
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
                    .thenByDescending { it.app.effectivePriorityScore }
                    .thenBy { it.app.displayLabel.lowercase(Locale.ROOT) }
            )
            .map { it.app }
    }

    fun generateQuickShortcuts(query: String, topApps: List<AppInfo>): List<AppShortcutItem> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.length < 2) return emptyList()

        val shortcuts = mutableListOf<AppShortcutItem>()
        for (app in topApps.take(4)) {
            val pkg = app.packageName.lowercase(Locale.ROOT)
            when {
                pkg.contains("whatsapp") -> {
                    shortcuts.add(AppShortcutItem(app.packageName, "new_chat", "Neuer WhatsApp-Chat", app.displayLabel))
                }
                pkg.contains("camera") || app.displayLabel.lowercase(Locale.ROOT).contains("kamera") -> {
                    shortcuts.add(AppShortcutItem(app.packageName, "selfie", "Selfie / Foto aufnehmen", app.displayLabel))
                }
                pkg.contains("chrome") || pkg.contains("sbrowser") || pkg.contains("browser") -> {
                    shortcuts.add(AppShortcutItem(app.packageName, "new_tab", "Neuer Browser-Tab", app.displayLabel))
                }
                pkg.contains("maps") -> {
                    shortcuts.add(AppShortcutItem(app.packageName, "nav_home", "Route & Navigation starten", app.displayLabel))
                }
                pkg.contains("gm") || pkg.contains("mail") || pkg.contains("outlook") -> {
                    shortcuts.add(AppShortcutItem(app.packageName, "compose", "Neue E-Mail verfassen", app.displayLabel))
                }
                pkg.contains("calendar") || app.displayLabel.lowercase(Locale.ROOT).contains("kalender") -> {
                    shortcuts.add(AppShortcutItem(app.packageName, "new_event", "Neues Ereignis eintragen", app.displayLabel))
                }
            }
        }
        return shortcuts.take(3)
    }

    fun searchContacts(query: String, allContacts: List<ContactItem>): List<ContactItem> {
        val q = query.trim().lowercase(Locale.ROOT)
        if (q.length < 2) return emptyList()
        return allContacts
            .filter {
                it.name.lowercase(Locale.ROOT).contains(q) ||
                    it.phoneNumber.contains(q)
            }
            .take(3)
    }

    private fun scoreApp(query: String, app: AppInfo): Int {
        val labelNorm = normalize(app.displayLabel)
        val pkgNorm = app.packageName.lowercase(Locale.ROOT)
        val catNorm = normalize(app.category.titleDe)
        val usageBoost = min(app.effectivePriorityScore / 5, 100)

        if (labelNorm == query) return 1000 + usageBoost
        if (labelNorm.startsWith(query)) {
            return 850 + (50 - min(labelNorm.length, 50)) + usageBoost
        }

        val acronym = buildAcronym(app.displayLabel)
        if (acronym.startsWith(query)) {
            return 780 + usageBoost
        }

        val words = labelNorm.split(" ", "-", "_", ".")
        if (words.any { it.startsWith(query) }) {
            return 720 + usageBoost
        }

        val indexInLabel = labelNorm.indexOf(query)
        if (indexInLabel >= 0) {
            return 600 - (indexInLabel * 5) + (usageBoost / 2)
        }

        if (query.length >= 2 && isSubsequence(query, labelNorm)) {
            return 450 + (usageBoost / 2)
        }

        if (query.length >= 3) {
            for (word in words) {
                if (word.isEmpty()) continue
                val prefixTarget = word.take(max(query.length, min(word.length, query.length + 1)))
                val dist = levenshtein(query, prefixTarget)
                val maxAllowed = if (query.length >= 5) 2 else 1
                if (dist <= maxAllowed) {
                    return 380 - (dist * 80) + (usageBoost / 3)
                }
            }
        }

        if (catNorm.contains(query)) return 220 + (usageBoost / 3)
        if (pkgNorm.contains(query)) return 180

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
            if (sub[sIdx] == target[tIdx]) sIdx++
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
     * Evaluates arithmetic expressions, percentages ("19% von 250"), and currency/unit conversions
     * ("500 aed in eur", "100 usd in eur", "250 eur in aed", "100 km in mi", "80 kg in lb")
     */
    fun evaluateMathOrConversion(query: String): String? {
        val qLower = query.trim().lowercase(Locale.ROOT).replace(",", ".")
        if (qLower.isEmpty()) return null

        // 1. Check Currency & Unit Conversion Patterns
        val convRegex = Regex("^(-?[0-9]+(?:\\.[0-9]+)?)\\s*([a-z€$£°]+)(?:\\s+(?:in|to|nach|=)\\s+([a-z€$£°]+))?$")
        val convMatch = convRegex.find(qLower)
        if (convMatch != null) {
            val amount = convMatch.groupValues[1].toDoubleOrNull()
            val fromUnit = convMatch.groupValues[2]
            val toUnit = convMatch.groupValues[3]
            if (amount != null) {
                val converted = tryConvertCurrencyOrUnit(amount, fromUnit, toUnit)
                if (converted != null) return converted
            }
        }

        // 2. Check Percentage Pattern ("19% von 250" or "19% of 250")
        val pctRegex = Regex("^([0-9]+(?:\\.[0-9]+)?)\\s*%\\s*(?:von|of)\\s*([0-9]+(?:\\.[0-9]+)?)$")
        val pctMatch = pctRegex.find(qLower)
        if (pctMatch != null) {
            val p = pctMatch.groupValues[1].toDoubleOrNull()
            val base = pctMatch.groupValues[2].toDoubleOrNull()
            if (p != null && base != null) {
                val res = (p / 100.0) * base
                return formatNumber(res)
            }
        }

        // 3. Arithmetic
        return SpotlightCalculator.evaluate(query)?.display
    }

    private fun tryConvertCurrencyOrUnit(amount: Double, from: String, toRaw: String): String? {
        // Rates relative to 1 EUR
        val ratesPerEur = mapOf(
            "eur" to 1.0,
            "€" to 1.0,
            "usd" to 1.08,
            "$" to 1.08,
            "aed" to 3.97,
            "dirham" to 3.97,
            "chf" to 0.94,
            "gbp" to 0.84,
            "£" to 0.84,
            "try" to 37.5,
            "tl" to 37.5
        )

        if (ratesPerEur.containsKey(from)) {
            val target = if (toRaw.isNotEmpty()) toRaw else if (from == "eur" || from == "€") "aed" else "eur"
            val fromRate = ratesPerEur[from] ?: return null
            val toRate = ratesPerEur[target] ?: return null
            val inEur = amount / fromRate
            val out = inEur * toRate
            val symbol = target.uppercase(Locale.ROOT)
            return "≈ ${String.format(Locale.GERMANY, "%.2f", out)} $symbol"
        }

        // Distance & Weight units
        return when {
            from in setOf("c", "°c") && (toRaw in setOf("f", "°f")) -> "${formatNumber(amount * 9 / 5 + 32)} °F"
            from in setOf("f", "°f") && (toRaw in setOf("c", "°c")) -> "${formatNumber((amount - 32) * 5 / 9)} °C"
            from == "cm" && (toRaw.isEmpty() || toRaw in setOf("in", "inch", "zoll")) -> "${formatNumber(amount / 2.54)} in"
            from in setOf("inch", "zoll") && (toRaw.isEmpty() || toRaw == "cm") -> "${formatNumber(amount * 2.54)} cm"
            from == "m" && (toRaw == "ft") -> "${formatNumber(amount * 3.28084)} ft"
            from == "ft" && (toRaw.isEmpty() || toRaw == "m") -> "${formatNumber(amount * 0.3048)} m"
            from == "g" && (toRaw == "oz") -> "${formatNumber(amount * 0.035274)} oz"
            from == "oz" && (toRaw.isEmpty() || toRaw == "g") -> "${formatNumber(amount * 28.3495)} g"
            from == "l" && (toRaw == "gal") -> "${formatNumber(amount * 0.264172)} gal"
            from == "gal" && (toRaw.isEmpty() || toRaw == "l") -> "${formatNumber(amount * 3.78541)} l"
            from == "km" && (toRaw.isEmpty() || toRaw == "mi") -> "${formatNumber(amount * 0.621371)} mi"
            from == "mi" && (toRaw.isEmpty() || toRaw == "km") -> "${formatNumber(amount * 1.60934)} km"
            from == "kg" && (toRaw.isEmpty() || toRaw == "lb" || toRaw == "lbs") -> "${formatNumber(amount * 2.20462)} lb"
            (from == "lb" || from == "lbs") && (toRaw.isEmpty() || toRaw == "kg") -> "${formatNumber(amount * 0.453592)} kg"
            else -> null
        }
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString()
        else String.format(Locale.GERMANY, "%.4f", value).trimEnd('0').trimEnd(',')
    }

}
