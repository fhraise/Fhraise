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

import org.jetbrains.exposed.dao.UUIDEntity
import org.jetbrains.exposed.dao.UUIDEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.Column
import xyz.xfqlittlefan.fhraise.AppDatabase
import java.util.*

object Users : UUIDTable() {
    val username = varchar("username", 16).nullable().uniqueIndex()
    val name = text("name").nullable()
    val email = text("email").nullable().uniqueIndex()
    val phoneNumber = char("phone_number", 11).nullable().uniqueIndex()
    val password = varchar("password", 72).nullable()
    val google = text("google").nullable().uniqueIndex()
    val microsoft = text("microsoft").nullable().uniqueIndex()
}

class User(id: EntityID<UUID>) : UUIDEntity(id) {
    companion object : UUIDEntityClass<User>(Users)

    var username by Users.username
    var name by Users.name
    var email by Users.email
    var phoneNumber by Users.phoneNumber
    var password by Users.password
    var google by Users.google
    var microsoft by Users.microsoft
}

suspend fun <T> AppDatabase.getOrCreateUser(column: Column<T>, value: T): User = dbQuery {
    User.find { column eq value }.firstOrNull() ?: User.new {
        when (column) {
            Users.username -> username = value as String
            Users.email -> email = value as String
            Users.phoneNumber -> phoneNumber = value as String
            Users.google -> google = value as String
            Users.microsoft -> microsoft = value as String
            else -> error("Not a valid column")
        }
    }
}
