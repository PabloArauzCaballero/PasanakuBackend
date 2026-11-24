# Plan — Nombre del operador en «Propuso» y «Aprobó» (CU-132, backoffice)

- Fecha: 2026-11-24 · Repos afectados: monorepo (servicios/identidad, apps/backoffice) · Predecesor: cb651d9f (contrato `propuestoPor`/`aprobadoPor`)
- Resultado observable: en Configuración › Parámetros financieros, «Propuso» y «Aprobó» muestran el nombre del operador interno en vez del UUID; si no se puede resolver, un texto claro («Operador no disponible»).
- Kill-test: abrir la pantalla con el E2E y ver un UUID en la celda «Propuso».

## Alcance
- IN: operación nueva y aditiva `POST /usuarios-internos/nombres` en identidad (contrato, controlador, caso de uso, repositorio jOOQ, pruebas web y de repositorio); ejemplo del simulado; inventario/postman regenerados; dominio + pantalla del backoffice (spec y E2E con capturas).
- OUT: contrato de tarifas (no cambia); app móvil; `listarUsuariosInternos` (expone correo/teléfono).
- Ambigüedades registradas:
  - Lote > 50: el contrato declara `maxItems: 50` (lo exige `verificar_contratos_limites.py`) y la validación del contrato responde **400 AP-VAL-01** antes del caso de uso, como todo arreglo acotado del repo; el pedido decía 422. Supuesto: 400 por contrato; duplicados → 422 desde el caso de uso. Confirmar con Pablo.
  - Formato del nombre: nombres + apellidos (`ConsultarMiPerfil.nombreCompleto`), no «Nombre A.»: el control de cuatro ojos necesita distinguir sin ambigüedad a dos colegas. Confirmar con Pablo.
  - Solo operadores ACTIVOS: uno suspendido después de firmar se mostrará como «Operador no disponible» (lo pide el diseño). Confirmar con Pablo.

## H1 — El backoffice nombra a quien propuso y a quien aprobó
**CA:** Dado un parámetro con versión propuesta y aprobada por dos operadores internos, cuando un administrador del tarifario abre la pantalla, entonces ve los nombres de ambos y nunca un UUID suelto.
**DoD:** verificadores del repo en 0 · spec del componente y E2E en verde · capturas revisadas.
**Estado:** A MEDIAS

### H1.S1 — Identidad resuelve nombres por lote
**CA:** Dado un operador con permiso de backoffice, cuando pide nombres de ≤50 ids, entonces recibe solo `{usuarioId, nombre}` de usuarios internos activos.
**DoD:** validación OpenAPI + verificadores; pruebas Java escritas (Gradle local no funciona: la CI las compila y corre).
**Estado:** A MEDIAS

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Contrato `resolverNombresDeUsuariosInternos` | El YAML valida y el esquema tiene lista blanca y límites | `java -jar openapi-generator-cli validate -i identidad.yaml` → «No validation issues» | HECHO |
| H1.S1.M2 | Controlador + caso de uso + repositorio jOOQ | Implementa la interfaz generada con `@Permiso` + alternativos | `python3 scripts/inventario_endpoints.py --check` → 0 | HECHO |
| H1.S1.M3 | Prueba web con matriz negativa y prueba de repositorio | 401/403/400/422 y omisión de clientes cubiertos | `python3 scripts/verificar_pruebas_web.py` → 0 (ejecución Java: CI) | A MEDIAS — pruebas escritas y `verificar_pruebas_web.py` en 0; sin ejecutar (Gradle local 429): las corre la CI |
| H1.S1.M4 | Ejemplo del simulado, Postman e inventario regenerados | Sin diff tras regenerar | `python3 scripts/generar_postman.py && git diff --stat postman/` | HECHO |

### H1.S2 — La pantalla usa los nombres
**CA:** Dado el catálogo cargado, cuando la pantalla pinta «Propuso»/«Aprobó», entonces muestra el nombre o «Operador no disponible» si la resolución falla.
**DoD:** spec + E2E + capturas.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Cliente Angular regenerado y función de dominio `resolverNombresDeOperadores` | Tipos del cliente compilan | `yarn tsc -p apps/backoffice/tsconfig.app.json --noEmit` → 0 | HECHO |
| H1.S2.M2 | Pantalla: una llamada por carga, nombre o texto de respaldo | Spec cubre nombres resueltos y fallo | `yarn vitest run …parametros…spec` → verde | HECHO |
| H1.S2.M3 | E2E con capturas en `evidencia/nombre-operador/` | 4 capturas revisadas | `yarn playwright test e2e/visual-parametros-financieros.e2e.ts` → verde | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Gradle local no funciona (429) | El Java no se compila localmente | Copiar patrones vecinos; declarar el peldaño WRITTEN para Java; CI compila con -Werror |
| Simulación de los tres niveles (regla 65) | El backend real no corre localmente | Doble `page.route`/HttpTestingController en tres niveles: correcto (nombres), límite (id omitido), inválido (503) |
