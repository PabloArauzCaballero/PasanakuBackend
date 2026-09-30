---
name: saga-distributed-transactions
description: Transacciones que cruzan servicios en Pasanaku — por qué no hay rollback distribuido, saga por coreografía vs orquestación, diseño de compensaciones idempotentes, orden de pasos (compensables primero, irreversibles al final), estado persistido y timeout por paso, semántica de compensación cuando el dinero ya salió, sagas de las tres operaciones críticas (acreditar aporte, entregar fondo, ejecutar aval) y cómo se prueba una saga rota a la mitad. Usar al diseñar o tocar cualquier flujo que cambie estado en dos o más servicios, especialmente si mueve plata, y al diagnosticar una operación que quedó a medio camino.
allowed-tools: Read Grep Glob Bash
effort: high
---

# Sagas y transacciones distribuidas

En un monolito, `ROLLBACK` deshace todo. Acá no existe: cuando `pagos` ya acreditó y
`contabilidad` falla, **el aporte ya está acreditado**. La única salida es una operación de
negocio que lo revierta, y esa operación hay que diseñarla, escribirla y probarla.

La prohibición de 2PC y el requisito de outbox están en la **regla 98.3**. Acá está el cómo.

## 1. Lo primero: ¿se puede evitar?

Una saga es cara de escribir y más cara de depurar. Antes de diseñar una:

1. **¿Los dos cambios son del mismo dueño?** Entonces es una transacción local y el límite estaba
   mal trazado (`microservices-architecture`).
2. **¿El segundo paso puede ser eventual?** Si `contabilidad` puede asentar 200 ms después sin que
   nadie decida nada con el saldo intermedio, no hay saga: hay un evento y un consumidor
   idempotente. **Este es el caso más común y el más barato.**
3. **¿Alguien toma una decisión de dinero en el medio?** Si sí, hay saga con estado explícito.

## 2. Coreografía vs orquestación

| | Coreografía | Orquestación |
|---|---|---|
| Cómo | Cada servicio reacciona a eventos del anterior | Un coordinador con estado persistido dirige |
| Bien para | 2–3 pasos, sin decisiones | 4+ pasos, timeouts, ramas, compensaciones |
| Mal para | Entender qué pasó: la lógica no está en ningún lado | Meter reglas de negocio ajenas en el coordinador |
| En Pasanaku | `notificaciones`, `confianza` reaccionando a hechos | **Toda saga de dinero** |

**Decisión de la casa:** los flujos de dinero se orquestan. El coordinador vive en el servicio
dueño del resultado (la entrega la orquesta `entregas`, no un "servicio de sagas" genérico).

## 3. Las tres sagas críticas de Pasanaku

### 3.1 Acreditar un aporte (`pagos` → `contabilidad` → `grupos` → `notificaciones`)

| Paso | Servicio | Compensación |
|---|---|---|
| 1. Registrar pago confirmado por webhook | `pagos` | Marcar pago como reversado (nunca borrar) |
| 2. Asentar en el mayor | `contabilidad` | Contra-asiento (regla 91.1.3) |
| 3. Marcar la obligación como cumplida | `grupos` | Reabrir obligación con motivo |
| 4. Notificar al participante | `notificaciones` | **Ninguna: irreversible, va último** |

### 3.2 Entregar el fondo (`entregas` orquesta)

| Paso | Servicio | Compensación |
|---|---|---|
| 1. Congelar el cálculo de la bolsa bruta | `entregas` | Descongelar |
| 2. Pedir deuda exigible y deducciones | `garantia` | Liberar la reserva de deducción |
| 3. Asentar liquidación | `contabilidad` | Contra-asiento |
| 4. Verificar cuenta bancaria y enfriamiento | `entregas` | — (solo lectura) |
| 5. **Ordenar el desembolso** | proveedor | **Punto de no retorno**: a partir de acá se corrige con una devolución, no con una compensación automática |
| 6. Confirmar recepción y notificar | `entregas` / `notificaciones` | Ninguna |

**El paso 5 define la saga entera.** Todo lo reversible va antes; después, solo hechos.

### 3.3 Ejecutar un aval por incumplimiento firme (`garantia` orquesta)

Precondición no negociable: el expediente está `FIRME` con el plazo de descargo cumplido
(regla 91.4). Una saga no puede saltearse el debido proceso porque "el caso era obvio".

## 4. Reglas de las compensaciones

1. **Idempotente**: compensar dos veces deja el mismo resultado que compensar una.
2. **Siempre posible**: si un paso no se puede compensar, va al final o la saga se rediseña. No
   existe "esto casi nunca falla".
3. **Semánticamente honesta**: una compensación **no borra**, registra lo contrario. El mayor
   muestra el asiento y su contra-asiento; la bitácora muestra los dos hechos. Quien audite tiene
   que poder ver que pasó y se revirtió.
4. **Puede fallar**: tiene reintento, DLQ y **procedimiento manual documentado** con dueño. Una
   compensación fallida es un incidente de dinero, no un log de error
   (`incident-response-postmortem`).
5. **No inventa plata**: compensar un desembolso ya enviado no es "restar el saldo", es abrir una
   devolución (`devolucion_fondo`) con su propio ciclo.

## 5. Estado de la saga: persistido o no existe

```ts
// La saga es una máquina de estados guardada, no variables en memoria de un worker
interface SagaEntrega {
  id: string;
  entregaId: string;
  estado: 'INICIADA' | 'DEDUCCIONES_RESERVADAS' | 'ASENTADA' | 'DESEMBOLSO_ORDENADO'
        | 'COMPLETADA' | 'COMPENSANDO' | 'FALLIDA_REQUIERE_HUMANO';
  pasoActual: number;
  claveIdempotencia: string;      // la misma para todos los reintentos de esta saga
  venceEn: Date;                  // timeout del paso actual
  intentos: number;
  ultimoError?: string;
  correlationId: string;
}
```

- **Timeout por paso, obligatorio.** Una saga colgada es peor que una fallida: nadie se entera.
  Al vencer, compensa o escala a humano; nunca se queda esperando.
- El estado se guarda **antes** de disparar el paso, no después. Si el proceso muere entre medio,
  al reiniciar sabés en qué paso estabas y que puede haberse ejecutado (por eso es idempotente).
- Estado terminal `FALLIDA_REQUIERE_HUMANO` es legítimo y necesario: alerta, dueño y runbook.
- El `correlationId` de la saga es el mismo de punta a punta (`distributed-tracing-correlation`).

## 6. Interacción con outbox e idempotencia

- Cada paso publica su evento **por outbox**, en la misma transacción que su cambio de estado.
- Cada consumidor deduplica por `message_id` (`async-messaging-events` §4).
- La clave de idempotencia de la saga se propaga a los efectos externos: el proveedor de pagos
  recibe siempre la misma clave para el mismo desembolso, así un reintento no manda dos veces.
- **Sin outbox, la saga miente**: podés tener el estado avanzado y el evento perdido, y la saga
  se detiene sin que nadie lo sepa.

## 7. Cómo se prueba (no es opcional)

Una saga que solo se probó por el camino feliz **no está probada**. El DoD exige:

1. **Fallo en cada paso**: apagar el dependido, o inyectar error, y verificar que compensa hasta
   dejar el sistema consistente. Un test por paso.
2. **Duplicado en cada paso**: reenviar el mismo mensaje y verificar efecto único.
3. **Muerte del orquestador** entre "guardar estado" y "disparar paso": reiniciar y verificar que
   retoma sin duplicar.
4. **Timeout**: forzar que un paso no responda y verificar que vence y compensa.
5. **Compensación fallida**: verificar que termina en `FALLIDA_REQUIERE_HUMANO` con alerta.

Herramientas: apagar contenedores (`docker compose stop pagos`), reglas de fallo en el broker,
`microservices-testing` para el andamiaje.

## Anti-patrones

- 2PC, o cualquier diseño que asuma atomicidad entre dos bases.
- Saga sin estado persistido: "lo maneja el worker en memoria".
- Compensación que hace `DELETE` o `UPDATE` sobre el mayor o la bitácora.
- Paso irreversible primero ("mandemos el WhatsApp así el usuario ve que arrancó").
- Saga sin timeout por paso.
- Compensación no idempotente, que al reintentarse devuelve la plata dos veces.
- Orquestador genérico que conoce las reglas de negocio de los cinco servicios.
- Probar solo el camino feliz y declarar `VERIFIED` (regla 98.8).

## Checklist

- [ ] Se descartó, por escrito, que el flujo pueda ser una transacción local o un evento simple.
- [ ] Pasos ordenados: compensables primero, irreversible al final, punto de no retorno marcado.
- [ ] Cada paso tiene su compensación, idempotente y semánticamente honesta.
- [ ] Estado de la saga persistido antes de disparar cada paso, con timeout e intentos.
- [ ] Estado terminal para intervención humana, con alerta, dueño y runbook.
- [ ] Publicación por outbox y consumidores con deduplicación.
- [ ] Clave de idempotencia única propagada a los efectos externos.
- [ ] Tests de fallo, duplicado, reinicio, timeout y compensación fallida, todos ejecutados.

## Evidencia / DoD

1. Tabla de pasos y compensaciones del flujo, con el punto de no retorno señalado.
2. Salida de un test por cada paso fallando, mostrando el estado final consistente.
3. Salida de la prueba de duplicado: efecto único demostrado con la consulta pegada.
4. Salida de la prueba de reinicio del orquestador a mitad de saga.
5. Traza distribuida de una saga completa con un solo `correlationId`.
