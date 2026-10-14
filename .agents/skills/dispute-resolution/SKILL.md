---
name: dispute-resolution
description: Disputas, descargos y apelaciones como proceso de primera clase — tipos de disputa en Pasanaku (pago desconocido, entrega no recibida, deducción incorrecta, sanción injusta, sorteo cuestionado), expediente con evidencia de las dos partes, plazos y estados, quién decide y con qué separación de funciones, efectos provisionales mientras se resuelve, apelación en dos instancias, y la resolución como operación contable trazable. Usar al modelar o tocar disputas, descargos, apelaciones, incidencias de entrega o cualquier flujo donde un participante cuestione una decisión del sistema.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Disputas, descargos y apelaciones

Un sistema que mueve plata entre desconocidos **va a equivocarse** y **va a ser acusado de
equivocarse cuando no lo hizo**. La diferencia entre un producto confiable y uno que se cae no
es la ausencia de disputas: es que haya un proceso claro, rápido y con evidencia.

Entidades: `disputa_pago`, `descargo_participante`, `apelacion_sancion`,
`apelacion_sancion_org`, `incidencia_entrega`. Regla dura: **91.4**.

## 1. Tipos de disputa y quién los resuelve

| Tipo | Origen | Quién decide |
|---|---|---|
| Pago desconocido / no acreditado | Participante vs `pagos` | Conciliación + operaciones |
| Entrega no recibida | Beneficiario vs `entregas` | Operaciones con evidencia del proveedor |
| Deducción incorrecta | Beneficiario vs `garantia` | Operaciones sobre el expediente de deuda |
| Mora o sanción injusta | Participante vs `garantia` | Comité / segunda instancia |
| Sorteo cuestionado | Participante vs `grupos` | **Se resuelve con la verificación pública**, no con criterio |
| Incumplimiento del organizador | Participante vs organizador | Comité |

El sorteo es el único que **no necesita juicio**: si el commit-reveal está bien implementado,
cualquiera recomputa el orden y la disputa se cierra con un cálculo
(`rosca-group-lifecycle` §3). Ese es exactamente el valor de haberlo hecho verificable.

## 2. Estados y plazos

```
ABIERTA → EN_ANALISIS → (PRUEBA_SOLICITADA) → RESUELTA → { ACEPTADA | RECHAZADA }
                                                  └─▶ APELADA → SEGUNDA_INSTANCIA → FINAL
```

- **Plazo por estado**, declarado y medido. Una disputa sin plazo se convierte en un abandono
  con otro nombre.
- El silencio de la empresa **no puede resolver a su favor**: vencido el plazo sin decisión, la
  política declarada se aplica (escalar, o resolver a favor del participante).
- Toda transición queda en historial append-only con actor y motivo
  (`audit-trail-history`).

## 3. Evidencia de las dos partes

- El participante puede **adjuntar** (comprobante, captura, mensaje) con las reglas de
  `file-uploads-media` y `data-privacy-financial`.
- El sistema aporta **su** evidencia, recolectada automáticamente: la traza de la operación por
  `correlationId`, los webhooks recibidos, los asientos, el registro de notificaciones enviadas y
  entregadas (`distributed-tracing-correlation`).
- La evidencia del sistema se arma **sola**, no "cuando alguien la busca a mano". Si armar el
  expediente de una disputa exige dos horas de un ingeniero, el proceso no escala y las disputas
  se resuelven mal por cansancio.
- Nada se edita: se agrega. La versión inicial del reclamo queda visible aunque después el
  participante cambie de versión.

## 4. Efectos provisionales

Mientras se resuelve, hay que decidir **qué pasa con la plata y con el estado**:

| Situación | Efecto provisional razonable |
|---|---|
| Pago en disputa | El aporte **no** se da por cumplido, pero la cobranza se pausa |
| Entrega no recibida | Se retiene un nuevo desembolso al mismo destino hasta aclarar |
| Sanción apelada | La sanción **se suspende** mientras dura la apelación |
| Deducción disputada | El monto queda en suspenso, no se libera ni se ejecuta |

- El efecto provisional es **reversible y explícito**, con su propio registro. No es un limbo
  silencioso.
- **Prohibido dejar plata en suspenso sin fecha**: el estado provisional tiene vencimiento y
  escala si se vence.

## 5. Separación de funciones

- Quien tomó la decisión cuestionada **no la revisa**. Si el organizador sancionó, el
  organizador no resuelve la apelación.
- Segunda instancia distinta de la primera, y **el código lo impone**: el actor que resolvió
  primera instancia no puede figurar como resolutor de la segunda (restricción de datos, no de
  UI).
- El comité, si existe, es una figura con permisos propios
  (`authz-access-control`), no "un admin con todo".

## 6. La resolución mueve plata: es contable

Resolver a favor del participante puede significar acreditar, reembolsar, revertir una deducción
o levantar una sanción. Todo eso:

- Genera **asientos**, nunca un ajuste de saldo (`money-movement-safety` §3).
- Queda ligado al expediente: cualquiera que mire el asiento llega a la disputa que lo originó y
  viceversa.
- Si revierte una cobertura del fondo, ajusta también la subrogación y la deuda
  (`guarantee-fund-workflows`).
- Si revierte una sanción, **restaura la reputación** por el mismo mecanismo que la bajó
  (`transparency-reputation`): un evento de reputación nuevo, no un `UPDATE` del score.

## 7. Comunicación

- El participante ve **el estado de su disputa**, qué falta y para cuándo. No saber es lo que
  convierte un error en una reseña de una estrella.
- La resolución se comunica con **motivo entendible** (`ux-writing-microcopy`), sin exponer
  criterios antifraude ni datos de terceros.
- Se registra la notificación y su entrega: "se le avisó" tiene que ser demostrable
  (`notifications-delivery`).
- Excepción: las disputas que tocan cumplimiento respetan el deber de reserva
  (`aml-sanctions-screening` §5).

## Anti-patrones

- Resolver disputas por WhatsApp sin expediente.
- Sin plazos, o con plazos que nadie mide.
- Que el silencio de la empresa resuelva a su favor.
- Expediente sin la evidencia del sistema, o que requiere armarse a mano.
- Editar el reclamo original.
- Sanción que sigue vigente durante la apelación.
- Plata en suspenso sin fecha de resolución.
- El mismo actor decidiendo primera y segunda instancia.
- Resolver con un `UPDATE` de saldo o de score.
- Resolución sin motivo comunicado.
- Discutir un sorteo por criterio en vez de recomputarlo.

## Checklist

- [ ] Tipos de disputa definidos, con quién resuelve cada uno.
- [ ] Estados con plazos declarados y medidos; política ante vencimiento.
- [ ] Evidencia del participante y del sistema, recolectada automáticamente.
- [ ] Historial append-only; nada se edita.
- [ ] Efectos provisionales explícitos, reversibles y con vencimiento.
- [ ] Sanción suspendida durante la apelación.
- [ ] Separación de instancias impuesta por datos.
- [ ] Resoluciones con efecto contable por asiento, ligadas al expediente.
- [ ] Reputación restaurada por evento, no por `UPDATE`.
- [ ] Estado y resolución comunicados y registrados.

## Evidencia / DoD

1. Expediente completo de una disputa de prueba, con evidencia de las dos partes.
2. Salida del intento de que el mismo actor resuelva las dos instancias: rechazado.
3. Asientos generados por una resolución a favor, cuadrados y ligados al expediente.
4. Prueba de que la sanción quedó suspendida durante la apelación.
5. Registro de la notificación de la resolución, con su entrega.
