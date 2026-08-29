package com.msc.distractionalert.util

import java.text.SimpleDateFormat
import java.time.format.DateTimeParseException
import java.util.Locale

/**
 * The backend sends ISO8601 timestamps (e.g. "2026-08-15T14:32:07Z"). Parsed with
 * java.time where available (API 26+); falls back to the raw string on older devices
 * or malformed input rather than crashing the list.
 */
fun formatTimestamp(isoTimestamp: String): String {
    return try {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val instant = java.time.Instant.parse(isoTimestamp)
            val formatter = java.time.format.DateTimeFormatter
                .ofPattern("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
                .withZone(java.time.ZoneId.systemDefault())
            formatter.format(instant)
        } else {
            legacyFormat(isoTimestamp)
        }
    } catch (e: DateTimeParseException) {
        isoTimestamp
    } catch (e: Exception) {
        isoTimestamp
    }
}

private fun legacyFormat(isoTimestamp: String): String {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US)
    val output = SimpleDateFormat("dd MMM yyyy, HH:mm:ss", Locale.getDefault())
    val cleaned = isoTimestamp.removeSuffix("Z")
    val date = parser.parse(cleaned) ?: return isoTimestamp
    return output.format(date)
}
