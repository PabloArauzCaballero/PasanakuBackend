---
name: consent-management
description: Gate de diseño para el consentimiento como dato de primera clase en software financiero — alcance, propósito, otorgante, vigencia, revocación con efecto inmediato, versionado del texto aceptado, prueba auditable, acceso de emergencia (break-glass) con justificación y revisión posterior, y representantes/menores como caso modelado. Usar al construir o revisar cualquier flujo que comparta, busque, muestre o exporte datos de un participante a otro actor, al tocar directorios o búsquedas que expongan personas, y antes de dar por terminado un endpoint de lectura financiera.
effort: high
---

# Gestión de consentimiento

Skill de **ingeniería**. Qué consentimientos exige la norma, qué forma deben tener, qué edad o
figura de representación aplica y cuándo un acceso no requiere consentimiento son decisiones del
responsable legal/financiero — ver `regulatory-compliance-mapping`. Acá está cómo modelarlo y
hacerlo cumplir para que ninguna de esas decisiones dependa de un `if` en el frontend.

## 1. Principios

1. **El consentimiento es un registro, no un booleano.** `acceptedTerms: true` no responde quién,
   a qué, para qué, cuándo, con qué texto, ni si sigue vigente.
2. **Se verifica en el servidor, en cada acceso.** La UI solo refleja la decisión.
3. **Denegar por defecto.** Sin consentimiento vigente que cubra el acceso → no hay dato. Ausencia
   de registro nunca es permiso.
4. **Revocar es tan fácil como otorgar**, y surte efecto inmediato.
5. **Consentimiento ≠ autorización.** Primero rol y relación (`authz-access-control`), después
   consentimiento. Ninguno reemplaza al otro.
6. **No se infiere ni se arrastra**: consentir la atención no es consentir investigación,
   marketing ni aparecer en un directorio.

## 2. Modelo mínimo

| Atributo | Pregunta que responde |
|---|---|
| `subject` | ¿De quién son los datos? |
| `grantor` | ¿Quién otorga? (el titular o su representante, con el vínculo registrado) |
| `grantee` | ¿A quién se le concede? (profesional, organización, rol, banco) |
| `purpose` | ¿Para qué? (atención, gestión de seguro, investigación, publicación…) — concepto codificado, ver `terminology-value-sets` |
| `scope` | ¿Sobre qué datos/acciones? (categorías de registro, lectura vs exportación) |
| `decision` | permitir / denegar |
| `validFrom` / `validUntil` | ¿Desde y hasta cuándo? |
| `status` | borrador, activo, revocado/inactivo, ingresado por error |
| `policyVersion` | ¿Qué versión exacta del texto aceptó? |
| `evidence` | ¿Cómo lo prueba? (evento de aceptación, documento firmado adjunto, canal) |
| `recordedBy` / `recordedAt` | ¿Quién lo registró y cuándo? |
| `revokedAt` / `revokedBy` / `revocationReason` | Cierre del ciclo |

- El recurso `Consent` de EMVCo ISO 20022 modela el mismo problema (sujeto, otorgante, receptor, período,
  decisión base permitir/denegar, provisiones como excepciones, documento fuente, texto de
  política). Servite de él como referencia de diseño y para intercambio — ver
  `payments-qr-integration` —, sin obligarte a copiar su forma internamente.
- Propósito y alcance son **value sets cerrados**, no texto libre: si no se puede evaluar por
  código, no se puede hacer cumplir.

## 3. Versionado del texto aceptado

1. El texto legal es un artefacto versionado e **inmutable**: versión, idioma, hash del contenido,
   vigencia. Corregir una coma es versión nueva.
2. El registro de consentimiento referencia la versión exacta. Nunca «el texto actual».
3. Cambio material del texto → el responsable legal define si exige re-consentimiento. El sistema
   debe poder: listar quién aceptó qué versión, pedir re-aceptación, y operar mientras tanto con
   el alcance anterior.
4. Guardá lo que la persona **vio** (versión + idioma + canal), no solo que hizo clic.

## 4. Verificación en el camino de lectura

```ts
// ❌ el guard mira el rol y el front decide si muestra el botón
@Roles('organizador')
@Get(':memberId/records')
find(@Param('memberId') id: string) { return this.records.findByMember(id); }

// ✅ rol + relación + consentimiento vigente, evaluados en servidor y registrados
async findForActor(actor: Actor, memberId: string) {
  await this.access.assertCareRelationship(actor, memberId);
  const grant = await this.consents.requireActive({
    subject: memberId, grantee: actor, purpose: Purpose.Care, scope: Scope.LedgerRead,
  });
  await this.audit.recordAccess({ actor, memberId, basis: grant.id });
  return this.records.findByMember(memberId, grant.scope);
}
```

- La evaluación es **una función central** (servicio/policy), no `if` repetidos por controlador.
- El acceso registrado guarda **en base a qué** se permitió (id del consentimiento o del evento
  break-glass) — ver `audit-trail-history`.
- Respuesta ante falta de consentimiento: no reveles si el participante existe. Alineá 403/404 con
  `error-handling-contract`.
- Multi-tenant: el consentimiento vive dentro del tenant; cruzar organizaciones es un otorgamiento
  explícito — ver `multi-tenancy`.

## 5. Revocación con efecto inmediato

1. Revocar cambia el estado y sella `revokedAt`; el registro original **no se borra ni se edita**.
2. «Inmediato» incluye: cachés de permisos, sesiones abiertas, tokens con claims embebidos,
   índices de búsqueda, suscripciones en tiempo real, exportes programados y jobs en cola. Si
   cacheás la decisión, invalidala en la revocación (`caching-strategy`).
3. Lo ya compartido antes de revocar no se «des-comparte»; lo que cambia es todo acceso futuro.
   Qué pasa con copias en terceros: validar con el responsable legal.
4. Vencimiento (`validUntil`) equivale a revocación: se evalúa en cada lectura, no con un job
   nocturno como única defensa.
5. Test obligatorio: otorgar → leer OK → revocar → leer falla, en la misma sesión.

## 6. Acceso de emergencia (break-glass)

Patrón reconocido en fintech (ISO 20022 lo documenta como *Break the Glass* en sus security labels) para
cuando negar el acceso daña al participante. Es una **excepción controlada**, no un bypass.

| Requisito | Detalle |
|---|---|
| Quién | Solo roles operativos habilitados; jamás soporte o administración por defecto |
| Justificación | Motivo obligatorio (codificado + texto) **antes** de ver el dato |
| Fricción explícita | Pantalla de advertencia que informa que el acceso queda registrado y será revisado |
| Alcance y tiempo | Un participante, ventana corta, no renovable en silencio |
| Registro reforzado | Evento propio, distinguible de un acceso normal |
| Aviso | Notificación a responsable de privacidad y, según política, al participante |
| Revisión posterior | Cola de revisión con resultado (justificado / indebido) y dueño; sin revisión no hay break-glass |

Criterios de cuándo procede: validar con el responsable de cumplimiento y legal.

## 7. Exposición en búsquedas y directorios

- Aparecer en un directorio público o en una búsqueda entre usuarios es un **acto de publicación**
  que requiere decisión explícita del titular, por campo publicable — ver
  `group-discovery-matching`.
- La búsqueda devuelve lo mínimo para elegir (ver `data-privacy-financial` §2); el resto, después de la
  relación/consentimiento.
- Una búsqueda nunca debe confirmar que alguien **es participante**. Cuidado con autocompletados,
  mensajes «ya existe un usuario con ese documento» y conteos — ver `search-and-filtering`.

## 8. Representantes, menores e incapacidad

Modelalo como caso de primera clase, no como parche:

- Vínculo `grantor → subject` con tipo (codificado), vigencia y evidencia del vínculo.
- El titular puede pasar a decidir por sí mismo: el sistema debe soportar la **transición**
  (caducidad del vínculo, revisión de consentimientos heredados) sin migración manual.
- Varios representantes, desacuerdo entre ellos, y datos que el menor puede reservarse: son
  decisiones normativas. **Validar con el responsable legal/financiero**; el modelo debe permitir
  configurarlas, no asumirlas.

## Anti-patrones

- Checkbox pre-marcado, o consentimientos agrupados en un único «acepto todo».
- Consentimiento guardado como columna booleana en el usuario.
- Editar el registro al revocar (se pierde la prueba de que existió).
- Verificar consentimiento al listar pero no en el endpoint de detalle, export o adjuntos.
- Permisos cacheados en el JWT que sobreviven a la revocación hasta que expire el token.
- Break-glass sin cola de revisión: es una puerta trasera con nombre elegante.

## Checklist

- [ ] Registro con sujeto, otorgante, receptor, propósito, alcance, decisión, vigencia, versión de texto y evidencia.
- [ ] Propósito y alcance como conceptos codificados.
- [ ] Texto legal versionado e inmutable; el registro referencia la versión exacta.
- [ ] Verificación central en servidor en **todos** los caminos: lista, detalle, adjuntos, export, tiempo real, búsqueda.
- [ ] Denegación por defecto; la respuesta no filtra existencia.
- [ ] Revocación invalida cachés, sesiones, índices y jobs.
- [ ] Break-glass con justificación previa, alcance acotado, registro propio y revisión posterior.
- [ ] Representación modelada con vínculo, vigencia y transición.
- [ ] Reglas normativas confirmadas por el responsable legal/financiero y registradas como decisión (`technical-docs-and-adr`).

## Evidencia / Definition of Done

Pegá salida literal (ver `evidence-and-verification`):

1. **Secuencia otorgar → leer → revocar → leer**: respuestas HTTP reales de los cuatro pasos,
   con el último denegado.
2. **Prueba negativa por cada camino** (detalle, adjunto, export, búsqueda) para un actor sin
   consentimiento — ver `api-testing`.
3. **Registro de acceso** mostrando la base del permiso (id de consentimiento).
4. **Evento break-glass** generado en un caso de prueba, con su entrada en la cola de revisión.
5. **Consulta** que demuestre que el registro revocado sigue existiendo con su versión de texto.
6. Caminos no ejercitados, declarados como «no cubierto».
