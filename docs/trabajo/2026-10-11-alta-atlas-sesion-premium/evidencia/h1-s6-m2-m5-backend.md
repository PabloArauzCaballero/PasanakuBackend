# H1.S6.M2–M5 · backend de sesión

## Implementado / cubierto en código

- `SesionesController` emite refresh para ANDROID e IOS en `tokenRefresco`, sin cookie; WEB conserva la cookie HttpOnly y no incluye el refresh en el JSON. Las pruebas web cubren ambas plataformas móviles, la cookie WEB y que el ingreso sin segundo factor no emite credenciales.
- `SesionController` admite POST de renovación sin `Content-Type` para el flujo cookie, además de JSON para móvil. La cookie prevalece si ambas credenciales llegan; el refresh móvil rotado vuelve en el cuerpo. Las pruebas web existentes cubren cookie-only, credencial ausente y rotación móvil.
- `CU04RenovacionTest` cubre rotación, replay fuera de gracia con revocación de familia, replay dentro de gracia sin revocación, expiración, cierre, titular incorrecto e idempotencia (la marca de revocación conserva su primer instante).
- `DELETE /sesion` obtiene titular e ID de sesión del bearer, devuelve 204 y declara autorización para PARTICIPANTE y BACKOFFICE. `CerrarSesion` revoca la fila coincidente con ambos IDs y los refresh emitidos de esa familia; si la petición trae cookie web, también la borra en la respuesta.
- `application.yml` configura `vigencia-sesion: PT8H`. La prueba web verifica que ese Duration llega a `CU04Autenticar`; la prueba de renovación sitúa la sesión a 8 h + 1 s y verifica rechazo.

## Cambios de pruebas en esta pasada

- `servicios/identidad/src/test/java/bo/aportaya/identidad/web/SesionesControllerWebTest.java`: test de ingreso exitoso con stub de refresh, cobertura ANDROID/IOS y comprobación de la configuración PT8H que recibe CU-04.
- `servicios/identidad/src/test/java/bo/aportaya/identidad/CU04RenovacionTest.java`: caso de expiración expresado como 8 h + 1 s e idempotencia mediante igualdad del instante de revocación.
- `servicios/identidad/src/test/java/bo/aportaya/identidad/web/SesionControllerWebTest.java`: escenarios HTTP de cookie sin body, body móvil, replay/rechazo y logout con JWT; se añadieron verificaciones para rol BACKOFFICE.

## Verificación pendiente / límites de evidencia

- No se ejecutaron pruebas ni tareas Gradle en esta pasada, por la restricción de RAM/infraestructura indicada. Por tanto, no se confirma compilación, arranque del contexto, resolución final de mappings ni comportamiento real de PostgreSQL/RLS/triggers.
- `git diff --check` se usó solo como revisión estática de whitespace; no sustituye compilación ni pruebas.
- Ejecutar en un entorno con PostgreSQL de pruebas y jOOQ disponible: `:servicios:identidad:test --tests '*SesionesControllerWebTest' --tests '*SesionControllerWebTest' --tests '*CU04RenovacionTest'`. Mantener H1.S6.M2–M5 como A MEDIAS hasta obtener esos resultados.
- Riesgo residual conocido: los JWT de acceso firmados son stateless y conservan su ventana de validez de 15 minutos aunque cierre la fila de sesión; el cierre sí impide renovar el refresh.
