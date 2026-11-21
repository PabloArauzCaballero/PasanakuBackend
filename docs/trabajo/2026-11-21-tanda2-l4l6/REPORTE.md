# Reporte — Tanda 2, carril L4L6: identidad, cumplimiento y frontends compartidos

> **AVANCE: 6 / 6 — 100,0 %** de las microtareas abiertas hasta ahora (T2.M1–T2.M4, F1.L6.M04, F1.L4.M25). El resto de F1/F2 de L4 y L6 sigue en TODO.

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Base: `origin/dev` 50cf3be9
- Ramas y PR (apilados, se mergean en orden):
  1. `correcciones/tanda2-l4l6-f1s3m1-token-vigente` — F1.S3.M1 (resto) · PR #420
  2. `correcciones/tanda2-l4l6-f1s4m1-campo-monto` — F1.S4.M1 / F1.L6.M03 · PR #426
  3. `correcciones/tanda2-l4l6-f1l6m04-patron-importe` — F1.L6.M04 · PR #427
  4. `correcciones/tanda2-l4l6-f1l4m25-licencia-alcance` — F1.L4.M25
- Peldaño por área: frontends (`packages/tokens`, `packages/ui`, `packages/diseno_flutter`) **TESTED**; identidad **TESTED** (unitarias, web y barrido verdes; mutante detectado). No VERIFIED: no se levantó el servicio con dos procesos reales.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T2.M2 (F1.S4.M1) | Referencia `interpretarMontoEscrito` en `packages/tokens/dinero/interpretar.ts` + 35 vectores en `vectores/monto-escrito.json`, en los tres niveles. «12.50», «12,50» y «12.5» → `12.50`; «1.250,50» → `1250.50`; «1.240.50», «1,250.50» y «-5» → `null`. | `yarn vitest run` (tokens) | PASS 52/52; mutante → 4 fallos. [`evidencia/t2-m2-m4-campo-monto.txt`](./evidencia/t2-m2-m4-campo-monto.txt) |
| T2.M3 | Angular `aCadenaDelContrato` delega en la referencia y reproduce los vectores | `yarn ng test --include='src/campo-monto/**'` | PASS 45/45 |
| T2.M4 | Dart `CampoMonto` y `CampoMontoGrande` usan el mismo parser. Muestran al vuelo «Vas a enviar Bs 12,50» o el error del campo «Escribí un monto como 1.240,50» | `flutter test` unidad + widget del paquete | PASS 168/168; la app con las pantallas de dinero: 13/13 |
| F1.L6.M04 | Los dos patrones rotos de `cumplimiento.yaml` (`'^-?\\d+…'` en comillas simples, que rechazaba «100.00») pasan a `'^\d+\.\d{2}$'`, sin negativos. Se reemplazan los 6 ejemplos basura del mock. Una prueba nueva barre todos los contratos. | `yarn vitest run` (simulado) | PASS 380/380; mutante → 4 fallos. [`evidencia/f1-l6-m04-patron-importe.txt`](./evidencia/f1-l6-m04-patron-importe.txt) |
| F1.L4.M25 | `GET /licencia/alcance` es una consulta pura (`readOnly`): no emite nada al outbox y no recibe `usuarioId` por query (se quitó del contrato; si alguien lo manda, se ignora). El rechazo se registra solo en `exigirHabilitado`, con el actor del token. | `./gradlew :servicios:cumplimiento:test --tests '*AlcanceSinEfectos*'` + webTest | PASS 2/2 + 87/87; mutante → 1 fallo. [`evidencia/f1-l4-m25-licencia-alcance.txt`](./evidencia/f1-l4-m25-licencia-alcance.txt) |
| T2.M1 (F1.S3.M1, resto) | La vida del token de acceso es `15 min − 60 s` (tolerancia por omisión de `JwtTimestampValidator`, verificada en el jar 6.5.11). Un servidor de recurso con el validador por omisión rechaza el token al minuto 15 + 1 s. Con eso, sumado a lo de #412 (suspender revoca sesiones, el refresco se niega con `PuedeAutenticarse`), una cuenta suspendida o dada de baja pierde todo acceso en ≤ 15 min. | `./gradlew :servicios:identidad:test --tests '*VidaDelAcceso*'` | PASS 2/2; mutante (15 min) → 2 fallos. [`evidencia/t2-m1-token-vigente.txt`](./evidencia/t2-m1-token-vigente.txt) |

## A medias

### F1.S3.M1 — revocación inmediata (no la de ≤ 15 min)
- Qué anda: el tope de 15 min (T2.M1) y todo lo de #412.
- Qué no anda: dentro de esos 14 min, el token de una cuenta suspendida sigue sirviendo en los otros servicios, también para operaciones de dinero.
- Qué falta exactamente: F1.L4.M15 — en `plataforma/comun-web` (fuera de mi alcance), validar `iss`/`aud` y, en las operaciones de dinero, consultar si `sesion_id` sigue vigente (introspección contra identidad o lista de revocación por evento `identidad.sesion_revocada`). Identidad ya emite `sesion_id` en el token.
- Dónde quedó: sin cambios para esto.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Resto de F1.L4, F1.L6, F2.L4, F2.L6 | TODO | Se toman en el orden del PLAN |

## Evidencia
- [`evidencia/t2-m1-token-vigente.txt`](./evidencia/t2-m1-token-vigente.txt)
- [`evidencia/t2-m2-m4-campo-monto.txt`](./evidencia/t2-m2-m4-campo-monto.txt)
- [`evidencia/f1-l6-m04-patron-importe.txt`](./evidencia/f1-l6-m04-patron-importe.txt)
- [`evidencia/f1-l4-m25-licencia-alcance.txt`](./evidencia/f1-l4-m25-licencia-alcance.txt)
- Verificadores de bóveda: `verificar_boveda`, `verificar_seguridad`, `verificar_carriles`, `verificar_maqueta`, `verificar_criterios` → `TODO OK` / sin divergencias.

## No cubierto
- No se probó con dos servicios reales (identidad emisor + otro validando por JWKS); la prueba decodifica con `NimbusJwtDecoder` y la clave pública, que es el mismo código.
- Las pruebas de integración con Testcontainers (`BaseDePrueba`) no corren acá: **NO COMPROBADO — requiere Docker (máquina de Pablo)**. Comando: `./gradlew :servicios:identidad:integrationTest`.

- `CampoMonto`: no hubo prueba en dispositivo ni capturas en tres viewports y dos temas (regla 95.7). Lo visible que cambia es solo el texto de ayuda o de error debajo del campo, con los mismos estilos de antes. Para las capturas, ver los comandos de `test/capturas` en la máquina de Pablo.
- La suite completa de la app da 912 pruebas en verde y 6 en rojo. Esas 6 fallan igual sin este cambio: son ENVIRONMENT, por el SDK 3.50.1 local, que es más nuevo que el de CI, y los goldens de macOS. Lo que vale es el CI.

- F1.L6.M04: los dos campos (`limitesNuevos[].monto`, `limitesSandbox.montoOperacion`) **no llevan moneda al lado** (regla 91.1.2). Agregarla es un cambio de contrato con campo obligatorio nuevo: queda registrado, no se hizo en este PR. No se subió `info.version` porque los 15 contratos están en 1.0.0 y no hay práctica de versionado; el patrón viejo no aceptaba ningún valor real, así que ningún cliente válido se rompe.

- F1.L4.M25: el **contador real de usuarios del sandbox** sigue en 0. Ese número es de `identidad` y cumplimiento no lo puede leer (invariante 11), así que hace falta un dato por API o por evento. Por ahora queda como el valor más conservador, que no consume cupo. Además, `exigirHabilitado` emite el evento de rechazo y lanza en la misma transacción, así que el evento se revierte. El registro persistente de una operación rechazada le toca al servicio que la intenta (grupos, nucleo-financiero), y queda pendiente con ellos. `CU46Test` (Testcontainers): NO COMPROBADO.
- `postman/` de cumplimiento se regeneró con `generar_postman.py`. `generar_catalogo_operaciones.py` produce deriva ajena (escribe en `auditoria` y suma operaciones de otros servicios), así que no se commiteó.

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
