# Plan — Alta CU-01: 500 por correo duplicado, catálogo de errores de la base y pantalla legal

- Fecha: 2026-10-15 · Repos afectados: Pasanaku (monorepo) · Predecesor: ninguno
- Rama: `pablo/fix/alta-correo-duplicado-2026-10-15`
- Resultado observable: quien intenta abrir cuenta con un correo que ya tiene cuenta ve en la fase 4
  «Ese correo ya tiene una cuenta…» (no «Algo salió mal de nuestro lado»), y ningún rechazo de la
  base de datos en ningún servicio sale como `500 AP-INT-01`.
- Kill-test: `POST /usuarios` con un correo existente devuelve 500. Un test web que lance la
  excepción de jOOQ (`IntegrityConstraintViolationException`) y reciba 500.

## Hallazgos (fase 1)

| Hecho | Evidencia |
|---|---|
| El 500 de la captura es `uq_usuario_correo` (SQLSTATE 23505) | log de `identidad` en TEST, trazaId `cfa6d668-…`; log de `aportaya-postgres` 17:06:07 UTC (`evidencia/01-diagnostico-test.txt`) |
| CU-01 revisa teléfono y documento antes de insertar, **no** el correo | `servicios/identidad/.../CU01RegistrarUsuario.java:115-123` |
| `ManejadorGlobalDeErrores` atrapa `org.springframework.dao.DataAccessException`; jOOQ lanza `org.jooq.exception.DataAccessException` y el `DSLContext` propio no traduce → **todo** rechazo de la base de los 14 servicios cae en `falloNoPrevisto` (500) | `plataforma/comun-web/.../ManejadorGlobalDeErrores.java:113`, `plataforma/comun-datos/.../ConfiguracionDeDatos.java:28` |
| Los tests del manejador lanzan solo la excepción de Spring, por eso nunca se vio | `ControladorDeEnsayo.java` `/ensayo/restriccion-*` |
| El catálogo generado cubre 193 restricciones (las de reglas R-XXX); el esquema nombra ~1769 (`ck/uq/fk/pk/ex`) | `scripts/generar_errores.py`; conteo en `evidencia/01-diagnostico-test.txt` |
| El código para cuenta duplicada ya existe: `CUENTA_YA_EXISTE = AP-CU01-03` | `docs/CasosDeUso/CU-01 Registro y apertura de billetera.md:102` |
| La app traduce por código y no muestra el mensaje del backend; `AP-CU01-03` dice «Ese celular…» para cualquier duplicado | `apps/movil/lib/dominio/errores.dart:10` |
| El contrato se pinta como un único `Text` plano con encabezados en mayúsculas y rayas ASCII | `apps/movil/lib/pantallas/identidad/pantalla_contrato.dart:131` |

## Alcance
- IN: manejo de excepciones de jOOQ en `comun-web`; respaldo catalogado por SQLSTATE; correo duplicado en
  CU-01 con `detalle.campo`; traducción por campo en la app; presentación estructurada del contrato.
- OUT: el texto legal en sí (no se cambia una palabra); los errores del relevo Kafka de `tarifas` y
  `nucleo-financiero` (se anotan en el reporte); escribir mensajes de usuario para las ~1600
  restricciones sin regla; despliegue a TEST (lo hace el temporizador del VPS / a mano, ver memoria).
- Ambigüedades registradas:
  1. «Todos los errores catalogados» — supuesto: toda falla de la base sale con un código estable por
     SQLSTATE (`AP-BD-0n`) y estado 4xx/503 en vez de `AP-INT-01`; las restricciones con regla siguen
     saliendo con su `R-XXX-nn`. La restricción sin regla se sigue registrando en la bitácora (WARN con
     su nombre) para que alguien la cure. Esto **cambia** la decisión escrita en el Javadoc del
     manejador («la restricción no catalogada sale 500»). Confirmar con Pablo.
  2. Para el correo duplicado se reusa `AP-CU01-03` con `detalle.campo = "correo"` en vez de un código
     nuevo (el CU declara uno solo para «cuenta ya existe»).

## H1 — El alta con correo repetido dice exactamente qué pasa
**CA:** Dado un correo ya registrado, cuando se abre otra cuenta con él, entonces la API responde 422
`AP-CU01-03` con `detalle.campo=correo` y la app muestra «Ese correo ya tiene una cuenta…».
**DoD:** tests de `identidad`, `comun-web` y `flutter test` dirigidos en verde, salida en `evidencia/`.
**Estado:** A MEDIAS — microtareas HECHO con tests; falta desplegar a TEST y repetir el alta real con un correo existente

### H1.S1 — Backend
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | El manejador atrapa `org.jooq.exception.DataAccessException` y lo traduce igual que el de Spring | test web con la excepción de jOOQ y restricción conocida → 409 con su `R-XXX` | `./gradlew :plataforma:comun-web:webTest --tests '*ManejadorGlobalDeErrores*'` verde | HECHO — `evidencia/02`, kill-test `evidencia/03` |
| H1.S1.M2 | Respaldo catalogado por SQLSTATE cuando la restricción no tiene regla (23505→409 AP-BD-01, 23503→409 AP-BD-02, 23514/23502/22xxx→422 AP-BD-03, 23P01→409 AP-BD-04, 40001/40P01→409 AP-BD-05, 57014→503 AP-BD-06); sin SQLSTATE → 500 AP-INT-01 | tests por clase de SQLSTATE; la respuesta no contiene el nombre de la restricción | mismo comando | HECHO — `evidencia/02` |
| H1.S1.M3 | CU-01 rechaza correo duplicado antes de insertar (`AP-CU01-03`, `detalle.campo`), y teléfono/documento llevan su `campo` | test de integración CU-01 con correo repetido → `ErrorDeNegocio` AP-CU01-03 campo correo | `./gradlew :servicios:identidad:integrationTest --tests '*CU01*'` verde | HECHO — `evidencia/07`, `09` |
| H1.S1.M4 | (agregada en ejecución) Documentar el contrato: skill `errores-api` (tabla SQLSTATE → AP-BD) y comentario de `POST /usuarios` en `identidad.yaml` | el contrato documentado coincide con el código | `./gradlew :servicios:identidad:contractTest` exit 0 | HECHO |

### H1.S2 — Móvil
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | `ErrorDeApi` lleva `campo` desde `detalle.campo`; `mensajeDe` da un texto por campo para `AP-CU01-03` y textos para `AP-BD-*` y `AP-CU01-04/05/06` | test unitario: (AP-CU01-03, correo) → texto de correo | `flutter test test/unidad/cliente_test.dart` verde | HECHO — `evidencia/04` |

## H2 — El contrato se lee como un documento, no como un volcado
**CA:** Dado el contrato de adhesión, cuando se abre CU-05, entonces cada cláusula tiene su
encabezado tipográfico, las listas tienen sangría con su marca, el tarifario va en un bloque propio,
hay una barra de lectura, y sigue sin poder aceptarse sin llegar al final. Claro y oscuro.
**DoD:** tests de widget del contrato en verde + capturas inspeccionadas (móvil, tablet, escritorio; claro y oscuro).
**Estado:** HECHO

### H2.S1 — Presentación
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | `bloquesDelContrato(texto)` parte el texto en título / cláusula / párrafo / ítem / separador sin perder ni cambiar ninguna palabra | test: concatenar los textos de los bloques reproduce todas las palabras del original; 23 cláusulas | `flutter test test/identidad/bloques_del_contrato_test.dart` | HECHO — `evidencia/05` |
| H2.S1.M2 | `pantalla_contrato.dart` pinta los bloques con tokens y barra de lectura; el gate de leer hasta el final sigue | `pantalla_contrato_test.dart` existente verde | `flutter test test/identidad/` | HECHO — `evidencia/05`, suite completa `evidencia/11` |
| H2.S1.M3 | Prueba visual en 3 viewports × 2 temas | capturas miradas, en `evidencia/` | capturas | HECHO — `evidencia/capturas/` (12) |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Otra sesión (pasanaku-ce) toca los mismos archivos de móvil | conflicto | mensaje enviado; se revisa antes de editar |
| Cambiar 500→4xx esconde un defecto de esquema | defecto invisible | WARN en bitácora con nombre de restricción y SQLSTATE |
| La corrección no llega a TEST hasta reconstruir imágenes | la captura sigue igual en el iPhone | declarado en el reporte; pasos en `despliegue/TEST.md` |
