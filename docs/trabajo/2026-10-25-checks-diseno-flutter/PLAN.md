# Plan — dejar en verde el lint propio de `packages/diseno_flutter` (paso f1 del CI)

- Fecha: 2026-10-25 · Repos afectados: PasanakuBackend (rama de trabajo sobre `dev`) · Predecesor: `2026-10-24-recorrido-real-local`
- Resultado observable: `python3 scripts/verificar_frontend.py diseno` termina en `TODO OK` y el job «Frontend — Flutter y Angular (ADR-044)» del CI deja de morir en el paso f1.
- Kill-test: `python3 scripts/verificar_frontend.py diseno` → hoy devuelve `3 FALLAS` (reproducido en `dev` 4b6800e6, ver `evidencia/00-baseline.txt`).

## Alcance
- IN: los siete archivos que el barrido señala en `packages/diseno_flutter/lib/` (literales de diseño, formato de dinero, > 200 líneas) y, descubiertos después, tres archivos > 200 líneas en `apps/movil` y `packages/ui` (H1.S3).
- OUT: cualquier cambio de comportamiento o de apariencia no impuesto por la regla; el repo `PasanakuFrontend` (su árbol es anterior y no contiene estos archivos; su CI no llega a ejecutar código, ver REPORTE); `GET /grupos/solicitudes` y `GET /grupos/solicitudes-escaladas` (H4.B.M1, trabajo aparte).
- Hechos (con ruta): la deuda viene de `c46d277a` (2026-10-20); `5b605dbf` ya dejó escrito «9 archivos que ya rompían el lint en dev». Baseline en `dev`: `dart format` OK, `dart analyze --fatal-infos` sin hallazgos, 102 pruebas de unidad/widget en verde.
- Ambigüedades registradas: (1) los literales se sustituyeron por las primitivas de `Paleta` más cercanas (sin colores nuevos ni `// permitido:`): blanco y el morado del tour quedan idénticos; verde/azul/ámbar/rojo y las medallas (bronce→`o600`, plata→`n400`, oro→`warn`, diamante→`info`, esmeralda→`ok`) cambian de matiz y `fontSize` 34/18/44/11 pasa al tamaño del estilo `Tipo` más cercano. Confirmar con diseño. (2) Dividir con `part`/`part of` o con archivos públicos propios: se elige la división que no cambia API pública. Confirmar con quien mantiene el paquete.

## H1 — El barrido de reglas propias del paquete de diseño pasa
**CA:** Dado `dev`, cuando se corre el barrido de los cinco paquetes (`diseno`, `movil`, `ui`, `web`, `backoffice`), entonces todas las reglas dan OK sin usar `// permitido:` para esconder una violación.
**DoD:** `python3 scripts/verificar_frontend.py diseno` → `TODO OK` · `dart format --set-exit-if-changed lib test` · `dart analyze --fatal-infos` · `flutter test test/unidad test/widget` con ≥ 102 en verde · apps/movil: `flutter analyze` y sus pruebas de widget que usan estos componentes.
**Estado:** HECHO (el job Frontend sigue rojo por f5, fuera de alcance: ver REPORTE)

### H1.S1 — Literales y formato
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Sustituir `Color(0x…)`, `Colors.red` y `fontSize:` de `tarjeta_riesgo_confianza.dart` por tokens | el barrido no reporta «sin literal de diseño» | `python3 scripts/verificar_frontend.py diseno` | HECHO |
| H1.S1.M2 | Reemplazar `toStringAsFixed` del porcentaje por una función de `dinero/formatear.dart` con prueba | no hay «sin formato de dinero» y `99.8`→`99.8%` igual que antes | `flutter test test/unidad` | HECHO |

### H1.S2 — Archivos de más de 200 líneas
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S2.M1 | Dividir `tarjeta_riesgo_confianza`, `tarjeta_puntos_mesa`, `grilla_metricas_miembro`, `insignia_tile` | cada archivo ≤ 200 líneas | `wc -l` + barrido + analyze + tests | HECHO |
| H1.S2.M2 | Dividir `celebracion_stage` (481), `tour_en_stages` (431), `barra_pestanas` (203) | cada archivo ≤ 200 líneas | ídem | HECHO |

### H1.S3 — Rojos ocultos tras el primero (descubiertos al correr el barrido de todos los paquetes)
El CI corta en el primer fallo; al correr `python3 scripts/verificar_frontend.py <paquete>` para los cinco paquetes aparecen tres archivos más sobre el límite de 200 líneas, ajenos a la deuda anterior. Se agregan aquí (regla 00 §3).
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H1.S3.M1 | `apps/movil/lib/pantallas/pasanaku/rutas.dart` (220) y `pantalla_sorteo.dart` (203) ≤ 200 líneas | `verificar_frontend.py movil` → TODO OK | barrido + `dart format` + `dart analyze` (limitado, ver No cubierto) | HECHO |
| H1.S3.M2 | `packages/ui/src/tabla-de-datos/tabla-de-datos.ts` (205) ≤ 200 líneas | `verificar_frontend.py ui` → TODO OK | barrido + tests de `@aportaya/ui` si el entorno los permite | HECHO |

## H2 — Verificación y entrega
| ID | Microtarea | CA (binario) | DoD | Estado |
|---|---|---|---|---|
| H2.S1.M1 | Regresión del paquete y de `apps/movil` | 0 fallos nuevos frente al baseline | `flutter test` de ambos | HECHO |
| H2.S1.M2 | Publicar la rama y comprobar el CI real | el paso f1 del CI deja de fallar en este cambio | `actions_list` sobre el run de la rama | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Cambio visual por tokens | golden o captura distinta | los goldens del paquete se corren en Linux aquí; apps/movil igual |
| División rompe un `import` | compilación | `dart analyze --fatal-infos` tras cada división |
| El CI revela fallos posteriores a f1 (hoy ocultos porque f1 corta) | trabajo adicional | se listan en REPORTE; no se esconden |
