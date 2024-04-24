package xyz.xfqlittlefan.fhraise.py

import kotlinx.cinterop.*

@ExperimentalForeignApi
typealias OnData = CPointer<CFunction<(data: CArrayPointer<ByteVar>, size: Int) -> Unit>>

@Deprecated("This function is for calling from C code only.", level = DeprecationLevel.HIDDEN)
@ExperimentalForeignApi
fun byteArrayToPointer(byteArray: ByteArray, onData: OnData) { memScoped { onData(allocArrayOf(byteArray), byteArray.size) } }
