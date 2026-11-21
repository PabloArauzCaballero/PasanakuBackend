# Plan — Tanda 2, lote L2 (garantía y entregas)

- Fecha: 2026-11-21 · Repo: `PasanakuBackend` · Base: `origin/dev` 50cf3be9 · Predecesor: plan de correcciones
  (`docs/trabajo/2026-11-21-plan-correcciones/PLAN.md`, H1 = F1, H2 = F2) y su anexo
  `anexos/ANEXO-L2-garantia-entregas.md`. Decisiones: `docs/trabajo/2026-11-20-auditoria-consolidado/DECISIONES.md`.
- Resultado observable: en `entregas` no queda camino para subastar un cupo ni comprar/vender el derecho a cobrar un
  turno (D-L2-1, tanda 7); el número de cuenta bancaria se cifra solo en el servidor; y las correcciones F1/F2 del
  anexo L2 que caben en `servicios/garantia` y `servicios/entregas` quedan en PR hacia `dev`.
- Kill-test: (1) `POST /api/v1/entregas/subastas` con un token válido devuelve algo distinto de 410; (2) un
  `POST /cuentas-bancarias` con `numeroCifrado` en el cuerpo se acepta; (3) `grep -rn 'class SubastaDelCupo' servicios`
  encuentra algo; (4) el piso de ramas de `entregas` (0.71) se baja para que el CI pase.

## Alcance

- IN: `servicios/garantia/**`, `servicios/entregas/**`; `docs/entidades/*.puml`, `contratos/**`, `pactos/**` solo
  de garantía y entregas; esta carpeta; filas de la sección de garantía/entregas de `docs/Restricciones.md`.
- OUT: `plataforma/**`, `apps/**`, `packages/**`, `clientes/**`, `sql/` a mano, `.github/**`, `build.gradle*` raíz,
  contratos y modelos de otros servicios. Lo que haga falta ahí se describe en el `REPORTE.md` y en el PR.
- Ambigüedades registradas:
  - AMB-1. Retiro de operaciones consumidas por otros (regla 98.1.3): la app móvil (L6) y `nucleo-financiero` (L1)
    todavía llaman a operaciones de subasta y mercado. Supuesto: **expandir/contraer**. En este lote las
    operaciones quedan en el contrato marcadas `deprecated` y responden `410` con código estable; el contrato
    no se achica hasta que los consumidores las suelten. Confirmar con Pablo en el PR.
  - AMB-2. Cesiones ya liquidadas en TEST (datos sintéticos, P-14): el fondeo sigue pagándole al cesionario de
    una cesión LIQUIDADA anterior al retiro, para no pagar dos veces el mismo turno. Confirmar con Pablo.

## H1 — Lote L2 (F1 y F2 en garantía y entregas)

**CA:** Dado `dev` con los PR del lote mergeados, cuando se busca la subasta, el mercado de turnos, el
`numeroCifrado` del cliente y las demás vías del anexo L2, entonces no existen o están cerradas con su prueba.
**DoD:** cada PR con CI verde y la salida pegada en `evidencia/`.
**Estado:** EN CURSO

### H1.S1 — Retiro de la cesión de turno (D-L2-1)

**CA:** Dado cualquier usuario, cuando intenta subastar, pujar, publicar o comprar un turno, entonces recibe 410
con código estable y no se escribe nada. **DoD:** `webTest` + `test` de entregas en verde; CI del PR en verde con
los pisos de cobertura sin cambios. **Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| L2.S1.M1 | Retirar subasta (CU-64 por subasta) y mercado de ofertas (CU-130): casos de uso, repositorios, adaptadores, trabajo programado, configuración y pruebas; las operaciones quedan `deprecated` y responden 410 | Las 10 operaciones responden 410 `AP-ENT-RETIRADA`; `grep -rln 'SubastaDelCupo\|GestionDeOfertas' servicios/entregas/src` vacío | `./gradlew :servicios:entregas:test :servicios:entregas:webTest` + grep | HECHO (evidencia/L2.S1.M1-retiro-cesion.txt; `integrationTest` lo corre el CI) |
| L2.S1.M2 | Contracción del contrato y del modelo (borrar las operaciones del OpenAPI y las tablas `subasta_cupo`, `puja_subasta`, `oferta_turno`, `cesion_derecho`) | Las operaciones y tablas no existen | Regeneración DDL + deriva | BLOQUEADO (espera a L6 y L1: consumidores) |

### H1.S2 — Seguridad (F1.L2)

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| F1.L2.M07 | `numeroCifrado` fuera del contrato de entrada; cifrado solo en servidor con clave propia | Un cuerpo con `numeroCifrado` → 400; ida y vuelta del cifrado coincide | `webTest` del rechazo + unitaria de ida y vuelta | A MEDIAS (contrato y cifrado en servidor HECHO; clave propia pendiente de `generar_compose.py`) |
| F1.L2.M05 | Retirar `POST /desembolsos/{id}/respuesta` (acreditación humana) | 410 y sin `desembolso_acreditado` | `webTest` + unitaria | TODO |
| F1.L2.M09 | Retirar `POST /entregas` legacy y `EFECTIVO_ORGANIZADOR` | 410 | `webTest` | TODO |
| F1.L2.M02 | Retirar la cobertura manual del fondo con datos del cuerpo | 410, sin escritura | `webTest` garantía | TODO |
| F1.L2.M03 | Devolución del fondo sin `grupoCerrado`/`deudasVivas` del cuerpo | Campos fuera del contrato | `webTest` | TODO |
| F1.L2.M10 | `saldoRetenido` fuera de la entrada del desembolso | Campo fuera del contrato | `webTest` | TODO |
| F1.L2.M01, M04, M06, M08, M11–M14 | Ver anexo; dependen de plataforma (token de servicio, cliente único) o del rediseño | — | — | TODO |

### H1.S3 — Retiro (F2.L2)

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| F2.L2.M09 | Mercado de ofertas | Se resuelve con L2.S1.M1 | — | HECHO (con L2.S1.M1) |
| F2.L2.M01 | Respaldo empresarial en entregas | `grep -rn Respaldo servicios/entregas/src` vacío | suites | TODO |
| F2.L2.M02 | Respaldo empresarial en garantía | Rutas `/garantia/respaldo/*` 410 | suites | TODO |
| F2.L2.M04 | Aval entre participantes | 410 | suites | TODO |

## Riesgos y bloqueos previstos

| Riesgo | Impacto | Mitigación |
|---|---|---|
| Pruebas con Testcontainers no corren acá (sin Docker) | La cobertura del CI usa `integrationTest` | Se mide en el CI del PR; localmente se corren `test` y `webTest` |
| Retenciones de pujas o compras en curso en TEST | Saldo sintético retenido sin liberar | Consulta previa al despliegue en el PR; liberación por la operación de nucleo |
| Consumidores fuera de alcance (app, nucleo) | Romper su build si se achica el contrato | Expandir/contraer (AMB-1) |
