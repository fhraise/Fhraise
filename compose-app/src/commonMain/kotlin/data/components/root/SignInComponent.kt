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
import androidx.compose.ui.text.input.KeyboardType
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.resources.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.AndroidPlatform
import xyz.xfqlittlefan.fhraise.ServerDataStore
import xyz.xfqlittlefan.fhraise.auth.JwtTokenPair
import xyz.xfqlittlefan.fhraise.data.AppComponentContext
import xyz.xfqlittlefan.fhraise.data.componentScope
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.Step.*
import xyz.xfqlittlefan.fhraise.datastore.PreferenceStateFlow
import xyz.xfqlittlefan.fhraise.oauth.oAuthSignIn
import xyz.xfqlittlefan.fhraise.pattern.phoneNumberRegex
import xyz.xfqlittlefan.fhraise.pattern.usernameRegex
import xyz.xfqlittlefan.fhraise.platform
import xyz.xfqlittlefan.fhraise.platform.Camera
import xyz.xfqlittlefan.fhraise.routes.Api
import xyz.xfqlittlefan.fhraise.routes.Api.Auth.Type.CredentialType
import xyz.xfqlittlefan.fhraise.routes.Api.Auth.Type.Request.VerificationType
import kotlin.js.JsName

internal typealias OnRequest = suspend (client: HttpClient, credential: String) -> Boolean
internal typealias OnVerify = suspend (client: HttpClient, verification: String) -> JwtTokenPair?

interface SignInComponent : AppComponentContext {
    var step: Step
    var credentialType: CredentialType
    var credential: String
    var verificationType: VerificationType?
    var verification: String
    var showVerification: Boolean
    var otp: String?

    val credentialValid: Boolean

    var cameraList: List<Camera>
    var showCameraMenu: Boolean
    var selectedCamera: Camera?
    var flipCameraPreview: Boolean

    fun refreshCameraList() {
        cameraList = Camera.list
    }

    fun switchShowCameraMenu() {
        refreshCameraList()
        showCameraMenu = !showCameraMenu
    }

    fun selectCamera(camera: Camera) {
        selectedCamera = camera
        showCameraMenu = false
    }

    fun checkStepAndCloseCameras() {
        if (step != Verification) {
            componentScope.launch {
                cameraList.forEach {
                    it.close()
                }
            }
        }
    }

    fun switchFlipCameraPreview() {
        flipCameraPreview = !flipCameraPreview
    }

    var showMoreSignInOptions: Boolean

    var showServerSettings: Boolean

    enum class Step(val displayName: String) {
        EnteringCredential("输入凭证"), SelectingVerification("选择验证方式"), Verification("验证"), Done("完成");

        val previous
            get() = entries[(ordinal - 1 + entries.size) % entries.size]
        val next
            get() = entries[(ordinal + 1) % entries.size]

        operator fun dec(): Step = previous
        operator fun inc(): Step = next
    }

    val CredentialType.displayName
        get() = when (this) {
            CredentialType.Username -> "用户名"
            CredentialType.PhoneNumber -> "手机号"
            CredentialType.Email -> "电子邮件地址"
        }

    val CredentialType.keyboardType
        get() = when (this) {
            CredentialType.Username -> KeyboardType.Text
            CredentialType.PhoneNumber -> KeyboardType.Phone
            CredentialType.Email -> KeyboardType.Email
        }

    val VerificationType.displayName
        get() = when (this) {
            VerificationType.FhraiseToken -> "Fhraise 令牌"
            VerificationType.QrCode -> "二维码"
            VerificationType.SmsCode -> "短信验证码"
            VerificationType.EmailCode -> "电子邮件验证码"
            VerificationType.Password -> "密码"
            VerificationType.Face -> "人脸"
        }

    val VerificationType.icon
        get() = when (this) {
            VerificationType.FhraiseToken -> Icons.Default.Key
            VerificationType.QrCode -> Icons.Default.QrCode
            VerificationType.SmsCode -> Icons.Default.Sms
            VerificationType.EmailCode -> Icons.Default.Mail
            VerificationType.Password -> Icons.Default.Password
            VerificationType.Face -> Icons.Default.Face
        }

    val VerificationType.onRequest: OnRequest
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

    val VerificationType.onVerify: OnVerify
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

            VerificationType.QrCode -> { _, _ -> null }
            VerificationType.Face -> { _, _ -> null }
        }

    val CredentialType.use: () -> Unit
        get() = {
            credentialType = this
        }

    val VerificationType.use: () -> Unit
        get() = {
            verificationType = this
        }

    fun switchShowVerification() {
        showVerification = !showVerification
    }

    fun switchShowMoreSignInOptions() {
        showMoreSignInOptions = !showMoreSignInOptions
    }

    fun back() {
        step--
        checkStepAndCloseCameras()
    }

    fun forward() {
        if (step.next != Done) {
            step++
        } else {
            enter()
        }
        checkStepAndCloseCameras()
    }

    val forwardAction: KeyboardActionScope.() -> Unit
        get() = {
            forward()
        }

    var verifyingToken: String?

    suspend fun requestVerification(): Boolean

    fun onGoogleSignIn()

    fun onGitHubSignIn()

    fun onMicrosoftSignIn()

    fun enter()

    val enterAction: KeyboardActionScope.() -> Unit
        get() = {
            enter()
        }

    fun onAdminSignIn()

    @JsName("fun_showServerSettings")
    fun showServerSettings() {
        showServerSettings = true
    }

    fun hideServerSettings() {
        showServerSettings = false
    }

    val hideServerSettingsAction: KeyboardActionScope.() -> Unit
        get() = {
            hideServerSettings()
        }

    val serverHost: PreferenceStateFlow<String, String>
    val serverPort: PreferenceStateFlow<Int, Int>

    val scrollState: ScrollState
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

    private val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")

    override var showMoreSignInOptions by mutableStateOf(false)

    override var showServerSettings by mutableStateOf(false)

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

    override fun hideServerSettings() {
        super.hideServerSettings()
        createClient()
    }

    override val serverHost = serverDataStore.serverHost
    override val serverPort = serverDataStore.serverPort

    private var _client: HttpClient? = null
    private val client
        get() = _client ?: createClient()

    @OptIn(ExperimentalSerializationApi::class)
    private fun createClient() = HttpClient {
        install(Resources)
        install(ContentNegotiation) { cbor() }
        defaultRequest {
            host = serverHost.value
            port = serverPort.value
        }
    }.also { _client = it }

    override val scrollState: ScrollState = ScrollState(initial = 0)
}
