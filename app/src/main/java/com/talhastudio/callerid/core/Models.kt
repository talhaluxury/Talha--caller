package com.talhastudio.callerid.core

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class CallState {
    CALLING, RINGING, CONNECTED, ENDED, FAILED;

    val terminal: Boolean get() = this == ENDED || this == FAILED

    companion object {
        /** Unknown values are treated as FAILED so a call is never shown as working by mistake. */
        fun fromWire(s: String): CallState = when (s.lowercase()) {
            "calling" -> CALLING
            "ringing" -> RINGING
            "connected" -> CONNECTED
            "ended" -> ENDED
            else -> FAILED
        }
    }
}

data class VerifiedNumber(val id: String, val e164: String, val status: String, val isDefault: Boolean, val callerName: String?) {
    val isVerified: Boolean get() = status == "verified"
}

data class CallerName(val name: String?, val status: String)
data class CallSession(val callId: String, val state: CallState, val durationSec: Int, val testMode: Boolean)
data class Balance(val amount: Double?, val currency: String?)

fun formatDuration(sec: Int): String = "%02d:%02d".format(sec / 60, sec % 60)
fun formatDate(ms: Long): String = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()).format(Date(ms))
