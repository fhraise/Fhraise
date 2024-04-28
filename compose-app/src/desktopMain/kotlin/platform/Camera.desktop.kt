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
import com.github.sarxos.webcam.WebcamResolution
import java.awt.Dimension
import java.awt.image.BufferedImage
import java.awt.image.DataBufferByte

actual class Camera(val webcam: Webcam) {
    actual companion object {
        actual val list
            get() = Webcam.getWebcams().map {
                Camera(it.apply {
                    setCustomViewSizes(Dimension(4000, 3000), Dimension(1920, 1080), Dimension(1280, 720))
                    viewSize = WebcamResolution.FHD.size
                })
            }
    }

    actual val name: String = webcam.name
    actual val facing = CameraFacing.Unknown
    actual val isStreamingAvailable = false

    actual fun open() {
        webcam.open(true)
    }

    actual fun takePicture() = webcam.image.toCameraImage()

    actual fun startStreaming(onImageAvailable: (CameraImage) -> Unit) {
        throw NotImplementedError("Streaming is not supported on desktop.")
    }

    actual fun stopStreaming() {}

    actual fun close() {
        webcam.close()
    }
}

fun BufferedImage.toCameraImage(): CameraImage {
    return when (type) {
        BufferedImage.TYPE_INT_RGB -> CameraImage(
            FrameFormat.RgbInt, width, (raster.dataBuffer as DataBufferByte).data
        )

        BufferedImage.TYPE_INT_ARGB -> CameraImage(
            FrameFormat.ArgbInt, width, (raster.dataBuffer as DataBufferByte).data
        )

        else -> error("Unsupported image type: $type")
    }
}
