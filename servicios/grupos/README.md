# Servicio `grupos`

Modulo 02 de la boveda — Grupos, Cupos, Turnos y Gobernanza.

> **Este README enlaza, no repite.** La especificacion esta en `docs/CasosDeUso/`;
> un dato en dos lugares diverge.

| | |
| --- | --- |
| **Esquema** | `grupos` |
| **Rol de base** | `svc_grupos` |
| **Prefijos de ruta** | `/grupos` · `/turnos` · `/acuerdos` |
| **Contrato** | [`openapi/grupos.yaml`](src/main/resources/openapi/grupos.yaml) |
| **Paquete** | `bo.aportaya.grupos` |

## Casos de uso

| CU | Nombre | Estado |
| --- | --- | --- |
| CU-59 | Mantener el calendario de días no hábiles | ⬜ sin implementar |

| CU-60 | Sortear los turnos | ⬜ sin implementar |

| CU-63 | Proponer y votar un acuerdo | ⬜ sin implementar |

| CU-62 | Permutar turnos entre participantes | ⬜ sin implementar |

| CU-65 | Retirarse de un grupo | ⬜ sin implementar |

| CU-64 | Traspasar un cupo | ⬜ sin implementar |

| CU-69 | Invitar a un contacto y registrar sus referencias | ⬜ sin implementar |

| CU-68 | Postular a un grupo y ser emparejado | ⬜ sin implementar |

| CU-20 | Crear grupo y congelar tarifario | ⬜ sin implementar |

## Eventos que emite

| Tema | Cuando |
| --- | --- |
| `grupos.cupo_traspasado` | Un cupo cambio de titular (CU-64 o una cesion de derecho del mercado, `motivo=VENTA`) |

## Eventos que consume

| Tema | De quien | Efecto |
| --- | --- | --- |
| `aportaya.entregas.derecho_cedido` | `entregas` | El cupo del turno vendido pasa del vendedor al comprador (`AplicarCesionDeDerecho`): un traspaso `VENTA` con deuda 0, una sola vez por evento y por hecho; fuera de orden se rechaza sin marcar. Encendido por omision (`CESION_CONSUMO_HABILITADO`). **Pendiente de negocio:** las obligaciones futuras ya generadas a nombre del vendedor y `permite_cupos_multiples` del comprador |
| `aportaya.organizador.habilitacion_revocada` | `organizador` | Sustitucion del administrador (H5.S1.M7); apagado por omision (`SUSTITUCION_CONSUMO_HABILITADO`) |

## Trabajos programados

| Bloqueo | Cron | Que hace |
| --- | --- | --- |
| | | |

## Lo que este servicio NO puede hacer

- Leer el esquema de otro servicio. No tiene `GRANT`, y jOOQ no le genero las
  clases (invariante 11).
- Escribir el libro contable, salvo que sea `nucleo-financiero` (invariante 12).
- Usar JPA (ADR-016).
- Publicar a Kafka dentro de una transaccion: para eso esta el outbox (ADR-018).

## Como se trabaja acá

```bash
docker compose --profile base up -d --wait
./gradlew :servicios:grupos:generateJooq
./gradlew :servicios:grupos:bootRun
```

Skills: `arrancar-carril` primero, despues las diecinueve de todo carril de backend.

## Vencimiento local (carril O2, H7.S1.M3)

`GET /grupos/calendario/vencimiento` (CU-59): de un instante UTC devuelve la fecha UTC, la CONTRACTUAL (dia en la zona del contrato,
`America/La_Paz` por omision) y la de LIQUIDACION (primer dia habil), con los dias saltados y su motivo. Habil = sin feriado del
calendario sembrado, sin sabado y sin domingo. Sin calendario del anio: `AP-CU59-05`. `FeriadoConProcedencia` valida la fuente de un
feriado; `catalogo.dia_no_habil` es de solo lectura (ADR-029), asi que nada lo escribe desde aca.

## Tercera vuelta (carril B3)

- **Calendario de aportes** (H8.S1.M1, productor real): `GET /grupos/periodos/{periodoId}/calendario-de-aportes`, permiso
  `ADMIN_PLATAFORMA` (el token de quien dispara la generacion viaja desde `aportes`). Devuelve turno, fechas y, por cupo
  OCUPADO, titular e importe (aporte x fraccion, half-even). Lo consume `CalendarioDelPeriodoPorHttp` de `aportes`; el
  archivo dorado `src/test/resources/fixtures/calendario-de-aportes.json` es el cuerpo que ambos lados prueban.
- **Circuit breaker** en `HechosPorHttp` (ocho dependidos, un circuito cada uno): `infraestructura/clientes/ClienteConCircuito`,
  parametros SINTETICOS en `aportaya.resiliencia.circuito.*`. Con el circuito abierto cada pregunta responde lo que no deja pasar.
  `usuarioDelTelefono` usa `POST /usuarios/busquedas/por-telefono` (el telefono ya no va en la URL).
- **Interruptores** (decision pendiente de negocio y cumplimiento, ver `evidencia/carril-B3/DECISIONES-Y-ABIERTOS.md`):
  `aportaya.grupos.admision.humana-obligatoria` (`ADMISION_HUMANA_OBLIGATORIA`, true por omision) cierra las dos puertas que
  saltean la admision humana (`AP-CU69-10`, `AP-CU68-15`); `aportaya.grupos.sustitucion.automatica`
  (`SUSTITUCION_AUTOMATICA_ADMIN`, false = manual) y `...consumo-habilitado` gobiernan la sustitucion del administrador al revocarse la
  habilitacion del organizador (`SustitucionAutomatica`, consumidor Kafka apagado por omision).
- Diccionario de variables de riesgo del motor: `docs/operacion/diccionario-variables-de-riesgo.md`.
