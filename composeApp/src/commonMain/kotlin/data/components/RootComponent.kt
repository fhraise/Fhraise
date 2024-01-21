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

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.*
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import data.AppComponentContext
import data.AppComponentContextValues
import data.components.root.AppSignInComponent
import data.components.root.SignInComponent
import kotlinx.serialization.Serializable

interface RootComponent : AppComponentContext, BackHandlerOwner {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class SignIn(val component: SignInComponent) : Child()
    }

    fun onBack()
}

class AppRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Configuration>()

    override val colorMode = mutableStateOf(AppComponentContextValues.ColorMode.SYSTEM)

    override fun changeColorMode(colorMode: AppComponentContextValues.ColorMode) {
        this.colorMode.value = colorMode
    }

    override val snackbarHostState = SnackbarHostState()

    override val stack: Value<ChildStack<*, RootComponent.Child>> = childStack(
        source = navigation,
        serializer = Configuration.serializer(),
        initialConfiguration = Configuration.SignIn,
        handleBackButton = true,
        childFactory = ::createChild
    )

    override val pop: MutableState<(() -> Unit)?> = mutableStateOf(null)

    init {
        stack.subscribe { childStack ->
            if (childStack.backStack.isEmpty()) {
                pop.value = null
            } else {
                pop.value = { navigation.pop() }
            }
        }
    }

    private fun createChild(config: Configuration, componentContext: ComponentContext): RootComponent.Child {
        val childComponentContext =
            object : AppComponentContext, ComponentContext by componentContext, AppComponentContextValues by this {}
        return when (config) {
            is Configuration.SignIn -> RootComponent.Child.SignIn(
                component = AppSignInComponent(componentContext = childComponentContext) {
                    AppSignInComponent.ComponentState.SignIn(
                        context = this,
                        onGuestSignIn = {},
                        onUsernameSignIn = {},
                        onFaceSignIn = {},
                        onSignUp = {
                            navigation.push(Configuration.SignUp)
                        },
                        onAdminSignIn = {},
                    )
                },
            )

            is Configuration.SignUp -> RootComponent.Child.SignIn(
                component = AppSignInComponent(componentContext = childComponentContext) {
                    AppSignInComponent.ComponentState.SignUp(
                        context = this,
                    )
                },
            )
        }
    }

    @Serializable
    private sealed class Configuration {
        @Serializable
        data object SignIn : Configuration()

        @Serializable
        data object SignUp : Configuration()
    }

    override fun onBack() = navigation.pop()
}
