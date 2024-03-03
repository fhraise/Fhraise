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

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import fhraise.`compose-app`.generated.resources.Res
import fhraise.`compose-app`.generated.resources.fhraise_logo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import xyz.xfqlittlefan.fhraise.compositionLocals.LocalWindowSize
import xyz.xfqlittlefan.fhraise.data.components.AppRootComponent
import xyz.xfqlittlefan.fhraise.datastore.preferencesDataStore
import xyz.xfqlittlefan.fhraise.platform.Notification
import xyz.xfqlittlefan.fhraise.platform.WindowEvent
import xyz.xfqlittlefan.fhraise.platform.windowFlow
import xyz.xfqlittlefan.fhraise.ui.pages.ThemedRoot
import javax.swing.SwingUtilities

val windowDataStore by preferencesDataStore(name = "window")

@OptIn(ExperimentalDecomposeApi::class, ExperimentalResourceApi::class)
fun main() {
    val lifecycleRegistry = LifecycleRegistry()

    val rootComponent = runOnUiThread {
        AppRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycleRegistry)
        )
    }

    val windowX = runBlocking { windowDataStore.data.map { it[doublePreferencesKey("x")] }.first() }?.dp
    val windowY =
        windowX?.let { runBlocking { windowDataStore.data.map { it[doublePreferencesKey("y")] }.first() } }?.dp
    val windowWidth =
        runBlocking { windowDataStore.data.map { it[doublePreferencesKey("width")] ?: 1000.0 }.first() }.dp
    val windowHeight =
        runBlocking { windowDataStore.data.map { it[doublePreferencesKey("height")] ?: 800.0 }.first() }.dp

    application {
        val windowState = rememberWindowState(
            position = if (windowX != null && windowY != null) WindowPosition.Absolute(
                x = windowX, y = windowY
            ) else WindowPosition.PlatformDefault,
            size = DpSize(width = windowWidth, height = windowHeight),
        )
        val trayState = rememberTrayState()

        Notification.send = { title, message, type ->
            trayState.sendNotification(Notification(title, message, type))
        }

        Tray(
            icon = painterResource(Res.drawable.fhraise_logo),
            state = trayState,
            tooltip = "Fhraise",
        )

        LifecycleController(lifecycleRegistry, windowState)

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Fhraise",
            icon = painterResource(Res.drawable.fhraise_logo),
        ) {
            CompositionLocalProvider(LocalWindowSize provides windowState.size) {
                rootComponent.ThemedRoot()
            }

            LaunchedEffect(Unit) {
                windowFlow.collect {
                    when (it) {
                        WindowEvent.BRING_TO_FRONT -> window.toFront()
                    }
                }
            }
        }

        LaunchedEffect(windowState.position) {
            windowDataStore.edit {
                it[doublePreferencesKey("x")] = windowState.position.x.value.toDouble()
                it[doublePreferencesKey("y")] = windowState.position.y.value.toDouble()
            }
        }

        LaunchedEffect(windowState.size) {
            windowDataStore.edit {
                it[doublePreferencesKey("width")] = windowState.size.width.value.toDouble()
                it[doublePreferencesKey("height")] = windowState.size.height.value.toDouble()
            }
        }
    }
}

private fun <T> runOnUiThread(block: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) {
        return block()
    }

    var result: T? = null

    SwingUtilities.invokeAndWait {
        result = block()
    }

    return result!!
}
