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

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpSize
import xyz.xfqlittlefan.fhraise.ui.composables.AdaptiveLayoutScope.Measurable

@Composable
fun <BREAK, ID : Any> AdaptiveLayout(
    modifier: Modifier = Modifier,
    generateBreakPoint: (size: DpSize) -> BREAK,
    generateId: (itemCount: Int) -> ID,
    block: AdaptiveLayoutScope<ID>.(breakPoint: BREAK) -> Unit
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val breakPoint = remember(density, width, height, generateBreakPoint) {
            with(density) { generateBreakPoint(DpSize(width.toDp(), height.toDp())) }
        }
        val scope = remember(breakPoint, generateId) { AdaptiveLayoutScopeImpl(generateId) }
        scope.block(breakPoint)
    }
}

abstract class AdaptiveLayoutScope<ID : Any>(
    private val generateId: (itemCount: Int) -> ID
) {
    protected val measurables = mutableStateMapOf<ID, Measurable>()

    @AdaptiveLayoutDsl
    fun component(block: AdaptiveLayoutComponentScope<ID>.() -> Unit) {
        val scope = AdaptiveLayoutScopeImpl.AdaptiveLayoutComponentScopeImpl(generateId(measurables.size))
        scope.block()
        measurables[scope.id] = scope.measurable
    }

    abstract class AdaptiveLayoutComponentScope<ID : Any>(var id: ID) {
        var alignment = Alignment(androidx.compose.ui.Alignment.TopStart, androidx.compose.ui.Alignment.TopStart)

        protected lateinit var generateSize: (DpSize) -> DpSize
        protected lateinit var content: @Composable () -> Unit

        @AdaptiveLayoutDsl
        fun size(generateSize: (DpSize) -> DpSize) {
            this.generateSize = generateSize
        }

        @AdaptiveLayoutDsl
        fun content(content: @Composable () -> Unit) {
            this.content = content
        }

        infix fun androidx.compose.ui.Alignment.to(to: androidx.compose.ui.Alignment) =
            Alignment(this, to, alignment.of).also { alignment = it }

        infix fun Alignment.of(of: ID) = this.copy(of = of).also { alignment = it }

        data class Alignment(
            val from: androidx.compose.ui.Alignment, val to: androidx.compose.ui.Alignment, val of: Any? = Parent
        )

        object Parent
    }

    data class Measurable(val generateSize: (DpSize) -> DpSize, val content: @Composable () -> Unit)
}

private class AdaptiveLayoutScopeImpl<ID : Any>(
    generateId: (itemCount: Int) -> ID
) : AdaptiveLayoutScope<ID>(generateId) {
    @Composable
    fun Layout(width: Int, height: Int) {
        SubcomposeLayout {
            layout(width, height) {
                val map = measurables.toMap()
                val placeables = mutableListOf<Placeable>()

            }
        }
    }

    data class Measurable(val generateSize: (DpSize) -> DpSize, val measurable: androidx.compose.ui.layout.Measurable)

    class AdaptiveLayoutComponentScopeImpl<ID : Any>(id: ID) : AdaptiveLayoutComponentScope<ID>(id) {
        val measurable = Measurable(generateSize, content)
    }
}

@DslMarker
annotation class AdaptiveLayoutDsl
