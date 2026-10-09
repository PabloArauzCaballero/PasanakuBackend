# Servicio `garantia`

Modulo 08 de la boveda — Garantía, Incumplimiento, Cobranza y Sanciones.

> **Este README enlaza, no repite.** La especificacion esta en `docs/CasosDeUso/`;
> un dato en dos lugares diverge.

| | |
| --- | --- |
| **Esquema** | `garantia` |
| **Rol de base** | `svc_garantia` |
| **Prefijos de ruta** | `/garantia` · `/incumplimientos` · `/cobranza` |
| **Contrato** | [`openapi/garantia.yaml`](src/main/resources/openapi/garantia.yaml) |
| **Paquete** | `bo.aportaya.garantia` |

## Casos de uso

| CU | Nombre | Estado |
| --- | --- | --- |
| CU-23 | Cubrir un incumplimiento con el fondo | Implementado |
| CU-25 | Declarar el incumplimiento con descargo y evidencia | Implementado |
| CU-26 | Ejecutar el aval y subrogar la deuda | Implementado |
| CU-27 | Restringir al deudor e incluirlo en la lista interna | Implementado |
| CU-29 | Devolver los aportes del fondo de garantia | Implementado |
| CU-66 | Reemplazar a un participante moroso | Implementado |
| CU-67 | Disolver el grupo anticipadamente | Implementado |

## Lo que hay que saber antes de tocar esto

**El expediente es append-only.** `registro_incumplimiento` bloquea UPDATE y DELETE: su
columna `estado` guarda el estado **al detectar**, no el de hoy. El estado corriente
vive en `historial_estado_incumplimiento` — cada transicion es una fila con su motivo,
quien la hizo y cuando. Es mas trabajo que un UPDATE y es lo unico que sostiene el
debido proceso: un expediente cuyo estado se puede reescribir no prueba nada.

**El plazo de descargo entra al abrir.** `notificado_en` y `fecha_limite_subsanacion`
estan en esa misma tabla append-only, asi que no se pueden escribir despues. Declarar el
incumplimiento y comunicarselo a la persona es **un solo acto**: enterarse despues de
que el plazo empezo a correr es no tener plazo.

**Cubrir no perdona.** La cobertura deja una deuda contra quien incumplio. Si borrara la
obligacion, el fondo seria un seguro gratuito pagado por los que si pagan.

**Los topes no son mezquindad.** Cubrir todo, siempre, vacia el fondo. Gana el mas chico
de cuatro limites, y el que mando viaja en la respuesta: el grupo tiene derecho a saber
por que se cubrio lo que se cubrio.

**La restriccion no le cierra la puerta a pagar.** Bloquear al deudor de todo, incluido
el camino para regularizarse, garantiza que no se regularice.

## Respaldo empresarial del pozo (carril C, H7/H8)

Un mecanismo APARTE del fondo mutual (CU-23 original, `fondo_garantia`): la empresa reserva
capacidad por ciclo y cubre lo que no llego al corte para que el pozo se entregue completo.
Rutas `/garantia/respaldo/*`; el permiso de cada una vive en `GarantiaController` y la
traduccion en `RespaldoWeb` (el generador agrupa por primer segmento de la ruta).

| Clase | Hace |
| --- | --- |
| `CU23ReservarRespaldo` | Reserva (compite por la fila de capacidad, con candado), amplia (contingencia) y libera |
| `CU23CubrirFaltanteDelCorte` | UNICA puerta HTTP para cubrir: pregunta a `aportes` el faltante, no cree al cliente |
| `CU23CubrirConRespaldo` | Escritura atomica de la cobertura (todo o nada), una por turno |
| `CU23RecuperarRespaldo` | Un aporte tardio devuelve a la empresa lo que adelanto, una vez por pago |
| `CU23ReversarCobertura` | Contra-asiento; no se reversa una cobertura con recuperaciones |

- Los contadores de `reserva_respaldo` y `capacidad_respaldo` son CACHE de `movimiento_reserva`
  (append-only): los mantiene el trigger `tg_movimiento_reserva_aplica` (R-GAR-09).
- Los asientos NO se escriben aca: viajan como `partidas` en el evento para que nucleo-financiero
  las registre con CU-24. Cuentas LOGICAS (`CAJA_EMPRESA`, `RESERVA_RESPALDO`, `POZO_GRUPO`):
  el mapeo al plan de cuentas es de contabilidad. **Hoy no hay consumidor**: ver el plan del carril C.
- Orden de candados: reserva, luego cobertura, luego linea. Siempre.
- Parametros SINTETICOS: el tope de capacidad lo carga gobierno (sin fila se deniega); ninguna
  tarifa ni politica de recuperacion esta decidida (ambiguedades A1-A4).

## Lo que este servicio NO puede hacer

- Leer el esquema de otro servicio. No tiene `GRANT`, y jOOQ no le genero las
  clases (invariante 11).
- Escribir el libro contable, salvo que sea `nucleo-financiero` (invariante 12).
- Usar JPA (ADR-016).
- Publicar a Kafka dentro de una transaccion: para eso esta el outbox (ADR-018).

## Como se trabaja acá

```bash
docker compose --profile base up -d --wait
./gradlew :servicios:garantia:generateJooq
./gradlew :servicios:garantia:bootRun
```

Skills: `arrancar-carril` primero, despues las diecinueve de todo carril de backend.
