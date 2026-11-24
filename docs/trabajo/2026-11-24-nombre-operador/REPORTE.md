# Reporte — Nombre del operador en «Propuso» y «Aprobó» (CU-132, backoffice)

> **AVANCE: 6 / 7 — 85,7 %.**

- Fecha: 2026-11-24 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/nombre-operador-20261124` (sin push)
- Peldaño de evidencia alcanzado: backoffice `TESTED` (spec + E2E contra doble declarado, capturas revisadas);
  identidad Java `WRITTEN` (no compilado ni ejecutado localmente: Gradle responde 429 de Maven Central; lo compila
  y corre la CI con `-Werror`). El trabajo queda en `WRITTEN` por su área más baja.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Contrato `POST /usuarios-internos/nombres` (`resolverNombresDeUsuariosInternos`) con lista blanca, `minItems 1`/`maxItems 50` | `openapi-generator-cli validate -i servicios/identidad/.../identidad.yaml` | PASS · `evidencia/H1.S1.M1-validate.txt` |
| H1.S1.M2 | Controlador (`@Permiso TARIFARIO_ADMINISTRAR` + alternativos `USUARIOS_INTERNOS_LEER`, `ACCESOS_ADMINISTRAR`), caso de uso y repositorio jOOQ | `python3 scripts/inventario_endpoints.py --check` | PASS · `evidencia/verificadores.txt` |
| H1.S1.M4 | Ejemplo del simulado, colecciones Postman | `generar_postman.py` sin diff; `vitest ejemplos-contra-esquema` 366/366 | PASS |
| H1.S2.M1 | Cliente Angular de identidad regenerado (local, ignorado por git) y `resolverNombresDeOperadores` | `yarn typecheck` (backoffice) | PASS (exit 0) |
| H1.S2.M2 | Pantalla: una llamada por carga, nombre o «Operador no disponible» | `ng test --include=…/parametros/*.spec.ts` | PASS 13/13 · `evidencia/H1.S2.M2-spec.txt` |
| H1.S2.M3 | E2E + 4 capturas revisadas | `yarn playwright test e2e/visual-parametros-financieros.e2e.ts --workers=1` | PASS 16/16 · `evidencia/H1.S2.M3-e2e.txt` |

## A medias
### H1.S1.M3 — Pruebas Java de identidad
- Qué anda: escritas `UsuariosInternosControllerWebTest` (200 solo `usuarioId`+`nombre`; permiso alternativo; 401;
  403 participante y sin permisos; 400 con 51 ids; 400 campo no declarado y lista vacía; 422 `AP-CU08-07`),
  `ResolverNombresDeUsuariosInternosTest` (orden, omisión, nombre vacío, repetidos) y un caso nuevo en
  `UsuariosInternosRepositorioTest` (Testcontainers: solo el operador ACTIVO sale; cliente, suspendido e
  inventado no). `verificar_pruebas_web.py` en 0. Formato palantir aplicado.
- Qué no anda: no se ejecutaron (Gradle local 429).
- Qué falta exactamente: que la CI corra `./gradlew :servicios:identidad:test` y spotless; si algo falla, corregirlo.
- Dónde quedó: commit del backend en la rama; código formateado, sin compilar localmente.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| ninguna | — | — |

## Evidencia
```text
$ python3 scripts/verificar_criterios.py → exit 0 · Sin divergencias entre la boveda y el codigo.
$ python3 scripts/verificar_boveda.py → exit 0 · TODO OK
$ python3 scripts/verificar_seguridad.py → exit 0 · TODO OK · 5 aviso(s)
$ python3 scripts/verificar_carriles.py → exit 0 · TODO OK (con avisos)
$ python3 scripts/verificar_maqueta.py → exit 0 · TODO OK
$ python3 scripts/verificar_contratos_limites.py → exit 0
$ python3 scripts/verificar_pruebas_web.py → exit 0 · La capa web esta cubierta segun ADR-043.
$ python3 scripts/inventario_endpoints.py --check → exit 0 · hallazgos: 0
$ python3 scripts/generar_postman.py && git diff --stat -- postman/ → (vacío)
$ yarn lint (backoffice) → exit 0
$ ng test parametros → Test Files 2 passed · Tests 13 passed
$ playwright visual-parametros-financieros → 16 passed (57.1s)
```
Capturas: `docs/trabajo/2026-11-23-continuar-traspaso/evidencia/nombre-operador/` (escritorio claro y oscuro,
móvil claro, nombre no disponible). Revisadas: nombres visibles en «Rige hoy», «Espera aprobación» e historial
(tabla y tarjetas); «Operador no disponible» en cursiva sin romper la pantalla; sin desborde horizontal.

## No cubierto
- Java sin compilar ni ejecutar localmente (ver A medias). La matriz negativa es de la capa web con el caso de
  uso doblado; la consulta real la cubre la prueba Testcontainers, tampoco ejecutada aquí.
- Spec y E2E corren contra un doble declarado de identidad (HttpTestingController / `page.route`) en tres niveles
  —correcto, límite (id omitido) e inválido (503)—; falta la integración real backoffice → gateway → identidad.
- Tablet no se capturó para el estado nuevo (sí lo cubre el E2E previo de la pantalla en tres viewports).
- `docs/auditoria-produccion/endpoints.md` no se regeneró: estaba desactualizado de antes (regenerarlo trae ~40
  filas ajenas). `--check` pasa.

## Desvíos del plan
- La pantalla superó el tope de 200 líneas del verificador de frontend: la resolución de nombres se extrajo a
  `parametros/nombres-de-operadores.ts` y sus pruebas a `pantalla-de-parametros-financieros.operadores.spec.ts`.

## Riesgos residuales
- Más de 50 operadores distintos en una carga: solo se resuelven los 50 primeros; el resto dice «no disponible».
- En pantallas táctiles el identificador interno no se ve (va en `title` y en texto para lectores de pantalla).
- Un operador suspendido después de firmar aparece como «Operador no disponible» (lo pide el diseño).

## Decisiones y ambigüedades
- **Lote > 50 → 400 `AP-VAL-01`, no 422** (pedido original). El contrato debe declarar `maxItems` (lo exige
  `verificar_contratos_limites.py`) y la validación del contrato corre antes del caso de uso, como en todo arreglo
  acotado del repo. Repetidos → 422 `AP-CU08-07` (nuevo, en la tabla de errores de CU-08 y en `identidad.yaml`).
  Confirmar con Pablo.
- **Formato del nombre: nombres y apellidos** (`ConsultarMiPerfil.nombreCompleto`), no «Nombre A.»
  (`nombreVisible`): el control de cuatro ojos necesita distinguir sin ambigüedad a dos colegas. Confirmar con Pablo.
- **Permiso**: `TARIFARIO_ADMINISTRAR` (quien usa la pantalla) con alternativos `USUARIOS_INTERNOS_LEER` y
  `ACCESOS_ADMINISTRAR`, que ya leen la ficha completa del personal y no ganan nada nuevo. `AUDITORIA_LEER` no se
  abrió: hoy no ve la pantalla de parámetros. Confirmar con Pablo si Auditoría la necesitará.
- El código `AP-CU08-07` se adscribió a CU-08 (identidad, roles de operador): CU-132 vive en `tarifas` y su
  verificador exigiría declararlo en `tarifas.yaml`.

## Seguridad (regla 90.6)
1. Amenaza: enumerar clientes o filtrar contacto de operadores por un endpoint de nombres.
2. Control: JOIN obligatorio con `usuario_interno` + `estado = 'ACTIVO'`
   (`NombresDeUsuariosInternosRepositorio`), respuesta de dos campos, omisión silenciosa, ids en el cuerpo y no en
   la URL, `@Permiso` en `UsuariosInternosController#resolverNombresDeUsuariosInternos`.
3. Tests: matriz negativa en `UsuariosInternosControllerWebTest` y Testcontainers en `UsuariosInternosRepositorioTest`.
4. Resultado: escritas; ejecución pendiente de la CI (ver A medias).
5. Riesgo residual: quien tiene `TARIFARIO_ADMINISTRAR` puede confirmar qué ids son operadores activos (no clientes).
