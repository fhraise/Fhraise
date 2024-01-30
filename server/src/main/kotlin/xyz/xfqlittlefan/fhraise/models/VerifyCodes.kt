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

import org.jetbrains.exposed.dao.Entity
import org.jetbrains.exposed.dao.EntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.kotlin.datetime.datetime
import xyz.xfqlittlefan.fhraise.applicationConfig
import kotlin.time.Duration.Companion.minutes

val verifyCodeTtl = applicationConfig.propertyOrNull("app.verify-code.ttl")?.getString()?.toLongOrNull()
    ?: 5.minutes.inWholeMilliseconds

object VerifyCodes : IdTable<String>() {
    val phoneNumber = char("phone_number", 11)
    val code = char("code", 6)
    val createdAt = datetime("created_at")
    override val id = phoneNumber.entityId()
    override val primaryKey = PrimaryKey(phoneNumber)
}

class VerifyCode(id: EntityID<String>) : Entity<String>(id) {
    companion object : EntityClass<String, VerifyCode>(VerifyCodes)

    var phoneNumber by VerifyCodes.phoneNumber
    var code by VerifyCodes.code
    var createdAt by VerifyCodes.createdAt
}
