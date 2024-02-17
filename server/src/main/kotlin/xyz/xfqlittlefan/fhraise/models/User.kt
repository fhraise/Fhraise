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

package xyz.xfqlittlefan.fhraise.models

import kotlinx.serialization.Serializable

data class UserQuery(var username: String? = null, var phoneNumber: String? = null, var email: String? = null) {
    val generatedUsername
        get() = username ?: email ?: phoneNumber ?: error("No username, phone number, or email provided")
}

@Serializable
data class UserRepresentation(
    var access: Map<String, Boolean>? = null,
    var attributes: Map<String, List<String>>? = null,
    var clientConsents: List<UserConsentRepresentation>? = null,
    var clientRoles: Map<String, List<String>>? = null,
    var createdTimestamp: Long? = null,
    var credentials: List<CredentialRepresentation>? = null,
    var disableableCredentialTypes: List<String>? = null,
    var email: String? = null,
    var emailVerified: Boolean? = null,
    var enabled: Boolean? = null,
    var federatedIdentities: List<FederatedIdentityRepresentation>? = null,
    var federationLink: String? = null,
    var firstName: String? = null,
    var groups: List<String>? = null,
    var id: String? = null,
    var lastName: String? = null,
    var notBefore: Int? = null,
    var origin: String? = null,
    var realmRoles: List<String>? = null,
    var requiredActions: List<String>? = null,
    var self: String? = null,
    var serviceAccountClientId: String? = null,
    var totp: Boolean? = null,
    var username: String? = null
)

@Serializable
data class UserConsentRepresentation(
    var clientId: String? = null,
    var createDate: Long? = null,
    var grantedClientScopes: List<String>? = null,
    var lastUpdatedDate: Long? = null
)

@Serializable
data class CredentialRepresentation(
    var algorithm: String? = null,
    var config: Map<String, String>? = null,
    var counter: Int? = null,
    var createdDate: Long? = null,
    var device: String? = null,
    var digits: Int? = null,
    var hashIterations: Int? = null,
    var hashedSaltedvarue: String? = null,
    var period: Int? = null,
    var salt: String? = null,
    var temporary: Boolean? = null,
    var type: String? = null,
    var varue: String? = null
)

@Serializable
data class FederatedIdentityRepresentation(
    var identityProvider: String? = null, var userId: String? = null, var userName: String? = null
)
