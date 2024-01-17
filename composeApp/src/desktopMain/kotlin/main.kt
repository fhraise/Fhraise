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

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import com.arkivanov.decompose.DefaultComponentContext
import com.arkivanov.decompose.ExperimentalDecomposeApi
import com.arkivanov.decompose.extensions.compose.lifecycle.LifecycleController
import com.arkivanov.essenty.lifecycle.LifecycleRegistry
import data.AppComponentContext
import data.components.AppRootComponent
import ui.pages.Root
import javax.swing.SwingUtilities

@OptIn(ExperimentalDecomposeApi::class)
fun main() {
    val lifecycleRegistry = LifecycleRegistry()

    val rootComponent = runOnUiThread {
        AppRootComponent(
            componentContext = AppComponentContext(componentContext = DefaultComponentContext(lifecycle = lifecycleRegistry))
        )
    }

    application {
        val windowState = rememberWindowState()

        LifecycleController(lifecycleRegistry, windowState)

        Window(onCloseRequest = ::exitApplication, state = windowState, title = "Fhraise") {
            Root(component = rootComponent)
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
