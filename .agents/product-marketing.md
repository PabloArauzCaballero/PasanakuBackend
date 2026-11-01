# Product Marketing Context

**Document version:** v1
**Last updated:** 2026-10-21

> Borrador automático desde el repositorio (skill `product-marketing`, opción 1), para el
> rediseño bancario de la app (`docs/trabajo/2026-10-21-rediseno-bancario/`). Todo lo
> literal viene con su fuente. Lo marcado **(inferido)** o **(a confirmar)** no tiene
> respaldo en el repo y no se usa como afirmación en la app.

## Product Overview
**One-liner:** «El pasanaku, en tu bolsillo.» (`docs/Views/AportaYa-Identidad.md` §05, hero de `landing/index.html`)
**What it does:** «AportaYa es una billetera móvil que digitaliza el pasanaku boliviano: ahorro rotativo comunitario, gestionado con la calidez de la comunidad y el rigor de una fintech. La persona puede recargar y retirar dinero en cualquier momento.» (Identidad.md:17)
**Product category:** billetera móvil de pasanaku / ahorro rotativo comunitario (ROSCA: tanda, junta, cundina en otros países).
**Product type:** app móvil B2C (Flutter) + backoffice de operación y cumplimiento.
**Business model:** comisión de plataforma por juego, cobrada al beneficiario al recibir su entrega (`docs/entidades/11_tarifas_comisiones.md`). El valor del seeder (0,30 %, piso Bs 10, techo Bs 50) es **placeholder sin aprobación comercial** (`landing/README.md`). El organizador **no** cobra comisión (RN-18).

## Target Audience
**Target users:** personas en Bolivia que ya hacen pasanaku presencial (de cuaderno o por WhatsApp), con montos en bolivianos (Bs). Criterio de claridad: «si tu abuela no lo entiende, lo rehacemos» (Identidad.md:33).
**Roles:** participante (una persona en un cupo) y organizador (administra el grupo, sin custodia).
**Primary use case:** ahorrar en grupo y recibir el turno completo sin depender de que alguien «guarde la plata».
**Jobs to be done:**
- Aportar cada período sin perseguir a nadie y con prueba de que pagué.
- Recibir mi entrega completa en mi turno, aunque alguien falle.
- Confiar en que el orden de cobro no se arregló.
- Organizar un grupo sin volverme el cobrador ni el sospechoso.
**Use cases:** unirse a un grupo por enlace o QR; recargar y aportar; ver el estado del grupo y del turno; retirar la plata; verificar el sorteo; armar un grupo nuevo.

## Personas
| Persona | Cares about | Challenge | Value we promise |
|---------|-------------|-----------|------------------|
| Participante | Que su plata esté segura y le llegue entera | No sabe si el resto paga ni si el orden fue arreglado | Custodia separada, todo registrado, fondo de garantía |
| Organizador | Que el grupo funcione sin quedar como sospechoso | Perseguir pagos, llevar el cuaderno, sostener la confianza | La app cobra, registra y sortea; él no toca la plata |

## Problems & Pain Points
**Core problem:** «El pasanaku falla por desconfianza, no por falta de plata.» (`docs/entidades/README.md:58`)
**Why alternatives fall short:**
- «En el pasanaku de cuaderno, la prueba de pago es la palabra del organizador.» (`03_aportes_pagos_qr.md:335`)
- «Todo el mundo sospecha que el organizador acomodó el sorteo.» (`02_grupos_turnos.md:414`)
- «En el tradicional, cuando el grupo se disuelve, la deuda se evapora.» (`08_garantia_incumplimiento.md:532`)
**What it costs them:** turnos cobrados de menos, grupos que se desarman y plata que nadie puede reclamar.
**Emotional tension:** sospecha entre conocidos, vergüenza de cobrar a un vecino, miedo a que alguien «cobre y desaparezca».

## Competitive Landscape
**Direct:** no encontrado en el repo **(a confirmar)**.
**Secondary:** pasanaku por planillas y grupos de WhatsApp. Falla porque «"ya te pagué" contra "a mí no me llegó" no tiene árbitro».
**Indirect:** el cuaderno y el sorteo «con papelitos en una bolsa». Falla porque no deja prueba.
**Referencia visual (no competidor):** apps bancarias hermanas como Meru, Nubank o Revolut, para el lenguaje visual del rediseño.

## Differentiation
**Key differentiators:**
- **Custodia separada:** «La plata no la tenemos nosotros.» / «El encaje se verifica todos los días.»
- **Sorteo verificable:** «El turno no se arregla.» Hash de la semilla antes y semilla después.
- **Fondo de garantía:** «Si alguien no aporta, el grupo no se frena.»
- **Historial sellado:** «Lo que ya pasó, no se toca.»
- **Debido proceso:** «Un incumplimiento es un expediente, no una banderita roja.»
- **Pago conciliado con el banco**, no contra una captura de pantalla.
**Why that's better:** la confianza deja de depender de una persona y pasa a ser comprobable: «Comprobalo. No hace falta que nos creas.»

## Objections
| Objection | Response |
|-----------|----------|
| «¿Y si la app se queda con la plata?» | Custodia en cuenta bancaria separada del dinero de la empresa, encaje diario |
| «¿Y si alguien no paga?» | El fondo de garantía cubre la entrega; la deuda queda viva |
| «¿Y si el sorteo está arreglado?» | Cualquiera del grupo puede reproducir el orden con los datos publicados |
| «¿Cuánto me cobran?» | Tarifario publicado y congelado al empezar el grupo (cifra a confirmar) |

**Anti-persona:** quien busca un juego de azar o un premio (la palabra «premio» está prohibida por ser otra figura regulatoria).

## Switching Dynamics
**Push:** peleas por pagos sin prueba y la sospecha sobre el orden de cobro.
**Pull:** cuentas claras, plata en custodia y retirar cuando quiera.
**Habit:** «siempre lo hicimos con el cuaderno», y la confianza en el organizador conocido.
**Anxiety:** poner plata en una app nueva y que todavía no tenga autorización de la ASFI (la landing lo dice sin disimular).

## Customer Language
**How they describe the problem (del dominio):** «ya te pagué» / «a mí no me llegó» · «cobrar y desaparecer» · «guardar la plata»
**How they describe us:** sin citas reales de clientes todavía **(a recolectar)**.
**Words to use:** pasanaku, grupo, cupo, turno, aporte, entrega, período, organizador, custodia, plata, recargar, retirar.
**Words to avoid:** «premio», «ganador», «sorteo de premios» (otra figura regulatoria); «transacción procesada exitosamente»; «estimado usuario»; «ronda» o «junta» como sinónimo de grupo.
**Glossary:**
| Term | Meaning |
|------|---------|
| Grupo | El pasanaku concreto: N cupos, un monto, una periodicidad |
| Cupo | La posición dentro del grupo |
| Turno | El orden en que a cada cupo le toca recibir |
| Aporte | Lo que cada participante pone en el período (no es «pago») |
| Entrega | Lo que recibe quien tiene el turno (no es «premio» ni «cobro») |
| Custodia | El dinero real en el banco que respalda el saldo |

## Brand Voice
| Attribute | Means | Doesn't mean | Example line |
|-----------|-------|--------------|--------------|
| Cercano | Voseo, palabras de todos los días | Chistes o confianza excesiva con la plata | «Te toca en 2 turnos. Te avisamos.» |
| Preciso | Montos, fechas y estados exactos | Jerga bancaria | «Listo, tu aporte de Bs 250 quedó guardado.» |
| Transparente | Decir qué pasa y qué no, aunque incomode | Prometer lo que todavía no está | «Todavía no tenés historial suficiente para un puntaje — no es una sanción, es que recién empezás.» |
| Firme con la plata | La regla se cumple igual para todos | Castigar o culpar | «La deuda no se perdona: queda viva y el fondo pasa a ser el acreedor.» |

**Tone by context:**
- Support: calmo, con la acción que destraba y la referencia del caso.
- Errors: qué pasó, por qué y qué hacer ahora, sin culpar.
- Dinero confirmado: corto y celebratorio sin exagerar.
- Mora y sanciones: sobrio, con el plazo de descargo y la apelación.
**Arquetipo:** «el Cuidador confiable — cercano al hablar, impecable al manejar la plata».
**Never use (and why):** «premio» (figura regulatoria), «exitosamente» (frío y de sistema), «ganaste» (juego de azar).

## Proof Points
**Metrics:** ninguna de clientes. Las cifras reales son de ingeniería: 99 casos de uso, 305 tablas, 141 restricciones en la base, 14 servicios (`landing/README.md`).
**Customers:** ninguno todavía. Los nombres de la landing (Marisol, Las Comadres) son placeholder.
**Testimonials:** ninguno. **No se inventan** (regla 00 §2).
**Value themes:**
| Theme | Proof |
|-------|-------|
| Custodia | Cuenta separada y encaje diario (diseño) |
| Transparencia | Sorteo reproducible y períodos sellados |
| Respaldo | Fondo de garantía por grupo |

## Goals
**Business goal:** que un pasanaku entero se lleve en la app con confianza comprobable.
**Conversion action:** crear un grupo o unirse a uno, y hacer el primer aporte.
**Current metrics:** no hay **(producto en desarrollo, sin autorización ASFI todavía)**.

## Learnings
*Decisions and tested lessons. Newest first.*
| Date | Learning | Evidence | Applies to | Status |
|------|----------|----------|------------|--------|
| 2026-10-21 | Verde = estructura y confianza; el naranja se reserva para la urgencia de pagar o aportar | Decisión de la persona usuaria (A2 del plan del rediseño) | App móvil, copy de botones | active |

## Changelog
*Newest first.*
- v1 (2026-10-21) — Contexto inicial, redactado automáticamente desde el repo (identidad de marca, landing, glosario, textos de la app) para el rediseño bancario.
