package xyz.xfqlittlefan.fhraise.py

import kotlinx.cinterop.*

@ExperimentalForeignApi
typealias OnError = (throwablePtr: ThrowableVar) -> Unit

@ExperimentalForeignApi
typealias OnResult = (type: CPointer<ByteVar>, ref: COpaquePointer) -> CPointer<*>
