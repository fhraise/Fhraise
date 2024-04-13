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

package xyz.xfqlittlefan.fhraise.flow

import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.*

open class IdMessageFlow<I, V>(
    mutableSharedFlow: MutableSharedFlow<Pair<I, V>> = MutableSharedFlow()
) : MutableSharedFlow<Pair<I, V>> by mutableSharedFlow {
    constructor(
        replay: Int = 0, extraBufferCapacity: Int = 0, onBufferOverflow: BufferOverflow = BufferOverflow.SUSPEND
    ) : this(MutableSharedFlow(replay, extraBufferCapacity, onBufferOverflow))

    suspend inline fun collect(id: I, block: FlowCollector<V>) =
        block.emitAll(filter { it.first == id }.map { it.second })

    suspend inline fun take(crossinline predicate: suspend (Pair<I, V>) -> Boolean): Pair<I, V> =
        filter(predicate).first()
}
