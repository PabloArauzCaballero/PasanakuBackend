# H10.S1.M1–M7 y H11.S1.M3–M5 — estado honesto tras la tercera vuelta (carril C3)

Reemplaza la tabla de `evidencia/carril-I2/contrato-libro-y-doble.md` §4, que estaba escrita cuando el productor real no existía.
Conteos: `conteos-carril-C3.txt` (corrida sobre el esquema ACTUAL, Postgres 16 desechable en 5443, RC 0).
Peldaño: **TESTED** por servicio contra PostgreSQL real; el par cliente/servidor del libro con pacto compartido. No `REGRESSION_VERIFIED` global.

Qué cambió desde la corrida de las 19:39 (invalidada): el esquema de nucleo cambió (moneda por cuenta contable, R-BIL-24/25/26) y nucleo ahora
PUBLICA `debitarInversion` y `acreditarRescate`. Por eso se repitió TODO: inversiones `test 39 · webTest 33 · integrationTest 147 ·
contractTest 32 · sagaTest 8 · testBarrido 2`, 0 fallos, 0 errores, 0 omitidas.

| ID | Estado | Con el productor real | Qué sigue siendo doble / sintético |
|---|---|---|---|
| H10.S1.M1 modelar producto, emisor y condiciones | HECHO (backend) | — (catálogo propio de inversiones) | El aliado es un simulador: fuente `SINTETICO`, `apto_produccion=false` por CHECK. Pantallas diferenciadas: frontend |
| H10.S1.M2 consentimiento y orden, saldo retenido | HECHO | `retener` y `saldo` contra el libro REAL: `CU13PactoInversionesTest` (nucleo, Spring completo + Postgres + JWKS real) y `LibroPorHttpPactoContratoTest` (cliente real) sobre el MISMO pacto | `aportes` (lo comprometido en pozos) es doble HTTP con la forma de su OpenAPI |
| H10.S1.M3 posición sin duplicar saldo | HECHO | `debitarInversion` real: el libro del titular y la custodia cuadran (1000,00 → 700,00 → 1000,00; custodia delta 0,00) | El aliado confirma por simulador |
| H10.S1.M4 calcular y mostrar DPF | HECHO | n/a (cálculo propio; 180 devengos = 197,26) | Tasas sintéticas (DR-INV-02) |
| H10.S1.M5 valorar cuotas | HECHO | n/a | Valores de cuota sintéticos |
| H10.S1.M6 rescate y liquidación | HECHO | `acreditarRescate` real: una sola transacción `RESCATE_INVERSION` por rescate (R-BIL-24), 403 a una persona | El rescate lo confirma el simulador, no un emisor |
| H10.S1.M7 inversión de grupos desactivada + simulación de caja | HECHO | n/a (`AP-CU121-05`, CU-129) | Tasas de la simulación sintéticas |

**A MEDIAS (honesto):** nada de H10 queda sin productor real en el libro. Lo que NO se puede llamar «real» es todo lo que cuelga del aliado
(DR-INV-01) y los parámetros comerciales y fiscales (DR-INV-02/03/04): son sintéticos y marcados, y ningún camino los promueve a producción.
La credencial de servicio hacia nucleo existe y se renueva sola, pero identidad solo valida un segundo factor real en el perfil `local`
(ver `credenciales-de-servicio.md`): en un entorno no-local inversiones no obtiene credencial hasta que identidad la provea.

| ID | Estado | Detalle |
|---|---|---|
| H11.S1.M3 distribuir intereses | HECHO (backend) | CU-126, `distribucion_interes` append-only, conciliación contra la liquidación externa y el devengo (`carril-I2/H11-S1-M3-…`). Retenciones: sintéticas (DR-INV-03) |
| H11.S1.M4 comisión por resultado | HECHO en código; **parámetro SINTÉTICO** | Pérdida, recuperación por tramos, lotes y residuo de redondeo: `CU125ComisionTest` (5) y `CU127Test` (9: ejemplo del plan 100 cuotas, marca 110, valor 112, 10 % → Bs 20,00; `AP-CU127-01/02/03` los emite un camino real). La tasa, la base y el momento de cobro son DR-INV-04 (legal, fiscal y aliado): abierta, registrada, nadie la cierra en código |
| H11.S1.M5 comprobantes y reporte fiscal | HECHO (backend) | CU-128 `GET /inversiones/reportes/fiscal` y exportación; principal, comisión, impuesto e interés separables y conciliados contra el libro. **No** es un documento tributario: tratamiento fiscal indefinido (DR-INV-03) |

## Decisiones de negocio abiertas (siguen sintéticas y registradas, nadie las adivinó)

DR-INV-01 aliado (dirección y legal) · DR-INV-02 tasas, plazos y costos (aliado y producto) · DR-INV-03 tratamiento fiscal (fiscal) ·
DR-INV-04 comisión de éxito (legal, fiscal y aliado) · DR-INV-06 titularidad y custodia (legal y proveedor). **DR-INV-05 (contrato de débito y
crédito en nucleo) QUEDA CERRADA en este carril**: el productor existe, está probado y su pacto es `contratos/pactos/inversiones-nucleo-financiero.json`.
DR-INV-07 no figura en la tabla del ADR (`carril-D/ADR-inversiones.md` llega a DR-INV-06): numeración a confirmar con quien la citó.
