# Franja A — shell y navegación (H1)

> **AVANCE: 12 / 13 — 92,3 %.** (H1.S1.M5 queda A MEDIAS: el peor caso de la precarga, abajo)

- Fecha: 2026-10-26 · Worktree `/home/user/wt-a` · Rama `claude/franja-a` (base `c9ac3966`) · Servidor de desarrollo `127.0.0.1:4301`.
- Plan raíz: `../PLAN.md` (F1, F2, A1). Guía: `../GUIA-DE-FRANJA.md`. Evidencia: `../evidencia/franja-a/`.
- Peldaño alcanzado: **VERIFIED** para el shell (navegador real, consola y red revisadas, capturas miradas) contra el
  **doble** OpenAPI sin estado; ninguna mutación se declara persistida. El peldaño del trabajo es el de H1.S1.M5 (A MEDIAS).

## Microtareas

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Latencia «antes» (CDP 150 ms + CPU ×4) | tabla por entrada | `e2e/apoyo/medir-latencia-del-menu.mjs` → `latencia-antes.json`, `latencia-prod-base*.json` | HECHO |
| H1.S1.M2 | F1 generalizado: menú, pestañas, buscador y migas muestran solo lo que las guardias dejan abrir | rol `oficial` no ve Sistemas; `PLATAFORMA` abre `/sistemas/servicios`; ninguna pestaña visible rebota y ninguna oculta abre | `menu-y-guardias.spec.ts` (router real, 87 sesiones) + sonda 0 rebotes | HECHO |
| H1.S1.M3 | A1: ≤ 7 entradas, ≤ 8 pestañas, sin duplicados; URLs viejas responden | 7 entradas; máx. 8 pestañas; sin pantallas repetidas | `menu.spec.ts` + e2e de roles | HECHO |
| H1.S1.M4 | Pestañas con ícono; subpestañas de grupo | cada pestaña y subpestaña con `ap-icono` | `pestanas-de-seccion.spec.ts` + axe | HECHO |
| H1.S1.M5 | F2: precarga en reposo de lo que la sesión puede abrir | ningún clic del menú descarga código tras el reposo; el clic temprano no empeora | mediciones prod | A MEDIAS |
| H1.S1.M6 | Ayuda en el menú de usuario; sin «Mi perfil» que rebotaba; teclado | ítems con ícono que navegan; flechas/Escape/foco | `shell-financiero.spec.ts` + recorrido | HECHO |
| H1.S1.M7 | Cumplimiento con una sola lista de códigos (pedido de la franja C) | pestaña visible ⇔ ruta abre, con `ENTRADAS_DE_CUMPLIMIENTO` | `menu-y-guardias.spec.ts` | HECHO |
| H1.S2.M1 | Cada botón del shell a 1280 y 375 (aceptado / límite / error) | 31 verificaciones | `e2e/apoyo/recorrido-del-shell.mjs` → `recorrido-shell.json` 31/0 | HECHO |
| H1.S2.M2 | Ingreso CU-04 | errores en su campo, 401/500 accionables, 1 pedido por envío | `pantalla-de-ingreso.spec.ts` (5) + recorrido | HECHO |
| H1.S2.M3 | Campana: datos, vacío, error 500 con reintento | 4 estados, acciones con ícono | `campana-de-notificaciones.spec.ts` (+3) + recorrido | HECHO |
| H1.S2.M4 | «Salir» revoca aunque el token esté vencido | DELETE 401 → refrescar → DELETE reintentado; si el refresco falla, igual sale | `layout/dominio/cerrar-sesion.spec.ts` | HECHO |
| H1.S3.M1 | Sonda con `ROL`, subpestañas y rebotes | 0 rebotes, 0 errores nuevos del shell | `sonda-despues-{PLATAFORMA,oficial}.json` | HECHO |
| H1.S3.M2 | Gates + capturas 1280/768/375 claro, 1280/375 oscuro, miradas | todo en 0 | `gate-*.txt`, PNG | HECHO |

### A MEDIAS — H1.S1.M5 (precarga)
- **Qué anda:** build de producción, red 150 ms + CPU ×4, quien mira el tablero 1,5 s antes de hacer clic: **mediana 984 → 438 ms,
  máx. 1227 → 517 ms, 0 archivos descargados al hacer clic** (antes 3–8 por entrada). Solo se precarga lo que las guardias
  `canMatch` dejan pasar a esa sesión (Sistemas no se baja sin su rol), de a una ruta, sin fichas `:id`, en pausa durante
  una navegación y retomando en reposo (`nucleo/precarga-segun-sesion.ts`, spec de 4 casos).
- **Qué no anda:** clic **inmediato** tras entrar, encadenando clics sin pausa (`SIN_REPOSO=1`): mediana 979 → 873 ms pero
  máximo 1418 → **2413 ms** (Sistemas, mientras llegaban chunks precargados). En modo desarrollo (módulos sin empaquetar) ese
  peor caso es peor (15 s): no representa producción.
- **Qué falta exactamente:** decidir entre (a) aceptar el peor caso, (b) precargar solo la pantalla de aterrizaje de cada entrada
  (requiere marcar `data` en las rutas hijas), o (c) precarga por intención (hover/foco del enlace). Medir de nuevo con
  `medir-latencia-del-menu.mjs` contra el build servido.
- **Dónde quedó:** `claude/franja-a`, compila, specs verdes.

## Recorte A1 — antes / después (para que Pablo lo revise)

Menú de primer nivel: **10 → 7**. Antes: Inicio · Operación · Admisiones · Cumplimiento · Sistemas · Catálogo de datos ·
Contabilidad · Publicidad · Configuración · Ayuda. Después: Inicio · Operación · Cumplimiento · Contabilidad · Publicidad ·
Sistemas · Configuración (los dos últimos bajo «Plataforma»).

| Sección | Antes (pestañas) | Después | Razón |
|---|---|---|---|
| Operación | 8 | 8: Retiros, Desembolsos, Reversos, Reclamos, Conciliación de custodia, Cierres diarios, **Admisiones a grupos**, Solicitudes escaladas | Admisiones era una entrada propia con una sola pantalla: es trabajo de operación (ingreso a grupos), junto a «Solicitudes escaladas». **Salió «Arquitectura y estado»**: repetía «Servicios y SLO» de Sistemas. Sigue en `/operacion/estado`. |
| Cumplimiento | 9 | 7: Verificación, Alertas, Casos, Reportes UIF, Requerimientos, Actas, **Riesgo y seguridad** ▸ (Eventos de riesgo, Pruebas de continuidad, Incidentes de seguridad) | Las tres colas del responsable de riesgo/seguridad (CU-54/55/56) agrupadas por tarea. |
| Contabilidad | 4 | 4 (sin cambios) | Probé mover custodia y cierres acá: le mostraba «Contabilidad» al administrador de plataforma (que no tiene `ver:contabilidad`). Revertido. |
| Publicidad | 5 | 5 | — |
| Sistemas | 14 | 7: Servicios y SLO, Despliegues, Base de datos, Respaldos, **Integraciones** ▸ (Proveedores, Outbox, Webhooks), **Personal y accesos** ▸ (Usuarios, Roles, Permisos internos, Revisión de accesos), Registro del sistema | Usuarios/Roles/Permisos: tres pestañas de una misma tarea. **Salió «Incidentes» de Sistemas**: duplicaba «Incidentes de seguridad» de Cumplimiento y en producción no tiene fuente de datos (`proveedor-fuentes.ts` entrega «no disponible»); sigue en `/sistemas/incidentes`. «Catálogos operativos» pasó a Configuración. |
| Configuración | 3 | 5: Tarifario, Mensajería, Reportes, **Catálogos operativos**, **Catálogo de datos** | Parámetros y catálogos son configuración. Catálogo de datos dejó de ser una entrada de primer nivel. |
| Ayuda | entrada | menú de usuario («Centro de ayuda») + lanzador | Aprender el backoffice no es una sección de trabajo. |

Ninguna ruta, guardia de servidor ni permiso de la matriz cambió; todo lo que salió del menú **responde por URL**.
`cumplimiento.routes.ts` sí cambió su guardia de `verificaciones` (antes solo `ver:cumplimiento`, ahora además
`VERIFICACION_RESOLVER`, la lista de la franja C): cierra más, no abre nada.

## Evidencia (salidas literales recortadas)

```text
$ npx tsc -p tsconfig.app.json --noEmit; npx tsc -p tsconfig.spec.json --noEmit
tsc app exit=0
tsc spec exit=0
$ npx ng lint                                   → All files pass linting.
$ python3 ../../scripts/verificar_frontend.py backoffice → TODO OK
$ npx ng test --exclude='src/**/*.a11y.spec.ts' → Test Files 152 passed (152) · Tests 1033 passed (1033)
$ npx ng test --include='src/**/*.a11y.spec.ts' → Test Files 43 passed (43) · Tests 79 passed (79)
$ npx playwright test shell, tutoriales, f2-posiciones-y-roles, admisiones, tablero-y-permisos, sistemas-sin-contrato, restaurar-sesion → 50 passed (1.2m)
$ suite e2e completa (antes del ajuste de f2-posiciones-y-roles) → 161 passed, 4 failed (los 4 de roles, ya corregidos)
$ sonda (ROL=PLATAFORMA) → {"vistas":43,"rebotes":0,"lentas":0,"errores":3}
$ sonda (ROL=oficial)    → {"vistas":30,"rebotes":0,"lentas":0,"errores":3}
$ recorrido-del-shell    → {"pass":31,"fail":0,"errores":10}
```

Los 3 errores de la sonda son F3 (`404 GET /api/v1/grupos/solicitudes-escaladas`, sin contrato; franja B). Los 10 del
recorrido son todos provocados o del doble: 401/401/500/401 del ingreso simulados a propósito; 500 de la campana y del
`DELETE` simulados; y `DELETE /sesion 401` + `/sesion/refrescar 401` del doble (que contesta 401 a todo `DELETE /sesion`).

Latencia (red 150 ms / 1,6 Mb/s, CPU ×4; clic → vista pintada, incluye la respuesta del simulado):

| Escenario | Antes: mediana / máx. / chunks al clic | Después |
|---|---|---|
| Producción, con 1,5 s de reposo | 984 / 1227 ms / 50 | **438 / 517 ms / 0** |
| Producción, sin reposo (peor caso) | 979 / 1418 ms / 50 | 873 / **2413** ms / 106 (precarga en curso) |
| Desarrollo, con reposo (medición intermedia, antes del ajuste final de la precarga) | 1352 / 4574 ms / 49 | 403 / 3893 ms / 0 |

Presupuesto del build: inicial **469,58 kB** (base 459,09; advertencia 460, error 500; +2,6 kB transferidos).

Capturas miradas (`../evidencia/franja-a/`): `shell-sistemas-outbox-{1280-claro,1280-oscuro,768-claro,375-claro,375-oscuro}`,
`shell-cumplimiento-incidentes-{1280-oscuro,768-claro,375-oscuro}`, `cajon-375-claro`, `shell-tablero-375-claro`,
`menu-de-usuario-1280`, `buscador-resultados-1280`, `campana-error-500-1280`, `ayuda-1280`, `ingreso-*`. Defectos vistos y
corregidos en ellas: migas apiladas en tres líneas y título cortado; séptima pestaña cortada; franja blanca en el cajón;
menú de usuario cortado a 1280.

## No cubierto
- Ninguna mutación persistida: el simulado no tiene estado («verificado contra el doble»).
- Lector de pantalla real: solo axe + teclado (flechas, Escape, foco devuelto) en el navegador.
- Firefox/WebKit: solo Chromium.
- La medición de latencia no incluye un despliegue real (CDN, HTTP/2): build servido localmente.
- `menu-y-guardias.spec.ts` falló **una vez** en una corrida de la suite completa (sesiones «todos») y no se reprodujo en 3
  corridas posteriores (completas y aisladas). Sin causa demostrada: se registra, no se clasifica.

## Pedidos a otras franjas
- **B (operación):** `solicitudes-escaladas` pide `/grupos/solicitudes-escaladas` → 404 (F3); la pestaña sigue en el menú
  porque la ruta abre. Si no hay contrato, que la pantalla diga «no disponible» en vez de un error.
- **C (cumplimiento):** a 375 px el contenido de Cumplimiento se corta a la derecha (la navegación interna duplicada y los
  chips de estado); con su retiro de la navegación interna puede quedar resuelto; verificar.
- **D (sistemas):** en desarrollo `Outbox y descartados` muestra «Algo salió mal» (la fuente de ejemplo falla); `guardia-rol-sistemas.ts`
  redirige a `/operacion` (podría ir a `/tablero`, como las demás guardias).
- **Dueños de `packages/tutoriales`:** el lanzador «Cómo funciona esta pantalla» y los botones del centro de ayuda no llevan
  ícono (no está en mi alcance editarlos).

## Hallazgos fuera de alcance
- El tutorial «Mirar la billetera de una persona» apunta a `/operacion`, que ya no es la billetera (H12): sus objetivos
  `operacion-billetera`/`operacion-saldo` no existen en esa ruta.
- `textosSistemas.perfil` («Mi perfil de operador») no tiene ruta: por eso «Mi perfil» salió del menú de usuario.
- Presupuesto inicial superado por 9,6 kB (advertencia, no error).
- La campana no carga el conteo hasta que se abre (no hay insignia al entrar).

## Desvíos y decisiones
- Custodia/cierres: movidos a Contabilidad y devueltos a Operación (ver tabla).
- `DELETE /sesion` marcado `YA_REINTENTADA` en `83d40c6d` era un **defecto de seguridad** (el coordinador lo detectó):
  revertido en `330cee5a`, con spec que fija la revocación. `POST /sesiones` (ingreso) sí queda marcado: no hay sesión
  que revocar y el 401 es la credencial.
- Durante la limpieza de capturas que la suite e2e reescribe en carpetas ajenas, un `git checkout` revirtió cambios míos
  sin commitear; los rehice y quedaron en `337f1a03`. Las capturas ajenas quedaron restauradas.
- A4 (paralelismo) es del plan raíz.
