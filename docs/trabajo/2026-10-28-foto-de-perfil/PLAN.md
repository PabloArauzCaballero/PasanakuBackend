# Plan — Subir la foto de perfil (full-stack)

- Fecha: 2026-10-28 · Repos afectados: monorepo Pasanaku (`servicios/identidad`, `plataforma/comun-archivos`, `apps/movil`) · Predecesor: ninguno
- Resultado observable: desde la pantalla Perfil de la app, la persona elige o saca una foto, la sube y la ve como su avatar; al volver a entrar sigue ahí (persistida en el servidor, no en el teléfono).
- Kill-test: subir una foto, cerrar y abrir la app, y que el perfil muestre las iniciales en vez de la foto; o que `GET /usuarios/yo/foto-de-perfil` responda 404 después de un `PUT` exitoso.
- Pedido de Pablo 2026-10-28: «necesito que se pueda subir la foto de perfil, corrección fullstack».

## Hechos (descubrimiento)
- Columna `identidad.usuario.url_avatar VARCHAR(255) NULL` ya existe (`sql/10_tablas/01_identidad_usuarios/usuario.sql:17`); ADR-034:23 la declara «Foto de perfil» y exige guardar **clave de objeto**, nunca URL pública. Ningún código Java la lee ni escribe → **no hay cambio de esquema**.
- Almacén: `AlmacenDeArchivos.guardar/leer` (`plataforma/comun-archivos`), tipo por bytes (`TipoPorContenido`), lista blanca por `AmbitoArchivo` (hoy IDENTIDAD/COMPROBANTE/REPORTE).
- Precedente de subida: `POST /usuarios/{id}/documentos` multipart (`identidad.yaml:382`, `ExpedienteController.java:84`, `CU02GuardarFotoDelExpediente`: primero el archivo, después la fila).
- Precedente de lectura: proxy de bytes `verContenidoDeFoto` (`VerificacionesController.java:80`) porque MinIO es interno.
- Perfil: `GET /usuarios/yo/perfil` (`identidad.yaml:734`, `ConsultarMiPerfil`), `MiPerfil` con `additionalProperties: false`.
- RLS `pol_usuario_titular` permite a la sesión propia actualizar su fila; `svc_identidad` tiene UPDATE en TEST.
- App: `pantalla_perfil.dart` no muestra avatar; `Avatar(nombre, tamano, imagen: ImageProvider?)` en `packages/diseno_flutter/lib/atomos/avatar.dart`; `image_picker` ya en `apps/movil/pubspec.yaml`; las llamadas de perfil van con el `dioProvider` crudo.

## Alcance
- IN: contrato OpenAPI de identidad (2 operaciones nuevas + campo opcional `tieneFotoDePerfil` en `MiPerfil`); `AmbitoArchivo.PERFIL`; `DestinoDeObjeto.deFotoDePerfil`; casos de uso y controlador; tests unitarios/web; app: avatar + cambiar foto en Perfil; despliegue a TEST y prueba real.
- OUT: mostrar la foto de OTRAS personas (integrantes, organizador, backoffice); recorte/edición de imagen; borrado de la foto; barrido de objetos viejos (los objetos no se sobrescriben, ADR-034).
- Decisiones (patrón existente): `PUT` (reemplazo idempotente por naturaleza: repetirlo deja la misma foto vigente) → no exige `Idempotency-Key`; lectura por proxy con `Cache-Control: private, no-store`; 5 MB, JPEG/PNG.
- Ambigüedad: «Apartir» se lee como «a partir de ahora». Se toma la app móvil como cliente. Confirmar con Pablo si también la quiere en la web o el backoffice.

## H1 — La persona sube y ve su foto de perfil
**CA:** Dado una persona con sesión, cuando elige una foto en Perfil, entonces la ve como avatar y sigue ahí al volver a abrir la app.
**DoD:** tests backend + tests de la app en verde; prueba real contra TEST (PUT→GET devuelve los mismos bytes; `url_avatar` con clave `s3://perfil/...`).
**Estado:** TODO

### H1.S1 — Backend
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contrato: `PUT/GET /usuarios/yo/foto-de-perfil` + `MiPerfil.tieneFotoDePerfil` | El código generado compila | `./gradlew :servicios:identidad:compileJava` → BUILD SUCCESSFUL | HECHO (EXIT=0) |
| H1.S1.M2 | `AmbitoArchivo.PERFIL` y `DestinoDeObjeto.deFotoDePerfil` | 5 MB, jpeg/png; destino `<usuarioId>/foto` | `./gradlew :plataforma:comun-archivos:test` verde | HECHO (7/7, evidencia/H1.S1.M2) |
| H1.S1.M3 | Casos de uso subir/ver + controlador + perfil con `tieneFotoDePerfil` | Archivo primero, después UPDATE; 404 contractual sin foto | `:servicios:identidad:test/webTest/testBarrido` + `integrationTest --tests CU07*` verde | HECHO (test 45, webTest 152, barrido 3, CU07 6+6; evidencia/H1.S1.M3) |
### H1.S2 — App
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Proveedor de la foto + subida (dio) | 404 → sin foto; subida invalida la lectura | `flutter test test/widget/pantalla_perfil_foto_test.dart` verde | HECHO (4/4) |
| H1.S2.M2 | Avatar y «Cambiar foto» en Perfil (cargando/error/vacío) | Se ve el avatar y el botón; error accionable | mismo test + `flutter analyze` sin errores nuevos | HECHO (perfil 10/10; analyze sin issues) |
### H1.S3 — Integración real
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | PR a dev y test, despliegue en TEST | Commit desplegado en TEST | PRs mergeadas + healthcheck de identidad | TODO |
| H1.S3.M2 | Prueba real en TEST: PUT, GET, perfil, sin sesión 401, otro tipo 422 | Bytes iguales; `url_avatar` = clave | salida de `curl` en `evidencia/` | TODO |

## Hallazgos de la ejecución
- Las suites de identidad están partidas (`test`, `webTest`, `integrationTest`, `testBarrido`): un `--tests` sobre `test` no corre los `CU*Test` ni los `*WebTest`.
- `integrationTest` completo: 201 tests, 1 falla (`CU04Test` criterio 3). Aislado pasa en la rama y en `2189e607`: depende del orden o del estado compartido; no se probó que sea preexistente en la suite completa.
- `flutter test test/widget`: 213 OK y 4 fallas, las MISMAS 4 en `2189e607` sin el cambio → preexistentes.
- `origin/dev` avanzó durante el trabajo (#229–#232, rediseño bancario de la app): se integra antes de la PR.

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Despliegue en TEST se construye a mano en el host | Demora | Seguir memoria `aportaya-test-contabo` |
| Goldens de perfil ya fallaban (`test/goldens/failures/h10-perfil-*`) | Ruido en la suite | No tocarlos; registrar como preexistente |
