# Reporte — Tanda 2, carril L4L6: identidad, cumplimiento y frontends compartidos

> **AVANCE: 7 / 8 — 87,5 %** de las microtareas abiertas hasta ahora (T2.M1–T2.M4, F1.L6.M04, F1.L4.M25, F1.L6.M12 HECHO; F1.L4.M03 A MEDIAS). El resto de F1/F2 de L4 y L6 sigue en TODO.

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Base: `origin/dev` 50cf3be9
- Ramas y PR (apilados, se mergean en orden):
  1. `correcciones/tanda2-l4l6-f1s3m1-token-vigente` — F1.S3.M1 (resto) · PR #420
  2. `correcciones/tanda2-l4l6-f1s4m1-campo-monto` — F1.S4.M1 / F1.L6.M03 · PR #426
  3. `correcciones/tanda2-l4l6-f1l6m04-patron-importe` — F1.L6.M04 · PR #427
  4. `correcciones/tanda2-l4l6-f1l4m25-licencia-alcance` — F1.L4.M25 · PR #432
  5. `correcciones/tanda2-l4l6-f1l4m03-arranque-fail-closed` — F1.L4.M03 · PR #439
  6. `correcciones/tanda2-l4l6-f1l6m12-idempotencia-app` — F1.L6.M12 · PR #442
  - Independiente, sobre `origin/dev`: `correcciones/tanda2-l4l6-osv-npm` (avisos OSV en npm) · PR #435, incorporado en 2–4.
- Peldaño por área: frontends (`packages/tokens`, `packages/ui`, `packages/diseno_flutter`) **TESTED**; identidad **TESTED** (unitarias, web y barrido verdes; mutante detectado). No VERIFIED: no se levantó el servicio con dos procesos reales.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T2.M2 (F1.S4.M1) | Referencia `interpretarMontoEscrito` en `packages/tokens/dinero/interpretar.ts` + 35 vectores en `vectores/monto-escrito.json`, en los tres niveles. «12.50», «12,50» y «12.5» → `12.50`; «1.250,50» → `1250.50`; «1.240.50», «1,250.50» y «-5» → `null`. | `yarn vitest run` (tokens) | PASS 52/52; mutante → 4 fallos. [`evidencia/t2-m2-m4-campo-monto.txt`](./evidencia/t2-m2-m4-campo-monto.txt) |
| T2.M3 | Angular `aCadenaDelContrato` delega en la referencia y reproduce los vectores | `yarn ng test --include='src/campo-monto/**'` | PASS 45/45 |
| T2.M4 | Dart `CampoMonto` y `CampoMontoGrande` usan el mismo parser. Muestran al vuelo «Vas a enviar Bs 12,50» o el error del campo «Escribí un monto como 1.240,50» | `flutter test` unidad + widget del paquete | PASS 168/168; la app con las pantallas de dinero: 13/13 |
| F1.L6.M04 | Los dos patrones rotos de `cumplimiento.yaml` (`'^-?\\d+…'` en comillas simples, que rechazaba «100.00») pasan a `'^\d+\.\d{2}$'`, sin negativos. Se reemplazan los 6 ejemplos basura del mock. Una prueba nueva barre todos los contratos. | `yarn vitest run` (simulado) | PASS 380/380; mutante → 4 fallos. [`evidencia/f1-l6-m04-patron-importe.txt`](./evidencia/f1-l6-m04-patron-importe.txt) |
| F1.L4.M25 | `GET /licencia/alcance` es una consulta pura (`readOnly`): no emite nada al outbox y no recibe `usuarioId` por query (se quitó del contrato; si alguien lo manda, se ignora). El rechazo se registra solo en `exigirHabilitado`, con el actor del token. | `./gradlew :servicios:cumplimiento:test --tests '*AlcanceSinEfectos*'` + webTest | PASS 2/2 + 87/87; mutante → 1 fallo. [`evidencia/f1-l4-m25-licencia-alcance.txt`](./evidencia/f1-l4-m25-licencia-alcance.txt) |
| F1.L6.M12 | La app genera la clave de idempotencia por intento (formulario + contenido normalizado) en recargar, retirar y transferir. Un 4xx la libera; timeout, 5xx, 408, 425 y 429 la conservan. | `flutter test test/unidad/idempotencia_por_intento_test.dart` | PASS 7/7 (los tres niveles contra un doble declarado); mutante → 1 fallo; app 919 en verde. [`evidencia/f1-l6-m12-idempotencia-app.txt`](./evidencia/f1-l6-m12-idempotencia-app.txt) |
| T2.M1 (F1.S3.M1, resto) | La vida del token de acceso es `15 min − 60 s` (tolerancia por omisión de `JwtTimestampValidator`, verificada en el jar 6.5.11). Un servidor de recurso con el validador por omisión rechaza el token al minuto 15 + 1 s. Con eso, sumado a lo de #412 (suspender revoca sesiones, el refresco se niega con `PuedeAutenticarse`), una cuenta suspendida o dada de baja pierde todo acceso en ≤ 15 min. | `./gradlew :servicios:identidad:test --tests '*VidaDelAcceso*'` | PASS 2/2; mutante (15 min) → 2 fallos. [`evidencia/t2-m1-token-vigente.txt`](./evidencia/t2-m1-token-vigente.txt) |

## A medias

### F1.S3.M1 — revocación inmediata (no la de ≤ 15 min)
- Qué anda: el tope de 15 min (T2.M1) y todo lo de #412.
- Qué no anda: dentro de esos 14 min, el token de una cuenta suspendida sigue sirviendo en los otros servicios, también para operaciones de dinero.
- Qué falta exactamente: F1.L4.M15 — en `plataforma/comun-web` (fuera de mi alcance), validar `iss`/`aud` y, en las operaciones de dinero, consultar si `sesion_id` sigue vigente (introspección contra identidad o lista de revocación por evento `identidad.sesion_revocada`). Identidad ya emite `sesion_id` en el token.
- Dónde quedó: sin cambios para esto.

### F1.L4.M03 — arranque fail-closed
- Qué anda: identidad no arranca sin `JWT_CLAVE_FIRMA`, cualquiera sea el perfil, salvo con `aportaya.entorno.descartable=true` declarado. Tampoco arranca con `INVITACIONES_CLAVE` ausente o de menos de 32 caracteres. PASS 95 + 160; mutante → 1 fallo. [`evidencia/f1-l4-m03-arranque-fail-closed.txt`](./evidencia/f1-l4-m03-arranque-fail-closed.txt)
- Qué no anda: el stack local (`despliegue/compose/servicios.yml`, generado por `scripts/generar_compose.py`) arranca identidad con `JWT_CLAVE_FIRMA:` vacía. **Con este cambio, identidad local no levanta** hasta que el compose declare el entorno descartable.
- Qué falta exactamente (archivos compartidos, fuera de mi alcance):
  1. `scripts/generar_compose.py`: en las variables locales de identidad, agregar `"APORTAYA_ENTORNO_DESCARTABLE": "true"` (solo en el perfil local, **nunca** en `--coolify`), y regenerar `despliegue/compose/servicios.yml`.
  2. `docker-compose.coolify.yml` / generador `--coolify`: `JWT_CLAVE_FIRMA: ${JWT_CLAVE_FIRMA:?}` e `INVITACIONES_CLAVE: ${INVITACIONES_CLAVE:?}`, para que el error aparezca en `docker compose config`.
  3. `despliegue/TEST.md:469`: la fila de `INVITACIONES_CLAVE` pasa a decir «no arranca».
  4. El pool alineado con `descriptor.yml` (anexo) no se tocó.
- Avance fuera de este carril: el punto 1 (compose local) lo resolvió el coordinador en **#457**, que se mergea antes que #439. El guion de servidor verifica además que identidad tenga `JWT_CLAVE_FIRMA` en TEST antes de promover. Quedan pendientes los puntos 2 y 3.
- Dónde quedó: rama `correcciones/tanda2-l4l6-f1l4m03-arranque-fail-closed`; compila y sus pruebas están en verde. TEST no se ve afectado: ya tiene cargados `JWT_CLAVE_FIRMA` e `INVITACIONES_CLAVE` (`despliegue/TEST.md:466-478`).

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| F1.L4.M06 (LFT: analista del token, decisión con otro permiso) | TODO | Toca el contrato, el formulario del backoffice y `CU44Test`/`CU44RechazosTest` (Testcontainers). El permiso nuevo `CUMPLIMIENTO_CASOS_DECIDIR` vive en `seeders/minimos/10-roles-y-permisos.json`, que es compartido |
| F1.L4.M05, M07, M08, M11, M20, M24 (cambios de esquema en `docs/entidades` de identidad/cumplimiento) | TODO | Siguiente sesión del carril; dependen de F0.L4.M04 |
| F1.L4.M04, M09, M10, M12–M14, M16–M19, M21–M23, M26 | TODO | Siguiente sesión del carril (M15, M17, M21–M23 coordinan con L1 o con `plataforma`) |
| F1.L6.M05–M11 | TODO | M07 (gateway) y M08 (SSR) tocan `plataforma`; M06 depende de F4.L5.M52; M11 es la otra mitad de M12 |
| F2.L4.M01–M02, F2.L6.M13–M14 | TODO | Después de cerrar F1 |

## Estado del CI al cerrar esta sesión
- #420: todos los jobs en verde salvo «Los cinco corredores». Falló `./gradlew contractTest` en **`servicios:inversiones`** (`AliadoSimuladoFlujosContratoTest.fondoDePuntaAPunta`: `IOException: HTTP/1.1 header parser received no bytes`, por una conexión del pool cerrada localmente). Este PR no toca ese módulo, y `identidad` pasó su `contractTest`. Clase: ajeno a este carril, probablemente intermitente (keep-alive entre el cliente de prueba y el servidor simulado). **No reproducido ni corregido**: es de otro carril y queda registrado para él. `test`, `webTest` e `integrationTest` dieron BUILD SUCCESSFUL.
- #426: el job OSV falló por avisos npm que ya estaban en `dev`. Se arregla con #435, que ya está incorporado en #426, #427 y #432.
- #427, #432, #435, #439 y #442: el CI seguía corriendo al cerrar.

## Evidencia
- [`evidencia/t2-m1-token-vigente.txt`](./evidencia/t2-m1-token-vigente.txt)
- [`evidencia/t2-m2-m4-campo-monto.txt`](./evidencia/t2-m2-m4-campo-monto.txt)
- [`evidencia/f1-l6-m04-patron-importe.txt`](./evidencia/f1-l6-m04-patron-importe.txt)
- [`evidencia/f1-l4-m25-licencia-alcance.txt`](./evidencia/f1-l4-m25-licencia-alcance.txt)
- [`evidencia/f1-l4-m03-arranque-fail-closed.txt`](./evidencia/f1-l4-m03-arranque-fail-closed.txt)
- [`evidencia/f1-l6-m12-idempotencia-app.txt`](./evidencia/f1-l6-m12-idempotencia-app.txt)
- Verificadores de bóveda: `verificar_boveda`, `verificar_seguridad`, `verificar_carriles`, `verificar_maqueta`, `verificar_criterios` → `TODO OK` / sin divergencias.

## No cubierto
- No se probó con dos servicios reales (identidad emisor + otro validando por JWKS); la prueba decodifica con `NimbusJwtDecoder` y la clave pública, que es el mismo código.
- Las pruebas de integración con Testcontainers (`BaseDePrueba`) no corren acá: **NO COMPROBADO — requiere Docker (máquina de Pablo)**. Comando: `./gradlew :servicios:identidad:integrationTest`.

- `CampoMonto`: no hubo prueba en dispositivo ni capturas en tres viewports y dos temas (regla 95.7). Lo visible que cambia es solo el texto de ayuda o de error debajo del campo, con los mismos estilos de antes. Para las capturas, ver los comandos de `test/capturas` en la máquina de Pablo.
- La suite completa de la app da 912 pruebas en verde y 6 en rojo. Esas 6 fallan igual sin este cambio: son ENVIRONMENT, por el SDK 3.50.1 local, que es más nuevo que el de CI, y los goldens de macOS. Lo que vale es el CI.

- F1.L6.M04: los dos campos (`limitesNuevos[].monto`, `limitesSandbox.montoOperacion`) **no llevan moneda al lado** (regla 91.1.2). Agregarla es un cambio de contrato con campo obligatorio nuevo: queda registrado, no se hizo en este PR. No se subió `info.version` porque los 15 contratos están en 1.0.0 y no hay práctica de versionado; el patrón viejo no aceptaba ningún valor real, así que ningún cliente válido se rompe.

- F1.L4.M25: el **contador real de usuarios del sandbox** sigue en 0. Ese número es de `identidad` y cumplimiento no lo puede leer (invariante 11), así que hace falta un dato por API o por evento. Por ahora queda como el valor más conservador, que no consume cupo. Además, `exigirHabilitado` emite el evento de rechazo y lanza en la misma transacción, así que el evento se revierte. El registro persistente de una operación rechazada le toca al servicio que la intenta (grupos, nucleo-financiero), y queda pendiente con ellos. `CU46Test` (Testcontainers): NO COMPROBADO.
- `postman/` de cumplimiento se regeneró con `generar_postman.py`. `generar_catalogo_operaciones.py` produce deriva ajena (escribe en `auditoria` y suma operaciones de otros servicios), así que no se commiteó.

- F1.L6.M12: probado contra un **doble** del servidor. Falta la integración contra el servidor real (máquina de Pablo). No se escribió el «ADR corto de cliente» que pide el anexo: la política quedó documentada en `apps/movil/lib/proveedores/idempotencia.dart`, y el ADR se escribe junto con F1.L6.M11 (backoffice), que es la otra mitad de esa política.

## Desvíos del plan
- Maven Central respondió 429 en este contenedor. Se usó un init script **local, no versionado**, que apunta al espejo oficial de Google de Maven Central. El build del repo no se tocó.

## Riesgos residuales
- Los clientes renuevan el acceso un minuto antes que antes (14 min). La app renueva por `expiraEn` de la respuesta, no por un 15 fijo (`apps/movil/.../cu04_autenticar.dart` solo lo mencionaba en un comentario, actualizado).

## Decisiones y ambigüedades
| # | Tema | Supuesto tomado | A quién confirmar |
|---|---|---|---|
| AMB-T2-2 | «1.240» (un punto seguido de tres dígitos) | Se lee como miles (`1240.00`, formato es-BO) y el campo lo muestra al vuelo antes de enviar | Pablo |
| AMB-T2-3 | Textos nuevos (P-16): «Vas a enviar Bs …» y «Escribí un monto como 1.240,50» | Los redactó Claude | Pablo los aprueba |
| AMB-T2-1 | «Un token vigente deja de servir en ≤ 15 min» | Vida + tolerancia del decodificador ≤ 15 min; la revocación inmediata es F1.L4.M15 | Pablo |
