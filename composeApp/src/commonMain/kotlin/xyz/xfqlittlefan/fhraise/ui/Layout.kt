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

package xyz.xfqlittlefan.fhraise.ui

import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * 一个专门传送给接收器的类，用于在密度范围内进行操作。
 *
 * @param delegate 一个 [Density] 实例，用于委托所有操作。
 *
 * @see Density
 */
open class DensityScope(private val delegate: Density) : Density by delegate {
    /**
     * 以给定的 [Number] 为 [Dp]，将其转换为像素。
     *
     * @see dp
     */
    val Number.dpAsPx get() = this.toFloat().dp.toPx()
}

/**
 * 一个数据类，存储一组值，用于在动画中进行插值。
 *
 * @param source 一个 [Number]，表示动画的起始值。
 * @param target 一个 [Number]，表示动画的目标值。
 */
data class AnimationValue(val source: Number, val target: Number) {
    /**
     * 通过给定的 [animation] 值，对动画进行插值。
     *
     * @param animation 一个 [Float]，表示动画的进度。
     *
     * @return 一个 [Float]，表示插值后的值。
     */
    fun animated(animation: Float): Float {
        val sourceFloat = source.toFloat()
        val targetFloat = target.toFloat()

        return sourceFloat + (targetFloat - sourceFloat) * animation
    }
}

/**
 * 一个中缀函数，用于创建一个 [AnimationValue] 实例。
 *
 * @param target 一个 [Number]，表示动画的目标值。
 *
 * @return 一个 [AnimationValue] 实例。
 */
infix fun Number.animateTo(target: Number) = AnimationValue(this, target)
