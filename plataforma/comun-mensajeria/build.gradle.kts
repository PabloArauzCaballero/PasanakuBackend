// Outbox transaccional, relevo a Kafka y consumidor idempotente (ADR-018). El
// evento se escribe en la MISMA transaccion que el hecho; publicar viene despues.
plugins { id("aportaya.libreria") }

dependencies {
    api(project(":plataforma:comun-dominio"))
    api(project(":plataforma:comun-datos"))
    implementation(libs.jackson.databind)
    implementation(libs.micrometer)
    compileOnly(libs.jakarta.xml.bind)
    implementation(libs.kafka)
    // api y no implementation: ConfiguracionMensajeria (H2.S1.M2) es publica y lleva
    // @EnableSchedulerLock y devuelve LockProvider -- quien la importa (comun-web,
    // via @Import) necesita esas clases en SU classpath de compilacion tambien.
    // Con implementation, comun-web fallaba en compileJava: "Cannot find annotation
    // method 'defaultLockAtMostFor()' in type 'EnableSchedulerLock'" bajo -Werror.
    api(libs.shedlock)
    implementation(libs.shedlock.jdbc)
    // H2.S1.M2: ConfiguracionMensajeria (produccion, no prueba) usa JdbcTemplate y las
    // anotaciones @ConditionalOn... de spring-boot-autoconfigure -- ambas llegan
    // transitivamente con el starter de JDBC. Antes solo estaba en testImplementation,
    // que alcanzaba para RelevoTest pero no para que el propio modulo compilara su
    // configuracion de Spring.
    implementation(libs.spring.boot.jdbc)

    testImplementation(project(":plataforma:comun-pruebas"))
    testImplementation(libs.bundles.pruebas)
    testImplementation(libs.testcontainers.kafka)
    testRuntimeOnly(libs.junit.platform.launcher)
}

// Piso de cobertura — TRINQUETE, fijado con evidencia (ADR-026, ADR-043).
// Medido con `./gradlew test webTest integrationTest && ./gradlew jacocoTestReport`,
// redondeado hacia abajo y con dos puntos de margen: no puede bajar, y un
// refactor legitimo no tumba el build. Para subirlo: `python3 scripts/cobertura.py`.
extra["pisoDeCobertura"] = 0.46
extra["pisoDeRamas"] = 0.64
