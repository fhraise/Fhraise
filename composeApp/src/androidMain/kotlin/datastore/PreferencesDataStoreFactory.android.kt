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

actual object PreferenceDataStoreFactory {
    fun create(
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
}
