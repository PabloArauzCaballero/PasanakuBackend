# Reporte — Simplificar el alta de grupos, la devolución y el pago (propuesta de producto)

> **AVANCE: 9 / 9 — 100 %.**

- Fecha: 2026-10-31 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/adr-054-grupos-sin-organizador-20261031`
  (solo `docs/`; commiteada junto con el ADR) · Propuesta: [PROPUESTA.md](./PROPUESTA.md)
- Peldaño de evidencia alcanzado: `DISCOVERED`. Es un trabajo de análisis y propuesta: no se escribió ni
  ejecutó código. Toda afirmación sobre el sistema actual está citada con ruta en `evidencia/hallazgos.md`.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Recorrido actual del alta de grupo, 12 pasos con CU y archivo | `grep -c "^| [0-9]" evidencia/hallazgos.md` | PASS: 18 filas (12 de §A + 6 de §D) — `evidencia/dod.txt` |
| H1.S1.M2 | Cinco caminos de devolución con CU y clase | `grep -c "CU-11\|CU-18\|CU-65\|CU-67\|CU-29\|CU-19" evidencia/hallazgos.md` | PASS: 6 — `evidencia/dod.txt` |
| H1.S1.M3 | Recorrido de pagos y ausencia de pasarela real | `grep -c "CU-10\|CU-21\|CU-99\|Simulado" evidencia/hallazgos.md` | PASS: 5 — `evidencia/dod.txt` |
| H2.S1.M1 | §1 diagnóstico con tres tablas «hoy» | `grep -c "^### 1\." PROPUESTA.md` | PASS: 3 — `evidencia/dod.txt` |
| H2.S1.M2 | §3 grupos: dos puertas, reglas, inventario | `grep -c "organizadorId\|es_autogestionado" PROPUESTA.md` | PASS: 3 — `evidencia/dod.txt` |
| H2.S1.M3 | §4 devolución: cinco casos | `grep -c "^### 4\." PROPUESTA.md` | PASS: 5 — `evidencia/dod.txt` |
| H2.S1.M4 | §5 pagos con el bloqueo real nombrado | `grep -c "example.bo\|ClienteProveedorSimulado" PROPUESTA.md` | PASS: 1 — `evidencia/dod.txt` |
| H2.S1.M5 | §7 decisiones D1…D10 con recomendación | `grep -c "^| D[0-9]" PROPUESTA.md` | PASS: 10 — `evidencia/dod.txt` |
| H3.S1.M1 | Pablo decidió D1…D10 («Sí a todo, D1 a D10, arrancá con el ADR», 2026-10-31); registradas en ADR-054 (escrito como 053 y renumerado al commitear: `test` ya tenía un 053) | `grep -cE "^\| D[0-9]+ \|" "docs/Arquitectura/ADR-054 Grupos sin organizador humano.md"` | PASS: 10 — `evidencia/dod.txt` (quinta corrida; el patrón laxo `^| D[0-9]` daba 13, ver Desvíos) |

## A medias
Ninguna.

## Pendiente
Ninguna. La implementación sigue en `docs/trabajo/2026-10-31-adr-054-grupo-sin-organizador/` y en los
carriles que el ADR-054 enumera en «Lo que falta en el código».

## Evidencia
- `evidencia/hallazgos.md` — cada hecho con ruta (archivo y línea cuando la hay).
- `evidencia/dod.txt` — salida literal de los comandos de la tabla de Completado.

## No cubierto
- Ninguna lectura se verificó ejecutando: no se corrió backend, app ni pruebas. Peldaño `DISCOVERED`.
- Débito programado (CU-13) y enlace de pago rápido (CU-99 §5): leídos en los CU, no buscados en código.
- El carril de inicio al 75 % se leyó en `origin/test` (nombres de clases), no se leyó su contenido.
- No se consultó a Cumplimiento ni a Legal: las hipótesis sobre R-ORG-06 y titularidad por proveedor
  quedan como tales.

## Desvíos del plan
- Dos DoD del plan se corrigieron después de correrlos, y queda constancia (regla 20 §6.7):
  - H1.S1.M1 pedía «≥ 10 líneas con CU-20/90/68/69/60» y dio **8**: varios pasos citan dos CU en la
    misma línea. El criterio real era «los 12 pasos tabulados»; el DoD pasó a contar filas de tabla.
  - El DoD de H2 pedía «≥ 6 coincidencias de `DECISION_REQUIRED\|Decisión D`» y dio **3**: las
    decisiones se tabularon como `| D1 |…`, que ya cuenta el DoD de H2.S1.M5 (10). Se unificó el patrón.
  El contenido de los documentos no se cambió para pasar; se cambió el comando porque medía mal.
  Las salidas originales (8 y 3) quedan en la primera corrida de `evidencia/dod.txt`.
- El DoD de H3.S1.M1 (`^| D[0-9]` sobre el ADR) dio **13**, no 10, porque tres filas de la sección
  «Ratificación pendiente» del ADR empiezan con «D8», «D7» y «D5». El reporte anterior decía 10 por error;
  se corrigió el patrón a `| Dn |` exacto (el mismo que usa el carril del ADR) y da 10. Las salidas 13 quedan
  en la tercera y cuarta corrida de `evidencia/dod.txt`.
- El ADR se escribió como 053 y se renumeró a 054 al commitear, porque `origin/test` ya tenía un ADR-053.
  Las referencias de este carril se actualizaron; las salidas viejas de `evidencia/dod.txt` no.

## Riesgos residuales
- La propuesta revierte parcialmente ADR-051 D-12. Si se implementa sin el ADR nuevo, el repo queda con
  dos verdades sobre si puede existir un grupo sin organizador.
- La mayor fricción restante (aprobación manual del KYC básico de cada participante) queda fuera: es de
  Cumplimiento, no de producto. Está anotada en `PROPUESTA.md` §6.
- Nada de lo propuesto cobra dinero real hasta que exista una pasarela contratada (CU-99).

## Decisiones y ambigüedades
- **Respuesta del dueño (2026-10-31): sí a D1…D10.** Una precisión posterior al descubrimiento: D9 se
  recomendó como columna `tipo` nueva, pero el modelo ya tiene `es_autogestionado`, `tipo_conformacion` y
  `visibilidad` con `CHECK` y código; el ADR-054 la cumple con esas columnas (regla 97.4.6) y se lo
  informa a Pablo en el cierre.
- Las cuatro ambigüedades del `PLAN.md` (interpretación de «automático», qué devolución, qué pago, y que
  el grupo sin organizador es decisión de Producto) se tomaron con el supuesto declarado y se le
  confirman a Pablo.
- Hipótesis declaradas en `PROPUESTA.md` §9 (R-ORG-06, titularidad por proveedor, CU-13/CU-99 sin
  verificar en código).
- No se tocó ningún CU, ADR, `Restricciones.md`, código ni seed.
