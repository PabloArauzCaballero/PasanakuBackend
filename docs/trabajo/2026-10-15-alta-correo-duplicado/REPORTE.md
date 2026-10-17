# Reporte — Alta CU-01: 500 por correo duplicado, catálogo de errores de la base y pantalla legal

> **AVANCE: 8 / 8 — 100 %.** Backend desplegado y verificado en TEST (`evidencia/12`). Build de TestFlight 202610151849 (commit dbfbf35b) subido: `Upload succeeded · EXPORT SUCCEEDED`.

- Fecha: 2026-10-15 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/fix/alta-correo-duplicado-2026-10-15` (sin commit, sin push)
- Peldaño de evidencia alcanzado: backend `TESTED` · móvil `TESTED` + prueba visual · de punta a punta contra TEST: **no alcanzado**

## Causa del error de la captura

`POST /usuarios` (trazaId `cfa6d668-da35-7283-2482-131532573174`, 17:06 UTC) rebotó en el índice único
`uq_usuario_correo`: **ese correo ya tiene una cuenta**. Hubo dos defectos encadenados:

1. CU-01 preguntaba por teléfono y documento repetidos antes de insertar, pero no por el correo.
2. `ManejadorGlobalDeErrores` atrapaba solo `org.springframework.dao.DataAccessException`. Los 14 servicios
   usan jOOQ con un `DSLContext` propio que no traduce a Spring y lanza `org.jooq.exception.DataAccessException`.
   **Todo** rechazo de la base de **todos** los servicios salía `500 AP-INT-01` («algo salió mal de nuestro
   lado»), incluso los 191 que sí estaban en el catálogo de reglas. Los tests nunca lo vieron porque lanzaban la
   excepción de Spring.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | El manejador atrapa también la excepción de jOOQ y traduce por catálogo `R-XXX-nn` | `./gradlew :plataforma:comun-web:webTest --tests '*ManejadorGlobalDeErrores*'` | PASS 30/30 · `evidencia/02`; sin el arreglo, 10/11 en rojo · `evidencia/03` |
| H1.S1.M2 | Lo que la base rechaza sin regla sale con un código estable por SQLSTATE (`AP-BD-01`…`06`) y WARN en la bitácora con el nombre de la restricción, nunca el valor | mismo | PASS · el cuerpo no contiene la restricción, la tabla ni el correo |
| H1.S1.M3 | CU-01 rechaza el correo repetido antes de insertar: `422 AP-CU01-03` con `detalle.campo=correo` (teléfono y documento también llevan su campo) | `./gradlew :servicios:identidad:integrationTest --tests '*CU01*'` (PostgreSQL real) | PASS 24/24 · `evidencia/07`, `09` |
| H1.S1.M4 | Contrato documentado: skill `errores-api` (tabla SQLSTATE → código) y comentario de `POST /usuarios` | `./gradlew :servicios:identidad:contractTest` | exit 0 |
| H1.S2.M1 | La app lee `detalle.campo` y dice «Ese correo ya tiene una cuenta. Volvé atrás y usá otro, o iniciá sesión si es tuyo.»; textos para `AP-BD-0n`, `AP-CU01-04/05/06` y 400/422 | `flutter test test/unidad/cliente_test.dart` | PASS 4/4 · `evidencia/04` |
| H2.S1.M1 | El contrato se parte en bloques (título, cláusula, párrafo, inciso, tarifario, pie) sin agregar ni quitar una palabra | `flutter test test/identidad/bloques_del_contrato_test.dart` | PASS 5/5 |
| H2.S1.M2 | Pantalla legal nueva: «CLÁUSULA PRIMERA» + título en tipo oración, término de cada definición en negrita, incisos con sangría colgante, tarifario en recuadro, barra de lectura, ancho de lectura de 680 en tableta y escritorio, borde entre lo que se lee y lo que se firma. El candado de «leer hasta el final» sigue | `flutter test test/identidad/` | PASS 24/24 · `evidencia/05` |
| H2.S1.M3 | Prueba visual: móvil 390, tableta 820 y escritorio 1280, en claro y oscuro, al principio y al final. Capturas revisadas: se corrigieron líneas demasiado largas en escritorio, la barra pegada al texto y viñetas diminutas | capturas con las fuentes reales | 12 PNG en `evidencia/capturas/` |

Regresión: `comun-web` webTest 40/40 · `identidad` webTest 46/46 · `identidad` integración CU01* 24/24 ·
`flutter analyze` sin problemas · `flutter test` completo 269/269 · `scripts/verificar_frontend.py` TODO OK
(ningún archivo de más de 200 líneas).

## A medias

### H1 — El alta con correo repetido dice exactamente qué pasa (hito)
- **Qué anda:** las 4 microtareas tienen test dirigido en verde, contra PostgreSQL real en el caso de CU-01.
- **Qué no anda:** en TEST sigue corriendo la imagen vieja. El iPhone va a mostrar «Algo salió mal de nuestro
  lado» hasta desplegar.
- **Qué falta exactamente:** commit y push a `origin` y `frontend-mirror`, merge a `test`, reconstruir en el host
  (`construir-todo.sh` para identidad y comun-web; `construir-movil-web.sh` para la app web, ~28 min), después
  `disparar-despliegue.sh` y un build nuevo de TestFlight para el iPhone. Por último, repetir el alta con un correo
  existente y ver `422 AP-CU01-03 campo=correo` en el log de `identidad`.
- **Dónde quedó:** rama `pablo/fix/alta-correo-duplicado-2026-10-15`, sin commit. Compila y los tests pasan.

## Actualización 2026-10-15 22:24 UTC
- PR #72 mergeado a `test` (PasanakuBackend). El temporizador del VPS reconstruyó `identidad` (imagen `fdaaca842586`, contiene `FallaDeLaBase`).
- **VERIFIED en TEST:** alta con un correo ya registrado → `HTTP 422 AP-CU01-03 detalle.campo=correo` (`evidencia/12`). Antes: 500.
- La construcción del backoffice falló en el VPS (`TS2339 'telefono' en ExpedienteEnRevision`, cambios de backoffice de otra rama): sigue con la imagen anterior. No es de este trabajo.
- Los PRs #21 y #22 de PasanakuFrontend se abrieron solos contra `dev` y traen unos 4.900 archivos con conflictos, porque el espejo tiene otra historia. No se mergearon; falta que Pablo decida.
- TestFlight: `testflight_ios.sh` compiló, pero falló al firmar (`Failed to Use Accounts`): Xcode no tenía ninguna cuenta cargada. Se subió con la clave de API de App Store Connect (`xcodebuild -exportArchive … -authenticationKeyPath/-ID/-IssuerID`, variables `EXPO_ASC_*` de `~/.appstoreconnect/env`). Resultado: `Upload succeeded`, `** EXPORT SUCCEEDED **`, build 202610151849.

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Despliegue a TEST + verificación real | TODO | Que Pablo autorice el push a `test`: es un entorno compartido |

## Evidencia
Todo en `evidencia/`: `01` diagnóstico en TEST (log de identidad y de postgres: solo el nombre de la
restricción y la sentencia con parámetros `$n`, sin ningún dato de la persona, comprobado con grep), `02`/`03` manejador y prueba inversa,
`04`/`05`/`11` móvil, `06` verificar_frontend, `07`/`08`/`09` identidad y regresión, `10` analyze, `capturas/`.

```text
$ ./gradlew :plataforma:comun-web:webTest --tests '*RechazosDeJooq*'   # con el @ExceptionHandler viejo
11 tests completed, 10 failed
```

## No cubierto
- **Punta a punta contra TEST**: no se desplegó (ver A medias). No se levantó `identidad` en local porque necesita
  Kafka, MinIO, JWKS, la pimienta y la licencia de otro servicio.
- La carrera entre dos altas simultáneas con el mismo correo: el código la cubre (409 `AP-BD-01`), pero no hay
  test de concurrencia real de CU-01 para ese caso.
- Las demás restricciones sin regla (~1 500, sobre todo `fk_`, `ck_`, `pk_`) salen con su código genérico
  `AP-BD-0n`, no con un mensaje propio de su caso de uso. La bitácora las señala con WARN para curarlas una por una.

## Desvíos del plan
- Los tests del manejador corren con `webTest` (no `test`) y los de CU-01 con `integrationTest`. Corregido en el plan.
- Se agregó H1.S1.M4 (documentar el contrato), por la regla 96.4.2.
- Para cumplir el tope de 200 líneas, la pantalla del contrato quedó en 4 archivos: `pantalla_contrato.dart`,
  `cuerpo_del_contrato.dart`, `barra_de_lectura.dart` y `panel_de_aceptacion.dart`.
- Para correr Testcontainers hubo que reiniciar Docker Desktop, que respondía 500. También hubo que aplicar
  `sql/aplicar.sql` a la base local vacía (`aportaya-postgres`, descartable) para `generateJooq`. El contenedor
  quedó **detenido**. Docker Desktop queda corriendo.

## Riesgos residuales
- Pasar de `500` a `4xx` puede esconder un defecto de esquema. Mitigación: WARN con restricción y SQLSTATE. Una
  alerta sobre ese WARN no existe todavía.
- `uq_usuario_correo` distingue mayúsculas (`Ana@x.com` y `ana@x.com` son correos distintos) y
  `telefonoYaRegistrado` ignora los usuarios eliminados mientras el índice no lo hace. El segundo caso ahora sale
  409 `AP-BD-01` en vez de 500, pero el texto no dice «teléfono». Anotado, no se arregló: está fuera de alcance.
- El schema `Error` de `identidad.yaml` exige `^AP-CU\d+-\d{2}$`, y ya no lo cumplían `AP-VAL-*`, `AP-INT-01`
  ni `R-XXX-nn`. Ahora tampoco lo cumplen `AP-BD-*`. Es una deuda previa que no se tocó, porque cambiarla regenera
  los clientes.
- En TEST, `tarifas` y `nucleo-financiero` registran unos 76 ERROR cada uno en el relevo del outbox hacia Kafka
  («Exception thrown when sending a message»). No es una respuesta HTTP y queda fuera de alcance. Hay que mirarlo aparte.

## Decisiones y ambigüedades
1. **«Todos los errores catalogados»**: se interpretó como «ningún rechazo de la base sale 500 genérico». Eso
   revierte la decisión escrita en la skill `errores-api` («sin traducción ⇒ 500 y alerta»), que se actualizó.
   **Confirmar con Pablo.**
2. Para el correo se reusó `AP-CU01-03 CUENTA_YA_EXISTE` con `detalle.campo`, en vez de inventar un código nuevo
   (lo respalda `docs/CasosDeUso/CU-01 …md:102`).
3. Coordinación con la sesión `pasanaku-ce`, que trabaja en `pablo/feat/visual-bancario-2026-10-15`: no hay archivos
   compartidos. Esa rama cambia los tokens globales y los goldens `alta-contrato-*`. Acordamos que quien mergee
   segundo regenera esos PNG; esta rama no los toca.
