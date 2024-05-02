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

interface Platform {
    val name: String
}

expect val platform: Platform

interface JvmPlatform : Platform

interface AndroidPlatform : JvmPlatform

sealed interface DesktopPlatform : JvmPlatform {
    interface Linux : DesktopPlatform
    interface Windows : DesktopPlatform
    interface MacOs : DesktopPlatform
    interface Unknown : DesktopPlatform

    companion object
}

data object WasmPlatform : Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

sealed interface NativePlatform : Platform {
    data object LinuxArm64 : NativePlatform {
        override val name: String = "Linux Arm64"
    }

    data object LinuxX64 : NativePlatform {
        override val name: String = "Linux x64"
    }

    data object WindowsX64 : NativePlatform {
        override val name: String = "Windows x64"
    }
}
