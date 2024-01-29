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
import kotlinx.atomicfu.locks.synchronized
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty

actual fun preferencesDataStore(name: String): ReadOnlyProperty<Any?, DataStore<Preferences>> {
    return PreferenceDataStoreSingletonDelegate(name)
}

internal class PreferenceDataStoreSingletonDelegate<T> internal constructor(
    private val name: String
) : ReadOnlyProperty<T, DataStore<Preferences>> {

    private val lock = Any()

    private var INSTANCE: DataStore<Preferences>? = null

    /**
     * Gets the instance of the DataStore.
     *
     * @param thisRef must be an instance of [Context]
     * @param property not used
     */
    override fun getValue(thisRef: T, property: KProperty<*>): DataStore<Preferences> {
        return INSTANCE ?: synchronized(lock) {
            if (INSTANCE == null) {
                INSTANCE = PreferenceDataStoreFactory.create(name)
            }
            INSTANCE!!
        }
    }
}
