# Flujos objetivo del modelo A + D (todavía sin código)

> Recorridos que fijan ADR-055 «Sin aprobaciones en el circuito», ADR-056 «Tu garantía vuelve y la puntualidad
> vale plata» y ADR-058 «Integración con proveedores externos» (propuestos en los PR #402, #403 y #404), y que el código todavía **no** implementa. Escrito en
> F0.M5 (2026-11-21).
>
> **Por qué no son `.yaml` del catálogo.** `scripts/validar_flujos.py` comprueba que cada paso exista en el
> código (el `operationId` en el contrato, el componente en `src/main/java`, la prueba con su método). No sabe si
> un paso está **decidido**: por eso validaba como correctos `cubrir-faltante` y `recuperar-respaldo`, que
> DECISIONES manda eliminar, y no puede validar estos pasos, que están decididos pero no existen. Para no inventar
> nombres de operaciones (regla 00 §1), acá cada paso se nombra por lo que hace y por la microtarea que lo
> construye. Cuando la microtarea se cierre, el paso pasa al `.yaml` correspondiente con su `operationId`, su
> componente y su prueba reales, y este documento se achica.
>
> Mientras tanto: **ningún paso de esta página está verificado.** Los casos de tres niveles son los criterios de
> aceptación que exige cada microtarea del plan de correcciones.

## 1. Cobro de la cuota: QR → webhook → cobrado (`aportes`, `nucleo-financiero`)

Reemplaza en `aporte-mora-cobertura.yaml` al paso `cobrar-aporte` con canales declarados.

| # | Paso | Actor y disparo | Servicio | Microtarea |
|---|---|---|---|---|
| 1 | Generar las cuotas del periodo | Sistema, programado | `aportes` | ADR-055 E3 (anexo L1) |
| 2 | Emitir la orden de cobro con QR de monto fijo, vencimiento y `clave_idempotencia`; dos líneas: cuota (incluye el aporte al fondo) y comisión | Participante | `aportes` | F3.S1.M1, PX1.M02 |
| 3 | Recibir el webhook firmado por el ingreso común: firma, ventana, crudo, deduplicación, outbox, 2xx | Pasarela | `comun-web` + `aportes` | PX0.M02, F3.S1.M2 |
| 4 | Por encima del umbral, reconsultar el estado antes de acreditar | Sistema | `aportes` | PX1.M03 |
| 5 | Publicar `aporte_cobrado` por outbox | Sistema, evento | `aportes` | F3.S1.M3 |
| 6 | Asentar y aplicar al pozo, al fondo del grupo y a la comisión, idempotente por `message_id` | Sistema, evento | `nucleo-financiero` | F3.S1.M3, F3.S3.M1, F3.S8.M5 |
| 6b | Alternativa: pagar con saldo de billetera recargado por QR (débito y crédito en un asiento) | Participante | `nucleo-financiero` | F3.S1.M4 |

Casos (de F3.S1.M2 y PX-1):
- **Correcto:** webhook firmado con el monto de la orden → un pago, asientos de pozo, fondo y comisión, `aporte_cobrado`.
- **Límite:** el mismo webhook dos veces → un solo efecto · evento en el borde de la ventana → aceptado · pago después del corte → se aplica primero a la deuda con el fondo (F3.S1.M6).
- **Inválido:** firma inválida → 401 sin efecto · monto distinto → excepción de conciliación, sin acreditar · orden inexistente o vencida → rechazo · evento viejo → rechazo · pasarela que no responde → la resuelve la reconsulta sin duplicar.

## 2. Mora, cobertura del fondo del grupo y subrogación (`garantia`, `aportes`)

Reemplaza en `aporte-mora-cobertura.yaml` a los pasos retirados `cubrir-faltante` y `recuperar-respaldo`.

| # | Paso | Actor y disparo | Servicio | Microtarea |
|---|---|---|---|---|
| 1 | Al vencer el plazo de gracia, cubrir la cuota impaga con el fondo del grupo; idempotente por obligación; límite = saldo del fondo leído del mayor dentro de la transacción | Sistema, programado | `garantia` | F3.S3.M2 |
| 2 | Nace la deuda del moroso con el fondo (subrogación) | Sistema | `garantia` | F3.S3.M3 |
| 3 | Calcular el recargo por mora y acreditarlo al fondo del grupo si el fondo cubrió, o a quien tuvo que esperar si el fondo no alcanzó (DECISIONES, tanda 9) | Sistema, programado | `aportes` / `nucleo-financiero` | F3.S3.M4, F3.L1.M14 |
| 4 | Declarar el incumplimiento y abrir el expediente con plazo de descargo | Sistema | `garantia` | F3.S6.M1 |
| 5 | Resolver el descargo (Soporte o Riesgos); si prospera, revertir el recargo con contra-asiento | Persona (C4 de ADR-055) | `garantia` | F3.S6.M2, F3.S3.M4 |
| 6 | El moroso paga: se imputa primero a su deuda con el fondo | Participante, webhook | `aportes` / `garantia` | F3.S1.M6, F3.S3.M3 |

Casos (de F3.S3.M2–M4):
- **Correcto:** cuota impaga al vencer la gracia → el fondo la cubre una vez, nace la deuda, el recargo entra al fondo del grupo.
- **Límite:** dos coberturas simultáneas de la misma obligación → un solo efecto · saldo del fondo exactamente igual a la cuota → cubre justo · descargo que prospera → contra-asiento del recargo · el fondo no alcanzó y el beneficiario esperó → el recargo va a quien esperó.
- **Inválido:** fondo sin saldo suficiente → no se cubre una parte; la entrega espera con el motivo visible · recargo hacia una cuenta de la empresa → la base lo rechaza (kill-test 3).

## 3. Entrega automática del turno (`entregas`)

Reemplaza en `entrega-del-turno.yaml` a los pasos humanos `autorizar-entrega` y `ejecutar-entrega`.

| # | Paso | Actor y disparo | Servicio | Microtarea |
|---|---|---|---|---|
| 1 | Pozo completo (cobrado + cubierto por el fondo) → orden de desembolso con `clave_idempotencia` | Sistema, evento | `entregas` | F3.S2.M1 |
| 2 | Verificar cuenta del beneficiario y enfriamiento tras un cambio de cuenta | Sistema | `entregas` / `identidad` | F3.S2.M3, PX6.M01–M02 |
| 3 | Enviar al proveedor de desembolso (puerto único) | Sistema | `entregas` | F3.S2.M2, PX2.M01 |
| 4 | Acreditada por webhook firmado del proveedor; un timeout se resuelve por consulta de estado | Proveedor | `entregas` | F3.S2.M1, PX0.M05 |
| 5 | Deducciones línea por línea; suma ≤ bruto garantizada en la base | Sistema | `entregas` | F3.S2.M4 |

Casos (de PX-2):
- **Correcto:** desembolso acreditado por webhook firmado → un solo asiento.
- **Límite:** la misma orden dos veces → una sola transferencia · webhook antes de la respuesta HTTP · cuenta que sale del enfriamiento justo a tiempo.
- **Inválido:** timeout sin respuesta → consulta de estado, sin duplicar · rechazo bancario → orden nueva · firma inválida → sin efecto · cuenta en enfriamiento → bloqueado con motivo visible.

## 4. Cierre del ciclo (`garantia`, `entregas`)

Reemplaza en `entrega-del-turno.yaml` a los pasos retirados `liberar-reserva` y `devolver-fondo`.

| # | Paso | Actor y disparo | Servicio | Microtarea |
|---|---|---|---|---|
| 1 | Último turno entregado → calcular desde el mayor la devolución de cada participante. Lo incobrable se compensa **primero** con la devolución del propio moroso (que no recibe nada mientras deba); el resto se descuenta en partes iguales **solo entre los que cumplieron** (DECISIONES, tanda 9) | Sistema, evento | `garantia` | F3.S3.M5 |
| 2 | Repartir el residuo de redondeo de a un centavo, por nivel y con desempate por sorteo | Sistema | `garantia` | F3.S3.M8 |
| 3 | Cada devolución es un desembolso con las reglas de la sección 3 | Sistema | `garantia` → `entregas` | F3.S3.M5 |
| 4 | El fondo queda en cero y cuadra; la deuda incobrable se castiga sin borrarse | Sistema | `garantia` | F3.S3.M5 |

Casos (de F3.S3.M5 y M8):
- **Correcto:** ciclo sin morosos → cada uno recupera su aporte íntegro; fondo en cero.
- **Límite:** división inexacta → la suma repartida es igual al total, sin perder un centavo · un solo moroso con deuda parcialmente recuperada → su devolución compensa primero y el resto lo absorben los que cumplieron · deuda menor que la devolución del moroso → nada se reparte y el moroso recibe el saldo · grupo que arma otro ciclo sin morosos («Grupo perfecto») → el ciclo siguiente nace con el porcentaje menor de aporte al fondo.
- **Inválido:** el fondo no cuadra con su cuenta segregada → se suspende el cierre y salta la alerta (PX3.M04).

## 5. Retiro de la billetera con controles (`nucleo-financiero`)

Reemplaza en `recarga-y-retiro.yaml` al paso humano `aprobar-retiro` y cablea `instruir-pago`.

| # | Paso | Actor y disparo | Servicio | Microtarea |
|---|---|---|---|---|
| 1 | Solicitar el retiro con segundo factor ligado a la operación | Participante | `nucleo-financiero` | F3.S5.M2 |
| 2 | Controles automáticos: cuenta verificada, enfriamiento, límites por ventana, monitoreo UIF | Sistema | `nucleo-financiero` / `cumplimiento` | F3.S5.M2, PX2.M04 |
| 3 | Sin alerta: instruir el pago al proveedor de desembolso. Con alerta: retener con motivo visible y pasar a Cumplimiento | Sistema / Cumplimiento | `nucleo-financiero` | F3.S5.M2 |

Casos (de F3.S5.M2):
- **Correcto:** retiro normal → sale sin persona.
- **Límite:** monto exactamente en el límite de la ventana → sale · cuenta que sale del enfriamiento justo a tiempo.
- **Inválido:** retiro con alerta → retenido con su motivo · sin segundo factor → rechazo · cuenta en enfriamiento → rechazo.

## 6. Vida del grupo: creación, arranque y disolución (`grupos`, `garantia`)

Sin equivalente en el catálogo vivo. Reglas de ADR-055 (§1-bis, E6, E14) y de DECISIONES, tanda 9.

| # | Paso | Actor y disparo | Servicio | Microtarea |
|---|---|---|---|---|
| 1 | Crear el grupo con su tamaño exacto, monto, periodicidad, fecha de inicio, nombre y fecha límite; quien lo crea queda como **creador** (administra la configuración, no toca dinero) | Participante | `grupos` | F3.S4.M1 |
| 2 | Invitar y revocar invitaciones mientras haya cupos; cambiar la configuración solo antes de arrancar | Creador | `grupos` | F3.S4.M1 |
| 3 | Cupos completos → inicio y sorteo automáticos (orden por nivel y sorteo dentro del nivel) | Sistema, programado | `grupos` | F3.S4.M2, F3.S4.M3 |
| 4 | Fecha límite sin llenar → el creador decide: achicar (si todos aceptan el nuevo pozo), extender o cancelar. Sin decisión en el plazo parametrizado, se cancela. Nadie pagó nada hasta el arranque | Creador / sistema, programado | `grupos` | F3.S4.M2 |
| 5 | Con el grupo en marcha: agregar cupos al final del calendario por votación, sin tocar turnos asignados | Grupo (votación) | `grupos` | F3.S4.M5 |
| 6 | Pasar el rol de creador a otro participante, que lo acepta | Creador → participante | `grupos` | F3.S4.M1 |
| 7 | Disolución automática: el fondo se agota y la mora supera el umbral (parámetro) → el grupo se disuelve y cada uno se liquida desde el mayor. Convive con la disolución votada | Sistema | `grupos` / `garantia` | F3.S4.M5, F3.S3.M5 |

Casos:
- **Correcto:** el grupo se llena antes de la fecha límite → arranca y sortea solo.
- **Límite:** a la fecha límite falta un cupo y todos aceptan el pozo menor → el grupo se achica y arranca · el creador no decide en el plazo → se cancela · mora exactamente en el umbral con el fondo agotado → se aplica la regla tal como la fije el parámetro (umbral inclusivo o no: a definir con su valor).
- **Inválido:** con el grupo en marcha el creador intenta cambiar el monto; o, en cualquier momento, expulsar a alguien, cambiar un turno, aprobar un pago o mover dinero → 403 sin efecto · con el grupo en marcha intenta agregar un cupo sin votación → rechazo · otra persona intenta un poder del creador → 403.

