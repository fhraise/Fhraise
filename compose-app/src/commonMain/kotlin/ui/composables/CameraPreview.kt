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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import xyz.xfqlittlefan.fhraise.platform.Camera

@Composable
expect fun CameraPreview(
    camera: Camera,
    onStateChange: (ready: Boolean) -> Unit = {},
    onDispose: () -> Unit = {},
    frame: @Composable (bitmap: ImageBitmap?) -> Unit
)

@Composable
fun CameraPreview(
    camera: Camera, onDispose: () -> Unit = {}, modifier: Modifier = Modifier, flipHorizontally: Boolean = false
) {
    var ready by remember { mutableStateOf(false) }
    val blurRadius by animateDpAsState(if (ready) 0.dp else 4.dp)

    CameraPreview(
        camera = camera, onStateChange = { ready = it }, onDispose = onDispose,
        frame = { bitmap ->
            Box {
                with(LocalDensity.current) {
                    bitmap?.let {
                        Image(
                            bitmap = it,
                            contentDescription = "相机预览",
                            modifier = Modifier.graphicsLayer {
                                if (flipHorizontally) scaleX = -1f
                            }.blur(blurRadius).then(modifier),
                        )
                    }
                }
                AnimatedVisibility(
                    visible = !ready, modifier = Modifier.matchParentSize(), enter = fadeIn(), exit = fadeOut()
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(), contentAlignment = androidx.compose.ui.Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            }
        },
    )
}
