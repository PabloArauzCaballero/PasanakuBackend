# Reporte — GU-A · Segregación de funciones y correcciones de lectura (identidad)

> **AVANCE: 16 / 16 — 100 %.** CI del PR #515 en verde; queda el merge, que hace la sesión principal.

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-a-segregacion` (desde `dev` 6729629d) · PR a `dev`
- Peldaño de evidencia alcanzado: **TESTED** (local y CI) en backend (unitarios, web y la suite de integración completa de
  `identidad` contra PostgreSQL real, en local) y en la semilla (aplicada dos veces sobre una base real ya sembrada).
  No es `VERIFIED`: no se ejercitó el camino HTTP completo con un `identidad` levantado ni el backoffice contra él.
  No hay cambio visual (solo una spec de matriz de roles), así que no aplica prueba visual.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Los tests CU08 de segregación usan los códigos reales; con la tabla vieja fallan 4 de 18 | `./gradlew :servicios:identidad:integrationTest --tests 'bo.aportaya.identidad.CU08*'` contra `SegregacionDeFunciones` de dev | rojo esperado · `evidencia/H1-segregacion.md` |
| H1.S1.M2 | `SegregacionDeFuncionesTest` lee la semilla: cada par en ambos sentidos, cada código existe, ningún rol sembrado viola por sí solo, publicidad en tres roles, bloqueo en dos | `./gradlew :servicios:identidad:test --tests '*SegregacionDeFuncionesTest'` | PASS 17/17 |
| H1.S1.M3 | Lista cerrada de P-3: ENTREGA_AUTORIZAR↔EJECUTAR · CONTABILIDAD_ERP_CUENTAS_POR_PAGAR↔PAGAR · BLOQUEO_AUTORIDAD_REGISTRAR↔LEVANTAR · PUBLICIDAD_ANUNCIANTES↔APROBAR_CAMPANA↔MODERAR | CU08 + unitario | PASS (CU08 20/20) |
| H2.S1.M1 | Rol `SUPERVISOR_CUMPLIMIENTO` (BLOQUEO_AUTORIDAD_LEVANTAR + AUDITORIA_LEER); `OFICIAL_CUMPLIMIENTO` ya no levanta, también en bases ya sembradas | `psql -f sql/60_semillas/sembrar.sql` ×2 sobre base con la semilla vieja | PASS · `evidencia/H2-semilla-dos-corridas.md` |
| H2.S1.M2 | Matriz de roles del backoffice al día | `yarn ng test --include=src/app/nucleo/matriz-de-roles.spec.ts` | PASS 45/45 |
| H2.S1.M3 | Tests de bloqueos de nucleo-financiero con el rol que levanta | `./gradlew :servicios:nucleo-financiero:webTest --tests '*Bloqueo*WebTest'` | PASS 7 + 14 |
| H2.S1.M4 | Rol `APROBADOR_PUBLICIDAD`; `OPERADOR_PUBLICIDAD` sin aprobar campañas (decisión de Pablo) | psql ×2 (34 roles · 74 permisos · 166 concesiones, igual en ambas) + `:servicios:publicidad:webTest` | PASS |
| H3.S1.M1 | Ficha y listado leen el bloqueo vigente de `bloqueo_cuenta` (temporal, indefinido; liberado y vencido no se ven) | `UsuariosInternosLecturaRepositorioTest` | PASS |
| H4.S1.M1 | Átomo `RolesParaPersonalInterno.admite(codigo, ambito)` | unitario vía web test y repositorio | PASS |
| H4.S1.M2 | `GET /roles?asignablesAPersonalInterno=true` filtra; sin parámetro, igual que antes; 401/403 | `RolesControllerWebTest` | PASS 5/5 |
| H4.S1.M3 | `PUT /usuarios-internos/{id}/roles` rechaza rol de servicio o de grupo con AP-CU08-08 y no cambia nada | `UsuariosInternosLecturaRepositorioTest` | PASS |
| H5.S1.M1 | `tieneSegundoFactor` = factor activo y confirmado | `UsuariosInternosLecturaRepositorioTest` | PASS |
| H5.S1.M2 | Listado paginado `(creadoEn, id)` desc, filtro de rol en SQL, cursor a medias → AP-CU08-12, límite > 100 → 400 | repositorio + `ListarUsuariosInternosTest` + `UsuariosInternosListadoWebTest` | PASS |
| H6.S1.M1 | Verificadores, DDL, postman, inventario, openapi validate, `check` estático, typecheck y 774 specs del backoffice | ver `evidencia/H6-gates.md` | PASS |
| H6.S1.M2 | PR #515 a `dev` con CI verde en el head cbda32a6 (formato y compilación, base efímera y semillas, contratos, bóveda, los cinco corredores, frontend Flutter/Angular, imagen, Trivy) y mergeable sin conflicto | GitHub checks del PR | PASS |
| H6.S1.M1b | `verificar_seguridad.py` acepta los bloques `sql` de la semilla | `python3 scripts/verificar_seguridad.py` | EXIT 0 |

Regresión del módulo: `./gradlew :servicios:identidad:integrationTest` → 222 tests, 0 fallos (`evidencia/H3-H5-lecturas.md`).

## A medias

ninguna

## Pendiente

ninguna

## Evidencia

- `evidencia/H1-segregacion.md` — rojo con la tabla vieja (4 fallos de R-SEG-04) y verde con la nueva.
- `evidencia/H2-semilla-dos-corridas.md` — semilla vieja → nueva ×2, consultas usadas y conteos.
- `evidencia/H2-matriz-de-roles.md` — spec de matriz y web tests de bloqueos.
- `evidencia/H3-H5-lecturas.md` — integración completa de identidad, unitarios y web; fallos de la primera corrida y su clase.
- `evidencia/H6-gates.md` — verificadores y gates.

```text
$ ./gradlew :servicios:identidad:integrationTest
BUILD SUCCESSFUL in 48s
tests=222 skipped=0 failures=0 errors=0
```

Datos: todos sintéticos (correos `@ejemplo.test`, teléfonos al azar, roles `ROL_xxxx`/`SERVICIO_xxxx`). Ninguna salida
pegada contiene datos de personas.

## No cubierto

- El camino HTTP real de punta a punta (backoffice → identidad levantado → PostgreSQL): solo MockMvc + repositorio real.
- `CU08AsignarRol` todavía no llama a `RolesParaPersonalInterno`: es del lote B (acordado por la sesión principal). Hasta
  entonces, la vía `POST /accesos/asignaciones` de B y el alta (`crear`, lotes B/C) no filtran roles ajenos; sí lo hace
  `PUT /usuarios-internos/{id}/roles`.
- El backoffice no usa todavía `asignablesAPersonalInterno`, ni pagina, ni muestra el bloqueo indefinido (`bloqueadoHasta`
  nulo con `motivoBloqueo`): es del lote D. **Con el default de 50, una pantalla que no pagina ve solo los 50 más
  recientes.**
- Ningún usuario de `seeders/dev` tiene `SUPERVISOR_CUMPLIMIENTO` ni `APROBADOR_PUBLICIDAD`: en una base de desarrollo
  nadie puede levantar un bloqueo por autoridad ni aprobar campañas hasta que se asigne el rol (no se tocó `seeders/dev`).
- La búsqueda libre del listado sigue sin normalizar acentos ni escapar `%`/`_` (preexistente, regla 96.6.4).
- No se probó en local que H3/H5 fallen con el código viejo (sí se probó para H1).

## Desvíos del plan

- AMB-1 se resolvió durante el trabajo (decisión de Pablo): se agregó H2.S1.M4 y cambió la lista de pares de H1.
- H6.S1.M1b agregada: la semilla usa un bloque `sql` (formato que `generar_semillas.py` ya admite) y
  `verificar_seguridad.py` indexaba `b["tabla"]` en cada bloque (KeyError). Se cambió a `b.get("tabla")` en 4 líneas.
- La lectura se movió a `LecturaDeUsuariosInternos` porque `UsuariosInternosRepositorio` tenía 292 líneas (el barrido
  corta en 300). Por acuerdo con B (vía sesión principal) se borraron `GestionarUsuariosInternos.listar` y el `listar`
  sin paginar del repositorio: ninguna ruta los usaba.
- El error de cursor a medias es AP-CU08-12: AP-CU08-09…11 quedaron para B (acuerdo vía sesión principal). Como
  `verificar_boveda.py` exige códigos correlativos en CU-08, la fila `FILTRO_DE_LISTADO_INVALIDO · AP-CU08-12` NO se
  agregó al documento todavía: la agrega quien mergee segundo, después de las 09–11 de B (está en el contrato y el código).
- La constante de tamaño de página se llamó `FILAS_POR_PAGINA` y no `LIMITE_POR_DEFECTO`: el barrido
  `sin-umbral-literal` trata los nombres `LIMITE*` como cifras regulatorias, y esto no lo es.
- Se tocó CU-08 (lista de pares en el flujo 3a, errores AP-CU08-08/09) y el ejemplo generado del simulado de
  `listarUsuariosInternos`.

## Riesgos residuales

- Conflictos de merge con el lote B en `identidad.yaml` y `UsuariosInternosRepositorio` (cambios aditivos y acotados;
  quien mergee segundo rebasea y regenera postman/inventario, según lo acordado).
- Bases ya desplegadas: el DELETE de la semilla retira `BLOQUEO_AUTORIDAD_LEVANTAR` de `OFICIAL_CUMPLIMIENTO` y
  `PUBLICIDAD_APROBAR_CAMPANA` de `OPERADOR_PUBLICIDAD` al volver a sembrar. Quien hoy levanta bloqueos o aprueba campañas
  con esos roles pierde la función hasta que se le asigne el rol nuevo; los tokens vivos la conservan hasta vencer.
- Asignaciones existentes que ya violen los pares nuevos (si las hubiera en TEST) no se detectan retroactivamente: la
  regla corre al asignar.

## Decisiones y ambigüedades

- **P-3 corregida por Pablo (2026-11-24, vía sesión principal `session_01ATKb2QAkFb98LJ18k7D6w4`):** par contable
  `CONTABILIDAD_ERP_CUENTAS_POR_PAGAR`↔`CONTABILIDAD_ERP_PAGAR` (reemplaza `CONTABILIDAD_ERP_PAGAR`↔tesorería); publicidad
  partida en `OPERADOR_PUBLICIDAD` + `APROBADOR_PUBLICIDAD`. Aplicado.
- **AMB-2 (confirmar con Pablo):** «lectura de cumplimiento» del supervisor no existe como permiso; se le dio
  `AUDITORIA_LEER` además de `BLOQUEO_AUTORIDAD_LEVANTAR` (que ya abre la bandeja y su listado).
- **AMB-4 (confirmar con Pablo):** `APROBADOR_PUBLICIDAD` recibe solo `PUBLICIDAD_APROBAR_CAMPANA`: listar, aprobar y
  rechazar campañas exigen solo ese permiso (`PublicidadController:88,148,155`), así que no necesita otra lectura.
- **AMB-3:** paginación aditiva (respuesta sigue siendo un array) con `limite` 1–100 (default 50) y cursor
  `antesDe`/`antesDeId`, patrón de erp/nucleo-financiero.
- Roles no asignables a personal interno: `PARTICIPANTE`, `ORGANIZADOR`, `ANUNCIANTE`, todo rol de ámbito `GRUPO` y todo
  `SERVICIO_*` (prefijo a propósito: una identidad de servicio nueva queda afuera sin acordarse de agregarla).
- Procesos: dockerd y un PostgreSQL local en `127.0.0.1:5433` (lo usa `generateJooq`) se levantaron para las pruebas y se
  bajan al cerrar el turno.
