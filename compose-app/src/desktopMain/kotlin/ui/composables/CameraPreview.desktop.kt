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

package xyz.xfqlittlefan.fhraise.ui.composables

import androidx.compose.runtime.*
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.toComposeImageBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.bytedeco.javacv.Java2DFrameConverter
import xyz.xfqlittlefan.fhraise.logger
import xyz.xfqlittlefan.fhraise.platform.Camera

@Composable
actual fun CameraPreview(
    camera: Camera,
    onStateChange: (ready: Boolean) -> Unit,
    onDispose: () -> Unit,
    frame: @Composable (bitmap: ImageBitmap?) -> Unit
) {
    val converter = remember { Java2DFrameConverter() }
    val image by camera.rawFlow.map { converter.convert(it)?.toComposeImageBitmap() }.collectAsState(null)

    frame(image)

    LaunchedEffect(camera) {
        withContext(Dispatchers.IO) {
            logger.debug("Opening camera preview.")
            runCatching { camera.open() }.onFailure { logger.error("Failed to open camera preview.", it) }
            camera.previewCount += 1
            onStateChange(true)
        }
    }

    DisposableEffect(camera) {
        onDispose {
            logger.debug("Disposing camera preview.")
            onStateChange(false)
            camera.previewCount -= 1
            onDispose()
            logger.debug("Camera preview disposed.")
        }
    }
}
