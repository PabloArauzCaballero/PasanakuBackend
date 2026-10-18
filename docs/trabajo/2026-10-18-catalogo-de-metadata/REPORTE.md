# Reporte — Catálogo de metadata del backoffice, con estado y revisión como el portal de Atlas

> **AVANCE: 11 / 12 — 91,7 %.** H1, H2 y H3 HECHO. H4 (verificación en TEST) en curso: depende del CI de #137 y de la reconstrucción del host.

- Fecha: 2026-10-18 · Plan: [PLAN.md](./PLAN.md) · Ramas: `pablo/feat/catalogo-de-metadata-2026-10-18` (#137 → test), `pablo/feat/catalogo-de-metadata-dev-2026-10-18` (#138 → dev)
- Peldaño de evidencia alcanzado:
  - Backend `auditoria`: `TESTED` (CU130Test contra PostgreSQL real, web test del contrato, regresión del servicio).
  - Backoffice: `VERIFIED_FUNCTIONAL_ONLY` + prueba visual contra un **doble declarado** de auditoria (e2e), capturas miradas en 3 anchos × 2 temas. La integración con el backend real se verifica en TEST (H4).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Tabla `auditoria.ficha_gobierno_tabla` desde el modelo PlantUML (estado, revisión, responsable, retención, crítica, nota, versión), con CU-130 en la bóveda | `python3 scripts/generar_ddl.py` · `verificar_boveda.py` | PASS · `evidencia/H1-S1-M1-esquema.txt` (338 tablas, «TODO OK») |
| H1.S1.M2 | Permiso `CATALOGO_DATOS_EDITAR` (con MFA) para ADMIN_PLATAFORMA | `generar_semillas.py` ×2 | PASS · `evidencia/H1-S1-M2-semillas.txt` (mismo md5) |
| H1.S1.M3 | Guardado con la versión como condición de la escritura | `integrationTest --tests '*CU130Test'` | PASS 5/5 · `evidencia/H1-cu130-integracion.txt` |
| H2.S1.M1 | Contrato aditivo `gobierno` + `politicasDeRetencion` y `PUT …/gobierno`; clientes y Postman regenerados | `generarClientes` · `generar_postman.py` | PASS |
| H2.S1.M2 | GET superpone la ficha (o activa/detectada por omisión) | `webTest` (`FichaDeGobiernoWebTest`) | PASS 4/4 |
| H2.S1.M3 | PUT: 200 / 403 / 400 / 422 AP-CU130-01·02·03 y bitácora `CAMBIO_CONFIGURACION` | regresión de auditoria | PASS · `evidencia/H2-regresion-auditoria.txt` (test 34, barrido 2, web 51, integración 55; `verificar_criterios` sin divergencias) |
| H3.S1.M1 | Dominio de gobierno: filtros, cifras, cuerpo del PUT | `ng test` | PASS 3/3 |
| H3.S1.M2 | Lista estilo Atlas: cifras, filtros por esquema/estado/revisión/módulo, chips de estado y revisión primero | e2e + capturas | PASS · `evidencia/capturas/cu130-lista-*` |
| H3.S1.M3 | Ficha con sección «Gobierno de la tabla» editable (sin doble envío, 422 explicado, borrador conservado, solo lectura sin permiso) | `seccion-de-gobierno.spec.ts` + e2e | PASS 4/4 + 3/3 · `cu130-ficha-*` |
| H3.S1.M4 | «Catálogo de datos» en el menú (grupo Control) | `test:front` | PASS 839/839, lint, tsc, `verificar_frontend.py` · `evidencia/H3-backoffice.txt` |

## A medias
### H4.S1.M1 — Verificado en TEST
- Qué anda: todo lo anterior, en local y en CI.
- Qué no anda: todavía no está desplegado; TEST sigue sin la tabla nueva.
- Qué falta exactamente: CI de #137 en verde → merge → el host reconstruye `esquema`, `auditoria` y backoffice → cambiar el estado de una tabla en `backoffice.aportaya…` y recargar.
- Dónde quedó: #137 (test) y #138 (dev), ramas subidas a los dos remotos; compila.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Revisión por columna (Atlas la tiene) | TODO | Fuera del alcance de este trabajo; siguiente paso si Pablo lo confirma |
| Catálogos operativos de valores con versiones (`operations/catalogs` de Atlas) | TODO | Confirmar que no era esto lo pedido (ambigüedad 1) |

## Evidencia
Índice en `evidencia/`: `H1-S1-M1-esquema.txt`, `H1-S1-M2-semillas.txt`, `H1-cu130-integracion.txt`, `H2-webtest-auditoria.txt`, `H2-regresion-auditoria.txt`, `H3-backoffice.txt`, `H3-e2e-cu130.txt`, `capturas/cu130-*` (lista y ficha en celular, tablet y escritorio, claro y oscuro; filtrada; guardada; solo lectura).

```text
$ ./gradlew :servicios:auditoria:test :servicios:auditoria:testBarrido :servicios:auditoria:webTest :servicios:auditoria:integrationTest
BUILD SUCCESSFUL in 1m 30s
test: 34 tests, 0 fallas · testBarrido: 2 · webTest: 51 · integrationTest: 55
$ python3 scripts/verificar_criterios.py --servicio auditoria
Sin divergencias entre la boveda y el codigo.
$ yarn test:front   (apps/backoffice)
Test Files 127 passed (127) · Tests 839 passed (839)
```

## No cubierto
- La pantalla se probó en e2e contra un doble de auditoria; la integración real queda para H4 en TEST.
- No se probó el 401 por sesión vencida dentro del formulario (lo maneja el interceptor común, no esta pantalla).

## Desvíos del plan
- Errores de regla con 422 (convención del proyecto, `ManejadorGlobalDeErrores`), no 409/404 como decía el plan.
- Se agregó AP-CU130-03 (política de retención inexistente), no previsto.
- La sección de gobierno se subió al principio de la ficha y las columnas de estado y revisión van segundas, tras mirar las capturas: en la primera versión quedaban cortadas.
- El shell en celular desbordaba 14 px en Linux con el tercer enlace del menú: `.lateral` usaba `1fr` y `.menu` tenía `min-width: auto`; corregido con `minmax(0, 1fr)` y `min-width: 0`.
- `ap-lista-de-opciones` (cumplimiento/compartido) recibió `textoVacio` para que un filtro diga «Todos».
- `generar_boveda.py` actualizó fichas de la bóveda que estaban atrasadas respecto de commits anteriores (p. ej. `cuenta_contable.moneda`); es salida del generador, no cambio de modelo.

## Riesgos residuales
- **Hallazgo, fuera de alcance:** el colapso a tarjetas de `@aportaya/ui/tabla-de-datos` desborda por debajo de ~340 px en tablas con nombres de columna largos (p. ej. `usuario`: 34 px a 320). En la ficha se agregó `overflow-wrap: anywhere` (a 360 px ninguna desborda; `pago` y `obligacion_aporte` tampoco a 320). El resto queda para `packages/ui`.
- Una tabla renombrada en el modelo deja su ficha huérfana (la fila sigue con el nombre viejo); el catálogo no la muestra. Impacto bajo; se ve al revisar.
- `marketing-ideas` (pedido de Pablo como referencia de diseño) es una skill de estrategias de adquisición, no de diseño de interfaz: no se aplicó al backoffice.

## Decisiones y ambigüedades
1. **«Catalogación de la metadata, los elementos activos»** se tomó como el catálogo de datos con ficha de gobierno (pantalla «Catálogo de datos» de Atlas), no como los catálogos operativos de valores. Confirmar con Pablo.
2. **Sensibilidad derivada, no editable** (en Atlas son banderas editables): una sola fuente de verdad, las columnas del modelo. Confirmar con Pablo.
3. **Permiso nuevo** `CATALOGO_DATOS_EDITAR` en lugar de reusar `CATALOGO_EDITAR` («Editar catálogos regulatorios»), para no cambiar su significado.
