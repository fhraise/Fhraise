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

import io.ktor.http.*

/**
 * 打开指定 URL
 *
 * 最终使用的 URL 为在 [url] 之上应用 [urlBuilder] 构建器构建的 URL
 *
 * @param url URL
 * @param optionsBuilder 浏览器启动选项构建器
 * @param urlBuilder URL 构建器
 *
 * @return [BrowserActions]
 *
 * @see BrowserOptions
 * @see URLBuilder
 * @see BrowserActions
 */
fun openUrl(url: String, optionsBuilder: BrowserOptions.() -> Unit = {}, urlBuilder: URLBuilder.() -> Unit = {}) =
    openUrlImpl(URLBuilder(url).apply(urlBuilder).buildString(), BrowserOptions().apply(optionsBuilder))

internal expect fun openUrlImpl(url: String, options: BrowserOptions): BrowserActions

/**
 * 浏览器启动类型，仅限 Android 且 [BrowserOptions.inApp] 为 `true`
 */
enum class BrowserType {
    /**
     * 不允许添加书签、下载网页、分享
     */
    Restricted,
}

/**
 * 浏览器启动选项
 */
class BrowserOptions {
    /**
     * 启动浏览器时使用的 [BrowserType]，默认为 [BrowserType.Restricted]
     */
    var browserType = BrowserType.Restricted

    /**
     * 是否在应用内打开，仅限 Android（使用 Custom Tabs）
     */
    var inApp = true

    /**
     * 是否使用弹出式窗口打开，仅限 Web
     */
    var popup = true
}

/**
 * 启动浏览器后的操作
 */
class BrowserActions {
    /**
     * 关闭浏览器，仅限 Android 且 [BrowserOptions.inApp] 为 `true`，行为只在堆栈顶部时符合预期
     */
    var close: () -> Unit = {}
}
