# AUDITORIA — frontends-compartidos (L6)

> Auditoría de solo lectura (L6). Se escribió por tandas, con commit y push de cada una (indicación de
> Pablo retransmitida por la sesión coordinadora). Peldaño de evidencia: `DISCOVERED` salvo donde se
> pega una ejecución.

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | `frontends-compartidos`: `packages/*`, `clientes/*`, shell y compartido de `apps/movil`, `apps/backoffice`, `apps/web` |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado `origin/test` | `07fc69719287b0dadd152d0293a054948df69ead` |
| Commit `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe`. `git diff --stat origin/test origin/dev -- packages clientes apps` da salida vacía: **sin diferencias en este alcance** |
| Historial | Completo (`git fetch --unshallow origin test`; `--is-shallow-repository` → `false`; 1 878 commits en `origin/test`) |
| Archivos y líneas cubiertos | Huella: **1 391 archivos**, ~**98 900 líneas** de texto (sin binarios). Leídos completos ≈ 750; el resto se inventarió, muestreó o escaneó de forma dirigida, declarado fila por fila en §12 |

## 2. Resumen

**Estado.** El código compartido de frontend está ordenado en la superficie: `verificar_frontend.py todo` da `TODO OK`, el dinero viaja como cadena, el token del backoffice vive en memoria y los errores crudos no llegan a la pantalla. Debajo hay defectos que tocan dinero y confianza:
- Un átomo de importe del sistema de diseño (`CampoMonto`) multiplica por 100 lo que se escribe con punto decimal, en las seis pantallas de dinero de la app (**S1**).
- Las claves de idempotencia tienen dos políticas distintas, y la de la app bloquea el reintento corregido.
- El refresco de sesión de la app se duplica en tres instancias.
- El validador del gateway se saltea.
- El SSR público no emite cabeceras de seguridad.
- Las exportaciones y lecturas de datos personales del backoffice no dejan rastro.

Frente al flujo nuevo, la distancia es sobre todo **de texto y de contrato**. El sitio público, el tour de la app y el mock prometen que un fondo cubre al que no paga y ofrecen pagar en efectivo, y el shell del backoffice hace de la admisión humana la pantalla de inicio de Operación. Los caminos vivos de aprobación y respaldo están en los servicios (L1–L4); acá se los publica, se los simula y se los enseña.

**Hallazgos por clase y severidad** (42 fichas en §5; además, 40 candidatos de lectores en §5-bis como *sospecha*):

| Clase | S1 | S2 | S3 | S4 |
|---|---|---|---|---|
| DINERO | 1 | 1 | 1 | – |
| RESPALDO | – | 3 | – | – |
| APROB | – | 1 | 1 | – |
| FLUJO-QR | – | 1 | 2 | – |
| COHERENCIA | – | 1 | – | – |
| TRAZA | – | 1 | – | – |
| SEG | – | 2 | 1 | – |
| DEFECTO | – | 2 | 4 | – |
| CONTRATO | – | 1 | 2 | – |
| PRUEBAS | – | – | 3 | – |
| DUP-X | – | – | 2 | – |
| REESCRITURA | – | – | 2 | – |
| HUECO | – | – | 2 | – |
| UX-A11Y | – | 1 | 1 | – |
| CONFIG, FRAGIL, DATOS | – | – | 3 | – |
| PARCHE, DUP-INT, DOC | – | – | – | 3 |
| **Total** | **1** | **14** | **24** | **3** |

**Los diez más graves**
1. `DINERO-002` (S1): `CampoMonto` convierte «12.50» en `1250.00` en recargar, retirar, transferir, puja, subasta y crear grupo.
2. `DEFECTO-002` (S2): la clave de idempotencia de la app es fija por cuenta y sobrevive al rechazo, así que corregir el importe choca con la clave vieja.
3. `DINERO-003` (S2): el backoffice dice «si lo repetís, no se duplica» aunque la clave solo sale si la pantalla la marca.
4. `RESPALDO-001` (S2): el sitio público y el JSON-LD prometen que el fondo cubre la entrega de quien no paga.
5. `RESPALDO-003` (S2): el tour de la app promete lo mismo.
6. `RESPALDO-002` (S2): el mock y los clientes normalizan `cubiertoEmpresa` y la familia de respaldo.
7. `FLUJO-QR-003` (S2): la portada ofrece aportar en efectivo y recargar por transferencia.
8. `SEG-003` (S2): `ap-listado` exporta datos de participantes desde el navegador por omisión y sin rastro.
9. `TRAZA-001` (S2): el registro de lecturas de datos personales está apagado y su aviso miente.
10. `DEFECTO-003` (S2): tres `Refrescador` independientes pueden revocar la sesión por reuso del refresh.

**Peldaño por área**
- `packages/diseno_flutter` y `apps/movil` (shell): `DISCOVERED`. Para `DINERO-002` hay una reproducción de la función portada; no hay ejecución en Dart.
- `packages/dominio-cliente`: `DISCOVERED` + una ejecución puntual con `node` (`SEG-001`).
- `packages/simulado` y `clientes`: `DISCOVERED` + validación por script del lector B (jsonschema de 349 ejemplos).
- `packages/ui`, `tutoriales` y los shells de backoffice y web: `DISCOVERED`.
- Verificadores del repositorio: `RUNS` (salidas en `evidencia/verificadores/`).

## 3. Huella y mapa de conexiones

**Huella** (supuesto de frontera en §11):
- `packages/diseno_flutter`: 242 archivos.
- `packages/simulado`: 374.
- `packages/ui`: 147.
- `packages/tutoriales`: 26.
- `packages/tokens`: 17.
- `packages/dominio-cliente`: 16.
- `clientes/*`: 26 versionados. El resto se genera y está en `.gitignore`.
- Shell del backoffice: 134, sin `rutas/` ni `e2e/`.
- Shell y páginas institucionales de la web: 115, sin `verificadores/`, `invitacion/` ni `publico-*`.
- Shell de la app: 116 de `lib/` fuera de `pantallas/`, más `arranque/` y `alianzas/`, configuración, `test_driver`, `terceros/` y las pruebas de `test/unidad` y `test/widget`.

**Mapa de conexiones**

| Desde (alcance) | Hacia | Mecanismo | Punto exacto |
|---|---|---|---|
| `apps/movil/lib/dominio/cliente.dart` | gateway → todos los servicios | HTTP (Dio + clientes Dart generados) | `cliente.dart:88-91` (refresco), interceptores `_TrazaYSesion` |
| `apps/movil/lib/dominio/cu10..cu15_*.dart` | `nucleo-financiero` | HTTP, cliente `aportaya_cliente_nucleofinanciero` | `solicitarRecarga`, `solicitarRetiro`, `transferirSaldo`, `pagarRecargaDePrueba`, `emitirExtracto`, saldo |
| `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart` | `identidad` | HTTP (estado de cuenta + `POST /sesion/refrescar`) | l.74-77 |
| `apps/movil/lib/navegacion/enlaces_profundos.dart` y `ruta_del_aviso.dart` | `notificaciones` (push) → rutas de la app | deep link | `enlaces_profundos.dart:12-17` |
| `apps/backoffice/src/app/nucleo/*.interceptor.ts` | gateway | HTTP (Bearer en memoria, cookie de refresco, `Idempotency-Key`, `x-request-id`, timeout) | `interceptores.ts` |
| `apps/backoffice/src/app/nucleo/avisos-internos.ts` | `notificaciones` | HTTP | `/notificaciones/avisos-internos` (l.41, 66, 90) |
| `apps/backoffice/src/app/nucleo/registro-de-acceso.interceptor.ts`, `exportador.ts` | servicio `extraccion` (no existe) | HTTP | `POST /extraccion/accesos`, `/extraccion/exportaciones` |
| `apps/backoffice/src/app/nucleo/tutoriales/almacen-remoto.ts`; `packages/tutoriales` | `identidad` | HTTP | `/identidad/tutoriales/progreso` (sin contrato, ADR-045) |
| `apps/backoffice/src/app/app.routes.ts` + `nucleo/menu.ts` | pantallas de módulo (L1–L5) | rutas lazy + permisos | `app.routes.ts:95-102` y siguientes |
| `apps/web/src/server.ts` | gateway interno | proxy HTTP `/api` | `server.ts:55-85` |
| `apps/web` y `apps/backoffice` `nucleo/gateway.ts` | `packages/dominio-cliente/configuracion.ts` | import | `validarConfiguracion` |
| `apps/backoffice/rutas/operacion/dominio/cu13-consultar-saldo.ts` | `packages/dominio-cliente/posiciones.ts` | import | cálculo de gastable |
| `apps/web/src/app/verificadores/verificador-de-sorteo.ts` (L5) | `packages/dominio-cliente/sorteo.ts` | import | `barajarDeterminista`, `verificarCompromiso` |
| `packages/simulado` | `servicios/*/openapi/*.yaml` | generación (Prism + ejemplos) | `scripts/generar-ejemplos.mjs` |
| `clientes/{angular,dart}` | `servicios/*/openapi/*.yaml` | generación (`buildSrc/aportaya.openapi.gradle.kts`) | `.gitignore:50-51` |
| `packages/tokens` | `apps/*`, `packages/diseno_flutter`, `packages/ui` | artefactos `tokens.css` / `tokens.dart` | `scripts/a-css.mjs`, `a-dart.mjs` |

**Saltos de dinero del alcance** (regla 98.8.1). La app arma el importe (`CampoMonto`), luego el cliente Dart, el gateway y `nucleo-financiero`. El QR y el webhook no pasan por el frontend. El backoffice opera escrituras de dinero con `Idempotency-Key` opcional. Ningún componente compartido decide la entrega del pozo.

## 4. Distancia al flujo nuevo

### 4a. Inventario de aprobaciones (shell y compartido)

| ID | Dónde | Qué aprueba y quién | Estado / endpoint / permiso / pantalla / evento | Clasificación | Qué se rompe aguas abajo si se quita |
|---|---|---|---|---|---|
| AP-01 | `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart:12-89`, montado en `apps/movil/lib/navegacion/shell.dart:72-73` | Aprobación de identidad (KYC) por backoffice; la app consulta cada 30 s hasta ver `EstadoDeCuenta.verificada` y renueva el token | `estadoDeCuentaProvider` → renovar acceso; permisos `BILLETERA_VER`/grupos se recalculan al renovar (comentario l.12-19) | DECISIÓN REQUERIDA (A-1) | Sin la aprobación, la cuenta nunca recibe los permisos de billetera y grupos: no hay pago QR posible |
| AP-02 | `apps/movil/lib/navegacion/aviso_de_verificacion.dart:44-45`, `acceso_a_rutas.dart:53,71` | Misma aprobación KYC: banda del shell que muestra el estado (`APROBADA` …) y guardia `requiereVerificacion` de rutas de dinero/pasanaku | `EstadoDeVerificacionEstadoEnum.APROBADA` (cliente generado de identidad) | DECISIÓN REQUERIDA (A-1) | Igual que AP-01 |
| AP-03 | `apps/backoffice/src/app/nucleo/menu.ts:76`, `secciones.ts:19,27` | «Retiros en revisión»: segundo responsable aprueba retiros (ADR-049) | permiso `RETIRO_APROBAR`; ruta `/operacion/retiros`; texto `explicaciones-operacion.ts:38` («regla de los cuatro ojos») | DECISIÓN REQUERIDA (A-1). Retiro de billetera, fuera del circuito del pasanaku | Si se quita, los retiros sobre el tope salen sin segunda firma |
| AP-04 | `apps/backoffice/src/app/app.routes.ts:95-97`, `menu.ts:85-86`, `secciones.ts:52-53` | «Admisiones a grupos»: backoffice resuelve el ingreso de una persona a un grupo | permiso de ruta `ver:admisiones` → rol `ADMIN_PLATAFORMA`; pantalla `rutas/admisiones` (L3) | ELIMINABLE según R-NUEVA-1 (no se encontró norma en el repo que lo exija). Lo confirma L3 con el código del servicio | Bandeja de backoffice, código `AP-CU68-15`, tutorial de backoffice y pruebas `admisiones/*.spec.ts` |
| AP-05 | `apps/backoffice/src/app/nucleo/errores.ts:41` | Texto del código `AP-CU68-15`: «Este grupo exige admisión humana: la decisión del organizador es una propuesta y la resuelve backoffice.» | código de error del servicio `grupos` | ELIMINABLE (R-NUEVA-1); contradice además ADR-054 | El texto quedaría huérfano si el servicio deja de emitir el código |
| AP-06 | `apps/movil/lib/dominio/errores.dart:76-77` | Texto de `AP-CU69-10`: «Este grupo revisa cada ingreso antes de aceptarlo y por enlace no se puede entrar todavía.» | código de `grupos` (CU-69) | ELIMINABLE (R-NUEVA-1) | Mensaje de la app al unirse por enlace |
| AP-07 | `apps/backoffice/src/app/app.routes.ts:100-102`, `menu.ts:87`, `secciones.ts:54-55`, `errores.ts:42-46` | «Postulaciones a organizador» (CU-90): backoffice aprueba a quien quiere organizar | `ver:postulaciones` → `ADMIN_PLATAFORMA`; códigos `AP-CU90-03..08` | ELIMINABLE o DECISIÓN REQUERIDA: ADR-054 ya quitó el organizador humano obligatorio. Queda el organizador opcional, y no se encontró norma que exija aprobarlo | Bandeja de postulaciones (L3) |
| AP-08 | `apps/movil/lib/dominio/errores.dart:57` | `AP-CU20-02`: «Tu cuenta todavía no está habilitada para organizar grupos.» | habilitación de organizador (CU-20) | ELIMINABLE o DECISIÓN REQUERIDA (igual que AP-07) | Texto de la app |

### 4b. Inventario de respaldo y cobertura (textos públicos y compartidos)

| ID | Dónde | Qué promete o pone la empresa, y cuándo | Asiento en el mayor | Clasificación | Qué depende de ello |
|---|---|---|---|---|---|
| RE-01 | `apps/web/src/app/paginas/inicio/secciones/cierre.ts:71` | «Si igual no aporta, el fondo de garantía cubre la entrega para que quien tenía el turno cobre igual» | ninguno (texto público) | CONTRADICE R-NUEVA-2 | Página de inicio pública |
| RE-02 | `apps/web/src/app/paginas/inicio/secciones/producto.ts:75` | «Si alguien no aporta, el fondo cubre la entrega de quien tenía el turno» | ninguno | CONTRADICE R-NUEVA-2 | Inicio |
| RE-03 | `apps/web/src/app/paginas/inicio/secciones/transparencia.ts:65` y `:31` | Historial de ejemplo: «Aportaron 9 de 10 · el fondo de garantía cubrió el que faltó · recibió … Bs 2.500,00»: pozo completo con un faltante | ninguno | CONTRADICE R-NUEVA-2 | Inicio |
| RE-04 | `apps/web/src/app/paginas/inicio/secciones/hero.ts:31`, `apps/web/contenido/paginas/inicio.md:5` | Insignia «Fondo de garantía» en el hero y en la meta descripción | ninguno | CONTRADICE R-NUEVA-2 | SEO de la portada |
| RE-05 | `apps/web/contenido/paginas/como-funciona.md:20`, `apps/web/contenido/paginas/preguntas.md:20` | «Un fondo de garantía cubre la falta de un participante…» | ninguno | CONTRADICE R-NUEVA-2 | Páginas públicas generadas desde `contenido/` |
| RE-06 | `apps/web/src/app/app.routes.ts:117` | Misma promesa dentro del JSON-LD `FAQPage`, que leen los buscadores y los asistentes | ninguno | CONTRADICE R-NUEVA-2 | Datos estructurados indexados |
| RE-07 | `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-tablero.ts:25` | Explicador interno: «que el fondo de garantía comunitario cuente con respaldo suficiente para cubrir cualquier falta» | ninguno | DECISIÓN REQUERIDA (A-3) | Ayuda del tablero de backoffice |

### 4c. Ambigüedades

| ID | Lo que muestra el código del alcance hoy | A quién confirmar |
|---|---|---|
| A-1 | La app espera la aprobación de KYC (AP-01/02) y el backoffice conserva la doble aprobación de retiros (AP-03). No se tocó ni se asume eliminado | Pablo; Cumplimiento |
| A-2 | Ningún componente compartido decide la entrega con faltante. Los textos públicos prometen entrega completa (RE-01..06) | Pablo |
| A-3 | El cliente Dart de `garantia` expone `RespaldoApi` y `EntradaAmpliacionRespaldo` (`clientes/dart/garantia/lib/aportaya_cliente_garantia.dart:13,18`). Lo que pasa con eso lo decide L2 | Pablo |
| A-4 | Ningún componente compartido calcula mora ni recargo. El mock sirve `cubiertoMutual`/`faltante` (`aportes/consultarRecaudoDelPeriodo.json`) y el catálogo de la app trae un cobro de aporte contra saldo (`AP-CU21-03`, FLUJO-QR-002): falta decidir si el débito de saldo recargado por QR cuenta como «pago por QR» | Pablo |

## 5. Hallazgos

### frontends-compartidos-RESPALDO-001 — El sitio público promete que «el fondo de garantía cubre la entrega» cuando alguien no paga
- Clase / Severidad / Confianza: RESPALDO (también COHERENCIA y REG) / S2 / confirmado
- Ubicaciones: `apps/web/src/app/paginas/inicio/secciones/cierre.ts:71`, `apps/web/src/app/paginas/inicio/secciones/producto.ts:75`, `apps/web/src/app/paginas/inicio/secciones/transparencia.ts:65`, `apps/web/src/app/paginas/inicio/secciones/hero.ts:31`, `apps/web/contenido/paginas/inicio.md:5`, `apps/web/contenido/paginas/como-funciona.md:20`, `apps/web/contenido/paginas/preguntas.md:20`, `apps/web/src/app/app.routes.ts:117`
- Evidencia: `cierre.ts:71` → «Si igual no aporta, el fondo de garantía cubre la entrega para que quien tenía el turno cobre igual, y la deuda queda a nombre de quien incumplió.»; `transparencia.ts:65` → `{ mes: 'Julio', aportaron: 9, nota: ' · el fondo de garantía cubrió el que faltó', … }`, que se muestra con «Bs 2.500,00» recibido (l.31); `app.routes.ts:117` → mismo texto en el `FAQPage` JSON-LD.
- Cómo se comprobó: `git grep -n -i -E "respaldo|cobertura|cubr|faltante|garantiz"` sobre `apps/web` y lectura de cada línea.
- Impacto: promesa pública de pozo completo que, con R-NUEVA-2, la empresa ya no cumple. Es además distinta de lo que decía ADR-051, donde el fondo mutual es «legado» (D-15) y el único respaldo era la reserva empresarial. Un participante que se une confiando en esa promesa y no cobra completo tiene un reclamo fundado.
- Causa de fondo: los textos de marketing se escribieron contra el modelo de garantía de `docs/entidades/08_garantia_incumplimiento.md` (fondo mutual) y no los toca ningún verificador cuando cambia la política. No existe una fuente única del texto de la garantía.
- Solución de fondo: depende de A-2 (qué pasa con la entrega con faltante), que la toma Pablo. Con la decisión, reescribir los ocho puntos desde una sola fuente (`contenido/`) y agregar un barrido en `scripts/verificar_frontend.py` que falle ante «cubr…», «fondo de garantía» o «pozo completo» sin marca de política vigente.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: L2 (garantía y entregas), L5 (transparencia); app móvil si repite el texto (a revisar en su lote)
- Estándares y normas: ISO/IEC 25010 — adecuación funcional (corrección). Riesgo de protección al consumidor: «norma a identificar por Cumplimiento». Regla 91.3 (nada se promete antes de estar cobrado).
- Esfuerzo estimado: chico (textos) + decisión de negocio

### frontends-compartidos-COHERENCIA-001 — El sitio se contradice sobre la autorización ASFI y la custodia
- Clase / Severidad / Confianza: COHERENCIA (también REG) / S2 / confirmado
- Ubicaciones: `apps/web/src/app/app.routes.ts:119`, `apps/web/src/app/paginas/inicio/secciones/cierre.ts:72`, `apps/web/src/app/paginas/inicio/secciones/producto.ts:74`
- Evidencia: JSON-LD → «¿AportaYa está regulado por ASFI? — Tenemos una solicitud de licencia en trámite ante la ASFI.»; `cierre.ts:72` → «Todavía no … Hasta tenerla, AportaYa no custodia fondos del público.»; `producto.ts:74` → «Custodia separada: El dinero de los participantes no se mezcla con el de la plataforma. El encaje se verifica todos los días».
- Cómo se comprobó: lectura de los tres archivos.
- Impacto: tres afirmaciones regulatorias distintas en el mismo sitio. La de `producto.ts` afirma custodia en presente, lo que choca con el dossier H15, que condiciona la habilitación de dinero real.
- Causa de fondo: textos duplicados entre `contenido/*.md`, componentes de la portada y `app.routes.ts` (JSON-LD), sin fuente única.
- Solución de fondo: una sola fuente para el estado regulatorio (ya existe `apps/web/contenido/legal/estado-regulatorio.md`) que alimente portada, FAQ y JSON-LD. Texto validado por Cumplimiento.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L4 (cumplimiento)
- Estándares y normas: ISO/IEC 25010 — adecuación funcional. Norma a identificar por Cumplimiento.
- Esfuerzo estimado: chico

### frontends-compartidos-APROB-001 — El backoffice conserva y publicita la «admisión humana» de integrantes y la postulación a organizador
- Clase / Severidad / Confianza: APROB / S2 / confirmado (en el shell); el estado del servicio lo confirma L3
- Ubicaciones: `apps/backoffice/src/app/app.routes.ts:95-102`, `apps/backoffice/src/app/nucleo/menu.ts:85-88`, `apps/backoffice/src/app/nucleo/secciones.ts:52-55`, `apps/backoffice/src/app/nucleo/errores.ts:41-46`, `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-operacion.ts:86`, `apps/movil/lib/dominio/errores.dart:57,76-77`
- Evidencia: `errores.ts:41` → `'AP-CU68-15': 'Este grupo exige admisión humana: la decisión del organizador es una propuesta y la resuelve backoffice.'`; `menu.ts:88` → «Operación sigue abriendo en Admisiones, donde está la decisión del día».
- Cómo se comprobó: `git grep -n -i -E "aprob|postulaci|habilit|admisi"` en el shell y lectura.
- Impacto: el shell hace de la admisión la pantalla de aterrizaje de Operación. Mientras exista, el ingreso a un grupo depende de un operador, lo que contradice R-NUEVA-1 y también ADR-054 (D3: sin aprobar invitados).
- Causa de fondo: ADR-054 cambió la regla pero el shell (menú, aterrizaje, catálogo de errores) no se actualizó. El menú decide el «trabajo del día» con una cadena fija.
- Solución de fondo: cuando L3 retire la admisión del servicio, retirar la ruta, la sección, el permiso de vista y los códigos del catálogo en el mismo cambio. Mover el aterrizaje de Operación a una pantalla que no sea de aprobación.
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: L3 (grupos, organizador)
- Estándares y normas: ISO/IEC 25010 — adecuación funcional
- Esfuerzo estimado: medio

### frontends-compartidos-TRAZA-001 — El registro de lecturas de datos personales del backoffice está apagado y su aviso de error afirma algo falso
- Clase / Severidad / Confianza: TRAZA (también HUECO y SEG) / S2 / confirmado
- Ubicaciones: `apps/backoffice/src/app/nucleo/registro-de-acceso.interceptor.ts:17-25` (bandera `false`), `:60-68` (aviso)
- Evidencia: `factory: () => false,` con el comentario «`POST /extraccion/accesos` no existe en ningún contrato real»; aviso: «No se pudo registrar este acceso (correlación …). El equipo de auditoría ya quedó avisado.», cuando el comentario de l.61-65 admite que «No hay un `frontend-error-monitoring` real todavía».
- Cómo se comprobó: lectura completa del archivo; `python3 scripts/verificar_rutas_frontend.py` (exit 1) lista `POST /extraccion/accesos` y `POST /extraccion/exportaciones` como rutas sin contrato.
- Impacto: ninguna lectura de expediente hecha desde el backoffice deja rastro desde el cliente. Si se encendiera, el mensaje le diría al operador que auditoría quedó avisada cuando nadie lo fue. La regla 90.2.7 exige rastro de lectura de datos financieros.
- Causa de fondo: el contrato de lectura auditada no existe en `servicios/auditoria`. El cliente se adelantó al servidor.
- Solución de fondo: registrar la lectura en el servidor que sirve el dato (no confiar en que el cliente la informe) y quitar el interceptor, o publicar el contrato en `auditoria`. Corregir el texto del aviso.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L5 (auditoría)
- Estándares y normas: ISO/IEC 25010 — seguridad (responsabilidad / no repudio); OWASP ASVS (registro de seguridad); regla 90.2.7
- Esfuerzo estimado: medio

### frontends-compartidos-CONTRATO-001 — El verificador de rutas del frontend está en rojo y seis rutas que usa el frontend no tienen contrato
- Clase / Severidad / Confianza: CONTRATO / S3 / confirmado
- Ubicaciones: `scripts/verificar_rutas_frontend.py` (lista `CONOCIDAS_SIN_CONTRATO`); consumidores: `apps/backoffice/src/app/nucleo/registro-de-acceso.interceptor.ts`, `apps/backoffice/src/app/nucleo/exportador.ts`, `apps/backoffice/src/app/nucleo/tutoriales/almacen-remoto.ts`
- Evidencia (salida literal):
  ```text
  rutas distintas que usa el frontend: 104 · con contrato: 98 · sin contrato: 6
    [conocida] DELETE /identidad/tutoriales/progreso/{x}
    [conocida] GET /identidad/tutoriales/progreso
    [conocida] GET /publicidad/socios-comerciales/{x}
    [conocida] POST /extraccion/accesos
    [conocida] POST /extraccion/exportaciones
    [conocida] PUT /identidad/tutoriales/progreso/{x}
  FALLA · la lista de conocidas se pudrió: estas rutas YA tienen contrato (sacalas de CONOCIDAS_SIN_CONTRATO):
     - GET /grupos/solicitudes-escaladas
  FALLAS
  ```
- Cómo se comprobó: `python3 scripts/verificar_rutas_frontend.py` → exit 1.
- Impacto: el gate del frontend está rojo en `origin/test`. Seis llamadas pueden dar 404/405 en ejecución. Las de tutoriales y exportaciones están en el shell.
- Causa de fondo: el frontend consume rutas que el servidor no publica (ADR-045 sin implementar en `identidad`; servicio `extraccion` inexistente). La lista de excepciones se mantiene a mano.
- Solución de fondo: publicar los contratos o quitar las llamadas. Sacar `GET /grupos/solicitudes-escaladas` de la lista.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L4 (identidad), L5 (auditoría/publicidad), L3 (grupos)
- Estándares y normas: ISO/IEC 25010 — compatibilidad (interoperabilidad); OpenAPI 3; regla 96.4
- Esfuerzo estimado: chico/medio

### frontends-compartidos-CONTRATO-002 — `clientes/` está en `.gitignore` pero hay 26 archivos generados versionados a mano
- Clase / Severidad / Confianza: CONTRATO (también PISADA) / S3 / confirmado
- Ubicaciones: `.gitignore:50-51`; `git ls-files clientes` → 4 archivos en `clientes/angular/publicidad/model/` y 22 en `clientes/dart/*/{lib/*.dart,pubspec.yaml}`
- Evidencia: `git check-ignore -v clientes/angular/aportes/api.ts` → `.gitignore:50:clientes/angular/`; el último commit que tocó `clientes/angular/publicidad/model/entradaCampana.ts` es `60f9fd1e` («Subida a TEST desde justin/feature/…»).
- Cómo se comprobó: `git ls-files`, `git check-ignore`, `git log`.
- Impacto: fragmentos versionados de un artefacto que se regenera. Un clon limpio los trae viejos y un generado nuevo los pisa sin que el diff lo muestre (están ignorados para cambios nuevos, pero no para los ya trackeados).
- Causa de fondo: `git add -f` o archivos agregados antes de la regla de ignore, durante una subida masiva a TEST.
- Solución de fondo: `git rm --cached` de esos 26 archivos y generar los clientes siempre desde el artifact por SHA (`docs/auditoria-recursos/FINAL.md` §20).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los clientes
- Estándares y normas: ISO/IEC 25010 — mantenibilidad (modificabilidad)
- Esfuerzo estimado: chico

### frontends-compartidos-PARCHE-001 — El vigilante de aprobación traga cualquier error, también los de programación
- Clase / Severidad / Confianza: PARCHE (también OBSERV) / S4 / confirmado
- Ubicaciones: `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart:84-86`
- Evidencia: `} on Object { // Sin red o con el servidor caído: se vuelve a intentar en la próxima vuelta. }`
- Cómo se comprobó: lectura completa.
- Impacto: un error de tipos o de contrato del cliente generado queda silenciado para siempre: la persona ve «verificando» y la consulta se repite cada 30 s sin registro.
- Causa de fondo: no se distinguen los fallos de red (reintentables) del resto.
- Solución de fondo: capturar solo `ErrorDeApi`/`DioException` de red y reportar el resto con el registrador seguro de `dominio/diagnostico_api.dart`.
- Relación con el flujo nuevo: APROB (A-1)
- Conexiones y módulos afectados: L4 (identidad)
- Estándares y normas: ISO/IEC 25010 — fiabilidad (tolerancia a fallos); Effective Dart (no capturar `Object`)
- Esfuerzo estimado: chico

### frontends-compartidos-DEFECTO-001 — La clave de idempotencia del backoffice vence a los 15 minutos sin respuesta definitiva y vive en un mapa global del módulo
- Clase / Severidad / Confianza: DEFECTO (también DINERO) / S3 / probable (falta ver cuánto conserva el servidor la clave y si la acota por actor)
- Ubicaciones: `apps/backoffice/src/app/nucleo/idempotencia.interceptor.ts:27-33` (vigencia), `:35-36` (`const intentos = new Map`), `:58-80`
- Evidencia: `export const VIGENCIA_DE_CLAVE_MS = 15 * 60_000` — «Pasado ese tiempo, repetir el mismo contenido se toma como una operación nueva a propósito»; `const intentos = new Map<string, Intento>()` a nivel de módulo, con huella `método + URL + cuerpo` sin el actor.
- Cómo se comprobó: lectura completa.
- Impacto: (1) Un cobro o cierre que quedó en 5xx o timeout y se reintenta pasados 15 minutos sale con otra clave: el servidor lo puede ejecutar dos veces. (2) Si un operador cierra sesión y otro repite el mismo contenido dentro de la ventana, se reutiliza la clave del primero. Que eso duplique o bloquee depende de si el servidor acota la clave por actor.
- Causa de fondo: la regla de «misma operación» la decide el cliente con un tiempo literal, en lugar de una clave atada al intento de negocio (orden, cierre), que es la que el servidor puede deduplicar.
- Solución de fondo: derivar la clave de la identidad del intento de negocio (p. ej. `ordenId` o `cierreId`), no del reloj; limpiar `intentos` al cerrar sesión (`layout/dominio/cerrar-sesion.ts`).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todas las pantallas del backoffice que usan `contextoIdempotente()` (21 archivos en `rutas/`)
- Estándares y normas: ISO/IEC 25010 — fiabilidad; regla 91.1.4, 96.3.3
- Esfuerzo estimado: medio

### frontends-compartidos-FLUJO-QR-001 — La app trae empaquetado el «simular pago» de un QR de prueba; lo apaga solo una propiedad del servidor
- Clase / Severidad / Confianza: FLUJO-QR / S3 / confirmado (cliente); el control del servidor lo audita L1
- Ubicaciones: `apps/movil/lib/dominio/cu10_pago_de_prueba.dart:9-45`; servidor: `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/RecargasProveedorLocal.java:33`, `…/web/RecargasDePruebaController.java:37`, `nucleo-financiero.yaml:221`
- Evidencia: «CU-10 · «Simular pago» de un QR **de prueba**. Solo existe donde el servidor tiene el proveedor de prueba (TEST); en producción responde 404»; servidor: `@ConditionalOnProperty(name = "aportaya.recarga.qr.modo", havingValue = "prueba")`.
- Cómo se comprobó: lectura del archivo y `git grep` del punto de contacto en el servidor.
- Impacto: existe un camino que acredita saldo sin webhook de pasarela. Su único cierre es una propiedad de despliegue: una configuración equivocada en producción habilita la acreditación sin pago y la app ya trae el botón.
- Causa de fondo: el modo de prueba vive en el mismo binario y en el mismo contrato público que el modo real.
- Solución de fondo: (L1) que el arranque falle si `modo=prueba` en un perfil productivo; (app) compilar la acción solo con un `--dart-define` de entorno de prueba.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: L1 (nucleo-financiero, aportes)
- Estándares y normas: ISO/IEC 25010 — seguridad (integridad); regla 91.3.1
- Esfuerzo estimado: chico

### frontends-compartidos-DEFECTO-002 — App móvil: la clave de idempotencia de recarga, retiro y transferencia es fija por cuenta y sobrevive al rechazo; corregir el importe choca con la clave vieja
- Clase / Severidad / Confianza: DEFECTO (también DINERO) / S2 / confirmado (lectura de cliente y servidor); el bloqueo en ejecución NO COMPROBADO
- Ubicaciones: `apps/movil/lib/dominio/cu10_recargar_saldo.dart:17,33-35,45`, `apps/movil/lib/dominio/cu11_retirar_saldo.dart:12,29-31,42`, `apps/movil/lib/dominio/cu12_transferir_saldo.dart:12,30-32,46-48`, `apps/movil/lib/proveedores/idempotencia.dart:11-17,26-28,31-32`; servidor: `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/idempotencia/Idempotencia.java:70,153`
- Evidencia: `static String formularioId(String cuentaId) => 'cu10-recarga-$cuentaId';` · la clave solo se libera en el camino feliz (`…cerrar(formularioId(cuentaId))` tras la respuesta 2xx) y el `catch` no la cierra · `idempotenciaProvider` es `NotifierProvider` (no `autoDispose`) · servidor: «Si habia una con OTRO cuerpo, lanza `IdempotenciaConflicto`».
- Cómo se comprobó: lectura completa de los cuatro archivos y del punto de contacto en `comun-web` (hallazgo del lector D, re-verificado).
- Impacto: tras un rechazo (422 por monto, MFA o destino inválido), la persona corrige y reenvía con la MISMA clave y otro cuerpo. El servidor responde conflicto de idempotencia, y la operación no se puede repetir con el dato correcto en toda la sesión de la app para esa cuenta. No duplica dinero, porque el servidor lo protege, pero bloquea la recarga o el retiro. Además, el comentario `cu10_recargar_saldo.dart:12-15` promete «un provider por pantalla», cosa que la clave por cuenta no cumple.
- Causa de fondo: la identidad del intento se ata a la cuenta y no al contenido. No se distingue el rechazo definitivo (4xx, que debería liberar la clave) del resultado desconocido (timeout o 5xx, que debe conservarla). El backoffice resolvió lo mismo de otra forma (DEFECTO-001): dos políticas de idempotencia en el mismo producto.
- Solución de fondo: una sola política para web y móvil. Clave por intento de negocio, liberada ante un 4xx definitivo y conservada ante un resultado desconocido. Prueba de unidad del ciclo rechazo → corrección → reenvío.
- Relación con el flujo nuevo: FLUJO-QR (la recarga es la entrada de dinero por QR)
- Conexiones y módulos afectados: L1 (nucleo-financiero); DUP-X con `apps/backoffice/src/app/nucleo/idempotencia.interceptor.ts`
- Estándares y normas: ISO/IEC 25010 — fiabilidad (recuperabilidad); regla 91.1.4, 96.3.3
- Esfuerzo estimado: medio

### frontends-compartidos-DEFECTO-003 — Tres instancias de `Refrescador`: el «un solo refresco en vuelo» no es global
- Clase / Severidad / Confianza: DEFECTO (también RESILIENCIA) / S2 / probable (estructura confirmada; la carrera y la revocación por reuso del refresh NO COMPROBADAS en ejecución)
- Ubicaciones: `apps/movil/lib/dominio/refrescador.dart:5-17` (single-flight por instancia), `apps/movil/lib/dominio/cliente.dart:90`, `apps/movil/lib/dominio/renovar_acceso.dart:13-17` (instancia nueva en cada llamada), `apps/movil/lib/pantallas/pasanaku/dominio/habilitacion_basica.dart:101` (otra más); disparo periódico en `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart:63,77`
- Evidencia: `Future<bool> refrescar() => _enVuelo ??= _hacerRefresco()…` con `_enVuelo` como campo de instancia; `renovar_acceso.dart:16` → `return Refrescador(ref.read(sesionProvider), sinInterceptores).refrescar();`
- Cómo se comprobó: `git grep -n "Refrescador(" -- apps/movil/lib` → 3 construcciones; lectura de las tres.
- Impacto: si el vigilante (cada 30 s) renueva mientras el interceptor también refresca por un 401, salen dos `POST /sesion/refrescar` con el mismo refresh. Si el servidor rota y detecta reuso (ADR-024/030), revoca la familia y la persona queda deslogueada. El refresco sale además sin `x-request-id` (Dio sin interceptores).
- Causa de fondo: el single-flight vive en la instancia y no en un proveedor único de la sesión.
- Solución de fondo: un único `Refrescador` provisto por Riverpod (`Provider` ligado a la sesión) que usen interceptor, vigilante y habilitación. Propagar la correlación al refresco.
- Relación con el flujo nuevo: APROB (A-1, lo dispara la espera de la aprobación KYC)
- Conexiones y módulos afectados: L4 (identidad, sesiones)
- Estándares y normas: ISO/IEC 25010 — fiabilidad; OWASP ASVS (gestión de sesión)
- Esfuerzo estimado: chico

### frontends-compartidos-PRUEBAS-001 — `test:front` de la app no corre `test/billetera` ni `test/navegacion`
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado
- Ubicaciones: `apps/movil/package.json:12`; huérfanas: `apps/movil/test/navegacion/vigilante_de_aprobacion_test.dart`, `apps/movil/test/billetera/*_test.dart` (4); CI: `.github/workflows/ci.yml:340` (usa `test:front`), `:609`, `:646` (archivos sueltos)
- Evidencia: `"test:front": "flutter test test/unidad test/widget test/contrato test/identidad test/pasanaku --timeout 60s"`
- Cómo se comprobó: `ls apps/movil/test` muestra `billetera` y `navegacion`; `git grep "test/billetera\|test/navegacion" -- .github` no da resultados.
- Impacto: las pruebas de las pantallas de dinero de la billetera y del vigilante de aprobación pueden romperse sin que nadie se entere.
- Causa de fondo: la lista de carpetas está escrita a mano en el script.
- Solución de fondo: `flutter test test --exclude-tags goldens,a11y` o un verificador que falle si hay una carpeta de pruebas fuera de algún script.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L1 (pantallas de billetera)
- Estándares y normas: ISO/IEC 25010 — mantenibilidad (capacidad de prueba); regla 80.1
- Esfuerzo estimado: chico

### frontends-compartidos-CONFIG-001 — Android: el release se firma con la clave de depuración y los App Links apuntan por omisión a TEST
- Clase / Severidad / Confianza: CONFIG (también SEG) / S3 / confirmado
- Ubicaciones: `apps/movil/android/app/build.gradle.kts:36-40`, `:26-27`; `apps/movil/android/app/src/main/AndroidManifest.xml:49-55`; `.github/workflows/apk-de-prueba.yml:77-92`
- Evidencia: `// TODO: Add your own signing config for the release build.` / `signingConfig = signingConfigs.getByName("debug")`; `?: "app.aportaya.161.97.85.216.sslip.io"`.
- Cómo se comprobó: lectura. El workflow `apk-de-prueba` re-firma con una keystore de depuración guardada como secreto, y se documenta como APK de prueba.
- Impacto: no hay configuración de firma de producción, y un `flutter build apk --release` sin parámetros sale firmado con la clave de depuración y con App Links hacia TEST. El `TODO` sustituye una implementación (regla 00 §5.4). Se baja a S3 porque hoy solo se distribuye el APK de prueba.
- Causa de fondo: la plantilla de Flutter quedó sin completar.
- Solución de fondo: `signingConfigs.release` desde secretos de CI, y que el build de release falle sin ella y sin `hostDeLaAppWeb` explícito.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: despliegue
- Estándares y normas: OWASP MASVS (resiliencia / configuración); ISO/IEC 25010 — seguridad
- Esfuerzo estimado: chico

### frontends-compartidos-HUECO-001 — Enlace profundo `aportaya://aporte` lleva a `/billetera/aportes`, que no existe
- Clase / Severidad / Confianza: HUECO / S3 / confirmado
- Ubicaciones: `apps/movil/lib/navegacion/enlaces_profundos.dart:16`; aceptado también por avisos push en `apps/movil/lib/navegacion/ruta_del_aviso.dart:46-48`
- Evidencia: `'aporte': '/billetera/aportes',`
- Cómo se comprobó: `git grep -n "'/billetera/aportes'\|path: 'aportes'" -- apps/movil/lib` → solo la línea del mapa.
- Impacto: el recordatorio de aporte (el enlace que lleva a pagar por QR) termina en una ruta inexistente. Es justo el camino que, con R-NUEVA-1, debe mover el flujo.
- Causa de fondo: mapa de prefijos escrito a mano, sin prueba contra el árbol de rutas.
- Solución de fondo: generar el mapa desde `rutas.dart`, o una prueba que resuelva cada prefijo contra el router.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: L1 (aportes), L5 (notificaciones)
- Estándares y normas: ISO/IEC 25010 — adecuación funcional
- Esfuerzo estimado: chico

### frontends-compartidos-UX-A11Y-001 — La guardia de identidad cubre solo parte de las rutas de dinero
- Clase / Severidad / Confianza: UX-A11Y (no SEG: la autorización la decide el servidor, regla 95.6.1) / S3 / confirmado en cliente
- Ubicaciones: `apps/movil/lib/navegacion/acceso_a_rutas.dart:17-22`; rutas no listadas en `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:7,15,25,44`
- Evidencia: `_exigenIdentidad` solo lista `recargar|retirar|transferir`, `grupos/nuevo`, `pedir-cupo` y `obligaciones/…/aportar`.
- Cómo se comprobó: lectura; el lector D listó las rutas de retiro del grupo, traspaso, subasta y permuta.
- Impacto: en esas rutas la persona sin identidad verificada llena el formulario y recién al enviar recibe un 403. Lo esperable es que se la lleve antes a verificar.
- Causa de fondo: lista de expresiones regulares paralela al árbol de rutas.
- Solución de fondo: marcar en la definición de cada ruta si exige identidad.
- Relación con el flujo nuevo: APROB (A-1)
- Conexiones y módulos afectados: L3, L4
- Estándares y normas: ISO/IEC 25010 — usabilidad
- Esfuerzo estimado: chico

### frontends-compartidos-DEFECTO-004 — Protección de captura de pantallas de dinero por mixin sin contador
- Clase / Severidad / Confianza: DEFECTO (también SEG) / S3 / probable (falta ejercitar el apilado en un dispositivo)
- Ubicaciones: `apps/movil/lib/dominio/proteccion_de_pantalla_dinero.dart:11-27`; implementación `apps/movil/lib/infraestructura/android/proteccion_pantalla_android.dart:14-28`; 5 usuarios (sesión, retirar, extracto, recargar, transferir)
- Evidencia: `initState → _proteccion.activar()` / `dispose → _proteccion.desactivar()`, sin conteo de referencias.
- Cómo se comprobó: lectura.
- Impacto: si una pantalla protegida se apila sobre otra protegida (p. ej. extracto sobre retirar) y luego se cierra, se desactiva la protección de la que queda visible: saldo y datos bancarios capturables.
- Causa de fondo: estado global (FLAG_SECURE) manejado como si fuera local de la pantalla.
- Solución de fondo: contador en el puerto (`activar` incrementa, `desactivar` decrementa).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L1 (billetera), L4 (sesión)
- Estándares y normas: OWASP MASVS-PLATFORM; ISO/IEC 25010 — seguridad (confidencialidad)
- Esfuerzo estimado: chico

### frontends-compartidos-FLUJO-QR-002 — El catálogo de la app contempla cobrar el aporte contra el saldo de la billetera
- Clase / Severidad / Confianza: FLUJO-QR (también COHERENCIA) / S3 / sospecha (el camino de cobro lo audita L1)
- Ubicaciones: `apps/movil/lib/dominio/errores.dart:88`
- Evidencia: `'AP-CU21-03': 'No pudimos cobrar. Revisá tu saldo e intentá de nuevo.',`
- Cómo se comprobó: lectura del catálogo.
- Impacto: existe, al menos en el contrato de errores, un camino de aporte por débito de saldo distinto del QR. R-NUEVA-1 dice que lo único que mueve el flujo es el pago por QR, así que hay que decidir si el débito de saldo (fondeado antes por QR) cuenta como pago.
- Causa de fondo: R-NUEVA-1 no define si «pago por QR» incluye el saldo ya recargado por QR.
- Solución de fondo: decisión de Pablo (se agrega a A-2/A-4 como pregunta) y alinear L1.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: L1
- Estándares y normas: regla 00 §1.7 (ambigüedad registrada)
- Esfuerzo estimado: chico (decisión)

### frontends-compartidos-FRAGIL-001 — Catálogos de errores escritos a mano en cada cliente; los códigos con dos significados se desambiguan por la URL
- Clase / Severidad / Confianza: FRAGIL (también DUP-X) / S3 / confirmado
- Ubicaciones: `apps/movil/lib/dominio/errores.dart:14-90` (50 códigos `AP-`), `apps/movil/lib/dominio/errores_catalogados.dart:10-29`, `apps/movil/lib/dominio/traduccion_de_errores.dart:50-68`; `apps/backoffice/src/app/nucleo/errores.ts` (74 códigos `AP-`); `apps/backoffice/src/app/nucleo/errores.ts:22-23` («Algunos números los reutilizan operaciones distintas del servidor»)
- Evidencia: `if (path.startsWith('/recargas-de-prueba/')) { return ContextoErrorApi.pagoDeRecargaDePrueba; }` y otras cuatro coincidencias por subcadena de ruta.
- Cómo se comprobó: lectura; `grep -c "'AP-"` en ambos catálogos.
- Impacto: si el servidor renombra una ruta o reutiliza un código, el texto que ve la persona cambia de significado sin que falle ninguna prueba. Es la cadena «código del servicio → texto del cliente» que el metaprompt pide señalar (FRAGIL).
- Causa de fondo: los códigos de error no se publican como catálogo versionado en el contrato (RFC 9457 `type`). Cada cliente los copia.
- Solución de fondo: códigos únicos por significado en el servidor, publicados en el OpenAPI, y catálogos de cliente generados o verificados contra él.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios
- Estándares y normas: RFC 9457; ISO/IEC 25010 — mantenibilidad; regla 96.4.4
- Esfuerzo estimado: medio

### frontends-compartidos-REESCRITURA-001 — Código sin referencias en el shell móvil, parte de él presentado como control
- Clase / Severidad / Confianza: REESCRITURA (también CLEAN) / S3 / confirmado
- Ubicaciones: `apps/movil/lib/navegacion/guardias.dart` (redirige a `/identidad/aumentar-limite` y `/soporte/servicio-no-habilitado`, l.17 y 24; ninguna existe), `apps/movil/lib/proveedores/conexion.dart:11-18` (`hayConexionProvider`), `apps/movil/lib/dominio/validacion.dart` (`montoValido`, `carnetValido`), `apps/movil/lib/proveedores/dispositivo.dart` (`dispositivoIdProvider`), `apps/movil/lib/main_h6_s3_m8_profile.dart` (149 líneas, punto de entrada de una microtarea de perfil)
- Evidencia: `hayConexionProvider` documenta «para bloquear operaciones de dinero con motivo visible (regla 1 del shell)».
- Cómo se comprobó: `git grep -l <símbolo> -- lib test integration_test test_driver` devuelve solo el archivo que lo define (para `guardias.dart`, ningún import).
- Impacto: quien lea el shell cree que las operaciones de dinero se bloquean sin red y que hay guardias de límite. Ninguna de las dos cosas pasa.
- Causa de fondo: reescrituras de agentes que dejaron la versión vieja.
- Solución de fondo: borrar o conectar, con su prueba. Si el bloqueo sin conexión es requisito (regla 95.2.4), implementarlo.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguno fuera del shell
- Estándares y normas: ISO/IEC 25010 — mantenibilidad (analizabilidad); ISO/IEC 5055 (código muerto)
- Esfuerzo estimado: chico

### frontends-compartidos-DUP-INT-001 — Dos configuraciones de idioma; la prueba valida la que la app no usa
- Clase / Severidad / Confianza: DUP-INT (también PRUEBAS) / S4 / confirmado
- Ubicaciones: `apps/movil/lib/localizacion.dart:10-15` (`es_BO` + `es`, solo `GlobalMaterialLocalizations.delegates`) frente a `apps/movil/lib/app_real.dart:18-24` (solo `es_BO`, tres delegados); prueba `apps/movil/test/widget/idioma_test.dart:4`
- Evidencia: `const idiomasDeLaApp = <Locale>[Locale('es', 'BO'), Locale('es')];` frente a `const localesDeAportaYa = <Locale>[localeDeAportaYa];`
- Cómo se comprobó: `git grep -n "localizacion.dart" -- lib test` → solo pruebas y capturas lo importan.
- Impacto: la prueba de idioma pasa aunque la app real cambie de configuración.
- Causa de fondo: clon divergente.
- Solución de fondo: una sola constante usada por la app y por la prueba.
- Relación con el flujo nuevo: ninguna · Conexiones: ninguna
- Estándares y normas: ISO/IEC 25010 — mantenibilidad
- Esfuerzo estimado: chico

### frontends-compartidos-DOC-001 — El avance de tutoriales no se borra al cerrar sesión aunque el comentario dice que sí
- Clase / Severidad / Confianza: DOC (también TRAZA y privacidad) / S4 / confirmado
- Ubicaciones: `apps/movil/lib/infraestructura/almacen_de_progreso_seguro.dart:14-16`; `apps/movil/lib/proveedores/sesion.dart:143-147`
- Evidencia: «el almacén seguro se limpia junto con la sesión — el avance de los tutoriales de una persona no queda en el teléfono después de que se fue»; `_borrarTokens` borra solo `_claveAcceso`, `_claveRefresco` y `_claveInicio`.
- Cómo se comprobó: lectura de ambos.
- Impacto: en un teléfono compartido queda el rastro de uso de la persona anterior. Es dato de comportamiento, no financiero.
- Causa de fondo: el comentario describe una intención que no se implementó.
- Solución de fondo: borrar las claves de progreso en el cierre de sesión, con su prueba.
- Relación con el flujo nuevo: ninguna · Conexiones: ninguna
- Estándares y normas: ISO/IEC 25010 — seguridad (confidencialidad); regla 90.2
- Esfuerzo estimado: chico

### frontends-compartidos-RESPALDO-002 — El mock y los clientes generados publican y normalizan la cobertura empresarial del pozo (`cubiertoEmpresa`) y toda la familia de respaldo
- Clase / Severidad / Confianza: RESPALDO / S2 en este alcance (el camino vivo del servidor es S1 y lo audita L2) / confirmado
- Ubicaciones: `packages/simulado/ejemplos/entregas/fondearPozo.json:5,25` (`POST /entregas/pozo-completo`, `cubiertoEmpresa`, `cubiertoMutual`); `packages/simulado/ejemplos/garantia/{cubrirConRespaldo,reservarRespaldo,ampliarRespaldo,liberarRespaldo,recuperarRespaldo,reversarCoberturaRespaldo,cubrirIncumplimiento}.json`; `packages/simulado/ejemplos/aportes/consultarRecaudoDelPeriodo.json` (`cubiertoMutual`, `faltante`); `clientes/dart/entregas/lib/aportaya_cliente_entregas.dart:20,38`; `clientes/dart/garantia/lib/aportaya_cliente_garantia.dart:13,18` (`respaldo_api.dart`, `entrada_ampliacion_respaldo.dart`); catálogo de operaciones del backoffice `apps/backoffice/src/app/rutas/sistemas/operaciones/dominio/operaciones.json:9917-10102,16804-16957`; fuente `servicios/entregas/…/openapi/entregas.yaml:792,800`
- Evidencia: `"ruta": "/entregas/pozo-completo"` … `"cubiertoEmpresa": { "monto": "4.44", "moneda": "BOB" }`.
- Cómo se comprobó: lectura de los ejemplos; `ls packages/simulado/ejemplos/garantia | grep -i respaldo` → 6 archivos; `git grep -n -i "fondearPozo|cubiertoEmpresa|cubrirConRespaldo|RespaldoApi" -- apps` → ninguna pantalla llama a esas operaciones; solo aparecen en el catálogo `operaciones.json`.
- Impacto: el entorno simulado (`yarn dev:mock`) y los clientes enseñan como normal un flujo que R-NUEVA-2 prohíbe. Cualquier pantalla nueva construida contra el mock nace con el respaldo empresarial. Cuando L2 retire la operación, el mock, los clientes y el catálogo quedan desalineados si no se regeneran.
- Causa de fondo: el mock y los clientes se generan del OpenAPI vigente. El problema es de la fuente (L2) y se propaga sin una lista de operaciones retiradas.
- Solución de fondo: depende de A-2/A-3 (Pablo). Retirar o versionar la operación en `entregas.yaml`/`garantia.yaml` y regenerar mock, clientes y `operaciones.json` en el mismo cambio.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: L2 (entregas, garantía), L1 (aportes)
- Estándares y normas: regla 91.5 y R-NUEVA-2; ISO/IEC 25010 — adecuación funcional
- Esfuerzo estimado: chico aquí (regenerar) tras la decisión

### frontends-compartidos-APROB-002 — El mock sirve pasos de aprobación humana como camino normal
- Clase / Severidad / Confianza: APROB / S3 / confirmado
- Ubicaciones: `packages/simulado/ejemplos/grupos/canjearInvitacion.json:11` (`PENDIENTE_BACKOFFICE`), `grupos/decidirSolicitudDeIngreso.json`, `garantia/cubrirIncumplimiento.json:17,31,45,59` (`requiereAprobacion`), `aportes/aprobarReembolso.json`, `nucleo-financiero/aprobarRetiro.json`, `garantia/aprobarReemplazo.json`, `organizador/{aprobarPostulacion,revisarHabilitacion,resolverHabilitacion,aprobarTarea}.json`
- Evidencia: `"requiereAprobacion": true` (`cubrirIncumplimiento.json:17`).
- Cómo se comprobó: `git grep -n -E "requiereAprobacion|PENDIENTE_BACKOFFICE" -- packages/simulado/ejemplos` y lectura (lector B, re-verificado).
- Impacto: igual que RESPALDO-002, pero para R-NUEVA-1. El inventario por operación lo cierran L1–L4.
- Causa de fondo, solución y conexiones: como RESPALDO-002, con fuente en cada `openapi/*.yaml`.
- Relación con el flujo nuevo: APROB
- Estándares y normas: R-NUEVA-1
- Esfuerzo estimado: chico tras decisión

### frontends-compartidos-SEG-001 — El validador del gateway acepta `/\host-ajeno/api/v1` como ruta relativa, y el navegador lo resuelve a otro host
- Clase / Severidad / Confianza: SEG / S3 (la URL sale de una meta inyectada por NGINX: explotarlo exige alterar la configuración o el HTML servido) / confirmado por ejecución
- Ubicaciones: `packages/dominio-cliente/src/configuracion.ts:52,80-89`; consumidores `apps/backoffice/src/app/nucleo/gateway.ts:2,33`, `apps/web/src/app/nucleo/gateway.ts:2,43`
- Evidencia: `if (cruda.startsWith('//')) return { valida: false … }` y luego `if (cruda.startsWith('/')) { … return { valida: true, url: ruta } }`, con `RUTA_VERSIONADA = /\/v\d+\/?$/`.
- Cómo se comprobó:
  ```text
  $ node -e 'console.log(new URL("/\\evil.example.com/api/v1","https://app.aportaya.bo/x").href)'
  https://evil.example.com/api/v1
  ```
- Impacto: el control que impide mandar el `Bearer` del operador a un host ajeno se saltea con una barra invertida. Toda petición del backoffice o de la web saldría a ese host, incluido el token.
- Causa de fondo: se validó la cadena con prefijos, en lugar de resolver la URL con `new URL(cruda, origen)` y comparar el `origin`.
- Solución de fondo: resolver siempre contra el origen propio y exigir `origin === origenPropio` o un host de la lista blanca. Agregar a `configuracion.spec.ts` los casos `/\`, `/%5C` y `\\`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: backoffice, web
- Estándares y normas: CWE-601 / CWE-20; OWASP ASVS (validación de configuración); ISO/IEC 25010 — seguridad
- Esfuerzo estimado: chico

### frontends-compartidos-CONTRATO-003 — El patrón de importe de `cumplimiento.yaml` está mal escapado; el mock lo «cumple» con basura y una respuesta real lo violaría
- Clase / Severidad / Confianza: CONTRATO (también DINERO) / S2 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/resources/openapi/cumplimiento.yaml:1251,1339` (fuente, L4); `packages/simulado/ejemplos/cumplimiento/elevarDiligencia.json:18`, `verificarAlcance.json:14`
- Evidencia: `monto: { type: string, pattern: '^-?\\d+\\.\\d{2}$' }`. En YAML con comillas simples, `\\` son dos barras literales: el patrón exige una barra invertida antes de cada dígito. El mock trae `"monto": "\\d\\O\\dd"`.
- Cómo se comprobó: lectura del YAML y del ejemplo; el lector B validó los 349 ejemplos con jsonschema (0 inválidos). Que pasen es el síntoma: el mock satisface el patrón roto.
- Impacto: un validador de contrato o un cliente que aplique el patrón rechaza `"100.00"`. Las pruebas contra el mock no lo detectan. Además, el patrón admite importes negativos (`-?`), contra la regla 91.2.2 para coberturas y deducciones.
- Causa de fondo: el escape de YAML no se verificó. Ninguna prueba contrasta el contrato con un valor real.
- Solución de fondo: (L4) corregir a `"^\\d+\\.\\d{2}$"` (comillas dobles) o `'^\d+\.\d{2}$'`. Que el generador del mock use ejemplos reales del contrato y agregar a la CI un caso real por patrón.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L4 (cumplimiento)
- Estándares y normas: OpenAPI 3 / JSON Schema; ISO/IEC 25010 — compatibilidad
- Esfuerzo estimado: chico

### frontends-compartidos-DINERO-001 — El cliente compartido calcula «gastable» y recorta en silencio lo comprometido que excede lo disponible
- Clase / Severidad / Confianza: DINERO (también DDD) / S3 / confirmado
- Ubicaciones: `packages/dominio-cliente/src/posiciones.ts:4-14,80-101`; consumidor `apps/backoffice/src/app/rutas/operacion/dominio/cu13-consultar-saldo.ts`
- Evidencia: `const comprometido = excede ? disponible : informado` · `gastable: aCadena(disponible - comprometido)` · `advertencias: excede ? ['comprometido-excede-disponible'] : []`; comentario l.7 «(el productor todavía NO implementa) agrega … `comprometidoEnPozos`», cuando `nucleo-financiero.yaml:1513-1515` ya lo publica.
- Cómo se comprobó: lectura del archivo y `git grep comprometidoEnPozos` en el OpenAPI.
- Impacto: la cifra «gastable» que ve el operador no viene del mayor sino de una resta en el navegador (regla 91.1.5). Cuando el servidor informa un comprometido mayor al disponible, que es un estado incoherente del libro, el cliente lo recorta y solo deja una advertencia. Lo hace en enteros de centavos, sin `float`.
- Causa de fondo: el contrato no entrega `gastable` y el cliente lo deduce.
- Solución de fondo: que `nucleo-financiero` devuelva `gastable` calculado desde el mayor. Ante la incoherencia, el cliente muestra error y no un número recortado. Actualizar el comentario.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L1 (nucleo-financiero)
- Estándares y normas: regla 91.1.5, 98.4.2; ISO/IEC 25010 — adecuación funcional (corrección)
- Esfuerzo estimado: chico

### frontends-compartidos-DUP-X-001 — `dominio-cliente/cadena.ts` es un clon divergente del hash de bloque de `transparencia`
- Clase / Severidad / Confianza: DUP-X (también DOC) / S3 / confirmado
- Ubicaciones: `packages/dominio-cliente/src/cadena.ts:17-28`, exportado en `packages/dominio-cliente/src/index.ts:3-4`; servidor `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/CadenaDeBloques.java:71-75`
- Evidencia: Java → `sha256("%d|%s|%s|%s|%s".formatted(numero, hashAnterior, raizMerkle, desde, hasta))`; el cliente encadena `hash_anterior || hash_contenido` con otro material y otro génesis. El comentario del cliente dice que el backend no tiene implementación propia.
- Cómo se comprobó: lectura de ambos; `git grep -n "recorrerCadena|hashDeBloque" -- apps` → ningún uso en ejecución (solo comentarios en `verificador-de-cadena.ts:12,37`).
- Impacto: el día que la web use el verificador del paquete, ninguna cadena real va a verificar: las personas verán «cadena rota» sobre datos íntegros. Hoy está latente.
- Causa de fondo: el cliente se escribió antes que la clase del servidor y no se reconcilió.
- Solución de fondo: vectores de prueba compartidos generados desde `CadenaDeBloques` (como ya se hace con `sorteo.vectores.json`) y alinear el algoritmo, o retirar el código del paquete.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: L5 (transparencia)
- Estándares y normas: ISO/IEC 25010 — compatibilidad / corrección
- Esfuerzo estimado: medio

### frontends-compartidos-DATOS-001 — El mock trae monedas inválidas, importes idénticos en todos los campos y escenarios «adverso» que copian a «ok»
- Clase / Severidad / Confianza: DATOS (también PRUEBAS) / S3 / confirmado
- Ubicaciones: `packages/simulado/ejemplos/aportes/consultarRecaudoDiario.json:14,40` (`"moneda": "MMM"`), `packages/simulado/ejemplos/grupos/consultarDetalleDelGrupo.json:14` (`"mon"`); `"4.44"` en 117 archivos de ejemplos; generador `packages/simulado/scripts/generar-ejemplos.mjs:107-109` (`adverso` = mismo cuerpo `representativo` que `ok`)
- Evidencia: `adverso: { estado: exito, cuerpo: cuerpo(documento, operacion, exito, 'representativo') },`
- Cómo se comprobó: `git grep -n -E '"moneda": "(MMM|mon|moneda)"'`; `grep -l "4.44" -r packages/simulado/ejemplos | wc -l` → 117; lectura del generador.
- Impacto: con pozo = confirmado = faltante = neto = bruto, ninguna pantalla probada contra el mock detecta un intercambio de campos de dinero. «Adverso» no ejercita nada adverso, y las monedas inválidas no son ISO-4217 (regla 91.1.2) y pasan porque el contrato no las restringe a un enum.
- Causa de fondo: generación por esquema sin semántica de dominio. El contrato `Dinero.moneda` no tiene `enum`/`pattern` en todos los servicios.
- Solución de fondo: ejemplos con importes distintos y coherentes (pozo = confirmado + faltante), escenarios adversos reales y `moneda` con `pattern: '^[A-Z]{3}$'` en el contrato.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios (contratos)
- Estándares y normas: ISO 4217; regla 97.6.2 (datos de prueba realistas); 80.3
- Esfuerzo estimado: medio

### frontends-compartidos-PRUEBAS-002 — Las pruebas de contraste WCAG de los tokens no corren
- Clase / Severidad / Confianza: PRUEBAS (también UX-A11Y) / S3 / confirmado
- Ubicaciones: `packages/tokens/vitest.config.ts:2` (`include: ['pruebas/**/*.spec.ts']`); no incluidas: `packages/tokens/pruebas/roles-contraste.test.mjs`, `packages/tokens/pruebas/texto-de-ejemplo-contraste.test.mjs`
- Evidencia: el `include` solo toma `*.spec.ts`, y los dos archivos son `*.test.mjs`. `git grep "\.test\.mjs|contraste" -- .github/workflows` no da resultados.
- Cómo se comprobó: lectura de `vitest.config.ts` y `package.json:17`.
- Impacto: un cambio de color que rompa el contraste AA pasa la CI. La regla 80.7 exige contraste.
- Causa de fondo: convención de nombres mezclada (`.spec.ts` frente a `.test.mjs`).
- Solución de fondo: incluir `pruebas/**/*.test.mjs` o renombrar.
- Relación con el flujo nuevo: ninguna · Conexiones: todas las apps (tokens)
- Estándares y normas: WCAG 2.2 AA (1.4.3); regla 80.7
- Esfuerzo estimado: chico

### frontends-compartidos-DINERO-002 — `CampoMonto` borra todos los puntos: «12.50» se envía como `1250.00` (×100) en recargar, retirar, transferir, puja, subasta y crear grupo
- Clase / Severidad / Confianza: DINERO (también DEFECTO) / **S1** / confirmado por lectura y reproducción de la función (la ejecución en Dart y en el dispositivo NO COMPROBADA: no hay SDK)
- Ubicaciones: `packages/diseno_flutter/lib/atomos/campo_monto.dart:30-49` (`aCadenaDelContrato`), `:72-73` (el teclado y el formateador **permiten** el punto: `RegExp(r'[0-9.,]')`); `packages/diseno_flutter/lib/atomos/campo_monto_grande.dart:50`; usuarios: `apps/movil/lib/pantallas/billetera/{pantalla_recargar,pantalla_retirar,pantalla_transferir}.dart`, `apps/movil/lib/pantallas/pasanaku/{formulario_de_puja,formulario_de_subasta,organismos_crear_grupo}.dart`; prueba `packages/diseno_flutter/test/unidad/logica_pura_test.dart:25-39` (ningún caso con punto decimal)
- Evidencia: `final limpio = escrito.replaceAll('.', '').replaceAll(' ', '').trim();`. Reproducción (port literal a Python de la función):
  ```text
  '12.50' -> 1250.00
  '12.5' -> 125.00
  '1.240,50' -> 1240.50
  '100.00' -> 10000.00
  ```
- Cómo se comprobó: lectura del átomo y de su prueba; `git grep -l "CampoMonto\b|CampoMontoGrande" -- apps/movil/lib` → 6 pantallas de dinero (hallazgo H01 del lector A, re-verificado).
- Impacto: quien escribe el decimal con punto, la forma que ofrecen por omisión muchos teclados numéricos con `decimal: true`, envía un importe 100 veces mayor sin ningún error. En retiro o transferencia, el servidor ejecuta lo que llega mientras haya saldo. En una puja o subasta de turno, compromete un importe ajeno a la intención de la persona. En crear grupo, fija una cuota 100 veces mayor. La pantalla de transferir muestra el `Monto` resultante (`pantalla_transferir.dart:70`), lo que mitiga solo si la persona lo lee.
- Causa de fondo: se asumió el punto solo como separador de miles (formato es-BO) y no se validó la ambigüedad. La prueba fija únicamente el formato con coma.
- Solución de fondo: rechazar la entrada ambigua (un único punto seguido de 1-2 dígitos al final) o interpretarla como decimal; formatear al vuelo mostrando el separador. Agregar pruebas de borde (`12.50`, `12.5`, `1.240`, `1.240.50`) y prueba de widget en cada pantalla de dinero.
- Relación con el flujo nuevo: FLUJO-QR (la recarga define el importe del QR)
- Conexiones y módulos afectados: L1 (billetera), L3 (grupos, subasta y puja)
- Estándares y normas: regla 91.1 (exactitud); ISO/IEC 25010 — adecuación funcional (corrección); CWE-20
- Esfuerzo estimado: chico

### frontends-compartidos-RESPALDO-003 — El tour de bienvenida de la app promete «un fondo de garantía que cubre la cuota para que el turno se entregue igual»
- Clase / Severidad / Confianza: RESPALDO / S2 / confirmado
- Ubicaciones: `packages/diseno_flutter/lib/ilustraciones/arte_fondo_de_garantia.dart:6-8` (en uso desde `apps/movil/lib/pantallas/identidad/laminas_del_tour.dart:2,45`); texto `apps/movil/lib/pantallas/identidad/textos.dart:42-46` (pantalla de L4, punto de contacto)
- Evidencia: «Si alguien no aporta, el grupo no se frena» / «Hay un fondo de garantía que cubre la cuota para que el turno se entregue igual. La deuda no se perdona: queda viva y el fondo pasa a ser el acreedor.»
- Cómo se comprobó: lectura y `git grep ArteFondoDeGarantia -- apps/movil/lib`.
- Impacto: es la primera promesa que lee quien abre la app, y contradice R-NUEVA-2. También contradice ADR-051 D-15, donde el fondo mutual es legado.
- Causa de fondo, solución y estándares: como RESPALDO-001. Una sola fuente de la política, tras la decisión A-2.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: L4 (textos del alta), L2
- Esfuerzo estimado: chico

### frontends-compartidos-UX-A11Y-002 — `CelebracionDeExito` pinta la tilde del mismo color que el círculo: «Cuenta creada» se ve vacía, en los dos temas
- Clase / Severidad / Confianza: UX-A11Y / S2 (en uso) / confirmado por lectura (la captura NO COMPROBADA)
- Ubicaciones: `packages/diseno_flutter/lib/moviles/celebracion_de_exito.dart:74,77`; `packages/tokens/tokens.json:234,237` (claro `brand` = `brandTexto` = `g600`), `:289,292` (oscuro `g300` = `g300`)
- Evidencia: `BoxDecoration(color: t.brand, …)` y `painter: _TildeDeExito(_trazo.value, t.brandTexto)`.
- Cómo se comprobó: lectura y comparación de los valores en `tokens.json`.
- Impacto: la confirmación visual de éxito no se ve: contraste 1:1 (WCAG 1.4.11). La prueba de widget solo verifica que exista un `CustomPaint`.
- Causa de fondo: se eligió un rol de texto sobre fondo neutro para dibujar sobre el propio color de marca. Falta un rol «sobre marca» (`brandInk`).
- Solución de fondo: usar `brandInk` o un rol dedicado, más un golden en claro y oscuro.
- Relación con el flujo nuevo: ninguna · Conexiones: L4 (alta)
- Estándares y normas: WCAG 2.2 AA 1.4.11; regla 95.4, 95.5.4
- Esfuerzo estimado: chico

### frontends-compartidos-HUECO-002 — Piezas públicas del sistema de diseño muestran datos inventados por omisión y prometen «asientos contables reales»
- Clase / Severidad / Confianza: HUECO (también DATOS y DINERO) / S3 (no se usan hoy en `apps`) / confirmado
- Ubicaciones: `packages/diseno_flutter/lib/moleculas/grilla_metricas_miembro.dart:21` (`'Bs 24.800'` preformateado, sin `Monto`); `packages/diseno_flutter/lib/moleculas/tarjeta_riesgo_confianza.dart:18,89,122` (`'Riesgo Bajo · A1'`, `'Nivel Excelente'` fijo, «calculado a partir de asientos contables reales»); `packages/diseno_flutter/lib/moviles/tour_en_stages_datos.dart:21`; origen declarado «GymSheet» en `tarjeta_puntos_mesa.dart:10`, `insignia_tile.dart:24`, `celebracion_stage.dart:16`
- Evidencia: `this.dineroDepositado = 'Bs 24.800',`; `this.calificacion = 'Riesgo Bajo · A1',`.
- Cómo se comprobó: `grep -rn` de las cadenas; el lector A verificó que no tienen referencias en `apps`.
- Impacto: un componente usado sin parámetros muestra a una persona un importe y una calificación de riesgo falsos, con la afirmación de que salen del mayor (reglas 00 §2.1 y 91.1.5). La reputación como cifra fija contradice la regla 91.4.2 (la reputación es consecuencia del expediente).
- Causa de fondo: piezas portadas de otro producto con datos de demostración como valores por omisión.
- Solución de fondo: parámetros obligatorios sin valor por omisión, datos de demo solo en `catalogo/`, y retirar lo que no tenga uso (ver REESCRITURA en §5).
- Relación con el flujo nuevo: ninguna · Conexiones: L5 (reputación)
- Estándares y normas: regla 00 §2.1, 91.4.2, 97.4.1; ISO/IEC 25010 — corrección
- Esfuerzo estimado: chico

### frontends-compartidos-DEFECTO-005 — Cuatro tratamientos de zona horaria y tres parsers de centavos en el sistema de diseño
- Clase / Severidad / Confianza: DEFECTO (también DUP-INT y DINERO) / S3 (las piezas con el error de borde no se usan hoy en la app) / confirmado por lectura
- Ubicaciones: `packages/diseno_flutter/lib/organismos/lista_de_movimientos.dart:41` (agrupa por `fechaIso.substring(0, 10)`, fecha UTC), `packages/diseno_flutter/lib/organismos/fila_de_cuota.dart:21`, `packages/diseno_flutter/lib/organismos/calendario_de_cuotas.dart:56-67` (`totalExigible`); detalle de `Fecha` (UTC-4 fijo) y de la app (`toLocal()`) en el anexo del lector A
- Evidencia: `centavos += BigInt.parse(p[0]) * BigInt.from(100) + BigInt.parse(p[1]);`. Con `'12.5'` suma 5 centavos en vez de 50, y con `'-12.50'` da -1150. `grupos.putIfAbsent(m.fechaIso.substring(0, 10), …)`.
- Cómo se comprobó: lectura; `git grep -n "CalendarioDeCuotas" -- apps/movil/lib` → sin usos.
- Impacto: un movimiento de las 22:00 en La Paz (02:00Z del día siguiente) se agrupa en el día equivocado. Un total exigible mal escrito por el servidor no falla: suma mal. Además es una suma de deuda en el cliente, no leída del mayor (regla 91.1.5).
- Causa de fondo: no hay un objeto de valor `Dinero` ni una `FechaLaPaz` únicos en el paquete.
- Solución de fondo: un único parser de importes (el de `packages/tokens`/`Monto`) y un único conversor a `America/La_Paz`, con vectores de prueba compartidos (`packages/tokens/vectores/monto.json` ya existe).
- Relación con el flujo nuevo: ninguna · Conexiones: L1, L3
- Estándares y normas: ISO 8601; regla 91.1.5; ISO/IEC 25010 — corrección
- Esfuerzo estimado: medio

### frontends-compartidos-FLUJO-QR-003 — La portada ofrece aportar «en efectivo en un punto de atención» y recargar por transferencia o efectivo
- Clase / Severidad / Confianza: FLUJO-QR (también COHERENCIA) / S2 / confirmado
- Ubicaciones: `apps/web/src/app/paginas/inicio/secciones/producto.ts:67,71`
- Evidencia: «Aportá cada período — Desde tu saldo, con QR o en efectivo en un punto de atención.»; «Recargás por QR, transferencia o efectivo».
- Cómo se comprobó: lectura (lector C, re-verificado).
- Impacto: promesa pública de caminos de pago que, con R-NUEVA-1, no mueven el flujo. No se encontró en el alcance un camino de efectivo implementado (lo confirma L1). Quien pague en efectivo creyendo en la portada queda en mora.
- Causa de fondo: textos de marketing sin fuente única frente al catálogo de medios de pago.
- Solución de fondo: los medios de pago visibles se publican desde el contrato de `aportes` (medios habilitados) y no desde texto fijo.
- Relación con el flujo nuevo: FLUJO-QR
- Conexiones y módulos afectados: L1
- Estándares y normas: R-NUEVA-1; protección al consumidor: norma a identificar por Cumplimiento
- Esfuerzo estimado: chico

### frontends-compartidos-SEG-002 — El SSR del sitio no emite cabeceras de seguridad y su proxy `/api` reenvía cabeceras del cliente sin timeout
- Clase / Severidad / Confianza: SEG (también RESILIENCIA) / S2 / probable (confirmado en el código; no se comprobó si un proxy de borde de Coolify agrega cabeceras)
- Ubicaciones: `apps/web/src/server.ts:55-85` (proxy `/api`), `:112-119` (respuesta HTML); `apps/web/docker/Dockerfile.web:1,20-33`; backoffice: `apps/backoffice/docker/nginx.conf:9` frente a `apps/backoffice/docker/nginx.desplegado.conf:23` (CSP distintas)
- Evidencia: el comentario de `Dockerfile.web:1` dice «servido por Node detrás de NGINX», pero la imagen es `node:22-alpine` con `CMD ["node", "./dist/server/server.mjs"]` y sin NGINX. `grep -n -i "Content-Security|X-Frame|Strict-Transport|timeout|AbortSignal" apps/web/src/server.ts` no da resultados. El proxy copia `req.headers` salvo una lista negra (`SIN_REENVIO`) y hace `fetch(gatewayInterno + req.originalUrl, …)` sin señal de aborto.
- Cómo se comprobó: lectura y `grep`.
- Impacto: la página pública (verificadores, invitaciones) queda sin CSP, `frame-ancestors` ni HSTS, expuesta a clickjacking e inyección. Un gateway lento cuelga conexiones del SSR. La lista negra deja pasar cabeceras que el cliente no debería poder fijar ante el gateway.
- Causa de fondo: se asumió un NGINX que no existe en la imagen. Reenvío por lista negra en lugar de lista blanca.
- Solución de fondo: cabeceras de seguridad en `server.ts` o un NGINX real; lista blanca de cabeceras; `AbortSignal.timeout(...)` en el `fetch`; una sola CSP versionada para el backoffice.
- Relación con el flujo nuevo: ninguna · Conexiones: gateway (L5)
- Estándares y normas: OWASP ASVS V14 (configuración); OWASP Top 10 A05; regla 98.2.1 (timeout explícito)
- Esfuerzo estimado: chico

### frontends-compartidos-SEG-003 — `ap-listado` exporta CSV/JSON desde el navegador por omisión, sin rastro y con valores sin enmascarar
- Clase / Severidad / Confianza: SEG (también TRAZA) / S2 / confirmado en el componente; el conteo de 30/32 listados es del lector C (probable)
- Ubicaciones: `packages/ui/src/listado/listado.ts:50,98,102` (`readonly exportable = input(true)`); canal auditado `apps/backoffice/src/app/nucleo/exportador.ts` → `POST /extraccion/exportaciones`, sin contrato (CONTRATO-001)
- Evidencia: `readonly exportable = input(true)`.
- Cómo se comprobó: lectura del componente; el lector C contó 30 de 32 listados del backoffice con exportación local.
- Impacto: cualquier operador con vista de un listado descarga datos de participantes sin que quede registro (regla 90.2.7) y sin el enmascarado de la vista. La exportación auditada no existe en el servidor.
- Causa de fondo: valor por omisión inseguro en un componente compartido. El canal auditado depende de un servicio inexistente.
- Solución de fondo: `exportable` en `false` por omisión. Exportaciones de datos personales solo por el servidor, con permiso `exportar:*` y bitácora.
- Relación con el flujo nuevo: ninguna · Conexiones: L5 (auditoría), todas las pantallas de backoffice
- Estándares y normas: regla 90.2.1/90.2.7, 95.6.5; OWASP API3:2023 (exposición excesiva)
- Esfuerzo estimado: medio

### frontends-compartidos-DEFECTO-006 — Al restaurar o refrescar la sesión del backoffice se pierde el `sujeto`, y el avance de tutoriales de todos los operadores comparte la clave `anonimo`
- Clase / Severidad / Confianza: DEFECTO / S3 / confirmado
- Ubicaciones: `apps/backoffice/src/app/nucleo/auth-bootstrap.ts:35`, `apps/backoffice/src/app/nucleo/sesion.interceptor.ts:27` (llaman `sesion.abrir(…)` sin el cuarto parámetro); `apps/backoffice/src/app/nucleo/sesion.ts:50` (`sujeto: string | null = null`); `packages/tutoriales/src/almacen-local.ts:70`
- Evidencia: `sesion.abrir(resultado.acceso, resultado.permisos, resultado.rol)` · `return PREFIJO + huella(this.identidad.sujeto() ?? 'anonimo')`.
- Cómo se comprobó: lectura de los cuatro archivos.
- Impacto: tras un F5 o un refresco por 401, el shell ya no sabe quién es el operador. El progreso de tutoriales de un operador lo ve el siguiente en el mismo navegador, y todo lo que dependa de `sujeto` queda nulo.
- Causa de fondo: `abrirConToken` lee los claims y `abrir` no. Hay dos caminos de apertura de sesión.
- Solución de fondo: un solo método que derive `sujeto` del token en todos los caminos.
- Relación con el flujo nuevo: ninguna · Conexiones: L4 (sesiones)
- Estándares y normas: ISO/IEC 25010 — corrección
- Esfuerzo estimado: chico

### frontends-compartidos-DINERO-003 — El backoffice promete «si la repetís igual, no se duplica», pero la clave de idempotencia solo sale si cada pantalla la marca
- Clase / Severidad / Confianza: DINERO (también CONTRATO) / S2 / probable (falta el inventario de escrituras sin `contextoIdempotente()`)
- Ubicaciones: `apps/backoffice/src/app/nucleo/errores.ts:104-108`; `apps/backoffice/src/app/nucleo/idempotencia.interceptor.ts:58-81` (sin marca de contexto, no agrega cabecera)
- Evidencia: «La respuesta tardó demasiado y no sabemos si la operación quedó registrada. Antes de repetirla, revisá su estado: si la repetís igual, no se duplica.»; el interceptor devuelve `next(req)` sin cabecera si no hay `CLAVE_POR_CONTENIDO` ni `CLAVE_IDEMPOTENCIA`.
- Cómo se comprobó: lectura; en `rutas/` hay 21 archivos con `contextoIdempotente` y 3 con clave por formulario. El resto de las escrituras no se inventarió (pantallas de módulo).
- Impacto: en una escritura sin marca, el texto invita a repetir y la repetición puede duplicar un cobro, un cierre o un desembolso.
- Causa de fondo: la idempotencia es opcional por pantalla y el texto es global.
- Solución de fondo: que el interceptor agregue la clave por contenido a TODO `POST/PUT/PATCH/DELETE` por omisión, con exclusión explícita, y que el texto solo aparezca cuando la petición llevó clave.
- Relación con el flujo nuevo: ninguna · Conexiones: L1, L2 (pantallas de dinero del backoffice)
- Estándares y normas: regla 91.1.4, 96.3.3
- Esfuerzo estimado: chico

### frontends-compartidos-DUP-X-002 — Núcleo de red clonado entre backoffice y web, con divergencias
- Clase / Severidad / Confianza: DUP-X / S3 / confirmado
- Ubicaciones: `apps/backoffice/src/app/nucleo/{gateway,errores,errores.interceptor,timeout.interceptor,traza.interceptor,configuracion-invalida}.ts` frente a `apps/web/src/app/nucleo/` con los mismos nombres
- Evidencia: `diff` de `timeout.interceptor.ts` muestra cuerpos distintos (lectura y escritura en el backoffice, solo lectura en la web). Según el lector C, la web no traduce 502/503/504 y `ErrorTraducido` está definido cuatro veces.
- Cómo se comprobó: `diff` (normalizado sin espacios) del interceptor de timeout; el resto, informe del lector C.
- Impacto: un arreglo de red en una app no llega a la otra. Mismo error, distinto mensaje.
- Causa de fondo: no hay un paquete Angular de infraestructura de red compartida (`packages/ui` es solo presentación).
- Solución de fondo: mover interceptores, traducción de errores y gateway a un paquete compartido (p. ej. `packages/red-angular`), parametrizado.
- Relación con el flujo nuevo: ninguna · Conexiones: backoffice, web
- Estándares y normas: ISO/IEC 25010 — mantenibilidad
- Esfuerzo estimado: medio

### frontends-compartidos-PRUEBAS-003 — Playwright del backoffice con `retries: 1`
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado
- Ubicaciones: `apps/backoffice/playwright.config.ts:30`
- Evidencia: `retries: 1,`
- Cómo se comprobó: `grep`.
- Impacto: un E2E intermitente se reporta verde. La regla 80.5.3 prohíbe reintentos sin causa demostrada.
- Causa de fondo: falta un triaje de las pruebas inestables.
- Solución de fondo: `retries: 0` en desarrollo y en la CI, con cuarentena con dueño y fecha para las inestables.
- Relación con el flujo nuevo: ninguna · Conexiones: ninguna
- Estándares y normas: regla 80.5.3
- Esfuerzo estimado: chico

### frontends-compartidos-REESCRITURA-002 — Código muerto o solo de catálogo en el shell del backoffice y en `packages/ui`, incluido un explicador con un endpoint inventado
- Clase / Severidad / Confianza: REESCRITURA (también DOC) / S3 / probable (búsquedas de referencias del lector C; muestra re-verificada: `ExplicadorDeVistaComponent`)
- Ubicaciones: `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.ts`, `apps/backoffice/src/app/layout/pestanas-de-seccion.ts`, `apps/backoffice/src/app/nucleo/filtros/barra-de-filtros.ts`, `apps/backoffice/src/app/nucleo/evidencia/panel-de-evidencia.ts`, `apps/backoffice/src/app/nucleo/explicador-de-vista.component.ts` y `nucleo/explicaciones/*` (~560 líneas); en `packages/ui`, unos 22 componentes usados solo por `src/catalogo` (tarjeta-saldo, lista-de-movimientos, tarjeta-de-solicitud…)
- Evidencia: según el lector C, el explicador menciona `/operaciones/retiros/{id}/aprobar`, que no existe en ningún contrato.
- Cómo se comprobó: informe del lector C (búsquedas `git grep` de selectores e imports).
- Impacto: código que se mantiene y empaqueta sin uso, con textos de aprobación y respaldo (RE-07) que siguen el flujo viejo.
- Causa de fondo: reescrituras de agentes sin retirar las versiones previas.
- Solución de fondo: borrar lo que no tiene consumidor real, o moverlo al catálogo con su marca.
- Relación con el flujo nuevo: APROB/RESPALDO (textos)
- Conexiones y módulos afectados: backoffice
- Estándares y normas: ISO/IEC 5055; ISO/IEC 25010 — mantenibilidad
- Esfuerzo estimado: chico

## 5-bis. Candidatos de los lectores no re-verificados uno por uno (confianza: sospecha)

Estos candidatos los encontraron los lectores de solo lectura. Su ficha completa (ubicación, cita y cómo se encontró) está en `evidencia/lotes/`. El auditor no reabrió cada ubicación, así que entran con confianza **sospecha** y **no** se cuentan en la tabla del §2. Quien los tome tiene que verificarlos antes de corregir.

| Origen | Clase · sev. propuesta | Resumen |
|---|---|---|
| A-H02 | DINERO · S3 | `CampoMonto(valorInicial:)` re-parsea la cadena del contrato (mismo error que DINERO-002); hoy nadie lo usa en `apps/movil/lib` |
| A-H04 | APROB · S3 | `TarjetaDeSolicitud`, `PanelDeFactores(aprobado)`, `EstadoDeTramite`, requisitos `HAB_*`: piezas de aprobación humana con pruebas que las fijan |
| A-H09, A-H12 | DINERO · S3 | Modelos con importe sin moneda; saldo oculto con «Bs» fijo; prefijo de moneda en tres lugares |
| A-H11 | UX-A11Y · S3 | `mostrarSnackbar` siempre verde, usado para errores |
| A-H13 | DINERO/RN-18 · S3 | «Comisión de entrega» y «bono de bienvenida» sin decir quién cobra ni quién paga |
| A-H14 | FLUJO-QR · S3 | Tipos `recargaTransferencia`, `canalBanco` y `canalTarjeta` (tarjeta, fuera de alcance PCI, regla 90.2.10) |
| A-H15, A-H16 | UX-A11Y · S3 | `TarjetaDeOferta` muestra códigos crudos; `TarjetaDeSolicitud` queda cargando o admite doble envío |
| A-H17, A-H18 | UX-A11Y · S3 | Literales `Paleta.*` que saltan el tema oscuro; duraciones > 320 ms que la prueba no ve |
| A-H19..H26 | CLEAN/PRUEBAS · S3-S4 | Clones internos, comentarios que mienten, mojibake, etiqueta no vinculada al `TextField`, animaciones creadas en `build`, goldens de plataforma |
| A-H10 | REESCRITURA · S3 | ~35 piezas públicas sin referencias; `ListaDeMovimientos` homónimo y divergente en la app |
| B-06 | DUP-X · S3 | Sorteo TS frente a Java: difieren con semilla o entropía vacías; vectores calculados en Python, no en Java |
| B-12, B-13, B-14 | PRUEBAS/DATOS · S3 | `intermitente.luego` sin implementar; 44 rechazos con códigos ajenos; rellenos con forma de dato |
| B-17, B-18 | DUP-INT/DINERO · S3-S4 | Lista de exclusión de tokens distinta entre CSS y Dart; bordes del formateo de montos sin cubrir |
| B-19, B-20 | CONFIG · S3 | Frontera del mock hacia producción (entra solo por `development`/`demo`); `/recargas-de-prueba` en el contrato productivo (ver FLUJO-QR-001) |
| C-H09 | DINERO · S3 | `lista-de-movimientos` (ui) agrupa por día UTC y suma sin verificar la moneda; `tarjeta-saldo` compara contra `'0.00'` |
| C-H10 | DOC · S3 | Notas internas de carril publicadas en el sitio; `.md` generado versionado |
| C-H12 | FRAGIL · S3 | Guardias y menú copian a mano la cadena de permisos; atajos `ver:*` |
| C-H13 | UX-A11Y · S3 | `.boton--claro` con contraste 3,32:1; hex literal en `tokens-sitio.css`; pasos de tutorial sobre hosts `display: contents` |
| C-H14, C-H16, C-H17, C-H19, C-H20 | CONFIG/FRAGIL/CLEAN/RESILIENCIA · S3-S4 | Configuración de despliegue contradictoria, listas de SEO duplicadas, duplicación visual, parches y arranque del SSR |
| C-H18 | SEG · S3 | `cuenta-enmascarada` recibe el número completo; `fila-de-cotejo` decide KYC en la vista (hoy solo en el catálogo) |
| D-13, D-19 | CLEAN · S4 | `!` sobre id de usuario; `r.data!`; `cu13_cuenta_propia` no observa la generación de sesión |
| D-17, D-18 | HUECO/CONFIG · S3-S4 | Push Android «soportado» que no entrega; puente de cámara de desarrollo sin guarda de release (solo con `--dart-define`) |
| D-20 | DINERO · S3 | Moneda `BOB` fija en 4 lugares; `montoValido` con `double.parse` (sin uso) |
| D-21, D-22 | UX-A11Y · S3 | Tres nombres de producto (AportaYa/Aporteo); hidratación solo en tema claro; textos de permisos iOS |
| D-23, D-24, D-25, D-26 | CLEAN/OBSERV · S4 | Drivers clonados que escriben en `docs/trabajo`; comentarios desactualizados; `capacidadesProvider` duplicado; errores tragados en cierre de sesión y tutoriales |

## 6. Matriz de contratos

Este módulo no es dueño de operaciones. La matriz cubre las operaciones que **el shell y los paquetes compartidos** llaman directamente, más los artefactos de contrato que el módulo produce (mock y clientes). Las operaciones de cada pantalla de módulo las cubre su lote.

| Operación / artefacto | OpenAPI | Implementación servidor | Pacto | Cliente generado | Modelo front | Discrepancias |
|---|---|---|---|---|---|---|
| `POST /sesion/refrescar` (app) | `identidad.yaml` (`SalidaRenovacion`, l.1280-1294 según lector C) | identidad | no revisado | sí (Dart) | `refrescador.dart` usa `Dio` crudo con `Map<String,dynamic>` y no el cliente | El cliente generado no se usa; la respuesta no trae `sub` (DEFECTO-006) |
| `POST /sesion/refrescar` (backoffice, cookie) | idem | idem | no revisado | doble `clientes-angular.doble.d.ts` | `refresco-de-sesion.ts:37` | `sujeto` se pierde (DEFECTO-006) |
| `POST /billetera/recargas`, `/retiros`, `/transferencias` | `nucleo-financiero.yaml` | `comun-web/idempotencia` | no revisado (L1) | sí | `cu10/11/12_*.dart` | Clave por cuenta frente a 409 por cuerpo distinto (DEFECTO-002); importe ×100 (DINERO-002) |
| `POST /recargas-de-prueba/{id}/pago` | `nucleo-financiero.yaml:221` | `RecargasDePruebaController` + `@ConditionalOnProperty` | – | sí | `cu10_pago_de_prueba.dart` | Operación de prueba en contrato productivo (FLUJO-QR-001) |
| `GET saldo` + posiciones | `nucleo-financiero.yaml:1513-1515` (`comprometidoEnPozos`) | L1 | – | doble Angular | `dominio-cliente/posiciones.ts` | El comentario dice que el campo no existe; gastable calculado en el cliente (DINERO-001) |
| `/notificaciones/avisos-internos` (3 ops.) | `notificaciones.yaml` | L5 | – | no (URL a mano) | `avisos-internos.ts` | `verificar_rutas_frontend` la da con contrato |
| `POST /extraccion/accesos`, `/extraccion/exportaciones` | **no existe** | **no existe** | – | – | interceptor y exportador | Rutas sin contrato (CONTRATO-001, TRAZA-001, SEG-003) |
| `GET/PUT/DELETE /identidad/tutoriales/progreso` | **no existe** (ADR-045) | – | – | – | `almacen-remoto.ts` | Sin contrato (CONTRATO-001) |
| `GET /publicidad/socios-comerciales/{x}` | no existe | – | – | 4 modelos Angular versionados | backoffice | Sin contrato (CONTRATO-001) |
| `POST /entregas/pozo-completo` | `entregas.yaml:792,800` | L2 | no revisado | sí (Dart) | ninguna pantalla | Contradice R-NUEVA-2 (RESPALDO-002) |
| Familia `garantia/*Respaldo*` | `garantia.yaml` | L2 | no revisado | `respaldo_api.dart` | ninguna pantalla | RESPALDO-002 |
| `cumplimiento` (`monto` con `pattern`) | `cumplimiento.yaml:1251,1339` | L4 | – | generado | mock | Patrón imposible (CONTRATO-003) |
| Mock (`packages/simulado`, 349 operaciones) | todos los `openapi` | – | – | – | – | 0 ejemplos inválidos por esquema según el lector B, pero monedas inválidas y `adverso` = `ok` (DATOS-001) |
| `clientes/*` versionados (26) | todos | – | – | instantánea parcial | – | Ignorados pero trackeados; el barril de aportes no exporta modelos recientes según el lector B (CONTRATO-002) |

**Eventos.** El alcance no publica ni consume eventos Kafka. Los avisos push llegan a la app como deep link: `aportaya://aporte` lleva a una ruta inexistente (HUECO-001).

**Prueba de contrato del lado productor** (98.1.4): no aplica al frontend. Del lado consumidor, `apps/movil/test/contrato` existe pero no se ejecutó (sin Flutter).

## 7. Evidencia obligatoria del módulo

| Evidencia (reglas 91.6 / 98.8 / 90.6) | Estado | Detalle |
|---|---|---|
| Asientos por operación de dinero | NO APLICA | El frontend no genera asientos; lo cubren L1/L2 |
| Idempotencia ejercitada (misma operación dos veces) | NO COMPROBADO | Sin backend ni SDK. Lectura: DEFECTO-001/002, DINERO-003 |
| Cuadre del mayor | NO APLICA | — |
| Caso de reversa | NO APLICA | — |
| Redondeo declarado | NO COMPROBADO | El formateo se hace con cadenas y BigInt (lectores B y C); `CalendarioDeCuotas` suma mal los bordes (DEFECTO-005) |
| Prueba de contrato (consumidor) | NO COMPROBADO | `apps/movil/test/contrato` no corre sin Flutter; `yarn test:front` no corre sin yarn 4 |
| Duplicado de mensaje | NO APLICA | Sin consumidores en el alcance |
| Servicio dependido apagado | NO COMPROBADO | Por lectura: timeouts en los interceptores del backoffice y la web; el proxy SSR no tiene timeout (SEG-002) |
| Traza con `correlationId` | NO COMPROBADO | Por lectura: `traza.interceptor.ts` (backoffice y web) agrega `x-request-id`; el refresco de la app sale sin él (DEFECTO-003) |
| Matriz de autorización negativa | NO APLICA en el cliente | Las guardias del frontend no autorizan (regla 95.6.1); lo prueba cada servicio |
| Verificadores del repo | **PASS** 12 / **FAIL** 1 | `verificar_rutas_frontend.py` exit 1; el resto exit 0. Salidas en `evidencia/verificadores/`. `idempotencia.py` no es un verificador (genera SQL idempotente) y no imprime nada |
| Ejecución puntual | PASS | `node`: `new URL("/\\evil.example.com/api/v1", origen)` → host ajeno (SEG-001); port Python de `aCadenaDelContrato` (DINERO-002) |

## 8. Cobertura por clase

| Clase | Qué se revisó y con qué método | Hallazgos (fichas + sospechas) |
|---|---|---|
| APROB | `git grep -i "aprob|APROBAD|postulaci|habilit|admisi|comprobante|PENDIENTE_"` en todo el alcance + lectura del menú, rutas, catálogo de errores y mock | 2 + A-H04 (inventario §4a: 8 puntos) |
| RESPALDO | `git grep -i "respaldo|cobertura|cubr|faltante|pozo completo|garantiz"` + lectura de textos web, tour, mock, clientes | 3 (inventario §4b: 7 + mock/clientes/tour) |
| FLUJO-QR | Lectura de `cu10_*`, enlaces profundos, portada; punto de contacto en `nucleo-financiero` | 3 + A-H14 |
| FLUJO-FALTANTE | Búsqueda de lógica de entrega con faltante en el alcance: ninguna. El frontend solo muestra textos (RESPALDO-001/003) | 0 (ninguna lógica; ver A-2) |
| COHERENCIA | Comparación de textos web entre sí y contra ADR-051/054 | 1 (+ RESPALDO-001) |
| DUP-INT | Diff de clones internos (idioma, catálogos, parsers) | 1 + A-H19 |
| DUP-X | Diff de `nucleo/` backoffice frente a web; `cadena.ts` frente a Java; sorteo | 2 + B-06 |
| REESCRITURA | `git grep -l <símbolo>` de referencias | 2 + A-H10 |
| PISADA | `git log` de `clientes/` versionados; dos políticas de idempotencia; comentarios con fechas de carriles | incluida en CONTRATO-002, DEFECTO-002 |
| PARCHE | `catch` vacíos, banderas apagadas, valores fijos | 1 + D-26, C-H19 |
| FRAGIL | Cadenas mágicas de códigos de error y rutas | 1 + C-H12 |
| DEFECTO | Lectura de bordes (importes, fechas, sesión, protección de pantalla) | 6 |
| HUECO | Rutas inexistentes, widgets con datos fijos | 2 + D-17 |
| RESILIENCIA | Timeouts, refresco, proxy SSR | incluida en DEFECTO-003, SEG-002 |
| DINERO | `double`/`number` para importes, moneda, sumas en cliente, idempotencia | 3 + A-H09/H12/H13, C-H09, D-20 |
| DEBIDO-PROCESO | Búsqueda de sanción, mora o reputación decididas en el cliente: solo `tarjeta_riesgo_confianza` con una calificación fija (HUECO-002) | 0 propios |
| DDD | Reglas de negocio en el cliente (posiciones, cotejo KYC) | incluida en DINERO-001, C-H18 |
| ARQ | Capa de red en vistas (`verificar_frontend` OK), paquetes sin red | 0 propios; DUP-X-002 |
| CLEAN | Lectura completa; `verificar_frontend` (tamaño, `print`, literales) en verde | sospechas A-H19..26, D-13/19/23..26 |
| ACOPLE | El frontend no lee tablas; reglas reimplementadas en el cliente (posiciones, sorteo, cadena) | incluida en DUP-X-001, DINERO-001 |
| CONTRATO | `verificar_rutas_frontend`, comparación mock/clientes/OpenAPI (lector B, jsonschema) | 3 |
| DATOS | Monedas, importes y procedencia en el mock y la web | 1 (+ transparencia sin rótulo de ejemplo, en RESPALDO-001) |
| RENDIMIENTO | Lectura de listados, tabla virtualizada (muerta), suscripciones | 0. No se observaron cargas sin límite en el shell; los listados los pagina cada pantalla de módulo |
| PRUEBAS | Scripts `test:*`, configs de vitest y Playwright, pruebas que fijan el flujo viejo | 3 + A-H25, B-12 |
| UX-A11Y | Estados, contraste en `tokens.json`, guardias de ruta, Semantics (lectores) | 2 + varias sospechas |
| OBSERV | Correlación, logs (`debugPrint` único constante, lector D), registro de lecturas | incluida en TRAZA-001, PARCHE-001 |
| CONFIG | Firma de Android, App Links, Docker/NGINX, `.gitignore` | 1 + D-18 |
| DOC | Comentarios que contradicen el código | 1 + A-H20, C-H10 |
| SEG | Validador del gateway, cabeceras SSR, exportación, almacenamiento de tokens | 3 |
| TRAZA | Registro de lecturas y exportaciones | 1 |
| REG | Afirmaciones regulatorias públicas (ASFI, custodia) | incluida en COHERENCIA-001, RESPALDO-001 |
| OTRO | — | 0 |

**Pasada inversa (Fase 7).**
- FLUJO-FALTANTE, DEBIDO-PROCESO y ARQ quedaron sin fichas propias. Se volvió a mirar con `git grep -i "mora|sanci|reputac|faltante"` en `packages/` y en el shell: solo aparecen textos y piezas de catálogo, ya cubiertos.
- RENDIMIENTO no se midió: no hay `build` ni perfilado posibles en la nube.

## 9. Lo que está bien

- `apps/backoffice/src/app/nucleo/sesion.ts:23-25`: el token del operador vive solo en memoria y el refresco en cookie HttpOnly (`refresco-de-sesion.ts:37`, `withCredentials`).
- `apps/movil/lib/dominio/cu10_pago_de_prueba.dart:13-15`: no suma saldo en memoria; relee el saldo del servidor (`saldoProvider`), que lo toma del libro.
- `scripts/verificar_frontend.py todo` → `TODO OK` (sin literales de diseño, sin formato de dinero fuera de `Monto`, sin `print`, sin archivos > 200 líneas en los paquetes y apps).
- Dinero como cadena en todo el alcance: formateo de montos con cadenas, sin `number` (lector B, `packages/tokens`); suma de neto con `BigInt` en `packages/ui` (lector C); `CalendarioDeCuotas` también usa `BigInt`.
- El backoffice refresca la sesión una sola vez aunque lleguen varios 401 a la vez, y la clave por contenido está probada (`idempotencia.interceptor.ts`, lector C).
- Ningún error crudo del backend llega a la pantalla: catálogos de texto con caída genérica por estado (backoffice `errores.ts`, app `errores.dart`).
- El CSV tiene defensa contra inyección de fórmulas (lector C); diálogos y menús accesibles; el movimiento reducido es global (lector C).
- El mock solo entra por import dinámico en las configuraciones `development`/`demo` del backoffice (lector B): no llega al bundle productivo.
- En la app, la red vive solo en `dominio/` y los adaptadores de plataforma tras puertos en `dominio/puertos` (ADR-036). Hay un único `debugPrint`, con mensaje constante (lector D).
- La app guarda los tokens en el Keychain con acceso solo con el dispositivo desbloqueado y sin respaldo en iCloud (`infraestructura/ios/almacen_seguro_ios.dart:5`).
- El sorteo en TypeScript coincide con Java en el camino normal y usa vectores compartidos (`packages/dominio-cliente/vectores/`); falta cubrir los bordes (B-06).

## 10. Observado fuera de alcance

- `servicios/entregas/src/main/resources/openapi/entregas.yaml:324-326,792,800`: CU-22 «pozo completo» con `cubiertoEmpresa` (L2, fuente de RESPALDO-002).
- `servicios/garantia/…/garantia.yaml:797,809`: `requiereAprobacion` en la cobertura de un incumplimiento (L2).
- `servicios/grupos/…/grupos.yaml:1638`: `enum: [PENDIENTE_BACKOFFICE]` (L3).
- `servicios/cumplimiento/src/main/resources/openapi/cumplimiento.yaml:1251,1339`: patrón de importe imposible (L4, CONTRATO-003).
- `servicios/*/openapi/*.yaml`: esquema de `moneda` no uniforme (`aportes.yaml:536` frente a `:570`; `grupos.yaml:1571,1614,1689,1741,1872`) (L5 contratos).
- `servicios/nucleo-financiero/src/main/resources/openapi/nucleo-financiero.yaml:219-232`: `/recargas-de-prueba` en el contrato productivo (L1).
- `servicios/nucleo-financiero/…/nucleo-financiero.yaml:517`: `/billetera/retiros/{ordenId}/aprobacion` (L1/L4, A-1).
- `servicios/transparencia/src/main/java/bo/aportaya/transparencia/dominio/CadenaDeBloques.java:40`: `PRIMER_NUMERO = 2`, parche por una restricción contradictoria de la base (L5).
- `servicios/identidad/src/main/resources/openapi/identidad.yaml:1280-1294`: `SalidaRenovacion` sin `sub` (L4).
- `sql/60_semillas/10-roles-y-permisos.sql`: no siembra `exportar:*` (L5).
- `despliegue/coolify/web.yml:39,49`: sin cabeceras de seguridad para el SSR.
- `buildSrc/src/main/kotlin/aportaya.openapi.gradle.kts`: comenta `clientes/typescript/` y un gate de diff sobre una ruta ignorada.
- `landing/assets/css/tokens.css`: segunda copia de tokens, hecha a mano fuera de `packages/tokens`.
- `apps/movil/lib/pantallas/identidad/pasos_alta/paso_perfil_transaccional.dart:85,107` y `…/dominio/borrador_alta.dart:112`: `double.tryParse` sobre montos (L4).
- `apps/movil/lib/pantallas/pasanaku/pantalla_invitar.dart:47`: teléfono `^\+591\d{8}$`, distinto de `dominio/validacion.dart:13` (L3).
- `apps/movil/lib/pantallas/pasanaku/{pantalla_solicitudes_del_grupo,dominio/habilitacion_basica,dominio/cu68_postular_al_grupo}.dart`: aprobaciones del lado de la app (L3).
- `apps/backoffice/src/app/rutas/**`: 30 de 32 `<ap-listado>` sin `[exportable]=false` (cada lote, SEG-003).
- `apps/backoffice/src/app/rutas/sistemas/dominio/datos-simulados.ts:16-20`: «doble confirmación simulada» (APROB, L5).
- `apps/backoffice/src/tipos/clientes-angular.doble.d.ts:1-77`: doble con `any` y fecha caducada (2026-09-21).
- Planes de otros trabajos con microtareas `BLOQUEADO`/`EN CURSO` que el candado `blocker_gate.py` señala: `docs/trabajo/2026-10-03-alta-fecha-actividad-autocaptura`, `2026-10-14-resolver-conflictos-prs-60-63`, `2026-10-16-ci-formato-test`, `2026-10-27-rediseno-backoffice-al-repo-desplegado`, `2026-10-28-admision-directa-backoffice`, `2026-10-30-arreglos-test-y-espejo`.

## 11. Límites de esta auditoría

- `yarn` 4.18 no arranca en la nube: `corepack yarn --version` → `ENOTFOUND repo.yarnpkg.com`. **`yarn lint`, `yarn typecheck`, `test:front` y `test:a11y` NO COMPROBADOS.**
- Flutter y Dart no están instalados: análisis, pruebas y goldens de `apps/movil` y `packages/diseno_flutter` NO COMPROBADOS.
- Los barridos Gradle no aplican a este módulo, que no tiene código Java.
- **Supuesto de frontera** («shell y componentes compartidos»):
  - App: todo `apps/movil/lib` fuera de `pantallas/`, más `pantallas/arranque` y `pantallas/alianzas`. `dominio/cu10..cu15_*.dart` son de billetera (L1), pero viven en el `dominio/` compartido y se auditaron acá.
  - Backoffice: todo menos `src/app/rutas/` y `e2e/`.
  - Web: todo menos `verificadores/`, `paginas/invitacion`, `paginas/publico-*` y sus E2E (L3/L5). Las páginas institucionales y `contenido/` se tomaron como compartidas porque no tienen módulo dueño.
- **Cobertura real:** unas 750 de 1 391 archivos leídos completos (≈54 %). El resto se inventarió, muestreó o escaneó, como declara §12. La auditoría **no** aparenta el 100 %.
- **Lectores delegados:** 4 subagentes de solo lectura con lotes disjuntos (autorizado en `CLAUDE.md`). Las 42 fichas del §5 las re-verificó el auditor. Los candidatos del §5-bis no, y por eso son *sospecha*.
- **Clasificación de RESPALDO-001/003 y RE-01..06 como «CONTRADICE R-NUEVA-2».** Supone que el único mecanismo vivo de cobertura es la reserva empresarial (ADR-051 D-15: el fondo mutual es legado). Si Pablo decide conservar un fondo mutual financiado por participantes, esas filas pasan a DECISIÓN REQUERIDA (A-2/A-3).
- **Fase 7 (refutación).**
  - DINERO-002 (S1): se re-abrió `campo_monto.dart:30-49,72-73` y la prueba `logica_pura_test.dart:25-39`, y se buscó una mitigación en las pantallas. Solo `pantalla_transferir.dart:70` muestra el monto resultante, y eso no impide el envío. Resiste.
  - SEG-002: se bajó de «confirmado» a «probable» porque un proxy de borde no visto podría agregar cabeceras.
  - CONFIG-001: se bajó de S2 a S3 porque solo se distribuye un APK de prueba.
  - DINERO-001: de S2 a S3, porque existe la advertencia `comprometido-excede-disponible`.
  - SEG-001: a S3, porque el valor sale de una meta inyectada por NGINX.
  - RESPALDO-002: queda S2 en este alcance (el S1 es del servidor, L2).
  - Los demás S2 se re-abrieron al escribir su ficha.
- No se ejecutaron migraciones, seeds ni nada contra TEST. No quedaron procesos en background: los 4 lectores terminaron.
- El candado `blocker_gate.py` señala microtareas abiertas de otros planes (`docs/trabajo/2026-10-03…`, `2026-10-14…`, `2026-10-16…`, `2026-10-27…`, `2026-10-28…`, `2026-10-30…`). Una auditoría de solo lectura no los modifica.

## 12. Anexo de cobertura de archivos

Las tablas que siguen son las de los cuatro lectores de solo lectura, copiadas sin cambios. Las fichas completas de sus candidatos están en `evidencia/lotes/`. Resumen:

| Lote | Alcance | Archivos | Leídos completos | Resto |
|---|---|---|---|---|
| A | `packages/diseno_flutter` | 242 | 213 (todo el texto) | 23 PNG + 6 TTF inventariados; `pubspec.lock` muestreado |
| B | `packages/{simulado,tokens,dominio-cliente}`, `clientes/` | 433 | código completo + 84 ejemplos de aportes, nucleo-financiero, entregas y garantia | 265 ejemplos muestreados (3 por servicio) + validados por script; `vectores/monto.json` solo por script |
| C | `packages/{ui,tutoriales}`, shell de backoffice y web | 422 | 235 | 187 por escaneo dirigido (sobre todo specs), declarado fila por fila |
| D | shell de `apps/movil` | 116 `lib` + 6 config + 8 drivers + 28 `terceros` + 112 pruebas | 116 + 6 + 8 + 5 pruebas | `terceros/` inventariado; 76 pruebas listadas y no leídas; 36 sin relación |
| Auditor | puntos de contacto, verificaciones de §5 | — | ~40 ubicaciones re-abiertas | — |

### 12.A — `packages/diseno_flutter`
#### 1) Cobertura

| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| packages/diseno_flutter/.fvmrc | 3 | sí | Fija Flutter 3.44.8 para FVM. |
| packages/diseno_flutter/.gitignore | 7 | sí | Ignora build, tokens.dart generado y fallos de goldens. |
| packages/diseno_flutter/analysis_options.yaml | 21 | sí | Reglas de análisis (flutter_lints + 5 reglas). |
| packages/diseno_flutter/lib/atomos/aspecto_de_etapa.dart | 68 | sí | Color e ícono de cada etapa de reputación por orden. |
| packages/diseno_flutter/lib/atomos/avatar.dart | 53 | sí | Avatar circular con iniciales o imagen. |
| packages/diseno_flutter/lib/atomos/ayuda_de_hoja.dart | 30 | sí | Línea de ayuda con ícono de la hoja de selección. |
| packages/diseno_flutter/lib/atomos/barra_de_puntos.dart | 80 | sí | Indicador de láminas (pasivo o navegable). |
| packages/diseno_flutter/lib/atomos/borde_con_halo.dart | 58 | sí | OutlineInputBorder con anillo de foco. |
| packages/diseno_flutter/lib/atomos/boton.dart | 185 | sí | Botón principal con variantes, carga y motivo de bloqueo. |
| packages/diseno_flutter/lib/atomos/boton_flotante.dart | 43 | sí | FAB circular sobre SuperficieViva. |
| packages/diseno_flutter/lib/atomos/boton_icono.dart | 50 | sí | IconButton redondo de 48 dp con tooltip. |
| packages/diseno_flutter/lib/atomos/boton_tamano.dart | 1 | sí | Enum de tamaños de botón. |
| packages/diseno_flutter/lib/atomos/boton_variante.dart | 3 | sí | Enum de variantes de botón. |
| packages/diseno_flutter/lib/atomos/campo.dart | 145 | sí | Campo de texto base con etiqueta, ayuda, error y éxito. |
| packages/diseno_flutter/lib/atomos/campo_busqueda.dart | 52 | sí | Campo de búsqueda con borrar. |
| packages/diseno_flutter/lib/atomos/campo_codigo.dart | 36 | sí | Campo de código de invitación (5 chars, mayúsculas). |
| packages/diseno_flutter/lib/atomos/campo_contrasena.dart | 84 | sí | Campo de contraseña con ojo y medidor de fortaleza. |
| packages/diseno_flutter/lib/atomos/campo_de_seleccion.dart | 168 | sí | Select que abre hoja con buscador. |
| packages/diseno_flutter/lib/atomos/campo_monto.dart | 80 | sí | Campo de importe que emite cadena del contrato. |
| packages/diseno_flutter/lib/atomos/campo_monto_grande.dart | 163 | sí | Importe protagonista con montos sugeridos. |
| packages/diseno_flutter/lib/atomos/campo_o_t_p.dart | 152 | sí | Seis celdas OTP/PIN. |
| packages/diseno_flutter/lib/atomos/casilla.dart | 43 | sí | Checkbox con etiqueta tocable. |
| packages/diseno_flutter/lib/atomos/chip_elegible.dart | 96 | sí | Chip de elección/filtro. |
| packages/diseno_flutter/lib/atomos/chip_estado.dart | 64 | sí | Chip de estado con punto y tono. |
| packages/diseno_flutter/lib/atomos/codigo_q_r.dart | 108 | sí | Dibujo de QR y exportación PNG para WhatsApp. |
| packages/diseno_flutter/lib/atomos/colores_de_turno.dart | 36 | sí | Paleta cíclica de colores por turno. |
| packages/diseno_flutter/lib/atomos/cuenta_enmascarada.dart | 36 | sí | Número de cuenta enmascarado a 4 dígitos. |
| packages/diseno_flutter/lib/atomos/decoracion_de_campo.dart | 110 | sí | InputDecoration compartida de campos. |
| packages/diseno_flutter/lib/atomos/entrada_de_estado.dart | 20 | sí | Fundido de entrada para vacío/error. |
| packages/diseno_flutter/lib/atomos/esqueleto.dart | 35 | sí | Bloques placeholder de carga. |
| packages/diseno_flutter/lib/atomos/esqueleto_brillante.dart | 64 | sí | Destello sobre placeholders (respeta movimiento reducido). |
| packages/diseno_flutter/lib/atomos/estrellas_calificacion.dart | 37 | sí | Calificación 1–5 con estrellas. |
| packages/diseno_flutter/lib/atomos/fecha.dart | 55 | sí | Formatea instantes a hora La Paz (UTC-4 fijo) y días de calendario. |
| packages/diseno_flutter/lib/atomos/fila_de_seleccion.dart | 104 | sí | Fila de la hoja de selección con resaltado. |
| packages/diseno_flutter/lib/atomos/filtrar_opciones_de_hoja.dart | 16 | sí | Filtro de índices por texto normalizado. |
| packages/diseno_flutter/lib/atomos/girador.dart | 31 | sí | Spinner con etiqueta accesible. |
| packages/diseno_flutter/lib/atomos/grupo_radio.dart | 54 | sí | Grupo de radios (o texto si hay 1 opción). |
| packages/diseno_flutter/lib/atomos/hoja_de_seleccion.dart | 183 | sí | Bottom sheet de selección con buscador. |
| packages/diseno_flutter/lib/atomos/hundido_al_tocar.dart | 79 | sí | Escala 0,97 al presionar con resorte. |
| packages/diseno_flutter/lib/atomos/iconos_de_marca.dart | 79 | sí | Íconos Lucide por significado. |
| packages/diseno_flutter/lib/atomos/insignia_de_etapa.dart | 94 | sí | Insignia redonda de etapa (actual/alcanzada/bloqueada). |
| packages/diseno_flutter/lib/atomos/interruptor.dart | 40 | sí | Switch con etiqueta. |
| packages/diseno_flutter/lib/atomos/lista_con_buscador.dart | 20 | sí | Fachada de compatibilidad sobre la hoja de selección. |
| packages/diseno_flutter/lib/atomos/logotipo.dart | 77 | sí | Isotipo + palabra AportaYa apilados. |
| packages/diseno_flutter/lib/atomos/luz_de_boton.dart | 41 | sí | Painter del filo de luz/brillo del botón. |
| packages/diseno_flutter/lib/atomos/marca.dart | 54 | sí | Isotipo dibujado con CustomPaint. |
| packages/diseno_flutter/lib/atomos/monto.dart | 40 | sí | Único átomo que pinta dinero desde cadena del contrato. |
| packages/diseno_flutter/lib/atomos/monto_que_sube.dart | 128 | sí | Importe animado que cuenta hasta el saldo. |
| packages/diseno_flutter/lib/atomos/movimiento.dart | 125 | sí | Duraciones/curvas/resortes y helpers de movimiento reducido. |
| packages/diseno_flutter/lib/atomos/normalizar_para_buscar.dart | 11 | sí | Normaliza tildes y mayúsculas. |
| packages/diseno_flutter/lib/atomos/numero_que_sube.dart | 114 | sí | Entero animado (puntos). |
| packages/diseno_flutter/lib/atomos/palabra_de_marca.dart | 54 | sí | Palabra «AportaYa» con «Ya» en acento. |
| packages/diseno_flutter/lib/atomos/pintor_de_aura.dart | 116 | sí | Fondo de luces y viñeteo. |
| packages/diseno_flutter/lib/atomos/pintor_de_marca.dart | 104 | sí | Painter de los seis trazos del isotipo. |
| packages/diseno_flutter/lib/atomos/pintor_de_rueda.dart | 79 | sí | Painter de la rueda de turnos. |
| packages/diseno_flutter/lib/atomos/progreso.dart | 63 | sí | Barra/anillo de progreso con valor accesible. |
| packages/diseno_flutter/lib/atomos/punto.dart | 41 | sí | Punto de novedades con cuenta. |
| packages/diseno_flutter/lib/atomos/rueda.dart | 73 | sí | Rueda de turnos cobrados/pendientes. |
| packages/diseno_flutter/lib/atomos/selector_segmentado.dart | 109 | sí | Control segmentado. |
| packages/diseno_flutter/lib/atomos/superficie_viva.dart | 168 | sí | Piel de botón con hundido y brillo. |
| packages/diseno_flutter/lib/atomos/tono.dart | 17 | sí | Enum de tonos semánticos y sus colores. |
| packages/diseno_flutter/lib/catalogo/catalogo.dart | 87 | sí | Widgetbook claro/oscuro y página de catálogo para goldens. |
| packages/diseno_flutter/lib/catalogo/muestras.dart | 1180 | sí | Muestras de catálogo con datos ficticios (1180 líneas). |
| packages/diseno_flutter/lib/dinero/formatear.dart | 103 | sí | Formateo/aritmética de importes como cadena y centavos. |
| packages/diseno_flutter/lib/errores.dart | 8 | sí | Interfaz ErrorPresentable. |
| packages/diseno_flutter/lib/fuentes/Bricolage-600.ttf | binario (90864 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Bricolage-700.ttf | binario (90788 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Bricolage-800.ttf | binario (90836 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Instrument-400.ttf | binario (68712 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Instrument-500.ttf | binario (68932 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/Instrument-600.ttf | binario (68900 B) | no (binario, solo inventario) | Fuente empaquetada. |
| packages/diseno_flutter/lib/fuentes/OFL-Bricolage.txt | 93 | sí | Licencia OFL de Bricolage. |
| packages/diseno_flutter/lib/fuentes/OFL-Instrument.txt | 93 | sí | Licencia OFL de Instrument Sans. |
| packages/diseno_flutter/lib/ilustraciones/arte_de_maqueta.dart | 52 | sí | Base de painters del tour (lienzo 140). |
| packages/diseno_flutter/lib/ilustraciones/arte_en_custodia.dart | 55 | sí | Lámina «en custodia» con comprobante verificado. |
| packages/diseno_flutter/lib/ilustraciones/arte_fondo_de_garantia.dart | 53 | sí | Lámina paraguas del fondo de garantía cubriendo la cuota. |
| packages/diseno_flutter/lib/ilustraciones/arte_sin_cuaderno.dart | 59 | sí | Lámina «sin cuaderno». |
| packages/diseno_flutter/lib/ilustraciones/arte_sorteo_a_la_vista.dart | 60 | sí | Lámina del sorteo. |
| packages/diseno_flutter/lib/ilustraciones/lienzo.dart | 31 | sí | Envoltorio Ilustracion con semántica. |
| packages/diseno_flutter/lib/ilustraciones/pedestal.dart | 56 | sí | Orbe con sombra bajo un dibujo. |
| packages/diseno_flutter/lib/moleculas/acciones_rapidas.dart | 79 | sí | Accesos circulares de la billetera. |
| packages/diseno_flutter/lib/moleculas/acordeon.dart | 40 | sí | ExpansionTile estilizado. |
| packages/diseno_flutter/lib/moleculas/alerta.dart | 75 | sí | Aviso con tono, detalle y acción. |
| packages/diseno_flutter/lib/moleculas/aparicion_escalonada.dart | 124 | sí | Entrada escalonada de hijos. |
| packages/diseno_flutter/lib/moleculas/barra_de_accion.dart | 53 | sí | Acción principal anclada abajo. |
| packages/diseno_flutter/lib/moleculas/barra_de_etapa.dart | 179 | sí | Etapa actual con barra de avance en diezmilésimos. |
| packages/diseno_flutter/lib/moleculas/barra_de_pasos.dart | 51 | sí | Paso N de M segmentado. |
| packages/diseno_flutter/lib/moleculas/cabecera.dart | 188 | sí | Cabeceras de inicio/sección y BotonDeCabecera. |
| packages/diseno_flutter/lib/moleculas/campo_de_fecha.dart | 174 | sí | Fecha en tres selects con validación de día. |
| packages/diseno_flutter/lib/moleculas/chips_de_filtro.dart | 48 | sí | Chips de filtro en Wrap. |
| packages/diseno_flutter/lib/moleculas/desglose_de_cobro.dart | 80 | sí | Líneas bruto/comisión/descuento y neto. |
| packages/diseno_flutter/lib/moleculas/escalera_de_etapas.dart | 79 | sí | Etapas de cobranza con canal y tope. |
| packages/diseno_flutter/lib/moleculas/fila_de_cotejo.dart | 95 | sí | Declarado vs leído de un documento. |
| packages/diseno_flutter/lib/moleculas/fila_de_dato.dart | 41 | sí | Etiqueta + valor seleccionable. |
| packages/diseno_flutter/lib/moleculas/fila_de_movimiento.dart | 193 | sí | Fila de extracto con signo e importe. |
| packages/diseno_flutter/lib/moleculas/grilla_metricas_miembro.dart | 123 | sí | Grilla de KPIs de miembro con valores por defecto ficticios. |
| packages/diseno_flutter/lib/moleculas/grilla_metricas_miembro_partes.dart | 92 | sí | Tarjeta individual de la grilla de métricas. |
| packages/diseno_flutter/lib/moleculas/insignia_tile.dart | 151 | sí | Tarjeta de logro con tiers (estilo GymSheet). |
| packages/diseno_flutter/lib/moleculas/insignia_tile_partes.dart | 114 | sí | Encabezado de la tarjeta de logro. |
| packages/diseno_flutter/lib/moleculas/intro_de_pantalla.dart | 70 | sí | Ícono, título y explicación de pantalla interior. |
| packages/diseno_flutter/lib/moleculas/item_de_notificacion.dart | 89 | sí | Ítem de bandeja de avisos. |
| packages/diseno_flutter/lib/moleculas/item_pasanaku.dart | 58 | sí | Grupo en una línea con avance y estado. |
| packages/diseno_flutter/lib/moleculas/medidor_de_rango.dart | 69 | sí | Rango mínimo/estimado/máximo de un importe. |
| packages/diseno_flutter/lib/moleculas/opcion_con_costo.dart | 78 | sí | Tarjeta de opción con su costo. |
| packages/diseno_flutter/lib/moleculas/opcion_seleccionable.dart | 101 | sí | Opción excluyente como tarjeta. |
| packages/diseno_flutter/lib/moleculas/parte_de_fecha.dart | 74 | sí | Meses, diasDelMes y un select de fecha. |
| packages/diseno_flutter/lib/moleculas/pestanas.dart | 61 | sí | Pestañas de contenido. |
| packages/diseno_flutter/lib/moleculas/pregunta.dart | 139 | sí | Bloque pregunta/control/ayuda y Contador ±1. |
| packages/diseno_flutter/lib/moleculas/referencia_de_error.dart | 26 | sí | Muestra trazaId de soporte. |
| packages/diseno_flutter/lib/moleculas/reloj_de_plazo.dart | 86 | sí | Plazo guardado con lo que falta. |
| packages/diseno_flutter/lib/moleculas/resumen_de_periodo.dart | 72 | sí | Entró/salió y saldos de un período. |
| packages/diseno_flutter/lib/moleculas/riel_de_turnos.dart | 151 | sí | Riel de turnos con pulso del actual. |
| packages/diseno_flutter/lib/moleculas/seccion.dart | 100 | sí | TituloDeSeccion, Panel y FilasDePanel. |
| packages/diseno_flutter/lib/moleculas/selectores_de_fecha_responsivos.dart | 104 | sí | Día/Mes/Año en fila o apilados. |
| packages/diseno_flutter/lib/moleculas/tarjeta.dart | 47 | sí | Superficie base de tarjeta. |
| packages/diseno_flutter/lib/moleculas/tarjeta_de_racha.dart | 72 | sí | Racha de meses al día. |
| packages/diseno_flutter/lib/moleculas/tarjeta_k_p_i.dart | 63 | sí | Indicador con variación y provisorio. |
| packages/diseno_flutter/lib/moleculas/tarjeta_puntos_mesa.dart | 137 | sí | Puntos «en la mesa de juego» con rango (estilo GymSheet). |
| packages/diseno_flutter/lib/moleculas/tarjeta_puntos_mesa_partes.dart | 85 | sí | Cabecera de puntos con ícono de casino. |
| packages/diseno_flutter/lib/moleculas/tarjeta_riesgo_confianza.dart | 129 | sí | Calificación de riesgo con valores por defecto ficticios. |
| packages/diseno_flutter/lib/moleculas/tarjeta_riesgo_confianza_partes.dart | 175 | sí | Cabecera y métricas de la tarjeta de riesgo. |
| packages/diseno_flutter/lib/moleculas/tarjeta_saldo.dart | 92 | sí | Saldo disponible/retenido/en pasanakus. |
| packages/diseno_flutter/lib/moleculas/tipo_de_movimiento.dart | 45 | sí | Enum de tipos de movimiento con ícono y nombre. |
| packages/diseno_flutter/lib/moviles/acciones_del_globo.dart | 77 | sí | Botones del globo de tutorial. |
| packages/diseno_flutter/lib/moviles/alcance_haptica.dart | 28 | sí | InheritedWidget de política háptica. |
| packages/diseno_flutter/lib/moviles/anclas_de_tutorial.dart | 120 | sí | Registro de GlobalKeys para tutoriales. |
| packages/diseno_flutter/lib/moviles/apertura_de_marca.dart | 145 | sí | Splash animado de marca (1,4 s). |
| packages/diseno_flutter/lib/moviles/barra_pestanas.dart | 174 | sí | Barra de pestañas flotante con píldora y acción central. |
| packages/diseno_flutter/lib/moviles/barra_pestanas_destino.dart | 191 | sí | Destino de la barra y botón central. |
| packages/diseno_flutter/lib/moviles/barra_pestanas_pildora.dart | 52 | sí | Píldora deslizante de la barra. |
| packages/diseno_flutter/lib/moviles/celebracion_de_exito.dart | 115 | sí | Círculo con tilde de «Cuenta creada». |
| packages/diseno_flutter/lib/moviles/como_en_un_telefono.dart | 114 | sí | Enmarca la app en un teléfono en web ancha. |
| packages/diseno_flutter/lib/moviles/foco_de_tutorial.dart | 119 | sí | Velo con agujero y anillo del tutorial. |
| packages/diseno_flutter/lib/moviles/fondo_de_marca.dart | 48 | sí | Fondo con aura de marca. |
| packages/diseno_flutter/lib/moviles/globo_de_tutorial.dart | 162 | sí | Globo del tutorial con progreso. |
| packages/diseno_flutter/lib/moviles/hoja_de_confirmacion.dart | 109 | sí | Bottom sheet para confirmar operación de dinero. |
| packages/diseno_flutter/lib/moviles/mostrar_snackbar.dart | 30 | sí | Snackbar verde con acción. |
| packages/diseno_flutter/lib/moviles/notificacion_emergente.dart | 91 | sí | Aviso emergente encolado. |
| packages/diseno_flutter/lib/moviles/panel_de_marca.dart | 128 | sí | Panel verde del zoom de marca (retirado). |
| packages/diseno_flutter/lib/moviles/pantalla_de_onboarding.dart | 53 | sí | Pantalla de tour genérica. |
| packages/diseno_flutter/lib/moviles/teclado_numerico.dart | 86 | sí | Teclado 3×4 para montos/PIN. |
| packages/diseno_flutter/lib/moviles/tour_en_stages.dart | 193 | sí | Walkthrough por etapas en bottom sheet. |
| packages/diseno_flutter/lib/moviles/tour_en_stages_datos.dart | 61 | sí | Textos por defecto del tour gamificado. |
| packages/diseno_flutter/lib/moviles/tour_en_stages_pagina.dart | 133 | sí | Página de una etapa del tour. |
| packages/diseno_flutter/lib/moviles/tour_en_stages_pie.dart | 77 | sí | Puntos y botones del tour. |
| packages/diseno_flutter/lib/moviles/transicion_con_zoom.dart | 67 | sí | Transición de página con zoom (reemplazada). |
| packages/diseno_flutter/lib/moviles/transicion_de_eje.dart | 80 | sí | Transición lateral corta con fundido. |
| packages/diseno_flutter/lib/moviles/transicion_de_marca.dart | 59 | sí | Transición de portada (fundido y subida). |
| packages/diseno_flutter/lib/organismos/banner_de_pauta.dart | 89 | sí | Publicidad rotulada. |
| packages/diseno_flutter/lib/organismos/calendario_de_cuotas.dart | 191 | sí | Calendario mensual de cuotas con total exigible. |
| packages/diseno_flutter/lib/organismos/camino_de_etapas.dart | 195 | sí | Camino vertical de etapas de reputación. |
| packages/diseno_flutter/lib/organismos/celda_de_cuota.dart | 127 | sí | Día del calendario de cuotas. |
| packages/diseno_flutter/lib/organismos/celebracion_stage.dart | 182 | sí | Diálogo de celebración de logro (rayos, partículas). |
| packages/diseno_flutter/lib/organismos/celebracion_stage_piezas.dart | 115 | sí | Ícono con halo y contador de puntos. |
| packages/diseno_flutter/lib/organismos/celebracion_stage_pintores.dart | 118 | sí | Estampilla, rayos y partículas. |
| packages/diseno_flutter/lib/organismos/celebracion_stage_tarjeta.dart | 97 | sí | Tarjeta central de la celebración. |
| packages/diseno_flutter/lib/organismos/cuota.dart | 8 | sí | Typedef Cuota (fecha, grupo, monto, estado). |
| packages/diseno_flutter/lib/organismos/encabezado_de_saldo.dart | 198 | sí | Tarjeta héroe del saldo con ocultar. |
| packages/diseno_flutter/lib/organismos/encabezado_de_saldo_piezas.dart | 120 | sí | Botón ocultar, chip, frase de custodia, transición. |
| packages/diseno_flutter/lib/organismos/estado_de_cuota.dart | 2 | sí | Enum de estados de cuota. |
| packages/diseno_flutter/lib/organismos/estado_de_pantalla.dart | 107 | sí | Pinta AsyncValue en cargando/error/vacío/datos. |
| packages/diseno_flutter/lib/organismos/estado_de_tramite.dart | 117 | sí | Estado de un trámite con pasos y caso. |
| packages/diseno_flutter/lib/organismos/estado_de_tramite_pasos.dart | 110 | sí | Pasos del trámite. |
| packages/diseno_flutter/lib/organismos/estado_error.dart | 110 | sí | Error con mensaje, traza y reintento. |
| packages/diseno_flutter/lib/organismos/estado_vacio.dart | 83 | sí | Vacío que orienta. |
| packages/diseno_flutter/lib/organismos/fila_de_cuota.dart | 46 | sí | Cuota del día elegido. |
| packages/diseno_flutter/lib/organismos/leyenda_de_cuotas.dart | 79 | sí | Leyenda del calendario. |
| packages/diseno_flutter/lib/organismos/lista_de_movimientos.dart | 139 | sí | Extracto agrupado por día con neto. |
| packages/diseno_flutter/lib/organismos/lista_de_requisitos.dart | 87 | sí | Requisitos cumplidos/faltantes (habilitación). |
| packages/diseno_flutter/lib/organismos/motivo_vacio.dart | 3 | sí | Enum de motivos de vacío. |
| packages/diseno_flutter/lib/organismos/movimiento.dart | 11 | sí | Typedef Movimiento. |
| packages/diseno_flutter/lib/organismos/panel_bienvenida.dart | 169 | sí | Bienvenida con bono y pasos. |
| packages/diseno_flutter/lib/organismos/panel_de_factores.dart | 90 | sí | Veredicto aprobado/no con factores. |
| packages/diseno_flutter/lib/organismos/panel_sorteo.dart | 106 | sí | Pasos del sorteo con hash y reproducción. |
| packages/diseno_flutter/lib/organismos/pantalla_con_cabecera.dart | 199 | sí | Pantalla con cabecera que se compacta. |
| packages/diseno_flutter/lib/organismos/tarjeta_de_oferta.dart | 89 | sí | Oferta de permuta de turno. |
| packages/diseno_flutter/lib/organismos/tarjeta_de_solicitud.dart | 155 | sí | Solicitud de ingreso con aceptar/rechazar. |
| packages/diseno_flutter/lib/organismos/vale.dart | 117 | sí | Cupón de comercio. |
| packages/diseno_flutter/lib/tema.dart | 159 | sí | ThemeData desde tokens. |
| packages/diseno_flutter/lib/tema_de_campos.dart | 45 | sí | InputDecorationTheme. |
| packages/diseno_flutter/lib/tema_de_controles.dart | 93 | sí | Estilos de botones y controles. |
| packages/diseno_flutter/lib/tema_de_superficies.dart | 68 | sí | Tarjeta, AppBar, hoja, diálogo, snackbar. |
| packages/diseno_flutter/package.json | 17 | sí | Envoltorio turbo: build copia tokens.dart, typecheck, lint, tests. |
| packages/diseno_flutter/pubspec.lock | 434 | no (muestreado: dependencias directas y sdks) | Lockfile de dependencias Dart (muestreado: versiones directas y sdks). |
| packages/diseno_flutter/pubspec.yaml | 46 | sí | Paquete aportaya_diseno: deps (riverpod, qr_flutter, lucide, widgetbook) y fuentes. |
| packages/diseno_flutter/test/a11y/catalogo_a11y_test.dart | 32 | sí | Guías de a11y sobre el catálogo en claro/oscuro. |
| packages/diseno_flutter/test/capturas/catalogo_capturas_test.dart | 101 | sí | Capturas con fuentes reales hacia docs/. |
| packages/diseno_flutter/test/comun.dart | 38 | sí | Helpers conTema y pantallaChica. |
| packages/diseno_flutter/test/goldens/catalogo_golden_test.dart | 68 | sí | Goldens del catálogo (no en CI). |
| packages/diseno_flutter/test/goldens/ilustraciones_golden_test.dart | 68 | sí | Goldens de ilustraciones. |
| packages/diseno_flutter/test/goldens/imagenes/apertura_medio.png | binario (8545 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/apertura_tarde.png | binario (13647 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/apertura_temprano.png | binario (5541 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_botones_claro.png | binario (23527 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_botones_oscuro.png | binario (23460 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_campos_claro.png | binario (18255 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_campos_oscuro.png | binario (18521 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_indicadores_claro.png | binario (46015 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_indicadores_oscuro.png | binario (49147 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_seleccion_claro.png | binario (16297 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/atomos_seleccion_oscuro.png | binario (16343 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/ilustraciones_claro.png | binario (17993 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/ilustraciones_oscuro.png | binario (17702 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/marca_claro.png | binario (7035 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/marca_oscuro.png | binario (6999 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/moleculas_claro.png | binario (40464 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/moleculas_oscuro.png | binario (40902 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/moviles_claro.png | binario (36205 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/moviles_oscuro.png | binario (40329 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/organismos_claro.png | binario (23324 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/organismos_oscuro.png | binario (25090 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/reputacion_etapas_claro.png | binario (36204 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/imagenes/reputacion_etapas_oscuro.png | binario (35832 B) | no (binario, solo inventario) | Golden PNG de referencia. |
| packages/diseno_flutter/test/goldens/marca_golden_test.dart | 66 | sí | Goldens de marca y apertura. |
| packages/diseno_flutter/test/unidad/aspecto_de_etapa_test.dart | 40 | sí | Contraste y unicidad de colores de etapa. |
| packages/diseno_flutter/test/unidad/fecha_test.dart | 21 | sí | Fecha de calendario y conversión a La Paz. |
| packages/diseno_flutter/test/unidad/formatear_monto_test.dart | 34 | sí | Vectores compartidos de formato de dinero. |
| packages/diseno_flutter/test/unidad/formatear_porcentaje_test.dart | 19 | sí | Formato de porcentaje. |
| packages/diseno_flutter/test/unidad/logica_pura_test.dart | 149 | sí | Elevación, CampoMonto, máscara, fecha, plazo, neto, exigible. |
| packages/diseno_flutter/test/unidad/movimiento_test.dart | 39 | sí | Parámetros de resortes y tope de 320 ms. |
| packages/diseno_flutter/test/widget/barra_pestanas_resorte_test.dart | 80 | sí | Píldora con resorte y movimiento reducido. |
| packages/diseno_flutter/test/widget/barra_pestanas_test.dart | 171 | sí | Semántica, toque y botón central. |
| packages/diseno_flutter/test/widget/campo_de_ejemplo_test.dart | 99 | sí | Ejemplo/hint de los campos. |
| packages/diseno_flutter/test/widget/campo_de_fecha_test.dart | 133 | sí | Selects de fecha y validación. |
| packages/diseno_flutter/test/widget/campo_de_seleccion_test.dart | 207 | sí | Hoja con buscador y foco. |
| packages/diseno_flutter/test/widget/campo_monto_grande_test.dart | 72 | sí | Contrato y sugeridos del monto grande. |
| packages/diseno_flutter/test/widget/campo_relleno_y_halo_test.dart | 39 | sí | Relleno y halo del campo. |
| packages/diseno_flutter/test/widget/comportamiento_test.dart | 573 | sí | Comportamientos varios (estados, OTP, monto, solicitud). |
| packages/diseno_flutter/test/widget/encabezado_de_saldo_desenfoque_test.dart | 73 | sí | Desenfoque al ocultar saldo. |
| packages/diseno_flutter/test/widget/encabezado_de_saldo_test.dart | 78 | sí | Semántica y ocultar del saldo. |
| packages/diseno_flutter/test/widget/estado_de_tramite_test.dart | 91 | sí | Pasos del trámite. |
| packages/diseno_flutter/test/widget/fila_de_cotejo_test.dart | 69 | sí | Cotejo pendiente/coincide/difiere. |
| packages/diseno_flutter/test/widget/fila_de_dato_test.dart | 28 | sí | Valor seleccionable. |
| packages/diseno_flutter/test/widget/fila_de_movimiento_test.dart | 96 | sí | Signo, semántica y apilado. |
| packages/diseno_flutter/test/widget/hoja_de_seleccion_lista_corta_con_teclado_test.dart | 54 | sí | Filas visibles con teclado. |
| packages/diseno_flutter/test/widget/hoja_de_seleccion_test.dart | 219 | sí | Hoja de selección: filtro, foco, semántica. |
| packages/diseno_flutter/test/widget/intro_de_pantalla_test.dart | 49 | sí | Encabezado y tono de peligro. |
| packages/diseno_flutter/test/widget/monto_que_sube_test.dart | 176 | sí | Reglas de animación del saldo. |
| packages/diseno_flutter/test/widget/movimiento_test.dart | 253 | sí | Botones vivos y transición sin zoom. |
| packages/diseno_flutter/test/widget/pantalla_con_cabecera_test.dart | 133 | sí | Cabecera compacta. |
| packages/diseno_flutter/test/widget/roles_de_color_accion_test.dart | 106 | sí | Colores por variante de botón. |
| packages/diseno_flutter/test/widget/roles_de_color_no_estado_test.dart | 400 | sí | Colores neutrales en varias piezas. |


### 12.B — simulado, tokens, dominio-cliente, clientes
#### 1) Tabla de cobertura
| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| clientes/angular/publicidad/model/entradaAnunciante.ts | 39 | sí | Modelo generado typescript-angular versionado como "doble" bajo ruta ignorada |
| clientes/angular/publicidad/model/entradaCampana.ts | 39 | sí | Modelo generado typescript-angular versionado como "doble" bajo ruta ignorada |
| clientes/angular/publicidad/model/entradaRevision.ts | 25 | sí | Modelo generado typescript-angular versionado como "doble" bajo ruta ignorada |
| clientes/angular/publicidad/model/salidaCampana.ts | 30 | sí | Modelo generado typescript-angular versionado como "doble" bajo ruta ignorada |
| clientes/dart/aportes/lib/aportaya_cliente_aportes.dart | 37 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/aportes/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/cumplimiento/lib/aportaya_cliente_cumplimiento.dart | 67 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/cumplimiento/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/entregas/lib/aportaya_cliente_entregas.dart | 49 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/entregas/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/garantia/lib/aportaya_cliente_garantia.dart | 53 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/garantia/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/grupos/lib/aportaya_cliente_grupos.dart | 82 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/grupos/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/identidad/lib/aportaya_cliente_identidad.dart | 65 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/identidad/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/notificaciones/lib/aportaya_cliente_notificaciones.dart | 31 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/notificaciones/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/nucleo-financiero/lib/aportaya_cliente_nucleofinanciero.dart | 67 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/nucleo-financiero/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/organizador/lib/aportaya_cliente_organizador.dart | 55 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/organizador/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/tarifas/lib/aportaya_cliente_tarifas.dart | 42 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/tarifas/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| clientes/dart/transparencia/lib/aportaya_cliente_transparencia.dart | 57 | sí | Barril generado (dart-dio) versionado bajo ruta ignorada; exporta api/model de src/ no versionado |
| clientes/dart/transparencia/pubspec.yaml | 17 | sí | pubspec generado del cliente Dart |
| packages/dominio-cliente/eslint.config.js | 7 | sí | Config ESLint |
| packages/dominio-cliente/package.json | 27 | sí | Manifiesto dominio-cliente |
| packages/dominio-cliente/src/cadena.spec.ts | 68 | sí | Autoconsistencia de la cadena |
| packages/dominio-cliente/src/cadena.ts | 90 | sí | serializarCanonico/hashDeBloque/recorrerCadena (sin referencia backend) |
| packages/dominio-cliente/src/configuracion.spec.ts | 140 | sí | Casos válidos/límite/inválidos de validarConfiguracion |
| packages/dominio-cliente/src/configuracion.ts | 122 | sí | Valida URL de gateway (D-A6) |
| packages/dominio-cliente/src/index.ts | 4 | sí | Barril |
| packages/dominio-cliente/src/posiciones.spec.ts | 81 | sí | Tres niveles de separarPosiciones |
| packages/dominio-cliente/src/posiciones.ts | 106 | sí | Separa disponible/gastable/comprometido/retenido con BigInt |
| packages/dominio-cliente/src/sha256.ts | 24 | sí | SHA-256 sobre SubtleCrypto + índice modular |
| packages/dominio-cliente/src/sorteo.spec.ts | 54 | sí | Vectores dorados del sorteo |
| packages/dominio-cliente/src/sorteo.ts | 51 | sí | Reimplementación TS de SorteoVerificable (compromiso + Fisher-Yates) |
| packages/dominio-cliente/tsconfig.json | 6 | sí | Config TS |
| packages/dominio-cliente/vectores/README.md | 31 | sí | Origen de los vectores (Python, no Java) |
| packages/dominio-cliente/vectores/sorteo.vectores.json | 146 | sí | 4 vectores dorados del sorteo |
| packages/dominio-cliente/vitest.config.ts | 2 | sí | Config vitest |
| packages/simulado/.gitignore | 2 | sí | Ignora generado/ |
| packages/simulado/ejemplos/aportes/aprobarReembolso.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/reembolsos/{reembolsoId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/cobrarAporte.json | 78 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /aportes/obligaciones/{obligacionId}/pagos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/consultarEstadoDelParticipante.json | 41 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /aportes/participantes/{participanteId}/estado (ok, vacio, adverso) |
| packages/simulado/ejemplos/aportes/consultarRecaudoDelPeriodo.json | 108 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /aportes/periodos/{periodoId}/recaudo (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/aportes/consultarRecaudoDiario.json | 60 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /aportes/recaudo-diario (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/aportes/contarMorosos.json | 29 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /aportes/grupos/{grupoId}/morosos (ok, vacio, adverso) |
| packages/simulado/ejemplos/aportes/darDeAltaProveedor.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/proveedores (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/enrutarCobro.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/enrutamiento (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/generarObligacionesDelPeriodo.json | 153 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /aportes/periodos/{periodoId}/obligaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/listarProveedoresDePago.json | 49 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /pagos/proveedores (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/aportes/listarWebhooksRecibidos.json | 51 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /pagos/webhooks-recibidos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/aportes/recibirWebhookPasarela.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/webhooks/{proveedorCodigo} (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/registrarDisputa.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/{pagoId}/disputas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/aportes/solicitarReembolso.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /pagos/{pagoId}/reembolsos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/actualizarEstadoElementoCatalogo.json | 90 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PATCH /auditoria/catalogos/{codigo}/elementos/{id} (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarBitacora.json | 86 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/bitacora (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarCatalogoOperativo.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/catalogos/{codigo} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarEventoBitacora.json | 86 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/bitacora/{id} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarOutboxPorServicio.json | 59 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/operacion/outbox (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/consultarSaludDeServicios.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/operacion/servicios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/descargarExportacion.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reportes/exportaciones/{exportacionId}/descargas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/ejecutarReporte.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reportes/{definicionId}/ejecuciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/ejercerDerechos.json | 76 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /auditoria/derechos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/guardarFichaDeGobierno.json | 82 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/gobierno (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/auditoria/listarCatalogosOperativos.json | 53 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/catalogos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarDefinicionesDeReporte.json | 59 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reportes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarEjecucionesDeReporte.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reportes/ejecuciones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarElementosCatalogo.json | 57 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /auditoria/catalogos/{codigo}/elementos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarEndpointsDelDia.json | 64 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/operacion/endpoints (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarIncidentesOperativos.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/operacion/incidentes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/listarTablasDelCatalogo.json | 600 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/catalogo-de-datos/tablas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/obtenerCatalogoDeDatos.json | 140 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/catalogo-de-datos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/obtenerResumenDelCatalogo.json | 402 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/catalogo-de-datos/resumen (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/obtenerTablaDelCatalogo.json | 550 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores/catalogo-de-datos/tablas/{esquema}/{tabla} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/publicarTablero.json | 93 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /indicadores (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/auditoria/revisarColumna.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /indicadores/catalogo-de-datos/{esquema}/{tabla}/columnas/{columna}/revision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/abrirCasoLft.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/casos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/aceptarContrato.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/contratos/{contratoId}/aceptaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/aprobarYEnviarReporte.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /uif/reportes/envios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/consultarEfectoDeReclamos.json | 75 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reclamos/usuarios/{usuarioId}/efecto (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/consultarReclamo.json | 95 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reclamos/{reclamoId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/decidirCasoLft.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/casos/{casoId}/decision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/declararPep.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/usuarios/{usuarioId}/pep (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/elevarDiligencia.json | 86 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/usuarios/{usuarioId}/diligencia (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/generarReporteRegulatorio.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /uif/reportes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarActasDeComite.json | 59 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/comites/actas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarBandejaDeAlertas.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/alertas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarBandejaDeCasos.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/casos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarBandejaDeReclamos.json | 75 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reclamos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarContratosVigentes.json | 76 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/contratos/vigentes (ok, vacio, adverso) |
| packages/simulado/ejemplos/cumplimiento/listarEventosDeRiesgo.json | 89 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/riesgos/eventos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarIncidentesDeSeguridad.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/seguridad/incidentes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarPruebasDeContinuidad.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /cumplimiento/continuidad/pruebas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarReportesRegulatorios.json | 69 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /uif/reportes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/listarRequerimientosDeAutoridad.json | 67 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /uif/requerimientos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/cumplimiento/notificarTitulares.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/seguridad/incidentes/{incidenteId}/notificaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/registrarIncidente.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/seguridad/incidentes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/registrarPruebaDeContinuidad.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/continuidad/pruebas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/registrarRequerimientoDeAutoridad.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /uif/requerimientos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/registrarRiesgoOperativo.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/riesgos/eventos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/reportarIncidente.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /cumplimiento/seguridad/incidentes/{incidenteId}/reportes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/responderReclamo.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reclamos/{reclamoId}/respuesta (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/responderRequerimientoDeAutoridad.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /uif/requerimientos/{requerimientoId}/respuesta (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/cumplimiento/verificarAlcance.json | 53 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /licencia/alcance (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/abrirSubastaDeCupo.json | 174 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/subastas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/anotarRespuestaDesembolso.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /desembolsos/{ordenId}/respuesta (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/autorizarEntrega.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/{entregaId}/autorizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/cancelarOferta.json | 54 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/mercado/ofertas/{ofertaId}/cancelacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/cancelarSubastaDeCupo.json | 174 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/subastas/{subastaId}/cancelacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/comprarOferta.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/mercado/ofertas/{ofertaId}/compra (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/consultarDisponibilidad.json | 41 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /cuentas-bancarias/{cuentaId}/disponibilidad (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/consultarOfertaParaPago.json | 65 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /entregas/mercado/ofertas/{ofertaId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/consultarSubastaDeCupo.json | 128 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /entregas/subastas/{subastaId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/designarCuentaPrincipal.json | 50 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /cuentas-bancarias/{cuentaId}/principal (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/ejecutarEntrega.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/{entregaId}/ejecucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/emitirOrdenDesembolso.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /desembolsos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/fondearPozo.json | 142 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/pozo-completo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/liquidarEntrega.json | 102 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/listarDesembolsos.json | 75 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /desembolsos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/listarOfertas.json | 76 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /entregas/mercado/ofertas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/listarSubastasDeCupo.json | 108 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /entregas/subastas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/entregas/publicarOferta.json | 54 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/mercado/ofertas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/pujarEnSubasta.json | 54 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /entregas/subastas/{subastaId}/pujas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/registrarCuentaDestino.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /cuentas-bancarias (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/entregas/verificarCuentaDestino.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /cuentas-bancarias/{cuentaId}/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/abrirCuentaPorCobrar.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/cuentas-por-cobrar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/abrirEjercicioFiscal.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/ejercicios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/aprobarOrdenDeCompra.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/ordenes-de-compra/{ordenId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/aprobarPresupuesto.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/presupuestos/{presupuestoId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/cerrarPeriodoContable.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/periodos/{periodoId}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/consultarPeriodoVigente.json | 53 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/periodos/vigente (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/correrDepreciacionDelPeriodo.json | 90 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/periodos/{periodoId}/depreciaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/crearOrdenDeCompra.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/ordenes-de-compra (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/crearPresupuesto.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/presupuestos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/darDeAltaTercero.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/terceros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/depreciarActivo.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/activos/{activoId}/depreciaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/descargarEstadoFinanciero.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/estados-financieros/{estadoFinancieroId}/documento (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/generarEstadoFinanciero.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/periodos/{periodoId}/estados-financieros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/listarActivosFijos.json | 95 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/activos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarCuentasPorCobrar.json | 85 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/cuentas-por-cobrar (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarEjerciciosFiscales.json | 76 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/ejercicios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarFacturasDeProveedor.json | 91 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/facturas-de-proveedor (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarOrdenesDeCompra.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/ordenes-de-compra (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarPartidasPresupuestarias.json | 85 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/partidas-presupuestarias (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/listarPeriodosDelEjercicio.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/ejercicios/{ejercicioId}/periodos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/erp/pagarFacturaDeProveedor.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/facturas-de-proveedor/{facturaId}/pagos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/registrarCobro.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/cuentas-por-cobrar/{cuentaId}/cobros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/registrarFacturaDeProveedor.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /erp/facturas-de-proveedor (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/erp/validarPlantillaDeAsiento.json | 44 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /erp/plantillas-de-asiento/{plantillaId}/validacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/garantia/ampliarRespaldo.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/reservas/{reservaId}/ampliacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/aprobarReemplazo.json | 50 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/reemplazos/{reemplazoId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/cerrarDisolucion.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/disoluciones/{disolucionId}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/consultarRestriccion.json | 32 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /cobranza/restricciones/vigentes/{usuarioId} (ok, vacio, adverso) |
| packages/simulado/ejemplos/garantia/cubrirConRespaldo.json | 126 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/coberturas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/cubrirIncumplimiento.json | 82 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/cobertura (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/declararIncumplimiento.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/devolverFondo.json | 138 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/fondos/{grupoId}/devolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/ejecutarAval.json | 78 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/aval/ejecucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/ejecutarReemplazo.json | 90 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/reemplazos/{reemplazoId}/ejecucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/iniciarDisolucion.json | 170 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/grupos/{grupoId}/disolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/levantarRestriccion.json | 50 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /cobranza/restricciones/{restriccionId}/levantamiento (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/liberarRespaldo.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/reservas/{reservaId}/liberacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/presentarDescargo.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/descargo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/proponerReemplazo.json | 90 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/reemplazo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/recuperarRespaldo.json | 102 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/recuperaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/reservarRespaldo.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/reservas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/resolverDescargo.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/descargo/resolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/restringirDeudor.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /incumplimientos/{expedienteId}/restriccion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/garantia/reversarCoberturaRespaldo.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /garantia/respaldo/coberturas/{coberturaId}/reversa (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/aceptarInvitacionPorEnlace.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/invitaciones/enlace/aceptar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/anularEnlaceDelGrupo.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/enlace-del-grupo/anulacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/calcularPlazoHabil.json | 48 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/calendario/calcular (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/calcularVencimientoLocal.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/calendario/vencimiento (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/canjearInvitacion.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/invitaciones/{invitacionId}/canje (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/comprometerSorteo.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/sorteo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/consultarActividad.json | 26 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/participantes/{usuarioId}/actividad (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarAdmisibilidadDePago.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/obligaciones/admisibilidad (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarCalendarioDeAportes.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/periodos/{periodoId}/calendario-de-aportes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/consultarDetalleDelGrupo.json | 114 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/detalle (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/consultarElegibilidadParaSubasta.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/elegibilidad-para-subasta (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/consultarEnlaceDelGrupo.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/enlace-del-grupo/consultar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/consultarGrupoDeBackoffice.json | 87 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/backoffice/{grupoId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/consultarInvitacionPorEnlace.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/invitaciones/enlace/consultar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/consultarMisLimites.json | 35 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/mis-limites (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarPaqueteDelSorteo.json | 56 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/sorteos/{sorteoId}/paquete (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarSiCompartenGrupo.json | 26 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/integrantes/{usuarioId}/comparte (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/consultarSubastabilidadDelCupo.json | 65 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/cupos/{cupoId}/subastabilidad (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/crearGrupo.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/decidirSolicitudDeIngreso.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/solicitudes/{solicitudId}/decision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/firmarReglamentoDelGrupo.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/reglamento/firma (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/generarEnlaceDelGrupo.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/enlace-del-grupo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/historialAdmision.json | 82 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/solicitudes/{solicitudId}/decisiones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/iniciarGrupo.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/inicio (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/invitarAlGrupo.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/invitaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/listarAcuerdos.json | 49 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /acuerdos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/listarBandejaDeAdmisiones.json | 57 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/solicitudes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/listarGruposDeBackoffice.json | 55 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/backoffice (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/listarMisParticipaciones.json | 32 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/participaciones (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/listarSolicitudesDeIngreso.json | 49 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/{grupoId}/solicitudes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/grupos/postularAlGrupo.json | 64 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/postulaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/proponerAcuerdo.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /acuerdos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/proponerAdmision.json | 86 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/solicitudes/{solicitudId}/propuestas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/resolverAdmision.json | 92 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/solicitudes/{solicitudId}/resoluciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/resolverAlias.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /grupos/participantes/alias/{alias} (ok, vacio, adverso) |
| packages/simulado/ejemplos/grupos/revelarSorteo.json | 68 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/sorteo/revelacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/revocarInvitacionDelGrupo.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/invitaciones/{invitacionId}/revocacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/solicitarIngresoPorEnlace.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/enlace-del-grupo/solicitudes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/solicitarPermuta.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /turnos/permutas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/solicitarRetiro.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/retiros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/sustituirAdministrador.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/administrador/sustitucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/traspasarCupo.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/cupos/{cupoId}/traspasos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/traspasarCupoPorSubasta.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /grupos/{grupoId}/cupos/{cupoId}/traspasos-por-subasta (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/grupos/votarAcuerdo.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /acuerdos/{acuerdoId}/votos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/abrirDesafioDeStepUp.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /sesiones/desafios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/activarRolOrganizador.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/{usuarioId}/roles/organizador (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/actualizarFechaExpiracionDocumento.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /usuarios/{usuarioId}/documento/fecha-expiracion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/actualizarUsuarioInterno.json | 130 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PATCH /usuarios-internos/{id} (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/asignarRolesUsuarioInterno.json | 130 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /usuarios-internos/{id}/roles (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/autenticar.json | 67 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /sesiones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/buscarPorTelefono.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/por-telefono (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/buscarUsuarioPorTelefono.json | 45 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/busquedas/por-telefono (ok, vacio, adverso, aceptado, intermitente) |
| packages/simulado/ejemplos/identidad/cambiarMiFotoDePerfil.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /usuarios/yo/foto-de-perfil (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/cerrarSesion.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos DELETE /sesion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/confirmarVerificacionCorreo.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/verificaciones/correo/{verificacionId}/confirmacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/consultarContactoEnGrupo.json | 35 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /identidad/contactos/grupos/{grupoId}/integrantes/{usuarioId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/consultarEstadoDeVerificacion.json | 48 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/{usuarioId}/verificacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/consultarMiPerfil.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/yo/perfil (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/consultarNivelKyc.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/{usuarioId}/kyc (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/consultarUsuarioInterno.json | 94 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios-internos/{id} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/consumirInvitacion.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/tokens/invitacion/{tokenId}/consumos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/crearUsuarioInterno.json | 130 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios-internos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/desbloquearUsuarioInterno.json | 130 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios-internos/{id}/desbloquear (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/emitirTokenDeInvitacion.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/tokens/invitacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/guardarBorradorOnboarding.json | 106 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos PUT /usuarios/verificaciones/correo/{verificacionId}/borrador (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/iniciarVerificacion.json | 72 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/{usuarioId}/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/listarPermisos.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /permisos (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/listarRoles.json | 46 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /roles (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/listarUsuariosInternos.json | 69 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios-internos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/listarVerificaciones.json | 56 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /identidad/verificaciones (ok, vacio, adverso) |
| packages/simulado/ejemplos/identidad/registrarUsuario.json | 101 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/renovarSesion.json | 64 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /sesion/refrescar (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/resolverVerificacion.json | 104 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /identidad/verificaciones/{verificacionId}/decision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/revocarInvitacion.json | 42 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/tokens/invitacion/{tokenId}/revocacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/solicitarVerificacionCorreo.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/verificaciones/correo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/subirDocumento.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/{usuarioId}/documentos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/validarTokenDeInvitacion.json | 41 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/tokens/invitacion/validar (ok, vacio, adverso, aceptado, intermitente) |
| packages/simulado/ejemplos/identidad/verContenidoDeFoto.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /identidad/verificaciones/{verificacionId}/fotos/{cara}/contenido (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/verFotoDelExpediente.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /identidad/verificaciones/{verificacionId}/fotos/{cara} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/verMiFotoDePerfil.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /usuarios/yo/foto-de-perfil (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/identidad/verificarDesafioDeStepUp.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /sesiones/desafios/{desafioId}/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/identidad/verificarTitularidad.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /usuarios/{usuarioId}/titularidad/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/correrDevengoDeDpf.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/devengos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/crearOrdenDeInversion.json | 98 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/ordenes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/estimarComisionDeExito.json | 98 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones/{posicionId}/comision (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/exportarReporteFiscalDeInversiones.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/reportes/fiscal/exportacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarComprobantesDeInversion.json | 99 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones/{posicionId}/comprobantes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarCorridasDeInversiones.json | 61 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/corridas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarDistribucionesDeInteres.json | 101 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones/{posicionId}/distribuciones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarPosicionesDeInversion.json | 167 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/listarProductosDeInversion.json | 105 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/productos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/reintentarInstruccionesAlLibro.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/instrucciones/reintento (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/reporteFiscalDeInversiones.json | 274 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/reportes/fiscal (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/simularCajaDeInversion.json | 165 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/simulaciones/caja (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/sincronizarCatalogoDeInversion.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/productos/sincronizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/sincronizarOrdenDeInversion.json | 98 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/ordenes/{ordenId}/sincronizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/sincronizarRescateDeInversion.json | 106 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/rescates/{rescateId}/sincronizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/sincronizarValoresDeCuota.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/valores-cuota/sincronizacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/solicitarRescateDeInversion.json | 106 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /inversiones/posiciones/{posicionId}/rescates (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/inversiones/verOrdenDeInversion.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/ordenes/{ordenId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/verPosicionDeInversion.json | 221 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/posiciones/{posicionId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/inversiones/verProductoDeInversion.json | 128 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /inversiones/productos/{codigo} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/consultarAvisosInternos.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/avisos-internos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/consultarSupresion.json | 26 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/supresion (ok, vacio, adverso) |
| packages/simulado/ejemplos/notificaciones/emitirAvisoInterno.json | 82 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/avisos-internos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/emitirNotificacion.json | 60 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/listarEnvios.json | 95 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/envios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/listarEventosDeEntregaRecibidos.json | 49 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/webhooks-recibidos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/listarPlantillas.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/plantillas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/listarProveedoresDeMensajeria.json | 49 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /notificaciones/proveedores (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/notificaciones/marcarAvisoLeido.json | 82 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/avisos-internos/{id}/leido (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/marcarTodosAvisosLeidos.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/avisos-internos/marcar-todos-leidos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/procesarRespuestaEntrante.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/webhooks/{proveedor} (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/notificaciones/verificarSupresion.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /notificaciones/supresion/consultas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/acreditarRecarga.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/recargas/{ordenId}/acreditacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/acreditarRescate.json | 66 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/inversiones/acreditaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/aprobarRetiro.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retiros/{ordenId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/bloquearPorAutoridad.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/bloqueos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/cerrarRetencion.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retenciones/{retencionId}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/conciliarCustodia.json | 94 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /custodia/conciliaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/consultarCuentaPropia.json | 41 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/cuentas/propia (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/consultarSaldo.json | 62 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/{cuentaId}/saldo (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/cotizarOperacion.json | 138 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/cotizaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/debitarInversion.json | 82 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/inversiones/debitos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/ejecutarCierreDiario.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /contabilidad/cierres-diarios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/emitirExtracto.json | 65 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/{cuentaId}/extracto (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/emitirQr.json | 78 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/qr (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/leerQr.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/qr/lecturas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/liberarRetencionDelMercado.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retenciones/{retencionId}/liberacion-de-mercado (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarCierresDiarios.json | 67 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /contabilidad/cierres-diarios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarConciliaciones.json | 97 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /custodia/conciliaciones (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarMovimientosBilletera.json | 76 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/{cuentaId}/movimientos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarRetirosEnRevision.json | 85 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /billetera/retiros (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/listarReversos.json | 67 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos GET /contabilidad/reversos (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/pagarAlTercero.json | 82 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retenciones/{retencionId}/pago-a-tercero (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/pagarQr.json | 94 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/qr/pagos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/pagarRecargaDePrueba.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /recargas-de-prueba/{ordenId}/pago (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/retenerSaldo.json | 86 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retenciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/reversarTransaccion.json | 58 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/reversos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/solicitarCierreBilletera.json | 70 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/cierres (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/solicitarRecarga.json | 90 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/recargas (ok, vacio, adverso, aceptado, intermitente, rechazo, qrDePrueba) |
| packages/simulado/ejemplos/nucleo-financiero/solicitarRetiro.json | 90 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/retiros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/nucleo-financiero/transferirSaldo.json | 74 | sí (ok/adverso/rechazo leídos + validación y greps sobre todos los escenarios) | Ejemplos POST /billetera/transferencias (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/aceptarHabilitacionBasica.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/yo/habilitacion-basica (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/activarRegla.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/reglas/{reglaId}/activacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/anotarEjecucion.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/tareas/{tareaId}/ejecuciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/apelarSancion.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/sanciones/{sancionId}/apelacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/aprobarPostulacion.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/postulaciones/{solicitudId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/aprobarTarea.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/tareas/{tareaId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/consultarExpedienteDeHabilitacion.json | 103 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/postulaciones/{solicitudId}/expediente (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/consultarHabilitacion.json | 38 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/{organizadorId}/habilitacion (ok, vacio, adverso) |
| packages/simulado/ejemplos/organizador/consultarHabilitacionBasica.json | 59 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/yo/habilitacion-basica (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/consultarHabilitacionPersonal.json | 39 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/usuarios/{usuarioId}/habilitacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/consultarMiExpedienteDeHabilitacion.json | 103 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/postulaciones/mia (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/definirRegla.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/reglas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/emitirContrato.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/{organizadorId}/contratos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/evaluarDesempeno.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/{organizadorId}/evaluaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/firmarContrato.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/contratos/{contratoId}/firma (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/firmarHabilitacionBasica.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/yo/habilitacion-basica/firma (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/habilitarOrganizador.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/{organizadorId}/habilitacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/listarBandejaDeHabilitaciones.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /organizadores/postulaciones/bandeja (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/organizador/postularOrganizador.json | 77 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/postulaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/programarTarea.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /automatizacion/tareas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/rescindirContrato.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/contratos/{contratoId}/rescision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/resolverApelacion.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/sanciones/{sancionId}/resolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/resolverHabilitacion.json | 90 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/postulaciones/{solicitudId}/resolucion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/revisarHabilitacion.json | 90 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/postulaciones/{solicitudId}/revision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/organizador/sancionarOrganizador.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /organizadores/{organizadorId}/sanciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/aprobarCampana.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/campanas/{campanaId}/aprobacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/consumoDelPeriodo.json | 41 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publicidad/cuentas/{cuentaId}/consumo (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/publicidad/crearCampana.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/campanas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/darDeAltaAnunciante.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/anunciantes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/entregarAnuncio.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/espacios/{espacioId}/entrega (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/liquidarPeriodo.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/cuentas/{cuentaId}/liquidaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/listarAnunciantes.json | 69 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publicidad/anunciantes (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/publicidad/listarCampanas.json | 81 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publicidad/campanas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/publicidad/listarPiezasCreativas.json | 63 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publicidad/piezas-creativas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/publicidad/moderarPiezaCreativa.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/piezas-creativas/{piezaId}/revision (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/postularSocioComercial.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/socios-comerciales (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/programarAnuncio.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/anuncios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/rechazarCampana.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/campanas/{campanaId}/rechazo (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/registrarClic.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/impresiones/{impresionId}/clic (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/registrarConversion.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/conversiones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/subirPiezaCreativa.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/piezas-creativas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/publicidad/verificarSocioComercial.json | 54 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /publicidad/socios-comerciales/{socioId}/verificacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/aceptarCotizacion.json | 50 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/cotizaciones/{cotizacionId}/aceptacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/anotarCobroDeComision.json | 62 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/devengos/{devengoId}/cobros (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/cerrarLiquidacion.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/liquidaciones/{periodo}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/consultarTarifarioVigente.json | 29 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /tarifas/vigentes/{codigo} (ok, vacio, adverso) |
| packages/simulado/ejemplos/tarifas/cotizarComision.json | 137 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/cotizaciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/crearSegmento.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/segmentos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/devengarComision.json | 78 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/devengos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/devolverComision.json | 78 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /comisiones/{devengoId}/devoluciones (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/emitirFactura.json | 66 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /facturas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/listarCostosDeProveedor.json | 45 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /tarifas/costos-de-proveedor (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/tarifas/listarTarifarios.json | 103 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /tarifas/tarifarios (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/tarifas/ponerTarifarioVigente.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/tarifarios/{tarifarioId}/vigencia (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/publicarTarifario.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/tarifarios (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/tarifas/resolverPrecio.json | 58 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /tarifas/precios/resolver (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/cerrarAlertaRiesgo.json | 43 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/riesgo/alertas/{alertaId}/cierre (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/compensarEventoReputacion.json | 63 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/eventos/{eventoId}/compensacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/consultarEtapasDePersonas.json | 51 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reputacion/etapas/de-personas (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/transparencia/consultarPerfilEnGrupo.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reputacion/{usuarioId}/perfil-en-grupo/{grupoId} (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/transparencia/consultarPuntaje.json | 32 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reputacion/{usuarioId}/puntaje (ok, vacio, adverso) |
| packages/simulado/ejemplos/transparencia/emitirCertificado.json | 75 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/certificados (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/evaluarInsignias.json | 70 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/insignias/evaluacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/evaluarRiesgo.json | 114 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/riesgo/evaluacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/listarBloques.json | 40 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publico/grupos/{grupoId}/bloques (ok, vacio, adverso) |
| packages/simulado/ejemplos/transparencia/listarEtapas.json | 48 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /reputacion/etapas (ok, vacio, adverso) |
| packages/simulado/ejemplos/transparencia/moderarResena.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/resenas/{resenaId}/moderacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/publicarResena.json | 73 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/resenas (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/recalcularPuntaje.json | 95 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/puntajes (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/registrarEventoReputacion.json | 63 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/eventos (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/revocarCertificado.json | 43 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/certificados/{codigo}/revocacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/revocarInsignia.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/insignias/{otorgadaId}/revocacion (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/sellarBloque.json | 71 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/bloques (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/tomarSnapshotReputacion.json | 51 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos POST /reputacion/snapshots (ok, vacio, adverso, aceptado, intermitente, rechazo) |
| packages/simulado/ejemplos/transparencia/verificarCadena.json | 48 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publico/grupos/{grupoId}/verificacion (ok, vacio, adverso, rechazo) |
| packages/simulado/ejemplos/transparencia/verificarCertificado.json | 35 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /verificar/{codigo} (ok, vacio, adverso) |
| packages/simulado/ejemplos/transparencia/verificarSorteo.json | 74 | muestreo: validación de esquema + greps programáticos sobre todos los escenarios; lectura manual solo de los citados | Ejemplos GET /publico/sorteos/{sorteoId}/verificacion (ok, vacio, adverso, rechazo) |
| packages/simulado/eslint.config.js | 8 | sí | Config ESLint |
| packages/simulado/package.json | 42 | sí | Manifiesto del mock: scripts contratos→ejemplos→inyectar→servidor Scalar en :4010 |
| packages/simulado/pruebas/catalogo-de-errores.spec.ts | 61 | sí | Rechazos usan códigos del catálogo |
| packages/simulado/pruebas/ejemplos-contra-esquema.spec.ts | 56 | sí | Cada ejemplo valida contra su esquema (requiere generado/) |
| packages/simulado/pruebas/qr-interno-vs-interoperable.spec.ts | 100 | sí | QR solo INTERNO; textos AP-CU12-07..10 |
| packages/simulado/scripts/contratos-a-json.mjs | 49 | sí | Convierte servicios/*/openapi/*.yaml a generado/*.json |
| packages/simulado/scripts/generar-ejemplos.mjs | 123 | sí | Crea ejemplos/<svc>/<op>.json desde el esquema y realinea rechazos al catálogo |
| packages/simulado/scripts/inyectar-ejemplos.mjs | 88 | sí | Fusiona contratos + ejemplos en generado/prism/todos.yaml |
| packages/simulado/scripts/servidor-mock.mjs | 14 | sí | Levanta @scalar/mock-server en 0.0.0.0:4010 |
| packages/simulado/src/backoffice-sistemas/adaptador.spec.ts | 31 | sí | Prueba que el adaptador resuelve |
| packages/simulado/src/backoffice-sistemas/adaptador.ts | 43 | sí | Adaptador simulado (retardo 30 ms) de 9 puertos |
| packages/simulado/src/backoffice-sistemas/datos.ts | 80 | sí | Arrays sintéticos (servicios, despliegues, accesos, etc.) |
| packages/simulado/src/backoffice-sistemas/index.ts | 35 | sí | Barril de datos sintéticos del backoffice/sistemas |
| packages/simulado/src/backoffice-sistemas/tipos.ts | 57 | sí | Tipos sintéticos de los 9 puertos |
| packages/simulado/src/catalogo.ts | 135 | sí | Construye catálogo de códigos AP-CUnn-nn desde YAML y docs |
| packages/simulado/src/ejemplos.ts | 47 | sí | ejemploDe(): lee el JSON de un escenario (node:fs) |
| packages/simulado/src/escenarios.ts | 19 | sí | Lista de escenarios y cabecera Prefer |
| packages/simulado/src/index.ts | 5 | sí | Barril del paquete |
| packages/simulado/src/muestra.ts | 162 | sí | Genera valor de muestra determinista desde un esquema |
| packages/simulado/src/randexp.d.ts | 10 | sí | Tipos mínimos de randexp |
| packages/simulado/src/referencias.ts | 38 | sí | Resuelve $ref locales |
| packages/simulado/src/tipos.ts | 48 | sí | Subconjunto de tipos OpenAPI |
| packages/simulado/tsconfig.json | 6 | sí | Config TS |
| packages/simulado/vitest.config.ts | 4 | sí | Config vitest (pruebas/ y src/**/*.spec.ts) |
| packages/tokens/.gitignore | 2 | sí | Ignora generado/ |
| packages/tokens/dinero/formatear.ts | 93 | sí | formatearMonto por cadenas (sin number) |
| packages/tokens/eslint.config.js | 8 | sí | Config ESLint |
| packages/tokens/package.json | 28 | sí | Manifiesto tokens: build a-css/a-dart/vectores |
| packages/tokens/pruebas/formatear-monto.spec.ts | 27 | sí | Pruebas del formato + propiedad sobre vectores |
| packages/tokens/pruebas/generados-coinciden.spec.ts | 46 | sí | CSS y Dart exponen mismos roles |
| packages/tokens/pruebas/roles-contraste.test.mjs | 127 | sí | Contraste WCAG por rol (node:test, NO lo corre vitest) |
| packages/tokens/pruebas/texto-de-ejemplo-contraste.test.mjs | 30 | sí | Contraste de placeholder (node:test, NO lo corre vitest) |
| packages/tokens/pruebas/tokens-contra-boveda.spec.ts | 143 | sí | tokens.json = estilos.css de la bóveda |
| packages/tokens/scripts/a-css.mjs | 83 | sí | Genera generado/tokens.css |
| packages/tokens/scripts/a-dart.mjs | 145 | sí | Genera generado/tokens.dart |
| packages/tokens/scripts/comun.mjs | 28 | sí | Lectura de tokens y helpers compartidos |
| packages/tokens/scripts/vectores.mjs | 46 | sí | Genera vectores/monto.json con LCG determinista |
| packages/tokens/tokens.json | 367 | sí | Fuente única de tokens de diseño |
| packages/tokens/tsconfig.json | 6 | sí | Config TS |
| packages/tokens/vectores/monto.json | 1 | no: 366 KB; verificado programáticamente (5006 casos, todos string, monedas BOB/USD, 2 decimales) | 5006 vectores de formato (366 KB, generado y versionado) |
| packages/tokens/vitest.config.ts | 2 | sí | Config vitest (solo pruebas/**/*.spec.ts) |

### 12.C — ui, tutoriales, shell backoffice y web
#### 1) Tabla de cobertura

Total de archivos del lote: 422 (packages/ui + packages/tutoriales: 173; shell backoffice: 134; shell web: 115). Leídos completos: 235. El resto, por escaneo dirigido (declarado en cada fila). La descripción sale del primer comentario del archivo cuando existe.

| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| `apps/backoffice/.editorconfig` | 17 | sí | Configuración: .editorconfig |
| `apps/backoffice/.gitignore` | 44 | sí | Configuración: .gitignore |
| `apps/backoffice/.prettierrc` | 12 | sí | Configuración: .prettierrc |
| `apps/backoffice/README.md` | 59 | sí | Doc: Backoffice |
| `apps/backoffice/angular.json` | 145 | parcial: configs build (l.40-100) + grep | Configuración: angular.json |
| `apps/backoffice/docker/Dockerfile.backoffice` | 32 | sí | Configuración: Dockerfile.backoffice |
| `apps/backoffice/docker/nginx.conf` | 31 | sí | Configuración: nginx.conf |
| `apps/backoffice/docker/nginx.desplegado.conf` | 59 | sí | Configuración: nginx.desplegado.conf |
| `apps/backoffice/eslint.config.js` | 36 | sí | Configuración: eslint.config.js |
| `apps/backoffice/package.json` | 47 | sí | Configuración: package.json |
| `apps/backoffice/playwright.config.ts` | 49 | sí | Configuración: playwright.config.ts |
| `apps/backoffice/proxy.desarrollo.json` | 7 | sí | Configuración: proxy.desarrollo.json |
| `apps/backoffice/proxy.e2e.json` | 7 | sí | Configuración: proxy.e2e.json |
| `apps/backoffice/proxy.stack-local.json` | 7 | sí | Configuración: proxy.stack-local.json |
| `apps/backoffice/pruebas/preparar.ts` | 4 | sí | preparar.ts |
| `apps/backoffice/pruebas/vitest-axe.d.ts` | 11 | sí | vitest-axe.d.ts |
| `apps/backoffice/public/favicon.ico` | 1 | no: binario vacío (0 líneas) | favicon.ico |
| `apps/backoffice/src/app/app.config.ts` | 26 | sí | Configuración: app.config.ts |
| `apps/backoffice/src/app/app.routes.ts` | 138 | sí | El enchufe por dominio.** Lo escribe el shell (F6) UNA vez y se congela: cada |
| `apps/backoffice/src/app/app.ts` | 10 | sí | app.ts |
| `apps/backoffice/src/app/enchufe-de-rutas.spec.ts` | 42 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de enchufe-de-rutas |
| `apps/backoffice/src/app/layout/buscador-global.spec.ts` | 79 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de buscador-global |
| `apps/backoffice/src/app/layout/buscador-global.ts` | 144 | sí | El buscador global de la cabecera: un combobox sobre pantallas y tablas (`IndiceDeBusqueda`). |
| `apps/backoffice/src/app/layout/campana-de-notificaciones.a11y.spec.ts` | 58 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de campana-de-notificaciones |
| `apps/backoffice/src/app/layout/campana-de-notificaciones.spec.ts` | 180 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de campana-de-notificaciones |
| `apps/backoffice/src/app/layout/campana-de-notificaciones.ts` | 175 | sí | H5.S0 · Campana de notificaciones y avisos internos para operadores. |
| `apps/backoffice/src/app/layout/dominio/cerrar-sesion.spec.ts` | 70 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cerrar-sesion |
| `apps/backoffice/src/app/layout/dominio/cerrar-sesion.ts` | 25 | sí | CU-04 · cerrar la sesión. Contrato real: `DELETE /sesion` (`cerrarSesion`), que revoca |
| `apps/backoffice/src/app/layout/icono-de-seccion.ts` | 92 | parcial: l.1-25 y 89-92 | Trazos de 24×24, uno por sección del menú. Mismo grosor y remate que `ap-icono`. |
| `apps/backoffice/src/app/layout/menu-de-usuario.ts` | 101 | sí | El menú de quien opera, en la cabecera: iniciales en un círculo, el rol en texto legible, y un |
| `apps/backoffice/src/app/layout/menu-lateral.css` | 123 | no: sólo grep (hex/motion) | Estilos de menu-lateral |
| `apps/backoffice/src/app/layout/menu-lateral.spec.ts` | 110 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de menu-lateral |
| `apps/backoffice/src/app/layout/menu-lateral.ts` | 129 | sí | La navegación del backoffice: grupos **fijos** con rótulo en mayúsculas y, debajo de cada sección, **todas** sus |
| `apps/backoffice/src/app/layout/navegacion-del-shell.a11y.spec.ts` | 32 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de navegacion-del-shell |
| `apps/backoffice/src/app/layout/nota-de-la-vista.spec.ts` | 55 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de nota-de-la-vista |
| `apps/backoffice/src/app/layout/nota-de-la-vista.ts` | 43 | sí | «Sobre esta pantalla»: las notas de la vista detrás de un botón ⓘ en la cabecera fija, en un panel que flota. |
| `apps/backoffice/src/app/layout/pestanas-de-seccion.spec.ts` | 54 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de pestanas-de-seccion |
| `apps/backoffice/src/app/layout/pestanas-de-seccion.ts` | 119 | sí | Las pantallas de la sección actual, como las pestañas de sección del portal interno de Atlas (`section-tabs.tsx`): |
| `apps/backoffice/src/app/layout/shell-financiero.css` | 60 | sí | Estilos de shell-financiero |
| `apps/backoffice/src/app/layout/shell-financiero.spec.ts` | 100 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de shell-financiero |
| `apps/backoffice/src/app/layout/shell-financiero.ts` | 131 | sí | El shell del backoffice con la anatomía del portal interno de Atlas: barra lateral oscura con la |
| `apps/backoffice/src/app/layout/simbolo-de-marca.ts` | 34 | sí | El símbolo de AportaYa (`landing/assets/img/simbolo.svg`): las patas de la apacheta en |
| `apps/backoffice/src/app/layout/sistemas/shell-sistemas.ts` | 25 | sí | El marco del backoffice de sistemas: el banner de datos de ejemplo (es del shell, no de cada |
| `apps/backoffice/src/app/layout/tutoriales/anfitrion-de-tutorial.a11y.spec.ts` | 64 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de anfitrion-de-tutorial |
| `apps/backoffice/src/app/layout/tutoriales/anfitrion-de-tutorial.spec.ts` | 162 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de anfitrion-de-tutorial |
| `apps/backoffice/src/app/layout/url-actual.ts` | 21 | sí | La URL de la vista, como señal, **una sola vez para todo el shell**. Antes cabecera, menú y pestañas se suscribían |
| `apps/backoffice/src/app/nucleo/auth-bootstrap.spec.ts` | 115 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de auth-bootstrap |
| `apps/backoffice/src/app/nucleo/auth-bootstrap.ts` | 54 | sí | Cuánto se espera la respuesta del refresco antes de rendirse con `ERROR`. |
| `apps/backoffice/src/app/nucleo/avisos-internos.spec.ts` | 107 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de avisos-internos |
| `apps/backoffice/src/app/nucleo/avisos-internos.ts` | 107 | sí | H5.S0 · Avisos internos para operadores (la campana del backoffice). |
| `apps/backoffice/src/app/nucleo/borrador.ts` | 56 | sí | Borrador local de formularios largos (ROS, actas) en `IndexedDB`, **nunca** en |
| `apps/backoffice/src/app/nucleo/cadena-de-red.apoyo.ts` | 25 | sí | Montaje compartido de las pruebas de la cadena de red real (`INTERCEPTORES_DE_RED`). |
| `apps/backoffice/src/app/nucleo/cadena-de-red.spec.ts` | 134 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cadena-de-red |
| `apps/backoffice/src/app/nucleo/cadena-de-red.timeout.spec.ts` | 57 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cadena-de-red.timeout |
| `apps/backoffice/src/app/nucleo/configuracion-invalida.a11y.spec.ts` | 24 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de configuracion-invalida |
| `apps/backoffice/src/app/nucleo/configuracion-invalida.ts` | 31 | sí | Configuración: configuracion-invalida.ts |
| `apps/backoffice/src/app/nucleo/entrada-estricta.spec.ts` | 162 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de entrada-estricta |
| `apps/backoffice/src/app/nucleo/errores.interceptor.ts` | 23 | sí | Todo error sale de acá ya traducido: la pantalla nunca ve el mensaje del backend ni |
| `apps/backoffice/src/app/nucleo/errores.spec.ts` | 52 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de errores |
| `apps/backoffice/src/app/nucleo/errores.ts` | 109 | sí | Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. Un `403` responde «no tenés acceso» |
| `apps/backoffice/src/app/nucleo/evidencia/panel-de-evidencia.ts` | 55 | sí | Bitácora, movimientos y trazas de un caso — reclamos, disputas y descargos la |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-cumplimiento.ts` | 106 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Explicaciones para el módulo Cumplimiento y Control Normativo de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-finanzas-publicidad.ts` | 111 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Explicaciones para los módulos de Finanzas (Contabilidad ERP) y Publicidad de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-operacion.ts` | 95 | parcial: l.25-95 + grep | Explicaciones para el módulo Operación y Admisiones de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-plataforma.ts` | 57 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Explicaciones para los módulos de Configuración de Plataforma y Centro de Ayuda de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-sistemas.ts` | 137 | parcial: grep + l.54-58 | Explicaciones para el módulo Sistemas e Infraestructura de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/explicaciones-tablero.ts` | 58 | parcial: grep + l.20-28 | Explicaciones para el módulo Inicio y Tableros Ejecutivos de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicaciones/index.spec.ts` | 69 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de index |
| `apps/backoffice/src/app/nucleo/explicaciones/index.ts` | 102 | parcial: l.40-102 | Comprueba si `pathname` coincide con un patrón `prefijo`. |
| `apps/backoffice/src/app/nucleo/explicaciones/tipos.ts` | 42 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Tipos de datos para el explicador de vistas del backoffice de AportaYa. |
| `apps/backoffice/src/app/nucleo/explicador-de-vista.component.css` | 175 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Estilos de explicador-de-vista.component |
| `apps/backoffice/src/app/nucleo/explicador-de-vista.component.spec.ts` | 91 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de explicador-de-vista.component |
| `apps/backoffice/src/app/nucleo/explicador-de-vista.component.ts` | 141 | sí | Componente que presenta el explicador de la vista y del módulo en dos planos: |
| `apps/backoffice/src/app/nucleo/explicador-de-vista.ts` | 54 | sí | Servicio central del explicador de vistas de AportaYa. |
| `apps/backoffice/src/app/nucleo/exportador.ts` | 48 | sí | CU-58, y nada más: **nunca** un `CSV.stringify` acá. Este servicio pide al backend que |
| `apps/backoffice/src/app/nucleo/filtros/barra-de-filtros.spec.ts` | 45 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de barra-de-filtros |
| `apps/backoffice/src/app/nucleo/filtros/barra-de-filtros.ts` | 57 | sí | Chips de filtro + búsqueda con **el estado en la `querystring`**: `?filtro=a,b&q=texto`. |
| `apps/backoffice/src/app/nucleo/gateway.spec.ts` | 87 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de gateway |
| `apps/backoffice/src/app/nucleo/gateway.ts` | 71 | sí | Una sola base URL: el gateway. El prefijo de la ruta enruta al servicio. |
| `apps/backoffice/src/app/nucleo/idempotencia.interceptor.ts` | 82 | sí | La clave viaja por el contexto de la petición: la genera el formulario al abrirse |
| `apps/backoffice/src/app/nucleo/indice-de-busqueda.ts` | 68 | sí | Un resultado del buscador global: a dónde lleva y cómo se describe. |
| `apps/backoffice/src/app/nucleo/interceptores.ts` | 26 | sí | La cadena de red del backoffice, en el orden en que se atraviesa HACIA el servidor (la respuesta vuelve en el orden inve… |
| `apps/backoffice/src/app/nucleo/matriz-de-roles.spec.ts` | 185 | parcial: l.1-30, 120-160 + nombres de tests | Pruebas unitarias/estructura de matriz-de-roles |
| `apps/backoffice/src/app/nucleo/menu-y-guardias.spec.ts` | 102 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de menu-y-guardias |
| `apps/backoffice/src/app/nucleo/menu.spec.ts` | 104 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de menu |
| `apps/backoffice/src/app/nucleo/menu.ts` | 197 | sí | Una condición de acceso: alcanza con **uno** de sus permisos (`Sesion.puede`) o con que el rol sea uno de sus `roles`. |
| `apps/backoffice/src/app/nucleo/navegacion.ts` | 81 | sí | Las preguntas que el shell le hace al árbol del menú (`menu.ts`): qué ve esta sesión, a dónde lleva cada entrada, |
| `apps/backoffice/src/app/nucleo/permisos.spec.ts` | 96 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de permisos |
| `apps/backoffice/src/app/nucleo/permisos.ts` | 58 | sí | `canMatch` funcional por permiso: si el rol no lo tiene, la ruta **no monta** (regla 4 |
| `apps/backoffice/src/app/nucleo/precarga-segun-sesion.spec.ts` | 66 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de precarga-segun-sesion |
| `apps/backoffice/src/app/nucleo/precarga-segun-sesion.ts` | 123 | sí | Precarga en reposo de **lo que esta sesión puede abrir** (F2, 2026-10-26). Sin estrategia, cada clic del menú |
| `apps/backoffice/src/app/nucleo/refresco-de-sesion.ts` | 47 | sí | Un solo refresco en vuelo por vez: N suscriptores concurrentes a `refrescar()` comparten |
| `apps/backoffice/src/app/nucleo/registro-de-acceso.interceptor.spec.ts` | 117 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de registro-de-acceso.interceptor |
| `apps/backoffice/src/app/nucleo/registro-de-acceso.interceptor.ts` | 74 | sí | Marca una petición como «vista datos personales» (CU-58, `R-SEG-02`). La pantalla que |
| `apps/backoffice/src/app/nucleo/restaurando-sesion.a11y.spec.ts` | 44 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de restaurando-sesion |
| `apps/backoffice/src/app/nucleo/restaurando-sesion.spec.ts` | 118 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de restaurando-sesion |
| `apps/backoffice/src/app/nucleo/restaurando-sesion.ts` | 83 | sí | Ruta interna verdadera: nunca una URL externa que el `volverA` pudiera traer. |
| `apps/backoffice/src/app/nucleo/secciones.spec.ts` | 60 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de secciones |
| `apps/backoffice/src/app/nucleo/secciones.ts` | 101 | sí | Qué permiso real abre cada sección del backoffice. |
| `apps/backoffice/src/app/nucleo/senal-de-red.ts` | 23 | sí | ¿El navegador dice que no hay red? Se actualiza con los eventos `online`/`offline` y se desuscribe |
| `apps/backoffice/src/app/nucleo/sesion.interceptor.spec.ts` | 180 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de sesion.interceptor |
| `apps/backoffice/src/app/nucleo/sesion.interceptor.ts` | 45 | sí | Marca la petición de refresco y el reintento, para no entrar en bucle. |
| `apps/backoffice/src/app/nucleo/sesion.spec.ts` | 67 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de sesion |
| `apps/backoffice/src/app/nucleo/sesion.ts` | 120 | sí | `UNKNOWN` es el único estado de arranque: todavía no se intentó el refresco. `RESTORING` |
| `apps/backoffice/src/app/nucleo/tabla/cursor.spec.ts` | 82 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cursor |
| `apps/backoffice/src/app/nucleo/tabla/cursor.ts` | 56 | sí | Bandejas paginadas por cursor (carril A, `convenciones-contrato.md`).** Todas las listas del backoffice responden |
| `apps/backoffice/src/app/nucleo/tabla/navegacion-por-teclado.spec.ts` | 29 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de navegacion-por-teclado |
| `apps/backoffice/src/app/nucleo/tabla/navegacion-por-teclado.ts` | 41 | sí | La parte pura de la navegación fila a fila: qué índice corresponde según la tecla. |
| `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.css` | 29 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Estilos de tabla-de-datos-virtualizada |
| `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.spec.ts` | 115 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de tabla-de-datos-virtualizada |
| `apps/backoffice/src/app/nucleo/tabla/tabla-de-datos-virtualizada.ts` | 197 | sí | El organismo que van a usar las 64 pantallas de `F7`/`F8`: columnas configurables, |
| `apps/backoffice/src/app/nucleo/tabla/tipos.ts` | 18 | sí | Tipos compartidos de la tabla de datos virtualizada. Los usan las 64 pantallas de F7 y F8. |
| `apps/backoffice/src/app/nucleo/timeout.interceptor.ts` | 26 | sí | H12.S1.M3 · ninguna llamada espera para siempre. Una lectura que no responde deja la pantalla en «cargando» sin salida; |
| `apps/backoffice/src/app/nucleo/traza.interceptor.ts` | 11 | sí | `x-request-id` en cada petición: sin esto «me falló el reverso» no se puede seguir. |
| `apps/backoffice/src/app/nucleo/tutoriales/almacen-local.spec.ts` | 82 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de almacen-local |
| `apps/backoffice/src/app/nucleo/tutoriales/almacen-remoto.ts` | 49 | sí | El progreso en el servidor**, contra el contrato descrito en |
| `apps/backoffice/src/app/nucleo/tutoriales/avance.spec.ts` | 83 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de avance |
| `apps/backoffice/src/app/nucleo/tutoriales/motor-salida.spec.ts` | 98 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de motor-salida |
| `apps/backoffice/src/app/nucleo/tutoriales/motor.spec.ts` | 131 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de motor |
| `apps/backoffice/src/app/nucleo/tutoriales/objetivo.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de objetivo |
| `apps/backoffice/src/app/nucleo/tutoriales/preparar-motor.ts` | 84 | sí | El andamiaje que comparten las pruebas del motor y del anfitrión: dos pantallas de |
| `apps/backoffice/src/app/nucleo/tutoriales/registro.spec.ts` | 91 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de registro |
| `apps/backoffice/src/app/nucleo/tutoriales/validacion.spec.ts` | 85 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de validacion |
| `apps/backoffice/src/app/nucleo/tutoriales/vigilar-accion.spec.ts` | 117 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de vigilar-accion |
| `apps/backoffice/src/index.desarrollo.html` | 19 | sí | index.desarrollo.html |
| `apps/backoffice/src/index.html` | 17 | sí | index.html |
| `apps/backoffice/src/main.ts` | 5 | sí | main.ts |
| `apps/backoffice/src/styles.css` | 14 | sí | Estilos de styles |
| `apps/backoffice/src/tipos/clientes-angular.doble.d.ts` | 77 | sí | DOBLE TEMPORAL (regla 65) — no es el contrato real. |
| `apps/backoffice/tsconfig.app.json` | 10 | sí | Configuración: tsconfig.app.json |
| `apps/backoffice/tsconfig.json` | 51 | sí | Configuración: tsconfig.json |
| `apps/backoffice/tsconfig.spec.json` | 14 | sí | Configuración: tsconfig.spec.json |
| `apps/backoffice/tsconfig.spec.richard.json` | 20 | sí | Configuración: tsconfig.spec.richard.json |
| `apps/backoffice/vitest-base.config.ts` | 9 | sí | Configuración: vitest-base.config.ts |
| `packages/tutoriales/eslint.config.js` | 36 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: eslint.config.js |
| `packages/tutoriales/package.json` | 38 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: package.json |
| `packages/tutoriales/src/almacen-local.ts` | 104 | sí | Una clave por operador: dos personas en la misma máquina no comparten avance. |
| `packages/tutoriales/src/almacen.ts` | 23 | sí | El puerto del progreso.** El motor no sabe si esto termina en `localStorage` o en |
| `packages/tutoriales/src/anfitrion-de-tutorial.ts` | 153 | sí | El anfitrión**: lo único que el shell monta para que los tutoriales existan. |
| `packages/tutoriales/src/avance.ts` | 102 | sí | Las cuentas del avance, sin estado y sin IO.** Todo lo que el centro de ayuda |
| `packages/tutoriales/src/bitacora.ts` | 38 | sí | Lo que vale la pena saber de un recorrido, para mejorarlo después. |
| `packages/tutoriales/src/centro/filtros-de-ayuda.ts` | 46 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Buscador y filtros del centro de ayuda. No filtra: emite qué se eligió. |
| `packages/tutoriales/src/centro/pantalla-centro-de-ayuda.ts` | 163 | sí | El centro de tutoriales.** Lista lo que esta persona puede aprender, con su avance, |
| `packages/tutoriales/src/centro/resumen-de-avance.ts` | 29 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Cuánto llevás hecho, arriba de todo. Dice el número además de pintar la barra: una |
| `packages/tutoriales/src/centro/tarjeta-de-tutorial.ts` | 83 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un tutorial en la lista: qué enseña, cuánto lleva y qué botón corresponde. |
| `packages/tutoriales/src/centro/textos.ts` | 61 | sí | Cómo se presenta el centro en cada producto.** Es lo único del centro que cambia |
| `packages/tutoriales/src/centro/vista-de-tutoriales.ts` | 94 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Los filtros del centro de ayuda, tal como viven en la dirección. |
| `packages/tutoriales/src/encendido.ts` | 16 | sí | Lo único que el shell necesita saber de los tutoriales: si hay uno corriendo. |
| `packages/tutoriales/src/identidad.ts` | 34 | sí | Quién está aprendiendo.** Es lo único que el motor necesita saber de la sesión, y |
| `packages/tutoriales/src/lanzador-de-tutorial.ts` | 72 | sí | El tour contextual**: un botón en la cabecera que abre el tutorial de la pantalla |
| `packages/tutoriales/src/motor.ts` | 199 | sí | El motor: dónde estoy, cómo sigo y qué hago si el paso no se puede dar. |
| `packages/tutoriales/src/objetivo.ts` | 98 | sí | El atributo con el que la interfaz se deja señalar. Nunca una clase de CSS. |
| `packages/tutoriales/src/progreso.ts` | 50 | sí | La única copia viva del avance.** El motor escribe por acá y el centro de tutoriales |
| `packages/tutoriales/src/registro.ts` | 98 | sí | Trae un pedazo del catálogo. Es una función para que el `import()` sea perezoso. |
| `packages/tutoriales/src/textos-del-recorrido.ts` | 13 | sí | Los textos del recorrido en curso. Los del centro de ayuda viven en `rutas/ayuda/textos.ts`. |
| `packages/tutoriales/src/tipos.ts` | 114 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El contrato del motor de tutoriales.** Solo tipos: ni una importación de Angular, |
| `packages/tutoriales/src/validacion.spec.ts` | 99 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de validacion |
| `packages/tutoriales/src/validacion.ts` | 116 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El catálogo se valida, no se confía.** Un tutorial mal escrito no se descubre |
| `packages/tutoriales/src/vigilar-accion.ts` | 87 | sí | Todo lo que hace falta para mirar sin tocar. |
| `packages/tutoriales/tsconfig.json` | 23 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.json |
| `packages/ui/angular.json` | 56 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: angular.json |
| `packages/ui/eslint.config.js` | 36 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: eslint.config.js |
| `packages/ui/ng-package.json` | 7 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: ng-package.json |
| `packages/ui/package.json` | 50 | sí | Configuración: package.json |
| `packages/ui/pruebas/preparar.ts` | 4 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | preparar.ts |
| `packages/ui/pruebas/vitest-axe.d.ts` | 11 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | vitest-axe.d.ts |
| `packages/ui/src/acciones-de-fila/acciones-de-fila.ts` | 66 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Las acciones de una fila de tabla, siempre **ícono + nombre** (nunca un ícono solo): Ver, Editar y |
| `packages/ui/src/acordeon/acordeon.ts` | 30 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un bloque que se abre y cierra. `details`/`summary` nativos: teclado y lector sin código. |
| `packages/ui/src/alerta/alerta.ts` | 34 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Aviso en línea. Dice qué pasó y qué hacer; el ícono acompaña al color. `error` se anuncia como alerta. |
| `packages/ui/src/area-de-texto/area-de-texto.spec.ts` | 59 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de area-de-texto |
| `packages/ui/src/area-de-texto/area-de-texto.ts` | 57 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Área de texto con la misma anatomía que `ap-campo`: etiqueta (con tooltip opcional), ayuda o |
| `packages/ui/src/avatar/avatar.spec.ts` | 8 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de avatar |
| `packages/ui/src/avatar/avatar.ts` | 31 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Iniciales sobre fondo de marca. Una imagen opcional, siempre con el nombre como alternativa. |
| `packages/ui/src/banda-de-proposito/banda-de-proposito.ts` | 20 | sí | «Para qué sirve»: toda pantalla del backoffice lleva arriba una frase de negocio. |
| `packages/ui/src/barra-de-filtros/barra-de-filtros.spec.ts` | 74 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de barra-de-filtros |
| `packages/ui/src/barra-de-filtros/barra-de-filtros.ts` | 80 | sí | Un selector de la barra: su nombre (la clave en la URL), su etiqueta, sus opciones y el valor vigente. |
| `packages/ui/src/barra-de-pasos/barra-de-pasos.ts` | 32 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | «Paso 3 de 8: Documento». Progreso por segmentos, el actual con relleno de marca y |
| `packages/ui/src/boton-copiar/boton-copiar.ts` | 59 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Copia un texto al portapapeles y lo dice («Copiado») en una región viva, además de cambiar el |
| `packages/ui/src/boton/boton.ts` | 79 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El único botón. **El color de acción (`--accent`) = acción** y hay uno por pantalla: el |
| `packages/ui/src/cabecera-de-pagina/cabecera-de-pagina.spec.ts` | 58 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cabecera-de-pagina |
| `packages/ui/src/cabecera-de-pagina/cabecera-de-pagina.ts` | 57 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | La cabecera de toda pantalla del backoffice: título, descripción y un carril de |
| `packages/ui/src/cabecera-de-seccion/cabecera-de-seccion.ts` | 36 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El encabezado de un bloque dentro de una pantalla: `h2` con ícono en el acento y una línea de |
| `packages/ui/src/cajon-lateral/cajon-lateral.spec.ts` | 69 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cajon-lateral |
| `packages/ui/src/cajon-lateral/cajon-lateral.ts` | 92 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un panel que entra desde la derecha para una acción sobre una fila («Revisar columna X»), sin |
| `packages/ui/src/campo-busqueda/campo-busqueda.ts` | 35 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Búsqueda con lupa y borrado. `type=search` para que el teclado móvil ofrezca «Buscar». |
| `packages/ui/src/campo-contrasena/campo-contrasena.ts` | 29 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Contraseña con «ver». El botón dice lo que hará, y el campo nunca se autocompleta con la actual. |
| `packages/ui/src/campo-monto/campo-monto.spec.ts` | 15 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de campo-monto |
| `packages/ui/src/campo-monto/campo-monto.ts` | 30 | sí | Monto con prefijo «Bs», teclado decimal y coma como separador. Guarda lo que la |
| `packages/ui/src/campo-otp/campo-otp.ts` | 45 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Código de un solo uso en celdas. Un único `input` real (`one-time-code`, numérico): |
| `packages/ui/src/campo/campo.ts` | 82 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Campo de texto con etiqueta, ayuda y error. El error **sustituye** a la ayuda y se |
| `packages/ui/src/casilla/casilla.ts` | 29 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Casilla nativa vestida: el `input` real queda, así el teclado y el lector la ven. |
| `packages/ui/src/catalogo/catalogo-atomos.ts` | 86 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Los átomos, cada uno con sus variantes y estados. Es datos: no tiene lógica propia. |
| `packages/ui/src/catalogo/catalogo-backoffice.ts` | 106 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | La anatomía de una pantalla del backoffice (la del portal interno de Atlas, con las piezas de |
| `packages/ui/src/catalogo/catalogo-moleculas.ts` | 85 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Las moléculas con datos de muestra fijos. |
| `packages/ui/src/catalogo/catalogo-organismos.ts` | 84 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Los organismos con datos de muestra fijos. |
| `packages/ui/src/catalogo/catalogo.a11y.spec.ts` | 39 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de catalogo |
| `packages/ui/src/catalogo/catalogo.spec.ts` | 46 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de catalogo |
| `packages/ui/src/catalogo/catalogo.ts` | 32 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El catálogo vivo de `@aportaya/ui`. Lo monta `/catalogo` en el sitio (sin indexar) y |
| `packages/ui/src/catalogo/muestras.ts` | 66 | sí | Datos fijos del catálogo. «Hoy» es fijo para que las capturas no cambien con el calendario. |
| `packages/ui/src/catalogo/seccion.ts` | 19 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un bloque del catálogo: nombre de la pieza y una grilla con sus variantes. |
| `packages/ui/src/chip-elegible/chip-elegible.ts` | 63 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un chip que se elige. **Elegido = relleno de marca**, no un cambio sutil de fondo |
| `packages/ui/src/chip-estado/chip-estado.ts` | 33 | sí | Un estado con nombre. Un color de estado en una superficie chica lleva **relleno y |
| `packages/ui/src/chips-de-filtro/chips-de-filtro.spec.ts` | 22 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de chips-de-filtro |
| `packages/ui/src/chips-de-filtro/chips-de-filtro.ts` | 31 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Filtros de varias líneas, con ícono por chip y **sin «otros»**: cada tipo tiene su chip |
| `packages/ui/src/codigo-copiable/codigo-copiable.ts` | 19 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Un identificador o código en monoespaciado con su botón de copiar al lado (esquemas, tablas, trazas, claves). |
| `packages/ui/src/codigo-qr/codigo-qr.ts` | 37 | sí | QR como SVG en línea (se ve en SSR y se imprime nítido). El contenido no se lee en |
| `packages/ui/src/cuenta-enmascarada/cuenta-enmascarada.spec.ts` | 8 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de cuenta-enmascarada |
| `packages/ui/src/cuenta-enmascarada/cuenta-enmascarada.ts` | 21 | sí | Cuenta bancaria mostrando solo los últimos cuatro. El lector oye «terminada en 1234». |
| `packages/ui/src/desglose-de-cobro/desglose-de-cobro.ts` | 41 | sí | Las líneas de un cobro y el total. **El total viene del servidor**: acá no se suma |
| `packages/ui/src/dialogo-de-confirmacion/dialogo-de-confirmacion.spec.ts` | 61 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de dialogo-de-confirmacion |
| `packages/ui/src/dialogo-de-confirmacion/dialogo-de-confirmacion.ts` | 57 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | La confirmación antes de una escritura, como en el portal de Atlas: ícono de advertencia, |
| `packages/ui/src/dialogo/dialogo.foco.spec.ts` | 45 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de dialogo.foco |
| `packages/ui/src/dialogo/dialogo.frontend.spec.ts` | 174 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de dialogo.frontend |
| `packages/ui/src/dialogo/dialogo.spec.ts` | 184 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de dialogo |
| `packages/ui/src/dialogo/dialogo.ts` | 138 | parcial: grep foco/Escape/confirm | Confirmación modal sobre `<dialog>` nativo: foco atrapado, Escape cierra, fondo |
| `packages/ui/src/dialogo/politica-de-descarte.ts` | 14 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Política de descarte única del organismo `Dialogo` (H3.S2, `entregables/contrato-dialogo.md`). |
| `packages/ui/src/error-de-envio/error-de-envio.spec.ts` | 42 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de error-de-envio |
| `packages/ui/src/error-de-envio/error-de-envio.ts` | 45 | sí | Lo que la capa de red entrega ya traducido (mismo contorno que `ErrorTraducido` de las apps). |
| `packages/ui/src/escalera-de-etapas/escalera-de-etapas.ts` | 44 | sí | Las etapas de cobranza en orden, con la actual marcada y las pasadas con su fecha. |
| `packages/ui/src/esqueleto/esqueleto.ts` | 38 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Silueta de carga. `lineas`: tantas líneas como `lineas`, con anchos que no se repiten (texto). |
| `packages/ui/src/estado-de-error/estado-de-error.spec.ts` | 74 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-de-error |
| `packages/ui/src/estado-de-error/estado-de-error.ts` | 49 | sí | Un error accionable, como en el portal de Atlas: ícono en loseta, título, qué pasó, el código |
| `packages/ui/src/estado-de-pantalla/estado-de-pantalla.exhaustividad.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-de-pantalla.exhaustividad |
| `packages/ui/src/estado-de-pantalla/estado-de-pantalla.frontend.spec.ts` | 201 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-de-pantalla.frontend |
| `packages/ui/src/estado-de-pantalla/estado-de-pantalla.spec.ts` | 153 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-de-pantalla |
| `packages/ui/src/estado-de-pantalla/estado-de-pantalla.ts` | 126 | sí | Lo que el interceptor de errores adjunta: el mensaje ya traducido y la traza. |
| `packages/ui/src/estado-vacio/estado-vacio.spec.ts` | 42 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de estado-vacio |
| `packages/ui/src/estado-vacio/estado-vacio.ts` | 56 | sí | Vacío con **por qué** y **qué hacer**. Vacío por filtro ofrece quitar el filtro; vacío |
| `packages/ui/src/estilos.css` | 75 | sí | Estilos de estilos |
| `packages/ui/src/estrellas/estrellas.ts` | 33 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | De una a cinco. Solo lectura muestra; con `editable` es un `radiogroup` con teclado. |
| `packages/ui/src/exportar-listado/exportar-listado.ts` | 49 | sí | «Descargar CSV» y «Descargar JSON» de las filas **filtradas** de un listado (todas las páginas, no solo la |
| `packages/ui/src/exportar-listado/exportar.spec.ts` | 60 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de exportar |
| `packages/ui/src/exportar-listado/exportar.ts` | 79 | sí | Exportación en el navegador de **lo que el listado muestra ya filtrado**: mismas filas, mismas columnas y los |
| `packages/ui/src/fecha/fecha.spec.ts` | 25 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de fecha |
| `packages/ui/src/fecha/fecha.ts` | 40 | sí | Fecha del contrato (ISO) en hora de Bolivia, **con la zona visible**: un plazo se |
| `packages/ui/src/fila-de-cotejo/fila-de-cotejo.spec.ts` | 11 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de fila-de-cotejo |
| `packages/ui/src/fila-de-cotejo/fila-de-cotejo.ts` | 33 | sí | Lo que dijo la persona contra lo que dice el documento. Coincide o no, con palabra y color. |
| `packages/ui/src/fila-de-movimiento/fila-de-movimiento.ts` | 43 | sí | Una línea del extracto: ícono tipado, concepto, hora, monto con signo y saldo corrido. |
| `packages/ui/src/foco-de-tutorial/foco-de-tutorial.ts` | 61 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El recuadro del elemento señalado, en coordenadas de la ventana. |
| `packages/ui/src/girador/girador.ts` | 19 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Actividad indeterminada. Siempre se anuncia (`role=status`); respeta *reduced motion*. |
| `packages/ui/src/globo-de-tutorial/globo-de-tutorial.ts` | 129 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Lo que se supone que mide el globo **antes de haberlo pintado una vez**. Se usa para |
| `packages/ui/src/globo-de-tutorial/ubicacion.spec.ts` | 66 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de ubicacion |
| `packages/ui/src/globo-de-tutorial/ubicacion.ts` | 92 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Aire entre el elemento y el globo, y entre el globo y el borde de la ventana. |
| `packages/ui/src/grupo-radio/grupo-radio.ts` | 39 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Una entre varias, con `fieldset`/`legend`. El elegido lleva relleno de marca en el aro. |
| `packages/ui/src/icono/icono.ts` | 132 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Trazos de 24×24 (stroke), uno por nombre. El ícono dice **qué pasó**, nunca si sube o baja. |
| `packages/ui/src/interruptor/interruptor.ts` | 27 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Sí/no inmediato (`role=switch`). Activo en verde **ok**: es un estado, no una acción. |
| `packages/ui/src/item-pasanaku/item-pasanaku.ts` | 44 | sí | Un grupo en una lista: nombre, aporte, avance del ciclo y el estado de mi cuota. |
| `packages/ui/src/lista-de-movimientos/lista-de-movimientos.spec.ts` | 22 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de lista-de-movimientos |
| `packages/ui/src/lista-de-movimientos/lista-de-movimientos.ts` | 68 | sí | El extracto **agrupado por día** (regla 2), con el neto del día y el saldo al cierre. |
| `packages/ui/src/lista-de-opciones/lista-de-opciones.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de lista-de-opciones |
| `packages/ui/src/lista-de-opciones/lista-de-opciones.ts` | 64 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Lista desplegable nativa (teclado y lector de pantalla de serie) con la anatomía de `ap-campo`: |
| `packages/ui/src/lista-de-requisitos/lista-de-requisitos.ts` | 44 | sí | Qué falta para habilitarse, con el avance arriba y una acción por requisito pendiente. |
| `packages/ui/src/listado/crear-listado.spec.ts` | 125 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de crear-listado |
| `packages/ui/src/listado/crear-listado.ts` | 148 | parcial: grep + l.40-48 | Un criterio de filtro del listado. Sin `opciones`, se arman con los valores distintos que traen las filas |
| `packages/ui/src/listado/listado.spec.ts` | 117 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de listado |
| `packages/ui/src/listado/listado.ts` | 142 | parcial: l.1-140 + grep | La columna que agrega `#acciones`: no choca con un campo real porque ningún contrato usa este nombre. |
| `packages/ui/src/listado/singular.spec.ts` | 27 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de singular |
| `packages/ui/src/listado/singular.ts` | 24 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | El singular del nombre de registro de un listado, para no escribir «1 anunciantes» (2026-10-27). La mayoría solo |
| `packages/ui/src/medidor-de-rango/medidor-de-rango.ts` | 45 | sí | Dónde cae un valor dentro de un rango con zonas (bajo/medio/alto). Un `meter` nativo |
| `packages/ui/src/migas/migas.spec.ts` | 44 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de migas |
| `packages/ui/src/migas/migas.ts` | 55 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Migas de pan: «Inicio › Sección › Vista». Las migas con `ruta` son enlaces; la última es la |
| `packages/ui/src/monto/formatear.ts` | 2 | sí | Reexporta la referencia: el formateo vive UNA vez, en @aportaya/tokens/dinero. |
| `packages/ui/src/monto/monto.spec.ts` | 34 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de monto |
| `packages/ui/src/monto/monto.ts` | 25 | sí | El único lugar de la web donde se dibuja dinero: `tabular-nums`, prefijo `Bs`, coma |
| `packages/ui/src/nota-de-vista/nota-de-vista.spec.ts` | 63 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de nota-de-vista |
| `packages/ui/src/nota-de-vista/nota-de-vista.ts` | 102 | sí | «Notas de la vista»: qué muestra esta pantalla y cómo se usa, en una línea plegada que solo |
| `packages/ui/src/paginacion/paginacion.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de paginacion |
| `packages/ui/src/paginacion/paginacion.ts` | 78 | sí | «Página 2 de 7 · 134 registros», filas por página, ir a una página y Anterior/Siguiente, como |
| `packages/ui/src/panel-de-factores/panel-de-factores.ts` | 41 | sí | Por qué el veredicto fue el que fue, en palabras. Cada factor dice si sumó, restó o |
| `packages/ui/src/pestanas/pestanas.ts` | 50 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Pestañas WAI-ARIA con flechas, Inicio y Fin. El panel lo dibuja quien las usa, con `aria-labelledby`. |
| `packages/ui/src/posiciones-de-saldo/posiciones-de-saldo.a11y.spec.ts` | 27 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de posiciones-de-saldo |
| `packages/ui/src/posiciones-de-saldo/posiciones-de-saldo.spec.ts` | 85 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de posiciones-de-saldo |
| `packages/ui/src/posiciones-de-saldo/posiciones-de-saldo.ts` | 112 | sí | Las posiciones de una billetera ya separadas (las calcula `separarPosiciones` de |
| `packages/ui/src/progreso/progreso.ts` | 32 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Avance determinado de 0 a 1. Se lee como barra de progreso con su valor en porcentaje. |
| `packages/ui/src/public-api.ts` | 4 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Sin barril a propósito (conflicto nº 2 de planes/16): cada componente se importa por su ruta, |
| `packages/ui/src/punto/punto.ts` | 17 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Marca de «hay algo nuevo». Sin texto no dice nada al lector: por eso `etiqueta` es obligatoria. |
| `packages/ui/src/reloj-de-plazo/reloj-de-plazo.spec.ts` | 14 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de reloj-de-plazo |
| `packages/ui/src/reloj-de-plazo/reloj-de-plazo.ts` | 57 | sí | Un plazo con fecha, restante y **de qué norma sale**. El vencimiento viene guardado del |
| `packages/ui/src/resumen-de-periodo/resumen-de-periodo.ts` | 34 | sí | Entradas, salidas y neto de un período. Tres cifras, todas por `ap-monto`. |
| `packages/ui/src/riel-de-turnos/riel-de-turnos.ts` | 44 | sí | El orden del grupo en una tira: entregados, el actual con relleno de marca, y «vos» marcado. |
| `packages/ui/src/seccion-clave-valor/seccion-clave-valor.spec.ts` | 62 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de seccion-clave-valor |
| `packages/ui/src/seccion-clave-valor/seccion-clave-valor.ts` | 95 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Una fila: rótulo y valor. `mono` lo muestra como código copiable; `tono` colorea el valor según lo que significa; |
| `packages/ui/src/seccion-de-expediente/seccion-de-expediente.ts` | 42 | sí | Un bloque del expediente: título, quién y cuándo, y una lista de datos con nombre. |
| `packages/ui/src/selector-segmentado/selector-segmentado.spec.ts` | 25 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de selector-segmentado |
| `packages/ui/src/selector-segmentado/selector-segmentado.ts` | 39 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Dos a cuatro vistas equivalentes (lista/calendario, QR/código). Es un `radiogroup` |
| `packages/ui/src/tabla-de-datos/tabla-de-datos-tipos.ts` | 30 | sí | `fijaDerecha`: la columna (de acciones) queda pegada al borde derecho al desplazar; `mono`: identificadores y códigos. |
| `packages/ui/src/tabla-de-datos/tabla-de-datos.css` | 65 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Estilos de tabla-de-datos |
| `packages/ui/src/tabla-de-datos/tabla-de-datos.spec.ts` | 242 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de tabla-de-datos |
| `packages/ui/src/tabla-de-datos/tabla-de-datos.ts` | 182 | parcial: l.1-40 y 100-182 | El organismo canónico de tabla de datos (único; ver `tabla-de-datos-virtualizada` en |
| `packages/ui/src/tarjeta-de-oferta/tarjeta-de-oferta.ts` | 45 | sí | Alguien ofrece cambiar de turno. Se ve quién, qué turno da y cuál pide, y **cuánto |
| `packages/ui/src/tarjeta-de-solicitud/tarjeta-de-solicitud.spec.ts` | 28 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de tarjeta-de-solicitud |
| `packages/ui/src/tarjeta-de-solicitud/tarjeta-de-solicitud.ts` | 57 | sí | Una persona pide entrar a un grupo. Aceptar es un toque; **rechazar exige motivo**: |
| `packages/ui/src/tarjeta-kpi/tarjeta-kpi.ts` | 53 | sí | Un número con nombre, como la tarjeta de métrica del portal de Atlas: ícono en loseta con |
| `packages/ui/src/tarjeta-saldo/tarjeta-saldo.ts` | 39 | sí | Encabezado verde con el saldo. «Recargar» es el único naranja; «Retirar» se pinta |
| `packages/ui/src/tarjeta/tarjeta.ts` | 22 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Superficie con borde. Con `titulo` es una `section` con encabezado; sin él, un `div`. |
| `packages/ui/src/tipo-de-movimiento/tipo-de-movimiento.ts` | 22 | sí | Los tipos que el extracto distingue. **El ícono dice qué pasó** (regla 1), el signo lo dice el monto. |
| `packages/ui/src/toast/toast.ts` | 49 | sí | Cola de avisos efímeros: uno a la vez, el siguiente espera. Un error no se va solo. |
| `packages/ui/src/tono/tono.ts` | 31 | sí | Los tonos semánticos que un componente puede pedir. Cada uno resuelve a un par |
| `packages/ui/src/tooltip/tooltip.spec.ts` | 47 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de tooltip |
| `packages/ui/src/tooltip/tooltip.ts` | 55 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Una ayuda corta que aparece al pasar el mouse o al enfocar lo que envuelve, y se va con |
| `packages/ui/src/vale/vale.ts` | 58 | sí | El vale de un partner: muesca de ticket, QR **rotativo** (el contenido cambia y la |
| `packages/ui/src/verificador-de-sorteo/verificador-de-sorteo.ts` | 53 | sí | El sorteo se puede reproducir: semilla comprometida, semilla revelada, orden |
| `packages/ui/tsconfig.json` | 44 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.json |
| `packages/ui/tsconfig.lib.json` | 18 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.lib.json |
| `packages/ui/tsconfig.lib.prod.json` | 9 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.lib.prod.json |
| `packages/ui/tsconfig.spec.json` | 14 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: tsconfig.spec.json |
| `packages/ui/vitest-base.config.ts` | 16 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: vitest-base.config.ts |
| `apps/web/.editorconfig` | 17 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: .editorconfig |
| `apps/web/.gitignore` | 9 | sí | Configuración: .gitignore |
| `apps/web/.prettierrc` | 12 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | Configuración: .prettierrc |
| `apps/web/README.md` | 59 | sí | Doc: Web |
| `apps/web/angular.json` | 114 | sí | Configuración: angular.json |
| `apps/web/contenido/legal/contrato-de-adhesion.md` | 18 | sí | Texto público: Contrato de adhesión |
| `apps/web/contenido/legal/estado-regulatorio.md` | 24 | sí | Texto público: Estado regulatorio |
| `apps/web/contenido/paginas/como-funciona.md` | 24 | sí | Texto público: Cómo funciona AportaYa |
| `apps/web/contenido/paginas/descargar.md` | 22 | sí | Texto público: Usar AportaYa |
| `apps/web/contenido/paginas/inicio.md` | 16 | sí | Texto público: El pasanaku de siempre, sin el cuaderno |
| `apps/web/contenido/paginas/preguntas.md` | 36 | sí | Texto público: Preguntas frecuentes |
| `apps/web/contenido/paginas/privacidad.md` | 26 | sí | Texto público: Privacidad y tratamiento de datos |
| `apps/web/contenido/paginas/reclamos.md` | 26 | sí | Texto público: Reclamos |
| `apps/web/contenido/paginas/seguridad.md` | 28 | sí | Texto público: Seguridad y custodia de tu dinero |
| `apps/web/contenido/paginas/transparencia.md` | 27 | sí | Texto público: Transparencia: cómo se sella y se verifica |
| `apps/web/docker/Dockerfile.web` | 33 | sí | Configuración: Dockerfile.web |
| `apps/web/eslint.config.js` | 36 | sí | Configuración: eslint.config.js |
| `apps/web/lighthouserc.cjs` | 32 | sí | Configuración: lighthouserc.cjs |
| `apps/web/package.json` | 57 | sí | Configuración: package.json |
| `apps/web/playwright.config.ts` | 45 | sí | Configuración: playwright.config.ts |
| `apps/web/playwright.dev.config.ts` | 19 | sí | Configuración: playwright.dev.config.ts |
| `apps/web/pruebas/e2e/accesibilidad.e2e.ts` | 39 | sí | E2E Playwright: accesibilidad |
| `apps/web/pruebas/e2e/catalogo.spec.ts` | 59 | sí | Pruebas unitarias/estructura de catalogo |
| `apps/web/pruebas/e2e/paginas.spec.ts` | 13 | sí | Pruebas unitarias/estructura de paginas |
| `apps/web/pruebas/e2e/web-seo.e2e.ts` | 66 | sí | E2E Playwright: web-seo |
| `apps/web/pruebas/e2e/web-verificacion.e2e.ts` | 72 | sí | E2E Playwright: web-verificacion |
| `apps/web/pruebas/preparar.ts` | 4 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | preparar.ts |
| `apps/web/pruebas/vitest-axe.d.ts` | 11 | parcial: escaneo grep dirigido (APROB/RESPALDO/literales/DOM/escapes) + uso | vitest-axe.d.ts |
| `apps/web/public/fuentes/OFL-Bricolage.txt` | 93 | no: licencia OFL estándar (texto legal de terceros) | OFL-Bricolage.txt |
| `apps/web/public/fuentes/OFL-Instrument.txt` | 93 | no: licencia OFL estándar (texto legal de terceros) | OFL-Instrument.txt |
| `apps/web/public/legal/estado-regulatorio.md` | 21 | sí | Texto público: Estado regulatorio |
| `apps/web/scripts/contenido.mjs` | 65 | sí | /*.md ANTES de `ng build`: produce src/generado/contenido.json (lo que |
| `apps/web/src/app/app.config.server.ts` | 10 | sí | Configuración: app.config.server.ts |
| `apps/web/src/app/app.config.ts` | 24 | sí | Configuración: app.config.ts |
| `apps/web/src/app/app.routes.server.ts` | 40 | sí | Estático por omisión** (ADR-041). Solo las rutas que dependen de un servicio en |
| `apps/web/src/app/app.routes.ts` | 185 | sí | `data.seo` es de `ServicioMeta` (F10, `apps/web/src/app/seo/`). Cada página le agrega |
| `apps/web/src/app/app.ts` | 35 | sí | El sitio: la barra y el pie de la landing alrededor de cada página. |
| `apps/web/src/app/enchufe-de-paginas.spec.ts` | 31 | sí | Pruebas unitarias/estructura de enchufe-de-paginas |
| `apps/web/src/app/geo/guia-de-redaccion.md` | 56 | sí | Doc: Guía de redacción para contenido legible por LLM (F11.4) |
| `apps/web/src/app/geo/llms.d.mts` | 17 | sí | llms.d.mts |
| `apps/web/src/app/geo/llms.mjs` | 85 | sí | /*.md. Plano y sin dependencias |
| `apps/web/src/app/geo/llms.spec.ts` | 53 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de llms |
| `apps/web/src/app/geo/llms.ts` | 2 | sí | Puente tipado sobre llms.mjs, que es lo que corre el script de build (Node, sin TS). |
| `apps/web/src/app/geo/robots.d.mts` | 4 | sí | robots.d.mts |
| `apps/web/src/app/geo/robots.mjs` | 16 | sí | La politica de rastreadores es decision de negocio (ADR-042) y por eso tiene prueba. |
| `apps/web/src/app/geo/robots.spec.ts` | 26 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de robots |
| `apps/web/src/app/geo/robots.ts` | 2 | sí | Puente tipado sobre robots.mjs, que es lo que corre el script de build (Node, sin TS). |
| `apps/web/src/app/layout/cabecera-de-pagina.ts` | 26 | sí | La banda de título de las páginas interiores: la misma voz visual que el hero de la |
| `apps/web/src/app/layout/cabecera.ts` | 42 | sí | La barra del sitio: la de la landing, con rutas reales en vez de anclas de una sola página. |
| `apps/web/src/app/layout/enlace-a-la-app.ts` | 35 | sí | «Abrir AportaYa en el navegador». La dirección la pone el servidor en |
| `apps/web/src/app/layout/icono.ts` | 44 | sí | Las formas se dibujan como elementos SVG de la plantilla, NO con `innerHTML`. El DOM |
| `apps/web/src/app/layout/marca.ts` | 29 | sí | El símbolo y la palabra, como en la landing y en la app. El aguayo naranja va por clase |
| `apps/web/src/app/layout/pie.ts` | 58 | sí | El pie de la landing, con las páginas legales que el sitio sí tiene. |
| `apps/web/src/app/layout/trazos.ts` | 37 | sí | Los trazos de los íconos del sitio, copiados de la landing (24×24, trazo redondeado). |
| `apps/web/src/app/nucleo/configuracion-invalida.a11y.spec.ts` | 24 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de configuracion-invalida |
| `apps/web/src/app/nucleo/configuracion-invalida.ts` | 30 | sí | Configuración: configuracion-invalida.ts |
| `apps/web/src/app/nucleo/errores.interceptor.ts` | 23 | sí | Todo error sale de acá ya traducido: la pantalla nunca ve el mensaje del backend ni |
| `apps/web/src/app/nucleo/errores.ts` | 28 | sí | Traducción de `AP-CU<NN>-<nn>` a lenguaje humano. Un `403` responde «no tenés acceso» |
| `apps/web/src/app/nucleo/gateway.spec.ts` | 73 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de gateway |
| `apps/web/src/app/nucleo/gateway.ts` | 74 | sí | Una sola base URL: el gateway. El prefijo de la ruta enruta al servicio. |
| `apps/web/src/app/nucleo/timeout.interceptor.spec.ts` | 36 | sí | Pruebas unitarias/estructura de timeout.interceptor |
| `apps/web/src/app/nucleo/timeout.interceptor.ts` | 20 | sí | H12.S1.M3 · el sitio solo LEE (verificadores y datos públicos): una lectura que no responde dejaba la pantalla en «carga… |
| `apps/web/src/app/nucleo/traza.interceptor.ts` | 11 | sí | `x-request-id` en cada petición: sin esto «me falló el reverso» no se puede seguir. |
| `apps/web/src/app/paginas/catalogo/catalogo.ts` | 30 | sí | El catálogo vivo de `@aportaya/ui`, prerenderizado y **sin indexar** (robots + meta): |
| `apps/web/src/app/paginas/como-funciona/como-funciona.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/contrato-de-adhesion/contrato-de-adhesion.ts` | 27 | sí | Contenido estático hoy — ver la nota técnica dentro del Markdown sobre por qué |
| `apps/web/src/app/paginas/descargar/descargar.ts` | 25 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/inicio/inicio.ts` | 27 | sí | El inicio del sitio: la estética y el recorrido de la landing, sobre los tokens y las |
| `apps/web/src/app/paginas/inicio/secciones/cierre.ts` | 74 | sí | Lo que hay debajo, las preguntas y el llamado final. |
| `apps/web/src/app/paginas/inicio/secciones/confianza.ts` | 77 | sí | Cuando algo sale mal, y la seguridad: por qué se puede confiar la plata. |
| `apps/web/src/app/paginas/inicio/secciones/hero.ts` | 58 | sí | hero.ts |
| `apps/web/src/app/paginas/inicio/secciones/producto.ts` | 78 | sí | Cómo funciona y la billetera: los dos bloques que explican el producto. |
| `apps/web/src/app/paginas/inicio/secciones/telefono.ts` | 75 | sí | La pantalla de inicio de la billetera, dibujada en HTML: la misma que muestra la app. |
| `apps/web/src/app/paginas/inicio/secciones/transparencia.ts` | 68 | sí | La transparencia verificable: qué se promete y cómo se ve un historial sellado. |
| `apps/web/src/app/paginas/legal-estado-regulatorio/legal-estado-regulatorio.ts` | 23 | sí | Contenido estático hoy — ver la nota técnica dentro del Markdown sobre el hueco de contrato. |
| `apps/web/src/app/paginas/plazos/plazos.ts` | 26 | sí | Ruta en servidor (consulta un servicio en vivo); el verificador se hidrata al verse. |
| `apps/web/src/app/paginas/preguntas/preguntas.a11y.spec.ts` | 18 | parcial: nombres de tests + grep | Prueba axe de accesibilidad de preguntas |
| `apps/web/src/app/paginas/preguntas/preguntas.spec.ts` | 24 | sí | Pruebas unitarias/estructura de preguntas |
| `apps/web/src/app/paginas/preguntas/preguntas.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/privacidad/privacidad.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/reclamos/reclamos.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/seguridad/seguridad.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/tarifas/tarifas.ts` | 41 | sí | CU-30/CU-34 — Tarifario vigente. `RenderMode.Server` (planes/14 F9.1): la página |
| `apps/web/src/app/paginas/transparencia/transparencia.ts` | 23 | sí | Página de contenido: se prerenderiza y no carga ningún cliente de API. |
| `apps/web/src/app/paginas/verificar/verificar.ts` | 42 | sí | CU-75 — `GET /verificar/:codigo`, ruta pública sin sesión. **No indexable**: es |
| `apps/web/src/app/rutas-de-servidor.spec.ts` | 46 | sí | Pruebas unitarias/estructura de rutas-de-servidor |
| `apps/web/src/app/seo/json-ld.spec.ts` | 40 | sí | Pruebas unitarias/estructura de json-ld |
| `apps/web/src/app/seo/json-ld.ts` | 99 | sí | Constructores de JSON-LD (schema.org) para el sitio público. |
| `apps/web/src/app/seo/servicio-meta.spec.ts` | 13 | sí | Pruebas unitarias/estructura de servicio-meta |
| `apps/web/src/app/seo/servicio-meta.ts` | 126 | sí | El mismo patrón que `scripts/contenido.mjs` usa para escribir el espejo: `/` → `/index.md`, |
| `apps/web/src/app/seo/sitemap.d.mts` | 3 | sí | sitemap.d.mts |
| `apps/web/src/app/seo/sitemap.mjs` | 23 | sí | true si la ruta nunca puede entrar al sitemap, sin importar lo que diga `indexable`. |
| `apps/web/src/app/seo/sitemap.spec.ts` | 50 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de sitemap |
| `apps/web/src/app/seo/sitemap.ts` | 2 | sí | Puente tipado sobre sitemap.mjs, que es lo que corre scripts/contenido.mjs (Node, sin TS). |
| `apps/web/src/app/sin-acciones-ajenas.spec.ts` | 40 | sí | Pruebas unitarias/estructura de sin-acciones-ajenas |
| `apps/web/src/app/tutoriales/catalogo/catalogo.spec.ts` | 55 | sí | Pruebas unitarias/estructura de catalogo |
| `apps/web/src/app/tutoriales/catalogo/catalogo.ts` | 17 | sí | El catálogo del sitio público.** Agregar un tutorial es escribir su constante en el |
| `apps/web/src/app/tutoriales/catalogo/centro.ts` | 40 | sí | Cómo se usa el propio centro de tutoriales del sitio. |
| `apps/web/src/app/tutoriales/catalogo/herramientas.ts` | 105 | sí | El cotizador: lo que AportaYa cobra, antes de operar y sin cuenta. |
| `apps/web/src/app/tutoriales/catalogo/primeros-pasos.ts` | 77 | sí | Los tutoriales del **sitio público**: los hace alguien sin cuenta, que todavía está |
| `apps/web/src/app/tutoriales/guia-del-sitio.a11y.spec.ts` | 63 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Prueba axe de accesibilidad de guia-del-sitio |
| `apps/web/src/app/tutoriales/guia-del-sitio.spec.ts` | 99 | parcial: nombres de tests + grep (skip/only/expect/aprob) | Pruebas unitarias/estructura de guia-del-sitio |
| `apps/web/src/app/tutoriales/proveer.ts` | 52 | sí | Las rutas del sitio contra las que se valida que ningún tutorial señale una página |
| `apps/web/src/app/tutoriales/tutoriales.routes.ts` | 13 | sí | La guía interactiva del sitio. Es una página más —estática, indexable— y el recorrido |
| `apps/web/src/index.html` | 15 | sí | index.html |
| `apps/web/src/main.server.ts` | 7 | sí | main.server.ts |
| `apps/web/src/main.ts` | 5 | sí | main.ts |
| `apps/web/src/server.ts` | 144 | sí | Example Express Rest API endpoints can be defined here. |
| `apps/web/src/sitio/fuentes.css` | 13 | sí | Estilos de fuentes |
| `apps/web/src/sitio/sitio.css` | 706 | parcial: cabecera, l.60-70 y 615-706 + grep de hex/rgba/motion/outline | Estilos de sitio |
| `apps/web/src/sitio/tokens-sitio.css` | 68 | sí | Estilos de tokens-sitio |
| `apps/web/src/styles.css` | 5 | sí | Estilos de styles |
| `apps/web/tsconfig.app.json` | 10 | sí | Configuración: tsconfig.app.json |
| `apps/web/tsconfig.json` | 52 | sí | Configuración: tsconfig.json |
| `apps/web/tsconfig.spec.json` | 14 | sí | Configuración: tsconfig.spec.json |

### 12.D — shell de apps/movil
#### 1) Cobertura

#### 1.a `lib/` fuera de `pantallas/` + `pantallas/arranque` + `pantallas/alianzas` (116 archivos, 5 982 líneas, todos leídos de punta a punta)

| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| `apps/movil/lib/app.dart` | 160 | sí | Raíz: hidrata sesión, vigilante de inactividad, tope de 8 h, crea el router |
| `apps/movil/lib/app_real.dart` | 73 | sí | MaterialApp.router con capas (bloqueo, tutorial, apertura, aviso de contrato) y locales |
| `apps/movil/lib/catalogo.dart` | 7 | sí | Entrypoint del catálogo del sistema de diseño |
| `apps/movil/lib/dominio/cierre_de_sesion.dart` | 73 | sí | Revoca token (DELETE /sesion, 2 intentos, 5 s) y borra local |
| `apps/movil/lib/dominio/cliente.dart` | 154 | sí | dioProvider, validación del gateway, interceptores de traza/sesión/refresco |
| `apps/movil/lib/dominio/configuracion.dart` | 115 | sí | validarGateway: esquema, loopback, hosts permitidos, TLS en release, ruta /vN |
| `apps/movil/lib/dominio/contexto_de_error.dart` | 36 | sí | Contexto por endpoint para traducir códigos reutilizados |
| `apps/movil/lib/dominio/cu10_pago_de_prueba.dart` | 53 | sí | «Simular pago» de recarga con QR de PRUEBA (POST /recargas-de-prueba/{id}/pago) |
| `apps/movil/lib/dominio/cu10_recargar_saldo.dart` | 54 | sí | Crea orden de recarga con Idempotency-Key por cuenta |
| `apps/movil/lib/dominio/cu11_retirar_saldo.dart` | 52 | sí | Solicita retiro con factorMfa e Idempotency-Key por cuenta |
| `apps/movil/lib/dominio/cu12_transferir_saldo.dart` | 59 | sí | Transferencia entre billeteras con Idempotency-Key por cuenta |
| `apps/movil/lib/dominio/cu13_consultar_saldo.dart` | 43 | sí | Lee saldo y movimientos (GET crudo con dio, no DefaultApi) |
| `apps/movil/lib/dominio/cu13_cuenta_propia.dart` | 61 | sí | Lee la cuenta propia (tipo escrito a mano, moneda BOB fija) |
| `apps/movil/lib/dominio/cu15_emitir_extracto.dart` | 45 | sí | Extracto por rango de fechas |
| `apps/movil/lib/dominio/diagnostico_api.dart` | 35 | sí | Log de rechazos API solo en debug, ruta anonimizada |
| `apps/movil/lib/dominio/errores.dart` | 200 | sí | Catálogo AP-* → texto, ErrorDeApi/ErrorDeRed |
| `apps/movil/lib/dominio/errores_catalogados.dart` | 35 | sí | Textos AP-CU01-03 por campo y AP-BD-0n |
| `apps/movil/lib/dominio/foto_del_puente.dart` | 26 | sí | GET de foto al puente de cámara de desarrollo |
| `apps/movil/lib/dominio/proteccion_de_pantalla_dinero.dart` | 28 | sí | Mixin que activa/desactiva FLAG_SECURE por pantalla |
| `apps/movil/lib/dominio/puertos/ajustes_del_sistema.dart` | 6 | sí | Puerto/proveedor/adaptador de ajustes del SO |
| `apps/movil/lib/dominio/puertos/almacen_de_progreso.dart` | 21 | sí | Puerto del progreso de tutoriales |
| `apps/movil/lib/dominio/puertos/almacen_seguro.dart` | 7 | sí | Puerto/proveedor/adaptador de almacén seguro |
| `apps/movil/lib/dominio/puertos/avisos_push.dart` | 13 | sí | Puerto/proveedor/adaptador de push |
| `apps/movil/lib/dominio/puertos/biometria.dart` | 19 | sí | Puerto/proveedor/adaptador de biometría |
| `apps/movil/lib/dominio/puertos/camara.dart` | 16 | sí | Puerto de cámara del sistema |
| `apps/movil/lib/dominio/puertos/camara_en_vivo.dart` | 46 | sí | Puerto de cámara en vivo (devuelve Widget) |
| `apps/movil/lib/dominio/puertos/compartir.dart` | 22 | sí | Puerto/proveedor/adaptador para compartir |
| `apps/movil/lib/dominio/puertos/conectividad.dart` | 5 | sí | Puerto de conectividad |
| `apps/movil/lib/dominio/puertos/haptica.dart` | 4 | sí | Puerto/proveedor/adaptador háptico |
| `apps/movil/lib/dominio/puertos/lector_de_qr_en_imagen.dart` | 28 | sí | Puerto/proveedor de lectura de QR en imagen |
| `apps/movil/lib/dominio/puertos/proteccion_pantalla.dart` | 7 | sí | Puerto/proveedor/adaptador de protección de pantalla |
| `apps/movil/lib/dominio/puertos/visor_qr.dart` | 23 | sí | Puerto/proveedor del visor de QR |
| `apps/movil/lib/dominio/reclamos_del_token.dart` | 29 | sí | Decodifica sub y permisos del JWT sin verificar firma |
| `apps/movil/lib/dominio/refrescador.dart` | 36 | sí | Refresco single-flight con Dio sin interceptores |
| `apps/movil/lib/dominio/renovar_acceso.dart` | 18 | sí | Renueva el token con un Refrescador NUEVO por llamada |
| `apps/movil/lib/dominio/tope_de_sesion.dart` | 35 | sí | Cierra sesión al superar el tope absoluto |
| `apps/movil/lib/dominio/traduccion_de_errores.dart` | 68 | sí | Interceptor DioException → ErrorDeApi/ErrorDeRed |
| `apps/movil/lib/dominio/tutoriales/autolanzamiento.dart` | 16 | sí | Regla de autolanzar la guía |
| `apps/movil/lib/dominio/tutoriales/avance.dart` | 107 | sí | Cálculos puros del progreso de tutoriales |
| `apps/movil/lib/dominio/tutoriales/escribano.dart` | 27 | sí | Persistencia del progreso sin bloquear el motor |
| `apps/movil/lib/dominio/tutoriales/estado_del_motor.dart` | 62 | sí | Estado inmutable del motor de tutoriales |
| `apps/movil/lib/dominio/tutoriales/modelo.dart` | 199 | sí | Modelo de tutoriales (Capacidad, pasos, progreso) |
| `apps/movil/lib/dominio/tutoriales/motor.dart` | 147 | sí | Máquina de estados del motor de tutoriales |
| `apps/movil/lib/dominio/tutoriales/motor_acciones.dart` | 118 | sí | Acciones públicas del motor (iniciar, avanzar, omitir…) |
| `apps/movil/lib/dominio/tutoriales/navegacion_del_tutorial.dart` | 15 | sí | llegarA() — sin referencias (clon muerto de motor._llegarA) |
| `apps/movil/lib/dominio/tutoriales/registro.dart` | 44 | sí | Filtro de tutoriales por capacidad y ruta |
| `apps/movil/lib/dominio/tutoriales/serializacion.dart` | 51 | sí | JSON del progreso |
| `apps/movil/lib/dominio/tutoriales/textos_del_motor.dart` | 11 | sí | Textos del motor |
| `apps/movil/lib/dominio/tutoriales/validacion.dart` | 174 | sí | Validadores cliente (teléfono, carnet, monto) / validación del catálogo de tutoriales |
| `apps/movil/lib/dominio/validacion.dart` | 26 | sí | Validadores cliente (teléfono, carnet, monto) / validación del catálogo de tutoriales |
| `apps/movil/lib/dominio/verificacion_contrato.dart` | 32 | sí | GET /version y compara contra versión compilada "1" |
| `apps/movil/lib/dominio/vigilante_de_sesion.dart` | 70 | sí | Regla de bloqueo por segundo plano/inactividad |
| `apps/movil/lib/infraestructura/ajustes_del_sistema_app_settings.dart` | 19 | sí | Adaptador app_settings |
| `apps/movil/lib/infraestructura/almacen_de_progreso_seguro.dart` | 75 | sí | Progreso de tutoriales en el almacén seguro, cola de escritura |
| `apps/movil/lib/infraestructura/android/almacen_seguro_android.dart` | 22 | sí | flutter_secure_storage Android |
| `apps/movil/lib/infraestructura/android/avisos_push_android.dart` | 35 | sí | Canal push Android (devuelve null; toques nunca emite) |
| `apps/movil/lib/infraestructura/android/biometria_android.dart` | 48 | sí | BiometricPrompt por MethodChannel |
| `apps/movil/lib/infraestructura/android/compartir_android.dart` | 46 | sí | Intents de compartir por MethodChannel |
| `apps/movil/lib/infraestructura/android/conectividad_android.dart` | 44 | sí | connectivity_plus + sonda GET /version |
| `apps/movil/lib/infraestructura/android/haptica_android.dart` | 5 | sí | Alias de HapticaFlutter |
| `apps/movil/lib/infraestructura/android/proteccion_pantalla_android.dart` | 30 | sí | FLAG_SECURE por canal |
| `apps/movil/lib/infraestructura/camara_del_sistema.dart` | 64 | sí | image_picker cámara/galería |
| `apps/movil/lib/infraestructura/camara_en_vivo_plugin.dart` | 157 | sí | Plugin camera + lector de rostro |
| `apps/movil/lib/infraestructura/camara_prestada.dart` | 53 | sí | Cámara del Mac vía puente (CAMARA_DEV) |
| `apps/movil/lib/infraestructura/capacidades.dart` | 121 | sí | Mapa de grados de soporte por plataforma / proveedor |
| `apps/movil/lib/infraestructura/escaner_de_documentos.dart` | 70 | sí | Escáner de documentos del sistema |
| `apps/movil/lib/infraestructura/haptica_flutter.dart` | 21 | sí | HapticFeedback semántico |
| `apps/movil/lib/infraestructura/imagen_del_qr.dart` | 18 | sí | PNG del QR del grupo en cache/compartir / proveedor |
| `apps/movil/lib/infraestructura/ios/almacen_seguro_ios.dart` | 28 | sí | Keychain unlocked_this_device |
| `apps/movil/lib/infraestructura/ios/avisos_push_ios.dart` | 24 | sí | Push iOS no soportado |
| `apps/movil/lib/infraestructura/ios/biometria_ios.dart` | 24 | sí | Biometría iOS no soportada (siempre false) |
| `apps/movil/lib/infraestructura/ios/haptica_ios.dart` | 4 | sí | Alias de HapticaFlutter |
| `apps/movil/lib/infraestructura/ios/proteccion_pantalla_ios.dart` | 39 | sí | Protección iOS por canal (degradada) |
| `apps/movil/lib/infraestructura/lector_de_qr_en_galeria.dart` | 40 | sí | Lee QR de imagen de galería |
| `apps/movil/lib/infraestructura/lector_de_rostro_mlkit.dart` | 110 | sí | ML Kit Android |
| `apps/movil/lib/infraestructura/lector_de_rostro_vision.dart` | 57 | sí | Vision iOS por canal |
| `apps/movil/lib/infraestructura/plataforma.dart` | 104 | sí | Resolución de adaptadores por plataforma |
| `apps/movil/lib/infraestructura/visor_qr_mobile_scanner.dart` | 52 | sí | Visor mobile_scanner |
| `apps/movil/lib/localizacion.dart` | 15 | sí | Locales es_BO/es y delegados (solo usado por pruebas) |
| `apps/movil/lib/main.dart` | 26 | sí | Entrypoint: valida gateway y elige AppAportaYa o pantalla de configuración inválida |
| `apps/movil/lib/main_h6_s3_m8_profile.dart` | 149 | sí | Entrypoint "temporal" de medición de cuadros de la prueba de vida |
| `apps/movil/lib/navegacion/abridor_de_avisos.dart` | 51 | sí | Abre rutas desde toques de push con antirrebote |
| `apps/movil/lib/navegacion/acceso_a_rutas.dart` | 94 | sí | Política de rutas (pública/privada/identidad requerida) |
| `apps/movil/lib/navegacion/accion_de_avisos.dart` | 28 | sí | Campana con no leídas |
| `apps/movil/lib/navegacion/accion_de_ayuda.dart` | 17 | sí | Botón de ayuda |
| `apps/movil/lib/navegacion/aviso_de_verificacion.dart` | 92 | sí | Banner de verificación de identidad sobre las pestañas |
| `apps/movil/lib/navegacion/enlaces_profundos.dart` | 40 | sí | aportaya:// y https /g/ → rutas internas |
| `apps/movil/lib/navegacion/guardias.dart` | 28 | sí | Guardias de nivel/habilitación — sin referencias |
| `apps/movil/lib/navegacion/lectura_de_qr.dart` | 58 | sí | Texto de QR → ruta de invitación validada |
| `apps/movil/lib/navegacion/refrescar_al_cambiar_de_pestana.dart` | 31 | sí | Invalida proveedores al cambiar de pestaña |
| `apps/movil/lib/navegacion/retorno_de_invitacion.dart` | 20 | sí | Valida retorno a invitación |
| `apps/movil/lib/navegacion/ruta_del_aviso.dart` | 55 | sí | Valida ruta que trae un push |
| `apps/movil/lib/navegacion/rutas.dart` | 155 | sí | GoRouter con StatefulShellRoute y redirect global / rutas de alianzas (vacío) |
| `apps/movil/lib/navegacion/shell.dart` | 185 | sí | Barra de pestañas + vigilante + aviso de verificación |
| `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart` | 93 | sí | Sondea cada 30 s si aprobaron la identidad y renueva token |
| `apps/movil/lib/proveedores/ajustes_del_sistema.dart` | 9 | sí | Puerto/proveedor/adaptador de ajustes del SO |
| `apps/movil/lib/proveedores/avisos_push.dart` | 9 | sí | Puerto/proveedor/adaptador de push |
| `apps/movil/lib/proveedores/biometria.dart` | 11 | sí | Puerto/proveedor/adaptador de biometría |
| `apps/movil/lib/proveedores/bloqueo.dart` | 60 | sí | ChangeNotifier del candado local |
| `apps/movil/lib/proveedores/capacidades.dart` | 15 | sí | Mapa de grados de soporte por plataforma / proveedor |
| `apps/movil/lib/proveedores/compartir.dart` | 8 | sí | Puerto/proveedor/adaptador para compartir |
| `apps/movil/lib/proveedores/conexion.dart` | 18 | sí | conectividadProvider/hayConexionProvider — sin consumidores |
| `apps/movil/lib/proveedores/dispositivo.dart` | 29 | sí | dispositivoIdProvider — sin consumidores |
| `apps/movil/lib/proveedores/haptica.dart` | 7 | sí | Puerto/proveedor/adaptador háptico |
| `apps/movil/lib/proveedores/huella_de_dispositivo.dart` | 44 | sí | Huella de instalación para CU-04 |
| `apps/movil/lib/proveedores/idempotencia.dart` | 41 | sí | Mapa formularioId → UUID v4 (Random.secure) |
| `apps/movil/lib/proveedores/imagen_del_qr.dart` | 12 | sí | PNG del QR del grupo en cache/compartir / proveedor |
| `apps/movil/lib/proveedores/lector_de_qr_en_imagen.dart` | 9 | sí | Puerto/proveedor de lectura de QR en imagen |
| `apps/movil/lib/proveedores/proteccion_pantalla.dart` | 8 | sí | Puerto/proveedor/adaptador de protección de pantalla |
| `apps/movil/lib/proveedores/renovar_acceso.dart` | 3 | sí | Renueva el token con un Refrescador NUEVO por llamada |
| `apps/movil/lib/proveedores/sesion.dart` | 196 | sí | Sesión (tokens en almacén seguro, generación, tope) |
| `apps/movil/lib/proveedores/tema.dart` | 16 | sí | temaProvider — sin consumidores |
| `apps/movil/lib/proveedores/tutoriales.dart` | 157 | sí | Proveedores del motor de tutoriales (define otro capacidadesProvider) |
| `apps/movil/lib/proveedores/visor_qr.dart` | 9 | sí | Puerto/proveedor del visor de QR |
| `apps/movil/lib/pantallas/alianzas/rutas.dart` | 5 | sí | GoRouter con StatefulShellRoute y redirect global / rutas de alianzas (vacío) |
| `apps/movil/lib/pantallas/arranque/pantalla_configuracion_invalida.dart` | 58 | sí | Pantalla de bloqueo por configuración inválida |

#### 1.b Configuración

| ruta | líneas | leído completo | qué hace |
|---|---|---|---|
| `apps/movil/pubspec.yaml` | 132 | sí | Dependencias (secure_storage ^10, riverpod 3, go_router 17, dio 5), 12 clientes generados, overrides ML Kit |
| `apps/movil/analysis_options.yaml` | 25 | sí | flutter_lints + 5 reglas; excluye `lib/tokens/tokens.dart` (no existe) |
| `apps/movil/package.json` | 21 | sí | Tareas turbo; `test:front` corre solo unidad/widget/contrato/identidad/pasanaku |
| `apps/movil/android/app/src/main/AndroidManifest.xml` | 86 | sí | CAMERA, 1 activity exported (launcher + deep links), FileProvider no exportado |
| `apps/movil/android/app/build.gradle.kts` | 61 | sí | compileSdk 37, host App Links por omisión de TEST, **release firmado con clave debug** |
| `apps/movil/ios/Runner/Info.plist` | 92 | sí | Esquema `aportaya`, textos de permisos cámara/fotos, sin excepciones ATS |
| `apps/movil/terceros/**` (28 archivos) | — | no: inventario (copias de plugins publicados, `README.md` leído) | ML Kit face detection 0.15.1 y commons 0.13.0 sin `ios/` |
| `apps/movil/test_driver/*.dart` (8) | 88 | sí | 6 drivers idénticos de 5 líneas + 2 que guardan una captura en `docs/trabajo/...` |

#### 1.c Pruebas de `test/unidad` y `test/widget` (112 archivos)

Leídos completos (prueban archivos del lote): `unidad/cliente_refresco_test.dart` (161), `unidad/refresco_movil_test.dart` (81), `unidad/catalogo_de_errores_test.dart` (75), `unidad/guardian_de_sesion_test.dart` (90), `widget/idioma_test.dart` (62). Revisados por búsqueda dirigida (casos y aserciones de idempotencia/«Simular pago»): `widget/cobro_de_recarga_test.dart` (215), `widget/pantalla_recargar_test.dart` (119), `billetera/recargar_inicio_test.dart`.

Inventario (una línea; importan archivos del lote según `grep package:aportaya_movil/...`, NO leídos completos por volumen — declarado):
- unidad: abridor_de_avisos (navegacion), acceso_a_rutas (navegacion), capacidades (infra), cierre_de_sesion, cliente_test, configuracion_test, control_de_bloqueo, diagnostico_api, enlaces_profundos, errores_del_alta/grupos/identidad/qr (errores.dart), estado_de_verificacion (cliente), juez_de_pose (puerto cámara), lector_de_rostro_vision, lectura_de_qr, purga_de_sesion, retorno_de_invitacion, ruta_del_aviso, sesion_observable, sesion_persiste, subida_del_expediente (idempotencia), tope_de_sesion, tutoriales/* (7), usuario_id_del_token, vencimiento_del_documento, vigilante_test.
- widget: acceso_escanear, bloqueo_plataforma, capa_de_bloqueo_* (4), capacidades_ui, cobro_de_recarga, configuracion_invalida, crear_grupo_etapa, datos_entre_pantallas (refrescar_al_cambiar), deep_link, escaner_del_carnet, etapa_y_refirma_del_grupo, haptica, idioma, inicio_de_billetera, lista_del_extracto, pantalla_camara_de_respaldo, pantalla_de_saldo, pantalla_escanear, pantalla_perfil_cerrar_sesion, pantalla_perfil_foto, pantallas_de_h_tema_y_desborde, paso_capturas, pautas_publicas(+golden), pestanas, portada, prueba_de_vida, tour, tutoriales/* (4).
- Sin import del lote (pantallas de módulo): 36 archivos restantes de unidad/widget (p. ej. calidad_de_captura, formatear_monto, iniciar_grupo, pantalla_aportar…).


