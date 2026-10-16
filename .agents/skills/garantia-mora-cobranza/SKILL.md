---
name: garantia-mora-cobranza
description: "Manejar el incumplimiento en AportaYa: mora, declaración de incumplimiento con descargo, fondo de garantía y cobertura, subrogación y deuda, cobranza, sanciones con apelación, reemplazo y disolución. Úsala cuando un participante no aporta, al tocar el fondo de garantía, al sancionar, o cuando el grupo quede con un cupo caído."
---

# Mora, garantía y cobranza

Es el módulo que decide si el pasanaku digital sirve o no: en el analógico, cuando
alguien no pone, el grupo se rompe. Acá el grupo **no se detiene** —el fondo cubre
y el sistema cobra después—, y ese mecanismo tiene que ser justo y demostrable en
ambas direcciones.

## La secuencia, sin saltos

```
obligacion_aporte vencida
  → mora (politica_mora: gracia, recargo, escalones)
    → registro_incumplimiento  (con descargo_participante antes de declarar)
      → cobertura_incumplimiento ← fondo_garantia (politica_cobertura)
        → subrogacion → deuda_participante
          → gestion_cobranza → accion_cobranza / promesa_pago / acuerdo_quita
            → abono_recuperacion → devolucion_fondo
              (o castigo_deuda, si se agota la gestión)
```

Cada flecha es un hecho registrado con fecha. Saltarse un paso —cubrir sin declarar
incumplimiento, cobrar sin deuda registrada— deja huecos que después nadie sabe
explicar.

## Mora no es incumplimiento

| Mora | Incumplimiento |
| --- | --- |
| Estado automático: venció y no pagó | **Declaración**, con evidencia y descargo |
| Genera recargo según `politica_mora` | Genera cobertura, deuda y sanción |
| Se sale pagando | Se sale pagando **y** con la gestión que corresponda |

Declarar incumplimiento sin darle al participante la posibilidad de descargo es lo
primero que va a reclamar, con razón. El descargo tiene plazo y queda escrito, se
acepte o no.

## El fondo de garantía

| Regla | Por qué |
| --- | --- |
| El fondo es **del grupo**, no del organizador ni de la plataforma | La titularidad importa: es dinero de terceros |
| Toda cobertura sale de `politica_cobertura`, con tope y condiciones | Cubrir "según criterio" es cubrir sin regla |
| La cobertura **subroga**: el fondo pasa a ser acreedor del participante | Sin subrogación, el dinero se regaló |
| El fondo no cubre si no alcanza; se activa `plan_contingencia` | Cubrir de más deja al grupo siguiente sin respaldo |
| Todo movimiento del fondo es un movimiento de dinero real | Con su transacción, su asiento y su contrapartida |

Recuperado el dinero, `devolucion_fondo` lo repone. Un fondo que cubre y nunca
recupera no es un fondo: es una pérdida diferida, y hay que verla como tal en la
base de eventos de riesgo.

## Cobranza

`estrategia_cobranza` define la secuencia de acciones por tramo de atraso: es
**dato**, no un `switch`. Reglas que no dependen del ánimo del día:

- Horarios y frecuencia de contacto acotados y registrados. La gestión abusiva es
  un riesgo legal, no una técnica.
- `promesa_pago` con fecha: si se cumple, cambia el tramo; si se rompe, escala.
- `acuerdo_quita` requiere autorización de nivel superior y queda con su motivo.
- `castigo_deuda` no borra la deuda: la reclasifica. El histórico permanece
  (`historial_incumplimiento_usuario`), y alimenta el riesgo del usuario.
- El aval (`aval_participante`) solo se ejecuta si existe y fue aceptado antes.

## Sanciones

```
politica_sancion + matriz_sancion → sancion → apelacion_sancion
```

| Regla | Por qué |
| --- | --- |
| La sanción sale de la matriz por tipo y reincidencia, no de una decisión suelta | Dos casos iguales con sanciones distintas es arbitrariedad demostrable |
| Toda sanción es **apelable**, con plazo | Y la apelación se resuelve por alguien distinto de quien sancionó |
| La sanción tiene vigencia y se levanta sola al vencer | Una sanción eterna que nadie recuerda levantar es un cliente perdido |
| Sancionar no reemplaza cobrar | Son cosas distintas: una castiga, la otra recupera |

## Cuando el cupo cae

| Camino | Cuándo | Qué preserva |
| --- | --- | --- |
| `reemplazo_participante` + `candidato_reemplazo` | Hay quien entre | El turno y las obligaciones del cupo, no de la persona |
| Traspaso de cupo (CU-64) | El participante consigue reemplazo | Igual, con acuerdo del grupo |
| Retiro (CU-65) | Antes de recibir su turno | Liquidación de lo aportado, menos lo que corresponda |
| `disolucion_anticipada` (CU-67) | Ya no es viable | `liquidacion_participante` para cada uno, con su causal |

En todos: **primero se cuadra el dinero, después se cambia el estado**. Y quien ya
recibió su turno no se retira sin resolver lo que debe: es la regla que hace que el
esquema no se pueda usar como préstamo gratis.

## Alerta temprana

`score_riesgo_incumplimiento` y `alerta_temprana` existen para actuar antes del
vencimiento —un aviso a tiempo evita una cobranza—, no para etiquetar personas. El
puntaje se descompone y se puede reclamar (skill `sorteo-transparencia`, sección de
reputación).

## Checklist

- [ ] La mora sale de `politica_mora`; ningún recargo escrito en el código.
- [ ] No se declara incumplimiento sin plazo de descargo, y hay prueba.
- [ ] Toda cobertura genera subrogación y deuda; probado que no se pierde el
      acreedor.
- [ ] El fondo no cubre por encima de su tope ni de su política.
- [ ] Los movimientos del fondo cuadran como cualquier otro dinero.
- [ ] La sanción sale de la matriz y es apelable ante alguien distinto.
- [ ] El reemplazo preserva turno y obligaciones del cupo.
- [ ] La disolución liquida a cada participante con su causal registrada.

## Ver también

`contabilidad-partida-doble` · `gobernanza-grupo` · `reclamos-consumidor` ·
`observabilidad` · CU-23, CU-65, CU-66, CU-67 · familia `R-GRP`
