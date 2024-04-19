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

package xyz.xfqlittlefan.fhraise.py

import kotlinx.cinterop.*
import kotlin.experimental.ExperimentalNativeApi

@ExperimentalForeignApi
class ThrowableVar(rawPtr: NativePtr) : CStructVar(rawPtr) {
    @Suppress("DEPRECATION")
    companion object : Type(40, 8)

    var type: CArrayPointer<ByteVar>?
        get() = memberAt<CArrayPointerVar<ByteVar>>(0).value
        set(value) {
            memberAt<CArrayPointerVar<ByteVar>>(0).value = value
        }

    var ref: COpaquePointer?
        get() = memberAt<COpaquePointerVar>(8).value
        set(value) {
            memberAt<COpaquePointerVar>(8).value = value
        }

    var message: CArrayPointer<ByteVar>?
        get() = memberAt<CArrayPointerVar<ByteVar>>(16).value
        set(value) {
            memberAt<CArrayPointerVar<ByteVar>>(16).value = value
        }

    var stacktrace: CArrayPointer<CArrayPointerVar<ByteVar>>?
        get() = memberAt<CArrayPointerVar<CArrayPointerVar<ByteVar>>>(24).value
        set(value) {
            memberAt<CArrayPointerVar<CArrayPointerVar<ByteVar>>>(24).value = value
        }

    var stacktraceSize: Int?
        get() = memberAt<IntVar>(32).value
        set(value) {
            memberAt<IntVar>(32).value = value ?: 0
        }
}

@ExperimentalForeignApi
internal class ThrowableWrapper(val throwable: ThrowableVar) : Throwable()

@ExperimentalNativeApi
@ExperimentalForeignApi
internal inline fun <R> Throwable.wrapToC(block: (ThrowableVar) -> R): R {
    val ref = StableRef.create(this)

    return memScoped {
        logger.debug("Wrapping throwable.")
        logger.warn(getStackTrace().joinToString("\n"))

        val throwable = alloc<ThrowableVar>()

        throwable.type = this::class.qualifiedName?.cstrPtr(this)
        throwable.ref = ref.asCPointer()
        throwable.message = this@wrapToC.message?.cstrPtr(this)
        val stacktraceList = mutableListOf<ByteVar>()
        this@wrapToC.getStackTrace().forEach {
            stacktraceList.add(it.cstrPtr(this).pointed)
        }
        val stacktraceArray = allocArrayOfPointersTo(stacktraceList)
        throwable.stacktrace = stacktraceArray
        throwable.stacktraceSize = stacktraceList.size
        block(throwable)
    }
}

@ExperimentalNativeApi
@ExperimentalForeignApi
internal inline fun <R> runCatching(
    onError: OnError, block: () -> R
) = runCatching(block).fold(onSuccess = {
    Result.success(it)
}, onFailure = { throwable ->
    throwable.wrapToC {
        onError(it)
        Result.failure(ThrowableWrapper(it))
    }
})
