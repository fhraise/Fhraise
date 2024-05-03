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
    sealed class Handshake : Message() {
        @Serializable
        data class Request(val userId: String) : Handshake() {
            internal companion object
        }

        @Serializable
        sealed class Response : Handshake() {
            @Serializable
            data object Success : Response()

            @Serializable
            data object Failure : Response()

            internal companion object
        }

        internal companion object
    }

    @Serializable
    sealed class Client : Message() {
        @Serializable
        data class Frame(val format: String, val width: Int, val content: ByteArray) : Client() {
            override fun equals(other: Any?): Boolean {
                if (this === other) return true
                if (other == null || this::class != other::class) return false

                other as Frame

                if (format != other.format) return false
                if (width != other.width) return false
                if (!content.contentEquals(other.content)) return false

                return true
            }

            override fun hashCode(): Int {
                var result = format.hashCode()
                result = 31 * result + width
                result = 31 * result + content.contentHashCode()
                return result
            }

            internal companion object
        }

        @Serializable
        data object Cancel : Client()

        internal companion object
    }

    @Serializable
    sealed class Result : Message() {
        /**
         * 指示客户端应该发送下一帧。
         *
         * 在注册模式和验证模式下，这个结果代表的含义不同。
         *
         * - 在注册模式下，这个结果代表先前的帧已被正确处理，客户端应该发送下一帧。
         * - 在验证模式下，这个结果代表先前的帧中检测到人脸，但是与目标人脸的差异过大，客户端应该发送下一帧。
         */
        @Serializable
        data object Next : Result()

        /**
         * 人脸已被成功注册或验证。
         */
        @Serializable
        data object Success : Result()

        /**
         * 在先前的帧中未检测到人脸。
         */
        @Serializable
        data object NoFaces : Result()

        /**
         * 先前的帧的分辨率过低或人脸置信度过低。
         */
        @Serializable
        data object LowResolution : Result()

        /**
         * Python 处理器发生了内部错误。
         *
         * 可能的原因：
         *
         * - 请求被分配到了一个错误的处理器。
         */
        @Serializable
        data object InternalError : Result()

        /**
         * 用户取消了注册或验证。
         */
        @Serializable
        data object Cancelled : Result()

        internal companion object
    }

    @Serializable
    sealed class Ping : Message() {
        @Serializable
        data object Request : Ping()

        @Serializable
        data object Response : Ping()

        internal companion object
    }

    internal companion object
}
