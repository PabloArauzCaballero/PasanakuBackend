# Franja D — Sistemas (`apps/backoffice/src/app/rutas/sistemas/**`)

> **AVANCE: se completa al cerrar (ver «Estado final»).**

- Fecha: 2026-10-26 · Worktree `/home/user/wt-d` · Rama `claude/franja-d` (desde `c9ac3966`) · Servidor `127.0.0.1:4304`
- Plan raíz: `../PLAN.md` (H4). Guía: `../GUIA-DE-FRANJA.md`.
- Resultado observable: un operador con rol `PLATAFORMA` entra a cada vista de Sistemas y ve cabecera con ícono, cifras arriba
  y cada colección como listado del kit (`ap-listado`): buscador multicampo, filtros, CSV/JSON de lo filtrado, acciones de
  fila ícono + nombre y paginación completa; las mutaciones que el contrato tiene (usuarios internos, elementos de catálogo)
  se hacen desde diálogos accesibles que conservan lo escrito ante un fallo.
- Kill-test: en Usuarios internos con 25 filas y 10 por página, «Ir a 3» muestra 5 filas; «Editar» abre un diálogo que
  dispara `PATCH /usuarios-internos/{id}`; un 500 en ese PATCH deja el diálogo abierto con lo escrito.

## Hallazgos del descubrimiento (hechos)

- D-F1 · Las 9 vistas «de puerto» (servicios, despliegues, base, respaldos, proveedores, outbox, webhooks, accesos,
  incidentes) no tienen contrato: con `detectarModoFuentesDeSistemas()` = `produccion` (sin `<meta name="aportaya-demo"
  content="true">`, que el `index.html` de desarrollo **no** trae) muestran el error «todavía no existe un origen de datos
  real»; con la etiqueta, `proveedor-fuentes.demo.ts` sirve los datos de `@aportaya/simulado/backoffice-sistemas`.
- D-F2 · Las 5 vistas con contrato real: usuarios internos (`identidad.yaml`: GET/POST `/usuarios-internos`, GET/PATCH
  `/usuarios-internos/{id}`, PUT `/{id}/roles`, POST `/{id}/desbloquear`), roles (`GET /roles`, solo lectura), permisos
  (`GET /permisos`, solo lectura), bitácora (`auditoria.yaml`: `GET /auditoria/bitacora` paginada por cursor
  `antesDe`/`antesDeId`/`limite≤100`, `GET /{id}`; append-only) y catálogos operativos (GET lista, ficha, elementos;
  `PATCH /elementos/{id}` solo `{activo, motivo}`).
- D-F3 · No hay `DELETE` en ninguno de esos contratos → **no se dibuja ningún «Eliminar»**. «Dar de baja» un usuario interno
  es `PATCH estado=SUSPENDIDO` con motivo («Suspender»); un elemento de catálogo solo se activa/desactiva.
- D-F4 · Botones que «no funcionan» (bugs):
  - B1 `pantalla-usuario-interno-detalle.ts`: «Suspender/Reactivar» y «Desbloquear ahora» usan `prompt()` nativo y
    `subscribe(next)` sin `error`: un fallo no muestra nada y el botón parece muerto.
  - B2 `modal-alta-usuario-interno.ts`, `modal-asignar-roles.ts`, `modal-cambiar-estado-elemento.ts`: leen
    `err.error?.mensaje`, pero `errores.interceptor.ts` ya entrega `ErrorTraducido` (`err.mensaje`): el mensaje real se
    pierde siempre y sale el genérico, sin código de seguimiento.
  - B3 `modal-alta-usuario-interno.ts`: `rolCodigo` arranca en `'OPERATIONS_ANALYST'` aunque el catálogo no lo traiga
    (el simulado trae `codigo`): se envía un rol inexistente.
  - B4 `modal-asignar-roles.ts`: inicializa la selección con `setTimeout`.
  - B5 Los tres modales son `div role=dialog` sin foco atrapado ni restauración (regla 95.4.2); `✕` es un botón solo con
    glifo.
  - B6 Íconos de cabecera repetidos (`candado` en 5 vistas, `baseDeDatos` en 3) y «+» como texto en «Nuevo usuario».
- D-F5 · Ninguna de las 15 tablas pagina ni exporta; los filtros son botones-pastilla o inputs sueltos con literales
  (`.875rem`, `2.5rem`, `18rem`).

## Alcance

- IN: todo `rutas/sistemas/**` salvo `sistemas.routes.ts`; specs y a11y specs de la sección.
- OUT: `packages/ui/**`, `nucleo/**`, `layout/**`, `*.routes.ts`, contratos, simulado.
- Ambigüedades:
  - AD1 «Eliminar usuario interno»: el contrato no tiene DELETE. Supuesto: «Suspender» (PATCH estado) con motivo, ícono
    `pausa`; «Reactivar» con `reproducir`. Confirmar con Pablo si quiere baja definitiva (exigiría contrato nuevo).
  - AD2 «Editar elemento de catálogo»: el PATCH solo admite `activo`+`motivo`. Supuesto: acción «Activar/Desactivar», sin
    «Editar».
  - AD3 Bitácora paginada por cursor: «Ir a la página N» recorre las páginas intermedias pidiendo cada cursor (no hay
    salto directo en el contrato). El buscador filtra la página cargada (el contrato no tiene búsqueda libre) y lo dice.
    CSV/JSON bajan lo filtrado de la página cargada.

## H4 — Listados de Sistemas con la disciplina del kit

**CA:** Dado un operador con rol PLATAFORMA, cuando abre cada vista de Sistemas, entonces cada colección tiene buscador,
filtros, CSV/JSON, acciones ícono + nombre, paginación y los estados cargando/vacío/error, y cada botón dispara la request
que dice.
**DoD:** `tsc` app y spec en 0 · `ng lint` + `verificar_frontend.py` en 0 · specs y a11y de `rutas/sistemas` verdes ·
recorrido en navegador con requests verificadas · capturas 1280/768/375 claro + 1280 oscuro miradas.

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H4.S1.M1 | `compartido/`: `cargaDe(recurso)` → `CargaDeListado` y `ap-cifras-de-sistemas` (fila de KPI) | error de puerto → `{tipo:'error', mensaje}`; trazaId → idPeticion | spec `compartido/carga.spec.ts` | TODO |
| H4.S2.M1 | Servicios y SLO con kit + cifras | 25 filas · 10/pág → «Página 1 de 3»; buscar/filtrar estado | spec vista | TODO |
| H4.S2.M2 | Despliegues (listado) + interruptores intactos | listado pagina; doble confirmación sigue | spec vista | TODO |
| H4.S2.M3 | Base y migraciones | filtro estado | spec | TODO |
| H4.S2.M4 | Respaldos (VENCIDA visible y filtrable) | filtro «Restauración» vencida/al día | spec | TODO |
| H4.S2.M5 | Proveedores (monto + moneda en CSV) | CSV con moneda | spec | TODO |
| H4.S2.M6 | Outbox y descartados (dos listados, sin Eliminar) | dos listados con nombres distintos | spec + a11y | TODO |
| H4.S2.M7 | Webhooks | filtros dirección/estado | spec | TODO |
| H4.S2.M8 | Accesos | filtros rol/ámbito | spec | TODO |
| H4.S2.M9 | Incidentes | filtros severidad/estado | spec | TODO |
| H4.S3.M1 | Usuarios internos: listado + Ver/Editar/Roles/Suspender | Editar → PATCH con motivo; fallo conserva lo escrito | spec vista (HttpTestingController) | TODO |
| H4.S3.M2 | Alta de usuario interno en `ap-dialogo` (B2, B3, B5) | rol inicial = primero del catálogo; error traducido | spec | TODO |
| H4.S3.M3 | Asignar roles en `ap-dialogo` (B2, B4, B5) | PUT `/roles` con motivo | spec | TODO |
| H4.S3.M4 | Detalle de usuario: Suspender/Desbloquear/Editar con diálogo (B1) | sin `prompt()`; error visible | spec | TODO |
| H4.S4.M1 | Roles internos (solo lectura, Ver permisos en cajón) | filtros departamento/ámbito/origen | spec | TODO |
| H4.S4.M2 | Permisos internos (solo lectura) | filtros riesgo/recurso/MFA | spec | TODO |
| H4.S5.M1 | Bitácora: cursor + Ir a N + cajón (sin Eliminar) | «Siguiente» manda `antesDe`/`antesDeId`; Ir a 3 recorre | spec | TODO |
| H4.S6.M1 | Catálogos operativos (listado + Ver) | filtros estado/dueño | spec | TODO |
| H4.S6.M2 | Ficha de catálogo + elementos + Activar/Desactivar en `ap-dialogo` (B2, B5) | PATCH `{activo, motivo}`; fallo conserva motivo | spec | TODO |
| H4.S7.M1 | Recorrido en navegador (aceptado/límite/error) + descargas | requests y archivos verificados | `node …/franja-d/recorrido.mjs` | TODO |
| H4.S7.M2 | Capturas 1280/768/375 claro + 1280 oscuro, miradas | sin desbordes a 375 | Read de cada PNG | TODO |
| H4.S7.M3 | Comandos de cierre | todos en 0 | salida literal abajo | TODO |
