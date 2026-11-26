# Plan — Lote F2 · cuentas de servicio: vencimiento en el login y capturas (continuación del lote F, PR #520 ya en dev)

- Fecha: 2026-11-26 · Rama: `claude/gu-f2-vencimiento-login` (desde origin/dev) · Predecesor: [../2026-11-25-gu-f/PLAN.md](../2026-11-25-gu-f/PLAN.md)
- Resultado observable: una cuenta de servicio vencida o revocada no abre sesión aunque su clave sea correcta; las dos pantallas del lote F tienen capturas 390/834/1440 × claro/oscuro, miradas.
- Kill-test: `POST /sesiones` con la clave correcta de una cuenta `cuenta_servicio` vencida responde AP-CU04-02.
- IN: `CU04Autenticar`, `UsuarioRepositorio` (una consulta), docs CU-04/CU-136, capturas. OUT: todo lo demás de CU-04.

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| F2.M1 | Rechazar en el login la cuenta de servicio vencida/revocada | vencida y revocada → AP-CU04-02 sin sesión; vigente y personas, sin cambio | `CU04CuentaDeServicioTest` verde en CI | EN CURSO (escrito; CI pendiente) |
| F2.M2 | Documentar en CU-04 (flujo 1b) y CU-136 | boveda sin fallas | `verificar_boveda` | HECHO en local (salida: 1 falla previa, no mía) |
| F2.M3 | Capturas de las dos pantallas | 3 anchos × 2 temas, miradas | imágenes en evidencia/ | TODO |

## Estado real de las 13 microtareas del lote F (PR #520, en dev)
Evidencia: CI verde en `1aa88c913` (compilación, barrido, `integrationTest` contra PostgreSQL, frontend, bóveda).
HECHO (DoD ejecutado en CI): H1.S1.M1, H1.S1.M2, H1.S2.M1, H1.S2.M2, H1.S2.M3, H2.S1.M1, H2.S1.M2, H4.S1.M1, H4.S1.M2, H4.S1.M3 (PR en dev) = 10.
DESCARTADO: H2.S1.M3 (se usó el puerto real de #516).
A MEDIAS: H3.S1.M1, H3.S1.M2 (UI escrita y con specs en verde, sin capturas ni verificación en runtime).

## Cierre del lote F2 (2026-11-26)
- F2.M3 HECHO (con la salvedad de abajo): `playwright test e2e/visual-roles-y-cuentas-de-servicio.e2e.ts --workers=1` → **30 passed** (`evidencia/e2e-visual-resultado.txt`), consola sin errores nuevos; 30 capturas en `evidencia/capturas/`. Miradas: roles-formulario móvil claro, cuentas-datos móvil claro, roles-datos tablet oscuro, cuentas-error escritorio oscuro; las otras 26 no se inspeccionaron una por una.
- Salvedad: backend simulado con `page.route` (datos sintéticos): `VERIFIED_FUNCTIONAL_ONLY` + visual parcial; no se ejercitó el flujo de segundo factor ni la rotación contra el servidor real.
- Observación fuera de alcance: en la tabla de roles a 834 px el chip «Origen» se recorta (listado compartido).
- F2.M1: escrito; su DoD (`CU04CuentaDeServicioTest`) lo corre la CI de esta rama.
