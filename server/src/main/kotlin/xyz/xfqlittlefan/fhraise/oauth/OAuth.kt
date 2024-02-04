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

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.http.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.serialization.Serializable

/**
 * OAuth 数据
 *
 * [Pair] 的第一个元素是请求 OAuth 的 callId，第二个元素是用户 ID，如果认证失败则为 null
 */
typealias OAuthData = Pair<String, String?>

val oAuthFlow = MutableSharedFlow<OAuthData>()

data class OAuthUserPrincipal(val id: String, val name: String? = null, val email: String? = null)

suspend fun HttpClient.getOAuthUserPrincipalFromMicrosoft(accessToken: String) = get {
    url("https://graph.microsoft.com/v1.0/me")
    headers {
        append(HttpHeaders.Authorization, "Bearer $accessToken")
    }
}.body<MicrosoftUserInfo>().let {
    OAuthUserPrincipal(it.id, it.displayName, it.mail)
}

@Serializable
data class MicrosoftUserInfo(val id: String, val displayName: String?, val mail: String?)
