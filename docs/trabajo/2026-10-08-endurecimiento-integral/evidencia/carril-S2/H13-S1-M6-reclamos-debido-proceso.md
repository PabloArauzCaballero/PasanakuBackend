# H13.S1.M6 · Reclamos y restricciones con debido proceso

CA del plan: «Dado caso en revisión, cuando se gestiona, entonces no equivale automáticamente a sanción firme o
deuda ejecutable» (E-H13-06: trazabilidad de reclamo, resolución y efecto permitido).

## Dónde vive cada cosa (y por qué se hizo acá lo que se hizo)

| Pieza | Servicio | Tabla / caso de uso | Tocado en este carril |
|---|---|---|---|
| Reclamo del cliente y su respuesta | cumplimiento | `cumplimiento.reclamo_cliente`, CU-52 | no (ya existía) |
| Elevación y resolución en instancia superior | cumplimiento | `instancia_reclamo`, CU-53 | no |
| **¿Frena un reclamo la ejecución de una deuda o la firmeza de una sanción?** | cumplimiento | `EfectoDelReclamo` + `ConsultarEfectoDelReclamo` | **sí (nuevo, con pruebas)** |
| Restricción del deudor, cobranza | garantia | `/cobranza/restricciones`, CU-27 | no (otro agente) |
| Sanción al organizador, apelación | organizador | `/organizadores/{id}/sanciones`, CU-93 | no (otro agente) |

El código que decide si un reclamo frena vive en cumplimiento (es dueño de la tabla y del plazo), así que se hizo
ahí. Quienes ejecutan o dejan firme algo —garantia (cobranza) y organizador (sanciones)— son de otros agentes:
para ellos queda el **contrato** de abajo y un **doble**.

## La regla implementada

`servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/dominio/EfectoDelReclamo.java` (política pura,
derivada del estado cada vez; no se guarda en ninguna columna que pueda quedar vieja):

| Estado del caso | ¿Frena sanción firme? | ¿Frena deuda ejecutable? | Motivo |
|---|---|---|---|
| `INGRESADO`, `EN_ANALISIS`, `RESPONDIDO` | sí | sí | `EN_REVISION` |
| instancia `PRESENTADA`/`EN_TRAMITE` sin resolver | sí | sí | `INSTANCIA_SIN_RESOLVER` |
| instancia resuelta con `monto_resarcido > 0` | sí | sí | `RESARCIMIENTO_DISPUESTO` (la plataforma debe, no cobra) |
| `CERRADO` + `FAVORABLE`/`PARCIAL` | sí | sí | `FAVORABLE_AL_CLIENTE` (hay reparación, R-CON-04) |
| `CERRADO` + `DESFAVORABLE`, dentro de `fecha_respuesta + dias-para-elevar` | sí | sí | `PLAZO_DE_ELEVACION_ABIERTO` (el cliente aún puede elevar) |
| `CERRADO` + `DESFAVORABLE`, vencido ese plazo | no | no | `TERMINADO_SIN_BLOQUEO` |
| `CERRADO` + `DESISTIDO`, o `ELEVADO` ya resuelto sin resarcimiento | no | no | `TERMINADO_SIN_BLOQUEO` |
| estado o resultado no reconocido, o `DESFAVORABLE` sin fecha | **sí** | **sí** | `ESTADO_NO_RECONOCIDO` (denegar por omisión, invariante 9) |

`dias-para-elevar` es `aportaya.reclamos.dias-para-elevar` (30, en `application.yml`): ningún plazo está cableado.
`ConsultarEfectoDelReclamo.delUsuario(usuarioId, producto?, ctx)` devuelve **el rastro entero** —cada reclamo
con su código, categoría, producto, estado, resultado, fechas y el motivo— y los dos indicadores agregados. No
devuelve el texto del reclamo ni de la respuesta.

**Que ningún reclamo frene es condición necesaria, no suficiente**: la causal, la notificación probada, el
descargo y la apelación siguen siendo del proceso que aplica el efecto (skill `debido-proceso`, regla 91.4).

## Pruebas

| Prueba | Nivel | Qué demuestra |
|---|---|---|
| `EfectoDelReclamoTest` (9, `test`) | unidad, **tres niveles** (regla 65) | correcto (en revisión frena; favorable frena), **límite** (el último instante del plazo de elevación frena; un segundo después ya no; resarcimiento frena aunque sea viejo), **inválido** (nulo, estado inventado, cerrado sin resultado, resultado raro, desfavorable sin fecha: todos frenan) |
| `CU52EfectoDelReclamoTest` (4, `integrationTest`) | PostgreSQL real, usando CU-52 y CU-53 | el ciclo de vida completo: ingresado frena; cerrado en contra frena y **deja de frenar** al vencer el plazo; elevado sin resolver frena, resuelto sin monto libera, resuelto con resarcimiento frena; reclamo de un producto no frena la deuda de otro; persona sin reclamos libre |

## Contrato para quien ejecuta o sanciona (doble, regla 65)

Garantia (restricciones, cobranza) y organizador (sanciones) deben consultar **antes** de dejar firme o ejecutable.
No se agregó la ruta HTTP en este carril: sumar un endpoint obliga a regenerar clientes, inventario de endpoints,
colección Postman, rutas del gateway y frontend, que son de otros dueños. La propuesta de contrato:

```yaml
/reclamos/usuarios/{usuarioId}/efecto:
  get:
    operationId: consultarEfectoDeReclamos
    parameters: [ { name: producto, in: query, required: false, schema: { type: string } } ]
    # permiso: PARTICIPANTE (lo propio) o GRUPO_ADMINISTRAR/SOPORTE (de otro) — mismo patron que
    # GET /cobranza/restricciones/vigentes/{usuarioId} (CobranzaController.java:49)
    responses: { '200': { schema: EfectoDeReclamos } }   # bloqueaSancionFirme, bloqueaDeudaEjecutable, casos[]
```

El **doble** para que los dos servicios sigan sin esperar: un puerto `ReclamosQueFrenan.efecto(usuarioId, producto)`
cuyo adaptador HTTP **deniega ante cualquier fallo** (sin respuesta = frena), probado en los tres niveles con
`DependidoDeEnsayo` (comun-pruebas): correcto (`bloqueaDeudaEjecutable=false` → procede), límite (`casos` vacío →
procede; un solo caso que frena entre diez → no procede), inválido (caído, 5xx, JSON roto → no procede). Esa parte
**no está escrita** (es código de garantia y organizador).

## Decisiones de negocio abiertas (no se cierran con código)

1. **Alcance del bloqueo**: ¿un reclamo de un producto frena TODA la deuda de la persona o sólo la del mismo
   producto/categoría? Se implementó por producto (filtro opcional). A confirmar con Cumplimiento y Legal.
2. **Plazo de elevación**: se usa el valor de configuración existente (30). Que ese plazo sea el que ampara al
   cliente es una decisión normativa (ASFI); no se verificó contra la norma en este carril.
3. **Medida cautelar** (suspensión urgente por fraude, que el debido proceso admite sin descargo): este
   componente nunca la autoriza; si un reclamo frena, la cautelar sigue siendo una decisión humana registrada.

## No cubierto

- El camino HTTP y el consumo desde garantia/organizador (ver arriba): sin ruta, sin cliente, sin doble.
- Las restricciones (`restriccion_usuario`) y las sanciones (`sancion_organizador`) no se modificaron: que **ellas**
  consulten este efecto antes de pasar a firmes es trabajo de sus dueños.
- No se probó la política de fila de `reclamo_cliente` con un administrador leyendo reclamos ajenos (haría falta un
  contexto de sistema en el consumidor, como hace `Consultas` en grupos).


## Cierre en el carril A3 (2026-10-10)

La ruta HTTP existe: `GET /reclamos/usuarios/{usuarioId}/efecto` (`consultarEfectoDeReclamos`, permiso `RECLAMO_VER`, 403 identico
exista o no la persona), con prueba de contrato del productor. El contrato para garantia y organizador y el doble de
`DependidoDeEnsayo` (`EfectoDeReclamosDeEnsayo`, tres niveles) estan en `evidencia/carril-A3/contrato-efecto-reclamo.md`.
Pendiente de otro agente: los adaptadores `...PorHttp` de garantia y organizador (hoy cerrados solo contra el doble) y las
decisiones de negocio de arriba (alcance por producto, plazo de elevacion, quien consulta sin usuario).
