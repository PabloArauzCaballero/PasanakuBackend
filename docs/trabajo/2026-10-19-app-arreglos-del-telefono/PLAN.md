# Plan — App: arreglos que el usuario encontró en su teléfono (2026-10-19)

- Fecha: 2026-10-19 · Repos afectados: PasanakuBackend (apps/movil, packages/tokens, packages/diseno_flutter, apps/backoffice) · Predecesor: `docs/trabajo/2026-10-18-app-aprobacion-recarga-invitar/` (PR #139, todavía abierto)
- Rama: `claude/app-kyc-diferido-y-pin-20261018` (se suma al PR #139); backoffice en rama propia desde `test`.
- Resultado observable: con el APK v9 en el teléfono, la persona termina el alta y entra a la app sin quedarse esperando; ve «Consultar estado» en vez de «Completar verificación» cuando ya mandó todo; ninguna pantalla parpadea; desliza para recargar Inicio y Movimientos; el correo del perfil no se edita; el candado abre el PIN del teléfono; el login rechaza una contraseña de puros espacios; el logo y los botones vuelven al naranja; el ícono se llama «Aporteo».
- Kill-test: en el emulador con solo PIN, tocar «Entrar con huella o PIN» y que no aparezca el teclado de PIN del sistema.

## Hallazgos (fase 1)
- Color: `8b6070ef` (2026-10-15, «identidad institucional») cambió `accent` de `o700`/`o400` (naranja) a `g700`/`g300` (verde) en `packages/tokens/tokens.json` y `docs/Views/Sistema-Diseno/estilos.css`, y las ilustraciones de `o500` a `g700`. El isotipo (`atomos/marca.dart`) pinta el gesto interior con `t.accent`. El ícono del lanzador (`mipmap-*/ic_launcher.png`) sigue con el naranja.
- Nombre: `android:label="aportaya_movil"` en `AndroidManifest.xml`.
- Parpadeo en bucle: `VerificacionNotifier.actualizar()` (`dominio/estado_de_verificacion.dart`) pone `AsyncLoading` sin el dato anterior cada 2 s, hasta 20 veces: la pantalla de estado borra el contenido y muestra el círculo 20 veces.
- «Esperando aprobación» al terminar el alta: `PENDIENTE` sin sesión dice «Estamos revisando tu documento / No cierres la app».
- «Completar verificación» con todo enviado: `billetera/resolver_cuenta.dart` lo muestra para PENDIENTE/EN_REVISION sin mirar `pendientesDeKyc`. Con APROBADA cierra la sesión en vez de renovar el acceso.
- Candado: «Entrar con mi contraseña» espera hasta 5 s la revocación remota sin ninguna señal (`dominio/cierre_de_sesion.dart`). «Entrar con huella o PIN» depende de `biometriaDisponibleProvider` (se calcula una vez) y de `DesbloqueoLocal.kt`, que cancela y relanza el diálogo en el mismo instante.
- Login: falta saber qué valida hoy (se mira en H6).

## Alcance
- IN: lo de la lista del usuario (H1–H6), el arreglo del backoffice que quedó pendiente (H7), goldens/CI/merge/despliegue/APK (H8).
- OUT: rediseño de colores más allá del acento (fondo, tipografías del mismo commit); App Links; iOS.
- Ambigüedades registradas:
  - «tenía una parte amarillo»: se supone que es el acento naranja (`o700` claro / `o400` oscuro) que había antes del 15-oct. Confirmar con el usuario al ver el APK.
  - «el nombre será Aporteo»: se supone el nombre bajo el ícono y en la lista de apps (Android `label`, iOS `CFBundleDisplayName`, `MaterialApp.title`). Los textos internos que dicen «AportaYa» no se tocan. Confirmar con el usuario.

## H1 — Marca: naranja y «Aporteo»
**CA:** Dado el APK nuevo, cuando se abre, entonces el isotipo y los botones principales usan el naranja de antes y el ícono se llama «Aporteo».
**DoD:** tokens regenerados, `flutter test` de roles de color en verde, goldens regenerados en Linux.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.M1 | Acento naranja en tokens, bóveda e ilustraciones | `Tokens.claro.accent == 0xFFB85B13` y `Tokens.oscuro.accent == 0xFFEF9E4E` | `yarn workspace @aportaya/tokens build` + `flutter test test/widget/roles_de_color_accion_test.dart` (diseno) | HECHO |
| H1.M2 | Nombre «Aporteo» | `android:label="Aporteo"` y `CFBundleDisplayName` = Aporteo | `aapt dump badging` del APK muestra `application-label:'Aporteo'` | TODO |

## H2 — Verificación sin esperas ni parpadeos
**CA:** Dado que mandé todo, cuando entro a Inicio, Grupos o a una operación, entonces veo «Consultar estado» (no «Completar verificación»), nada parpadea, y si ya me aprobaron se activa la cuenta ahí mismo.
**DoD:** pruebas de widget dirigidas en verde.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.M1 | El sondeo del estado no borra lo que se ve | Durante una consulta automática la pantalla no muestra el círculo de carga | `flutter test test/identidad/estado_sin_parpadeo_test.dart` | HECHO |
| H2.M2 | Fin del alta sin «No cierres la app» | PENDIENTE sin sesión muestra «Recibimos tus documentos» y «Entrar a AportaYa» | mismo test | HECHO |
| H2.M3 | «Consultar estado» reutilizable | Botón que consulta, avisa «sigue en revisión» o activa la cuenta si la aprobaron | `flutter test test/identidad/consultar_estado_test.dart` | HECHO |
| H2.M4 | Inicio: «Completar verificación» solo si falta algo | Con expediente completo se ve «Consultar estado»; con faltantes, «Completar verificación» | `flutter test test/billetera/resolver_cuenta_estado_test.dart` | HECHO |
| H2.M5 | Pantalla «identidad requerida» con «Consultar estado» y sin parpadeo | Muestra «Consultar estado»; al refrescar no muestra el círculo | `flutter test test/identidad/identidad_requerida_test.dart` | HECHO |
| H2.M6 | Grupos: aviso con «Consultar estado» mientras la cuenta espera | Con `requiereVerificacion` la pestaña muestra el aviso y el botón | `flutter test test/pasanaku/pantalla_mis_grupos_test.dart` | HECHO |

## H3 — Deslizar para recargar en Inicio y Movimientos
**CA:** Dado Inicio o Movimientos, cuando deslizo hacia abajo, entonces se vuelven a pedir los datos sin salir de la pantalla.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H3.M1 | Inicio | El gesto vuelve a pedir la cuenta y el saldo | `flutter test test/billetera/recargar_inicio_test.dart` | HECHO |
| H3.M2 | Movimientos | El gesto vuelve a pedir los movimientos | mismo archivo | HECHO |

## H4 — Perfil: el correo no se edita
**CA:** Dado el perfil, entonces el correo se ve deshabilitado y no hay botón «Guardar».
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H4.M1 | Correo de solo lectura, sin «Guardar» | `find.text('Guardar')` no encuentra nada; el campo no acepta texto | `flutter test test/identidad/perfil_correo_test.dart` | HECHO |

## H5 — Candado que responde
**CA:** Dado un teléfono con solo PIN (o PIN y cara), cuando toco «Entrar con huella o PIN», entonces aparece el PIN del sistema; «Entrar con mi contraseña» responde al instante, también después de apagar y prender la pantalla.
**Estado:** TODO

| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H5.M1 | Reproducir en emulador con PIN | Se observa (o no) el fallo con el APK v8 | `adb` + captura | HECHO |
| H5.M2 | Arreglo del lado nativo/Dart según la causa | Tocar el botón muestra el PIN del sistema, también tras apagar la pantalla | emulador + captura | A MEDIAS — anda en emulador solo-PIN; falta el teléfono del usuario |
| H5.M3 | «Entrar con mi contraseña» sin espera muda | Se cierra local al instante; la revocación remota sigue sola | `flutter test test/dominio/cierre_de_sesion_test.dart` | HECHO |

## H6 — Login: sin contraseñas de puros espacios
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H6.M1 | Validar identificador y contraseña no vacíos ignorando espacios | Con «     » no se llama al servidor y se ve el error en el campo | `flutter test test/identidad/login_validacion_test.dart` | HECHO |

## H7 — Backoffice atascado en «Restaurando tu sesión…»
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H7.M1 | `RestaurandoSesion` navega al resolver la sesión | Abrir `/arranque?volverA=%2F` sin sesión termina en `/ingreso` | spec de Angular + sonda Playwright contra TEST tras desplegar | TODO |

## H8 — Entrega
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H8.M1 | Suite no visual + analyze + reglas de frontend | Todo verde salvo píxeles | `flutter analyze`, `flutter test`, `python scripts/verificar_frontend.py` | TODO |
| H8.M2 | Goldens regenerados en Linux | Job «App — goldens (Linux)» en verde | CI del PR #139 | TODO |
| H8.M3 | Merge a `test` y despliegue | Contabo con el commit nuevo | `gh pr view` + estado del despliegue | TODO |
| H8.M4 | APK v9 | APK en `Downloads` | `flutter build apk` + `aapt` | TODO |

## Desvío registrado
- H2.M1: además del sondeo, `EstadoDePantalla` (sistema de diseño) borraba la pantalla en cada refresco por defecto (`omitirSkeletonAlActualizar = false`). Se cambió el valor por defecto a `true`: al refrescar se conserva lo que se ve. Afecta a todas las pantallas que lo usan; lo cubre la suite completa (H8.M1).

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| El fallo del PIN no se reproduce en emulador | El arreglo queda sin prueba real | Registrar como No cubierto; el usuario lo prueba en su teléfono |
| Jobs iOS rojos (`Pods_Runner not found`) | No es del cambio | Comparar con otras ramas; registrar |
