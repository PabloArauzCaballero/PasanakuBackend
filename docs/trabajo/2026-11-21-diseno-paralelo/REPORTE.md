# Reporte — Carril «diseño en paralelo» (solo documentación)

> **AVANCE: 17 / 17 — 100,0 %** de microtareas HECHO. Los cinco hitos quedan **A MEDIAS**: falta la revisión y
> aprobación de Pablo en cada PR, y los documentos tienen `DECISION_REQUIRED` abiertas (lista al final).

- Fecha: 2026-11-21 · Plan: [PLAN.md](./PLAN.md) · Base: `origin/dev` 850e0f51
- Ramas y PR (todos hacia `dev`; no se mergea nada):

| Punto | Rama | PR |
|---|---|---|
| 1 · PX0.M10 + plan y reporte | `correcciones/diseno-paralelo-2026-11-21` | https://github.com/PabloArauzCaballero/PasanakuBackend/pull/410 |
| 2 · RFP PX-1 a PX-8 | `correcciones/diseno-paralelo-2026-11-21-rfp` | https://github.com/PabloArauzCaballero/PasanakuBackend/pull/406 |
| 3 · F5 «Poder de compra» | `correcciones/diseno-paralelo-2026-11-21-poder-de-compra` | https://github.com/PabloArauzCaballero/PasanakuBackend/pull/407 |
| 4 · F3.S8.M1 textos | `correcciones/diseno-paralelo-2026-11-21-textos` | https://github.com/PabloArauzCaballero/PasanakuBackend/pull/408 |
| 5 · PX9.M02 central de riesgo | `correcciones/diseno-paralelo-2026-11-21-central-riesgo` | https://github.com/PabloArauzCaballero/PasanakuBackend/pull/409 |

- **Peldaño de evidencia (regla 30): `TESTED` para el cruce de requisitos** (el verificador `cruce_rfp.py` corre,
  pasa con 0 faltantes y falla cuando se le quita un requisito). Para el contenido, el peldaño es `WRITTEN` más
  esa verificación mecánica: la revisión de Pablo es la que lo valida. No hay comportamiento en runtime que
  observar, porque son documentos.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `docs/proveedores/00-plantilla-rfp.md`, con RM-01 a RM-20 en lenguaje para el proveedor y SEG, DAT y CON | `cruce_rfp.py plantilla` | PASS, 20/20 · `evidencia/H1-cruce-plantilla-matriz.txt` |
| H1.S1.M2 | `docs/proveedores/01-matriz-de-evaluacion.md`: 7 criterios, escala 0–4 anclada, 11 eliminatorios y pesos `DECISION_REQUIRED` | `cruce_rfp.py matriz` | PASS, 8/8 · ídem |
| H2.S1.M1–M8 | Ocho RFP en `docs/proveedores/rfp/` (rama `-rfp`) | `RAIZ_REPO=… cruce_rfp.py rfp` | PASS, 0 faltantes; prueba negativa: exit 1 · `evidencia/H2-cruce-rfp.txt` |
| H2.S1.M9 | Sin nombres de empresas | `grep` de sufijos societarios y marcas frecuentes | PASS (un falso positivo: «Reunión») · ídem |
| H3.S1.M1 | ADR-057, en estado propuesta (rama `-poder-de-compra`) | `grep -c "98.7"` | PASS · `evidencia/H3-poder-de-compra.txt` |
| H3.S1.M2 | Contrato tipo con 6 cláusulas `[L-11]` y su índice | `grep -n "\[L-11\]"` | PASS · ídem |
| H3.S1.M3 | Diseño de pantallas, que cubre RF-PX10-01 a 06 | `grep -oE "RF-PX10-0[1-6]" \| sort -u` | PASS, 6/6 · ídem |
| H4.S1.M1 | Inventario de 389 textos, verificado contra `dev` | muestra de 15 filas con `sed -n` | PASS · `evidencia/H4-textos/verificacion.txt` |
| H4.S1.M2 | Tabla con propuesta para las 389 filas (rama `-textos`) | `python3 gen.py` + control de frases prohibidas | PASS: 389 filas, 0 sensibles sin propuesta, 0 frases prohibidas · ídem |
| H5.S1.M1 | Diseño de PX-9 condicionado a L-04 (rama `-central-riesgo`) | `cruce_rfp.py px9` | PASS, 5/5 · `evidencia/H5-cruce-px9.txt` |

## A medias

### H1 a H5 — los cinco hitos
- **Qué anda:** los documentos existen, y el verificador mecánico confirma que cada requisito del plan de
  proveedores está en su documento.
- **Qué no anda:** nada en rojo. Lo que falta es aprobación, no ejecución.
- **Qué falta exactamente:**
  1. Pablo revisa y mergea los cinco PR.
  2. Pablo completa los campos `[…]` y las `DECISION_REQUIRED` antes de enviar cada RFP.
  3. El abogado revisa el contrato tipo (L-11) y el diseño de PX-9 (L-04).
  4. Pablo aprueba cada texto de F3.S8.M1 (P-16).
- **Dónde quedó:** en las cinco ramas de la tabla de arriba. Solo hay archivos bajo `docs/`; no se compila nada.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Aplicar los textos de F3.S8.M1 al código y correr el `grep` de frases prohibidas | TODO (otro carril) | Aprobación de Pablo |
| Tipo `REPORTE_CENTRAL_RIESGO` en `consentimiento` y tabla de bitácora de PX-9 | TODO (F3, fuente `.puml`) | L-04 |
| Modelo de datos del comercio y del canje (PX10.M04) | TODO (F5) | L-11 y un comercio firmado |

## Evidencia

Todo está en `evidencia/`:
- `cruce_rfp.py`: el verificador. Se puede volver a correr; `RAIZ_REPO` apunta a otro checkout.
- `H1-cruce-plantilla-matriz.txt`
- `H2-cruce-rfp.txt`: tabla completa y pruebas negativas.
- `H3-poder-de-compra.txt`
- `H4-textos/`: inventario TSV, generador, propuestas y verificación.
- `H5-cruce-px9.txt`

```text
$ RAIZ_REPO=/tmp/w-rfp python3 docs/trabajo/2026-11-21-diseno-paralelo/evidencia/cruce_rfp.py rfp
37/37 · 36/36 · 33/33 · 36/36 · 31/31 · 29/29 · 34/34 · 30/30 presentes
TOTAL FALTANTES: 0   (exit 0)
# negativo: RF-PX1-04 → RF-PX1-XX  ⇒  FALTAN: RF-PX1-04 · exit 1
# negativo: "SMS, push y WhatsApp" en PX-7  ⇒  FALTAN: WhatsApp solo como exclusión · exit 1
$ python3 gen.py
filas 389 {'REESCRIBIR': 105, 'ELIMINAR': 279, 'SIN CAMBIO': 1, 'AJUSTE PARCIAL': 4}
defaults sensibles: []   ids en P/E que no existen: []
```

## No cubierto
- **La fidelidad semántica** de cada requisito transcripto a las RFP la revisé yo. No hubo una revisión
  independiente: el cruce prueba la presencia del código, no que el texto diga lo mismo que el plan.
- **El inventario de textos lo hizo un subagente.** Lo verifiqué con una muestra de 15 filas, no fila por fila.
  Los textos que el cliente arma en tiempo de ejecución pueden no estar.
- Las rutas y líneas de los textos son de `dev` 850e0f51. Se moverán con los merges de F2 y F3.
- **El plan fuente no está en `dev`** (rama `claude/nice-fermat-essf9e`). El verificador lo lee de un worktree de
  esa rama.

## Desvíos del plan
- **Un PR por punto** exigió cinco ramas. La asignada (`correcciones/diseno-paralelo-2026-11-21`) lleva el
  punto 1, el plan y el reporte. Las otras cuatro son ramas hijas con ese mismo prefijo, todas desde
  `origin/dev`.
- **Las RFP no repiten** las secciones comunes: se envían con el «Anexo A», que es la plantilla del PR del
  punto 1. Por eso el PR #406 depende del PR del punto 1.
- **El plan del carril** se escribió primero con IDs de dos capas. `plan_status.py` los rechazó, así que se
  pasaron a tres capas (H<n>.S1.M<n>) sin cambiar el contenido.

## Riesgos residuales
- **Si se envía una RFP antes de completar sus campos y sus `DECISION_REQUIRED`**, sale incompleta. Cada RFP lo
  advierte en su encabezado y en su §10.
- **La RFP de PX-3 no debe salir antes de L-01**: lo dice su encabezado.

## Decisiones y ambigüedades
No se tomó ninguna decisión de negocio. Lo que sigue queda registrado.

**DECISION_REQUIRED (Pablo):**
1. **Pesos de la matriz de evaluación** (C1 a C7) y el orden de desempate propuesto.
2. **Campos de cada RFP antes de enviarla:**
   - razón social, marca y canal de recepción;
   - fechas y volúmenes estimados;
   - plazos de RM-15 (consulta después de la baja), RM-18 (deprecación), SEG-05 (notificación de incidentes y
     brechas) y DAT-06 (cambio de suboperador).
3. **Destino del rendimiento de las cuentas custodias** (RF-PX3-07).
4. **Medio de verificación de titularidad** (PX6.M03), junto con PX-2 o PX-3.
5. **F5:**
   - plazo de canje y qué pasa cuando vence (también L-11);
   - turno temprano pagando por adelantado (F5.M4) y cómo convive con el orden por nivel y el sorteo;
   - diferencia a favor si el precio de lista baja;
   - si la comisión al comercio se descuenta del pago o se factura aparte, y su porcentaje;
   - si el canje espera al enfriamiento de la cuenta del participante;
   - si se revisa el ADR-057 cuando el piloto supere [N] comercios.
6. **PX-9:**
   - si el consentimiento es opcional o requisito para entrar a un grupo (también L-04);
   - si se informa el monto de las cuotas pagadas a tiempo.
7. **Textos:** si sigue existiendo un límite de grupos activos por persona (A-3).

**Para el abogado o Cumplimiento:**
- **L-01** antes de enviar la RFP de PX-3.
- **L-06** para los tipos de documento de PX-4.
- **Listas de PX-5.**
- **Registro de remitentes de SMS** (PX-7).
- **Datos fiscales y contingencia** de PX-8.
- **L-11** para las 6 cláusulas marcadas del contrato tipo.
- **L-04**, con una sub-pregunta nueva: qué pasa con lo ya reportado cuando la persona revoca su consentimiento.

**Ambigüedades registradas:**
- **`evaluacion_tercero`.** El plan la usa como «evaluación ponderada», pero en el modelo mide el desempeño
  periódico de un contrato ya firmado. Las planillas de selección quedan como archivo referenciado desde
  `contrato_tercero.evaluacion_riesgo_url`. Confirmar con Pablo.
- **`entorno_prueba_regulado`.** El plan lo usa como el sandbox técnico del proveedor, pero en el modelo es el
  sandbox regulatorio de ASFI (con licencia, tope de usuarios y garantía). Hay que confirmar dónde se registran
  los casos aprobados de cada proveedor. Esto alcanza a RM-11 y a PX0.M04, que son de otro carril.
- **«Opción de efectivo» en F5.** Se lee como recibir el pozo en la cuenta verificada, por ADR-039.
- **Textos A-1 a A-6:**
  - A-1: si el botón de compartir puede nombrar WhatsApp;
  - A-2: el enlace del grupo de 7 días frente al token de propósito único;
  - A-3: el límite de grupos por persona;
  - A-4: la marca «Aporteo»;
  - A-5: montos en la bandeja dentro de la app;
  - A-6: nombres de pasarelas en el backoffice.

**Hallazgos fuera de alcance (anotados, no corregidos):**
- En el código de `dev`, el explicativo de proveedores del backoffice nombra pasarelas concretas
  (`apps/backoffice/src/app/nucleo/explicaciones/explicaciones-sistemas.ts:48`).
- Las plantillas del canal WhatsApp incluyen montos atribuibles
  (`seeders/minimos/15-eventos-y-plantillas.json`).

**Procesos:** ningún proceso quedó corriendo. Los worktrees temporales están en `/tmp/w-*` y no se versionan.
