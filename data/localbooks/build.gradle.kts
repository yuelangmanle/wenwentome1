plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.wenwentome.reader.data.localbooks"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
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

    implementation(libs.epublib.core)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}

// 计划文档里用的是 `:data:localbooks:test`；Android library 默认是 `testDebugUnitTest`。
// 这里做一个轻量别名，避免后续指令跑不起来。
val testTask = tasks.findByName("test")
if (testTask == null) {
    tasks.register("test") {
        dependsOn("testDebugUnitTest")
    }
} else {
    testTask.dependsOn("testDebugUnitTest")
}
