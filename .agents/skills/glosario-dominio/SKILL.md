---
name: glosario-dominio
description: "Nombrar bien en AportaYa: el vocabulario del pasanaku y de la billetera, en español, con el término exacto para cada concepto y los pares que se confunden. Úsala al nombrar una tabla, una función, una variable, un endpoint o un texto de interfaz, y cuando dos personas del equipo usen palabras distintas para lo mismo."
---

# Vocabulario del dominio

El código, el modelo, la especificación y la interfaz usan **las mismas palabras**.
Cuando el nombre de una tabla, el de una función y el del botón difieren, cada
conversación empieza traduciendo, y las traducciones se equivocan.

Todo en **español**, sin acentos en identificadores (`snake_case` en base,
`camelCase` en TypeScript), con acentos en textos.

## El pasanaku

| Término | Qué es | No confundir con |
| --- | --- | --- |
| **Grupo** | El pasanaku concreto: N cupos, un monto, una periodicidad | "Ronda", "junta", "partida" |
| **Cupo** | La posición dentro del grupo. Se ocupa, se traspasa, se libera | Participante: el cupo persiste aunque cambie la persona |
| **Participante** | La persona en un cupo, con su historia en ese grupo | Usuario: alguien es usuario una vez y participante muchas |
| **Turno** | El orden en que a cada cupo le toca recibir | Período: el turno es de quién, el período es cuándo |
| **Período** | Cada ciclo de aportes del grupo | Turno |
| **Organizador** | Quien administra el grupo. **No cobra comisión** (RN-18) | Plataforma: la comisión la cobra AportaYa |
| **Aporte** | Lo que cada participante pone en el período | Pago: el aporte es la obligación, el pago es cómo se cumple |
| **Obligación de aporte** | La deuda concreta de un participante en un período | Aporte en abstracto |
| **Entrega** | Lo que recibe quien tiene el turno | "Premio", "cobro": no se ganó nada, le tocó |
| **Deducción de entrega** | Lo que se descuenta antes de entregar (deuda, comisión, cobertura) | Comisión, que es solo una de las deducciones |

## La billetera

| Término | Qué es | No confundir con |
| --- | --- | --- |
| **Cuenta de billetera** | Dónde vive el saldo de un titular | Cuenta contable, que es del plan de cuentas |
| **Saldo disponible / retenido / total** | Tres números distintos. Retener no es debitar | Entre sí. Es el error más caro del módulo |
| **Transacción** | El hecho económico completo | Movimiento: una transacción tiene dos o más movimientos |
| **Movimiento** | Una línea del libro, con sentido y monto positivo | Transacción |
| **Custodia** | El dinero real en el banco que respalda el saldo emitido | Saldo: uno es la promesa, el otro el respaldo |
| **Encaje** | Que la custodia cubra el 100% de lo emitido | "Reserva" |
| **Recarga / Retiro** | Entrada y salida de dinero del sistema | Aporte / Entrega, que son internos al grupo |
| **Reverso** | Movimiento inverso que corrige | "Anulación", "borrado": nada se borra |

## Cumplimiento

| Término | Qué es |
| --- | --- |
| **Debida diligencia** | Conocer al cliente. Simplificada, estándar o reforzada |
| **Umbral** | El monto a partir del cual nace una obligación de registro |
| **Formulario por umbral** (PCC-01, ROG) | Declaración de origen y destino. **Se le pide al cliente** |
| **Reporte de operación sospechosa** | Decisión del oficial de cumplimiento. **Jamás se le comunica al cliente** |
| **Alerta / Caso** | Lo que saltó / el expediente que lo investiga |
| **Observación** | Lo que el supervisor devuelve sobre un reporte |
| **Hallazgo** | Lo que encuentra auditoría, interna o externa |

## Pares que se confunden todo el tiempo

| A | B | Diferencia |
| --- | --- | --- |
| Cotizar | Cobrar | Cotizar es decir cuánto sería; cobrar es moverlo |
| Devengar | Cobrar | Devengar es ganar el ingreso; cobrar es recibirlo |
| Retener | Debitar | Retener mueve de disponible a retenido; el total no cambia |
| Mora | Incumplimiento | La mora es el atraso; el incumplimiento es la declaración formal |
| Sanción | Cobranza | Una castiga; la otra recupera |
| Cobertura | Devolución | La cobertura la pone el fondo de garantía; la devolución la ponemos nosotros |
| Bloquear | Suspender | Bloquear es sobre el saldo; suspender es sobre el servicio |
| Verificar | Validar | Verificar es contra la realidad (identidad, banco); validar es contra el esquema |

## Cómo se nombran las cosas

| Elemento | Regla | Ejemplo |
| --- | --- | --- |
| Tabla | Sustantivo singular, `snake_case`, sin acentos | `obligacion_aporte` |
| Columna de fecha | `<hecho>_en` para instante, `fecha_<hecho>` para día | `acreditado_en`, `fecha_ingreso` |
| Columna de plazo | `plazo_<qué>` o `vence_en`, siempre persistida | `plazo_respuesta` |
| Booleano | Afirmativo, nunca negado | `es_obligatorio`, no `no_suprimible` |
| Función de base | `fn_<dominio>_<accion>` | `fn_bil_recalcular_saldos` |
| Caso de uso | `CU<NN><VerboInfinitivo>` | `CU21CobrarAporte` |
| Evento de dominio | `<sustantivo>.<participio>` | `pago.acreditado`, `sorteo.revelado` |
| Endpoint | Sustantivo plural en la ruta, verbo en el método | `POST /aportes` |

## Palabras prohibidas

| No usar | Usar | Por qué |
| --- | --- | --- |
| `data`, `info`, `datos` a secas | El sustantivo real | No dice nada |
| `manager`, `helper`, `utils`, `common` | El nombre de la pieza | Son átomos sin dueño (`arquitectura-atomica`) |
| `process`, `handle`, `procesar` | El verbo del dominio | Oculta qué hace |
| "premio", "ganador", "sorteo de premios" | "entrega", "turno" | Sugiere juego de azar, que es otra figura regulatoria |
| Mezcla inglés/español (`getUsuario`) | Español consistente | Dos idiomas, dos vocabularios, cero acuerdos |

## Ver también

`boveda-modelo` · `codigo-limpio` · `caso-de-uso` · `disenar-frontend` ·
`docs/Index.md` · `docs/entidades/README.md`
