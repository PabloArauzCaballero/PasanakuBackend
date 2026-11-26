# Plan UI — Lote C · Invitación, credencial y recuperación del operador (backoffice)

- Fecha: 2026-11-24 · Repos afectados: `apps/backoffice`, `packages/simulado` · Predecesor: [PLAN.md](./PLAN.md) (hito H6)
- Rama: `claude/gu-c-credencial-ui` (base `claude/gu-c-credencial@291a638b4`)
- Resultado observable: una persona con una invitación (alta, clave o factor) la completa desde el backoffice sin dejar el token en la URL;
  quien olvidó su clave o perdió su autenticador pide una recuperación con respuesta neutra; quien tiene sesión cambia su clave; quien tiene
  `SEGURIDAD_ACCESO_RESTABLECER` aprueba o rechaza recuperaciones ajenas con motivo.
- Kill-test: abrir `/acceso/invitacion#token=x`, ver que la URL queda sin fragmento y que la pantalla muestra el estado de error accionable
  (no el texto crudo del backend) cuando el simulado responde `AP-CU09-04`.

## Alcance
- IN: `apps/backoffice/src/app/rutas/acceso/**`, `app.routes.ts` (dos entradas), `nucleo/menu.ts` (una línea), `nucleo/errores.ts` (códigos AP-CU09-xx),
  `layout/menu-de-usuario.ts` (enlace «Cambiar mi clave»), `rutas/ingreso/pantalla-de-ingreso.ts` (enlace «No puedo entrar»),
  `packages/simulado/ejemplos/identidad/*` (solo agregar), capturas y documentos de este directorio.
- OUT: `servicios/**`, `sql/**`, `identidad.yaml`, `docs/CasosDeUso`, `docs/entidades`, `scripts/**`, `postman/**`, `rutas/sistemas/**` (lote D).
- Ambigüedades registradas:
  1. El QR se dibuja con `ap-codigo-qr` (ya existe en `@aportaya/ui`, usa `qrcode`): no se agrega ninguna librería.
  2. La política de clave real vive en el servidor (`identidad.clave.largo-minimo`, por defecto 8). La UI muestra solo el piso de 8 y deja que el
     servidor decida; si exige más, el error `AP-CU09-02` aparece en el campo «clave nueva».
  3. El permiso `SEGURIDAD_ACCESO_RESTABLECER` hoy no está en el catálogo de pestañas de `menu.ts`; la pestaña se agrega en «Configuración»
     (una línea) y se confirma con la sesión principal / lote D dónde debe vivir definitivamente (¿«Personal y accesos»?).

## H6 — Backoffice (regla 95) · detalle por microtarea

**CA:** Dado el backoffice, entonces existen las pantallas de invitación, recuperar acceso, cambiar mi clave y aprobaciones de recuperación, cada una con
cargando/datos/vacío/error, errores por campo, sin doble envío, foco visible, tema claro y oscuro, AA.
**DoD:** specs de componente en verde · capturas 390/834/1440 × claro/oscuro con `animations: 'disabled'`, inspeccionadas · consola y red sin errores nuevos.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| U1 | Cliente tipado de `/cuenta/*` + catálogo de errores AP-CU09-xx → mensaje y campo | cada código del contrato tiene mensaje accionable y campo; ninguno devuelve el texto del backend | `yarn workspace @aportaya/backoffice test:front --include='src/app/rutas/acceso/dominio/*.spec.ts'` → verde | TODO |
| U2 | Ejemplos del simulado de las 8 operaciones, 3 niveles (correcto/límite/inválido) | cada JSON valida contra su esquema | `python3 scripts/verificar_maqueta.py` y `python3 scripts/verificar_pruebas_web.py` → 0 y `yarn workspace @aportaya/simulado test:front` verde | TODO |
| U3 | Pantalla `/acceso/invitacion` (token del fragmento, clave, QR TOTP, código, éxito, token inválido) | el fragmento se borra de la URL; token solo en memoria; error por campo; sin doble envío; token inválido sin distinguir causa | spec de componente + a11y (axe) verde | TODO |
| U4 | Pantalla `/acceso/recuperar` + enlace «No puedo entrar» en el ingreso | respuesta neutra idéntica con 202 sea cual sea el correo | spec de componente + a11y verde | TODO |
| U5 | Pantalla `/acceso/cambiar-clave` + enlace en el menú de usuario | error por campo; si `cierraLaSesionActual`, aviso y vuelta al ingreso con la sesión cerrada | spec de componente + a11y verde | TODO |
| U6 | Pantalla `/acceso/recuperaciones` (bandeja por cursor, filtro por estado, modal aprobar/rechazar) + guardia + entrada de menú | sin permiso no hay ruta ni enlace; AP-CU09-09 → «No podés decidir tu propia solicitud»; tras aprobar muestra el enfriamiento | spec de componente + a11y + `menu-y-guardias.spec.ts` verde | TODO |
| U7 | Rutas (`app.routes.ts`) y regresión del enchufe/menú | `enchufe-de-rutas.spec`, `menu-y-guardias.spec` y `menu.spec` verdes | `yarn workspace @aportaya/backoffice test:front` verde | TODO |
| U8 | Capturas 390/834/1440 × claro/oscuro de cada pantalla y estado, inspeccionadas; consola y red sin errores | cada captura mirada y corregida | `playwright test e2e/visual-acceso.e2e.ts` verde + capturas en `evidencia/capturas/` | TODO |
| U9 | Gates: typecheck, lint, `verificar_frontend.py` | 0 errores | `yarn workspace @aportaya/backoffice typecheck` / `lint` | TODO |
| U10 | `REPORTE-UI.md` con avance, evidencia literal, no cubierto | primera línea con `AVANCE: X / N` | archivo en disco | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| `clientes/angular/identidad` está ignorado por git y no estaba generado (Gradle sin Maven Central) | Sin cliente no compila el backoffice | Se generó con `openapi-generator-cli 7.14.0` desde el mirror de Maven en Google; mismos parámetros que `aportaya.openapi.gradle.kts`. Es artefacto local, no se commitea |
| El simulado (Prism) responde un ejemplo fijo por operación | Los estados de error se ven con la cabecera `Prefer` | Las capturas de error usan `page.route` solo para fijar el escenario; los datos son sintéticos |
| `nucleo/menu.ts` y `nucleo/errores.ts` los toca otro lote | Conflicto de merge | Una línea en `menu.ts`, un bloque aparte al final del catálogo en `errores.ts` |
