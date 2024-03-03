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

package xyz.xfqlittlefan.fhraise.ui.modifiers

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import kotlinx.coroutines.launch

/**
 * [origin](https://stackoverflow.com/a/76137220/16137246)
 */
fun Modifier.animatedPlacement(
    animationSpec: AnimationSpec<IntOffset> = spring(stiffness = Spring.StiffnessMediumLow)
) = composed {
    val scope = rememberCoroutineScope()
    var targetOffset by remember { mutableStateOf(IntOffset.Zero) }
    var animatable by remember {
        mutableStateOf<Animatable<IntOffset, AnimationVector2D>?>(null)
    }

    onPlaced {
        // Calculate the position in the parent layout
        targetOffset = it.positionInParent().round()
    }.offset {
        // Animate to the new target offset when alignment changes.
        (animatable ?: Animatable(targetOffset, IntOffset.VectorConverter).also {
            animatable = it
        }).let {
            if (it.targetValue != targetOffset) {
                scope.launch {
                    it.animateTo(targetOffset, animationSpec)
                }
            }
        }

        // Offset the child in the opposite direction to the targetOffset, and slowly catch
        // up to zero offset via an animation to achieve an overall animated movement.
        animatable?.let { it.value - targetOffset } ?: IntOffset.Zero
    }
}
