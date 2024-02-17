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

package xyz.xfqlittlefan.fhraise.data

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.essenty.instancekeeper.InstanceKeeper
import com.arkivanov.essenty.lifecycle.doOnDestroy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlin.coroutines.CoroutineContext

private const val INSTANCE_KEY = "com.arkivanov.decompose.lifecycle.ComponentContextCoroutineScope.INSTANCE_KEY"

val ComponentContext.componentScope: CoroutineScope
    get() {
        val scope = instanceKeeper.get(INSTANCE_KEY)
        if (scope is CoroutineScope) return scope

        fun destroy() {
            try {
                scope?.onDestroy()
            } catch (e: Exception) {
                throw RuntimeException(e)
            } finally {
                instanceKeeper.remove(INSTANCE_KEY)
            }
        }

        lifecycle.doOnDestroy {
            destroy()
        }

        return DestroyableCoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also {
            instanceKeeper.put(INSTANCE_KEY, it)
        }
    }

class DestroyableCoroutineScope(context: CoroutineContext) : CoroutineScope, InstanceKeeper.Instance {
    override val coroutineContext: CoroutineContext = context

    override fun onDestroy() {
        coroutineContext.cancel()
    }
}
