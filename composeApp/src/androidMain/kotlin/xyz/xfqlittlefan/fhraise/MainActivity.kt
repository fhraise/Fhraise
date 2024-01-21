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

package xyz.xfqlittlefan.fhraise

import Notification
import Permission
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.arkivanov.decompose.defaultComponentContext
import data.AppComponentContextValues.ColorMode.*
import data.components.AppRootComponent
import ui.pages.Root
import xyz.xfqlittlefan.fhraise.compositionLocals.LocalActivity
import xyz.xfqlittlefan.fhraise.utils.isMiui
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

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

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val verifyCodeChannelId = "verifyCode"
            val verifyCodeChannelName = "模拟验证码"
            val verifyCodeChannelDescription = "模拟验证码的通知"
            val verifyCodeChannelImportance = NotificationManager.IMPORTANCE_HIGH
            val verifyCodeChannel = android.app.NotificationChannel(
                verifyCodeChannelId, verifyCodeChannelName, verifyCodeChannelImportance
            ).apply {
                description = verifyCodeChannelDescription
            }

            val notificationManager = getSystemService(android.app.NotificationManager::class.java)
            notificationManager.createNotificationChannel(verifyCodeChannel)
        }

        var notificationPermissionRequestContinuation: Continuation<Boolean>? = null
        val notificationPermissionRequestLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
                notificationPermissionRequestContinuation?.resumeWith(Result.success(granted))
            }

        Permission.checkNotificationPermissionGranted = {
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU || ContextCompat.checkSelfPermission(
                this, android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }

        Permission.requestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && Permission.checkNotificationPermissionGranted() != true) {
                suspendCoroutine {
                    notificationPermissionRequestContinuation = it
                    notificationPermissionRequestLauncher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
                }
            } else {
                true
            }
        }

        Notification.send = { channel, title, message, priority ->
            val notification = NotificationCompat.Builder(this, "verifyCode").apply {
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

        val rootComponent = AppRootComponent(componentContext = defaultComponentContext())

        setContent {
            val colorMode by rootComponent.colorMode

            LaunchedEffect(colorMode) {
                val systemBarStyle = when (colorMode) {
                    LIGHT -> SystemBarStyle.light(scrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT)
                    DARK -> SystemBarStyle.dark(scrim = Color.TRANSPARENT)
                    SYSTEM -> SystemBarStyle.auto(
                        lightScrim = Color.TRANSPARENT, darkScrim = Color.TRANSPARENT
                    )
                }

                enableEdgeToEdge(
                    statusBarStyle = systemBarStyle,
                    navigationBarStyle = systemBarStyle,
                )
            }

            CompositionLocalProvider(LocalActivity provides this@MainActivity) {
                Root(component = rootComponent)
            }
        }
    }
}
