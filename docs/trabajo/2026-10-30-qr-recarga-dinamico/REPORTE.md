# Reporte — QR bancario dinámico en la recarga, y ver subir el saldo

> **AVANCE: 14 / 15 — 93,3 %.** Regresión móvil con 353 rojos **preexistentes** (idénticos en `origin/test`).

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/qr-recarga-dinamico-20261030` (base `origin/test` `ebe0ceb0`)
- Peldaño de evidencia alcanzado: backend `TESTED` (+ deriva de esquema verificada contra el guion real de
  despliegue); app `TESTED` + prueba visual revisada. **No** `VERIFIED` de punta a punta: no se ejercitó contra
  el backend corriendo ni en TEST (ver «No cubierto»).

## Qué se pidió y qué quedó

- **Pedido:** «la app no muestra el QR bancario; que sea dinámico; de momento uno de prueba, pero todo listo
  para cuando conectemos la cuenta real del banco» + «que se vea cómo sube el dinero».
- **Decisiones de Pablo en la sesión:** pantalla = **Recargar billetera**; pago de prueba = **botón «Simular pago»**.
- **Causa de que no se viera el QR:** el contrato `SalidaRecarga` no tenía ningún campo de QR y la pantalla solo
  pintaba un reloj de arena; además, fuera del modo `simulado` el adaptador del proveedor rechazaba la recarga.
- **Ahora:** cada orden de recarga trae su **QR dinámico** (`cobroQr: {contenido, origen}`), guardado en la orden.
  En TEST (perfil `local`) el proveedor es el doble `RecargasProveedorLocal`: emite un QR **de prueba** (no
  pagable, con sello visible) y «Simular pago» acredita **por el camino contable real**. Al acreditarse, el saldo
  cuenta del valor anterior al nuevo (`MontoQueSube`, nunca pasa por cero).
- **Listo para el banco real:** el adaptador del banco implementa `ProveedorDeRecargas.solicitar(referencia, monto)
  → Optional<CobroQr>` con origen `BANCO`, y el QR del banco se guarda y viaja sin cambiar nada más. En producción
  el doble de prueba no puede existir (si se pide por propiedad, el servicio no arranca: probado).

## Completado

| ID | Qué se logró (observable) | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `orden_recarga` con `qr_contenido VARCHAR(512)` y `qr_origen VARCHAR(10)` + CHECK (`PRUEBA`,`BANCO`), desde el `.puml` | `python3 scripts/generar_ddl.py` + psql | PASS — `evidencia/h1-s1-m1-esquema.txt` |
| H1.S1.M2 | Puerto `ProveedorDeRecargas.solicitar` devuelve `Optional<CobroQr>`; dobles y adaptador simulado adaptados sin cambiar comportamiento | `./gradlew :servicios:nucleo-financiero:compileTestJava` | PASS (exit 0) |
| H1.S1.M3 | Doble `RecargasProveedorLocal` en 3 niveles (correcto / límite / inválido) | `… test --tests '*RecargasProveedorLocalTest'` | PASS 9/9 — `evidencia/h1-tests-dirigidos.txt` |
| H1.S2.M1 | La orden guarda y devuelve el QR; reintento con la misma clave = misma orden y mismo QR | `… integrationTest --tests '*CU10QrDinamicoTest'` | PASS 6/6 (PostgreSQL real) |
| H1.S2.M2 | Contrato: `SalidaRecarga.cobroQr` opcional + `POST /billetera/recargas/{ordenId}/pago-de-prueba` | `… webTest` | PASS 82/82 |
| H1.S2.M3 | Pago de prueba: acredita una vez (repetido = misma transacción), ajeno rechazado, sin QR de prueba rechazado; HTTP 200 / 404 `AP-VAL-05` sin doble / 403 con `BILLETERA_VER` | idem | PASS |
| H1.S2.M4 | Perfil `local` (TEST) → doble de prueba activo; producción → adaptador de siempre; producción + `modo=prueba` → no arranca | `… integrationTest --tests '*ArranqueRecargaQrTest'` | PASS 3/3 — `evidencia/h1-s2-m4-arranque.txt` |
| H2.S1.M1 | Cliente Dart regenerado: `cobroQr`, `CobroQrRecargaOrigenEnum`, `pagarRecargaDePrueba` | `generarClienteDart --no-build-cache` + `build_runner` | PASS — `evidencia/h2-s1-m1-cliente-dart.txt` |
| H2.S1.M2 | La pantalla muestra el QR, «Monto a acreditar», sello «QR de prueba», «Tu saldo» y «Simular pago» | `flutter test test/widget/cobro_de_recarga_test.dart test/widget/pantalla_recargar_test.dart` | PASS 7/7 — `evidencia/h2-tests-movil.txt` |
| H2.S1.M3 | Simular pago: el saldo sube **desde 500 a 650** (a mitad de la animación vale entre 500 y 650, nunca 0), termina exacto, 1 pedido con clave, 1 háptico de éxito; con movimiento reducido aparece entero | idem | PASS |
| H2.S1.M4 | Prueba visual: 3 estados × 360/412/768 × claro/oscuro, sin desborde, **miradas** (2 rondas) | `flutter test test/capturas/qr_recarga_capturas_test.dart --update-goldens` | PASS 18/18 — `evidencia/capturas/` |
| H2.S1.M5 | El rechazo `AP-CU10-05` se explica con texto propio y una sola acción de reintento (misma clave) | `flutter test test/widget/cobro_de_recarga_test.dart` | PASS |
| H3.S1.M3 | PR #341 → `test` (merge `e132bc86`) y #342 → `dev` (merge `0376fb4c`); `frontend-mirror` sincronizado (`test` 13e03b06, `dev` 2fc369ee, mismo árbol que origin) | `gh pr view 341/342` · `git diff --quiet origin/<b> frontend-mirror/<b>` | PASS |
| H3.S1.M1 | Regresión backend completa del servicio | `./gradlew :servicios:nucleo-financiero:test integrationTest webTest` | PASS 676/676 — `evidencia/h3-regresion-backend.txt` |

## A medias

### H3.S1.M2 — Regresión móvil completa
- Qué anda: `flutter test` (suite entera, 26 min): **2238 en verde**, incluidos los 7 dirigidos y las 18 capturas;
  `dart analyze --fatal-infos` sin issues; `verificar_frontend.py movil` TODO OK.
- Qué no anda: **353 tests en rojo, todos goldens/capturas** (`Pixel test failed` o archivo de referencia inexistente)
  en 51 archivos (`test/goldens/h10_*`, `test/capturas/h17_*`, `rediseno/`, etc.). Corridos los mismos 51 archivos
  sobre un worktree limpio de `origin/test` `ebe0ceb0`: **las mismas 353 fallas, por nombre** (`comm`: 0 solo en la
  rama, 0 solo en la base). Clasificación `ENVIRONMENT`/preexistente: este trabajo no introduce ninguna. La memoria
  del proyecto registra que la autoridad de los goldens es el CI de macOS.
- `yarn lint`: `movil` falla **solo** por `lib/pantallas/identidad/pasos_alta/paso_cuenta.dart` sin formatear, idéntico
  en `origin/test` (no tocado). `backoffice`, `ui`, `tutoriales` y `web` fallan por `ENVIRONMENT` del worktree
  (`Cannot find package 'angular-eslint'`: sin `yarn install` propio); este trabajo no cambia TypeScript.
- Qué falta exactamente: que los goldens se regeneren en el CI de macOS (no es de este trabajo) y formatear
  `paso_cuenta.dart` (fuera de alcance, anotado).
- Dónde quedó: rama `claude/qr-recarga-dinamico-20261030`, compila; la app pasa analyze.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| — | ninguna | — |

## Evidencia

Índice en `evidencia/`:
- `h1-s1-m1-esquema.txt` — columnas y CHECK en la base aplicada.
- `h1-deriva.txt` — generador estable (2ª corrida igual), jOOQ ve `QR_CONTENIDO`/`QR_ORIGEN`, y **el guion real
  `despliegue/aplicar-esquema.sh` sobre una base con el esquema viejo de `origin/test`**: agrega 2 columnas + 1 CHECK;
  2ª corrida: 0 y 0.
- `h1-tests-dirigidos.txt` — `CU10QrDinamicoTest` 6/6 y `RecargasProveedorLocalTest` 9/9.
- `h1-s2-m4-arranque.txt` — selección del proveedor por perfil.
- `h3-regresion-backend.txt` — test 94 · integrationTest 500 · webTest 82, 0 fallas, 0 saltados.
- `h2-s1-m1-cliente-dart.txt`, `h2-tests-movil.txt` — cliente y tests de la app.
- `capturas/recarga-qr-{pendiente,acreditada,error}-{360,412,768}-{claro,oscuro}.png`.

### Evidencia de dinero (regla 91.6)
1. **Asiento:** `CU10QrDinamicoTest.simularElPagoAcreditaUnaSolaVezYElAsientoCuadra` afirma una sola transacción
   con débitos = créditos = `150.00` (puente de custodia → billetera), saldo final 150.
2. **Idempotencia:** pagar dos veces la misma orden devuelve el mismo `SalidaAcreditacion` (misma `transaccionId`)
   y el saldo no cambia; en la app, reintentar tras un error viaja con la **misma** `Idempotency-Key` (test).
3. **Cuadre:** el saldo se lee del libro (`saldo_disponible`), no de una suma ad hoc.
4. **Reversa:** sin cambios. El pago de prueba usa `RecargasConProveedor.confirmar`, cuyas ramas de rechazo y
   discrepancia ya están cubiertas por `CU10DiscrepanciaTest`/`CU10RechazosTest` (en la regresión, en verde).
5. **Redondeo:** no hay división; importes en `NUMERIC`/`Dinero`, en la app en centavos enteros.

### Seguridad (regla 90.6)
- **Amenaza:** que «Simular pago» acredite dinero sin pago real en producción, o sobre la orden de otra persona.
- **Controles:** (a) el doble va con `@Profile({"local","test"})` + `aportaya.recarga.qr.modo=prueba`
  (`RecargasProveedorLocal.java`); (b) sin doble, el endpoint responde 404 igual que una ruta inexistente
  (`FondeoDeLaBilletera.pagarRecargaDePrueba`); (c) en producción con `modo=prueba` el proceso **no arranca**;
  (d) titularidad por `consultarPropia`; (e) solo órdenes cuyo QR guardado es de origen `PRUEBA`; (f) permiso
  `BILLETERA_OPERAR`.
- **Tests:** `ArranqueRecargaQrTest` (3), `CU10QrDinamicoTest.otraPersonaNoPuede…` y `unaOrdenSinQrDePrueba…`,
  `BilleteraFondeoWebTest.pagoDePruebaRechazos` (404 + 403).
- **Riesgo residual:** en TEST cualquier titular puede acreditarse saldo simulado en su propia billetera. Es el
  propósito pedido, en un ambiente con datos sintéticos.

## No cubierto
- **Ejercicio de punta a punta en runtime** (app → gateway → `nucleo-financiero` → PostgreSQL) y en TEST: no se
  levantó el stack ni se desplegó. Lo que hay es integración contra PostgreSQL real + HTTP con MockMvc + app con
  el backend simulado. `tarifas` (cotización) quedó doblado con `TarifaGratuita` en los tests de integración.
- **Banco real:** no existe adaptador. El formato del QR del banco es desconocido (ver ambigüedades). Tampoco
  existe cómo se entera el sistema del pago real: hoy `acreditarRecarga` exige `TESORERIA`; hace falta el
  webhook o una consulta programada del proveedor. Cuando eso exista, la app va a ver el saldo subir al releerlo.
- La animación se verificó por números en el test (valor intermedio) y por capturas estáticas, no grabada en video.
- Skills D1/D2 de `AGENTS.md` no se leyeron completas; se leyeron las D4 (`pasanaku-diseno`,
  `pasanaku-ui-componentes`, `pasanaku-movimiento`, `pasanaku-app-movil-ux`).

## Desvíos del plan
- **H2.S1.M5 agregada** en la marcha: la prueba visual mostró el genérico «Algo salió mal de nuestro lado» para
  el rechazo; se agregó el texto de `AP-CU10-05` al catálogo de la app.
- **«Ya pagué» no se hizo:** acreditar exige `TESORERIA` (regla 91.3: no confirma quien pide). Con QR de banco la
  app muestra el QR y espera; no ofrece confirmar.
- **Error en la pantalla:** pasó de dos botones («Volver a intentar» + «Simular pago») a una sola acción.
- **`ArranqueRecargaQrTest`** no estaba en el plan: se agregó para demostrar la selección del proveedor por perfil.
- **Fallos encontrados y corregidos:** `PRODUCT_BUG` — sin doble, el pago de prueba devolvía **500** en vez de 404
  (el proxy de `@Permiso` envolvía la excepción en `UndeclaredThrowableException`); se responde el 404 estándar
  directamente. `TEST_BUG` ×5, sin debilitar aserciones (detalle en cada archivo de evidencia).

## Procesos y recursos al cerrar
- Contenedores `pg-qr-recarga` y `pg-qr-viejo`: detenidos (eran `--rm`). Worktree base temporal: eliminado.
- Queda el worktree `../Pasanaku-qr` (rama ya publicada); se puede borrar con `git worktree remove ../Pasanaku-qr`.
- **No se desplegó en TEST desde esta sesión.**

## Riesgos residuales y deuda
- **TEST necesita el cliente Dart regenerado en el host:** el camino automático no regenera `clientes/dart`
  (memoria `aportaya-test-contabo`). Tras el merge hay que correr en el host
  `/opt/aportaya/bin/construir-movil-web.sh` y después `bin/disparar-despliegue.sh`; si no, la app web de TEST
  queda con la imagen vieja (sin QR) aunque el backend ya lo envíe.
- **El estado del doble vive en memoria:** si `nucleo-financiero` se reinicia en TEST, «Simular pago» igual
  funciona (vuelve a registrar la referencia), pero la orden necesita estar `PENDIENTE` y sin vencer (30 min).
- **Hallazgos fuera de alcance (anotados, no tocados):** (1) `generar-ejemplos.mjs` crea 12 ejemplos que faltan en
  `origin/test` (aportes, auditoría, grupos, identidad, notificaciones, tarifas) — se borraron de este trabajo;
  (2) `dart format` reformatea `pasos_alta/paso_cuenta.dart` (1 línea) — revertido; (3) `pantalla_aportar.dart`
  (aporte al grupo) tampoco muestra QR: el canal «QR» pide una referencia a mano.

## Decisiones y ambigüedades
- **Formato del QR del banco real** — desconocido. Supuesto: texto codificable en QR de hasta 512 caracteres
  (`CobroQr.LARGO_MAXIMO`). Si el banco entrega una imagen, se agrega otro campo. **Confirmar con Pablo** al elegir banco.
- **Contenido del QR de prueba** — `PASANAKU-PRUEBA|NO-PAGABLE|ref=<orden>|monto=<importe>|BOB`: sintético, sin
  datos de la persona, declarado como de prueba.
- **Qué medio emite QR** — el doble emite QR para cualquier `medio` (QR, tarjeta, transferencia), porque el puerto
  no recibe el medio. **Confirmar con Pablo** si con tarjeta/transferencia no debe mostrarse QR.
- **Producción con `modo=prueba`** — se eligió que no arranque (cierre en falso) en lugar de ignorarlo en silencio.
