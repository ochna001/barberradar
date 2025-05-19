plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.barberradar"
    compileSdk = 35
    
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }

    defaultConfig {
        applicationId = "com.example.barberradar"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // PayMongo configuration
        buildConfigField("String", "PAYMONGO_PUBLIC_KEY", "\"pk_test_zPYnBVAJeMsB4S2tieMK6sV5\"")
        buildConfigField("String", "PAYMONGO_RETURN_URL", "\"barberradar://payment/return\"")
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
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    buildFeatures {
        viewBinding = true
        buildConfig = true
    }
    
    packagingOptions {
        resources.excludes.add("META-INF/*")
    }

    sourceSets {
        getByName("main") {
            java.srcDirs("src/main/java", "src/main/java/com/example/barberradar/models")
            assets.srcDirs("src/main/assets")
        }
    }
}

dependencies {
    implementation(libs.room.common.jvm)
    implementation(libs.room.runtime.android)
    implementation(libs.cardview)
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    
    // Google Sign-In dependencies
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    
    // Firebase UI dependencies
    implementation("com.firebaseui:firebase-ui-auth:8.0.2")
    implementation(libs.firebase.storage)
    
    // PayMongo API integration
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")  // This will automatically use the correct Gson version
    implementation("com.squareup.okhttp3:logging-interceptor:4.9.3")
    implementation("com.google.code.gson:gson:2.10.1")  // Explicit Gson version
    
    // Ensure we have the correct OkHttp version
    implementation(platform("com.squareup.okhttp3:okhttp-bom:4.9.3"))
    implementation("com.squareup.okhttp3:okhttp")
    implementation("com.squareup.okhttp3:logging-interceptor")

    coreLibraryDesugaring(libs.desugar.jdk.libs)
    implementation(libs.view.v262)
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.constraintlayout)
    implementation(libs.lifecycle.livedata.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
    implementation(libs.play.services.maps)

    // Location Services (optional, for user location)
    implementation(libs.play.services.location)

    implementation(libs.navigation.fragment.ktx)
    implementation(libs.navigation.ui.ktx)
    implementation(libs.places)
    implementation(libs.volley)
    implementation(libs.android.maps.utils)
    implementation(libs.play.services.location.v2101)
    implementation(libs.glide)
    annotationProcessor(libs.compiler)


    // Room runtime
    implementation (libs.room.runtime)
    // Room annotation processor
    annotationProcessor (libs.room.compiler)
    
    // SwipeRefreshLayout dependency
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}
