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

import com.github.sarxos.webcam.Webcam

actual class Camera(private val webcam: Webcam) {
    actual companion object {
        actual val list
            get() = Webcam.getWebcams().map { Camera(it) }
    }

    init {
        webcam.open(true)
    }

    actual val name: String = webcam.name
    actual val isStreamingAvailable: Boolean
        get() = TODO("Not yet implemented")

    actual suspend fun takePicture(): CameraImage {
        TODO("Not yet implemented")
    }

    actual fun startStreaming(onImageAvailable: (CameraImage) -> Unit) {
    }

    actual fun stopStreaming() {
    }

    actual fun close() {
        webcam.close()
    }
}
