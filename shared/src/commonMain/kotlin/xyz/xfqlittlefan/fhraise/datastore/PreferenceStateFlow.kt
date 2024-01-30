/*
 * This file is part of Fhraise.
 * Copyright (c) 2024 HSAS Foodies. All Rights Reserved.
 *
 * Fhraise is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option)
 * any later version.
 *
 * Fhraise is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with Fhraise. If not, see <https://www.gnu.org/licenses/>.
 */

package xyz.xfqlittlefan.fhraise.datastore

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

open class PreferenceStateFlowBase<T> internal constructor(protected val base: MutableStateFlow<T>) :
    MutableStateFlow<T> by base

open class PreferenceStateFlow<K, V>(
    private val scope: CoroutineScope,
    private val store: DataStore<Preferences>,
    private val key: Preferences.Key<K>,
    @Suppress("UNCHECKED_CAST") transform: (K) -> V = { it as V },
    @Suppress("UNCHECKED_CAST") private val restore: (V) -> K = { it as K },
    defaultValue: V
) : PreferenceStateFlowBase<V>(MutableStateFlow(defaultValue)), ReadWriteProperty<Any?, V> {
    init {
        scope.launch {
            store.data[key].transform(transform).map { it ?: defaultValue }.collect { base.value = it }
        }
    }

    override var value
        get() = base.value
        set(value) = updateValue(value)

    private fun updateValue(value: V) {
        scope.launch {
            store.edit { it[key] = value.let(restore) }
        }
    }

    override fun getValue(thisRef: Any?, property: KProperty<*>) = value

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: V) {
        this.value = value
    }
}

private operator fun <T> Flow<Preferences>.get(key: Preferences.Key<T>) = map { it[key] }
private operator fun <T, R> Flow<T?>.invoke(transform: (T) -> R?) = this.transform(transform)
private fun <T, R> Flow<T?>.transform(transform: (T) -> R?) = map { it?.let { transform(it) } }
