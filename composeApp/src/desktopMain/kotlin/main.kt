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
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import compositionLocals.LocalWindowSize
import data.components.AppRootComponent
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.pages.Root
import javax.swing.SwingUtilities

@OptIn(ExperimentalDecomposeApi::class, ExperimentalResourceApi::class)
fun main() {
    val lifecycleRegistry = LifecycleRegistry()

    val rootComponent = runOnUiThread {
        AppRootComponent(
            componentContext = DefaultComponentContext(lifecycle = lifecycleRegistry)
        )
    }

    application {
        val windowState = rememberWindowState(size = DpSize(width = 1000.dp, height = 800.dp))
        val trayState = rememberTrayState()

        Notification.send = { title, message, type ->
            trayState.sendNotification(Notification(title, message, type))
        }

        Tray(
            icon = painterResource(DrawableResource("drawable/fhraise_logo.xml")),
            state = trayState,
            tooltip = "Fhraise",
        )

        LifecycleController(lifecycleRegistry, windowState)

        Window(
            onCloseRequest = ::exitApplication,
            state = windowState,
            title = "Fhraise",
            icon = painterResource(DrawableResource("drawable/fhraise_logo.xml")),
        ) {
            CompositionLocalProvider(LocalWindowSize provides windowState.size) {
                Root(component = rootComponent)
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
