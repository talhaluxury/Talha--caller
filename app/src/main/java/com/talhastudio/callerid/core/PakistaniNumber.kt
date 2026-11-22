package com.talhastudio.callerid.core

/** Pakistani mobile number handling. Keep in sync with backend/src/validate.js. */
object PakistaniNumber {
    enum class Operator(val label: String) { JAZZ("Jazz"), ZONG("Zong"), UFONE("Ufone"), TELENOR("Telenor"), UNKNOWN("Unknown") }

    private val mobile = Regex("^3\\d{9}$")
    private val e164 = Regex("^\\+[1-9]\\d{7,14}$")

    /** Accepts 03XXXXXXXXX, +923XXXXXXXXX, 923XXXXXXXXX or 3XXXXXXXXX. Returns +923XXXXXXXXX or null. */
    fun normalize(raw: String): String? {
        val c = raw.trim().replace(Regex("[\\s\\-()]"), "")
        val national = when {
            c.startsWith("+92") -> c.substring(3)
            c.startsWith("0092") -> c.substring(4)
            c.startsWith("92") && c.length == 12 -> c.substring(2)
            c.startsWith("0") -> c.substring(1)
            else -> c
        }
        return if (mobile.matches(national)) "+92$national" else null
    }

    /** Caller IDs may be Pakistani numbers or any provider-issued international E.164 number. */
    fun parseCallerId(raw: String): String? {
        normalize(raw)?.let { return it }
        val c = raw.trim().replace(Regex("[\\s\\-()]"), "")
        return if (e164.matches(c)) c else null
    }

    /** Best-effort network guess from the number range. Ported numbers may differ. */
    fun operator(e164: String): Operator = when (e164.removePrefix("+92").take(2)) {
        "30", "32" -> Operator.JAZZ
        "31" -> Operator.ZONG
        "33" -> Operator.UFONE
        "34" -> Operator.TELENOR
        else -> Operator.UNKNOWN
    }

    fun isValidCallerName(n: String): Boolean = Regex("^[A-Za-z0-9 &.'-]{2,30}$").matches(n.trim())
}
