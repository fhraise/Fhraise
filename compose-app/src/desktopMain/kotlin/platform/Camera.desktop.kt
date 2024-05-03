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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import org.bytedeco.javacv.Frame
import org.bytedeco.javacv.VideoInputFrameGrabber
import xyz.xfqlittlefan.fhraise.logger
import java.nio.ByteBuffer


actual class Camera(private val deviceId: Int, actual val name: String) {
    actual companion object {
        actual val list: List<Camera>
            get() {
                logger.debug("Getting camera list.")
                return VideoInputFrameGrabber.getDeviceDescriptions().mapIndexed { index, name ->
                    logger.debug("Camera $index: $name")
                    Camera(index, name)
                }
            }
    }

    private val grabber = VideoInputFrameGrabber(deviceId)

    actual val width: Int
        get() = grabber.imageWidth

    actual val height: Int
        get() = grabber.imageHeight

    actual val facing = CameraFacing.Unknown
    actual val isStreamingAvailable = true

    actual val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var rawStreamingJob: Job? = null
    actual var streamingJob: Job? = null
    val rawFlow = MutableSharedFlow<Frame?>(1)
    actual val frameFlow: SharedFlow<CameraImage?> = MutableSharedFlow(1)

    var previewCount = 0
        set(value) {
            if (value > 0) {
                field = value
                startRawStreaming()
            } else {
                field = 0
                rawStreamingJob?.cancel()
            }
            logger.debug("Preview count updated: $field.")
        }

    actual suspend fun open() {
        logger.debug("Opening camera $name.")
        grabber.start()
        logger.debug("Camera $name opened.")
    }

    actual suspend fun takePicture() = grabber.grabCameraImage().also {
        (frameFlow as? MutableSharedFlow)?.emit(it)
        logger.debug("Picture taken.")
    }

    actual fun startStreaming() {
        logger.debug("Starting streaming.")
        if (streamingJob?.isActive == true) {
            logger.debug("Streaming already started.")
            return
        }
        if (rawStreamingJob?.isActive != true) {
            logger.debug("Raw streaming not started. Starting.")
            startRawStreaming()
        }

        streamingJob = scope.launch(Dispatchers.IO) {
            logger.debug("Streaming started.")
            rawFlow.collect { cvFrame ->
                runCatching {
                    cvFrame?.toCameraImageOrNull()?.let { (frameFlow as? MutableSharedFlow)?.emit(it) }
                }.onFailure {
                    logger.error("Failed to stream CameraImage.", it)
                }
            }
        }
    }

    actual fun stopStreaming() {
        logger.debug("Stopping streaming.")
        streamingJob?.cancel()
        streamingJob = null

        if (previewCount == 0) {
            rawStreamingJob?.cancel()
            rawStreamingJob = null
        }
    }

    actual suspend fun close() {
        logger.debug("Closing camera $name.")
        previewCount = 0
        stopStreaming()
        grabber.close()
    }

    private fun startRawStreaming() {
        logger.debug("Starting raw streaming.")
        if (rawStreamingJob?.isActive == true) {
            logger.debug("Raw streaming already started.")
            return
        }

        rawStreamingJob = scope.launch(Dispatchers.IO) {
            logger.debug("Raw streaming started.")
            while (true) {
                runCatching { rawFlow.emit(grabber.grab()) }
            }
        }
    }
}

private fun Frame.toCameraImage() = CameraImage(
    FrameFormat.Bgr,
    imageWidth,
    imageHeight,
    ByteArray(imageWidth * imageHeight * 3).apply {
        val buffer = image[0] as ByteBuffer
        buffer.get(this)
    },
)

private fun Frame.toCameraImageOrNull() = runCatching { toCameraImage() }.getOrNull()

fun VideoInputFrameGrabber.grabCameraImage() = grab().toCameraImage()
