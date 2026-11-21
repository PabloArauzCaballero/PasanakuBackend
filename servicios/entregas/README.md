# Servicio `entregas`

Modulo 04 de la boveda — Entregas de Fondo.

> **Este README enlaza, no repite.** La especificacion esta en `docs/CasosDeUso/`;
> un dato en dos lugares diverge.

| | |
| --- | --- |
| **Esquema** | `entregas` |
| **Rol de base** | `svc_entregas` |
| **Prefijos de ruta** | `/entregas` · `/desembolsos` · `/cuentas-bancarias` |
| **Contrato** | [`openapi/entregas.yaml`](src/main/resources/openapi/entregas.yaml) |
| **Paquete** | `bo.aportaya.entregas` |

## Casos de uso

| CU | Nombre | Estado |
| --- | --- | --- |
| CU-18 | Registrar y verificar una cuenta bancaria de destino | Implementado |
| CU-22 | Liquidar y entregar el fondo | Implementado |
| CU-28 | Emitir la orden de desembolso y ejecutar el intento | Implementado |

Los tres del modulo 04.

## Lo que hay que saber antes de tocar esto

**El numero de cuenta no existe en claro en ninguna parte.** Ni en una columna, ni en
un evento, ni en un log. Lo que se guarda es el cifrado, un hash con **pimienta** —que
vive en el almacen de secretos, no junto al hash— y un enmascarado con cuatro digitos.
Sin pimienta el hash es adivinable: el espacio de numeros de cuenta posibles es chico.

**Verificada no es utilizable.** Tras verificar corre una ventana de enfriamiento, y su
fin **se guarda** en `bloqueada_hasta`. Si se recalculara al consultar, acortar la
politica liberaria de golpe todas las cuentas que estaban enfriando.

**Los totales de la entrega los recalcula la base.** `tg_deduccion_recalcula` los
actualiza en cada deduccion. Escribirlos a mano permitiria que un neto y sus
deducciones dejaran de coincidir sin que nada avise.

**Un intento de desembolso es una fila.** La que se abre al enviar la orden es la misma
que se cierra con la respuesta del proveedor. Agregar otra al contestar convertiria un
intento en dos, y el conteo que decide si se reintenta dejaria de significar algo.

## Pozo completo (carril C, H8) y cesion de turnos retirada

**Pozo completo** (`CU22EntregarPozoCompleto`, `POST /entregas/pozo-completo`). Recibir mesa es
recibir el pozo entero: `aportes` afirma lo confirmado, `garantia` cubre el faltante con la
reserva, y recien ahi se escribe (`RegistroDelFondeo`, una transaccion local). Si el respaldo no
alcanza, la entrega queda BLOQUEADA con el pozo como principal, el faltante como deuda
conservada (`fondeo_entrega.monto_pendiente`) y una incidencia critica. `CU22LiquidarEntrega`
sigue ahi, intacto, pero su `recaudado` lo afirma el cliente: es el camino anterior.

**Cesion de turnos: RETIRADA** (D-L2-1, `DECISIONES.md` tanda 7, 2026-11-21). El mercado del derecho a cobrar
(`/entregas/mercado/*`, CU-130) y la subasta de cupos (`/entregas/subastas*`, CU-64 por subasta) ya no existen: el
orden de cobro lo fijan el nivel de puntualidad y el sorteo. Las operaciones siguen en el contrato como `deprecated`
y responden `410` (`AP-CU130-10`, `AP-CU136-09`) hasta que la app y `nucleo-financiero` las suelten (regla 98.1.3);
`GET /entregas/mercado/ofertas/{id}` responde el `422 AP-CU130-05` del pacto y nucleo no paga. Las tablas
`oferta_turno`, `cesion_derecho`, `subasta_cupo` y `puja_subasta` siguen hasta la contraccion: `CesionRepositorio`
solo LEE las cesiones ya liquidadas, para que el fondeo le pague el pozo a quien compro el turno y no dos veces.

## Lo que este servicio NO puede hacer

- Leer el esquema de otro servicio. No tiene `GRANT`, y jOOQ no le genero las
  clases (invariante 11).
- Escribir el libro contable, salvo que sea `nucleo-financiero` (invariante 12).
- Usar JPA (ADR-016).
- Publicar a Kafka dentro de una transaccion: para eso esta el outbox (ADR-018).

## Como se trabaja acá

```bash
docker compose --profile base up -d --wait
./gradlew :servicios:entregas:generateJooq
./gradlew :servicios:entregas:bootRun
```

Skills: `arrancar-carril` primero, despues las diecinueve de todo carril de backend.

## Carril O2

`entregas.desembolso_acreditado` lleva el `asiento` propuesto (`AsientoDeEntrega`, origen `ENTREGA`); el contrato con nucleo-financiero y
lo que quedo contra doble esta en `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-C/contrato-nucleo-partidas-y-pago-vendedor.md`.

## Tercera vuelta (carril B3)

- **Circuit breaker** en `RecaudoPorHttp`, `RespaldoPorHttp` y `TitularPorHttp` (`ClienteConCircuito`, parametros sinteticos en
  `aportaya.resiliencia.circuito.*`).
