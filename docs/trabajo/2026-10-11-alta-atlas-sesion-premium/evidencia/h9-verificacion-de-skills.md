# H9 — Verificación de skills contra el checkout

Fecha: 2026-10-12 · Rama: `codex/alta-atlas-sesion-premium-2026-10-11`.

| ID | Afirmación | Comprobación | Resultado |
|---|---|---|---|
| H9.S1.M1 | Tokens fuente y salida Dart: `packages/tokens/tokens.json` y `packages/diseno_flutter/lib/tokens/tokens.dart`; nombres `Paleta`, `Tokens`, `Espacio`, `Radios`, `Borde`, `Tactil`, `Tipo` | `Test-Path` en ambas rutas; `rg -n 'abstract final class (Paleta|Espacio|Radios|Borde|Tipo|Tactil)|class Tokens' packages/tokens/scripts/a-dart.mjs` | PASS; rutas presentes y clases declaradas por el generador. `tokens.json` documenta generación a Dart y el generado está en `lib/tokens/`. |
| H9.S1.M1 | Inventarios `atomos/`, `moleculas/`, `organismos/`, `moviles/`, catálogo y gate | `rg --files packages/diseno_flutter/lib`; `Test-Path packages/diseno_flutter/lib/catalogo`; `scripts/verificar_frontend.py`; comprobación de 81 símbolos con mayúscula en backticks contra el Dart de diseño | PASS; existen las cuatro carpetas y catálogo; 81/81 símbolos encontrados. El gate/reglas sostienen límite de 200 líneas y restricciones de selects. |
| H9.S1.M1 | `MovimientoDeInterfaz.*` forma parte de tokens | `rg -n 'MovimientoDeInterfaz' packages/tokens/tokens.json packages/diseno_flutter/lib/tokens/tokens.dart`; `rg -n 'abstract final class Movimiento|static const (micro|entrada|pagina|paginaDeVuelta)' packages/diseno_flutter/lib/atomos/movimiento.dart` | FALSO antes de corregir: no aparece en tokens; sí existe `Movimiento` con duraciones 120/220/300/240 ms. La skill fue corregida para quitar la atribución a tokens. |
| H9.S1.M3 | `Movimiento` define duraciones, curvas y resorte | Lectura de `packages/diseno_flutter/lib/atomos/movimiento.dart`; cotejo con tabla de `pasanaku-movimiento/SKILL.md` | PASS; 120/220/300/240 ms, curvas `llega/sale/pareja`, resorte 1/520/19 y corrimiento 30 dp coinciden. |
| H9.S1.M3 | Existen `HundidoAlTocar`, `AparicionEscalonada`, `TransicionDeEje`, `LuzDeBoton`, `SuperficieViva`, `MontoQueSube` | `rg -n 'class (HundidoAlTocar|AparicionEscalonada|TransicionDeEje|LuzDeBoton|SuperficieViva|MontoQueSube)' packages/diseno_flutter/lib` | PASS; declaración encontrada para cada una en `atomos/`, `moleculas/` o `moviles/`. |
| H9.S1.M4 | Los 22 archivos de piezas enumerados están bajo `pantallas/identidad/pasos_alta/`; los 9 nombres de dominio están bajo identidad | Búsqueda recursiva de cada basename citado en `pasanaku-ui-componentes/SKILL.md` | PASS; 31/31 existen en la ruta/área indicada. |
| H9.S1.M4 | Existe catálogo y gate aplica regla de 200 líneas a Dart de app (con excepción del texto contractual) | `Test-Path packages/diseno_flutter/lib/catalogo`; revisión de `scripts/verificar_frontend.py` líneas del gate móvil | PASS; el catálogo existe y la regla de tamaño está implementada. Esta verificación no afirma que el gate integral actual esté verde. |
| H9.S1.M5 | §95.8 enumera D2 del plan y las cuatro skills D4 | Comparación de `.claude/rules/95-frontend.md` §95.8 con `PLAN.md` §1.2.2/§1.2.4; `git diff -- .claude/rules/95-frontend.md` | PASS; lista relacionada incluye D2/D4 y no se modificaron otras secciones de reglas. |
| H9.S1.M6 | `.agents/skills` es espejo de `.claude/skills` | Comparación recursiva de rutas y SHA-256 | PASS; 289 archivos idénticos. |
| H9.S1.M2 | `decidirRuta`, `Vigilante`, `generacionDeSesionProvider` y reglas 60 s/5 min/8 h | Cotejo de la skill con rutas, proveedores, vigilancia, candado, cliente refresh y contrato/backend | PASS tras corregir cinco afirmaciones; detalle debajo. |
| H9.S1.M4 | Inventario de piezas de identidad tras H4–H6, catálogo y límite de 200 líneas | Aplazado hasta cotejar todos los nombres del inventario con los cambios H5/H6 | NO CERRADO; no se marca como verificado integralmente. |

Resultado: M1/M2/M3/M4 cotejadas; la skill de UX móvil se corrigió donde describía una intención como comportamiento aplicado o generalizaba una prueba parcial. El conteo de líneas de app es una regla del gate, no un resultado de cumplimiento; H10 todavía debe correr el gate integral. Skills leídas completas antes del cambio: `skill-authoring`, `anti-hallucination-guard`, `pasanaku-diseno`, `pasanaku-app-movil-ux` y `factual-discovery`.

## H9.S1.M2 — verificación de UX de sesión

La auditoría paralela fue de solo lectura; no ejecutó tests/builds. Se contrastaron las afirmaciones con el checkout y se corrigieron estos desajustes:

| Afirmación previa | Evidencia en código | Corrección aplicada |
|---|---|---|
| La prueba recorre el árbol entero de rutas | `apps/movil/test/unidad/acceso_a_rutas_test.dart:10-66` enumera 41 rutas y prueba una ruta sintética desconocida | La skill ahora dice inventario explícito y fallback privado, no recorrido automático del `GoRouter`. |
| Ninguna pantalla consulta si hay token | `apps/movil/lib/pantallas/pasanaku/pantalla_unirse.dart:68` lee credenciales para una operación | La regla ahora prohíbe guards locales, no lecturas puntuales para una acción. |
| La biometría se solicita siempre | `capa_de_bloqueo.dart:132` la omite si no está disponible; `capa_de_bloqueo_vista.dart:101-114` ofrece ingreso alternativo | La skill indica que se intenta si está disponible y conserva la alternativa de ingreso. |
| El cierre local ocurre siempre | `apps/movil/lib/proveedores/sesion.dart:113-119,130-140`; `apps/movil/lib/dominio/cierre_de_sesion.dart:25-31` | La skill distingue fallo de red (el cierre local se intenta) de fallo del almacén seguro (no se garantiza borrar token/estado). |
| `proveedores/haptica.dart` aplica una política única | `haptica.dart:6` ofrece el puerto; hay llamadas directas en `shell.dart:96`, `controlador_prueba_de_vida.dart:122`, `pantalla_prueba_de_vida.dart:74`; `haptica_android.dart:15` y `haptica_ios.dart:14` mapean `toqueLigero` a `selectionClick` | La skill separa política deseada de implementación actual y deja claro que todavía no está centralizada. |
| Toda captura permite «Usar esta foto igual» | `apps/movil/lib/pantallas/identidad/dominio/cu02_subir_foto.dart` valida/rechaza según el contrato sin esa acción | La skill ya no promete esa salida si no existe en la API. |

Coincidencias confirmadas: `decidirRuta` está en `navegacion/acceso_a_rutas.dart`; `Sesion` observable refresca el router; el token emitido dura 15 min; el cliente rota/persiste refresh; el servidor revoca familia al detectar reuso; el límite local es 8 h; el candado aplica 60 s en segundo plano y 5 min de inactividad; el cierre intenta revocación con plazo de 5 s y como máximo dos intentos.
