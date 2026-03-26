package com.uandcode.hilt.autobind.app.network

import com.uandcode.hilt.autobind.AutoBinds
import com.uandcode.hilt.autobind.app.di.RetrofitBindingFactory
import retrofit2.http.GET
import javax.inject.Singleton

// No need to write a Hilt module manually.
// The @AutoBinds annotation with a factory auto-generates a Hilt @Provides module
// that uses RetrofitBindingFactory to create this Retrofit API implementation.
@AutoBinds(factory = RetrofitBindingFactory::class)
@Singleton
interface RandomImageApi {

    @GET("breeds/image/random")
    suspend fun getRandomImage(): RandomImageResponse

}
