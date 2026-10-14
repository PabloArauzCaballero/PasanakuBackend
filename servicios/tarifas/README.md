# Servicio `tarifas`

Modulo 11 de la boveda — Tarifas, Comisiones, Impuestos y Facturación.

> **Este README enlaza, no repite.** La especificacion esta en `docs/CasosDeUso/`;
> un dato en dos lugares diverge.

| | |
| --- | --- |
| **Esquema** | `tarifas` |
| **Rol de base** | `svc_tarifas` |
| **Prefijos de ruta** | `/tarifas` · `/comisiones` · `/facturas` |
| **Contrato** | [`openapi/tarifas.yaml`](src/main/resources/openapi/tarifas.yaml) |
| **Paquete** | `bo.aportaya.tarifas` |

## Casos de uso

| CU | Nombre | Estado |
| --- | --- | --- |
| CU-30 | Cotizar la comision antes de operar | Implementado |
| CU-31 | Devengar y cobrar la comision | Implementado |
| CU-32 | Emitir factura electronica | Implementado, contra el **simulador** del servicio de impuestos (default declarado del proyecto) |
| CU-33 | Devolver comision y emitir nota de credito | Implementado |
| CU-34 | Publicar un tarifario nuevo con preaviso | Implementado |
| CU-35 | Cerrar la liquidacion mensual de ingresos | Implementado |
| CU-36 | Segmentar comercialmente y aplicar precio diferenciado | Implementado |

Los siete del modulo 11. Ninguno queda a medias.

## Eventos

| Tema | Direccion | Efecto |
| --- | --- | --- |
| `aportaya.grupos.grupo_creado` | consume (`grupos`) | CU-20 · congela el precio del grupo (`CongelarTarifaDelGrupo`): `tarifa_congelada_grupo` con el snapshot del tarifario (conceptos activos y sus reglas) y su hash SHA-256, una sola vez por evento y por grupo (R-TAR-07). Encendido por omision (`CONGELAMIENTO_CONSUMO_HABILITADO`). **Supuesto A2 (a confirmar con producto y finanzas):** el contenido del snapshot |
| `tarifas.tarifa_congelada` | emite | Responde al `grupo_creado`; `grupos` lo consume para abrir el grupo a inscripciones. Tema nuevo: `despliegue/host/crear-topicos.sh` lo provisiona en el broker |

## Lo que hay que saber antes de tocar esto

**El devengo es append-only.** `tg_devengo_comision_append_only` bloquea UPDATE *y*
DELETE. La columna `estado` guarda el estado **al devengar**, no el de hoy; el estado
corriente lo deriva `EstadoDelDevengo` de los `cargo_comision` y las
`devolucion_comision`. Es mas trabajo que un UPDATE y es la unica forma honesta: un
ingreso que se puede reescribir no prueba nada.

**Un tarifario vigente es inmutable.** `tg_concepto_tarifa_inmutable` rechaza insertar
o editar conceptos de un tarifario VIGENTE o SUSTITUIDO. Para cargar conceptos hay que
hacerlo en BORRADOR y recien despues activarlo — las fixturas de prueba siguen ese
orden por eso, no por gusto.

**Los tres datos que este servicio no puede saber llegan como entrada**: los dias sin
cerrar y el saldo del mayor son de `nucleo_financiero`, y las excepciones de
conciliacion son de `aportes`. No se leen esos esquemas (invariante 11).

## Lo que este servicio NO puede hacer

- Leer el esquema de otro servicio. No tiene `GRANT`, y jOOQ no le genero las
  clases (invariante 11).
- Escribir el libro contable, salvo que sea `nucleo-financiero` (invariante 12).
- Usar JPA (ADR-016).
- Publicar a Kafka dentro de una transaccion: para eso esta el outbox (ADR-018).

## Como se trabaja acá

```bash
docker compose --profile base up -d --wait
./gradlew :servicios:tarifas:generateJooq
./gradlew :servicios:tarifas:bootRun
```

Skills: `arrancar-carril` primero, despues las diecinueve de todo carril de backend.
