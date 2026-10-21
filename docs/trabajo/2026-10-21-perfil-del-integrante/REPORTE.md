# Reporte — Perfil del integrante con datos reales (teléfono y reputación solo para el mismo grupo)

> **AVANCE: 5 / 7 — 71,4 %.** Backend (grupos, identidad, transparencia) y app terminados y probados en local; faltan el CI de la PR (H1.S6.M1) y el APK de prueba (H1.S6.M2).

- Fecha: 2026-10-21 · Plan: [PLAN.md](./PLAN.md) · Rama: `claude/perfil-participante-20261021` (desde `origin/dev` `4e296bc8`)
- Peldaño de evidencia: `TESTED` (pruebas dirigidas en verde en los tres servicios y en la app, contra PostgreSQL real y con el cliente HTTP real contra un doble del productor). No `VERIFIED`: no se ejercitó con los servicios reales levantados juntos ni en un teléfono.

## Completado
| ID | Qué se logró | Comando | Resultado |
|---|---|---|---|
| H1.S1.M1 | grupos: `GET /grupos/{grupoId}/integrantes/{usuarioId}/comparte` responde sí/no con el mismo criterio de integrante que `/detalle`; quien pregunta sale del token | `gradlew :servicios:grupos:webTest --tests *GruposDetalleYFirmaWebTest* :servicios:grupos:integrationTest --tests *PertenenciaCompartida*` | PASS web 10/10 (3 nuevos), integración 4/4 |
| H1.S2.M1 | identidad: `GET /identidad/contactos/grupos/{grupoId}/integrantes/{usuarioId}` devuelve solo el teléfono si grupos confirma; si no o si grupos falla, 403. Cada lectura queda en `comun.registro_acceso_datos` (TELEFONO, LECTURA) | `:servicios:identidad:webTest --tests *ContactoEnGrupoWebTest* --tests *VerificacionesControllerWebTest*`, `:contractTest --tests *GruposPorHttpPacto*`, `:integrationTest --tests *ContactoEnGrupo*`, `testBarrido` | PASS web 3/3 + 4/4, contrato 4/4, integración 3/3 (incluye el rastro de auditoría) |
| H1.S3.M1 | transparencia: `GET /reputacion/{usuarioId}/perfil-en-grupo/{grupoId}`: puntaje, nivel, puntualidad, grupos completados y abandonados, incumplimientos abiertos, antigüedad e insignias vigentes. Sin montos. Sin historial no hay ceros | `:servicios:transparencia:webTest --tests *PerfilEnGrupoWebTest* --tests *ReputacionControllerWebTest*`, `:contractTest --tests *GruposPorHttpPacto*`, `:integrationTest --tests *PerfilEnGrupo*`, `testBarrido` | PASS web 4/4, contrato 4/4, integración 3/3 |
| H1.S4.M1 | Matriz de autorización: ámbito declarado de las tres rutas (cada una con un patrón que la prueba busca en el código); pactos `identidad-grupos-pertenencia.json` y `transparencia-grupos-pertenencia.json`; clientes Dart/Angular regenerados | `gradlew :plataforma:comun-pruebas:contractTest --rerun` | PASS 8/8 (antes de declararlas: 1 falla con las tres rutas listadas) |
| H1.S5.M1 | app: la pantalla del integrante muestra solo datos del servidor, con «Volver», y resuelve cargando / 403 / sin historial / error parcial | `flutter test test/pasanaku/pantalla_detalle_miembro_test.dart` · `flutter test test/pasanaku` · `flutter test test/a11y` · `dart format` (Dart 3.12.2, el del CI) | PASS 7/7 · 85/85 · 6/6 · 0 cambios. Capturas: 18 en [evidencia/](./evidencia/) (360/412/600 dp × claro/oscuro × con historial / sin historial / sin permiso), miradas |

## A medias
ninguna

## Pendiente
| ID | Estado | Qué lo destraba |
|---|---|---|
| H1.S6.M1 | TODO | Abrir la PR a `dev` y esperar el CI. El job de Frontend ya está rojo en `dev` por `dart format` de 11 archivos de `diseno_flutter`/`movil` (de Pablo) y puede seguir rojo por eso |
| H1.S6.M2 | TODO | APK de prueba con el workflow «APK de prueba (Android)» después de que el cambio llegue a la rama desde la que se construye |

## Evidencia
```text
gradlew :plataforma:comun-pruebas:contractTest --rerun   (antes de declarar el ámbito)
MatrizDeAutorizacionContratoTest > rechaza: toda operacion de participante que nombra a una persona declara como no cruza a otra FAILED
  grupos GET /grupos/{grupoId}/integrantes/{usuarioId}/comparte (consultarSiCompartenGrupo) ... sin ambito declarado
  identidad GET /identidad/contactos/grupos/{grupoId}/integrantes/{usuarioId} (consultarContactoEnGrupo) ... sin ambito declarado
  transparencia GET /reputacion/{usuarioId}/perfil-en-grupo/{grupoId} (consultarPerfilEnGrupo) ... sin ambito declarado
(después)  tests="8" failures="0"

flutter test test/pasanaku/pantalla_detalle_miembro_test.dart
00:01 +7: All tests passed!
flutter test test/pasanaku          -> 00:09 +85: All tests passed!
flutter test test/a11y              -> 00:02 +6: All tests passed!
python scripts/verificar_frontend.py movil
  FALLA · ningún archivo de más de 200 líneas ['pantalla_sorteo.dart (203)', 'rutas.dart (220)']   <- ya estaban así en dev; no se tocaron
```
Los teléfonos y nombres de las pruebas y capturas son sintéticos.

### Seguridad (regla 90.6)
- **Amenaza**: que cualquiera con sesión lea el teléfono o el historial de otra persona cambiando ids en la URL (IDOR), o que un servicio caído deje pasar.
- **Control**: la pertenencia la decide `grupos` (`ConsultarDetalleDelGrupo.comparten`, quien pregunta sale del token). `identidad` (`ConsultarContactoEnGrupo`) y `transparencia` (`ConsultarPerfilEnGrupo`) preguntan antes de leer, y cualquier falla de `grupos` (4xx, 5xx, cuerpo ilegible, caída) es «no» → 403 (`GruposPorHttp`, en los dos servicios). Ámbito declarado en `PoliticaDeAcceso`.
- **Pruebas**: `PertenenciaCompartidaRepositorioTest` (otro grupo, retirado, expulsado, postulante, invitado, inexistente → no); `ContactoEnGrupoRepositorioTest` y `PerfilEnGrupoRepositorioTest` (no comparten → 403, nada leído ni auditado); los dos `GruposPorHttpPactoContratoTest` (500, 403, ilegible, sin campo, caído → no); web tests 401/403/400.
- **Riesgo residual**: ver abajo.

### Microservicios (regla 98.8)
- Saltos: app → identidad → grupos (teléfono); app → transparencia → grupos (reputación). Un salto por servicio; la app pide los dos en paralelo.
- Contrato: lado consumidor con el cliente real contra un servidor HTTP del JDK que responde el pacto, y forma del productor comparada con `grupos.yaml`. **No cubierto**: el proceso real de `grupos` respondiendo.
- Caída del dependido: simulada (servidor detenido) en los pactos → 403. No se apagó un contenedor real.
- Duplicado: no aplica (solo lecturas; la única escritura es el registro de acceso, que es un rastro por lectura).
- Inconsistencia: ninguna persistente; si `grupos` cae, el perfil muestra «solo el mismo grupo» hasta que vuelva.

## No cubierto
- Los tres servicios reales levantados juntos (E2E) y la app en un teléfono real.
- La traza con `correlationId` cruzando servicios (el cliente la propaga; no se observó).
- El circuit breaker: `identidad` y `transparencia` usan `ClienteDeServicio` (timeout de 3 s, sin breaker), el mismo patrón que ya usan para otras llamadas. El plan decía «cliente con circuito».
- Rate limiting de las rutas nuevas (rige el del gateway).

## Desvíos del plan
- La ruta de `identidad` quedó en `/identidad/contactos/...` y no en `/usuarios/...`: `/contactos` no es un prefijo reservado (`scripts/modelo.py`) y bajo `/usuarios` obligaba a tocar `UsuariosController` y sus pruebas.
- Se agregó el registro de lectura del teléfono en `comun.registro_acceso_datos` (no estaba en el plan; lo pide `data-privacy-financial`).
- Se reemplazó `test/pasanaku/pantalla_detalle_miembro_test.dart`: verificaba los datos inventados («Identidad Verificada · Nivel 2 Oficial», «1 Punto por cada Bs en Juego», insignias fijas) que el requisito manda quitar. No se borró para que la suite pase: se reescribió para el comportamiento nuevo.

## Riesgos residuales y deuda
- La lectura de la **reputación** no queda auditada: `comun.registro_acceso_datos.tipo_dato` no tiene un valor para reputación. Agregarlo es un cambio del modelo (`scripts/generar_ddl.py`). A decidir.
- La ruta `/pasanaku/grupos/:grupoId/integrante/:cupoId` (go_router, `rutas.dart:92`) pasa el **nombre** en la query. En la app web queda en la URL (regla 90.2.2). La tarjeta abre el perfil sin esa ruta, pero la ruta sigue existiendo. No se tocó (`rutas.dart` ya está sobre el límite de líneas y es de otro trabajo).
- Los componentes de Pablo que la pantalla usaba (`TarjetaRiesgoConfianza`, `GrillaMetricasMiembro`, `TarjetaPuntosMesa`, `InsigniaTile`, `TourEnStages`, `CelebracionStage`) quedan en `diseno_flutter` sin usar desde esta pantalla.
- `ReporteRepositorio` (auditoria) inserta la IP con `DSL.field("inet ?", …)`. La misma forma falló en identidad con un error de sintaxis y se cambió a `?::inet`. No se verificó si en auditoria pasa lo mismo.

## Decisiones y ambigüedades
- **Del dueño (2026-10-21)**: el teléfono y la reputación los ven solo los miembros del mismo grupo; APK nuevo al terminar.
- **Ambigüedad** (a confirmar con el dueño): el 2026-10-20 se decidió que `/grupos/{id}/detalle` no lleve teléfono ni correo, y que las solicitudes muestren solo el nombre. Supuesto: la decisión de hoy aplica solo al perfil de un integrante ya aceptado; la lista y las solicitudes quedan igual.
- **Ambigüedad** (a confirmar con el dueño / cumplimiento): la base legal para mostrar el teléfono a los miembros. Supuesto: el reglamento del grupo que todos firman. No se revisó su texto.
- **Ambigüedad** (a confirmar con quien mantiene el modelo de reputación): `indice_puntualidad` («proporción de aportes pagados antes del vencimiento», `NUMERIC(5,2)`) no dice si va de 0 a 1 o de 0 a 100. Supuesto: 0–100; la app lo muestra como «96.40 %».
