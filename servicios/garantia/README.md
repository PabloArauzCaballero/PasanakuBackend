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
| CU-23 | Cubrir un incumplimiento con el fondo | Pendiente: la cobertura manual (F1.L2.M02) y el respaldo empresarial (F2.L2.M02) se retiraron; la cobertura con el fondo del grupo llega con F3.L2.M05 |
| CU-25 | Declarar el incumplimiento con descargo y evidencia | Implementado |
| CU-26 | Ejecutar el aval y subrogar la deuda | Retirado (F2.L2.M04) |
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

## Respaldo empresarial del pozo: RETIRADO (F2.L2.M02)

La empresa no reserva ni cubre nada (ADR-055 E7, ADR-056): el faltante del pozo lo cubre el fondo del grupo
(F3.L2.M05). Las seis rutas `/garantia/respaldo/*` quedan `deprecated` en `garantia.yaml` 1.3.0 y responden
410 `AP-CU23-15` sin efecto (`RespaldoRetiradoWebTest`); se borran del contrato con la contraccion. Los casos de
uso del respaldo, la contingencia de caja, `AsientoPropuesto` (cuentas `CAJA_EMPRESA` / `RESERVA_RESPALDO`) y
sus repositorios ya no existen, y `garantia` no publica ningun topico `respaldo_*`. Las tablas del respaldo y sus
restricciones (R-GAR-08 a R-GAR-13) siguen en el esquema hasta F2.L2.M03.

`RecaudoPorHttp` (puerto `RecaudoDelPeriodo`) queda sin uso hasta F3.L2.M05, que vuelve a leer de `aportes`
la mora y el monto de la cuota impaga.

## Eventos que consume

| Tema | De quien | Efecto |
| --- | --- | --- |
| `aportaya.nucleo_financiero.aporte_aplicado` | `nucleo-financiero` | Solo con `destino = FONDO_GARANTIA` (hoy, el recargo por mora cobrado): suma al `saldo_disponible` del fondo del grupo y deja un `movimiento_fondo` `RECARGO_MORA` (`IngresoAlFondo`). Idempotente por `evento_consumido`. No suma a `total_aportado`: el recargo no se devuelve al cerrar el fondo. Grupo sin fondo propio: se rechaza sin marcar y el reproceso lo aplica. Se apaga con `aportaya.garantia.consumo-de-aportes-aplicados.habilitado=false` |

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

## Tercera vuelta (carril B3)

- `RecaudoPorHttp` lleva circuit breaker (`ClienteConCircuito`, parametros sinteticos en `aportaya.resiliencia.circuito.*`); con
  `aportes` caido o con el circuito abierto el recaudo es vacio y la cobertura se rechaza (`AP-CU23-12`).
