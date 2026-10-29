# Voz y microcopy de la app (H1.S1.M2)

Fuente: `.agents/product-marketing.md` (v1), la identidad de marca (`docs/Views/AportaYa-Identidad.md`)
y los textos que ya existen en `apps/movil/lib/pantallas/**/textos*.dart`. Skills aplicadas:
`copywriting`, `copy-editing` y `ux-writing-microcopy` (la de la casa manda si chocan).

**Diagnóstico (skill `copywriting`): situación B, voz real presente.** La app ya habla con
voseo, dice «plata», «custodia» y «cuentas claras», y tiene un arquetipo («el Cuidador confiable»).
El rediseño **conserva** esa voz y la ordena. No la reemplaza por tono de banco ni por tono de startup.

## 1. Cinco reglas

1. **Claro antes que ingenioso.** Un botón dice lo que pasa al tocarlo: «Aportar Bs 250», no «Continuar».
2. **El número manda.** Si hay un monto, una fecha o un turno, va escrito con su unidad: «Bs 1.250,00», «vence el jueves 24», «te toca en 2 turnos».
3. **Nada en pasado que no pasó.** «Te mandamos un código» solo después de mandarlo. Un pago es «en camino» hasta que el banco lo confirma (regla 91.3).
4. **Errores con tres partes:** qué pasó, por qué (si se sabe) y qué hacer ahora. Sin culpar y con la referencia del caso.
5. **Una palabra por concepto** (glosario): aporte, entrega, turno, cupo, grupo, custodia.

## 2. Decimos / no decimos

| # | Contexto | Decimos | No decimos | Por qué |
|---|---|---|---|---|
| 1 | Aporte confirmado | «Listo, tu aporte de Bs 250 quedó guardado.» | «Transacción procesada exitosamente.» | Monto concreto y voz de persona |
| 2 | Aporte en camino | «Tu aporte está en camino. Te avisamos cuando el banco lo confirme.» | «¡Pago realizado!» | No se promete antes de cobrar (91.3) |
| 3 | Turno | «Te toca en 2 turnos.» | «Su posición en la cola es la número 3.» | Cercano y en la unidad de la persona |
| 4 | Entrega | «Recibiste tu entrega: Bs 2.400.» | «¡Ganaste el pozo!» | «Ganar» y «premio» sugieren juego de azar |
| 5 | Saldo | «Disponible» / «Retenido» | «Su saldo disponible es de…» | Etiqueta corta, cifra protagonista |
| 6 | Ocultar saldo | «Ocultar saldo» / «Mostrar saldo» | Ícono sin nombre | Botón de solo ícono con nombre accesible (95.4.1) |
| 7 | Recargar | «Recargar» | «Fondear cuenta» | Palabra de todos los días |
| 8 | Retirar | «Retirá tu plata cuando quieras.» | «Solicitud de rescate de fondos» | La retirada es un derecho, no un trámite |
| 9 | Custodia | «Tu plata queda en una cuenta separada del dinero de la empresa.» | «Fondos segregados bajo esquema fiduciario» | Se entiende sin glosario |
| 10 | Confianza | «Comprobalo. No hace falta que nos creas.» | «Somos 100 % seguros.» | Falsable; no es un adjetivo |
| 11 | Sorteo | «El turno se sorteó a la vista de todos. Cualquiera puede comprobarlo.» | «Algoritmo de aleatorización criptográfica» | Beneficio antes que mecanismo |
| 12 | Sorteo, detalle | «Ver los datos del sorteo» (plegable) | Hashes a la vista de entrada | El detalle técnico va detrás de un toque |
| 13 | Fondo de garantía | «Si alguien no aporta, el fondo cubre tu entrega.» | «Mecanismo de mitigación de riesgo de contraparte» | La persona entiende qué le pasa a ella |
| 14 | Mora propia | «Tenés un aporte pendiente de Bs 250. Ponete al día antes del jueves 24.» | «Usted se encuentra en mora.» | Sin culpa, con la acción y el plazo |
| 15 | Sanción | «Abrimos un expediente. Tenés hasta el 30/10 para contar tu versión.» | «Fuiste sancionado.» | Debido proceso visible (91.4) |
| 16 | Vacío de grupos | «Todavía no estás en ningún pasanaku. Unite con un enlace o armá el tuyo.» | «No hay datos.» | El vacío orienta (95.2.2) |
| 17 | Vacío de movimientos | «Cuando recargues o aportes, tus movimientos aparecen acá.» | (pantalla en blanco) | Dice qué va a aparecer |
| 18 | Error de servicio | «El servicio no responde en este momento. No es tu conexión: probá de nuevo en unos minutos. Código: 3a1b05ac» | «Error 503» | Tres partes y referencia |
| 19 | Sin conexión | «Estás sin conexión. Lo que ves es de las 10:30.» | «Network error» | Dice la hora del dato |
| 20 | Botón apagado | «Escribí el monto para seguir.» (debajo del botón) | Botón gris sin motivo | Un botón apagado dice por qué |
| 21 | Campo de monto | Etiqueta «¿Cuánto aportás?» + ayuda «Con coma para los centavos» | Placeholder como única etiqueta | El placeholder no es etiqueta (95.3.5) |
| 22 | Invitar | «Invitá a tu grupo. Les llega un enlace y vos aceptás a quién entra.» | «Comparte tu código de referido» | Explica qué pasa del otro lado |
| 23 | Mensaje de invitación | «Te invito a mi pasanaku "Las Comadres" en AportaYa: Bs 250 por mes, 10 cupos. Entrá acá: …» | «¡Descargá la app y ganá!» | Concreto; nada de premios |
| 24 | Unirse | «Pedir mi cupo» | «Enviar solicitud» | Nombra lo que la persona quiere |
| 25 | Crear grupo | «Armar mi grupo» | «Crear nueva entidad grupo» | Verbo del día a día |
| 26 | Confirmar dinero | «Aportar Bs 250» | «Aceptar» | Repite el monto en el botón final |
| 27 | Cerrar sesión | «Cerrar sesión» | «Desloguearse» | Sin anglicismos |
| 28 | Baja | «Dar de baja tu cuenta» (con consecuencias listadas arriba) | «Eliminar» sin contexto | Acción destructiva explicada |
| 29 | Candado | «Hola de nuevo. Entrá con tu huella o tu PIN.» | «Sesión bloqueada por inactividad.» | Cálido y con la salida |
| 30 | Sesión vencida | «Por seguridad, tu sesión dura 8 horas. Entrá de nuevo.» | «Token expirado» | Motivo humano |
| 31 | Verificación | «Estamos revisando tu identidad. Ya podés usar la app; la billetera se activa cuando terminemos.» | «Estado: PENDIENTE_KYC» | Estado en palabras |
| 32 | Permiso de cámara | «Necesitamos tu cámara para la foto de tu carnet. Solo se usa para eso.» | «Permitir acceso a la cámara» a secas | Pide en el momento y dice para qué |
| 33 | Éxito de alta | «Tu cuenta está lista.» | «¡Felicitaciones! ¡Bienvenido a bordo!» | Sin exclamaciones de relleno |
| 34 | Ayuda | «¿Cómo funciona el fondo de garantía?» | «FAQ #12» | La pregunta de la persona |

## 3. Tics de IA prohibidos (skill `copywriting`, en español)

- **Revelación por contraste:** «No es un X, es un Y.» → decí Y con su motivo.
- **Listas de negaciones:** «Sin cuadernos, sin planillas, sin vueltas.» → decí lo que sí pasa. La landing tiene una; en la app no se repite.
- **Pregunta que se contesta sola:** «¿El resultado? Tu turno a tiempo.» → «Tu turno llega a tiempo.»
- **Frases de relleno:** «en un mundo cada vez más…», «llevá tu X al siguiente nivel», «desbloqueá».
- **Exclamaciones** en estados de dinero. Una celebración se dice con la cifra, no con «¡¡!!».
- **Guiones largos** en títulos y botones.

## 4. Fórmulas

- **Botón:** verbo + objeto (+ monto si mueve plata): «Recargar», «Aportar Bs 250», «Ver el sorteo».
- **Estado vacío:** qué no hay + qué hacer: «Todavía no tenés avisos. Te avisamos cuando te toque aportar.»
- **Error:** qué pasó + qué hacer + referencia.
- **Confirmación:** cifra + destino + cuándo: «Bs 250 a Las Comadres, hoy 21/10.»
