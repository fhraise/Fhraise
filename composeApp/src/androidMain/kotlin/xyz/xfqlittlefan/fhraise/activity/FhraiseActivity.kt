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
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.runtime.*
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import com.arkivanov.decompose.defaultComponentContext
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
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
import xyz.xfqlittlefan.fhraise.oauth.AndroidStartOAuthApplicationImpl
import xyz.xfqlittlefan.fhraise.permission
import xyz.xfqlittlefan.fhraise.platform.*
import xyz.xfqlittlefan.fhraise.service.OAuthService
import xyz.xfqlittlefan.fhraise.ui.LocalWindowSizeClass
import xyz.xfqlittlefan.fhraise.ui.pages.AppTheme
import xyz.xfqlittlefan.fhraise.ui.windowSizeClass
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
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

        NotificationManagerCompat.from(this).apply {
            AppNotificationChannel.entries.forEach { channel ->
                NotificationChannelCompat.Builder(channel.name, channel.importance).apply {
                    setName(getString(channel.channelName))
                    setDescription(getString(channel.description))
                }.build().let { createNotificationChannel(it) }
            }
        }

        AndroidNotificationImpl.send = { channel, title, message, priority ->
            val notification = NotificationCompat.Builder(this, channel.name).apply {
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentTitle(title)
                setContentText(message)
                setPriority(priority)
                setCategory(NotificationCompat.CATEGORY_MESSAGE)
                setDefaults(NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE)
            }.build()
            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            val id = "$channel:$title:$message:$priority".hashCode()
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
                lifecycleScope.launch(Dispatchers.IO, start = CoroutineStart.UNDISPATCHED) {
                    browserFlow.take { (id, message) -> id == usedId && message == BrowserMessage.Ready }
                    onReady()
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

        AndroidStartOAuthApplicationImpl.start = { host, port, callback ->
            var continuation: Continuation<EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>>? =
                null
            Intent(this, OAuthService::class.java).apply {
                putExtra(OAuthService.EXTRA_HOST, host)
                putExtra(OAuthService.EXTRA_PORT, port)
            }.let { intent ->
                ContextCompat.startForegroundService(this, intent)
                object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
                        if (service !is OAuthService.Binder) error("Invalid service")
                        service.subscriber = { message ->
                            when (message) {
                                is OAuthService.Message.Start -> runCatching {
                                    continuation?.resume(message.server)
                                }.onFailure {
                                    it.printStackTrace()
                                    rootComponent.snackbarHostState.showSnackbar(
                                        message = "出错了，请尝试重新启动认证（${it.localizedMessage}）",
                                        withDismissAction = true,
                                    )
                                }

                                is OAuthService.Message.Stop -> {
                                    unbindService(this)
                                    callback(message.tokenPair)
                                }
                            }
                        }
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {}
                }.let { bindService(intent, it, Context.BIND_AUTO_CREATE) }
            }
            suspendCoroutine { continuation = it }
        }
    }

    fun setContent(content: @Composable RootComponent.() -> Unit) {
        setContentBase {
            rootComponent.AdaptiveColorMode {
                AppTheme {
                    CompositionLocalProvider(LocalActivity provides this@FhraiseActivity) {
                        CompositionLocalProvider(LocalWindowSizeClass provides windowSizeClass) {
                            content()
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun RootComponent.AdaptiveColorMode(content: @Composable RootComponent.() -> Unit) {
        val colorMode by settings.colorMode.collectAsState()

        LaunchedEffect(colorMode) {
            when (colorMode) {
                AppComponentContextValues.ColorMode.LIGHT -> SystemBarStyle.light(
                    scrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT
                )

                AppComponentContextValues.ColorMode.DARK -> SystemBarStyle.dark(scrim = Color.TRANSPARENT)
                AppComponentContextValues.ColorMode.SYSTEM -> SystemBarStyle.auto(
                    lightScrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT
                )
            }.let {
                enableEdgeToEdge(
                    statusBarStyle = it,
                    navigationBarStyle = it,
                )
            }
        }

        content()
    }
}
