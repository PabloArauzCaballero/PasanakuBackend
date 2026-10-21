# Reporte — Verificación de identidad opcional al registrarse + desbloqueo con PIN

> **AVANCE: 8 / 11 — 72,7 %.** (backend en plan propio, PR #126)

- Fecha: 2026-10-17 · Plan: [PLAN.md](./PLAN.md) · Backend: PR #126 `claude/kyc-diferido-20261017` · App: `alta-atlas-sesion-work/apps/movil` (sin commit)
- Peldaño: backend `TESTED` (web + matriz; integración en CI); app `TESTED` (pruebas dirigidas) y APK v7 compilado. Sin prueba en teléfono todavía.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1–M3 | «Saltar por ahora» en el paso del carnet; quien salta recorre cuenta → correo → datos → contrato y la cuenta se crea sin `documento` | `flutter test test/identidad/alta_sin_verificacion_test.dart test/identidad/identidad_requerida_test.dart` | PASS (8 + 6) |
| H1.M4 | Recargar, retirar, transferir, crear grupo, pedir cupo y aportar exigen identidad verificada → «Verificá tu identidad» | `flutter test test/unidad/acceso_a_rutas_test.dart` | PASS (8) |
| H1.M5 | Pantalla «Verificá tu identidad»: verificar ahora / en revisión / activar cuenta / error con reintento | `identidad_requerida_test.dart` | PASS |
| H1.M6 | Verificación dentro de la app: carnet + fotos → actividad → `POST /usuarios/{id}/verificacion` → subida de fotos → estado de la sesión | `alta_sin_verificacion_test.dart` (correcto / ya iniciada / carnet ajeno) | PASS |
| H2.M1 | El botón «Entrar con huella o PIN» reemplaza un pedido colgado | `flutter test test/widget/capa_de_bloqueo_interaccion_test.dart test/widget/capa_de_bloqueo_test.dart test/unidad/vigilante_test.dart` | PASS (+26) |
| H2.M2 | Android: `DesbloqueoLocal.kt` pasa al PIN del teléfono si la huella falla o no está enrolada | `flutter build apk --release …` | Built → `Downloads/AportaYa-test-2026-10-17-v7.apk` (69 073 764 bytes, build 2026101705) |

## A medias
### H1.M7 — Aviso superior para la cuenta sin verificar
- Qué anda: el aviso usa `estadoDeCuenta`; sin verificar dice «Verificá tu identidad para mover dinero y participar en pasanakus» y abre la verificación. `flutter analyze lib` sin problemas.
- Qué no anda / falta: no hay prueba de widget dedicada a ese texto (las existentes del aviso en revisión pasan).
- Dónde quedó: `lib/navegacion/aviso_de_verificacion.dart`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.M8 | TODO | CI verde del PR #126 → fusión en `test` → despliegue TEST → `curl` alta sin documento |
| H2.M3 | TODO | Que el usuario pruebe el APK v7 en su teléfono (PIN sin huella) |

## Evidencia
```text
flutter test (suite completa, --concurrency=2): +702 ~1 -307
  fallas: 304 en test/goldens y test/capturas (comparación de píxeles, ya fallaban antes),
          2 en motivo_de_salida_en_ingreso_test (ya fallaban), 1 h4_ci_icono_visual_test (golden de píxeles).
  Ninguna falla de comportamiento nueva.
gradlew :servicios:identidad:webTest → 20 suites, 0 fallos
MatrizDeAutorizacionContratoTest → 8/8 · verificar_criterios.py → sin divergencias · verificar_seguridad.py → TODO OK
```

## No cubierto
- Integración contra PostgreSQL real del backend: corre en CI (no hay Docker local).
- El flujo completo en un teléfono real (saltar, operar, verificar, subir fotos).
- El respaldo al PIN en Android 10 o anterior (`KeyguardManager`): compilado, no ejercitado.

## Desvíos del plan
- El alta pasó a llamar `POST /usuarios` con Dio directo: el cliente Dart generado exige `documento` y no conoce `INCOMPLETO_KYC`.
- Dos pruebas web del backend usan `@MockitoBean(types = …)` a nivel de clase para no superar 300 líneas.

## Riesgos residuales
- La actividad económica del registro no se envía al servidor (ya era así antes de este trabajo).
- Un APK viejo contra el backend nuevo sigue funcionando (el documento sigue aceptándose); el APK v7 contra un backend sin #126 falla al saltar.

## Decisiones y ambigüedades
- «Transacción» = recargar, retirar, transferir, aportar; «participar» = crear grupo, pedir cupo. Ver grupos e invitaciones sigue libre. Confirmar con el usuario.
- En el registro rápido se piden nombre, apellidos y fecha de nacimiento (columnas obligatorias del esquema).
