# H7.S1.M3 · Calendario y cortes: fecha UTC, contractual y de liquidacion

## Que se hizo
- `VencimientoLocal` (dominio de `grupos`, puro): de un instante UTC y una zona saca **tres fechas explicitas**: `fechaEnUtc`, `fechaContractual` (dia calendario en la zona del contrato, `America/La_Paz` por omision) y `fechaDeLiquidacion` (primer dia habil desde la contractual, nunca anterior), mas los dias saltados con su motivo.
- `GET /grupos/calendario/vencimiento` (`calcularVencimientoLocal`, CU-59): expone las tres fechas, la zona y los dias saltados; sin calendario del anio responde `AP-CU59-05`; zona inexistente `AP-CU59-06`.
- Correccion de un defecto: `CU59CalcularPlazo.correrSiCae` solo miraba el calendario y corria un viernes feriado a **sabado**; ahora usa el mismo criterio que `PlazoHabil` (feriado, sabado, domingo). `CU59Test.criterio3` esperaba el sabado 2 de mayo de 2026; ahora espera el lunes 4 (la expectativa vieja era el defecto).
- `aportes` contaba "hoy" en UTC (`CU21`, `CU19`): a las 21:00 de La Paz del ultimo dia de gracia el servidor ya veia el dia siguiente, marcaba mora y generaba recargo antes de tiempo. Ahora usa `Reloj.hoy()` (La Paz). `garantia` (`CU23CubrirIncumplimiento`) calculaba plazos de la deuda con la fecha UTC: idem.
- Hallazgo menor: `ObligacionRepositorio.vencidasSinRecargo` filtraba por el estado `PARCIAL`, que la base rechaza (`PAGADO_PARCIAL`): una obligacion con pago parcial que vencia nunca recibia recargo. Corregido con prueba (`CU21ZonaHorariaTest.elPagoParcialVencidoTambienGeneraRecargo`).

## Casos de la evidencia (E-H7-03)
| Caso | Instante | Contractual (La Paz) | Liquidacion | Prueba |
|---|---|---|---|---|
| UTC cae un dia despues | 2026-05-02T02:00Z | 1 may (feriado) | lun 4 may | `CU59VencimientoTest.criterio5` |
| Misma hora, zona UTC | 2026-05-02T02:00Z | 2 may (sab) | lun 4 may | `criterio6` |
| Fin de anio, UTC en feriado | 2027-01-01T03:30Z | 31 dic | 31 dic (sin corrimiento) | `criterio7` |
| Viernes Santo + fin de semana | 2027-03-26T15:00Z | 26 mar | lun 29 mar | `criterio8` |
| Feriado local de un grupo | 2026-10-14T15:00Z | 14 oct | 15 oct (GRUPO) / 14 oct (NACIONAL) | `criterio9` |
| Anio sin calendario | 2031-01-05 | - | rechazo | `criterio10` |
| Borde medianoche | 04:00:00Z vs 03:59:59Z | dia nuevo / dia anterior | - | `VencimientoLocalTest.bordeDeMedianoche` |
| Aporte, UTC vs La Paz | 01:00Z / 03:59:59Z / 04:00Z del 8 may | ultimo dia de gracia 7 may | EN_MORA solo desde 04:00Z | `CU21ZonaHorariaTest.criterio8` |

## Feriados: procedencia (no se inventaron)
- No se agrego ni se modifico ningun feriado de la semilla. Los 33 feriados 2026-2028 (`seeders/minimos/12-calendario-habil.json`) citan norma pero **no tienen copia de la fuente**: quedan "con cita, sin verificar" (O2-C2).
- Las pruebas siembran sus propios dias con la fuente en la descripcion, o `[SINTETICO]` los de grupo.
- Mecanismo de procedencia: `FeriadoConProcedencia` (fuente obligatoria `AP-CU59-04`, alcance completo `AP-CU59-02`, fecha no vencida `AP-CU59-03`, marca `[SINTETICO - no apto para produccion]`). No persiste: `catalogo` es solo lectura (ADR-029); ver O2-C1.
- Ambiguedades: `ambiguedades-y-decisiones-abiertas.md` (O2-C1 a O2-C7).
