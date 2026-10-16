# ADR propuesto — Núcleo de decisión automática y gobierno de políticas

- Estado: **Propuesto** (carril A · H1.S2). No crea tablas ni código: es la decisión de modelo que
  la skill `boveda-modelo` exige antes de tocar `docs/entidades/*.puml` y `sql/` (ambigüedad A-2).
- Fecha: 2026-10-16 · Decide: dueño de la bóveda + Riesgos + Cumplimiento (y Legal para §7).
- Fuente: plan estratégico `docs/trabajo/2026-10-15-backoffice-financiero/PLAN.md` §2 (A2, A3).

## 1. Contexto: lo que ya existe (inventario verificado)

El repositorio **ya decide automáticamente** en varios dominios; lo que falta no es un motor, sino un
registro uniforme de *por qué* se decidió y un gobierno uniforme de *quién cambia la política*.

| Módulo | Tabla | Qué es hoy |
| --- | --- | --- |
| 01 identidad | `politica_token` | vigencia por propósito (`vigente_desde`), sin perfil de usuario (§9.4 del flujo) |
| 02 grupos | `decision_ingreso` | registro **append-only** de decisiones de ingreso |
| 03 aportes | `politica_mora` | parámetros de mora |
| 04 entregas | `regla_entrega` | reglas de entrega |
| 05 notificaciones | `version_plantilla` | versiones de plantilla |
| 06 transparencia | `regla_impacto_evento` | impacto en reputación |
| 07 organizador | `decision_habilitacion` | registro **append-only** de decisiones de habilitación |
| 07 organizador | `regla_automatizacion` | reglas de automatización (CU-95) |
| 08 garantía | `politica_cobertura` | parámetros de cobertura |
| 08 garantía | `politica_sancion` | parámetros de sanción |
| 09 auditoría | `politica_retencion` | retención de evidencia |
| 09 auditoría | `regla_cumplimiento` | `expresion`, `umbral`, `severidad`, `accion_automatica`, `activa` |
| 10 billetera | `politica_billetera` | límites/condiciones de billetera |
| 10 billetera | `regla_antifraude` | patrones antifraude |
| 11 tarifas | `politica_redondeo` | redondeo |
| 11 tarifas | `regla_tarifa` | reglas de tarifa |
| 12 cumplimiento | `politica_interna` | políticas internas aprobadas |
| 12 cumplimiento | `regla_monitoreo_lft` | `expresion`, `fuente_normativa`, `activa`, `vigente_desde`, **`aprobada_por`** |
| 15 inversiones | `version_condiciones` | versiones **append-only** de condiciones |

Disciplina ya escrita en `.claude/skills/motor-de-reglas/SKILL.md`: umbral al catálogo (nunca un
número en la expresión), expresión compilada, **simulación obligatoria antes de activar**, acción
proporcional a la severidad, versionado sin borrado, alerta cerrada con conclusión.
Implementación existente: `CU48CalibrarReglas` (activación con aprobador) y, en `nucleo-financiero`,
`ReglasDeRetiro` / `ReglasDeRecarga` / `ReglasDeEjecucion` (decisión automática del retiro con
umbral de doble aprobación configurable, ADR-049).

**Huecos** (lo que este ADR resuelve):

1. No hay un registro de **intento de decisión** común: cada dominio guarda (o no) qué política,
   qué versión y qué entradas produjeron el resultado. `decision_ingreso` y `decision_habilitacion`
   lo hacen para su dominio; retiros, recargas y antifraude no dejan la versión de política aplicada.
2. Solo `regla_monitoreo_lft` tiene aprobador —y `CU48CalibrarReglas.activar` lo recibe **en la
   entrada** (`EntradaActivacion.aprobadaPor`), no de la sesión: el mismo defecto que se corrigió en
   la decisión de casos LFT (H5.S2)—, y no hay **etapas** (autor → validación →
   simulación → Riesgos → Cumplimiento → aprobación → publicación) ni segregación entre ellas.
3. No hay **publicación separada de la aprobación**, ni rollback que deje constancia.
4. No hay cola de **revisión por excepción** genérica con plazo y responsable.

## 2. Decisión propuesta

### 2.1 Un contrato común, datos en el servicio dueño (regla 98)

No se crea un "servicio motor" central ni una tabla compartida: eso es una base compartida entre
servicios (prohibido por la regla 98.1). Se define:

- **En `plataforma/comun-dominio`** (micro-PR): el átomo puro `Evaluacion` —entradas referenciadas,
  versión y hash de la política, reglas disparadas, resultado y motivos— y el puerto
  `ExtractorDeEvidencia` (§2.6). Sin Spring ni jOOQ.
- **En cada servicio que decide** (nucleo-financiero, cumplimiento, organizador, grupos…): su propia
  tabla append-only `decision_automatica` con el MISMO esquema lógico, en su esquema.

`decision_automatica` (append-only, `UPDATE`/`DELETE` revocados al rol de la app):

| Campo | Regla |
| --- | --- |
| `id`, `clave_idempotencia` **UNIQUE** | reintento = mismo resultado (invariante 7) |
| `tipo_solicitud`, `sujeto_tipo`, `sujeto_id` | qué se decidió (sin datos personales) |
| `politica_codigo`, `politica_version`, `politica_hash` | qué versión EXACTA resolvió |
| `entradas` JSONB | referencias y hashes de las entradas, no copias de datos personales |
| `reglas_disparadas` JSONB, `motivo_codigo`, `motivo_legible` | explicable al operador y al cliente |
| `resultado` | `APROBADA_AUTOMATICA` · `RECHAZADA_AUTOMATICA` · `REVISION_REQUERIDA` · `ERROR_TECNICO` |
| `actor` | identidad técnica del motor (rol de servicio con permisos mínimos), nunca un usuario ficticio |
| `evaluada_en`, `traza_id`, `operacion_resultante_id` | causalidad con el movimiento |

`ERROR_TECNICO` es un resultado aparte: **una imposibilidad técnica nunca se muestra como rechazo**
al cliente (plan §2.1). La operación de dinero y su decisión se escriben en la misma transacción con
su evento de outbox (regla 98.3).

### 2.2 Revisión humana por excepción

`revision_de_decision` (por servicio): `decision_id`, `motivo` (dato insuficiente, conflicto,
umbral, fraude, alerta normativa, error del motor, regla sin vigencia), `plazo` **guardado al
crear**, `asignada_a`, `estado` (`PENDIENTE` → `EN_REVISION` → `RESUELTA`), `resolucion`,
`resuelta_por` (≠ actor que la originó cuando hay SoD), `resuelta_en`. Transición con `UPDATE …
WHERE estado = ?` verificado (patrón ya aplicado en H2/H5).

**Por flujo, falla cerrada** (parámetros cerrados hasta aprobación):

| Flujo | Si el motor no puede decidir |
| --- | --- |
| Retiro / desembolso | se **retiene** (no se paga) y va a revisión |
| Recarga | se acredita solo con confirmación del proveedor (regla 91.3); si no, pendiente |
| Reclamo con auto-resolución | va a revisión humana; nunca se cierra solo |
| Antifraude / LFT | alerta + revisión; la acción automática se limita a la tabla de severidad |

### 2.3 Gobierno de políticas: versiones, etapas y publicación separada

`version_politica` (por servicio dueño de la política), **inmutable una vez enviada a revisión**:
`politica_codigo`, `version`, `contenido` (expresión compilable + parámetros que apuntan al
catálogo), `hash`, `estado`, `autor`, `vigente_desde`, `plan_de_reversion`.

Estados: `BORRADOR` → `VALIDADA` (compila y pasa pruebas de regresión) → `SIMULADA` (impacto sobre
casos históricos/sintéticos: volumen, aprobadas, rechazadas, excepciones por segmento) →
`PENDIENTE_APROBACION` → `APROBADA` → `PUBLICADA` → (`SUSPENDIDA` | `REVOCADA`).

`aprobacion_de_politica`: una fila por etapa requerida según el riesgo de la política (`orden`,
`rol_requerido`, `actor`, `decision`, `comentario` obligatorio en rechazo, `evidencia_id`,
`factor_verificado_en` —MFA reciente—, `decidida_en`). Restricciones en la **base**:
`actor <> autor`, un actor no firma dos etapas incompatibles, y una sola fila por (versión, orden)
**UNIQUE** —dos aprobadores simultáneos producen una sola transición (el segundo recibe conflicto).

`publicacion_de_politica` (append-only): quién publicó (≠ autor; ≠ aprobador cuando el riesgo lo
exija), cuándo, qué versión, porcentaje de despliegue gradual. **Rollback = publicar de nuevo una
versión anterior APROBADA** (fila nueva); nunca un `UPDATE` sobre la versión ni sobre su historial.
Cada `decision_automatica` apunta a la versión que la resolvió, así que una decisión histórica sigue
apuntando a la versión anterior después del rollback.

### 2.4 Idempotencia y concurrencia

- Aprobar/rechazar/publicar llevan `Idempotency-Key`; el reintento devuelve el mismo recibo
  (registro de respuesta o idempotencia por estado, ver `convenciones-contrato.md` §3).
- Toda transición: `UPDATE … SET estado = :nuevo WHERE id = :id AND estado = :esperado AND version = :v`
  y se verifica el conteo. Los defectos encontrados en H5 (`decidir` ignoraba ese conteo) son
  exactamente lo que esta regla evita.

### 2.5 "Firma" de aprobación

Se registra **aprobación autenticada**: intención explícita, actor, permiso, alcance exacto
(versión y hash), estado previo, resultado, fecha, MFA reciente, motivo y evidencia. **No** se
llama "firma digital": una firma con efecto jurídico (certificado, sellado de tiempo, conservación)
es una decisión aparte de Legal (§7).

### 2.6 Evidencia documental preparada para OCR

Puerto `ExtractorDeEvidencia` con salida `EvidenciaDocumental(origen, hash, campos, confianza,
version_extractor, finalidad, consentimiento, revisada_por)`. Hoy el adaptador por omisión es la
**captura humana**; un OCR futuro cambia el adaptador, no la API de decisiones ni el expediente.
Un dato leído por OCR **nunca** queda como verificado sin `revisada_por` cuando la norma o el
riesgo lo exigen.

### 2.7 Acceso por permiso y propósito

La identidad técnica del motor es un rol de servicio con permisos mínimos; la lectura de datos
financieros de una persona durante la revisión deja rastro en `registro_acceso_datos` con
finalidad (R-SEG-02, regla 90.2.7).

## 3. Cobertura de las decisiones del plan (§2.1–§2.7)

| Plan | Dónde lo resuelve este ADR |
| --- | --- |
| §2.1 Decisión automática por defecto | §2.1 (`decision_automatica`, resultados, `ERROR_TECNICO`) |
| §2.2 Revisión humana por excepción | §2.2 (`revision_de_decision`, falla cerrada por flujo) |
| §2.3 Gobierno humano de políticas | §2.3 (etapas, SoD en base, publicación separada, rollback) |
| §2.4 OCR posterior | §2.6 (puerto y adaptador humano por omisión) |
| §2.5 Firma de aprobación bien definida | §2.5 |
| §2.6 Acceso por permiso y propósito | §2.7 y `matriz-autorizacion.md` |
| §2.7 Terminología y estado visibles | §2.1–§2.3 (estados enumerados con su transición) |

## 4. Consecuencias

- Antes de escribir código: `.puml` de las cuatro entidades por servicio, restricciones `R-…`,
  CU de gobierno (crear/validar/simular/aprobar/publicar/revertir) y semillas de permisos
  (`POLITICA_REDACTAR`, `POLITICA_APROBAR_RIESGOS`, `POLITICA_APROBAR_CUMPLIMIENTO`,
  `POLITICA_PUBLICAR` — **propuestos**, no sembrados; los decide Seguridad, S-8).
- Primer corte vertical sugerido: retiros (`nucleo-financiero`), porque ya tiene umbral
  configurable y doble aprobación (ADR-049): registrar `decision_automatica` en cada solicitud con
  la versión de `politica_billetera` aplicada.
- Lo que **no** decide este ADR: límites y plazos normativos, qué productos admiten
  automatización, matriz de incompatibilidades, retención de evidencia y firma digital (Legal).
