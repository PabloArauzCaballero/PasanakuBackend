# AUDITORIA — frontends-compartidos (L6)

> Documento en construcción: se escribe a medida que avanza la auditoría (indicación de Pablo,
> retransmitida por la sesión coordinadora). Las secciones vacías se completan en las próximas tandas.

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | `frontends-compartidos`: `packages/*`, `clientes/*`, shell y compartido de `apps/movil`, `apps/backoffice`, `apps/web` |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado `origin/test` | `07fc69719287b0dadd152d0293a054948df69ead` |
| Commit `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe`. `git diff --stat origin/test origin/dev -- packages clientes apps` da salida vacía: **sin diferencias en este alcance** |
| Historial | Completo (`git fetch --unshallow origin test`; `--is-shallow-repository` → `false`; 1 878 commits en `origin/test`) |
| Archivos y líneas cubiertos | (se completa al cerrar, ver §12) |

## 2. Resumen

(se completa al cerrar)

## 3. Huella y mapa de conexiones

(se completa al cerrar)

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
| A-4 | Sin rastro en el alcance compartido (pendiente revisar los lotes) | Pablo |

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

## 6. Matriz de contratos

(se completa al cerrar)

## 7. Evidencia obligatoria del módulo

(se completa al cerrar)

## 8. Cobertura por clase

(se completa al cerrar)

## 9. Lo que está bien

- `apps/backoffice/src/app/nucleo/sesion.ts:23-25`: el token del operador vive solo en memoria y el refresco en cookie HttpOnly (`refresco-de-sesion.ts:37`, `withCredentials`).
- `apps/movil/lib/dominio/cu10_pago_de_prueba.dart:13-15`: no suma saldo en memoria; relee el saldo del servidor (`saldoProvider`), que lo toma del libro.
- `scripts/verificar_frontend.py todo` → `TODO OK` (sin literales de diseño, sin formato de dinero fuera de `Monto`, sin `print`, sin archivos > 200 líneas en los paquetes y apps).

## 10. Observado fuera de alcance

(se completa al cerrar)

## 11. Límites de esta auditoría

- `yarn` 4.18 no arranca en la nube: `corepack yarn --version` → `ENOTFOUND repo.yarnpkg.com`. **`yarn lint`, `yarn typecheck`, `test:front` y `test:a11y` NO COMPROBADOS.**
- Flutter y Dart no están instalados: análisis, pruebas y goldens de `apps/movil` y `packages/diseno_flutter` NO COMPROBADOS.
- Los barridos Gradle no aplican a este módulo, que no tiene código Java.
- El candado `blocker_gate.py` señala microtareas abiertas de otros planes (`docs/trabajo/2026-10-03…`, `2026-10-14…`, `2026-10-16…`, `2026-10-27…`, `2026-10-28…`, `2026-10-30…`). Una auditoría de solo lectura no los modifica.

## 12. Anexo de cobertura de archivos

(se completa al cerrar)
