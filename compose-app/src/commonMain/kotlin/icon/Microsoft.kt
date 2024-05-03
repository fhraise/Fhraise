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

val AppIcons.Microsoft by lazy {
    ImageVector.Builder(
        name = "Microsoft", defaultWidth = 21.dp, defaultHeight = 24.dp, viewportWidth = 448f, viewportHeight = 512f
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
            moveTo(0f, 32f)
            horizontalLineToRelative(214.6f)
            verticalLineToRelative(214.6f)
            horizontalLineTo(0f)
            verticalLineTo(32f)
            close()
            moveToRelative(233.4f, 0f)
            horizontalLineTo(448f)
            verticalLineToRelative(214.6f)
            horizontalLineTo(233.4f)
            verticalLineTo(32f)
            close()
            moveTo(0f, 265.4f)
            horizontalLineToRelative(214.6f)
            verticalLineTo(480f)
            horizontalLineTo(0f)
            verticalLineTo(265.4f)
            close()
            moveToRelative(233.4f, 0f)
            horizontalLineTo(448f)
            verticalLineTo(480f)
            horizontalLineTo(233.4f)
            verticalLineTo(265.4f)
            close()
        }
    }.build()
}
