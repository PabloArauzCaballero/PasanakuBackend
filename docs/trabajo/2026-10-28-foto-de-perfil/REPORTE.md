# Reporte — Subir la foto de perfil (full-stack) y APK que se actualiza encima

> **AVANCE: 10 / 12 — 83,3 %.** Falta la prueba real con sesión en TEST (subir y ver la foto). El CI de la PR se mergeó con dos checks en rojo que vienen de otro trabajo.

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama(s): `claude/foto-de-perfil-20261028` (#236 dev, #237 test), `claude/apk-firma-estable-20261029` (#247, #248), `claude/apk-firma-apksigner-20261029` (#251 test), `claude/apk-firma-apksigner-dev-20261029` (#253 dev)
- Peldaño de evidencia alcanzado: backend `TESTED` (Postgres real; el almacén es un doble). App `TESTED` (widget tests). TEST desplegado (`RUNS`: el contenedor tiene el código y está healthy). Falta `VERIFIED`: nadie subió una foto contra TEST con una sesión real. APK `VERIFIED` en firma y contenido; no se instaló en un teléfono.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Contrato `PUT/GET /usuarios/yo/foto-de-perfil` + `MiPerfil.tieneFotoDePerfil` | `./gradlew :servicios:identidad:compileJava` | PASS (EXIT=0) |
| H1.S1.M2 | `AmbitoArchivo.PERFIL` (JPEG/PNG, 5 MB) y `DestinoDeObjeto.deFotoDePerfil` | `./gradlew :plataforma:comun-archivos:test` | PASS 7/7 · [evidencia](evidencia/H1.S1.M2-comun-archivos-test.txt) |
| H1.S1.M3 | `CU07FotoDePerfil` + `FotoDePerfilEnLaCuenta` + controlador | identidad `test`/`webTest`/`testBarrido` + `integrationTest --tests CU07*` | PASS 45 / 152 / 3 / 6+6 · [evidencia](evidencia/H1.S1.M3-identidad-suites.txt), [post-merge](evidencia/H1.S1-post-merge.txt) |
| H1.S2.M1 | Proveedores de lectura (404 → sin foto) y subida en la app | `flutter test test/widget/pantalla_perfil_foto_test.dart` | PASS 4/4 |
| H1.S2.M2 | Avatar + «Agregar/Cambiar foto» (cámara o galería) en Perfil | perfil 3 archivos + `flutter analyze` | PASS 10/10; sin issues · [evidencia](evidencia/H1.S2-flutter-perfil-post-merge.txt) |
| H1.S2b.M1 | Spotless + Postman regenerado | `spotlessCheck`; regenerar deja 0 archivos con diff | PASS |
| H1.S2b.M2 | Deriva de DDL ajena (`disponibilidad_servicio_dia`) | `generar_ddl.py` deja 0 archivos con diff | PASS |
| H1.S2b.M4 | `dart format` (mi test y `pantalla_del_grupo.dart`) | `yarn lint` de movil | PASS «TODO OK» |
| H1.S3.M1 | PRs mergeadas y TEST corriendo el código | `docker exec identidad… find … CU07FotoDePerfil.class` | PASS: 1 clase, contenedor `20261029T123741` healthy |
| H2.S1.M1 | APK de prueba firmado siempre con la misma clave | `apksigner verify --print-certs aportaya-test-18.apk` | PASS `f8f513e5…` (= keystore) · versionCode 18 · incluye 509dad69 |

## A medias
### H1.S3.M2 — Prueba real en TEST
- Qué anda: sin sesión, PUT y GET responden 401 a través del gateway público ([evidencia](evidencia/H1.S3.M2-test-sin-sesion.txt)). El contenedor de identidad tiene las clases nuevas.
- Qué no anda: no está probado. El 401 también sale para una ruta que no existe, así que no demuestra que la ruta nueva exista.
- Qué falta exactamente: con el APK 18, entrar a Perfil → «Agregar foto» → elegir una foto → ver el avatar. Después revisar que `identidad.usuario.url_avatar` empiece con `s3://perfil/<id>/foto-` y que el objeto exista en MinIO.
- Dónde quedó: código en `test` (`e475be32` y siguientes) y `dev`; despliegue en TEST con `ea6dd5e`.

### H1.S2b.M3 — CI de la PR
- Qué anda: Formato, Contratos, Esquema/semillas/humo, Dependencias y Base efímera pasaron en la 2.ª corrida.
- Qué no anda: `verificar_boveda.py` (ADR-053 sin índice ni secciones, índice de skills, «50 ADR» en el plan maestro) y `:servicios:auditoria:testBarrido` (`SaludDeServicios.java:42,55`, `DisponibilidadDelServicio.java:18` con cifras literales, `IndicadoresController.java` 309 líneas). Las dos fallas vienen del trabajo de SLO (c51a17cf y siguientes).
- Qué falta exactamente: que el dueño del SLO las arregle. La corrida final sobre `b50dfe3f` no se esperó.
- Dónde quedó: mergeado por pedido explícito de Pablo («de una vez»).

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| ninguna | — | — |

## Evidencia
```text
$ apksigner verify --print-certs aportaya-test-18.apk
Signer #1 certificate SHA-256 digest: f8f513e54ae2a4a8079360878abb082d064e061574dd3f8af0e6eeba63520ad2
$ keytool -list -v -keystore ~/.android/aportaya-test-debug.keystore   (SHA256)
F8:F5:13:E5:4A:E2:A4:A8:07:93:60:87:8A:BB:08:2D:06:4E:06:15:74:DD:3F:8A:F0:E6:EE:BA:63:52:0A:D2
# apk-prueba-12 a55de554…, apk-prueba-13 b4a20ff6…, apk-prueba-17 cc59616a… (antes del refirmado)

$ docker exec identidad-…-20261029T123741 find /app/BOOT-INF/classes -name CU07FotoDePerfil.class | wc -l
1
```
Índice de `evidencia/`: suites de identidad, flutter (perfil, widget completo, analyze), prueba sin sesión en TEST.

## No cubierto
- Subida real contra MinIO: en los tests el almacén es un doble en memoria (`CU07FotoDePerfilTest.AlmacenEnMemoria`).
- La RLS de `usuario` con el rol de la aplicación: el test de integración se conecta como superusuario.
- La app en un teléfono y en web: no se instaló el APK 18; no hay capturas por viewport ni en tema oscuro (regla 95.7). Peldaño visual: `VERIFIED_FUNCTIONAL_ONLY` como máximo.
- `integrationTest` completo de identidad: 201 tests, 1 falla `CU04Test` (criterio 3). Pasa aislado en la rama y en `2189e607`. No se corrió la suite completa en la base para probar que sea preexistente.
- 4 widget tests (goldens de pautas, `paso_capturas`) fallan igual en `2189e607` → preexistentes.

## Desvíos del plan
- `origin/dev` avanzó durante el trabajo (rediseño bancario #229–#232). Se integró y hubo que ajustar el test de cerrar sesión para que se desplace hasta la fila (`ensureVisible`).
- Se agregaron H1.S2b (CI) y H2 (firma del APK) a pedido.
- `UsuariosAutorizacionNegativaWebTest.miPerfilSaleDeLaSesion` se movió a `UsuariosFotoDePerfilWebTest` por el límite de 300 líneas, sin cambiar sus aserciones.
- El primer arreglo de la firma (keystore en `~/.android/debug.keystore`, #247/#248) no alcanzó. Se reemplazó por el refirmado con apksigner (#251/#253).

## Riesgos residuales
- Cambiar la foto agrega un objeto nuevo y no borra el anterior (ADR-034). No hay barrido de objetos viejos de perfil.
- `pantalla_perfil` hace un GET extra a `/usuarios/yo/perfil` para las iniciales (`nombreDeLaCuentaProvider`).
- La keystore de depuración vive en el secreto `APK_DEBUG_KEYSTORE` y en `~/.android/aportaya-test-debug.keystore` de la Mac de Pablo. Si se pierde, el próximo APK vuelve a obligar a desinstalar.

## Decisiones y ambigüedades
- «Apartir» se leyó como «a partir de ahora» y el cliente es la app móvil. Confirmar con Pablo si también la quiere en web o backoffice.
- `PUT` sin `Idempotency-Key`: es un reemplazo, y repetirlo deja la misma foto vigente.
- Merge con dos checks rojos ajenos: lo decidió Pablo el 2026-10-29.
- Se incluyeron dos correcciones de generadores ajenas (DDL y `dart format`) porque bloqueaban checks de esta PR. Solo comentarios y espacios.
