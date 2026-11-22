package com.talhastudio.callerid.data.remote

import com.google.gson.Gson
import com.talhastudio.callerid.core.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.*
import java.io.IOException

const val NOT_CONFIGURED = "Calling service is not configured yet."

class ApiException(message: String, val code: Int = 0) : Exception(message)

data class AuthReq(val email: String, val password: String)
data class AuthRes(val token: String)
data class MeDto(val email: String)
data class ModeRes(val providerConfigured: Boolean, val mode: String)
data class NumberDto(val id: String, val e164: String, val status: String, val isDefault: Boolean, val callerName: String?)
data class AddNumberReq(val e164: String)
data class VerifyReq(val code: String)
data class CallerNameDto(val name: String?, val status: String)
data class SetNameReq(val name: String)
data class CallReq(val to: String, val callerIdId: String)
data class CallDto(val callId: String, val status: String, val durationSec: Int, val testMode: Boolean)
data class BalanceDto(val balance: Double?, val currency: String?)
private data class ErrorBody(val error: String?)

interface ApiService {
    @POST("auth/register") suspend fun register(@Body b: AuthReq): AuthRes
    @POST("auth/login") suspend fun login(@Body b: AuthReq): AuthRes
    @GET("me") suspend fun me(): MeDto
    @GET("mode") suspend fun mode(): ModeRes
    @GET("numbers") suspend fun numbers(): List<NumberDto>
    @POST("numbers") suspend fun addNumber(@Body b: AddNumberReq): NumberDto
    @POST("numbers/{id}/verify") suspend fun verify(@Path("id") id: String, @Body b: VerifyReq): NumberDto
    @POST("numbers/{id}/default") suspend fun setDefault(@Path("id") id: String): NumberDto
    @DELETE("numbers/{id}") suspend fun remove(@Path("id") id: String): Response<Unit>
    @GET("caller-name") suspend fun callerName(): CallerNameDto
    @POST("caller-name") suspend fun setCallerName(@Body b: SetNameReq): CallerNameDto
    @POST("calls") suspend fun startCall(@Body b: CallReq): CallDto
    @GET("calls/{id}") suspend fun callStatus(@Path("id") id: String): CallDto
    @POST("calls/{id}/end") suspend fun endCall(@Path("id") id: String): CallDto
    @GET("balance") suspend fun balance(): BalanceDto
}

/** Runs a network call and converts every failure into a readable ApiException. */
suspend fun <T> safeApi(block: suspend () -> T): Result<T> = try {
    Result.success(withContext(Dispatchers.IO) { block() })
} catch (e: CancellationException) {
    throw e
} catch (e: HttpException) {
    val text = runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    val fromServer = runCatching { Gson().fromJson(text, ErrorBody::class.java)?.error }.getOrNull()
    val msg = fromServer ?: if (e.code() == 401) "Session expired. Sign in again." else "Server error (${e.code()}). Try again."
    Result.failure(ApiException(msg, e.code()))
} catch (e: IOException) {
    Result.failure(ApiException("No connection. Check your internet and try again."))
} catch (e: Exception) {
    Result.failure(ApiException("Something went wrong. Try again."))
}

fun NumberDto.toDomain() = VerifiedNumber(id, e164, status, isDefault, callerName)
fun CallDto.toDomain() = CallSession(callId, CallState.fromWire(status), durationSec, testMode)
