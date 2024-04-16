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

package xyz.xfqlittlefan.fhraise.py

import kotlinx.serialization.Serializable

@Serializable
sealed class Message {
    @Serializable
    sealed class Register : Message() {
        @Serializable
        data class Frame(val callId: String, val content: ByteArray) : Register() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Frame

                if (callId != other.callId) return false
                if (!content.contentEquals(other.content)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = callId.hashCode()
                result = 31 * result + content.contentHashCode()
                return result
            }
        }

        @Serializable
        sealed class Result : Register() {
            @Serializable
            data object Success : Result()

            @Serializable
            data object LowResolution : Result()
        }
    }

    @Serializable
    sealed class Ping : Message() {
        @Serializable
        data object Request : Ping()

        @Serializable
        data object Response : Ping()
    }
}
