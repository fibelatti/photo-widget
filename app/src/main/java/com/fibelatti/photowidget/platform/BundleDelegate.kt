@file:Suppress("Unused")

package com.fibelatti.photowidget.platform

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> bundleDelegate(
    key: String? = null,
    default: T? = null,
): ReadWriteProperty<Bundle, T> = BundleDelegate(key = key, default = default)

fun <T> intentExtras(
    key: String? = null,
    default: T? = null,
): ReadWriteProperty<Intent, T> = bundleDelegate(key = key, default = default).map(
    postWrite = Intent::replaceExtras,
    mapper = Intent::ensureExtras,
)

fun <T> activityIntent(
    key: String? = null,
    default: T? = null,
): ReadWriteProperty<Activity, T> = intentExtras(key = key, default = default).map(
    postWrite = Activity::setIntent,
    mapper = Activity::getIntent,
)

fun <T> fragmentArgs(
    key: String? = null,
    default: T? = null,
): ReadWriteProperty<Fragment, T> = bundleDelegate(key = key, default = default).map(
    mapper = Fragment::ensureArgs,
)

@Suppress("UNCHECKED_CAST")
fun <T> SavedStateHandle.savedState(
    key: String? = null,
    default: T? = null,
): ReadOnlyProperty<ViewModel, T> = ReadOnlyProperty { _, property ->
    get<T>(key ?: property.name) ?: default as T
}

fun <T> Bundle.asDelegate(
    key: String? = null,
    default: T? = null,
): ReadWriteProperty<Any?, T> = bundleDelegate(key = key, default = default).map(
    mapper = { this },
)

private val Intent.ensureExtras: Bundle get() = extras ?: Bundle().also(::putExtras)

private val Fragment.ensureArgs: Bundle get() = arguments ?: Bundle().also(::setArguments)

private fun <In, Out, T> ReadWriteProperty<In, T>.map(
    mapper: (Out) -> In,
    postWrite: ((Out, In) -> Unit)? = null,
): ReadWriteProperty<Out, T> = MappedDelegate(source = this, mapper = mapper, postWrite = postWrite)

private class BundleDelegate<T>(
    private val key: String? = null,
    private val default: T? = null,
) : ReadWriteProperty<Bundle, T> {

    @Suppress("UNCHECKED_CAST", "DEPRECATION")
    override operator fun getValue(
        thisRef: Bundle,
        property: KProperty<*>,
    ): T = (thisRef.get(key ?: property.name) ?: default) as T

    override fun setValue(
        thisRef: Bundle,
        property: KProperty<*>,
        value: T,
    ) = thisRef.putAll(bundleOf((key ?: property.name) to value))
}

private class MappedDelegate<In, Out, T>(
    private val source: ReadWriteProperty<In, T>,
    private val mapper: (Out) -> In,
    private val postWrite: ((Out, In) -> Unit)? = null,
) : ReadWriteProperty<Out, T> {

    override fun getValue(thisRef: Out, property: KProperty<*>): T = source.getValue(
        thisRef = mapper(thisRef),
        property = property,
    )

    override fun setValue(thisRef: Out, property: KProperty<*>, value: T) {
        val mapped = mapper(thisRef)
        source.setValue(thisRef = mapped, property = property, value = value)
        postWrite?.invoke(thisRef, mapped)
    }
}
