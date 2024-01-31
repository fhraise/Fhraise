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

package xyz.xfqlittlefan.fhraise.data.components.root

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.KeyboardActionScope
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.serialization.kotlinx.cbor.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.DefaultServerPort
import xyz.xfqlittlefan.fhraise.api.Auth
import xyz.xfqlittlefan.fhraise.data.AppComponentContext
import xyz.xfqlittlefan.fhraise.data.componentScope
import xyz.xfqlittlefan.fhraise.sendVerifyCodeNotification

interface SignInComponent : AppComponentContext {
    val state: ComponentState

    sealed interface ComponentState {
        fun submit()

        interface KeyboardNextState : ComponentState {
            val onNext: KeyboardActionScope.() -> Unit
        }

        interface KeyboardDoneState : ComponentState {
            val onDone: KeyboardActionScope.() -> Unit
        }

        interface MultiStepState : ComponentState {
            fun nextOrSubmit()
        }

        interface PhoneNumberVerifyCodeState : ComponentState, MultiStepState, KeyboardNextState, KeyboardDoneState {
            var phoneNumber: String
            val phoneNumberVerified: Boolean
            var canInputVerifyCode: Boolean
            var verifyCode: String

            val textFieldError
                get() = phoneNumber.isNotEmpty() && !phoneNumberVerified

            fun sendVerifyCode()
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
            val onFaceSignIn: () -> Unit

            fun onAdminSignIn()
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
    componentContext: AppComponentContext, stateBuilder: AppSignInComponent.() -> ComponentState
) : SignInComponent, AppComponentContext by componentContext {
    override var state: ComponentState by mutableStateOf(stateBuilder())

    sealed class ComponentState(context: AppComponentContext) : AppComponentContext by context,
        SignInComponent.ComponentState {
        class SignIn(
            context: AppComponentContext,
            phoneNumber: String = "",
            verifyCode: String = "",
            canInputVerifyCode: Boolean = false,
            showMoreSignInOptions: Boolean = false,
            override val onGuestSignIn: () -> Unit,
            override val onFaceSignIn: () -> Unit,
        ) : ComponentState(context), SignInComponent.ComponentState.SignIn {
            private val phoneNumberRegex =
                Regex("^1(3(([0-3]|[5-9])[0-9]{8}|4[0-8][0-9]{7})|(45|5([0-2]|[5-6]|[8-9])|6(2|[5-7])|7([0-1]|[5-8])|8[0-9]|9([0-3]|[5-9]))[0-9]{8})$")

            private var _phoneNumber by mutableStateOf(phoneNumber)
            override var phoneNumber
                get() = _phoneNumber
                set(value) {
                    canInputVerifyCode = false
                    _phoneNumber = value
                }

            override val phoneNumberVerified
                get() = phoneNumberRegex.matches(phoneNumber)

            private var _canInputVerifyCode by mutableStateOf(canInputVerifyCode)
            override var canInputVerifyCode
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

            private var verifyCodeSentSnackbarJob: Job? = null

            @OptIn(ExperimentalSerializationApi::class)
            override fun sendVerifyCode() {
                canInputVerifyCode = true
                if (!canInputVerifyCode) return

                componentScope.launch {
                    val requestResult = requestAppNotificationPermission()
                    verifyCodeSentSnackbarJob?.cancel()
                    verifyCodeSentSnackbarJob = componentScope.launch {
                        snackbarHostState.showSnackbar("验证码已获取", withDismissAction = true)
                    }
                    val client = HttpClient {
                        install(Resources)
                        install(ContentNegotiation) { cbor() }
                        defaultRequest {
                            host = "192.168.2.42"
                            port = DefaultServerPort
                        }
                    }

                    val code = try {
                        val response = client.post(Auth.PhoneNumber(phoneNumber = phoneNumber))
                        response.body<Auth.PhoneNumber.Response.Success>().verifyCode
                    } catch (e: Throwable) {
                        verifyCodeSentSnackbarJob?.cancel()
                        snackbarHostState.showSnackbar("验证码获取失败", withDismissAction = true)
                        e.printStackTrace()
                        return@launch
                    }
                    sendVerifyCodeNotification(code)
                    if (requestResult != true) {
                        verifyCode = code
                        snackbarHostState.showSnackbar("通知权限未授予，验证码已自动填写", withDismissAction = true)
                    }
                }
            }

            override var verifyCode by mutableStateOf(verifyCode)
            override var showMoreSignInOptions by mutableStateOf(showMoreSignInOptions)

            override fun submit() {
                // TODO
            }

            override val onNext: KeyboardActionScope.() -> Unit = {
                sendVerifyCode()
            }

            override fun nextOrSubmit() {
                if (canInputVerifyCode) {
                    submit()
                } else {
                    sendVerifyCode()
                }
            }

            override val onDone: KeyboardActionScope.() -> Unit = {
                submit()
            }

            override fun onAdminSignIn() {
                // TODO
            }
        }

        class SignUp(
            context: AppComponentContext,
            email: String = "",
            username: String = "",
            password: String = "",
            showPassword: Boolean = false,
            confirmPassword: String = "",
            showConfirmPassword: Boolean = false
        ) : ComponentState(context), SignInComponent.ComponentState.SignUp {
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
