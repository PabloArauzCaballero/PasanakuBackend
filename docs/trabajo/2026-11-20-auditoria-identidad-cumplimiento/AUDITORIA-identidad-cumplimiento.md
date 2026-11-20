# AUDITORÍA — identidad-cumplimiento (lote L4)

> Estado del documento: **EN CONSTRUCCIÓN**. Se escribe por tandas y cada tanda se sube a la rama
> `auditoria/identidad-cumplimiento-2026-11-20`. Las secciones marcadas «pendiente» todavía no se cerraron.

## 1. Ficha

| Campo | Valor |
|---|---|
| Módulo | identidad-cumplimiento (L4): `servicios/identidad`, `servicios/cumplimiento`, KYC, UIF/ASFI, retiros con doble aprobación |
| Responsable | Pablo (dueño del producto) |
| Fecha | 2026-11-20 |
| Commit auditado `origin/test` | `07fc69719287b0dadd152d0293a054948df69ead` |
| Commit `origin/dev` | `850e0f51192d03094304630a61e44e5bd2a97efe` — `git diff --stat origin/test origin/dev` vacío: **mismo árbol** |
| Historial | completo (`git fetch --unshallow`; `git rev-parse --is-shallow-repository` → `false`; 1878 commits en `origin/test`) |
| Archivos / líneas cubiertos | pendiente (anexo §12) |

## 2. Resumen

Pendiente de cierre. Tabla de severidades y diez más graves se completan al terminar la lectura.

## 3. Huella y mapa de conexiones

Pendiente.

## 4. Distancia al flujo nuevo

Pendiente (inventarios a, b y tabla c de ambigüedades A-1 a A-4).

## 5. Hallazgos

### IDCU-REESCRITURA-001 — CU-40 «Evaluar límites» existe dos veces; la copia de cumplimiento está muerta y diverge de la que mueve dinero
- Clase / Severidad / Confianza: REESCRITURA (también ACOPLE y DEFECTO) / S2 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU40EvaluarLimites.java:34-98`;
  `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/LimiteRepositorio.java:46-110,113-118`;
  `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU40EvaluarLimites.java:41-120` (la viva)
- Evidencia:
  - Javadoc de cumplimiento, `LimiteRepositorio.java:49-52`: «El consumo se lee con {@code FOR UPDATE} … leerlo sin bloquear la fila permite que dos operaciones simultaneas … pasen el tope diario».
  - La consulta (`:72-110`) es un `leftJoin(DSL.table(DSL.name("nucleo_financiero", "consumo_limite")))` **sin** `forUpdate()`: `grep -n 'forUpdate\|FOR UPDATE' LimiteRepositorio.java` → solo la línea 49 (el comentario).
  - La versión de nucleo-financiero sí bloquea: `var consumo = bloqueando ? limites.acumuladoBloqueado(...)` y ofrece `exigirDentroDe(dsl, …)` dentro de la transacción del dinero.
- Cómo se comprobó: `grep -rn 'CU40EvaluarLimites\b' servicios/cumplimiento/src` → solo pruebas (`ArmadoDeOla3.java:70`, `BaseDeCumplimiento.java:82`, `CU40Test`, `CU40RechazosTest`); ningún controlador ni consumidor de `servicios/cumplimiento/src/main/**/web` lo invoca; `grep` de una ruta `/limites` en el OpenAPI de cumplimiento → sin operación. Los consumidores reales (`CU11RetirarSaldo`, `SolicitudDeRetiro`, `CU12TransferirSaldo`, `CU10RecargarSaldo`) importan el de nucleo-financiero.
- Impacto: hoy ninguno en dinero (la copia no se ejecuta en producción), pero hay dos respuestas para la misma regla R-LIM-01: la copia muerta no bloquea, resuelve la ventana distinto (`ventana_inicio/ventana_fin` de la fila vs `VentanaDeLimite.resolver`) y emite `cumplimiento.limites_evaluados` con el `monto` en el payload. Si alguien la cablea creyendo el Javadoc, dos operaciones concurrentes pasan el tope. Las pruebas `CU40Test`/`CU40RechazosTest` dan cobertura a código que no corre y no a la regla viva.
- Causa de fondo: CU-40 se implementó en dos carriles; la decisión de en qué servicio vive quedó escrita como «divergencia declarada» (`LimiteRepositorio.java:59-71`) en lugar de resolverse.
- Solución de fondo: un único CU-40 en `nucleo-financiero` (dueño de `consumo_limite` y de la transacción); retirar la copia de cumplimiento con sus pruebas; si cumplimiento necesita el rastro, consumirlo por evento (`nucleo_financiero.limite_rechazo`) en vez de leer el esquema ajeno.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: nucleo-financiero (lote L1), esquema `nucleo_financiero` (`consumo_limite`, `cuenta_billetera`), `catalogo.limite_operativo_billetera`
- Estándares y normas: ISO/IEC 25010 Mantenibilidad (modularidad) y Fiabilidad; reglas 98.1.1 (lectura de base ajena), 96.3.1, 00 §1.1 (pieza duplicada)
- Esfuerzo estimado: chico

### IDCU-ACOPLE-002 — Cumplimiento lee tablas del esquema `nucleo_financiero`
- Clase / Severidad / Confianza: ACOPLE / S3 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/infraestructura/LimiteRepositorio.java:83-84` (`nucleo_financiero.consumo_limite`), `:113-118` (`nucleo_financiero.cuenta_billetera`)
- Evidencia: `.from(DSL.table(DSL.name("nucleo_financiero", "cuenta_billetera")))`; el propio archivo lo declara: «INVARIANTE-11 DECLARADO · `consumo_limite` vive en `nucleo_financiero`» (`:60`).
- Cómo se comprobó: lectura del archivo; `python3 scripts/auditar_backend.py` lo lista en «DIVERGENCIAS DECLARADAS» (`evidencia/auditar_backend.txt`).
- Impacto: un cambio de columna en `nucleo_financiero` rompe cumplimiento sin que ninguna prueba de contrato lo avise; el rol de base de cumplimiento necesita permisos sobre el esquema ajeno. Severidad S3 porque el único camino que lo usa es la copia muerta de IDCU-REESCRITURA-001.
- Causa de fondo: la misma que IDCU-REESCRITURA-001.
- Solución de fondo: se resuelve al retirar la copia; si cumplimiento necesitara el nivel de diligencia de la cuenta, pedirlo por API o recibirlo por evento.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: nucleo-financiero
- Estándares y normas: ISO/IEC 25010 Mantenibilidad (modularidad); regla 98.1.1 y 98.1.2
- Esfuerzo estimado: chico

### IDCU-DEFECTO-003 — La fecha de vigencia de los límites se calcula en UTC, no en `America/La_Paz`
- Clase / Severidad / Confianza: DEFECTO / S3 / confirmado
- Ubicaciones: `servicios/cumplimiento/src/main/java/bo/aportaya/cumplimiento/aplicacion/CU40EvaluarLimites.java:51`
- Evidencia: `var hoy = reloj.ahora().atOffset(ZoneOffset.UTC).toLocalDate();` — mientras `plataforma/comun-dominio/.../Reloj.java:26-30` ofrece `ZONA = ZoneId.of("America/La_Paz")` y `hoy()`.
- Cómo se comprobó: lectura de ambos archivos.
- Impacto: entre las 20:00 y las 24:00 de La Paz (UTC-4) el código ya considera «mañana» para `vigente_desde`/`vigente_hasta`: un tope que vence hoy deja de aplicarse cuatro horas antes y uno nuevo entra cuatro horas antes. Hoy acotado porque la copia no se ejecuta (IDCU-REESCRITURA-001).
- Causa de fondo: no se usa `Reloj.hoy()`.
- Solución de fondo: usar `reloj.hoy()` en todo cálculo de fecha calendario; se resuelve junto con la unificación de CU-40.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: la copia viva de nucleo-financiero tiene el mismo patrón (ver §10).
- Estándares y normas: ISO/IEC 25010 Adecuación funcional (corrección); ISO 8601
- Esfuerzo estimado: chico

### IDCU-SEG-004 — TEST corre con perfil `local`: segundo factor «000000» para cualquiera y clave JWT en memoria permitida
- Clase / Severidad / Confianza: SEG (también CONFIG y REG) / S2 / confirmado (por documentación y código; el entorno TEST no se tocó)
- Ubicaciones: `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/EmisorDeAcceso.java:73-80,82-91`; `despliegue/TEST.md:86-88,458`; `docker-compose.coolify.yml:448`
- Evidencia:
  - `EmisorDeAcceso.java:74-75`: `boolean produccion = entorno.acceptsProfiles(Profiles.of("staging", "production")); if (generada && produccion) { throw … }` → con cualquier otro perfil, una clave vacía genera una **en memoria** (`:80`) y solo deja un `WARN` (`:89`).
  - `despliegue/TEST.md:86-87`: «TEST corre con el perfil `local`, que trae el segundo factor de desarrollo: **el código es `000000` para cualquiera**».
  - `docker-compose.coolify.yml:448`: `JWT_CLAVE_FIRMA: ${JWT_CLAVE_FIRMA}` sin `:?` que obligue a definirla.
- Cómo se comprobó: lectura; no se accedió al servidor TEST (prohibido por el metaprompt §5).
- Impacto: en un servidor expuesto, el segundo factor del backoffice no protege nada (cualquier credencial de operador filtrada basta); con dos réplicas y la clave vacía, cada réplica firma con la suya. Hoy TEST no maneja dinero real; pero el perfil es la única barrera entre «desarrollo» y «compartido», y el fail-closed no cubre el perfil que efectivamente usa el entorno compartido.
- Causa de fondo: el fail-closed se ata a nombres de perfil (`staging`, `production`) y no a «entorno compartido»; TEST quedó en `local` como decisión documentada.
- Solución de fondo: un perfil propio para TEST que exija `JWT_CLAVE_FIRMA` y deshabilite el segundo factor fijo, o invertir la lógica (fallar salvo perfil explícitamente descartable); `${JWT_CLAVE_FIRMA:?}` en el compose.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios (validan contra el JWKS de identidad); backoffice
- Estándares y normas: ISO/IEC 25010 Seguridad (autenticidad); OWASP ASVS (autenticación multifactor); regla 90.3.4 (config que no falla al arrancar); habilitación de dinero real condicionada al dossier H15
- Esfuerzo estimado: chico

### IDCU-SEG-005 — Los servidores de recurso no validan emisor ni audiencia; el token de acceso no los lleva
- Clase / Severidad / Confianza: SEG / S3 / confirmado
- Ubicaciones: `plataforma/comun-web/src/main/java/bo/aportaya/plataforma/web/seguridad/ConfiguracionDelDecodificador.java:27-28` (punto de contacto, transversal); `servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/EmisorDeAcceso.java:112-121` (acceso sin `iss`/`aud`), `:155-157` (evidencia step-up con `iss`/`aud`, misma clave)
- Evidencia: `return NimbusJwtDecoder.withJwkSetUri(jwksUri).build();` (sin `setJwtValidator`); el constructor del token de acceso no llama `.issuer(...)` ni `.audience(...)`.
- Cómo se comprobó: lectura. Refutación: `SesionDeLaPeticion.java:71-77` rechaza todo token sin claim `rol`, y la evidencia step-up no lo trae → hoy un token de evidencia no sirve como token de acceso. Por eso queda S3 (defensa en profundidad), no S1.
- Impacto: la separación entre tipos de token depende de que ningún otro token firmado con la misma clave lleve `rol`; cualquier token futuro firmado por identidad con ese claim sería aceptado por los catorce servicios.
- Causa de fondo: hallazgo previo `docs/auditoria-produccion/security-matrix.md:52` («JWT sin `iss`/`aud`») — **sigue vigente** en el commit auditado (la parte de clave efímera fue cerrada para `staging`/`production`, ver IDCU-SEG-004).
- Solución de fondo: emitir `iss` y `aud` en el token de acceso y validar ambos en el decodificador compartido.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todos los servicios; `plataforma/comun-web` (lote L5)
- Estándares y normas: ISO/IEC 25010 Seguridad; OWASP API Security Top 10 (API2 Broken Authentication); regla 98.6.1
- Esfuerzo estimado: chico

### IDCU-DOC-006 — El cliente de aprobación de retiros dice que no existe la bandeja que el archivo vecino consume
- Clase / Severidad / Confianza: DOC (también PISADA) / S4 / confirmado
- Ubicaciones: `apps/backoffice/src/app/rutas/operacion/retiros/dominio/cu11-aprobacion-de-retiro.ts:10-12,15-18`; `apps/backoffice/src/app/rutas/operacion/retiros/dominio/cu11-retiros-en-revision.ts:3-4,12`; `servicios/nucleo-financiero/src/main/resources/openapi/*.yaml:457-460`
- Evidencia: «El contrato NO publica un listado ni una consulta de órdenes en revisión: la pantalla trabaja con el identificador que llega en el aviso» vs `GET /billetera/retiros` («Bandeja de retiros que esperan la segunda aprobacion») usado en `cu11-retiros-en-revision.ts:12`. Además `cu11-aprobacion-de-retiro.ts` declara los tipos a mano (`:15-18`) mientras el vecino importa el cliente generado (`clientes/angular/nucleo-financiero/model/...`).
- Cómo se comprobó: lectura de ambos archivos y `grep -n retiros` del OpenAPI de nucleo-financiero.
- Impacto: comentario que miente y dos convenciones para el mismo contrato en la misma carpeta: si el OpenAPI cambia `EntradaAprobacionRetiro`, el tipo escrito a mano no se entera.
- Causa de fondo: dos agentes en momentos distintos (el listado se agregó después).
- Solución de fondo: usar los tipos del cliente generado y borrar el comentario obsoleto.
- Relación con el flujo nuevo: APROB (pantalla de la doble aprobación de retiros, ver §4.a)
- Conexiones y módulos afectados: nucleo-financiero (contrato)
- Estándares y normas: ISO/IEC 25010 Mantenibilidad; regla 96.4.1
- Esfuerzo estimado: chico

#### Tanda 2 — app móvil, pantallas de identidad (lote 4)

Rutas sin prefijo: relativas a `apps/movil/lib/pantallas/identidad/`. Peldaño: DISCOVERED (nada se ejecutó: Flutter no está instalado).

### IDCU-RESPALDO-007 — El tour promete que «un fondo de garantía cubre la cuota» de quien no paga
- Clase / Severidad / Confianza: RESPALDO / S2 / confirmado (el texto); la clasificación del fondo es DECISIÓN REQUERIDA (A-2/A-3)
- Ubicaciones: `apps/movil/lib/pantallas/identidad/textos.dart:42-46`; `apps/movil/lib/pantallas/identidad/laminas_del_tour.dart:44-50`
- Evidencia: `'Hay un fondo de garantía que cubre la cuota para que el turno se entregue igual.'`; etiqueta `'Un paraguas cubriendo la moneda de la cuota'`
- Cómo se comprobó: lectura completa de ambos archivos; búsqueda de «garant/cubr/respald» en el lote; el contrato local (`texto_del_contrato.dart`) no menciona fondo de garantía ni cobertura.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: promesa comercial de cobertura en la primera pantalla que ve cualquier persona. Choca con R-NUEVA-2 (la empresa no pone plata por quien no paga) si el fondo es de la empresa, y aun si es mutual promete «el turno se entrega igual», que la regla nueva ya no garantiza. Además contradice el contrato, que es silencioso: promesa sin respaldo contractual.
- Nota del orquestador: el subagente lo propuso S1. Se baja a S2 porque es un texto de pantalla, no un camino que mueva dinero (criterio S1 de la §10 del metaprompt). Además el texto describe el «fondo de garantía» mutual, que `docs/Arquitectura/ADR-051 Garantía del pozo, comisión y autogestión.md:57` declara **legado** («la reserva empresarial es el único respaldo; el fondo mutual … no cubre faltantes de pozo»): contradice a la vez R-NUEVA-2 y la decisión vigente.
- Causa de fondo: copy de producto escrito para el modelo anterior (fondo que cubre) sin trazabilidad a la regla de negocio vigente.
- Solución de fondo: retirar la lámina 4 y su arte, o reescribirla según la decisión de R-NUEVA-2 (qué pasa si alguien no aporta: mora + debido proceso, sin cobertura); alinear con el contrato de participación.
- Relación con el flujo nuevo: RESPALDO
- Conexiones y módulos afectados: ninguna técnica; dominio garantía (ilustración `ArteFondoDeGarantia` en `packages/diseno_flutter`).
- Estándares y normas: 91.5 / R-NUEVA-2; 00 §1.6 (no cambiar la semántica de un requisito).
- Esfuerzo estimado: chico

### IDCU-APROB-008 — Todo el circuito de identidad del cliente asume aprobación humana de KYC como compuerta de dinero y pasanaku
- Clase / Severidad / Confianza: APROB / S2 / confirmado (en el cliente)
- Ubicaciones: `pantalla_identidad_requerida.dart:15-18,91-111,133-136`; `dominio/verificacion_diferida.dart:13-31`; `dominio/kyc_del_titular.dart:42-51`; `pantalla_estado_de_verificacion.dart:78-112`; `dominio/consultar_estado_de_cuenta.dart:8-49`; `boton_consultar_estado.dart:11-56`; `acciones_de_verificacion.dart:33-39`; `textos_de_verificacion.dart:6-21,42-44,56-58,72-81`; `texto_completar_kyc.dart:7-8,32-33`; `textos_del_perfil.dart:29-30`; `apps/movil/lib/proveedores/sesion.dart:149-159`; spec `identidad.yaml:350-352,458`
- Evidencia: `EstadoDeVerificacionEstadoEnum.EN_REVISION => 'En revisión humana'`; `'Apenas una persona los confirme vas a poder hacer esta operación.'`
- Cómo se comprobó: lectura de todo el lote + paths del spec (`resolverVerificacion`, «la aprobacion sigue siendo manual»).
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: si A-1 se resuelve como «el KYC entra en R-NUEVA-1», hay que rediseñar la compuerta, los estados mostrados, el sondeo y la activación por renovación de token; si queda fuera, el copy debe decirlo sin prometer plazos.
- Causa de fondo: el KYC está modelado como revisión humana del backoffice (decisión `resolverVerificacion`), el cliente solo refleja.
- Solución de fondo: DECISIÓN REQUERIDA A-1 (negocio/cumplimiento). Implementar la rama que se decida en el servidor; el cliente solo debe mapear `estadoDeCuenta` y no codificar «una persona».
- Relación con el flujo nuevo: APROB
- Conexiones y módulos afectados: `GET /usuarios/yo/perfil`, `GET /usuarios/{id}/verificacion`, `POST /sesion/refrescar`, backoffice.
- Estándares y normas: 00 §1.7 (ambigüedad registrada, no resuelta).
- Esfuerzo estimado: grande

### IDCU-HUECO-009 — El perfil transaccional (origen de fondos, actividad, monto) se pide y nunca se registra
- Clase / Severidad / Confianza: HUECO / FLUJO-FALTANTE / S2 / confirmado
- Ubicaciones: `dominio/cu01_registrar.dart:61-78` (cuerpo sin perfil); `dominio/envio_del_alta.dart:38-55`; `pantalla_verificar_identidad.dart:71-79` (verificación diferida: solo documento); `pasos_alta/catalogo_del_perfil.dart:16-18` («el día que el alta mande el perfil al servidor»); `pasos_alta/paso_perfil_transaccional.dart:97-110`; spec `identidad.yaml:1415-1418` (`EntradaRegistro additionalProperties: false`, sin perfil); test `apps/movil/test/identidad/estado_alta_test.dart:135-160` (lista blanca que excluye el perfil)
- Evidencia: `'aceptaContratos': contratosAceptados,` es el último campo; ningún `origenDeFondos/actividadEconomica/montoMensualEstimado` fuera del borrador
- Cómo se comprobó: lectura; `grep origenDeFondos` en `servicios/` solo aparece en `BorradorOnboarding*` de identidad, no en el alta ni en cumplimiento.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: la persona declara origen de fondos y actividad (exigencia UIF/ASFI para comparar contra lo operado) y el dato solo vive en un borrador temporal de onboarding; `cumplimiento.perfil_transaccional` / `declaracion_origen_fondos` no lo reciben desde la app. Declaración regulatoria perdida y un paso de UI que miente sobre su efecto.
- Causa de fondo: el contrato `EntradaRegistro` nunca incorporó el perfil y el test fija esa ausencia como correcta.
- Solución de fondo: decidir el dueño (cumplimiento) y su contrato; publicar evento o endpoint con el perfil (con moneda e importe decimal); o retirar el paso si no se va a usar.
- Relación con el flujo nuevo: ninguna directa (cumplimiento UIF, A-1)
- Conexiones y módulos afectados: identidad → cumplimiento (inexistente hoy).
- Estándares y normas: 98.1 (contrato), 90.2, 97.4.5.
- Esfuerzo estimado: medio

### IDCU-HUECO-010 — La declaración PEP se guarda solo en memoria y se pierde
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `pantalla_verificacion_profunda.dart:104-117`; `dominio/estado_cuenta.dart:23-46`
- Evidencia: `onPressed: ... ref.read(pepProvider.notifier).declarar(esPep: _esPep!, detalle: _detalle.text)` — `PepNotifier` solo hace `state = EstadoPep(...)`, sin llamada de red ni navegación
- Cómo se comprobó: lectura; `grep pepProvider` en `lib` solo lo usa esta pantalla.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: la persona responde una pregunta de cumplimiento, toca «Confirmar respuesta» y no pasa nada; ni el servidor ni cumplimiento se enteran. Riesgo regulatorio (debida diligencia reforzada no disparada) y UX engañosa.
- Causa de fondo: pantalla construida antes de que existiera el endpoint; sin estado de error/éxito.
- Solución de fondo: endpoint de declaración PEP en cumplimiento con idempotencia y acuse; mientras no exista, ocultar la ruta `/identidad/verificacion-profunda`.
- Relación con el flujo nuevo: ninguna (A-1)
- Conexiones y módulos afectados: ninguna hoy.
- Estándares y normas: 95.2 (estados), 90.1.
- Esfuerzo estimado: medio

### IDCU-HUECO-011 — «Cambiar contraseña» no cambia nada y no avisa
- Clase / Severidad / Confianza: HUECO / SEG / S2 / confirmado
- Ubicaciones: `pantalla_contrasena.dart:92-104`; `dominio/estado_cuenta.dart:114-136` (`confirmar()` lanza `UnimplementedError` y nadie lo llama)
- Evidencia: `onPressed: valido ? () => ref.read(contrasenaProvider.notifier).actualizar(actual: _actual.text, nueva: _nueva.text) : null`
- Cómo se comprobó: lectura; `grep contrasenaProvider` solo aparece en esa pantalla.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: quien sospecha que su clave fue expuesta cree haberla cambiado. Además la contraseña actual y la nueva quedan en el estado de un `Notifier` global sin limpiarse.
- Causa de fondo: falta el endpoint en identidad (el comentario lo dice) y la UI se publicó igual.
- Solución de fondo: endpoint con verificación de la clave actual, rate limit y revocación de sesiones; hasta entonces ocultar la fila del perfil (`pantalla_perfil.dart:87-93`).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: identidad (inexistente).
- Estándares y normas: 90.1, 95.3.4, 00 §5.4.
- Esfuerzo estimado: medio

### IDCU-HUECO-012 — «Confirmar baja» solo cierra la hoja; el contrato promete cerrar la cuenta «desde la app»
- Clase / Severidad / Confianza: HUECO / S2 / confirmado
- Ubicaciones: `pantalla_baja.dart:50-67`; `dominio/estado_cuenta.dart:138-156` (`confirmarBaja` lanza `UnimplementedError`, no usado); `texto_del_contrato.dart:186` («puede cerrar su cuenta cuando quiera, desde la app»); test `apps/movil/test/widget/pantalla_baja_test.dart:8-27` (solo prueba cancelar)
- Evidencia: `onConfirmar: () => Navigator.of(context).pop(),`
- Cómo se comprobó: lectura.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: derecho contractual (cláusula 17.ª y Ley 453 citada) incumplido; la persona cree haberse dado de baja.
- Causa de fondo: endpoint inexistente; pantalla publicada como si existiera.
- Solución de fondo: endpoint de baja con precondiciones (saldo cero, sin cupos activos) validadas en servidor; o retirar la fila y la cláusula hasta tenerlo.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: identidad, núcleo financiero, grupos (precondiciones).
- Estándares y normas: 95.2, 00 §6.
- Esfuerzo estimado: medio

### IDCU-COHERENCIA-013 — Se acepta por id un contrato del servidor pero se muestra un texto y un tarifario fijos del cliente
- Clase / Severidad / Confianza: COHERENCIA / DUP-X / S2 / probable
- Ubicaciones: `texto_del_contrato.dart:24-28,226-260`; `cuerpo_del_contrato.dart:20`; `dominio/estado_contrato.dart:42-47`; `pantalla_contrato.dart:27-35`; `panel_de_aceptacion.dart:57-61` (comentario obsoleto); `avisos_de_sesion.dart:28`, `capa_de_bloqueo_vista.dart:155-156`
- Evidencia: `const String versionDelContrato = 'CTO-BILLETERA v1';` y `Comisión por cobro de turno: 0,30 % ... mínimo de Bs 10,00 y un máximo de Bs 50,00.` escritos en el cliente, mientras `aceptados` envía `vigentes.deTipo(...).id` del servidor
- Cómo se comprobó: lectura; la pantalla nunca compara versión/hash del vigente con el texto pintado.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: la aceptación auditada (id del servidor) puede corresponder a un texto distinto del que la persona leyó; un cambio de tarifario en servidor no actualiza lo mostrado. Parámetros de negocio (3 intentos, 5 min, 8 h, 30 días, 0,30 %) duplicados en el cliente.
- Causa de fondo: el endpoint de vigentes no devuelve (o no se usa) el cuerpo del contrato.
- Solución de fondo: servir el texto (o su URL firmada + hash) desde cumplimiento y pintar exactamente eso; bloquear aceptar si hash ≠ versión pintada.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: cumplimiento `GET /cumplimiento/contratos/vigentes`; tarifas.
- Estándares y normas: 91.1.5 (una sola fuente), 97.4.6.
- Esfuerzo estimado: medio

### IDCU-HUECO-014 — Fotos del expediente que fallaron se borran al salir y no hay camino para re-subir anverso, reverso ni selfie
- Clase / Severidad / Confianza: HUECO / S2 / probable
- Ubicaciones: `pantalla_subida_del_expediente.dart:101-110,157-175`; `dominio/seguimiento_del_alta.dart:62-71`; `dominio/kyc_del_titular.dart:28-40`; `pantalla_completar_kyc.dart:127-153`
- Evidencia: `dispose(){ ... addPostFrameCallback((_) { unawaited(_seguimientoDelAlta.liberarCapturas()); });` y «Continuar» habilitado con fotos `fallida`; `pendientesDeKyc` solo lista perfiles y fecha
- Cómo se comprobó: lectura del flujo completo (subida → continuar → perfil/completar).
- Impacto: expediente incompleto sin recuperación en la app; con revisión humana termina en rechazo o en soporte.
- Causa de fondo: `pendientesDeKyc` modela solo los faltantes «nuevos» (perfiles, fecha), no las cinco caras.
- Solución de fondo: derivar faltantes de `EstadoDeVerificacion.fotos` para las cinco caras y ofrecer recaptura; no borrar copias locales de caras fallidas.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `POST /usuarios/{id}/documentos`.
- Estándares y normas: 95.2.3.
- Esfuerzo estimado: chico

### IDCU-SEG-015 — Prueba de vida y cotejo son escenografía en el cliente
- Clase / Severidad / Confianza: SEG / DATOS / S2 / confirmado (en cliente); lo que hace el servidor no se verificó
- Ubicaciones: `dominio/ciclo_de_pose.dart:115-117`; `pasos_alta/pie_de_prueba_de_vida.dart:105-112`; `controlador_prueba_de_vida.dart:96-99`; `pasos_alta/paso_cotejo.dart:12-55`; `dominio/datos_del_alta.dart:92-104`; `texto_del_contrato.dart:76`
- Evidencia: `/// «Tomar ahora»: siempre disponible ...`; `FilaDeCotejo(campo: ..., declarado: datos.nombres, leido: datos.nombres)`
- Cómo se comprobó: lectura.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: el detector de pose se saltea con un toque; el «cotejo» compara el dato consigo mismo y siempre «coincide». El contrato le dice a la persona que hay «prueba de vida y cotejo». Datos simulados en producción presentados como verificación.
- Causa de fondo: OCR/liveness no contratados (hueco H-CLIENTE declarado en comentarios).
- Solución de fondo: quitar el cotejo hasta tener OCR real o rotularlo como «revisá tus datos»; la decisión de vida debe tomarla el servidor/proveedor; ajustar el contrato.
- Relación con el flujo nuevo: APROB (hoy lo compensa la revisión humana)
- Conexiones y módulos afectados: proveedor KYC (inexistente).
- Estándares y normas: 00 §2.1, 97.4.1.
- Esfuerzo estimado: medio

### IDCU-DINERO-016 — Monto mensual estimado como `double`, sin moneda en el contrato, 'Bs' literal
- Clase / Severidad / Confianza: DINERO / S3 / confirmado
- Ubicaciones: `pasos_alta/paso_perfil_transaccional.dart:81-88,107,163,170-173`; `dominio/modelo_alta.dart:111,156`; `dominio/notifier_alta.dart:104`; `dominio/borrador_alta.dart:45,111-114`; `dominio/modelo_del_alta.dart:72`
- Evidencia: `final double? montoMensualEstimado;`; `montoMensualEstimado: state.montoMensualEstimado?.toString()` (envía `"1500.0"`); formateador `RegExp(r'[0-9.]')` admite `1.2.3`
- Cómo se comprobó: lectura; spec `BorradorOnboarding.montoMensualEstimado: { type: string }` sin moneda.
- Impacto: importe declarado sin moneda y con representación binaria; `round()` al restaurar pierde decimales.
- Causa de fondo: falta tipo `Dinero` en el modelo del alta.
- Solución de fondo: usar el `Dinero` del cliente generado (existe `dinero.dart`) con `moneda`, `Decimal`/string validado.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: borrador de onboarding.
- Estándares y normas: 91.1.1, 91.1.2.
- Esfuerzo estimado: chico

### IDCU-CONTRATO-017 — Seis operaciones del spec se llaman por Dio crudo con comentarios obsoletos y casteos sin validar
- Clase / Severidad / Confianza: CONTRATO / DUP-INT / S3 / confirmado
- Ubicaciones: `dominio/cu01_registrar.dart:27-28,48-87`; `dominio/estado_cuenta.dart:6-17`; `dominio/verificacion_diferida.dart:9-12,34-76`; `dominio/cu02_actualizar_fecha_expiracion.dart:5-30`; `dominio/cu02_subir_foto.dart:61-83`; `dominio/foto_de_perfil.dart:20-75`; `dominio/datos_del_alta.dart:92-94`; spec `identidad.yaml:99,435-437,502-503,734-736,747-773`; barrel `clientes/dart/identidad/lib/aportaya_cliente_identidad.dart` (exporta `mi_perfil`, `fecha_expiracion_documento`, `entrada_registro`, `salida_registro`)
- Evidencia: `/// Va por Dio directo y no por el cliente generado: ese cliente todavía exige el documento y no conoce INCOMPLETO_KYC` — el spec actual ya declara `documento` opcional y `INCOMPLETO_KYC`.
- Cómo se comprobó: comparación de paths/schemas del spec y exports del barrel (el `src/` generado no está en el repo: `.gitignore:51`).
- Impacto: el cliente no se rompe en compilación cuando cambia el contrato; `MiPerfil` se pide tres veces por providers distintos; `r.data!` / `as String` revientan con TypeError ante una respuesta inesperada.
- Causa de fondo: deriva entre spec y cliente nunca reconciliada.
- Solución de fondo: regenerar cliente, usar `registrarUsuario`, `consultarMiPerfil`, `actualizarFechaExpiracionDocumento`, `iniciarVerificacion`, `subirDocumento`, `cambiarMiFotoDePerfil`; un solo provider de `MiPerfil`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: todas las de identidad listadas arriba.
- Estándares y normas: 96.1.2, 98.1.3.
- Esfuerzo estimado: medio

### IDCU-COHERENCIA-018 — Vocabularios de tipo de documento y teléfono divergentes; coerción silenciosa a CI
- Clase / Severidad / Confianza: COHERENCIA / S3 / probable (latente: la UI solo ofrece CI)
- Ubicaciones: `dominio/cu01_registrar.dart:105-109`; `dominio/borrador_alta.dart:7-12,72-76`; spec `identidad.yaml:1387-1398` (`Documento.tipo: [CI, CEX, PASAPORTE]`) vs `:1038-1048` (`BorradorOnboarding.tipoDocumento: [CI, PASAPORTE, CARNET_EXTRANJERO]`, `telefonoE164: '^\+591[67]\d{7}$'`); `dominio/validaciones.dart:62-68` (`^\+591\d{8}$`)
- Evidencia: `'tipo': switch (tipo.toUpperCase()) { 'CEX' => 'CEX', 'PASAPORTE' => 'PASAPORTE', _ => 'CI' }` — un borrador restaurado trae `CARNET_EXTRANJERO` y se envía como `CI`.
- Cómo se comprobó: lectura + spec.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: un extranjero restaurado desde borrador quedaría registrado como CI; un teléfono `+5912…` pasa el alta y rompe el guardado del borrador (tragado por `catch (_)`).
- Causa de fondo: dos enums para el mismo concepto en el mismo contrato.
- Solución de fondo: unificar el enum en el spec; el cliente debe fallar, no adivinar, ante un valor desconocido.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: alta, borrador.
- Estándares y normas: 97.4.7, 98.1.3.
- Esfuerzo estimado: chico

### IDCU-HUECO-019 — El alta solo admite CI boliviano
- Clase / Severidad / Confianza: HUECO / REG / S3 / confirmado
- Ubicaciones: `pasos_alta/paso_documento.dart:93-166` (sin selector de tipo); `dominio/datos_del_alta.dart:13`; `dominio/validaciones.dart:89-95`; `pasos_alta/formateadores_del_alta.dart:3-6`; contra `texto_del_contrato.dart:76` y spec `Documento.tipo`
- Evidencia: `'Escribí un número de CI de entre 5 y 10 dígitos.'` y `this.tipoDocumento = 'CI'`
- Cómo se comprobó: lectura.
- Impacto: residentes con CEX o pasaporte no pueden verificarse; el contrato y el spec lo prevén.
- Causa de fondo: alcance recortado sin decisión registrada.
- Solución de fondo: selector de tipo con validaciones por tipo, o decisión de negocio que lo excluya.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: alta, verificación diferida.
- Estándares y normas: 00 §1.6.
- Esfuerzo estimado: medio

### IDCU-FRAGIL-020 — Catch que tragan y estados que quedan colgados
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado (por lectura)
- Ubicaciones: `dominio/verificacion_correo.dart:186-188` (`catch (_) {}`); `dominio/subida_del_expediente.dart:122-156` (solo `on DioException`: un `FileSystemException` de `readAsBytes` o un `TypeError` de `as String` deja la cara en «subiendo» y el botón Continuar bloqueado); `pantalla_camara.dart:70-97` (`_tomarFoto` sin try: `_tomando` queda `true`); `pantalla_identidad_requerida.dart:51-63` (`onActivar` sin try); `pantalla_perfil.dart:123-129` (sin finally); `dominio/notifier_alta.dart:117-124` (`unawaited(guardarBorrador())`)
- Evidencia: `} on DioException catch (e) {` como única rama en `_subirUna`
- Cómo se comprobó: lectura.
- Impacto: pantallas trabadas sin salida, y fallos de borrador invisibles (la persona pierde el rescate sin saberlo).
- Causa de fondo: manejo de errores por tipo de excepción esperado, no por resultado.
- Solución de fondo: `on Object` con estado `fallida` accionable; registrar (sin PII) el fallo de borrador.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: subida de fotos, borrador.
- Estándares y normas: 95.2.1, 96.3.
- Esfuerzo estimado: chico

### IDCU-DEFECTO-021 — Clave de idempotencia de «completar KYC» nunca rota
- Clase / Severidad / Confianza: DEFECTO / S3 / probable
- Ubicaciones: `pantalla_completar_kyc.dart:80-88`; contrastar con la rotación correcta en `dominio/subida_del_expediente.dart:187-191`
- Evidencia: `formularioId: 'kyc-completar-${cara.name}',` sin `cerrar` previo
- Cómo se comprobó: lectura de `idempotenciaProvider.claveDe` usado en `cu02_subir_foto.dart:70-72`.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: tras un fallo con respuesta (p. ej. 422 por imagen inválida), la foto nueva viaja con la misma clave; el servidor puede reproducir el resultado anterior o rechazar por cuerpo distinto.
- Causa de fondo: patrón de rotación no aplicado en esta pantalla.
- Solución de fondo: rotar la clave al repetir la foto (igual que `rotarClaveParaRepetir`).
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `POST /usuarios/{id}/documentos`.
- Estándares y normas: 96.3.3.
- Esfuerzo estimado: chico

### IDCU-SEG-022 — Credenciales: backup Android por defecto y candado biométrico solo visual
- Clase / Severidad / Confianza: SEG / CONFIG / S3 / probable
- Ubicaciones: `apps/movil/android/app/src/main/AndroidManifest.xml:10` (sin `android:allowBackup="false"` ni `dataExtractionRules`); `apps/movil/lib/infraestructura/android/almacen_seguro_android.dart:9`; `capa_de_bloqueo_biometria.dart:63-67`; `apps/movil/lib/proveedores/sesion.dart:48-49`
- Evidencia: `const FlutterSecureStorage(aOptions: AndroidOptions())`; comentario «Confirma identidad local para quitar el candado; no autentica»
- Cómo se comprobó: lectura y grep del manifiesto.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: (a) el contenedor cifrado de tokens entra en el backup automático (las claves del Keystore no viajan: al restaurar falla la lectura y la sesión se pierde; MASVS-STORAGE recomienda excluirlo); (b) los tokens se leen sin biometría: el candado protege la UI, no la credencial (MASVS-AUTH-2). Lo positivo: iOS usa `unlocked_this_device` y nada va a SharedPreferences.
- Causa de fondo: configuración por defecto.
- Solución de fondo: excluir el almacén del backup; evaluar ligar el refresh token a una clave del Keystore con autenticación de usuario.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: almacén seguro.
- Estándares y normas: 95.6.2, OWASP MASVS-STORAGE-1 / AUTH-2.
- Esfuerzo estimado: chico

### IDCU-RESILIENCIA-023 — Refresco de sesión sin timeouts y con dos refrescadores no coordinados
- Clase / Severidad / Confianza: RESILIENCIA / S3 / probable
- Ubicaciones: `apps/movil/lib/dominio/cliente.dart:88-90`; `apps/movil/lib/dominio/renovar_acceso.dart:12-16`
- Evidencia: `Dio(BaseOptions(baseUrl: dio.options.baseUrl))` sin `connectTimeout/receiveTimeout`, y `Refrescador(...)` nuevo en cada «Activar mi cuenta»
- Cómo se comprobó: lectura (Dio no aplica timeout si no se configura).
- Impacto: «Activar mi cuenta» / «Consultar estado» pueden quedar cargando indefinidamente; un refresco del interceptor y otro del botón en paralelo pueden presentar el mismo refresh token rotado (detección de reúso → sesión cerrada).
- Causa de fondo: se copió `BaseOptions` sin los timeouts del Dio principal.
- Solución de fondo: compartir el `Refrescador` single-flight y heredar timeouts.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `POST /sesion/refrescar`.
- Estándares y normas: 98.2.1.
- Esfuerzo estimado: chico

### IDCU-UX-A11Y-024 — Errores crudos y estados vacíos mudos
- Clase / Severidad / Confianza: UX-A11Y / S3 / confirmado
- Ubicaciones: `textos_de_verificacion.dart:94-101` (`_ => codigo`: el `motivoRechazo` libre del servidor, `maxLength 160`, se muestra tal cual; el mapeo de 4 códigos «igual que Atlas» no tiene fuente en el spec); `capa_de_bloqueo_vista.dart:122-126` (fallo biométrico del sistema en el texto); `pantalla_estado_de_verificacion.dart:50` y `pantalla_identidad_requerida.dart:41` (`mensajeVacio: ''`); `pantalla_completar_kyc.dart:110` (vacío = «La revisión ya terminó»)
- Evidencia: `'No se pudo abrir la huella o el PIN del teléfono ($falloBiometrico).'`
- Cómo se comprobó: lectura; spec `EstadoDeVerificacion.motivoRechazo` es string libre.
- Impacto: mensajes técnicos al usuario; vacío sin orientación.
- Causa de fondo: catálogo de motivos no codificado en el contrato.
- Solución de fondo: enum de motivos en el spec y texto genérico accionable para valores desconocidos.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: `GET /usuarios/{id}/verificacion`.
- Estándares y normas: 95.2.2, 95.2.3, 97.5.2.
- Esfuerzo estimado: chico

### IDCU-HUECO-025 — «Confiar en este dispositivo por 30 días» no se envía; recuperación de contraseña inexistente
- Clase / Severidad / Confianza: HUECO / S3 / confirmado
- Ubicaciones: `pantalla_dispositivos.dart:45-58`; `dominio/estado_sesion.dart:155-156`; `sesion_pie.dart:10-28`; `textos.dart:145-147`; contrato `texto_del_contrato.dart:105`
- Evidencia: `onChanged: notifier.elegirConfianza` solo cambia estado local; `'Todavía no se puede recuperar desde la app.'`
- Cómo se comprobó: lectura.
- Re-verificación del orquestador: ubicaciones re-abiertas y confirmadas (lectura/grep).
- Impacto: casilla con efecto falso; quien olvida la clave queda fuera de su dinero sin flujo (el contrato dice que se desbloquea «por el procedimiento de recuperación que la app indique»).
- Causa de fondo: endpoints ausentes.
- Solución de fondo: implementar o retirar; recuperación con token de un solo uso y sin enumeración.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: identidad.
- Estándares y normas: 90.1.11.
- Esfuerzo estimado: medio

### IDCU-FRAGIL-026 — Rutas que revientan con enlace profundo o sin `extra`
- Clase / Severidad / Confianza: FRAGIL / S3 / confirmado (por lectura, no ejecutado)
- Ubicaciones: `rutas_de_entrada.dart:96,104,113,120`
- Evidencia: `PantallaDeCamara(cara: state.extra! as CaraDelCarril)`; `usuarioId: state.uri.queryParameters['usuario']!`
- Cómo se comprobó: lectura.
- Impacto: `/registro/subida` o `/registro/estado` sin `usuario`, o reapertura de la app en `/registro/camara`, lanzan excepción en build.
- Causa de fondo: sin validación de parámetros de ruta.
- Solución de fondo: validar y redirigir a una pantalla de estado.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: 95.2.
- Esfuerzo estimado: chico

### IDCU-UX-A11Y-027 — Botón de solo icono sin nombre accesible
- Clase / Severidad / Confianza: UX-A11Y / S3 / confirmado
- Ubicaciones: `pasos_alta/cabecera_de_camara.dart:24-27` (usado por `encabezado_y_consejos_de_camara.dart:24`)
- Evidencia: `IconButton(icon: const Icon(Icons.arrow_back, color: Paleta.white), onPressed: onVolver)` sin `tooltip`
- Cómo se comprobó: lectura (comparar con `barra_superior_de_vida.dart:28-29` y `visor_ampliado_de_captura.dart:76-77`, que sí lo tienen).
- Impacto: el lector de pantalla anuncia «botón» sin nombre en la cámara del carnet.
- Causa de fondo: ver solución.
- Solución de fondo: `tooltip: 'Volver'`.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: ninguna.
- Estándares y normas: 95.4.1.
- Esfuerzo estimado: chico

### IDCU-PRUEBAS-028 — Los tests fijan los huecos como comportamiento correcto
- Clase / Severidad / Confianza: PRUEBAS / S3 / confirmado
- Ubicaciones: `apps/movil/test/identidad/estado_alta_test.dart:135-160` (lista blanca del alta sin perfil transaccional); `apps/movil/test/widget/pantalla_baja_test.dart:8-27` (solo cancelar); ausencia de tests para `pepProvider`, `contrasenaProvider`, `confiarEnDispositivo` (grep sin resultados); goldens con `skip: reutilizaH8 && _omitirH8EnActualizacion` (`test/goldens/h10_alta_*_visual_test.dart`, p. ej. `h10_alta_actividad_visual_test.dart:20,130`)
- Evidencia: `expect(body.keys.toSet().difference(permitidas), isEmpty);`
- Cómo se comprobó: grep dirigido y lectura de los rangos citados (no se leyeron completos los 112 archivos de test).
- Impacto: suites verdes que blindan la pérdida de la declaración y las acciones falsas.
- Causa de fondo: ver solución.
- Solución de fondo: tests de contrato que exijan el envío o la ausencia deliberada con decisión registrada.
- Relación con el flujo nuevo: ninguna
- Conexiones y módulos afectados: —
- Estándares y normas: 80.2, 80.5.4.
- Esfuerzo estimado: chico

### IDCU-DEFECTO-029 — El sondeo del estado KYC se rinde a los 40 s frente a una revisión humana
- Clase / Severidad / Confianza: DEFECTO / S4 / confirmado
- Ubicaciones: `dominio/estado_de_verificacion.dart:21-22,79-81,91-93`
- Evidencia: `_intervalo = Duration(seconds: 2); _maximoDeIntentos = 20;`
- Impacto: la pantalla deja de actualizar sin decirlo; mitigado por «Actualizar estado».
- Causa de fondo: ver solución.
- Solución de fondo: backoff largo o push; avisar cuando se detiene.
- Relación con el flujo nuevo: APROB
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

### IDCU-DUP-INT-030 — Código muerto y duplicado
- Clase / Severidad / Confianza: DUP-INT / CLEAN / S4 / confirmado
- Ubicaciones: `dominio/modelo_del_alta.dart:1-126` (copia divergente de `EstadoAlta`/`PasoAlta`, sin importadores); `dominio/carnet_de_prueba.dart:55-82` (`generarCapturasDePrueba` sin uso en `lib`; viaja en el binario aunque bloqueado por `kReleaseMode`); `pasos_alta/errores_paso_datos.dart:3-44` (sin uso; `paso_datos.dart:67-89` reimplementa); `dominio/estado_cuenta.dart:48-86,138-156` (`perfilProvider`, `bajaProvider` sin uso); `textos_de_verificacion.dart:6-7`, `textos_de_captura.dart:21` (textos sin uso); `pantalla_bienvenida.dart:9-12` y `panel_de_aceptacion.dart:57-61` (comentarios obsoletos)
- Impacto: confusión sobre cuál `EstadoAlta` rige; superficie de dev en release.
- Causa de fondo: ver solución.
- Solución de fondo: borrar.
- Relación con el flujo nuevo: ninguna
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

### IDCU-CLEAN-031 — Literales de tamaño y textos fuera de los archivos de textos
- Clase / Severidad / Confianza: CLEAN / S4 / confirmado
- Ubicaciones: tamaños: `pasos_alta/bloque_de_formulario.dart:26` (14), `avisos_de_sesion.dart:46` (16), `pasos_alta/tarjeta_de_confianza.dart:24` (18), `pantalla_pautas_de_fotos.dart:158` (20), `pasos_alta/miniaturas_de_vida.dart:80-81,88` (76/34), `capa_de_bloqueo_vista.dart:83,92` (420/88), `banda_de_marca.dart:62` (148), `pantalla_subida_del_expediente.dart:122` (112), `lamina_del_tour.dart:86`, `pantalla_portada.dart:67` (320); textos: `capa_de_bloqueo_vista.dart:99,113,134,150-156`, `avisos_de_sesion.dart:28-30`, `pantalla_pautas_de_fotos.dart:29,49,56,63-64`, `pasos_alta/opciones_de_captura_rechazada.dart:30,43`, `pasos_alta/hoja_antes_de_escanear.dart:34-57`, `pantalla_completar_kyc_de_sesion.dart:22,31`, `pantalla_subida_del_expediente.dart:126-127`, `paso_contrato_de_alta.dart:25-28`, `pasos_alta/canal_de_verificacion.dart:30-32`, `pantalla_verificacion_profunda.dart:52`, `pantalla_bienvenida.dart:53` (`'BOB'`), `pasos_alta/paso_perfil_transaccional.dart:163` (`'Bs'`)
- Impacto: tokens y textos se desincronizan; parámetros de negocio (8 h, 5 min) escritos en la UI.
- Causa de fondo: ver solución.
- Solución de fondo: tokens de `aportaya_diseno` y archivos de textos.
- Relación con el flujo nuevo: ninguna
- Estándares y normas: 95.1.5.
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Esfuerzo estimado: chico

### IDCU-UX-A11Y-032 — Movimiento no condicionado a «reducir movimiento» en dos puntos
- Clase / Severidad / Confianza: UX-A11Y / S4 / confirmado
- Ubicaciones: `dominio/comportamiento_de_pantalla_del_alta.dart:54-59` (`Scrollable.ensureVisible(duration: Movimiento.entrada)` sin `duracion(context, …)`); `fila_de_subida.dart:95-98` (`LinearProgressIndicator` indeterminado)
- Impacto: animación con movimiento reducido activo.
- Causa de fondo: ver solución.
- Solución de fondo: usar `duracion(context, …)`.
- Relación con el flujo nuevo: ninguna
- Estándares y normas: 95.5.1.
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Esfuerzo estimado: chico

### IDCU-FRAGIL-033 — La cámara devuelve dos formas distintas
- Clase / Severidad / Confianza: FRAGIL / CONTRATO interno / S4 / confirmado
- Ubicaciones: `pantalla_camara.dart:23-25,96,110`; `pasos_alta/sacar_captura.dart:85`; `rutas_de_entrada.dart:89-91` (documenta un `'prueba'` que ya no existe)
- Evidencia: `Navigator.of(context).pop(evaluacion.captura)` vs `pop({widget.cara: captura})`
- Impacto: contrato `Object?` difuso entre pantallas; cualquier consumidor nuevo se equivoca.
- Causa de fondo: ver solución.
- Solución de fondo: un único tipo de resultado.
- Relación con el flujo nuevo: ninguna
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

### IDCU-TRAZA-034 — `tokenDeAltaVenceEn` ignorado y provider KYC sin invalidación por sesión
- Clase / Severidad / Confianza: TRAZA / SEG / S4 / confirmado
- Ubicaciones: `dominio/cu01_registrar.dart:84-87` (no lee `tokenDeAltaVenceEn`, spec `identidad.yaml:1497`); `dominio/seguimiento_del_alta.dart:19-20`; `dominio/kyc_del_titular.dart:23-26` (`FutureProvider.family` sin `autoDispose` ni `generacionDeSesionProvider`)
- Impacto: tras vencer el token de alta, subida/consulta dan 403 genérico; caché de KYC que sobrevive al cambio de sesión del mismo id.
- Causa de fondo: ver solución.
- Solución de fondo: usar el vencimiento para ofrecer «ingresá para continuar»; `autoDispose` + generación.
- Relación con el flujo nuevo: ninguna
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

### IDCU-REG-035 — Validaciones de negocio duplicadas en el cliente
- Clase / Severidad / Confianza: REG / S4 / confirmado
- Ubicaciones: `pasos_alta/paso_cuenta.dart:80-98` (clave no contiene celular/documento, «las mismas dos comprobaciones que hace el servidor»); `dominio/validaciones.dart:70-75` (edad por `inDays / 365.25`, falla en el cumpleaños 18); `pasos_alta/paso_datos.dart:141` (otra regla de 18 años por fecha)
- Impacto: dos implementaciones de `PoliticaDeClave` y de mayoría de edad que pueden divergir; error de borde en el día del cumpleaños.
- Causa de fondo: ver solución.
- Solución de fondo: comparar por fecha civil; considerar exponer la política desde el servidor.
- Relación con el flujo nuevo: ninguna
- Evidencia: ver ubicaciones.
- Cómo se comprobó: ver ubicaciones.
- Conexiones y módulos afectados: ver ubicaciones.
- Estándares y normas: ISO/IEC 25010 Mantenibilidad.
- Esfuerzo estimado: chico

## 6. Matriz de contratos

Pendiente.

## 7. Evidencia obligatoria del módulo

Pendiente.

## 8. Cobertura por clase

Pendiente.

## 9. Lo que está bien

- No hay confianza en cabeceras de identidad sin firmar: `grep -rniE 'X-User|X-Usuario|X-Actor|X-Rol'` sobre identidad, cumplimiento y `comun-web` → sin resultados; la identidad se resuelve del JWT validado (`SesionDeLaPeticion.java:66-84`).
- El emisor de tokens falla al arrancar sin clave en `staging`/`production` (`EmisorDeAcceso.java:75-79`).

## 10. Observado fuera de alcance

- `servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU40EvaluarLimites.java:80,132` — `reloj.ahora().atOffset(ZoneOffset.UTC)` para la vigencia de límites (mismo patrón que IDCU-DEFECTO-003; lote L1).

## 11. Límites de esta auditoría

- Gradle no corrió: `./gradlew :servicios:identidad:testBarrido` → `BUILD FAILED`, «Received status code 429 from server: Too Many Requests» al resolver dependencias de `buildSrc` (`evidencia/gradle-identidad-testBarrido.txt`). Además la generación jOOQ exige PostgreSQL con el esquema aplicado (`buildSrc/src/main/kotlin/aportaya.jooq.gradle.kts:22`) y el metaprompt prohíbe aplicar migraciones. Todas las pruebas Java del módulo y el barrido de tamaño/umbrales/secretos quedan **NO COMPROBADO**.
- `yarn typecheck` (backoffice) no corrió: «packageManager yarn@4.18.0 … current global version of Yarn is 1.22.22» (Corepack deshabilitado; no se instaló nada). Flutter/Dart no están instalados. **NO COMPROBADO**.
- No se ejecutaron `scripts/inventario_endpoints.py` ni `scripts/matriz_de_pruebas.py` sin `--check`/`--servicio` porque escriben archivos del repo; `inventario_endpoints.py --check` sí corrió.

## 12. Anexo de cobertura de archivos

Pendiente.
