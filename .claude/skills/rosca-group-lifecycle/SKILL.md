---
name: rosca-group-lifecycle
description: Gate de diseño del corazón del producto — el grupo de pasanaku y su ciclo de vida. Cupo separado de participante, reglamento versionado con aceptación probada, sorteo de turnos verificable con commit-reveal, calendario de periodos y turnos, acuerdos que autorizan lo que no puede ser unilateral (permuta, condonación, expulsión, disolución), traspaso de cupo conservando la posición económica, y estados del grupo con sus transiciones. Usar al modelar o tocar grupo, cupo, participante, turno, periodo, reglamento, acuerdo, permuta, ingreso, retiro o disolución, y al revisar cualquier endpoint que cambie quién cobra cuándo.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Ciclo de vida del grupo de pasanaku

El pasanaku es un contrato entre personas que no se conocen del todo, donde el orden de cobro
vale plata y la confianza es el producto. El software no "gestiona un grupo": **hace cumplir un
acuerdo**. Cada decisión de diseño acá tiene una contraparte de desconfianza en el mundo real.

Servicio dueño: `grupos` (módulo M2). Las prohibiciones de dinero, en la **regla 91**.

## 1. Cupo ≠ participante — la separación que sostiene todo

Una persona puede tener **dos manos o media mano**. Las obligaciones y los turnos cuelgan del
**cupo**, no de la persona.

```
Participante (persona en el grupo) ──1:N──▶ Cupo (posición económica)
                                              └── obligaciones, turno, deuda
```

Consecuencias que **no se pueden romper**:

- Un moroso se reemplaza **conservando el cupo**: el calendario del grupo no se mueve
  (`traspaso_cupo`). Si las obligaciones colgaran de la persona, reemplazarla reordenaría los
  turnos de todos, que es exactamente lo que rompe la confianza del grupo.
- Media mano es un cupo con monto distinto, no un caso especial en el código.
- Todo lo que dice "el participante debe" en realidad es "el cupo debe". Revisá el naming:
  `deuda_participante` existe porque la deuda sí persigue a la persona; la obligación, no.

## 2. Reglamento: versionado y aceptado, no un texto en una pantalla

- `reglamento_grupo` es **versionado**. Cambiarlo no es editar: es publicar una versión nueva.
- `aceptacion_reglamento` guarda **qué versión exacta** aceptó cada participante, cuándo, desde
  dónde y con qué token firmado (`consent-management`).
- Un cambio de reglamento con el grupo en marcha **requiere acuerdo** (§4), no decisión del
  organizador.
- Al mostrar el reglamento a alguien, se muestra **la versión que aceptó**, no la última.

## 3. Sorteo de turnos verificable (commit-reveal)

El orden de cobro es **el punto de desconfianza número uno** del pasanaku. Un sorteo que no se
puede recomputar no sirve, por más honesto que haya sido.

```
1. COMMIT   → se publica hash(semilla) ANTES de sortear, con timestamp y en la bitácora
2. SORTEO   → orden = f(semilla, lista ordenada de cupos)   ← función determinista y publicada
3. REVEAL   → se publica la semilla; cualquiera recomputa y verifica
```

Reglas:

1. La semilla se genera con un generador criptográficamente seguro y **no se conoce antes** del
   commit por nadie, incluido el organizador.
2. La función de ordenamiento es **determinista, documentada y estable**: misma semilla + misma
   lista ⇒ mismo orden, hoy y dentro de un año.
3. La lista de cupos que entra al sorteo se congela en el commit y se publica.
4. Commit, reveal y resultado van a la bitácora encadenada (`audit-trail-history`) y al panel de
   transparencia (`transparency-reputation`).
5. **Existe una herramienta de verificación** que cualquier participante puede correr. Si no
   existe, el esquema es teatro.
6. Rehacer un sorteo requiere acuerdo y deja los dos registros visibles. Nunca se pisa.

## 4. Acuerdos: lo que no puede ser unilateral

`acuerdo` + `voto_participante` autorizan las decisiones que afectan al patrimonio o la posición
de otros:

| Decisión | Por qué necesita acuerdo |
|---|---|
| Permuta de turnos | Cambia quién cobra cuándo |
| Condonación / quita | Alguien deja de recibir lo que le correspondía |
| Expulsión de un participante | Afecta su patrimonio y su reputación |
| Cambio de reglamento en marcha | Cambia las reglas del contrato aceptado |
| Disolución anticipada | Liquida el grupo |
| Admisión fuera del proceso normal | Cambia el riesgo de todos |

- El quórum y la mayoría **salen del reglamento aceptado**, no de una constante en el código.
- El acuerdo tiene estado, plazo de votación y resultado; vencido sin quórum, **no se aprueba
  por silencio**.
- El organizador **no puede** ejecutar ninguna de estas por su cuenta (regla 91.5).

## 5. Estados del grupo

```
BORRADOR → ABIERTO → CONFORMADO → EN_CURSO → CERRADO
                │          │          │
                │          │          └─▶ DISUELTO (acuerdo o causa reglamentaria)
                └──────────┴─▶ CANCELADO (no llegó a conformarse)
```

Cada transición (`state-machines-workflows`):

- Tiene **precondiciones verificadas en el servidor**: `CONFORMADO` exige cupos completos,
  reglamento aceptado por todos y KYC aprobado de cada participante
  (`kyc-identity-verification`).
- `EN_CURSO` exige sorteo revelado y calendario generado (`contribution-calendar`).
- `CERRADO` exige todas las entregas confirmadas y el mayor del grupo en cero
  (`distributed-data-integrity`).
- Queda en `historial_estado_grupo`, append-only, con actor y motivo.

**Prohibido un endpoint que setee el estado directamente.** Se invoca la transición, que valida.

## 6. Entradas y salidas en marcha

| Caso | Entidad | Regla dura |
|---|---|---|
| Ingreso | `solicitud_ingreso`, `invitacion` | Nadie entra sin reglamento aceptado y KYC aprobado |
| Retiro voluntario | `solicitud_retiro` | Liquidación de lo aportado según reglamento; no se "borra" |
| Traspaso de cupo | `traspaso_cupo` | El cupo conserva turno y obligaciones; el entrante acepta el reglamento vigente |
| Reemplazo por mora | `candidato_reemplazo` + expediente firme | Requiere el debido proceso (regla 91.4) |
| Expulsión | `acuerdo` | Nunca unilateral |

**Nadie se elimina.** Un participante que sale queda con su historia: los aportes que hizo, la
deuda que dejó y el rastro del acuerdo que lo autorizó.

## 7. El organizador no es caja

Regla 91.5, y acá se instrumenta: el organizador tiene funciones administrativas
(`contrato_organizador`, `evaluacion_desempeno`) y **cero** capacidad de mover fondos, cobrar
comisión o ser cuenta de paso. Cualquier endpoint que le dé discrecionalidad sobre dinero viola
RN-18 y no se mergea.

## Anti-patrones

- Obligaciones colgadas del participante en vez del cupo.
- Sorteo sin commit previo, o con semilla que el organizador conocía.
- Función de ordenamiento no determinista (depende de un `ORDER BY` sin desempate, de la hora o
  del orden de inserción).
- Reglamento editado en el lugar, sin versión ni nueva aceptación.
- Permuta, condonación o expulsión resueltas por el organizador.
- Acuerdo que se aprueba por silencio al vencer.
- `UPDATE grupo SET estado = 'EN_CURSO'` desde un endpoint genérico.
- Borrar un participante que se fue.
- Quórum hardcodeado.
- Recalcular turnos al reemplazar a alguien.

## Checklist

- [ ] Las obligaciones y turnos cuelgan del cupo.
- [ ] Reglamento versionado; la aceptación apunta a la versión exacta.
- [ ] Sorteo con commit-reveal, función determinista publicada y herramienta de verificación.
- [ ] Commit, semilla, lista congelada y resultado en bitácora y en transparencia.
- [ ] Toda decisión de la tabla de §4 pasa por `acuerdo` con quórum del reglamento.
- [ ] Transiciones de estado con precondiciones en el servidor e historial append-only.
- [ ] Ingreso condicionado a reglamento aceptado y KYC aprobado.
- [ ] Reemplazo por mora solo con expediente firme.
- [ ] El organizador no tiene ningún camino para mover fondos ni cobrar.

## Evidencia / DoD

1. Salida de la verificación del sorteo: hash publicado, semilla revelada y orden recomputado
   por la herramienta, coincidiendo.
2. Salida de un intento de transición sin precondiciones: rechazado, con el motivo.
3. Salida de un intento del organizador de ejecutar una decisión que exige acuerdo: 403.
4. Registro de aceptación de reglamento con la versión, para un participante.
5. Prueba de traspaso de cupo: el turno y las obligaciones no se movieron.
