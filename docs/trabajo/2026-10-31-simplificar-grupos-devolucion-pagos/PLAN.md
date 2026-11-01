# Plan — Simplificar el alta de grupos, la devolución de dinero y el pago (propuesta de producto)

- Fecha: 2026-10-31 · Repos afectados: Pasanaku (solo `docs/`) · Predecesor:
  `docs/trabajo/2026-10-30-regla-sorteo-75-y-comision-por-turno/`
- Resultado observable: Pablo abre `PROPUESTA.md` y encuentra, para cada uno de los tres problemas que
  planteó (alta de grupo compleja, devolución compleja, pago complejo), qué pasos tiene hoy la persona,
  qué se propone sacar, qué regla automática lo reemplaza, qué piezas del repo ya existen para hacerlo y
  qué decisiones son suyas. Nada se implementa en este trabajo.
- Kill-test: `PROPUESTA.md` no existe, o no cita la decisión D-12 de ADR-051 (la que hoy prohíbe grupos
  sin organizador humano), o no distingue hecho de hipótesis.

## Alcance
- IN: este directorio (`PLAN.md`, `PROPUESTA.md`, `REPORTE.md`, `evidencia/`).
- OUT: cualquier `.java`, `.dart`, `.sql`, `.yaml`, `.puml`, seed o ADR. Los CU y `Restricciones.md` no se
  tocan: la propuesta se escribe aparte y recién cuando Pablo decida se abre el carril que los cambia.
  El carril «arenas e inicio 75» (`claude/arenas-y-inicio-75-20261030`, PRs #364/#365, ya mergeados en
  `test` y `dev`) lo lleva la sesión `pasanaku-c1`; acá solo se cita.
- Ambigüedades registradas:
  1. **«NO MANUAL, AUTOMÁTICO Y EN BASE A UN ALGORITMO»** — Supuesto: ninguna persona (ni el que arma el
     grupo ni backoffice) toma decisiones operativas del grupo: admitir, iniciar, sortear, cobrar,
     entregar y devolver las ejecuta el sistema con reglas deterministas; para desconocidos, el grupo lo
     arma el motor de emparejamiento (CU-68 §5-7), no una persona. Confirmar con Pablo.
  2. **«Devolución de dinero»** — Supuesto: abarca (a) sacar saldo de la billetera al banco, (b) irse de
     un grupo, (c) grupo que no arranca o se disuelve, (d) pago duplicado o erróneo. Se tratan los cuatro.
     Confirmar con Pablo cuál le duele más.
  3. **«Procesamiento de pagos»** — Supuesto: se refiere al recorrido recargar → aportar y a que todavía no
     hay pasarela real (el seed de proveedores apunta a `example.bo` y TEST usa `ClienteProveedorSimulado`).
     Confirmar con Pablo.
  4. **Grupo sin organizador humano** — ADR-051 D-12 lo prohibió como opción conservadora «sujeta a
     ratificación» y dejó escrito que si Producto lo quiere es «una decisión nueva y explícita». Pablo es
     Producto. La propuesta la plantea como ADR nuevo, no la da por tomada.

## H1 — Descubrimiento factual de los tres flujos
**CA:** Dado un lector sin acceso al chat, cuando abre `evidencia/hallazgos.md`, entonces encuentra cada
afirmación sobre el sistema actual con su ruta en el repo.
**DoD:** `grep -c "servicios/\|docs/\|apps/" evidencia/hallazgos.md` → ≥ 30 citas.
**Estado:** HECHO

### H1.S1 — Mapear lo que existe
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Alta de grupo: gate de organizador, contrato Aprendiz, formulario, admisión, firma, inicio, sorteo | Cada paso tiene CU, archivo y línea | `grep -c "^| [0-9]" evidencia/hallazgos.md` → ≥ 12 filas de pasos en §A (corregido: el umbral original «≥ 10 líneas con CU-20/90/68/69/60» dio 8 porque varios pasos citan el CU en la misma línea; ver REPORTE §Desvíos) | HECHO |
| H1.S1.M2 | Devolución: retiro de saldo, retiro de grupo, disolución, fondo, reembolso | Los cinco caminos citados con CU y clase | `grep -c "CU-11\|CU-18\|CU-65\|CU-67\|CU-29\|CU-19" evidencia/hallazgos.md` → ≥ 6 | HECHO |
| H1.S1.M3 | Pagos: recarga, aporte, pasarela, modo simulado | Camino y proveedor real identificados | `grep -c "CU-10\|CU-21\|CU-99\|Simulado" evidencia/hallazgos.md` → ≥ 4 | HECHO |

## H2 — Propuesta escrita
**CA:** Dado Pablo leyendo `PROPUESTA.md`, cuando llega al final, entonces sabe qué se saca, qué regla lo
reemplaza, qué ya existe en el repo para hacerlo y qué tiene que decidir él.
**DoD:** `grep -c "^| D[0-9]" PROPUESTA.md` → ≥ 6 (corregido: el patrón original `DECISION_REQUIRED\|Decisión D` dio 3 porque las decisiones se tabularon como `| D1 |…`; ver REPORTE §Desvíos) · `grep -c "Hipótesis\|hipótesis" PROPUESTA.md` → ≥ 3.
**Estado:** HECHO

### H2.S1 — Redacción
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | §1 diagnóstico con el recorrido actual paso a paso para los tres flujos | Tres tablas «hoy» con pasos numerados | `grep -c "^### 1\." PROPUESTA.md` → 3 | HECHO |
| H2.S1.M2 | §3 grupos: dos puertas (confianza y emparejamiento), reglas del algoritmo, inventario de piezas a tocar | Sección con tabla de reglas y tabla de piezas | `grep -c "organizadorId\|es_autogestionado" PROPUESTA.md` → ≥ 3 | HECHO |
| H2.S1.M3 | §4 devolución: cuatro casos con «hoy / propuesta / qué toca / quién decide» | Cuatro casos | `grep -c "^### 4\." PROPUESTA.md` → ≥ 4 | HECHO |
| H2.S1.M4 | §5 pagos: un toque, débito programado, enlace de pago, pasarela real | Sección con el bloqueo real nombrado | `grep -c "example.bo\|ClienteProveedorSimulado" PROPUESTA.md` → ≥ 1 | HECHO |
| H2.S1.M5 | §7 decisiones de Pablo numeradas, con recomendación cada una | Lista D1…Dn | `grep -c "^| D[0-9]" PROPUESTA.md` → ≥ 6 | HECHO |

## H3 — Decisión del dueño
**CA:** Dado el documento, cuando Pablo responde, entonces cada D1…Dn tiene un sí, un no o una variante.
**DoD:** respuestas registradas en `REPORTE.md` §Decisiones del trabajo que las implemente.
**Estado:** HECHO — Pablo, 2026-10-31: «Sí a todo, D1 a D10, arrancá con el ADR». Registrado en
`docs/Arquitectura/ADR-054 Grupos sin organizador humano.md` (carril
`docs/trabajo/2026-10-31-adr-054-grupo-sin-organizador/`). D9 se cumple sin columna nueva porque el
modelo ya tiene `es_autogestionado`, `tipo_conformacion` y `visibilidad` (ver ADR-054 §Motivo).

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Pablo decide D1…Dn | Cada decisión tiene respuesta | `grep -cE "^\| D[0-9]+ \|" "docs/Arquitectura/ADR-054 Grupos sin organizador humano.md"` → 10 (las diez, con «Cómo queda»; corregido: el patrón laxo `^| D[0-9]` daba 13 por las filas de §Ratificación, ver REPORTE §Desvíos) | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Revertir D-12 sin ADR | Dos fuentes de verdad sobre si puede existir un grupo sin organizador | La propuesta exige ADR nuevo que supersede D-12 parcialmente, antes de tocar código |
| Confundir «automático» con «sin control» | Cumplimiento rechaza el diseño | Cada regla automática cita el invariante que conserva (KYC, append-only, idempotencia, RN-18) |
| El carril de inicio al 75 % cambia a la vez | Propuesta desactualizada | Se cita el estado mergeado (`origin/test` 5c06f9e95) y se nombra a `pasanaku-c1` como dueño |
