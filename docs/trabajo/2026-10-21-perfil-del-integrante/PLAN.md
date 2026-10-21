# Plan — Perfil del integrante con datos reales (teléfono y reputación solo para el mismo grupo)

- Fecha: 2026-10-21 · Repos afectados: PasanakuBackend (servicios `grupos`, `identidad`, `transparencia`, `apps/movil`) · Predecesor: `e99570d2`/`c46d277a` (pantalla de detalle de miembro, Pablo) · Rama: `claude/perfil-participante-20261021` (desde `origin/dev` `4e296bc8`)
- Resultado observable: dentro de un grupo, al tocar la tarjeta de un integrante se abre una **pantalla** (con «volver») que muestra su nombre, su teléfono y su reputación REAL (puntaje y nivel de confianza, índice de puntualidad, grupos completados y abandonados, incumplimientos abiertos, antigüedad, insignias). Solo la ve quien está en el mismo grupo.
- Kill-test: (1) un participante de OTRO grupo pide el contacto o el perfil de reputación de alguien → no recibe nada (403); (2) la pantalla ya no tiene ninguna cifra calculada a partir del número de cupo.

## Hallazgos (fase 1)
- El tap ya abre `PantallaDetalleMiembro` a pantalla completa en `dev`/`test` (`tarjeta_de_integrante.dart:101`), pero **muestra datos inventados**: teléfono y correo «enmascarados» calculados a partir del número de cupo (`_enmascararTelefono`, `_enmascararCorreo`), puntaje de respaldo `92 + numero % 7`, «puntos en la mesa» `14500 + numero*1250` (`pantalla_detalle_miembro.dart:53-80`). Viola la regla 00 §2.1. El modal que ve el dueño es el de un APK viejo (último APK: 2026-10-16).
- `grupos` es dueño de la pertenencia: el predicado de integrante activo está en `ConsultarDetalleDelGrupo.java:35-41` (excluye RETIRADO, EXPULSADO, REEMPLAZADO, POSTULANTE, INVITADO).
- `identidad` es dueña del teléfono (`identidad.usuario.telefono_e164`).
- `transparencia` es dueña de la reputación: `puntaje_reputacion` tiene `indice_puntualidad`, `grupos_completados`, `grupos_abandonados`, `incumplimientos_abiertos`, `antiguedad_meses`; `insignia_otorgada` + `insignia_logro`. Hoy solo se publica `GET /reputacion/{usuarioId}/puntaje` (puntaje, nivel, tieneHistorial) con permiso PARTICIPANTE.
- Las llamadas entre servicios propagan el token de quien pregunta (`ClienteConCircuito`, con timeout y circuit breaker). No existe cuenta de servicio `SERVICIO_GRUPOS`.

## Alcance
- IN: ruta de pertenencia compartida en `grupos`; contacto en grupo en `identidad`; perfil de reputación en grupo en `transparencia`; clientes HTTP con circuito; pactos y matriz de autorización; pantalla con datos reales y estados cargando / error / sin historial; pruebas; APK de prueba.
- OUT: monto total aportado y cualquier dato bancario (no se exponen); correo (no se pidió); lista del grupo (`/detalle`) sigue sin teléfono; rediseño de los componentes de `diseno_flutter` (de Pablo); formato de los 11 archivos que rompen el lint de Frontend en `dev` (de Pablo).
- Decisiones del dueño (2026-10-21, en esta sesión): teléfono visible **solo para miembros del mismo grupo**; reputación visible **solo para miembros del mismo grupo**; APK nuevo al terminar.
- Ambigüedades registradas:
  - El 2026-10-20 se decidió que `/grupos/{id}/detalle` no lleve teléfono ni correo y que las solicitudes muestren solo el nombre. Supuesto: la decisión de hoy aplica solo al perfil de un integrante ya aceptado, y la lista y las solicitudes quedan como estaban. A confirmar con el dueño.
  - Base legal para mostrar el teléfono a los miembros: supuesto, el reglamento del grupo que todos firman (`consentimiento.tipo = REGLAMENTO_GRUPO`). No se verificó el texto del reglamento. A confirmar con el dueño / cumplimiento.

## H1 — El perfil del integrante muestra datos reales y solo a su grupo
**CA:** Dado un grupo con Ana y Beto como integrantes y Carla en otro grupo, cuando Ana abre el perfil de Beto, entonces ve su nombre, su teléfono y su reputación real; y cuando Carla pide los mismos datos de Beto con ese grupo, recibe 403 y nada más.
**DoD:** webTest + integrationTest + contractTest de los tres servicios en verde · `flutter test test/pasanaku/` en verde · CI de la PR en verde · APK generado.
**Estado:** EN CURSO

| ID | Microtarea | CA (binario) | DoD (comando) | Estado |
|---|---|---|---|---|
| H1.S1.M1 | grupos: `GET /grupos/{grupoId}/integrantes/{usuarioId}/comparte` (contrato + controlador + caso de uso con el predicado existente) | quien pregunta y el otro integrantes activos → `comparten=true`; cualquiera fuera → `false` | `gradlew :servicios:grupos:webTest :servicios:grupos:integrationTest --tests *Comparte*` | HECHO (webTest 10/10, integrationTest 4/4) |
| H1.S2.M1 | identidad: `GET /identidad/contactos/grupos/{grupoId}/integrantes/{usuarioId}` (bajo el prefijo reservado `/identidad`) → pregunta a grupos (cliente con circuito); 403 si no comparten o grupos no responde | co-integrante → teléfono; otro / caído → 403 | `gradlew :servicios:identidad:webTest --tests *ContactoEnGrupo*` | HECHO (webTest 3/3 + 4/4 verificaciones, contractTest 4/4, integrationTest 3/3, testBarrido) |
| H1.S3.M1 | transparencia: `GET /reputacion/{usuarioId}/perfil-en-grupo/{grupoId}` → misma pregunta; devuelve puntaje, nivel, puntualidad, grupos, incumplimientos, antigüedad, insignias vigentes (sin montos) | co-integrante → perfil; otro / caído → 403; sin historial → `tieneHistorial=false` sin ceros inventados | `gradlew :servicios:transparencia:webTest :servicios:transparencia:integrationTest --tests *PerfilEnGrupo*` | HECHO (webTest 4/4 + ReputacionController, contractTest 4/4, integrationTest 3/3, testBarrido) |
| H1.S4.M1 | Pactos y matriz: ámbito declarado en `PoliticaDeAcceso` para las rutas nuevas; pactos consumidor/productor identidad→grupos y transparencia→grupos | `contractTest` verde | `gradlew contractTest` | EN CURSO (matriz 8/8; faltan clientes generados) |
| H1.S5.M1 | app: la pantalla usa solo los datos de las dos rutas; sin cifras inventadas; estados cargando / error / sin historial; «volver» | prueba de widget con dobles en los tres estados | `flutter test test/pasanaku/` | TODO |
| H1.S6.M1 | CI de la PR en verde | todos los jobs que corren en pass | `gh pr checks` | TODO |
| H1.S6.M2 | APK de prueba con el perfil | APK descargable | `gh run` del workflow «APK de prueba (Android)» | TODO |

## Riesgos y bloqueos previstos
| Riesgo | Impacto | Mitigación |
|---|---|---|
| El job de Frontend ya está rojo en `dev` por `dart format` de `diseno_flutter` (de Pablo) | la PR no queda verde por algo ajeno | se declara aparte; no se toca `diseno_flutter` |
| Goldens de la pantalla de detalle de miembro | difieren tras el cambio | se regeneran solo en Linux (CI), docs incluido |
| Dos saltos para un perfil (app → identidad → grupos) | latencia | un salto por servicio, en paralelo desde la app; timeout y circuito ya existentes |
