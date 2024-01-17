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

package data.components

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.value.MutableValue
import com.arkivanov.decompose.value.Value
import data.AppComponentContext
import data.FhraiseComponentContext
import data.components.root.AppSignInComponent
import data.components.root.SignInComponent
import kotlinx.serialization.Serializable

interface RootComponent {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class SignIn(val component: SignInComponent) : Child()
    }

    val colorMode: Value<ColorMode>
    fun changeAppColorMode(colorMode: ColorMode)

    enum class ColorMode(val displayName: String) {
        LIGHT("亮色"), DARK("暗色"), SYSTEM("跟随系统")
    }
}

class AppRootComponent(
    componentContext: FhraiseComponentContext,
) : RootComponent, FhraiseComponentContext by componentContext {
    private val navigation = StackNavigation<Configuration>()

    override val colorMode: MutableValue<RootComponent.ColorMode> = MutableValue(componentContext.colorMode.value)

    override fun changeAppColorMode(colorMode: RootComponent.ColorMode) {
        this.colorMode.value = colorMode
    }

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = Configuration.SignIn,
        handleBackButton = true,
        childFactory = ::createChild
    )

    private fun createChild(config: Configuration, componentContext: ComponentContext): RootComponent.Child {
        val childComponentContext = AppComponentContext(
            componentContext = componentContext,
            colorMode = colorMode,
            changeColorMode = ::changeAppColorMode,
        )
        return when (config) {
            is Configuration.SignIn -> RootComponent.Child.SignIn(component = AppSignInComponent(componentContext = childComponentContext))
        }
    }

    @Serializable
    private sealed class Configuration {
        @Serializable
        data object SignIn : Configuration()
    }
}
