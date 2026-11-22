package com.talhastudio.callerid

import android.app.Application
import android.content.Context
import com.talhastudio.callerid.data.AppRepository
import com.talhastudio.callerid.data.SessionStore
import com.talhastudio.callerid.data.local.AppDatabase
import com.talhastudio.callerid.data.provider.BackendTelephonyProvider
import com.talhastudio.callerid.data.provider.NotConfiguredProvider
import com.talhastudio.callerid.data.remote.ApiService
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

class TalhaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        Graph.init(this)
    }
}

/** Tiny service locator. Keeps the project dependency-free; swap for Hilt if the app grows. */
object Graph {
    lateinit var repo: AppRepository
        private set

    fun init(ctx: Context) {
        val session = SessionStore(ctx)
        val url = BuildConfig.BACKEND_URL.trim()
        val api: ApiService? = if (url.isEmpty()) null else buildApi(url, session)
        val provider = if (api == null) NotConfiguredProvider() else BackendTelephonyProvider(api)
        repo = AppRepository(api, provider, AppDatabase.get(ctx), session)
    }

    private fun buildApi(url: String, session: SessionStore): ApiService {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                val t = session.token
                val req = if (t != null) chain.request().newBuilder().header("Authorization", "Bearer $t").build() else chain.request()
                chain.proceed(req)
            }
            .build()
        return Retrofit.Builder()
            .baseUrl(if (url.endsWith("/")) url else "$url/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
