# Plan — Quitar «Ver el estado de mis aportes» de la pantalla del grupo

- Fecha: 2026-10-28 · Repos afectados: Pasanaku (`apps/movil`) · Predecesor: ninguno
- Resultado observable: en la app móvil, la pantalla de un grupo (p. ej. «Cumple richard») ya no muestra el botón «Ver el estado de mis aportes» al pie de la lista de cupos.
- Kill-test: `grep -rn "estado de mis aportes" apps/movil/lib` devuelve algo.

## Alcance
- IN: `apps/movil/lib/pantallas/pasanaku/pantalla_del_grupo.dart` — borrar el botón, la constante y los imports que queden sin uso.
- OUT: la ruta `/pasanaku/mi-estado` y `PantallaMiEstado` (las usan `vistas_de_unirse.dart`, `pantalla_unirse_al_grupo.dart`, `pantalla_mis_grupos.dart` y el tutorial `primeros_pasos.dart`); los parámetros `participanteId`/`usuarioId` del constructor (los pasa `rutas.dart`; quitarlos es un refactor no pedido).
- Ambigüedades registradas: ninguna. La captura del usuario señala ese botón exacto.

## H1 — El botón ya no aparece
**CA:** Dado un participante dentro de un grupo, cuando abre la pantalla del grupo, entonces no ve «Ver el estado de mis aportes».
**DoD:** H1.S1.M1 y H1.S1.M2 en HECHO.
**Estado:** HECHO

### H1.S1 — Quitar el botón
**CA:** igual que H1.
**DoD:** grep vacío + `flutter analyze` sin hallazgos nuevos en el archivo.
**Estado:** HECHO

| ID | Microtarea | CA (binario) | DoD (comando de verificación) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | Borrar el botón y la constante `_misAportes` | El texto no existe en `apps/movil/lib` | `grep -rn "estado de mis aportes" apps/movil/lib` → sin salida | HECHO |
| H1.S1.M2 | El archivo compila sin imports sobrantes | Analyzer limpio | `cd apps/movil && flutter analyze lib/pantallas/pasanaku/pantalla_del_grupo.dart` → `No issues found!` | HECHO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| Sin acceso directo a «mi estado» desde el grupo | El participante llega a su estado por otras vías (Mis grupos) | Pedido explícito del usuario; se anota en el reporte |
