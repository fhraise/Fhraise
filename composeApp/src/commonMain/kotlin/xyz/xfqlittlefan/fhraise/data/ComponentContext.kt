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

package xyz.xfqlittlefan.fhraise.data

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arkivanov.decompose.ComponentContext
import xyz.xfqlittlefan.fhraise.SettingsDataStore

interface AppComponentContext : ComponentContext, AppComponentContextValues

interface AppComponentContextValues {
    val settings: SettingsDataStore.Preferences

    fun switchColorMode() {
        settings.colorMode.value = settings.colorMode.value.next
    }

    enum class ColorMode(val displayName: String) {
        LIGHT("亮色"), DARK("暗色"), SYSTEM("跟随系统");

        val next
            get() = ColorMode.entries[(ordinal + 1) % ColorMode.entries.size]
    }

    val pop: State<(() -> Unit)?>

    val snackbarHostState: SnackbarHostState

    // 不能替换为成员，否则会因为未知原因的找不到 setter 而崩溃
    @Composable
    fun SnackbarHost() {
        SnackbarHost(hostState = snackbarHostState)
    }

    suspend fun requestAppNotificationPermission(): Boolean?
}
