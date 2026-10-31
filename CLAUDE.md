# CLAUDE.md — Pasanaku

Las instrucciones del proyecto viven en `AGENTS.md` y en `.claude/rules/`. Este archivo solo
declara lo que la regla 70 exige dejar escrito acá.

## Concurrencia de agentes (regla 70.1.2 / 70.1.3)

Autorizado por Pablo el 2026-10-30:

- **Hasta 4 subagentes simultáneos**, con lotes **disjuntos en archivos** (regla 70.4.9). Dos
  agentes nunca escriben el mismo archivo.
- Los subagentes de un workflow orquestado pueden correr en background mientras la sesión
  principal espera su resultado y lo integra antes de cerrar el turno.
- Builds, typecheck y suites corren **de a uno por herramienta** (70.1.4, ampliado por Pablo el
  2026-10-30 para ir más rápido): como máximo un build de Gradle/Java y uno de Flutter/Dart a la
  vez. No comparten daemon, caché ni puertos.
- **Sin cambios** en el resto de la regla 70: un solo navegador de test (70.1.5) y un servidor
  por puerto (70.1.6).
