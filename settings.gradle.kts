/*
 * Copyright (c) 2018.
 *
 * This file is part of MoneyWallet.
 *
 * MoneyWallet is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MoneyWallet is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MoneyWallet.  If not, see <http://www.gnu.org/licenses/>.
 */

pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Some dependencies only exist on JitPack.
        maven("https://jitpack.io")
        // JCenter is shut down. A few artifacts (bottomsheetbuilder, bottomsheetpickers,
        // fingerprint-auth, free_reflection) are only available from this read-only mirror.
        // Remove it once those libraries have been replaced (see the modernization plan, Phase 3).
        maven("https://maven.aliyun.com/repository/public")
    }
}

plugins {
    // Provisions a JDK for the Gradle daemon when the required one is not installed locally.
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

rootProject.name = "RabbitWallet"

include(":app")
