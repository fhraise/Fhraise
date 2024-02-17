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

package xyz.xfqlittlefan.fhraise

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

class Permission(private val activity: ComponentActivity, private val permission: String) {
    private var firstRequest by PermissionDataStore.get(activity.lifecycleScope, permission)

    val granted: Boolean?
        get() {
            if (ContextCompat.checkSelfPermission(
                    activity, permission
                ) == PackageManager.PERMISSION_GRANTED
            ) return true

            if (firstRequest) return null

            return if (activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) null else false
        }

    fun <T> registerRequestLauncher(onRegister: (request: suspend () -> Boolean?) -> T): T {
        var continuation: Continuation<Boolean?>? = null
        val launcher = activity.registerForActivityResult(ActivityResultContracts.RequestPermission()) {
            val result = if (it) true else granted
            continuation?.resumeWith(Result.success(result))
        }

        return onRegister {
            suspendCoroutine { cont ->
                continuation = cont
                launcher.launch(permission)
                firstRequest = false
            }
        }
    }
}

fun ComponentActivity.permission(permission: String) = Permission(this, permission)
