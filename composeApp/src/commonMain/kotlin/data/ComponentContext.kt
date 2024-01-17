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

package data

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import data.components.RootComponent

interface FhraiseComponentContext : ComponentContext {
    val colorMode: Value<RootComponent.ColorMode>
    val changeColorMode: (RootComponent.ColorMode) -> Unit
}

class AppComponentContext(
    componentContext: ComponentContext,
    override val colorMode: Value<RootComponent.ColorMode> = MutableValue(RootComponent.ColorMode.SYSTEM),
    override val changeColorMode: (RootComponent.ColorMode) -> Unit = {},
) : FhraiseComponentContext, ComponentContext by componentContext
