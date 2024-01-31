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
import io.ktor.serialization.kotlinx.cbor.*
import kotlinx.coroutines.launch
import kotlinx.serialization.ExperimentalSerializationApi
import xyz.xfqlittlefan.fhraise.ServerDataStore
import xyz.xfqlittlefan.fhraise.api.Auth
import xyz.xfqlittlefan.fhraise.data.AppComponentContext
import xyz.xfqlittlefan.fhraise.data.componentScope
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.CredentialType.*
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.Step.EnteringCredential
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.Step.Verification
import xyz.xfqlittlefan.fhraise.data.components.root.SignInComponent.VerificationType.Password
import xyz.xfqlittlefan.fhraise.datastore.PreferenceStateFlow
import xyz.xfqlittlefan.fhraise.models.usernameRegex

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

    enum class Step(val displayName: String) {
        EnteringCredential("输入凭证"), SelectingVerification("选择验证方式"), Verification("验证");

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

        class SmsCode(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("短信验证", Icons.Default.Sms, onRequest, onVerify)

        class EmailCode(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("电子邮件验证", Icons.Default.Mail, onRequest, onVerify)

        class Password(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("密码", Icons.Default.Password, onRequest, onVerify)

        class Face(onRequest: OnRequest, onVerify: OnVerify) :
            VerificationType("人脸", Icons.Default.Face, onRequest, onVerify)
    }

    fun switchShowMoreSignInOptions() {
        showMoreSignInOptions = !showMoreSignInOptions
    }

    fun back() {
        step--
    }

    fun forward() {
        step++
    }

    val defaultVerifications
        get() = listOf(
            VerificationType.FhraiseToken(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
            VerificationType.SmsCode(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
            VerificationType.EmailCode(
                onRequest = { client, credential ->
                    val result = try {
                        client.post(Auth.Email.Request(email = credential)).body<Auth.Email.Request.Response>()
                    } catch (e: Throwable) {
                        e.printStackTrace()
                        null
                    } ?: return@EmailCode false
                    true
                },
                onVerify = { client, credential, verification ->
                    false
                },
            ),
            Password(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
            VerificationType.Face(onRequest = { _, _ -> false }, onVerify = { _, _, _ -> false }),
        )

    fun emailCodeVerification() = VerificationType.EmailCode(
        onRequest = { client, credential ->
            val result = try {
                client.post(Auth.Email.Request(email = credential)).body<Auth.Email.Request.Response>()
            } catch (e: Throwable) {
                e.printStackTrace()
                null
            } ?: return@EmailCode false
            true
        },
        onVerify = { client, credential, verification ->
            false
        },
    )

    fun requestVerification()
    fun enter()
    fun onAdminSignIn()

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
            Username -> usernameRegex.matches(credential)
            PhoneNumber -> phoneNumberRegex.matches(credential)
            Email -> emailRegex.matches(credential)
        }

    private val phoneNumberRegex =
        Regex("^1(3(([0-3]|[5-9])[0-9]{8}|4[0-8][0-9]{7})|(45|5([0-2]|[5-6]|[8-9])|6(2|[5-7])|7([0-1]|[5-8])|8[0-9]|9([0-3]|[5-9]))[0-9]{8})$")
    private val emailRegex = Regex("^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+\$")

    override var showMoreSignInOptions by mutableStateOf(false)

    override fun requestVerification() {
        val verification = verificationType ?: return

        componentScope.launch {
            if (verification.onRequest(client, credential)) {
                // TODO
            }
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
                    onEnter()
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
        step = if (type != null) {
            requestVerification()
            Verification
        } else {
            EnteringCredential
        }
    }

    private val serverDataStore by ServerDataStore.Preferences.preferences()

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
