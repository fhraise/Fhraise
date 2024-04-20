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

package xyz.xfqlittlefan.fhraise.platform

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import androidx.annotation.StringRes
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import xyz.xfqlittlefan.fhraise.R

enum class AppNotificationChannel(
    @StringRes val channelName: Int, @StringRes val description: Int, val importance: Int
) {
    OAuthService(
        R.string.oauth_service_title, R.string.oauth_service_description, NotificationManager.IMPORTANCE_DEFAULT
    ),
}

object AndroidNotificationImpl {
    lateinit var send: (channel: AppNotificationChannel, title: String, message: String, priority: Int) -> Unit
}

inline fun Context.Notification(channel: AppNotificationChannel, block: NotificationCompat.Builder.() -> Unit = {}) =
    NotificationCompat.Builder(this, channel.name).apply(block).build()

inline fun NotificationCompat.Builder.addAction(block: NotificationActionBuilder.() -> Unit = {}) {
    addAction(NotificationActionBuilder().apply(block).build())
}

class NotificationActionBuilder {
    var icon: IconCompat? = null
    var title: CharSequence? = null
    var intent: PendingIntent? = null

    fun build() = NotificationCompat.Action.Builder(icon, title, intent).build()
}
