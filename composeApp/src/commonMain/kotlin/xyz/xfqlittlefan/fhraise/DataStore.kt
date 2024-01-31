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

package xyz.xfqlittlefan.fhraise

import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.arkivanov.decompose.ComponentContext
import kotlinx.coroutines.CoroutineScope
import xyz.xfqlittlefan.fhraise.data.AppComponentContextValues.ColorMode
import xyz.xfqlittlefan.fhraise.data.componentScope
import xyz.xfqlittlefan.fhraise.datastore.PreferenceStateFlow
import xyz.xfqlittlefan.fhraise.datastore.preferencesDataStore
import kotlin.properties.ReadOnlyProperty

private interface Preferences {
    interface PreferencesFactory {
        fun preferences(): ReadOnlyProperty<ComponentContext, Preferences>
    }
}

object SettingsDataStore {
    private val store by preferencesDataStore("settings")

    class Preferences(scope: CoroutineScope) : xyz.xfqlittlefan.fhraise.Preferences {
        companion object : xyz.xfqlittlefan.fhraise.Preferences.PreferencesFactory {
            override fun preferences() =
                ReadOnlyProperty<ComponentContext, Preferences> { thisRef, _ -> Preferences(thisRef.componentScope) }
        }

        val colorMode = PreferenceStateFlow(
            scope,
            store,
            intPreferencesKey("colorMode"),
            { ColorMode.entries[it] },
            ColorMode::ordinal,
            ColorMode.SYSTEM
        )
    }
}

object ServerDataStore {
    private val store by preferencesDataStore("server")

    class Preferences(scope: CoroutineScope) : xyz.xfqlittlefan.fhraise.Preferences {
        companion object : xyz.xfqlittlefan.fhraise.Preferences.PreferencesFactory {
            override fun preferences() =
                ReadOnlyProperty<ComponentContext, Preferences> { thisRef, _ -> Preferences(thisRef.componentScope) }
        }

        val serverHost =
            PreferenceStateFlow(scope, store, stringPreferencesKey("serverHost"), defaultValue = "localhost")
        val serverPort =
            PreferenceStateFlow(scope, store, intPreferencesKey("serverPort"), defaultValue = DefaultServerPort)
    }
}
