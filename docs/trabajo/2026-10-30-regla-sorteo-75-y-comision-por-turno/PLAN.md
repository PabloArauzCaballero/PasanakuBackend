# Plan — Regla de inicio del sorteo (≥ 75 % y primera cuota) y comisión por turno

- Fecha: 2026-10-30 · Repos afectados: Pasanaku (solo `docs/`) · Predecesor: `docs/trabajo/2026-10-20-detalle-firma-y-sorteo/`
- Resultado observable: quien abra `docs/Restricciones.md` o el CU-60 encuentra la regla nueva del dueño
  («solo se sortea con el grupo completo o ≥ 75 % de cupos ocupados y la primera cuota pagada por todos»)
  como restricción con código, no como mensaje de chat; y quien abra `ANALISIS-COMISION.md` encuentra
  una recomendación de porcentaje por turno con el modelo de costos reproducible por script.
- Kill-test: `grep -n "R-GRP-20" docs/Restricciones.md` vacío, o `ANALISIS-COMISION.md` sin tabla de
  costos por tamaño de grupo.

## Alcance
- IN: `docs/Restricciones.md` (fila nueva `R-GRP-20`), `docs/CasosDeUso/CU-60 Sortear los turnos.md`
  (disparador, precondiciones, flujo alternativo), este directorio (análisis, evidencia, reporte),
  aviso a las sesiones concurrentes de Pasanaku.
- OUT: cualquier `.java`, `.dart`, `.sql`, `.yaml` de contrato o seed. La implementación del cambio en
  `CU60Sortear.comprometer` (`AP-CU60-01`), `ConsultarDetalleDelGrupo.puedeSortear` y
  `pantalla_del_grupo.dart` la lleva la sesión dueña del carril de grupos. Cambiar el 0,3 % del seed
  `04-tarifario.json` exige decisión de Pablo + CU-34 (preaviso), no se toca acá.
- Ambigüedades registradas:
  1. **Redondeo del 75 %** — RESUELTA: `ceil(0,75 × cupos)`, decisión de Pablo tomada hoy por la
     sesión `pasanaku-c1` (`docs/trabajo/2026-10-30-arenas-e-inicio-75/PLAN.md`).
  2. **Qué pasa con el 25 % de cupos vacíos** — RESUELTA por la misma decisión: el organizador inicia el
     grupo y este **se achica** a los cupos ocupados (`cupos_totales` y `num_periodos` = ocupados);
     nadie más entra. Por eso `AP-CU60-01` (cupos completos) no se relaja: tras el inicio el grupo está
     al 100 % de su tamaño nuevo.
  3. **«Primera cuota» antes del sorteo invierte el orden actual** — RESUELTA (decisión de Pablo vía
     `pasanaku-c1`, 2026-10-30): la primera cuota nace al iniciar el grupo (`iniciarGrupo` abre el
     cobro del período 1 para cada cupo del grupo achicado) y el sorteo se habilita cuando todas
     están acreditadas por webhook verificado. El código lo implementa `pasanaku-c1` como hito H3 de
     `docs/trabajo/2026-10-30-arenas-e-inicio-75/PLAN.md`, con el código `AP-CU60-11`
     `PRIMER_APORTE_PENDIENTE` propuesto acá.
  4. Los costos de proveedores del seed (`14-proveedores-externos.json`) apuntan a `example.bo`: son
     supuestos del proyecto, no tarifas contratadas. El análisis los declara como tales.

## H1 — La regla del sorteo queda registrada como restricción
**CA:** Dado un lector sin acceso al chat, cuando abre `Restricciones.md` y el CU-60, entonces encuentra la
regla con su código, su motivo y el error que la hace cumplir.
**DoD:** `grep -n "R-GRP-20" docs/Restricciones.md` → 1 fila · `grep -n "75" "docs/CasosDeUso/CU-60 Sortear los turnos.md"` → precondición y flujo alternativo.
**Estado:** HECHO

### H1.S1 — Documentar y difundir
**CA:** Regla en docs y aviso entregado a las sesiones concurrentes.
**DoD:** los tres comandos de la tabla.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Fila `R-GRP-20` en la familia R-GRP de `Restricciones.md` | La fila existe con regla, motivo y CU | `grep -n "R-GRP-20" docs/Restricciones.md` → 1 línea (línea 1909, `evidencia/h1-dod.txt`) | HECHO |
| H1.S1.M2 | CU-60: disparador, precondiciones 2 y 4, error `PRIMER_APORTE_PENDIENTE` y fila en flujos alternativos | El CU describe el umbral del 75 % y la primera cuota | `grep -c -E "75 ?%|primer aporte|PRIMER_APORTE_PENDIENTE" "docs/CasosDeUso/CU-60 Sortear los turnos.md"` → 5 (`evidencia/h1-dod.txt`) | HECHO |
| H1.S1.M3 | Aviso a las 4 sesiones de Pasanaku vivas (`pasanaku-58`, `-ab`, `-c1`, `-20`) | 4 envíos con `success:true` | ids de mensaje en `evidencia/avisos-sesiones.md` | HECHO |

## H2 — Recomendación de comisión por turno con modelo de costos reproducible
**CA:** Dado el modelo de costos del repo (tarifario v1, proveedores, IVA/IT), cuando se corre el script,
entonces imprime margen por turno para 4 tamaños de grupo y muestra si el 0,3 % actual cubre el costo.
**DoD:** `python3 -I evidencia/modelo_comision.py` → tabla sin error · `ANALISIS-COMISION.md` cita cada
número de entrada con su ruta en el repo o su URL externa.
**Estado:** HECHO

### H2.S1 — Modelo y recomendación
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Script `evidencia/modelo_comision.py` con entradas tomadas del seed (0,35 % QR, Bs 2,5 ACH, Bs 0,28 WhatsApp, IVA 13 %, IT 3 %, piso 10 / techo 50) | Imprime margen por turno para 5 escenarios y 5 porcentajes, más equilibrio con QR 0,35 % y 0 % | `python3 -I evidencia/modelo_comision.py` → `exit=0`, salida en `evidencia/modelo_comision.txt` | HECHO |
| H2.S1.M2 | Contraste con referencias externas (plataformas ROSCA, costo QR Bolivia) con URL y fecha | Cada referencia tiene URL y fecha de consulta; lo no verificable se marca hipótesis | sección 9 de `ANALISIS-COMISION.md` (4 búsquedas, 2026-10-30); Oraan/StepLadder/Esusu y apps mexicanas sin cifra → declarado | HECHO |
| H2.S1.M3 | `ANALISIS-COMISION.md` con recomendación, estructura (porcentaje + piso + techo), supuestos y qué confirmar | El documento cierra con un porcentaje recomendado y su justificación numérica | `head -3` + `grep "Recomendación en una línea"` → 2 % / piso 10 / techo 300; 29 filas de tabla (`evidencia/h2-dod.txt`) | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Otra sesión edita `Restricciones.md` o CU-60 a la vez | Conflicto de merge | Aviso enviado pidiendo no tocar esos `.md` |
| Los costos del seed no son los contratados | El porcentaje recomendado se basa en supuestos | Declarado; el análisis muestra sensibilidad al costo del QR |
| El 75 % cambia el ciclo del grupo (turnos sin dueño) | Decisión de producto no tomada | Registrada como `DECISION_REQUIRED`, no se implementa |
