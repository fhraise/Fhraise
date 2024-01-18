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

import androidx.compose.runtime.*
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.browser.window
import org.w3c.dom.Window
import org.w3c.dom.events.Event

actual val windowSizeClass: WindowSizeClass
    @Composable get() {
        var windowSizeClass by remember {
            mutableStateOf(
                WindowSizeClass.calculateFromSize(window.getDpSize()),
            )
        }

        DisposableEffect(Unit) {
            val callback: (Event) -> Unit = {
                windowSizeClass = WindowSizeClass.calculateFromSize(window.getDpSize())
            }

            window.addEventListener("resize", callback)

            onDispose {
                window.removeEventListener("resize", callback)
            }
        }

        return windowSizeClass
    }

private fun Window.getDpSize(): DpSize = DpSize(innerWidth.dp, innerHeight.dp)
