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

package xyz.xfqlittlefan.fhraise.platform

import androidx.browser.customtabs.CustomTabsIntent

object AndroidUrlImpl {
    lateinit var openInApp: (url: String, builder: CustomTabsIntent.Builder.() -> Unit) -> BrowserActions
    lateinit var open: (url: String) -> BrowserActions
}

internal actual fun openUrlImpl(url: String, options: BrowserOptions) =
    if (options.inApp) AndroidUrlImpl.openInApp(url) {
        when (options.browserType) {
            BrowserType.Restricted -> {
                setBookmarksButtonEnabled(false)
                setDownloadButtonEnabled(false)
                setShareState(CustomTabsIntent.SHARE_STATE_OFF)
            }
        }
    } else AndroidUrlImpl.open(url)
