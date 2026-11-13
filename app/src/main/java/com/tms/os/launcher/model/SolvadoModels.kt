package com.tms.os.launcher.model

/** How a Solvado figure is written. */
enum class SolvadoFormat { MONEY, COUNT, PERCENT, RATIO }

/**
 * Every figure a Solvado widget can show. [ranged] figures belong to the
 * widget's period (today / 7 days / 30 days); the others are a current state.
 */
enum class SolvadoMetric(
    val id: String,
    val admin: Boolean,
    val format: SolvadoFormat,
    val ranged: Boolean,
    private val de: String,
    private val en: String,
    private val tr: String
) {
    // Platform view (admins)
    SUBSCRIBERS("subscribers", true, SolvadoFormat.COUNT, false, "Abonnenten", "Subscribers", "Aboneler"),
    CREDIT_REVENUE("credit_revenue", true, SolvadoFormat.MONEY, true, "Credit-Umsatz", "Credit revenue", "Kredi cirosu"),
    NEW_ACCOUNTS("new_accounts", true, SolvadoFormat.COUNT, true, "Neue Accounts", "New accounts", "Yeni hesaplar"),
    ACCOUNTS_TOTAL("accounts_total", true, SolvadoFormat.COUNT, false, "Accounts gesamt", "Accounts total", "Toplam hesap"),
    MRR("mrr", true, SolvadoFormat.MONEY, false, "MRR", "MRR", "MRR"),
    CREDIT_PURCHASES("credit_purchases", true, SolvadoFormat.COUNT, true, "Credit-Käufe", "Credit purchases", "Kredi alımları"),
    TRIALS("trials", true, SolvadoFormat.COUNT, false, "Testphasen", "Trials", "Denemeler"),

    // One shop (merchants)
    REVENUE("revenue", false, SolvadoFormat.MONEY, true, "Umsatz", "Revenue", "Ciro"),
    PROFIT("profit", false, SolvadoFormat.MONEY, true, "Gewinn", "Profit", "Kâr"),
    CONVERSION("conversion_rate", false, SolvadoFormat.PERCENT, true, "Conversion", "Conversion", "Dönüşüm"),
    AD_SPEND("ad_spend", false, SolvadoFormat.MONEY, true, "Werbekosten", "Ad spend", "Reklam gideri"),
    PRODUCT_COSTS("product_costs", false, SolvadoFormat.MONEY, true, "Produktkosten", "Product costs", "Ürün maliyeti"),
    ORDERS("orders", false, SolvadoFormat.COUNT, true, "Bestellungen", "Orders", "Siparişler"),
    VISITORS("visitors", false, SolvadoFormat.COUNT, true, "Besucher", "Visitors", "Ziyaretçiler"),
    AOV("aov", false, SolvadoFormat.MONEY, true, "Ø Bestellwert", "Avg. order", "Ort. sipariş"),
    ROAS("roas", false, SolvadoFormat.RATIO, true, "ROAS", "ROAS", "ROAS"),
    MARGIN("margin_pct", false, SolvadoFormat.PERCENT, true, "Marge", "Margin", "Marj");

    fun label(language: AppLanguage): String = when (language) { AppLanguage.EN -> en; AppLanguage.TR -> tr; else -> de }

    companion object {
        fun of(admin: Boolean): List<SolvadoMetric> = entries.filter { it.admin == admin }
        fun fromId(id: String): SolvadoMetric? = entries.find { it.id == id }
        val adminDefault = listOf(SUBSCRIBERS, CREDIT_REVENUE, NEW_ACCOUNTS, ACCOUNTS_TOTAL)
        val shopDefault = listOf(REVENUE, PROFIT, CONVERSION, AD_SPEND)
    }
}

/** What one Solvado widget shows: which shop, which period, which figures. Stored with the widget. */
data class SolvadoWidgetConfig(
    val shopId: String = "",
    val range: String = "today",
    val metrics: List<SolvadoMetric> = emptyList()
) {
    fun encode(): String = "shop=$shopId;range=$range;metrics=${metrics.joinToString(",") { it.id }}"

    companion object {
        val ranges = listOf("today", "7d", "30d")

        fun decode(raw: String, admin: Boolean): SolvadoWidgetConfig {
            val map = raw.split(';').mapNotNull { p -> p.split('=', limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }.toMap()
            val metrics = map["metrics"].orEmpty().split(',').mapNotNull { SolvadoMetric.fromId(it) }.filter { it.admin == admin }
            return SolvadoWidgetConfig(
                shopId = map["shop"].orEmpty(),
                range = map["range"].takeIf { it in ranges } ?: "today",
                metrics = metrics.ifEmpty { if (admin) SolvadoMetric.adminDefault else SolvadoMetric.shopDefault }
            )
        }
    }
}

data class SolvadoShop(
    val id: String,
    val name: String,
    val currency: String,
    /** The shop's logo tile, once downloaded. */
    val icon: androidx.compose.ui.graphics.ImageBitmap? = null
)

/** Figures of one shop for one period; a value that could not be measured is null. */
data class SolvadoShopStats(
    val id: String,
    val name: String,
    val currency: String,
    val values: Map<String, Double?>,
    /** Days of the period without a measured ad spend: ad spend and profit are then incomplete. */
    val adSpendDaysMissing: Int
)

data class SolvadoState(
    val connected: Boolean = false,
    val email: String = "",
    val isAdmin: Boolean = false,
    val shops: List<SolvadoShop> = emptyList(),
    /** Admin figures: "subscribers", "mrr", … and "credit_revenue:today", "new_accounts:7d", … */
    val admin: Map<String, Double?>? = null,
    /** Keyed "<shop id>|<range>". */
    val shopStats: Map<String, SolvadoShopStats> = emptyMap(),
    val updatedAt: Long = 0L,
    val busy: Boolean = false,
    val error: String? = null
)
