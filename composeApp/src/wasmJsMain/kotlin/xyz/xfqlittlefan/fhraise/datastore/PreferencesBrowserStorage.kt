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

import androidx.datastore.core.*
import androidx.datastore.preferences.core.*
import await
import io.ktor.util.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.flow
import org.khronos.webgl.Int8Array
import push
import kotlin.js.Promise

internal const val preferencesVersion = 1

internal typealias JsPreferences = JsArray<Preference>

internal external object PreferenceType : JsAny {
    val INT: Int
    val DOUBLE: Int
    val STRING: Int
    val BOOLEAN: Int
    val FLOAT: Int
    val LONG: Int
    val STRING_SET: Int
    val BYTE_ARRAY: Int
}

internal external class Preference(key: String, value: JsAny, type: Int) : JsAny {
    val key: String
    val value: JsAny
    val type: Int
}

internal external class PreferencesStorage(name: String, version: Int) : JsAny {
    val eventName: String
    internal val preferences: JsPreferences

    fun write(preferences: JsPreferences): Promise<JsAny>

    fun unsubscribe()
}

class PreferencesBrowserStorage(private val name: String) : Storage<Preferences> {
    override fun createConnection(): PreferencesBrowserStorageConnection = PreferencesBrowserStorageConnection(name)
}

class PreferencesBrowserStorageConnection(name: String) : StorageConnection<Preferences> {
    private val storage = PreferencesStorage(name, preferencesVersion)
    val eventName = storage.eventName

    override suspend fun <R> readScope(block: suspend ReadScope<Preferences>.(locked: Boolean) -> R): R {
        return block(PreferencesBrowserReadScope(storage), false)
    }

    override suspend fun writeScope(block: suspend WriteScope<Preferences>.() -> Unit) {
        return block(PreferencesBrowserWriteScope(storage))
    }

    override val coordinator = object : InterProcessCoordinator {
        private var version by atomic(-2147483648)

        override val updateNotifications = flow<Unit> {}
        override suspend fun <T> lock(block: suspend () -> T) = block()
        override suspend fun <T> tryLock(block: suspend (Boolean) -> T) = block(true)
        override suspend fun getVersion() = version
        override suspend fun incrementAndGetVersion() = ++version
    }

    override fun close() = storage.unsubscribe()
}

internal class PreferencesBrowserReadScope(private val storage: PreferencesStorage) : ReadScope<Preferences> {
    override suspend fun readData() = storage.preferences.toPreferences()
    override fun close() = Unit
}

internal class PreferencesBrowserWriteScope(private val storage: PreferencesStorage) : WriteScope<Preferences> {
    override suspend fun writeData(value: Preferences) {
        storage.write(value.toJsPreferences()).await()
    }

    override suspend fun readData() = storage.preferences.toPreferences()
    override fun close() = Unit
}

internal fun JsPreferences.toPreferences(): Preferences {
    val preferences = mutablePreferencesOf()
    for (i in 0 until length) {
        val preference = get(i)
        preference?.let {
            val name = it.key
            when (val type = it.type) {
                PreferenceType.INT -> preferences[intPreferencesKey(name)] = it.value.unsafeCast<JsNumber>().toInt()
                PreferenceType.DOUBLE -> preferences[doublePreferencesKey(name)] =
                    it.value.unsafeCast<JsNumber>().toDouble()

                PreferenceType.STRING -> preferences[stringPreferencesKey(name)] =
                    it.value.unsafeCast<JsString>().toString()

                PreferenceType.BOOLEAN -> preferences[booleanPreferencesKey(name)] =
                    it.value.unsafeCast<JsBoolean>().toBoolean()

                PreferenceType.FLOAT -> preferences[floatPreferencesKey(name)] =
                    it.value.unsafeCast<JsNumber>().toDouble().toFloat()

                PreferenceType.BYTE_ARRAY -> preferences[byteArrayPreferencesKey(name)] =
                    it.value.unsafeCast<Int8Array>().toByteArray()

                else -> throw IllegalStateException("PreferencesSerializer does not support type: $type")
            }
        }
    }
    return preferences
}

internal fun Preferences.toJsPreferences(): JsPreferences {
    val jsPreferences = JsArray<Preference>()
    for ((key, value) in asMap()) {
        val preference = when (value) {
            is Int -> Preference(key.name, value.toJsNumber(), PreferenceType.INT)
            is Double -> Preference(key.name, value.toJsNumber(), PreferenceType.DOUBLE)
            is String -> Preference(key.name, value.toJsString(), PreferenceType.STRING)
            is Boolean -> Preference(key.name, value.toJsBoolean(), PreferenceType.BOOLEAN)
            is Float -> Preference(key.name, value.toDouble().toJsNumber(), PreferenceType.FLOAT)
            is ByteArray -> Preference(key.name, value.toJsArray(), PreferenceType.BYTE_ARRAY)
            else -> throw IllegalStateException("PreferencesSerializer does not support type: ${value::class.simpleName}")
        }
        jsPreferences.push(preference)
    }
    return jsPreferences
}
