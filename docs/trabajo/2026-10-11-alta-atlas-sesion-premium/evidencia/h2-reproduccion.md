# H2.S1 — reproducción de errores de alta/ingreso (2026-10-12)

Resultado: no reproducido. No se obtuvo una respuesta de aplicación/backend que permita observar un código HTTP de negocio. Esto es una limitación del entorno, no evidencia de que los errores de producto no existan.

| Superficie/acción | Texto visto | Código real | HTTP | Clasificación | Hipótesis |
|---|---|---|---:|---|---|
| Arranque de la reproducción | No se pudo iniciar el recorrido funcional: no consta la URL del backend de Pablo; el fallback local no está escuchando y no hay dispositivo móvil. | — | — | `ENVIRONMENT` | Nueva: falta URL/backend y dispositivo para reproducir; no asignable honestamente a E1–E7. |

Comprobaciones realizadas:

- `rg -n "API|Prism|4010|backend|gateway" .../NOTAS.md apps/movil/scripts packages/simulado` no encontró una URL de backend de Pablo; `packages/simulado` confirma `:4010` como mock local.
- `Get-NetTCPConnection -State Listen ...` filtrado a puertos 4010, 5433 y 4011 no devolvió listeners.
- `Test-Path packages/simulado/node_modules` y `Test-Path node_modules` devolvieron `False`; no se arrancó una instalación de dependencias porque no están disponibles en el checkout.
- `flutter devices` devolvió únicamente `Windows (desktop)` y `Chrome (web)`; no hay Android/iOS conectado ni emulador iniciado.

## H2.S1.M2 — auditoría de clasificación

La tabla contiene 1 fila observable y las 1/1 filas están clasificadas (`ENVIRONMENT`) y ligadas a una hipótesis nueva, no atribuible a E1–E7 sin evidencia. Esto satisface el CA literal de M2 para los registros actuales. No convierte la falta de entorno en PASS ni afirma que se hayan clasificado errores funcionales que no se pudieron observar.

No se ejecutó el alta o login contra un backend distinto al indicado por el plan ni se inventaron textos/códigos. No se generaron capturas porque no apareció ningún error funcional. El 2026-10-14 se pidió a Pablo la URL base de ensayo o una captura redactada con pantalla, acción, texto, código y HTTP en [el comentario de PR #61](https://github.com/PabloArauzCaballero/PasanakuBackend/pull/61#issuecomment-6177599498), sin solicitar credenciales ni datos personales. Esto satisface H2.S1.M3 (registrar «no reproducido» y pedir captura); H2.S1.M1 sigue A MEDIAS hasta poder correr los recorridos contra el backend indicado o el fallback local.

### Verificación adicional de entorno (2026-10-14)

Se comprobó otra ruta para ejecutar identidad localmente, sin iniciar contenedores ni mostrar valores de secretos:

- PostgreSQL de Windows acepta conexiones en `127.0.0.1:5432`.
- No hay listeners en `127.0.0.1:8085` (identidad), `:9092` (Kafka) ni `:9000` (almacenamiento de archivos).
- Las variables requeridas `BD_URL`, `KAFKA_URL`, `JWKS_URI`, `SEGURIDAD_PIMIENTA`, `ARCHIVOS_URL`, `ARCHIVOS_USUARIO` y `ARCHIVOS_CLAVE` no están configuradas en el entorno de esta sesión.
- El servicio `com.docker.service` está detenido. Se dejó detenido para no exceder el margen de RAM; por tanto, PostgreSQL por sí solo no permite arrancar identidad ni reproducir alta/login.
- Hay AVD Android disponible y ya se verificó una prueba de teclado H3, pero eso no sustituye el backend faltante para H2.

Resultado de esta verificación inicial: el intento de completar H2.S1.M1 no pudo ejecutarse con evidencia funcional. Próximo paso: obtener de Pablo una URL de ensayo sin credenciales o una captura redactada; alternativamente, preparar el stack local cuando Docker esté disponible y se confirme margen suficiente de RAM. En ese momento no se modificó código ni se inició ningún servicio.

### Intento con Docker local (2026-10-14)

Con autorización para usar Docker se inició Docker Desktop y el subconjunto `postgres` + `kafka` de `despliegue/compose/base.yml` (sin gateway). Ambos contenedores llegaron a `healthy`. En el volumen nuevo `aportaya-datos-pg` se ejecutó `psql -v ON_ERROR_STOP=1 -U pasanaku -d pasanaku -q -f /repo/sql/aplicar.sql`; terminó con código cero y quedaron 18 esquemas no-sistema. No se ejecutó el recorrido de alta/login porque falta el almacenamiento exigido por identidad y no hay variables locales configuradas.

MinIO no pudo descargarse: `quay.io/minio/minio:RELEASE.2025-09-07T16-13-09Z` devolvió `401 UNAUTHORIZED`. No se sustituyó la imagen fijada por otra. El máximo observado fue 14,59 GB usados de 15,32 GB; se detuvieron PostgreSQL y Kafka y se apagó Docker Desktop para liberar RAM y no interferir con un proceso Gradle que estaba solicitando memoria. Tras el apagado: 11,56 GB usados, 3,77 GB libres. Los contenedores quedaron detenidos (no eliminados) y el volumen con el esquema aplicado se conserva.

Retomar este intento: abrir Docker Desktop; verificar RAM; ejecutar `docker compose -f despliegue/compose/base.yml up -d postgres kafka` y comprobar salud; el volumen `aportaya-datos-pg` persiste. Antes de continuar identidad, resolver el acceso de descarga de MinIO y configurar secretos locales de ensayo sin subirlos al repositorio. Docker quedó apagado al terminar esta sesión.

### Continuación local con el margen de RAM ampliado (2026-10-14)

Se reabrió Docker Desktop después de verificar 9,87 GB usados. PostgreSQL y Kafka volvieron a `healthy`; el volumen conservó los 18 esquemas. Se aplicaron las semillas oficiales `sql/60_semillas/sembrar.sql` y `sql/61_dev/sembrar_dev.sql` (ambas terminaron correctamente; hay 14 usuarios seed). La compilación limitada a un worker, `:servicios:identidad:bootJar -x test` con heap Gradle de 768 MiB, terminó `BUILD SUCCESSFUL` en 19 s.

Se arrancó identidad en `127.0.0.1:8085` con perfil `local` y heap Java limitado a 640 MiB; `/actuator/health` respondió `UP` y `/.well-known/jwks.json` publicó una clave. Control API de credencial errónea: `POST /sesiones` directo al servicio (sin el prefijo `/api/v1`, que pertenece al gateway) respondió **HTTP 422**, `AP-CU04-01`; PostgreSQL registró el intento como fallido `CREDENCIAL_INVALIDA`. Se usó el usuario sembrado `USR000090` y una contraseña deliberadamente inválida; esto añade un intento fallido al fixture local. La primera solicitud a `/api/v1/sesiones` fue a la ruta equivocada del servicio y no dejó intento en DB; no se clasifica como error del producto.

Límites: esto verifica una respuesta de identidad en backend local, no el flujo visual de Flutter; no prueba alta, ingreso válido ni caducidad a 15 min y no genera capturas. El servicio de archivos sigue ausente: Quay devolvió `401` para la imagen fijada y Docker Hub respondió que `minio/minio` no existe/no permite pull. No se sustituyó la imagen. En esta medición la RAM usada fue ~13,2/15,32 GB; Docker, PostgreSQL, Kafka e identidad siguen en ejecución al guardar esta evidencia.
