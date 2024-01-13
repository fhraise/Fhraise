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

package ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

@Composable
fun Theme(colorScheme: ColorScheme, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = colorScheme, content = content)
}

@Composable
fun DayNightTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    Theme(colorScheme = if (dark) darkColorScheme() else lightColorScheme(), content = content)
}

@Composable
expect fun AppTheme(dark: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit)
