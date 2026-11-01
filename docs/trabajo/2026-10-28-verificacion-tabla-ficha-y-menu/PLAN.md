# Plan — Verificación de identidad en tabla, ficha completa del expediente y «Plataforma» fija en el menú

- Fecha: 2026-10-28 · Repos afectados: PasanakuFrontend (`apps/backoffice`) · Rama: `claude/loving-tesla-bpulmt` · Predecesor: `2026-10-26-backoffice-listados-y-navegacion`
- Pedido literal (Pablo, 2026-10-28, con dos capturas de `/cumplimiento/verificaciones`): «hagamos el layout que hemos dicho mil veces: ponerlo como TABLA, la disciplina de tablas · la mejor calidad de los datos · [señalando PLATAFORMA en el menú] que sea un elemento fijo arriba, no desplegable, el ícono con el texto ya fijo; aparte, que esto abra · revisar porque muchas cosas están vacías: el detalle y la explicación completa de sistema y negocio, ultra completa».
- Resultado observable: quien revisa identidad entra a «Verificación de identidad» y ve una **tabla** paginada (buscador, filtros, CSV/JSON, acción de fila «Revisar» con ícono + nombre); al pulsarla abre la **ficha del expediente** —una tarjeta a lo ancho con pestañas— donde ningún dato queda en blanco sin explicar por qué, con las fotos, la decisión y la explicación de sistema y negocio. En el menú, «Plataforma» es un rótulo fijo con ícono, siempre abierto, que además abre la sección.
- Kill-test: (1) la pantalla monta `ap-tabla-de-datos` y no `ap-tarjeta-de-expediente`; (2) `/cumplimiento/verificaciones/<id>` muestra la ficha con sus pestañas y un campo ausente se lee con su motivo, nunca vacío; (3) en el menú no hay `aria-expanded` ni chevron y «Plataforma» es un enlace con ícono.

## Hallazgos del descubrimiento (hechos)
- F1 · `rutas/cumplimiento/verificaciones/pantalla-de-expedientes.ts` dibuja tarjetas (`ap-tarjeta-de-expediente`) en grilla; es la única bandeja de Cumplimiento fuera de la anatomía `ap-listado` del 2026-10-26.
- F2 · La tarjeta no muestra `iniciadaEn`, `resueltaEn` ni el estado en palabras (`{{ e.estado }}` imprime `EN_REVISION`); el selector de estado ofrece los enums crudos; las caras salen como `PERFIL_IZQUIERDO`; el documento como `CI SC` sin decir qué es.
- F3 · `documento` lo arma el servidor como `<tipo> <lugar>` (`servicios/identidad/.../FilaDeExpediente.java:64`): tipo ∈ `CI`, `CEX`, `PASAPORTE`, `CARNET_EXTRANJERO`; lugar ∈ los nueve departamentos (`identidad.yaml`, `lugarExpedicion`). Los nombres de los departamentos ya existen en la app (`apps/movil/lib/pantallas/identidad/pasos_alta/departamentos.dart`).
- F4 · El contrato no publica «consultar un expediente»: solo `listarVerificaciones` (sin filtro trae todos los estados), fotos y decisión. La ficha se resuelve pidiendo la lista y eligiendo por `verificacionId`.
- F5 · `layout/menu-lateral.ts` pliega los grupos con un botón (`aria-expanded`, chevron, `sessionStorage`); `Grupo.icono` existe en `nucleo/menu.ts` y no se dibuja.
- F6 · El tutorial `cumplimiento-verificaciones` (`rutas/ayuda/catalogo/cumplimiento.ts`) habla de «tarjetas» y apunta a `cumplimiento-cola` / `cumplimiento-filtros`.

## Alcance
- IN: `apps/backoffice/src/app/layout/menu-lateral*`, `rutas/cumplimiento/verificaciones/**`, `rutas/cumplimiento/dominio/cu02-*`, `rutas/cumplimiento/cumplimiento.routes.ts`, `rutas/cumplimiento/textos*.ts`, el tutorial de verificaciones, pruebas unitarias y evidencia visual.
- OUT: backend y contrato de `identidad`; otras bandejas; `packages/ui` (se reusan sus piezas, no se cambian).
- Ambigüedades registradas (a confirmar con Pablo):
  - A1 «que esto abra»: supuesto = el rótulo «Plataforma» deja de plegar y pasa a ser un enlace fijo (ícono + texto) que abre la primera sección de Plataforma que la sesión puede abrir; sus entradas quedan siempre visibles.
  - A2 «muchas cosas están vacías»: supuesto = en la vista de verificación; cada dato del expediente se muestra o se explica por qué falta, y la ficha suma la explicación de sistema y de negocio. Otras pantallas no se revisan en este trabajo.

## H1 — Menú: «Plataforma» fija
**CA:** Dado un operador con acceso a Sistemas, cuando mira el menú, entonces «Plataforma» es un rótulo fijo con su ícono que no se pliega, sus entradas siempre se ven, y al pulsarlo abre su primera sección.
**DoD:** `vitest` de `menu-lateral.spec.ts` verde · captura del menú.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Rótulo fijo con ícono y enlace; sin chevron ni almacenamiento | sin `aria-expanded`; `a.rotulo` con `href` a la primera entrada | spec menú + e2e | HECHO |

## H2 — La bandeja en tabla
**CA:** Dado la cola de verificación, cuando carga, entonces es una tabla con columnas de persona, contacto, documento en palabras, vencimiento, fotos n de 5, estado en palabras, iniciada y resuelta; con buscador, filtros, CSV/JSON y «Revisar» por fila.
**DoD:** spec de la pantalla verde · typecheck · lint.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Presentación pura del expediente (estado, documento, caras, fotos) | `CI SC` → «Cédula de identidad · Santa Cruz» | spec dominio | HECHO |
| H2.S1.M2 | Pantalla con `ap-listado` + acción «Revisar» | tabla con N filas y enlace a la ficha | spec pantalla | HECHO |

## H3 — La ficha del expediente
**CA:** Dado un expediente, cuando se abre su ficha, entonces se ve una tarjeta a lo ancho con pestañas Resumen · Documento y fotos · Decisión · Cómo funciona; ningún dato queda en blanco sin su motivo; un id inexistente dice que no existe y ofrece volver.
**DoD:** spec de la ficha verde (correcto, límite, inválido) · typecheck · lint.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H3.S1.M1 | Ruta `verificaciones/:verificacionId` + recurso por id | id existente → ficha; inexistente → aviso | spec ficha | HECHO |
| H3.S1.M2 | Pestañas Resumen / Documento y fotos / Decisión / Cómo funciona | cada pestaña con su contenido | spec ficha | HECHO |
| H3.S1.M3 | Decidir desde la ficha y volver a la bandeja | aprobar/rechazar envía y recarga | spec ficha | HECHO |

## H4 — Regresión, tutorial y prueba visual
**CA:** La suite del backoffice queda verde, el tutorial habla de filas, y hay capturas de bandeja y ficha en 3 anchos × 2 temas.
**DoD:** `yarn typecheck`, `yarn lint`, `test:front`, `test:a11y` del backoffice · capturas en `evidencia/`.
**Estado:** HECHO

| ID | Microtarea | CA | DoD | Estado |
|---|---|---|---|---|
| H4.S1.M1 | Tutorial a filas | textos sin «tarjeta» | spec tutoriales + e2e `tutoriales.e2e.ts` | HECHO |
| H4.S1.M2 | Regresión del backoffice | suites en verde | comandos | HECHO |
| H4.S1.M3 | Capturas 3 anchos × 2 temas | PNG mirados | `e2e/verificacion-de-identidad.e2e.ts` → `evidencia/capturas/` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| El doble de Prism trae un solo expediente con textos de relleno | la prueba visual no muestra casos límite | la captura intercepta `/identidad/verificaciones` con datos sintéticos declarados (regla 65) |
| La ficha pide la lista entera para un expediente | más bytes por apertura | registrado: el contrato no tiene consulta por id (F4); pedirla es trabajo del backend |
