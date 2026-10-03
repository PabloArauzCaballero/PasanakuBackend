> **AVANCE: 30 / 54 — 55,6 %.** En rojo: 16 microtareas BLOQUEADAS (13 por defectos de producto o de datos, 3 por entorno) y 3 A MEDIAS. De los 13 casos de uso probados, **solo CU-21 pasa de punta a punta**; CU-22 pasa a medias; ningún flujo de dinero de billetera (CU-10 acreditar, CU-12, CU-11) se completa. Sin evidencia de la app en el emulador.

# Reporte — Campaña E2E de los casos de uso principales (API real + backoffice real)

- Fecha: 2026-10-03 · Plan: [PLAN.md](./PLAN.md) · Rama: `justin/test/campania-cu-principales` (worktree `PasanakuBackend-campania`, desde `origin/test` 22c5e687).
- Stack: 11 servicios + Postgres/PgBouncer/Redis/Kafka/MinIO/Gateway/Nginx en Docker, imágenes construidas desde esta rama; base recreada con `bd:reset`; usuarios sintéticos de `seeders/dev`.
- Peldaños de evidencia por área (regla 30):
  - **API / servicios (runtime real, base real, roles `svc_*` reales): VERIFIED** para lo ejercitado, con sus fallos declarados (matriz abajo).
  - **Esquema (correcciones B6 y B7): TESTED** (`probar_humo.py`: 165 OK · 0 FALLA) y observado en runtime: tras la corrección el error de secuencia y el de `digest` desaparecen; el siguiente fallo es B8, ajeno a lo corregido. No se corrieron los tests de integración de Gradle (ENTORNO).
  - **Backoffice: VERIFIED_FUNCTIONAL_ONLY → con prueba visual parcial.** 30 capturas (3 viewports × claro/oscuro) y consola/red registradas; se **inspeccionaron individualmente** las de escritorio-claro de las 4 pantallas y la de tesorería; las otras 24 no se miraron una por una.
  - **App móvil: sin evidencia** (build del APK detenido por memoria; H3.S15 BLOQUEADO).
  - **Peldaño del trabajo (el más bajo de sus áreas): TESTED.** No es REGRESSION_VERIFIED.

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H0.S1.M1–M6 | Precondiciones, actores y destrabes de cada CU descubiertos y citados | `grep` dirigidos (ver PLAN) | DISCOVERED — hallazgos 1–11 del PLAN |
| H1.S1.M3–M4 | Seeder dev idempotente (11/2/9/7/19 filas en la 1.ª y 2.ª corrida); claves de dev y dispositivo confiado | `bd:dev` dos veces + SQL | PASS — `evidencia/h1-idempotencia.txt`, `h1-s1-m4-*.txt` |
| H1.S2.M2–M3, M6 | `GRP-DEMO-02` (3 cupos, 2 libres) y 1 obligación PENDIENTE sembrados con un solo reset | `generar_semillas.py` + `bd:reset` + SQL | PASS — `evidencia/h1-s2-m6-reset.txt` (1463 filas validadas contra el modelo) |
| H1.S3.M1–M2, M4 | **B6 y B7 corregidos en la fuente:** `USAGE` sobre secuencias para los `svc_*` y `public.digest()` en los triggers de cadena | `generar_ddl.py`, `extraer_sql.py`, SQL de privilegios | PASS — `h1-s3-m1-diff.txt`, `h1-s3-m2-privilegios.txt` (14 true / 0 false; aislamiento entre servicios intacto) |
| H2.S1.M1–M7 | Compose y gateway regenerados; 11 imágenes construidas en serie; 18 contenedores `healthy`; sondas de autorización | `docker ps`, scripts | PASS — `h2-s1-m2-*.txt`, `h2-s1-m4-docker-ps*.txt`, `h2-s1-m6-sondas.txt` |
| H3.S8.M1 | **CU-21 aportar:** 201, repetición con la misma clave → 200 con el mismo `pagoId`, 1 solo `pago`, billetera sin débito (brecha declarada) | `h3_grupos.py` | PASS — `h3-grupos-primera-pasada.txt` (¡pero ver B15!) |
| H3.S11.M1 | **CU-74:** el resultado real (403 por SOPORTE) queda registrado | `h3_grupos.py` | PASS como registro; veredicto BRECHA — `h3-campania-final.txt` |
| H4.S1.M1–M2, M4 | Compuertas Python en 0; humo del esquema sin fallas; capturas del backoffice | scripts + Playwright | PASS — `h4-gates-python.txt`, `h4-humo-esquema.txt`, `bo-*.png` |

## A medias

### H3.S5.M1 — CU-10 recargar
- **Qué anda:** `solicitarRecarga` → 201 (orden PENDIENTE, `acreditara 200.00`); acreditar una orden ajena → 422 `AP-CU10-05`.
- **Qué no anda:** `…/acreditacion` → 500 (B8: el insert en `movimiento_billetera` viola la RLS con el contexto del participante). No hay saldo, mayor ni idempotencia que demostrar.
- **Qué falta exactamente:** decidir qué contexto acredita (webhook de sistema) y cambiarlo en nucleo-financiero; reejecutar `h3_dinero.py`.
- **Dónde quedó:** código de servicios sin tocar; solo DDL/Restricciones corregidos (B6/B7).

### H3.S12.M1 — CU-22 entrega del fondo
- **Qué anda:** liquidar (USR8, 201, PROGRAMADA) → autorizar (USR9, 200, AUTORIZADA) → ejecutar (USR8, 200, ENTREGADA); USR9 liquida → 403; USR8 autoriza → 403; bruto > recaudado → 422 `AP-CU22-01`.
- **Qué no anda:** repetir `liquidar` con la misma clave → 500 (B9; la base impidió la entrega doble con `uq_entrega_turno`); no se genera asiento ni se acredita al beneficiario (saldo 600.00 intacto); `ejecutar` aceptó `montoEntregado` 2989.50 con neto 3000.00 sin objeción (a confirmar si es diseño).
- **Qué falta exactamente:** idempotencia por clave y asiento de la entrega (91.6).
- **Dónde quedó:** sin cambios de código.

### H3.S14.M1 — Negativos de autorización e idempotencia
- **Qué anda:** 403 de permiso en CU-60, CU-64, CU-22, CU-74; 422 de objeto ajeno en CU-10; repetición idempotente OK en CU-21.
- **Qué no anda:** pago de un tercero en CU-21 → 201 (B15); CU-22 repetición → 500 (B9); CU-10/12/11 no llegan a la repetición.
- **Qué falta exactamente:** corregir B15 y B9 y reejecutar.
- **Dónde quedó:** `evidencia/h3-campania-final.txt`.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S3.M3 | BLOQUEADO | B8: acreditar con el rol real exige cambiar el contexto en nucleo-financiero (dueño: nucleo-financiero) |
| H3.S1.M1, M2 | BLOQUEADO | B11 (RLS al postular) y B1 (rol del token): identidad/organizador |
| H3.S2.M1 | BLOQUEADO | B2 (RLS de `organizador`) y B3 (`/licencia/alcance` exige SOPORTE) |
| H3.S3.M1, M2 | BLOQUEADO | B3: `/notificaciones/supresion` exige SOPORTE y suprime la invitación |
| H3.S4.M1 | BLOQUEADO | B3: `garantia /cobranza/restricciones/vigentes` exige GRUPO_ADMINISTRAR → «restricción vigente» falsa; además aceptar al miembro es BRECHA (stub) |
| H3.S6.M1 | BLOQUEADO | B8: el alias no resuelve (RLS de `participante`) y el libro daría el mismo 500 |
| H3.S7.M1, M2 | BLOQUEADO | H3.S3 (no se puede conformar el grupo) y B12 (sorteo sembrado sin semilla) |
| H3.S9.M1 | BLOQUEADO | B16: `proponerAcuerdo` con `TRASPASO_CUPO` → 500 |
| H3.S10.M1 | BLOQUEADO | B13/B14: FK de `solicitante_id` y falta de validación de titularidad |
| H3.S13.M1 | BLOQUEADO | B10: `tarifas` rechaza la cotización del retiro |
| H3.S15.M2, M3 | BLOQUEADO (ENTORNO) | Liberar memoria y relanzar el build del APK; después el AVD `pasanaku` |
| H4.S1.M3 | BLOQUEADO (ENTORNO) | Correr los tests de integración de Gradle con memoria libre |
| H4.S2.M2 | TODO | Empujar la rama y abrir el PR a `test` (sin merge) |
| H1.S1.M2, H1.S2.M1, H1.S2.M4–M5 | DESCARTADO | SOPORTE por datos no sirve (B1); sandbox retirado (rompía `R-LIC-01`); MFA no hacía falta; instrumento de USR90 innecesario |

## Matriz por caso de uso (canal × veredicto)

| CU | Canal probado | Veredicto | Hallazgos |
|---|---|---|---|
| CU-90 postular | API | **FAIL** (500) | B11, B8 |
| CU-90 aprobar/habilitar | API + backoffice (pantalla real) | **FAIL** (403; la pantalla muestra «No tenés acceso a esto») | B1 |
| CU-20 crear grupo | API | **FAIL** (`AP-CU20-02`, y detrás `AP-CU20-04`) | B2, B3 |
| CU-69 invitar / aceptar | API | **FAIL** (201 con `enlace: null`; aceptar imposible) | B3 |
| CU-68 postular / aceptar | API | **FAIL** (restricción falsa); aceptar = **BRECHA** (stub) | B3 |
| CU-10 recargar | API | solicitar **PASS** · acreditar **FAIL** | B8 |
| CU-10 saldo en backoffice | backoffice | **FAIL** (403) | B17 |
| CU-12 transferir | API | **FAIL** («Ese destino no existe») | B8 |
| CU-60 sorteo | API | negativos **PASS**; positivo no alcanzable | B3 |
| CU-61 verificar | API pública | **DATA** (`verifica:false`, sin semilla sembrada) | B12 |
| CU-21 aportar | API | **PASS** (+ defecto de autorización) | B15 |
| CU-64 traspasar | API | **FAIL** (el voto previo da 500) | B16 |
| CU-62 permutar | API | **FAIL** (500); aceptar = **BRECHA** | B13, B14 |
| CU-74 insignias | API | **BRECHA** (403) | B1 |
| CU-22 entrega | API | **PASS parcial** (3 pasos) · repetición **FAIL** | B9 |
| CU-11 retirar | API | **FAIL** (no cotiza) | B10 |
| Solicitudes escaladas | backoffice | **BRECHA** (permiso inexistente, endpoint inexistente) | B18 |

## Hallazgos de producto (resumen; detalle y evidencia en el PLAN)

| # | Clase | Qué | Corregido aquí |
|---|---|---|---|
| B1 | PRODUCT_BUG | El `rol` del token es solo `PARTICIPANTE`/`BACKOFFICE`; `ADMIN_PLATAFORMA` y `SOPORTE` son inalcanzables (403 `AP-SEG-01`) | no |
| B2 | PRODUCT_BUG | `GET /organizadores/{id}/habilitacion` da `INEXISTENTE` a un organizador HABILITADO (RLS) | no |
| B3 | PRODUCT_BUG | `grupos` llama a otros servicios con el token del participante y el destino exige un permiso que no tiene; «denegar por omisión» lo disfraza | no |
| B4 | DATA | El `UPDATE` documentado de la licencia afecta 0 filas (`LICENCIA_FUNCIONAMIENTO` vs `CERTIFICADO_ADECUACION`) | no |
| B5 | PRODUCT_BUG | `cobrarAporte` fija `periodoAbierto=true` | no |
| **B6** | PRODUCT_BUG | **Ningún `svc_*` podía usar las secuencias del libro y de la bitácora → 500 en todo movimiento** | **sí** |
| **B7** | PRODUCT_BUG | **`digest()` sin calificar en los triggers de cadena (`search_path` sin `public`)** | **sí** |
| B8 | PRODUCT_BUG (diseño) | 55 políticas RLS son «solo privilegiado o sistema»; varios caminos corren con contexto de usuario sobre ellas | no |
| B9 | PRODUCT_BUG | `POST /entregas` repetido con la misma clave → 500 | no |
| B10 | PRODUCT_BUG | Contrato nucleo-financiero↔tarifas: la cotización del retiro no se puede leer | no |
| B11 | PRODUCT_BUG | Postular a organizador viola la RLS de `solicitud_organizador` | no |
| B12 | DATA | Sorteo sembrado sin semilla ni entropías | no |
| B13 | PRODUCT_BUG | `solicitarPermuta` viola la FK de `solicitante_id` | no |
| B14 | PRODUCT_BUG | `CU62Permutar.solicitar` no valida que el solicitante sea titular del turno | no |
| B15 | PRODUCT_BUG | **Cualquier participante puede marcar PAGADA la obligación de otro, sin dinero** | no |
| B16 | PRODUCT_BUG | `TipoDeAcuerdo` no tiene `TRASPASO_CUPO` que el contrato lista → 500 | no |
| B17 | PRODUCT_BUG | Backoffice «Billetera del titular»: el permiso de la ruta no coincide con el del endpoint | no |
| B18 | PRODUCT_BUG | «Solicitudes escaladas»: permiso inexistente y endpoint inexistente | no |
| B19 | UI menor | Pestañas «EnRevision» sin espacio | no |

## Gates de la casa

### Dinero (regla 91.6) — **no cumplido**
1. Asientos generados: **ninguno** (CU-10 y CU-12 no llegan al libro; CU-22 y CU-21 no los generan).
2. Idempotencia ejecutada: CU-21 ✔ (mismo `pagoId`, 1 pago); CU-22 ✘ (500, B9); CU-10, CU-12, CU-11 no alcanzada.
3. Cuadre contra el mayor: no ejercitado.
4. Reversa: no ejercitada. 5. Redondeo: no ejercitado.
Peldaño del dinero: **TESTED/VERIFIED a medias**, no VERIFIED.

### Seguridad (regla 90.6)
- **Amenaza** B15 (pagar/marcar pagada la obligación ajena sin dinero), B14 (permutar turnos ajenos), B1/B3 (denegación disfrazada). **Control** esperado: validar titularidad del objeto (90.1.3) y verificar el pago con la pasarela (91.3). **Test que lo demuestra:** negativo de autorización en `h3-campania-final.txt`. **Riesgo residual:** abierto, sin corregir.

### Microservicios (regla 98.8)
1. **Saltos:** `grupos` → organizador, tarifas, cumplimiento, garantía, notificaciones, aportes; `nucleo-financiero` → tarifas, aportes, grupos; `entregas` → identidad.
2. Pruebas de contrato productor/consumidor: **no corridas** (ENTORNO). Los desajustes B10 y B16 se vieron en runtime.
3. Prueba de duplicado de mensaje: no aplica (no hay consumidores de eventos; verificado en la exploración).
4. **Caída de un dependido ejercitada: no.** Lo más parecido: los 403 de servicio a servicio mostraron el «denegar por omisión» (B3).
5. Traza con `correlationId`: los logs llevan `trazaId`; no se reconstruyó una traza cruzada.
6. Qué queda inconsistente: una obligación PAGADA sin dinero (B15), una entrega ENTREGADA sin asiento ni crédito al beneficiario, una orden de recarga PENDIENTE.

## Evidencia (índice de `evidencia/`)

`h1-*` (licencia local, idempotencia, claves, privilegios, diff de la corrección), `h2-*` (build, `docker ps`, sondas), `h3-*` (CU-20, dinero, CU-12, grupos, permuta, traspaso, causas raíz de B2/B3/B8/B9/B10/B11/B13, corrida consolidada `h3-campania-final.txt`), `h4-*` (compuertas, humo), `bo-*.png` (30 capturas del backoffice) y `bo-capturas-*.txt`, y los scripts reproducibles `campania_lib.py`, `campania_todo.py`, `h3_*.py`, `bo_capturas.mjs`.

```text
$ python scripts/probar_humo.py                      (con las correcciones B6 y B7)
165 OK · 0 FALLA · 1 rechazos del motor (esperados en los casos negativos)

$ SQL tras la corrección (h1-s3-m2-privilegios.txt)
svc_nucleo_financiero → transaccion_billetera_secuencia_seq = true
14 true / 0 false  (svc_* sobre comun.bitacora_evento_secuencia_seq)
svc_aportes → nucleo_financiero.transaccion_billetera_secuencia_seq = false   (aislamiento intacto)

$ CU-21 (h3-grupos-primera-pasada.txt)
POST …/pagos -> 201   {'estadoObligacion': 'PAGADO', 'esNuevo': True}
POST …/pagos (misma clave) -> 200   {'esNuevo': False}   mismo pagoId
```

## No cubierto

- **La app móvil en el emulador** (login, billetera, recarga, transferencia, grupos, aporte): no hay una sola captura (H3.S15).
- Tests de integración de Gradle y `EsquemaAlDiaRepositorioTest` (deriva): no corridos.
- El sandbox regulado se retiró **después** de la corrida consolidada; esa corrida lo tenía activo. No cambia ningún veredicto (CU-20 falla antes por B2/B3 y los demás CU no consultan licencia), pero no se reejecutó.
- CU-60 y CU-61 con un caso positivo; CU-69 aceptar; CU-64 positivo; CU-62 aceptar; CU-11 aprobar y conciliación; CU-10 con saldo; reversas.
- Caída deliberada de un servicio dependido; traza cruzada.
- Backoffice en tablet/escritorio de las demás pantallas más allá de las inspeccionadas; lector de pantalla; teclado.
- iOS.
- CU-63 solo se tocó al proponer el acuerdo (falla por B16, no se llegó a votar); CU-65 a CU-67 (retirarse, reemplazar, disolver) no se probaron.

## Desvíos del plan

- **Alcance ampliado con autorización:** B6 y B7 se corrigieron en `scripts/generar_ddl.py` y `docs/Restricciones.md` (y se regeneraron `sql/00_base/02_esquemas.sql`, `03_permisos.sql`, `sql/40_reglas/restricciones.sql`). Decisión del usuario («Corregirlo en esta rama»). B8 y los demás **no** se tocaron.
- **Sandbox regulado:** aplicado a mano → sembrado → **retirado** por romper `R-LIC-01`.
- **newman** no está instalado: driver Python con la librería estándar, sin dependencias nuevas.
- **Windows:** `bd:levantar` (MinIO one-shot) y `bd:generarSemillas` (alias `python3`) fallan; se sortearon con `-x` y `python`.
- Se restauraron `packages/tokens/vectores/monto.json` y `apps/movil/pubspec.lock`, que el build/`pub get` modificaron (ruido de fin de línea / versión de Flutter).

## Riesgos residuales y deuda

- Con B6/B7 corregidos pero B8 abierto, el libro sigue sin poder registrarse desde un participante: **no hay manera de demostrar un solo saldo real**.
- B15 permite marcar pagos sin dinero: gravedad alta.
- El stack local queda con datos mutados por la campaña (obligación PAGADA, entrega ENTREGADA, órdenes de recarga PENDIENTE): reseteable con `bd:reset`.

## Decisiones y ambigüedades

1. «Aceptar a otro miembro»: `CU68AceptarIngreso` es un stub y no hay endpoint; se tomó como BRECHA. **Confirmar con Pablo.**
2. «Recibir insignia de pago»: solo hay `evaluarInsignias` (SOPORTE, inalcanzable); se probó como BRECHA. **Confirmar con producto.**
3. «Vender cupo» = traspaso (CU-64), que ejecuta el organizador. **Confirmar con producto.**
4. `ejecutarEntrega` acepta un `montoEntregado` menor que el neto: ¿es diseño? **Confirmar con tesorería.**
5. Las 3 correcciones de DDL deben revisarse por quien mantenga `generar_ddl.py`/`Restricciones.md` (CI de otros carriles usa estos archivos).
6. El PR no se replica a `PasanakuFrontend`: lo decide el equipo.

## Procesos que quedan corriendo

- Docker: 18 contenedores `aportaya-*` (11 servicios + infraestructura) y `buildx_buildkit_aportaya0` (~2,5 GiB). **Se dejan arriba** para que se pueda reproducir; para liberar memoria: `docker compose -f despliegue/compose/base.yml -f despliegue/compose/servicios.yml --profile todo down`.
- No quedan servidores de desarrollo ni emuladores (el de `ng serve` se cerró; el AVD nunca se arrancó).
