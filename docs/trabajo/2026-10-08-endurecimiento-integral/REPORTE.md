# Reporte — Endurecimiento integral (cierre tras el corte por falta de RAM)

> **AVANCE: 89 / 114 — 78,1 %.** (`microtareas HECHO / total` de `PLAN.md`, calculado con `python .claude/hooks/plan_status.py`; `A MEDIAS` cuenta como no hecha. Retomada 2026-10-12: de 69 / 86 a 89 / 114; H17 agregó 28 microtareas, 20 en HECHO.)

- Fecha: 2026-10-08 · Plan: [PLAN.md](./PLAN.md) · Notas: [NOTAS.md](./NOTAS.md) · Repos: PasanakuBackend (principal), PasanakuFrontend (carril E). Integrado sobre `origin/test` en la rama `justin/feature/cierre-endurecimiento-2026-10-08` (ver «Integración sobre test»).
- Peldaño de evidencia alcanzado: **TESTED por servicio** (regla 30, peldaño 4), contra PostgreSQL real vía Testcontainers. **No** se alcanzó `REGRESSION_VERIFIED` global: falta integración real entre servicios y E2E de punta a punta.
- Conteo histórico (corte del 2026-10-09; el vigente es el de la línea AVANCE): `python .claude/hooks/plan_status.py` → HECHO=35, A MEDIAS=24, BLOQUEADO=10, TODO=17 (de 86). `A MEDIAS` cuenta como no hecha.
- Estado de la máquina al cerrar: sin `heavy.lock`, sin procesos java ni Docker, sin `hs_err_pid*`/`replay_pid*` (se borraron los restos del crash), unos 6,7 GB libres.

## Retomada 2026-10-12 (H17): carriles P, Q, R y T verificados

- Rama: `claude-marcelo/affectionate-heisenberg-nzjzml` (Backend). Parte de `dev` (`08042090`, ya con el PR #53) más los commits `WIP:` de P/Q/R/T de `justin/feature/cierre-endurecimiento-2026-10-08`. Frontend: sin cambios (`PasanakuFrontend` no se tocó).
- Peldaño de evidencia por área (regla 30): **Backend P, Q, R: `TESTED`** contra PostgreSQL real; la cadena P·Q llegó a **`VERIFIED` en un stack local** (servicios, Kafka y Postgres reales, con las caídas provocadas de abajo); **Frontend JS: `TESTED`**; **App móvil (Flutter): `TESTED`** (sin prueba visual: `VERIFIED_FUNCTIONAL_ONLY` en lo que sea visual); **TEST (Contabo): `UNKNOWN`**, no se tocó. El peldaño del trabajo es el más bajo de sus áreas: `UNKNOWN` en TEST. No es `REGRESSION_VERIFIED`.
- Entorno: contenedor Linux con 16 GB; Postgres 16 y Kafka en Docker, Flutter 3.44.8 (tag de GitHub), Node 24 y Yarn 4.18.0 instalados desde el registro de npm (`repo.yarnpkg.com` devuelve 403 por la política de salida de la organización), y un espejo de Maven Central para Gradle (`~/.gradle/init.d`, fuera del repo) porque `repo.maven.apache.org` respondía 429. Nada de eso entra al repositorio.

### Completado en H17 (comando → resultado; salidas en `evidencia/carril-H17/`)

| Qué | Comando | Resultado |
|---|---|---|
| Esquema aplicado y compila todo | `psql -f sql/aplicar.sql`; `./gradlew compileJava compileTestJava generarClientes` | RC 0; 436 tablas; clientes sin diff |
| Formato | `./gradlew spotlessApply` y `spotlessCheck` (en la regresión) | verde |
| Regresión completa del backend | `./gradlew spotlessCheck test webTest integrationTest contractTest sagaTest testBarrido --continue` | 22 módulos, **3.587 pruebas, 0 omitidas, 3 rojas** (las 3 son anteriores a H17, ver Hallazgos) — `regresion-completa.log`, conteo desde XML |
| P: grupos / tarifas / identidad | ídem por servicio | 385 / 140 / 274 pruebas, 0 fallas |
| Q: nucleo-financiero / aportes / entregas | ídem | 581 / 187 / 212 pruebas, 0 fallas (nucleo y entregas con `contractTest` 10 y 9) |
| R: erp / publicidad | ídem | 135 / 75 pruebas, 0 fallas |
| Ensayo de esquema aditivo (R-CTB-11) | `bash evidencia/carril-H17/r_upgrade_local.sh` | columna `clave_idempotencia` NULL + índice `uq_pago_a_proveedor_clave`; 2 pagos legados intactos; 2.ª corrida «columnas expandidas: 0»; el UNIQUE corta el duplicado — `r-upgrade.log` |
| Siembra de la política de invitación (ENLACE) | `politica_invitacion_test.sh` ×2 sobre una base desechable con el estado de TEST | `permite_ENLACE` false → true; 2.ª corrida «cambios: 0 (idempotente)» — `politica-invitacion-ensayo.log` |
| Gates | `verificar_criterios/seguridad/pruebas_web/boveda/maqueta/contratos_limites/rutas_frontend/frontend` | todos RC 0 |
| Frontend JS | `yarn turbo run typecheck lint test:front` (sin los paquetes Flutter) y `build` de web y backoffice | typecheck, lint y build RC 0; **1.007 pruebas** (simulado 261, tokens 15, dominio-cliente 44, tutoriales 9, web 74, ui 125, backoffice 479) |
| App móvil y paquete de diseño | `dart format`, `dart analyze --fatal-infos`, `flutter test` (unidad, widget, contrato, identidad, pasanaku, a11y) | formato y análisis RC 0; movil **407 + 4 a11y**, diseño **47 + 14 a11y**, 0 fallas |
| Recorrido A/B local con servicios y Kafka reales | `python3 evidencia/carril-H17/recorrido_ab.py` | A crea el grupo (202) → `ABIERTO_A_INSCRIPCION` en 1–3 s por la cadena `grupo_creado` → `tarifa_congelada`; 1 tarifa con hash y 1 historial; nucleo abre la billetera del grupo en 0,1 s; invitar por **ENLACE 201**; B consulta (200) y acepta (201) con participación en la base; segundo pedido sobre el enlace usado → 422, sin duplicar; canje con postulación pendiente → `AP-CU68-06`, enlace sin consumir (`EMITIDO`), revocación 204 |
| Billetera de persona al evento, con entrega duplicada | `bash evidencia/carril-H17/e2e-usuario-registrado.sh` | 1.ª entrega `ABIERTA` (ACTIVA BOB), 2.ª `DUPLICADO`, una sola billetera |
| Caídas provocadas (regla 98.8) | `python3 evidencia/carril-H17/e2e_caidas.py` | **tarifas apagado**: alta rechazada `AP-CU20-01`, sin escribir; **grupos con `kill -9` tras confirmar el alta**: el evento queda `PENDIENTE` en el outbox y el grupo `BORRADOR`, al volver se publica y el grupo abre solo (1 tarifa, 1 billetera); **garantia apagado**: la postulación falla cerrada `AP-CU68-01` sin escribir nada y entra (201) al volver |
| Traza | `e2e-traza.log` | `grupo_creado` y `tarifa_congelada` comparten el `correlationId` |

### Defectos hallados al correr por primera vez lo que el relevo dejó en `WRITTEN` (regla 80.4)

| # | Qué falló | Clase | Corrección |
|---|---|---|---|
| 1 | grupos `AbrirInscripcionDelGrupo`: `INSERT … SELECT` con la fecha sin tipo → `varchar` en una columna `timestamptz` | PRODUCT_BUG (P) | `?::timestamptz`, `?::uuid`, `?::text` |
| 2 | tarifas `TarifaCongeladaRepositorio.congelar`: misma causa | PRODUCT_BUG (P) | `?::timestamptz` |
| 3 | `CanjearInvitacion.comprobarAntesDeConsumir`: el `return` salía solo de la lambda; el reintento de un canje ya completado igual pasaba por `postulaciones.comprobar` (rechazo `AP-CU68-06`), contra su propio javadoc | PRODUCT_BUG (P) | devuelve si ya estaba canjeada y sale del método |
| 4 | `CU69CanjeTest`: ponía `REVOCADA` sin `fecha_respuesta` (viola `ck_invitacion_respuesta`) | TEST_BUG | agrega la fecha |
| 5 | `CongelarTarifaDelGrupoTest` corría en `test` (límite de 5 s, sin contenedor) | TEST_BUG | pasa a `CU20CongelarTarifaTest` (corredor de integración) |
| 6 | `UsuariosTitularidadLimiteWebTest`: faltaba mockear `CU02ConsultarEstadoDeVerificacion`, dependencia que trajo `dev` | TEST_BUG | agrega el mock |
| 7 | erp `EscenarioDeFactura` seguía con `UUID aprobadaPor` (R pasó los llamadores a `boolean aprobar`) | TEST_BUG | helper con `boolean aprobar` |
| 8 | tarifas: dos `assertThat` ambiguos / mal tipados que no compilaban | TEST_BUG | tipos explícitos |
| 9 | nucleo `CUConsumoDeAportesTest.fueraDeOrden`: esperaba excepción para un aporte sin saldo; el contrato vigente (CU-21 flujo 3b, carril Q) es rechazo definitivo con `aporte_no_aplicado` | TEST obsoleto | se reescribe el tramo (b) con el contrato vigente y las mismas garantías: cero efectos, evento marcado consumido, duplicado sin efecto, y el fondeo tardío no resucita el aporte |
| 10 | nucleo `CU13PactoInversionesTest`: el doble solo simulaba `aportes`; `HechosPorHttp` ahora pregunta primero a `grupos` | TEST_BUG (doble incompleto) | el doble de `IdentidadesDelPacto` simula también `GET /grupos/participaciones` |
| 11 | tres archivos pasaban de 300 líneas (`CU13EjecutarConDestino` 313, `CU13MercadoTest` 356, `CU21CompensaTest` 349) | regla propia (`BarridoTest`) | `RepartoDeCompraEnMercado`; `BaseDeMercado` + `CU13MercadoRechazosTest`; `BaseDeCompensacion` + `CU21CompensaRechazosTest` |
| 12 | app: `pantalla_mfa_test` contaba pedidos dentro del callback de `onPost`, que corre al registrar la ruta (daba 1 antes de escribir); texto de `AP-CU04-04` distinto del de `dev`; `idioma_test` esperaba `CANCELAR` (la etiqueta es `Cancelar`) | TEST_BUG | contador con interceptor; texto de `dev`; etiqueta real |
| 13 | app: `estado_alta.dart` (247) y `motor.dart` (206) pasaban de 200 líneas; formato Dart roto en 8 archivos (3 de T, 5 heredados de `dev`) | regla propia / ENVIRONMENT de la deriva | `modelo_del_alta.dart`, `textos_del_motor.dart`; `dart format` |
| 14 | CU-20, `grupos.yaml` y `CU20CrearGrupo` seguían diciendo que nucleo «todavía no» abre la cuenta del grupo | documentación desactualizada | actualizados |

### Hallazgos que NO se corrigieron (registrados en el plan, H17.S9)

1. **`aceptarInvitacionPorEnlace` ignora la `Idempotency-Key`** (regla 96.3.3): reintentar con la misma clave devuelve 422 `AP-CU69-05` aunque la participación ya se creó. Anterior a P (`d8e103ec`). Visto en `e2e-recorrido-ab.log` (único `FAIL` del recorrido, deliberadamente dejado a la vista).
2. **3 pruebas rojas de `plataforma/comun-pruebas`** (`MatrizDeAutorizacionContratoTest`): `renovarSesion` y `consultarEstadoDeVerificacion` no están en `PoliticaDeAcceso`, y `historialAdmision` no tiene controlador. Rojas también en `origin/dev` `08042090` (`baseline-dev-contractTest.log`). Son decisiones de seguridad y de contrato de otros carriles.
3. `verificar_carriles.py`: «sin carril dueño: inversiones» (anterior).
4. `packages/diseno_flutter/pubspec.lock` está resuelto con otro Flutter (con 3.44.8 baja `matcher`/`meta`) y `.flutter-plugins-dependencies` está versionado con rutas de otra máquina. Se revirtió el cambio que hizo `flutter pub get`.
5. Goldens de Flutter: movil 9 y diseño 15 fallan por píxeles en Linux (el CI los genera en macOS): clasificados `ENVIRONMENT`, no se regeneran.

### A medias

#### H17 — Retomada de los carriles P, Q, R y T
- Qué anda: todo lo de la tabla de Completado.
- Qué no anda: nada en rojo propio de H17. Quedan los hallazgos 1–5.
- Qué falta exactamente: **H17.S7.M3**, redesplegar a TEST y repetir el recorrido allí. Hace falta la clave `id_ed25519_contabo` del dueño; con ella: `subir_a_test.sh`, `crear-topicos.sh` (topico nuevo `aportaya.tarifas.tarifa_congelada`), `politica_invitacion_test.sh`, aplicar el esquema aditivo de R (`aplicar-esquema.sh`) y los scripts de siembra.
- Dónde quedó: rama `claude-marcelo/affectionate-heisenberg-nzjzml`, compila y las pruebas de arriba están en verde.

### No cubierto en H17

- **TEST (Contabo)**: no se desplegó ni se ejercitó nada ahí. Los scripts de TEST se ensayaron contra dobles locales declarados (el shim de `docker` que hace pasar al proceso del host por los contenedores `identidad-local`/`cumplimiento-local`, y una base desechable con el estado que L3 observó en TEST).
- **Clientes**: el recorrido es por API. No se ejercitó la app (emuladores), la web ni el backoffice contra el stack; no hay **prueba visual** (capturas por viewport y tema) de lo que cambió en pantalla (MFA, billetera, idioma, quitar QR de pagos a proveedor). Las pruebas de widget y a11y pasan, pero eso es `VERIFIED_FUNCTIONAL_ONLY`.
- **Alta real de usuario** (CU-01 con OTP de correo): el OTP no se registra en el log, así que `usuario_registrado` se publicó por el outbox real con el contrato de CU-01 (productor sintético declarado).
- **Q sobre Kafka real**: el consumo de `aporte_cobrado` y la compensación `aporte_no_aplicado` ↔ `aportes` solo se probaron con base real y dobles de contrato (`CUAporteNoAplicadoTest`, `CU21Compensa*Test`), no con los dos servicios y Kafka a la vez; tampoco el reparto precio − cargos contra el `entregas` real.
- **Servicios no levantados** en el recorrido: entregas, transparencia, inversiones, erp, publicidad, auditoría, gateway; ni tokens de servicio ni mTLS entre servicios.
- **Cross-browser, rotación de claves, restauración de backups, rate limit de CU-18 bajo carga** (solo el test web con tope 3).

### Desvíos del plan

- H17 se agregó al `PLAN.md` al empezar (28 microtareas) y el recorrido local (S8) y los hallazgos (S9) se sumaron después, cuando el recorrido local resultó posible; el relevo pedía el recorrido contra TEST.
- Las 3 pruebas rojas de `comun-pruebas` y los otros hallazgos se anotaron y no se arreglaron (regla 00 §3).
- `r_upgrade.sh` de R, tal como lo dejó, sembraba pagos «legados» con las FK apagadas apuntando a facturas y usuarios inexistentes; al reponer las FK el esquema aditivo fallaba (RC 3) por culpa del ensayo, no del esquema. Se rehízo como `r_upgrade_local.sh` con una cadena válida (usuarios, proveedor, facturas) y ahí pasa.
- Se formatearon 5 archivos Dart heredados de `dev` porque `dart format --set-exit-if-changed` es parte del CI de la app (commit aparte).

### Decisiones y ambigüedades de H17

- **Merge**: en `UsuariosController` se conservó la estructura de `dev` (clase base `ExpedienteController`) y se añadió solo el tope de titularidad de P; en `errores.dart` se conservaron los textos de `dev` (los cubren `autenticar_test` y `errores_identidad_test`). Confirmar con el dueño de la app si prefiere los textos más precisos que escribió T.
- Siguen abiertas, sin cambios, las ambigüedades A1/A2 de P (apertura con precio congelado sin esperar la cuenta del grupo; contenido del snapshot de tarifa) y las de Q (billetera de persona `ACTIVA` al registrarse; solo BOB) y R (`aprobar` boolean; código nuevo `AP-CU103-05`): dueños en `ADR-052` y en los `ESTADO.md` de cada carril.
- El límite de 120 consultas/min de CU-18 sigue siendo sintético (Seguridad).

## Qué pasó y por qué hubo que repetir

La sesión anterior (carriles A–F en paralelo) se cortó hacia las 13:58 por falta de RAM. Según la sesión `observatorioeconomico-f0`, la causa raíz fue una fuga de un `svchost.exe` de Windows (de 13 a 28 GB entre las 12:03 y las 14:47) y la máquina se reinició a las 14:52; Gradle, Docker, Chrome headless y varios procesos `claude` la agravaron. Dejó **falsos rojos** (`Test process … exit value 1`, `fork: Cannot allocate memory`, `hs_err_pid*`). Los carriles A–E se relanzaron desde su evidencia, uno por uno bajo el guardián compartido `C:/Users/Usuario/.claude/ram-guard/heavy.sh` (espera ≥3500 MB libres y un solo proceso pesado a la vez en toda la máquina; Gradle con `--max-workers=2`, sin paralelo, `--stop` al final; Docker levantado y bajado dentro del guardián). Esa regla la fijó la sesión `resolver-crash-memoria-ram`. La fuga ya no está: el `svchost` actual (AppXSvc) se mantiene estable en 300–360 MB.

## Completado

| ID | Qué se logró | Comando de verificación | Resultado |
|---|---|---|---|
| H1.S1.M1, M2, M4 | Inventario de repos, matriz F01–F21 (4 existentes, 9 parciales, 8 ausentes) y registro de 16 decisiones comerciales abiertas | Archivos `evidencia/carril-F/E-H1-01…04` | PASS (documental) |
| H2.S1.M2, M3 | Disponible/retenido/liquidado con doble gasto concurrente rechazado; idempotencia con huella de petición | `:servicios:nucleo-financiero:integrationTest` (CU12DobleGastoTest, CU10/11/12/13IdempotenciaTest) | PASS, 280/280 |
| H3.S1.M1, M2, M5 | Ejemplos del mock validados contra esquema y catálogo; proveedor simulado persistente con reinicio; el gateway/mock no arranca en producción | `yarn test` simulado 171/171; CU10ServidorRealTest; E2E de build productivo 2/2 | PASS |
| H4.S1.M1–M5 | Recarga acreditada una vez, firma inválida con cero cambios de saldo y discrepancia, retiro vs transferencia sin exceder disponible, pago con resultado desconocido, QR interno vencido/alterado/reutilizado | `…nucleo-financiero:test :webTest :integrationTest :testBarrido` | PASS 40/25/280/2 |
| H5.S1.M1–M6 | Expediente de habilitación, resolución humana, creación de grupo con creador, invitación única, solicitud con propuesta, resolución final | `…grupos`, `…organizador`, `…identidad` (test, webTest, integrationTest, testBarrido) | PASS (grupos 20/50/167/2, organizador 5/35/105/2, identidad 7/55/113/2) |
| H6.S1.M2–M5 | Motor versionado y reproducible, SIN_DATOS sin rechazo automático, apartamiento humano registrado, bitácora inmutable | CU68MotorTest (8), CU68AdmisionTest, `historialNoSePuedeModificarOBorrar` | PASS |
| H7.S1.M1, M2, M4, M5 | Snapshot de roster, reserva de capacidad concurrente, sorteo verificable, sin repetición selectiva | CU60SnapshotTest, CU60Test, SorteoDelGrupoTest, CU131ConcurrenciaTest | PASS |
| H8.S1.M2, M5 | Faltante al corte desde el servidor (aportes), aporte tardío con recuperación única | CU132RecaudoTest, CU131RecuperacionTest, CU131FaltanteDelCorteTest | PASS (garantia 20/41/144/2, aportes 18/28/64/2) |
| H9.S1.M2–M4 | Oferta con validaciones, oferta visible sin datos del vendedor, compra concurrente con una sola reserva | CU130OfertasTest, CU130CompraTest, CU130CarrerasTest | PASS (entregas 10/45/94/2) |
| H11.S1.M1, M2 | Cotización antes de confirmar (el costo del cliente no manda) y reversa de cargos con asiento nuevo | CU10CotizacionTest, CU14CargosTest | PASS |
| H13.S1.M1 | Conciliación idempotente que no fabrica abonos | CU132ConciliacionTest | PASS |

Salidas literales en `evidencia/carril-A…F/` (Backend) y `evidencia/carril-E/` (Frontend). Verificación final de todo el repo, con todos los cambios incluidos: `spotlessCheck testBarrido` en todos los módulos → `BUILD SUCCESSFUL`, `RC check: 0` (log `final_gradle2.log` del scratchpad), y re-corrida completa de inversiones y nucleo-financiero tras `spotlessApply` → `BUILD SUCCESSFUL in 2m 15s`.

**Cierre del hallazgo de seguridad MEDIO** (`GarantiaController.cubrirConRespaldo` aceptaba importes afirmados por el cliente): el cuerpo ya no admite importes (`additionalProperties:false`, solo `grupoId`, `periodoId`, `turnoId`); pozo, confirmado y pendientes los trae aportes por `RecaudoDelPeriodo`; si aportes cae se deniega (AP-CU23-12); periodo ajeno o faltante que no coincide da AP-CU23-13; rol `ENTREGA_EJECUTAR`. Pruebas negativas verdes: monto inflado, periodo y turno de otro grupo, aportes caído, rol insuficiente, sin sesión.

## A medias

Todo lo de esta sección compila y la suite del servicio correspondiente está en verde. Lo que falta es lo que se indica.

### H1.S1.M3 — ADR de contradicciones
- Qué anda: el ADR existe con antes/después (`evidencia/carril-F/E-H1-03-ADR-contradicciones.md`).
- Qué no anda: está en estado **propuesto**, sin aprobación de negocio.
- Qué falta exactamente: que Dirección/negocio lo apruebe o lo corrija.
- Dónde quedó: solo documento.

### H3.S1.M3 — Inyección de fallos y reloj
- Qué anda: respuesta perdida, reinicio y doble confirmación se ejercitan contra el servidor Python real (CU10/CU11ServidorRealTest).
- Qué no anda: no hay un escenario reproducible con semilla y cronología documentado como pide el DoD.
- Qué falta: publicar el escenario con semilla y su cronología en el README del proveedor.
- Dónde quedó: `herramientas/proveedor_simulado/`, compila, 27/27 de Python.

### H4.S1.M6 — QR interno vs interoperable
- Qué anda: el backend no afirma interoperabilidad y distingue el tipo (QrDeDominioTest, CU12QrTest).
- Qué no anda: no se verificó el lado cliente (tipos, rutas y mensajes del frontend).
- Qué falta: prueba del cliente web/móvil con el mensaje distinto.
- Dónde quedó: backend en verde; sin proveedor interoperable real.

### H5.S1.M7 — Revisar decisiones y sustituir administrador
- Qué anda: `POST /grupos/{id}/administrador/sustitucion` (CU68SustitucionTest, 10 pruebas): un solo administrador vigente, idempotente, append-only, concurrente.
- Qué no anda: no se dispara solo cuando organizador revoca o suspende; no se inventó.
- Qué falta: decisión de negocio (¿automática o siempre una persona de backoffice?) y si retirar al saliente es un acto aparte (CU-65).
- Dónde quedó: `servicios/grupos`, compila.

### H6.S1.M1 — Diccionario de variables de riesgo
- Qué anda: cada postulación guarda entradas, versión y evidencia.
- Qué no anda: no existe el documento de diccionario con origen, consentimiento y calidad.
- Qué falta: escribirlo y que cumplimiento fije la definición de «apartamiento».
- Dónde quedó: sin archivo.

### H8.S1.M3, M4, M6 — Cobertura empresarial, entrega del pozo, falta extraordinaria de caja
- Qué anda: reserva/ampliación/liberación, cobertura, entrega de pozo completo una sola vez y todo el rechazo en base (CU131Cobertura, CU133PozoCompleto + concurrencia de 8 pedidos).
- Qué no anda: los asientos salen como evento con partidas y se validan contra `MayorDeDoble`; **nucleo-financiero no los consume** (D5 del plan) y no existe en él «pagar al vendedor con retención de un tercero». M6 (simulacro con responsables y comunicación) no tiene prueba nombrada.
- Qué falta: que nucleo implemente el consumo y la operación, y la prueba contra el productor real; el simulacro de M6.
- Dónde quedó: `servicios/{garantia,entregas,aportes}`; compila.

### H9.S1.M5, M6 — Liquidación de la cesión y vencimiento/cancelación
- Qué anda: compra con retención, reserva, aborto que libera, cesión liquidada que no se deshace.
- Qué no anda: pago real al vendedor contra nucleo (doble); no hay mapeo ID→prueba individual para M6.
- Qué falta: lo de nucleo de arriba y una prueba de carrera cancelación vs compra en el punto de liquidación con el productor real.
- Dónde quedó: `servicios/entregas`.

### H10.S1.M1–M7 — Inversiones (7 microtareas)
- Qué anda: catálogo, orden con consentimiento, posición, devengo (180 devengos suman exactamente 197,26), rescate y comisión de éxito; 159+ pruebas verdes con Postgres real, concurrencia real y doble del libro. Todo parámetro comercial/fiscal es SINTÉTICO con `apto_produccion=false` por CHECK; arranque en producción con el aliado simulado rechazado.
- Qué no anda: `LibroPorHttp` nunca se probó contra el productor: nucleo no publica `POST /billetera/inversiones/debitos` ni `/acreditaciones` (DR-INV-05). No existe simulación comparativa de caja (H10.S1.M7), ni trabajos programados ni reintento de instrucciones pendientes; el ADR menciona `movimiento_posicion` y `distribucion_interes`, que no están en el DDL.
- Qué falta: contrato del lado productor en nucleo (`contrato-requerido-nucleo.md`), la simulación comparativa, los trabajos, y las decisiones DR-INV-01…07 (aliado, tasas, fiscal, comisión, custodia).
- Dónde quedó: `servicios/inversiones` + `herramientas/aliado_simulado`, compila. Se escribieron los CU-120 a CU-125 en `docs/CasosDeUso` y el README del servicio.

### H11.S1.M4 — Comisión por resultado
- Qué anda: `ComisionDeExito` y CU125ComisionTest (100 cuotas, marca 110, valor 112, 10 % → Bs 20).
- Qué no anda: todo sintético; DR-INV-04 abierta (legal, fiscal, aliado). `AP-CU127-01` está declarado y ningún camino lo emite.
- Qué falta: la decisión comercial/legal.
- Dónde quedó: `servicios/inversiones`.

### H12.S1.M1–M5 — Frontend (web, backoffice)
- Qué anda: admisión humana en backoffice (E2E 8/8) e invitación por enlace en web (5/5); regresión serial: simulado 171/171, backoffice 296 + a11y 32, web 71 + a11y 4; tsc y lint limpios.
- Qué no anda: todo ese E2E corre **contra Prism (mock)**, es `VERIFIED_FUNCTIONAL_ONLY` y no sirve para declarar la funcionalidad terminada (regla 80.3.10). Saldos y posiciones separados (M2) solo a nivel de textos y pantalla de billetera de backoffice, sin captura ni E2E. De 25 capturas se inspeccionaron 4. Sin lector de pantalla ni otros navegadores que Chromium.
- Qué falta: cableado real (ver H3.S1.M4), inspección de las 21 capturas restantes, verificación con lector de pantalla.
- Dónde quedó: `PasanakuFrontend`, compila, tests verdes; `yarn humo` falla de verdad (28 de 147 aserciones) porque Prism no tiene estado ni credenciales sembradas.

### H13.S1.M3 — Protección de datos y secretos
- Qué anda: revisión escrita de PII y tokens (`evidencia/carril-F/H13-S1-M3-revision-pii-y-tokens.md`).
- Qué no anda: no hay revisión sobre logs reales ni rotación de secretos ensayada.
- Qué falta: ensayo con muestras sintéticas y rotación.
- Dónde quedó: documento.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H2.S1.M1, M4, M5 | TODO | Ningún carril lo tomó: reconciliación de cuentas/monedas, outbox con reversos y reinicio, proyección vs ledger con caché desactualizada |
| H7.S1.M3 | TODO | Calendario y cortes con feriados (UTC y America/La_Paz); depende del servicio de calendario |
| H8.S1.M1 | TODO | Generación y cobro de obligaciones; no se tocó |
| H9.S1.M1 | TODO | Ficha contractual de la cesión; decisión del área legal |
| H11.S1.M3, M5 | TODO | Distribución de intereses (no hay `distribucion_interes` en el DDL) y comprobantes/reporte fiscal (CU-128 declarado, sin camino); necesitan DR-INV-03 fiscal |
| H13.S1.M2, M4, M5, M6 | TODO | Matriz de autorización negativa transversal, restauración de backup, alarmas, reclamos con debido proceso |
| H14.S1.M1–M5 | TODO | Recorridos de punta a punta; requieren el backend integrado levantable |
| H3.S1.M4 | BLOQUEADO | Cableado cliente-dominio: no hay gateway en `localhost:8080` ni credenciales de prueba sembradas. Se destraba levantando gateway y servicios con credenciales y corriendo `yarn humo`. De: quien opere el entorno |
| H15.S1.M1–M5 | BLOQUEADO | Evidencia externa: dictamen jurídico, contratos con proveedores, capital y caja comprobables, matriz fiscal, certificación de adaptadores. Dueños propuestos en `evidencia/carril-F/H15-dossier-habilitacion-real.md`. No se afirma ninguna autorización |
| H16.S1.M1–M4 | BLOQUEADO | Depende de H15 (piloto, parada, revisión, transferencia) |

## Evidencia

Backend: `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-{A,B,C,D,F}/`. Frontend: `PasanakuFrontend/docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-E/`. Conteos finales leídos de los XML de `build/test-results` (fallos 0, errores 0, omitidas 0 en todo):

```text
inversiones        test=36 webTest=25 integrationTest=77 contractTest=17 sagaTest=6 testBarrido=2
nucleo-financiero  test=40 webTest=25 integrationTest=280 testBarrido=2
aportes            test=18 webTest=28 integrationTest=64  testBarrido=2
garantia           test=20 webTest=41 integrationTest=144 testBarrido=2
entregas           test=10 webTest=45 integrationTest=94  testBarrido=2
grupos             test=20 webTest=50 integrationTest=167 testBarrido=2
identidad          test=7  webTest=55 integrationTest=113 testBarrido=2
organizador        test=5  webTest=35 integrationTest=105 testBarrido=2
```

Gates de repo al cierre:

```text
verificar_criterios      rc=0  102 casos de uso implementados y verificados · Sin divergencias
verificar_seguridad      rc=0  TODO OK · 2 aviso(s)
verificar_pruebas_web    rc=0  controladores 33 · con matriz propia 33 · en deuda 0
verificar_boveda         rc=1  única falla: «índice de skills completo (260 skills)»
gradle spotlessCheck testBarrido (todos los módulos)  BUILD SUCCESSFUL, RC 0
```

## No cubierto

- **Integración entre servicios reales.** Garantia/entregas/aportes, inversiones y nucleo-financiero se probaron contra dobles de contrato; ninguna saga completa con trazas reales ni `correlationId` cruzando servicios.
- **Caída real de un dependido** (apagar el contenedor, regla 98.8.4): solo dobles en tres niveles.
- **Autorización por grupo de `ENTREGA_EJECUTAR`**: es de plataforma; no hay chequeo de que quien llama pertenezca al grupo más allá de la coherencia periodo/grupo/turno. Riesgo residual declarado por el carril C.
- **Sin circuit breaker** en `RecaudoPorHttp`, `RespaldoPorHttp` ni `HechosPorHttp` (hay timeout y se deniega por omisión); regla 98.2.3 queda como deuda.
- **Retención fiscal de retiros (ISR/IVA), QR interoperable, webhook de entrada propio** (la confirmación sale de una respuesta firmada consultada por el servidor), carga y rendimiento, mutación.
- **Un E2E de toda la app**, lectores de pantalla, cross-browser; 21 de 25 capturas del carril E sin inspeccionar.
- **Permisos reales del rol `svc_grupos`** (sin superusuario) para la sustitución de administrador: solo con el rol de las pruebas.
- **Formato spotless**: verificado al final con `spotlessCheck`; tres archivos de prueba hubo que reformatearlos con `spotlessApply` (solo formato) y por eso se repitió la corrida completa de inversiones y nucleo.
- **Corrección**: una versión previa de este reporte decía que no había git; sí lo hay en `PasanakuBackend` y `PasanakuFrontend`. El trabajo se integró en una rama sobre `origin/test` (ver «Integración sobre test»).

## Desvíos del plan

- Se relanzaron los carriles A–E por el corte de RAM; se agregó el guardián `heavy.sh` y se serializó todo lo pesado (acordado con `resolver-crash-memoria-ram`).
- **Gates de repo en rojo por trabajo de los carriles**, resueltos aparte: 114 «prueba sin criterio en la bóveda» (se agregaron escenarios Dado/Cuando/Entonces a los CU-01, 10, 11, 12, 14, 60, 68, 69 y 90 y se alinearon `@DisplayName`, sin cambiar aserciones; 38 pruebas con prefijo permitido), 22 restricciones sin cita (R-BIL-21/22, R-DES-03/04/05, R-GAR-08…12, R-GRP-17/18/19, R-INV-01…08, R-ORG-08), 8 SQL concatenados en tests (parametrizados), 4 archivos sobre 300 líneas (partidos), cifras desfasadas en `planes/` y `README.md` (142→164 restricciones, 305→334 tablas, 99→105 casos de uso).
- **Pruebas nuevas no previstas** (la regla 20 pide registrarlas): `CU12QrRechazosTest.rechazaRBIL21`, `CU10RechazosTest.rechazaRBIL22`, `CU22PozoYMercadoRechazosTest` (3), `CU23RespaldoRechazosTest` (5), `rechaza por R-INV-04/06/08` (4), `HechosPorHttpTest` (7), `CU68MotorTest` (8), `CU68SustitucionTest` (10).
- **Código de producción corregido durante la reanudación**: `fail-on-unknown-properties: true` en nucleo-financiero (un campo fuera de contrato daba 500, ahora 400); `AdmisionDelGrupo` (enum de recomendación); normalización a UTC al leer la sustitución; `MercadoRepositorio` partido en `OfertaRepositorio` y `CesionRepositorio`.
- `InversionesOrdenesWebTest` e `InversionesPosicionesWebTest` se fundieron en `InversionesControllerWebTest` porque el gate de pruebas web busca `<Controlador>WebTest`. Ese límite de 300 líneas y ese nombre pueden volver a chocar si la clase crece.
- Se escribieron los **CU-120 a CU-125** y se actualizó el índice (105 casos): derivan del código y del ADR, con la inversión de grupos DESACTIVADA y todo parámetro marcado sintético; hay que validarlos con negocio.
- Se borraron los `hs_err_pid*`/`replay_pid*` del crash y las clases viejas fusionadas/partidas.

## Riesgos residuales

- **Publicación**: el usuario pidió subir todo a su servidor de TEST; el estado de esa publicación se registra al final de «Integración sobre test».
- Aserción débil en `rechazaRBIL21` (solo afirma el prefijo `ck_qr_`; no se verificó cuál de los dos checks dispara primero) y aserción `isNotBlank()` en el INSERT parametrizado de CU-90: no se puede comprobar que el motivo sea idéntico al previo.
- `docs/Restricciones.md` sigue apuntando R-INV-01…08 al ADR de evidencia en vez de a los CU-120…125; el regex de `verificar_criterios` y `verificar_boveda` captura «DR-INV-0x» como «R-INV-0x» (falta un lookbehind `(?<![A-Z])`).
- Cifras viejas sin gate propio: `docs/Restricciones.md` (frontmatter `total_restricciones: 138`), `docs/Stack.md:122` («138») y `docs/Views/AportaYa-Maqueta.html` («142»).
- `verificar_boveda`: el índice de skills está desactualizado (260 skills sin listar; `.claude/skills/README.md` no cambia desde el 22 de septiembre). Previo a este trabajo. `verificar_carriles.py` además da «sin carril dueño: inversiones» y 194 skills huérfanas.
- Un `sagaTest` de inversiones falló una vez por `BindException` (puertos efímeros agotados por conexiones sin pool de pruebas anteriores) y pasó 6/6 al repetir: clasificado `ENVIRONMENT`, no maquillado.
- Un error de socket aislado (`ConnectionAbortedError 10053`) en el Python del aliado simulado, no reproducido en 9 corridas.
- Los contenedores `datacenter-staging-postgis`, `-minio` y `plataforma-keycloak` arrancan solos con Docker Desktop; no son del proyecto y se bajan al apagar Docker.
- Los avisos de `verificar_seguridad`: S-8 (24 permisos que un CU exige y el catálogo no tiene) y S-9 (3 propósitos de token sin canal activo).

## Integración sobre test (2026-10-09)

El trabajo se hizo sobre una rama 240 commits detrás de `origin/test` y arrastraba un commit marcado «no publicar» (`396629da wip(F-04)`), que NO se incluyó. Se integró en una rama nueva creada desde `origin/test` (`justin/feature/cierre-endurecimiento-2026-10-08`) con `git apply --3way`: 35 archivos en conflicto (nucleo-financiero, grupos, identidad, aportes, esquema y docs), resueltos conservando los arreglos de Pablo que ya corren en TEST (B21, B33, `comoSistema`, Gmail API, escáner Atlas, gateway fail-fast) y el comportamiento de los carriles. Decisiones por área en `evidencia/merge/*.md`.

Verificación del árbol combinado (guardián de RAM, Postgres desechable con el esquema fusionado):

```text
aportes            test=27 webTest=32 integrationTest=76  testBarrido=2
auditoria          test=16 webTest=27 integrationTest=40  testBarrido=2
cumplimiento       test=5  webTest=20 integrationTest=276 testBarrido=2
entregas           test=10 webTest=45 integrationTest=94  testBarrido=2
erp                test=5  webTest=19 integrationTest=82  testBarrido=2
garantia           test=20 webTest=43 integrationTest=144 testBarrido=2
grupos             test=23 webTest=62 integrationTest=183 testBarrido=2
identidad          test=12 webTest=59 integrationTest=120 testBarrido=2
inversiones        test=36 webTest=25 integrationTest=85  contractTest=17 sagaTest=6 testBarrido=2
notificaciones     test=5  webTest=17 integrationTest=60  testBarrido=2
nucleo-financiero  test=54 webTest=32 integrationTest=340 testBarrido=2
organizador        test=5  webTest=35 integrationTest=105 testBarrido=2
publicidad         test=5  webTest=16 integrationTest=52  testBarrido=2
tarifas            test=5  webTest=28 integrationTest=96  testBarrido=2
transparencia      test=7  webTest=30 integrationTest=120 testBarrido=2
plataforma/comun-pruebas: contractTest, test y testBarrido en verde
frontend: backoffice 373 pruebas + 39 a11y; web 64 + 6 a11y; simulado 231; tsc y lint limpios
```

Todo con 0 fallos, 0 errores, 0 omitidas. Un rojo real apareció y se corrigió: `RutaEnPrefijoContratoTest` fijaba 14 servicios y ahora son 15 (`inversiones`). Gates de Python: `verificar_boveda`, `verificar_seguridad` y `verificar_pruebas_web` en verde; `verificar_criterios` da 16 fallas (CU-08, 22, 62, 63, 64, 68) que **ya existían en `origin/test`** (17 en la base limpia); no se agregó ninguna.

Del frontend se descartaron por duplicar lo que Pablo ya tiene en `test`: el gateway fail-fast, su spec, `main.ts`, `server.ts` y `app.config.ts` de web, la prueba de arranque en producción y el arreglo de `yarn humo`. Se conservaron las pantallas de admisión (backoffice), invitación (web) y los ejemplos del mock (49 creados).

Deuda de seguridad PREEXISTENTE en `test` (no introducida aquí): `sql/00_base/03_permisos.sql` aplica `SECURITY DEFINER` a todas las funciones del esquema `aportes` salvo `fn_seg_*`; la corrección sugerida (lista explícita, `REVOKE` a `PUBLIC`, `search_path` fijo) quedó documentada en `evidencia/merge/esquema-y-docs.md`.

Decisiones abiertas del merge (detalle en `evidencia/merge/`): la admisión humana convive con dos puertas de upstream que la saltean (aceptar por enlace crea el participante `ACTIVO`; `decidirSolicitudDeIngreso` deja decidir al organizador); conviven dos puertos de proveedor de retiros (`ProveedorDeRetiro` de upstream y `ProveedorDeRetiros` nuestro); `CotizadorPorHttp.cotizar` sin reintento ni circuit breaker. Cuando el retiro choca con lo de Pablo, gana lo de `test` (estado inicial `AUTORIZADA`; replay con otro costo devuelve el costo almacenado).

## Decisiones y ambigüedades

| Tema | Supuesto tomado | A quién confirmarlo |
|---|---|---|
| Sustitución de administrador por revocación/suspensión | La decide una persona de backoffice; no es automática | Negocio y cumplimiento |
| Retirar al saliente del grupo | Es un acto aparte (CU-65), la sustitución solo mueve la administración | Negocio |
| «Apartamiento» | Aceptar pese a alerta/SIN_DATOS o rechazar cuando el motor recomienda aceptar; solo se registra | Cumplimiento |
| Tarifa de cobertura, recuperación frente al miembro, cargos de oferta (A1, A3, A9 de PLAN-CARRIL-C) | Sin fuente: sintéticos | Dirección comercial y tarifas |
| Tope de capacidad empresarial, cobertura parcial (A2, A4) | Sin fila de capacidad se deniega; la cobertura hoy es todo o nada | Dirección financiera |
| Cuentas contables de la cobertura (A5) | Cuentas lógicas en el evento | Contabilidad |
| Aliado, tasas, plazos, tratamiento fiscal, comisión de éxito, custodia (DR-INV-01…06) | Todo sintético, `apto_produccion=false` | Dirección, legal, fiscal, aliado |
| Retención fiscal de retiros, QR interoperable | No se afirma cumplimiento ni interoperabilidad | Dueño de negocio y cumplimiento |
| Disparo `Idempotency-Key` en sincronización y devengo de inversiones | Se exige pero no se usa; son idempotentes por contenido | Carril de inversiones |

## Tercera vuelta (2026-10-10): cierre verificado desde XML

Conteo desde `build/test-results/*.xml`: 3307 pruebas en 79 suites, 0 omitidas, 0 fallas, 0 errores (script `scratchpad/xml_conteo.py`).
Pasan a HECHO (15): H10.S1.M1–M7 y H11.S1.M3–M5 (inversiones sobre el esquema actual, libro real por pacto; aliado y parámetros sintéticos DR-INV-01..07), H3.S1.M3, H6.S1.M1, H8.S1.M1 (productor real == archivo dorado; integración cliente real↔servidor real queda para e2e compose), H5.S1.M7 (interruptor manual/automático; decisión de Negocio abierta), H13.S1.M2.
Siguen A MEDIAS por falta de insumo ajeno: H4.S1.M6 (lado cliente), H9.S1.M5 (nucleo contra doble de entregas), el resto de A MEDIAS de frontend/aceptación real.
Aceptación con dos usuarios: modo mock verde (18/18 backoffice, 1/1 web); modo real BLOQUEADO hasta correr sobre el stack de D3 (ver `evidencia/carril-E3` y `evidencia/carril-D3/ENTORNO-E2E.md`).
Causa raíz del 503 (reproducida, `evidencia/carril-D3/FASE1.md`): el job de esquema falla sobre la base poblada (columna `consumidor_id` ausente, CHECK viejo) y `QR_INTERNO_CLAVE` sin cargar tumba nucleo-financiero. Corregido en `despliegue/aplicar-esquema.sh` y `scripts/generar_compose.py`.

### Carril F4 (cuarta vuelta)
Frontend: simulado 258, backoffice 457 + 47 a11y, web 74 + 8 a11y, aceptación mock 21 (166 capturas, 0 violaciones axe). Todo VERIFIED_FUNCTIONAL_ONLY (doble/Prism). Pasan a HECHO H12.S1.M1 y H12.S1.M3 (contra doble; falta backend real). A MEDIAS: H12.M2 (cifras vs mayor real), H12.M4 (lector de pantalla/otros navegadores), H4.M6 (correr test Dart), H3.M4 (recorrido real), H1.M3 (ADR-051 propuesto, falta aceptación de producto), H14.M4/M5 (corrida contra backend = G). Defecto de producto corregido: `ng build` del sitio fallaba por la ruta de invitación. Pendiente erp: `pagarFacturaDeProveedor` exige `autorizadoPor` que el cliente no manda (decisión de seguridad; derivarlo del token).

### Carril G (cuarta vuelta): integración real
Corrida real (stack mínimo + Kafka, tokens reales): 121 PASS, 2 FAIL, 5 GAP (`evidencia/carril-G/integracion-dinero.log`, script `despliegue/ensayos/integracion/correr.sh dinero`). Pasan a HECHO: H8.M3, H8.M4, H9.M5, H13.M6, H14.M2, H14.M3. H14.M1 queda A MEDIAS: ciclo feliz con F02,F03,F05,F08,F09,F15–F21 fuera del conjunto mínimo.
Defectos reales arreglados con prueba de regresión: relevo del outbox que nunca arrancaba (ningún evento llegaba), tópicos Kafka sin crear, CU-21 reintento concurrente (422), CU-13 retener repetido (500), canje concurrente de invitación (500).
ABIERTOS con dueño: CU-18 ningún participante registra cuenta de destino (identidad+entregas+Seguridad); `historialAdmision` con @Permiso("PARTICIPANTE") da 403 a ADMIN_PLATAFORMA (grupos+roles); `aportes.aporte_cobrado` no mueve billetera ni mayor (aportes+nucleo); grupos no consume `entregas.derecho_cedido`; DECISION_REQUIRED: vendedor cobra precio−cargos (entregas) vs precio completo (nucleo) — Dirección comercial/tarifas; garantia necesita actor con GRUPO_ADMINISTRAR+RECLAMO_VER (Seguridad).
Aceptación real en navegador: BLOQUEADA por RAM (necesita ≥6 GB libres): `bash despliegue/ensayos/integracion/correr.sh aceptacion`.

### Carril H (cuarta vuelta): app móvil en emuladores
VERIFIED_FUNCTIONAL_ONLY contra un doble con estado (Prism + sesiones/invitaciones en memoria), NO contra el backend real. Dos emuladores Android (aportaya_a, aportaya_b) corridos en sesiones separadas (no entran a la vez en RAM). A: login, crear grupo, invitar; B: abre enlace, ingresa, acepta; rechazo = invitación revocada (por API del doble). 66 capturas en `evidencia/carril-H/` inspeccionadas. 13 defectos corregidos en apps/movil (host 10.0.2.2, http en debug, «Crear grupo» nunca habilitado, sin acceso a crear/invitar, subrayado amarillo por falta de Material, éxito de invitación, importes crudos, enlace viejo con app abierta, prefijo +591, tests rotos). flutter test 277 pasan / 9 fallan (goldens de otra plataforma, ENVIRONMENT). No cubierto: recorrido contra stack real, integration_test Patrol, cámara, botón Compartir (falta plugin), tema oscuro salvo pantalla de rechazo, pantalla de miembros para el organizador (no existe). APK: `Descargas\APK-Pasanaku\aportaya-test-3-debug.apk` (apunta a 10.0.2.2:4010: no sirve contra TEST).

### Carril K (quinta vuelta): erp
`pagarFacturaDeProveedor`: `autorizadoPor` sale del token (ctx.usuarioId); fuera del contrato (400 si viene en el cuerpo). erp test 5 / webTest 26 / integrationTest 83 / barrido 2, 0 fallas; backoffice vitest 457/457; simulado 259/259. ABIERTO: idempotencia del pago (falta `clave_idempotencia` UNIQUE en `pago_a_proveedor`; dueño erp), `aprobadaPor` de la factura sigue dictado por el cliente (Contabilidad+Seguridad), forma de pago QR admitida por contrato pero no por la restricción (Tesorería/producto). 19 rutas del frontend sin contrato: 11 de erp/publicidad requieren construir endpoints de listado (inventario con dueño en `evidencia/carril-K/`).

### Carril I (quinta vuelta): peldaño WRITTEN + javac + ensayo en seco con rollback; NO TESTED por Gradle ni VERIFIED en stack (RAM)
Escrito: consumidor `aporte_cobrado` en nucleo (dedup por evento y pago, asiento cuadrado, saldo desde el libro), `derecho_cedido` en grupos, `@PermisoAlternativo` para `historialAdmision`, SERVICIO_ENTREGAS verifica titularidad en identidad. Pendiente de ejecución: test/webTest/integrationTest/testBarrido de comun-web, grupos, nucleo, aportes, entregas, identidad y recorridos reales (`evidencia/carril-I/INFORME.md`). H14.M1 sigue A MEDIAS.
