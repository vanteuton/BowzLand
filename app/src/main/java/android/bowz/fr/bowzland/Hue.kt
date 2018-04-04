package android.bowz.fr.bowzland

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.PUT
import retrofit2.http.Path


data class Hue(val error : String,val success : String)

//todo créer une Raspiterface
class HueRequest internal constructor( internal val on: Boolean)


interface HueTrofitInterface {

    @PUT("/api/NVUnYtbjgMZU4enxV569fKf5i0XB07F1O8Dwcwj9/lights/{lightNumber}/state/")
    fun singleLightState(@Path("lightNumber") lightNumber : Int, @Body json : HueRequest) : Call<Array<HueResult>>


    @PUT("/api/NVUnYtbjgMZU4enxV569fKf5i0XB07F1O8Dwcwj9/groups/{roomNumber}/state/")
    fun roomState(@Path("roomNumber") roomNumber : Int, @Body json : HueRequest) : Call<Array<HueResult>>

    companion object Factory {
        fun create(): HueTrofitInterface {
            val interceptor : HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
                this.level = HttpLoggingInterceptor.Level.BODY
            }

            val client : OkHttpClient = OkHttpClient.Builder().apply {
                this.addInterceptor(interceptor)
            }.build()

            val retrofit = Retrofit.Builder()
                    .baseUrl("http://192.168.1.15")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            return retrofit.create(HueTrofitInterface::class.java)
        }
    }

}


interface RaspberryTerface {

    @PUT("/api/")
    fun singleLightState(@Body json : HueRequest) : Call<Array<HueResult>>


    companion object Factory {
        fun create(): RaspberryTerface {
            val interceptor : HttpLoggingInterceptor = HttpLoggingInterceptor().apply {
                this.level = HttpLoggingInterceptor.Level.BODY
            }

            val client : OkHttpClient = OkHttpClient.Builder().apply {
                this.addInterceptor(interceptor)
            }.build()

            val retrofit = Retrofit.Builder()
                    .baseUrl("http://192.168.1.255")
                    .addConverterFactory(GsonConverterFactory.create())
                    .client(client)
                    .build()
            return retrofit.create(RaspberryTerface::class.java)
        }
    }

}

