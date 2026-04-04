package dagger.hilt

annotation class DefineComponent(val parent: kotlin.reflect.KClass<*> = Nothing::class) {
    annotation class Builder
}
