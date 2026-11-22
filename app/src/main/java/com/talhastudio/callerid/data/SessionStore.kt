package com.talhastudio.callerid.data

import android.content.Context

/** Keeps the sign-in token in app-private storage. For stricter protection, move it to EncryptedSharedPreferences. */
class SessionStore(ctx: Context) {
    private val p = ctx.getSharedPreferences("session", Context.MODE_PRIVATE)
    var token: String?
        get() = p.getString("token", null)
        set(v) { p.edit().putString("token", v).apply() }
    var email: String?
        get() = p.getString("email", null)
        set(v) { p.edit().putString("email", v).apply() }
    fun clear() { p.edit().clear().apply() }
}
