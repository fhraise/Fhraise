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

import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.cbor.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.cio.*
import io.ktor.server.config.*
import io.ktor.server.engine.*
import io.ktor.server.plugins.callid.*
import io.ktor.server.plugins.contentnegotiation.*
import io.ktor.server.plugins.ratelimit.*
import io.ktor.server.resources.*
import io.ktor.server.routing.*
import io.ktor.server.websocket.*
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import xyz.xfqlittlefan.fhraise.api.apiAuth
import xyz.xfqlittlefan.fhraise.api.apiOAuth
import xyz.xfqlittlefan.fhraise.api.appAuth
import xyz.xfqlittlefan.fhraise.api.registerAppCodeVerification
import xyz.xfqlittlefan.fhraise.models.cleanupVerificationCodes
import xyz.xfqlittlefan.fhraise.proxy.proxyKeycloak

fun main() {
    embeddedServer(CIO, port = defaultServerPort, host = "0.0.0.0", module = Application::module).start(wait = true)
}

@OptIn(ExperimentalSerializationApi::class)
fun Application.module() {
    install(CallId) { generate() }

    install(Resources)

    install(RateLimit) {
        registerAppCodeVerification()
    }

    authentication {
        appAuth()
    }

    install(ContentNegotiation) { cbor() }

    install(WebSockets) {
        contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
    }

    cleanupVerificationCodes()

    routing {
        proxyKeycloak()
        apiAuth()
        apiOAuth()
    }
}

val appConfig = ApplicationConfig("application.yaml")
val appSecret = ApplicationConfig("secret.yaml")
