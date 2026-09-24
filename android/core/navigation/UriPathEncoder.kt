package com.nivya.core.navigation

import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets

/**
 * RFC 2396 compliant URI path segment encoder and decoder.
 * Uses Android's native android.net.Uri.encode / Uri.decode when available,
 * with a deterministic RFC-compliant JVM fallback for host unit tests.
 */
object UriPathEncoder {

    // RFC 2396 unreserved characters allowed in URI path segments without escaping
    private const val ALLOWED_UNRESERVED = "-_.*!~'()"

    /**
     * Encodes a string (e.g. an email address) as a safe URI path segment for Navigation routes.
     * Preserves '+' safely as '%2B' so Jetpack Compose Navigation does not convert it to space.
     */
    fun encode(s: String?): String {
        if (s == null) return ""
        if (s.isEmpty()) return ""
        return try {
            val androidEncoded = android.net.Uri.encode(s)
            if (!androidEncoded.isNullOrEmpty()) {
                androidEncoded
            } else {
                encodeInternal(s)
            }
        } catch (_: Throwable) {
            encodeInternal(s)
        }
    }

    /**
     * Decodes a URI path segment back into its exact original string.
     * Unlike java.net.URLDecoder, this does NOT convert '+' into space.
     */
    fun decode(s: String?): String {
        if (s == null) return ""
        if (s.isEmpty()) return ""
        return try {
            val androidDecoded = android.net.Uri.decode(s)
            if (androidDecoded != null) {
                androidDecoded
            } else {
                decodeInternal(s)
            }
        } catch (_: Throwable) {
            decodeInternal(s)
        }
    }

    private fun isAllowed(c: Char): Boolean {
        return (c in 'a'..'z') || (c in 'A'..'Z') || (c in '0'..'9') || ALLOWED_UNRESERVED.indexOf(c) != -1
    }

    private fun encodeInternal(s: String): String {
        val bytes = s.toByteArray(StandardCharsets.UTF_8)
        val out = StringBuilder()
        for (b in bytes) {
            val c = b.toInt().toChar()
            if (b in 0..127 && isAllowed(c)) {
                out.append(c)
            } else {
                out.append('%')
                val hex = String.format("%02X", b.toInt() and 0xFF)
                out.append(hex)
            }
        }
        return out.toString()
    }

    private fun decodeInternal(s: String): String {
        val len = s.length
        val baos = ByteArrayOutputStream(len)
        var i = 0
        while (i < len) {
            val c = s[i]
            if (c == '%' && i + 2 < len) {
                val hex = s.substring(i + 1, i + 3)
                val byteVal = hex.toIntOrNull(16)
                if (byteVal != null) {
                    baos.write(byteVal)
                    i += 3
                    continue
                }
            }
            val charBytes = c.toString().toByteArray(StandardCharsets.UTF_8)
            baos.write(charBytes, 0, charBytes.size)
            i++
        }
        return baos.toString(StandardCharsets.UTF_8)
    }
}
