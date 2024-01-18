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

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import com.arkivanov.decompose.extensions.compose.subscribeAsState
import data.components.RootComponent
import data.components.root.SignInComponent
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.LocalWindowSizeClass
import ui.WindowSizeClass
import ui.WindowWidthSizeClass
import ui.modifiers.applyBrush
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun SignIn(component: SignInComponent) {
    val colorMode by component.colorMode.subscribeAsState()
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior(rememberTopAppBarState())

    val state = component.state

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
        SignInLayout(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .windowInsetsPadding(WindowInsets.safeContent.only(WindowInsetsSides.Horizontal)),
            header = {
                Text(
                    text = "开启你的\n 美食之旅_",
                    modifier = Modifier.applyBrush(
                        brush = Brush.horizontalGradient(
                            listOf(
                                Color.Red.copy(alpha = 0.7f), Color.Blue.copy(alpha = 0.7f)
                            )
                        )
                    ),
                    style = MaterialTheme.typography.displayMedium,
                )
            },
            content = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (state is SignInComponent.State.SignIn) {
                            Spacer(modifier = Modifier.height(16.dp))
                            OutlinedTextField(
                                value = state.username,
                                onValueChange = state::username::set,
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                                label = { Text(text = "用户名") },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
                                ),
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
                }
            },
            additionalContent = {
                if (state is SignInComponent.State.SignIn) {
                    state.MoreMethods(modifier = Modifier.fillMaxWidth())
                }
            },
            initialWindowSizeClass = LocalWindowSizeClass.current.widthSizeClass,
        )
    }
}

@Composable
fun SignInLayout(
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
    additionalContent: @Composable () -> Unit,
    initialWindowSizeClass: WindowWidthSizeClass = WindowWidthSizeClass.Compact,
) {
    var windowSizeClass by remember { mutableStateOf(initialWindowSizeClass) }
    val animation by animateFloatAsState(
        targetValue = when (windowSizeClass) {
            WindowWidthSizeClass.Medium -> 1f
            WindowWidthSizeClass.Expanded -> 2f
            else -> 0f
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
    )

    Layout(
        content = {
            Box(modifier = Modifier.layoutId("header")) { header() }
            Box(modifier = Modifier.layoutId("content")) { content() }
            Box(modifier = Modifier.layoutId("additionalContent")) { additionalContent() }
        },
        modifier = modifier,
    ) { measurables, constraints ->
        val headerMeasurable = measurables.find { it.layoutId == "header" }!!
        val contentMeasurable = measurables.find { it.layoutId == "content" }!!
        val additionalContentMeasurable = measurables.find { it.layoutId == "additionalContent" }!!

        // Layout sizes
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(width.toDp(), height.toDp())).widthSizeClass

        // Animation
        val animationFirstStage = animation.coerceAtMost(1f)
        val animationFirstStageReversed = 1f - animationFirstStage
        val animationSecondStage = (animation - 1f).coerceAtLeast(0f)

        // Header
        val headerPaddingLeft = 32.dp.toPx()

        val headerPaddingTop = 24.dp.toPx() * animationFirstStageReversed

        val headerCompatPaddingRight = 32.dp.toPx()
        val headerMediumExpandedPaddingRight = 16.dp.toPx()
        val headerPaddingRight =
            headerCompatPaddingRight + (headerMediumExpandedPaddingRight - headerCompatPaddingRight) * animationFirstStage

        val headerCompatPaddingBottom = 8.dp.toPx()
        val headerPaddingBottom = headerCompatPaddingBottom * animationFirstStageReversed

        val headerCompatWidth = width.toFloat()
        val headerMediumWidth = width * 4f / 9f
        val headerExpandedWidth = width * 3f / 9f
        val headerWidth = when {
            animation == 0f -> headerCompatWidth
            animation < 1f -> headerCompatWidth + (headerMediumWidth - headerCompatWidth) * animationFirstStage
            animation == 1f -> headerMediumWidth
            animation < 2f -> headerMediumWidth + (headerExpandedWidth - headerMediumWidth) * animationSecondStage
            else -> headerExpandedWidth
        } - (headerPaddingLeft + headerPaddingRight)

        val headerHeight = height - (headerPaddingTop + headerPaddingBottom)

        val headerConstraints = Constraints(
            minWidth = 0, maxWidth = headerWidth.roundToInt(), minHeight = 0, maxHeight = headerHeight.roundToInt()
        )
        val headerPlaceable = headerMeasurable.measure(headerConstraints)

        val headerX = (headerWidth - headerPlaceable.width) * animationFirstStage + headerPaddingLeft

        val headerY = (height - headerPlaceable.height) / 2f * animationFirstStage + headerPaddingTop

        val headerActualWidth = headerWidth + headerPaddingLeft + headerPaddingRight
        val headerActualHeight = headerPlaceable.height + headerPaddingTop + headerPaddingBottom

        // Content
        val contentCompatPaddingLeft = 32.dp.toPx()
        val contentMediumExpandedPaddingLeft = 16.dp.toPx()
        val contentPaddingLeft =
            contentCompatPaddingLeft + (contentMediumExpandedPaddingLeft - contentCompatPaddingLeft) * animationFirstStage

        val contentCompatPaddingTop = 8.dp.toPx()
        val contentPaddingTop = contentCompatPaddingTop * animationFirstStageReversed

        val contentCompatMediumPaddingRight = 32.dp.toPx()
        val contentExpandedPaddingRight = 16.dp.toPx()
        val contentPaddingRight =
            contentCompatMediumPaddingRight + (contentExpandedPaddingRight - contentCompatMediumPaddingRight) * animationSecondStage

        val contentCompatWidth = width.toFloat()
        val contentMediumWidth = width * 5f / 9f
        val contentExpandedWidth = width * 4f / 9f
        val contentWidth = when {
            animation == 0f -> contentCompatWidth
            animation < 1f -> contentCompatWidth + (contentMediumWidth - contentCompatWidth) * animationFirstStage
            animation == 1f -> contentMediumWidth
            animation < 2f -> contentMediumWidth + (contentExpandedWidth - contentMediumWidth) * animationSecondStage
            else -> contentExpandedWidth
        } - (contentPaddingLeft + contentPaddingRight)

        val contentHeight =
            height - headerActualHeight * animationFirstStageReversed - (contentPaddingTop + headerCompatPaddingBottom)

        val contentConstraints = Constraints(
            minWidth = contentWidth.roundToInt(),
            maxWidth = contentWidth.roundToInt(),
            minHeight = 0,
            maxHeight = contentHeight.roundToInt()
        )
        val contentPlaceable = contentMeasurable.measure(contentConstraints)

        val contentX = headerActualWidth * animationFirstStage + contentPaddingLeft

        val contentMediumExpandedY = (height - contentPlaceable.height) / 2f
        val contentY =
            headerActualHeight + (contentMediumExpandedY - headerActualHeight) * animationFirstStage + contentPaddingTop

        val contentActualWidth = contentWidth + contentPaddingLeft + contentPaddingRight

        // Additional content
        val additionalContentCompatMediumPaddingLeft = 32.dp.toPx()
        val additionalContentExpandedPaddingLeft = 16.dp.toPx()
        val additionalContentPaddingLeft =
            additionalContentCompatMediumPaddingLeft + (additionalContentExpandedPaddingLeft - additionalContentCompatMediumPaddingLeft) * animationSecondStage

        val additionalContentPaddingRight = 32.dp.toPx()

        val additionalContentCompatMediumWidth = width.toFloat()
        val additionalContentExpandedWidth = width * 2f / 9f
        val additionalContentWidth =
            additionalContentCompatMediumWidth + (additionalContentExpandedWidth - additionalContentCompatMediumWidth) * animationSecondStage - (additionalContentPaddingLeft + additionalContentPaddingRight)

        val additionalContentConstraints = Constraints(
            minWidth = additionalContentWidth.roundToInt(),
            maxWidth = additionalContentWidth.roundToInt(),
            minHeight = 0
        )
        val additionalContentPlaceable = additionalContentMeasurable.measure(additionalContentConstraints)

        val additionalContentCompatMediumX = 0f
        val additionalContentExpandedX = headerActualWidth + contentActualWidth
        val additionalContentX =
            additionalContentCompatMediumX + (additionalContentExpandedX - additionalContentCompatMediumX) * animationSecondStage + additionalContentPaddingLeft

        val additionalContentCompatMediumY = (height - additionalContentPlaceable.height).toFloat()
        val additionalContentExpandedY = (height - additionalContentPlaceable.height) / 2f
        val additionalContentY =
            additionalContentCompatMediumY + (additionalContentExpandedY - additionalContentCompatMediumY) * animationSecondStage

        layout(width, height) {
            headerPlaceable.placeRelative(headerX.roundToInt(), headerY.roundToInt())
            contentPlaceable.placeRelative(contentX.roundToInt(), contentY.roundToInt())
            additionalContentPlaceable.placeRelative(additionalContentX.roundToInt(), additionalContentY.roundToInt())
        }
    }
}

@Composable
fun SignInComponent.State.SignIn.MoreMethods(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        FilledTonalButton(
            onClick = ::onGuestSignIn,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = "游客登录",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "游客登录")
        }
        TextButton(
            onClick = ::switchShowMoreSignInOptions,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = "更多登录选项",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "更多登录选项")
        }
        AnimatedVisibility(
            visible = showMoreSignInOptions,
        ) {
            FilledTonalButton(
                onClick = ::onAdminSignIn,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(
                    imageVector = Icons.Default.AdminPanelSettings,
                    contentDescription = "管理员登录",
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "管理员登录")
            }
        }
    }
}
