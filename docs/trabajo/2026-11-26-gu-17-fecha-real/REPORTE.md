> **AVANCE: 6 / 6 — 100,0 %.** (con las limitaciones de «No cubierto»; con H6.S1.M2 de #522 cerrada, el plan de #522 queda en 23/23)

# Reporte — GU-17 · fecha de nacimiento real en el alta + cierre de H6.S1.M2

- Fecha: 2026-11-26 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/gu-c-credencial` (desde `origin/dev`)
- Decisión de Pablo (2026-11-26, vía sesión principal): pedir la fecha real en el alta; PT24H del enfriamiento confirmado.
- Peldaño de evidencia alcanzado: **TESTED** (backend y modal) · UI de invitación: verificada visualmente contra backend simulado declarado.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | `fechaNacimiento` (date) obligatoria en `EntradaAltaUsuarioInterno` | `./gradlew :servicios:identidad:generarServidorOpenApi` | BUILD SUCCESSFUL |
| H1.S1.M2 | El backend valida (ausente/futura → AP-CU08-13) y guarda la fecha declarada; se quitó el literal `1990-01-01` | `./gradlew :servicios:identidad:test --tests '*CU08AltaConFechaReal*' :servicios:identidad:webTest` | BUILD SUCCESSFUL (3 pruebas del criterio + 400 sin fecha) |
| H1.S1.M3 | Criterios CU-08 y código AP-CU08-13 | `python3 scripts/verificar_criterios.py; python3 scripts/verificar_boveda.py` | «Sin divergencias» / TODO OK |
| H1.S2.M1 | Modal de alta con «Fecha de nacimiento» obligatoria, error por campo (futura, o AP-CU08-13 del servidor), y sin el texto de «contraseña provisional» | `npx ng test --include='src/app/rutas/sistemas/**/*.spec.ts'` | 50 archivos, 220 pruebas pasan |
| H1.S2.M2 | Typecheck y lint del backoffice; gates | `yarn typecheck`, `yarn lint`, `spotlessCheck`, `testBarrido`, `contractTest`, `inventario_endpoints --check` | todo en verde / «alineado» |
| H2.S1.M1 | Capturas de la invitación: cargando, errores por campo, QR TOTP (más las ya hechas) | `npx playwright test e2e/visual-cuenta.e2e.ts --workers=1 --project=chromium --trace off` | pasan; capturas miradas (móvil claro errores, escritorio oscuro QR, tablet claro cargando) |

## A medias
Ninguna.

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| Operadores ya dados de alta con `1990-01-01` ficticio (A8) | DECISION_REQUIRED | Pablo/RR.HH.: quién y cómo carga la fecha real de cada uno |

## Evidencia
Salidas en la tabla. Capturas en `evidencia/capturas-acceso/` (backend simulado con `page.route`, datos sintéticos).

## No cubierto
- Integración contra base de la alta con fecha (`UsuariosInternosRepositorioTest` requiere Docker): corre en CI.
- Las capturas usan backend simulado: no prueban el servicio real.
- Observación sin corregir: en el paso del QR los seis recuadros del código no muestran etiqueta visible (revisar nombre accesible).
- No hay edad mínima del operador (A9): solo se rechaza fecha futura.

## Desvíos del plan
Se agregó a este cambio quitar el texto de «contraseña provisional» del modal (quedó obsoleto por P-1).

## Riesgos residuales
- Campo obligatorio nuevo en un contrato existente: el único consumidor conocido es el backoffice, ajustado en este mismo cambio; un cliente externo del alta se rompería.

## Decisiones y ambigüedades
- A8: operadores previos conservan `1990-01-01`; no se tocó ningún dato.
- A9: sin edad mínima por falta de fuente; confirmar con Pablo/RR.HH.
