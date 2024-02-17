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

import kotlinx.datetime.Instant
import kotlinx.serialization.Contextual
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import xyz.xfqlittlefan.fhraise.serializers.InstantEpochMillisecondsSerializer

@Serializable
data class UserRepresentation(
    var id: String? = null,
    var username: String? = null,
    var firstName: String? = null,
    var lastName: String? = null,
    var email: String? = null,
    var emailVerified: Boolean? = null,
    var attributes: Map<String, List<String>>? = null,
    var userProfileMetadata: UserProfileMetadata? = null,
    var self: String? = null,
    var origin: String? = null,
    @SerialName("createdTimestamp") @Serializable(InstantEpochMillisecondsSerializer::class) var createdAt: Instant? = null,
    var enabled: Boolean? = null,
    var totp: Boolean? = null,
    var federationLink: String? = null,
    var serviceAccountClientId: String? = null,
    var credentials: List<CredentialRepresentation>? = null,
    var disableableCredentialTypes: Set<String>? = null,
    var requiredActions: List<String>? = null,
    var federatedIdentities: List<FederatedIdentityRepresentation>? = null,
    var realmRoles: List<String>? = null,
    var clientRoles: Map<String, List<String>>? = null,
    var clientConsents: List<UserConsentRepresentation>? = null,
    var notBefore: Int? = null,
    var groups: List<String>? = null,
    var access: Map<String, Boolean>? = null
)

@Serializable
data class UserProfileMetadata(
    var attributes: List<UserProfileAttributeMetadata>? = null,
    var groups: List<UserProfileAttributeGroupMetadata>? = null
)

@Serializable
data class UserProfileAttributeMetadata(
    var name: String? = null,
    var displayName: String? = null,
    var required: Boolean? = null,
    var readOnly: Boolean? = null,
    var annotations: Map<String, @Contextual Any>? = null,
    var validators: Map<String, Map<String, @Contextual Any>>? = null,
    var group: String? = null
)

@Serializable
data class UserProfileAttributeGroupMetadata(
    var name: String? = null,
    var displayHeader: String? = null,
    var displayDescription: String? = null,
    var annotations: Map<String, @Contextual Any>? = null
)

@Serializable
data class CredentialRepresentation(
    var id: String? = null,
    var type: CredentialType? = null,
    var userLabel: String? = null,
    var createdDate: Long? = null,
    var secretData: String? = null,
    var credentialData: String? = null,
    var priority: Int? = null,
    var value: String? = null,
    var temporary: Boolean? = null
) {
    @Serializable
    enum class CredentialType {
        @SerialName("secret")
        Secret,

        @SerialName("password")
        Password,

        @SerialName("totp")
        Totp,

        @SerialName("hotp")
        Hotp,

        @SerialName("kerberos")
        Kerberos
    }
}

@Serializable
data class FederatedIdentityRepresentation(
    var identityProvider: String? = null, var userId: String? = null, var userName: String? = null
)

@Serializable
data class UserConsentRepresentation(
    var clientId: String? = null,
    var createDate: Long? = null,
    var grantedClientScopes: List<String>? = null,
    var lastUpdatedDate: Long? = null
)
