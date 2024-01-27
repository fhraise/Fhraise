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

package datastore

import androidx.datastore.core.DataMigration
import androidx.datastore.core.DataStoreFactory
import androidx.datastore.core.Storage
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.core.okio.OkioStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okio.FileSystem
import okio.Path.Companion.toOkioPath
import java.io.File

actual object PreferenceDataStoreFactory {
    fun create(
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>? = null,
        migrations: List<DataMigration<Preferences>> = listOf(),
        scope: CoroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob()),
        produceFile: () -> File
    ): DataStore<Preferences> {
        val delegate = create(
            storage = OkioStorage(FileSystem.SYSTEM, PreferencesSerializer) {
                val file = produceFile()
                check(file.extension == PreferencesSerializer.fileExtension) {
                    "File extension for file: $file does not match required extension for" + " Preferences file: ${PreferencesSerializer.fileExtension}"
                }
                file.absoluteFile.toOkioPath()
            }, corruptionHandler = corruptionHandler, migrations = migrations, scope = scope
        )
        return PreferenceDataStore(delegate)
    }

    fun create(
        storage: Storage<Preferences>,
        corruptionHandler: ReplaceFileCorruptionHandler<Preferences>?,
        migrations: List<DataMigration<Preferences>>,
        scope: CoroutineScope,
    ): DataStore<Preferences> {
        return PreferenceDataStore(
            DataStoreImpl(
                DataStoreFactory.create(
                    storage = storage, corruptionHandler = corruptionHandler, migrations = migrations, scope = scope
                )
            )
        )
    }
}
