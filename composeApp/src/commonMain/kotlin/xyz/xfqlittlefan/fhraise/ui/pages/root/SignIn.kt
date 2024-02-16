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

package xyz.xfqlittlefan.fhraise.ui.pages.root

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
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import xyz.xfqlittlefan.fhraise.data.AppComponentContextValues.ColorMode.*
import xyz.xfqlittlefan.fhraise.data.componentScope
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.Step.*
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.VerificationType.*
import xyz.xfqlittlefan.fhraise.defaultServerPort
import xyz.xfqlittlefan.fhraise.icon.AppIcons
import xyz.xfqlittlefan.fhraise.icon.Github
import xyz.xfqlittlefan.fhraise.icon.Google
import xyz.xfqlittlefan.fhraise.icon.Microsoft
import xyz.xfqlittlefan.fhraise.rememberMutableState
import xyz.xfqlittlefan.fhraise.routes.Api
import xyz.xfqlittlefan.fhraise.ui.*
import xyz.xfqlittlefan.fhraise.ui.composables.TypeWriter
import xyz.xfqlittlefan.fhraise.ui.composables.VerticalScrollbar
import xyz.xfqlittlefan.fhraise.ui.composables.safeDrawingWithoutIme
import xyz.xfqlittlefan.fhraise.ui.modifiers.applyBrush
import kotlin.math.max
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class, ExperimentalLayoutApi::class)
@Composable
fun SignInComponent.SignIn() {
    val colorMode by settings.colorMode.collectAsState()
    val pop by pop
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
                        onClick = ::switchColorMode,
                        content = {
                            Icon(
                                imageVector = when (colorMode) {
                                    LIGHT -> Icons.Default.LightMode
                                    DARK -> Icons.Default.DarkMode
                                    SYSTEM -> Icons.Default.Adjust
                                },
                                contentDescription = "当前颜色模式：${colorMode.displayName}，改变到：${colorMode.next.displayName}",
                            )
                        },
                    )
                },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost() },
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { paddingValues ->
        SignInLayout(
            modifier = Modifier.fillMaxSize(),
            contentPadding = paddingValues,
            header = {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.displayMedium) {
                    TypeWriter(
                        text = "开启你的\n 美食之旅",
                        modifier = Modifier.padding(it).applyBrush(
                            brush = Brush.horizontalGradient(
                                listOf(Color.Red.copy(alpha = 0.7f), Color.Blue.copy(alpha = 0.7f))
                            )
                        ),
                        infinite = false,
                    )
                }
            },
            content = { padding ->
                AnimatedContent(
                    targetState = step,
                    transitionSpec = {
                        if (targetState.ordinal > initialState.ordinal) {
                            slideInVertically { it / 2 } + fadeIn() togetherWith slideOutHorizontally() + fadeOut()
                        } else {
                            slideInHorizontally() + fadeIn() togetherWith slideOutVertically { it / 2 } + fadeOut()
                        } using SizeTransform(clip = false)
                    },
                    contentAlignment = Alignment.Center,
                ) { step ->
                    when (step) {
                        EnteringCredential -> {
                            Column(modifier = Modifier.padding(padding).fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = step.displayName, style = MaterialTheme.typography.headlineSmall)
                                Spacer(modifier = Modifier.height(16.dp))
                                FlowRow(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
                                ) {
                                    Api.Auth.Type.CredentialType.entries.forEach {
                                        FilterChip(
                                            selected = credentialType == it,
                                            onClick = it.use,
                                            label = { Text(text = "使用${it.displayName}") },
                                            enabled = step == EnteringCredential,
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Credential()
                                Spacer(modifier = Modifier.height(32.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ForwardButton(requiredStep = step)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        SelectingVerification -> {
                            Column(modifier = Modifier.padding(padding).fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                BackButton(requiredStep = step)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = step.displayName, style = MaterialTheme.typography.headlineSmall)
                                defaultVerifications.forEach { verification ->
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Card(
                                        onClick = verification.use, modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            modifier = Modifier.padding(16.dp),
                                        ) {
                                            Icon(
                                                imageVector = verification.icon,
                                                contentDescription = null,
                                            )
                                            Spacer(modifier = Modifier.width(16.dp))
                                            Text(text = verification.displayName)
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        Verification -> {
                            val type = verificationType ?: error("Verification type is not selected")
                            Column(modifier = Modifier.padding(padding).fillMaxWidth()) {
                                Spacer(modifier = Modifier.height(16.dp))
                                BackButton(requiredStep = step)
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(text = step.displayName, style = MaterialTheme.typography.headlineSmall)
                                Spacer(modifier = Modifier.height(16.dp))
                                when (type) {
                                    is FhraiseToken, is SmsCode, is EmailCode -> {
                                        VerificationCode()
                                    }

                                    is Password -> {
                                        Password()
                                    }

                                    is QrCode, is Face -> {}
                                }
                                Spacer(modifier = Modifier.height(32.dp))
                                Box(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 32.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    ForwardButton(requiredStep = step)
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                            }
                        }

                        Done -> {}
                    }
                }
            },
            additionalContent = {
                MoreMethods(modifier = Modifier.fillMaxWidth().padding(it))
            },
        )
    }

    if (showServerSettings) {
        var host by serverHost.rememberMutableState()
        var port by serverPort.rememberMutableState()

        AlertDialog(
            onDismissRequest = ::hideServerSettings,
            confirmButton = {
                Button(onClick = ::hideServerSettings) {
                    Text("确定")
                }
            },
            title = { Text("服务器设置") },
            text = {
                Column {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        maxLines = 1,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    OutlinedTextField(
                        value = port.toString(),
                        onValueChange = { port = it.toIntOrNull() ?: defaultServerPort },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(onDone = hideServerSettingsAction),
                        maxLines = 1,
                    )
                }
            },
        )
    }
}


@Composable
private fun SignInLayout(
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(0.dp),
    scrollState: ScrollState = rememberScrollState(),
    additionalContentScrollState: ScrollState = rememberScrollState(),
    header: @Composable (PaddingValues) -> Unit,
    content: @Composable (PaddingValues) -> Unit,
    additionalContent: @Composable (PaddingValues) -> Unit,
) {
    var windowSizeClass: WindowWidthSizeClass? by remember { mutableStateOf(null) }
    val animationTargetValue = when (windowSizeClass) {
        WindowWidthSizeClass.Compact -> 0f
        WindowWidthSizeClass.Medium -> 1f
        WindowWidthSizeClass.Expanded -> 2f
        else -> null
    }

    val insetsWithoutIme = WindowInsets.safeDrawingWithoutIme

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
        val safeHeight = (height - contentPaddingVertical).coerceAtLeast(0f)
        windowSizeClass = WindowSizeClass.calculateFromSize(DpSize(width.toDp(), height.toDp())).widthSizeClass

        // == Animation ==
        if (animatable == null && animationTargetValue != null) animatable = Animatable(animationTargetValue)
        if (animation == null) return@SubcomposeLayout layout(0, 0) {}

        with(SignInLayoutScope(this, animation)) {
            // == Measure additional content ==
            val insetsBottom = insetsWithoutIme.getBottom(this@SubcomposeLayout)

            val additionalContentWidth = (((width) animatedSecondStageTo (width * 2f / 9f))).roundToInt()

            val additionalContentMeasurable = subcompose(slotId = "additionalContent") {
                Box(modifier = Modifier.verticalScroll(state = additionalContentScrollState)) {
                    additionalContent(
                        PaddingValues(
                            start = ((32 animatedSecondStageTo 16).dpAsPx + contentPaddingLeft).toDp(),
                            top = (8.dpAsPx animatedFirstStageTo 0).toDp(),
                            end = (32.dpAsPx + contentPaddingRight).toDp(),
                            bottom = (16.dpAsPx + insetsBottom animatedSecondStageTo 0).toDp()
                        )
                    )
                }
            }.first()

            val additionalContentConstraints = Constraints(
                minWidth = additionalContentWidth,
                maxWidth = additionalContentWidth,
                minHeight = 0,
                maxHeight = safeHeight.roundToInt().coerceAtLeast(0)
            )
            val additionalContentPlaceable = additionalContentMeasurable.measure(additionalContentConstraints)

            // == Measure main ==
            val mainPaddingBottom = max(contentPaddingBottom, additionalContentPlaceable.height animatedSecondStageTo 0)

            val mainMeasurable = subcompose("main") {
                SignInMainLayout(
                    scrollState = scrollState,
                    contentPaddingLeft = contentPaddingLeft,
                    contentPaddingTop = contentPaddingTop,
                    contentPaddingRight = contentPaddingRight,
                    contentPaddingBottom = mainPaddingBottom,
                    header = header,
                    mainContent = content,
                )
            }.first()

            val mainWidth = (width animatedSecondStageTo (width * 7f / 9f)).coerceAtLeast(0f).roundToInt()

            val mainConstraints = Constraints.fixed(width = mainWidth, height = height)
            val mainPlaceable = mainMeasurable.measure(mainConstraints)

            // == Place ==
            val additionalContentX = 0 animatedSecondStageTo mainPlaceable.width

            val additionalContentCompatMediumY = height - additionalContentPlaceable.height
            val additionalContentExpandedY = (safeHeight - additionalContentPlaceable.height) / 2f + contentPaddingTop
            val additionalContentY = additionalContentCompatMediumY animatedSecondStageTo additionalContentExpandedY

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
            }.first().measure(Constraints.fixed(width = width, height = additionalContentPlaceable.height))

            val additionalContentBackgroundY = additionalContentY animatedSecondStageTo height

            // == Scrollbars ==
            val mainScrollbarHeight = (height - contentPaddingTop - mainPaddingBottom).roundToInt().coerceAtLeast(0)

            val mainScrollbarPlaceable = subcompose("mainScrollBar") {
                VerticalScrollbar(scrollState = scrollState)
            }.firstOrNull()?.measure(
                Constraints(maxWidth = mainWidth, minHeight = mainScrollbarHeight, maxHeight = mainScrollbarHeight)
            )

            val mainScrollbarX = mainScrollbarPlaceable?.let { mainWidth - mainScrollbarPlaceable.width }
            val mainScrollbarY = contentPaddingTop.roundToInt()

            val additionalContentScrollbarHeight =
                (additionalContentPlaceable.height - (insetsBottom animatedSecondStageTo 0)).roundToInt()
                    .coerceAtLeast(0)

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
                additionalContentScrollbarPlaceable?.let { additionalContentX + additionalContentPlaceable.width - additionalContentScrollbarPlaceable.width }

            layout(width, height) {
                mainPlaceable.placeRelative(0, 0)
                mainScrollbarX?.let { mainScrollbarPlaceable.placeRelative(mainScrollbarX, mainScrollbarY) }
                additionalContentBackgroundPlaceable.placeRelative(
                    0, additionalContentBackgroundY.roundToInt()
                )
                additionalContentPlaceable.placeRelative(
                    additionalContentX.roundToInt(), additionalContentY.roundToInt()
                )
                additionalContentScrollbarX?.let {
                    additionalContentScrollbarPlaceable.placeRelative(
                        additionalContentScrollbarX.roundToInt(), additionalContentY.roundToInt()
                    )
                }
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
private fun SignInLayoutScope.SignInMainLayout(
    scrollState: ScrollState,
    contentPaddingLeft: Float,
    contentPaddingTop: Float,
    contentPaddingRight: Float,
    contentPaddingBottom: Float,
    header: @Composable (PaddingValues) -> Unit,
    mainContent: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val boxHeight = constraints.maxHeight

        SubcomposeLayout(modifier = Modifier.fillMaxSize().verticalScroll(state = scrollState)) { layoutConstraints ->
            // == Layout size ==
            val width = layoutConstraints.maxWidth

            // == Header's padding and width ==
            val headerPaddingLeft = 32.dpAsPx + contentPaddingLeft
            val headerPaddingTop = (24.dpAsPx animatedFirstStageTo 0) + contentPaddingTop
            val headerPaddingRight = 32.dpAsPx + contentPaddingRight animatedFirstStageTo 16.dpAsPx
            val headerPaddingBottom = 8.dpAsPx animatedFirstStageTo contentPaddingBottom

            val headerCompatWidth = width.toFloat()
            val headerMediumWidth = width * 4f / 9f
            val headerExpandedWidth = width * 3f / 7f
            val headerWidth = when {
                animation == 0f -> headerCompatWidth
                animation < 1f -> headerCompatWidth animatedFirstStageTo headerMediumWidth
                animation == 1f -> headerMediumWidth
                animation < 2f -> headerMediumWidth animatedSecondStageTo headerExpandedWidth
                else -> headerExpandedWidth
            }

            val headerMeasurable = subcompose("header") {
                header(
                    PaddingValues(
                        start = headerPaddingLeft.toDp(),
                        top = headerPaddingTop.toDp(),
                        end = headerPaddingRight.toDp(),
                        bottom = headerPaddingBottom.toDp()
                    )
                )
            }.firstOrNull() ?: return@SubcomposeLayout layout(0, 0) {}

            // == Main content's padding and width ==
            val mainContentPaddingLeft = 32.dpAsPx + contentPaddingLeft animatedFirstStageTo 16.dpAsPx
            val mainContentPaddingTop = 8.dpAsPx animatedFirstStageTo 16.dpAsPx + contentPaddingTop
            val mainContentPaddingRight = 32.dpAsPx + contentPaddingRight animatedSecondStageTo 16.dpAsPx
            val mainContentPaddingBottom = 16.dpAsPx + contentPaddingBottom

            val mainContentMaxWidth = 512.dpAsPx
            val mainContentCompatWidth = width.toFloat()
            val mainContentMediumWidth = width * 5f / 9f
            val mainContentExpandedWidth = width * 4f / 7f
            val mainContentWidth = when {
                animation == 0f -> mainContentCompatWidth
                animation < 1f -> mainContentCompatWidth animatedFirstStageTo mainContentMediumWidth
                animation == 1f -> mainContentMediumWidth
                animation < 2f -> mainContentMediumWidth animatedSecondStageTo mainContentExpandedWidth
                else -> mainContentExpandedWidth
            }

            val mainContentMeasurable = subcompose("mainContent") {
                mainContent(
                    PaddingValues(
                        start = mainContentPaddingLeft.toDp(),
                        top = mainContentPaddingTop.toDp(),
                        end = mainContentPaddingRight.toDp(),
                        bottom = mainContentPaddingBottom.toDp()
                    )
                )
            }.firstOrNull() ?: return@SubcomposeLayout layout(0, 0) {}

            // == Measure header ==
            val headerHeight = headerMeasurable.minIntrinsicHeight(headerWidth.roundToInt()).coerceAtLeast(0)

            val headerConstraints = Constraints(
                maxWidth = headerWidth.roundToInt().coerceAtLeast(0), maxHeight = headerHeight
            )
            val headerPlaceable = headerMeasurable.measure(headerConstraints)

            // == Measure main content ==
            val mainContentConstraintWidth = mainContentWidth.coerceAtMost(mainContentMaxWidth).roundToInt()
            val mainContentConstraints = Constraints(maxWidth = mainContentConstraintWidth.coerceAtLeast(0))
            val mainContentPlaceable = mainContentMeasurable.measure(mainContentConstraints)

            val requiredColumnHeight = headerPlaceable.height + mainContentPlaceable.height
            val requiredRowHeight = max(headerPlaceable.height, mainContentPlaceable.height)
            val requiredHeight = requiredColumnHeight animatedFirstStageTo requiredRowHeight
            val requiredBoxHeight = max(boxHeight, requiredHeight.roundToInt())

            val headerX = 0 animatedFirstStageTo headerWidth - headerPlaceable.width
            val headerY = 0 animatedFirstStageTo (boxHeight - headerPlaceable.height) / 2f + scrollState.value

            val mainContentX =
                (0 animatedFirstStageTo headerWidth) + (mainContentWidth - mainContentConstraintWidth) / 2f

            val mainContentCompatY =
                (requiredBoxHeight - headerPlaceable.height - mainContentPlaceable.height) / 2f + headerPlaceable.height
            val mainContentMediumExpandedY = (requiredBoxHeight - mainContentPlaceable.height) / 2f
            val mainContentY = mainContentCompatY animatedFirstStageTo mainContentMediumExpandedY

            layout(width, requiredBoxHeight) {
                headerPlaceable.placeRelative(headerX.roundToInt(), headerY.roundToInt())
                mainContentPlaceable.placeRelative(mainContentX.roundToInt(), mainContentY.roundToInt())
            }
        }
    }
}

@Composable
private fun SignInComponent.MoreMethods(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        FilledTonalButton(
            onClick = ::enter,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Default.NoAccounts,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "仅浏览")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(
            onClick = ::onGoogleSignIn,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = AppIcons.Google,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "使用 Google 登录")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(
            onClick = ::onMicrosoftSignIn,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = AppIcons.Github,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "使用 GitHub 登录")
        }
        Spacer(modifier = Modifier.height(8.dp))
        FilledTonalButton(
            onClick = ::onMicrosoftSignIn,
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = AppIcons.Microsoft,
                contentDescription = null,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "使用 Microsoft 登录")
        }
        Spacer(modifier = Modifier.height(8.dp))
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
            Column {
                Spacer(modifier = Modifier.height(8.dp))
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
                Spacer(modifier = Modifier.height(8.dp))
                FilledTonalButton(
                    onClick = ::showServerSettings,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                ) {
                    Icon(
                        imageVector = Icons.Default.Settings,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "服务器设置")
                }
            }
        }
    }
}

@Composable
private fun SignInComponent.Credential() {
    OutlinedTextField(
        value = credential,
        onValueChange = ::credential::set,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        label = { Text(text = credentialType.displayName) },
        prefix = if (credentialType == Api.Auth.Type.CredentialType.PhoneNumber) {
            { Text(text = "+86") }
        } else null,
        supportingText = {
            AnimatedVisibility(
                visible = credential.isNotEmpty() && !credentialValid,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically(),
            ) {
                Text(text = "${credentialType.displayName}格式不正确")
            }
        },
        isError = credential.isNotEmpty() && !credentialValid,
        keyboardOptions = KeyboardOptions(
            keyboardType = credentialType.keyboardType, imeAction = ImeAction.Next
        ),
        keyboardActions = KeyboardActions(onNext = forwardAction),
        maxLines = 1,
    )
}

@Composable
private fun SignInComponent.VerificationCode() {
    OutlinedTextField(
        value = verification,
        onValueChange = ::verification::set,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        label = { Text(text = "验证码") },
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number, imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = enterAction),
        maxLines = 1,
    )
}

@Composable
private fun SignInComponent.Password() {
    OutlinedTextField(
        value = verification,
        onValueChange = ::verification::set,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        label = { Text(text = "密码") },
        trailingIcon = {
            IconButton(onClick = ::switchShowVerification) {
                Icon(
                    imageVector = if (showVerification) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                    contentDescription = if (showVerification) "隐藏密码" else "显示密码",
                )
            }
        },
        visualTransformation = if (showVerification) PasswordVisualTransformation() else VisualTransformation.None,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password, imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(onDone = enterAction),
        maxLines = 1,
    )
}

@Composable
private fun SignInComponent.BackButton(requiredStep: SignInComponent.Step) {
    TextButton(onClick = ::back, enabled = step == requiredStep) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowBack,
            contentDescription = "返回",
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = "返回")
    }
}

@Composable
private fun SignInComponent.ForwardButton(
    requiredStep: SignInComponent.Step, condition: suspend () -> Boolean = { true }
) {
    Button(
        onClick = { componentScope.launch { if (condition()) forward() } },
        enabled = step == requiredStep && when (requiredStep) {
            EnteringCredential -> credentialValid
            Verification -> verification.isNotBlank()
            else -> false
        },
        shape = MaterialTheme.shapes.large,
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Default.ArrowForward,
            contentDescription = requiredStep.next.displayName,
        )
    }
}

/**
 * 一个专门传送给接收器的类，用于 [SignInLayout]。
 *
 * @param density 用于计算布局的密度。
 * @param animation 用于计算布局的动画值，取值范围为 0 <= [animation] <= 2。
 *
 * @see DensityScope
 */
private class SignInLayoutScope(density: Density, val animation: Float) : DensityScope(density) {
    /**
     * 从 [animation] 中提取的第一阶段动画值。对应于从 [WindowWidthSizeClass.Compact] 到 [WindowWidthSizeClass.Medium] 的过渡。
     */
    val animationFirstStage by lazy { animation.coerceAtMost(1f) }

    /**
     * 从 [animation] 中提取的第二阶段动画值。对应于从 [WindowWidthSizeClass.Medium] 到 [WindowWidthSizeClass.Expanded] 的过渡。
     */
    val animationSecondStage by lazy { (animation - 1f).coerceAtLeast(0f) }

    /**
     * 将给定的 [AnimationValue] 应用于从 [WindowWidthSizeClass.Compact] 到 [WindowWidthSizeClass.Medium] 的过渡。
     *
     * @see AnimationValue.animated
     */
    val AnimationValue.animatedFirstStage
        get() = animated(animationFirstStage)

    infix fun Number.animatedFirstStageTo(target: Number) = (this animateTo target).animatedFirstStage

    /**
     * 将给定的 [AnimationValue] 应用于从 [WindowWidthSizeClass.Medium] 到 [WindowWidthSizeClass.Expanded] 的过渡。
     *
     * @see AnimationValue.animated
     */
    val AnimationValue.animatedSecondStage
        get() = animated(animationSecondStage)

    infix fun Number.animatedSecondStageTo(target: Number) = (this animateTo target).animatedSecondStage
}
