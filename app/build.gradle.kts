import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.secrets)
}

val localSecrets = Properties().apply {
    val secretsFile = rootProject.file("local.properties")
    if (secretsFile.exists()) {
        secretsFile.inputStream().use { load(it) }
    }
}

fun String.asBuildConfigString(): String {
    val cleanValue = trim().trim('"')
    return "\"${cleanValue.replace("\\", "\\\\").replace("\"", "\\\"")}\""
}

android {
    namespace = "com.example.resume"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.example.resume"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "GENAI_API_KEY",
            localSecrets.getProperty("GENAI_API_KEY").orEmpty().asBuildConfigString()
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += setOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE*",
                "META-INF/NOTICE*",
                "META-INF/*.kotlin_module"
            )
        }
    }
}

secrets {
    defaultPropertiesFileName = "local.defaults.properties"
    ignoreList.add("GENAI_API_KEY")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.google.genai.kotlin)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
