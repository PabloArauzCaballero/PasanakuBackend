# Carril O2 · Indice y resultados (2026-10-09)

Corridas bajo el guardian de RAM, Postgres desechable en 5437 (`aplicar.sql` RC 0). Logs literales en esta carpeta.

| Corrida | Log | Resultado |
|---|---|---|
| 1 · spotlessApply + test/webTest/integrationTest/testBarrido de los 4 servicios | `corrida-guardada-1.log` | grupos y aportes verdes; entregas 104 tests, 1 fallo (mi prueba usaba un parametro `OffsetDateTime` sobre `timestamptz`); garantia 149 tests, 35 fallos (mi `PlanDeContingenciaRepositorio` pasaba la fecha como `varchar`) |
| 2 · entregas + garantia + generarClientes (tras corregir) | `corrida-guardada-2.log` | entregas integrationTest y testBarrido verdes; garantia: 1 fallo (`CU131ConcurrenciaTest.dosTurnosPorLaMismaReserva`: la contingencia que deja el INSUFICIENTE de una ronda frena la reserva de la siguiente, que es el diseno); `generarClientes` de todos los servicios corrio |
| 3 · garantia integrationTest + testBarrido (tras limpiar `plan_contingencia` entre rondas en esa prueba) | `corrida-guardada-3.log` | `BUILD SUCCESSFUL`, RC 0 |
| Regresion front (simulado, backoffice, web) contra los clientes regenerados | `regresion-front.log` | tsc, eslint, `ng lint` y `verificar_frontend.py` en verde; backoffice 421 + a11y 42; web 72 + a11y 8; simulado 232 de 233 (ver abajo) |

## Conteos reales (XML de `build/test-results`; fallos 0, errores 0, omitidas 0)

| Servicio | test | webTest | integrationTest | testBarrido | Referencia | Variacion |
|---|---|---|---|---|---|---|
| aportes | 32 | 36 | 88 | 2 | 27/32/76/2 | sube |
| grupos | 46 | 68 | 192 | 2 | 23/62/183/2 | sube |
| entregas | 10 | 45 | 104 | 2 | 10/45/94/2 | sube (+10 integracion) |
| garantia | 20 | 43 | 149 | 2 | 20/43/144/2 | sube (+5 integracion) |

## Gates (`gates.log`)
- `verificar_pruebas_web.py`: "La capa web esta cubierta segun ADR-043" (33/33 controladores, 15 sabanas).
- `verificar_criterios.py`: CU-21, CU-22, CU-23, CU-59, CU-60, CU-19, CU-99 sin divergencias. Quedan 14 fallas **ajenas a este carril**: CU-08 (identidad), CU-62/63/64/68 (grupos, pruebas de otros carriles sin escenario en la boveda) y inversiones.
- `verificar_seguridad.py`: 1 falla ajena (`nucleo-financiero/.../CU24MonedaTest.java`, SQL concatenado en una prueba; no se toca nucleo-financiero).
- `verificar_boveda.py`: 2 fallas ajenas ("Ver tambien" reciproco CU-13, cifra desfasada en `planes/17`). La mia (wikilink roto en CU-21 y codigos AP-CU21 no correlativos) se corrigio.
- `packages/simulado` vitest: 1 fallo ajeno (`inversiones/listarComprobantesDeInversion` usa `AP-CU120-01`, no declarado para esa operacion); los ejemplos nuevos de este carril (`calcularVencimientoLocal`, `generarObligacionesDelPeriodo`, `comprometerSorteo` regenerado) pasan `ejemplos-contra-esquema` y `catalogo-de-errores`.

## Peldaño y no cubierto
TESTED (pruebas dirigidas verdes con Postgres real; web/backoffice compilan y pasan contra el cliente regenerado). No es VERIFIED de extremo a extremo: sin servidor levantado, sin integracion real con `grupos` (ruta de calendario inexistente) ni con `nucleo-financiero` (consumo de asientos y `pagarAlVendedor` pendientes; todo contra doble), sin E2E, sin la regresion completa de todo el repositorio, movil Dart no compilado.
