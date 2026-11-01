# Reporte — Traspaso del cupo por subasta (puja con pago entre las partes)

> **AVANCE: 15 / 21 — 71,4 %.** (`python3 .claude/hooks/plan_status.py --path docs/trabajo/2026-10-30-traspaso-por-subasta/PLAN.md`)

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/traspaso-por-subasta` (integra `origin/test` @ 10 commits posteriores a `ebe0ceb0`)
- Peldaño de evidencia alcanzado: **`TESTED`** en backend (grupos, nucleo-financiero, entregas: integración contra Postgres real, con `grupos` y la billetera como dobles declarados en `entregas`) y en la app (widget + capturas miradas). **No `VERIFIED`**: falta el recorrido real en TEST (H6).

## Qué se encontró al empezar
- El traspaso desplegado (CU-64) **no era el pedido**: pasaba el cupo a alguien que ya estaba en el grupo, sin precio ni pago, y exigía una votación aprobada; sin votación el servidor rechazaba con `AP-CU64-05`. Así se planificó el 29/10 («sin precio ni pago»).
- El mercado de `entregas` (venta del turno a precio fijo) **nunca pudo publicar por HTTP**: `HechosDeGruposPorHttp.turno()` devuelve siempre vacío (`servicios/entregas/.../HechosDeGruposPorHttp.java:46`). Queda anotado, fuera de alcance.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `grupos` dice si el titular puede subastar su cupo (deuda o turno cobrado lo impiden; a otro, AP-CU64-06) con aporte, nombre y periodicidad | `./gradlew :servicios:grupos:integrationTest --tests '*CU64SubastaTest'` | PASS 9/9 · `evidencia/h1-grupos-cu64-subasta.txt` |
| H1.S1.M2 | `grupos` dice si alguien puede pujar: restricción, KYC, reputación (con historial), máximo de grupos | idem + `webTest` | PASS · webTest 137/137 |
| H1.S1.M3 | `POST …/traspasos-por-subasta` (solo `SERVICIO_ENTREGAS`): alta del ganador de afuera + traspaso en una transacción, idempotente; dos cierres a la vez → uno solo; participante → 403 | idem | PASS (incluye concurrencia real) |
| H2.S1.M1 | `nucleo-financiero` libera una retención del mercado con identidad de servicio; usuario → AP-CU13-09; repetir no duplica | `./gradlew :servicios:nucleo-financiero:integrationTest --tests '*CU13LiberacionDelMercadoTest'` | PASS 4/4 · integración del módulo 507/507 · `evidencia/h2-nucleo-liberacion.txt` |
| H3.S1.M1 | Tablas `entregas.subasta_cupo` y `entregas.puja_subasta` desde el PlantUML (solo aditivo) | `python3 scripts/generar_ddl.py` | «Sin pendientes a nivel de datos» |
| H3.S1.M3 | Abrir subasta (base, 24/48/72 h), una viva por cupo, evento `entregas.subasta_abierta` | `./gradlew :servicios:entregas:integrationTest --tests '*CU64*'` | PASS 16/16 · `evidencia/h3-entregas-regresion.txt` |
| H3.S1.M4 | Pujar: retener → la más alta bajo candado → la superada se devuelve; 6 pujas simultáneas: 1 vigente, total del sistema intacto | idem | PASS |
| H3.S1.M5 | Lista para todos (orden de cierre) y última por cupo; sin saliente ni postores | idem | PASS |
| H3.S1.M6 | Cierre: gana la más alta → grupos pasa el cupo → la billetera paga EXACTAMENTE la puja; sin pujas DESIERTA; grupos caído / billetera caída se reanudan sin pagar dos veces; grupos rechaza → FALLIDA y el ganador recupera todo | idem | PASS |
| H3.S1.M8 | Cancelar sin pujas; con pujas AP-CU136-02; otro AP-CU136-05 | idem | PASS |
| H5.S1.M1 | «Subastar mi cupo» reemplaza el traspaso con votación en la pantalla del grupo | `flutter test test/pasanaku/subastar_cupo_test.dart` | PASS 5/5 |
| H5.S1.M2 | «Cupos en subasta» en el inicio y en su pantalla (cargando, datos, vacío, error) | `flutter test test/pasanaku/pujar_en_subasta_test.dart` | PASS |
| H5.S1.M3 | Detalle + ofrecer: mínimo, aceptar reglas, hoja con lo que se retiene, AP-CU136-xx en voseo, «te superaron» | idem | PASS 8/8 (13/13 con M1) |
| H5.S1.M4 | Gates de la app | `dart analyze --fatal-infos lib test` · suite completa contra línea base | sin issues · **0 regresiones** (`evidencia/h5-app-regresion.txt`) |
| H5.S1.M5 | Capturas 360/412 claro/oscuro de lista, detalle y subastar, **miradas** | `flutter test test/capturas/subasta_capturas_test.dart --update-goldens` | 12/12 · `evidencia/capturas/` |

## A medias
### H1.S1.M4 — Obligaciones de los períodos futuros al ganador
- Qué anda: `aportes` emite cada período desde el calendario de `grupos`, que lee el titular actual del cupo (`CalendarioDelPeriodoPorHttp.java:41`): tras el traspaso, el período siguiente le llega al ganador.
- Qué no anda: no está probado de punta a punta; las obligaciones YA emitidas quedan con el saliente (igual que el CU-64 de antes).
- Qué falta exactamente: el recorrido H6.S1.M2 en TEST, mirando la obligación del período siguiente.
- Dónde quedó: sin código propio; rama `claude/traspaso-por-subasta`.

### H3.S1.M7 — La billetera paga al saliente consultando la subasta
- Qué anda: `GET /entregas/mercado/ofertas/{id}` responde por el id de una subasta adjudicada con el precio = puja ganadora y cargos 0 (`EntregasController.consultarOfertaParaPago` → `SubastasWeb.paraElPago`); probado en la consulta (`CU64CierreDeSubastaTest`).
- Qué no anda: la ida y vuelta real `nucleo → entregas` no se ejercitó (en `entregas` la billetera es un doble).
- Qué falta exactamente: H6.S1.M2 (cierre real en TEST y asiento del pago).
- Dónde quedó: compila y pasa; rama `claude/traspaso-por-subasta`.

### H4.S1.M1 — Aviso a todos los usuarios
- Qué anda: el evento `entregas.subasta_abierta` sale por el outbox; dentro de la app, «Cupos en subasta» está en el inicio de cualquier usuario.
- Qué no anda: no hay notificación push. La app no tiene push (sin Firebase en `pubspec.yaml`), la bandeja (`proveedor_bandeja.dart`) es solo local y `notificaciones` no consume eventos de dominio.
- Qué falta exactamente: un canal push (proveedor, registro de dispositivos, consumidor del evento en `notificaciones`). Es un trabajo propio.
- Dónde quedó: evento listo; sin consumidor.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H6.S1.M1 | TODO | PR a `dev` y `test` mergeados + reconstruir en el VPS (servicios, esquema y `construir-movil-web.sh`) |
| H6.S1.M2 | TODO | H6.S1.M1 · dos usuarios sintéticos con saldo en TEST |
| H3.S1.M2 | DESCARTADO | Decisión técnica 2026-10-30: la subasta es del cupo (antes del sorteo no hay turno); arreglar el mercado a precio fijo es otro trabajo |

## Evidencia
```text
$ ./gradlew :servicios:grupos:integrationTest            → 294 tests, 0 fallas
$ ./gradlew :servicios:grupos:webTest                    → 137 tests, 0 fallas
$ ./gradlew :servicios:nucleo-financiero:integrationTest → 507 tests, 0 fallas
$ ./gradlew :servicios:nucleo-financiero:webTest         → 82 tests, 0 fallas
$ ./gradlew :servicios:entregas:test / webTest / integrationTest → 40 / 60 / 130, 0 fallas · testBarrido 2/2
$ flutter test (9 carpetas, 1160) → 0 regresiones vs línea base; 13 nuevas en verde
```
Detalle en `evidencia/` (h1, h2, h3, h5 y `capturas/`).

## No cubierto
- El flujo real en TEST (abrir, dos pujas de dos personas, cierre, cupo cambiado, saldos y asientos del libro).
- Cuadre contra el mayor real (regla 91.6): en `entregas` la billetera es un doble que conserva el total; los asientos reales solo se ejercitaron en `nucleo` (`CU13MercadoTest`, `CU13LiberacionDelMercadoTest`).
- Una puja que quedó PENDIENTE porque el proceso murió después de retener y antes de anotar la referencia: solo se recupera si quien pujó repite con la misma clave (la app lo hace al reintentar).
- Push a todos los usuarios (ver H4).

## Desvíos del plan
- La subasta tiene su propio agregado (`subasta_cupo`/`puja_subasta`) en vez de ser una modalidad de `oferta_turno`: lo que se traspasa es el cupo, y antes del sorteo no hay turno.
- El traspaso al ganador lo hace `grupos` por llamada sincrónica e idempotente ANTES de pagar (no por evento después de pagar como el mercado): lo compensable primero, lo irreversible al final (98.3.5).
- La pantalla de traspaso con votación queda en el código, sin acceso desde la app (se reemplazó la entrada). Borrarla con sus pruebas es una decisión aparte.

## Riesgos residuales y deuda
- **Gate ajeno en rojo**: `apps/movil/lib/dominio/errores.dart` (203 líneas) y `CU10RecargarSaldo.java` (317) llegaron así de `origin/test` (`a989582c`, otra sesión). Bloquean `verificar_frontend.py` y el barrido de `nucleo` de cualquier PR a `test`. No se tocaron (regla 00 §3).
- En esta máquina 282 pruebas de la app (goldens y algunas de identidad) fallan **igual sin este cambio** (`ENVIRONMENT`, línea base en `evidencia/h5-app-linea-base-goldens.txt` y `h5-app-regresion.txt`).
- Barrels de `clientes/dart/{aportes,notificaciones,tarifas}` y 12 ejemplos del simulado quedan desalineados de sus contratos en `origin/test` (ajenos, no se commitearon).

## Decisiones y ambigüedades (confirmar con Pablo)
- D1–D4 son tuyas (2026-10-30): todos pueden pujar; pago retenido por la app; gana la más alta al cierre; sin votación.
- A1 · Incremento mínimo: un centavo sobre la más alta.
- A2 · Cargos de plataforma sobre la puja: 0. No se inventó tarifa.
- A3 · Plazo: 24, 48 o 72 h.
- A4 · El ganador entra ACTIVO aceptando las reglas del grupo al pujar (casilla obligatoria); no firma aparte.
- A5 · Las obligaciones ya emitidas quedan con el saliente; las futuras, con el ganador.
- A6 · «Les llega a todos» hoy es dentro de la app, no push.
- La puja retiene el saldo al ofrecer (no al ganar): la oferta es seria y el ganador no puede quedarse sin pagar.
