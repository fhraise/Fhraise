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

import io.ktor.server.application.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.kotlin.datetime.timestamp
import xyz.xfqlittlefan.fhraise.appDatabase
import java.util.*

object RefreshTokens : IdTable<UUID>() {
    val user = reference("user", Users)
    val token = text("token")
    val expiresAt = timestamp("expires_at")
    override val id = user
    override val primaryKey = PrimaryKey(user)
}

class RefreshToken(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<RefreshToken>(RefreshTokens)

    var user by User referencedOn RefreshTokens.user
    var token by RefreshTokens.token
    var expiresAt by RefreshTokens.expiresAt
}

fun Application.cleanupRefreshTokens() {
    launch(Dispatchers.IO) {
        log.trace("Cleaning up expired refresh tokens")
        appDatabase.dbQuery {
            RefreshToken.find { RefreshTokens.expiresAt less Clock.System.now() }.forEach {
                log.trace("Deleting expired refresh token for user {}", it.user.id.value)
                it.delete()
            }
        }
        log.trace("Finished cleaning up expired refresh tokens")
    }
}
