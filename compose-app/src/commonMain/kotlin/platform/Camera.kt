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

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharedFlow

expect class Camera {
    companion object {
        val list: List<Camera>
    }

    val name: String
    val facing: CameraFacing
    val width: Int
    val height: Int

    val isStreamingAvailable: Boolean

    val scope: CoroutineScope
    var streamingJob: Job?
        private set
    val frameFlow: SharedFlow<CameraImage?>

    suspend fun open()

    suspend fun takePicture(): CameraImage
    fun startStreaming()
    fun stopStreaming()

    suspend fun close()
}

enum class CameraFacing {
    Front, Back, Unknown
}

data class CameraImage(
    val format: FrameFormat,
    val width: Int,
    val height: Int,
    val content: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as CameraImage

        if (format != other.format) return false
        if (width != other.width) return false
        if (height != other.height) return false
        if (!content.contentEquals(other.content)) return false

        return true
    }

    override fun hashCode(): Int {
        var result = format.hashCode()
        result = 31 * result + width
        result = 31 * result + height
        result = 31 * result + content.contentHashCode()
        return result
    }
}

enum class FrameFormat {
    AndroidRgba8888, Rgb, Bgr, RgbInt, ArgbInt
}
