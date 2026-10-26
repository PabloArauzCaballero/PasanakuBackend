# Franja A — shell y navegación (H1)

> **AVANCE: 0 / 12 — 0 %.** (se actualiza al cerrar cada microtarea)

- Worktree `/home/user/wt-a`, rama `claude/franja-a`, base `c9ac3966`. Servidor `127.0.0.1:4301`.
- Plan raíz: `../PLAN.md` (F1, F2, A1). Guía: `../GUIA-DE-FRANJA.md`.

## Microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Medir latencia «antes» (CDP 150 ms + CPU ×4) | tabla por entrada del menú | `e2e/apoyo/medir-latencia-del-menu.mjs` → `evidencia/franja-a/latencia-antes.json` | TODO |
| H1.S1.M2 | Requisitos de acceso declarativos por pantalla (F1, generalizado) | con rol `oficial` «Sistemas» no aparece; con `PLATAFORMA` abre `/sistemas/servicios`; ninguna pestaña visible rebota | spec `nucleo/menu.spec.ts` + spec de coherencia menú⇔router | TODO |
| H1.S1.M3 | Recorte A1: ≤ 7 entradas, ≤ 8 pestañas, sin duplicados, URLs viejas siguen andando | conteos | spec `nucleo/menu.spec.ts` | TODO |
| H1.S1.M4 | Pestañas con ícono y subpestañas de grupo | cada pestaña con `ap-icono` | spec `pestanas-de-seccion.spec.ts` | TODO |
| H1.S1.M5 | Precarga en reposo de lo que la sesión puede abrir (F2) | chunks pedidos antes del clic; nada de lo que la guardia corta | spec de la estrategia + medición «después» | TODO |
| H1.S1.M6 | Ayuda en el menú de usuario; «Mi perfil» sin rebote | ítems con ícono que navegan sin rebote | spec + recorrido navegador | TODO |
| H1.S2.M1 | Recorrido de botones del shell a 1280 y 375 | hamburguesa, buscador, campana, menú usuario, tutorial, Salir (200 y 500) | script de recorrido → `evidencia/franja-a/recorrido-shell.json` | TODO |
| H1.S2.M2 | Ingreso CU-04: aceptado / límite / error | mensajes accionables, sin doble envío | spec + recorrido navegador | TODO |
| H1.S2.M3 | Ayuda: cada tarjeta/botón | navega | recorrido navegador | TODO |
| H1.S3.M1 | Sonda de navegación con rol por variable | 0 rebotes, 0 errores nuevos del shell | `sonda-despues.json` | TODO |
| H1.S3.M2 | Capturas 1280/768/375 claro + 1280 oscuro, miradas | sin defectos visibles | PNG en `evidencia/franja-a/` | TODO |
| H1.S3.M3 | Gates de cierre | tsc, lint, verificar_frontend, specs, a11y en 0 | salidas pegadas abajo | TODO |
