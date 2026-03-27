package com.uandcode.hilt.autobind

/**
 * Specifies which Hilt component the generated module should be installed in.
 *
 * Each entry corresponds to a standard Dagger Hilt component.
 *
 * Use [Unspecified] (the default) to let the processor auto-detect the component
 * from the scope annotation on the class. If the class has no scope annotation,
 * [SingletonComponent][Singleton] is used as the fallback.
 *
 * @param componentClass fully qualified class name of the Hilt component,
 *   or empty string for [Unspecified].
 * @param scopeClass fully qualified class name of the corresponding scope annotation,
 *   or empty string for [Unspecified].
 */
public enum class HiltComponent(
    public val componentClass: String,
    public val scopeClass: String,
) {

    /**
     * Auto-detect the component from the scope annotation on the annotated class.
     * Falls back to [Singleton] if the class is unscoped.
     */
    Unspecified(
        componentClass = "",
        scopeClass = "",
    ),

    /** Scoped to the application lifetime. */
    Singleton(
        componentClass = "dagger.hilt.components.SingletonComponent",
        scopeClass = "javax.inject.Singleton",
    ),

    /** Scoped to the activity-retained lifecycle (survives configuration changes). */
    ActivityRetained(
        componentClass = "dagger.hilt.android.components.ActivityRetainedComponent",
        scopeClass = "dagger.hilt.android.scopes.ActivityRetainedScoped",
    ),

    /** Scoped to a single activity instance. */
    Activity(
        componentClass = "dagger.hilt.android.components.ActivityComponent",
        scopeClass = "dagger.hilt.android.scopes.ActivityScoped",
    ),

    /** Scoped to an `androidx.lifecycle.ViewModel`. */
    ViewModel(
        componentClass = "dagger.hilt.android.components.ViewModelComponent",
        scopeClass = "dagger.hilt.android.scopes.ViewModelScoped",
    ),

    /** Scoped to a single fragment instance. */
    Fragment(
        componentClass = "dagger.hilt.android.components.FragmentComponent",
        scopeClass = "dagger.hilt.android.scopes.FragmentScoped",
    ),

    /** Scoped to a single Android view. */
    View(
        componentClass = "dagger.hilt.android.components.ViewComponent",
        scopeClass = "dagger.hilt.android.scopes.ViewScoped",
    ),

    /**
     * Scoped to a view that is associated with a fragment.
     *
     * Note: `@ViewScoped` auto-detection resolves to [View], not [ViewWithFragment],
     * because both components share the same scope annotation.
     * Use `installIn = HiltComponent.ViewWithFragment` explicitly when targeting this component.
     */
    ViewWithFragment(
        componentClass = "dagger.hilt.android.components.ViewWithFragmentComponent",
        scopeClass = "dagger.hilt.android.scopes.ViewScoped",
    ),

    /** Scoped to a single Android service. */
    Service(
        componentClass = "dagger.hilt.android.components.ServiceComponent",
        scopeClass = "dagger.hilt.android.scopes.ServiceScoped",
    ),
}
