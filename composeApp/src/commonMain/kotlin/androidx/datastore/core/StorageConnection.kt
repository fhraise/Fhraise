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
 * StorageConnection provides a way to read and write a particular type <T> of data.
 * StorageConnections are created from [Storage] objects.
 */
interface StorageConnection<T> : Closeable {

    /**
     * Creates a scope for reading to allow storage reads, and will try to obtain a read lock.
     *
     * @param block The block of code that is performed within this scope.  Block will
     * receive `locked` parameter which is true if the try lock succeeded.
     *
     * @throws IOException when there is an unrecoverable exception in reading.
     */
    suspend fun <R> readScope(
        block: suspend ReadScope<T>.(locked: Boolean) -> R
    ): R

    /**
     * Creates a write scope that guaranteed to only have one single writer, ensuring also
     * that any reads within this scope have the most current data.
     *
     * @throws IOException when there is an unrecoverable exception in writing.
     */
    suspend fun writeScope(block: suspend WriteScope<T>.() -> Unit)

    /**
     * Provides a coordinator to guarantee data consistency across multiple threads and processes.
     */
    val coordinator: InterProcessCoordinator
}

/**
 * The scope used for a read transaction.
 */
interface ReadScope<T> : Closeable {

    /**
     * Read the data <T> from the underlying storage.
     */
    suspend fun readData(): T
}

/**
 * The scope used for a write transaction.
 */
interface WriteScope<T> : ReadScope<T> {

    /**
     * Writes the data <T> to the underlying storage.
     */
    suspend fun writeData(value: T)
}

/* Convenience method for opening a read scope, doing a single read, and closing the scope. */
suspend fun <T> StorageConnection<T>.readData(): T = readScope { readData() }

/* Convenience method for opening a write scope, doing a single write, and closing the scope. */
suspend fun <T> StorageConnection<T>.writeData(value: T) = writeScope { writeData(value) }
