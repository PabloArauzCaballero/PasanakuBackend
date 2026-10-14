# Notas — alta-atlas-sesion-premium

## Base
- Rama: `codex/alta-atlas-sesion-premium-2026-10-11` · base `080420903b62ce396751ab396ca54c2f23fb0c2b` (`origin/dev`).
- Diferencias con el Anexo C: 221 archivos comparados tras normalizar saltos de línea; 0 diferencias de contenido.
- Diferencias del entorno: Flutter 3.47.5 / Dart 3.13.4, mientras el plan pide Flutter 3.44.x; se mantendrá el SDK disponible y se verificará compatibilidad, sin degradar globalmente el entorno.
- Estado previo preservado: `docs/Views/AportaYa-Maqueta.html` estaba modificado antes de iniciar esta tarea.

## Diferencia con el prompt original
- El prompt de escritorio (`C:\Users\leonardo\Desktop\pasanaku\PLAN-alta-atlas-sesion-premium-2026-10-11.md`) declara 210 microtareas. La copia de trabajo contiene esos mismos 210 IDs y agrega tres tareas H0 verificables: `H0.S1.M8` (generación local de clientes OpenAPI), `H0.S2.M8` (lectura UTF-8 del gate frontend en Windows) y `H0.S2.M9` (conteo correcto del plan). El contador de esta copia mide 213; las tres adicionales están HECHO. El prompt original de escritorio se dejó intacto.
- Conflicto de duración en H8.S3.M10: el plan propone rotación del reintento en 400 ms, mientras D4 `pasanaku-movimiento` impone menos de 320 ms al movimiento disparado por una persona. Prevalece la regla de la casa; si se implementa, será una sola rotación de 360° en 300 ms lineales, sin bucle. Registrado en `evidencia/h8-oportunidades.md`.

## Baseline (H0)
| Comando | Resultado | Fallas preexistentes |
|---|---|---|
| `flutter --version` | Flutter 3.47.5 / Dart 3.13.4; no coincide con 3.44.x solicitado | Desviación del entorno, guardada en `evidencia/h0-flutter-version.txt` |
| `dart analyze --fatal-infos` (baseline inicial antes de generar los clientes OpenAPI locales) | 232 issues, exit 3 | Los clientes ignorados por Git aún no estaban generados; salida en `evidencia/h0-analyze.txt` |
| `dart format --output=none --set-exit-if-changed lib test` | Exit 1; 4 archivos requieren formato | `dominio/errores.dart`, `pantallas/identidad/dominio/estado_de_verificacion.dart`, `test/unidad/errores_identidad_test.dart`, `test/unidad/estado_de_verificacion_test.dart` |
| `python ..\..\scripts\verificar_frontend.py movil` | Tras corrección UTF-8, 1 falla | `pantallas/identidad/dominio/estado_alta.dart` tiene 247 líneas; falla previa del límite 200 |
| `flutter pub get` en `packages/diseno_flutter` y `apps/movil` | Ambos exit 0 | No se detectaron fallas de resolución |
| Análisis textual Anexo C vs `apps/movil/lib` | 221 archivos comparados; 0 diferencias de contenido | Ninguna; solo saltos de línea Windows en hashes crudos |
| Comando | Resultado | Fallas preexistentes |
|---|---|---|
| `flutter test test/unidad test/widget test/contrato test/identidad test/pasanaku --timeout 60s` | 267 pasaron, 24 fallaron; exit 1 | Fallas de carga de `consultar_saldo_test.dart`, `autenticar_test.dart`, `estado_alta_test.dart`, `pantalla_contrato_test.dart` y otras 20; log completo en `evidencia/h0-test-front.txt` |
| `flutter test test/a11y --timeout 60s` | 2 pasaron, 1 falló; exit 1 | `pantalla_de_saldo_a11y_test.dart` no cargó por error de compilación |
| `flutter test test/goldens --timeout 60s` | 0 pasaron, 8 fallaron; exit 1 | `pantalla_de_saldo_golden_test.dart` no cargó; `pantalla_escanear_golden_test.dart` tuvo diferencias en claro/oscuro, escritorio/móvil; log en `evidencia/h0-goldens.txt` |
| `flutter test` en `packages/diseno_flutter` | 64 pasaron, 15 fallaron; exit 1 | Fallas en `test/goldens/catalogo_golden_test.dart` (Moléculas/Móviles, claro/oscuro) y 11 más; salida en `evidencia/h0-diseno.txt` |

La primera corrida de las suites usó clientes OpenAPI no generados en el checkout y reportó errores de compilación. Tras generarlos, `dart analyze --fatal-infos` dio `No issues found!`; repetir las suites de H0 con ese entorno antes de clasificar sus fallas como preexistentes.

El contador del plan pasó `python .claude/hooks/plan_status.py --self-test`: 31 PASS, 0 FAIL. Se agregó cobertura para contar `S0.M1` e ignorar IDs repetidos dentro de anexos.

Skills leídas para H0: `factual-discovery`, `windows-dev-environment`, `agent-resource-control`, `flutter-testing`. Confirmaron discovery antes de editar, rutas/encoding UTF-8 explícitos, una tarea pesada por vez y pruebas seriales por nivel.

### Corrección del entorno de generación (H0)
La tarea de CI `generateOpenApiClients` es el generador oficial desde los contratos; `apps/movil/scripts/generar-clientes.sh` solo corre build_runner y no podía producir tipos con los clientes fuente ausentes. Tras ejecutar ambos pasos, `dart analyze --fatal-infos` pasó con `No issues found!`; evidencia en `evidencia/h0-openapi-generator.txt` y `evidencia/h0-analyze-after-clients.txt`.

## Skills leídas
### Skill: skills-router
- Mapea las skills obligatorias por tipo de tarea y ordena su precedencia.
- Para esta app exige combinar las skills generales de disciplina con Flutter, identidad, privacidad, seguridad y QA.
- Los gates de evidencia, privacidad, seguridad y UI aplican antes de cerrar el trabajo.

### Skill: evidence-and-verification
- La fuerza de las afirmaciones debe corresponder al peldaño de evidencia alcanzado.
- Las salidas deben ser literales, posteriores al último cambio y acompañadas por lo que no cubren.
- UI móvil requiere observar la app real; analizar o compilar no basta.

### Skill: scope-discipline
- Mantener cada cambio ligado a un requisito explícito y declarar alcance antes de cada slice.
- No arreglar problemas ajenos al alcance; registrarlos con evidencia.
- Revisar el diff archivo por archivo y verificar otra vez después de la limpieza.

### Skill: milestone-planning
- Cada microtarea necesita criterio binario y una verificación concreta.
- El porcentaje sale de HECHO/total y se actualiza al cambiar el estado.
- El plan se mantiene sincronizado con el trabajo real y los desvíos.

### Skill: pasanaku-diseno
- Reutilizar los tokens, componentes y catálogo existentes de `packages/diseno_flutter`.
- Selectores y fechas deben pasar por los componentes propios; evitar valores visuales literales.
- Las pantallas requieren análisis, pruebas, gates y capturas reales en tamaños y temas relevantes.

### Skill: pasanaku-app-movil-ux
- `Sesion` y el guardián son la fuente de verdad; las rutas privadas requieren sesión válida.
- El teclado, áreas táctiles de 48 dp, permisos, fallos de hardware y estados vacíos/error se diseñan explícitamente.
- Cámara y biometría necesitan dispositivo real; la web debe degradar con sentido.

### Skill: mobile-native
- Sus principios de safe area, teclado, foco táctil y gesto se traducen a Flutter mediante APIs nativas del framework.
- No desactivar zoom ni sustituir accesibilidad por detección del dispositivo.
- La emulación no demuestra el tacto real; reportar qué necesita hardware.

### Skill: apple-design
- Gestos directos deben seguir el dedo, conservar velocidad y permitir interrupción.
- Usar resortes con moderación, continuidad espacial y reducción de movimiento accesible.
- La respuesta táctil y háptica debe coincidir con la acción que la causa.

### Skill: emil-design-eng
- Cada animación necesita propósito; frecuencia, curva, duración e interrupción se consideran antes de implementarla.
- Preferir feedback rápido y explícito; evitar movimiento decorativo repetido o costoso.
- Revisar rendimiento, estados accesibles y el resultado visual con capturas.

### Skill: frontend-responsive-layout (H8.S4.M4, 2026-10-13)
- Elegir el punto de apilado donde el contenido deja de ser legible, no por nombre de dispositivo.
- Consultar el ancho local del formulario con `LayoutBuilder`; el mismo viewport global puede alojar el campo en contenedores distintos.
- Re-capturar y revisar visualmente el mismo viewport/tema/estado después de corregir el layout.

### Skills aplicadas a H8.S4.M4 (2026-10-13)
- `frontend-responsive-layout`: elegir el punto de apilado donde el contenido deja de ser legible y consultar el ancho local con `LayoutBuilder`.
- `frontend-forms-ux`: campos relacionados de formularios largos pueden apilarse; conservar la ayuda junto al campo.
- `frontend-accessibility`: mantener etiquetas visibles y comprobar el formulario con escala de texto ampliada.
- `pasanaku-diseno`: reutilizar `Campo`/`CampoDeSeleccion`, sus roles y el comportamiento predeterminado fuera de esta ayuda larga.
- `flutter-testing`, `visual-proof` y `visual-regression-testing`: hacer regresión reproducible, revisar la captura y re-capturar la misma celda tras corregir.

### Resultados definitivos tras generar clientes OpenAPI

Las primeras suites de H0 se ejecutaron antes de reconstruir los clientes Dart ignorados por Git; esos fallos de compilación no son baseline válido. Luego de `generateOpenApiClients`, el análisis terminó limpio y las suites se repitieron serialmente:

| Suite | Resultado repetido | Clasificación / evidencia |
|---|---|---|
| App: unidad, widget, contrato, identidad y pasanaku | 373 aprobados, 3 fallidos | Los tres son los rojos intencionales H0.S3.M1–M3: dos de guardián y el refresco móvil. `evidencia/h0-test-front-post-clientes.txt` |
| A11y | 4/4 aprobados | `evidencia/h0-a11y-post-clientes.txt` |
| Goldens móviles | 0/9 aprobados | Saldo éxito claro/oscuro; escáner cámara denegada escritorio claro/oscuro y 5 pruebas más (nombres en el log completo). `evidencia/h0-goldens-post-clientes.txt` |
| Sistema de diseño | 64 aprobados, 15 fallidos | 4 catalog golden visibles y 11 pruebas adicionales; log completo en `evidencia/h0-diseno-post-clientes.txt` |

### H1.S1 — sesión observable

Skills leídas antes de la implementación: `authn-identity`, `auth-session-pentest`, `flutter-state-architecture`, `frontend-navigation-ia`, `frontend-ux-states`, `error-handling-contract`, `data-privacy-financial`, `pasanaku-app-movil-ux`, `apple-design`, `mobile-native`. Implementada `Sesion` observable con estado desconocido/anónimo/activo, hidratación fail-closed, tope absoluto configurable de 8 h, inicio inmutable durante refresh, motivo de salida de un solo uso y generación para invalidar estado de cuenta. La prueba del contrato de refresco móvil sigue roja hasta H1.S6, como se planificó.

### H1.S3 — cierre y purga

`Sesion.cerrar` comparte un único Future cuando llegan 401 simultáneos, de modo que los 5 errores producen un solo borrado y una sola notificación. Los providers de saldo, extracto, mutaciones de billetera, bandeja de avisos, estados/operaciones de Pasanaku, KYC, perfil, contraseña, PEP, alta/contrato y subida observan `generacionDeSesionProvider`; al cambiar de actor Riverpod reconstruye su estado. Verificación: el saldo de A consulta una vez; cerrar y abrir B provoca segunda consulta en vez de reutilizar la primera. Tests focalizados y análisis limpios en `evidencia/h1-s3-m4.txt` y `h1-s3-analyze.txt`.

## Inventarios
### Rutas (H1.S2.M1)
Inventario por `rg -n "path: '" apps/movil/lib --glob '*.dart'`: 41 declaraciones, 40 rutas únicas; `/identidad` aparece dos veces (la ruta de identidad y el marcador del shell). Las rutas de identidad y alta se definieron como entrada/privadas según el flujo real. `/pasanaku/unirse/:codigo` es pública: la prueba de deep link ya demuestra que su vista de invitación incluye la opción de ingresar y no necesita el shell.

| Ruta declarada | Acceso previsto | Archivo |
|---|---|---|
| /pasanaku | privada | navegacion/rutas.dart |
| /identidad | privada (declarada dos veces; revisar colisión al probar árbol) | navegacion/rutas.dart + pantallas/identidad/rutas.dart |
| /notificaciones/bandeja | privada | pantallas/notificaciones/rutas.dart |
| /billetera/inicio | privada | pantallas/billetera/rutas.dart |
| /billetera/recargar | privada | pantallas/billetera/rutas.dart |
| /billetera/retirar | privada | pantallas/billetera/rutas.dart |
| /billetera/transferir | privada | pantallas/billetera/rutas.dart |
| /billetera/extracto | privada | pantallas/billetera/rutas.dart |
| /pasanaku/grupos/nuevo | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/grupos/:grupoId/pedir-cupo | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/grupos/:grupoId/invitar | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/grupos/:grupoId/retiro | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/turnos/permuta | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/sorteos/:sorteoId/verificar | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/grupos/:grupoId/transparencia | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/mi-estado | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/mi-puntaje | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/sorteos/:sorteoId/turno | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/obligaciones/:obligacionId/aportar | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/reclamos/nuevo | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/escanear | privada | pantallas/pasanaku/rutas.dart |
| /pasanaku/unirse/:codigo | pública | pantallas/pasanaku/rutas.dart |
| /identidad | privada (ruta autenticada) | pantallas/identidad/rutas.dart |
| /identidad/verificacion-basica | pública | pantallas/identidad/rutas.dart |
| /identidad/mfa | pública (mover fuera del shell en M2) | pantallas/identidad/rutas.dart |
| /identidad/dispositivos | privada | pantallas/identidad/rutas.dart |
| /identidad/verificacion-profunda | privada | pantallas/identidad/rutas.dart |
| /identidad/perfil | privada | pantallas/identidad/rutas.dart |
| /identidad/contrasena | privada | pantallas/identidad/rutas.dart |
| /identidad/baja | privada | pantallas/identidad/rutas.dart |
| /portada | soloSinSesion | pantallas/identidad/rutas.dart |
| /tour | soloSinSesion | pantallas/identidad/rutas.dart |
| /ingreso | soloSinSesion | pantallas/identidad/rutas.dart |
| /registro | soloSinSesion | pantallas/identidad/rutas.dart |
| /registro/camara | pública | pantallas/identidad/rutas.dart |
| /registro/prueba-de-vida | pública | pantallas/identidad/rutas.dart |
| /registro/subida | pública | pantallas/identidad/rutas.dart |
| /registro/estado | pública | pantallas/identidad/rutas.dart |
| /identidad/contrato | pública | pantallas/identidad/rutas.dart |
| /identidad/bienvenida | pública | pantallas/identidad/rutas.dart |
| /soporte/ayuda | privada (predeterminado seguro) | pantallas/soporte/rutas.dart |
### Providers de cuenta (H1.S3.M3)
Inventario (`rg -n -g '*.dart' '(Notifier|Future|Stream)?Provider(<|\\.)' apps/movil/lib`); clasificados todos los providers declarados (las referencias `.notifier` no cuentan como declaraciones):

| Dominio/archivo | Providers declarados | Clase | Acción al cerrar |
|---|---|---|---|
| `dominio/cu10_recargar_saldo.dart`, `cu11_retirar_saldo.dart`, `cu12_transferir_saldo.dart` | `recargarSaldoProvider`, `retirarSaldoProvider`, `transferirSaldoProvider` | Cuenta: mutaciones con importes | Invalidar por generación; limpiar resultado/error del formulario |
| `dominio/cu13_consultar_saldo.dart`, `cu15_emitir_extracto.dart` | `saldoProvider`, `extractoProvider` | Cuenta: datos financieros sensibles | Invalidar por generación |
| `pantallas/notificaciones/proveedor_bandeja.dart` | `bandejaProvider`, `noLeidasProvider` | Cuenta: bandeja personal | Invalidar por generación |
| `pantallas/pasanaku/dominio/*` | `crearGrupoProvider`, `habilitacionOrganizadorProvider`, `puntajeProvider`, `estadoDelParticipanteProvider`, `restriccionVigenteProvider`, `miParticipacionProvider`, `verificarCadenaProvider`, `verificarSorteoProvider`, `paqueteDelSorteoProvider`, `cobrarAporteProvider`, `solicitarPermutaProvider`, `comprometerSorteoProvider`, `revelarSorteoProvider`, `solicitarRetiroProvider`, `invitarAlGrupoProvider`, `postularAlGrupoProvider` | Cuenta: estado/operaciones grupales | Invalidar por generación |
| `pantallas/identidad/dominio/estado_cuenta.dart` | `pepProvider`, `perfilProvider`, `contrasenaProvider`, `bajaProvider` | Cuenta: perfil/seguridad | Invalidar por generación |
| `pantallas/identidad/dominio/estado_de_verificacion.dart` | `verificacionProvider` | Cuenta: KYC | Invalidar por generación |
| `pantallas/identidad/dominio/estado_sesion.dart` | `sesionIdentidadProvider` | Cuenta: formulario de autenticación | Invalidar al cambiar de sesión/cuenta; no guardar credenciales fuera del estado efímero |
| `pantallas/identidad/dominio/estado_contrato.dart` | `contratoProvider` | Alta, antes de existir cuenta | Resetear con `altaProvider` al cerrar |
| `pantallas/identidad/dominio/estado_alta.dart`, `seguimiento_del_alta.dart`, `subida_del_expediente.dart` | `altaProvider`, `seguimientoDelAltaProvider`, `subidaProvider` | Alta/KYC pre-cuenta | Resetear alta parcial al cerrar |
| `proveedores/sesion.dart`, `huella_de_dispositivo.dart` | `sesionProvider`, `generacionDeSesionProvider`, `almacenSeguroProvider`, `relojDeSesionProvider`, `huellaDeDispositivoProvider`, `plataformaProvider` | Sesión/dispositivo | Tokens e inicio se borran; huella deliberadamente persiste |
| `proveedores/biometria.dart`, `dispositivo.dart`, `proteccion_pantalla.dart`, `visor_qr.dart`, `proveedores/haptica.dart`, `camara_identidad.dart` | `biometriaProvider`, `biometriaDisponibleProvider`, `dispositivoIdProvider`, `proteccionPantallaProvider`, `visorQrProvider`, `hapticaProvider`, `camaraProvider` | Dispositivo/hardware | No purgar; conservar la instalación y sus capacidades |
| `dominio/cliente.dart`, `verificacion_contrato.dart`, `identidad/*` servicios (`registro`, `verificacionCorreo`, `subidaDeFotos`, `contratosVigentes`, `contratosDelAlta`, `consultarVerificacion`, `autenticacion`) | `dioProvider`, `verificacionContratoProvider`, `registroProvider`, `verificacionCorreoProvider`, `subidaDeFotosProvider`, `contratosVigentesProvider`, `contratosDelAltaProvider`, `consultarVerificacionProvider`, `autenticacionProvider` | App/servicios de dominio | Mantener DI; no son caches de cuenta, invalidar solo dependencias de sesión si corresponde |
| `proveedores/tutoriales.dart`, `dominio/tutoriales/escribano.dart` | `catalogoDeTutorialesProvider`, `capacidadesProvider`, `registroDeTutorialesProvider`, `almacenDeProgresoProvider`, `progresoDeTutorialesProvider`, `motorDeTutorialesProvider`, `autolanzarGuiaDeInicioProvider`, `verGuiaDeInicioOtraVezProvider` | App/tutoriales | Cancelar guía activa al cerrar; conservar preferencias/progreso local |
| `proveedores/ajustes_del_sistema.dart`, `conexion.dart`, `tema.dart`, `capacidades.dart`, `avisos_push.dart`, `idempotencia.dart`, `guardias.dart` | `ajustesDelSistemaProvider`, `conectividadProvider`, `hayConexionProvider`, `temaProvider`, `capacidadesProvider`, `avisosPushProvider`, `idempotenciaProvider`, `guardiasProvider` | App/dispositivo | No purgar; limpiar solo claves de idempotencia ligadas a mutaciones en curso |

Los declarados dinámicamente con `AsyncNotifierProvider.family` también están incluidos por su nombre. La regla de aislamiento elegida es: providers de `cuenta` observan `generacionDeSesionProvider`; alta pre-cuenta se reinicia al cierre; providers de instalación/dispositivo y preferencias no se atan a una identidad.
### Selects (H3.S3.M1)

Inventario con `rg -n "CampoDeSeleccion|mostrarListaConBuscador|SelectorSegmentado|ChipElegible|GrupoRadio|OpcionSeleccionable" apps/movil/lib -g '*.dart'` (2026-10-12). Los selectores de valor único con listas no visibles ya pasan por `CampoDeSeleccion`; al retirar el umbral, todos abren `HojaDeSeleccion`.

| Pantalla/campo | Opciones | Componente actual | ¿Es select? | Resultado H3 |
|---|---:|---|---|---|
| Alta · lugar de expedición del carnet | 9 departamentos | `CampoDeSeleccion` (`campos_del_alta.dart`) | Sí: valor único en lista | Hoja con buscador |
| Alta · actividad económica | 19 | `CampoDeActividad` → `CampoDeSeleccion` | Sí: valor único en lista extensa | Hoja; admite buscar en texto y detalle |
| Alta · origen de fondos | 7 | `CampoDeSeleccion` (`paso_perfil_transaccional.dart`) | Sí | Hoja con buscador |
| Alta · fecha de vencimiento · día / mes / año | 31 / 12 / 31 | `CampoDeFecha` → `ParteDeFecha` → `CampoDeSeleccion` | Sí para cada parte; calendario es atajo distinto | Hoja para día, mes y año |
| Alta · fecha de nacimiento · día / mes / año | 31 / 12 / hasta 103 según edad admisible | `CampoDeFecha` → `ParteDeFecha` → `CampoDeSeleccion` | Sí para cada parte; calendario es atajo distinto | Hoja para día, mes y año |
| Billetera · medio de recarga | 3 | `SelectorSegmentado` | No: las tres alternativas se muestran juntas | Se conserva |
| Billetera · destino de transferencia | 2 | `SelectorSegmentado` | No: dos alternativas visibles | Se conserva |
| Pasanaku · canal de aporte | 4 | `SelectorSegmentado` | No: alternativas simultáneas, sin lista oculta | Se conserva |
| Pasanaku · canal de invitación | 3 | `SelectorSegmentado` | No: tres alternativas visibles | Se conserva |
| Alta · canal de verificación | 1 habilitado | `SelectorSegmentado` | No: una sola alternativa habilitada | Se conserva |
| Crear grupo · periodicidad | 4 | `GrillaDeOpciones` (tarjetas 2 columnas) | No: las cuatro tarjetas se presentan en la misma pregunta | Se conserva como elección visual simultánea |
| Crear grupo · modalidad de turnos | 3 | `GrillaDeOpciones` (tarjetas 1 columna) | No: tres tarjetas visibles como opciones | Se conserva como elección visual simultánea |
| Pasanaku · acciones de grilla | 2 columnas | `OpcionSeleccionable` | No: acciones/tarjetas de acción, no campo de valor del formulario | Se conserva |
| `ChipElegible`, `GrupoRadio`, `mostrarListaConBuscador` en pantallas | 0 usos encontrados | — | No hay otros consumidores app-side | La fachada se conserva por compatibilidad |

No se encontraron selectores de banco o motivo de soporte en el runtime actual (`apps/movil/lib`). El gate `scripts/verificar_frontend.py movil` ahora busca también `DropdownButton*`, `DropdownMenu`, `PopupMenuButton` y `showMenu(` en `packages/diseno_flutter/lib`; no requiere excepciones de menús de acción porque el barrido actual no encontró ninguno.

### Verificación H3 (2026-10-12)

- `evidencia/h3-s1-s2.txt`: 13 pruebas widget de hoja/campo, 14 variantes de accesibilidad del catálogo, análisis del paquete y del móvil sin issues, gate móvil `TODO OK` y 27 pruebas dirigidas de alta/tutorial/sesión; todos esos comandos finalizaron con código 0.
- En la suite completa de `packages/diseno_flutter`, 69 pruebas pasaron y 18 comparaciones de golden fallaron; el baseline `evidencia/h0-diseno-post-clientes.txt` ya tenía 15 fallos de golden. No regenerar referencias sin inspeccionar cada diferencia. H3 conserva su verificación dirigida en verde, pero no se declara regresión integral verde.
- Las cuatro sobrelongitudes detectadas por el gate se separaron en `avisos_de_sesion.dart`, `modelo_alta.dart`, `notifier_alta.dart`, `pie_del_paso_datos.dart`, `errores_paso_datos.dart` y `navegacion_del_tutorial.dart`; objetivos quedaron en 188, 2, 197 y 200 líneas, y el gate volvió a `TODO OK`.
- H3.M1 no tiene salida roja funcional del código previo (la prueba redactada inicialmente no compilaba antes de existir la nueva API); solo se conserva el PASS posterior. La geometría probada es simulada, no captura de un teclado de Android/iOS. No hay dispositivo/emulador disponible y no se encontró selector de soporte para la tercera captura.

### Canal único del alta (H4.S4)

- La restricción funcional está en `apps/movil/lib/pantallas/identidad/dominio/canales_de_verificacion.dart`: CU-01 del contrato `servicios/identidad/src/main/resources/openapi/identidad.yaml` solo enumera `CORREO` como `canalVerificacion` del alta. Si el contrato agrega `SMS`, ampliar esa constante y el copy traducido en `CanalDeVerificacion`; con una sola alternativa el widget debe seguir mostrando texto y no un selector.
- `evidencia/h4-s4.txt` conserva el rojo original (un `SelectorSegmentado` para la única opción) y el verde tras la corrección.

### Partes del nombre en el alta (H4.S2)

- `DatosPersonales` conserva las cuatro partes escritas y expone `nombres`/`apellidos` armados para no alterar CU-01, el cotejo ni los clientes generados. Los constructores y `copiarCon` heredados que reciben cadenas completas las guardan en la primera parte, sin separarlas por espacios: «José Luis» y «De la Cruz» pueden ser nombres compuestos.
- La distribución responde al ancho local con `LayoutBuilder`; se prueba a 360 y 412 dp, junto con autofill hints, capitalización y orden del foco. Actualización 2026-10-14: las capturas Android del emulador a 360/412 dp y sus verificaciones quedaron registradas; H4.S2.M4 está HECHO. No equivale a QA en teléfono físico. Ver `evidencia/h4-s2-m4-android.md` y la actualización en `evidencia/h4-s2.txt`.

### Placeholders del alta y el ingreso (H4.S1)

- Se agregó el token semántico `textoDeEjemplo` a ambos temas; deriva del rol `text3`, elegido por ser más apagado que el texto escrito y conservar al menos 3:1 sobre el campo. La prueba Node del paquete de tokens mide claro y oscuro. Los generadores se ejecutaron directamente porque el Yarn global es 1.22.22 mientras el repo declara `yarn@4.18.0`; no se habilitó Corepack ni se alteró la instalación de herramientas.
- `Campo`, contraseña, monto, selección, fecha y OTP comparten el estilo/hint. El set de widget tests completo de `diseno_flutter` pasó (52); el alta/login dirigido pasó (18), y el gate móvil devolvió `TODO OK`.
- Se regeneraron y miraron solo los dos goldens del grupo «Átomos · campos» (claro/oscuro): el hint aparece dentro del campo y no desplaza la composición. La suite completa conserva discrepancias históricas en otros grupos (18 en H3; 15 en H0), sin mezclar esos cambios aquí. Ver `evidencia/h4-s1.txt`.

### Inventario de captura del carnet (H5.S1.M1–M3)

- El barrido inicial de `apps/movil/lib/pantallas/identidad` encontró el copy heredado `TextosIdentidad.escribirAMano` y textos de cámara que ofrecían escribir los datos; no había consumidores de esos cuatro textos en runtime. También encontró dos atajos de desarrollo que generaban/reemplazaban las cinco capturas sin cámara (`pantalla_camara.dart` y `pie_de_capturas.dart`). Se retiraron los copy y los atajos.
- Ya existía el puerto `Camara.elegirDeLasFotos()` y `CamaraDelSistema` lo implementa con `ImageSource.gallery`; antes no tenía uso en el alta. Se conectó a la pantalla de cámara cuando el permiso está denegado o bloqueado, y procesa el archivo con el validador actual (sin tocar sus umbrales). La salida de galería devuelve la captura a la cara activa.
- La prueba de widget cubre estados con permiso reintentable/bloqueado, disponibilidad de «Elegir de mis fotos» y ausencia de «Escribir a mano». No se ejecutó Flutter ni build en esta intervención por el límite de recursos; queda pendiente validar compilación, análisis y la integración del selector nativo. «Usar esta foto igual» tras rechazo local no está implementado por la API actual, que rechaza con `capturaDesdeArchivo`; no se cambió calidad de captura.

### Fases y español del alta (H4.S5 / H4.S6)

- El alta ya presenta una cabecera con cuatro nombres de fase; los siete subpasos se asignan a ellas y el retorno conserva el nombre. La separación interna de los campos heredados de `PasoDatos` aún no completa la fase «Tu identidad»; ver `evidencia/h4-s5-componentes.txt`.
- La app configura `es_BO` y el `DatePicker` widget probado muestra octubre y la navegación mensual en español. El test de barrido de literales no encuentra copy inglés visible; los identificadores/valores internos del contrato se conservan. El 13-10 se revisaron manualmente las capturas widget golden del ingreso y de todas las vistas del alta: 0 hallazgos de inglés visible. Son capturas de prueba, no de dispositivo; los diálogos del sistema operativo no están cubiertos. Ver `evidencia/h4-s6.txt`.
  - Ingreso: `evidencia/h8-ingreso-oscuro-360-130.png`.
  - Tu cuenta: `evidencia/h8-alta-oscuro-paso-1-360-130.png` y `evidencia/h8-alta-oscuro-paso-1-abajo-360-130.png`; confirmar correo: `evidencia/h8-alta-oscuro-paso-2-360-130.png`.
  - Identidad/carnet: `evidencia/h8-alta-oscuro-paso-3-360-130.png`, `evidencia/h8-alta-oscuro-paso-3-abajo-360-130-corregido.png` y `evidencia/h8-alta-oscuro-paso-3-fecha-responsive-360-130.png`.
  - Datos personales/revisión: `evidencia/h8-alta-oscuro-paso-4-360-130-scroll-restablecido.png`, `evidencia/h8-alta-oscuro-paso-4-fecha-responsive-360-130.png` y `evidencia/h8-alta-oscuro-paso-4-abajo-360-130.png`.
  - Actividad: `evidencia/h8-alta-oscuro-paso-5-360-130-scroll-restablecido.png` y `evidencia/h8-alta-oscuro-paso-5-abajo-360-130-scroll-restablecido.png`; contrato: `evidencia/h8-alta-oscuro-paso-6-360-130.png`.

### Umbrales de captura (H5.S3.M2)

- La prueba inicial expuso error de coma flotante en el extremo exacto de ±12 %. La comparación acepta ahora matemáticamente ambos límites con epsilon 1e-12, probado además con un caso apenas fuera. Ver `evidencia/h5-calidad-captura.txt`.

## Decisiones tomadas en ejecución
| Fecha | Decisión | Por qué | A confirmar con |
|---|---|---|---|
| 2026-10-12 | Usar el checkout `PasanakuBackend` anidado y la base exacta `08042090`; crear la rama sugerida. | Es el monorepo y commit explícitos por el plan. | Nadie; decisión ejecutable del plan. |
| 2026-10-12 | Conservar intacto el cambio preexistente en `docs/Views/AportaYa-Maqueta.html`. | Ya estaba modificado antes de comenzar. | — |
| 2026-10-12 | Mantener Flutter 3.47.5 disponible y registrar desviación de la versión pedida. | Evitar cambiar el SDK global mientras se mide compatibilidad con el código real. | — |
| 2026-10-12 | `DELETE /sesion` autoriza explícitamente `PARTICIPANTE` y `BACKOFFICE`; renovación declara `consumes=*/*` en el handler para conservar web cookie-only sin `Content-Type`. | La guardia de endpoints exige decisión explícita y OpenAPI Generator añade `consumes=application/json` al body opcional, incompatible con solicitudes de cookie sin cuerpo. | Validar en Gradle cuando la BD de pruebas vuelva a estar disponible. |
| 2026-10-12 | El cierre invalida la sesión y su familia refresh, pero no revoca access JWT ya emitidos en otros servicios. | El acceso JWT es stateless en el diseño actual; queda una ventana residual hasta su expiración (15 min). No prometer revocación instantánea. | ADR-024 / arquitectura de tokens. |
| 2026-10-12 | Mapear errores a campos solo cuando el contrato lo hace inequívoco: CU01-06 a contraseña y CU01-07 a vencimiento; CU01-03 queda general. | `detalle` es opaco y CU01-03 mezcla celular/documento; CU01-08..11 son decisiones manuales de expedientes, no errores de correo. | — |
| 2026-10-12 | Todo `CampoDeSeleccion` abre hoja modal; los grupos de 2–4 opciones que ya se presentan simultáneamente (segmentados y tarjetas) no se convierten en selects. | La regla acordada clasifica como select solo una lista de valor único con al menos 3 opciones que no se ven todas a la vez; la búsqueda modal no reemplaza grupos visuales simultáneos. | — |

## Evidencia adicional H1 (2026-10-12)

- `evidencia/h1-sesion-final.txt`: 54 pruebas Flutter dirigidas de sesión, refresco, purga, tutoriales, candado, ciclo de vida, tope de ocho horas, aviso al ingresar y cierre desde perfil; salida final `All tests passed!`.
- `evidencia/h1-s6-backend-bloqueado.md`: pruebas backend no ejecutables por PostgreSQL de pruebas ausente (`127.0.0.1:5433` rechaza conexión); el intento sin jOOQ falla porque las clases generadas no existen. Docker Desktop no está disponible en esta sesión. H1.S6.M2–M5 quedan `A MEDIAS` y M8 `BLOQUEADO`, no se infiere éxito del código escrito.
- `evidencia/h1-s6-openapi-client.txt`: generación oficial de OpenAPI y los 11 clientes Dart completada; `openApiValidate` no está configurada en el proyecto (Gradle indica que falta `inputSpec` en esa tarea). El generador oficial consumió el YAML y generó servidor/modelos/clientes.
- `evidencia/h1-s5-final.txt`: 30 pruebas dirigidas de candado, ciclo de vida, reintento biométrico serializado, retorno desde inactividad, animación, mensaje de cierre y los seis goldens visuales + dos de ingreso; salida `All tests passed!`.
- `evidencia/h1-s5-revision-visual.md`: revisión manual de las ocho capturas con fuentes reales, tamaños/tema y límites de la matriz. Incluye los PNG de H1.S5.M1/M3/M4 y conserva la evidencia de la primera corrida Ahem sólo en la historia local; no se usa como evidencia visual válida.
- `evidencia/h2-reproduccion.md`: intento de descubrimiento de backend/dispositivo para H2; no se encontró URL del backend de Pablo, listener en Prism/backend local ni dispositivo móvil, y el workspace de simulado no tiene dependencias instaladas. Se marca como entorno/no reproducido, sin inventar errores de producto. No se solicita captura a Pablo por la instrucción del usuario de no preguntar.
- `evidencia/h2-s2-m1-ROJO.txt` y `evidencia/h2-s2-m2.txt`: catálogo de errores, prueba roja y regresión verde de todos los códigos de sesión/alta definidos como mensajes de usuario.
- `evidencia/h2-s2-m3-ROJO.txt` y `evidencia/h2-s2-m3.md`: enrutamiento de errores de campo y retorno desde el último paso del alta, con prueba de foco y mensaje inline.
- `evidencia/h2-s2-m4.md`: cuerpos serializados CU-01/CU-04 cotejados contra los límites y las propiedades del contrato OpenAPI; verificación local 10/10.
- `evidencia/h2-s2-m5.md`: alerta persistente de rechazo y desplazamiento al error comprobados en viewport pequeño.
- `evidencia/h2-s2-m6.md`: `trazaId` conservado/renderizado en alta, login, correo y subida; pruebas negativas para ausencia de ID; el alcance se limita expresamente a identidad y `EstadoError` genérico.

## H4.S3.M5 — icono de CI (2026-10-12)

- Revisé [la captura del anverso](./evidencia/h4-s3-icono/anverso-360.png) y [la del campo Número de CI](./evidencia/h4-s3-icono/numero-ci-360.png), generadas por `test/identidad/h4_ci_icono_visual_test.dart` sobre `PasoDocumento` con viewport 360×800.
- `Icons.badge_outlined` se distingue como credencial por la silueta y las líneas de datos; el rótulo y la guía lo sitúan explícitamente en el carnet. No se percibe como tarjeta de pago, así que mantengo el icono Material y descarto el `CustomPainter` condicional. Resultado: no añadir dibujo redundante.
- Prueba verificada en Flutter 3.44.8: 1/1 PASS, `tester.takeException() == null`. Evidencia completa: [H4.S3.M5](./evidencia/h4-s3-icono.md).
