package com.tms.os.launcher.domain

import android.content.pm.ApplicationInfo
import android.os.Build
import com.tms.os.launcher.model.AppCategory
import java.util.Locale

object SmartCategorizer {

    private val socialKeywords = listOf(
        "whatsapp", "telegram", "messenger", "instagram", "facebook", "snapchat",
        "tiktok", "musically", "discord", "signal", "threema", "twitter", "x.android",
        "threads", "reddit", "linkedin", "pinterest", "bereal", "jodel", "tinder",
        "bumble", "hinge", "lovoo", "badoo", "skype", "viber", "wechat", "line",
        "mastodon", "bluesky", "twitch.android.app.chat"
    )

    private val financeKeywords = listOf(
        "sparkasse", "finanzstatus", "dkb", "ing.diba", "comdirect", "commerzbank",
        "deutschebank", "postbank", "n26", "revolut", "traderepublic", "scalable",
        "paypal", "klarna", "curve", "google.android.apps.walletnfcrel", "samsung.android.spay",
        "coinbase", "binance", "kraken", "bitpanda", "bison", "metamask", "trustwallet",
        "vr.banking", "consorsbank", "targobank", "santander", "c24", "bunq", "wise",
        "finanzguru", "outbank", "steuer", "wiso", "elster", "banking", "wallet", "finance",
        "kredit", "aktien", "investing", "tradingview", "yahoo.mobile.client.android.finance"
    )

    private val productivityKeywords = listOf(
        "gmail", "outlook", " email", "mail", "calendar", "kalender", "notion", "obsidian",
        "evernote", "onenote", "keep", "notes", "notizen", "samsung.android.app.notes",
        "slack", "teams", "zoom", "webex", "meet", "trello", "asana", "jira", "todoist",
        "ticktick", "microsoft.office", "word", "excel", "powerpoint", "docs", "sheets",
        "slides", "drive", "onedrive", "dropbox", "nextcloud", "box.android", "adobe.reader",
        "pdf", "scanner", "camscanner", "github", "gitlab", "termux", "1password", "bitwarden",
        "keepass", "authenticator", "authy"
    )

    private val entertainmentKeywords = listOf(
        "youtube", "netflix", "spotify", "primevideo", "amazon.avod", "disney", "dazn",
        "sky", "wowtv", "joyn", "rtlplus", "zdf", "ardmediathek", "arte", "twitch",
        "soundcloud", "deezer", "tidal", "apple.android.music", "shazam", "audible",
        "podcast", "pocketcasts", "vlc", "mxtech", "plex", "kodi", "stremio", "crunchyroll",
        "steam", "playstation", "xbox", "nintendo", "epicgames", "kino", "eventim"
    )

    private val creativityKeywords = listOf(
        "camera", "kamera", "gallery", "galerie", "photos", "lightroom", "photoshop",
        "snapseed", "canva", "capcut", "inshot", "vsco", "picsart", "procreate", "sketch",
        "infinitepainter", "PENUP", "penup", "garagband", "flstudio", "bandlab",
        "luma", "davinci", "figma", "behance", "unsplash"
    )

    private val shoppingFoodKeywords = listOf(
        "amazon.mShop", "ebay", "kleinanzeigen", "vinted", "zalando", "otto", "aboutyou",
        "shein", "temu", "aliexpress", "ikea", "mediamarkt", "saturn", "idealo", "geizhals",
        "mydealz", "payback", "deutschlandcard", "lidl", "rewe", "edeka", "aldi", "kaufland",
        "dm.ella", "rossmann", "lieferando", "wolt", "ubereats", "gorillas", "flink",
        "too.good.to.go", "mcdonalds", "burgerking", "kfc", "starbucks", "hellofresh",
        "dhl", "paket", "hermes", "dpd", "ups", "gls"
    )

    private val travelMapsKeywords = listOf(
        "apps.maps", "waze", "osmand", "komoot", "alltrails", "dbnavigator", "bahn",
        "flixbus", "uber", "bolt", "freenow", "sixt", "miles", "sharenow", "tier",
        "lime", "voi", "booking", "airbnb", "skyscanner", "lufthansa", "ryanair",
        "eurowings", "condor", "tripadvisor", "adac", "clever.tanken", "mehr.tanken",
        "parkster", "easypark", "flightradar", "marinetraffic", "bvg", "hvv", "mvg", "vrs"
    )

    private val healthFitnessKeywords = listOf(
        "shealth", "health", "fitness", "strava", "garmin", "fitbit", "polar", "whoop",
        "yazio", "myfitnesspal", "lifesum", "freeletics", "adidas", "nike", "gymondo",
        "urban.sports", "doctolib", "tk.app", "barmer", "aok", "dak", "gesundheit",
        "apotheke", "shopapotheke", "docmorris", "e-rezept", "gematik", "headspace",
        "calm", "sleep", "water", "period", "clue", "flo"
    )

    private val infoReadingKeywords = listOf(
        "chrome", "browser", "firefox", "opera", "brave", "duckduckgo", "edge", "sbrowser",
        "chatgpt", "openai", "claude", "anthropic", "gemini", "bard", "perplexity", "copilot",
        "deepseek", "wikipedia", "kindle", "tolino", "readera", "pocket", "feedly",
        "spiegel", "tagesschau", "zeit", "faz", "sueddeutsche", "bild", "welt", "ntv",
        "handelsblatt", "heise", "golem", "reddit", "quora", "medium", "substack",
        "deepl", "translate", "leo.org", "duolingo", "babbel", "wetter", "weather", "rainviewer"
    )

    private val gamesKeywords = listOf(
        "supercell", "brawlstars", "clash", "king.candycrush", "roblox", "minecraft",
        "mojang", "genshin", "hoyoverse", "pubg", "activision", "callofduty", "ea.game",
        "fifa", "fc24", "ubisoft", "niantic", "pokemongo", "chess", "lichess", "sudoku",
        "solitaire", "uno", "monopoly", "gameloft", "zynga", "rovio", "angrybirds", "game"
    )

    fun categorizeApp(
        packageName: String,
        label: String,
        appInfo: ApplicationInfo?,
        customCategoryOverride: AppCategory? = null
    ): AppCategory {
        if (customCategoryOverride != null) {
            return customCategoryOverride
        }

        val pkgLower = packageName.lowercase(Locale.ROOT)
        val labelLower = label.lowercase(Locale.ROOT)
        val combined = "$pkgLower $labelLower"

        // 1. Check curated package & keyword database
        if (matchesAny(combined, socialKeywords)) return AppCategory.SOCIAL
        if (matchesAny(combined, financeKeywords)) return AppCategory.FINANCE
        if (matchesAny(combined, shoppingFoodKeywords)) return AppCategory.SHOPPING_FOOD
        if (matchesAny(combined, travelMapsKeywords)) return AppCategory.TRAVEL_MAPS
        if (matchesAny(combined, healthFitnessKeywords)) return AppCategory.HEALTH_FITNESS
        if (matchesAny(combined, gamesKeywords)) return AppCategory.GAMES
        if (matchesAny(combined, entertainmentKeywords)) return AppCategory.ENTERTAINMENT
        if (matchesAny(combined, creativityKeywords)) return AppCategory.CREATIVITY
        if (matchesAny(combined, productivityKeywords)) return AppCategory.PRODUCTIVITY
        if (matchesAny(combined, infoReadingKeywords)) return AppCategory.INFO_READING

        // 2. Check Android ApplicationInfo.category metadata
        if (appInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_GAME -> return AppCategory.GAMES
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> return AppCategory.PRODUCTIVITY
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.ENTERTAINMENT
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.CREATIVITY
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.TRAVEL_MAPS
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.INFO_READING
            }
            if ((appInfo.flags and ApplicationInfo.FLAG_IS_GAME) != 0) {
                return AppCategory.GAMES
            }
        }

        // 3. Heuristic label fallbacks (German & English)
        when {
            combined.contains("bank") || combined.contains("pay") || combined.contains("finanz") -> return AppCategory.FINANCE
            combined.contains("chat") || combined.contains("social") || combined.contains("forum") -> return AppCategory.SOCIAL
            combined.contains("mail") || combined.contains("office") || combined.contains("notiz") || combined.contains("doc") -> return AppCategory.PRODUCTIVITY
            combined.contains("photo") || combined.contains("foto") || combined.contains("video") || combined.contains("cam") -> return AppCategory.CREATIVITY
            combined.contains("music") || combined.contains("musik") || combined.contains("tv") || combined.contains("radio") || combined.contains("player") -> return AppCategory.ENTERTAINMENT
            combined.contains("shop") || combined.contains("store") || combined.contains("markt") || combined.contains("kauf") -> return AppCategory.SHOPPING_FOOD
            combined.contains("map") || combined.contains("nav") || combined.contains("reise") || combined.contains("hotel") || combined.contains("flug") -> return AppCategory.TRAVEL_MAPS
            combined.contains("fit") || combined.contains("health") || combined.contains("arzt") || combined.contains("med") -> return AppCategory.HEALTH_FITNESS
            combined.contains("news") || combined.contains("nachrichten") || combined.contains("buch") || combined.contains("book") || combined.contains("wiki") -> return AppCategory.INFO_READING
        }

        return AppCategory.UTILITIES
    }

    private fun matchesAny(haystack: String, keywords: List<String>): Boolean {
        for (kw in keywords) {
            if (haystack.contains(kw)) return true
        }
        return false
    }
}
