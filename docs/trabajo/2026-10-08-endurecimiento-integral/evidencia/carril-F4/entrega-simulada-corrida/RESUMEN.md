# Entrega simulada — 20261010-085541

- Alcance pedido: `frontend` · commit: `0b2de6a6d59a6b95c1c866c211d9216ad380bf74` · cambios sin confirmar: 192 archivos
- node v22.23.1 · yarn 4.18.0 · Python 3.14.2
- Resultado: **TODOS LOS PASOS EN VERDE**

| # | Paso | Estado | Tiempo | Salida literal |
|---|---|---|---|---|
| 01 | preflight | PASS | 1 s | `01-preflight.log` |
| 02 | tokens | PASS | 635 s | `02-tokens.log` |
| 03 | contenido-sitio | PASS | 1 s | `03-contenido-sitio.log` |
| 04 | simulado-build | PASS | 2 s | `04-simulado-build.log` |
| 05 | simulado-tipos | PASS | 2 s | `05-simulado-tipos.log` |
| 06 | simulado-lint | PASS | 3 s | `06-simulado-lint.log` |
| 07 | simulado-pruebas | PASS | 3 s | `07-simulado-pruebas.log` |
| 08 | rutas-vs-contratos | PASS | 0 s | `08-rutas-vs-contratos.log` |
| 09 | backoffice-tipos | PASS | 6 s | `09-backoffice-tipos.log` |
| 10 | backoffice-lint | PASS | 5 s | `10-backoffice-lint.log` |
| 11 | backoffice-pruebas | PASS | 13 s | `11-backoffice-pruebas.log` |
| 12 | backoffice-a11y | PASS | 12 s | `12-backoffice-a11y.log` |
| 13 | web-tipos | PASS | 5 s | `13-web-tipos.log` |
| 14 | web-lint | PASS | 4 s | `14-web-lint.log` |
| 15 | web-pruebas | PASS | 9 s | `15-web-pruebas.log` |
| 16 | web-a11y | PASS | 7 s | `16-web-a11y.log` |
| 17 | aceptacion-mock | PASS | 56 s | `17-aceptacion-mock.log` |

## Límites (qué NO prueba esta entrega)

- Toda la parte web corre contra el **doble con estado** y Prism (`VERIFIED_FUNCTIONAL_ONLY`): no es integración con los servicios reales.
- La aceptación en modo `real` no corre: `sembrarReal` (apps/backoffice/e2e/aceptacion/apoyo/siembra-real.ts) no está implementado.
- Flutter (apps/movil) no entra en este recorrido: sus pruebas se corren en una máquina con el SDK.
- No hay lector de pantalla real: la accesibilidad es axe-core + teclado + capturas inspeccionadas.
- Sin proveedor de pagos, banco, SIN ni UIF reales: cualquier parámetro comercial o fiscal es sintético (`apto_produccion=false`).
- Los secretos de este recorrido son sintéticos y se generan en el momento; nada de esto sirve en un entorno real.
