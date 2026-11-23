# Traspaso de Pasanaku a Claude

**Fecha:** 23 de noviembre de 2026 (America/La_Paz)
**Objetivo:** continuar el trabajo pendiente en Pasanaku y promover a TEST solo cambios integrados y validados.
**Repositorios:** `PabloArauzCaballero/PasanakuBackend` y `PabloArauzCaballero/PasanakuFrontend`.

## Decisiones confirmadas por Pablo

- `PasanakuFrontend/dev` se despliega y debe seguir compatible con la app móvil. Conservar `cobrarAporte` y su contrato legado.
- Para CU-132, `TARIFARIO_ADMINISTRAR` permite proponer y aprobar parámetros, con aprobación por una persona distinta de quien propuso.
- TEST tiene credenciales/cuentas de servicio preparadas y el usuario confirmó que `chequear` da **PASA**. Después de desplegar, aún se debe ejecutar `estado`/health desde el terminal del usuario.
- M08: opción A, partición mensual y tabla de deduplicación; retención acordada de 24 meses. Queda por precisar si se acepta que una partición mensual implique retener registros casi 25 meses o si se exige un máximo estricto de 24 meses (lo que podría requerir partición diaria/subpartición y política de purga).
- CU-132 M6: el conjunto completo para congelar tarifas incluye **todos los niveles aplicables definidos por Transparencia**.
- No hacer merge ni promoción a `main` sin autorización explícita de Pablo.

## Estado de ramas y cambios publicados

### Backend `PasanakuBackend`

- `dev` está en `aa73389502d71298d8883c29db9b2b2336405598`; PR #500 (“retirar factor fijo”) ya se integró. Sus checks quedaron verdes.
- `test` estaba en `5dca19d6efa983aa16fa6fbacba38811f64fab9d` al último registro (promoción de #498). No promover las PR abiertas hasta corregir los fallos y completar revisión.
- PR #503, rama/head `bf40633d9a473583ffcf5b1ec2de2e4aaa2db00d`, backoffice CU-132: abierta y mergeable. Seguridad, esquema, humo PostgreSQL, contratos y dependencias pasaron. CI reportó fallo genérico en `spotlessCheck`, aunque localmente pasó. El chequeo Frontend ADR-044 seguía ejecutándose; revisar estado actual antes de avanzar.
- PR #504, rama/head `2ae4f00ae1a5ad2df6b9b26009b57fcd14733b54`, `grupos.turnos_asignados`: abierta y mergeable. `integrationTest` falla/no llega a probar. Diagnóstico local: bloqueo de JaCoCo desactualizado (lockfile de `servicios/grupos` fija 0.8.14 y Gradle selecciona 0.8.15; error “Dependency lock state out of date” al resolver `jacocoAgent`). Antes hubo reproducción inconclusa de compilación y DB: usar `BD_URL_ADMIN=jdbc:postgresql://127.0.0.1:55448/pasanaku`; logs de Actions no accesibles (403). Corregir el lockfile mediante Gradle `--write-locks` sobre esta rama, no editándolo a mano; luego repetir test e inspeccionar el resultado.
- PR #505, rama/head `152dc128e6707c46f0ac10695a01eb32cbe6244a`, CU-44 separación analista/revisor: abierta y mergeable. Se mantiene `analistaId` opcional/deprecado e ignorado y no se cambia la ruta; se conserva `cobrarAporte`. Postman/contratos pasaron tras regenerar colecciones; Spotless local de cumplimiento pasó. CI de reglas/formato falla genéricamente en `./gradlew check -x test -x jacocoTestCoverageVerification -x jacocoTestReport`. El agente `/root/fix_505` estaba ejecutando reproducción local con `--stacktrace`; obtener su resultado antes de cerrar el diagnóstico.
- Límite actual: hay 3 PR abiertas a `dev` (#503–#505), por eso no abrir otra para el checkpoint de este traspaso. PR abiertas a `main`: #468, #495, #496 y #497; #468 está roja/bloqueada por permisos. No mezclar con el trabajo hacia `dev`/TEST.

### Checkpoints para continuar (no son merges a `dev`)

- M08: rama `codex/claude-handoff-m08`, commit `731459c5fa3ca7f6aa03258db1905b88252fbedc`, worktree `/tmp/pasanaku-l1-m08`. Checkpoint de 88 archivos con modelo/esquema/generador, 13 seeds y documentación/migración. `python3 scripts/generar_ddl.py` pasó (356 tablas, 704 FKs, 818 índices, 561 CHECKs); `git diff --check` y `git show --check` pasaron. **Incompleto:** aún falta validar swap/backfill en PostgreSQL aislado, añadir/validar purga de dedupe e historial, resolver granularidad exacta de TTL, y migrar/validar código Java.
- M6 Transparencia: rama `codex/claude-handoff-m6-transparencia`, commit `fcbc557b12cdd32906402cbc89a6c13ab27812ae`, worktree `/tmp/pasanaku-m6-levels`. Ya consume códigos de nivel desde `GET /reputacion/etapas` (`EtapaDelCatalogo.codigo`) y modela snapshot append-only `nivel_aplicable_congelado_grupo` con unicidad `(grupo_id,nivel_codigo)` y parte de la lógica freeze/validate. Documentación sustenta que `porNivel` usa niveles/códigos de Transparencia. `git diff --check` y validación JSON/pact pasaron; **no** hubo build ni pruebas. Falta completar la verificación de todos los niveles aplicables, pruebas y revisión integral.
- Frontend: commit `c28f69bb` en `codex/cu21-pagar-cuota-saldo-frontend`, publicado en origin. Conserva endpoint/OpenAPI `cobrarAporte` para compatibilidad y cambia la pantalla móvil a `pagarCuotaConSaldo`. `git diff --check` pasó; no había Flutter/Dart local, CI ni PR para este commit al último estado. Verificar CI cuando exista capacidad y no abrir PR adicional si sigue vigente el límite de PR abiertas.

## Promoción a TEST y límites de este entorno

- La configuración de TEST está preparada según Pablo (`chequear=PASA`), pero no se hizo despliegue/promoción en esta sesión. Primero completar los checks, integrar los cambios aprobados a `dev`, revisar el diff acumulado y ejecutar la ruta acordada `dev → test` con sus validaciones y aprobación operativa habitual.
- Después del despliegue, pedir/ejecutar en el terminal del usuario el comando `estado`/health para confirmar el servicio real. El acceso SSH a `161.97.85.216` está bloqueado desde este entorno.
- La descarga de logs/artefactos GitHub Actions respondió 403. Hay un borrador guardado de salida de red que incluye hosts de resultados de Actions (`results-receiver.actions.githubusercontent.com`, `productionresultssa6.blob.core.windows.net` y otros), pero el entorno activo continúa en revisión de especificación 3; el borrador/configuración revisada es revisión 5 y **no está publicada**. No se pudo publicar desde las herramientas disponibles. Para diagnosticar CI, pedir a Pablo/administración que revise y publique el borrador, o reproducir fallos localmente.
- No se copiaron secretos ni credenciales a este documento.

## Orden recomendado para Claude

1. Consultar PR/checks y heads actuales; los hashes anteriores son la última evidencia disponible y pueden haber cambiado. Recuperar el resultado final del agente local de #505.
2. Diagnosticar y arreglar #505 hasta que `check -x test -x jacocoTestCoverageVerification -x jacocoTestReport` pase local y CI; actualizar la PR y esperar checks completos.
3. Corregir en #504 el lock de JaCoCo usando Gradle `--write-locks`, revisar el diff del lockfile y ejecutar `integrationTest`/el test afectado en ambiente PostgreSQL compatible; luego esperar CI verde.
4. Resolver el `spotlessCheck` de CI y completar todos los checks de Frontend ADR-044 en #503; confirmar que los permisos y reglas de doble aprobación reflejan las decisiones del usuario.
5. Revisar en conjunto y mergear a `dev` solamente PRs verdes y coherentes. Evitar promover a TEST partes incompletas de M08/M6.
6. Completar M6/M08 en sus ramas checkpoint, respetando las decisiones arriba, incorporando pruebas de base de datos/migración y revisión de compatibilidad. No integrar esos checkpoints hasta cerrar sus pendientes.
7. Cuando todo el conjunto destinado a TEST esté integrado a `dev`, validar el diff/versiones y promover `dev → test`; ejecutar smoke/health y pedir la comprobación final `estado` en el host del usuario.
8. Mantener `main` intacta hasta que Pablo autorice explícitamente esa promoción.

## Nota de traspaso

Las ramas `codex/claude-handoff-*` guardan checkpoints para recuperación. No son aprobación de merge ni certificación de release. El estado de CI, PRs y ramas puede cambiar; volver a consultarlo antes de cualquier operación.
