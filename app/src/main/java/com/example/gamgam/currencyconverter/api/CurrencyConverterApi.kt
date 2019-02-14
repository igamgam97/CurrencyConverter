package com.example.gamgam.currencyconverter.api

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.example.gamgam.currencyconverter.model.Currency
import io.reactivex.Observable
import okhttp3.Cache
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface CurrencyConverterApi{

    @GET("convert?compact=ultra")
    fun getExchangeRates(@Query("q") q:String): Observable<Map<String,String>>

    @GET("currencies")
    fun getCurrencies():Observable<Currency>

    companion object {
        fun create(context: Context): CurrencyConverterApi {
            val cacheSize = (5 * 1024 * 1024).toLong() //5mb
            val myCache = Cache(context.cacheDir, cacheSize)

            val client : OkHttpClient = OkHttpClient.Builder()
                    .cache(myCache)
                    .addInterceptor{chain ->
                        var request = chain.request()
                        request = if (hasNetwork(context)!!)
                            request.newBuilder().header("Cache-Control", "public, max-age=" + 5).build()
                        else
                            request.newBuilder().header("Cache-Control", "public, only-if-cached, max-stale=" + 60 * 30).build()
                        chain.proceed(request)
                    }.build()
            val retrofit = Retrofit.Builder()
                    .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                    .addConverterFactory(GsonConverterFactory.create())
                    .baseUrl("https://free.currencyconverterapi.com/api/v6/")
                    .client(client)
                    .build()

            return retrofit.create(CurrencyConverterApi::class.java)


        }

        private fun hasNetwork(context: Context): Boolean? {
            var isConnected: Boolean? = false
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork: NetworkInfo? = connectivityManager.activeNetworkInfo
            if (activeNetwork != null && activeNetwork.isConnected)
                isConnected = true
            return isConnected
        }
    }

}
