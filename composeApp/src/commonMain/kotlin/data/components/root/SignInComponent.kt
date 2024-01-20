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
            val phoneNumberVerified: Boolean
            var canInputVerifyCode: Boolean
            var verifyCode: String

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

            fun nextOrSubmit()

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
            private val phoneNumberRegex =
                Regex("^1(3(([0-3]|[5-9])[0-9]{8}|4[0-8][0-9]{7})|(45|5([0-2]|[5-6]|[8-9])|6(2|[5-7])|7([0-1]|[5-8])|8[0-9]|9([0-3]|[5-9]))[0-9]{8})$")

            private var _phoneNumber by mutableStateOf(phoneNumber)
            override var phoneNumber: String
                get() = _phoneNumber
                set(value) {
                    canInputVerifyCode = false
                    _phoneNumber = value
                }

            override val phoneNumberVerified: Boolean
                get() = phoneNumberRegex.matches(phoneNumber)

            private var _canInputVerifyCode by mutableStateOf(canInputVerifyCode)
            override var canInputVerifyCode: Boolean
                get() = _canInputVerifyCode
                set(value) {
                    if (!phoneNumberVerified) {
                        _canInputVerifyCode = false
                        return
                    }
                    _canInputVerifyCode = value
                    if (!value) {
                        verifyCode = ""
                    }
                }

            override var verifyCode by mutableStateOf(verifyCode)
            override var showMoreSignInOptions by mutableStateOf(showMoreSignInOptions)

            override fun submit() {
                // TODO
            }

            override val onNext: KeyboardActionScope.() -> Unit = {
                this@SignIn.canInputVerifyCode = true
            }

            override fun nextOrSubmit() {
                if (canInputVerifyCode) {
                    submit()
                } else {
                    canInputVerifyCode = true
                }
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
