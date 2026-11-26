> **AVANCE: 4 / 5 — 80,0 %.**

# Reporte — GU-C · cierre de lo que quedó sin cubrir

- Fecha: 2026-11-26 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-c-credencial` (desde `origin/dev` tras #522)
- Peldaño de evidencia alcanzado: **TESTED** (enfriamiento, unitario con repositorio doblado) · UI: **WRITTEN** (spec visual escrito, corrida en curso, sin capturas miradas)

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | OpenAPI de identidad procesado por el generador | `./gradlew :servicios:identidad:generarServidorOpenApi --rerun-tasks` | BUILD SUCCESSFUL |
| H1.S2.M1 | Criterios CU-04 del enfriamiento (AP-CU04-11) con prueba | `python3 scripts/verificar_criterios.py; python3 scripts/verificar_boveda.py` | «Sin divergencias» / TODO OK |
| H1.S2.M2 | `CU04StepUp.abrir` rechaza dentro del enfriamiento (`aportaya.acceso.recuperacion.enfriamiento-factor`, PT24H) | `./gradlew :servicios:identidad:test :servicios:identidad:webTest :plataforma:comun-pruebas:testBarrido :plataforma:comun-pruebas:contractTest` | BUILD SUCCESSFUL |
| H1.S4.M1 | Plan de #522 con estado real: 18/23 HECHO | diff de `2026-11-25-gu-c/PLAN.md` | 18/23 |

## A medias
### H1.S3.M1 — Capturas UI
- Qué anda: cliente Angular generado en local (`generateOpenApiClients`); `apps/backoffice/e2e/visual-cuenta.e2e.ts` escrito (invitación datos/inválida, recuperar, recuperaciones datos/vacío/error, cambiar clave; 3 viewports × 2 temas; movimiento reducido).
- Qué no anda: la corrida de Playwright no terminó; ninguna captura se miró todavía.
- Qué falta exactamente: correr el spec (`--workers=1`), mirar las capturas, corregir lo que muestren, y revisar consola y red.
- Dónde quedó: solo el spec; compila no verificado.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| GU-17 (fecha de nacimiento del alta) | DECISION_REQUIRED | Pablo: columna nullable (modelo + ADR) o pedir la fecha real en el alta |
| H6 de #522 (5 microtareas de UI) | A MEDIAS | las capturas de H1.S3.M1 |

## Evidencia
Salidas citadas en la tabla de Completado.

## No cubierto
- El enfriamiento solo se probó con el repositorio doblado; la consulta SQL real (`factorReinscritoDesde`) no corrió contra base (integración solo en CI).
- No hay prueba de integración del flujo completo reinscribir → step-up rechazado.

## Desvíos del plan
Ninguno.

## Riesgos residuales
- El enfriamiento solo se aplica al abrir el desafío de step-up, no a otras acciones `requiere_mfa` fuera de él.

## Decisiones y ambigüedades
- A6: enfriamiento post-reinscripción PT24H (supuesto, igual al de A1); confirmar con Pablo / cumplimiento.
- A7: GU-17 requiere decisión de negocio/modelo.
