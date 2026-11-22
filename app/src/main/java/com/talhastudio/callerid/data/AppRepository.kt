package com.talhastudio.callerid.data

import com.talhastudio.callerid.core.*
import com.talhastudio.callerid.data.local.AppDatabase
import com.talhastudio.callerid.data.local.CallRecord
import com.talhastudio.callerid.data.provider.TelephonyProvider
import com.talhastudio.callerid.data.remote.*
import kotlinx.coroutines.flow.Flow

class AppRepository(
    private val api: ApiService?,
    val provider: TelephonyProvider,
    private val db: AppDatabase,
    val session: SessionStore
) {
    val configured: Boolean get() = api != null

    private suspend fun <T> withApi(block: suspend (ApiService) -> T): Result<T> =
        if (api == null) Result.failure(ApiException(NOT_CONFIGURED)) else safeApi { block(api) }

    suspend fun signIn(email: String, password: String, create: Boolean): Result<Unit> = withApi {
        val r = if (create) it.register(AuthReq(email, password)) else it.login(AuthReq(email, password))
        session.token = r.token
        session.email = email.trim().lowercase()
    }
    fun signOut() = session.clear()

    suspend fun mode() = withApi { it.mode() }
    suspend fun numbers() = withApi { api -> api.numbers().map { it.toDomain() } }
    suspend fun setDefault(id: String) = withApi { it.setDefault(id).toDomain() }
    suspend fun removeNumber(id: String) = withApi { it.remove(id); Unit }
    suspend fun callerName() = withApi { it.callerName().let { n -> CallerName(n.name, n.status) } }

    fun recent(): Flow<List<CallRecord>> = db.calls().recent()
    fun search(q: String): Flow<List<CallRecord>> = db.calls().search(q)
    suspend fun saveCall(r: CallRecord) { db.calls().insert(r) }
    suspend fun clearHistory() = db.calls().clear()
}
