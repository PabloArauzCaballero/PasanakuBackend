# Plan del carril N2 (nucleo financiero, segunda vuelta) — 2026-10-09

Sub-plan operativo dentro de `../../PLAN.md` (H2.S1.M1, M4, M5 y las operaciones que otros servicios ya esperan de
`nucleo-financiero`). No edita PLAN.md ni REPORTE.md. Alcance de archivos: `servicios/nucleo-financiero/**`,
`herramientas/proveedor_simulado/**` (no se toca salvo necesidad), `docs/CasosDeUso/CU-1[0-4]*`, `CU-24*` (los
escenarios y R-xxx del mayor viven ahi), esquema (`docs/entidades/{03,10}*.puml`, `docs/Restricciones.md`,
`scripts/generar_ddl.py` en bloques minimos) y `sql/` regenerado. Evidencia en `evidencia/carril-N2/`.

## Hechos del descubrimiento (con ruta)

- El mayor contable no tiene moneda: `movimiento_contable` y `cuenta_contable` no la llevan; `Partida`/`CuadrarPartidas`
  asumen BOB (`dominio/Partida.java`, `dominio/CuadrarPartidas.java`) y `CU24RegistrarAsiento.reversar` fija BOB.
- `RetencionRepositorio.ver` fija `Moneda.BOB` en el importe de la retencion (la retencion no tiene columna de moneda).
- Las decisiones de saldo leen `cuenta_billetera.saldo_disponible` (cache derivada por trigger), no el libro
  (`CuentaBilleteraRepositorio.bloquear/ver`). Es la «proyeccion» de H2.S1.M5.
- No existe consumidor de eventos en ningun servicio (`Consumidos` de comun-mensajeria no lo usa nadie). Garantia emite
  `garantia.*` con `asiento` = `{origenTipo, origenId, partidas[{cuenta,debe,haber,moneda}]}` con cuentas LOGICAS
  (`garantia/.../AsientoPropuesto.java`, ambiguedad A5 de PLAN-CARRIL-C). Entregas NO emite partidas.
- `inversiones` pide (`evidencia/carril-D/contrato-requerido-nucleo.md`): motivo `INVERSION_VOLUNTARIA`, `titularId` y
  `comprometidoEnPozos` en el saldo, `POST /billetera/inversiones/debitos` y `/acreditaciones`.
- `entregas` define `FondosDelComprador.pagarAlVendedor(retencionRef, vendedor, paraElVendedor, cargos, clave)`; no
  existe en nucleo (hoy ejecutar una retencion es un cierre sin destinatario).
- `aportes` publica `GET /aportes/participantes/{id}/estado` con `deudaVigente`, `porAportar`, `moneda`.

## Decisiones tecnicas

- D1. Un asiento es mono-moneda; las cuentas contables llevan moneda; un movimiento solo toca cuentas de su moneda
  (R-AUD-12, R-CTB-10). Sin tipo de cambio registrado no se mezclan monedas (91.1.2). `DEFAULT 'BOB'` transitorio en
  `cuenta_contable.moneda` y `movimiento_contable.moneda` para no romper a `erp` (escribe sin moneda); CU-24 siempre la
  informa explicita.
- D2. Cuentas de sistema nuevas se resuelven por TIPO y moneda, no por variable de entorno: `CUSTODIA_INVERSION` unica
  por moneda (indice parcial). Los cargos van a `PLATAFORMA_INGRESOS` de esa moneda (debe haber exactamente una activa).
- D3. Decisiones de dinero con el LIBRO: `CuentaBilleteraRepositorio.bloquear` calcula disponible/retenido desde
  `movimiento_billetera` y `retencion_saldo` bajo el bloqueo de fila; la columna cache ya no autoriza nada.
- D4. Consumo de asientos: marca `evento_consumido` + CU-24 en UNA transaccion; cuenta logica sin mapeo = rechazo
  (`AP-CU24-02`), sin consumir; mapeo por configuracion, vacio por defecto (A5: lo define contabilidad).
- D5. `acreditarRescate` exige `TESORERIA` (confirmar que la plata volvio no lo hace quien la pide, 91.3); el debito exige
  ser el titular de la retencion. Unicidad real por origen (una orden se debita una vez, un rescate se acredita una vez).
- D6. `comprometidoEnPozos` = `deudaVigente + porAportar` de `aportes` (contrato existente) si la moneda coincide; sin
  respuesta el campo se OMITE y el consumidor falla cerrado (ya lo hace).

## Ambiguedades / DECISION_REQUIRED (supuesto — a quien confirmar)

| # | Pregunta | Supuesto | Confirmar con |
|---|---|---|---|
| N1 | Codigos del plan de cuentas para CAJA_EMPRESA / RESERVA_RESPALDO / POZO_GRUPO | Sin mapeo por defecto: el evento no se registra hasta que exista | Contabilidad (ERP) |
| N2 | Credencial de servicio con que `inversiones` acredita rescates (hoy propaga el token del usuario) | Permiso `TESORERIA` | Seguridad + dueño de inversiones |
| N3 | `aportes` consulta `estado` por `participanteId`; nucleo lo llama con `usuarioId` (patron previo) | Se conserva el patron | dueño de aportes |
| N4 | Comision de plataforma en la compra del mercado (`cargos`) | La fija entregas/tarifas; nucleo solo exige que la suma cuadre | Tarifas (H11) |

## Microtareas

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Moneda en el mayor, cuentas de custodia de inversion, moneda de la retencion | Un asiento USD no entra en una cuenta BOB ni se mezcla con BOB; cada tipo de cuenta tiene titular y disponibilidad inequivocos | `CU24*Test` (cuadre por moneda, rechazo en base R-AUD-12/R-CTB-10), `CU13*Test` | TODO |
| H2.S1.M5 | Decidir contra el libro, no contra la cache | Con la cache mostrando saldo anterior, un debito/retencion/transferencia que el libro no cubre es rechazado | `CU12*Test`/`CU13*Test` con cache corrompida a proposito | TODO |
| H2.S1.M4 | Consumo idempotente de eventos con partidas, reversos y reinicio | Evento repetido = un asiento; caida tras commit y reproceso = un asiento; reversa = contra-asiento enlazado, original intacto | `CU24*Test` (evento duplicado, fuera de orden, compensa, reinicio) | TODO |
| N2.A | `debitarInversion` / `acreditarRescate` / saldo con `titularId` y `comprometidoEnPozos` / motivo | Contrato de `inversiones` cumplido del lado productor; 5 puntos de 91.6 | `CU13*Test`, `CU13*WebTest`, prueba de contrato productor | TODO |
| N2.B | Pago al vendedor con retencion del comprador (mercado) | Retencion VIGENTE del comprador se liquida: vendedor + cargos = retencion, una vez | `CU13*Test` | TODO |
| N2.C | Documentos (CU-13, CU-14, CU-24, README) y gate de criterios | `verificar_criterios.py --servicio nucleo-financiero` sin fallas | script | TODO |
| N2.D | Corrida completa guardada | spotlessApply, test, webTest, integrationTest, testBarrido; conteos >= 54/32/340/2 | `jj_full.sh` | TODO |

Estados: se actualizan al final del archivo (§Registro).
