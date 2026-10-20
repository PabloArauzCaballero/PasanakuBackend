# Reporte — Correcciones de la app tras la prueba en teléfono (APK v5)

> **AVANCE: 7 / 7 — 100 %.**

- Fecha: 2026-10-17 · Plan: [PLAN.md](./PLAN.md) · Ramas: backend `test` (PR #120 → 3c797aa, PR #121 → 02bbd73); app: cambios locales sin commit en `alta-atlas-sesion-work/apps/movil`
- Peldaño de evidencia: `TESTED` (pruebas dirigidas y la suite no visual de la app en verde). No probé en un teléfono el flujo contra TEST: queda para el usuario.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.M1 | La billetera en 0,00 muestra la cifra, «Recargar» y «Todavía no tenés movimientos» | `flutter test test/widget/pantalla_de_saldo_test.dart test/widget/inicio_de_billetera_test.dart` | PASS |
| H1.M2 | El paso 1 de la guía encuentra `billetera.saldo` en una cuenta en cero (por eso salía el aviso amarillo) | igual que M1 | PASS |
| H1.M3 | `GET /grupos/participaciones` trae `nombreDelGrupo`, `montoAporte`, `moneda` y `esOrganizador` | `gradlew :servicios:grupos:webTest`; CI del PR #120 | PASS |
| H1.M4 | La pestaña Grupos muestra una tarjeta por grupo; el estado del grupo se titula con su nombre | `flutter test test/pasanaku/pantalla_mis_grupos_test.dart` → +4 | PASS |
| H1.M5 | El perfil trae el correo de la cuenta (`GET /usuarios/yo/perfil`, nuevo) | `gradlew :servicios:identidad:webTest` (19 suites, 0 fallos) + `flutter test test/widget/pantalla_perfil_correo_test.dart`; CI del PR #121 | PASS |
| H1.M6 | El candado pide la huella cuando la app está al frente; textos «Hola de nuevo», «Entrar con huella o PIN», «Entrar con mi contraseña» | `flutter test test/widget/capa_de_bloqueo_test.dart test/widget/capa_de_bloqueo_interaccion_test.dart test/unidad/vigilante_test.dart` → +25 | PASS |
| H1.M7 | APK v5 en Descargas | `ls -la` → 69073664 bytes | PASS |

Suite no visual de la app (`test/a11y test/contrato test/identidad test/pasanaku test/unidad test/widget`): +683 −2. Los 2 fallos son goldens de píxeles de la pantalla de ingreso (`identidad/motivo_de_salida_en_ingreso_test.dart`, 1,38 % de diferencia), una pantalla que este trabajo no tocó.

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| — | TODO | El usuario prueba el APK v5 en su teléfono contra TEST |

## Evidencia
```text
flutter test (no visuales) → 01:57 +683 -2: Some tests failed. (los 2: goldens de ingreso)
Built build\app\outputs\flutter-apk\app-release.apk (65.9MB)
PR #120 MERGED 3c797aae · PR #121 MERGED 02bbd73c
```

## No cubierto
- La prueba de integración `CU07MiPerfilTest` (Postgres real) corre en `integrationTest` con Docker; localmente no se corrió para no levantar Docker. La cubre el CI del PR #121, que pasó.
- Las capturas visuales (`test/capturas`, `test/goldens`) no se regeneraron; en Windows fallan por el renderizado de fuentes.
- La causa exacta de que la huella no se pidiera sola en el teléfono es una hipótesis (se pedía antes de que la app estuviera al frente). No se reprodujo en un dispositivo.

## Desvíos del plan
- M5 necesitó un endpoint de backend nuevo (no había forma de leer el correo propio). Va en el PR #121.
- `capa_de_bloqueo_test` reveló que un callback de fin de cuadro no pide un cuadro por sí solo: se agregó `scheduleFrame()`, y un estado del sistema desconocido (`null`) se trata como «al frente».

## Riesgos residuales
- Si TEST todavía no desplegó 3c797aa/02bbd73, las tarjetas salen como «Grupo» sin nombre y el correo queda vacío; la app no se rompe.
- El nombre del grupo viaja en un parámetro de la ruta interna de la app (no es dato personal y no sale a la red).

## Decisiones y ambigüedades
- «Me salió esto, nada que ver» (bloqueo al abrir): se mantuvo el bloqueo por seguridad y se arregló que la huella se pida sola, con textos más claros. A confirmar con el usuario si prefiere entrar sin bloqueo.
