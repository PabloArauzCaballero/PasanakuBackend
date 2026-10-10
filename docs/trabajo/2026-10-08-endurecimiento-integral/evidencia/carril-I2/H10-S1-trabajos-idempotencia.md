# Hallazgos del carril D cerrados en el carril I2 (R-INV-10)

## 1. Idempotency-Key exigida y no usada (sincronización y devengo)

**Antes:** el contrato exigía `Idempotency-Key` en `sincronizarCatalogoDeInversion`, `sincronizarValoresDeCuota` y
`correrDevengoDeDpf` y el controlador la descartaba.

**Ahora:** la clave se **reserva** en `corrida_trabajo` (`UNIQUE (operacion, clave_idempotencia)`, `R-INV-10`) por
`OperacionesProgramables`:

| Situación | Resultado | Prueba |
| --- | --- | --- |
| Misma clave, mismo contenido, ya terminada | Devuelve lo guardado, no corre otra vez | `CU123TrabajosTest.devengoConClave`, `.valoresConClave`, `CU122ReintentoTest.claveRepetida` |
| Misma clave, otro contenido | `AP-CU123-03` / `AP-CU122-03` / `AP-CU120-04` | `CU123TrabajosTest.claveConOtroContenido`, `CU122ReintentoTest.claveConOtroContenidoYCorridaEnCurso` |
| Misma clave, corrida en curso no vencida | `AP-CU123-04` / `AP-CU122-04` / `AP-CU120-05` | ídem |
| Corrida fallida o en curso vencida | Se retoma con la misma clave (misma fila) | `CU120OperacionesTest.sincronizacionConClave` |
| Ocho hilos con la misma clave | Una sola corrida | `CU122ReintentoTest.concurrenciaConLaMismaClave` |

**Sincronización de una orden y de un rescate** (`.../sincronizacion`): la clave pasa a ser **opcional** en el contrato
(`ClaveIdempotenciaOpcional`). Consultar al aliado y avanzar es idempotente por la máquina de estados (una orden o un rescate
ya resuelto no se mueve; pruebas `reintento: confirmar dos veces la misma orden`), y guardar el resultado de una consulta de
estado para reproducirlo sería devolver un estado viejo. Cambio compatible: quien la mande sigue funcionando.

## 2. Trabajos programados y reintento de instrucciones sin cablear

| Pieza | Cableado | Prueba |
| --- | --- | --- |
| `ReintentoDeInstrucciones` (cada minuto) | `@Scheduled` + `@SchedulerLock` + `@EnableScheduling` | `ArranqueTest.losTrabajosEstanCableados`, `CU122ReintentoTest.elTrabajoDelRelojReintenta` |
| `DevengoDiario` (00:30) | ídem | `CU123TrabajosTest.elTrabajoDevenga` |
| `SincronizacionDelAliado` (catálogo 06:00, valores cada hora) | ídem | `CU123TrabajosTest.lasSincronizacionesSobrevivenAlAliadoCaido` |
| Apagado por configuración | `aportaya.inversiones.trabajos.habilitados=false` | `ArranqueTest.losTrabajosSePuedenApagar` |

- **Timeout:** cada llamada al aliado/libro tiene el suyo (ya existía); el barrido de reintentos tiene un plazo máximo
  (`trabajos.plazo-maximo`, 45 s) y se corta entero → corrida `PARCIAL` (`CU122ReintentoTest.barridoQueSePasaDelPlazo`).
  Una corrida `EN_CURSO` más vieja que `trabajos.en-curso-caduca` (10 min) se da por caída y se retoma.
- **Política de reintento** (`PoliticaDeReintento`): espera exponencial con tope (30 s, 1 h) desde que nació la instrucción y
  máximo de 12 intentos; pasado el máximo **no se descarta**: queda pendiente y atascada (`CU122ReintentoTest.esperaAntesDeReintentar`,
  `.atascadaYReintentoAMano`). Parámetros técnicos, no comerciales.
- **Visibilidad:** fila en `corrida_trabajo` por vuelta con trabajo (nada si no hay pendientes), `GET /inversiones/corridas`
  (`ADMIN_PLATAFORMA`), medidores `inversiones.instrucciones.pendientes` / `.atascadas` (leídos de la base) y contador
  `inversiones.corridas{operacion,estado,origen}`; log `WARN` cuando hay atascadas
  (`CU122ReintentoTest.medidoresCoincidenConLaBase`).
- Reintento manual: `POST /inversiones/instrucciones/reintento` (`ADMIN_PLATAFORMA`), sin esperas ni tope.

## 3. Códigos declarados que ningún camino emitía

| Código | Decisión | Camino que lo emite / justificación |
| --- | --- | --- |
| `AP-CU120-01` PRODUCTO_INEXISTENTE | **Se emite** | `GET /inversiones/productos/{codigo}` (`CU120OperacionesTest.productoInexistente`) |
| `AP-CU120-03` RESPUESTA_DEL_ALIADO_NO_VERIFICABLE | **Se emite** | `RespuestaNoVerificable` (firma, ventana o forma) distinta de aliado caído (`CU120OperacionesTest.respuestaNoVerificable`, `AliadoSimuladoContratoTest.firmaInvalida`) |
| `AP-CU127-01` COMISION_NO_CONFIGURADA | **Se emite** | `GET /inversiones/posiciones/{id}/comision` sobre un DPF o un fondo sin tasa (`CU127Test.productoSinComision`) |

Ningún código se retiró del contrato.

## 4. Un defecto real encontrado y corregido

`CalculoDeLiquidacion` registraba la `marca_maxima_nueva` en `comision_exito` pero **nunca la guardaba en la posición**: un rescate
parcial con beneficio seguía comparando contra la marca de entrada y el siguiente rescate cobraba otra vez el mismo tramo de
ganancia (R-INV-07 dice que la marca solo sube). Ahora `descontarCuotas` la persiste, la base rechaza bajarla (trigger
`tg_posicion_inversion_marca_no_decrece`) y `CU127Test.rescateParcialNoCobraDosVecesElMismoTramo` lo demuestra (primer rescate
10,00; segundo 1,00 y no 11,00).
