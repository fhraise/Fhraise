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

package xyz.xfqlittlefan.fhraise.proxy

import io.ktor.client.*
import io.ktor.http.*
import io.ktor.server.routing.*
import xyz.xfqlittlefan.fhraise.appConfig
import xyz.xfqlittlefan.fhraise.http.keycloakPath
import xyz.xfqlittlefan.fhraise.http.reverseProxy

private val keycloakProxyClient = HttpClient {
    followRedirects = false
}

val keycloakScheme = appConfig.propertyOrNull("keycloak.scheme")?.getString() ?: "http"
val keycloakHost = appConfig.propertyOrNull("keycloak.host")?.getString() ?: "localhost"
val keycloakPort = appConfig.propertyOrNull("keycloak.port")?.getString()?.toInt() ?: 8080

fun Route.proxyKeycloak() {
    reverseProxy(keycloakPath, keycloakProxyClient) {
        url {
            protocol = URLProtocol.createOrDefault(keycloakScheme)
            host = keycloakHost
            port = keycloakPort
        }
    }
}
