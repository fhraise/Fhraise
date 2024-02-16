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

package xyz.xfqlittlefan.fhraise.oauth

import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.platform.bringWindowToFront
import xyz.xfqlittlefan.fhraise.platform.openUrl
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.time.Duration.Companion.minutes

expect val sendDeepLink: Boolean

@OptIn(ExperimentalCoroutinesApi::class)
actual suspend fun CoroutineScope.oAuthSignIn(host: String, port: Int, provider: Api.OAuth.Provider) = coroutineScope {
    withContext(Dispatchers.IO) {
        val channel = Channel<JwtTokenPair?>()

        val server = startOAuthApplication(host, port) {
            bringWindowToFront()
            channel.send(it)
        }

        val actions = openUrl {
            takeFrom(Api.OAuth.PATH)
            this.port = server.engine.resolvedConnectors().first().port
            parameters.apply {
                append(Api.OAuth.Query.PROVIDER, provider.brokerName)
            }
        }

        val clean = {
            actions.close()
            server.stop()
        }

        select {
            channel.onReceiveCatching {
                clean()
                it.getOrNull()
            }
            onTimeout(5.minutes) {
                clean()
                null
            }
        }
    }
}
