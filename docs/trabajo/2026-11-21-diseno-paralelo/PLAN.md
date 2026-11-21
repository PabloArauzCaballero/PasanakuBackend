# Plan — Carril «diseño en paralelo» (solo documentación)

- Fecha: 2026-11-21 · Repo: `PabloArauzCaballero/PasanakuBackend` · Base: `origin/dev` 850e0f51
- Predecesor: plan de correcciones del 2026-11-21 (rama `claude/nice-fermat-essf9e`, todavía no en `dev`):
  `docs/trabajo/2026-11-21-plan-correcciones/{PLAN.md, PLAN-PROVEEDORES.md, PREGUNTAS-LEGALES.md, EJECUCION.md}`
  y `docs/trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md`.
- Resultado observable: Pablo abre cinco PR hacia `dev` y encuentra, en `docs/`, (1) la plantilla de RFP y la
  matriz de evaluación, (2) ocho RFP listas para enviar al mercado, (3) el ADR-057, el contrato tipo con
  comercios y el diseño de pantallas de «Poder de compra», (4) la tabla de textos a reescribir con su
  propuesta y (5) el diseño del consentimiento y del reporte a la central de riesgo. Ningún archivo fuera de
  `docs/` cambia.
- Kill-test: (a) un requisito `RF-PXn-xx` o `RM-xx` de `PLAN-PROVEEDORES.md` que no aparece en la RFP que le
  corresponde; (b) un nombre de empresa proveedora o una norma que no esté citada en el repo; (c) un archivo
  modificado fuera de `docs/`; (d) una decisión de negocio tomada que no esté en `DECISIONES.md`.

## Alcance
- **IN:** documentos nuevos en `docs/proveedores/`, `docs/Arquitectura/ADR-057 …`, `docs/poder-de-compra/`,
  `docs/textos/` y esta carpeta de trabajo.
- **OUT:** código, `sql/`, `docs/entidades/*.puml`, contratos OpenAPI, seeders, y aplicar cualquier texto
  (P-16: Pablo los aprueba primero). Tampoco se modifican ADR existentes ni `PLAN-PROVEEDORES.md`.
- **Fase 6 (prueba visual):** no aplica, no hay cambio de UI; las pantallas de F5 son diseño funcional escrito.
- **Fase 7 (regresión):** se reduce al cruce de requisitos y a `git diff --stat` acotado a `docs/`.
- **Ambigüedades registradas:** ver `REPORTE.md` §Decisiones y ambigüedades (se registran, no se resuelven).

## Estrategia de ramas (un PR por punto)
La rama asignada es `correcciones/diseno-paralelo-2026-11-21`. Como el pedido exige un PR por punto, cada
punto vive en una rama hija con ese prefijo, todas desde `origin/dev`:

| Punto | Rama | Contenido |
|---|---|---|
| 1 | `correcciones/diseno-paralelo-2026-11-21` | PX0.M10 + este `PLAN.md` y el `REPORTE.md` |
| 2 | `correcciones/diseno-paralelo-2026-11-21-rfp` | 8 RFP + cruce |
| 3 | `correcciones/diseno-paralelo-2026-11-21-poder-de-compra` | ADR-057, contrato tipo, pantallas |
| 4 | `correcciones/diseno-paralelo-2026-11-21-textos` | F3.S8.M1 borrador |
| 5 | `correcciones/diseno-paralelo-2026-11-21-central-riesgo` | PX9.M02 |

## H1 — Plantilla de RFP y matriz de evaluación (PX0.M10)
**CA:** Dado un proveedor a contratar, cuando Pablo arma su RFP, entonces parte de una plantilla única con los
siete criterios comunes, los RM aplicables y una matriz con escala anclada y pesos marcados `DECISION_REQUIRED`.
**DoD:** `python3 evidencia/cruce_rfp.py plantilla` → los 7 criterios y RM-01..RM-20 presentes. **Estado:** A MEDIAS — microtareas HECHO; falta la revisión y aprobación de Pablo en el PR

### H1.S1 — Documentos de PX0.M10
**Estado:** HECHO (todas sus microtareas)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `docs/proveedores/00-plantilla-rfp.md` | Contiene las secciones fijas y los RM-01..RM-20 | `cruce_rfp.py plantilla` → 0 faltantes | HECHO |
| H1.S1.M2 | `docs/proveedores/01-matriz-de-evaluacion.md` con los 7 criterios, escala 0–4 anclada, eliminatorios y pesos `DECISION_REQUIRED` | Los 7 criterios de PLAN-PROVEEDORES «Selección» aparecen con su pregunta | `cruce_rfp.py matriz` → 0 faltantes | HECHO |

## H2 — Ocho RFP listas para enviar
**CA:** Dado el mercado abierto, cuando Pablo envía la RFP de PX-n, entonces el proveedor recibe todos los RF
de su sección, los RM aplicables, los casos obligatorios de prueba y lo exigido en seguridad, datos y
continuidad, sin nombres de empresas ni normas ajenas al repo; PX-1 y PX-2 exigen segundo proveedor.
**DoD:** `python3 evidencia/cruce_rfp.py rfp` → tabla de cruce sin faltantes, pegada en el PR. **Estado:** A MEDIAS — microtareas HECHO; falta la revisión y aprobación de Pablo en el PR

### H2.S1 — Una RFP por proveedor
**Estado:** HECHO (todas sus microtareas)

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | RFP PX-1 pasarela QR | RF-PX1-01..10 + RM + casos de 3 niveles + segundo proveedor | `cruce_rfp.py rfp PX-1` | HECHO |
| H2.S1.M2 | RFP PX-2 desembolso | RF-PX2-01..09 + RM + casos + segundo proveedor | `cruce_rfp.py rfp PX-2` | HECHO |
| H2.S1.M3 | RFP PX-3 banco custodio | RF-PX3-01..07 + RM | `cruce_rfp.py rfp PX-3` | HECHO |
| H2.S1.M4 | RFP PX-4 KYC | RF-PX4-01..10 + RM + casos | `cruce_rfp.py rfp PX-4` | HECHO |
| H2.S1.M5 | RFP PX-5 listas y PEP | RF-PX5-01..05 + RM + casos | `cruce_rfp.py rfp PX-5` | HECHO |
| H2.S1.M6 | RFP PX-6 titularidad | RF-PX6-01..03 + RM + casos | `cruce_rfp.py rfp PX-6` | HECHO |
| H2.S1.M7 | RFP PX-7 mensajería (SMS principal y respaldo, push, correo; sin WhatsApp) | RF-PX7-01..07 + RM + casos; ninguna mención a WhatsApp salvo la exclusión | `cruce_rfp.py rfp PX-7` | HECHO |
| H2.S1.M8 | RFP PX-8 facturación autorizada | RF-PX8-01..04 + RM + casos | `cruce_rfp.py rfp PX-8` | HECHO |
| H2.S1.M9 | Control de contenido prohibido | Sin «WhatsApp» como canal ofrecido, sin nombres de empresas | `grep` documentado en evidencia | HECHO |

## H3 — «Poder de compra» (F5), solo diseño
**CA:** Dado el piloto aceptado como dirección (tandas 5 y 6), cuando Pablo y el abogado revisan, entonces
encuentran el ADR-057 con la ubicación del agregado del comercio justificada por la regla 98.7, el contrato
tipo con las cláusulas dependientes de L-11 marcadas, y el diseño de pantallas (canje por QR, precio
congelado, opción de recibir el dinero siempre disponible).
**DoD:** revisión de Pablo; `grep -c "L-11"` sobre el contrato > 0 y lista de cláusulas marcadas. **Estado:** A MEDIAS — microtareas HECHO; falta la revisión y aprobación de Pablo en el PR

### H3.S1 — Documentos de diseño de F5
**Estado:** HECHO (todas sus microtareas)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | ADR-057 (estado `propuesta`) | Opciones evaluadas contra los cuatro criterios de 98.7 y decisión recomendada | Revisión + `grep "98.7"` | HECHO |
| H3.S1.M2 | Contrato tipo con comercios | Cada cláusula que depende de L-11 lleva la marca `[L-11]` | `grep -n "\[L-11\]"` | HECHO |
| H3.S1.M3 | Diseño funcional de pantallas (participante y panel del comercio) | Cubre canje por QR, precio congelado, opción de dinero siempre visible, cuatro estados | Revisión + lista de estados por pantalla | HECHO |

## H4 — Textos nuevos (F3.S8.M1, borrador)
**CA:** Dado cada texto visible que promete respaldo, revisión humana, WhatsApp u organizador, cuando Pablo
revisa la tabla, entonces ve su ruta:línea en `dev`, el texto actual y una propuesta alineada a A + D.
**DoD:** inventario verificado con `sed -n` sobre una muestra; tabla en `docs/textos/`. **Estado:** A MEDIAS — microtareas HECHO; falta la revisión y aprobación de Pablo en el PR

### H4.S1 — Inventario y tabla
**Estado:** HECHO (todas sus microtareas)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Inventario de textos (agente de solo lectura) | Cada fila verificable con `sed -n '<l>p' <ruta>` | Muestra de 15 filas verificada, salida en evidencia | HECHO |
| H4.S1.M2 | Tabla con texto propuesto | Toda fila del inventario tiene propuesta o «eliminar» con motivo | Conteo filas inventario = filas tabla | HECHO |

## H5 — Central de riesgo (PX9.M02), solo diseño
**CA:** Dado L-04 abierta, cuando Pablo lee el diseño, entonces encuentra el consentimiento (expreso, separado,
versionado, revocable), el contenido del reporte (solo hechos, nunca antes de `FIRME`), la rectificación y la
bitácora, todo condicionado a L-04.
**DoD:** `cruce_rfp.py px9` → RF-PX9-01..04 presentes. **Estado:** A MEDIAS — microtareas HECHO; falta la revisión y aprobación de Pablo en el PR

### H5.S1 — Documento de diseño PX-9
**Estado:** HECHO (todas sus microtareas)

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H5.S1.M1 | Diseño del consentimiento y del reporte | RF-PX9-01..04 cubiertos y marcados «condicionado a L-04» | `cruce_rfp.py px9` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| El plan fuente está en otra rama | Los enlaces relativos no resuelven en `dev` hasta que se mergee | Las RFP transcriben los requisitos (son autocontenidas) y citan la rama |
| ADR-055, 056 y 058 los escriben otras sesiones | El ADR-057 los cita sin poder enlazarlos | Se citan por número y título del plan |
| Pesos, plazos de la RFP y datos de la empresa | No se pueden decidir acá | `DECISION_REQUIRED` en el reporte |
