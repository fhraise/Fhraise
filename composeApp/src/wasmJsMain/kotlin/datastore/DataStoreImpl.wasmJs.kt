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

package datastore

import androidx.datastore.core.DataStore
import androidx.datastore.core.readData
import androidx.datastore.core.writeData
import androidx.datastore.preferences.core.Preferences
import kotlinx.browser.document
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch

actual class PreferencesDataStoreImpl(storage: PreferencesBrowserStorage) : DataStore<Preferences> {
    private val storageConnection: PreferencesBrowserStorageConnection by lazy {
        storage.createConnection()
    }

    @OptIn(DelicateCoroutinesApi::class)
    override val data: Flow<Preferences> = channelFlow {
        document.addEventListener(storageConnection.eventName) {
            GlobalScope.launch(Dispatchers.Default) {
                trySend(storageConnection.readData())
            }
        }
    }

    override suspend fun updateData(transform: suspend (preferences: Preferences) -> Preferences): Preferences {
        val newPreferences = transform(storageConnection.readData().toPreferences())
        storageConnection.writeData(newPreferences)
        return newPreferences
    }
}
