---
name: service-contracts-versioning
description: Contratos entre servicios de Pasanaku y cómo evolucionan sin romper a nadie — qué es compatible y qué no, versionado de API y de eventos, consumidores tolerantes, expand/contract para cambios incompatibles, pruebas de contrato dirigidas por el consumidor (Pact) en CI, catálogo de eventos versionado, deprecación con fecha y cómo detectar un breaking change antes del merge. Usar al crear o modificar cualquier endpoint interno, evento de integración o payload que consuma otro servicio, al agregar o quitar un campo, y antes de mergear un cambio que otro equipo consume.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Contratos entre servicios y su versionado

Un contrato entre servicios no es la documentación: es **una promesa que otro equipo desplegó en
producción confiando en ella**. Romperla no se nota en tu CI; se nota en el turno de guardia del
otro.

La regla dura está en **98.1**. Acá está cómo evolucionar sin romper.

## 1. Qué es compatible y qué no

| Cambio | ¿Compatible? | Nota |
|---|---|---|
| Agregar un campo **opcional** a una respuesta | ✅ | Solo si los consumidores toleran campos desconocidos |
| Agregar un campo **opcional** a un request | ✅ | Con default que preserva el comportamiento anterior |
| Agregar un valor a un enum **de salida** | ⚠️ | Rompe al consumidor con `switch` exhaustivo. Se anuncia |
| Agregar un valor a un enum **de entrada** | ✅ | |
| Quitar o renombrar un campo | ❌ | Incompatible |
| Cambiar el tipo (`string` → `number`, `number` → `string`) | ❌ | Incompatible, aunque "se parsee igual" |
| Hacer obligatorio un campo antes opcional | ❌ | Incompatible |
| Cambiar la **semántica** con el mismo tipo | ❌❌ | El peor: nada falla, todo miente |
| Endurecer una validación | ❌ | Requests que antes pasaban, ahora fallan |
| Cambiar un código de error o su significado | ❌ | Es parte del contrato (`error-handling-contract`) |
| Cambiar el orden de una lista sin garantía previa | ⚠️ | Si alguien dependía, ya estaba roto |

**El cambio semántico es el que más plata cuesta.** Si `monto` pasaba de ser bruto a ser neto y
el tipo sigue siendo `DECIMAL(14,2)`, ningún test de esquema lo detecta y `entregas` empieza a
descontar dos veces. **Un cambio de semántica se trata como incompatible, siempre**, y se hace con
un campo nuevo y nombre distinto.

## 2. Versionado

### API sincrónica

- Versión en la ruta para lo público (`/v1/…`); para lo interno, versión en la ruta o en
  cabecera, pero **una sola convención en toda la casa**.
- La versión cambia solo ante un cambio incompatible. Agregar campos no bump-ea nada.
- Dos versiones conviven mientras haya consumidores en la vieja, con **fecha de retiro
  declarada** en el momento de publicar la nueva. Sin fecha, la vieja vive para siempre.

### Eventos

```ts
{
  id: '…', type: 'aporte.acreditado', version: 2,   // ← versión del payload
  occurredAt: '…', correlationId: '…', payload: { … }
}
```

- `type` + `version` identifican el esquema. **No reutilices `type` con semántica nueva.**
- Cambio incompatible ⇒ `version: 2`, y el productor publica **las dos** hasta que todos los
  consumidores migren. Lo verificás mirando quién consume la 1, no preguntando.
- Un evento no se "corrige" republicándolo: ya pasó. Se emite un evento nuevo que lo rectifica
  (`aporte.acreditacion-reversada`).

## 3. Consumidores tolerantes (Postel aplicado con criterio)

```ts
// ✅ tolera campos desconocidos y no se cae por lo que no conoce
const dto = EsquemaAporte.parse(payload);   // esquema que ignora extras

// ❌ falla ante cualquier campo nuevo del productor
if (Object.keys(payload).length !== 4) throw new Error('payload inesperado');
```

- **Ignorá lo que no conocés, validá lo que usás.** Un consumidor que revienta porque el
  productor agregó un campo convierte un cambio compatible en un incidente.
- Pero **validá siempre lo que sí usás**: el payload es entrada externa. Esquema al consumir, sin
  excepción, incluso entre servicios propios.
- Enum de entrada desconocido ⇒ tratalo como "otro" o rechazá explícitamente, nunca asumas.

## 4. Expand / contract: cómo se hace un cambio incompatible

Tres despliegues, en este orden, nunca en uno:

1. **Expand** — el productor agrega lo nuevo y mantiene lo viejo. Escribe los dos. Nadie rompe.
2. **Migrate** — cada consumidor pasa a lo nuevo, a su ritmo. Se verifica consumidor por
   consumidor, con evidencia de cuál quedó migrado.
3. **Contract** — cuando no queda nadie en lo viejo (verificado por métrica de uso, no por
   memoria), el productor lo retira.

Ejemplo real: renombrar `monto` a `monto_bruto` en `entrega.liquidada`.
Expand: emitir los dos campos con el mismo valor → Migrate: `contabilidad` y `confianza` pasan a
`monto_bruto` → Contract: se deja de emitir `monto` y se anuncia con fecha.

**Prohibido el atajo de "lo cambio y aviso por el chat".** El chat no es un mecanismo de
despliegue.

## 5. Pruebas de contrato dirigidas por el consumidor

Un test de contrato es lo único que hace que el CI del **productor** se ponga rojo cuando rompe
al **consumidor**. Sin eso, el contrato es una costumbre (regla 98.1.4).

```
consumidor (contabilidad)                    productor (pagos)
  define qué espera del evento     ──pacto──▶  el CI de pagos verifica
  y corre su test contra un doble              que su salida real cumple
  que cumple el pacto                          todos los pactos publicados
```

- El pacto lo define **quien consume**, no quien produce. Es lo que realmente usa, no todo lo que
  el productor ofrece.
- El CI del productor falla si rompe cualquier pacto publicado. Ese job es **bloqueante**
  (`code-quality-gates`).
- Aplica igual a eventos: el consumidor declara los campos que lee y sus tipos.
- No reemplaza a los tests de integración; reemplaza al E2E gigante que nadie mantiene
  (`microservices-testing`).

## 6. Catálogo de contratos

Versionado en el repo, actualizado en el mismo PR que el cambio:

| Qué | Dónde | Contenido |
|---|---|---|
| API sincrónica | `openapi/<servicio>.yaml` | Spec OpenAPI 3.1 (`api-openapi-docs`) |
| Eventos | `contratos/eventos/<tipo>.v<n>.json` | Esquema, productor, consumidores conocidos, ejemplo |
| Pactos | `contratos/pactos/` | Un pacto por par consumidor–productor |

Regla: **un evento sin ficha en el catálogo no se publica a producción.** Un catálogo
desactualizado es peor que no tenerlo, porque se le cree.

## 7. Detectar el breaking change antes del merge

- Diff de OpenAPI en CI (herramienta de breaking-change detection) con salida bloqueante.
- Diff de esquema de eventos contra la versión anterior en el índice.
- Test de contrato de todos los consumidores registrados.
- Revisión humana enfocada en la columna "semántica": lo único que las herramientas no ven
  (`code-review-standard`).

## Anti-patrones

- Entidad del ORM publicada como respuesta o como evento.
- "Es un campo interno, nadie lo usa" — sin haber mirado quién lo usa.
- Versionar todo a cada cambio: `v7` en tres meses es un contrato que nadie estabilizó.
- Nunca versionar: un solo `v1` que cambió de significado cuatro veces.
- Dos versiones conviviendo sin fecha de retiro.
- Consumidor que valida el payload entero y revienta por campos nuevos.
- Consumidor que no valida nada y confía.
- Cambiar semántica manteniendo el nombre.
- Anunciar el cambio por chat y considerarlo coordinado.

## Checklist

- [ ] El cambio está clasificado como compatible o incompatible según la tabla de §1.
- [ ] Si es incompatible: versión nueva, convivencia y fecha de retiro declarada.
- [ ] Ningún cambio de semántica viaja con el nombre viejo.
- [ ] Consumidores identificados por búsqueda en el código, no de memoria.
- [ ] Pacto del consumidor actualizado y corriendo en el CI del productor.
- [ ] Catálogo de eventos / OpenAPI actualizado en el mismo PR.
- [ ] Detector de breaking changes en CI, bloqueante.
- [ ] Consumidor tolerante a campos desconocidos y validando lo que usa.

## Evidencia / DoD

1. Salida del detector de breaking changes sobre el diff, pegada.
2. Salida del job de pactos del productor, con todos los consumidores en verde.
3. Lista de consumidores encontrados, con el comando de búsqueda usado.
4. Para un incompatible: el PR de expand, la verificación de migración de cada consumidor y la
   fecha de contract anotada.
