# Defines obligatorios de `apps/movil`, por plataforma y modo

`--dart-define` en tiempo de compilación — nunca un valor leído en tiempo de
ejecución, porque `String.fromEnvironment` es un const de compilación (H3.S3/H5.S3).
Sin `API`, `validarGateway` rechaza con motivo `vacia`; sin `HOSTS_PERMITIDOS` en
release, rechaza con `host-ajeno` (ninguna URL externa puede ser "propia" sin lista).

| Plataforma | Modo | `API` | `HOSTS_PERMITIDOS` | Notas |
|---|---|---|---|---|
| Android (emulador) | debug | `http://10.0.2.2:4010/api/v1` | *(vacío, no hace falta)* | `10.0.2.2` es el alias fijo del emulador de Android a la máquina anfitriona — así llega a Prism corriendo en la máquina de desarrollo. |
| Android / iOS (dispositivo físico) | debug | `http://<ip-de-la-red-local>:4010/api/v1` | *(vacío)* | La IP de la red local de la máquina que corre Prism; cambia según la red Wi-Fi. |
| Android / iOS | release | `https://api.aportaya.bo/api/v1` | `api.aportaya.bo` | Sin los dos, el build no debe salir a producción: `validarGateway(..., release: true)` rechaza. |
| Web (si aplica, Flutter Web) | debug | `http://localhost:4010/api/v1` | *(vacío)* | `localhost` se admite explícitamente en modo no-release. |
| CI / verificación de build | release, sin defines | *(ninguno)* | *(ninguno)* | Debe rechazar (`vacia`) — es el caso que prueba que "sin defines no hay release". |
| CI / verificación de build | release, host fuera de lista | `https://otra-cosa.example.com/api/v1` | `api.aportaya.bo` | Debe rechazar (`host-ajeno`) — el caso que prueba que la lista compilada, no una URL cualquiera con TLS, decide. |

## Quién carga esto (Q-J4)

Infra (Leo) carga `API` y `HOSTS_PERMITIDOS` en el pipeline de release — este carril
entrega la tabla y el comportamiento fail-fast (`validarGateway`,
`dominio/cliente.dart`), no el pipeline.

## Comando de verificación (H3.S3.M3)

```bash
# 1. Sin defines: debe fallar el análisis en tiempo de ejecución (resultadoGateway.valida == false),
#    no el build en sí (Flutter no puede fallar el build por un dart-define ausente sin un
#    check explícito de compilación) — se verifica con el widget test de arriba, que corre
#    exactamente en esa condición (`flutter test` no pasa defines).
flutter test test/unidad/configuracion_test.dart test/widget/configuracion_invalida_test.dart

# 2. Con los dos defines correctos: build de release sale con éxito.
flutter build apk --release \
  --dart-define=API=https://api.aportaya.bo/api/v1 \
  --dart-define=HOSTS_PERMITIDOS=api.aportaya.bo

# 3. Con la URL fuera de la lista compilada: el build en sí no falla (Flutter no valida esto
#    en tiempo de compilación), pero la app arranca en la pantalla de bloqueo — no hay
#    manera de que "compile" signifique "server real" sin un paso explícito de CI que
#    corra la app y lea `resultadoGateway.valida`, que no existe todavía (brecha para
#    Leo/infra: agregar ese chequeo al pipeline de release).
```

**Hallazgo (no se resuelve acá, es de Leo/infra):** Flutter no tiene un mecanismo
nativo para que `flutter build` falle por un `--dart-define` ausente o inválido — el
build compila igual, y es la app la que se bloquea al abrir. Si se quiere que el
*build* en sí falle (no solo que la app recién instalada muestre el bloqueo), hace
falta un paso de CI que corra `validarGateway` contra los defines antes de invocar
`flutter build`, o un test de integración que lo verifique post-build. Se documenta
como brecha, no se inventa ese paso de CI acá (fuera de mi alcance: `.github/
workflows/**` es de Leo).
