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

package xyz.xfqlittlefan.fhraise.auth

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTCreator
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.interfaces.DecodedJWT
import com.auth0.jwt.interfaces.Verification
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.toJavaInstant
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.encodeToJsonElement
import kotlinx.serialization.json.jsonPrimitive
import xyz.xfqlittlefan.fhraise.appSecret
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.time.Duration

private val jwtSecret = appSecret.propertyOrNull("auth.jwt.secret")?.getString() ?: "secret"
val jwtIssuer = appSecret.propertyOrNull("auth.jwt.issuer")?.getString() ?: "fhraise"
val jwtAudience = appSecret.propertyOrNull("auth.jwt.audience")?.getString() ?: "fhraise-user"
val jwtRealm = appSecret.propertyOrNull("auth.jwt.realm")?.getString() ?: "fhraise"

private val jwtAlgorithm: Algorithm = Algorithm.HMAC256(jwtSecret)

/**
 * 创建一个包含默认签发者和受众的 JWT token。
 *
 * @param builder 配置这个 JWT token 的构造器。
 * @return 生成的 JWT token。
 *
 * @throws com.auth0.jwt.exceptions.JWTCreationException 如果构造 JWT token 时发生错误。
 *
 * @see JWTCreator.Builder
 * @see JWTCreator.Builder.sign
 */
fun jwt(builder: JWTCreator.Builder.() -> Unit = {}): String = JWT.create().apply {
    withIssuer(jwtIssuer)
    withAudience(jwtAudience)
    builder()
}.sign(jwtAlgorithm)

/**
 * 验证并解码一个 JWT token。
 *
 * @param jwt 要验证的 JWT token。
 * @param builder 配置这个 JWT token 的验证器。
 * @return 验证通过的已解码的 JWT token。
 *
 * @throws com.auth0.jwt.exceptions.SignatureVerificationException 如果签名无效。
 * @throws com.auth0.jwt.exceptions.TokenExpiredException 如果 token 已经过期。
 * @throws com.auth0.jwt.exceptions.MissingClaimException 如果缺少要验证的声明。
 * @throws com.auth0.jwt.exceptions.IncorrectClaimException 如果声明的值与预期值不同。
 * @throws com.auth0.jwt.exceptions.JWTVerificationException
 *
 * @see Verification
 * @see com.auth0.jwt.JWTVerifier
 * @see com.auth0.jwt.JWTVerifier.verify
 */
fun verifiedJwt(jwt: String, builder: Verification.() -> Unit = {}): DecodedJWT = JWT.require(jwtAlgorithm).apply {
    withIssuer(jwtIssuer)
    withAudience(jwtAudience)
    builder()
}.build().verify(jwt)

/**
 * 验证并解码一个 JWT token，如果验证失败则返回 null。
 *
 * @param jwt 要验证的 JWT token。
 * @param builder 配置这个 JWT token 的验证器。
 * @return 验证通过的已解码的 JWT token，或者 null。
 *
 * @see verifiedJwt
 */
fun verifiedJwtOrNull(jwt: String, builder: Verification.() -> Unit = {}) =
    runCatching { verifiedJwt(jwt, builder) }.getOrNull()

/**
 * 验证一个 JWT token。
 *
 * @param jwt 要验证的 JWT token。
 * @param builder 配置这个 JWT token 的验证器。
 * @return 是否验证通过。
 *
 * @see verifiedJwtOrNull
 */
fun verifyJwt(jwt: String, builder: Verification.() -> Unit = {}) = verifiedJwtOrNull(jwt, builder) != null

fun JWTCreator.Builder.withIssuedAtNow(): JWTCreator.Builder = withIssuedAt(Clock.System.now().toJavaInstant())

fun JWTCreator.Builder.withExpiresAt(instant: Instant): JWTCreator.Builder = withExpiresAt(instant.toJavaInstant())

fun JWTCreator.Builder.withExpiresIn(duration: Duration): JWTCreator.Builder =
    withExpiresAt(Clock.System.now() + duration)

inline fun <reified T> JWTCreator.Builder.withPayload(payload: T) = Json.encodeToJsonElement(payload).let {
    if (it !is JsonObject) error("Payload must be a JsonObject")
    it.entries.forEach { (key, value) -> withClaim(key, value.jsonPrimitive.content) }
}

inline val json
    get() = Json {
        ignoreUnknownKeys = true
    }

@OptIn(ExperimentalEncodingApi::class)
inline fun <reified T> DecodedJWT.decodedPayload(): T = json.decodeFromString(Base64.decode(payload).decodeToString())

inline fun <reified T> DecodedJWT.decodedPayloadOrNull(): T? = runCatching { decodedPayload<T>() }.getOrNull()
