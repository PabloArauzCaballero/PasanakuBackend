# Preguntas para el abogado y Cumplimiento — Pasanaku (AportaYa)

Contexto para quien responde: Pasanaku es una app de pasanaku digital. Desde el 2026-11-20 rigen dos reglas.
Primera: el flujo solo avanza con pagos por QR confirmados por la pasarela; nadie aprueba nada a mano.
Segunda: la empresa nunca pone plata por quien no paga.

Modelo elegido (ADR-056):
- **Fondo del grupo.** Cada participante aporta un porcentaje extra a un fondo del grupo, en una cuenta
  segregada que nunca es de la empresa.
- **Cobertura.** El fondo cubre la cuota de quien no paga, y ese participante queda debiéndole al fondo.
- **Cierre del ciclo.** Al cerrar, cada participante recupera su aporte al fondo menos su parte de lo que
  no se pudo cobrar.
- **Ingresos de la empresa.** La empresa cobra un porcentaje único por servicio.

Hoy no se mueve dinero real: la habilitación depende del dossier H15.

| # | Pregunta | Por qué importa | Microtarea que bloquea |
|---|---|---|---|
| L-01 | ¿Retener un aporte de los participantes en un fondo del grupo, en una cuenta segregada, y devolverlo al cierre del ciclo constituye captación de recursos del público o intermediación ante ASFI? ¿Qué figura lo permite: cuenta en una entidad regulada, fideicomiso u otra? | Si es captación, hay que rediseñar el modelo A | F3.S3.M1–M7 |
| L-02 | ¿Puede el fondo cubrir automáticamente la cuota impaga y subrogarse en la deuda del moroso sin un expediente previo, si eso está previsto en el contrato que firma al entrar? | Define si la cobertura espera al descargo | F3.S3.M2–M3 |
| L-03 | ¿Puede cobrarse un recargo por mora automático (anulable si prospera el descargo) y acreditarse al participante perjudicado? ¿Hay tasa o tope legal? | El recargo no va a la empresa | F3.S3.M4 |
| L-04 | ¿Puede la empresa reportar impagos a una central de riesgo crediticio, con el consentimiento firmado al entrar? ¿Con qué requisitos? | Es parte del costo de irse sin pagar | F3.S7 / F5 |
| L-05 | ¿La doble aprobación humana de retiros (ADR-049) es exigible, o alcanza con niveles de autorización aprobados por el Directorio, más segundo factor, cuenta verificada, enfriamiento y monitoreo UIF? | Se decidió quitarla | F3.S5.M2 |
| L-06 | ¿La verificación de identidad puede hacerla un proveedor automático, con revisión humana solo de excepciones y PEP? ¿Qué tipos de documento se admiten? (AMB-L4-2) | KYC automático | F3.S5.M1 |
| L-07 | ¿Los bloqueos automáticos por listas LFT requieren un plazo de descargo? (AMB-L4-1) ¿El bloqueo judicial de saldo debe ser un acto humano, con dos personas distintas para bloquear y para levantar? (AMB-L1-3) | Debido proceso en cumplimiento | F4 de L1 y L4 |
| L-08 | ¿El «Instructivo EIF» citado en las semillas aplica a una ETF/fintech como AportaYa? (AMB-L4-4) | Define qué normas citar | F4 de L4 |
| L-09 | ¿La publicación del tarifario (porcentaje único de comisión) requiere la aprobación de un comité y un preaviso a los usuarios? (D-L5-3) | Gobierno de tarifas | F4 de L5 |
| L-10 | Valores de los parámetros normativos: umbrales UIF, montos de diligencia reforzada y plazos legales de descargo y apelación. (AMB-L4-3) | Son parámetros obligatorios sin valor por defecto | F3.S8.M3 |
| L-11 | **«Poder de compra»:** grupos que ahorran para comprar en un comercio aliado con descuento, con canje en la tienda y turnos tempranos pagando cuotas por adelantado. ¿Cómo lo clasifica ASFI? En Brasil, el «consórcio» es una actividad regulada. ¿Se puede dejar el bien como garantía hasta terminar de pagar, por ejemplo con reserva de propiedad en vehículos? | Condición para empezar a construir la fase 5 | F5 |
| L-12 | ¿Las transferencias de saldo entre personas (P2P) dentro de la billetera requieren algún control o límite específico? | Se decidió permitirlas | F4 de L1 |
