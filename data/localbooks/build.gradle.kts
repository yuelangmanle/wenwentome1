plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wenwentome.reader.data.localbooks"
    compileSdk = 35

    defaultConfig {
        minSdk = 21
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")

    implementation(libs.epublib.core) {
        exclude(group = "xmlpull", module = "xmlpull")
    }

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
