package com.tms.os.launcher.data

import android.content.Context
import androidx.compose.ui.graphics.asImageBitmap
import com.tms.os.launcher.model.SolvadoMetric
import com.tms.os.launcher.model.SolvadoShop
import com.tms.os.launcher.model.SolvadoShopStats
import com.tms.os.launcher.model.SolvadoState
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * The launcher's link to TMS Solvado: signs in with the Solvado account and
 * reads the widget figures from the read-only API (`/api/widgets/…`).
 *
 * The session (access + refresh token) lives in this app's private storage,
 * like the Solvado phone app keeps its own. The last figures are kept too, so
 * widgets show something the instant the launcher starts and while offline.
 * All calls block: run them off the main thread.
 */
class SolvadoRepository(context: Context) {

    private val prefs = context.getSharedPreferences("tms_os_solvado", Context.MODE_PRIVATE)
    private val iconDir = java.io.File(context.filesDir, "solvado_icons").apply { mkdirs() }
    private val iconMemory = java.util.concurrent.ConcurrentHashMap<String, androidx.compose.ui.graphics.ImageBitmap>()

    class ApiError(val status: Int, message: String) : Exception(message)

    var baseUrl: String
        get() = prefs.getString("base_url", DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
        set(value) = prefs.edit().putString("base_url", value.trim().trimEnd('/').ifEmpty { DEFAULT_BASE_URL }).apply()

    val connected: Boolean get() = !prefs.getString("refresh_token", null).isNullOrEmpty()

    /* ------------------------------------------------------------- session */

    fun login(email: String, password: String) {
        val body = JSONObject().put("email", email.trim()).put("password", password)
        storeSession(call("POST", "/api/widgets/login", body, auth = false))
    }

    fun logout() {
        val keep = baseUrl
        prefs.edit().clear().putString("base_url", keep).apply()
        iconMemory.clear()
        iconDir.listFiles()?.forEach { it.delete() }
    }

    /* --------------------------------------------------------------- logos */

    private fun iconFile(shopId: String) = java.io.File(iconDir, "${shopId.replace(Regex("[^A-Za-z0-9_-]"), "_")}.png")

    private fun icon(shopId: String): androidx.compose.ui.graphics.ImageBitmap? = iconMemory[shopId] ?: runCatching {
        val f = iconFile(shopId)
        if (!f.exists()) null else android.graphics.BitmapFactory.decodeFile(f.path)?.asImageBitmap()?.also { iconMemory[shopId] = it }
    }.getOrNull()

    /**
     * Downloads the logo tiles that are not on the phone yet. They are small
     * prepared squares (192 px) and do not change often, so each is fetched once.
     */
    fun loadMissingIcons(shopIds: List<String>) {
        val missing = shopIds.distinct().filter { !iconFile(it).exists() }
        missing.chunked(40).forEach { chunk ->
            val icons = call("GET", "/api/widgets/shop-icons?ids=${chunk.joinToString(",")}").optJSONObject("icons") ?: return@forEach
            chunk.forEach { id ->
                val url = icons.optString(id).takeIf { it.startsWith("https://") } ?: return@forEach
                runCatching {
                    val connection = (URL(url).openConnection() as HttpURLConnection).apply { connectTimeout = 8_000; readTimeout = 15_000 }
                    try {
                        if (connection.responseCode == 200) {
                            val tmp = java.io.File(iconDir, "$id.tmp")
                            connection.inputStream.use { input -> tmp.outputStream().use { input.copyTo(it) } }
                            tmp.renameTo(iconFile(id))
                        }
                    } finally { connection.disconnect() }
                }
            }
        }
    }

    private fun storeSession(json: JSONObject) {
        val access = json.optString("access_token")
        val refresh = json.optString("refresh_token")
        if (access.isEmpty() || refresh.isEmpty()) throw ApiError(401, "Anmeldung fehlgeschlagen.")
        prefs.edit()
            .putString("access_token", access)
            .putString("refresh_token", refresh)
            .putLong("expires_at", System.currentTimeMillis() + json.optLong("expires_in", 3600L) * 1000L)
            .apply()
    }

    private fun accessToken(): String {
        val refresh = prefs.getString("refresh_token", null) ?: throw ApiError(401, "Nicht angemeldet.")
        val fresh = prefs.getLong("expires_at", 0L) - System.currentTimeMillis() > 60_000L
        if (!fresh) storeSession(call("POST", "/api/widgets/refresh", JSONObject().put("refresh_token", refresh), auth = false))
        return prefs.getString("access_token", "").orEmpty()
    }

    /* ------------------------------------------------------------- figures */

    /** Who is signed in and which shops they have. Also remembered for the next start. */
    fun loadAccount(): SolvadoState {
        val json = call("GET", "/api/widgets/me")
        prefs.edit().putString("cache_me", json.toString()).apply()
        return cachedState()
    }

    fun loadAdmin(): Map<String, Double?> {
        val json = call("GET", "/api/widgets/admin")
        prefs.edit().putString("cache_admin", json.toString()).apply()
        return parseAdmin(json)
    }

    fun loadShops(ids: List<String>, range: String): List<SolvadoShopStats> {
        if (ids.isEmpty()) return emptyList()
        val out = ArrayList<SolvadoShopStats>()
        // The API answers at most eight shops per call.
        ids.distinct().chunked(8).forEach { chunk ->
            val json = call("GET", "/api/widgets/shops?range=$range&ids=${chunk.joinToString(",")}")
            val shops = json.optJSONArray("shops") ?: JSONArray()
            for (i in 0 until shops.length()) parseShop(shops.getJSONObject(i))?.let { out += it }
        }
        val cache = JSONObject(prefs.getString("cache_shops", "{}") ?: "{}")
        out.forEach { cache.put("${it.id}|$range", encodeShop(it)) }
        prefs.edit().putString("cache_shops", cache.toString()).putLong("updated_at", System.currentTimeMillis()).apply()
        return out
    }

    /** Everything known from the last successful calls. */
    fun cachedState(): SolvadoState {
        if (!connected) return SolvadoState()
        val me = prefs.getString("cache_me", null)?.let { runCatching { JSONObject(it) }.getOrNull() }
        val shops = me?.optJSONArray("shops")?.let { arr ->
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val id = o.optString("bundle_id")
                SolvadoShop(id, o.optString("name"), o.optString("currency", "EUR"), icon(id))
            }
        }.orEmpty()
        val admin = prefs.getString("cache_admin", null)?.let { runCatching { parseAdmin(JSONObject(it)) }.getOrNull() }
        val stats = HashMap<String, SolvadoShopStats>()
        runCatching {
            val cache = JSONObject(prefs.getString("cache_shops", "{}") ?: "{}")
            cache.keys().forEach { key -> parseShop(cache.getJSONObject(key))?.let { stats[key] = it } }
        }
        return SolvadoState(
            connected = true,
            email = me?.optString("email").orEmpty(),
            isAdmin = me?.optBoolean("is_admin", false) ?: false,
            shops = shops,
            admin = admin,
            shopStats = stats,
            updatedAt = prefs.getLong("updated_at", 0L)
        )
    }

    fun markUpdated() = prefs.edit().putLong("updated_at", System.currentTimeMillis()).apply()

    /* ------------------------------------------------------------- parsing */

    private fun JSONObject.number(key: String): Double? = if (isNull(key) || !has(key)) null else optDouble(key).takeIf { !it.isNaN() }

    private fun parseAdmin(json: JSONObject): Map<String, Double?> {
        val out = HashMap<String, Double?>()
        out["subscribers"] = json.number("subscribers_active")
        out["mrr"] = json.number("mrr_eur")
        out["trials"] = json.number("trials_active")
        out["accounts_total"] = json.number("accounts_total")
        for ((metric, field) in listOf("credit_revenue" to "credit_revenue_eur", "credit_purchases" to "credit_purchases", "new_accounts" to "new_accounts")) {
            val perRange = json.optJSONObject(field)
            for (range in listOf("today", "7d", "30d")) out["$metric:$range"] = perRange?.number(range)
        }
        return out
    }

    private fun parseShop(o: JSONObject): SolvadoShopStats? {
        if (!o.optBoolean("found", true)) return null
        val values = HashMap<String, Double?>()
        SolvadoMetric.of(admin = false).forEach { values[it.id] = o.number(it.id) }
        return SolvadoShopStats(
            id = o.optString("bundle_id"),
            name = o.optString("name", "Shop"),
            currency = o.optString("currency", "EUR"),
            values = values,
            adSpendDaysMissing = if (o.isNull("ad_spend_days_missing")) 0 else o.optInt("ad_spend_days_missing", 0)
        )
    }

    private fun encodeShop(s: SolvadoShopStats): JSONObject {
        val o = JSONObject().put("bundle_id", s.id).put("name", s.name).put("currency", s.currency).put("ad_spend_days_missing", s.adSpendDaysMissing)
        s.values.forEach { (k, v) -> o.put(k, v ?: JSONObject.NULL) }
        return o
    }

    /* ---------------------------------------------------------------- http */

    private fun call(method: String, path: String, body: JSONObject? = null, auth: Boolean = true): JSONObject {
        val connection = (URL(baseUrl + path).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 10_000
            readTimeout = 45_000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "TMS-OS-Launcher")
            if (auth) setRequestProperty("Authorization", "Bearer ${accessToken()}")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
                outputStream.use { it.write(body.toString().toByteArray()) }
            }
        }
        try {
            val status = connection.responseCode
            val text = (if (status in 200..299) connection.inputStream else connection.errorStream)?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (status !in 200..299) {
                val detail = runCatching { JSONObject(text).optString("detail") }.getOrNull().orEmpty()
                throw ApiError(status, detail.ifEmpty { "Solvado antwortet mit Fehler $status." })
            }
            return JSONObject(text)
        } finally {
            connection.disconnect()
        }
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.tms-solvado.com"
    }
}
