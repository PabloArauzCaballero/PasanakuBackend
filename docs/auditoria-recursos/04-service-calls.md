# Inventario de llamadas síncronas entre servicios

Captura estática en Backend `main` `4e7bb51f47a948f38adc2598452f24492f4c1b07` (2026-10-07). Las relaciones salen de `@Value` `aportaya.servicios.*`, los adaptadores `RestClient` y las variables `URL_*`; no se midió tráfico en vivo.

| Origen | Destino | Tipo | Tiempo máximo visible | Manejo de falla / necesidad inmediata |
|---|---|---|---:|---|
| grupos | organizador | HTTP síncrono | 1 s conexión + 3 s lectura | `ClienteDeServicio` devuelve vacío ante caída/timeout; el caso de uso decide |
| grupos | tarifas | HTTP síncrono | 1 s + 3 s | Igual; consulta de dominio |
| grupos | cumplimiento | HTTP síncrono | 1 s + 3 s | Igual |
| grupos | aportes | HTTP síncrono | 1 s + 3 s | Igual |
| grupos | transparencia | HTTP síncrono | 1 s + 3 s | Igual |
| grupos | garantia | HTTP síncrono | 1 s + 3 s | Igual |
| grupos | notificaciones | HTTP síncrono | 1 s + 3 s | Igual; candidato a evento si no necesita respuesta para confirmar el caso de uso |
| grupos | identidad | HTTP síncrono | 1 s + 3 s | Igual |
| nucleo-financiero | tarifas | HTTP síncrono | 1 s + 3 s | Retry/circuit breaker ya existe; tarifa desconocida o falla impide completar el movimiento |
| nucleo-financiero | aportes | HTTP síncrono | 1 s + 3 s | `ClienteDeServicio`; hechos para operación financiera |
| nucleo-financiero | grupos | HTTP síncrono | 1 s + 3 s | `ClienteDeServicio`; hechos para operación financiera |
| entregas | identidad | HTTP síncrono | 1 s + 3 s | `ClienteDeServicio`; datos del titular |
| transparencia | grupos | HTTP síncrono | 1 s + 3 s | `ClienteDeServicio`; lectura del paquete/grupo |
| gateway | microservicios por ruta | HTTP síncrono | conexión 2 s, respuesta 10 s | Configurado en Spring Cloud Gateway |

## Controles ya presentes

`plataforma/comun-web/.../ConfiguracionDeClientes.java` aplica timeouts comunes de 1 s para conexión y 3 s para lectura. El cliente común limita las consultas y entrega el token JWT del usuario al servicio destino. `nucleo-financiero` protege el cliente de tarifas con retry y circuit breaker; el fallback no autoriza un movimiento sin cotización.

## Decisión

No se migra una llamada financiera a eventual consistency como parte de esta optimización. Notificaciones aparece como primer candidato de evento, pero debe revisarse el contrato de cada caso de uso y su outbox antes de cambiar el momento de confirmación o la semántica de error. No se encontró evidencia para convertir los demás flujos síncronos de forma segura en esta auditoría estática.
