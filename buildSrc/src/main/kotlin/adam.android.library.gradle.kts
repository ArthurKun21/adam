import adam.buildlogic.AndroidConfig

plugins {
    id("com.android.library")
    // id("adam.code.lint")
}

android {
    defaultConfig {
        lint.targetSdk = AndroidConfig.TARGET_SDK
    }
    configureAndroid(this)
}
