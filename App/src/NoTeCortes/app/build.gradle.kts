plugins {
    id("com.android.application")
    id("com.google.gms.google-services")
    //id("com.android.application")
    //id("com.google.gms.google-services") // 🔑 debe estar aquí
}

android {
    namespace = "com.example.notecortes"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.notecortes"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
}

dependencies {
    // AndroidX y UI (Alineadas a versiones estables)
    implementation("androidx.appcompat:appcompat:1.6.1") // Más estable para Java que la 1.7.x
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation ("androidx.activity:activity:1.8.0")

    // Firebase (USA SOLO UN BOM, el más reciente)
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-auth")      // Sin versión manual
    implementation("com.google.firebase:firebase-firestore") // Sin versión manual
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation ("com.google.firebase:firebase-messaging-ktx")
    implementation ("com.google.firebase:firebase-messaging")

    // Otras utilidades
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // UI components
    implementation("com.github.bumptech.glide:glide:4.16.0")
    implementation("androidx.navigation:navigation-fragment:2.9.8")
    implementation("androidx.navigation:navigation-ui:2.9.8")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.cardview:cardview:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
}