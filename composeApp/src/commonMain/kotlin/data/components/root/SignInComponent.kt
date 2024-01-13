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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.arkivanov.decompose.ComponentContext

interface SignInComponent {
    val state: State

    sealed interface State {
        interface Common {
            fun submit()
        }

        interface SignIn : Common {
            var username: String
            var password: String
            var showPassword: Boolean
        }

        interface SignUp : Common {
            var email: String
            var username: String
            var password: String
            var showPassword: Boolean
            var confirmPassword: String
            var showConfirmPassword: Boolean
        }
    }
}

class AppSignInComponent(
    componentContext: ComponentContext, override val state: State.SignIn = State.SignIn()
) : SignInComponent, ComponentContext by componentContext {
    sealed class State : SignInComponent.State {
        class SignIn(username: String = "", password: String = "", showPassword: Boolean = false) : State(),
            SignInComponent.State.SignIn {
            override var username by mutableStateOf(username)
            override var password by mutableStateOf(password)
            override var showPassword by mutableStateOf(showPassword)

            override fun submit() {
                // TODO
            }
        }

        class SignUp(
            email: String = "",
            username: String = "",
            password: String = "",
            showPassword: Boolean = false,
            confirmPassword: String = "",
            showConfirmPassword: Boolean = false
        ) : State(), SignInComponent.State.SignUp {
            override var email by mutableStateOf(email)
            override var username by mutableStateOf(username)
            override var password by mutableStateOf(password)
            override var showPassword by mutableStateOf(showPassword)
            override var confirmPassword by mutableStateOf(confirmPassword)
            override var showConfirmPassword by mutableStateOf(showConfirmPassword)

            override fun submit() {
                // TODO
            }
        }
    }
}
