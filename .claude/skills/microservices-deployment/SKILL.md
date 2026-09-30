---
name: microservices-deployment
description: Desplegar servicios de forma independiente sin romper a los vecinos — despliegue por servicio y por qué no hay "release del sistema", compatibilidad hacia atrás y hacia adelante durante la ventana en que conviven dos versiones, migraciones de base expand/contract compatibles con la versión anterior corriendo, feature flags para desacoplar deploy de release, orden de despliegue cuando hay cambio de contrato, canary y rollback por servicio, y matriz de versiones para saber qué está corriendo. Usar antes de desplegar un servicio, al planificar un cambio que toca dos, al escribir una migración, o al diseñar el rollback de un release.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Despliegue de microservicios

La ventaja de partir el sistema es poder desplegar una pieza sin tocar las otras. Esa ventaja se
pierde en el momento en que hace falta coordinar el despliegue de cuatro servicios en un orden
exacto: eso es un monolito con más pasos y más riesgo.

## 1. Cada servicio se despliega solo

- **No existe "el release del sistema".** Cada servicio tiene su versión, su pipeline y su
  historial (`github-releases-versioning`).
- Si un cambio **obliga** a desplegar dos servicios juntos y en orden, el contrato está mal
  diseñado: se rehace con expand/contract (`service-contracts-versioning` §4).
- Excepción legítima y rara: un cambio coordinado planificado, con orden escrito, ventana y
  rollback ensayado. Se documenta como excepción, no como costumbre.

## 2. Durante el despliegue conviven dos versiones

Aunque despliegues "todo junto", hay minutos con instancias v1 y v2 al mismo tiempo. Eso obliga a
**doble compatibilidad**:

| Dirección | Qué significa | Ejemplo |
|---|---|---|
| **Hacia atrás** | La v2 entiende lo que produjo la v1 | Evento sin el campo nuevo: default sensato |
| **Hacia adelante** | La v1 tolera lo que produce la v2 | Campo desconocido ignorado, no error |

La compatibilidad hacia adelante se gana **antes**: un consumidor tolerante desplegado hoy
permite que el productor evolucione mañana (`service-contracts-versioning` §3). Si tus
consumidores no son tolerantes, cada cambio es un despliegue coordinado.

## 3. Migraciones: expand / contract también en la base

Una migración corre mientras la versión anterior **sigue sirviendo tráfico**. Por eso:

| Fase | Migración | Código |
|---|---|---|
| 1. Expand | Agregar columna **nullable** o con default; crear índice `CONCURRENTLY` | v1 sigue andando |
| 2. Backfill | Rellenar por lotes, sin lock largo | v1 sigue andando |
| 3. Deploy | — | v2 escribe y lee lo nuevo, sigue escribiendo lo viejo |
| 4. Verificar | Métrica de uso de lo viejo en cero | |
| 5. Contract | Quitar la columna vieja | v3 ya no la menciona |

**Prohibido en una sola migración:** renombrar una columna en uso, agregar `NOT NULL` sin default,
crear índice bloqueante sobre tabla grande, o borrar algo que la versión anterior todavía lee.
Cada una tira producción durante el rollout (`postgresql-advanced`).

**La migración tiene que poder convivir con un rollback del código.** Si desplegás v2, migrás y
tenés que volver a v1, la base ya cambió: por eso la fase 1 es siempre compatible con v1.

## 4. Deploy ≠ release: feature flags

- **Deploy** es poner el código en producción. **Release** es que los usuarios lo vean.
- Un flag permite desplegar la pieza apagada y encenderla cuando los demás servicios están
  listos. Es la alternativa sana al despliegue coordinado.
- Flag con **dueño y fecha de retiro**. Un flag viejo es una rama muerta que duplica los caminos
  posibles y nadie prueba (`technical-debt-management`).
- El flag de un flujo de dinero se enciende por porcentaje y con un plan de apagado inmediato.

## 5. Orden cuando hay cambio de contrato

Regla simple: **primero el que tolera, después el que cambia.**

| Cambio | Orden |
|---|---|
| Productor agrega campo | Consumidores tolerantes ya desplegados → productor |
| Consumidor necesita campo nuevo | Productor (expand) → consumidor |
| Se retira un campo | Todos los consumidores migrados y verificados → productor (contract) |
| Evento con versión nueva | Productor publica v1 **y** v2 → consumidores migran → productor deja de publicar v1 |

"Verificado" significa **medido**: métrica de consumo de la versión vieja en cero, no "creo que
ya migraron todos".

## 6. Estrategia de salida a producción

| Estrategia | Cuándo en Pasanaku |
|---|---|
| **Rolling** | Default para servicios sin estado |
| **Canary** | Obligatorio para `pagos`, `contabilidad`, `entregas` y `garantia`: un porcentaje del tráfico, con métricas comparadas antes de seguir |
| **Blue/green** | Cuando el rollback tiene que ser instantáneo y la base lo permite |
| **Big bang** | Nunca en un servicio de dinero |

Criterios de aborto del canary, escritos **antes**: tasa de error, latencia p95, y una métrica de
negocio (asientos por minuto, acreditaciones fallidas). Sin métrica de negocio, un canary verde
puede estar perdiendo plata en silencio.

## 7. Rollback

- **Por servicio**, a la versión anterior, con un comando en el runbook y probado
  (`release-and-rollback`).
- Si la migración ya corrió, el rollback del código tiene que seguir funcionando: por eso la
  fase expand.
- **Un rollback en medio de una saga** deja sagas colgadas: el estado terminal
  `FALLIDA_REQUIERE_HUMANO` y su alerta son lo que evita que queden invisibles.
- Documentá cuánto tarda el rollback. Un rollback de 40 minutos no es un rollback: es un
  incidente largo.

## 8. Matriz de versiones

Tiene que existir en algún lado consultable en 10 segundos:

| Servicio | Versión | Desplegado | Contratos que produce | Contratos que consume |
|---|---|---|---|---|
| `pagos` | 1.8.2 | 2026-09-20 14:02 | `aporte.acreditado` v2 | `grupo.periodo-abierto` v1 |

En un incidente, la primera pregunta es "¿qué cambió y quién está en qué versión?". Si la
respuesta tarda, el incidente dura más (`incident-response-postmortem`).

## Anti-patrones

- Desplegar los servicios en un orden exacto y frágil, sin plan escrito.
- Migración que rompe la versión anterior mientras sigue sirviendo.
- `ALTER TABLE ... NOT NULL` sin default sobre tabla con tráfico.
- Índice sin `CONCURRENTLY` sobre tabla grande en producción.
- Flags eternos sin dueño ni fecha.
- Canary sin métrica de negocio y sin criterio de aborto escrito.
- Rollback nunca ensayado.
- Big bang en un servicio de dinero.
- No saber qué versión de qué servicio está corriendo.

## Checklist

- [ ] El cambio se puede desplegar sin coordinar con otro servicio; si no, se rehízo con expand.
- [ ] Compatibilidad hacia atrás y hacia adelante verificada para la ventana de convivencia.
- [ ] Migración en fase expand, compatible con la versión anterior y con el rollback.
- [ ] Índices creados `CONCURRENTLY`; backfill por lotes.
- [ ] Flags con dueño y fecha de retiro.
- [ ] Orden de despliegue declarado cuando hay cambio de contrato, con verificación medida.
- [ ] Canary con criterios de aborto, incluida una métrica de negocio, para servicios de dinero.
- [ ] Rollback documentado, cronometrado y ensayado.
- [ ] Matriz de versiones actualizada.

## Evidencia / DoD

1. Salida del despliegue y del health check posterior, por servicio.
2. Salida de la migración, con el tiempo de ejecución y sin locks largos.
3. Prueba de convivencia: v1 y v2 procesando el mismo evento correctamente.
4. Métricas del canary antes y después, con el criterio de aborto aportedo.
5. Salida del rollback ensayado, con el tiempo que tardó.
