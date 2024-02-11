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

package xyz.xfqlittlefan.fhraise.keycloak.providers

import org.keycloak.models.KeycloakSession
import org.keycloak.models.UserModel
import org.keycloak.models.UserProvider
import org.keycloak.provider.ConfiguredProvider
import org.keycloak.provider.ProviderConfigProperty
import org.keycloak.validate.AbstractStringValidator
import org.keycloak.validate.ValidationContext
import org.keycloak.validate.ValidationError
import org.keycloak.validate.ValidatorConfig
import java.util.stream.Stream

class UniqueAttributeValidatorProvider : AbstractStringValidator(), ConfiguredProvider {
    companion object {
        private const val ID = "unique-attribute-validator"
    }

    override fun getId() = ID

    override fun doValidate(
        attributeValue: String?, attributeName: String, context: ValidationContext, config: ValidatorConfig?
    ) {
        val session = context.session

        val currentUser = context.attributes[UserModel::class.java.name] as UserModel?

        if (!isAttributeUnique(attributeValue, attributeName, session, currentUser)) {
            context.addError(ValidationError(ID, attributeName, "User exists with the same $attributeName"))
        }
    }

    private fun isAttributeUnique(
        attributeValue: String?, attributeName: String?, session: KeycloakSession, currentUser: UserModel?
    ): Boolean {
        val userProvider = session.getProvider(UserProvider::class.java)
        val realm = getRealm(session)

        val usersWithSameAttributeValue: Stream<UserModel> =
            userProvider.searchForUserByUserAttributeStream(realm, attributeName, attributeValue)

        return currentUser?.let {
            usersWithSameAttributeValue.filter { !it.id.equals(currentUser.id) }.findAny().isEmpty
        } ?: usersWithSameAttributeValue.findAny().isEmpty
    }

    override fun getHelpText() = "Unique Attribute"

    override fun getConfigProperties() = emptyList<ProviderConfigProperty>()

    private fun getRealm(session: KeycloakSession) = when {
        session.context == null -> null
        session.context.realm != null -> session.context.realm
        session.context.authenticationSession != null && session.context.authenticationSession.realm != null -> session.context.authenticationSession.realm
        session.context.client != null && session.context.client.realm != null -> session.context.client.realm
        else -> null
    }
}
