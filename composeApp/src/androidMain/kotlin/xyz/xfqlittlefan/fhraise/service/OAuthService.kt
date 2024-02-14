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

package xyz.xfqlittlefan.fhraise.service

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import androidx.core.app.ServiceCompat
import io.ktor.server.cio.*
import io.ktor.server.engine.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.xfqlittlefan.fhraise.R
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.oauth.OAuthApplication
import xyz.xfqlittlefan.fhraise.platform.AppNotificationChannel
import xyz.xfqlittlefan.fhraise.platform.Notification
import xyz.xfqlittlefan.fhraise.platform.addAction

class OAuthService : Service() {
    companion object {
        val ID = this::class.qualifiedName.hashCode()

        const val ACTION_STOP = "xyz.xfqlittlefan.fhraise.service.OAuthService.ACTION_STOP"

        const val EXTRA_HOST = "host"
        const val EXTRA_PORT = "port"
    }

    private val binder = Binder()

    private val job = SupervisorJob()
    private val scope = CoroutineScope(Dispatchers.IO + job)

    private fun start(host: String, port: Int) {
        runCatching {
            Notification(AppNotificationChannel.OAuthService) {
                setSmallIcon(R.drawable.ic_launcher_foreground)
                setContentTitle(getString(R.string.oauth_service_title))
                setContentText(getString(R.string.oauth_service_description))
                addAction {
                    title = getString(R.string.oauth_service_notification_action_stop)
                    intent = PendingIntent.getService(
                        this@OAuthService, 0, Intent(this@OAuthService, OAuthService::class.java).apply {
                            action = ACTION_STOP
                        }, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                }
            }.let {
                ServiceCompat.startForeground(
                    this,
                    ID,
                    it,
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
                )
            }
            scope.launch {
                embeddedServer(
                    CIO, port = 0,
                    module = OAuthApplication(host, port) {
                        binder.subscriber(Message.Stop(it))
                        stop()
                    }.module,
                ).start().let { binder.subscriber(Message.Start(it)) }
            }
        }.onFailure { it.printStackTrace() }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int) = START_REDELIVER_INTENT.also {
        if (intent?.action == ACTION_STOP) {
            stop()
            return@also
        }
        runCatching {
            start(intent!!.getStringExtra(EXTRA_HOST)!!, intent.getIntExtra(EXTRA_PORT, 0))
        }.onFailure { it.printStackTrace() }
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }

    override fun onBind(intent: Intent?) = binder

    private fun stop() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    sealed class Message {
        data class Start(val server: EmbeddedServer<CIOApplicationEngine, CIOApplicationEngine.Configuration>) :
            Message()

        data class Stop(val tokenPair: JwtTokenPair) : Message()
    }

    class Binder : android.os.Binder() {
        var subscriber: suspend (Message) -> Unit = {}
    }
}
