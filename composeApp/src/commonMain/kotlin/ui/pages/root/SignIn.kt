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

package ui.pages.root

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import data.components.RootComponent
import data.components.root.SignInComponent
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.modifiers.applyBrush

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun SignIn(component: SignInComponent) {
    val colorMode by component.colorMode.subscribeAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.fillMaxSize().nestedScroll(connection = scrollBehavior.nestedScrollConnection),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Icon(
                        modifier = Modifier.applyBrush(
                            Brush.horizontalGradient(
                                listOf(Color.Magenta.copy(alpha = 0.8f), Color.Cyan.copy(alpha = 0.8f))
                            )
                        ),
                        painter = painterResource("drawable/fhraise_logo.xml"),
                        contentDescription = "Fhraise Logo",
                    )
                },
                actions = {
                    IconButton(
                        onClick = component::switchColorMode,
                        content = {
                            Icon(
                                imageVector = when (colorMode) {
                                    RootComponent.ColorMode.LIGHT -> Icons.Default.LightMode
                                    RootComponent.ColorMode.DARK -> Icons.Default.DarkMode
                                    RootComponent.ColorMode.SYSTEM -> Icons.Default.Adjust
                                },
                                contentDescription = "当前颜色模式：${component.colorMode.value.displayName}，改变到：${component.nextColorMode.displayName}",
                            )
                        },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(state = component.scrollState).padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Horizontal)),
            verticalArrangement = Arrangement.Center,
        ) {
            val state = component.state

            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = "开启你的\n 美食之旅_",
                modifier = Modifier.applyBrush(
                    brush = Brush.horizontalGradient(
                        listOf(
                            Color.Red.copy(alpha = 0.7f), Color.Blue.copy(alpha = 0.7f)
                        )
                    )
                ).padding(horizontal = 32.dp),
                style = MaterialTheme.typography.displayMedium,
            )
            Spacer(modifier = Modifier.height(48.dp))
            Card(
                modifier = Modifier.padding(horizontal = 32.dp).fillMaxWidth(),
            ) {
                if (state is SignInComponent.State.SignIn) {
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.username,
                        onValueChange = state::username::set,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        label = { Text(text = "用户名") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text, imeAction = ImeAction.Next),
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = state.password,
                        onValueChange = state::password::set,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                        label = { Text(text = "密码") },
                        trailingIcon = {
                            IconButton(
                                onClick = state::switchShowPassword,
                                content = {
                                    Icon(
                                        imageVector = if (state.showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                        contentDescription = if (state.showPassword) "隐藏密码" else "显示密码",
                                    )
                                },
                            )
                        },
                        visualTransformation = if (state.showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = state.onDone),
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Button(
                    onClick = state::submit,
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "登录",
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
