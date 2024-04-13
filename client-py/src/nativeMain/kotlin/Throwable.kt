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

@OptIn(ExperimentalForeignApi::class)
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
internal class ThrowableWrapper(val throwable: CPointer<ThrowableVar>) : Throwable()

@ExperimentalNativeApi
@ExperimentalForeignApi
internal fun Throwable.wrapToC(): CPointer<ThrowableVar> {
    logger.debug("Wrapping throwable.")
    logger.warn(getStackTrace().joinToString("\n"))

    val throwable = nativeHeap.alloc<ThrowableVar>()
    throwable.type = this::class.qualifiedName?.cstrPtr
    throwable.ref = StableRef.create(this).asCPointer()
    throwable.message = this.message?.cstrPtr
    val stacktraceList = mutableListOf<ByteVar>()
    this.getStackTrace().forEach {
        stacktraceList.add(it.cstrPtr.pointed)
    }
    val stacktraceArray = nativeHeap.allocArrayOfPointersTo(stacktraceList)
    throwable.stacktrace = stacktraceArray
    throwable.stacktraceSize = stacktraceList.size
    return throwable.ptr
}

@ExperimentalNativeApi
@ExperimentalForeignApi
internal inline fun <R> runCatching(
    throwablePtr: CPointer<CPointerVar<ThrowableVar>>?, block: () -> R
) = runCatching(block).fold(onSuccess = {
    Result.success(it)
}, onFailure = {
    val throwableVarPtr = it.wrapToC()
    if (throwablePtr != null && throwablePtr.rawValue != nativeNullPtr) {
        throwablePtr.pointed.value = throwableVarPtr
    }
    Result.failure(ThrowableWrapper(throwableVarPtr))
})
