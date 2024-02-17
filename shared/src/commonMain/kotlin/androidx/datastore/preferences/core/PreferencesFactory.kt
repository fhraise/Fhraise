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

package androidx.datastore.preferences.core

/**
 * Get a new empty Preferences.
 *
 * @return a new Preferences instance with no preferences set
 */
fun emptyPreferences(): Preferences = MutablePreferences(startFrozen = true)

/**
 * Construct a Preferences object with a list of Preferences.Pair<T>. Comparable to mapOf().
 *
 * Example usage:
 * ```
 * val counterKey = intPreferencesKey("counter")
 * val preferences = preferencesOf(counterKey to 100)
 * ```
 *
 * @param pairs the key value pairs with which to construct the preferences
 */
fun preferencesOf(vararg pairs: Preferences.Pair<*>): Preferences = mutablePreferencesOf(*pairs)

/**
 * Construct a MutablePreferences object with a list of Preferences.Pair<T>. Comparable to mapOf().
 *
 * Example usage:
 * ```
 * val counterKey = intPreferencesKey("counter")
 * val preferences = mutablePreferencesOf(counterKey to 100)
 * ```
 * @param pairs the key value pairs with which to construct the preferences
 */
fun mutablePreferencesOf(vararg pairs: Preferences.Pair<*>): MutablePreferences =
    MutablePreferences(startFrozen = false).apply { putAll(*pairs) }
