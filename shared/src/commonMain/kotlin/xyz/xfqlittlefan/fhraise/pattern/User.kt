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

package xyz.xfqlittlefan.fhraise.pattern

val usernameRegex = Regex("^[a-zA-Z0-9_-]{4,16}\$")
val phoneNumberRegex =
    Regex("^1(3(([0-3]|[5-9])[0-9]{8}|4[0-8][0-9]{7})|(45|5([0-2]|[5-6]|[8-9])|6(2|[5-7])|7([0-1]|[5-8])|8[0-9]|9([0-3]|[5-9]))[0-9]{8})$")
