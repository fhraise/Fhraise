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

package ui.composables

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun TypeWriter(
    text: String,
    suffix: String = "_",
    modifier: Modifier = Modifier,
    duration: Duration = 200.milliseconds,
    delta: Int = 1,
    infinite: Boolean = true,
    suffixFlashTimes: Int = 3,
    reserveSpace: Boolean = true,
) {
    var index by remember { mutableStateOf(0) }
    var indexDelta by remember { mutableStateOf(delta) }
    var showSuffix by remember { mutableStateOf(true) }
    var showSuffixTimes by remember { mutableStateOf(0) }

    Box(modifier = modifier) {
        if (reserveSpace) Text(text = "${text.replace("\n", "\n$suffix")}$suffix", modifier = Modifier.alpha(0f))
        Text(text = "${text.substring(0, index)}${if (showSuffix) suffix else ""}")
    }

    fun applyDelta() {
        index = (index + indexDelta).coerceIn(0, text.length)
    }

    LaunchedEffect(text, suffix) {
        applyDelta()
        while (true) {
            delay(duration)
            if (index > 0 && index < text.length) {
                applyDelta()
            } else {
                if (showSuffix) {
                    showSuffix = false
                } else if (showSuffixTimes < suffixFlashTimes) {
                    showSuffix = true
                    showSuffixTimes++
                } else if (!infinite && showSuffixTimes >= suffixFlashTimes) {
                    showSuffix = true
                    break
                } else {
                    showSuffix = true
                    showSuffixTimes = 0
                    indexDelta = -indexDelta
                    index += indexDelta
                }
            }
        }
    }
}
