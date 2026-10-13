---
name: data-privacy-financial
description: Gate obligatorio de privacidad para datos personales y financieros — clasificación por sensibilidad, minimización por vista y por endpoint, dónde NUNCA pueden aparecer (logs, URLs, trazas, métricas, analytics, reportes, eventos), cifrado y enmascarado, seudonimización vs anonimización, retención y borrado con el límite de lo que la contabilidad obliga a conservar, datos de prueba sintéticos, y auditoría de acceso y de exportes. Usar en todo PR que toque datos de una persona o un importe atribuible, al agregar un log o una métrica, al exponer un endpoint, al armar datos de prueba, o antes de exportar cualquier cosa.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Privacidad de datos personales y financieros

Pasanaku sabe cuánto gana, cuánto debe y con quién se junta cada participante. Esa combinación
—identidad + dinero + red social— es más sensible que cualquiera de las tres por separado: sirve
para extorsionar, para discriminar y para robar.

Este es un **gate obligatorio**: aplica aunque el ticket no lo mencione. Las prohibiciones duras
están en la **regla 90.2**.

## 1. Clasificá antes de tocar

| Nivel | Qué | Regla |
|---|---|---|
| **Crítico** | Documento de identidad, cuenta bancaria, credenciales, tokens, payload de QR, datos biométricos del KYC | Nunca en logs, URLs, eventos, analytics ni reportes. Cifrado en reposo. Acceso auditado |
| **Sensible** | Importes atribuibles a una persona, deuda, mora, score, expediente de incumplimiento, teléfono, dirección | Mínimo necesario, auditado, jamás en URL |
| **Interno** | Identificadores (`cupo_id`, `pago_id`, `grupo_id`), estados, fechas | Se pueden loguear |
| **Público** | Catálogos, moneda, nombre del grupo si el grupo es público | Libre |

Un `cupo_id` es interno. "Bs 350 del cupo 44" ya es sensible: revela el monto y la posición.
**En duda, tratalo como el nivel de arriba.**

## 2. Minimización por vista y por endpoint

- Cada endpoint devuelve **solo lo que esa pantalla muestra**. El `SELECT *` mapeado a DTO
  filtra en el papel y filtra datos en la práctica (regla 90.1.8).
- El listado de un grupo no necesita el documento de nadie. El panel de transparencia muestra
  **totales del grupo**, no el detalle de cada participante, salvo lo que el reglamento aceptado
  permita.
- Un participante ve **su** detalle financiero. Que estén en el mismo grupo no da derecho a ver
  la deuda del otro: eso lo define el reglamento y se resuelve en el servidor
  (`authz-access-control`).
- En eventos entre servicios: **identificadores, no contenidos**. El consumidor autorizado
  consulta el resto al dueño (`service-communication-patterns`).

## 3. Dónde NUNCA pueden aparecer

| Lugar | Por qué |
|---|---|
| **Logs y trazas** | Se replican, se exportan a terceros y se conservan meses |
| **URL (path o query)** | Historial, proxies, logs de acceso del gateway y de cada salto |
| **Mensajes de error** | Llegan al cliente y a Sentry (`frontend-error-monitoring`) |
| **Métricas y etiquetas** | Alta cardinalidad y exportadas fuera |
| **Analytics y píxeles** | Terceros por definición |
| **Atributos de span** | Indexados en el backend de trazas (`distributed-tracing-correlation`) |
| **Nombres de archivo y rutas de storage** | Se filtran en URLs y en listados |
| **`PLAN.md` / `REPORTE.md`** | Regla 40; si una salida los traía, se enmascara **y se aclara** |
| **Mensajes de commit y PRs** | Públicos para siempre en el historial |

```ts
// ❌
logger.info(`Acreditado Bs ${monto} de ${usuario.nombre} (${usuario.documento})`);
GET /api/pagos?documento=1234567&monto=350

// ✅
logger.info({ evento: 'aporte.acreditado', obligacionId, correlationId });
GET /api/pagos?obligacionId=obl_44
```

## 4. Cifrado y enmascarado

- **En tránsito**: TLS en todo, incluido entre servicios (`service-to-service-security`).
- **En reposo**: cifrado de disco como piso; cifrado a nivel de columna para documento de
  identidad, cuenta bancaria y datos del KYC.
- **Tokens y credenciales**: solo hash con *pepper*, nunca el valor plano (regla 90.2.9).
- **Enmascarado en UI y soporte**: cuenta bancaria como `****3421`, documento parcial. La
  pantalla de soporte no necesita el número completo; si alguna vez lo necesita, ese acceso se
  audita y se justifica.
- Al pegar una salida en un reporte: enmascarar **y declarar que se enmascaró**.

## 5. Seudonimización vs anonimización

- **Seudonimizado**: se reemplaza el identificador, pero con la tabla de equivalencias se
  revierte. **Sigue siendo dato personal** y sigue bajo todas las reglas.
- **Anonimizado**: no se puede revertir ni por cruce. Solo entonces sale del alcance.
- Cuidado con la reidentificación por cruce: "el único participante de ese grupo que pagó tarde
  en marzo" identifica a una persona sin nombrarla. En agregados, **umbral mínimo de k** antes de
  publicar.

## 6. Retención y borrado

Tensión real: el derecho a que te borren **contra** la obligación contable y de prevención de
lavado de conservar registros.

| Dato | Retención |
|---|---|
| Asientos, movimientos, pagos, entregas | Lo que exija la normativa contable y de cumplimiento: **no se borran** |
| Documento y datos de KYC | Lo que exija la normativa; después se elimina o se anonimiza |
| Bitácora de auditoría | Larga, append-only |
| Datos de perfil y contacto | Se borran al borrar la cuenta |
| Adjuntos y comprobantes | Con el registro que respaldan |
| Logs | Corta y definida (30–90 días) |

Ante un pedido de borrado: se borra lo borrable, **se anonimiza lo que debe conservarse por
obligación legal** (el asiento queda, el nombre se desvincula) y se responde qué se hizo con
cada categoría. Se coordina con `regulatory-compliance-mapping`.

## 7. Datos de prueba

- **Prohibido copiar producción a desarrollo o prueba** (regla 90.2.5), incluido "un dump chico
  para reproducir el bug".
- **Prohibido restaurar un backup de producción en staging sin anonimizar.**
- Datos sintéticos con generador versionado (`synthetic-test-data-generation`), realistas en
  forma pero falsos en contenido.
- En una demo se usan datos sintéticos. Mostrar datos reales en una demo es una filtración con
  público.

## 8. Acceso, exportes y auditoría

- **Toda lectura de datos financieros de una persona deja rastro** (regla 90.2.7): quién, qué,
  cuándo, desde dónde y con qué justificación. Bitácora de lectura separada de la de escritura
  (`audit-trail-history`).
- Los **exportes** son la superficie de fuga más grande: autorización explícita, límite de
  volumen, marca de agua o registro de quién lo generó, y auditoría del export completo
  (`api-gateway-bff` limita su tasa).
- Acceso de soporte y de administración: con motivo, acotado en el tiempo y revisado. Un panel
  interno que muestra todo a cualquier empleado es una brecha esperando.

## Anti-patrones

- `console.log(usuario)` o `logger.info(req.body)`.
- Documento o cuenta en la query string "porque es un GET".
- Sentry con el payload completo del error.
- Métrica etiquetada por usuario.
- Dump de producción en la laptop de alguien.
- Captura de pantalla con datos reales en el reporte o en el chat.
- Evento de integración con el perfil completo del usuario.
- "Se borra cuando lo pidan" sin procedimiento.
- Export sin límite ni auditoría.
- Enmascarar y no decir que se enmascaró.

## Checklist por PR

- [ ] Los datos que toca el cambio están clasificados.
- [ ] El endpoint devuelve el mínimo; nada de `SELECT *` mapeado entero.
- [ ] Ningún dato personal o financiero en logs, URLs, errores, métricas, spans o analytics.
- [ ] Nada sensible en el evento; solo identificadores.
- [ ] Cifrado y enmascarado donde corresponde; tokens solo hasheados.
- [ ] Retención definida para lo nuevo que se guarda.
- [ ] Datos de prueba sintéticos.
- [ ] Lecturas sensibles auditadas; exportes limitados y auditados.
- [ ] Salidas pegadas en el reporte, enmascaradas y declaradas.

## Evidencia / Definition of Done

1. `grep` sobre el diff buscando campos sensibles en logs y URLs, con su salida.
2. Respuesta real del endpoint, pegada, mostrando que devuelve el mínimo.
3. Línea de log de la operación, pegada, sin datos sensibles.
4. Consulta a la bitácora de lectura demostrando que el acceso quedó registrado.
5. Declaración de retención para cualquier dato nuevo persistido.
