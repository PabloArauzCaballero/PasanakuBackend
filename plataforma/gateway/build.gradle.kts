// Unica entrada publica detras de NGINX: enruta por prefijo, corta por tasa y
// propaga x-request-id. NO compone respuestas, no traduce errores y no consulta la
// base — un gateway con logica es el monolito volviendo por la puerta de atras.
plugins { id("aportaya.servicio") }

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

    testImplementation(libs.bundles.pruebas)
    testRuntimeOnly(libs.junit.platform.launcher)
}
