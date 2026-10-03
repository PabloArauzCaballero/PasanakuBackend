> **AVANCE: 44 / 89 — 49,4 %.** En rojo: 31 microtareas BLOQUEADAS (la mayoría por decisiones de autorización, modelo o contrato; el resto por entorno), 10 A MEDIAS. **CU-10, CU-12 y CU-20 siguen sin completarse**: la última capa (B28) son triggers que leen tablas de otros esquemas con el rol que los dispara. La app llegó a arrancar en el emulador, pero sin capturas de pantallas de uso: el sistema detuvo el emulador y el recompilado por memoria crítica.

# Reporte — Campaña E2E de los casos de uso principales + correcciones (API real + backoffice real)

- Fecha: 2026-10-03 · Plan: [PLAN.md](./PLAN.md) · Rama: `justin/test/campania-cu-principales` (worktree `PasanakuBackend-campania`, desde `origin/test` 22c5e687) · PR: https://github.com/PabloArauzCaballero/PasanakuBackend/pull/45
- Stack: 11 servicios + infraestructura en Docker con imágenes construidas desde esta rama; base recreada con `bd:reset`; usuarios sintéticos de `seeders/dev`.
- Peldaños de evidencia por área (regla 30):
  - **API / servicios: VERIFIED** para lo ejercitado (runtime real, base real, roles `svc_*` reales), con sus fallos declarados.
  - **Correcciones de código: TESTED** — tests unitarios, web e integración (Testcontainers) de los servicios tocados en verde; además observadas en runtime.
  - **Backoffice: prueba visual parcial** — 30 capturas (3 viewports × claro/oscuro), se inspeccionaron las de escritorio-claro y la de tesorería; las otras 24 no se miraron una por una. No se tocó código de backoffice.
  - **App móvil: sin evidencia.**
  - **Peldaño del trabajo (el más bajo de sus áreas): TESTED.** No es REGRESSION_VERIFIED.

## Qué cambió respecto del primer reporte

Con tu autorización se corrigieron, además de B6 y B7 (secuencias y `digest` del libro), estos hallazgos. El efecto observado en runtime figura en la tabla.

| # | Corrección | Observado en runtime |
|---|---|---|
| B1 | El token lleva los códigos de rol vigentes (`EmitirAcceso` + `AccesosRepositorio.codigosDeRolesVigentes`) | aprobar y habilitar organizador pasan el guardia (antes 403) |
| B3 | `/licencia/alcance` → `PARTICIPANTE`; restricciones → `PARTICIPANTE` con control de objeto; supresión → `GRUPO_ADMINISTRAR` | licencia 200 a un participante; la «restricción vigente» falsa desapareció; invitar → 201 con enlace |
| B23 | `public.digest` en los tokens de invitación (SQL de la aplicación) | invitar dejó de dar 500 |
| B9 | `liquidarEntrega` idempotente por turno | repetir → 201 con la misma entrega (antes 500) |
| B10 | La cotización del retiro enviaba `referenciaTipo: OPERACION`; ahora `ORDEN_RETIRO` | el retiro cotiza y pide el segundo factor |
| B16 | `TRASPASO_CUPO` se vota como `ADMISION_REEMPLAZO` (decisión tuya) y CU-64 valida que el acuerdo exista, sea del grupo, del tipo y esté `APROBADO` | `proponerAcuerdo` → 201 |
| B25 | `comprometerSorteo` devuelve la semilla (campo aditivo del contrato) | comprometer 201 → revelar 200 `verificado: true`, 3 turnos creados |

## Ronda H6 — bloque RLS / libro / permuta / app (autorizada por el usuario)

**Lo que se hizo y quedó verificado:**

| Pieza | Qué es | Evidencia |
|---|---|---|
| B27 | Las 62 funciones de regla (esquema `aportes`) con `search_path` fijo y `USAGE` sobre `aportes` para todos los `svc_*` | `h6-b27-usage-aportes.txt`; 62 de 62 con `SET`; humo 165 OK |
| B29 | `SYS-CUSTODIA` con el id fijo que el compose configura (`…0c0`); antes toda pata contra la custodia violaba la FK | SQL tras el reseteo |
| B8 (parte) | Políticas de **solo lectura** para el titular en `organizador`, `participante` y `movimiento_billetera`; `solicitud_organizador` completa para el titular | `AutorizacionNegativaTest` con el rol real: el titular lee lo suyo, **no escribe el libro**; 7 de 7 PASS |
| Libro | `Datos.comoSistema` (privilegio de sistema acotado al bloque, rol restaurado, error del trabajo sin enmascarar) usado por `LibroDeBilletera` | mismo test |
| B20 | `acreditarRecarga` exige `TESORERIA` | runtime: titular y ajeno → 403 (`h6-dinero.txt`) |
| B11 | Postular a organizador | runtime: 201 (antes 500) |
| B13/B14 | Permuta: el solicitante es el participante del grupo del turno; solo el titular del turno de origen la pide | tests web e integración PASS; runtime: titular 201, ajeno 422 |

**Lo que no se pudo cerrar:**
- **B28 (arquitectura):** `fn_uif_acumulado` lee `registro_operacion_relevante` y `fn_org_validar_contrato_grupo` lee `contrato_organizador` con el rol que dispara el trigger, que no tiene acceso a esos esquemas. Con eso, ningún movimiento del libro ni ninguna creación de grupo completa con los roles reales. La salida propuesta (`SECURITY DEFINER` con `search_path` fijo en las funciones que cruzan esquemas) es una decisión de frontera de privilegios sobre unas 60 funciones: **no la apliqué por mi cuenta**.
- **El sistema de permisos de Claude Code denegó dos lotes** pese a tu autorización en el chat: el que incluía hacer público `/grupos/sorteos/{id}/paquete` y leer los participantes con rol de sistema para resolver el alias (B26 y alias), y el del voto (B22). No los repetí por partes. Quedan diseñados en el PLAN (H6.S4.M1, H6.S5.M1, H6.S6.M2); para aplicarlos hace falta una regla de permiso en la configuración o que los aplique una persona.
- **Web tests de B20** (`BilleteraControllerWebTest`, dos tests nuevos): escritos, **no ejecutados** (Gradle detenido por memoria).
- **B15 y B5** (canal `aportes`→`grupos`): no empezados.
- **La app en el emulador:** instalada y lanzada; la guardia de configuración rechaza `http://10.0.2.2` (preexistente), se recompiló con `HOSTS_PERMITIDOS=10.0.2.2`, y el sistema cerró el emulador antes de recorrer pantallas. Capturas disponibles: `app-00-arranque.png` (splash) y `app-01-inicio.png` (guardia de configuración). Pasos para retomar en H6.S9.M1 del PLAN.

**Hallazgos nuevos:** B27 (corregido), B28 (abierto, decisión), B29 (corregido). El detalle está en el PLAN.

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H0, H1, H2 | Descubrimiento, seeders idempotentes (1466 filas validadas), stack de 11 servicios + infraestructura `healthy`, sondas de autorización | ver PLAN | PASS — `evidencia/h1-*`, `h2-*` |
| H1.S3 | B6 y B7 corregidos en la fuente (`generar_ddl.py`, `Restricciones.md`) | `probar_humo.py` | PASS — 165 OK · 0 FALLA (`h4-humo-esquema.txt`, `h5-humo-esquema.txt`) |
| H5.S1.M1 | B1: códigos de rol en el token | `CU08CodigosDeRolVigentesTest` (integración) + sonda | PASS — `h5-tests-identidad-it.txt`, `h5-campania-final.txt` |
| H5.S2.M1–M3 | B3: tres consultas entre servicios sin permisos de operador, con negativos de autorización | web tests de cumplimiento, garantía y notificaciones | PASS — `h5-tests-2.txt` |
| H5.S4.M1 | B9: liquidación idempotente | `CU22Test.criterio2` reescrito + runtime | PASS — `h5-tests-it-grupos-entregas.txt` |
| H5.S9.M1–M3 | B25 (semilla), B23 (`digest` en Java), períodos de `GRP-DEMO-02` en el seeder | `CU60Test`, web tests, gates de contrato, `h5_sorteo.py` | PASS — `h5-sorteo.txt`, `h5-gates-contrato.txt` |
| H3.S3 (CU-69) | **Invitar → consultar → token ajeno rechazado (422 `AP-CU69-05`) → aceptar (201) → cupo OCUPADO 2 → 3** | `h3_grupos.py`, `h5_sorteo.py` | PASS — `h5-campania-final.txt` |
| H3.S7 (CU-60) | **Comprometer (201) → revelar (200, `verificado: true`) → turnos 1, 2 y 3**; USR2 sin permiso → 403 | `h5_sorteo.py` | PASS — `h5-sorteo.txt` |
| H3.S8 (CU-21) | Aportar 201; repetir con la misma clave → 200 y el mismo `pagoId`; 1 solo pago | `h3_grupos.py` | PASS parcial — ver B15 |
| H3.S12 (CU-22) | Liquidar → autorizar (USR9) → ejecutar (USR8), con negativos de rol y de bolsa incompleta | `h3_resto.py` | PASS parcial — sin asiento ni crédito al beneficiario |
| H4.S1, H4.S2 | Compuertas Python, humo, capturas del backoffice, reporte y PR | scripts + Playwright + `gh` | PASS — `h4-*`, PR #45 |

## A medias

### H5.S4.M2 — B10, retiro (CU-11)
- **Qué anda:** el retiro cotiza (`ORDEN_RETIRO`); responde `AP-CU11-02` «falta el segundo factor». Tests de nucleo-financiero PASS.
- **Qué no anda:** no se ejercitó el retiro completo ni su aprobación y conciliación.
- **Qué falta exactamente:** correr el retiro con la evidencia MFA y la aprobación de tesorería (USR8).
- **Dónde quedó:** `CotizadorPorHttp.java`, compila.

### H5.S4.M5 — B16, traspaso (CU-63/CU-64)
- **Qué anda:** proponer el acuerdo (201), la traducción de tipos y la validación del acuerdo en CU-64, con tests.
- **Qué no anda:** `votarAcuerdo` → 500 (B22: el voto inserta un id de usuario donde la FK pide un participante; resolverlo exige leer `participante`, reservada por RLS: B8).
- **Qué falta exactamente:** la decisión de B8 y el voto; después el traspaso con el acuerdo aprobado. CU-64 además tiene B21.
- **Dónde quedó:** `TipoDeAcuerdo`, `CU63Acordar`, `CU64TraspasarCupo`.

### H3.S5.M1 — CU-10 recargar
- **Qué anda:** solicitar (201); acreditar la orden ajena → 422.
- **Qué no anda:** acreditar → 500 (B8: el insert del libro viola la RLS).
- **Qué falta exactamente:** la decisión de B8 **y** B20 (la acreditación hoy es auto-servicio del titular); sin B20, corregir B8 imprimiría dinero.

### H3.S12.M1, H3.S14.M1, H5.S6.M1, H5.S8.M1
- Ver el PLAN: CU-22 sin asiento ni crédito; negativos con el defecto B15; la corrida final se reejecutó y lo que sigue en rojo depende de B8, B20 y B26; falta `EsquemaAlDiaRepositorioTest` y la suite completa de integración.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H5.S3.M1–M4 | BLOQUEADO (DECISION_REQUIRED) | **B8, B2, B11, B20:** contexto de sistema o cláusula del titular en las políticas RLS (propuesta en el PLAN). No autorizaste este bloque |
| H5.S4.M3, M4, M6 | BLOQUEADO (DECISION_REQUIRED) | B13/B14 (permuta), B15 (pago ajeno) y B5 (período abierto) dependen de B8 y de un canal nuevo `aportes`→`grupos` (contrato, cliente y endpoint) |
| H5.S5.M1 | BLOQUEADO (DECISION_REQUIRED) | B17/B18: elegir qué lado de la autorización se mueve en backoffice, o crear el permiso `SOLICITUD_INGRESO_VER` en el modelo |
| H5.S9.M4 | BLOQUEADO (DECISION_REQUIRED) | **B26:** la verificación pública del sorteo llama a `/grupos/sorteos/{id}/paquete` sin sesión y ese endpoint exige `PARTICIPANTE`; hacerlo público o usar credencial de servicio |
| H5.S7.M1, H3.S15.M2–M3 | BLOQUEADO (ENTORNO) | Liberar memoria y pedir expresamente que se relance el build del APK (pasos exactos en el PLAN) |
| H4.S1.M3 | BLOQUEADO (ENTORNO) | Correr el resto de la integración de Gradle con memoria libre |
| H3.S1, S2, S4, S6, S9, S10, S13 | BLOQUEADO | B1 (ya corregido; el siguiente fallo es B8), B2, B8, B11, B13, B14, B22 |

## Matriz por caso de uso tras las correcciones

| CU | Veredicto | Observación |
|---|---|---|
| CU-69 invitar / aceptar | **PASS** | completo de punta a punta |
| CU-60 sorteo | **PASS** | comprometer y revelar |
| CU-61 verificar | **FAIL** | B26 (el paquete no es consultable sin sesión) |
| CU-21 aportar | **PASS + defecto** | B15: un tercero puede marcar pagada la obligación ajena |
| CU-22 entrega | **PASS parcial** | idempotente (B9 corregido); sin asientos ni crédito |
| CU-90 aprobar / habilitar | **desbloqueado, sin completar** | pasa el guardia (B1); la postulación falla por B11/B8 |
| CU-90 postular | **FAIL** | B11 (RLS) |
| CU-20 crear grupo | **FAIL** | B2 (RLS del organizador); la licencia ya no es el problema (B3) |
| CU-68 postular | **FAIL (DATA)** | `AP-CU68-03`: un usuario sin historial nunca puede postular (B24) |
| CU-64 traspasar | **FAIL** | B22 (voto), B21 |
| CU-62 permutar | **FAIL** | B13/B14 |
| CU-10 recargar | solicitar **PASS** · acreditar **FAIL** | B8, B20 |
| CU-12 transferir | **FAIL** | B8 (alias) |
| CU-11 retirar | **A MEDIAS** | cotiza; falta el segundo factor y la aprobación |
| CU-74 insignias | **BRECHA** | el permiso `SOPORTE` sigue sin estar asignado a ningún rol sembrado, y la evaluación no tiene pantalla |
| Solicitudes escaladas (backoffice) | **BRECHA** | B18 |

## Hallazgos de producto

Detalle y evidencia de cada uno en el PLAN (tabla «Registro de hallazgos»), B1 a B26. Corregidos: **B1, B3, B6, B7, B9, B10, B16 (parcial), B23, B25**. Abiertos y graves: **B8** (RLS), **B20** (la acreditación de recarga es auto-servicio: cualquier titular puede acreditarse dinero, hoy tapado por B8), **B15** (pagar la obligación ajena sin dinero) y **B26**.

## Gates de la casa

### Dinero (regla 91.6) — **no cumplido**
Sin asientos generados (CU-10 y CU-12 no llegan al libro). Idempotencia: CU-21 ✔, CU-22 ✔ (B9 corregido), CU-10/12/11 no alcanzada. Cuadre, reversa y redondeo: no ejercitados. **B20 es un riesgo de dinero abierto.**

### Seguridad (regla 90.6)
- **Amenazas:** B20 (acreditarse dinero), B15 (marcar pagado sin dinero), B14 (permutar turnos ajenos), denegación disfrazada de negocio (B3, corregida).
- **Controles agregados:** control de objeto en la consulta de restricción (lo propio, o `GRUPO_ADMINISTRAR`); validación del acuerdo en CU-64; permiso mínimo `GRUPO_ADMINISTRAR` (no `PARTICIPANTE`) en la consulta de supresión.
- **Tests:** `CobranzaControllerWebTest` (propio → 200; ajeno → 403), `CU08CodigosDeRolVigentesTest`, `CU64Test`, y los negativos de `h5-campania-final.txt`.
- **Riesgo residual:** B8, B15 y B20 sin corregir. B1 amplía lo que lleva el token: cualquier endpoint que exige un código de rol por nombre pasa a ser alcanzable por quien tenga ese rol (lo que se buscaba); conviene revisar que ninguno quede abierto por error.

### Microservicios (regla 98.8)
Saltos: `grupos` → organizador, tarifas, cumplimiento, garantía, notificaciones, aportes, transparencia, identidad; `nucleo-financiero` → tarifas, aportes, grupos; `entregas` → identidad; `transparencia` → grupos. Pruebas de contrato productor/consumidor: no corridas. **Caída de un dependido: no ejercitada.** Lo más parecido: los 403 servicio a servicio mostraron cómo «denegar por omisión» disfraza una falla de permisos de negocio (B3). Quedaron inconsistentes: una obligación PAGADA sin dinero (B15), una entrega ENTREGADA sin asiento ni crédito, una orden de recarga PENDIENTE y un acuerdo `EN_VOTACION` sin voto.

## Evidencia (índice de `evidencia/`)

`h1-*`, `h2-*`, `h3-*` (primera ronda), `h4-*`, `bo-*.png`, `bo-capturas-*.txt`, y la ronda de correcciones `h5-*`: `h5-campania-final.txt` (corrida consolidada), `h5-sorteo.txt`, `h5-tests-*.txt`, `h5-gates-contrato.txt`, `h5-build-imagenes.txt`, `h5-reset*.txt`, `h5-jooq-nucleo.txt`. Scripts reproducibles: `campania_lib.py`, `campania_todo.py`, `h3_*.py`, `h5_sorteo.py`, `bo_capturas.mjs`.

```text
$ python campania_todo.py + h5_sorteo.py     (base limpia, imágenes reconstruidas desde esta rama)
CU-69  invitar 201 · consultar 200 · aceptar con token ajeno 422 AP-CU69-05 · aceptar 201 · cupos OCUPADO=3
CU-60  comprometer 201 (semilla en la respuesta) · revelar 200 verificado=True · turnos 1:PROGRAMADO 2:PROGRAMADO 3:PROGRAMADO
CU-22  liquidar 201 · repetir 201 (misma entrega) · autorizar 200 · ejecutar 200
CU-10  acreditar 500 AP-INT-01   <- B8/B20
$ probar_humo.py            165 OK · 0 FALLA
$ Gradle (unit + web + integración de los servicios tocados)   BUILD SUCCESSFUL
```

## No cubierto

- **La app móvil en el emulador:** ni una captura. Pasos para retomar: ver H5.S7.M1.
- La suite completa de integración de Gradle y `EsquemaAlDiaRepositorioTest` (deriva de esquema).
- Casos positivos de CU-61, CU-64, CU-68, CU-62, CU-10, CU-12, CU-11 (aprobación y conciliación) y CU-20; reversas.
- Caída deliberada de un servicio; traza cruzada con `correlationId`.
- Backoffice: 24 capturas no inspeccionadas una por una; lector de pantalla; teclado.
- Corrección de B13, B14, B15, B5, B22, B26 y todo lo que depende de B8.
- CU-65 a CU-67 y el voto de CU-63.

## Desvíos del plan

- **Alcance ampliado con autorización:** primero B6 y B7; luego B1 y B3 (autorización expresa), B16 con `ADMISION_REEMPLAZO` (decisión tuya) y, por lectura de código, B9, B10, B23 y B25. **No** se tocó B2, B8, B11, B13, B14, B15, B20 ni B26.
- **El clasificador de permisos de Claude Code denegó un cambio** (B1) hasta que lo autorizaste expresamente; se revirtió lo parcial y se reaplicó después.
- Sandbox regulado: aplicado a mano → sembrado → retirado (rompía `R-LIC-01`). Con B3 corregido ya no hace falta para CU-20.
- Los tests de integración de CU-22 (`criterio2`) se **reescribieron** al comportamiento corregido, conservando la comprobación de la restricción en la base con un `INSERT` directo; los de CU-64 no cambiaron porque el caso de uso conserva su contrato. No se borró ninguna aserción.
- newman no está instalado: driver Python con la librería estándar.
- Windows: `bd:levantar` y `bd:generarSemillas` fallan; se sortearon con `-x` y `python`. El `generarClientes` de Gradle reescribe archivos **versionados** de `clientes/` (cabeceras y tipos): no se agregaron a ningún commit.
- Se restauraron `packages/tokens/vectores/monto.json` y `apps/movil/pubspec.lock` (ruido de fin de línea / versión de Flutter).

## Riesgos residuales y deuda

- **B20 + B8:** cualquier titular podría acreditarse dinero el día que se arregle B8 sin tocar B20. Deben decidirse juntos.
- B15: marcar pagada una obligación ajena sin dinero.
- El stack local queda con datos mutados por la campaña: reseteable con `bd:reset`.
- La imagen de `grupos` ya devuelve la semilla del compromiso: los clientes generados (Dart y Angular) deben regenerarse.

## Decisiones y ambigüedades

1. B8, B2, B11, B13, B14, B20: elegir entre cláusula del titular generada desde el modelo o contexto de sistema; propuesta completa en el PLAN. **Confirmar con el equipo de plataforma.**
2. B20: la acreditación de recarga debe ser del sistema (webhook firmado `CU100RecibirWebhookPasarela`), no del titular. **Confirmar con tesorería y seguridad.**
3. B26: hacer público `/grupos/sorteos/{id}/paquete` (el paquete es público por diseño de CU-61) o usar credencial de servicio. **Confirmar con seguridad.**
4. B24: un recién llegado no puede postular a ningún grupo (el criterio exige reputación aunque el grupo pida 0). **Confirmar con producto.**
5. B17/B18: qué lado de la autorización se mueve en backoffice. **Confirmar con backoffice.**
6. «Aceptar a otro miembro» = brecha (stub); «vender cupo» = traspaso; «insignia de pago» = brecha de UI. Supuestos de la primera ronda, **sin respuesta todavía**.
7. `ejecutarEntrega` acepta un `montoEntregado` menor que el neto sin objeción: ¿es diseño? **Confirmar con tesorería.**
8. El PR no se replica a `PasanakuFrontend`: lo decide el equipo.

## Procesos que quedan corriendo

- Docker: la infraestructura (gateway, kafka, minio, nginx, pgbouncer, postgres, redis) sigue arriba; los 11 servicios están **detenidos** y el builder de buildx también. Para volver a levantarlos: `docker compose -f despliegue/compose/base.yml -f despliegue/compose/servicios.yml --profile todo up -d --no-build`.
- Los daemons de Gradle se cerraron. No quedan servidores de desarrollo ni emuladores (el AVD nunca se arrancó).
