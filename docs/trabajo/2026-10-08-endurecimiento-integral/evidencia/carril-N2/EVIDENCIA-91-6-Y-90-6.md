# Carril N2 · evidencia de dinero (regla 91.6) y de seguridad (regla 90.6)

Peldaño: TESTED contra Postgres 16 real y Testcontainers (`04-completa-2.log`, conteos `05-conteos.txt`). Los asientos
pegados salen de las pruebas (`07-salidas-91-6.txt`, impresos con `FixturaDeSistema.imprimir`). `entregas` y el
productor de eventos `garantia` son DOBLES de contrato; no hubo broker Kafka real ni los servicios reales.

## 91.6 por operación
| Operación | 1 Asientos con suma | 2 Idempotencia 2x | 3 Cuadre desde el mayor | 4 Reversa | 5 Redondeo |
|---|---|---|---|---|---|
| debitarInversion | `CU13InversionTest.criterio1`: DEBITO titular 300.00 / CREDITO custodia 300.00, suma 300.00=300.00 | `reintentoMismaClave` (mismo transaccionId, movimientos antes/después iguales); `concurrencia` 8 claves distintas → 1 | `sistemaFx.saldoDelLibro` (SUM movimientos − SUM retenciones VIGENTES) 1000→700; custodia +300 | `reversa`: espejo, original intacto | no hay división: monto == retenido exacto, 299.99/300.01 rechazados (AP-CU13-06); 3 decimales ni se construyen |
| acreditarRescate | `CU12RescateTest.criterio5`: DEBITO custodia / CREDITO titular 300.00 | misma clave, mismo rescate con otra clave, 6 en paralelo → 1 transacción | `idaYVuelta`: titular+custodia 4590.00 antes y después | `reversa` | idem |
| pagarAlTercero | `CU13MercadoTest.criterio2`: DEBITO comprador 500 / CREDITO vendedor 480 / CREDITO plataforma 20 | `reintento` (aunque entregas ya esté caído/LIQUIDADA), `unaSolaEjecucion`, `concurrencia` 6 → 1 | libro: vendedor 480.00, plataforma +20.00 | `reversa` | `repartoExacto`: 33.33 al vendedor, 66.67 a la plataforma; el residuo SIEMPRE va a la plataforma (cargos = retenido − precio), nunca se pierde |
| CU-24 en USD / eventos | `CU24MonedaTest.asientoEnDolares` (debe=haber, moneda USD); `CU24EventosTest.criterio5` | `criterio5` (evento 2x → 1 asiento), `CU24EventosReinicioTest` (caída antes/después del commit, 8 consumidores) | saldo de cada cuenta del mayor por naturaleza | `criterio6`: contra-asiento enlazado `asiento_reversa_id`, original CONFIRMADO; `reversaEnDolares` conserva moneda | importes sin redondeo (BigDecimal escala 2, `Dinero.de` rechaza >2 decimales) |

## H2.S1.M5 (proyección vs libro)
`CU12ProyeccionTest`: caché `saldo_disponible=9999.00`, libro 100.00 → transferir 500 rechazado AP-CU12-01, libro intacto;
retener 500 rechazado AP-CU13-01; la consulta informa 100.00. `CuentaBilleteraRepositorio` ya no lee las columnas `saldo_*`
para decidir: las calcula del libro.

## 90.6 Hallazgo ALTO: operaciones servicio a servicio
1. Amenaza: un usuario con `BILLETERA_OPERAR` pagaba la retención de otro a un beneficiario elegido por él, se acreditaba rescates o debitaba
   inversiones ajenas.
2. Control: (a) ruta con permiso de identidad de servicio: `BilleteraController.java` `pagarAlTercero` → `SERVICIO_ENTREGAS`,
   `debitarInversion` y `acreditarRescate` → `SERVICIO_INVERSIONES` (roles de sistema nuevos en `seeders/minimos/10-roles-y-permisos.json`);
   (b) se vuelve a exigir en el caso de uso (`ReglasDeEjecucion.exigirServicio`, AP-CU13-09 / AP-CU12-08); (c) `pagarAlTercero` NO
   tiene cuerpo: beneficiario y precio los trae `entregas` (`PagoDeCompraEnMercado` → `OfertasDelMercado`), el cargo = retenido − precio, la retención
   debe nombrar la oferta (`referencia_tipo=OFERTA_MERCADO`), y sin confirmación de entregas no se paga (falla cerrada); (d) `debitarInversion` verifica
   `titularId` (opcional, lo informa el servicio llamador) contra la dueña de la billetera; (e) unicidad por origen en la base (R-BIL-24).
3. Pruebas: `InversionesWebTest` (usuario final con BILLETERA_OPERAR / TESORERIA / servicio equivocado → 403 y el caso de uso no se invoca; sin sesión → 401;
   campo fuera de contrato → 400); `CU13InversionTest.usuarioFinalNoDebita`, `titularQueNoEs`; `CU13MercadoTest.usuarioFinalNoPaga`,
   `ofertaNoConfirmada`, `beneficiarioIncorrecto`, `retencionNoPagable`; `CU12RescateTest.soloElServicio`.
4. Resultado: `05-conteos.txt` (todo verde).
5. Riesgo residual: (i) nadie aprovisiona todavía credenciales con el rol `SERVICIO_*` (identidad); hasta entonces solo el actor sistema puede llamar;
   (ii) `GET /entregas/mercado/ofertas/{id}` es contrato PROPUESTO, `entregas` no lo publica: en producción el pago al vendedor falla cerrado;
   (iii) retención de OTRO titular: el llamador de servicio no es el titular, así que no se compara con la sesión; se compara solo `titularId` si lo envía.
