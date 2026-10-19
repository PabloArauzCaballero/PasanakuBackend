# Reporte — App: arreglos que el usuario encontró en su teléfono

> **AVANCE: 13 / 20 — 65,0 %.**

- Fecha: 2026-10-19 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/app-kyc-diferido-y-pin-20261018` (PR #139 a `test`)
- Peldaño: `TESTED` (pruebas dirigidas en verde; candado ejercitado en emulador). Falta CI de goldens, despliegue y prueba en el teléfono.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | Acento naranja de vuelta (logo, botones, ilustraciones) | `flutter test test/widget/` (diseno_flutter) | +89 PASS · `evidencia/h1-m1-acento.txt` |
| H2.M1/M2 | El estado de la verificación ya no parpadea y no dice «No cierres la app» | `flutter test test/identidad/estado_sin_parpadeo_test.dart` | +3 PASS; con el código viejo +1 −2 · `evidencia/h2-m1-m2-sin-parpadeo.txt` |
| H2.M3 | «Consultar estado»: activa la cuenta ahí mismo si ya la aprobaron | `flutter test test/identidad/consultar_estado_test.dart` | +6 PASS |
| H2.M4 | Inicio: «Completar verificación» solo si falta algo; si no, «Consultar estado» | `flutter test test/identidad/resolver_cuenta_kyc_test.dart` | +4 PASS |
| H2.M5 | «Identidad requerida» con «Consultar estado», sin borrar la pantalla | `flutter test test/identidad/identidad_requerida_test.dart` | +7 PASS |
| H2.M6 | Grupos: tarjeta «Estamos verificando tu identidad» + «Consultar estado» | `flutter test test/pasanaku/aviso_de_revision_en_grupos_test.dart test/pasanaku/pantalla_mis_grupos_test.dart` | +8 PASS · `evidencia/h2-pruebas.txt` |
| H3.M1/M2 | Deslizar para recargar en Inicio y Movimientos (también con la billetera inactiva) | `flutter test test/billetera/recargar_inicio_test.dart` | +4 PASS |
| H4.M1 | Correo del perfil deshabilitado, sin «Guardar» | `flutter test test/widget/pantalla_perfil_correo_test.dart test/widget/pantalla_perfil_cerrar_sesion_test.dart` | +6 PASS |
| H5.M1 | Candado ejercitado en emulador con solo PIN | `adb` + `dumpsys window` | `evidencia/h5-candado-emulador.txt` |
| H5.M3 | «Entrar con mi contraseña» cierra al instante | `flutter test test/unidad/cierre_de_sesion_test.dart` | +2 PASS |
| H6.M1 | Contraseña de puros espacios rechazada en login y cambio (el alta NO estaba cubierta: ver H9) | `flutter test test/identidad/login_validacion_test.dart …` | +14 PASS |
| H7.M1 (código) | Backoffice sale solo de `/arranque` | `ng test --include=src/app/nucleo/restaurando-sesion*.spec.ts` | 8 PASS; con el código viejo 3 fallan · `evidencia/h7-backoffice.txt` |
| H8.M1 | Suite completa + analyze + reglas | `flutter test`, `flutter analyze`, `verificar_frontend.py` | +754; las 311 rojas son todas de imagen · `evidencia/h8-m1-suite.txt` |

| H9.M1 | Alta: la contraseña no acepta espacios; aviso al instante con una sugerencia válida (`Grupo4247Futuro`) | `flutter test test/identidad/paso_contrasena_test.dart test/identidad/login_validacion_test.dart` | +11 PASS · `evidencia/h9-clave-sin-espacios.txt` |

## A medias
### H5.M2 — «Entrar con huella o PIN» en el teléfono del usuario
- Qué anda: en el emulador (Android 13) con solo PIN, el botón abre el PIN del sistema y desbloquea: al abrir, al volver de la pantalla apagada y al volver desde el inicio del teléfono.
- Qué no anda: no se reprodujo el fallo del teléfono del usuario (Samsung con PIN y rostro); el código de v8 también funcionaba en el emulador.
- Qué falta: que el usuario pruebe el APK v9. Se blindaron dos caminos que pierden el pedido sin aviso (`authenticate` con la actividad detenida; error del sistema contestado como «canceló») y, si el sistema falla, el candado ahora muestra el código del error para diagnosticarlo.
- Dónde quedó: `DesbloqueoLocal.kt`, `capa_de_bloqueo*.dart`, `biometria_android.dart`. Compila (APK debug x64).

### H7.M1 — prueba contra TEST
- Qué falta: desplegar y repetir la sonda Playwright sobre `/arranque?volverA=%2F`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.M2 | TODO (código hecho) | Verificar `application-label:'Aporteo'` con `aapt` en el APK v9 |
| H8.M2 | TODO | Regenerar goldens en Linux (flujo temporal de GitHub) |
| H8.M3 | TODO | CI verde → merge a `test` → despliegue Coolify |
| H8.M4 | TODO | Compilar APK v9 |

## Evidencia
Ver `evidencia/`.

## No cubierto
- Ningún cambio se probó todavía en el teléfono del usuario.
- Movimientos con la billetera activa: el gesto de recargar el extracto se probó solo por la igualdad de la clave (`ParametrosExtracto`), no con una prueba de pantalla.
- El servidor sigue aceptando contraseñas con espacios (`minLength: 8` en `identidad.yaml`, `PoliticaDeClave`); la app ya no las deja enviar.

## Desvíos del plan
- Error de reporte corregido: H6 decía «login, alta y cambio», pero el alta valida con su propia regla en `pasos_alta/paso_cuenta.dart` y seguía aceptando espacios. Lo encontró el usuario; se arregló en H9. Ahora una contraseña nueva (alta o cambio) no lleva espacios en ningún lado; el login solo rechaza las de puros espacios, para no dejar afuera a quien ya tenía una con espacios.
- `EstadoDePantalla` (sistema de diseño) borraba la pantalla en cada refresco por defecto; se cambió el valor por defecto para conservar el dato (afecta a todas las pantallas; cubierto por la suite).
- `ParametrosExtracto` no tenía igualdad por valor: cada redibujo pedía el extracto de nuevo. Se agregó (hacía falta para recargar).
- El PR #139 ya estaba unido a `test` (por otra persona) cuando terminé: los cambios van en el PR #147.
- Error mío: en la regeneración de goldens excluí `docs/`, pero muchas pruebas (`h10_*`, `capturas`, `motivo_de_salida`) comparan contra imágenes guardadas en `docs/.../evidencia/` con `matchesGoldenFile`: son referencias, no solo evidencia. El CI del #147 falló por eso (2 pruebas); se regeneró de nuevo incluyéndolas. En el #139 también se habían revertido esas imágenes.

## Riesgos residuales
- Si el fallo del PIN es del teléfono (no del pedido perdido), el usuario verá el mensaje con el código del sistema en vez del PIN: hace falta esa captura para seguir.
- Jobs iOS del CI rojos por `Pods_Runner not found` (entorno de macOS); ajeno a estos cambios.

## Decisiones y ambigüedades
- «Una parte amarillo»: se tomó como el acento naranja anterior al 15-oct (`o700` claro / `o400` oscuro). Confirmar con el usuario.
- «Aporteo»: nombre bajo el ícono (Android `label`, iOS `CFBundleDisplayName`) y el de la lista de apps recientes (`MaterialApp.title`). Los textos internos siguen diciendo «AportaYa». Confirmar con el usuario.
