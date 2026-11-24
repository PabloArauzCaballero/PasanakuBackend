# H3–H5 — lecturas de personal interno, roles asignables y paginación

## Integración de identidad completa (PostgreSQL real, Testcontainers, local)
```text
$ ./gradlew :servicios:identidad:integrationTest
BUILD SUCCESSFUL in 48s
tests=222 skipped=0 failures=0 errors=0
testsuite name="bo.aportaya.identidad.UsuariosInternosLecturaRepositorioTest" tests="4" skipped="0" failures="0"
testsuite name="bo.aportaya.identidad.UsuariosInternosRepositorioTest" tests="3" skipped="0" failures="0"
```

Primera corrida: 2 fallos clasificados TEST_BUG y corregidos sin debilitar aserciones: (1) el texto del par esperado cambió de orden al aplicar la decisión de publicidad; (2) la fixtura de `bloqueo_cuenta` enviaba las fechas como varchar (se agregó `::timestamptz`).

## Unitarios y web
```text
$ ./gradlew :servicios:identidad:test :servicios:identidad:webTest :servicios:identidad:testBarrido :servicios:publicidad:webTest
BUILD SUCCESSFUL in 29s
testsuite name="bo.aportaya.identidad.SegregacionDeFuncionesTest" tests="17" skipped="0" failures="0"
testsuite name="bo.aportaya.identidad.ListarUsuariosInternosTest" tests="2" skipped="0" failures="0"
testsuite name="bo.aportaya.identidad.web.RolesControllerWebTest" tests="5" skipped="0" failures="0"
testsuite name="bo.aportaya.identidad.web.UsuariosInternosListadoWebTest" tests="3" skipped="0" failures="0"
testsuite name="bo.aportaya.identidad.web.UsuariosInternosControllerWebTest" tests="9" skipped="0" failures="0"
testsuite name="bo.aportaya.identidad.web.SeguridadWebTest" tests="6" skipped="0" failures="0"
testsuite name="bo.aportaya.identidad.web.NombresDeUsuariosInternosWebTest" tests="7" skipped="0" failures="0"
testsuite name="bo.aportaya.publicidad.web.PublicidadListadosWebTest" tests="5" skipped="0" failures="0"
Primera corrida: testBarrido rojo «sin-umbral-literal» por la constante LIMITE_POR_DEFECTO = 50 (es tamaño de página, no cifra regulatoria): renombrada a FILAS_POR_PAGINA.
```
