# Reporte — Continuar traspaso Pasanaku (PR #503–#505 → dev → test) y correcciones de la app móvil

> **AVANCE: 22 / 35 — 62,9 %.** (provisional: CI de #503 y #506 en curso; H2 sin empezar)

- Fecha: 2026-11-23 / 24 · Plan: [PLAN.md](./PLAN.md)
- Ramas: `codex/f3s8-m4-parametros-backoffice` (#503), `codex/f3-l2-turnos-asignados` (#504, integrada), `codex/l4l6-resume` (#505, integrada), `claude/movil-correcciones-20261124` (#506), `claude/modest-newton-4ckcid` (este reporte)
- Peldaño de evidencia: #504 y #505 `TESTED` en CI e integradas a `dev`; móvil `TESTED` local (957 pruebas) a la espera de CI; #503 `TESTED` parcial (CI en curso).

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | Backoffice compila con tipos estrictos (TS4111 en el spec; plantilla: variante inexistente, `undefined` sin normalizar) | CI #503 job 138136110143 (bundle compila) | PASS — commits 31234da, 28cb588b |
| H1.S1.M3 | Cuatro ojos en CU-132 | lectura `CU132RegistroDeParametros.java:168`, `@Permiso("TARIFARIO_ADMINISTRAR")` en `TarifasController.java:245-265`, test `CU132Test.mismaPersona` | control presente |
| H1.S2.M1 | Causa del rojo de #504 | log job 138026360778 | fixture `cuposOcupados` sin titular ni firma del reglamento |
| H1.S2.M2 | #504 en verde | check-runs de 29dc2b22 | 12 success / 0 fallos |
| H1.S3.M1 | #505 en verde | check-runs de ed9bb51 | 12 success / 0 fallos |
| H3.S1.M1 | `aportaya://aporte` va a una ruta existente | `flutter test destinos_de_enlaces_test ruta_del_aviso_test enlaces_profundos_test` | rojo 3 → verde 37/37 |
| H3.S1.M2 | Un solo refresco de sesión por sesión | `flutter test refrescador_compartido_test cliente_refresco_test` | rojo (2 refrescos) → verde 6/6 |
| H3.S1.M3 | `test:front` incluye billetera y navegación | `flutter test` 7 carpetas | 929 → 957 en verde |
| H3.S1.M4 | FLAG_SECURE con conteo | `flutter test proteccion_apilada_test` | rojo → verde 3/3 (+24 de protección) |
| H3.S1.M5 | Guardia de identidad en retiro/traspaso/permuta | `flutter test exige_identidad_test` | rojo 4 → verde 10/10 |
| H3.S1.M6 | El vigilante informa errores de programación | `flutter test vigilante_de_aprobacion_test` | rojo → verde 5/5 |
| H3.S1.M7 | Cerrar sesión borra el progreso de tutoriales | `flutter test cierre_borra_progreso_test` | rojo → verde 3/3 (+199) |
| H3.S1.M8 | Una sola configuración de idioma | `dart analyze --fatal-infos` + suite | sin issues / 954 |
| H3.S1.M9 | Código muerto retirado (5 archivos, 0 importadores) | `grep` + analyze + suite | sin issues / 954 |
| H3.S1.M10 | Prueba de CU-21 con `pagarCuotaConSaldo` | `flutter test pantalla_aportar_test` | 4/4 |
| H3.S2.M2 | SEG-005: el código del enlace del grupo en el fragmento | tests + nginx real | rojo 3 → verde 25/25; log de acceso: `GET /g` (antes el secreto) |
| H3.S2.M3 | `cobrarAporte` deprecated (sin retirarlo) | `openapi-generator validate`, `verificar_contratos_limites.py`, postman y catálogo | sin problemas / sin deriva |

## A medias
### H1.S1.M2 — CI de #503
- Qué anda: bundle del backoffice compila; la matriz de roles pasa local (43/43).
- Qué no anda: falta la corrida completa de CI sobre `bcfc6274`.
- Qué falta exactamente: CI en verde y merge a `dev`.
- Dónde quedó: `codex/f3s8-m4-parametros-backoffice`, empujada.

### H3.S2.M1 — FLUJO-QR-001
- Qué anda: app — el botón «Simular pago» exige `--dart-define=PAGOS_DE_PRUEBA=true` (rojo → verde 8/8); `apk-de-prueba.yml` y `dev:simulado` lo encienden.
- Qué no anda: la guardia backend `GuardiaDeRecargaDePrueba` no se compiló local (Maven Central 429).
- Qué falta exactamente: CI de #506 ("Formato" y "Los cinco corredores") en verde.
- Dónde quedó: f66cdb94 en `claude/movil-correcciones-20261124`.

### H1.S4.M1 / H3 — integración
- Qué anda: #505 → `dev` (64da8b97); #504 → `dev` (98b03005).
- Qué falta: #503 y #506 con CI verde → merge a `dev`.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H2.S1.M1–M2 | TODO | #503 y #506 integradas; revisar diff `test..dev` y promover por PR `dev → test` (como #498) |
| H2.S1.M3 | TODO | Pablo ejecuta `estado` en su host (SSH bloqueado desde aquí) |
| H3.S1.M11 CONFIG-001 | BLOQUEADO | Clave de release que solo Pablo puede crear; verificar exige correr `apk-de-prueba.yml`, que publica un prerelease |
| H3.S3.M1 RESPALDO-003 | BLOQUEADO — DECISION_REQUIRED | Pablo aprueba el texto del tour sobre el fondo de garantía |
| H3.S3.M2 APROB-001 | BLOQUEADO — DECISION_REQUIRED | Pablo decide si se retiran AP-CU20-02 / AP-CU69-10 |
| H3.S3.M3 grupos/detalle | BLOQUEADO — DECISION_REQUIRED | Decisión de negocio (PENDIENTES.md) |
| H3.S4.M1 dispositivo real | BLOQUEADO | Push FCM, rostros reales, goldens macOS: dispositivo y cuentas fuera de este entorno |
| M08 / M6 (checkpoints del traspaso) | TODO | Fuera de este trabajo: incompletos, no se integran a TEST |

## Evidencia
```text
#503 job 138033735520: ... Received status code 429 from server: Too Many Requests      (ENVIRONMENT)
#503 job 138035184830: pantalla-de-parametros-financieros.spec.ts(98,42): error TS4111 ...
#503 job 138065322825: TS2322 Type '"terciario"' is not assignable to type 'VarianteDeBoton'
#503 job 138136110143: × COMERCIAL abre ninguna sección — expected [ 'configuracion' ] to deeply equal []
#504 job 138026360778: 337 tests completed, 10 failed — El turno <uuid> no tiene titular
#504 job 138068129687: ErrorDeNegocio: Falta que alguien acepte el reglamento del grupo.
#504 29dc2b22: 12 success, 6 skipped, 0 failure  → merge 98b03005
#505 ed9bb51: 12 success, 6 skipped, 0 failure   → merge 64da8b97
movil (local, Flutter 3.44.8): flutter test 7 carpetas → 01:56 +957: All tests passed!
movil: dart analyze --fatal-infos → No issues found! · dart format → 0 changed · verificar_frontend.py movil → OK
nginx (SEG-005), log de acceso: "GET /g HTTP/1.1" (nuevo) · "GET /g/<uuid>.<SECRETO> HTTP/1.1" (viejo, enmascarado)
```

## No cubierto
- Gradle local: Maven Central respondió 429 a esta máquina; todo lo Java (fixture de #504, guardia de FLUJO-QR-001) se verificó solo en CI.
- `AndroidManifest.xml` (`path="/g"`) no se compiló ni se probó en dispositivo: App Links requiere un teléfono.
- Typecheck completo del backoffice local: faltan los clientes generados (el artefacto de CI no se puede bajar: host de blob bloqueado por el proxy).
- Flujos E2E móviles (patrol, simulador iOS) no se ejecutaron.

## Desvíos del plan
- #504 no era el lock de JaCoCo (diagnóstico del traspaso): era la fixture. Y el primer arreglo destapó un segundo control que la fixture saltaba (firma del reglamento).
- #503 "spotlessCheck" era un 429 de Maven Central; detrás había tres fallos reales (TS4111, plantilla, matriz de roles).
- H3 (app móvil) se agregó por pedido de Pablo del 2026-11-24, dentro del mismo plan.

## Riesgos residuales
- **COMERCIAL ve y opera «Parámetros financieros»**: ya tenía `TARIFARIO_ADMINISTRAR` (CU-36) y, con la decisión de que ese permiso propone y aprueba, el backend ya lo autorizaba. Confirmar si COMERCIAL debe conservarlo.
- `RecargasProveedorSimulado` se activa por omisión (`matchIfMissing=true`) también en producción; no se tocó.
- Deriva de generados versionados: barriles de clientes Dart y ejemplos de `packages/simulado` no coinciden con los contratos actuales (CI los regenera). No se commitearon.
- SEG-005: hasta que se actualice la app instalada, quien tenga una versión vieja abre la web en vez de la app.

## Decisiones y ambigüedades
- `aportaya://aporte` → `/pasanaku/mi-estado` (cuota pendiente y pago con saldo, P-06), porque el recordatorio todavía no emite un `obligacionId` (F3.L5.M43). Confirmar con Pablo.
- Matriz de roles de #503: se alineó al permiso real en vez de ocultar el menú (ocultarlo no cambia la autorización).
- M08 TTL (24 meses estricto vs ~25): DECISION_REQUIRED, sin tocar.
