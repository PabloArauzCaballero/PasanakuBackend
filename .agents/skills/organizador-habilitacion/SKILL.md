---
name: organizador-habilitacion
description: "El ciclo de vida del organizador de AportaYa: requisitos de habilitación medibles, capacitación con vigencia, contrato firmado con hash, evaluación de desempeño por período, sanción con descargo y apelación, y rescisión sin dejar grupos huérfanos. Úsala al tocar cualquier cosa del módulo 07, al decidir quién puede crear grupos, o cuando haya que apartar a un organizador."
---

# El organizador: habilitar, medir, sancionar

Administrar el dinero de otros es un privilegio con requisitos, contrato y
consecuencias. Y el principio que ordena todo lo demás:

> **El grupo no paga la sanción del organizador.** Ninguna medida sobre un
> organizador puede dejar a un grupo sin quien lo administre.

```
solicitud → requisitos → capacitación → contrato firmado → puede crear grupos
                                              ↓
                            evaluación por período → sanción → apelación
                                                        ↓
                                              rescisión con transición
```

## Habilitar

`requisito_habilitacion` es un **catálogo con vigencia**, no una lista en el código:

| `tipo` | Ejemplo | `es_obligatorio` |
| --- | --- | --- |
| `ANTIGUEDAD` | 6 meses en la plataforma | sí |
| `EXPERIENCIA` | 2 grupos completados como participante | sí |
| `REPUTACION` | puntaje mínimo | sí |
| `KYC` | debida diligencia reforzada vigente | sí |
| `SIN_INCUMPLIMIENTOS` | ninguno en la ventana | sí |
| `CAPACITACION` | módulo obligatorio aprobado | sí |

Los no obligatorios determinan el `nivel_requerido` alcanzado, que limita cuántos
grupos y de qué monto.

**Se evalúa con los requisitos vigentes al momento de solicitar**, no al resolver.
No se mueve el arco con la pelota en el aire.

**Al postulante se le muestran los requisitos con su estado antes de postular.**
Nadie debería enterarse de que no califica después de escribir una carta.

## Contrato

- Un solo contrato vigente por organizador (`R-ORG-02`, `EXCLUDE` por rango).
- **Sin contrato firmado y vigente no se crea un grupo**: lo impide un trigger sobre
  `grupo`, no un `if`.
- Se guarda `contenido_hash` **junto con la firma** (`R-ORG-03`). Firmar "el
  contrato" sin fijar cuál no es firmar nada.
- Un contrato firmado no se edita: se emite versión nueva y hasta que la firme
  **rige la anterior**. Los grupos ya creados conservan sus condiciones, igual que el
  tarifario congelado.

## Medir

`evaluacion_desempeno` es única por organizador y período (`R-ORG-04`), y su
`puntaje_global` es **exactamente** la suma ponderada de sus
[[metrica_organizador]]. Cada componente se guarda: el organizador tiene que poder
discutir una métrica concreta, no un número global.

| Trampa | Cómo se evita |
| --- | --- |
| Castigar por muestra chica | se marca `representatividad baja` y no penaliza |
| Métrica incalculable = peor caso | `cumple = null` y peso cero para ese período |
| Metas fijadas después de ver el resultado | `SIN_METAS_VIGENTES` bloquea el cierre |
| Cartera heredada de una rescisión | se pondera aparte los primeros períodos |

**La evaluación sugiere, no ejecuta.** Subir de nivel o sancionar afectan el
sustento de una persona: los decide alguien con nombre.

## Sancionar

| Tipo | Qué bloquea | Qué **no** toca |
| --- | --- | --- |
| `AMONESTACION` | nada; pesa en la próxima evaluación | — |
| `LIMITACION_DE_NIVEL` | grupos nuevos por encima de cierto monto | los vigentes |
| `SUSPENSION` | crear grupos | **sigue administrando los suyos**, salvo riesgo |
| `RESCISION` | todo | encadena transición ordenada de sus grupos |

Procedimiento obligatorio: causal escrita → notificación → **plazo de descargo
calculado y guardado** → decisión fundada → apelación única (`R-ORG-05`), resuelta
por quien no la aplicó (`R-SEG-04`).

Dos detalles que suelen olvidarse:

- **Fraude**: suspensión cautelar inmediata, descargo después. El riesgo es el
  propio organizador y la transición ordenada no aplica.
- **El comité que no resuelve la apelación en plazo**: la sanción queda **revocada a
  favor del apelante**. La demora del órgano no la paga la persona.

## Rescindir

Antes de que sea efectiva: reasignar los grupos, liquidar las comisiones devengadas,
revocar los roles. `GRUPOS_SIN_REASIGNAR` bloquea el cierre. Si no hay otro
organizador, los administra la plataforma con un operador designado y el costo se
registra.

Renunciar durante un proceso sancionatorio **no lo detiene**: el antecedente queda
para una postulación futura.

## Qué no hacer

- No crear un camino de habilitación "por confianza" fuera del catálogo.
- No sancionar sin plazo de descargo guardado.
- No dejar que quien aplicó la sanción resuelva la apelación.
- No mostrar la sanción a los participantes del grupo: ven el cambio de
  administrador, no el expediente.
- No dar de baja a un organizador con grupos activos sin destino.

## Ver también

- [[CU-90 Postular a organizador y habilitarse]] · [[CU-91 Firmar y rescindir el contrato de organizador]] ·
  [[CU-92 Evaluar el desempeño del organizador]] · [[CU-93 Sancionar al organizador y resolver su apelación]]
- `R-ORG-01` a `R-ORG-05` en [[Restricciones]]
- Skills: `gobernanza-grupo`, `roles-y-accesos`, `gobierno-comites`,
  `indicadores-tablero`, `garantia-mora-cobranza`
