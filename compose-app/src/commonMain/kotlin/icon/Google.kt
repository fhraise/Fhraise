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

package xyz.xfqlittlefan.fhraise.icon

import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val AppIcons.Google by lazy {
    ImageVector.Builder(
        name = "Google", defaultWidth = 22.875.dp, defaultHeight = 24.dp, viewportWidth = 488f, viewportHeight = 512f
    ).apply {
        path(
            fill = SolidColor(Color.Black),
            fillAlpha = 1.0f,
            stroke = null,
            strokeAlpha = 1.0f,
            strokeLineWidth = 1.0f,
            strokeLineCap = StrokeCap.Butt,
            strokeLineJoin = StrokeJoin.Miter,
            strokeLineMiter = 1.0f,
            pathFillType = PathFillType.NonZero
        ) {
            moveTo(488f, 261.8f)
            curveTo(488f, 403.3f, 391.1f, 504f, 248f, 504f)
            curveTo(110.8f, 504f, 0f, 393.2f, 0f, 256f)
            reflectiveCurveTo(110.8f, 8f, 248f, 8f)
            curveToRelative(66.8f, 0f, 123f, 24.5f, 166.3f, 64.9f)
            lineToRelative(-67.5f, 64.9f)
            curveTo(258.5f, 52.6f, 94.3f, 116.6f, 94.3f, 256f)
            curveToRelative(0f, 86.5f, 69.1f, 156.6f, 153.7f, 156.6f)
            curveToRelative(98.2f, 0f, 135f, -70.4f, 140.8f, -106.9f)
            horizontalLineTo(248f)
            verticalLineToRelative(-85.3f)
            horizontalLineToRelative(236.1f)
            curveToRelative(2.3f, 12.7f, 3.9f, 24.9f, 3.9f, 41.4f)
            close()
        }
    }.build()
}
