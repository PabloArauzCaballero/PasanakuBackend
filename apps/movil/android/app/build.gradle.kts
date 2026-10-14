plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

android {
    namespace = "bo.aportaya.aportaya_movil"
    // `permission_handler_android` (escáner de identidad, CU-02) exige compileSdk 37;
    // el de Flutter por omisión es 36. compileSdk es hacia atrás compatible, así que
    // subirlo no afecta a minSdk/targetSdk (siguen en lo que define Flutter).
    compileSdk = 37
    ndkVersion = flutter.ndkVersion

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    defaultConfig {
        // TODO: Specify your own unique Application ID (https://developer.android.com/studio/build/application-id.html).
        applicationId = "bo.aportaya.aportaya_movil"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    buildTypes {
        release {
            // TODO: Add your own signing config for the release build.
            // Signing with the debug keys for now, so `flutter run --release` works.
            signingConfig = signingConfigs.getByName("debug")
        }
    }
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17
    }
}

flutter {
    source = "../.."
}

dependencies {
    // El puerto `Biometria` (carril F2, `dominio/puertos/biometria.dart`) habla con
    // `BiometricPrompt` nativo por `MethodChannel`: no agrega paquete de pub, solo
    // esta dependencia de Gradle. `android/app/build.gradle.kts` no figura entre los
    // archivos «no tocás» del carril — sí `pubspec.yaml` y `pubspec.lock`.
    implementation("androidx.biometric:biometric:1.1.0")
}
