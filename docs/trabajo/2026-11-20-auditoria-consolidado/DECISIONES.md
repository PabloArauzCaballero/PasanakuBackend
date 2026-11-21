# Decisiones de Pablo para el plan de correcciones (2026-11-21)

Respuestas dadas por Pablo en la sesión. Lo que no está acá sigue abierto.

| ID | Pregunta | Decisión |
|---|---|---|
| P-14 | ¿Se mueve dinero real hoy? | **No.** Todo sigue condicionado al dossier H15. Los S1 son deuda bloqueante antes de producción, no un incidente en curso. |
| P-04 | Respaldo de la empresa (tablas, cuentas 9.x, endpoints, consumidor) | **Eliminar todo.** |
| P-01/P-02 | Faltante y fondo mutual | **Modelo A + D.** A: «Tu garantía vuelve». Cada participante hace un aporte chico extra a un fondo **del grupo**, en una cuenta segregada que nunca es de la empresa. El fondo cubre la cuota de quien no paga; esa persona queda debiendo al fondo (subrogación) y lo que se recupera vuelve al fondo. Al cierre del ciclo se devuelve a cada participante lo que aportó; la deuda que no se pudo recuperar se descuenta de esa devolución, en partes iguales. D: «La puntualidad vale plata». La puntualidad da beneficios (turnos tempranos, menos aporte al fondo, parte de los recargos). Descartados por ahora: B (turno con precio), C (colchón propio) y E (fondo que rinde). |
| P-05 | Recargo por mora | **Va al perjudicado** (quien tuvo que esperar), no a la empresa. |
| — | Parámetros financieros (% del fondo, días de gracia, tasa de recargo) | **Configurables y obligatorios, sin valor por defecto.** Los fijan Pablo y Cumplimiento antes de producción; el servicio no arranca sin ellos. |
| — | Ingreso de la empresa | **Comisión fija por servicio**, transparente e igual para todos. |

## Para validar con Cumplimiento
- Que el fondo del grupo en cuenta segregada no constituya captación de dinero ante ASFI.
- Que la cobertura automática con el fondo del grupo, y la subrogación, no requieran expediente previo (la sanción y la reputación sí lo requieren, regla 91.4).

## Tandas 2 a 4

| ID | Tema | Decisión |
|---|---|---|
| P-03 | Aval entre participantes | **Eliminarlo.** El fondo del grupo (A) cubre esos casos. |
| P-06 | Pagar la cuota con saldo de billetera recargado por QR | **Cuenta como pago por QR.** El dinero entró por un QR confirmado por webhook, y esto permite el pago automático. |
| P-07 | Comprobante manual | **Eliminarlo del todo:** pantalla, endpoint, tabla y canales manuales. |
| P-08 | KYC | **Validación automática por un proveedor.** Una persona de Cumplimiento revisa solo las excepciones y los casos PEP. |
| P-09 | Doble aprobación de retiros | **Automático con controles:** segundo factor, cuenta verificada, enfriamiento tras un cambio de cuenta y monitoreo UIF. Cumplimiento revisa solo lo que dispare una alerta. Requiere un ADR que reemplace a ADR-049. |
| P-10 | Cuatro ojos en la entrega del pozo | **Entrega automática** a la cuenta verificada del beneficiario, con confirmación del proveedor por webhook firmado. |
| P-11 | Organizador profesional | **Eliminarlo.** Todos los grupos los gestiona el sistema. Se quitan postulaciones, habilitación, contrato y evaluación de desempeño. |
| P-12 | Debido proceso | **El sistema declara el incumplimiento** al vencer el plazo de gracia. El descargo lo resuelve Soporte o Riesgos; la apelación, otra persona. |
| — | Admisión a grupos | **Automática con requisitos:** KYC aprobado, reglamento aceptado y sin deudas vivas. El nivel de puntualidad (D) limita a qué turnos se accede. |
| — | Inicio y sorteo | **Automáticos** al completarse los cupos, con sorteo verificable (compromiso y revelación). |
| P-13 | Reembolsos, reversos y cierre de billetera | **Cuatro ojos reales:** dos personas distintas, tomadas del token de cada una y nunca del cuerpo de la petición. |
| P-17 | Descuentos y exenciones de comisión | **Ninguno.** Comisión igual para todos. |
| P-15 | Pasarela QR y proveedor de desembolso | **No hay todavía.** Se define un puerto y un doble con firma real en tres niveles (regla 65). |
| P-16 | Textos nuevos | **Los redacta Claude y Pablo los aprueba** antes de subirlos. |
| P-18 | Orden de ejecución | **Fase 1:** S1 de seguridad e integridad del mayor. **Fase 2:** retirar el respaldo y las aprobaciones. **Fase 3:** QR → webhook → cobro, y el modelo A + D. **Fase 4:** S2 a S4. Un PR por cambio hacia `dev`. |
| — | Revisor | **Pablo** revisa y mergea cada PR. |

Siguen abiertas: P-19 (dónde correr Docker) y P-20 (el candado `blocker_gate`).

## Tanda 5 (producto y operación)

| ID | Tema | Decisión |
|---|---|---|
| — | Entrada a los grupos | **Solo por invitación** de un miembro: entre conocidos. Requisitos: KYC aprobado, reglamento aceptado y sin deudas vivas. No hay grupos públicos. Esto reemplaza lo de «grupos públicos» de la tanda 4. |
| — | Orden de turnos | **Por nivel de puntualidad:** los niveles más altos eligen primero. Dentro del mismo nivel se ordena por sorteo verificable (compromiso y revelación). En el primer ciclo, como todos son «Semilla», el orden sale entero del sorteo. |
| — | Calificación y gamificación | Pieza D, ampliada. La calificación se calcula solo con hechos (pagos confirmados por QR y su fecha), sin calificaciones entre personas. Incluye niveles con beneficios concretos, rachas, insignias y el logro de grupo «Grupo perfecto». La reputación negativa solo nace de un expediente (regla 91.4). Se construye sobre el servicio `transparencia` existente. |
| P-19 | Dónde correr Docker y la integración | **En la máquina de Pablo**, por Remote Control, con permiso en cada caso. |
| P-20 | Las 9 microtareas viejas que traban `blocker_gate` | **Marcarlas DESCARTADO**, con el motivo «reemplazadas por el plan de correcciones del 2026-11-21», en un PR que revisa Pablo. |
| — | Fase 5 «Poder de compra» | Propuesta aceptada como dirección. Es un piloto condicionado a dos cosas: la opinión legal (ASFI y garantía sobre bienes) y al menos un comercio firmado. Fuera de las fases 1 a 4. |
