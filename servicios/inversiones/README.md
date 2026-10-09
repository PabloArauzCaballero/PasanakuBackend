# Servicio `inversiones`

Modulo 15 de la boveda — Inversiones voluntarias.

> **Este README enlaza, no repite.** La especificacion esta en `docs/CasosDeUso/`;
> un dato en dos lugares diverge.

| | |
| --- | --- |
| **Esquema** | `inversiones` |
| **Rol de base** | `svc_inversiones` |
| **Prefijos de ruta** | `/inversiones` |
| **Contrato** | [`openapi/inversiones.yaml`](src/main/resources/openapi/inversiones.yaml) |
| **Paquete** | `bo.aportaya.inversiones` |

## Casos de uso

| CU | Nombre | Estado |
| --- | --- | --- |
| CU-120 | Consultar productos de inversión y sus condiciones | Implementado (contra el aliado simulado) |
| CU-121 | Aceptar las condiciones y ordenar una inversión | Implementado (contra dobles del libro y del aliado) |
| CU-122 | Confirmar la posición sin duplicar el saldo | Implementado (contra dobles del libro y del aliado) |
| CU-123 | Devengar y valorar una posición de inversión | Implementado (contra el aliado simulado) |
| CU-124 | Solicitar el rescate de una inversión | Implementado (contra dobles del libro y del aliado) |
| CU-125 | Liquidar el rescate y acreditar al titular | Implementado (contra dobles del libro y del aliado) |

Los seis del módulo 15. El contrato también etiqueta `CU-127` (comisión de éxito) y
`CU-128` (listado de comprobantes); hoy los absorben CU-125 y CU-123.

## Lo que hay que saber antes de tocar esto

**La inversión de dinero de grupos está DESACTIVADA.** Un saldo afectado a un aporte o a
un pozo no se invierte (`AP-CU121-05`). Este servicio es inversión **voluntaria** del
titular con su saldo libre.

**Todo parámetro comercial o fiscal es SINTÉTICO.** Tasas, plazos, base de días,
mínimos, costos, retención, penalización, comisión de éxito, riesgo y titularidad
salen marcados `origenDatos: SINTETICO` y `aptoProduccion: false` (la base lo exige,
`R-INV-01`). El único aliado es un simulador (`herramientas/aliado_simulado`) y
`GuardiaDeProduccion` impide arrancar un proceso productivo con él.

**No se activa contra un núcleo real.** El débito y el crédito de inversión, el motivo de
retención y el saldo con lo comprometido en pozos no existen en el contrato publicado de
`nucleo-financiero`. El pedido está en `docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-D/contrato-requerido-nucleo.md`
y hoy el adaptador HTTP solo está probado contra el consumidor.

**Ningún trabajo programado está cableado.** `AplicadorDeInstrucciones.reintentarPendientes`,
el devengo diario y la sincronización de valores de cuota se disparan por API o desde las
pruebas.

## Eventos que emite

| Tema | Cuando |
| --- | --- |
| `inversiones.condiciones_publicadas` | Nace una versión nueva de condiciones de un producto (CU-120) |
| `inversiones.orden_creada` | Se registra el consentimiento y la orden (CU-121) |
| `inversiones.saldo_reservado` | El libro retuvo el importe de la orden (CU-122) |
| `inversiones.posicion_constituida` | El aliado confirmó y nació la posición (CU-122) |
| `inversiones.orden_rechazada` | La orden se rechazó y lo retenido se libera (CU-122) |
| `inversiones.instruccion_aplicada` | Se aplicó en el libro una instrucción de débito o liberación (CU-122) |
| `inversiones.rescate_solicitado` | Se registró un pedido de rescate (CU-124) |
| `inversiones.rescate_rechazado` | El aliado rechazó el rescate (CU-125) |
| `inversiones.rescate_confirmado` | El rescate se liquidó y quedó pendiente de acreditar (CU-125) |
| `inversiones.rescate_liquidado` | El libro acreditó el rescate (CU-125) |

## Eventos que consume

| Tema | De quien | Efecto |
| --- | --- | --- |
| *(ninguno)* | — | El libro y el aliado se consumen por contrato HTTP (puertos `LibroDelTitular` y `AliadoDeInversion`), no por eventos |

## Trabajos programados

| Bloqueo | Cron | Que hace |
| --- | --- | --- |
| *(ninguno cableado)* | — | Pendiente: reintentar instrucciones pendientes, devengo diario de DPF y sincronización de valores de cuota |

## Decisiones abiertas

Ningún parámetro de negocio de este servicio tiene fuente aprobada. Hasta que cada
decisión se tome, el dato es sintético y no apto para producción. Detalle en
`docs/trabajo/2026-10-08-endurecimiento-integral/evidencia/carril-D/ADR-inversiones.md`.

| ID | Decisión | Dueño | Caso de uso que la usa |
| --- | --- | --- | --- |
| DR-INV-01 | Qué aliado (banco o SAFI), con qué facultades y quién contrata con el cliente | Dirección + legal | CU-120 |
| DR-INV-02 | Tasas, plazos, base de días, mínimos y costos reales de cada producto | Aliado + producto | CU-121 |
| DR-INV-03 | Tratamiento fiscal de intereses y de la comisión (alícuotas, retención, rescate anticipado) | Fiscal | CU-125 |
| DR-INV-04 | Existencia, base contractual y fórmula de la comisión de éxito (marca máxima, cristalización) | Legal + fiscal + aliado | CU-125 |
| DR-INV-05 | Contrato en `nucleo-financiero` para el débito de inversión y el crédito de rescate | Carril A (núcleo) | CU-124 |
| DR-INV-06 | Titularidad y custodia de las posiciones | Legal + proveedor | CU-122 |
| DR-INV-07 | Permiso propio de inversión en el catálogo de roles (hoy se reutilizan los de la billetera) | Seguridad + producto | CU-125 |

## Lo que este servicio NO puede hacer

- Leer el esquema de otro servicio. No tiene `GRANT`, y jOOQ no le genero las
  clases (invariante 11).
- Escribir el libro contable, salvo que sea `nucleo-financiero` (invariante 12).
- Usar JPA (ADR-016).
- Publicar a Kafka dentro de una transaccion: para eso esta el outbox (ADR-018).

## Como se trabaja acá

```bash
docker compose --profile base up -d --wait
./gradlew :servicios:inversiones:generateJooq
./gradlew :servicios:inversiones:bootRun
```

Skills: `arrancar-carril` primero, despues las diecinueve de todo carril de backend.
