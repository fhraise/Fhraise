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

package ui.pages

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import data.components.RootComponent
import ui.AppTheme
import ui.LocalWindowSizeClass
import ui.pages.root.SignIn
import ui.windowSizeClass

@Composable
fun Root(component: RootComponent) {
    val colorMode by component.colorMode.subscribeAsState()

    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        AppTheme(
            dark = when (colorMode) {
                RootComponent.ColorMode.LIGHT -> false
                RootComponent.ColorMode.DARK -> true
                RootComponent.ColorMode.SYSTEM -> isSystemInDarkTheme()
            },
        ) {
            Children(
                stack = component.stack,
                animation = stackAnimation(fade() + scale()),
            ) {
                when (val child = it.instance) {
                    is RootComponent.Child.SignIn -> SignIn(component = child.component)
                }
            }
        }
    }
}