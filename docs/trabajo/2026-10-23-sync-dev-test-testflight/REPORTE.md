# Reporte — Unificar dev y test y relanzar TestFlight

> **AVANCE: 4 / 4 — 100,0 %.**

- Fecha: 2026-10-23 · Plan: [PLAN.md](./PLAN.md) · Rama(s): dev, test
- Peldaño de evidencia alcanzado: subida `VERIFIED` hasta App Store Connect (aceptada); el build no se abrió en un teléfono.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | PR #185 test→dev mergeado | `gh pr merge 185 --merge` | PASS |
| H1.S1.M2 | PR #186 dev→test mergeado | `gh pr merge 186 --merge` | PASS; `git diff --stat origin/dev origin/test` vacío |
| H2.S1.M1 | Runner.xcarchive de `origin/test` b4ba8263, build 202610231045 | `bash scripts/testflight_ios.sh` (pasos 1-2) | PASS tras regenerar clientes Dart |
| H2.S1.M2 | Build subido a App Store Connect | `xcodebuild -exportArchive … -authenticationKey*` | `EXPORT SUCCEEDED` · [evidencia/subida.txt](./evidencia/subida.txt) |

## A medias
ninguna

## Pendiente
ninguna (el procesado en TestFlight lo hace Apple en unos minutos)

## Evidencia
Ver `evidencia/subida.txt` (clave y emisor de ASC no se pegan).

## No cubierto
- El build no se instaló ni abrió en un dispositivo.
- No se corrieron tests de movil; el cambio entre el build anterior (f7bab6ac) y este solo tocó `packages/ui` (web).

## Desvíos del plan
- El primer intento falló por clientes Dart viejos copiados de otro árbol; se borraron y se regeneraron con Java 21.
- `pub.dev` dio un corte de red transitorio en el primer intento; se reintentó sin cambios.
- `test` queda un commit de merge adelante de `dev`, sin diferencia de contenido (inherente a merges por PR).

## Riesgos residuales
- Xcode sigue sin sesión de Apple: cada subida depende de la clave ASC en `~/.appstoreconnect`.

## Decisiones y ambigüedades
- La subida la lanzó Pablo con `!`, porque la clave ASC no está en el entorno del agente.
