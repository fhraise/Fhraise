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

/*
 * Copyright 2023 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.datastore.core

import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.contracts.ExperimentalContracts

/**
 * SingleProcessCoordinator does coordination within a single process. It is used as the default
 * [InterProcessCoordinator] immplementation unless otherwise specified.
 */
internal class SingleProcessCoordinator(
    /** The canonical file path of the file managed by [SingleProcessCoordinator]. */
    private val filePath: String
) : InterProcessCoordinator {
    private val mutex = Mutex()
    private var version by atomic(0)

    override val updateNotifications: Flow<Unit> = flow {}

    // run block with the exclusive lock
    override suspend fun <T> lock(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }

    // run block with an attempt to get the exclusive lock, still run even if
    // attempt fails. Pass a boolean to indicate if the attempt succeeds.
    @OptIn(ExperimentalContracts::class) // withTryLock
    override suspend fun <T> tryLock(block: suspend (Boolean) -> T): T {
        return mutex.withTryLock {
            block(it)
        }
    }

    // get the current version
    override suspend fun getVersion(): Int = version

    // increment version and return the new one
    override suspend fun incrementAndGetVersion(): Int = ++version
}
