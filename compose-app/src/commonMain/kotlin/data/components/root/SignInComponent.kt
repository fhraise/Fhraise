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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.plugins.websocket.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.*
import io.ktor.serialization.kotlinx.cbor.*
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.cbor.Cbor
import xyz.xfqlittlefan.fhraise.AndroidPlatform
import xyz.xfqlittlefan.fhraise.ServerDataStore
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.data.AppComponentContext
import xyz.xfqlittlefan.fhraise.data.componentScope
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.Step
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.Step.*
import xyz.xfqlittlefan.fhraise.datastore.PreferenceStateFlow
import xyz.xfqlittlefan.fhraise.logger
import xyz.xfqlittlefan.fhraise.oauth.oAuthSignIn
import xyz.xfqlittlefan.fhraise.pattern.phoneNumberRegex
import xyz.xfqlittlefan.fhraise.pattern.usernameRegex
import xyz.xfqlittlefan.fhraise.platform
import xyz.xfqlittlefan.fhraise.platform.Camera
import xyz.xfqlittlefan.fhraise.py.Message
import xyz.xfqlittlefan.fhraise.routes.Api
import xyz.xfqlittlefan.fhraise.routes.Api.Auth.Type.CredentialType
import xyz.xfqlittlefan.fhraise.routes.Api.Auth.Type.Request.VerificationType
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.js.JsName

internal typealias OnRequest = suspend (client: HttpClient, credential: String) -> Boolean
internal typealias OnVerify = suspend (client: HttpClient, verification: String) -> JwtTokenPair?

interface SignInComponent : AppComponentContext {
    /**
     * 登录过程中当前的步骤。
     */
    val step: Step

    /**
     * 当前选择的凭证类型。
     */
    val credentialType: CredentialType

    /**
     * 当前输入的凭证。
     */
    val credential: String

    /**
     * 当前选择的验证方式。
     */
    val verificationType: VerificationType?

    /**
     * 当前输入的验证信息。
     */
    val verification: String

    /**
     * 是否显示明文验证信息。
     */
    val showVerification: Boolean

    /**
     * 当前输入的 OTP。`null` 表示不需要 OTP。
     */
    val otp: String?

    /**
     * 更改凭证。
     */
    fun changeCredential(credential: String)

    /**
     * 更改验证信息。
     */
    fun changeVerification(verification: String)

    /**
     * 切换显示验证信息。
     */
    fun switchShowVerification()

    /**
     * 更改 OTP。
     */
    fun changeOtp(otp: String)

    /**
     * 当前输入的凭证是否有效。
     */
    val credentialValid: Boolean

    /**
     * 当前可用的摄像头列表。
     */
    val cameraList: List<Camera>

    /**
     * 是否显示选择摄像头菜单。
     */
    val showCameraMenu: Boolean

    /**
     * 当前选择的摄像头。
     */
    val selectedCamera: Camera?

    /**
     * 是否翻转摄像头预览。
     */
    val flipCameraPreview: Boolean

    /**
     * 刷新摄像头列表。
     */
    fun refreshCameraList()

    /**
     * 显示选择摄像头菜单。
     */
    @JsName("fun_showCameraMenu")
    fun showCameraMenu()

    /**
     * 隐藏选择摄像头菜单。
     */
    fun hideCameraMenu()

    /**
     * 选择摄像头。
     */
    fun selectCamera(camera: Camera)

    /**
     * 切换翻转摄像头预览。
     */
    fun switchFlipCameraPreview()

    /**
     * 是否显示更多登录选项。
     */
    val showMoreSignInOptions: Boolean

    /**
     * 切换显示更多登录选项。
     */
    fun switchShowMoreSignInOptions()

    /**
     * 是否显示服务器设置对话框。
     */
    val showServerSettings: Boolean

    /**
     * 显示服务器设置对话框。
     */
    @JsName("fun_showServerSettings")
    fun showServerSettings()

    /**
     * 隐藏服务器设置对话框。
     */
    fun hideServerSettings()

    /**
     * 隐藏服务器设置对话框的键盘操作。
     */
    val hideServerSettingsAction: KeyboardActionScope.() -> Unit

    /**
     * 为登录过程中的步骤提供显示名称。
     */
    val Step.displayName: String

    /**
     * 为凭证类型提供显示名称。
     */
    val CredentialType.displayName: String

    /**
     * 为凭证类型提供键盘类型。
     */
    val CredentialType.keyboardType: KeyboardType

    /**
     * 为验证方式提供显示名称。
     */
    val VerificationType.displayName: String

    /**
     * 为验证方式提供图标。
     */
    val VerificationType.icon: ImageVector

    /**
     * 为验证方式提供请求验证的方法。
     */
    val VerificationType.onRequest: OnRequest

    /**
     * 为验证方式提供验证的方法。
     */
    val VerificationType.onVerify: OnVerify

    /**
     * 使用当前凭证类型。
     */
    val CredentialType.use: () -> Unit

    /**
     * 使用当前验证方式。
     */
    val VerificationType.use: () -> Unit

    /**
     * 返回上一步。
     */
    fun back()

    /**
     * 前进到下一步。
     */
    fun forward()

    /**
     * 前进到下一步的键盘操作。
     */
    val forwardAction: KeyboardActionScope.() -> Unit

    /**
     * 服务器下发的，用于验证的令牌。
     */
    val verifyingToken: String?

    /**
     * 请求验证。
     *
     * @return 是否请求成功。
     */
    suspend fun requestVerification(): Boolean

    /**
     * Google 登录。
     */
    fun onGoogleSignIn()

    /**
     * GitHub 登录。
     */
    fun onGitHubSignIn()

    /**
     * Microsoft 登录。
     */
    fun onMicrosoftSignIn()

    /**
     * 进入主界面。
     */
    fun enter()

    /**
     * 进入主界面的键盘操作。
     */
    val enterAction: KeyboardActionScope.() -> Unit

    /**
     * 管理员登录。
     */
    fun onAdminSignIn()

    /**
     * 服务器主机。
     */
    val serverHost: PreferenceStateFlow<String, String>

    /**
     * 服务器端口。
     */
    val serverPort: PreferenceStateFlow<Int, Int>

    /**
     * 滚动状态。
     */
    val scrollState: ScrollState

    /**
     * 登录过程步骤。
     */
    enum class Step {
        /**
         * 输入凭证，如用户名。
         */
        EnteringCredential,

        /**
         * 选择验证方式，如选择密码验证。这个步骤只会提供验证方式对应的选项。
         */
        SelectingVerification,

        /**
         * 验证，如输入密码。
         */
        Verification,

        /**
         * 完成。该步骤仅为一个状态，不会显示在界面上。
         */
        Done;

        /**
         * 循环获取上一个步骤。
         */
        val previous
            get() = entries[(ordinal - 1 + entries.size) % entries.size]

        /**
         * 循环获取下一个步骤。
         */
        val next
            get() = entries[(ordinal + 1) % entries.size]

        /**
         * 循环获取上一个步骤。
         */
        operator fun dec(): Step = previous

        /**
         * 循环获取下一个步骤。
         */
        operator fun inc(): Step = next
    }
}

class AppSignInComponent(
    componentContext: AppComponentContext, onEnter: (JwtTokenPair?) -> Unit
) : SignInComponent, AppComponentContext by componentContext {
    override var step by mutableStateOf(EnteringCredential)
    override var credentialType by mutableStateOf(CredentialType.Username)
    override var credential by mutableStateOf("")
    private var _verificationType: VerificationType? by mutableStateOf(null)
    override var verificationType
        get() = _verificationType
        set(value) = changeVerificationType(value)
    override var verification by mutableStateOf("")
    override var showVerification by mutableStateOf(false)
    override var otp: String? by mutableStateOf(null)

    override fun changeCredential(credential: String) {
        this.credential = credential
    }

    override fun changeVerification(verification: String) {
        this.verification = verification
    }

    override fun switchShowVerification() {
        showVerification = !showVerification
    }

    override fun changeOtp(otp: String) {
        this.otp = otp
    }

    private val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")

    override val credentialValid
        get() = when (credentialType) {
            CredentialType.Username -> credential.matches(usernameRegex)
            CredentialType.PhoneNumber -> credential.matches(phoneNumberRegex)
            CredentialType.Email -> credential.matches(emailRegex)
        }

    override var cameraList by mutableStateOf(Camera.list)
    override var showCameraMenu by mutableStateOf(false)
    override var selectedCamera: Camera? by mutableStateOf(cameraList.firstOrNull())
    override var flipCameraPreview by mutableStateOf(true)

    override fun refreshCameraList() {
        cameraList = Camera.list
    }

    override fun showCameraMenu() {
        refreshCameraList()
        showCameraMenu = true
    }

    override fun hideCameraMenu() {
        showCameraMenu = false
    }

    override fun selectCamera(camera: Camera) {
        selectedCamera = camera
        logger.debug("Selected camera: ${camera.name}")
        componentScope.launch {
            camera.open()
            camera.startStreaming()
        }
        showCameraMenu = false
    }

    override fun switchFlipCameraPreview() {
        flipCameraPreview = !flipCameraPreview
    }

    override var showMoreSignInOptions by mutableStateOf(false)

    override fun switchShowMoreSignInOptions() {
        showMoreSignInOptions = !showMoreSignInOptions
    }

    override var showServerSettings by mutableStateOf(false)

    override fun showServerSettings() {
        showServerSettings = true
    }

    override fun hideServerSettings() {
        showServerSettings = false
        createClient()
    }

    override val hideServerSettingsAction: KeyboardActionScope.() -> Unit
        get() = {
            hideServerSettings()
        }

    override val Step.displayName: String
        get() = when (this) {
            EnteringCredential -> "输入凭证"
            SelectingVerification -> "选择验证方式"
            Verification -> "验证"
            Done -> "完成"
        }

    override val CredentialType.displayName
        get() = when (this) {
            CredentialType.Username -> "用户名"
            CredentialType.PhoneNumber -> "手机号"
            CredentialType.Email -> "电子邮件地址"
        }

    override val CredentialType.keyboardType
        get() = when (this) {
            CredentialType.Username -> KeyboardType.Text
            CredentialType.PhoneNumber -> KeyboardType.Phone
            CredentialType.Email -> KeyboardType.Email
        }

    override val VerificationType.displayName
        get() = when (this) {
            VerificationType.FhraiseToken -> "Fhraise 令牌"
            VerificationType.QrCode -> "二维码"
            VerificationType.SmsCode -> "短信验证码"
            VerificationType.EmailCode -> "电子邮件验证码"
            VerificationType.Password -> "密码"
            VerificationType.Face -> "人脸"
        }

    override val VerificationType.icon
        get() = when (this) {
            VerificationType.FhraiseToken -> Icons.Default.Key
            VerificationType.QrCode -> Icons.Default.QrCode
            VerificationType.SmsCode -> Icons.Default.Sms
            VerificationType.EmailCode -> Icons.Default.Mail
            VerificationType.Password -> Icons.Default.Password
            VerificationType.Face -> Icons.Default.Face
        }

    override val VerificationType.onRequest: OnRequest
        get() = { client, credential ->
            runCatching {
                client.post(Api.Auth.Type.Request(credentialType, this)) {
                    contentType(ContentType.Application.Cbor)
                    setBody(Api.Auth.Type.Request.RequestBody(credential))
                }.body<Api.Auth.Type.Request.ResponseBody>()
            }.getOrElse {
                it.printStackTrace()
                null
            }?.let {
                if (it !is Api.Auth.Type.Request.ResponseBody.Success) return@let false
                verifyingToken = it.token
                otp = if (it.otpNeeded) "" else null
                true
            } ?: false
        }

    override val VerificationType.onVerify: OnVerify
        get() = when (this) {
            VerificationType.FhraiseToken, VerificationType.SmsCode, VerificationType.EmailCode, VerificationType.Password -> { client, verification ->
                runCatching {
                    client.post(Api.Auth.Type.Verify(credentialType, verifyingToken!!)) {
                        contentType(ContentType.Application.Cbor)
                        setBody(
                            Api.Auth.Type.Verify.RequestBody(
                                Api.Auth.Type.Verify.RequestBody.Verification(verification, otp)
                            )
                        )
                    }.body<Api.Auth.Type.Verify.ResponseBody>()
                }.getOrElse {
                    it.printStackTrace()
                    null
                }?.let {
                    if (it !is Api.Auth.Type.Verify.ResponseBody.Success) return@let null
                    verifyingToken = null
                    it.tokenPair
                }
            }

            VerificationType.Face -> { client, _ ->
                suspendCoroutine { continuation ->
                    faceVerificationWebsocketJob = componentScope.launch {
                        client.ws(path = Api.Auth.Face.PATH) {
                            sendSerialized<Api.Auth.Face.Handshake.Request>(
                                Api.Auth.Face.Handshake.Request(credentialType, verifyingToken!!)
                            )

                            val handshakeResult = receiveDeserialized<Api.Auth.Face.Handshake.Response>()
                            if (handshakeResult !is Api.Auth.Face.Handshake.Response.Success) return@ws

                            logger.debug("Face verification started.")

                            while (true) {
                                val frame = selectedCamera?.frameFlow?.firstOrNull() ?: continue

                                logger.debug("Frame received. (${frame.width}x${frame.height})")

                                sendSerialized<Message.Client>(
                                    Message.Client.Frame(
                                        frame.format.name, frame.width, frame.content
                                    )
                                )
                                val frameResult = receiveDeserialized<Message.Result>()

                                if (frameResult is Message.Result.Success) {
                                    val tokenPairResult = receiveDeserialized<Api.Auth.Type.Verify.ResponseBody>()
                                    if (tokenPairResult is Api.Auth.Type.Verify.ResponseBody.Success) {
                                        continuation.resume(tokenPairResult.tokenPair)
                                    } else {
                                        continuation.resume(null)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            VerificationType.QrCode -> { _, _ -> null }
        }

    override val CredentialType.use: () -> Unit
        get() = {
            credentialType = this
        }

    override val VerificationType.use: () -> Unit
        get() = {
            verificationType = this
        }

    private var faceVerificationWebsocketJob: Job? = null

    private fun changeStepHook() {
        faceVerificationWebsocketJob?.cancel()
        faceVerificationWebsocketJob = null
    }

    override fun back() {
        step--
        changeStepHook()
    }

    override fun forward() {
        if (step.next != Done) {
            step++
        } else {
            enter()
        }
        changeStepHook()
    }

    override val forwardAction: KeyboardActionScope.() -> Unit
        get() = {
            forward()
        }

    override var verifyingToken: String? by mutableStateOf(null)

    override suspend fun requestVerification() = verificationType?.run {
        try {
            onRequest(client, credential)
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    } ?: false

    private fun onOAuthSignIn(provider: Api.OAuth.Provider) {
        componentScope.launch {
            if (platform is AndroidPlatform) {
                requestAppNotificationPermission("Fhraise 需要显示一条通知以确保身份认证服务正常运行。您可以拒绝该权限，但是拒绝该权限将导致身份认证功能在某些设备上无法工作。如果您选择拒绝，可在手机设置中重新授予")
            }
            oAuthSignIn(serverHost.value, serverPort.value, provider)
        }
    }

    override fun onGoogleSignIn() = onOAuthSignIn(Api.OAuth.Provider.Google)

    override fun onGitHubSignIn() = onOAuthSignIn(Api.OAuth.Provider.GitHub)

    override fun onMicrosoftSignIn() = onOAuthSignIn(Api.OAuth.Provider.Microsoft)

    private val onEnter: (JwtTokenPair?) -> Unit = onEnter@{ tokenPair ->
        onEnter(tokenPair)
        if (tokenPair != null) return@onEnter
        componentScope.launch {
            snackbarHostState.showSnackbar(
                message = "登录账号可享受完整服务",
                actionLabel = "返回登录",
                withDismissAction = true,
                duration = SnackbarDuration.Short
            ).let {
                if (it == SnackbarResult.ActionPerformed) Unit // TODO
            }
        }
    }

    override fun enter() {
        if (!credentialValid) return
        verificationType?.let {
            componentScope.launch {
                it.onVerify(client, verification)?.let {
                    snackbarHostState.showSnackbar(message = "验证成功", withDismissAction = true)
                    onEnter(it)
                } ?: snackbarHostState.showSnackbar(
                    message = "验证失败",
                    actionLabel = "仅浏览",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                ).let {
                    if (it == SnackbarResult.ActionPerformed) onEnter(null)
                }
            }
        } ?: run {
            onEnter(null)
        }
    }

    override val enterAction: KeyboardActionScope.() -> Unit
        get() = {
            enter()
        }

    override fun onAdminSignIn() {
        // TODO
    }

    private fun changeVerificationType(type: VerificationType?) {
        verification = ""
        showVerification = when (type) {
            VerificationType.Password -> false
            else -> true
        }
        _verificationType = type
        componentScope.launch {
            if (requestVerification()) {
                step = Verification
                if (type == VerificationType.Face) {
                    enter()
                }
            } else {
                snackbarHostState.showSnackbar(
                    message = "请求验证失败",
                    actionLabel = "仅浏览",
                    withDismissAction = true,
                    duration = SnackbarDuration.Short
                )
            }
        }
    }

    private val serverDataStore = ServerDataStore.Preferences(componentScope)

    override val serverHost = serverDataStore.serverHost
    override val serverPort = serverDataStore.serverPort

    private var _client: HttpClient? = null
    private val client
        get() = _client ?: createClient()

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient {
        install(Resources)
        install(ContentNegotiation) { cbor() }
        install(WebSockets) {
            contentConverter = KotlinxWebsocketSerializationConverter(Cbor)
        }
        defaultRequest {
            host = serverHost.value
            port = serverPort.value
        }
    }.also { _client = it }

    override val scrollState: ScrollState = ScrollState(initial = 0)
}
