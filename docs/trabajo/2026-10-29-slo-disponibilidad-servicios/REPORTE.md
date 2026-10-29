# Reporte — Disponibilidad 30d, presupuesto de error y última interrupción en Sistemas › Servicios

> **AVANCE: 10 / 10 — 100,0 %.** Verificado en TEST con datos reales; falta solo la captura visual (`VERIFIED_FUNCTIONAL_ONLY`).

- Fecha: 2026-10-29 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/slo-disponibilidad-20261029` (PR #222 → dev, mergeado; PR #223 → test)
- Peldaño de evidencia: backend en `TESTED` (regresión del módulo verde contra Postgres real); frontend en
  `TESTED` (specs). `VERIFIED` queda pendiente del despliegue en TEST.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `disponibilidad_servicio_dia` en el modelo; DDL generado | `python3 scripts/generar_ddl.py` | PASS, exit 0 · `evidencia/M1-generar-ddl.txt` |
| H1.S1.M2 | Esquema aplicado en una base desechable; jOOQ y auditoria compilan | `psql -f sql/aplicar.sql` + `gradlew :servicios:auditoria:compileJava` | PASS, exit 0 · `evidencia/M2-compile.txt` |
| H1.S1.M3 | UPSERT idempotente y resumen de la ventana contra Postgres real | `gradlew :servicios:auditoria:integrationTest --tests '*DisponibilidadRepositorioTest'` | PASS, 4/4 · `evidencia/H1.S1.M3-integracion.txt` |
| H1.S2.M1 | Job por minuto que pasa la sonda al registro con la hora de antes de consultar | `gradlew :servicios:auditoria:test --tests '*MuestreoDeDisponibilidadTest'` | PASS, 1/1 · `evidencia/H1.S2-unitarios.txt` |
| H1.S2.M2 | Cálculo de disponibilidad y presupuesto (correcto, límite e inválido) | `--tests '*DisponibilidadDelServicioTest' '*SaludDeServiciosTest'` | PASS, 7/7 y 3/3 |
| H1.S3.M1 | Contrato con campos opcionales y controlador | `gradlew :servicios:auditoria:webTest --tests '*SaludDeServiciosWebTest' '*SeguridadWebTest'` | PASS, 4/4 y 6/6 · `evidencia/H1.S3-webtest.txt` |
| H1.S3.M2 | La pantalla formatea los campos y deja «—» si no hay medición | `npx ng test --include='src/app/rutas/sistemas/**/*.spec.ts'` + `tsc -p tsconfig.app.json` | PASS, 170/170, tsc exit 0 · `evidencia/H1.S3.M2-frontend.txt` |
| H1.S4.M1 | Narrativa de la tabla y cifra de tablas en los planes | `verificar_narrativas.py`, `verificar_boveda.py` | PASS · `evidencia/verificadores-ci.txt` |
| H1.S4.M2 | «Última interrupción» se ordena por instante | spec de sistemas | PASS |
| H1.S3.M3 | En TEST, auditoria muestrea cada minuto y el endpoint devuelve los campos | login real + `GET /api/v1/indicadores/operacion/servicios` | PASS, 200 con `disponibilidad30d`, `presupuestoErrorRestante`, `ultimaInterrupcion` · `evidencia/H1.S3.M3-endpoint-test.txt` |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
Ninguna.

## Evidencia
```text
regresión auditoria (evidencia/regresion-auditoria.txt)
test: tests=55 fallas=0 errores=0 omitidos=0
webTest: tests=75 fallas=0 errores=0 omitidos=0
integrationTest: tests=70 fallas=0 errores=0 omitidos=0

primer intento de integración (FAIL, corregido):
PSQLException: column "ultimo_minuto_en" is of type timestamp with time zone but expression is of type character varying
→ causa: con SQL plano el driver manda las fechas como texto; fix: casts ?::date / ?::timestamptz
```

## No cubierto
- `ultimaInterrupcion` llega como `null` (no ausente) cuando nunca hubo una; el backoffice lo trata igual («—»).
- Con 11 minutos medidos, los reinicios del despliegue pesan mucho: varios servicios muestran presupuesto negativo. Es lo medido; se normaliza a medida que se acumulan los 30 días.
- Prueba visual (capturas por viewport y tema): la estructura de la pantalla no cambió, solo el
  texto de tres celdas. Queda pendiente igual: `VERIFIED_FUNCTIONAL_ONLY` como máximo hasta capturarla.
- Con varias instancias de auditoria la protección es ShedLock más la escritura condicionada. La
  carrera entre dos instancias no se ejercitó, solo la repetición secuencial del mismo minuto.

## Desvíos del plan
- Primero se planeó una muestra por minuto con purga. Se cambió a un resumen por día porque los
  roles de servicio no tienen DELETE (`sql/00_base/03_permisos.sql:12`). El cambio está anotado en el PLAN.
- Regenerar `catalogo-operaciones.json` arrastraba unas 1.500 líneas de otras operaciones: estaba
  desactualizado desde el 2026-10-20. Se revirtió y queda fuera de alcance, porque la CI no lo verifica.

## Riesgos residuales
- La disponibilidad solo cuenta lo medido. Los primeros días la ventana tiene menos de 30 días de datos;
  el contrato expone `minutosMedidos30d` para que se vea cuánto hay.
- Prettier marca los archivos de Sistemas desde antes de este cambio. No se reformateó.

## Decisiones y ambigüedades
- Fuente: muestras en auditoria, y SLO de 99,5 % para todos los servicios. **Las decidió Pablo el 2026-10-29.**
- Supuesto: «Última interrupción» es el último minuto no ARRIBA, lo que incluye DEGRADADO. Confirmar con Pablo.
- Los minutos sin medición no cuentan ni a favor ni en contra. Confirmar con Pablo.
