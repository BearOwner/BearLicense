plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.bearmod.owner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.bearmod.owner"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val supabaseUrl: String = project.findProperty("supabaseUrl") as String? ?: ""
        val supabaseAnonKey: String = project.findProperty("supabaseAnonKey") as String? ?: ""
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("boolean", "USE_MOCKS", (supabaseUrl.isEmpty() || supabaseAnonKey.isEmpty()).toString())
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions { jvmTarget = "11" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(project(":shared"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // Use direct coordinate to avoid catalog accessor issues in new module
    implementation("androidx.compose.material3:material3")
    implementation(libs.coroutines.android)
}
