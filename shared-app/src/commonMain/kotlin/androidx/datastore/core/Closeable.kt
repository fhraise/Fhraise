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
 * Copyright 2022 The Android Open Source Project
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

/**
 * Datastore common version of java.io.Closeable
 */
@Suppress("NotCloseable") // No closable in KMP common.
interface Closeable {

    /**
     * Closes the specified resource.
     */
    fun close()
}

/**
 * Ensures a Closeable object has its close() method called at the end of the supplied block.
 *
 * @throws Throwable any exceptions thrown in the block will propagate through this method.
 */
@Suppress("NotCloseable", "DocumentExceptions") // No closable in KMP common.
inline fun <T : Closeable, R> T.use(block: (T) -> R): R {
    var thrown: Throwable? = null

    try {
        return block(this)
    } catch (t: Throwable) {
        thrown = t
    } finally {
        try {
            this.close()
        } catch (t: Throwable) {
            if (thrown == null) {
                thrown = t
            } else {
                thrown.addSuppressed(t)
            }
        }

        if (thrown != null) {
            throw thrown
        }
    }
    // We either returned in the try block, or thrown must be not null, so this code is unreachable.
    error(
        """Unreachable code. If this occurs, please file a bug here:
        https://b.corp.google.com/issues/new?component=907884&template=1466542"""
    )
}
