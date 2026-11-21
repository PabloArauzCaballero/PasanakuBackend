# Reporte — Tanda 2, lote L3L5

> **AVANCE: 6 / 24 — 25,0 %.** (F1.L3.M04 en A MEDIAS)

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `correcciones/tanda2-l3l5-test-admision` (#423), `correcciones/tanda2-l3l5-f1-retiro`
- Peldaño de evidencia alcanzado: T2.M0 `TESTED` (reproducción bajo carga antes/después + suites del servicio).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T2.M0 | El arranque único de Mockito ya no cae dentro de `armar()`; con la CPU repartida la clase pasa 3/3 (antes `TimeoutException` a 7,3 s) sin cambiar ningún límite | `taskset -c 0 ./gradlew --no-daemon :servicios:grupos:test --tests '*AdmisionDelGrupoSustitucionTest' --rerun` ×3 con 3 procesos ocupados en el mismo núcleo | PASS · [evidencia](./evidencia/T2.M0-mockito-arranque.txt) |

| F1.L3.M06 | «Grupos activos» cuenta al participante en mora y excluye los grupos terminales reales; los estados salen de enumerados de dominio comparados con el `CHECK` | `./gradlew :servicios:grupos:test --tests '*EstadosContraElModeloTest'` + `psql` vieja/nueva | PASS · [evidencia](./evidencia/F1.L3.M06-estados-vivos.txt) |
| F1.L3.M07 | Un acuerdo se resuelve una vez: precondición `estado = 'ABIERTO'` en el `UPDATE`; quien pierde la carrera no reemite | dos conexiones `psql` (vieja `UPDATE 1`/`UPDATE 1`, nueva `UPDATE 1`/`UPDATE 0`) + `ResolucionDeAcuerdoTest` | PASS · [evidencia](./evidencia/F1.L3.M07-acuerdo-precondicion.txt) |
| F1.L5.M26 | Marcar leído un aviso interno exige ser su destinatario (usuario o rol), en la escritura; ajeno → 404 | `psql` (B: `UPDATE 0`) + `AvisosInternosWebTest` | PASS · [evidencia](./evidencia/F1.L5.M26-avisos-destinatario.txt) — integración `NO COMPROBADO` (Docker); RLS por destinatario pendiente (bloque genérico) |
| F1.L5.M27 | El contrato de `notificaciones` no tiene ninguna operación con el contacto en la ruta o la query: se retiró el `GET /notificaciones/supresion` | verificación del contrato con PyYAML + `webTest` (el GET no contesta ni con permiso) | PASS · [evidencia](./evidencia/F1.L5.M27-supresion-url.txt) |
| F1.L5.M23 | Webhook CU-82: remitente, contenido, canal e id salen de la carga firmada; fuera de ventana (5 min) se rechaza; el `idMensaje` firmado deduplica | `WebhookDeMensajeriaTest` (doble con HMAC real, tres niveles) | PASS contra doble · [evidencia](./evidencia/F1.L5.M23-webhook-mensajeria.txt) — `CU82Test` `NO COMPROBADO` (Docker) |

## A medias
### T2.M0b — El mismo arreglo para todos los servicios
- Qué anda: en `grupos`, Mockito entra como `-javaagent` y su arranque se paga en `MockitoAlAbrirLaSesion`.
- Qué no anda: los otros servicios siguen con el auto-attach de Mockito dentro de la primera prueba que mockea.
- Qué falta exactamente: mover el bloque `tasks.withType<Test>` de `servicios/grupos/build.gradle.kts` a
  `buildSrc/src/main/kotlin/aportaya.base.gradle.kts`, y `MockitoAlAbrirLaSesion` + su archivo
  `META-INF/services` a `plataforma/comun-pruebas` (con `junit-platform-launcher` en su `api`); después borrar la
  copia de `grupos`. Lo hace la sesión dueña de `buildSrc`/`plataforma`.
- Dónde quedó: rama `correcciones/tanda2-l3l5-test-admision`, compila, suites de `grupos` en verde.

### F1.L3.M04 — El retiro lo pide solo su titular
- Qué anda: en la API, el participante sale del token y del `grupoId` de la ruta; otro integrante en el cuerpo,
  otro usuario y rol insuficiente reciben 403, sin token 401, sin invocar el caso de uso
  ([evidencia](./evidencia/F1.L3.M04-retiro-titular.txt), incluye el contraexperimento contra `dev`: 500 con el ajeno).
- Qué no anda: la base todavía no lo impide: `solicitud_retiro` no tiene titular y cae en la política genérica.
- Qué falta exactamente: (1) en `docs/entidades/02_grupos_turnos.puml`, `SolicitudRetiro.usuario_id UUID NOT NULL`
  (FK `identidad.usuario`), regenerar `sql/` con `generar_ddl.py`; (2) en el bloque genérico `fn_seg_aplicar_rls`
  (`docs/Restricciones.md` → `sql/40_reglas/restricciones.sql`), agregar `'solicitud_retiro'` a
  `visibles_por_titular` — compartido, fuera del lote; (3) `RetiroRepositorio.solicitar` escribe `usuario_id`
  con `ctx.usuarioId()`; (4) integración con RLS activa (Docker). (1) y (3) sin (2) romperían el alta del titular:
  van juntos.
- Dónde quedó: rama `correcciones/tanda2-l3l5-f1-retiro`, compila, suites de `grupos` en verde.
### F1.L3.M05 — El traspaso verifica el KYC real del entrante
- Qué anda: el controlador pasa a `CU-64` el nivel que dice `identidad` (`afuera.nivelDeKyc`); un entrante sin KYC
  llega como `NINGUNO` y la regla lo rechaza (`ENTRANTE_SIN_KYC`, 422); con `identidad` caída el puerto degrada a
  `NINGUNO` y el traspaso falla cerrado ([evidencia](./evidencia/F1.L3.M05-kyc-traspaso.txt); contraexperimento:
  el código viejo pasaba `BASICO`).
- Qué no anda: con `identidad` caída la respuesta es 422 y no 503, como pide el CA del anexo.
- Qué falta exactamente: un método de puerto que distinga «no responde» de «nivel NINGUNO» (p. ej.
  `Optional<String> nivelDeKycVerificado`) y un mapeo a 503 en el borde; decidir si los otros tres llamadores
  (`MapeoDeAltaDeGrupo`, `MapeoDePostulacion`, `RutasDeSubastaDelCupo`) adoptan el mismo contrato. Integración con
  `identidad` apagado (Docker).
- Dónde quedó: rama `correcciones/tanda2-l3l5-f1-kyc`, compila, suites de `grupos` en verde.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Resto de H1.S1, H1.S2 y H2 | TODO | Se ejecutan en orden en esta sesión |

## Evidencia
- [`evidencia/T2.M0-mockito-arranque.txt`](./evidencia/T2.M0-mockito-arranque.txt)

## No cubierto
- Corredores con Testcontainers de `grupos` (`integrationTest`): no corren sin Docker.

## Desvíos del plan
- El `PLAN.md` se escribió después de la primera edición de `servicios/grupos/build.gradle.kts` (la reproducción
  y el diagnóstico se hicieron antes). Se registra como desvío de la regla 20.

## Riesgos residuales
- CI de #423: «13 · test» cayó en `identidad · RotacionDeClavesDeFirmaTest.laAnteriorSoloSePublica() timed out after
  5 seconds`. No es de este lote. Causa probable, sin demostrar: genera dos pares RSA de 2048 bits dentro de cada
  prueba (`RSAKeyGenerator(2048)`), un costo aleatorio y atado a la CPU que, con la CPU del runner repartida, puede
  pasar los 5 s del atom. Mismo patrón que T2.M0 (costo de preparación dentro del atom). A derivar a la sesión L4.
- CI: `osv-scan` falla en los PR del lote por vulnerabilidades npm de `yarn.lock` (hono 4.13.11, @hono/node-server
  2.1.3, minimatch 10.2.6). Clase `EXTERNAL`, preexistente y ajena al lote: `yarn.lock` está fuera de alcance.
- Bajo la carga extrema simulada el primer caso tarda 3,3 s de 5 s: el margen restante es trabajo propio de la
  clase (mockear cuatro tipos del proyecto).

## Decisiones y ambigüedades
- F1.L5.M23: ventana del webhook de mensajería = 5 minutos (`aportaya.webhook.ventana: PT5M` en `application.yml`).
  No es un parámetro financiero; es un valor de seguridad elegido por esta sesión, a confirmar con Pablo. El formato
  de la carga firmada es el del puerto propio (no hay proveedor contratado, P-15).
- F1.L3.M06: «vinculados» = `ACEPTADO_PENDIENTE_FIRMA`, `ACTIVO`, `EN_MORA`, `SUSPENDIDO`. Un `EXPULSADO` que
  todavía debe al fondo no cuenta como grupo activo: su deuda la custodia `garantia` (deudas vivas), no este conteo.
  Supuesto a confirmar con Pablo: que el cierre de billetera también consulte deudas vivas.
- Se declaró `junit-platform-launcher` como `testImplementation` de `grupos` (ya estaba en el catálogo y en el
  runtime de pruebas; solo cambia `testCompileClasspath` en el lockfile). A confirmar con Pablo al hacer T2.M0b.
