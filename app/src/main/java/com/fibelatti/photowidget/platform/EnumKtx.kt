package com.fibelatti.photowidget.platform

inline fun <reified T : Enum<T>> enumValueOfOrNull(name: String?): T? {
    return try {
        enumValueOf<T>(name = name ?: return null)
    } catch (_: Exception) {
        null
    }
}
