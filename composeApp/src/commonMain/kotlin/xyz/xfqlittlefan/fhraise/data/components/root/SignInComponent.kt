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
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.cbor.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.ServerDataStore
import xyz.xfqlittlefan.fhraise.data.AppComponentContext
import xyz.xfqlittlefan.fhraise.data.componentScope
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.CredentialType.*
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.Step.*
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.VerificationType.*
import xyz.xfqlittlefan.fhraise.datastore.PreferenceStateFlow
import xyz.xfqlittlefan.fhraise.models.phoneNumberRegex
import xyz.xfqlittlefan.fhraise.models.usernameRegex
import xyz.xfqlittlefan.fhraise.oauth.microsoftSignIn
import xyz.xfqlittlefan.fhraise.routes.Api
import kotlin.js.JsName

internal typealias OnRequest = suspend (client: HttpClient, credential: String) -> Boolean
internal typealias OnVerify = suspend (client: HttpClient, credential: String, verification: String) -> Boolean

interface SignInComponent : AppComponentContext {
    var step: Step
    var credentialType: CredentialType
    var credential: String
    var verificationType: VerificationType?
    var verification: String
    var showVerification: Boolean

    val credentialValid: Boolean

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

    enum class CredentialType(val displayName: String, val keyboardType: KeyboardType) {
        Username("用户名", KeyboardType.Text), PhoneNumber("手机号", KeyboardType.Phone), Email(
            "电子邮件地址", KeyboardType.Email
        )
    }

    sealed class VerificationType(
        val displayName: String,
        val icon: ImageVector,
        internal val onRequest: OnRequest,
        internal val onVerify: OnVerify
    ) {
        class FhraiseToken(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("Fhraise令牌", Icons.Default.Key, onRequest, onVerify)

        class QrCode(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("二维码", Icons.Default.QrCode, onRequest, onVerify)

        class SmsCode(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("短信验证", Icons.Default.Sms, onRequest, onVerify)

        class EmailCode(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("电子邮件验证", Icons.Default.Mail, onRequest, onVerify)

        class Password(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("密码", Icons.Default.Password, onRequest, onVerify)

        class Face(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("人脸", Icons.Default.Face, onRequest, onVerify)
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
    }

    fun forward() {
        if (step.next != Done) {
            step++
        } else {
            enter()
        }
    }

    val forwardAction: KeyboardActionScope.() -> Unit
        get() = {
            forward()
        }

    val defaultVerifications
        get() = listOf(
            FhraiseToken(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
            QrCode(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
            SmsCode(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
            EmailCode(
                onRequest = { client, credential ->
                    client.post(Api.Auth.Email.Request()) {
                        contentType(ContentType.Application.Cbor)
                        setBody(Api.Auth.Email.Request.RequestBody(credential))
                    }.body<Api.Auth.Email.Request.ResponseBody>()
                    true
                },
                onVerify = { client, credential, verification ->
                    client.post(Api.Auth.Email.Verify()) {
                        contentType(ContentType.Application.Cbor)
                        setBody(Api.Auth.Email.Verify.RequestBody(credential, verification))
                    }.body<Api.Auth.Email.Verify.ResponseBody>() == Api.Auth.Email.Verify.ResponseBody.Success
                },
            ),
            Password(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
            Face(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
        )

    suspend fun requestVerification(): Boolean

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
    componentContext: AppComponentContext, private val onEnter: () -> Unit
) : SignInComponent, AppComponentContext by componentContext {
    override var step by mutableStateOf(EnteringCredential)
    override var credentialType by mutableStateOf(Username)
    override var credential by mutableStateOf("")
    private var _verificationType: SignInComponent.VerificationType? by mutableStateOf(null)
    override var verificationType
        get() = _verificationType
        set(value) = changeVerificationType(value)
    override var verification by mutableStateOf("")
    override var showVerification by mutableStateOf(false)

    override val credentialValid
        get() = when (credentialType) {
            Username -> credential.matches(usernameRegex)
            PhoneNumber -> credential.matches(phoneNumberRegex)
            Email -> credential.matches(emailRegex)
        }

    private val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")

    override var showMoreSignInOptions by mutableStateOf(false)

    override var showServerSettings by mutableStateOf(false)

    override suspend fun requestVerification() = verificationType?.run {
        try {
            onRequest(client, credential)
        } catch (e: Throwable) {
            e.printStackTrace()
            false
        }
    } ?: false

    override fun onMicrosoftSignIn() {
        componentScope.launch {
            microsoftSignIn(serverHost.value, serverPort.value)
        }
    }

    override fun enter() {
        if (!credentialValid) return
        val verification = verificationType
        if (verification == null) {
            // TODO: 询问是否验证
            onEnter()
        } else {
            componentScope.launch {
                if (verification.onVerify(client, credential, this@AppSignInComponent.verification)) {
                    snackbarHostState.showSnackbar(
                        message = "验证成功", withDismissAction = true
                    )
                    onEnter()
                } else {
                    snackbarHostState.showSnackbar(
                        message = "验证失败", withDismissAction = true
                    )
                }
            }
        }
    }

    override fun onAdminSignIn() {
        // TODO
    }

    private fun changeVerificationType(type: SignInComponent.VerificationType?) {
        showVerification = when (type) {
            is Password -> false
            else -> true
        }
        _verificationType = type
        componentScope.launch {
            if (requestVerification()) {
                step = Verification
            } else {
                snackbarHostState.showSnackbar(
                    message = "请求验证失败", withDismissAction = true
                )
            }
        }
    }

    private val serverDataStore by ServerDataStore.Preferences.preferences()

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
