# H1 — segregación con códigos reales (PostgreSQL real vía Testcontainers, local)

## Rojo esperado: tests nuevos contra la SegregacionDeFunciones de dev (6729629d)
```text
$ ./gradlew :servicios:identidad:integrationTest --tests 'bo.aportaya.identidad.CU08Test' --tests 'bo.aportaya.identidad.CU08RechazosTest'
CU08RechazosTest > rechaza por R-SEG-04 · quien modera piezas no aprueba campañas: publicidad son tres personas FAILED
CU08RechazosTest > rechaza por R-SEG-04 · quien registra bloqueos por autoridad no recibe el rol que los levanta FAILED
CU08RechazosTest > rechaza por R-SEG-04 FAILED
CU08Test > Dado un usuario que ya tiene el permiso de autorizar entregas · Cuando se le intenta asignar el rol que ejecuta entregas · Entonces se rechaza con INCOMPATIBILIDAD_DE_FUNCIONES FAILED
18 tests completed, 4 failed
BUILD FAILED in 1m 17s

        at bo.aportaya.identidad.CU08RechazosTest.rechazaRSEG04Bloqueos(CU08RechazosTest.java:64)

CU08RechazosTest > rechaza por R-SEG-04 FAILED
    org.opentest4j.AssertionFailedError: 
    Expecting value to be true but was false
        at app//bo.aportaya.identidad.CU08RechazosTest.rechazaRSEG04(CU08RechazosTest.java:51)

CU08Test > Dado un usuario que ya tiene el permiso de autorizar entregas · Cuando se le intenta asignar el rol que ejecuta entregas · Entonces se rechaza con INCOMPATIBILIDAD_DE_FUNCIONES FAILED
    java.lang.AssertionError: 
```

## Verde con la tabla nueva
```text
$ ./gradlew :servicios:identidad:integrationTest --tests 'bo.aportaya.identidad.CU08*' :servicios:identidad:test --tests 'bo.aportaya.identidad.SegregacionDeFuncionesTest'
BUILD SUCCESSFUL
testsuite name="bo.aportaya.identidad.CU08CodigosDeRolVigentesTest" tests="2" skipped="0" failures="0" errors="0"
testsuite name="bo.aportaya.identidad.CU08RechazosTest" tests="9" skipped="0" failures="0" errors="0"
testsuite name="bo.aportaya.identidad.CU08Test" tests="9" skipped="0" failures="0" errors="0"
testsuite name="bo.aportaya.identidad.SegregacionDeFuncionesTest" tests="13" skipped="0" failures="0" errors="0"
```
