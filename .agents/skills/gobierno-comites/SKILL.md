---
name: gobierno-comites
description: "Las decisiones que no puede tomar una sola persona en AportaYa: comités con composición y quórum, actas con voto nominal y abstención registrada, efectos que se aplican en la misma transacción que cierra el acta, evaluación de riesgo de producto con no objeción, y designación del oficial de cumplimiento. Úsala al aprobar una política, lanzar un producto, resolver una apelación, o cuando alguien pregunte quién autoriza esto."
---

# Gobierno: comités, actas y aprobaciones

Hay decisiones que no tienen dueño único: aprobar una política, aceptar un riesgo,
lanzar un producto, revocar una sanción. Esta skill es cómo se toman y cómo se
prueban después.

```
asunto con expediente → convocatoria → quórum + composición → voto nominal
   → acta_comite (append-only) → efectos en la MISMA transacción → planes con dueño
```

## Quórum no es contar cabezas

`comite_gobierno` tiene `quorum_minimo` **y** `composicion_requerida`. Un comité de
cumplimiento con tres personas de negocio no tiene quórum aunque sean tres:
`COMPOSICION_INCOMPLETA`. Los asuntos que exigen el rol ausente se posponen; los
demás pueden tratarse.

## El acta es la evidencia

Se registra lo tratado, la decisión, **los fundamentos** y **quién votó qué**,
incluidas las abstenciones con su motivo.

> Un acta sin disidencias registradas es un acta incompleta.

Quien tenga interés directo **se abstiene y la abstención queda escrita**: es la
única forma de demostrar después que no participó (`R-SEG-04`). Y si *todos* tienen
interés, el asunto se eleva: un comité que no puede abstenerse no puede decidir.

El acta es *append-only* (`R-AUD-01`). Una decisión que después resulta equivocada
**no se reescribe**: se decide distinto en una sesión nueva y ambas quedan.

## Los efectos van en la misma transacción

Cerrar el acta y aplicar lo aprobado son un solo acto atómico: la política pasa a
vigente, la evaluación de producto queda `VIGENTE`, la sanción queda revocada, y se
crean los [[plan_accion_riesgo]] de cada compromiso con responsable y fecha.

**Ningún efecto de gobierno se aplica sin el acta que lo respalda** (`R-LIC-03`).

## No sesionar es un hallazgo

`periodicidad_minima` se controla. Un comité que no sesiona genera
[[hallazgo_auditoria]] automático y alerta al directorio. También se registra el
**intento fallido por falta de quórum**: que un comité no logre reunirse es
información de gobierno, no un no-evento.

Y el primer punto del orden del día es siempre el estado de los compromisos de la
sesión anterior.

## Evaluación de riesgo de producto

Antes de lanzar cualquier producto, canal o cambio material
([[CU-47 Evaluar el riesgo del producto antes de lanzarlo]]):

| Exigencia | Detalle |
| --- | --- |
| Los **cuatro factores** | cliente, producto, canal, geografía. Falta uno → rechazo |
| Cada riesgo con su control | y el control **apunta a un CU o a una restricción**; si no, es una intención |
| Riesgo alto sin control | **es un no**, no un "se monitorea" |
| `requiere_no_objecion` | si es true, el producto **no se habilita** hasta tener respuesta (`R-LIC-04`, en la base) |
| Cambio material | **versión nueva**, no edición |

Y el cierre del círculo: los controles definidos se instrumentan como
[[regla_cumplimiento]] concretas (skill `motor-de-reglas`). **La evaluación no
termina en un documento; termina en reglas que corren.**

## Oficial de cumplimiento

- **Un solo titular activo** (`R-UIF-12`), con suplente obligatorio.
- No puede tener funciones operativas incompatibles (`R-SEG-04`): quien vigila no
  ejecuta.
- La designación se comunica al regulador **en plazo guardado**; una designación no
  comunicada es tan observable como no tener oficial.
- La baja y la activación del reemplazo van **en la misma transacción**: la función
  no queda vacante ni un día.

La capacitación anual se mide como **lista nominal de quién falta**, no como
porcentaje. Y el personal dado de alta en noviembre tiene su plazo desde el alta, no
desde enero.

## Qué no hacer

- No aplicar un efecto aprobado "en una transacción aparte, después".
- No cerrar un acta sin voto nominal.
- No dejar que el comité decida algo fuera de su facultad: se eleva.
- No habilitar un producto que exige no objeción sin tenerla — y no confiar en que
  el procedimiento lo evite: lo impide la base.
- No medir capacitación en porcentajes.
- No dejar al titular sin suplente.

## Ver también

- [[CU-94 Elevar una decisión al comité de gobierno]] ·
  [[CU-47 Evaluar el riesgo del producto antes de lanzarlo]] ·
  [[CU-49 Designar al oficial de cumplimiento y capacitar]] ·
  [[CU-46 Verificar el alcance de la licencia]]
- `R-LIC-03` · `R-LIC-04` · `R-UIF-12` · `R-SEG-04` en [[Restricciones]]
- Skills: `cumplimiento-uif`, `norma-nueva`, `roles-y-accesos`, `motor-de-reglas`,
  `organizador-habilitacion`, `observabilidad`
