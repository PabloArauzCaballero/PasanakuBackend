// Unica entrada publica detras de NGINX: enruta por prefijo, corta por tasa y
// propaga x-request-id. NO compone respuestas, no traduce errores y no consulta la
// base — un gateway con logica es el monolito volviendo por la puerta de atras.
plugins { id("aportaya.servicio") }

// aportaya.servicio agrega testFixtures(comun-web) a TODO servicio (linea 25,
// buildSrc/aportaya.servicio.gradle.kts) para que la capa web venga con MockMvc
// y la sabana de seguridad sin repetir catorce veces el mismo arnes. Gateway es
// el UNICO que no la puede usar: comun-web trae Spring Security de verdad
// (@EnableWebSecurity via WebSecurityConfiguration) y eso colisiona con el
// ReactiveManagementWebSecurityAutoConfiguration propio de un gateway sin
// Spring Security por diseno (comentario de arriba). Descubierto escribiendo
// ArranqueRateLimitGatewayTest (H3.S1): el contexto de Spring no levantaba por
// dos beans `springSecurityFilterChain` (uno servlet, uno reactivo) peleando
// por el mismo nombre.
configurations.testImplementation {
    exclude(group = "bo.aportaya", module = "comun-web")
}

dependencies {
    implementation(platform(libs.spring.cloud.bom))
    implementation(libs.spring.cloud.gateway)
    implementation(libs.spring.boot.actuator)
    // SIN Spring Security. El gateway enruta; quien autoriza es cada servicio, que
    // deniega por omision, valida el JWT contra el JWKS de identidad y comprueba el
    // permiso del endpoint. Con el starter puesto y ninguna cadena declarada, la
    // autoconfiguracion cerraba TODO: `POST /api/v1/sesiones` —la ruta por la que se
    // entra al sistema— devolvia 403 sin llegar nunca a identidad, y desde afuera
    // parecia que el gateway no enrutaba. La alternativa era declarar una cadena con
    // `permitAll()`, que es la misma regla escrita dos veces y ademas lo que el gate
    // de seguridad prohibe con razon. No tener la dependencia es mas honesto: el
    // gateway no sabe de permisos porque no le toca.
    implementation(libs.micrometer)
    implementation(libs.micrometer.tracing)
    // H3.S1 (AMB-6): RequestRateLimiter necesita Redis reactivo. Coordenada
    // literal y no `libs.spring.boot.redis`: esa entrada del catalogo vive en
    // el micro-PR troncal (PR #2, pablo/troncal/cyclonedx), todavia sin
    // mergear — mismo criterio que CycloneDX (commit 8c6bc4d), para no
    // depender de ese merge esta noche. El BOM de Spring Boot (ya aplicado
    // por aportaya.servicio) fija la version; no hace falta declararla aca.
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    testImplementation(libs.bundles.pruebas)
    // Modulo oficial de Testcontainers para Redis (com.redis, no
    // org.testcontainers): verificado contra testcontainers.com/modules/redis
    // el 2026-09-21.
    testImplementation("com.redis:testcontainers-redis:2.2.2")
    testRuntimeOnly(libs.junit.platform.launcher)
}
