# Reporte — Regla de inicio del sorteo (≥ 75 % y primera cuota) y comisión por turno

> **AVANCE: 6 / 6 — 100 %.**

- Fecha: 2026-10-30 · Plan: [PLAN.md](./PLAN.md) · Rama: `pablo/feat/backoffice-como-atlas-h0-h1-2026-10-18`, commit `7767060b` (el checkout compartido estaba en esa rama al commitear, no en `claude/traspaso-por-subasta` como mostraba el estado inicial de la sesión; solo se tocaron `.md` y un script de evidencia)
- Peldaño de evidencia alcanzado: trabajo **solo documental**; la escalera de código (regla 30) no aplica.
  Cada DoD es una observación sobre disco ejecutada y pegada en `evidencia/`. Ningún comportamiento del
  sistema cambió: **no se implementó nada**, y el reporte no dice lo contrario.

## Completado

| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `R-GRP-20` registrada en la familia R-GRP de `docs/Restricciones.md`: sorteo solo con grupo `CONFORMADO` (completo o iniciado al ≥ 75 % y achicado) y primer aporte acreditado por cupo | `grep -n "R-GRP-20" docs/Restricciones.md` | PASS · línea 1909 · `evidencia/h1-dod.txt` |
| H1.S1.M2 | CU-60: disparador, precondiciones 2 y 4, error `PRIMER_APORTE_PENDIENTE` (`AP-CU60-11` propuesto) y flujo alternativo 1b | `grep -c -E "75 ?%\|primer aporte\|PRIMER_APORTE_PENDIENTE" "docs/CasosDeUso/CU-60 Sortear los turnos.md"` | PASS · 5 coincidencias · `evidencia/h1-dod.txt`; tras la decisión final, `grep -c "PRIMER_APORTE_PENDIENTE\|nace al iniciar"` → 3 · `evidencia/h2-dod.txt` |
| H1.S1.M3 | Aviso de la regla a las 4 sesiones vivas de Pasanaku y coordinación con `pasanaku-c1`, dueña del 75 % | `SendMessage` × 6 | PASS · 6 × `success:true` · `evidencia/avisos-sesiones.md` |
| H2.S1.M1 | Modelo de margen por turno en `Decimal`, 5 escenarios × 5 tarifas, equilibrio con QR al 0,35 % y al 0 %, ingreso por ciclo | `python3 -I evidencia/modelo_comision.py` | PASS · `exit=0` · `evidencia/modelo_comision.txt` |
| H2.S1.M2 | Referencias externas con URL y fecha (Money Fellows, QR Bolivia, gestores de ROSCA, tandas MX); lo que no tiene cifra se declara | 4 `WebSearch` | PASS · sección 9 de `ANALISIS-COMISION.md` |
| H2.S1.M3 | `ANALISIS-COMISION.md`: recomendación **2 % de la bolsa por turno, piso Bs 10, techo Bs 300**, con justificación numérica, supuestos y qué confirmar | `head -3` · `grep "Recomendación en una línea" -A3` · `grep -c "^\|"` | PASS · 29 filas de tabla · `evidencia/h2-dod.txt` |

## A medias

ninguna

## Pendiente

| ID | Estado | Qué lo destraba |
|---|---|---|
| — Implementar `R-GRP-20` en código (`CU60Sortear.comprometer`, `puedeSortear`, `pantalla_del_grupo.dart`) | **Fuera de este alcance** · lo lleva `pasanaku-c1` como hito H3 de `docs/trabajo/2026-10-30-arenas-e-inicio-75/PLAN.md` | Que c1 cierre su H1 (inicio al 75 %). Usa `AP-CU60-11 PRIMER_APORTE_PENDIENTE`; si cambia, c1 actualiza CU-60 y `Restricciones.md` |
| — Adoptar el 2 % en el tarifario | `DECISION_REQUIRED` (Pablo) | Decidir: (a) corregir `seeders/minimos/04-tarifario.json` antes de que exista un grupo con tarifa congelada en producción, o (b) publicar tarifario v2 por CU-34 con preaviso. En TEST ya hay grupos con `tarifa_congelada_grupo` v1 |
| — PR a `dev` y `test` con estos `.md` | TODO | Decidir si va solo o junto con el carril de c1, que va a tocar el mismo CU-60 |

## Evidencia

- `evidencia/h1-dod.txt` — greps del DoD de H1 (R-GRP-20 en la línea 1909; 5 coincidencias en CU-60).
- `evidencia/h2-dod.txt` — encabezado y recomendación del análisis, conteo de tablas, `git status` y `git diff --stat` de los `.md` tocados.
- `evidencia/modelo_comision.py` / `.txt` — el modelo y su salida literal.
- `evidencia/avisos-sesiones.md` — ids de los 6 mensajes y resumen de lo que respondió `pasanaku-c1`.

```text
$ python3 -I evidencia/modelo_comision.py   (recorte)
cupos  aporte    bolsa |  tarifa  comisión neto imp. | costo push   margen
    6     500     3000 | 0.30%≤50     10.00      8.40 |      13.00    -4.60
    6     500     3000 |   2.00%     60.00     50.40 |      13.00    37.40
   20    1000    20000 | 0.30%≤50     50.00     42.00 |      72.50   -30.50
   20    1000    20000 |   2.00%    400.00    336.00 |      72.50   263.50
Punto de equilibrio ... 6 cupos × Bs 500 (bolsa 3000): p_equilibrio = 0.52 % (QR 0,35 %) · 0.10 % (QR 0 %)
exit=0
```

```text
$ git diff --stat -- docs/Restricciones.md "docs/CasosDeUso/CU-60 Sortear los turnos.md"
 docs/CasosDeUso/CU-60 Sortear los turnos.md | 18 +++++++++++++++---
 docs/Restricciones.md                       |  1 +
```

## No cubierto

- El contrato OpenAPI de `grupos` (`puedeSortear`, errores de CU-60) **no se tocó**: el código de error
  `AP-CU60-11` está propuesto en el `.md`, no en el `.yaml` ni en `CodigoError`. Lo agrega quien implemente.
- Ningún test, ningún build, ningún servicio corrido: no había código en alcance.
- Los costos del modelo son los **sembrados** (`14-proveedores-externos.json`, URLs `example.bo`), no los
  contratados. El margen real se verá en `costo_proveedor_operacion` cuando haya proveedor real.
- KYC, infraestructura, soporte y cumplimiento quedaron fuera del modelo porque no hay cifra en el repo.

## Desvíos del plan

- El plan nacía con la regla «relajar el 100 % a ≥ 75 %». A mitad del trabajo `pasanaku-c1` informó
  una decisión de Pablo tomada hoy: el organizador **inicia** el grupo al ≥ `ceil(0,75 × cupos)` y el
  grupo **se achica**; por lo tanto `AP-CU60-01` no se relaja y la única precondición nueva es el
  primer aporte. Los `.md` se escribieron con esa semántica y el plan se corrigió (ambigüedades 1–3
  marcadas RESUELTAS). No hubo dos definiciones de «iniciar».
- Se agregó al modelo la sensibilidad «QR al 0 %» porque las fuentes públicas indican que el QR
  interoperable no cobra al comercio; no estaba previsto en el DoD original de H2.S1.M1.

## Riesgos residuales

- **Conflicto de merge en CU-60**: `pasanaku-c1` va a editar el mismo archivo al implementar. Se le
  cedió la propiedad de esos dos `.md` a partir de ahora (mensaje `23241b58…`).
- **La tarifa vigente pierde plata**: con los supuestos del seed, el 0,3 % con techo Bs 50 deja margen
  negativo en 4 de 5 escenarios antes de costos fijos. Mientras no se decida, cada grupo que congele
  tarifa v1 en producción queda con esa comisión (`R-TAR-07`).
- La hipótesis «el organizador informal cobra una vuelta» no está verificada y condiciona la
  percepción de valor del 2 %.

## Decisiones y ambigüedades

| Qué | Decidido por | Estado |
|---|---|---|
| 75 % con `ceil`; el grupo se achica al iniciar; nadie entra después | Pablo, vía `pasanaku-c1` (AskUserQuestion, 2026-10-30) | Resuelta · registrada en `R-GRP-20` y CU-60 |
| La primera cuota nace en `iniciarGrupo`; el sorteo se habilita con todas `ACREDITADO` por webhook verificado | Pablo, vía `pasanaku-c1` | Resuelta · registrada en CU-60 precondición 4 |
| Código `AP-CU60-11` para `PRIMER_APORTE_PENDIENTE` | Propuesto acá (1–10 ocupados en `CodigoError.de(60, n)`) | Adoptado por c1; confirmar al implementar |
| **Porcentaje por turno: 2 %, piso 10, techo 300** | **Recomendación**, no decisión | `DECISION_REQUIRED` → Pablo. Alternativa documentada: tramos 2,0 / 1,75 / 1,5 % |
| Precio por posición del turno (modelo Money Fellows) | No recomendado para el lanzamiento | Fase 2, decisión de equidad de Pablo |
| Supuestos de costo (QR 0,35 %, ACH 2,50, 3 mensajes/cupo/período) | Seed del proyecto + supuesto propio | Confirmar con contrato real y con `15-eventos-y-plantillas.json` |
