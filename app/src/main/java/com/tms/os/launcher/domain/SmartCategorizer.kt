package com.tms.os.launcher.domain

import android.content.pm.ApplicationInfo
import android.os.Build
import com.tms.os.launcher.model.AppCategory
import java.util.Locale

object SmartCategorizer {

    private val financeKeywords = listOf(
        "binance", "metamask", "crypto.exchange", "cryptocom", "coinbase", "kraken",
        "bybit", "okx", "kucoin", "bitget", "bitpanda", "bison", "trustwallet",
        "phantom", "ledger", "trezor", "uniswap", "1inch", "tradingview", "investing",
        "n26", "revolut", "paypal", "fab", "firstabudhabi", "adcb", "emiratesnbd",
        "enbd", "mashreq", "wio", "liv.", "alhilal", "adib", "dib", "tabby", "tamara",
        "sparkasse", "finanzstatus", "dkb", "ing.diba", "comdirect", "commerzbank",
        "deutschebank", "postbank", "traderepublic", "scalable", "klarna", "curve",
        "walletnfcrel", "spay", "samsungpay", "googlepay", "vr.banking", "consorsbank",
        "targobank", "santander", "c24", "bunq", "wise", "remitly", "westernunion",
        "finanzguru", "outbank", "wiso", "elster", "stripe", "sumup"
    )

    private val socialKeywords = listOf(
        "whatsapp", "discord", "facebook.katana", "facebook.lite", "facebook.orca",
        "messenger", "instagram", "telegram", "org.telegram", "linkedin", "skype",
        "snapchat", "tiktok", "musically", "com.twitter.android", "threads",
        "reddit", "signal", "threema", "pinterest", "bereal", "jodel", "tinder",
        "bumble", "hinge", "lovoo", "badoo", "viber", "wechat", "line.android",
        "mastodon", "bluesky", "clubhouse", " Bottim", "botim", "imo.android"
    )

    private val shoppingFoodKeywords = listOf(
        "shopify", "shop.app", "amazon.mshop", "amazon.windowshop", "flink",
        "temu", "shein", "aliexpress", "alibaba", "zalando", "otto", "aboutyou",
        "asos", "ebay", "kleinanzeigen", "vinted", "ikea", "mediamarkt", "saturn",
        "idealo", "geizhals", "mydealz", "payback", "deutschlandcard", "lidl",
        "rewe", "edeka", "aldi", "kaufland", "dm.ella", "rossmann", "lieferando",
        "wolt", "ubereats", "gorillas", "talabat", "deliveroo", "instashop",
        "noon", "namshi", "carrefour", "lulu", "smiles", "entertainer",
        "mcdonalds", "burgerking", "kfc", "starbucks", "hellofresh", "toogoodtogo",
        "dhl", "hermes", "dpd", "ups", "fedex", "aramex"
    )

    private val travelMapsKeywords = listOf(
        "airbnb", "bolt", "booking", "dbnavigator", "deutschebahn", "apps.maps",
        "waze", "osmand", "komoot", "alltrails", "flixbus", "uber", "careem",
        "freenow", "sixt", "miles", "sharenow", "tier", "lime", "voi",
        "skyscanner", "kayak", "expedia", "agoda", "tripadvisor", "trivago",
        "lufthansa", "ryanair", "eurowings", "condor", "emirates", "etihad",
        "flydubai", "airarabia", "qatarairways", "turkishairlines", "wizzair",
        "flightradar", "rta", "dubaidrive", "salik", "darb", "parkonic",
        "adac", "clevertanken", "mehrtanken", "parkster", "easypark", "bvg", "hvv", "mvg"
    )

    private val entertainmentKeywords = listOf(
        "youtube", "netflix", "spotify", "audible", "primevideo", "amazon.avod",
        "disney", "dazn", "sky", "wowtv", "joyn", "rtlplus", "zdf", "ardmediathek",
        "arte", "twitch", "soundcloud", "deezer", "tidal", "apple.android.music",
        "shazam", "pocketcasts", "antennapod", "vlc", "mxtech", "plex", "kodi",
        "stremio", "crunchyroll", "anghami", "shahid", "osn", "starzplay",
        "steam", "playstation", "xbox", "nintendo", "epicgames", "eventim"
    )

    private val creativityKeywords = listOf(
        "camera", "kamera", "gallery", "galerie", "apps.photos", "edgegallery",
        "lightroom", "photoshop", "snapseed", "canva", "capcut", "inshot",
        "vsco", "picsart", "sketch", "infinitepainter", "penup", "garageband",
        "flstudio", "bandlab", "luma", "davinci", "figma", "behance", "unsplash"
    )

    private val aiAndWebKeywords = listOf(
        "copilot", "chatgpt", "openai", "claude", "anthropic", "gemini", "bard",
        "perplexity", "deepseek", "poe", "character.ai", "midjourney",
        "chrome", "sbrowser", "firefox", "opera", "brave", "duckduckgo", "emmx",
        "vivaldi", "torproject", "googlequicksearchbox", "wikipedia", "kindle",
        "tolino", "readera", "pocket", "feedly", "spiegel", "tagesschau", "zeit",
        "faz", "sueddeutsche", "bild", "welt", "ntv", "handelsblatt", "heise",
        "khaleejtimes", "gulfnews", "thenational", "medium", "substack",
        "deepl", "translate", "duolingo", "babbel", "wetter", "weather", "rainviewer"
    )

    private val healthFitnessKeywords = listOf(
        "shealth", "strava", "garmin", "fitbit", "polar", "whoop", "yazio",
        "myfitnesspal", "lifesum", "freeletics", "adidas", "nike", "gymondo",
        "urbansports", "classpass", "doctolib", "tk.app", "barmer", "aok", "dak",
        "apotheke", "docmorris", "erezept", "gematik", "mohap", "alhosn", "dha",
        "seha", "headspace", "calm", "clue", "flo"
    )

    private val gamesKeywords = listOf(
        "supercell", "brawlstars", "clash", "candycrush", "roblox", "minecraft",
        "mojang", "genshin", "hoyoverse", "pubg", "activision", "callofduty",
        "ea.game", "fifa", "ubisoft", "niantic", "pokemongo", "chess", "lichess",
        "sudoku", "solitaire", "uno", "monopoly", "gameloft", "zynga", "rovio",
        "angrybirds", "darts", "8ball", "ludo"
    )

    private val utilitiesSystemKeywords = listOf(
        "knox", "securefolder", "sichererordner", "uaepass", "dubainow", "icp",
        "dewa", "etisalat", "eand", "du.ae", "virginmobile", "vodafone", "telekom",
        "o2", "meinmagenta", "freenet", "1und1", "congstar", "fraenk", "aldi_talk",
        "authenticator", "authy", "bitwarden", "1password", "keepass", "dashlane",
        "samsungpass", "protonpass", "nordvpn", "expressvpn", "surfshark", "protonvpn",
        "wireguard", "openvpn", "tailscale", "cloudflare.onedotonedotonedotone",
        "com.android.settings", "einstellungen", "microg", "vending", "galaxystore",
        "goodlock", "fine_lock", "nicelock", "devicecare", "myfiles", "documentsui",
        "calculator", "rechner", "deskclock", "clock", "uhr", "dialer", "telecom",
        "contacts", "kontakte", "stk", "sim", "isync", "smartswitch", "wearable",
        "buds", "findmy", "smartthings", "bixby", "routine", "termux", "tms.os.launcher"
    )

    private val productivityKeywords = listOf(
        "gmail", "outlook", "protonmail", "gmx", "webde", "thunderbird", "spark",
        "calendar", "kalender", "notion", "obsidian", "evernote", "onenote",
        "keep", "app.notes", "notizen", "slack", "teams", "zoom", "webex",
        "apps.meetings", "trello", "asana", "jira", "linear", "todoist", "ticktick",
        "microsoft.office", "office.word", "office.excel", "office.powerpoint",
        "apps.docs", "onedrive", "dropbox", "nextcloud", "box.android",
        "adobe.reader", "acrobat", "camscanner", "geniusscan", "github", "gitlab"
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

        // Exact label or package special cases (e.g. "X", "Shop", "FAB", "e&")
        if (labelLower == "x" || pkgLower == "com.twitter.android") return AppCategory.SOCIAL
        if (labelLower == "shop" || pkgLower.contains("shopify")) return AppCategory.SHOPPING_FOOD
        if (labelLower.startsWith("fab") || labelLower.contains("bank") || labelLower.contains("pay")) return AppCategory.FINANCE
        if (labelLower == "e&" || labelLower.contains("uae pass") || labelLower.contains("sicherer ordner")) return AppCategory.UTILITIES

        // 1. Check Games flag/category first
        if (appInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (appInfo.category == ApplicationInfo.CATEGORY_GAME) return AppCategory.GAMES
        }
        if (matchesAny(combined, gamesKeywords)) return AppCategory.GAMES

        // 2. Check Finance & Crypto
        if (matchesAny(combined, financeKeywords)) return AppCategory.FINANCE

        // 3. Check Social Networks
        if (matchesAny(combined, socialKeywords)) return AppCategory.SOCIAL

        // 4. Check Shopping & Food
        if (matchesAny(combined, shoppingFoodKeywords)) return AppCategory.SHOPPING_FOOD

        // 5. Check Travel & Navigation
        if (matchesAny(combined, travelMapsKeywords)) return AppCategory.TRAVEL_MAPS

        // 6. Check Entertainment & Media
        if (matchesAny(combined, entertainmentKeywords)) return AppCategory.ENTERTAINMENT

        // 7. Check Creativity & Photo
        if (matchesAny(combined, creativityKeywords)) return AppCategory.CREATIVITY

        // 8. Check AI, Browser & Info
        if (matchesAny(combined, aiAndWebKeywords)) return AppCategory.INFO_READING

        // 9. Check Health & Fitness
        if (matchesAny(combined, healthFitnessKeywords)) return AppCategory.HEALTH_FITNESS

        // 10. Check System, Security, Telecom & Utilities BEFORE generic Productivity!
        if (matchesAny(combined, utilitiesSystemKeywords)) return AppCategory.UTILITIES

        // 11. Check Explicit Productivity apps
        if (matchesAny(combined, productivityKeywords)) return AppCategory.PRODUCTIVITY

        // 12. Check Android ApplicationInfo.category metadata (except generic CATEGORY_PRODUCTIVITY unless label hints office/work)
        if (appInfo != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            when (appInfo.category) {
                ApplicationInfo.CATEGORY_SOCIAL -> return AppCategory.SOCIAL
                ApplicationInfo.CATEGORY_AUDIO,
                ApplicationInfo.CATEGORY_VIDEO -> return AppCategory.ENTERTAINMENT
                ApplicationInfo.CATEGORY_IMAGE -> return AppCategory.CREATIVITY
                ApplicationInfo.CATEGORY_MAPS -> return AppCategory.TRAVEL_MAPS
                ApplicationInfo.CATEGORY_NEWS -> return AppCategory.INFO_READING
                ApplicationInfo.CATEGORY_PRODUCTIVITY -> {
                    if (combined.contains("mail") || combined.contains("doc") ||
                        combined.contains("note") || combined.contains("office") ||
                        combined.contains("pdf") || combined.contains("scan") ||
                        combined.contains("cloud") || combined.contains("drive") ||
                        combined.contains("work") || combined.contains("task")
                    ) {
                        return AppCategory.PRODUCTIVITY
                    }
                    return AppCategory.UTILITIES
                }
            }
        }

        // 13. Heuristic fallbacks
        when {
            combined.contains("crypto") || combined.contains("wallet") || combined.contains("finanz") || combined.contains("trade") -> return AppCategory.FINANCE
            combined.contains("chat") || combined.contains("social") || combined.contains("messenger") -> return AppCategory.SOCIAL
            combined.contains("photo") || combined.contains("foto") || combined.contains("cam") || combined.contains("edit") -> return AppCategory.CREATIVITY
            combined.contains("music") || combined.contains("musik") || combined.contains("tv") || combined.contains("stream") || combined.contains("player") -> return AppCategory.ENTERTAINMENT
            combined.contains("shop") || combined.contains("store") || combined.contains("markt") || combined.contains("food") -> return AppCategory.SHOPPING_FOOD
            combined.contains("map") || combined.contains("reise") || combined.contains("hotel") || combined.contains("taxi") || combined.contains("flight") -> return AppCategory.TRAVEL_MAPS
            combined.contains("fit") || combined.contains("health") || combined.contains("med") -> return AppCategory.HEALTH_FITNESS
            combined.contains("news") || combined.contains("buch") || combined.contains("book") || combined.contains("browser") -> return AppCategory.INFO_READING
            combined.contains("mail") || combined.contains("office") || combined.contains("notiz") -> return AppCategory.PRODUCTIVITY
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
