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

/*
 * Copyright 2022 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.datastore.preferences.core

import androidx.datastore.core.DataStore
import kotlinx.atomicfu.atomic

/**
 * Preferences and MutablePreferences are a lot like a generic Map and MutableMap keyed by the
 * Preferences.Key class. These are intended for use with DataStore. Construct a
 * DataStore<Preferences> instance using [PreferenceDataStoreFactory.create].
 */
abstract class Preferences internal constructor() {
    /**
     * Key for values stored in Preferences. Type T is the type of the value associated with the
     * Key.
     *
     * T must be one of the following: Boolean, Int, Long, Float, String, Set<String>.
     *
     * Construct Keys for your data type using: [booleanPreferencesKey], [intPreferencesKey],
     * [longPreferencesKey], [floatPreferencesKey], [stringPreferencesKey], [stringSetPreferencesKey]
     */
    class Key<T>
    internal constructor(val name: String) {
        /**
         * Infix function to create a Preferences.Pair.
         * This is used to support [preferencesOf] and [MutablePreferences.putAll]
         * @param value is the value this preferences key should point to.
         */
        infix fun to(value: T): Pair<T> = Pair(this, value)

        override fun equals(other: Any?): Boolean = if (other is Key<*>) {
            name == other.name
        } else {
            false
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }

        override fun toString(): String = name
    }

    /**
     * Key Value pairs for Preferences. Type T is the type of the value.
     *
     * Construct these using the infix function [to].
     */
    class Pair<T> internal constructor(internal val key: Key<T>, internal val value: T)

    /**
     * Returns true if this Preferences contains the specified key.
     *
     * @param key the key to check for
     */
    abstract operator fun <T> contains(key: Key<T>): Boolean

    /**
     * Get a preference with a key. If the key is not set, returns null.
     *
     * If T is Set<String>, this returns an unmodifiable set which will throw a runtime exception
     * when mutated. Do not try to mutate the returned set.
     *
     * Use [MutablePreferences.set] to change the value of a preference (inside a
     * [DataStore<Preferences>.edit] block).
     *
     * @param T the type of the preference
     * @param key the key for the preference
     * @throws ClassCastException if there is something stored with the same name as [key] but
     * it cannot be cast to T
     */
    abstract operator fun <T> get(key: Key<T>): T?

    /**
     * Retrieve a map of all key preference pairs. The returned map is unmodifiable, and attempts
     * to mutate it will throw runtime exceptions.
     *
     * @return a map containing all the preferences in this Preferences
     */
    abstract fun asMap(): Map<Key<*>, Any>

    /**
     * Gets a mutable copy of Preferences which contains all the preferences in this Preferences.
     * This can be used to update your preferences without building a new Preferences object from
     * scratch in [DataStore.updateData].
     *
     * This is similar to [Map.toMutableMap].
     *
     * @return a MutablePreferences with all the preferences from this Preferences
     */
    fun toMutablePreferences(): MutablePreferences {
        return MutablePreferences(asMap().toMutableMap(), startFrozen = false)
    }

    /**
     * Gets a read-only copy of Preferences which contains all the preferences in this Preferences.
     *
     * This is similar to [Map.toMap].
     *
     * @return a copy of this Preferences
     */
    fun toPreferences(): Preferences {
        return MutablePreferences(asMap().toMutableMap(), startFrozen = true)
    }
}

/**
 * Mutable version of [Preferences]. Allows for creating Preferences with different key-value pairs.
 */
class MutablePreferences internal constructor(
    internal val preferencesMap: MutableMap<Key<*>, Any> = mutableMapOf(), startFrozen: Boolean = true
) : Preferences() {

    /**
     * If frozen, mutating methods will throw.
     */
    private var frozen by atomic(startFrozen)

    internal fun checkNotFrozen() {
        check(!frozen) { "Do mutate preferences once returned to DataStore." }
    }

    /**
     * Causes any future mutations to result in an exception being thrown.
     */
    internal fun freeze() {
        frozen = true
    }

    override operator fun <T> contains(key: Key<T>): Boolean {
        return preferencesMap.containsKey(key)
    }

    override operator fun <T> get(key: Key<T>): T? {
        @Suppress("UNCHECKED_CAST") return when (val value = preferencesMap[key]) {
            is ByteArray -> value.copyOf()
            else -> value
        } as T?
    }

    override fun asMap(): Map<Key<*>, Any> {
        return preferencesMap.entries.associate { entry ->
            when (val value = entry.value) {
                is ByteArray -> Pair(entry.key, value.copyOf())
                else -> Pair(entry.key, entry.value)
            }
        }.toMap()
    }

    // Mutating methods below:

    /**
     * Set a key value pair in MutablePreferences.
     *
     * Example usage:
     * val COUNTER_KEY = intPreferencesKey("counter")
     *
     * // Once edit completes successfully, preferenceStore will contain the incremented counter.
     * preferenceStore.edit { prefs: MutablePreferences ->
     *   prefs\[COUNTER_KEY\] = prefs\[COUNTER_KEY\] :? 0 + 1
     * }
     *
     * @param key the preference to set
     * @param key the value to set the preference to
     */
    operator fun <T> set(key: Key<T>, value: T) {
        setUnchecked(key, value)
    }

    /**
     * Private setter function. The type of key and value *must* be the same.
     */
    internal fun setUnchecked(key: Key<*>, value: Any?) {
        checkNotFrozen()

        when (value) {
            null -> remove(key)
            // Copy set so changes to input don't change Preferences. Wrap in unmodifiableSet so
            // returned instances can't be changed.
            is Set<*> -> preferencesMap[key] = value.toSet()
            is ByteArray -> preferencesMap[key] = value.copyOf()
            else -> preferencesMap[key] = value
        }
    }

    /**
     * Appends or replaces all pairs from [prefs] to this MutablePreferences. Keys in [prefs]
     * will overwrite keys in this Preferences.
     *
     * Example usage:
     * mutablePrefs += preferencesOf(COUNTER_KEY to 100, NAME to "abcdef")
     *
     * @param prefs Preferences to append to this MutablePreferences
     */
    operator fun plusAssign(prefs: Preferences) {
        checkNotFrozen()
        preferencesMap += prefs.asMap()
    }

    /**
     * Appends or replaces all [pair] to this MutablePreferences.
     *
     * Example usage:
     * mutablePrefs += COUNTER_KEY to 100
     *
     * @param pair the Preference.Pair to add to this MutablePreferences
     */
    operator fun plusAssign(pair: Pair<*>) {
        checkNotFrozen()
        putAll(pair)
    }

    /**
     * Removes the preference with the given key from this MutablePreferences. If this
     * Preferences does not contain the key, this is a no-op.
     *
     * Example usage:
     * mutablePrefs -= COUNTER_KEY
     *
     * @param key the key to remove from this MutablePreferences
     */
    operator fun minusAssign(key: Key<*>) {
        checkNotFrozen()
        remove(key)
    }

    /**
     * Appends or replaces all [pairs] to this MutablePreferences.
     *
     * @param pairs the pairs to append to this MutablePreferences
     */
    fun putAll(vararg pairs: Pair<*>) {
        checkNotFrozen()
        pairs.forEach {
            setUnchecked(it.key, it.value)
        }
    }

    /**
     * Remove a preferences from this MutablePreferences.
     *
     * @param key the key to remove this MutablePreferences
     * @return the original value of this preference key.
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> remove(key: Key<T>): T {
        checkNotFrozen()
        return preferencesMap.remove(key) as T
    }

    /* Removes all preferences from this MutablePreferences. */
    fun clear() {
        checkNotFrozen()
        preferencesMap.clear()
    }

    // Equals and hash code for use by DataStore
    override fun equals(other: Any?): Boolean {
        if (other !is MutablePreferences) {
            return false
        }

        if (other.preferencesMap === preferencesMap) return true

        if (other.preferencesMap.size != preferencesMap.size) return false

        return other.preferencesMap.all { otherEntry ->
            preferencesMap[otherEntry.key]?.let { value ->
                when (val otherVal = otherEntry.value) {
                    is ByteArray -> value is ByteArray && otherVal.contentEquals(value)
                    else -> otherVal == value
                }
            } ?: false
        }
    }

    override fun hashCode(): Int {
        return preferencesMap.entries.sumOf { entry ->
            when (val value = entry.value) {
                is ByteArray -> value.contentHashCode()
                else -> value.hashCode()
            }
        }
    }

    /**
     * For better debugging.
     */
    override fun toString(): String = preferencesMap.entries.joinToString(
        separator = ",\n", prefix = "{\n", postfix = "\n}"
    ) { entry ->
        val value = when (val value = entry.value) {
            is ByteArray -> value.joinToString(", ", "[", "]")
            else -> "${entry.value}"
        }
        "  ${entry.key.name} = $value"
    }
}

/**
 * Edit the value in DataStore transactionally in an atomic read-modify-write operation. All
 * operations are serialized.
 *
 * The coroutine completes when the data has been persisted durably to disk (after which
 * [DataStore.data] will reflect the update). If the transform or write to disk fails, the
 * transaction is aborted and an exception is thrown.
 *
 * Note: values that are changed in [transform] are NOT updated in DataStore until after the
 * transform completes. Do not assume that the data has been successfully persisted until after
 * edit returns successfully.
 *
 * Note: do NOT store a reference to the MutablePreferences provided to transform. Mutating this
 * after [transform] returns will NOT change the data in DataStore. Future versions of this may
 * throw exceptions if the MutablePreferences object is mutated outside of [transform].
 *
 * See [DataStore.updateData].
 *
 * Example usage:
 * val COUNTER_KEY = intPreferencesKey("my_counter")
 *
 * dataStore.edit { prefs ->
 *   prefs\[COUNTER_KEY\] = prefs\[COUNTER_KEY\] :? 0 + 1
 * }
 *
 * @param transform block which accepts MutablePreferences that contains all the preferences
 * currently in DataStore. Changes to this MutablePreferences object will be persisted once
 * transform completes.
 * @throws androidx.datastore.core.IOException when an exception is encountered when writing
 * data to disk
 * @throws Exception when thrown by the transform block
 */
suspend fun DataStore<Preferences>.edit(
    transform: suspend (MutablePreferences) -> Unit
): Preferences {
    return this.updateData {
        // It's safe to return MutablePreferences since we freeze it in
        // PreferencesDataStore.updateData()
        it.toMutablePreferences().apply { transform(this) }
    }
}
