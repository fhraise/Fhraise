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

package data.components.root

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import data.AppComponentContext
import data.components.RootComponent
import data.components.RootComponent.ColorMode.*

interface SignInComponent {
    val colorMode: State<RootComponent.ColorMode>
    val nextColorMode: RootComponent.ColorMode
        get() = when (colorMode.value) {
            LIGHT -> DARK
            DARK -> SYSTEM
            SYSTEM -> LIGHT
        }

    fun switchColorMode()

    val pop: State<(() -> Unit)?>

    val state: ComponentState

    sealed interface ComponentState {
        fun submit()

        interface KeyboardNextState : ComponentState {
            val onNext: KeyboardActionScope.() -> Unit
        }

        interface KeyboardDoneState : ComponentState {
            val onDone: KeyboardActionScope.() -> Unit
        }

        interface PhoneNumberVerifyCodeState : ComponentState, KeyboardNextState, KeyboardDoneState {
            var phoneNumber: String
            var verifyCode: String
            var canInputVerifyCode: Boolean

            fun switchCanInputVerifyCode() {
                canInputVerifyCode = !canInputVerifyCode
                if (!canInputVerifyCode) {
                    verifyCode = ""
                }
            }
        }

        interface UsernamePasswordState : ComponentState, KeyboardDoneState {
            var username: String
            var password: String
            var showPassword: Boolean

            fun switchShowPassword() {
                showPassword = !showPassword
            }
        }

        interface SignIn : ComponentState, PhoneNumberVerifyCodeState {
            var showMoreSignInOptions: Boolean

            fun switchShowMoreSignInOptions() {
                showMoreSignInOptions = !showMoreSignInOptions
            }

            val onGuestSignIn: () -> Unit
            val onUsernameSignIn: () -> Unit
            val onFaceSignIn: () -> Unit
            val onSignUp: () -> Unit
            val onAdminSignIn: () -> Unit
        }

        interface SignUp : ComponentState, UsernamePasswordState {
            var email: String
            var confirmPassword: String
            var showConfirmPassword: Boolean

            fun switchShowConfirmPassword() {
                showConfirmPassword = !showConfirmPassword
            }
        }
    }

    val scrollState: ScrollState
}

class AppSignInComponent(
    componentContext: AppComponentContext, state: ComponentState
) : SignInComponent, AppComponentContext by componentContext {
    override var state: ComponentState by mutableStateOf(state)

    override fun switchColorMode() {
        changeColorMode(
            when (colorMode.value) {
                LIGHT -> DARK
                DARK -> SYSTEM
                SYSTEM -> LIGHT
            }
        )
    }

    sealed class ComponentState : SignInComponent.ComponentState {
        class SignIn(
            phoneNumber: String = "",
            verifyCode: String = "",
            canInputVerifyCode: Boolean = false,
            showMoreSignInOptions: Boolean = false,
            override val onGuestSignIn: () -> Unit,
            override val onUsernameSignIn: () -> Unit,
            override val onFaceSignIn: () -> Unit,
            override val onSignUp: () -> Unit,
            override val onAdminSignIn: () -> Unit,
        ) : ComponentState(), SignInComponent.ComponentState.SignIn {
            override var phoneNumber by mutableStateOf(phoneNumber)
            override var verifyCode by mutableStateOf(verifyCode)
            override var canInputVerifyCode by mutableStateOf(canInputVerifyCode)
            override var showMoreSignInOptions by mutableStateOf(showMoreSignInOptions)

            override fun submit() {
                // TODO
            }

            override val onNext: KeyboardActionScope.() -> Unit = {
                // TODO
            }

            override val onDone: KeyboardActionScope.() -> Unit = {
                submit()
            }
        }

        class SignUp(
            email: String = "",
            username: String = "",
            password: String = "",
            showPassword: Boolean = false,
            confirmPassword: String = "",
            showConfirmPassword: Boolean = false
        ) : ComponentState(), SignInComponent.ComponentState.SignUp {
            override var email by mutableStateOf(email)
            override var username by mutableStateOf(username)
            override var password by mutableStateOf(password)
            override var showPassword by mutableStateOf(showPassword)
            override var confirmPassword by mutableStateOf(confirmPassword)
            override var showConfirmPassword by mutableStateOf(showConfirmPassword)

            override fun submit() {
                // TODO
            }

            override val onDone: KeyboardActionScope.() -> Unit = {
                submit()
            }
        }
    }

    override val scrollState: ScrollState = ScrollState(initial = 0)
}
