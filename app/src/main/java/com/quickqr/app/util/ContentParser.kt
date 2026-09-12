package com.quickqr.app.util

import java.net.URLDecoder

/** The different kinds of content a decoded QR/barcode value can represent. */
sealed class ParsedContent(val rawValue: String) {
    data class Url(val url: String) : ParsedContent(url)
    data class Wifi(val ssid: String, val password: String, val security: String) : ParsedContent(ssid)
    data class Email(val address: String, val subject: String?, val body: String?) : ParsedContent(address)
    data class Phone(val number: String) : ParsedContent(number)
    data class Sms(val number: String, val message: String?) : ParsedContent(number)
    data class Contact(val name: String?, val phone: String?, val email: String?) : ParsedContent(name ?: "")
    data class PlainText(val text: String) : ParsedContent(text)
}

/**
 * Best-effort classifier for raw barcode/QR payloads into the common QR sub-formats
 * (WIFI:, MATMSG:/mailto:, tel:, smsto:/sms:, vCard). Anything that doesn't match a
 * known pattern safely falls through to PlainText.
 */
object ContentParser {

    fun parse(raw: String): ParsedContent {
        val trimmed = raw.trim()
        return when {
            trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true) ->
                ParsedContent.Url(trimmed)
            trimmed.startsWith("WIFI:", true) -> parseWifi(trimmed)
            trimmed.startsWith("MATMSG:", true) || trimmed.startsWith("mailto:", true) -> parseEmail(trimmed)
            trimmed.startsWith("tel:", true) -> ParsedContent.Phone(trimmed.removePrefix("tel:"))
            trimmed.startsWith("smsto:", true) || trimmed.startsWith("sms:", true) -> parseSms(trimmed)
            trimmed.startsWith("BEGIN:VCARD", true) -> parseVCard(trimmed)
            else -> ParsedContent.PlainText(trimmed)
        }
    }

    private fun namedField(raw: String, key: String): String {
        // Terminator is a semicolon OR end-of-string: not every QR generator in the
        // wild appends the trailing ";;" the spec calls for, and without this the
        // last field (often the Wi-Fi password) would silently come back empty.
        val match = Regex("$key:((?:\\\\.|[^;])*)(?:;|\$)").find(raw)
        return match?.groupValues?.get(1)
            ?.replace("\\;", ";")
            ?.replace("\\,", ",")
            ?.replace("\\:", ":")
            ?.replace("\\\\", "\\")
            ?: ""
    }

    private fun parseWifi(raw: String): ParsedContent.Wifi {
        val ssid = namedField(raw, "S")
        val password = namedField(raw, "P")
        val type = namedField(raw, "T").ifEmpty { "WPA" }
        return ParsedContent.Wifi(ssid, password, type)
    }

    private fun parseEmail(raw: String): ParsedContent.Email {
        if (raw.startsWith("mailto:", true)) {
            val rest = raw.removePrefix("mailto:")
            val address = rest.substringBefore("?")
            val query = rest.substringAfter("?", "")
            val params = query.split("&").mapNotNull { part ->
                val kv = part.split("=", limit = 2)
                if (kv.size == 2) kv[0].lowercase() to urlDecode(kv[1]) else null
            }.toMap()
            return ParsedContent.Email(address, params["subject"], params["body"])
        }
        val to = namedField(raw, "TO")
        val subject = namedField(raw, "SUB").ifEmpty { null }
        val body = namedField(raw, "BODY").ifEmpty { null }
        return ParsedContent.Email(to, subject, body)
    }

    private fun parseSms(raw: String): ParsedContent.Sms {
        val withoutScheme = raw.substringAfter(":")
        val parts = withoutScheme.split(":", limit = 2)
        val number = parts.getOrNull(0).orEmpty()
        val message = parts.getOrNull(1)
        return ParsedContent.Sms(number, message)
    }

    private fun urlDecode(value: String): String = try {
        URLDecoder.decode(value, "UTF-8")
    } catch (e: Exception) {
        value
    }

    private fun parseVCard(raw: String): ParsedContent.Contact {
        fun field(key: String): String? {
            val match = Regex("(?m)^$key[^:]*:(.*)$").find(raw)
            return match?.groupValues?.get(1)?.trim()?.ifEmpty { null }
        }
        val name = field("FN") ?: field("N")
        val phone = field("TEL")
        val email = field("EMAIL")
        return ParsedContent.Contact(name, phone, email)
    }
}
