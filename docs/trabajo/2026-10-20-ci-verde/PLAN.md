# Plan — CI en verde en dev y test sin regresiones

- Fecha: 2026-10-20 · Repos afectados: PasanakuBackend · Predecesor: `2026-10-20-fix-ddl-metadata`, `2026-10-20-firma-ocupa-cupo`
- Resultado observable: los checks de CI de un PR a `dev`/`test` terminan en verde.
- Kill-test: abrir el PR y mirar `gh pr checks`; cualquier rojo = no está hecho.

## Hallazgos
- Los jobs de CI se detienen en el primer paso rojo y algunos dependen de otros (`Los cinco corredores` necesita que `Formato…` pase). Había fallas **ocultas** detrás de las visibles.
- `verificar_frontend.py` muestra solo las 5 primeras fallas de cada regla: había 25 literales de diseño, no 5.
- Fallas visibles en `dev` (60b6b775): `Formato` (spotless en `comun-mensajeria`), `La bóveda y el esquema no divergen` (3 criterios CU-69 sin gherkin, introducidos por 4240ffd8 — propio), `Frontend` (lint de `diseno_flutter`).
- Fallas ocultas encontradas al reproducir en local: spotless en `comun-web`; `testBarrido` (`CU69FirmaYSorteoTest` 323 líneas — propio; `UsuariosInternosRepositorio` 330 líneas); 13 fallas de `identidad:integrationTest` (fixtures sin `nivel_riesgo`, prefijos de `ArranqueTest` desactualizados, bind mal usado en `BorradorOnboardingRepositorioTest`); `diseno_flutter`: 9 archivos sin `dart format`, 25 literales de diseño, 1 `toStringAsFixed`, 6 archivos de más de 200 líneas.

## Alcance
- IN: los arreglos de arriba, mínimos y sin cambiar comportamiento salvo los colores (decisión del dueño, abajo).
- OUT: rediseño de las pantallas; regenerar goldens en Windows (se regeneran en el CI de Linux).
- Decisión del dueño (2026-10-20): los colores sueltos de los componentes nuevos pasan a **colores de marca** existentes (agregar los Tailwind como tokens nuevos viola «la prueba que impide inventar» de `packages/tokens`).

## H1 — CI en verde
**CA:** Dado el PR con estos cambios, cuando corre el CI, entonces todos los checks aplicables terminan en verde.
**DoD:** reproducción local de cada job + `gh pr checks` en verde.
**Estado:** A MEDIAS

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Criterios gherkin de los 3 tests nuevos de CU-69 | `verificar_criterios.py` sin divergencias | `python scripts/verificar_criterios.py` → exit 0 | HECHO |
| H1.S1.M2 | spotless en `comun-mensajeria` y `comun-web` | `spotlessCheck` 0 | `gradlew spotlessCheck` → 0 | HECHO |
| H1.S1.M3 | `CU69FirmaYSorteoTest` vuelve a su versión; los 3 casos nuevos en `CU69FirmaSinCupoTest` | `testBarrido` grupos 0 | `gradlew testBarrido` | HECHO |
| H1.S1.M4 | `UsuariosInternosRepositorio` < 300 líneas moviendo la rotación de roles a `RotacionDeRolesInternos` (mismo código) | `testBarrido` identidad 0 | `gradlew testBarrido` | HECHO |
| H1.S1.M5 | Fixtures de permisos con `nivel_riesgo`; prefijos de `ArranqueTest` alineados con `scripts/modelo.py`; bind de `BorradorOnboardingRepositorioTest` | 183/183 | `gradlew :servicios:identidad:integrationTest` | HECHO |
| H1.S2.M1 | `dart format` con el Dart del CI (3.12.2) | exit 0 | `dart format --set-exit-if-changed` | DESCARTADO (ver REPORTE) |
| H1.S2.M2 | Literales de diseño → tokens (exactos cuando existen; marca cuando no) | regla OK | `verificar_frontend.py diseno` | DESCARTADO (ver REPORTE) |
| H1.S2.M3 | 6 archivos > 200 líneas partidos con `part` (mismo código, mismas expresiones) | regla OK + analyze limpio | `verificar_frontend.py diseno` + `flutter analyze` | DESCARTADO (ver REPORTE) |
| H1.S3.M1 | Regresión backend completa (test, webTest, integrationTest, contractTest, sagaTest, cobertura) | 0 fallos | corrida local en serie | HECHO (CI real de #168/#169 en `3c29e51b`: «Los cinco corredores» pass) |
| H1.S3.M2 | Frontend completo en el CI real (pasos que nunca corrían) | verde | `gh pr checks` | TODO (el CI saltea Frontend: «Detectar impacto» no ve cambios de frontend en esta PR) |
| H1.S4.M1 | P1: el borrador sin sesión solo se guarda con el token CONSUMIDO (código ya confirmado) + huella del correo; control declarado en `PoliticaDeAcceso` | token ENVIADO / correo ajeno / id inexistente → rechazo; CONSUMIDO → guarda | `gradlew :plataforma:comun-pruebas:contractTest` + `:servicios:identidad:integrationTest --tests *BorradorOnboarding*` | HECHO (evidencia/p1-p2-2026-10-21.txt) |
| H1.S4.M2 | P2: pacto nucleo-financiero ↔ entregas (GET oferta) | `PactoConEntregasContratoTest` 0 | `gradlew :servicios:nucleo-financiero:contractTest` | DESCARTADO: no hay falla en el CI; solo en Windows por CRLF (evidencia/p1-p2-2026-10-21.txt) |
| H1.S4.M3 | P3: pisos de cobertura con pruebas nuevas (sin bajar pisos) | auditoria, identidad, notificaciones ≥ piso | `gradlew jacocoTestCoverageVerification` | HECHO (notificaciones L0.91/R0.83 · auditoria L0.94/R0.76 · identidad L0.79/R0.70, local) |
| H1.S4.M4 | Bug hallado por P3: activar/desactivar un elemento de catálogo operativo violaba `ck_bitacora_evento_accion` (acción fuera de la lista) → se registra como `CAMBIO_ESTADO` (acción existente; el sentido queda en `activo`) | el cambio se guarda y deja rastro en la bitácora | `gradlew :servicios:auditoria:integrationTest --tests *CatalogosOperativosRepositorioTest*` | HECHO (6/6; con el nombre viejo 2 fallan por ck_bitacora_evento_accion) |

## Riesgos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Más fallas ocultas detrás de las que se arreglan | más vueltas | reproducir cada job completo, con `--continue` |
| Goldens de los componentes con colores nuevos | golden rojo | se regeneran en el CI de Linux (regla de la casa) |
