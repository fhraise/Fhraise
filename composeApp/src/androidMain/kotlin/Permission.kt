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

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.lifecycleScope
import datastore.edit
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class Permission(private val permission: String) {
    private val Context.firstRequestFlow
        get() = permissionsDataStore.data.map { it[booleanPreferencesKey(permission)] ?: true }

    val Activity.granted: Boolean?
        get() {
            if (ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED) return true

            if (runBlocking { firstRequestFlow.first() }) return null

            return if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) null else false
        }

    fun <T> ComponentActivity.registerRequestLauncher(onRegister: (request: suspend () -> Boolean?) -> T): T {
        var continuation: Continuation<Boolean?>? = null
        val launcher = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            val result = if (it) true else granted
            continuation?.resumeWith(Result.success(result))
        }

        return onRegister {
            suspendCoroutine { cont ->
                continuation = cont
                launcher.launch(permission)
                lifecycleScope.launch {
                    permissionsDataStore.edit { it[booleanPreferencesKey(permission)] = false }
                }
            }
        }
    }
}
