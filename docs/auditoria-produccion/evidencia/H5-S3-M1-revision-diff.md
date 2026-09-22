# H5.S3.M1 — Revisión independiente del diff

Agente de solo lectura (Explore), sin editar, sin ser el mismo que escribió
el código (regla 70.4.8). Diff revisado: `git diff origin/dev...HEAD` en
`pablo/feature/carril-PR5-ci-operacion` (112 archivos).

## Hallazgo real, corregido

**NGINX `/actuator/` no bloqueaba la ruta sin barra final `/actuator`** —
`location /actuator/ { return 404; }` es un match de prefijo sobre el string
`/actuator/` (con barra); una petición a `/actuator` (sin barra) caía en
`location /` y llegaba al documento de descubrimiento del gateway (nombres
de endpoints habilitados — no métricas ni env, pero tampoco es de nadie de
afuera). Reproducido con `curl` real antes del fix (`200`), corregido con
`location ^~ /actuator { return 404; }` (prefijo, sin barra), verificado
`404` en los tres casos: `/actuator`, `/actuator/`, `/actuator/prometheus`.

## Segundo hallazgo — intentar cerrarlo encontró una regresión real

La guarda de CORS fail-closed (`ConfiguracionCors`) solo tenía verificación
manual (`evidencia/H3-S2-cors.txt`). Al escribir un test para cerrar ese
hueco y correr la suite **completa** de `integrationTest` (no solo el test
nuevo en aislamiento), los 3 tests de `ArranqueRateLimitGatewayTest` —que ya
estaban en verde desde H3.S1— **rompieron**: `ConfiguracionCors` trataba
"ningún perfil activo" (el caso de esa prueba, que nunca fija
`spring.profiles.active`) igual que "perfil de producción", y exigía
`APORTAYA_CORS_ORIGENES` para poder arrancar. Es exactamente lo que la regla
30.4 describe — un cambio posterior invalida el peldaño `VERIFIED` del área
tocada, y hay que re-verificar, no asumir que sigue en pie.

**Corregido:** la condición pasó de "todo lo que no sea local/test" (lista
de excluidos, atrapaba también "sin perfil") a "explícitamente staging o
production" (lista de permitidos). Verificado en los tres frentes: `docker
run` con `production` sin origen sigue cortando el arranque; con
`production` y un origen real arranca igual que antes; y la suite completa
de `:plataforma:gateway:integrationTest` (los 3 tests de rate limiting)
vuelve a pasar junta. El test nuevo específico de CORS se **descartó**
después (ver abajo) por un problema de aislamiento entre pruebas distinto a
este, no porque la guarda estuviera mal — la guarda ya quedó corregida y
verificada por otras vías.

## Deuda declarada: sin test automatizado para la guarda de CORS

El test que se escribió para cerrar el hueco (`ArranqueCorsGatewayTest`,
caso "arranca normal con un origen real") dejaba el proceso escuchando en el
puerto `8080` real en vez del efímero que pedía `server.port=0`, y chocaba
con los tests de rate limiting corridos después en la misma JVM
(`PortInUseException`). El caso crítico ("no arranca sin origen") sí corría
limpio solo, pero la clase junto a los demás tests seguía interfiriendo. Se
descartó el archivo entero en vez de dejar una prueba a medias rompiendo la
suite (regla 80.5.1: nada de tests que tapan a otros para poder cerrar).
Queda como deuda para el carril: un test de arranque de `ConfiguracionCors`
en un módulo/JVM aislado del resto.

## Confirmado correcto (no era lo que parecía a primera vista)

- El filtro `denegarSiRedisNoResponde` (fail-closed real, orden de filtros
  correcto, con test Testcontainers real) — verificado, no fabricado.
- `.github/.trivyignore` no excepciona nada: documenta los CVE reales sin
  parche como hallazgo abierto, no los tapa.
- CODEOWNERS con un solo dueño provisional — declarado como tal, no un
  descuido silencioso.
- `https://app.aportaya.example` en la documentación usa el TLD reservado
  RFC 2606, no un dominio inventado presentado como real.

## Sin hallazgos

Código de depuración, secretos nuevos hardcodeados, `TODO`/`FIXME`
sustituyendo implementación, integraciones simuladas presentadas como
reales, SQL sin parametrizar, tests deshabilitados o debilitados, imports
muertos.
