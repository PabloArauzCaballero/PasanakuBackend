# H10.S4.M1 - ampliacion de auditoria estatica (2026-10-13)

Se repitio la revision sobre Dart de produccion y pruebas, el cliente HTTP y el registrador Android generado:

```text
rg -n -i -g '*.dart' 'developer\.log|print\s*\(|debugPrint\s*\(|logger\.|log\.(info|debug|warning|severe|shout|fine|config)' apps/movil/lib apps/movil/test packages/diseno_flutter/lib
rg -n -i -g '*.kt' -g '*.java' 'android\.util\.Log|Log\.(v|d|i|w|e)\s*\(|println\s*\(|System\.out|Timber\.' apps/movil/android
rg -n 'LogInterceptor|PrettyDioLogger|HttpLoggingInterceptor|interceptors\.add|onRequest|onResponse|onError' apps/movil/lib
```

Resultado: en Dart solo aparece el `developer.log` constante documentado en `h10-s4-m1-estatica.md`. El cliente HTTP (`apps/movil/lib/dominio/cliente.dart`) agrega interceptores de sesion/traza y traduccion de errores, pero no un interceptor de logging ni impresion de cuerpos/cabeceras. En Android, las unicas coincidencias son llamadas generadas `Log.e` de `GeneratedPluginRegistrant.java` al fallar el registro de plugins; los mensajes nombran el plugin y adjuntan la excepcion de inicializacion. No se les pasan datos de alta o sesion y ocurren durante el arranque. No se encontraron logs de tokens, contrasenas, codigo CI ni rutas de fotos en el codigo de logging revisado.

Esta ampliacion fortalece solo la evidencia estatica. No sustituye la captura de `flutter logs` durante un recorrido real de alta en Android ni audita internamente los logs propios de cada dependencia/plugin. H10.S4.M1 continua `A MEDIAS` hasta observar el runtime Android.
