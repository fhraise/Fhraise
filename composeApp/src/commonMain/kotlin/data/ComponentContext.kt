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

package data

import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import com.arkivanov.decompose.ComponentContext

interface AppComponentContext : ComponentContext, AppComponentContextValues

interface AppComponentContextValues {
    val colorMode: State<ColorMode>
    fun changeColorMode(colorMode: ColorMode)

    enum class ColorMode(val displayName: String) {
        LIGHT("亮色"), DARK("暗色"), SYSTEM("跟随系统")
    }

    val pop: State<(() -> Unit)?>

    val snackbarHostState: SnackbarHostState

    @Composable
    fun SnackbarHost() {
        SnackbarHost(hostState = snackbarHostState)
    }

    suspend fun requestAppNotificationPermission(): Boolean?
}
