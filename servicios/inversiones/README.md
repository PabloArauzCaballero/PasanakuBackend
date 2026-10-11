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
| CU-126 | Distribuir los intereses al titular y conciliarlos contra el devengo | Implementado |
| CU-127 | Estimar y cobrar la comisión por resultado (marca máxima por lote) | Implementado; fórmula sintética (DR-INV-04) |
| CU-128 | Informar comprobantes y reporte fiscal separables, reconciliados con el libro | Implementado; tratamiento fiscal sintético (DR-INV-03) |
| CU-129 | Simular la caja de una inversión sin tocar saldos | Implementado |

Los diez del módulo 15. La inversión de dinero de grupos sigue desactivada: la simulación (CU-129) es una
cuenta separada que no lee ni mueve ningún saldo.

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

**Los trabajos programados están cableados** (ver «Trabajos programados»): reintento de las
instrucciones pendientes al libro, devengo diario y sincronizaciones con el aliado, con bloqueo entre
réplicas, clave de idempotencia reservada en `corrida_trabajo`, plazo máximo y política de reintento.

**Identidad de servicio.** Debitar la inversión y acreditar el rescate se piden al núcleo **solo con la
credencial de servicio**; sin ella fallan cerrados y quedan pendientes. Retener, liberar y leer el saldo llevan
el token de la persona (o, en un reintento del reloj sin sesión, la credencial de servicio).

La credencial es el token de acceso de una **cuenta de servicio** con el rol `SERVICIO_INVERSIONES`, emitido por
identidad (RS256, 15 min, el único emisor, ADR-024). Dos formas, ambas secretas y por entorno (nunca en el repo):

1. **Cuenta de servicio** (la forma de operar): `INVERSIONES_SERVICIO_TELEFONO`, `INVERSIONES_SERVICIO_CLAVE` y un
   segundo factor (`INVERSIONES_SERVICIO_TOTP_SEMILLA`; en TEST, con el perfil `local` de identidad,
   `INVERSIONES_SERVICIO_FACTOR_FIJO`). El servicio entra por `POST /sesiones` (CU-04) y **renueva el token solo**
   antes de que venza (`CredencialPorLogin`); si identidad no responde espera 30 s antes de volver a intentar
   (cada intento fallido cuenta para el bloqueo de la cuenta) y mientras tanto falla cerrado. Configuración a
   medias = el servicio no arranca.
2. **Token emitido** (`INVERSIONES_LIBRO_CREDENCIAL_SERVICIO`): vive minutos; sirve para una prueba manual.

El nucleo reconoce al servicio por el permiso `SERVICIO_INVERSIONES` del token (el reclamo `rol` de un token real
es `BACKOFFICE`; `IdentidadDeServicio` en nucleo-financiero lo mapea). **Pendiente del lado de identidad (no es de
este servicio):** hoy identidad solo valida un segundo factor real en el perfil `local` (código fijo), y no existe
el endpoint de credenciales de cliente que pide el ADR-028; hasta entonces TEST usa la cuenta de servicio con el
factor fijo y producción no puede usarla. Aprovisionar la cuenta (usuario, rol `SERVICIO_INVERSIONES`, factor) es
una acción del entorno, no de una semilla (la tabla de usuarios no entra a `seeders/minimos`).

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
| `inversiones.interes_distribuido` | El interés de un DPF se repartió y concilió con el devengo (CU-126) |
| `inversiones.distribucion_con_discrepancia` | El interés se repartió pero no coincide con el devengo propio (CU-126) |

## Eventos que consume

| Tema | De quien | Efecto |
| --- | --- | --- |
| *(ninguno)* | — | El libro y el aliado se consumen por contrato HTTP (puertos `LibroDelTitular` y `AliadoDeInversion`), no por eventos |

## Trabajos programados

| Bloqueo | Cron (La Paz) | Que hace |
| --- | --- | --- |
| `inversiones.reintento-instrucciones` | Cada minuto | Reintenta las instrucciones pendientes con espera exponencial (30 s a 1 h) y máximo de 12 intentos; sin pendientes no abre corrida (CU-122) |
| `inversiones.devengo-diario` | 00:30 | Devenga los DPF abiertos de hoy; clave `DEV:<fecha>` (CU-123) |
| `inversiones.sincronizar-valores` | Cada hora, :15 | Trae el último valor de cuota de cada fondo; clave `VAL:<fecha y hora>` (CU-123) |
| `inversiones.sincronizar-catalogo` | 06:00 | Trae el catálogo del aliado; clave `CAT:<fecha>` (CU-120) |

Cada corrida deja su fila en `corrida_trabajo` (`GET /inversiones/corridas`, `ADMIN_PLATAFORMA`), los
medidores `inversiones.instrucciones.pendientes` y `.atascadas` se leen de la base y el contador
`inversiones.corridas` cuenta por operación, estado y origen. `aportaya.inversiones.trabajos.habilitados=false`
los apaga; `plazo-maximo` (45 s) corta un barrido largo entero y lo deja `PARCIAL`.

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
