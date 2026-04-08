package com.example.androidwake.domain

object MacAddress {
    private val separatorRegex = "[-:]".toRegex()
    private val strictRegex = Regex("^[0-9A-F]{2}(:[0-9A-F]{2}){5}$")

    fun normalize(raw: String): String? {
        val compact = raw.trim().uppercase().replace(separatorRegex, "")
        if (compact.length != 12 || compact.any { !it.isDigit() && it !in 'A'..'F' }) {
            return null
        }
        val octets = compact.chunked(2)
        val normalized = octets.joinToString(":")
        if (!strictRegex.matches(normalized)) return null
        if (normalized == "FF:FF:FF:FF:FF:FF" || normalized == "00:00:00:00:00:00") {
            return null
        }
        return normalized
    }

    fun formatForInput(raw: String): String {
        val compact = raw.uppercase()
            .filter { it.isDigit() || it in 'A'..'F' }
            .take(12)
        if (compact.isEmpty()) return ""

        val pairs = compact.chunked(2)
        return pairs.joinToString(":")
    }

    fun defaultMachineName(normalizedMac: String): String {
        val suffix = normalizedMac.replace(":", "").takeLast(6)
        return "Machine $suffix"
    }
}

object NetworkIdentityValidator {
    fun normalizeSsid(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty() || value.length > 32) return null
        return value
    }

    fun normalizeBssid(raw: String): String? = MacAddress.normalize(raw)
}
