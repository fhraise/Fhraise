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

package xyz.xfqlittlefan.fhraise.ui.pages

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.fade
import com.arkivanov.decompose.extensions.compose.stack.animation.plus
import com.arkivanov.decompose.extensions.compose.stack.animation.predictiveback.predictiveBackAnimation
import com.arkivanov.decompose.extensions.compose.stack.animation.scale
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import xyz.xfqlittlefan.fhraise.data.AppComponentContextValues.ColorMode.*
import xyz.xfqlittlefan.fhraise.data.components.RootComponent
import xyz.xfqlittlefan.fhraise.ui.AppTheme
import xyz.xfqlittlefan.fhraise.ui.LocalWindowSizeClass
import xyz.xfqlittlefan.fhraise.ui.pages.root.SignIn
import xyz.xfqlittlefan.fhraise.ui.windowSizeClass

@OptIn(ExperimentalDecomposeApi::class)
@Composable
fun RootComponent.Root() {
    val colorMode by settings.colorMode.collectAsState()

    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
        AppTheme(
            dark = when (colorMode) {
                LIGHT -> false
                DARK -> true
                SYSTEM -> isSystemInDarkTheme()
            },
        ) {
            Children(
                stack = stack,
                animation = predictiveBackAnimation(
                    backHandler = backHandler, fallbackAnimation = stackAnimation(fade() + scale()), onBack = ::onBack
                ),
            ) {
                when (val child = it.instance) {
                    is RootComponent.Child.SignIn -> child.component.SignIn()
                }
            }

            if (showNotificationPermissionDialog) {
                AlertDialog(
                    onDismissRequest = ::cancelNotificationPermissionRequest,
                    title = { Text("请授予通知权限") },
                    text = { Text("开启通知权限，及时接收最新消息") },
                    confirmButton = {
                        Button(onClick = ::startNotificationPermissionRequest) {
                            Text("确定")
                        }
                    },
                    dismissButton = {
                        Button(onClick = ::cancelNotificationPermissionRequest) {
                            Text("取消")
                        }
                    },
                )
            }
        }
    }
}
