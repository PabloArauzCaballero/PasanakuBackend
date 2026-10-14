---
name: gobernanza-grupo
description: "Manejar el ciclo de vida y las decisiones de un grupo de AportaYa: creación y reglamento, cupos y turnos, períodos, acuerdos con votación, permutas, traspasos, retiros y disolución. Úsala al tocar el estado del grupo, el orden de turnos, quién ocupa un cupo, o cualquier decisión que el grupo tome en conjunto."
---

# Gobernanza del grupo

Un grupo es un contrato entre personas que no se conocen, ejecutado por software.
Las reglas se fijan **antes de que empiece** y cambiarlas después exige acuerdo,
porque cada cambio beneficia a alguien y perjudica a otro.

## Ciclo de vida

```
BORRADOR → CONFORMADO → EN_CURSO → FINALIZADO
                ↓            ↓
            CANCELADO     DISUELTO
```

Cada transición queda en `historial_estado_grupo` con actor y motivo. La transición
válida se verifica contra el estado actual, en la base; no con banderas sueltas.

| Estado | Qué se puede |
| --- | --- |
| `BORRADOR` | Configurar todo. Nadie se comprometió aún |
| `CONFORMADO` | Cupos ocupados, reglamento aceptado. **Acá se sortea, y no antes** |
| `EN_CURSO` | Aportes, entregas, acuerdos. La configuración base ya no se toca |
| `FINALIZADO` | Solo lectura, liquidación cerrada |
| `DISUELTO` | Liquidación anticipada, con causal (skill `garantia-mora-cobranza`) |

## Lo que se fija antes de empezar

`configuracion_grupo` + `reglamento_grupo` + `aceptacion_reglamento`: monto,
periodicidad, cantidad de cupos, modalidad de turnos, reglas de mora y de
cobertura, y el tarifario congelado (skill `facturacion-sin`).

**Cada participante acepta el reglamento con su versión**, y esa versión se guarda.
Un grupo `EN_CURSO` con un participante que nunca aceptó es un grupo cuyo
reglamento no es oponible a esa persona.

## Cupo, participante, turno

Tres cosas distintas que el código confunde apenas se descuida:

| Concepto | Persiste cuando… |
| --- | --- |
| **Cupo** | La persona cambia. El cupo conserva turno y obligaciones |
| **Participante** | Es la persona en ese cupo, con su historia en el grupo |
| **Turno** | Es del cupo, no de la persona |

Por eso `traspaso_cupo` funciona: entra otra persona al mismo cupo, con el mismo
turno y las mismas obligaciones pendientes. Si el turno colgara de la persona, cada
traspaso rompería el orden del grupo.

## Turnos

| Modalidad | Cómo se asigna |
| --- | --- |
| `SORTEO_ALEATORIO` | Compromiso y revelación, verificable (skill `sorteo-transparencia`) |
| Orden acordado | Se registra el criterio en `criterio_asignacion`, no "porque sí" |
| Permuta | `solicitud_permuta` entre dos participantes, con aceptación de ambos |

Reglas: **un solo sorteo por grupo** (`R-GRP-05`); permutar exige que **ninguno de
los dos turnos ya haya ocurrido**; y quien ya recibió no vuelve a recibir en el
mismo ciclo.

## Acuerdos: decisiones del grupo

```
acuerdo → voto_participante → resuelto_en
```

| Regla | Por qué |
| --- | --- |
| Un solo acuerdo abierto por grupo (`uq_acuerdo_abierto`) | Dos votaciones simultáneas producen resultados contradictorios |
| **El voto es inmutable** (`tg_voto_inmutable`) | Poder cambiar el voto después de ver el resultado no es votar |
| El quórum y la mayoría se fijan en el reglamento, antes | Definirlos al necesitarlos es definirlos a medida |
| Solo vota quien tiene cupo activo y está al día, según el reglamento | Escrito antes, no discutido después |
| El acuerdo resuelto se ejecuta o se archiva, con constancia | Un acuerdo aprobado que nadie ejecuta destruye la confianza más que uno rechazado |

Qué se decide por acuerdo: cambiar una fecha, admitir un reemplazo, aceptar una
quita, disolver anticipadamente. Qué **no**: nada que viole el reglamento aceptado
ni una norma —el grupo no puede votar saltarse un límite regulatorio.

## Salidas

| Camino | Requisito |
| --- | --- |
| **Traspaso** (CU-64) | Aceptación del entrante y del grupo; deudas resueltas o asumidas explícitamente |
| **Retiro** (CU-65) | Antes de recibir el turno; liquidación de lo aportado menos lo que corresponda |
| **Reemplazo por mora** (CU-66) | Incumplimiento declarado, con descargo previo |
| **Disolución** (CU-67) | Acuerdo o causal objetiva; liquidación individual para cada participante |

En todos, la regla es la misma: **primero se cuadra el dinero, después se cambia el
estado**. Un participante removido con saldo a favor sin liquidar es un reclamo
seguro.

## Períodos y calendario

`periodo` se abre y se cierra; `dia_no_habil` corrige los vencimientos. El
vencimiento se calcula **al abrir el período y se guarda**: recalcularlo después
con el calendario de hoy cambia la mora de operaciones pasadas.

## Emparejamiento

`postulacion_emparejamiento`, `criterio_emparejamiento`, `propuesta_grupo`: armar
grupos entre desconocidos. El criterio es dato con vigencia, y la propuesta se
acepta explícitamente —nadie queda dentro de un grupo por omisión.

## Checklist

- [ ] Las transiciones de estado se validan contra el estado actual, en la base.
- [ ] Nadie está `EN_CURSO` sin haber aceptado el reglamento, con su versión.
- [ ] Turno y obligaciones cuelgan del cupo, no de la persona.
- [ ] Un solo sorteo por grupo, con prueba del rechazo del segundo.
- [ ] Un solo acuerdo abierto, y el voto no se puede modificar —probado.
- [ ] Quórum y mayoría salen del reglamento, no del código.
- [ ] Toda salida liquida antes de cambiar el estado.
- [ ] El vencimiento del período se guarda al abrirlo.

## Ver también

`sorteo-transparencia` · `garantia-mora-cobranza` · `contabilidad-partida-doble` ·
`glosario-dominio` · CU-20, CU-60 a CU-67 · familia `R-GRP`
