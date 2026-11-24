# H2.S1.M2 — matriz de roles del backoffice

```text
$ yarn ng test --include='src/app/nucleo/matriz-de-roles.spec.ts' --watch=false   (apps/backoffice)
 Test Files  1 passed (1)
      Tests  44 passed (44)
```

Incluye «el catálogo parseado tiene los treinta y tres roles…» y «SUPERVISOR_CUMPLIMIENTO abre cumplimiento».

# H2.S1.M3 — etiquetas de rol de los tests que levantan bloqueos
```text
$ ./gradlew :servicios:nucleo-financiero:webTest --tests '*Bloqueo*WebTest'
BUILD SUCCESSFUL
testsuite name="bo.aportaya.nucleofinanciero.web.BloqueoAutoridadWebTest" tests="7" skipped="0" failures="0" errors="0"
testsuite name="bo.aportaya.nucleofinanciero.web.BloqueoListadoWebTest" tests="14" skipped="0" failures="0" errors="0"
$ grep -rn "\"OFICIAL_CUMPLIMIENTO\", \"BLOQUEO_AUTORIDAD_LEVANTAR\"" servicios apps   → sin resultados
```
