package com.talhastudio.callerid.data.provider

import com.talhastudio.callerid.core.*
import com.talhastudio.callerid.data.remote.*

/**
 * Everything the app needs from a telecom provider. The phone never talks to the provider directly:
 * [BackendTelephonyProvider] calls our backend, which holds the credentials and runs the real adapter.
 */
interface TelephonyProvider {
    suspend fun requestCallerIdVerification(e164: String): Result<VerifiedNumber>
    suspend fun verifyCallerId(numberId: String, code: String): Result<VerifiedNumber>
    suspend fun setApprovedCallerName(name: String): Result<CallerName>
    suspend fun makeCall(to: String, callerIdId: String): Result<CallSession>
    suspend fun getCallStatus(callId: String): Result<CallSession>
    suspend fun endCall(callId: String): Result<CallSession>
    suspend fun getAccountBalance(): Result<Balance>
}

class BackendTelephonyProvider(private val api: ApiService) : TelephonyProvider {
    override suspend fun requestCallerIdVerification(e164: String) = safeApi { api.addNumber(AddNumberReq(e164)).toDomain() }
    override suspend fun verifyCallerId(numberId: String, code: String) = safeApi { api.verify(numberId, VerifyReq(code)).toDomain() }
    override suspend fun setApprovedCallerName(name: String) = safeApi { api.setCallerName(SetNameReq(name)).let { CallerName(it.name, it.status) } }
    override suspend fun makeCall(to: String, callerIdId: String) = safeApi { api.startCall(CallReq(to, callerIdId)).toDomain() }
    override suspend fun getCallStatus(callId: String) = safeApi { api.callStatus(callId).toDomain() }
    override suspend fun endCall(callId: String) = safeApi { api.endCall(callId).toDomain() }
    override suspend fun getAccountBalance() = safeApi { api.balance().let { Balance(it.balance, it.currency) } }
}

/** Used when no backend URL was set at build time. Every action fails with a clear message. */
class NotConfiguredProvider : TelephonyProvider {
    private fun <T> fail(): Result<T> = Result.failure(ApiException(NOT_CONFIGURED))
    override suspend fun requestCallerIdVerification(e164: String) = fail<VerifiedNumber>()
    override suspend fun verifyCallerId(numberId: String, code: String) = fail<VerifiedNumber>()
    override suspend fun setApprovedCallerName(name: String) = fail<CallerName>()
    override suspend fun makeCall(to: String, callerIdId: String) = fail<CallSession>()
    override suspend fun getCallStatus(callId: String) = fail<CallSession>()
    override suspend fun endCall(callId: String) = fail<CallSession>()
    override suspend fun getAccountBalance() = fail<Balance>()
}
