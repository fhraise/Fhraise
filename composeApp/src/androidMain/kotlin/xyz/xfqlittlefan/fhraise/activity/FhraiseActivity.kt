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

package xyz.xfqlittlefan.fhraise.activity

import android.Manifest
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.core.app.NotificationCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import xyz.xfqlittlefan.fhraise.*
import xyz.xfqlittlefan.fhraise.R
import xyz.xfqlittlefan.fhraise.browser.BrowserActivity
import xyz.xfqlittlefan.fhraise.browser.BrowserMessage
import xyz.xfqlittlefan.fhraise.browser.browserFlow
import xyz.xfqlittlefan.fhraise.browser.browserFlowId
import xyz.xfqlittlefan.fhraise.compositionLocals.LocalActivity
import xyz.xfqlittlefan.fhraise.data.AppComponentContextValues
import xyz.xfqlittlefan.fhraise.data.components.AppRootComponent
import xyz.xfqlittlefan.fhraise.data.components.RootComponent
import xyz.xfqlittlefan.fhraise.datastore.AndroidPreferencesDataStoreImpl
import xyz.xfqlittlefan.fhraise.platform.AndroidUrlImpl
import xyz.xfqlittlefan.fhraise.platform.BrowserActions
import xyz.xfqlittlefan.fhraise.ui.AppTheme
import xyz.xfqlittlefan.fhraise.ui.LocalWindowSizeClass
import xyz.xfqlittlefan.fhraise.ui.windowSizeClass
import androidx.activity.compose.setContent as setContentBase

open class FhraiseActivity : ComponentActivity() {
    companion object {
        private var _rootComponent: RootComponent? = null
        private val rootComponent get() = _rootComponent!!
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            navigationBarStyle = SystemBarStyle.auto(lightScrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT)
        )

        @Suppress("DEPRECATION") if (isMiui) {
            window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS, WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS
            )
            window.setFlags(
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION,
                WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION
            )
        }

        super.onCreate(savedInstanceState)

        if (_rootComponent == null) _rootComponent = AppRootComponent(componentContext = defaultComponentContext())
    }

    fun initialize() {
        installSplashScreen()

        AndroidPreferencesDataStoreImpl.get = { applicationContext.it() }

        val notificationPermission =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) permission(Manifest.permission.POST_NOTIFICATIONS) else null

        AndroidNotificationImpl.send = { channel, title, message, priority ->
            val notification = NotificationCompat.Builder(this, channel).apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentTitle(title)
                setContentText(message)
                setPriority(priority)
                setCategory(NotificationCompat.CATEGORY_MESSAGE)
                setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            }.build()
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            val id =
                (channel.hashCode() shl 48) or (title.hashCode() shl 32) or (message.hashCode() shl 16) or priority.hashCode()
            notificationManager.notify(id, notification)
        }

        AndroidPermissionImpl.checkNotificationPermissionGranted =
            notificationPermission?.run { { granted } } ?: { true }

        AndroidPermissionImpl.requestNotificationPermission = notificationPermission?.run {
            registerRequestLauncher { it }
        } ?: { true }

        AndroidUrlImpl.openInApp = { url, builder ->
            val usedId = browserFlowId

            val startActivity: (onReady: suspend CoroutineScope.() -> Unit) -> Unit = { onReady ->
                lifecycleScope.launch(Dispatchers.IO) {
                    browserFlow.filter { (id, message) -> id == usedId && message == BrowserMessage.Ready }.collect {
                        onReady()
                        cancel()
                    }
                }
                startActivity(Intent(this, BrowserActivity::class.java).apply {
                    putExtra(BrowserActivity.EXTRA_ID, usedId)
                })
            }

            startActivity {
                browserFlow.emit(usedId to BrowserMessage.Launch {
                    CustomTabsIntent.Builder().apply(builder).build().launchUrl(this, Uri.parse(url))
                })
            }

            BrowserActions().apply {
                close = { browserFlow.tryEmit(usedId to BrowserMessage.Close) }
            }
        }
    }

    fun setContent(content: @Composable RootComponent.() -> Unit) {
        setContentBase {
            rootComponent.AdaptiveColorMode { colorMode ->
                CompositionLocalProvider(LocalActivity provides this@FhraiseActivity) {
                    CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                        AppTheme(
                            dark = when (colorMode) {
                                AppComponentContextValues.ColorMode.LIGHT -> false
                                AppComponentContextValues.ColorMode.DARK -> true
                                AppComponentContextValues.ColorMode.SYSTEM -> isSystemInDarkTheme()
                            },
                        ) {
                            content()

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
            }
        }
    }

    @Composable
    fun RootComponent.AdaptiveColorMode(content: @Composable RootComponent.(colorMode: AppComponentContextValues.ColorMode) -> Unit) {
        val colorMode by settings.colorMode.collectAsState()

        LaunchedEffect(colorMode) {
            val systemBarStyle = when (colorMode) {
                AppComponentContextValues.ColorMode.LIGHT -> SystemBarStyle.light(
                    scrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT
                )

                AppComponentContextValues.ColorMode.DARK -> SystemBarStyle.dark(scrim = Color.TRANSPARENT)
                AppComponentContextValues.ColorMode.SYSTEM -> SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT
                )
            }

            enableEdgeToEdge(
                statusBarStyle = systemBarStyle,
                navigationBarStyle = systemBarStyle,
            )
        }

        content(colorMode)
    }
}
