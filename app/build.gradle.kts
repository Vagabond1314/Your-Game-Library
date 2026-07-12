import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

// 1. Виправлено зчитування local.properties
val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

android {
    namespace = "com.your_game_library"
    compileSdk = 36

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
}
    defaultConfig {
        applicationId = "com.your_game_library"
        minSdk = 24
        targetSdk = 36
        versionCode = 2
        versionName = "1.1"

        // 2. Виправлено синтаксис buildConfigField
        buildConfigField("String", "IGDB_ID", "\"${localProperties.getProperty("IGDB_CLIENT_ID")}\"")
        buildConfigField("String", "IGDB_SECRET", "\"${localProperties.getProperty("IGDB_CLIENT_SECRET")}\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        externalNativeBuild {
            cmake {
                cppFlags("")
            }
        }
    }

    buildTypes {
        release {
            // 3. У Kotlin DSL булеві змінні пишуться через "is..." та "="
            isMinifyEnabled = true
            isShrinkResources = true

            // 4. Виправлено шлях до Proguard та подвійні лапки
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.squareup.picasso:picasso:2.71828")

    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.retrofit2:converter-scalars:2.9.0")

    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("net.dankito.readability4j:readability4j:1.0.8")
    implementation("org.jsoup:jsoup:1.15.3")
    implementation("com.github.chrisbanes:PhotoView:2.3.0")
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
    implementation("org.slf4j:slf4j-android:1.7.36")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.work:work-runtime:2.8.1")
    implementation("com.squareup.okhttp3:okhttp:4.10.0")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")
    implementation("com.google.code.gson:gson:2.10.1")
}