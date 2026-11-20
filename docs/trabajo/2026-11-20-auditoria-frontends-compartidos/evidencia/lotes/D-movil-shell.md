# Lote D — app Flutter `apps/movil`: shell y compartido

Auditoría de solo lectura sobre `origin/test @07fc697`. Lector: subagente D. No se modificó ningún archivo del repo.

## 1) Cobertura

### 1.a `lib/` fuera de `pantallas/` + `pantallas/arranque` + `pantallas/alianzas` (116 archivos, 5 982 líneas, todos leídos de punta a punta)

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

### 1.b Configuración

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

### 1.c Pruebas de `test/unidad` y `test/widget` (112 archivos)

Leídos completos (prueban archivos del lote): `unidad/cliente_refresco_test.dart` (161), `unidad/refresco_movil_test.dart` (81), `unidad/catalogo_de_errores_test.dart` (75), `unidad/guardian_de_sesion_test.dart` (90), `widget/idioma_test.dart` (62). Revisados por búsqueda dirigida (casos y aserciones de idempotencia/«Simular pago»): `widget/cobro_de_recarga_test.dart` (215), `widget/pantalla_recargar_test.dart` (119), `billetera/recargar_inicio_test.dart`.

Inventario (una línea; importan archivos del lote según `grep package:aportaya_movil/...`, NO leídos completos por volumen — declarado):
- unidad: abridor_de_avisos (navegacion), acceso_a_rutas (navegacion), capacidades (infra), cierre_de_sesion, cliente_test, configuracion_test, control_de_bloqueo, diagnostico_api, enlaces_profundos, errores_del_alta/grupos/identidad/qr (errores.dart), estado_de_verificacion (cliente), juez_de_pose (puerto cámara), lector_de_rostro_vision, lectura_de_qr, purga_de_sesion, retorno_de_invitacion, ruta_del_aviso, sesion_observable, sesion_persiste, subida_del_expediente (idempotencia), tope_de_sesion, tutoriales/* (7), usuario_id_del_token, vencimiento_del_documento, vigilante_test.
- widget: acceso_escanear, bloqueo_plataforma, capa_de_bloqueo_* (4), capacidades_ui, cobro_de_recarga, configuracion_invalida, crear_grupo_etapa, datos_entre_pantallas (refrescar_al_cambiar), deep_link, escaner_del_carnet, etapa_y_refirma_del_grupo, haptica, idioma, inicio_de_billetera, lista_del_extracto, pantalla_camara_de_respaldo, pantalla_de_saldo, pantalla_escanear, pantalla_perfil_cerrar_sesion, pantalla_perfil_foto, pantallas_de_h_tema_y_desborde, paso_capturas, pautas_publicas(+golden), pestanas, portada, prueba_de_vida, tour, tutoriales/* (4).
- Sin import del lote (pantallas de módulo): 36 archivos restantes de unidad/widget (p. ej. calidad_de_captura, formatear_monto, iniciar_grupo, pantalla_aportar…).

## 2) Hallazgos candidatos

### D-01 — Clave de idempotencia fija por cuenta y reusada aunque cambie el cuerpo (recarga/retiro/transferencia)
- **Clase:** DINERO · **Severidad:** S2
- **Ubicaciones:** `apps/movil/lib/dominio/cu10_recargar_saldo.dart:17,33-35,45`; `apps/movil/lib/dominio/cu11_retirar_saldo.dart:12,29-31,42`; `apps/movil/lib/dominio/cu12_transferir_saldo.dart:12,30-32,46-48`; `apps/movil/lib/proveedores/idempotencia.dart:11-17,26-28`
- **Cita:** `static String formularioId(String cuentaId) => 'cu10-recarga-$cuentaId';` · `.claveDe(formularioId(cuentaId));` · `cerrar(...)` solo tras éxito; en `catch` no se libera.
- **Comprobación:** lectura completa; la clave sólo se borra en `cerrar` tras `await` exitoso. No existe prueba que cambie el monto/destino/`factorMfa` entre intentos (`grep -rn "claves" test/widget/pantalla_recargar_test.dart test/widget/cobro_de_recarga_test.dart` sólo cubre doble toque y reintento idéntico).
- **Impacto:** tras un rechazo (422 de límite, MFA vencido, timeout), la persona corrige el monto o el destino y la app reenvía **la misma clave con otro cuerpo**. Según cómo el servidor guarde la clave: (a) devuelve la respuesta vieja (otro monto) o (b) rechaza por conflicto de clave **para siempre en esa cuenta** hasta reiniciar la app (el mapa vive en memoria y no se limpia en errores). En retiro, el `factorMfa` nuevo viaja con clave vieja.
- **Causa de fondo:** la clave se asocia a «formulario por cuenta» y no a «intento con este contenido»; no hay regla para regenerar la clave cuando cambia el cuerpo ni cuando el error es definitivo (4xx no reintentable). Requiere confirmar el comportamiento del servidor ante clave repetida con cuerpo distinto (fuera de lote).

### D-02 — «Simular pago» acredita sin webhook; la única barrera es el servidor
- **Clase:** FLUJO-QR · **Severidad:** S2
- **Ubicaciones:** `apps/movil/lib/dominio/cu10_pago_de_prueba.dart:9-15,36-41`; `apps/movil/lib/dominio/traduccion_de_errores.dart:61-63`; `apps/movil/lib/dominio/contexto_de_error.dart:15-17,22-25`; punto de contacto `apps/movil/lib/pantallas/billetera/cobro_de_recarga.dart:41-42`
- **Cita:** `/// CU-10 · «Simular pago» de un QR **de prueba**. Solo existe donde el servidor tiene el proveedor de prueba (TEST); en producción responde 404` · `DefaultApi(dio).pagarRecargaDePrueba(ordenId: ordenId, idempotencyKey: clave)` · botón visible si `orden.cobroQr?.origen == CobroQrRecargaOrigenEnum.PRUEBA`.
- **Comprobación:** lectura; `grep -rn "kReleaseMode\|kDebugMode" lib/dominio/cu10_pago_de_prueba.dart lib/pantallas/billetera/cobro_de_recarga.dart` → sin resultados: no hay guarda de compilación.
- **Impacto:** un release apuntado a un servidor con el proveedor PRUEBA activo (TEST, o un PROD mal configurado) acredita saldo con un toque, sin QR real ni webhook firmado: contradice R-NUEVA-1 si ese saldo puede pagar aportes (ver D-14, `AP-CU21-03`). La operación está en el contrato generado (`pagarRecargaDePrueba`), así que el código llega a producción.
- **Causa de fondo:** atajo de pruebas convertido en operación del contrato público y gobernado sólo por un campo de la respuesta.

### D-03 — Renovación de token sin single-flight compartido (carrera con el interceptor)
- **Clase:** DEFECTO · **Severidad:** S2
- **Ubicaciones:** `apps/movil/lib/dominio/renovar_acceso.dart:11-17`; `apps/movil/lib/dominio/cliente.dart:88-91`; `apps/movil/lib/dominio/refrescador.dart:13-17`; disparo periódico en `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart:63,77`
- **Cita:** `return Refrescador(ref.read(sesionProvider), sinInterceptores).refrescar();` (instancia nueva por llamada) vs `final refrescador = Refrescador(sesion, dioDeRefresco);` en el interceptor.
- **Comprobación:** lectura; `_enVuelo` es campo de instancia, así que el single-flight no se comparte. `unidad/cliente_refresco_test.dart` sólo cubre 10×401 sobre el mismo interceptor.
- **Impacto:** si el vigilante (cada 30 s y al volver al frente) renueva mientras una petición recibe 401, salen dos `POST /sesion/refrescar` con el mismo refresco; con rotación + detección de reuso en el servidor, el segundo revoca la familia y la sesión se cierra sola («me echa de la app»).
- **Causa de fondo:** dos dueños del refresco; además se duplica la construcción del `Dio` sin interceptores (DUP-INT, `cliente.dart:88-89` ≈ `renovar_acceso.dart:14-15`).

### D-04 — Pruebas de `test/billetera` y `test/navegacion` no corren en CI
- **Clase:** PRUEBAS · **Severidad:** S2
- **Ubicaciones:** `apps/movil/package.json:12`; pruebas huérfanas `apps/movil/test/navegacion/vigilante_de_aprobacion_test.dart`, `apps/movil/test/billetera/{comprobante_de_retiro,movimientos_del_periodo,recargar_inicio,resumen_de_pasanakus}_test.dart`
- **Cita:** `"test:front": "flutter test test/unidad test/widget test/contrato test/identidad test/pasanaku --timeout 60s"`
- **Comprobación:** `grep -rn "test/billetera\|test/navegacion" .github/workflows` → sin resultados; CI llama `yarn workspace @aportaya/movil test:front` (`ci.yml:669`) y dos archivos sueltos (`ci.yml:609,646`).
- **Impacto:** 5 suites (incluido billetera, dinero) pueden quedar en rojo sin que nadie se entere; un «CI verde» no las cubre.
- **Causa de fondo:** lista de carpetas escrita a mano en vez de `flutter test` sobre `test/` con exclusiones explícitas.

### D-05 — Release de Android firmado con la clave de depuración
- **Clase:** SEG · **Severidad:** S2
- **Ubicaciones:** `apps/movil/android/app/build.gradle.kts:36-40` (bloque `buildTypes.release`)
- **Cita:** `// TODO: Add your own signing config for the release build.` · `signingConfig = signingConfigs.getByName("debug")`
- **Comprobación:** lectura; CI publica `apps/movil/build/aportaya-test-${{ github.run_number }}.apk` (`ci.yml:127`).
- **Impacto:** cualquier clave debug instala una «actualización» encima; App Links (`autoVerify="true"`) verificados contra una huella de depuración; no es distribuible ni confiable para una app que mueve dinero.
- **Causa de fondo:** plantilla de Flutter sin completar; `TODO` también en `applicationId` (`build.gradle.kts:21`).

### D-06 — Host de App Links por omisión apunta a TEST (IP en sslip.io)
- **Clase:** CONFIG · **Severidad:** S3
- **Ubicaciones:** `apps/movil/android/app/build.gradle.kts:26-27`; `apps/movil/android/app/src/main/AndroidManifest.xml:49-55`
- **Cita:** `(project.findProperty("hostDeLaAppWeb") as String?) ?: "app.aportaya.161.97.85.216.sslip.io"`
- **Comprobación:** lectura.
- **Impacto:** un build sin `-PhostDeLaAppWeb` reclama los enlaces del dominio de TEST; valor de infraestructura incrustado en el código.
- **Causa de fondo:** valor por omisión silencioso (contrario a lo que el propio `cliente.dart:20-27` corrigió para la URL del gateway).

### D-07 — Vocabulario de aprobación humana y habilitación en el shell (APROB)
- **Clase:** APROB · **Severidad:** S3 (identidad/KYC y organizador; confirmar si R-NUEVA-1 alcanza a la admisión a grupos, en cuyo caso `AP-CU69-10` sube a S2)
- **Ubicaciones y citas:**
  - `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart:12` «Se entera solo de que aprobaron la identidad»; `:20` `class VigilanteDeAprobacion`; `:41` `_esperandoAprobacion`; `:19` «con la cuenta ya habilitada».
  - `apps/movil/lib/navegacion/shell.dart:3,72-73` «El vigilante se entera solo de que aprobaron la identidad y activa la cuenta.»
  - `apps/movil/lib/navegacion/aviso_de_verificacion.dart:17` «donde se activa la cuenta cuando la aprueban»; `:44-45` `EstadoDeVerificacionEstadoEnum.APROBADA => (TextosDeVerificacion.avisoAprobada`.
  - `apps/movil/lib/dominio/renovar_acceso.dart:8` «cuando aprueban la identidad».
  - `apps/movil/lib/dominio/errores.dart:37-43` `'AP-CU01-08': '...Actualizá la bandeja antes de intentar otra vez.'`, `'AP-CU01-09': '...Completá la carga antes de aprobar.'`, `'AP-CU01-11': 'El documento venció y no se puede aprobar.'` (textos del revisor del backoffice dentro de la app del participante).
  - `apps/movil/lib/dominio/errores.dart:57` `'AP-CU20-02': 'Tu cuenta todavía no está habilitada para organizar grupos.'`; `:76-77` `'AP-CU69-10': 'Este grupo revisa cada ingreso antes de aceptarlo y por enlace no se puede entrar todavía.'` (admisión con aprobación humana).
  - `apps/movil/lib/dominio/contexto_de_error.dart:29` «necesitás estar habilitado como organizador».
  - `apps/movil/lib/navegacion/guardias.dart:20-24` `exigirHabilitado` (código muerto, ver D-10).
  - `apps/movil/lib/dominio/tutoriales/modelo.dart:17` `Capacidad.organizador` «habilitada para organizar un grupo (CU-90)».
  - Prueba que congela esos textos: `apps/movil/test/unidad/catalogo_de_errores_test.dart:21-27`.
- **Comprobación:** `grep -rn "aprob\|Aprob\|APROB\|habilitad" apps/movil/lib` (excluyendo pantallas).
- **Impacto:** el shell supone una revisión humana diferida (sondeo cada 30 s); `AP-CU69-10` describe un grupo que aprueba ingresos a mano, en conflicto con el flujo nuevo si alcanza al circuito del pasanaku.
- **Causa de fondo:** catálogo de errores único compartido entre backoffice y app; flujo de admisión con revisión.

### D-08 — Catálogo de errores del cliente duplicado con el backend y códigos con doble significado
- **Clase:** FRAGIL / DUP-X · **Severidad:** S3
- **Ubicaciones:** `apps/movil/lib/dominio/errores.dart:14-90,106-125,172-178`; `apps/movil/lib/dominio/errores_catalogados.dart:10-29`; `apps/movil/lib/dominio/traduccion_de_errores.dart:22,50-68`
- **Cita:** `'AP-CU01-07': 'Tu carnet está vencido...'` y, en contexto correo, `'AP-CU01-07' => 'No pudimos enviar un código al correo...'`; `AP-CU01-08..11` significan expediente/backoffice en general y código de correo en `verificacionCorreo`; `final codigo = mapa['codigo'] as String? ?? 'AP-DESCONOCIDO';`; el contexto se deduce del path: `if (path.startsWith('/recargas-de-prueba/'))`, `RegExp(r'/usuarios/[^/]+/verificacion$')`.
- **Comprobación:** lectura completa de los tres archivos.
- **Impacto:** el significado de un código depende de adivinar el endpoint por cadena; renombrar una ruta del backend cambia el texto mostrado en silencio. Los mapas son copia manual de los catálogos de servicios (comentario `errores_catalogados.dart:18-19` remite a `comun-web`).
- **Causa de fondo:** códigos no únicos por operación en el contrato; ausencia de catálogo generado.

### D-09 — Configuración de idioma duplicada y divergente; la prueba valida la que la app no usa
- **Clase:** DUP-INT / PRUEBAS · **Severidad:** S3
- **Ubicaciones:** `apps/movil/lib/localizacion.dart:10,14-15` vs `apps/movil/lib/app_real.dart:18-24`; prueba `apps/movil/test/widget/idioma_test.dart:22-23,60`
- **Cita:** `const idiomasDeLaApp = <Locale>[Locale('es', 'BO'), Locale('es')];` vs `const localesDeAportaYa = <Locale>[localeDeAportaYa];` (sin `es`) y `delegadosDeIdioma = GlobalMaterialLocalizations.delegates` vs lista de 3 delegados.
- **Comprobación:** `grep -rn "idiomasDeLaApp\|delegadosDeIdioma" lib test` → sólo `localizacion.dart`, `test/widget/idioma_test.dart`, `test/capturas/ayudas_de_captura.dart`.
- **Impacto:** `idioma_test` («los idiomas soportados son es_BO y es») pasa aunque la app real no declare `es`; prueba verde sobre código que no corre.
- **Causa de fondo:** reescritura sin borrar la versión anterior.

### D-10 — Código muerto en el shell (sin referencias)
- **Clase:** REESCRITURA · **Severidad:** S3
- **Ubicaciones (grep de referencias vacío fuera del propio archivo):**
  - `apps/movil/lib/navegacion/guardias.dart:6-28` (`Guardias`, `guardiasProvider`) — además redirige a `/identidad/aumentar-limite` y `/soporte/servicio-no-habilitado`, rutas que no existen.
  - `apps/movil/lib/dominio/tutoriales/navegacion_del_tutorial.dart:1-15` `llegarA` — clon divergente de `motor.dart:136-146` (muestra la ruta interna «No pudimos abrir «$ruta»», lo que `textos_del_motor.dart:9` prohíbe).
  - `apps/movil/lib/proveedores/conexion.dart:7-18` (`conectividadProvider`, `hayConexionProvider`): el comentario promete «bloquear operaciones de dinero con motivo visible», pero nadie lo consume.
  - `apps/movil/lib/proveedores/dispositivo.dart:13-29` (`dispositivoIdProvider`, clave `dispositivo.id`) — paralelo a `huella_de_dispositivo.dart` (clave `dispositivo.huella`): dos identificadores de dispositivo.
  - `apps/movil/lib/proveedores/tema.dart:7-16` (`temaProvider`); `app_real.dart:47` fija `ThemeMode.system`.
  - `apps/movil/lib/dominio/validacion.dart:16-25` (`carnetValido`, `montoValido`).
  - `apps/movil/lib/proveedores/huella_de_dispositivo.dart:44` (`claveHuellaDeDispositivo`).
  - `apps/movil/lib/main_h6_s3_m8_profile.dart:19` «Punto de entrada temporal» (2026-10-14), casi idéntico a `integration_test/h6_s3_m8_profile_test.dart`; exento a mano en `scripts/verificar_frontend.py:60`.
- **Comprobación:** `grep -rnw <símbolo> lib test integration_test` para cada uno.
- **Impacto:** guardias y conectividad aparentan controles que no existen; deriva de reglas (dos validadores de monto potenciales, dos ids de dispositivo).

### D-11 — Enlace profundo `aportaya://aporte` lleva a una ruta inexistente
- **Clase:** DEFECTO · **Severidad:** S3
- **Ubicaciones:** `apps/movil/lib/navegacion/enlaces_profundos.dart:16`; aceptado también por avisos push vía `ruta_del_aviso.dart:46-48`
- **Cita:** `'aporte': '/billetera/aportes',`
- **Comprobación:** `grep -n "path:" lib/pantallas/billetera/rutas.dart` → sólo inicio/recargar/retirar/transferir/extracto; ningún `aportes`.
- **Impacto:** un enlace o push de «aporte» (camino de dinero) termina en `errorBuilder` con «Algo salió mal de nuestro lado» (texto genérico de `EstadoError`, no «ruta no encontrada»).

### D-12 — Identidad requerida sólo para una lista parcial de rutas de dinero
- **Clase:** FRAGIL · **Severidad:** S3 (el servidor autoriza; es coherencia de UX)
- **Ubicaciones:** `apps/movil/lib/navegacion/acceso_a_rutas.dart:17-22`; rutas no listadas en `apps/movil/lib/pantallas/pasanaku/rutas_operacion.dart:7,15,25,44` (retiro del grupo, traspaso, subasta, permuta)
- **Cita:** `RegExp(r'^/billetera/(recargar|retirar|transferir)$'), ... RegExp(r'^/pasanaku/obligaciones/[^/]+/aportar$')`
- **Impacto:** quien no verificó identidad entra a retiro/traspaso/subasta y recibe el rechazo del servidor en vez de la pantalla que explica qué falta. Las rutas son cadenas repetidas entre `acceso_a_rutas.dart`, `shell.dart:40-58`, `rutas.dart:59,74,89` y `enlaces_profundos.dart:14-17` (`'/billetera/inicio'` aparece en 4 archivos).

### D-13 — `!` sobre el id de usuario al construir una ruta
- **Clase:** DEFECTO · **Severidad:** S3
- **Ubicación:** `apps/movil/lib/navegacion/rutas.dart:101-103`
- **Cita:** `PantallaDeEstadoDeVerificacion(usuarioId: sesion.usuarioIdActual!, sesionActiva: true)`
- **Impacto:** un token sin `sub` legible (o la ruta abierta por enlace en la carrera de cierre de sesión) tira una excepción en el builder. `usuarioIdDelToken` devuelve `null` en esos casos (`reclamos_del_token.dart:4-8`).

### D-14 — Texto de cobro de aporte contra saldo de billetera (camino distinto del QR)
- **Clase:** FLUJO-QR · **Severidad:** S3 (a confirmar con el lote del backend)
- **Ubicación:** `apps/movil/lib/dominio/errores.dart:88`
- **Cita:** `'AP-CU21-03': 'No pudimos cobrar. Revisá tu saldo e intentá de nuevo.'`
- **Impacto:** indica que CU-21 cobra el aporte desde el saldo; combinado con D-02 (saldo acreditado sin webhook en TEST) el flujo del pasanaku se mueve sin QR confirmado.

### D-15 — Progreso de tutoriales no se borra al cerrar sesión; el comentario dice lo contrario
- **Clase:** DOC / DATOS · **Severidad:** S3
- **Ubicaciones:** `apps/movil/lib/infraestructura/almacen_de_progreso_seguro.dart:14-16`; `apps/movil/lib/proveedores/sesion.dart:143-147`
- **Cita:** «el almacén seguro se limpia junto con la sesión — el avance de los tutoriales de una persona no queda en el teléfono después de que se fue» vs `_borrarTokens()` borra sólo `sesion.acceso`, `sesion.refresco`, `sesion.inicio-con-credencial`.
- **Comprobación:** `grep -rn "claveDeProgreso\|deleteAll" lib test` → nadie borra `tutoriales.progreso`.
- **Impacto:** la siguiente persona en el mismo teléfono hereda el avance (guía de inicio no se le ofrece).

### D-16 — Protección de pantalla por mixin: el `dispose` de una pantalla apaga la protección de la de abajo
- **Clase:** DEFECTO · **Severidad:** S3
- **Ubicaciones:** `apps/movil/lib/dominio/proteccion_de_pantalla_dinero.dart:11-27`; usado por 5 pantallas (`pantalla_sesion`, `pantalla_retirar`, `pantalla_extracto`, `pantalla_recargar`, `pantalla_transferir`); `apps/movil/lib/infraestructura/android/proteccion_pantalla_android.dart:14-28`
- **Cita:** `void dispose() { _proteccion.desactivar(); ...}` sin contador; el puerto dice «`go_router` lo activa y desactiva por ruta» (`dominio/puertos/proteccion_pantalla.dart:2-3`) y el mixin dice «`navegacion/` (congelado) no activa nada por ruta todavía» (`proteccion_de_pantalla_dinero.dart:8-10`).
- **Impacto:** al volver de una pantalla protegida apilada sobre otra protegida (p. ej. extracto desde recargar), la de abajo queda capturable. `PlatformException` del canal no se atrapa (sólo `MissingPluginException`) y `activar()` corre sin `await` en un post-frame: error no manejado. La pantalla de saldo (`PantallaDeSaldo`) no usa el mixin (grep).

### D-17 — `AvisosPush` Android declarado «soportado» pero nunca entrega nada
- **Clase:** HUECO · **Severidad:** S3
- **Ubicaciones:** `apps/movil/lib/infraestructura/capacidades.dart:79-92`; `apps/movil/lib/infraestructura/android/avisos_push_android.dart:7-14,20,34`
- **Cita:** «El hueco de `AvisosPush` (sin SDK de Firebase todavía) … sigue siendo "soportado"»; `final _toques = StreamController<String>.broadcast();` sin ningún `add` ni `setMethodCallHandler`.
- **Impacto:** `AbridorDeAvisos` (`navegacion/abridor_de_avisos.dart`) y `ruta_del_aviso.dart` escuchan un flujo que nunca emite en ninguna plataforma; el mapa de capacidades miente para Android.

### D-18 — Cámara de desarrollo y puente HTTP sin guarda de modo release
- **Clase:** CONFIG · **Severidad:** S3
- **Ubicaciones:** `apps/movil/lib/infraestructura/plataforma.dart:76-84`; `apps/movil/lib/infraestructura/camara_prestada.dart:20-47`; `apps/movil/lib/dominio/foto_del_puente.dart:11-25`
- **Cita:** «Sin la bandera —o sea, en cualquier build que no sea de desarrollo— esta rama no existe.» pero la condición es sólo `_camaraDeDesarrollo.isEmpty`; `(cliente ?? Dio()).get<List<int>>(url, ...)` sin validar esquema/host; `on Object { return null; }`.
- **Impacto:** un release compilado con `CAMARA_DEV` toma la foto del carnet de un HTTP arbitrario, fuera de `validarGateway`. Riesgo de build, no de usuario.

### D-19 — Inconsistencia de `generacionDeSesionProvider` y lecturas con `r.data!`
- **Clase:** FRAGIL · **Severidad:** S4
- **Ubicaciones:** `apps/movil/lib/dominio/cu13_cuenta_propia.dart:42-51` (no observa `generacionDeSesionProvider`, a diferencia de `cu13_consultar_saldo.dart:14,30`, `cu15_emitir_extracto.dart:35`, `cu10_*`, `cu11_*`, `cu12_*`); `r.data!` en `cu13_consultar_saldo.dart:20,37`, `cu13_cuenta_propia.dart:51`, `cu15_emitir_extracto.dart:41`.
- **Impacto:** un 204/cuerpo vacío produce `TypeError` en vez de `ErrorDeApi` (se muestra el genérico); la cuenta propia puede sobrevivir a un cambio de sesión si algo la mantiene viva.
- **Causa:** `CuentaPropia` escrito a mano porque «el cliente Dart generado todavía no trae esta operación» (`cu13_cuenta_propia.dart:13-15`); saldo/movimientos con `dio.get` crudo en vez de `DefaultApi` (rutas `'/billetera/$cuentaId/saldo'` como cadena).

### D-20 — Moneda fija y validación de monto con `double`
- **Clase:** DINERO · **Severidad:** S4
- **Ubicaciones:** `apps/movil/lib/dominio/cu10_recargar_saldo.dart:41`, `cu11_retirar_saldo.dart:37`, `cu12_transferir_saldo.dart:42` (`DineroMonedaEnum.BOB`); `apps/movil/lib/dominio/cu13_cuenta_propia.dart:8` (`'BOB'` como cadena, segunda fuente); `apps/movil/lib/dominio/validacion.dart:24` `final n = double.parse(valor); // permitido`
- **Impacto:** importes viajan como `String` con moneda (correcto); pero la moneda es literal en 4 lugares con dos tipos distintos (enum y cadena), y `montoValido` usa `double` (además está muerto, D-10). No hay cálculo de dinero en `double` dentro del lote.

### D-21 — Hidratación en tema claro fijo y nombre de producto inconsistente
- **Clase:** UX-A11Y / COHERENCIA · **Severidad:** S4
- **Ubicaciones:** `apps/movil/lib/app.dart:141,145` (`title: 'AportaYa'`, sólo `Tokens.claro`); `apps/movil/lib/app_real.dart:41` (`title: 'Aporteo'`); `AndroidManifest.xml:11` y `Info.plist:10` (`Aporteo`); `Info.plist:88-90` y `pantalla_configuracion_invalida.dart:31` (`AportaYa`); `pantalla_configuracion_invalida.dart:16-17` `MaterialApp` sin `theme`/`darkTheme`/`locale`.
- **Impacto:** destello claro al arrancar en modo oscuro (regla 95.5.4); tres nombres de producto.

### D-22 — Permisos de iOS con texto incompleto/engañoso
- **Clase:** DOC · **Severidad:** S4
- **Ubicación:** `apps/movil/ios/Runner/Info.plist:88-90`
- **Cita:** «AportaYa guarda la foto de tu documento solo si elegis tomarla desde tus fotos…»; cámara «para fotografiar tu documento y tu cara».
- **Impacto:** la galería y la cámara también se usan para leer QR de grupos (`lector_de_qr_en_galeria.dart`, `visor_qr_mobile_scanner.dart`); el texto no lo dice. iOS no declara Associated Domains para `/g/` (Android sí).

### D-23 — Drivers de integración clonados y con rutas a `docs/trabajo` fijas
- **Clase:** DUP-INT · **Severidad:** S4
- **Ubicaciones:** `apps/movil/test_driver/h10_s2_m6_selectores_ime_test.dart`, `h10_s4_m1_logs_test.dart`, `h10_s4_m3_m4_privacidad_test.dart`, `h3_s1_m3_teclado_test.dart`, `h3_s3_m3_selectores_reales_test.dart`, `h4_s2_m4_visual_test.dart` (6 copias idénticas de 5 líneas); `test_driver/integration_test.dart:18` y `h6_s3_m8_profile_test.dart:18` escriben en `../../docs/trabajo/2026-10-11-alta-atlas-sesion-premium/evidencia/...`.
- **Impacto:** el driver genérico guarda siempre la captura «H5.S4.M3 samsung-a06» y falla si hay ≠1 captura.

### D-24 — Configuración y comentarios desactualizados
- **Clase:** DOC · **Severidad:** S4
- **Ubicaciones:** `apps/movil/analysis_options.yaml:9` excluye `lib/tokens/tokens.dart` (no existe en `git ls-files`); `apps/movil/package.json:15` instala `patrol_cli 3.11.0` con `patrol: ^3.15.0` en `pubspec.yaml:88`; `apps/movil/test/unidad/cliente_refresco_test.dart:86-88` «Hoy `_TrazaYSesion._refrescar()` llama `POST /sesion/refrescar` una vez por cada error» (falso desde `Refrescador`); `apps/movil/lib/infraestructura/plataforma.dart:28-31` «ÚNICO lugar donde se pregunta por la plataforma» (también `capacidades.dart:119`, `camara_en_vivo_plugin.dart:60,150`, `lector_de_rostro_mlkit.dart:75,79,93`); `apps/movil/lib/dominio/validacion.dart:12` «empieda».

### D-25 — Nombre `capacidadesProvider` definido dos veces con tipos distintos
- **Clase:** FRAGIL · **Severidad:** S4
- **Ubicaciones:** `apps/movil/lib/proveedores/capacidades.dart:13` (`Provider<Capacidades>`, plataforma) y `apps/movil/lib/proveedores/tutoriales.dart:30` (`FutureProvider<Set<Capacidad>>`, tutoriales)
- **Impacto:** importar los dos archivos produce conflicto de nombres; las pruebas (`guardian_de_sesion_test.dart`, `portada_test.dart`) sobreescriben uno u otro según el import. `_uuidV4` clonado en `proveedores/idempotencia.dart:34-41` y `proveedores/dispositivo.dart:22-29`; clave `'dispositivo.huella'` repetida en `sesion.dart:13` y `huella_de_dispositivo.dart:8`.

### D-26 — Errores tragados sin rastro en el shell
- **Clase:** OBSERV · **Severidad:** S4
- **Ubicaciones:** `apps/movil/lib/navegacion/vigilante_de_aprobacion.dart:84-86` (`on Object {}`), `apps/movil/lib/dominio/cierre_de_sesion.dart:30,33-40,63-66`, `apps/movil/lib/proveedores/sesion.dart:80-82` (falla del almacén seguro ⇒ sesión anónima sin registro), `apps/movil/lib/proveedores/tutoriales.dart:70-72,137-139`, `apps/movil/lib/dominio/refrescador.dart:32-34`; el refresco sale sin `x-request-id` (Dio sin interceptores, `cliente.dart:88`).
- **Impacto:** fallos de Keychain/Keystore o del refresco no dejan correlación para diagnóstico (no hay monitoreo de errores en el cliente).

## 3) Lo que está bien (preservar)
- `dominio/configuracion.dart` + `main.dart:14-16`: sin URL por omisión; release exige https y host de lista; la app no crea cliente HTTP con configuración inválida.
- Tokens sólo en almacén seguro (`proveedores/sesion.dart:28,48-49`), Keychain `unlocked_this_device` (`ios/almacen_seguro_ios.dart:12-14`); tope absoluto de 8 h y bloqueo por inactividad con reloj inyectado y retroceso de reloj tratado como vencido (`vigilante_de_sesion.dart:31-32`, `sesion.dart:171-179`).
- Refresco single-flight en el interceptor con un solo reintento y cierre idempotente (`cliente.dart:116-141`, `refrescador.dart`), con prueba de carrera real.
- Importes como `String` + `Dinero(moneda:)`; la app nunca suma saldo en memoria: relee `saldoProvider` (`cu10_pago_de_prueba.dart:41`, `cu13_consultar_saldo.dart:10-11`). `Idempotency-Key` con `Random.secure()`.
- Errores nunca muestran el texto del backend: catálogo + genérico por estado; 5xx dice «no es tu conexión» (`errores.dart:136-141`).
- Logs de rechazo sólo en debug y con ruta anonimizada (`diagnostico_api.dart:25-35`).
- Validación estricta de enlaces profundos, QR y rutas de push (`retorno_de_invitacion.dart`, `ruta_del_aviso.dart`, `lectura_de_qr.dart:8,23-31`, `acceso_a_rutas.dart:83-94`): sin redirección abierta, rutas desconocidas privadas por omisión.
- `ProviderScope(retry: ... => null)` (`main.dart:22`): sin reintentos silenciosos en dinero.
- Movimiento reducido respetado en la transición de pestañas (`shell.dart:158-161`).
- AndroidManifest: `FileProvider` no exportado, sin `usesCleartextTraffic`; Info.plist sin excepciones ATS.

## 4) Fuera de lote (vistos al pasar)
- `apps/movil/lib/pantallas/billetera/cobro_de_recarga.dart:23,41-42` — visibilidad del botón «Simular pago» por `origen == PRUEBA`.
- `apps/movil/lib/pantallas/identidad/dominio/borrador_alta.dart:112` y `apps/movil/lib/pantallas/identidad/pasos_alta/paso_perfil_transaccional.dart:85,107` — `double.tryParse` sobre `montoMensualEstimado`/`monto` (DINERO).
- `apps/movil/lib/pantallas/pasanaku/pantalla_invitar.dart:47` — teléfono validado con `^\+591\d{8}$`, distinto de `telefonoValido` (`^[67]\d{7}$`) en `dominio/validacion.dart:13` (regla duplicada divergente).
- `apps/movil/test/billetera/comprobante_de_retiro_test.dart` — palabra «comprobante» (posible APROB) y suite fuera de CI (D-04).
- `apps/movil/lib/pantallas/pasanaku/dominio/habilitacion_basica.dart`, `solicitudes_del_grupo.dart`, `cu68_postular_al_grupo.dart` — nombres de habilitación/solicitudes/postulación (APROB) para el lector de pasanaku.
- Backend: confirmar qué hace el servidor con `Idempotency-Key` repetida y cuerpo distinto (D-01) y si `/recargas-de-prueba/{id}/pago` está apagado en PROD (D-02).
- `scripts/verificar_frontend.py:60` — exención por nombre de archivo para `main_h6_s3_m8_profile.dart`.
