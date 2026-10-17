# Reporte — Tableros por rol, catálogos, pruebas con Faker y OpenTelemetry

> **AVANCE: 78 / 90 — 86,7 %.** (2 descartadas con motivo, 2 a medias, 7 bloqueadas por la pila compartida con su simulación declarada, 1 por hacer: los PRs)

- Fecha: 2026-10-16 → 2026-10-17 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `pablo/feat/tableros-por-rol-2026-10-16`
  (worktree `../Pasanaku-tableros`, desde `origin/dev` `d63e4fef`)
- Peldaño de evidencia alcanzado, por área:
  - Backend (H1–H8): `TESTED` — regresión completa (4.009 pruebas, todos los runners y pisos de cobertura); los tres
    rojos eran preexistentes o de entorno y quedaron resueltos o demostrados (ver H9.S1.M1).
  - Frontend (tres tableros y catálogo): `VERIFIED_FUNCTIONAL_ONLY` **contra un backend doble** — E2E de 16 recorridos y
    52 capturas inspeccionadas (3 viewports × 2 temas, con desborde y contraste medidos); falta contra el backend real.
  - Punta a punta entre servicios (Kafka, OpenTelemetry): verificado por pactos de los dos lados; **sin corrida real**
    (BLOQUEADO: levantar la pila pisaría la de otra sesión).
  - Peldaño del trabajo (el más bajo de sus áreas): `TESTED`.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H0.S1.M1 | Mapa operación → asiento del mayor | `grep -rln -iE "asiento|registrarAsiento|contabiliz" servicios/*/src/main/java` | PASS — `evidencia/H0-01-asientos.md` |
| H0.S1.M2–M6 | Patrón de lectura con `@Permiso`, tablas de billetera, A5 resuelta, guardias del backoffice y versiones verificadas | ver evidencia | PASS — `evidencia/H0-02-a-06.md` |
| H1.S1.M1 | Disparo diario `RecargosDeMoraDiarios` (`@Scheduled` + ShedLock, contexto de sistema) | `./gradlew :servicios:aportes:integrationTest --tests '*CU21*' --tests '*ArranqueTest'` | PASS — 11 suites de CU-21 con 0 fallas + `ArranqueTest` 2/2; `evidencia/H1-S1-recargos-diarios.txt` |
| H1.S1.M2 | Dos corridas el mismo día → un solo recargo | ídem (`dosCorridasElMismoDia`) | PASS |
| H1.S2.M0a | `aportes.aporte_cobrado` publica `tipo` | `./gradlew :servicios:aportes:integrationTest --tests '*CU21*'` | PASS — `evidencia/H1-S2-M0a-tipo-en-evento.txt` |
| H1.S2.M0b | `nucleo-financiero` lee `tipo` opcional (compatibilidad con el productor viejo) | `ConsumidorDeAportesKafkaTest` + consumo de aportes | PASS — `evidencia/H1-S2-M0b-consumidor-tipo.txt` |
| H1.S2.M0c | Billetera de sistema `FONDO_GARANTIA` por moneda, sembrada | siembra doble en Postgres descartable (855 → 0 filas) | PASS — `evidencia/H1-S2-M0c-semilla-fondo.txt` |
| H1.S2.M0d | `RECARGO_MORA` en `TipoTransaccion` y `TipoMovimientoFondo`, con `CK_AMPLIADOS` | `generar_ddl.py` + reaplicar sobre base existente + gates del CI | PASS — `evidencia/H1-S2-M0d-esquema-recargo.txt` |
| H1.S2.M1 | El recargo cobrado va a la billetera del fondo; asiento DEBE `SALDO_USUARIOS` / HABER `RESERVA_RESPALDO`; el pozo no se mueve | `CUConsumoDeRecargoTest` + regresión de nucleo | PASS — `evidencia/H1-S2-M1-recargo-al-fondo.txt` |
| H1.S2.M1b | Sin billetera del fondo: AP-CU12-02 sin marcar (no cae en el pozo) | `sinBilleteraDelFondo` | PASS |
| H1.S2.M1c | `garantia` anota el ingreso en el fondo del grupo | `CUIngresoAlFondoTest` + regresión de garantia | PASS — `evidencia/H1-S2-M1c-garantia-anota.txt` |
| H1.S2.M2 | Idempotencia de punta a punta (duplicado y republicación) | tests `duplicado`/`reintento` en nucleo y garantia | PASS |
| H1.S2.M4 | El reverso de un recargo se descuenta del fondo en `garantia` | `CUIngresoAlFondoTest` 8/8 | PASS — `evidencia/H1-S2-M4-reversa-en-garantia.txt` |
| H2.S1–S3 | Permisos de tablero y rol `COMERCIAL`; volumen sintético con Faker (12 clones); `DatosSinteticos` (Datafaker, semilla fija) | siembra doble, sha256 de dos corridas, 4/4 | PASS — `evidencia/H2-*` |
| H3.S1.M1-M2 | Contrato `Indicador` (null no es cero, tasa con num/den, muestra mínima) | 8/8 · cobertura de comun-dominio | PASS |
| H4.S0.M1-M4 | 35 definiciones; publicador por outbox; `auditoria` guarda en `indicador_kpi`; CU-98 filtra familias por permiso | siembra 36→0 · tests web con 403 | PASS |
| H4.S1.M1-M7 | Calculadores por servicio dueño (nucleo, garantia, aportes, grupos, entregas, inversiones) con disparo diario y relleno | suite completa y cobertura de cada servicio | PASS |
| H4.S2.M1-M2 · H5.S1 · H5.S2.M1 | Tableros financiero y comercial en el backoffice; indicadores comerciales en identidad, grupos y aportes | `yarn --cwd apps/backoffice test:front` / `test:a11y` | PASS |
| H6.S1.M1 · H6.S2.M1 · H6.S3.M1-M3 | OpenTelemetry (bridge OTel, exportadores OTLP), Collector + Prometheus en compose, endpoints del día en auditoria, tablero de sistemas | tests de auditoria + `test:front` 798 / `test:a11y` 68 | PASS — `evidencia/H6-*` |
| H7.S1.M1-M7 | Catálogo de datos: lector de fichas, `COMMENT ON` con narrativa (386/386 tablas comentadas en una base aplicada), gate de narrativas (337/337 tablas con ficha; trinquete de 524 columnas), 66 fichas nuevas, catálogo generado determinista, `GET /indicadores/catalogo-de-datos` con permiso, gate en el CI | `verificar_narrativas.py` · `psql obj_description` · `shasum` ×2 · webTest | PASS — `evidencia/H7-S1-*` |
| H7.S2.M1 | Sistemas → Catálogo de datos (lista con búsqueda normalizada, filtro y página en la URL; ficha con sensibilidad en palabras y relaciones navegables) | `ng test` del catálogo 21/21 · `test:front` 817 · `test:a11y` 70 | PASS — `evidencia/H7-S2-M1-pantalla-catalogo.txt` |
| H8.S1.M1-M3 | Formato y validador del catálogo de flujos; matriz de pruebas por endpoint (282) que reusa `inventario_endpoints.py` | `validar_flujos.py --self-test` · `matriz_de_pruebas.py` | PASS — `evidencia/H8-S1-*` |
| H8.S2.M1-M3 | Árboles de decisión de los tres flujos de dinero (aporte → mora → cobertura → recuperación; entrega del turno; recarga y retiro) | `python3 scripts/validar_flujos.py` → 3 OK | PASS |
| H8.S3.M1 | Pactos productor/consumidor de los eventos que H1 cambió (`aporte_cobrado`, `aporte_aplicado`, `transaccion_reversada`), verificados de los dos lados | 6 pruebas en aportes, nucleo y garantia | PASS — `evidencia/H8-S2-M1-S3-M1-aporte-mora-cobertura.txt` |
| H8.S3.M2 | Cadena de la entrega con el asiento cuadrado (prueba existente, ejecutada) | `CU133*` + `CU28Test` 26/26 | PASS |
| H8.S3.M3 | `CUFlujoRecargaYRetiroTest`: recargar → confirmar → retirar → pagar con Datafaker; retirar todo; un centavo de más (AP-CU11-01) | integrationTest 3/3 | PASS |
| H8.S3.M4 | Prueba web de los 11 endpoints de dinero sin cubrir: aportes, garantia, entregas y nucleo quedan 75/75 | webTest de los 4 servicios: 46+50+79+60, 0 fallas | PASS — `evidencia/H8-S3-M4-pruebas-web-dinero.txt` |
| H9.S0.M1 | (Agregada) Las tarjetas de acceso con dos segmentos llevaban a `/tablero%2Ffinanciero`; ahora a la ruta real | spec en rojo antes, verde después; E2E «acceso» | PASS — `evidencia/H9-S0-M1-enlaces-de-acceso.txt` |
| H4.S2.M4 · H7.S2.M2 | Prueba visual de los tres tableros y del catálogo; corrigió desborde en móvil y tablet, orden del tablero de sistemas, cortes de ruta y Markdown crudo | `yarn test:e2e tableros-por-rol.e2e.ts --retries=0` 16/16 | PASS (backend doble declarado) — `evidencia/H9-visual/INFORME.txt` |
| H9.S1.M1 | Regresión del backend | `./gradlew test webTest integrationTest contractTest sagaTest jacocoTestCoverageVerification` | 4.009 pruebas; rojos preexistentes/entorno resueltos tras el merge de `dev` — `evidencia/H9-S1-M1-regresion-backend.txt` |
| H9.S1.M2 | Regresión del backoffice | `test:front` 818 · `test:a11y` 70 · lint · build | PASS — `evidencia/H9-S1-M2-regresion-backoffice.txt` |
| H9.S1.M3 | Gate de seguridad y privacidad | revisión contra 90.6 | PASS — sección de abajo |

## A medias

### H1.S2.M3 — Reversa del recargo
- Qué anda: CU-14 sobre una transacción `RECARGO_MORA` devuelve Bs 25 a la billetera del participante y los descuenta de la del fondo, con una transacción `REVERSO`; `garantia` resta lo anotado (H1.S2.M4). Pacto del reverso verificado de los dos lados (H8.S3.M1).
- Qué no anda: CU-14 no genera contra-asiento en el mayor, para ninguna transacción (preexistente: `CU14ReversarTransaccion` no llama a CU-24).
- Qué falta exactamente: decidir si CU-14 asienta el espejo para todas las transacciones; cambia el comportamiento de todas las reversas y está fuera de este trabajo.
- Dónde quedó: rama `pablo/feat/tableros-por-rol-2026-10-16`; compila; tests en verde.

### H6.S1.M2 — Configuración común de OpenTelemetry y redacción
- Qué anda: `DefaultsDeObservabilidad` fija recurso, muestreo e histogramas; el Collector redacta atributos sensibles (configuración validada).
- Qué no anda: no se vio ningún span real pasar por la redacción.
- Qué falta exactamente: levantar la pila con el perfil `observabilidad`, generar tráfico con un documento en un atributo y mirar el span en el Collector (H9).
- Dónde quedó: `plataforma/comun-web/.../observabilidad`, `despliegue/observabilidad/otel-collector.yml`; compila.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| H4.S1.M8 | BLOQUEADO | Pila arriba: calculadores → Kafka → `auditoria` → `GET /indicadores`. Simulado con el pacto `servicios-auditoria-indicador-calculado.json` (productor y consumidor en verde) |
| H4.S2.M3 · H5.S2.M2 · H6.S3.M4 | BLOQUEADO | E2E contra el backend real sembrado. Simulado: 16 recorridos con `auditoria` como doble (datos, vacío, cargando, 500/403) |
| H6.S1.M3 · H6.S2.M3 | BLOQUEADO | Series reales en Prometheus y apagar el Collector con la pila arriba. Simulado con `PrometheusDobleTest` y el estado «sin fuente» |
| H6.S2.M2 | BLOQUEADO | Jaeger en dev (opcional, sin doble posible) |
| H9.S1.M4 | TODO | PR a `dev` y PR a `test`, CI en verde y merge |
| H3.S1.M3 · H3.S1.M4 | DESCARTADO | Redundantes tras el reencuadre en CU-98 (decidido por el agente con evidencia; confirmar con Pablo) |

**Por qué está bloqueado lo que necesita la pila:** el compose fija los nombres `aportaya-postgres` y `aportaya-interna`, y
ese contenedor ya corre para otra sesión (worktree `backoffice-dominio`, 25 h arriba). Levantar la pila desde acá la
recrearía con otro esquema: es una acción destructiva sobre algo compartido, que decide Pablo.

## Evidencia

- Una salida por microtarea en `evidencia/` (índice: los nombres llevan el ID de la microtarea).
- Decisiones D1 a D12 tomadas con Pablo el 2026-10-16; D13 a D15 tomadas en la ejecución por evidencia (PLAN).
- Patrón de referencia de Atlas (`AtlasDashboardsBackend`, `AtlasBackend/docs/endpoints/workflow-catalog.md`,
  `AtlasBackend/docs/data/entity-catalog.md`, `AtlasBackend/docs/observability/01-architecture-design.md`).
- Matriz de pruebas por endpoint: `docs/catalogo-de-flujos/matriz.md` (282 endpoints; 220 con prueba web propia; 219
  con CU; 205 con prueba de integración del CU; 16 en algún flujo).
- Catálogo de datos: `docs/catalogo-de-datos.md` y `servicios/auditoria/src/main/resources/catalogo/catalogo-datos.json`.
- Catálogo de flujos: `docs/catalogo-de-flujos/{aporte-mora-cobertura,entrega-del-turno,recarga-y-retiro}.yaml`.

```text
$ python3 scripts/validar_flujos.py
  OK    · aporte-mora-cobertura.yaml: 12 pasos, 6 validos, 7 limite, 7 invalidos
  OK    · entrega-del-turno.yaml: 7 pasos, 4 validos, 5 limite, 6 invalidos
  OK    · recarga-y-retiro.yaml: 6 pasos, 3 validos, 5 limite, 6 invalidos
  AVISO · recarga-y-retiro.yaml: paso `instruir-pago` SIN CABLEAR — H8-01 …
Sin fallas.

$ python3 -I scripts/verificar_narrativas.py
  OK    · tablas con ficha: 337/337
  OK    · columnas con explicación de negocio: 524/3680 (piso 524)
```

## No cubierto

- Nada se ejercitó todavía con la pila completa: ni Kafka real entre servicios, ni spans de OpenTelemetry, ni los
  tableros contra datos sembrados en el navegador. Los saltos entre servicios están probados por pacto (los dos
  lados contra el mismo archivo), no con los procesos juntos.
- La prueba de la entrega usa `MayorDeDoble` en vez de `nucleo-financiero` real; la de recarga y retiro usa los
  proveedores locales del perfil test.
- `RecargosDeMoraDiarios` se probó llamando a `correr()`, no esperando al reloj: el cron y el lock de ShedLock entre
  réplicas no se ejercitaron.
- Narrativa de negocio por columna: 524 de 3680 (14 %). El resto tiene descripción estructural y de sistema (A8).
- Hallazgo H0-05 (fuera de alcance): `guardia-rol-sistemas.ts` admite los roles `PLATAFORMA` y `SEGURIDAD`, que no
  existen en el catálogo.

## Desvíos del plan

- H1.S2.M0c: el primer diseño (una billetera del fondo por grupo) violó R-BIL-05 y rompió 7 tests existentes. Se
  revirtió y se rediseñó como billetera de sistema por moneda, que es lo que el modelo define.
- H1.S2.M1b: sin billetera del fondo se rechaza sin marcar (AP-CU12-02), siguiendo el patrón de la billetera del grupo.
- Pruebas de CU-21 renombradas con los prefijos que acepta `verificar_criterios.py` (el primer intento dejó el gate en
  rojo; corregido). Lo mismo con la prueba de pacto de `CU21EventoDeAporteTest`.
- **D13 (H7):** la narrativa de negocio se lee de las fichas existentes de `docs/entidades/*.md` en vez de agregar una
  sintaxis nueva al PlantUML (habría dos fuentes de verdad). Los módulos 13 y 14 ya tenían su narrativa por grupo de
  tablas: se extendió el lector en lugar de reescribirla.
- **D14 (H7):** el JSON del catálogo NO va a `apps/backoffice/public/` (archivo estático sin sesión que publicaría el
  esquema completo); lo sirve `auditoria` en `GET /indicadores/catalogo-de-datos` con `TABLERO_SISTEMAS_VER`. Se puso
  bajo `/indicadores` porque el gateway ya enruta ese prefijo; el primer intento (`/catalogo-de-datos`) no lo habría
  enrutado.
- **D15 (H8):** el inventario ya relevaba 282 endpoints (el plan decía 162) y se reusó. Las «pruebas de integración de
  flujo» son la cadena de cada servicio dueño contra Postgres más los pactos entre servicios; la cadena con todos los
  procesos juntos queda para H9.
- H8.S3.M2: no se escribió una prueba nueva de la entrega; la cadena existente (`CU133EntregaConsistenteTest`) ya cubre
  fondeo → banco → asiento y se ejecutó.

## Riesgos residuales

- **H8-01 (dinero, preexistente):** en producción nada instruye al proveedor un retiro AUTORIZADO: `RetirosConProveedor`
  y `CU11RetirarSaldo.instruirPago` no tienen llamador y `retiro_autorizado` no tiene consumidor. Una orden queda
  retenida sin pagarse. Declarado `sin-cablear` en el catálogo de flujos (el validador lo avisa). A9.
- **H8-02 (preexistente):** nada lleva un grupo a `FINALIZADO`. `RETENCION_60D` (tablero comercial) cuenta grupos
  FINALIZADOS: con datos reales no tendrá base y saldrá «sin datos». La reserva del ciclo y el fondo se liberan y
  devuelven por operador. A10.
- **Pactos que faltaban (corregido en este trabajo):** H1 cambió `aporte_cobrado` y `aporte_aplicado` y agregó un
  consumidor de `transaccion_reversada` sin pacto (regla 98.1.4). Se agregaron en H8.S3.M1.
- **`proponerReemplazo` responde 200 en el reintento** y el contrato solo declara 201/422 (preexistente; está entre las
  «operaciones con el estado HTTP fuera de su contrato» que lista `verificar_pruebas_web.py`).
- **Fallas preexistentes:** `CU04CodigoPorCorreoTest`, el lint de `pantalla-de-expedientes.ts` y la cobertura de
  `comun-datos` venían de la base y los corrigió `dev` (traído con el merge `00183992`). Sigue sin corregir
  `verificar_rutas_frontend.py` con la lista `CONOCIDAS_SIN_CONTRATO` desactualizada (reproducida sin estos cambios).
- **Entorno:** las pruebas de contrato del aliado simulado de `inversiones` lanzan `python`; en esta máquina solo hay
  `python3` (con un alias pasan 23/23). Hay que fijarlo en el CI o en el script.
- **Capturas versionadas:** la prueba visual deja 14 MB en `evidencia/H9-visual/` (hay precedente: el carril F2 dejó 5 MB).
- El selector de mes nativo de Chromium sale en inglés y `EstadoDePantalla` ofrece «Volver a intentar» ante un 403 (ajenos).
- **Semilla sin historial de grupos ni retiros/recargos/comisiones por transacción:** `GRUPOS_COMPLETOS`,
  `DIAS_LLENADO_GRUPO`, `RETENCION_60D`, `SALDO_RETIRADO` e `INTERESES_MORA_COBRADOS` salen sin datos con la base
  sembrada.
- **`provisorio` siempre falso:** se emiten solo períodos cerrados (A7).
- **`DatosSinteticos` y la prohibición 6:** usa `RandomGeneratorFactory` + `Random.from` con semilla (datos de prueba,
  no seguridad); revisar con seguridad si se prefiere una excepción explícita en `verificar_seguridad.py`.
- **Dos commits entraron con un gate en rojo** y se corrigieron en el siguiente (`665c3e0a` → `99b4f803`,
  `3eeec2d8` → `0d596e11`). Desde entonces el commit va encadenado al verde.
- **H1-02:** el único camino de dinero hacia el fondo de garantía es `RECARGO_MORA`. `APORTE_FONDO_GARANTIA` y
  `REPOSICION_COBERTURA` siguen cayendo al pozo del grupo (H0-01): decisión aparte.
- **Desplegar H1.S1 solo junto con H1.S2**, y con `RESERVA_RESPALDO` mapeada en el entorno. Sin mapeo, el pago del
  recargo se rechaza (AP-CU24-02) y queda reprocesándose.
- **H1-03:** el índice `uq_cuenta_billetera_titular_moneda` que nombran `scripts/generar_errores.py` y
  `DestinoRepositorio` no existe en `sql/`. Solo se anota.
- El catálogo de datos (1,7 MB) viaja entero en una respuesta; si crece, conviene compresión o paginado.
- El patrón de Atlas (un servicio de reporting que lee las bases hermanas) no se copió: contradice la regla 98.1.1.

## Gate de seguridad y privacidad (regla 90.6)

| Cambio sensible | Amenaza considerada | Control (dónde vive) | Prueba negativa | Resultado | Riesgo residual |
|---|---|---|---|---|---|
| `GET /indicadores` por rol | Un rol ve familias de indicadores que no le tocan (BFLA) | `FamiliasVisibles` filtra en el servidor por permiso; `@Permiso` + `@PermisoAlternativo` en `IndicadoresController` | `IndicadoresControllerWebTest`: 403 sin permiso de tablero; cada permiso pide SOLO sus familias | PASS (14/14) | La pantalla oculta lo que no corresponde, pero la barrera es el servidor |
| Privacidad de los indicadores | Una cifra con pocos casos identifica a una persona | Mínimo de casos por definición; `suprimidoPorPrivacidad` viaja sin valor | `IndicadoresControllerWebTest` «suprimido viaja SIN valor»; specs del tablero | PASS | El mínimo (5) es un parámetro de negocio sintético a confirmar |
| `GET /indicadores/operacion/endpoints` | Identificadores o datos de personas en las etiquetas de métricas o en la URL | Solo la ruta PLANTILLA (`/grupos/{id}`); el Collector redacta atributos sensibles (`otel-collector.yml`) | 403 con el permiso de otro tablero; spec «ruta plantilla» | PASS | La redacción no se vio con spans reales (H6.S1.M2 a medias) |
| `GET /indicadores/catalogo-de-datos` | Publicar el esquema completo de la base sin sesión (reconocimiento para un atacante) | D14: lo sirve `auditoria` con `TABLERO_SISTEMAS_VER`; NO es un estático del backoffice; solo estructura, sin datos de personas | `CatalogoDeDatosWebTest`: 403 con otro tablero sin leer el catálogo; sábana: 401 sin sesión | PASS (3/3 + 6/6) | Quien tenga el permiso ve nombres de columnas sensibles (no sus valores) |
| Recargo por mora al fondo (dinero) | Doble acreditación por reintento o evento duplicado; el recargo cae en el pozo | Idempotencia por evento (`Consumidos`) y por pago; `destino` en el evento; pactos | `CUConsumoDeRecargoTest` (duplicado, reintento, sin mapeo, sin billetera); `CUIngresoAlFondoTest` (duplicado, otra moneda) | PASS | Ver H1.S2.M3 (CU-14 no asienta el espejo) |
| Webhook de la pasarela (público) | Acreditar dinero con un webhook falsificado; filtrar por qué falló | HMAC + ventana (CU-100); 401 sin cuerpo | `PagosEnrutamientoYWebhookWebTest`: firma inválida → 401 vacío; firma mal formada → 400 sin llegar al caso de uso | PASS | — |
| Operaciones de billetera sin prueba web | Operar con el permiso de VER; campos de más (mass assignment); reintento sin clave | `@Permiso("BILLETERA_OPERAR")`; `fail-on-unknown-properties`; `Idempotency-Key` obligatoria | `BilleteraOperacionesWebTest`: 403 con BILLETERA_VER, 400 sin clave, 400 con campo no declarado | PASS (11/11) | — |
| Datos de prueba (Faker) | Datos reales de personas en semillas o pruebas | `scripts/generar_volumen_dev.py` y `DatosSinteticos`: sintéticos, deterministas, marcados | Revisión del generador; semillas con marca `volumen` | PASS (revisión) | — |
| Pantallas nuevas | Error crudo del backend o datos de personas en la URL | `EstadoDePantalla` traduce el error; la URL solo lleva búsqueda de nombres de tablas, esquemas y página | Specs y E2E: 403/500 no muestran el detalle interno | PASS | — |

## Decisiones y ambigüedades

Ver «Decisiones tomadas con Pablo» (D1–D12), las correcciones D13–D15 y las ambigüedades A1–A10 en el PLAN. Las que
esperan confirmación:

| # | Qué | Supuesto tomado | Confirmar con |
|---|---|---|---|
| A7 | Indicadores de períodos abiertos | Solo se guardan períodos cerrados | Pablo |
| A8 | Narrativa de negocio por columna | Ficha cuando existe, convención o estructural; trinquete | Pablo |
| A9 | Quién instruye un retiro autorizado (H8-01) | Nada; no se arregla aquí | Pablo y Tesorería |
| A10 | Qué finaliza un grupo (H8-02) | Nada; no se arregla aquí | Pablo |
| H3.S1.M3/M4 | Descartadas por el reencuadre CU-98 | Redundantes | Pablo |
