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

package xyz.xfqlittlefan.fhraise.data.components

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.router.stack.StackNavigation
import com.arkivanov.decompose.router.stack.childStack
import com.arkivanov.decompose.router.stack.pop
import com.arkivanov.decompose.value.Value
import com.arkivanov.essenty.backhandler.BackHandlerOwner
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import xyz.xfqlittlefan.fhraise.SettingsDataStore
import xyz.xfqlittlefan.fhraise.data.AppComponentContext
import xyz.xfqlittlefan.fhraise.data.AppComponentContextValues
import xyz.xfqlittlefan.fhraise.data.componentScope
import xyz.xfqlittlefan.fhraise.data.components.root.AppSignInComponent
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent
import xyz.xfqlittlefan.fhraise.platform.notificationPermissionGranted
import xyz.xfqlittlefan.fhraise.platform.requestNotificationPermission
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

interface RootComponent : AppComponentContext, BackHandlerOwner {
    val stack: Value<ChildStack<*, Child>>

    sealed class Child {
        class SignIn(val component: SignInComponent) : Child()
    }

    val notificationPermissionRequestReason: String?

    fun startNotificationPermissionRequest()
    fun cancelNotificationPermissionRequest()

    fun onBack()
}

class AppRootComponent(
    componentContext: ComponentContext,
) : RootComponent, ComponentContext by componentContext {
    private val navigation = StackNavigation<Configuration>()

    override val settings = SettingsDataStore.Preferences(componentScope)

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
                component = AppSignInComponent(componentContext = childComponentContext) {},
            )
        }
    }

    @Serializable
    private sealed class Configuration {
        @Serializable
        data object SignIn : Configuration()
    }

    override var notificationPermissionRequestReason: String? by mutableStateOf(null)
    private var permissionRequestContinuation: Continuation<Boolean?>? = null

    override fun startNotificationPermissionRequest() {
        componentScope.launch {
            permissionRequestContinuation?.resumeWith(Result.success(requestNotificationPermission()))
            notificationPermissionRequestReason = null
        }
    }

    override fun cancelNotificationPermissionRequest() {
        permissionRequestContinuation?.resumeWith(Result.success(null))
        notificationPermissionRequestReason = null
    }

    override suspend fun requestAppNotificationPermission(reason: String): Boolean? = suspendCoroutine {
        if (notificationPermissionGranted != null) {
            it.resumeWith(Result.success(notificationPermissionGranted))
            return@suspendCoroutine
        }

        permissionRequestContinuation = it
        notificationPermissionRequestReason = reason
    }

    override fun onBack() = navigation.pop()
}
