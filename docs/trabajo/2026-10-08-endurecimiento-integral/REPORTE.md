# Reporte — Endurecimiento integral (cierre tras el corte por falta de RAM)

> **AVANCE: 35 / 86 — 40,7 %.**

- Fecha: 2026-10-08 · Plan: [PLAN.md](./PLAN.md) · Notas: [NOTAS.md](./NOTAS.md) · Repos: PasanakuBackend (principal), PasanakuFrontend (carril E). Sin git en la raíz: no hay rama ni diff.
- Peldaño de evidencia alcanzado: **TESTED por servicio** (regla 30, peldaño 4), contra PostgreSQL real vía Testcontainers. **No** se alcanzó `REGRESSION_VERIFIED` global: falta integración real entre servicios y E2E de punta a punta.
- Conteo: `python .claude/hooks/plan_status.py` → HECHO=35, A MEDIAS=24, BLOQUEADO=10, TODO=17 (de 86). `A MEDIAS` cuenta como no hecha.
- Estado de la máquina al cerrar: sin `heavy.lock`, sin procesos java ni Docker, sin `hs_err_pid*`/`replay_pid*` (se borraron los restos del crash), unos 6,7 GB libres.

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
- **Qué cambió respecto del estado previo**: no hay git en la raíz, así que no existe un diff verificable; el inventario de archivos tocados sale de los reportes de cada carril.

## Desvíos del plan

- Se relanzaron los carriles A–E por el corte de RAM; se agregó el guardián `heavy.sh` y se serializó todo lo pesado (acordado con `resolver-crash-memoria-ram`).
- **Gates de repo en rojo por trabajo de los carriles**, resueltos aparte: 114 «prueba sin criterio en la bóveda» (se agregaron escenarios Dado/Cuando/Entonces a los CU-01, 10, 11, 12, 14, 60, 68, 69 y 90 y se alinearon `@DisplayName`, sin cambiar aserciones; 38 pruebas con prefijo permitido), 22 restricciones sin cita (R-BIL-21/22, R-DES-03/04/05, R-GAR-08…12, R-GRP-17/18/19, R-INV-01…08, R-ORG-08), 8 SQL concatenados en tests (parametrizados), 4 archivos sobre 300 líneas (partidos), cifras desfasadas en `planes/` y `README.md` (142→164 restricciones, 305→334 tablas, 99→105 casos de uso).
- **Pruebas nuevas no previstas** (la regla 20 pide registrarlas): `CU12QrRechazosTest.rechazaRBIL21`, `CU10RechazosTest.rechazaRBIL22`, `CU22PozoYMercadoRechazosTest` (3), `CU23RespaldoRechazosTest` (5), `rechaza por R-INV-04/06/08` (4), `HechosPorHttpTest` (7), `CU68MotorTest` (8), `CU68SustitucionTest` (10).
- **Código de producción corregido durante la reanudación**: `fail-on-unknown-properties: true` en nucleo-financiero (un campo fuera de contrato daba 500, ahora 400); `AdmisionDelGrupo` (enum de recomendación); normalización a UTC al leer la sustitución; `MercadoRepositorio` partido en `OfertaRepositorio` y `CesionRepositorio`.
- `InversionesOrdenesWebTest` e `InversionesPosicionesWebTest` se fundieron en `InversionesControllerWebTest` porque el gate de pruebas web busca `<Controlador>WebTest`. Ese límite de 300 líneas y ese nombre pueden volver a chocar si la clase crece.
- Se escribieron los **CU-120 a CU-125** y se actualizó el índice (105 casos): derivan del código y del ADR, con la inversión de grupos DESACTIVADA y todo parámetro marcado sintético; hay que validarlos con negocio.
- Se borraron los `hs_err_pid*`/`replay_pid*` del crash y las clases viejas fusionadas/partidas.

## Riesgos residuales

- **Sin commit, push ni despliegue.** El trabajo (≈75 archivos de Codex más lo de los carriles) está sin commitear y sin git en la raíz; nadie lo autorizó. Un peer pidió «desplegar en test»; no se hizo, solo el usuario puede pedirlo.
- Aserción débil en `rechazaRBIL21` (solo afirma el prefijo `ck_qr_`; no se verificó cuál de los dos checks dispara primero) y aserción `isNotBlank()` en el INSERT parametrizado de CU-90: no se puede comprobar que el motivo sea idéntico al previo.
- `docs/Restricciones.md` sigue apuntando R-INV-01…08 al ADR de evidencia en vez de a los CU-120…125; el regex de `verificar_criterios` y `verificar_boveda` captura «DR-INV-0x» como «R-INV-0x» (falta un lookbehind `(?<![A-Z])`).
- Cifras viejas sin gate propio: `docs/Restricciones.md` (frontmatter `total_restricciones: 138`), `docs/Stack.md:122` («138») y `docs/Views/AportaYa-Maqueta.html` («142»).
- `verificar_boveda`: el índice de skills está desactualizado (260 skills sin listar; `.claude/skills/README.md` no cambia desde el 22 de septiembre). Previo a este trabajo. `verificar_carriles.py` además da «sin carril dueño: inversiones» y 194 skills huérfanas.
- Un `sagaTest` de inversiones falló una vez por `BindException` (puertos efímeros agotados por conexiones sin pool de pruebas anteriores) y pasó 6/6 al repetir: clasificado `ENVIRONMENT`, no maquillado.
- Un error de socket aislado (`ConnectionAbortedError 10053`) en el Python del aliado simulado, no reproducido en 9 corridas.
- Los contenedores `datacenter-staging-postgis`, `-minio` y `plataforma-keycloak` arrancan solos con Docker Desktop; no son del proyecto y se bajan al apagar Docker.
- Los avisos de `verificar_seguridad`: S-8 (24 permisos que un CU exige y el catálogo no tiene) y S-9 (3 propósitos de token sin canal activo).

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
