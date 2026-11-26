> **AVANCE: 5 / 5 — 100,0 %.**

# Reporte — GU-C · cierre de lo que quedó sin cubrir

- Fecha: 2026-11-26 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-c-credencial` (desde `origin/dev` tras #522)
- Peldaño de evidencia alcanzado: **TESTED** (enfriamiento, unitario con repositorio doblado) · UI: **VERIFIED_FUNCTIONAL_ONLY→visual** con backend simulado declarado (42 pruebas pasan, 48 capturas, miradas por muestreo)

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | OpenAPI de identidad procesado por el generador | `./gradlew :servicios:identidad:generarServidorOpenApi --rerun-tasks` | BUILD SUCCESSFUL |
| H1.S2.M1 | Criterios CU-04 del enfriamiento (AP-CU04-11) con prueba | `python3 scripts/verificar_criterios.py; python3 scripts/verificar_boveda.py` | «Sin divergencias» / TODO OK |
| H1.S2.M2 | `CU04StepUp.abrir` rechaza dentro del enfriamiento (`aportaya.acceso.recuperacion.enfriamiento-factor`, PT24H) | `./gradlew :servicios:identidad:test :servicios:identidad:webTest :plataforma:comun-pruebas:testBarrido :plataforma:comun-pruebas:contractTest` | BUILD SUCCESSFUL |
| H1.S3.M1 | Capturas de `/acceso/*` con datos sintéticos, backend simulado con `page.route` (declarado) | `npx playwright test e2e/visual-cuenta.e2e.ts --workers=1 --project=chromium --trace off` | 42 passed; consola sin errores inesperados; 48 capturas en `.playwright/backoffice/visual/cuenta/` (gitignored) |
| H1.S4.M1 | Plan de #522 con estado real: 18/23 HECHO | diff de `2026-11-25-gu-c/PLAN.md` | 18/23 |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| GU-17 (fecha de nacimiento del alta) | DECISION_REQUIRED | Pablo: columna nullable (modelo + ADR) o pedir la fecha real en el alta |
| H6.S1.M2 de #522 (invitación) | A MEDIAS | falta capturar errores por campo, paso del QR TOTP y estado de carga |

## Evidencia
Salidas citadas en la tabla de Completado.

## No cubierto
- Capturas: no se ejercitaron los errores por campo, el paso del QR TOTP de la invitación, el estado de carga ni los diálogos de aprobar/rechazar; se miraron 14 de 48 capturas (las demás tienen el mismo diseño por viewport/tema y las aserciones de consola pasaron).
- Hallazgo menor de UX: con el filtro «Pendiente de decisión» por omisión, el vacío de recuperaciones muestra el texto genérico de filtros, no el `textoVacio` propio (sigue siendo accionable: «Quitar filtros»). No se corrigió (fuera de alcance).
- Backend simulado: las capturas no prueban integración con el servicio real.
- El enfriamiento solo se probó con el repositorio doblado; la consulta SQL real (`factorReinscritoDesde`) no corrió contra base (integración solo en CI).
- No hay prueba de integración del flujo completo reinscribir → step-up rechazado.

## Desvíos del plan
Ninguno.

## Riesgos residuales
- El enfriamiento solo se aplica al abrir el desafío de step-up, no a otras acciones `requiere_mfa` fuera de él.

## Decisiones y ambigüedades
- A6: enfriamiento post-reinscripción PT24H (supuesto, igual al de A1); confirmar con Pablo / cumplimiento.
- A7: GU-17 requiere decisión de negocio/modelo.
