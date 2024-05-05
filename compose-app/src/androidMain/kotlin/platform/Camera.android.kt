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

actual class Camera {
    actual companion object {
        actual val list: List<Camera>
            get() = TODO("Not yet implemented")
    }

    actual val name: String
        get() = TODO("Not yet implemented")
    actual val facing: CameraFacing
        get() = TODO("Not yet implemented")
    actual val width: Int
        get() = TODO("Not yet implemented")
    actual val height: Int
        get() = TODO("Not yet implemented")
    actual val isStreamingAvailable: Boolean
        get() = TODO("Not yet implemented")
    actual val scope: CoroutineScope
        get() = TODO("Not yet implemented")
    actual var streamingJob: Job?
        get() = TODO("Not yet implemented")
        set(value) {}
    actual val frameFlow: SharedFlow<CameraImage?>
        get() = TODO("Not yet implemented")

    actual suspend fun open() {
    }

    actual suspend fun takePicture(): CameraImage {
        TODO("Not yet implemented")
    }

    actual fun startStreaming() {
    }

    actual fun stopStreaming() {
    }

    actual suspend fun close() {
    }
}
