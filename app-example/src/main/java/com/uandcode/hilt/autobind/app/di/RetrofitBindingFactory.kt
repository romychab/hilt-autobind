package com.uandcode.hilt.autobind.app.di

import com.uandcode.hilt.autobind.annotations.factories.AutoScoped
import com.uandcode.hilt.autobind.annotations.factories.ClassBindingFactory
import retrofit2.Retrofit
import javax.inject.Inject
import kotlin.reflect.KClass

class RetrofitBindingFactory @Inject constructor(
    private val retrofit: Retrofit,
) : ClassBindingFactory {

    @AutoScoped
    override fun <T : Any> create(kClass: KClass<T>): T {
        return retrofit.create(kClass.java)
    }

}
