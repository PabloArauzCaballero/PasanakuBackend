# Plan — `menu-y-guardias.spec.ts` sin agotar el plazo

- Fecha: 2026-10-29 · Repos: PasanakuBackend (`apps/backoffice`) · Predecesor: `2026-10-28-verificacion-tabla-ficha-y-menu` (donde se registró el hallazgo)
- Pedido (Pablo): «arregla el timeout de menu-y-guardias».
- Resultado observable: la matriz menú ⇔ guardias corre entera sin acercarse al plazo de 5 s por caso, sola y dentro de la suite completa.
- Kill-test: el primer caso («todos · PLATAFORMA») sigue tardando más de 2 s, o la suite completa vuelve a fallar en ese archivo.

## Hallazgos (hechos)
- F1 · Sobre `origin/dev` (`aa4b658b`), solo: «todos · PLATAFORMA» 4270 ms, «todos · oficial» 349 ms (`evidencia/antes.txt`). El 5 de 87 casos que pasa cerca del plazo es siempre el primero.
- F2 · Causa: el primer caso paga el `import()` en frío de cada ruta perezosa (`loadChildren`/`loadComponent` de `app.routes.ts` y de cada `*.routes.ts`); los demás los encuentran en la caché de módulos. En la suite completa, con otros archivos compitiendo por CPU, ese primer caso pasa de 5 s (medido 5026 ms en `952e606e`) y el siguiente hereda la navegación a medias («rebota a /»).
- F3 · Cada caso crea su `TestBed` nuevo; la caché de módulos ES se comparte en el archivo, así que precargar una vez alcanza para todos.

## Alcance
- IN: `apps/backoffice/src/app/nucleo/menu-y-guardias.spec.ts`.
- OUT: rutas, guardias, menú; el plazo global de Vitest; otras pruebas.

## H1 — La matriz no paga la carga en frío
**CA:** Dado el archivo de la matriz, cuando corre, entonces ningún caso tarda más de 2 s y los 87 pasan, solos y dentro de la suite completa.
**DoD:** `ng test --include=src/app/nucleo/menu-y-guardias.spec.ts --reporters=verbose` con el primer caso < 2 s · `yarn test:front` completo en verde · `yarn lint` y `yarn typecheck`.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | `beforeAll` que resuelve una vez todos los `loadChildren`/`loadComponent` (recursivo), con plazo propio declarado | primer caso < 2 s; 87/87 | spec solo, verbose → `evidencia/despues.txt` | HECHO |
| H1.S1.M2 | Regresión | suite completa verde | `yarn test:front` · `yarn lint` · `yarn typecheck` | HECHO |
