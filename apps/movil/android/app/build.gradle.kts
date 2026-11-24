plugins {
    id("com.android.application")
    // The Flutter Gradle Plugin must be applied after the Android and Kotlin Gradle plugins.
    id("dev.flutter.flutter-gradle-plugin")
}

// Firma de release (CONFIG-001). La clave NUNCA vive en el repositorio: se lee de variables de
// entorno o de gradle properties (`-PAPORTAYA_KEYSTORE_FILE=...`). Si falta alguna, el build de
// release FALLA (ver `verificarFirmaDeRelease`); jamas cae a la clave de depuracion.
// Pasos para crear la keystore y los secretos: docs/operacion/firma-android-release.md
fun valorDeFirma(nombre: String): String? =
    (System.getenv(nombre) ?: project.findProperty(nombre) as String?)?.takeIf { it.isNotBlank() }

val variablesDeFirma = listOf(
    "APORTAYA_KEYSTORE_FILE",
    "APORTAYA_KEYSTORE_PASSWORD",
    "APORTAYA_KEY_ALIAS",
    "APORTAYA_KEY_PASSWORD",
)
val firmaFaltante = variablesDeFirma.filter { valorDeFirma(it) == null }

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
        applicationId = "bo.aportaya.aportaya_movil"
        // El dominio de la app web: los enlaces https://<este>/g/<codigo> (enlace del grupo)
        // abren esta app (App Links). Por omision, TEST; otro entorno lo pasa con
        // -PhostDeLaAppWeb=... y publica su propio assetlinks.json.
        manifestPlaceholders["hostDeLaAppWeb"] =
            (project.findProperty("hostDeLaAppWeb") as String?) ?: "app.aportaya.161.97.85.216.sslip.io"
        // You can update the following values to match your application needs.
        // For more information, see: https://flutter.dev/to/review-gradle-config.
        minSdk = flutter.minSdkVersion
        targetSdk = flutter.targetSdkVersion
        versionCode = flutter.versionCode
        versionName = flutter.versionName
    }

    signingConfigs {
        create("release") {
            if (firmaFaltante.isEmpty()) {
                storeFile = file(valorDeFirma("APORTAYA_KEYSTORE_FILE")!!)
                storePassword = valorDeFirma("APORTAYA_KEYSTORE_PASSWORD")
                keyAlias = valorDeFirma("APORTAYA_KEY_ALIAS")
                keyPassword = valorDeFirma("APORTAYA_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
        }
    }
}

// Cierre seguro: cualquier build de release sin la firma completa aborta antes de compilar.
val verificarFirmaDeRelease by tasks.registering {
    doLast {
        if (firmaFaltante.isNotEmpty()) {
            throw GradleException(
                "Build de release SIN firma: faltan " + firmaFaltante.joinToString(", ") +
                    ". El release no se firma con la clave de depuracion. Defini esas variables de " +
                    "entorno (o -P<NOMBRE>=...) con la keystore de produccion; ver " +
                    "docs/operacion/firma-android-release.md. Para probar localmente use `flutter run` o `--debug`."
            )
        }
        val ks = file(valorDeFirma("APORTAYA_KEYSTORE_FILE")!!)
        if (!ks.isFile) {
            throw GradleException("Build de release: APORTAYA_KEYSTORE_FILE no apunta a un archivo existente.")
        }
    }
}
tasks.configureEach {
    if (name == "preReleaseBuild") dependsOn(verificarFirmaDeRelease)
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
