# Reporte — GU-E · Revocación inmediata, inactividad del backoffice y bitácora de cambios de acceso

> **AVANCE: 13 / 16 — 81,3 %.** Dos microtareas esperan su prueba de integración en la CI (necesitan Docker, que este entorno no tiene) y una es el propio PR.

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-e-revocacion` (desde `dev` 6729629d)
- Peldaño de evidencia alcanzado:
  - Revocación (H1): `TESTED` — pruebas dirigidas en verde contra dobles de Kafka (`MockConsumer`, `KafkaTemplate` mockeado) + SQL real del vigía sobre PostgreSQL 16 con el rol `svc_identidad` y RLS. **No** ejercitado contra un broker real.
  - Inactividad, cliente (H2.S2): `VERIFIED` en el cliente — specs, suite completa del front y E2E visual (Playwright, 3 viewports × 2 temas, capturas inspeccionadas) contra el simulado; el refresco y el `DELETE /sesion` respondidos por la prueba.
  - Inactividad, servidor (H2.S1): `RUNS` (compila); su prueba de integración corre en la CI.
  - Bitácora (H3): `TESTED` el lector del mensaje; persistencia e idempotencia observadas con SQL real como `svc_auditoria`; la prueba Java de integración corre en la CI.

## Completado

| ID | Qué se logró (observable) | Comando de verificación | Resultado |
|---|---|---|---|
| H1.S1.M1 | Índice `ix_sesion_revocada_en` generado desde el `.puml`, aplicado y usado por la consulta del vigía | `python3 scripts/generar_ddl.py` (2 corridas) · `psql -f sql/aplicar.sql` · `EXPLAIN` | PASS — `evidencia/h1s1m1-ddl.txt`, `evidencia/h1s1m1-esquema-aplicado.txt` (Index Scan using ix_sesion_revocada_en) |
| H1.S1.M2 | Contrato `RevocacionDeSesion` (JSON ida y vuelta, tolerante a campos nuevos, rechaza inválidos) | `./gradlew :plataforma:comun-web:test --tests '*Revocacion*'` | PASS 4/4 — `evidencia/h1s2-comun-web-test.txt` |
| H1.S1.M3 | Identidad publica cada sesión cerrada una vez, directo al tema; reintenta si Kafka falla; recorre revocaciones masivas | `./gradlew :servicios:identidad:test --tests '*VigiaDeRevocaciones*'` + SQL del vigía como `svc_identidad` | PASS 6/6 — `evidencia/h1s1m3-h3s1m1-unitarias.txt`, `evidencia/h1s1m3-sql-vigia.txt` |
| H1.S1.M4 | `crear-topicos.sh` crea `aportaya.identidad.revocacion` con `compact,delete` y retención de 1 h | `bash -n despliegue/host/crear-topicos.sh` | PASS (rc=0) — sin broker para ejecutarlo |
| H1.S2.M1 | `ListaDeRevocaciones` recuerda hasta `venceEn`, purga, nace no-al-día | `--tests '*ListaDeRevocacionesTest'` | PASS 4/4 |
| H1.S2.M2 | El decodificador rechaza el token de una sesión revocada (401 `invalid_token`), acepta el resto, falla cerrado sin rehidratar | `--tests '*ValidadorDeRevocacionTest'` | PASS 7/7 |
| H1.S2.M3 | El lector reconstruye la lista desde el tema al arrancar y tras un reinicio; salta ilegibles y lápidas | `--tests '*LectorDeRevocacionesTest'` (`MockConsumer`) | PASS 6/6 |
| H1.S2.M4 | Sonda `revocacion` DOWN hasta estar al día; métrica `revocacion_propagacion_segundos` | incluido en las dos anteriores | PASS |
| H2.S2.M1 | Vigilante: aviso a los 13 min, cierre a los 15, latido cada 5 min con actividad, nada sin sesión, equipo suspendido | `yarn ng test --include=…inactividad.spec.ts` | PASS 7/7 — `evidencia/h2s2-backoffice.txt` |
| H2.S2.M2 | Aviso accesible en el shell (`ap-dialogo`), cierre real (`DELETE /sesion`) y vuelta al ingreso con motivo y `volverA` | 2 specs + `e2e/inactividad-visual.e2e.ts` | PASS 4/4 + 6/6 E2E — capturas `evidencia/visual-inactividad-*.png` (12) |
| H3.S1.M1 | Lector del evento de acceso: seis tipos actuales, tipos nuevos, campos nuevos, sin datos de contacto, rechaza sin `event_id` | `./gradlew :servicios:auditoria:test --tests '*ConsumidorDeCambiosDeAcceso*'` | PASS 6/6 |
| H4.S1.M1 | Verificadores de `scripts/` en 0 y generadores sin diff | ver `evidencia/verificadores.txt` | PASS (12 comandos rc=0) |
| H4.S1.M3 | Este reporte | — | escrito |

## A medias

### H2.S1.M1 — Inactividad del operador en el servidor (`RenovarSesion`)
- Qué anda: compila sin avisos (`./gradlew :servicios:identidad:compileTestJava` rc=0); la regresión unitaria y web de identidad sigue verde (104 + 167, `evidencia/regresion-java.txt`); el criterio está en el gherkin de CU-04 y `verificar_criterios.py` da rc=0.
- Qué no anda / no está demostrado: `CU04InactividadTest` (operador inactivo 16 min → rechazado y revocado `INACTIVIDAD`; activo 14 min → renueva; participante → sin corte) no se ejecutó: necesita Testcontainers y aquí no hay Docker.
- Qué falta exactamente: que el job `integrationTest` de la CI lo corra en verde.
- Dónde quedó: `servicios/identidad/src/main/java/.../aplicacion/RenovarSesion.java`, `servicios/identidad/src/test/java/.../CU04InactividadTest.java`, rama `claude/gu-e-revocacion`, compila.

### H3.S1.M2 — Registro idempotente en la bitácora (`RegistroDeCambiosDeAcceso`)
- Qué anda: las mismas sentencias, ejecutadas con el rol real `svc_auditoria` en PostgreSQL 16 local, escriben la fila con hash de 64 y la segunda entrega del mismo `event_id` no inserta nada (`INSERT 0 0`); el rol no puede borrar la bitácora (`evidencia/h3s1m2-sql-bitacora.txt`).
- Qué no anda / no está demostrado: `CUBitacoraDeAccesosTest` (Java, Testcontainers) no se ejecutó aquí.
- Qué falta exactamente: `integrationTest` en la CI.
- Dónde quedó: `servicios/auditoria/.../aplicacion/RegistroDeCambiosDeAcceso.java`, `.../infraestructura/BitacoraRepositorio.java`, `.../consumo/ConsumidorDeCambiosDeAccesoKafka.java`; compila.

### H4.S1.M2 — PR y CI
- Qué anda: rama publicada.
- Qué falta: PR contra `dev` y CI en verde (se actualiza este reporte con el resultado).

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| Endpoint de respaldo `GET /sesion/validez/{id}` (ADR-030 §3) | TODO (fuera de alcance) | Toca `identidad.yaml`, que es del lote B/C. Hoy el arranque en frío **falla cerrado** (A4). Decide Pablo si hace falta además del fallo cerrado. |
| Gateway consume la revocación (ADR-030 §5) | TODO (fuera de alcance) | El gateway no usa `comun-web`; es trabajo propio del borde. |
| Eventos de CU-08 con actor | DECISION_REQUIRED / coordinación | El lote B debe incluir `actorId` (y opcionalmente `actorRol`, `origen`, `motivo`) en la carga de sus eventos; el consumidor ya los lee. |

## Evidencia

Índice de `evidencia/`:

- `h1s1m1-ddl.txt`, `h1s1m1-esquema-aplicado.txt` — generador idempotente, esquema aplicado, índice usado.
- `h1s2-comun-web-test.txt` — 21 pruebas de revocación en `comun-web`.
- `h1s1m3-h3s1m1-unitarias.txt` — 6 del vigía + 6 del consumidor.
- `h1s1m3-sql-vigia.txt` — paginación por clave con tres sesiones del mismo instante (orden sin signo de UUID) y RLS (sin contexto de sistema: 0 filas).
- `h3s1m2-sql-bitacora.txt` — inserción, duplicado `INSERT 0 0`, `DELETE` denegado.
- `h2s2-backoffice.txt` — typecheck, lint, specs dirigidas, `test:front` 1375/1375, `test:a11y` 98/98.
- `visual-inactividad-{aviso,ingreso}-{movil,tablet,escritorio}-{claro,oscuro}.png` — 12 capturas, mirados los seis pares.
- `verificadores.txt`, `regresion-java.txt`.

```text
$ ./gradlew spotlessCheck …comun-web/identidad/auditoria {test,webTest,testBarrido} --continue
BUILD SUCCESSFUL in 35s
  plataforma/comun-web:test: 45 pruebas, 0 fallas · webTest: 54
  servicios/identidad:test: 104 · webTest: 167
  servicios/auditoria:test: 64 · webTest: 82
```

### Latencia (ADR-030 §4, P-8)
Argumento, no medición contra broker: el vigía mira cada 250 ms en su propio hilo (no comparte el planificador con el relevo del outbox, que puede esperar 10 s a Kafka); publica sin outbox y espera el acuse; cada servicio tiene un consumidor sin grupo que recibe en cuanto el broker tiene el registro (`fetch.min.bytes` = 1). Peor caso típico ≈ 250 ms + red. La métrica `revocacion_propagacion_segundos` (p99) lo mide en producción; la prueba `enVivoMideLaPropagacion` verifica que se registra.

## No cubierto

- Ningún componente de la revocación corrió contra un **Kafka real**: el tema compactado, `crear-topicos.sh`, el `KafkaConsumer` real y la latencia de punta a punta quedan para `e2eTest`/TEST.
- Prueba de **caída del dependido** (regla 98.8.4): no se apagó Kafka de verdad; está simulado (envío fallido → reintento; lector sin broker → no al día).
- `CU04InactividadTest` y `CUBitacoraDeAccesosTest` (Testcontainers) — dependen de la CI.
- Sincronía de actividad **entre pestañas** (`BroadcastChannel`): escrita, sin prueba automatizada.
- El consumidor Kafka de auditoría con `@KafkaListener` real (patrón de temas, reintentos del `DefaultErrorHandler`).
- Lector de pantalla real sobre el aviso (se verificó por rol/nombre y el diálogo existente tiene sus pruebas de foco).

## Desvíos del plan

- **Paginación por clave en el vigía** (H1.S1.M3): el diseño inicial consultaba `revocada_en >= desde LIMIT 500`; una revocación masiva (más de 500 en el margen) se habría quedado devolviendo siempre las mismas filas. Se cambió a `(revocada_en, id) > cursor`.
- **Hilo propio** para el vigía y el lector en vez de `@Scheduled`: el planificador de Spring tiene un solo hilo compartido con el relevo del outbox.
- **Criterios en la bóveda**: se agregaron tres escenarios gherkin a CU-04 (lo exigió `verificar_criterios.py`); el requisito ya estaba en el paso 7 de CU-04 y en ADR-038 §1.
- **SQL**: el `UPDATE` de inactividad pasó a jOOQ tipado porque `verificar_seguridad.py` marcó la concatenación de literales.
- **Prueba visual**: la primera tanda de capturas salió con la animación de entrada a medias; se corrigió la prueba (espera `document.getAnimations()`), no el producto.
- **Gradle local funcionó** (Maven Central respondió, con 429 intermitentes): se pudo compilar y correr las pruebas sin contenedor aquí.

## Riesgos residuales y deuda

- **Arranque en frío falla cerrado**: si Kafka no responde cuando un servicio arranca, ese servicio rechaza todo token con sesión (401) y su sonda `revocacion` queda DOWN hasta alcanzar el tema. Es lo que pide ADR-030 §3 sin el endpoint de respaldo; puede sorprender en un entorno con Kafka caído. Se apaga por servicio con `aportaya.revocacion.lector.habilitado=false` (vuelve al comportamiento anterior y lo dice en el log).
- **Tema inexistente = al día**: con `KAFKA_AUTO_CREATE_TOPICS_ENABLE=false`, si nadie corrió `crear-topicos.sh` los servicios aceptan tokens (no hay revocaciones que leer) y el vigía de identidad no puede publicar (lo registra en el log y reintenta). Hay que correr el script en cada entorno.
- **Readiness**: la sonda `revocacion` entra en `/actuator/health`; si el grupo `readiness` del despliegue no la incluye explícitamente, el orquestador no la mira (mismo caso que la sonda `base` existente).
- **Índice nuevo sobre `sesion`**: `CREATE INDEX` sin `CONCURRENTLY` (el generador no lo admite); bloquea escrituras en `sesion` mientras se crea. Tabla chica hoy.
- **Inactividad en el servidor**: el servidor solo ve refrescos; la actividad humana la decide el cliente (A6). Un cliente que no corre el vigilante (p. ej. un script con la cookie) mantiene la sesión mientras refresque cada < 15 min.
- **Datos cargados al cerrar** (regla 95.3.3): al cerrar por inactividad se vuelve a la misma pantalla (`volverA`) y los formularios largos con borrador (`ServicioBorrador`) conservan lo cargado; un formulario sin borrador pierde lo escrito. El aviso de 2 minutos mitiga, no elimina.
- **Deriva preexistente en `dev`**: regenerar los clientes Dart y los ejemplos del simulado cambia archivos versionados ajenos a este lote; se revirtieron sin commitear.

## Decisiones y ambigüedades

Registradas en el PLAN (A1–A6), a confirmar con **Pablo**:

1. **A1 latencia**: P-8 dice < 1 s, ADR-030 dice ≤ 5 s. Se diseñó para < 1 s típico; no se reescribió el ADR.
2. **A2 clave = `sesion_id`**, no `jti` (identidad no guarda `jti`).
3. **A3 publicación por vigía** sobre `sesion.revocada_en` en vez de «en el mismo commit»: cubre los seis puntos de revocación (dos son del lote B y uno es un trigger).
4. **A4 arranque en frío falla cerrado** sin endpoint de respaldo.
5. **A5 actor de la bitácora**: hoy los eventos no lo traen → `actor_usuario_id` nulo; coordinar con el lote B.
6. **A6 inactividad**: el cliente decide la actividad humana; el servidor niega refrescos de operador con más de 15 min.
7. Al cerrar el aviso con «Cerrar aviso» o Escape se considera que la persona está presente (no cierra sesión); «Salir» sigue en el shell.
8. El consumidor de auditoría **no copia** a la bitácora campos de la carga cuyo nombre contenga correo, teléfono, documento, clave, contraseña, token o secreto (regla 90.2).

### Coordinación con otros lotes
- Lote B: nombrar los eventos nuevos de CU-08 como `identidad.usuario_interno_*`, `identidad.rol_*` o `identidad.asignacion_rol_*` (los escucha el patrón) e incluir `actorId`, `actorRol`, `origen` (`PANEL_ADMIN`/`TAREA_PROGRAMADA`) y `motivo`.
- Lote C (ola 2): `pantalla-de-ingreso.ts` ganó el mensaje de inactividad (cuatro líneas) — tenerlo en cuenta al rehacer el ingreso.
- Ningún archivo de los lotes A, B o G fue tocado.

### Procesos al cerrar
- PostgreSQL 16 efímero en `/var/tmp/gue-pg` (puerto 55432): se detiene al cerrar el turno.
- Servidores de Playwright (simulado :4010, `ng serve` :4300): los levanta y baja Playwright.
