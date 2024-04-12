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

package xyz.xfqlittlefan.fhraise.html

import io.ktor.server.html.*
import io.ktor.server.routing.*
import kotlinx.html.*

inline fun HTML.autoClosePage(
    title: String = "Fhraise", crossinline body: BODY.() -> Unit = {
        p { +"你现在可以关闭这个页面" }
    }
) {
    head {
        title(title)
        script { unsafe { raw("window.close()") } }
    }
    body(block = body)
}

suspend inline fun RoutingCall.respondAutoClosePage(
    title: String = "Fhraise", crossinline body: BODY.() -> Unit = {
        p { +"你现在可以关闭这个页面" }
    }
) = respondHtml { autoClosePage(title, body) }
