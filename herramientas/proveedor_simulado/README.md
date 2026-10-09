# Proveedor externo simulado

Solo desarrollo local; este servidor no mantiene saldos de usuarios de Pasanaku ni reemplaza sus servicios Java. Conserva operaciones ficticias en SQLite y firma sus respuestas con HMAC-SHA256. La interfaz es un contrato interno de simulación, no la API de un banco ni de ningún proveedor real: cuando exista uno elegido, se escribe su adaptador contra su documentación oficial.

## Arranque

Desde la raíz del backend, configurar `PASANAKU_AMBIENTE=simulado`, `PROVEEDOR_API_KEY`, `PROVEEDOR_CONTROL_KEY` y `PROVEEDOR_FIRMA_SECRETO`. Usar tres secretos distintos generados localmente de al menos 32 bytes; no guardarlos en Git ni imprimirlos en reportes.

```powershell
python -m herramientas.proveedor_simulado.servidor --database .local/proveedor.sqlite --port 4020
```

El proceso escucha exclusivamente en loopback. `Ctrl+C` lo detiene. Reiniciar con el mismo archivo conserva los estados y los fallos armados. Para un escenario nuevo utilizar otro archivo; no existe endpoint de borrado de evidencia. Sin `PASANAKU_AMBIENTE=simulado` el proceso se niega a arrancar.

## Configuración del backend (`nucleo-financiero`)

| Propiedad | Valor para desarrollo | Nota |
| --- | --- | --- |
| `aportaya.ambiente` | `simulado` | Cualquier otro ambiente rechaza el modo simulado al arrancar. |
| `aportaya.proveedor.modo` | `simulado` | Por omisión es `deshabilitado`: no confirma fondos y el servicio levanta igual. Cualquier otro valor se rechaza. |
| `aportaya.proveedor.url` | `http://127.0.0.1:4020` | Solo `http` hacia `127.0.0.1` o `localhost`, sin usuario, query ni fragmento. |
| `aportaya.proveedor.api-key` | igual a `PROVEEDOR_API_KEY` | Al menos 32 caracteres. |
| `aportaya.proveedor.firma-secreto` | igual a `PROVEEDOR_FIRMA_SECRETO` | Al menos 32 bytes. |
| `aportaya.proveedor.timeout` | `PT5S` | Obligatorio positivo: no hay llamada sin tiempo máximo. |

La clave de control (`PROVEEDOR_CONTROL_KEY`) **no** se le da al backend: solo la usa quien opera el escenario. La clave operativa no puede invocar los controles.

## Contrato

- `GET /health`: identifica explícitamente simulación.
- `POST /v1/operaciones`: Bearer con clave operativa; cabecera `Idempotency-Key` UUID. Cuerpo: `referencia` UUID, `tipo`, `monto` decimal textual positivo, `moneda=BOB`, `escenario` opcional y `condiciones` opcionales. La referencia permanece estable en los reintentos; la misma clave con otro contenido es `409 IDEMPOTENCIA_CON_CONTENIDO_DISTINTO`.
- `GET /v1/operaciones/{referencia}`: consulta autenticada, devuelve el recibo firmado (`404` si no la conoce).
- `POST /control/resolver`: clave de control distinta; cuerpo `referencia`, `estado=CONFIRMADO|RECHAZADO`. Un estado terminal no retrocede.
- `POST /control/reloj`: clave de control; avanza `segundos` enteros sin retroceder.
- `POST /control/fallo`: clave de control; cuerpo `modo` y `cantidad` (1 a 10). Arma cuántas creaciones siguientes sufren un corte de transporte. Sobrevive al reinicio.
- `GET /control/estado`: clave de control; devuelve `{operaciones, confirmadas, pendientes, rechazadas}` para afirmar «una sola liquidación» sin leer el archivo.

Sobre de respuesta: `payload` contiene Base64 de bytes UTF-8 originales; `firma` es HMAC-SHA256 hexadecimal de esos bytes. Verificar la firma antes de interpretar el contenido. El recibo vincula referencia, importe, moneda, tipo, estado, transacción externa, fecha de liquidación y emisión. La emisión usa tiempo real para control de frescura; el reloj de negocio permite simular vencimientos.

### Escenarios y fallos

Escenarios por operación (campo `escenario`): `PENDIENTE` por defecto, `CONFIRMADO`, `RECHAZADO` y `RESPUESTA_PERDIDA` (liquida y corta la respuesta de la primera creación; consultar o reintentar devuelve la misma transacción).

Fallos armados por el control (`modo` de `/control/fallo`), independientes de lo que pida la operación y sin alterar su huella de idempotencia:

| Modo | Qué pasa con la creación | Estado que queda |
| --- | --- | --- |
| `PERDER_ANTES` | Corta la conexión sin registrar nada. | Ninguna operación; reenviar la crea una sola vez. |
| `PERDER_DESPUES` | Registra la operación y corta sin responder. | Operación según su escenario; reenviar devuelve la misma. |
| `LIQUIDAR_Y_PERDER` | Registra la operación **ya liquidada** y corta sin responder. | `CONFIRMADO`; es el timeout después de liquidar, indistinguible desde afuera de `PERDER_ANTES`. |

Tipos admitidos por el proveedor: RECARGA, RETIRO, SUSCRIPCION_DPF, SUSCRIPCION_FONDO y RESCATE. Admitir un tipo en este simulador no implica que exista su flujo de negocio integrado en la aplicación: hoy están cableados RECARGA y RETIRO (`nucleo-financiero`); los demás son solo vocabulario.

No hay webhook de entrada propio: el backend se entera de la confirmación **consultando** la referencia (tirar, no empujar). Una «doble confirmación» es, por eso, dos consultas que ven el mismo recibo; el backend acredita una sola vez.

### Defensas del borde HTTP

Toda petición hostil recibe una respuesta controlada y firmada de error; nunca un hilo caído ni un `TypeError` sin respuesta:

- Cuerpo mayor de 16 KiB: `413 CUERPO_DEMASIADO_GRANDE`. Hasta 1 MiB se descarta en trozos antes de contestar (cerrar con bytes sin leer resetea la conexión y el cliente pierde la respuesta); lo que declare más no se lee.
- `Content-Length` ausente, no numérico, cero o negativo: `400`. `Content-Type` distinto de JSON: `415`.
- JSON con claves duplicadas, `NaN`/`Infinity`, anidado en exceso o que no sea un objeto: `400`.
- `tipo`, `escenario`, `moneda`, `monto` o `condiciones` con tipo equivocado: `400`.
- Credencial incorrecta: `401`, sin evaluar el cuerpo. Cada conexión tiene tiempo máximo (10 s) para que un cliente lento no retenga un hilo.
- Cualquier otra falla interna: `500 ERROR_INTERNO_CONTROLADO` o `503` si SQLite no responde, sin reproducir contenido de la petición.

## Verificación

Pruebas del proveedor (rápidas, sin Docker; usan un servidor HTTP real en loopback y un SQLite temporal):

```powershell
python -m unittest herramientas.proveedor_simulado.test_proveedor herramientas.proveedor_simulado.test_servidor_adverso -v
```

Pruebas del backend contra el **proceso Python real** (necesitan `python` o `python3` en el PATH, o `PYTHON_EJECUTABLE`; si no está, fallan con ese motivo y no se saltean). Levantan el servidor con secretos al azar, matan el proceso, cortan conexiones y afirman que el proveedor termina con **una** operación y el libro con **un** movimiento:

```powershell
.\gradlew.bat :servicios:nucleo-financiero:integrationTest --tests '*CU10ServidorReal*' --tests '*CU11ServidorReal*'
```

Qué NO cubren: un proveedor real (no hay contrato certificado), la aplicación completa con gateway y clientes, ni la concurrencia entre varios procesos del backend.
