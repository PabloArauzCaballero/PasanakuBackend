---
tags:
  - plan
  - informe
  - carril
  - frontend
titulo: "Carril B3 — contabilidad ERP (Angular)"
ola: nuevo
fase: F13
mundo: Angular
modulo: apps/backoffice/src/app/rutas/contabilidad
rama: pablo/feature/carril-B3-contabilidad
estado: cerrado
---

# Carril B3 — contabilidad ERP

**Fase** F13 · **Mundo** Angular · **Casos de uso** CU-100–106 · carril nuevo (deuda adelantada, decisión del 2026-08-18)

> Ficha en [[18 Fichas de carril · las 38 unidades de trabajo]] · `F13`.
> Posee `apps/backoffice/src/app/rutas/contabilidad/`. Corrió sobre el `dev` ya fusionado
> con el shell (F6), `packages/ui` congelado, y B1/B2/B5 (no los tocó — `git diff --stat`
> contra `nucleo/`, `layout/`, `app.routes.ts` y las otras rutas queda vacío).

## Qué está hecho, con la salida que lo prueba

| Entregable | Evidencia | Estado |
| --- | --- | :-: |
| Seis pantallas: períodos, presupuesto, compras (con `TablaDeFacturas`/`TablaDeOrdenes`), cobros, activos, estados financieros | `find apps/backoffice/src/app/rutas/contabilidad` → 6 pantallas + `dominio/` con 7 módulos (uno por CU) + `textos.ts` | ✅ |
| `contabilidad.routes.ts` — enchufe por dominio, `canMatch` con `requierePermiso` real del shell | 6 entradas, permisos `CONTABILIDAD_ERP_*` por pantalla | ✅ |
| `yarn workspace @aportaya/backoffice lint` | `TODO OK` — sin red en vista, sin literal de diseño, sin formato de dinero manual, sin console, ningún archivo > 200 líneas | ✅ |
| `yarn workspace @aportaya/backoffice typecheck` | exit 0, sin salida | ✅ |
| `yarn workspace @aportaya/backoffice test:front` | **26 archivos, 122 pruebas, todas verdes** | ✅ |
| `yarn workspace @aportaya/backoffice test:a11y` | **18 archivos, 22 pruebas, todas verdes** (axe) | ✅ |
| `yarn workspace @aportaya/backoffice build` | compila; warning preexistente de presupuesto (+19,21 kB sobre 300 kB), heredado del resto del backoffice fusionado, no de este carril | ✅ |

## Decisiones tomadas, y por qué

- **Período cerrado, irreversible por diseño (CU-100).** `dominio/cu100-periodos.ts` centraliza
  la regla: un período con `cerradoEn` no nulo no admite ningún asiento nuevo. La pantalla
  no ofrece el botón de asentar — lo consulta a esa única función, para que la regla no se
  repita ni se desalinee entre pantallas.
- **El hash de un estado financiero nunca se recalcula en el cliente (CU-106).** El backend
  genera el documento y devuelve `hashContenido`; una segunda llamada (o la cabecera
  `X-Hash-Contenido`, con hueco declarado hasta que exista) permite comparar, nunca
  recomputar. Si el cliente rehiciera el hash, dejaría de probar que el documento es el
  que generó el backend — es la razón de ser de CU-106, no un detalle de implementación.
- **Paginado y ordenado en el cliente, explícitamente, hasta que el backend lo tenga.**
  `contrato-erp.ts` documenta que `erp.yaml` no publica ninguna operación `GET` de listado
  (ver huecos abajo) y que el filtrado ocurre en el adaptador, nunca dentro de
  `TablaDeDatosVirtualizada` — el organismo compartido sigue sin saber de HTTP ni de
  memoria, tal como lo dejó el carril B.

## Supuestos declarados

- Las tres rutas que necesitan saber cuál es el ejercicio o el período vigente
  (`periodo/:ejercicioId`, `activos/:periodoVigenteId`, `estados/:periodoId`) lo llevan en
  la URL, porque `erp.yaml` no publica un endpoint de "período vigente" — mismo criterio
  que ya usa `cumplimiento` con sus expedientes. Permite además pegar el enlace de un mes
  concreto.
- La redirección de `''` apunta a un ejercicio de demostración fijo, igual que `operacion`
  con su cuenta de ejemplo.

## Huecos encontrados, no completados con una suposición

**Pedido al carril de backend `5A` (`erp`).** `erp.yaml` publica quince operaciones, catorce
`POST` y un `GET` de validación de plantilla — **ninguna es de listado**. Sin `GET
/erp/periodos`, `/presupuestos`, `/ordenes-de-compra`, `/facturas-de-proveedor`,
`/cuentas-por-cobrar`, `/activos` ni `/estados-financieros`, las seis pantallas (que son en
su mayoría listados) no tienen qué mostrar en producción. Documentado con la forma exacta
esperada en `dominio/contrato-erp.ts`, para que cuando el backend las publique el único
archivo que cambie sea ese.

**Cabecera `X-Hash-Contenido` de CU-106**, para que el backend repita el hash junto al
documento binario y el cliente pueda comparar sin una segunda llamada — hoy la función que
la lee existe pero la cabecera todavía no la manda ningún servicio real.

## Matriz de gates

| Área | Gate | Evidencia | Estado |
| --- | --- | --- | :-: |
| Especificación | Cada pantalla sale de CU-100 a CU-106 | 6 pantallas ↔ 6 CU, 1 a 1 | ✅ |
| Dinero | Todo importe por `Monto`; el tipo `Importe` es cadena decimal, nunca `number` | grep negativo de `toFixed`/`Intl.NumberFormat` en el carril: vacío | ✅ |
| Período cerrado | La pantalla no ofrece asentar en un período cerrado | `cu100-periodos.spec.ts` | ✅ |
| Hash del estado financiero | El cliente nunca recalcula, solo compara | `cu106-estados-financieros.spec.ts` | ✅ |
| Contrato | Huecos declarados con la forma exacta pedida, no inventados como si existieran | `contrato-erp.ts`, comentario de cabecera | ✅ |
| Diseño | Cero literales; piezas del paquete, no locales | `lint` → `TODO OK` | ✅ |
| Accesibilidad | axe sin violaciones serias | `test:a11y` 22/22 | ✅ |
| Arquitectura | Shell intacto, sin tocar rutas de otros carriles | `git diff --stat` contra `nucleo/`, `layout/`, `app.routes.ts`, `operacion/`, `cumplimiento/`, `sistemas/` → vacío | ✅ |
| Entrega | Lint, tipos, pruebas, build | citado arriba | ✅ |

### Frases prohibidas sin evidencia

En vez de «está listo»: *6 pantallas, 122 pruebas de front y 22 de a11y en verde, build sin
errores, dos huecos de contrato declarados con la forma exacta pedida al backend.*

## Lo que queda abierto, y de quién es

| Qué | De quién | Cuándo |
| --- | --- | --- |
| Publicar los siete `GET` de listado en `erp.yaml` | carril de backend `5A` | antes de que esto sirva contra el backend real |
| Cabecera `X-Hash-Contenido` en la respuesta de descarga | carril de backend `5A` | idem |
| Goldens/capturas contra la maqueta (no se corrió `verificar_maqueta.py` en esta pasada) | quien retome el carril | siguiente punto de sincronización |

## Ver también

[[informe]] · [[16 Carriles de frontend]] · [[18 Fichas de carril · las 38 unidades de trabajo]] · [[13 Fases F6 a F8 · Backoffice]]
