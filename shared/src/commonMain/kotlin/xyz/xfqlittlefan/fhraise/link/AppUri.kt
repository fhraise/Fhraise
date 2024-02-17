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

package xyz.xfqlittlefan.fhraise.link

object AppUri {
    data class OAuthCallback(val token: String?, val refreshToken: String?, val requestId: String) {
        companion object {
            const val PREFIX = "fhraise://oauth/callback?"
            const val TOKEN_PARAM = "t="
            const val REFRESH_TOKEN_PARAM = "r="
            const val REQUEST_ID_PARAM = "i="

            fun fromUriOrNull(uri: String): OAuthCallback? {
                if (!uri.startsWith(PREFIX)) return null
                val token = uri.substringAfter(TOKEN_PARAM).substringBefore("&")
                val refreshToken = uri.substringAfter(REFRESH_TOKEN_PARAM).substringBefore("&")
                val state = uri.substringAfter(TOKEN_PARAM).substringBefore("&")
                return OAuthCallback(token, refreshToken, state)
            }
        }

        val uri = "$PREFIX$TOKEN_PARAM$token&$REFRESH_TOKEN_PARAM$refreshToken&$REQUEST_ID_PARAM$requestId"
    }
}
