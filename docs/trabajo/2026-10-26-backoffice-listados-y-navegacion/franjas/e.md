# Franja E — Contabilidad y Configuración (H5)

- Fecha: 2026-10-26 · Worktree `/home/user/wt-e` · Rama `claude/franja-e` (desde `c9ac3966`) · Puerto 4305
- Plan raíz: [`../PLAN.md`](../PLAN.md) · Guía: [`../GUIA-DE-FRANJA.md`](../GUIA-DE-FRANJA.md)
- Resultado observable: en cada vista de `/contabilidad/**` y `/configuracion/**` el operador ve cabecera con ícono,
  cifras arriba cuando corresponde y cada listado con buscador multicampo, filtros, descarga CSV/JSON de lo filtrado,
  paginación (filas por página, ir a la página N, Anterior/Siguiente) y acciones de fila con ícono + nombre; cada botón
  dispara la petición del contrato o no se dibuja.
- Kill-test: en Cuentas por cobrar, buscar un texto inexistente muestra «Quitar filtros» y «Descargar CSV» baja solo el
  encabezado; «Registrar el cobro» confirmado dispara `POST /erp/cuentas-por-cobrar/{id}/cobros` y la lista se recarga.

## Hallazgos (descubrimiento)

- E-F1 · Todos los listados de la franja paginan **por cursor en el servidor** (`erp.yaml` A6, `tarifas.yaml`,
  `notificaciones.yaml`, `auditoria.yaml`): `limite` 1–100, filtro `estados`, sin total, sin búsqueda de texto, sin
  orden por columna. No se bajan enteros: búsqueda y filtros que no son `estados` se aplican **a la página cargada** y
  la ayuda del buscador lo dice; CSV/JSON exportan lo filtrado de esa página (ambigüedad A3 del plan raíz).
- E-F2 · `erp.yaml`, `tarifas.yaml` (tarifarios) y `notificaciones.yaml` (plantillas) **no tienen PUT/PATCH/DELETE**:
  ningún listado de esta franja lleva Editar ni Eliminar (además, asientos, facturas, cobros, pagos y períodos son
  append-only, regla 91.1.3). Las acciones de fila son de dominio: Aprobar orden (`aprobarOrdenDeCompra`, BORRADOR),
  Registrar el pago (`pagarFacturaDeProveedor`), Registrar el cobro (`registrarCobro`), Depreciar (`depreciarActivo`).
- E-F3 · Botones que «no funcionan» encontrados:
  - Períodos · «Asentar en este período» solo guardaba una señal (`asentadoEn`): no hay operación de alta de asiento en
    ningún contrato → botón muerto.
  - Estados financieros · «Descargar el documento» traía el blob y nunca lo entregaba al navegador.
  - Facturas / Cobros / Activos · tras pagar, cobrar o depreciar con éxito la lista no se recargaba y no había aviso:
    parecía que no pasó nada.
  - Órdenes · `crearAprobacionDeOrden` existía en el dominio sin botón (una orden en borrador no se podía aprobar).
- E-F4 · Mensajería pinta el costo del envío como `{{ costo }} {{ moneda }}` (sin `ap-monto`).
- E-F5 · `contabilidad/listado/estado-de-listado.ts` y `listado.apoyo.ts` los usa también Publicidad (franja F):
  no se borran. `configuracion/aviso-de-listado.ts` y `filtro-de-estado.ts` solo los usa Configuración.

## Microtareas

Estados: TODO · EN CURSO · HECHO · A MEDIAS · BLOQUEADO · DESCARTADO.

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H5.S0.M1 | Listado por cursor compartido (`listado/listado-por-cursor.ts` + `listado/tabla-por-cursor.ts`) | 25 filas en 3 páginas de 10 (cursor), «Ir a 3» pide con el cursor de la 2; buscar filtra la página; CSV con lo filtrado; vacío / error con reintento | `ng test --include=…/listado/**` | TODO |
| H5.S1.M1 | Presupuesto con el listado + KPI de conteos | spec 3 niveles verde | `ng test --include=…/presupuesto/**` | TODO |
| H5.S1.M2 | Órdenes con listado + «Aprobar» (BORRADOR) | POST `/aprobacion` con Idempotency-Key, recarga | `ng test --include=…/compras/**` | TODO |
| H5.S1.M3 | Facturas con listado + «Registrar el pago» que recarga y avisa | POST `/pagos`, recarga | ídem | TODO |
| H5.S1.M4 | Cobros con listado + «Registrar el cobro» que recarga y avisa | POST `/cobros`, recarga | `ng test --include=…/cobros/**` | TODO |
| H5.S1.M5 | Activos con listado + «Depreciar» que recarga y avisa | POST `/depreciaciones`, recarga | `ng test --include=…/activos/**` | TODO |
| H5.S1.M6 | Períodos: layout, íconos, sin botón muerto | período abierto dice «Admite asientos» sin botón; cerrar sigue | `ng test --include=…/periodo/**` | TODO |
| H5.S1.M7 | Estados financieros: layout + la descarga entrega el archivo | `descargar` crea el archivo con el nombre que lleva el hash | `ng test --include=…/estados/**` | TODO |
| H5.S2.M1 | Tarifario con listado | spec 3 niveles | `ng test --include=…/configuracion/tarifario/**` | TODO |
| H5.S2.M2 | Mensajería: dos listados, costo con `ap-monto` | spec 3 niveles | `ng test --include=…/configuracion/mensajeria/**` | TODO |
| H5.S2.M3 | Reportes: dos listados (convive con `Exportador` CU-58, no se toca) | spec 3 niveles | `ng test --include=…/configuracion/reportes/**` | TODO |
| H5.S2.M4 | Inicio de Configuración con íconos | cada acceso con ícono | inspección + spec de rutas | TODO |
| H5.S3.M1 | Recorrido en navegador (aceptado/límite/error) con red verificada y descargas | script Playwright en verde | `node franja-e/recorrido.mjs` | TODO |
| H5.S3.M2 | Capturas 1280/768/375 claro + 1280 oscuro, miradas | PNG en `evidencia/franja-e/` | Read de cada PNG | TODO |
| H5.S3.M3 | Comandos de cierre | tsc, lint, verificar_frontend, tests y a11y en verde | comandos de la guía | TODO |
