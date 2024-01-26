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

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
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
import data.AppComponentContextValues.ColorMode.*
import data.components.root.SignInComponent
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import ui.WindowSizeClass
import ui.WindowWidthSizeClass
import ui.composables.VerticalScrollbar
import ui.modifiers.applyBrush
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)
@Composable
fun SignIn(component: SignInComponent) {
    val colorMode by component.colorMode
    val pop by component.pop
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
                        painter = painterResource(DrawableResource("drawable/fhraise_logo.xml")),
                        contentDescription = "Fhraise Logo",
                    )
                },
                navigationIcon = {
                    AnimatedVisibility(
                        visible = pop != null,
                        enter = slideInHorizontally { -it },
                        exit = slideOutHorizontally { -it },
                    ) {
                        IconButton(
                            onClick = pop ?: {},
                            content = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                    contentDescription = "返回",
                                )
                            },
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = component::switchColorMode,
                        content = {
                            Icon(
                                imageVector = when (colorMode) {
                                    LIGHT -> Icons.Default.LightMode
                                    DARK -> Icons.Default.DarkMode
                                    SYSTEM -> Icons.Default.Adjust
                                },
                                contentDescription = "当前颜色模式：${component.colorMode.value.displayName}，改变到：${component.nextColorMode.displayName}",
                            )
                        },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { component.SnackbarHost() },
        contentWindowInsets = WindowInsets.safeDrawing,
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
                    Spacer(modifier = Modifier.height(16.dp))
                    with(state) {
                        if (this is SignInComponent.ComponentState.SignIn) {
                            PhoneNumber()
                            AnimatedVisibility(visible = canInputVerifyCode) {
                                Spacer(modifier = Modifier.height(16.dp))
                                VerifyCode()
                            }
                            Spacer(modifier = Modifier.height(32.dp))
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                NextOrSubmitButton()
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }
            },
            additionalContent = {
                if (state is SignInComponent.ComponentState.SignIn) {
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
    scrollState: ScrollState = rememberScrollState(),
    additionalContentScrollState: ScrollState = rememberScrollState(),
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
        val contentPaddingHorizontal = contentPaddingLeft + contentPaddingRight

        // == Layout sizes ==
        val width = constraints.maxWidth
        val height = constraints.maxHeight
        val safeHeight = (height - contentPaddingVertical).coerceAtLeast(0f)
        windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(width.toDp(), height.toDp())).widthSizeClass

        // == Animation ==
        if (animatable == null && animationTargetValue != null) animatable = Animatable(animationTargetValue)
        if (animation == null) return@SubcomposeLayout layout(0, 0) {}
        val animationSecondStage = (animation - 1f).coerceAtLeast(0f)
        val animationSecondStageReversed = 1f - animationSecondStage

        // == Measure additional content ==
        val additionalContentMeasurable = subcompose(slotId = "additionalContent") {
            Box(modifier = Modifier.verticalScroll(state = additionalContentScrollState)) {
                additionalContent()
            }
        }.firstOrNull() ?: return@SubcomposeLayout layout(0, 0) {}

        val additionalContentCompatMediumPaddingLeft = 32.dp.toPx()
        val additionalContentExpandedPaddingLeft = 16.dp.toPx()
        val additionalContentPaddingLeft =
            additionalContentCompatMediumPaddingLeft + (additionalContentExpandedPaddingLeft - additionalContentCompatMediumPaddingLeft) * animationSecondStage

        val additionalContentPaddingTop = 8.dp.toPx() * animationSecondStageReversed

        val additionalContentPaddingRight = 32.dp.toPx()

        val additionalContentPaddingBottom = 16.dp.toPx() * animationSecondStageReversed

        val additionalContentPaddingHorizontal = additionalContentPaddingLeft + additionalContentPaddingRight
        val additionalContentPaddingVertical = additionalContentPaddingTop + additionalContentPaddingBottom

        val additionalContentCompatMediumWidth = width.toFloat()
        val additionalContentExpandedWidth = width * 2f / 9f
        val additionalContentWidth =
            (additionalContentCompatMediumWidth + (additionalContentExpandedWidth - additionalContentCompatMediumWidth) * animationSecondStage - additionalContentPaddingHorizontal - contentPaddingHorizontal).roundToInt()
                .coerceAtLeast(0)

        val additionalContentHeight = safeHeight - additionalContentPaddingVertical

        val additionalContentConstraints = Constraints(
            minWidth = additionalContentWidth,
            maxWidth = additionalContentWidth,
            minHeight = 0,
            maxHeight = additionalContentHeight.roundToInt().coerceAtLeast(0)
        )
        val additionalContentPlaceable = additionalContentMeasurable.measure(additionalContentConstraints)

        val additionalContentActualWidth =
            additionalContentPlaceable.width + additionalContentPaddingHorizontal + contentPaddingHorizontal
        val additionalContentActualHeight = additionalContentPlaceable.height + additionalContentPaddingVertical

        // == Measure main ==
        val mainBottomSpace =
            ((additionalContentPlaceable.height + additionalContentPaddingVertical) * animationSecondStageReversed)

        val mainMeasurable = subcompose("main") {
            SignInMainLayout(
                animation = animation,
                animationSecondStage = animationSecondStage,
                scrollState = scrollState,
                contentPaddingLeft = contentPaddingLeft,
                contentPaddingTop = contentPaddingTop,
                contentPaddingRight = contentPaddingRight,
                contentPaddingBottom = contentPaddingBottom,
                bottomSpace = mainBottomSpace.roundToInt(),
                header = header,
                mainContent = content,
            )
        }.first()

        val mainCompatWidth = width.toFloat()
        val mainMediumExpandedWidth = width * 7f / 9f
        val mainWidth =
            (mainCompatWidth + (mainMediumExpandedWidth - mainCompatWidth) * animationSecondStage).coerceAtLeast(0f)
                .roundToInt()

        val mainConstraints = Constraints.fixed(width = mainWidth, height = height)
        val mainPlaceable = mainMeasurable.measure(mainConstraints)

        // == Place ==
        val additionalContentCompatMediumX = 0f
        val additionalContentX =
            additionalContentCompatMediumX + (mainPlaceable.width - additionalContentCompatMediumX) * animationSecondStage + additionalContentPaddingLeft + contentPaddingLeft

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
        }.first().measure(
            Constraints.fixed(
                width = width, height = (height - additionalContentY).roundToInt().coerceAtLeast(0)
            )
        )

        val additionalContentBackgroundY = additionalContentY + (height - additionalContentY) * animationSecondStage

        // == Scrollbars ==
        val mainScrollbarHeight = (safeHeight - mainBottomSpace).roundToInt().coerceAtLeast(0)

        val mainScrollbarPlaceable = subcompose("mainScrollBar") {
            VerticalScrollbar(scrollState = scrollState)
        }.firstOrNull()?.measure(
            Constraints(
                maxWidth = mainWidth, minHeight = mainScrollbarHeight, maxHeight = mainScrollbarHeight
            )
        )

        val mainScrollbarX = mainScrollbarPlaceable?.let { mainWidth - mainScrollbarPlaceable.width }
        val mainScrollbarY = contentPaddingTop.roundToInt()

        val additionalContentScrollbarHeight = additionalContentActualHeight.roundToInt().coerceAtLeast(0)

        val additionalContentScrollbarPlaceable = subcompose("additionalContentScrollbar") {
            VerticalScrollbar(scrollState = additionalContentScrollState)
        }.firstOrNull()?.measure(
            Constraints(
                maxWidth = additionalContentWidth,
                minHeight = additionalContentScrollbarHeight,
                maxHeight = additionalContentScrollbarHeight
            )
        )

        val additionalContentScrollbarX =
            additionalContentScrollbarPlaceable?.let { additionalContentX - additionalContentPaddingLeft - contentPaddingLeft + additionalContentActualWidth - additionalContentScrollbarPlaceable.width }

        layout(width, height) {
            mainPlaceable.placeRelative(0, 0)
            mainScrollbarX?.let { mainScrollbarPlaceable.placeRelative(mainScrollbarX, mainScrollbarY) }
            additionalContentBackgroundPlaceable.placeRelative(0, additionalContentBackgroundY.roundToInt())
            additionalContentPlaceable.placeRelative(additionalContentX.roundToInt(), additionalContentY.roundToInt())
            additionalContentScrollbarX?.let {
                additionalContentScrollbarPlaceable.placeRelative(
                    additionalContentScrollbarX.roundToInt(), additionalContentY.roundToInt()
                )
            }
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
            val headerMeasurable = subcompose("header", header).firstOrNull() ?: return@SubcomposeLayout layout(0, 0) {}
            val mainContentMeasurable =
                subcompose("mainContent", mainContent).firstOrNull() ?: return@SubcomposeLayout layout(0, 0) {}

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

            // == Measure header ==
            val headerHeight = headerIntrinsicHeight.roundToInt().coerceAtLeast(0)

            val headerConstraints = Constraints(
                maxWidth = headerWidth.roundToInt().coerceAtLeast(0), maxHeight = headerHeight
            )
            val headerPlaceable = headerMeasurable.measure(headerConstraints)

            val headerActualWidth = headerWidth + headerPaddingHorizontal

            val headerX = (headerWidth - headerPlaceable.width) * animationFirstStage + headerPaddingLeft

            val headerY =
                ((boxHeight - headerIntrinsicActualHeight) / 2f + scrollState.value) * animationFirstStage + headerPaddingTop

            // == Measure main content ==
            val mainContentHeight = mainContentIntrinsicHeight.roundToInt()

            val mainContentConstraintWidth = mainContentWidth.coerceAtMost(mainContentMaxWidth).roundToInt()
            val mainContentConstraints = Constraints(
                maxWidth = mainContentConstraintWidth.coerceAtLeast(0), maxHeight = mainContentHeight.coerceAtLeast(0)
            )
            val mainContentPlaceable = mainContentMeasurable.measure(mainContentConstraints)

            val mainContentActualHeight = mainContentPlaceable.height + mainContentPaddingVertical

            val mainContentX =
                headerActualWidth * animationFirstStage + (mainContentWidth - mainContentConstraintWidth) / 2f + mainContentPaddingLeft

            val requiredColumnHeight = headerIntrinsicActualHeight + mainContentActualHeight
            val requiredRowHeight = max(headerIntrinsicActualHeight, mainContentActualHeight)
            val requiredHeight = requiredColumnHeight + (requiredRowHeight - requiredColumnHeight) * animationFirstStage
            val requiredBoxHeight = max(boxHeight, requiredHeight)

            val mainContentCompatY =
                (requiredBoxHeight - headerIntrinsicActualHeight - mainContentActualHeight) / 2f + headerIntrinsicActualHeight
            val mainContentMediumExpandedY = (requiredBoxHeight - mainContentActualHeight) / 2f
            val mainContentY =
                mainContentCompatY + (mainContentMediumExpandedY - mainContentCompatY) * animationFirstStage + mainContentPaddingTop

            val requiredContentHeight = requiredBoxHeight.roundToInt() + bottomSpace
            layout(width, requiredContentHeight) {
                headerPlaceable.placeRelative(headerX.roundToInt(), headerY.roundToInt())
                mainContentPlaceable.placeRelative(mainContentX.roundToInt(), mainContentY.roundToInt())
            }
        }
    }
}

@Composable
fun SignInComponent.ComponentState.SignIn.MoreMethods(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        FilledTonalButton(
            onClick = onGuestSignIn,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Default.NoAccounts,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "游客登录")
        }
        FilledTonalButton(
            onClick = onFaceSignIn,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Default.Face,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "人脸登录")
        }
        TextButton(
            onClick = ::switchShowMoreSignInOptions,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Default.MoreHoriz,
                contentDescription = null,
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
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "管理员登录")
            }
        }
    }
}

@Composable
fun SignInComponent.ComponentState.UsernamePasswordState.UserName() {
    OutlinedTextField(
        value = username,
        onValueChange = ::username::set,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        label = { Text(text = "用户名") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text, imeAction = ImeAction.Next
        ),
        maxLines = 1,
    )
}

@Composable
fun SignInComponent.ComponentState.UsernamePasswordState.Password() {
    OutlinedTextField(
        value = password,
        onValueChange = ::password::set,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        label = { Text(text = "密码") },
        trailingIcon = {
            IconButton(
                onClick = ::switchShowPassword,
                content = {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = if (showPassword) "隐藏密码" else "显示密码",
                    )
                },
            )
        },
        visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = onDone),
        maxLines = 1,
    )
}

@Composable
fun SignInComponent.ComponentState.PhoneNumberVerifyCodeState.PhoneNumber() {
    OutlinedTextField(
        value = phoneNumber,
        onValueChange = ::phoneNumber::set,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        label = { Text(text = "手机号") },
        prefix = { Text(text = "+86") },
        supportingText = {
            AnimatedVisibility(
                visible = !phoneNumberVerified,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(text = "手机号格式不正确")
            }
        },
        isError = !phoneNumberVerified,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Phone, imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = onNext),
        maxLines = 1,
    )
}

@Composable
fun SignInComponent.ComponentState.PhoneNumberVerifyCodeState.VerifyCode() {
    OutlinedTextField(
        value = verifyCode,
        onValueChange = ::verifyCode::set,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        label = { Text(text = "验证码") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = onDone),
        maxLines = 1,
    )
}

@Composable
fun SignInComponent.ComponentState.PhoneNumberVerifyCodeState.NextOrSubmitButton() {
    Button(
        onClick = ::nextOrSubmit,
        shape = MaterialTheme.shapes.large,
    ) {
        AnimatedContent(
            targetState = canInputVerifyCode,
            transitionSpec = {
                if (targetState) {
                    fadeIn() + slideInHorizontally { -it } togetherWith fadeOut() + slideOutHorizontally { it }
                } else {
                    fadeIn() + slideInHorizontally { it } togetherWith fadeOut() + slideOutHorizontally { -it }
                }
            },
        ) { targetState ->
            if (targetState) {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.ArrowForward,
                    contentDescription = "登录",
                )
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Default.Send,
                    contentDescription = "发送验证码",
                )
            }
        }
    }
}
