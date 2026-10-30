# Plan — QR bancario dinámico en la recarga, y ver subir el saldo

- Fecha: 2026-10-30 · Repos afectados: Pasanaku (backend `nucleo-financiero`, `apps/movil`) · Predecesor: ninguno
- Rama: `claude/qr-recarga-dinamico-20261030` (worktree `../Pasanaku-qr`, base `origin/test` `ebe0ceb0`)
- Resultado observable: en la app, Billetera → Recargar → medio QR → «Recargar» muestra un **QR dinámico de la
  orden** (hoy muestra un reloj de arena). En TEST el QR es **de prueba** y aparece marcado así; el botón
  «Simular pago» lo da por pagado, la orden se acredita por el camino contable real y la persona **ve
  contar el saldo** desde el valor anterior hasta el nuevo.
- Kill-test: `POST /billetera/recargas` con medio `QR` en perfil local responde sin `cobroQr` → no está hecho.

## Decisiones del usuario (2026-10-30, en la sesión)
- Pantalla: **Recargar billetera** (no el aporte al grupo).
- Pago de prueba: **botón «Simular pago»**, visible solo cuando el QR es de prueba; en producción no existe.

## Hallazgos (descubrimiento)
| Hecho | Dónde |
|---|---|
| `SalidaRecarga` no tiene ningún campo de QR | `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:1240` |
| El Javadoc del CU dice «solicitar crea la orden y devuelve el QR», el código no lo hace | `aplicacion/CU10RecargarSaldo.java:31` |
| La pantalla de confirmación pinta solo un reloj y «pendiente» | `apps/movil/lib/pantallas/billetera/pantalla_recargar.dart:92` |
| Puerto del proveedor: `solicitar` es `void` | `dominio/puertos/ProveedorDeRecargas.java` |
| Único adaptador `RecargasProveedorSimulado` (`@Component` sin condición); fuera de `modo=simulado` rechaza | `infraestructura/ClienteProveedorSimulado.java:235` |
| Precedente de doble local en memoria con `@Profile({"local","test"})` | `infraestructura/ProveedorDeRetiroLocal.java` |
| TEST corre con perfil Spring `local` | memoria `aportaya-test-contabo` |
| Esquema: fuente `docs/entidades/*.puml` → `scripts/generar_ddl.py` → `sql/` | `sql/README.md` |
| El despliegue agrega solo las columnas nuevas (ADD COLUMN derivado) | `despliegue/aplicar-esquema.sh` `expandir_columnas` |
| Átomo de QR existente | `packages/diseno_flutter/lib/atomos/codigo_q_r.dart` |
| Animación de saldo existente (nunca pasa por cero) | `packages/diseno_flutter/lib/atomos/monto_que_sube.dart` |
| Acreditar ya existe y es idempotente por orden | `POST /billetera/recargas/{ordenId}/acreditacion`, `RecargasConProveedor.confirmar` |

## Alcance
- IN: modelo `orden_recarga` (+2 columnas nulas), puerto y adaptadores de recarga, `RecargasConProveedor`,
  contrato `nucleo-financiero.yaml` (campo opcional + operación de pago de prueba), controlador, perfil local,
  cliente Dart regenerado, `pantalla_recargar.dart` y su dominio, tests.
- OUT: aporte al grupo (`pantalla_aportar.dart`), adaptador del banco real, retiros, backoffice, cualquier
  cambio de `tarifas`. Lo roto que aparezca fuera de esto se anota, no se arregla.

## Diseño
1. `ProveedorDeRecargas.solicitar` devuelve `Optional<CobroQr>` (`contenido`, `origen` PRUEBA|BANCO). El
   adaptador del banco real, cuando exista, devuelve el QR del banco por acá: **no cambia nada más**.
2. `RecargasProveedorLocal` (`@Profile({"local","test"})` + `aportaya.recarga.qr.modo=prueba`): emite un QR
   sintético marcado `PRUEBA` y no pagable, guarda el estado en memoria (precedente `ProveedorDeRetiroLocal`)
   y expone `marcarPagado`. `RecargasProveedorSimulado` queda como bean cuando el modo no es `prueba`.
3. `RecargasConProveedor.solicitar` guarda el QR en la orden (`qr_contenido`, `qr_origen`) en una
   transacción corta **después** de hablar con el proveedor (nunca red dentro de la transacción); un
   reintento con la misma clave devuelve el mismo QR.
4. `POST /billetera/recargas/{ordenId}/pago-de-prueba`: solo titular; sin el adaptador de prueba responde
   404; marca pagado en el doble y acredita por `confirmar` (mismo asiento, misma idempotencia).
5. Móvil: la confirmación pinta `CodigoQR` + sello «QR de prueba», botón «Simular pago» (solo PRUEBA) y
   «Ya pagué» (acreditación normal). Acreditado: `MontoQueSube` del saldo anterior a `saldoDespues`.

## H1 — El backend devuelve el QR dinámico y acepta el pago de prueba
**CA:** Dado el perfil local, cuando el titular solicita una recarga, entonces la respuesta trae `cobroQr`
con origen `PRUEBA`; y cuando pide el pago de prueba, la orden queda `ACREDITADA` una sola vez.
**DoD:** `./gradlew :servicios:nucleo-financiero:test` en verde (pegado) · asientos y doble disparo en evidencia.
**Estado:** HECHO (backend `TESTED` + deriva verificada; ver REPORTE)

### H1.S1 — Modelo y puerto
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `orden_recarga` + `qr_contenido`, `qr_origen` en el `.puml`, regenerar `sql/` | el DDL generado tiene las dos columnas nulas | `python3 scripts/generar_ddl.py && grep qr_ sql/10_tablas/10_billetera_custodia/orden_recarga.sql` | HECHO — `evidencia/h1-s1-m1-esquema.txt` |
| H1.S1.M2 | Puerto devuelve `Optional<CobroQr>`; simulado y dobles de test adaptados | compila | `./gradlew :servicios:nucleo-financiero:compileTestJava` exit 0 | HECHO — `compileTestJava` exit 0 |
| H1.S1.M3 | `RecargasProveedorLocal` con los tres niveles | correcto/límite/inválido pasan | `./gradlew … test --tests '*RecargasProveedorLocalTest'` | HECHO — `evidencia/h1-tests-dirigidos.txt` (9/9) |

### H1.S2 — Orden con QR y pago de prueba
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `RecargasConProveedor` guarda y devuelve el QR; reintento devuelve el mismo | test de integración con PG real pasa | `--tests '*CU10QrDinamicoTest'` | HECHO — `evidencia/h1-tests-dirigidos.txt` (6/6) |
| H1.S2.M2 | Contrato: `SalidaRecarga.cobroQr` opcional + `pagoDePruebaRecarga`; controlador | build regenera interfaz y compila | `./gradlew :servicios:nucleo-financiero:compileJava` | HECHO — `webTest` 82/82 |
| H1.S2.M3 | Pago de prueba: titular acredita 1 vez; otro usuario rechazado; sin adaptador 404 | tests pasan | `--tests '*CU10QrDinamicoTest'` | HECHO — CU10QrDinamicoTest 6/6 + BilleteraFondeoWebTest 404/403/200 |
| H1.S2.M4 | `application-local.yml` activa `aportaya.recarga.qr.modo: prueba` | ArranqueTest y ArranqueProduccionTest verdes | `--tests '*Arranque*'` | HECHO — `evidencia/h1-s2-m4-arranque.txt` (3/3) |

## H2 — La app muestra el QR y el saldo sube
**CA:** Dado un QR de prueba, cuando la persona toca «Simular pago», entonces ve el saldo contar del valor
anterior al nuevo, sin pasar por cero, y el lector de pantalla anuncia el valor final.
**DoD:** `flutter test` dirigido + `flutter analyze` en verde · capturas móvil/tablet en claro y oscuro.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Regenerar el cliente Dart de `nucleo-financiero` | `SalidaRecarga.cobroQr` y `pagoDePruebaRecarga` existen | `grep -n cobroQr clientes/dart/nucleofinanciero/...` | HECHO — `evidencia/h2-s1-m1-cliente-dart.txt` |
| H2.S1.M2 | Confirmación: `CodigoQR` + sello de prueba + botones | widget test pasa | `flutter test test/…/pantalla_recargar_test.dart` | HECHO — `evidencia/h2-tests-movil.txt` (7/7) |
| H2.S1.M3 | Acreditación: notifier + `MontoQueSube` desde saldo anterior | widget test (sube, error, sin QR) pasa | idem | HECHO — idem (sube desde 500, termina en 650) |
| H2.S1.M4 | Prueba visual | capturas revisadas | `evidencia/capturas/*` | HECHO — `evidencia/capturas/` 18 PNG, 2 rondas mirada (ver REPORTE) |
| H2.S1.M5 | (agregada 2026-10-30, hallada en la prueba visual) Texto propio para `AP-CU10-05` en `lib/dominio/errores.dart` | el error de pago de prueba no muestra el genérico «de nuestro lado» | `flutter test test/widget/cobro_de_recarga_test.dart` con 422 `AP-CU10-05` | HECHO — `evidencia/h2-tests-movil.txt` |

## H3 — Regresión y entrega
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Regresión `nucleo-financiero` completa | suite verde | `./gradlew :servicios:nucleo-financiero:test` | HECHO — `evidencia/h3-regresion-backend.txt` (676/676) |
| H3.S1.M2 | Regresión móvil + `flutter analyze` | verde | `flutter test` · `flutter analyze` | A MEDIAS — 2238 verdes; 353 rojos idénticos a origin/test (goldens, preexistentes) · `evidencia/h3-regresion-movil.txt` |
| H3.S1.M3 | PRs a `test` y `dev` | PR abierto con reporte | `gh pr view` | HECHO — #341 (test, e132bc86) y #342 (dev, 0376fb4c) mergeados; espejo sincronizado |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| jOOQ se genera desde una base viva en `:5433` | sin base no compila | levantar PG16 local descartable y aplicar `sql/aplicar.sql` |
| Cambiar la firma del puerto rompe dobles de test | compilación de tests | adaptarlos en H1.S1.M2 sin cambiar su comportamiento |
| El QR de prueba confundido con uno real | alguien intenta pagarlo con su banco | contenido con prefijo `PASANAKU-PRUEBA`, sello visible, botón solo con origen PRUEBA |
| Estado del doble en memoria se pierde al reiniciar | QR de prueba vuelve a pendiente | `marcarPagado` registra la referencia aunque no la conozca; declarado |

## Ambigüedades registradas
- Formato del QR del banco real (EMV/QR interoperable o imagen del banco): **desconocido**. Supuesto: el
  contenido es texto codificable en QR, hasta 512 caracteres. Confirmar con Pablo cuando se elija banco.
