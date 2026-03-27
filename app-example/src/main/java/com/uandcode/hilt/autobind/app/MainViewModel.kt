package com.uandcode.hilt.autobind.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.elveum.container.pendingContainer
import com.elveum.container.subject.LazyFlowSubject
import com.elveum.container.subject.listenReloadable
import com.elveum.container.subject.reloadAsync
import com.uandcode.hilt.autobind.app.greeter.Greeter
import com.uandcode.hilt.autobind.app.network.RandomImageApi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val randomImageApi: RandomImageApi,
    greeter: Greeter,
) : ViewModel() {

    private val subject = LazyFlowSubject.create {
        delay(DELAY_TIMEOUT)
        emit(randomImageApi.getRandomImage())
    }

    val responseFlow = subject.listenReloadable()
        .stateIn(
            scope = viewModelScope,
            started = DefaultSharingStarted,
            initialValue = pendingContainer(),
        )

    val greeting: String = greeter.greet("Hilt AutoBind")

    fun loadImage() {
        subject.reloadAsync()
    }

}

private val DefaultSharingStarted = SharingStarted.WhileSubscribed(SUBSCRIPTION_TIMEOUT, SUBSCRIPTION_TIMEOUT)

private const val DELAY_TIMEOUT = 1000L
private const val SUBSCRIPTION_TIMEOUT = 5000L
