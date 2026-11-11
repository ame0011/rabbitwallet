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

plugins {
    alias(libs.plugins.android.application)
}

// The api-keys can be provided at build time inside gradle.properties (or with -P on the command
// line). Never commit a real key.
val apiKeyDropbox: String = providers.gradleProperty("ApiKey_Dropbox").get()
val apiKeyOpenExchangeRates: String = providers.gradleProperty("ApiKey_OpenExchangeRates").get()

android {
    namespace = "com.amesingularity.rabbitwallet"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.amesingularity.rabbitwallet"
        minSdk = 23
        targetSdk = 36
        versionCode = 75
        versionName = "4.0.5.10"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        testInstrumentationRunnerArguments["clearPackageData"] = "true"

        manifestPlaceholders["DropBoxApiKey"] = apiKeyDropbox.replace("\"", "")

        buildConfigField("String", "API_KEY_DROPBOX", apiKeyDropbox)
        buildConfigField("String", "API_KEY_OPEN_EXCHANGE_RATES", apiKeyOpenExchangeRates)
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android.txt"), "proguard-rules.pro")
        }
        debug {
            applicationIdSuffix = ".dev"
            isDebuggable = true
        }
    }

    flavorDimensions += "version"
    productFlavors {
        create("proprietary") {
            dimension = "version"
        }
        create("floss") {
            dimension = "version"
        }
    }

    buildFeatures {
        buildConfig = true
        // TODO: enable view binding once the layout/ and layout-w600dp/ variants of
        // activity_single_panel_appbar.xml expose the same root element.
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        // The legacy sources predate the current lint checks. The baseline records the existing
        // findings so that only newly introduced issues fail the build.
        baseline = file("lint-baseline.xml")
        checkDependencies = false
    }

    testOptions {
        unitTests {
            // Required by Robolectric to load the merged resources (R classes, assets, manifest).
            isIncludeAndroidResources = true
            all {
                // JDK21: Robolectric's AndroidInterceptors need jdk.internal.access,
                // otherwise reflection fails with IllegalAccessException.
                it.jvmArgs(
                    "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
                    "--add-opens=java.base/java.io=ALL-UNNAMED",
                    "--add-opens=java.base/java.lang=ALL-UNNAMED",
                )
            }
        }
    }
}

configurations.all {
    exclude(module = "commons-logging")
    // The FastAdapter artifacts published to JCenter are gone. Swap the whole FastAdapter family
    // for the upstream build on JitPack. The v3.2.7 classes are identical to the 3.2.6 ones this
    // project was written against.
    resolutionStrategy.eachDependency {
        if (requested.group == "com.mikepenz") {
            when (requested.name) {
                "fastadapter", "library-core" ->
                    useTarget("com.github.mikepenz.FastAdapter:library-core:v3.2.7")
                "library" ->
                    useTarget("com.github.mikepenz.FastAdapter:library:v3.2.7")
                "fastadapter-extensions-expandable" ->
                    useTarget("com.github.mikepenz.FastAdapter:library-extensions-expandable:v3.2.7")
            }
        }
    }
}

dependencies {
    // common dependencies
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.cardview)
    implementation(libs.androidx.annotation)
    implementation(libs.material)
    implementation(libs.androidx.preference)
    implementation(libs.androidx.work.runtime)
    implementation(libs.material.dialogs.core)
    implementation(libs.material.dialogs.commons)
    implementation(libs.materialdrawer) {
        isTransitive = true
    }
    implementation(libs.materialish.progress)
    implementation(libs.bottomsheetbuilder)
    // JCenter shutdown: replaced with builds of the upstream sources hosted on JitPack.
    implementation(libs.pinlockview)
    implementation(libs.patternlockview)
    implementation(libs.bottomsheetpickers) {
        exclude(group = "com.android.support", module = "appcompat-v7")
        exclude(group = "com.android.support", module = "design")
    }
    implementation(libs.expansionpanel)
    implementation(libs.commons.io)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.fragment)
    implementation(libs.zip4j)
    implementation(libs.material.about.library)
    implementation(libs.appintro)
    implementation(libs.fingerprint.auth)
    implementation(libs.mpandroidchart)
    implementation(libs.viewpager.indicator)
    implementation(libs.material.spinner)
    implementation(libs.lib.recur)
    implementation(libs.opencsv)
    implementation(libs.jxl)
    implementation(libs.itextpdf)
    implementation(libs.free.reflection)
    implementation(libs.glide)
    annotationProcessor(libs.glide.compiler)

    // proprietary dependencies
    "proprietaryImplementation"(libs.dropbox.core.sdk)

    // map dependencies
    implementation(libs.osmdroid.android)

    // test dependencies (local tests)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)

    // android test dependencies (on-device tests)
    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.androidx.test.core)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.rules)
    androidTestUtil(libs.androidx.test.orchestrator)
}
