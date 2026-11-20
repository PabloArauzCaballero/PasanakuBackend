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
