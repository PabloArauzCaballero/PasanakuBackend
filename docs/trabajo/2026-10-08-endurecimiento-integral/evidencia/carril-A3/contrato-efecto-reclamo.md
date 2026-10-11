# Contrato: efecto de un reclamo sobre una sancion firme o una deuda ejecutable

H13.S1.M6 (debido proceso, regla 91.4). Productor: `cumplimiento`. Consumidores (los implementa otro agente):
`garantia` (cobranza / restricciones) y `organizador` (sanciones).

## La ruta

```
GET /api/v1/reclamos/usuarios/{usuarioId}/efecto[?producto=APORTE]
operationId: consultarEfectoDeReclamos        (servicios/cumplimiento/src/main/resources/openapi/cumplimiento.yaml)
permiso:     RECLAMO_VER                       (seeders/minimos/10-roles-y-permisos.json)
```

- `RECLAMO_VER` lo tienen `SOPORTE`, `PUNTO_RECLAMO`, `OFICIAL_CUMPLIMIENTO` y `ADMIN_PLATAFORMA`. NO lo tiene un participante,
  ni un administrador de grupo (`GRUPO_ADMINISTRAR`), ni los roles de servicio (`SERVICIO_ENTREGAS`, `SERVICIO_INVERSIONES`).
  Quien no lo tiene recibe 403 `AP-SEG-01`, exista o no la persona; sin token, 401.
- Lee, no escribe: sin `Idempotency-Key`. Seguro de reintentar.
- Un caso no interpretable FRENA (denegar por omision). `producto` opcional: solo cuentan los reclamos de ese producto.

### Respuesta 200 (`EfectoDeReclamos`)

```json
{
  "usuarioId": "uuid",
  "bloqueaSancionFirme": true,
  "bloqueaDeudaEjecutable": true,
  "motivoGlobal": "SIN_RECLAMOS",            // solo cuando no hay reclamos que mirar; si no, ausente/null
  "casos": [{
    "reclamoId": "uuid", "codigo": "RC-2026-0001", "categoria": "COBRO_INDEBIDO", "producto": "APORTE",
    "estado": "RESPONDIDO", "resultado": null, "fechaIngreso": "...Z", "fechaRespuesta": "...Z",
    "bloqueaSancionFirme": true, "bloqueaDeudaEjecutable": true,
    "motivo": "EN_REVISION"
  }]
}
```

`motivo` (por caso): `EN_REVISION`, `INSTANCIA_SIN_RESOLVER`, `RESARCIMIENTO_DISPUESTO`, `FAVORABLE_AL_CLIENTE`,
`PLAZO_DE_ELEVACION_ABIERTO`, `TERMINADO_SIN_BLOQUEO`, `ESTADO_NO_RECONOCIDO` (y `SIN_RECLAMOS` en `motivoGlobal`).
Los agregados son el OR de los casos. No viaja el texto del reclamo ni de su respuesta.
La regla completa (tabla estado -> frena) esta en `evidencia/carril-S2/H13-S1-M6-reclamos-debido-proceso.md`.

## Regla para el consumidor (obligatoria)

**Todo lo que no sea un 200 entendible FRENA.** Servicio caido, timeout, 4xx/5xx (incluido 403), JSON roto, falta de campos,
tipos equivocados: "no se pudo saber si hay un reclamo" nunca se lee como "no hay reclamo". Ademas, aunque el agregado diga
`false`, un solo caso con el indicador en `true` manda. Que nada frene es condicion necesaria, NO suficiente: causal,
notificacion probada, descargo y apelacion siguen siendo del proceso que aplica el efecto.

- `garantia`: antes de volver ejecutable una deuda / dejar firme una restriccion -> mira `bloqueaDeudaEjecutable`.
- `organizador`: antes de dejar firme una sancion -> mira `bloqueaSancionFirme`.
- Las llamadas entre servicios reenvian el token del usuario que origino el pedido (hoy no hay token de servicio con alcance
  acotado: hallazgo 6 de H13.S1.M2). El usuario tiene que tener `RECLAMO_VER` (hoy: `ADMIN_PLATAFORMA` lo tiene, que es quien
  sanciona en `organizador`); si el flujo de `garantia` corre con un `GRUPO_ADMINISTRAR` o sin token (trabajo programado), la
  consulta da 403/401 y por la regla de arriba FRENA: es fail-closed pero BLOQUEA el cobro. Resolverlo es una decision
  (token de servicio con alcance `RECLAMO_VER`, o dar el permiso al rol que ejecuta): ver "Decisiones abiertas".

## El doble (regla 65), ya escrito

`plataforma/comun-pruebas/src/main/java/bo/aportaya/plataforma/pruebas/EfectoDeReclamosDeEnsayo.java`
(sobre `DependidoDeEnsayo`). Habla exactamente el JSON de arriba y trae la lectura de referencia del consumidor
(`leer(estado, cuerpo, Que.DEUDA_EJECUTABLE | Que.SANCION_FIRME)`), que es la que se copia al adaptador `...PorHttp`.

```java
try (var cumplimiento = new EfectoDeReclamosDeEnsayo().sinReclamos(persona)) {
    // el adaptador real apunta a cumplimiento.urlBase()
    assertThat(cumplimiento.consultar(persona, Que.DEUDA_EJECUTABLE)).isEqualTo(Decision.PROCEDE);
}
```

Tres niveles ya ejercitados (`EfectoDeReclamosDeEnsayoTest`, comun-pruebas):

| Nivel | Casos |
|---|---|
| correcto | sin reclamos procede (deuda y sancion); un caso en revision frena las dos |
| limite | uno que frena entre diez libres frena; diez libres proceden; `soloFrenaLaSancion` frena la sancion y no la deuda; agregado `false` con un caso que frena -> FRENA |
| invalido | servicio caido (conexion rechazada, 0 peticiones), 401/403/404/500/502/503, cuerpos vacios / no-JSON / `{}` / `[]` / sin `casos` / tipo equivocado / null, respuesta mas lenta que el tiempo maximo (1 s) -> todos FRENAN |

Prueba de contrato del lado productor: `ReclamosControllerWebTest` (cumplimiento) le da a `EfectoDeReclamosDeEnsayo.leer`
la respuesta REAL del controlador; si el productor cambia la forma, cae.

## Lo que queda contra el doble (no contra el servicio real)

El adaptador, el cortacircuitos y el timeout de cada consumidor se verifican contra el doble. La integracion con `cumplimiento`
levantado (dos servicios, token real) sigue pendiente de quien implemente los consumidores.

## Decisiones abiertas (no se cierran con codigo)

1. Alcance del bloqueo: por producto (implementado, filtro opcional) o TODA la deuda de la persona. -> Cumplimiento y Legal.
2. Plazo de elevacion (`aportaya.reclamos.dias-para-elevar`, 30): que sea el que ampara al cliente es norma ASFI. -> Legal.
3. Quien consulta cuando no hay usuario humano (trabajo programado de cobranza): token de servicio con alcance acotado
   (98.6.3) o asignar `RECLAMO_VER` al rol que ejecuta. -> Seguridad / arquitectura de plataforma.
4. Medida cautelar por fraude: este componente nunca la autoriza; sigue siendo una decision humana registrada.
