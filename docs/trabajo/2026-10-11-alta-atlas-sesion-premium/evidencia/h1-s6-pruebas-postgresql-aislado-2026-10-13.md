# H1.S6.M2–M5 y H10.S4.M5 — pruebas web y de integración

Fecha: 2026-10-13 (America/La_Paz).

## Entorno reproducible

- Para no tocar PostgreSQL existente ni depender de Docker, se inicializó un clúster PostgreSQL 18 aislado bajo `%LOCALAPPDATA%\Temp\pasanaku-pg-5433-20261013`, limitado a `127.0.0.1:5433`, `shared_buffers=32MB`, `max_connections=20` y `work_mem=4MB`.
- En una base limpia del clúster se aplicó `sql/aplicar.sql` con `ON_ERROR_STOP=1` (exit 0) y se fijó el `search_path` igual al configurado por los tests Testcontainers.
- Se añadió a `BaseDeCU04` un modo opt-in por `APORTAYA_TEST_DB_URL` (y variables opcionales de usuario/clave). Sin esa variable, el test conserva su ruta Testcontainers original. La corrida usó la base temporal limpia, no la instancia PostgreSQL existente.
- `:servicios:identidad:generateJooq` terminó `BUILD SUCCESSFUL` contra la base aislada.

## Resultados

Se ejecutaron las tareas correctas por corredor —no `test`, que excluye las clases `*WebTest` y `CU*Test`— con Gradle serial, un worker y heap limitado a 384 MiB:

- `:servicios:identidad:webTest --tests '*SesionesControllerWebTest' --tests '*SesionControllerWebTest'`: **22/22 PASS**, 0 fallos, 0 errores. XML: 16 casos de `SesionesControllerWebTest` y 6 de `SesionControllerWebTest`.
- `:servicios:identidad:integrationTest --tests '*CU04*Test'`: **31/31 PASS**, 0 fallos, 0 errores en una base limpia. XML: `CU04Test` 13/13, `CU04RechazosTest` 9/9 y `CU04RenovacionTest` 9/9.
- El gate de `H10.S1.M8`, `:servicios:identidad:test --max-workers=2 -Dorg.gradle.parallel=false`, terminó `BUILD SUCCESSFUL`; `gradlew --stop` respondió `No Gradle daemons are running`.

Los casos cubren: token móvil en cuerpo sin cookie y cookie WEB; refresh por cuerpo, rotación y rechazos; `DELETE /sesion` con id del JWT e idempotencia 204; revocación de sesión/familia, no revocar una sesión ajena, reuso fuera de gracia; y expiración absoluta de sesión de ocho horas. La suite también verifica que solo se almacena el hash del refresh.

Una corrida inicial de los tres grupos sobre una base temporal ya usada falló por teléfonos fixture duplicados. Se repitió el conjunto en una base nueva y aislada: los 31 casos pasaron. No se regeneró DDL ni baseline de producto a partir de esa corrida fallida.

## Estado

La evidencia satisface los criterios de `H1.S6.M2–M5`, `H10.S1.M8` y `H10.S4.M5`; sus estados quedan `HECHO`. No equivale al recorrido E2E de toda la pila: `H1.S6.M8` sigue bloqueado hasta ejecutar los servicios completos y su flujo de red. Tampoco acredita ningún recorrido Android de H10.S2/H10.S4.M3–M4.
