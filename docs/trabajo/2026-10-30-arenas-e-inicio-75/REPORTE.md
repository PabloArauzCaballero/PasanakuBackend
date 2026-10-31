# Reporte — Etapas de reputación, inicio al 75 % y cierre de ingresos

> **AVANCE: 25 / 35 — 71,4 % (1 DESCARTADO).** H1 quedó en código y probado, con un bloqueante de despliegue: falta aprovisionar la cuenta de servicio `SERVICIO_GRUPOS` en TEST. H2 (gamificación, ronda 2) y H3 (primera cuota antes del sorteo) no se empezaron.

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/arenas-y-inicio-75-20261030` (commits `3e94c14a`, `f7801563`, merge de `origin/test` `b64e073d`, `a8080faa`)
- Peldaño de evidencia alcanzado: **TESTED**. Los tests de grupos y transparencia corren contra Postgres real con el rol de la aplicación. Lo que cruza servicios (grupos → transparencia con token de servicio) solo se probó contra un doble HTTP. La app solo se probó contra dobles de su cliente. No hay prueba de punta a punta en TEST ni captura en dispositivo.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S0.M1, H1.S7.M1 | Contrato de grupos y transparencia: `iniciarGrupo`, `etapaMinima`, `GET /reputacion/etapas`, `GET /reputacion/etapas/de-personas` (solo servicio), etapa por integrante, re-firma | `./gradlew :servicios:grupos:compileJava :servicios:transparencia:compileJava` | exit 0 |
| H1.S1.M1, M1b, H1.S7.M2 | Catálogo de etapas y desbloqueos generado desde el modelo, semilla idempotente, lectura bajo RLS (`Datos.comoSistema`), puntaje exacto solo para su dueño, catálogo para backoffice | `./gradlew :servicios:transparencia:{test,webTest,integrationTest}` | 221 tests, 0 fallas · `evidencia/h1-s7-t-*.txt` |
| H1.S2.M0–M3, H1.S7.M3 | `iniciarGrupo` (≥ ceil(0,75 × cupos), achica el grupo, precondición en el UPDATE, idempotente), cierre de ingresos, etapa mínima en todos los caminos, versión N+1 del reglamento que cierra la anterior, re-firma idempotente | `./gradlew :servicios:grupos:{test,webTest,integrationTest,spotlessCheck}` | 610 tests, 0 fallas · `evidencia/h1-s8-grupos-final.txt` |
| H1.S8.M2 | Etapa mínima al pujar en la subasta del cupo (#346, llegó con `test`), antes de retener la plata; falla cerrado | `--tests '*GruposSubastaWebTest*'` | 9/9; con el control desactivado fallan 2 (prueba de mutación) |
| H1.S3.M1–M2, H1.S4.M1–M3, H1.S7.M4 | App: selector de etapa mínima, progreso al 75 % y botón Iniciar, camino de etapas, insignia por integrante, aviso de re-firma, textos «Etapa N · Nombre» | `flutter analyze` · `flutter test` | sin issues · 2668 tests, **0 fallas nuevas** contra la base |
| H1.S6.M1 | Renombre arena → etapa sin restos | `grep -rniw arena` (código, contratos, esquema, semillas, app) | solo 3 tests que comprueban que no aparece y el «reloj de arena» de recargas · `evidencia/h1-s6-sin-arena.txt` |
| H1.S5.M1 | Generadores y regresión de los fronts: `SERVICIO_GRUPOS` sembrado, compose con las variables vacías, Postman, catálogo de datos y de operaciones | `MatrizDeAutorizacionContratoTest` · backoffice `test:front` · web `test:front` · lints | 20/0 · 1322/0 · 74/0 · lints «TODO OK» |
| H1.S8.M1 | Integración con `origin/test` (merge, 5 conflictos resueltos) | suites completas de arriba después del merge | en verde |

## A medias
### H1.S6.M2 — prueba visual en dispositivo
- Qué anda: tests de widget y goldens del sistema de diseño en verde, en claro y oscuro.
- Qué no anda: no se vio en un teléfono contra el backend real.
- Qué falta exactamente: aprovisionar `SERVICIO_GRUPOS` en TEST (ver Riesgos 1), desplegar, sacar capturas a 360/412/600 dp en claro y oscuro de Mi puntaje, camino de etapas, tarjeta con insignia, progreso al 75 % y re-firma, y mirarlas.
- Dónde quedó: la rama compila y los tests pasan.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H2.S1.M1–M5 | TODO | Ronda 2 de gamificación, diseño en [GAMIFICACION.md](./GAMIFICACION.md); se lanza después de este PR |
| H2.S2.M1 | TODO — DECISION_REQUIRED | Pablo: ¿endpoint agregado por etapa y resumen de cierre? |
| H3.S1.M4 | A MEDIAS | Test que genere el periodo 1 dos veces y demuestre una sola obligación (la base ya lo impide por `uq_obligacion_periodo_cupo`) |
| H3.S2.M1 | TODO | Pantalla «esperando la primera cuota de N» y botón de sorteo bloqueado con motivo |
| H3 (resto) | ver PLAN | Emitir `grupos.grupo_iniciado` al iniciar y sumar `puedeSortear` con la primera cuota; el lado de aportes está listo en `claude/sorteo-primera-cuota-20261030 @ 20f874d0` (pasanaku-1f). **Esa rama no se mergea sola: deja todos los sorteos en `AP-CU60-11`** |

## Evidencia
```text
grupos (después del merge con test y del cambio de subasta)
  ./gradlew :servicios:grupos:spotlessCheck :servicios:grupos:test :servicios:grupos:webTest :servicios:grupos:integrationTest --rerun-tasks
  exit=0 · grupos total 610 fallas 0 errores 0
transparencia
  test 24/0 · webTest 51/0 · integrationTest 146/0 (total 221/0)
plataforma
  :plataforma:comun-pruebas:contractTest → 20/0 (MatrizDeAutorizacionContratoTest en verde con SERVICIO_GRUPOS sembrado)
esquema
  generar_ddl.py → 347 tablas · «Sin pendientes a nivel de datos.» · sin diff
  semillas (base descartable): corrida 1 → 123 INSERT; corrida 2 → filas escritas: 0 · verificar_deriva_bd.py → SIN DERIVA
app
  flutter analyze → No issues found!
  flutter test --concurrency=1 → 2668 tests · 351 fallas, todas preexistentes (goldens y capturas): 0 nuevas, 2 que ya no fallan
fronts web
  backoffice test:front → 186 archivos, 1322 tests, 0 fallas · web test:front → 74/0 · ng lint + verificar_frontend → TODO OK
```
Archivos en `evidencia/`: `h1-s1-a-*`, `h1-s7-t-*` (RLS, mutación de privacidad, semillas, matriz), `h1-s5-*` y `h1-s8-*` (suites, lints, generadores), `h1-s6-sin-arena.txt`.

**Seguridad (regla 90.6)**
- Amenazas: entrar por un camino lateral sin la etapa mínima, leer el puntaje ajeno, usar la etapa de otro como oráculo, iniciar sin ser organizador, carrera de dos inicios.
- Controles: la etapa en todos los caminos de ingreso y en la puja (`EtapaDelIngreso`), `consultarPuntaje` y `consultarPerfilEnGrupo` solo para el dueño, autorizar antes de consultar la etapa, `AP-CU69-07` para quien no organiza, y la precondición en el UPDATE.
- Tests: `EtapaMinima*`, `CU69IniciarGrupoConcurrenciaTest`, `Privacidad.*` y las pruebas de mutación.
- Riesgo residual: ver más abajo.

**Dinero (regla 91.6)**
- Este cambio **no genera asientos ni mueve fondos**. Cambia cuántos cupos y periodos tiene un grupo antes de que exista cualquier obligación.
- Idempotencia probada: la misma clave da el mismo resultado y dos inicios simultáneos aplican uno solo.
- Redondeo del umbral: `ceil(0,75 × cupos)`, con `cupos_totales` entero, así que no queda residuo.
- No aplican cuadre, asientos ni reversa.

**Microservicios (regla 98.8)**
- Saltos: grupos → transparencia (`/reputacion/etapas/de-personas` y `/reputacion/etapas`).
- Caída del dependido: con un doble HTTP, los ingresos fallan cerrados y el detalle sale sin etapas.
- **No se hizo prueba de contrato productor/consumidor ni traza con `correlationId`.** Ver No cubierto.

## No cubierto
- Punta a punta en TEST, porque falta la credencial de `SERVICIO_GRUPOS` en el entorno.
- Prueba de contrato productor/consumidor (pact) entre grupos y transparencia. La prueba de caída se hizo contra un doble, no apagando el contenedor.
- Carrera entre un inicio y un ingreso simultáneos. Solo se probaron dos inicios a la vez.
- La adjudicación de la subasta no revalida la etapa (`traspasarCupoPorSubasta`, que llama `entregas`). Se controla solo al pujar. Si la etapa del ganador baja entre la puja y el cierre, igual recibe el cupo.
- `test:a11y` del backoffice no se corrió: vence por plazo también en el árbol limpio.
- Capturas en dispositivo (H1.S6.M2).

## Desvíos del plan
- CI de #364 en rojo por gates que no se corrieron en local (testBarrido y verificar_criterios). Se corrigieron así:
  - archivos de 300 líneas o más, partidos sin cambiar comportamiento;
  - los topes de 60 personas por consulta pasaron a configuración;
  - los 36 tests de grupos se renombraron a «Dado … · Cuando … · Entonces …», con su escenario en CU-20, CU-68 y CU-69;
  - prueba de rechazo de R-REP-07.
- **`scripts/generar_postman.py`** ahora lee los `@Permiso` también de las clases `Rutas*`. Antes solo leía `*Controller.java`, así que partir los controladores borraba los casos «sin sesión». Además recupera 28 casos de operaciones `RutasDe*` que nunca los habían tenido. El diff de Postman toca 10 colecciones, todo salida generada.
- Dos clases de prueba de transparencia que salieron del split caían en el corredor unitario, con 5 s de preparación, aunque usan Postgres. Se renombraron a `…RepositorioTest`.
- H1.S9 (cuenta de servicio en TEST): en TEST tampoco están aprovisionadas las cuentas de entregas ni de inversiones. Se hace por la API de identidad, después del despliegue.
- H3: la emisión de `grupos.grupo_iniciado` la escribió pasanaku-1f (`bd7280d9`) y se absorbió acá después de revisarla. `CU60Sortear` y aportes quedan en la rama de pasanaku-1f, que se mergea **junto** con esta.
- Se integró `origin/test` con **merge** y no con rebase, porque la rama ya estaba publicada. Hubo 5 conflictos: 3 de imports en la app, la lista de dobles de un WebTest y `metadata_enriquecida.json`, que se regeneró.
- Renombre arena → etapa a pedido de Pablo (H1.S6), con catálogo nuevo: códigos `RECUPERACION…PILAR`.
- Se agregó H1.S8.M2 (subasta), descubierta al integrar `test`.
- Al regenerar, el catálogo de operaciones del backoffice sumó 18 operaciones, entre ellas las de subasta y foto de perfil, que ya faltaban en `test`.
- Un carril se cortó por una falla de red y se relanzó para continuar su trabajo, no desde cero (ronda 1b).
- Integración con la rama de pasanaku-1f: se acordó el contrato del evento, pero la emisión queda para H3.

## Riesgos residuales
0. Intermitente sin explicar: `AdmisionDelGrupoSustitucionTest.entranteHabilitadoDelegaYDevuelveElRegistro` falló una vez por `armar() timed out after 5 seconds`. El test solo arma dobles y ninguna clase de esta rama lo toca. Corrido solo, pasó 3 de 3, y también en la corrida completa siguiente. Por la regla 80.5.3 es un bug hasta que se demuestre lo contrario.
1. **BLOQUEANTE DE DESPLIEGUE: `SERVICIO_GRUPOS` sin aprovisionar en TEST.**
   - Ya está en el repo: el rol en `seeders/minimos/10-roles-y-permisos.json` y las variables vacías en `scripts/generar_compose.py` y `docker-compose.coolify.yml`.
   - Falta en el entorno: crear en identidad una cuenta de servicio con ese rol y cargar en Coolify `GRUPOS_SERVICIO_TELEFONO`, `GRUPOS_SERVICIO_CLAVE` y `GRUPOS_SERVICIO_TOTP_SEMILLA` (o `GRUPOS_SERVICIO_FACTOR_FIJO` en TEST), igual que `SERVICIO_ENTREGAS`.
   - **Sin eso, en TEST toda alta de grupo y todo ingreso fallan con «No pudimos confirmar tu etapa»**, y la app no sirve para probar esto.
2. Los desbloqueos por etapa son valores **PROPUESTOS y sintéticos**. No se aplican hasta que alguien los pase a `VIGENTE` con la confirmación de Pablo y de cumplimiento.
3. Posible inferencia del puntaje ajeno a partir de los factores que siguen visibles en el perfil del grupo. No se verificó.
4. Al integrar la rama de pasanaku-1f: `CU60Sortear` cambia de constructor y `AP-CU60-05` se calcula de otra forma. Los dos hacen chequeos de la versión vigente del reglamento que se pisan, y hay que dejar uno solo.

5. **La regla «todos aceptaron la versión vigente del reglamento» está escrita dos veces**, en `SorteoRepositorio.todosAceptaronElReglamento` (esta rama, para `puedeSortear` del detalle) y en `AceptacionVigenteRepositorio` (rama de pasanaku-1f, para bloquear `CU60Sortear`). Hoy coinciden: el merge simulado de las dos ramas da grupos 621/621, según pasanaku-1f. Si una cambia sin la otra, la app mostraría «se puede sortear» y el servidor rechazaría. Deuda: dejar una sola consulta.

## Decisiones y ambigüedades
- Decisiones de Pablo (AskUserQuestion, 2026-10-30), en la tabla del PLAN:
  - trofeos = puntaje existente;
  - etapa mínima por grupo;
  - el inicio lo hace el organizador desde el 75 %;
  - el grupo se achica;
  - con cupos RESERVADO no se puede iniciar;
  - el traspaso sigue permitido respetando la etapa mínima;
  - la etapa de otros integrantes se ve, el puntaje no;
  - el reglamento se re-firma;
  - el puntaje exacto solo lo ve su dueño;
  - nombre «Etapa» con número y un nombre sobrio;
  - desbloqueos con valores a confirmar;
  - la primera cuota nace al iniciar (H3).
- Supuestos a confirmar con Pablo:
  - los nombres y tramos de las etapas, propuestos por Claude;
  - quien recién llega empieza en «Etapa 1 · Inicial»;
  - con la subasta alcanza controlar al pujar y no al adjudicar.
