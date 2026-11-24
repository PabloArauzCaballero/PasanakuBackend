# Reporte — Contrato: alerta de origen y titular del caso (CU-44) y quién propuso/aprobó (CU-132)

> **AVANCE: 7 / 8 — 87,5 %.** Java escrito y formateado, pero sin compilar ni testear acá (Gradle local: 429 de Maven Central); lo verifica CI.

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/contrato-caso-parametros-20261124` (sin push)
- Peldaño: contratos `RUNS` (validate + verificadores); backend Java `WRITTEN`; backoffice `TESTED` + E2E contra el simulado con prueba visual (`VERIFIED_FUNCTIONAL_ONLY` respecto del backend real, que no corrió).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `VersionDeParametro` publica `propuestoPor` (uuid, opcional) y `aprobadoPor` (uuid \| null) | `openapi-generator validate -i tarifas.yaml` | PASS · `evidencia/contrato-y-gates.txt` |
| H1.S2.M1 | Ruta nueva `GET /cumplimiento/casos/{casoId}` (`obtenerCasoLft`) → `FichaDeCasoLft` + `AlertaDeOrigenLft` | `validate -i cumplimiento.yaml` | PASS |
| H1.S3.M1 | Postman regenerado, ejemplo del simulado curado, verificadores de deriva | `verificar_pruebas_web.py`, `verificar_contratos_limites.py`, `inventario_endpoints.py --check`, vitest simulado 384/384 | PASS |
| H2.S1.M1 | Clientes Angular regenerados (no versionados) | `ls clientes/angular/cumplimiento/model/fichaDeCasoLft.ts` | PASS |
| H2.S2.M1 | Resumen del caso con alerta de origen y titular enmascarado; estados cargando/error/sin alerta | specs + E2E caso 19/19 | PASS · `evidencia/frontend.txt` |
| H2.S3.M1 | Parámetros: «Propuso»/«Aprobó» en tarjetas e historial; sin aprobador dice por qué | spec + E2E parámetros 12/12 | PASS |
| H2.S4.M1 | Gates del backoffice | typecheck, lint, test:front 1361/1361, test:a11y 98/98 | PASS |

## A medias
### H1.S2.M2 — Repositorio, caso de uso, controlador y pruebas Java de la ficha (y mapeo de tarifas)
- Qué anda: código escrito siguiendo los vecinos (`BandejasLftRepositorio.ficha`, `ConsultarBandejasLft.ficha`, `MapeoDeLft.ficha`, `CumplimientoController.obtenerCasoLft`, `MapeoDeParametros`); firmas verificadas contra la interfaz Spring generada localmente con las plantillas del repo; formato estable con palantir-java-format 2.50.0.
- Qué no anda / no se sabe: no compiló ni corrió ningún test Java aquí.
- Qué falta exactamente: que CI corra `compileJava` (-Werror), `spotlessCheck`, `ArquitecturaTest`, `LftBandejaWebTest` (3 tests nuevos), `CU44BandejaTest` (2 nuevos, Postgres real) y `ParametrosWebTest` (aserciones cambiadas).
- Dónde quedó: commits `576770ad` (tarifas) y `424db313` (cumplimiento).

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| ninguna | — | — |

## Evidencia
- `evidencia/contrato-y-gates.txt` — validate, verificadores, Postman, formato, intento de Gradle.
- `evidencia/frontend.txt` — typecheck, lint, unitarias, a11y, E2E (incluye los dos rojos y su corrección).
- Capturas revisadas: `docs/trabajo/2026-11-23-continuar-traspaso/evidencia/contrato/` (caso: móvil/escritorio × claro/oscuro; parámetros: móvil claro, escritorio oscuro) y `evidencia/visual-caso-*.png` regeneradas.

## No cubierto
- Java sin compilar/ejecutar localmente (ver A medias).
- La ficha nunca se pidió a un backend real: los E2E usan el simulado (caso) y `page.route` (parámetros).
- La pantalla del caso sigue con causal/notificación/plazo locales: el contrato no publica esos pasos del debido proceso (fuera de alcance).

## Desvíos del plan
- El E2E de disposición móvil se puso rojo (el resumen creció); se compactó el titular a una línea.
- axe marcó `role` dentro del `<dl>`; se cambió por `aria-live` en el `<dd>`.

## Riesgos residuales
- `ParametrosWebTest` invierte aserciones previas («sin identidades»): cambio de requisito pedido por Pablo, no debilitamiento.
- Seudónimo de 4 caracteres: colisiones posibles entre titulares distintos; es solo una pista visual.

## Decisiones y ambigüedades (confirmar con Pablo)
- A1: el operador se publica como UUID (patrón de `aprobadoPor`/`registradoPor`); no hay nombre visible en ningún contrato.
- A2: titular = `****` + últimos 4 hex del `usuario_id`. Cumplimiento no tiene nombre ni documento (viven en identidad, regla 98.1). No se agregó rastro de lectura propio: no es un dato personal en claro; confirmar con cumplimiento.
- A3: «alerta de origen» = la más antigua enlazada al caso, y se publica el total de enlazadas.
- No se commitearon ejemplos del simulado de operaciones que ya faltaban antes (p. ej. `listarParametrosFinancieros`): no son de este cambio.
