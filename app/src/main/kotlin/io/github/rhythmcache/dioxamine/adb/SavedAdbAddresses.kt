package io.github.rhythmcache.dioxamine.adb

import android.content.Context

/**
 * Simple SharedPreferences-backed store for manually entered ADB TCP addresses ("ip:port").
 *
 * Order is preserved (most-recently-added/used first), which is why addresses are kept as a
 * single delimited String rather than a SharedPreferences String Set (whose iteration order is
 * not guaranteed).
 */
object SavedAdbAddresses {
    private const val PREFS_NAME = "adb_saved_addresses"
    private const val KEY_ADDRESSES = "addresses"
    private const val DELIMITER = "|"
    private const val MAX_SAVED = 15

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    /** Returns saved "ip:port" addresses, most recently added/used first. */
    fun getAll(context: Context): List<String> {
        val raw = prefs(context).getString(KEY_ADDRESSES, null) ?: return emptyList()
        return raw.split(DELIMITER).filter { it.isNotBlank() }
    }

    /** Adds an "ip:port" address, or moves it to the front if it's already saved. */
    fun add(context: Context, address: String) {
        val trimmed = address.trim()
        if (trimmed.isEmpty() || trimmed == ":") return
        val current = getAll(context).toMutableList()
        current.remove(trimmed)
        current.add(0, trimmed)
        while (current.size > MAX_SAVED) current.removeAt(current.lastIndex)
        prefs(context).edit().putString(KEY_ADDRESSES, current.joinToString(DELIMITER)).apply()
    }

    /** Removes a single saved "ip:port" address. */
    fun remove(context: Context, address: String) {
        val current = getAll(context).toMutableList()
        if (current.remove(address)) {
            prefs(context).edit().putString(KEY_ADDRESSES, current.joinToString(DELIMITER)).apply()
        }
    }

    /** Returns saved addresses containing [query] (case-insensitive). A blank query returns all. */
    fun filter(context: Context, query: String): List<String> {
        val all = getAll(context)
        if (query.isBlank()) return all
        return all.filter { it.contains(query, ignoreCase = true) }
    }
}

fun parseIpAndPort(input: String): Pair<String, String?> {
    val trimmed = input.trim()
    if (trimmed.contains(":")) {
        val parts = trimmed.split(":")
        if (parts.size == 2) {
            return Pair(parts[0].trim(), parts[1].trim())
        }
    }
    return Pair(trimmed, null)
}

fun isValidIp(ip: String): Boolean {
    if (ip.equals("localhost", ignoreCase = true)) return true
    val ipv4Regex = Regex("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$")
    return ipv4Regex.matches(ip)
}

fun isValidPort(portStr: String): Boolean {
    val p = portStr.toIntOrNull() ?: return false
    return p in 1..65535
}

