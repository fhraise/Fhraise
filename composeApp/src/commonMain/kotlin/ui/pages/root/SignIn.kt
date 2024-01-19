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
import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
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
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.WindowSizeClass
import ui.WindowWidthSizeClass
import ui.modifiers.applyBrush
import kotlin.math.max
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
        contentWindowInsets = WindowInsets.safeContent,
    ) { paddingValues ->
        SignInLayout(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
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
        )
    }
}

@Composable
fun SignInLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
    additionalContent: @Composable () -> Unit,
) {
    var windowSizeClass: WindowWidthSizeClass? by remember { mutableStateOf(null) }
    val animationTargetValue = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 0f
        WindowWidthSizeClass.Medium -> 1f
        WindowWidthSizeClass.Expanded -> 2f
        else -> null
    }

    var animatable: Animatable<Float, AnimationVector1D>? by remember { mutableStateOf(null) }
    val animation = animatable?.value
    val firstPrintAnimation by animateFloatAsState(
        targetValue = if (animation == null) 0f else 1f, animationSpec = spring(stiffness = Spring.StiffnessLow)
    )

    SubcomposeLayout(
        modifier = Modifier.alpha(firstPrintAnimation).offset(y = 32.dp * (1f - firstPrintAnimation)).then(modifier)
    ) { constraints ->
        // == Content padding ==
        val contentPaddingLeft = contentPadding.calculateLeftPadding(layoutDirection).toPx()
        val contentPaddingTop = contentPadding.calculateTopPadding().toPx()
        val contentPaddingRight = contentPadding.calculateRightPadding(layoutDirection).toPx()
        val contentPaddingBottom = contentPadding.calculateBottomPadding().toPx()
        val contentPaddingVertical = contentPaddingTop + contentPaddingBottom

        // == Layout sizes ==
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val safeHeight = height - contentPaddingVertical
        windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(width.toDp(), height.toDp())).widthSizeClass

        // == Animation ==
        if (animatable == null && animationTargetValue != null) animatable = Animatable(animationTargetValue)
        if (animation == null) return@SubcomposeLayout layout(width, height) {}
        val animationSecondStage = (animation - 1f).coerceAtLeast(0f)
        val animationSecondStageReversed = 1f - animationSecondStage

        // == Measure additional content ==
        val additionalContentMeasurable = subcompose(slotId = "additionalContent", content = additionalContent).first()

        val additionalContentCompatMediumPaddingLeft = 32.dp.toPx()
        val additionalContentExpandedPaddingLeft = 16.dp.toPx()
        val additionalContentPaddingLeft =
            additionalContentCompatMediumPaddingLeft + (additionalContentExpandedPaddingLeft - additionalContentCompatMediumPaddingLeft) * animationSecondStage

        val additionalContentPaddingRight = 32.dp.toPx()

        val additionalContentPaddingBottom = 16.dp.toPx() * animationSecondStageReversed

        val additionalContentCompatMediumWidth = width.toFloat()
        val additionalContentExpandedWidth = width * 2f / 9f
        val additionalContentWidth =
            additionalContentCompatMediumWidth + (additionalContentExpandedWidth - additionalContentCompatMediumWidth) * animationSecondStage - (additionalContentPaddingLeft + additionalContentPaddingRight)

        val additionalContentConstraints = Constraints.fixedWidth(width = additionalContentWidth.roundToInt())
        val additionalContentPlaceable = additionalContentMeasurable.measure(additionalContentConstraints)

        // == Measure main ==
        val mainMeasurable = subcompose("main") {
            SignInMainLayout(
                animation = animation,
                animationSecondStage = animationSecondStage,
                scrollState = rememberScrollState(),
                contentPaddingLeft = contentPaddingLeft,
                contentPaddingTop = contentPaddingTop,
                contentPaddingRight = contentPaddingRight,
                contentPaddingBottom = contentPaddingBottom,
                bottomSpace = (additionalContentPlaceable.height * animationSecondStageReversed).roundToInt(),
                header = header,
                mainContent = content,
            )
        }.first()

        val mainCompatWidth = width.toFloat()
        val mainMediumExpandedWidth = width * 7f / 9f
        val mainWidth = mainCompatWidth + (mainMediumExpandedWidth - mainCompatWidth) * animationSecondStage

        val mainConstraints = Constraints.fixed(width = mainWidth.roundToInt(), height = height)
        val mainPlaceable = mainMeasurable.measure(mainConstraints)

        val mainActualWidth = mainPlaceable.width

        // == Place ==
        val additionalContentCompatMediumX = 0f
        val additionalContentX =
            additionalContentCompatMediumX + (mainActualWidth - additionalContentCompatMediumX) * animationSecondStage + additionalContentPaddingLeft

        val additionalContentCompatMediumY =
            (height - additionalContentPlaceable.height).toFloat() - additionalContentPaddingBottom - contentPaddingBottom
        val additionalContentExpandedY = (safeHeight - additionalContentPlaceable.height) / 2f + contentPaddingTop
        val additionalContentY =
            additionalContentCompatMediumY + (additionalContentExpandedY - additionalContentCompatMediumY) * animationSecondStage

        val additionalContentBackgroundPlaceable = subcompose("additionalContentBackground") {
            val backgroundColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
            Column(modifier = Modifier.fillMaxSize()) {
                Spacer(
                    modifier = Modifier.fillMaxWidth().height(32.dp).background(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent, backgroundColor
                            )
                        )
                    )
                )
                Spacer(modifier = Modifier.fillMaxSize().background(color = backgroundColor))
            }
        }.first().measure(Constraints.fixed(width = width, height = (height - additionalContentY).roundToInt()))

        val additionalContentBackgroundY = additionalContentY + (height - additionalContentY) * animationSecondStage

        layout(width, height) {
            mainPlaceable.placeRelative(0, 0)
            additionalContentBackgroundPlaceable.placeRelative(0, additionalContentBackgroundY.roundToInt())
            additionalContentPlaceable.placeRelative(additionalContentX.roundToInt(), additionalContentY.roundToInt())
        }
    }

    LaunchedEffect(windowSizeClass) {
        if (animation != null && animationTargetValue != null && animation != animationTargetValue) {
            launch {
                animatable?.animateTo(
                    targetValue = animationTargetValue, animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                )
            }
        }
    }
}

@Composable
fun SignInMainLayout(
    animation: Float,
    animationSecondStage: Float,
    scrollState: ScrollState,
    contentPaddingLeft: Float,
    contentPaddingTop: Float,
    contentPaddingRight: Float,
    contentPaddingBottom: Float,
    bottomSpace: Int,
    header: @Composable () -> Unit,
    mainContent: @Composable () -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxHeight = constraints.maxHeight.toFloat() - bottomSpace

        SubcomposeLayout(
            modifier = Modifier.fillMaxSize().verticalScroll(state = scrollState)
        ) { layoutConstraints ->
            val headerMeasurable = subcompose("header", header).first()
            val mainContentMeasurable = subcompose("mainContent", mainContent).first()

            // == Layout size ==
            val width = layoutConstraints.maxWidth

            // == Animation ==
            val animationFirstStage = animation.coerceAtMost(1f)
            val animationFirstStageReversed = 1f - animationFirstStage

            // == Header's padding and width ==
            val headerPaddingLeft = 32.dp.toPx() + contentPaddingLeft

            // 24dp if compact, 0dp otherwise
            val headerPaddingTop = 24.dp.toPx() * animationFirstStageReversed + contentPaddingTop

            val headerCompatPaddingRight = 32.dp.toPx() + contentPaddingRight
            val headerMediumExpandedPaddingRight = 16.dp.toPx()
            val headerPaddingRight =
                headerCompatPaddingRight + (headerMediumExpandedPaddingRight - headerCompatPaddingRight) * animationFirstStage

            val headerCompatPaddingBottom = 8.dp.toPx()
            val headerPaddingBottom =
                headerCompatPaddingBottom + (contentPaddingBottom - headerCompatPaddingBottom) * animationFirstStage

            val headerPaddingHorizontal = headerPaddingLeft + headerPaddingRight
            val headerPaddingVertical = headerPaddingTop + headerPaddingBottom

            val headerCompatWidth = width.toFloat()
            val headerMediumWidth = width * 4f / 9f
            val headerExpandedWidth = width * 3f / 7f
            val headerWidth = when {
                animation == 0f -> headerCompatWidth
                animation < 1f -> headerCompatWidth + (headerMediumWidth - headerCompatWidth) * animationFirstStage
                animation == 1f -> headerMediumWidth
                animation < 2f -> headerMediumWidth + (headerExpandedWidth - headerMediumWidth) * animationSecondStage
                else -> headerExpandedWidth
            } - headerPaddingHorizontal

            // == Main content's padding and width ==
            val mainContentCompatPaddingLeft = 32.dp.toPx() + contentPaddingLeft
            val mainContentMediumExpandedPaddingLeft = 16.dp.toPx()
            val mainContentPaddingLeft =
                mainContentCompatPaddingLeft + (mainContentMediumExpandedPaddingLeft - mainContentCompatPaddingLeft) * animationFirstStage

            val mainContentCompatPaddingTop = 8.dp.toPx()
            val mainContentMediumExpandedPaddingTop = 16.dp.toPx() + contentPaddingTop
            val mainContentPaddingTop =
                mainContentCompatPaddingTop + (mainContentMediumExpandedPaddingTop - mainContentCompatPaddingTop) * animationFirstStage

            val mainContentCompatMediumPaddingRight = 32.dp.toPx() + contentPaddingRight
            val mainContentExpandedPaddingRight = 16.dp.toPx()
            val mainContentPaddingRight =
                mainContentCompatMediumPaddingRight + (mainContentExpandedPaddingRight - mainContentCompatMediumPaddingRight) * animationSecondStage

            val mainContentPaddingBottom = 16.dp.toPx() + contentPaddingBottom

            val mainContentPaddingHorizontal = mainContentPaddingLeft + mainContentPaddingRight
            val mainContentPaddingVertical = mainContentPaddingTop + mainContentPaddingBottom

            val mainContentMaxWidth = 512.dp.toPx()
            val mainContentCompatWidth = width.toFloat()
            val mainContentMediumWidth = width * 5f / 9f
            val mainContentExpandedWidth = width * 4f / 7f
            val mainContentWidth = when {
                animation == 0f -> mainContentCompatWidth
                animation < 1f -> mainContentCompatWidth + (mainContentMediumWidth - mainContentCompatWidth) * animationFirstStage
                animation == 1f -> mainContentMediumWidth
                animation < 2f -> mainContentMediumWidth + (mainContentExpandedWidth - mainContentMediumWidth) * animationSecondStage
                else -> mainContentExpandedWidth
            } - mainContentPaddingHorizontal

            // == Intrinsic height ==
            val headerIntrinsicHeight = headerMeasurable.minIntrinsicHeight(headerWidth.roundToInt()).toFloat()
            val mainContentIntrinsicHeight =
                mainContentMeasurable.minIntrinsicHeight(mainContentWidth.roundToInt()).toFloat()
            val headerIntrinsicActualHeight = headerIntrinsicHeight + headerPaddingVertical
            val mainContentIntrinsicActualHeight = mainContentIntrinsicHeight + mainContentPaddingVertical
            val requiredColumnIntrinsicHeight = headerIntrinsicActualHeight + mainContentIntrinsicActualHeight
            val requiredRowIntrinsicHeight = max(headerIntrinsicActualHeight, mainContentIntrinsicActualHeight)
            val requiredIntrinsicHeight =
                requiredColumnIntrinsicHeight + (requiredRowIntrinsicHeight - requiredColumnIntrinsicHeight) * animationFirstStage
            val requiredBoxHeight = max(boxHeight, requiredIntrinsicHeight)

            // == Measure header ==
            val headerHeight = headerIntrinsicHeight.roundToInt()

            val headerConstraints = Constraints(
                maxWidth = headerWidth.roundToInt(), minHeight = headerHeight, maxHeight = headerHeight
            )
            val headerPlaceable = headerMeasurable.measure(headerConstraints)

            val headerActualWidth = headerWidth + headerPaddingHorizontal
            val headerActualHeight = headerPlaceable.height + headerPaddingVertical

            val headerX = (headerWidth - headerPlaceable.width) * animationFirstStage + headerPaddingLeft

            val headerY =
                ((boxHeight - headerActualHeight) / 2f + scrollState.value) * animationFirstStage + headerPaddingTop

            // == Measure main content ==
            val mainContentHeight = mainContentIntrinsicHeight.roundToInt()

            val mainContentConstraintWidth = mainContentWidth.coerceAtMost(mainContentMaxWidth).roundToInt()
            val mainContentConstraints =
                Constraints.fixed(width = mainContentConstraintWidth, height = mainContentHeight)
            val mainContentPlaceable = mainContentMeasurable.measure(mainContentConstraints)

            val mainContentActualHeight = mainContentPlaceable.height + mainContentPaddingVertical

            val mainContentX =
                headerActualWidth * animationFirstStage + (mainContentWidth - mainContentConstraintWidth) / 2f + mainContentPaddingLeft

            val mainContentMediumExpandedY = (requiredBoxHeight - mainContentActualHeight) / 2f
            val mainContentY =
                headerActualHeight + (mainContentMediumExpandedY - headerActualHeight) * animationFirstStage + mainContentPaddingTop

            val requiredMainContentHeight = requiredBoxHeight.roundToInt() + bottomSpace
            layout(width, requiredMainContentHeight) {
                headerPlaceable.placeRelative(headerX.roundToInt(), headerY.roundToInt())
                mainContentPlaceable.placeRelative(mainContentX.roundToInt(), mainContentY.roundToInt())
            }
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
        FilledTonalButton(
            onClick = ::onPhoneSignIn,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = "手机号登录",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "手机号登录")
        }
        FilledTonalButton(
            onClick = ::onRegister,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Default.PersonAdd,
                contentDescription = "注册",
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "注册")
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
