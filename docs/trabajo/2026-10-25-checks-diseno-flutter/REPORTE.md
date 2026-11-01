# Reporte — dejar en verde el lint propio de `packages/diseno_flutter` (paso f1 del CI)

> **AVANCE: 8 / 8 microtareas HECHO — 100 %.** Pero **el job «Frontend» sigue en rojo**: al pasar f1 apareció otro rojo previo, f5 (alturas táctiles < 44 px), que no es de este cambio y queda **PENDIENTE**.

- Fecha: 2026-10-25 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/awesome-turing-5wmr4r` (sobre `dev` 4b6800e6) · Repo: PasanakuBackend
- Peldaño de evidencia alcanzado: **VERIFIED** para el objetivo (f1 y los pasos que antes quedaban ocultos —f2, f3, «app ·» ×4, f4— pasan en el CI real de GitHub, ver evidencia 02). **No** `REGRESSION_VERIFIED`: el job completo no está verde (f5) y f6/f7 no se ejecutaron. **Sin prueba visual** (ver «No cubierto»).

## Qué pasaba
- `dev` tenía el job «Frontend — Flutter y Angular» en rojo desde el 20/10: el paso `f1 · lint` moría en `@aportaya/diseno-flutter#lint` con 3 reglas propias. Deuda de `c46d277a`; `5b605dbf` ya la dejó anotada («9 archivos que ya rompían el lint en dev»).
- Como el CI corta en el primer fallo, había tres rojos más ocultos (`apps/movil` ×2, `packages/ui` ×1).
- Un segundo rojo independiente en `dev`, «Los cinco corredores» → `13 · test` → `AdmisionDelGrupoSustitucionTest` (`armar() timed out after 5 seconds`), **no se repitió** en este run: pasó. Es intermitente; no se tocó.

## Completado
Evidencia: [00-baseline](./evidencia/00-baseline.txt) · [01-verificacion-final](./evidencia/01-verificacion-final.txt) · [02-ci-run-40151865521](./evidencia/02-ci-run-40151865521.txt).

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Ningún `Color(0x…)`/`Colors.*`/`fontSize:` suelto en `diseno_flutter` | `python3 scripts/verificar_frontend.py diseno` | PASS (`TODO OK`) · CI f1 success |
| H1.S1.M2 | `formatearPorcentaje` en `dinero/formatear.dart` con 2 pruebas | `flutter test test/unidad` | PASS (104 = 102 + 2) |
| H1.S2.M1/M2 | Siete archivos ≤ 200 líneas con `part`/`part of` (patrón de `capa_de_bloqueo_*`), API pública intacta | barrido + `dart analyze --fatal-infos` + `flutter test` (unidad/widget 104, a11y 14, goldens 18) | PASS |
| H1.S3.M1 | `apps/movil`: `rutas.dart` 220→149 y `pantalla_sorteo.dart` 203→196 | barrido `movil` · CI: f2, «app · pruebas», «app · análisis», «app · formato» | PASS (en el CI; local solo barrido y formato) |
| H1.S3.M2 | `tabla-de-datos.ts` 205→177; tipos reexportados | `ng lint` + barrido `ui` · `tsc` · `ng test` de la tabla | PASS (15 pruebas) · CI f1/f2/f3/f4 success |
| H2.S1.M1 | Regresión del paquete y de `apps/movil` | locales + CI f2/f3/«app ·» | PASS |
| H2.S1.M2 | CI real sobre la rama | run 40151865521 | f1 pasa; el job sigue rojo por f5 (ver Pendiente) |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| **f5** · `apps/web/pruebas/e2e/catalogo.spec.ts:48` «todo lo que se toca mide al menos 44 píxeles»: 9 elementos más bajos (enlaces «Inicio»/«Sistemas», botones «Notas de la vista», «Ayuda sobre Estado» ×2, «Vista general», «Columnas 18», «Gobierno», «Ayuda sobre Nota de revisión») | TODO | Es de los componentes `ui` de Atlas del 18/10 (`b6ea08a6`, `dfad6d48`). Arreglarlo es CSS con efecto visual y no se puede medir sin construir `apps/web` (faltan los clientes generados aquí): decisión/verificación con el navegador del equipo. |
| f6 (e2e backoffice) y f7 (humo) | TODO | Hoy no corren porque f5 falla; pueden esconder más rojos. |
| `AdmisionDelGrupoSustitucionTest` intermitente | TODO | Reproducirlo; si es arranque en frío de Mockito, acotar concurrencia (no subir el timeout, `aportaya.base.gradle.kts`). |
| Repo `PasanakuFrontend`: CI no arranca | BLOQUEADO | Ver «Decisiones y ambigüedades». |

## Evidencia
Ver los tres archivos de `evidencia/`. Resumen literal del CI real:

```text
Frontend — Flutter y Angular (ADR-044): f1 success · f2 success · f3 success · app·formato success ·
app·análisis success · app·pruebas success · app·accesibilidad success · f4 success · f5 FAILURE · f6/f7 skipped
Los cinco corredores: success   bootJar y docker build: success   Formato, reglas propias y compilacion: success
```

## No cubierto
- **Prueba visual** de los widgets cambiados: ningún golden existente los cubre (los 18 pasan sin cambios), así que el cambio de matiz y de tamaños **no está verificado visualmente**; sin capturas ni doble revisión (regla 35).
- f5, f6, f7 del CI; goldens de macOS; e2e del backoffice.
- 19b (gitleaks) y OSV-Scanner: rojos del run por lanzarlo con `workflow_dispatch` (escanea todo el historial); en PR/push el alcance es el del cambio. Se verán en el PR.

## Desvíos del plan
- Se agregó H1.S3 al descubrir que el CI cortaba en el primer fallo.
- Los literales se resolvieron con `Paleta` y no con tokens de rol nuevos ni `// permitido:`: tocar el sistema de tokens (4 temas, contraste, bóveda) es decisión de diseño.

## Riesgos residuales
- Cambios de matiz/tamaño: tarjeta de riesgo (cifra 34→40 px y «/ 100» 18→22 px), puntos de la mesa (44→40), grilla de métricas (rojo/verde/azul/ámbar/violeta a `Paleta`), medallas (bronce→`o600`, plata→`n400`, oro→`warn`, diamante→`info`, esmeralda→`ok`), acentos del tour. Blanco y el morado de privacidad quedan idénticos.
- No pude ejecutar el formateador Spotless/palantir en las ediciones de Java de otro trabajo (ver su reporte); en este trabajo no se tocó Java.

## Decisiones y ambigüedades
- Mapeo de colores a `Paleta`; división con `part` vs. archivos públicos. A confirmar con diseño / mantenedor del paquete.
- **PasanakuFrontend (privado):** todos sus jobs mueren a los 2–4 s sin runner ni pasos ni logs, en todos los workflows, desde al menos el 19/10, también en los PR de dependabot; PasanakuBackend (público) con los mismos workflows consigue runner. Es el patrón de un bloqueo de Actions de la cuenta (minutos/facturación de repos privados): **hipótesis no verificada**, el motivo está en la página del run y no es legible por API. Confirmar a: titular de la cuenta (Settings → Billing → Actions). Además su árbol es anterior y no contiene estos archivos.
