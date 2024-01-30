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

package xyz.xfqlittlefan.fhraise.js

import org.khronos.webgl.Int8Array
import org.khronos.webgl.get
import org.khronos.webgl.set

private fun <T : JsAny?> pushImpl(array: JsArray<T>, value: T) {
    js("array.push(value)")
}

fun <T : JsAny?> JsArray<T>.push(value: T) = pushImpl(this, value)

fun Int8Array.toByteArray(): ByteArray {
    val result = ByteArray(this.length)
    for (i in 0 until this.length) {
        result[i] = this[i]
    }
    return result
}

fun ByteArray.toJsArray(): Int8Array {
    val result = Int8Array(this.size)
    forEachIndexed { index, byte ->
        result[index] = byte
    }
    return result
}
