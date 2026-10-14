---
name: debido-proceso
description: "El patrón que se repite cada vez que AportaYa toma una decisión que perjudica a alguien: causal escrita, notificación probada, plazo calculado y guardado, descargo con evidencia, decisión motivada, apelación única resuelta por otro, prescripción y reversión con compensación. Úsala al implementar incumplimientos, sanciones, expulsiones, restricciones, bajas de organizador o cualquier flujo que termine con alguien perdiendo algo."
---

# Debido proceso

Seis flujos distintos de AportaYa terminan con alguien perdiendo algo: incumplimiento,
sanción al participante, expulsión, restricción, sanción al organizador y disolución.
Los seis siguen **la misma secuencia**, y cuando uno se aparta de ella es donde el
sistema se vuelve injusto.

```
causal escrita → notificación PROBADA → plazo guardado → descargo con evidencia
   → decisión MOTIVADA → apelación única, resuelta por otro → efecto
                              ↘ prescripción si nadie resuelve
                              ↘ reversión con compensación si prospera
```

## Los siete pasos, y qué los rompe

### 1. Causal escrita y anterior al hecho

La regla que se invoca tiene que existir **antes** del hecho: [[politica_sancion]],
[[reglamento_grupo]], [[contrato_organizador]] o [[matriz_sancion]], todos con
vigencia. Sancionar por una regla publicada después es sancionar retroactivamente.

### 2. La notificación se prueba, no se supone

> **No se declara incumplido a quien nunca fue notificado**, y eso se prueba con los
> acuses de [[evento_entrega_mensaje]], no con "el sistema mandó el mensaje".

Si el afectado no tiene canal verificado activo, **el plazo no corre** hasta
acreditar el aviso por otra vía. Un plazo que corre sin notificación no es un plazo.

### 3. El plazo se calcula al notificar y se guarda

En **días hábiles** (skill `plazos-habiles`), en su columna, y nunca se recalcula.
`R-GAR-01` lo exige para incumplimientos; el resto de los flujos usa la misma
disciplina.

### 4. Descargo con evidencia de ambas partes

La evidencia automática —lo que el sistema ya sabe— y la que aporta el afectado van
a la **misma tabla**, con `aportada_por` distinguiéndolas y `es_inmutable = true`
(`R-GAR-02`). Nadie edita evidencia después.

### 5. Decisión motivada

No hay decisión por silencio administrativo **en contra** del afectado. Se escribe el
fundamento y cada transición deja fila en el historial de estados
([[historial_estado_incumplimiento]] y equivalentes). **Esa tabla es el expediente**:
sin ella no hay debido proceso que demostrar, aunque el procedimiento se haya seguido.

### 6. Apelación única, resuelta por otro

Una por sanción (`R-ORG-05`), y **quien la resuelve no es quien la aplicó**
(`R-SEG-04`, trigger en la base). Dos consecuencias que suelen olvidarse:

- **Si el órgano no resuelve en plazo, se resuelve a favor del apelante.** La demora
  del comité no la paga la persona.
- La apelación **no frena** el efecto operativo urgente (un reemplazo, una
  suspensión cautelar por fraude), pero si prospera se revierte con compensación.

### 7. Prescripción y reversión

- Cumplido `prescribe_en_dias` sin resolución, se cierra como `PRESCRITO` y **no se
  puede reabrir por el mismo hecho**.
- Si la decisión se revoca: se restituye, **se compensa la reputación con un evento
  compensatorio** (no se borra el original, `R-REP-01`) y, si hubo perjuicio
  económico medible, se resarce y entra como [[evento_riesgo_operativo]].

## Dónde vive cada uno

| Flujo | Caso de uso | Plazo en |
| --- | --- | --- |
| Incumplimiento | [[CU-25 Declarar el incumplimiento con descargo y evidencia]] | `registro_incumplimiento.fecha_limite_subsanacion` |
| Expulsión / reemplazo | [[CU-66 Reemplazar a un participante moroso]] | vía acuerdo y descargo |
| Restricción | [[CU-27 Restringir al deudor e incluirlo en la lista interna]] | `restriccion_usuario.vigente_hasta` |
| Sanción al organizador | [[CU-93 Sancionar al organizador y resolver su apelación]] | `sancion_organizador.vigente_desde` + política |
| Reclamo del cliente | [[CU-52 Atender un reclamo en plazo]] | `reclamo_cliente.fecha_limite_respuesta` |
| Acuerdo del grupo | [[CU-63 Proponer y votar un acuerdo]] | `acuerdo.fecha_limite` |

## Cautelar: la única excepción

Cuando el riesgo es el propio afectado —fraude con evidencia, apropiación de
efectivo— la medida es **cautelar e inmediata** y el descargo viene después. Se marca
como cautelar, y si el descargo prospera se revierte con compensación. Es una
excepción acotada, no un atajo disponible para cualquier urgencia.

## Qué no hacer

- No hacer firme una decisión antes de vencer el plazo de descargo.
- No dejar que quien decide resuelva su propia apelación.
- No borrar ni editar evidencia.
- No sancionar sin política vigente ("después la escribimos").
- No usar el silencio del afectado como consentimiento, salvo el vencimiento
  explícito del plazo, con notificación probada.
- No aplicar una medida sobre alguien que perjudique a terceros inocentes: el grupo
  nunca paga la sanción de su organizador.

## Ver también

- `R-GAR-01` · `R-GAR-02` · `R-ORG-05` · `R-SEG-04` · `R-CON-01` en [[Restricciones]]
- Skills: `garantia-mora-cobranza`, `organizador-habilitacion`, `plazos-habiles`,
  `reclamos-consumidor`, `gobernanza-grupo`, `gobierno-comites`
