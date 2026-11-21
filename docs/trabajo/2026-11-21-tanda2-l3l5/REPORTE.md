# Reporte — Tanda 2, lote L3L5

> **AVANCE: 10 / 27 — 37,0 %.** A MEDIAS: F1.L3.M04, F1.L3.M05, F1.L5.M28. Abiertos: #448 y #486. Retoma: ver «Para quien retome».

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md)
- PR hacia `dev`: mergeados #428 (F1.L3.M04), #431 (F1.L3.M06), #434 (F1.L3.M07), #437 (F1.L3.M05), #453 (T2.M1);
  #423 (T2.M0) incluido en #444. **Abierto: #448**, consolidado (F1.L5.M26, M27, M23, M21 y M28); #438, #440, #443
  y #446 se cerraron absorbidos en #448 (tope de 3 PR abiertos por sesión, pedido de la coordinación).
- Estado del CI: `origin/dev` (con #433 OSV, #419, #444 Mockito y #453 aliado) mergeado en toda la pila el
  2026-11-21; el CI de cada PR se vuelve a correr con eso. Antes: `osv-scan` en rojo por npm de `yarn.lock`
  (resuelto por #433) y #423 en rojo por `identidad · RotacionDeClavesDeFirmaTest` (cubierto por #444).
- Peldaño de evidencia alcanzado: `TESTED` en todo lo completado (pruebas dirigidas + suites unitaria, web y
  barrido de cada servicio tocado, más `psql` contra PostgreSQL local donde hay SQL). No llega a `VERIFIED`: las
  integraciones con Testcontainers no corrieron (Docker) y el CI de los PR sigue en cola.
- Conteo: 27 microtareas en `PLAN.md` (H1.S0: 3 · H1.S1: 7 · H1.S2: 11 · H2: 2 · H3: 4, agregado por la coordinación); `plan_status.py` no reconoce el
  formato de IDs de este plan, así que el número sale de la tabla.

## Para quien retome este lote (traspaso de coordinación, 2026-11-21)

- **Estado:** 10/27 (ver primera línea). Todo está empujado a `origin`; no queda nada solo en local.
- **PR abiertos (tope: 3 por sesión):**
  - **#448** · rama `correcciones/tanda2-l3l5-f1-tarifario` · F1.L5.M26, M27, M23, M21 y M28 (parcial). Es la punta
    de la pila: acá viven `PLAN.md` y `REPORTE.md` al día. Absorbe a #438, #440, #443 y #446 (cerrados).
  - **#486** · rama `correcciones/tanda2-l3l5-f3-parametros` · F3.S8.M2a: modelo del registro de parámetros en
    `tarifas` + siembra de los 7 códigos sin valores (unidades: DECISIONES tanda 11, #489). Su copia de
    `PLAN.md`/`REPORTE.md` es la de `dev`; la vigente es la de #448.
- **Mergeados:** #428, #431, #434, #437, #453; #423 dentro de #444.
- **Siguiente, en orden:** F3.S8.M2b (API en `tarifas`: proponer, aprobar con `CuatroOjos` de `plataforma/comun-web`,
  consultar vigentes y `GET /tarifas/parametros/grupos/{grupoId}` según el contrato de L2 en
  `docs/trabajo/2026-11-21-tanda2-l2/evidencia/F3.L2.M14-contrato-parametros-tarifas.md`: 200 / 404 sin congelamiento
  / 422 con el código que falta), después F3.S8.M6 (consumidor idempotente de `grupos.grupo_iniciado` que escribe
  `parametro_congelado_grupo`) y F3.S8.M4 (pantalla del backoffice). Luego, lo pendiente de la tabla «Pendiente».
- **Archivos vedados por aviso de otros lotes:** `servicios/grupos/.../HechosPorHttp.java`, `PrimerAportePorHttp.java`
  y su credencial de servicio, hasta que entre el PR de L1 (M07). F1.L3.M05 (503) espera ese PR.
- **Cómo verificar en la nube:** receta de PostgreSQL en 5433 de
  `docs/trabajo/2026-11-21-plan-correcciones/EJECUCION.md`; `generateJooq` solo declara salidas, así que tras un
  cambio de esquema hay que correrlo con `--rerun`. Para intermitentes: reproducir con `taskset -c 0` y 3 procesos
  ocupados (ver T2.M0 y T2.M1).
- **Decisiones que esperan a Pablo:** ventana del webhook de mensajería (5 min, F1.L5.M23); fuente para derivar el
  tipo de cambio del tarifario (F1.L5.M28, o F3.L5.M36); D-L5-3 (segundo firmante del tarifario).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| T2.M0 | El arranque único de Mockito ya no cae dentro de `armar()`; con la CPU repartida la clase pasa 3/3 (antes `TimeoutException` a 7,3 s) sin cambiar ningún límite | `taskset -c 0 ./gradlew --no-daemon :servicios:grupos:test --tests '*AdmisionDelGrupoSustitucionTest' --rerun` ×3 con 3 procesos ocupados en el mismo núcleo | PASS · [evidencia](./evidencia/T2.M0-mockito-arranque.txt) |

| T2.M0b | El arranque de Mockito se paga fuera de los atoms en TODA JVM de prueba (`buildSrc` + `comun-pruebas`); lo hizo la coordinación en #444 sobre el commit de #423 | en la punta de la pila, tras mergear `dev`: `./gradlew :servicios:grupos:spotlessCheck test webTest testBarrido --rerun` → 119/184/2 en verde, 0 avisos `self-attaching` | PASS · #444 en `dev` |
| F1.L3.M06 | «Grupos activos» cuenta al participante en mora y excluye los grupos terminales reales; los estados salen de enumerados de dominio comparados con el `CHECK` | `./gradlew :servicios:grupos:test --tests '*EstadosContraElModeloTest'` + `psql` vieja/nueva | PASS · [evidencia](./evidencia/F1.L3.M06-estados-vivos.txt) |
| F1.L3.M07 | Un acuerdo se resuelve una vez: precondición `estado = 'ABIERTO'` en el `UPDATE`; quien pierde la carrera no reemite | dos conexiones `psql` (vieja `UPDATE 1`/`UPDATE 1`, nueva `UPDATE 1`/`UPDATE 0`) + `ResolucionDeAcuerdoTest` | PASS · [evidencia](./evidencia/F1.L3.M07-acuerdo-precondicion.txt) |
| F1.L5.M26 | Marcar leído un aviso interno exige ser su destinatario (usuario o rol), en la escritura; ajeno → 404 | `psql` (B: `UPDATE 0`) + `AvisosInternosWebTest` | PASS · [evidencia](./evidencia/F1.L5.M26-avisos-destinatario.txt) — integración `NO COMPROBADO` (Docker); RLS por destinatario pendiente (bloque genérico) |
| F1.L5.M27 | El contrato de `notificaciones` no tiene ninguna operación con el contacto en la ruta o la query: se retiró el `GET /notificaciones/supresion` | verificación del contrato con PyYAML + `webTest` (el GET no contesta ni con permiso) | PASS · [evidencia](./evidencia/F1.L5.M27-supresion-url.txt) |
| F1.L5.M23 | Webhook CU-82: remitente, contenido, canal e id salen de la carga firmada; fuera de ventana (5 min) se rechaza; el `idMensaje` firmado deduplica | `WebhookDeMensajeriaTest` (doble con HMAC real, tres niveles) | PASS contra doble · [evidencia](./evidencia/F1.L5.M23-webhook-mensajeria.txt) — `CU82Test` `NO COMPROBADO` (Docker) |
| F1.L5.M21 | El certificado firma los valores de la foto del propio titular y la identidad la dice `identidad` (falla cerrado); `disponibles` e `identidadVerificada` del cuerpo se ignoran (deprecados) | `CertificadoConDatosDelServidorTest` + `psql` + `webTest` | PASS contra doble · [evidencia](./evidencia/F1.L5.M21-certificado-servidor.txt) — `CU75*Test` `NO COMPROBADO` (Docker) |
| T2.M1 | `inversiones · AliadoSimuladoFlujosContratoTest`: el simulador del aliado respondía HTTP/1.0 y cerraba el socket sin decirlo; el `HttpClient` de Java reutilizaba la conexión cerrada. Ahora habla HTTP/1.1 | diagnóstico bajo carga (29/60 → 0/60 con A y con B) + `contractTest` ×3 con carga + 27 pruebas Python | PASS · [evidencia](./evidencia/T2.M1-inversiones-aliado-http.txt) · #453 |
| F3.S8.M2a | Modelo del registro de parámetros en `tarifas`: catálogo sin valores, versiones con cuatro ojos e inmutables, congelamiento único por grupo, parámetro y nivel (R-TAR-14 a 17), CU-132 | `generar_ddl.py` + `aplicar.sql` ×2 + `psql` con cada violación + verificadores de bóveda | PASS · [evidencia](./evidencia/F3.S8.M2a-modelo-parametros.txt) · #486 |

## A medias
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
### F1.L5.M28 — Publicación de tarifario
- Qué anda: el aprobador registrado es el actor de la sesión; un aprobador inventado en el cuerpo es 422
  `AP-CU34-06` sin publicar nada ([evidencia](./evidencia/F1.L5.M28-tarifario-actor.txt)).
- Qué no anda: el tipo de cambio (`INCREMENTO`/`REDUCCION`…) sigue viniendo del cuerpo y decide el preaviso.
- Qué falta exactamente: una fuente para comparar. `CU-34` clona el tarifario base sin precios nuevos, así que no hay
  comisión nueva en este flujo. Se resuelve con F3.L5.M36 (comisión fija como parámetro obligatorio, cambio con dos
  firmas: DECISIONES tanda 7) o, si se conserva el tarifario, con los precios nuevos como entrada de `CU-34` y la
  comparación en `EntradaEnVigencia`. Segundo firmante: depende de D-L5-3 (`PREGUNTAS-LEGALES.md`).
- Dónde quedó: rama `correcciones/tanda2-l3l5-f1-tarifario`, compila, suites de `tarifas` en verde.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| F1.L3.M03 (destino de transferencia por id único) | TODO | Toca `nucleo-financiero` e `identidad` (otros lotes) además de `grupos`; coordinar el contrato nuevo |
| F1.L3.M08 (importes con moneda y `CHECK >= 0` en `grupos`) | TODO | Depende de F2.L3.M12 (no migrar tablas que se borran); cambio por `.puml` + `generar_ddl.py` |
| F1.L3.M09 (secreto de invitación fuera del path) | TODO | Toca `apps/movil` (fuera de este lote) |
| F1.L5.M20 (sorteo con entropías reveladas) | TODO | Siguiente en el orden de este lote |
| F1.L5.M22 (sello de la cadena con permiso propio) | TODO | Depende de F4.L5.M52 (API de saldos del mayor) |
| F1.L5.M13/M14/M15 (comisión: devolución, descuentos, marcado manual) | TODO | Relocalizar en `tarifas` (`ComisionesController`) |
| F1.L5.M29 (publicidad: propiedad, impresión única, tope) | TODO | Siguiente en el orden de este lote |
| F1.L5.M09 (`Idempotency-Key` en los servicios de apoyo) | TODO | Siguiente en el orden de este lote |
| F1.S2.M3 (matriz BOLA en `scripts/verificar_seguridad.py`) | TODO | Ampliar el script con las matrices ya escritas en los `*WebTest` de este lote |
| F2.M6 (retirar el organizador profesional) | TODO | Empieza por el inventario de consumidores (`inventario_endpoints.py`, `listar_topicos.py`) |
| F2.M8 (retirar aprobaciones eliminables en el lote) | TODO | F2.L3.M10 depende de F3.L3.M17; F2.L3.M15 se mergea después de F3.L3.M18 |
| F3.S8.M2b/M6/M4 (API, congelamiento, pantalla) | TODO | Modelo en #486; dueño `tarifas` a confirmar con Pablo; siembra de códigos espera las unidades DECISION_REQUIRED |
| Corridas con Docker (`integrationTest` de `grupos`, `notificaciones`, `transparencia`) | BLOQUEADO (P-19) | Máquina de Pablo; comandos en cada evidencia |

## Evidencia
- [`evidencia/T2.M0-mockito-arranque.txt`](./evidencia/T2.M0-mockito-arranque.txt)

## No cubierto
- Corredores con Testcontainers (`integrationTest`) de `grupos`, `notificaciones` y `transparencia`: no corren sin
  Docker. Se actualizaron `AvisoInternoRepositorioTest`, `CU82Test`, `CU75Test`, `CU75RechazosTest` y
  `BaseDeTransparencia`; compilan, `NO COMPROBADO` en ejecución.
- Políticas RLS por titular/destinatario (`solicitud_retiro`, `aviso_interno`): bloque genérico compartido.
- Pactos nuevos (`transparencia → identidad`): no escritos.

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
