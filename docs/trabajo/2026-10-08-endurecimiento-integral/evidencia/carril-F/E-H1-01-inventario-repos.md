# E-H1-01 · Inventario de repos, ramas, HEAD y cambios locales
Generado: 2026-10-08T11:52:50-04:00 · SOLO LECTURA (git rev-parse, branch, log, status, worktree list). El árbol del backend cambia mientras otros carriles trabajan: esta es una instantánea.

## PasanakuBackend
$ git -C PasanakuBackend branch --show-current
pablo/troncal/security-cves-f04
$ git -C PasanakuBackend rev-parse HEAD
24deb3ebc31290bd378cdcab77fbb1a1e3bfece0
$ git -C PasanakuBackend log -1 --format='%h %ad %s' --date=short
24deb3eb 2026-10-07 feat(identidad): verificar correo del alta mediante Gmail API
$ git -C PasanakuBackend status --short   # 78 entradas
 M docs/Restricciones.md
 M docs/entidades/01_identidad_usuarios.puml
 M docs/entidades/02_grupos_turnos.puml
 M docs/entidades/10_billetera_custodia.puml
 M scripts/modelo.py
 M seeders/minimos/13-politicas-de-token.json
 M servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU20CrearGrupo.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU68AceptarIngreso.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU68Postular.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CU69Invitar.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/dominio/puertos/HechosDeOtrosServicios.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/CreacionRepositorio.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/InvitacionRepositorio.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/clientes/HechosPorHttp.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/web/GruposController.java
 M servicios/grupos/src/main/java/bo/aportaya/grupos/web/MapeoDeAltaDeGrupo.java
 M servicios/grupos/src/main/resources/openapi/grupos.yaml
 M servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU20.java
 M servicios/grupos/src/test/java/bo/aportaya/grupos/BaseDeCU69.java
 M servicios/grupos/src/test/java/bo/aportaya/grupos/CU20Test.java
 M servicios/grupos/src/test/java/bo/aportaya/grupos/CU69Test.java
 M servicios/grupos/src/test/java/bo/aportaya/grupos/FixturaDeGrupos.java
 M servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposControllerWebTest.java
 M servicios/grupos/src/test/java/bo/aportaya/grupos/web/GruposPostulacionWebTest.java
 M servicios/grupos/src/test/java/bo/aportaya/grupos/web/SeguridadWebTest.java
 M servicios/identidad/src/main/java/bo/aportaya/identidad/aplicacion/EmitirTokenDeInvitacion.java
 M servicios/identidad/src/main/java/bo/aportaya/identidad/web/UsuariosController.java
 M servicios/identidad/src/main/resources/application.yml
 M servicios/identidad/src/main/resources/openapi/identidad.yaml
 M servicios/identidad/src/test/java/bo/aportaya/identidad/web/SeguridadWebTest.java
 M servicios/identidad/src/test/java/bo/aportaya/identidad/web/UsuariosControllerWebTest.java
 M servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/CU10RecargarSaldo.java
 M servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/LibroDeBilletera.java
 M servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/OrdenRecargaRepositorio.java
 M servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/web/BilleteraController.java
 M servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10ConcurrenciaTest.java
 M servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10RechazosTest.java
 M servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10Test.java
 M servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/FixturaDeBilletera.java
 M servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/BilleteraControllerWebTest.java
 M servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/web/SeguridadWebTest.java
 M servicios/organizador/src/main/java/bo/aportaya/organizador/aplicacion/ConsultarHabilitacion.java
 M servicios/organizador/src/main/java/bo/aportaya/organizador/web/OrganizadoresController.java
 M servicios/organizador/src/main/resources/openapi/organizador.yaml
 M sql/00_base/03_permisos.sql
 M sql/10_tablas/02_grupos_turnos/invitacion.sql
 M sql/15_infra/nulabilidad.sql
 M sql/20_claves/01_identidad_usuarios.sql
 M sql/30_indices/01_identidad_usuarios.sql
 M sql/30_indices/02_grupos_turnos.sql
 M sql/35_append_only/append_only.sql
 M sql/40_reglas/restricciones.sql
 M sql/60_semillas/13-politicas-de-token.sql
 M sql/aplicar.sql
?? docs/trabajo/
?? herramientas/
?? servicios/grupos/src/main/java/bo/aportaya/grupos/aplicacion/CanjearInvitacion.java
?? servicios/grupos/src/main/java/bo/aportaya/grupos/infraestructura/AdmisionRepositorio.java
?? servicios/grupos/src/main/java/bo/aportaya/grupos/web/AdmisionDelGrupo.java
?? servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeAdmision.java
?? servicios/grupos/src/main/java/bo/aportaya/grupos/web/RutasDeInvitacion.java
?? servicios/grupos/src/test/java/bo/aportaya/grupos/CU68AdmisionTest.java
?? servicios/grupos/src/test/java/bo/aportaya/grupos/CU69CanjeTest.java
?? servicios/identidad/src/main/java/bo/aportaya/identidad/aplicacion/ConsultarNivelKyc.java
?? servicios/identidad/src/main/java/bo/aportaya/identidad/aplicacion/ConsumirInvitacion.java
?? servicios/identidad/src/main/java/bo/aportaya/identidad/infraestructura/SecretoDeInvitacion.java
?? servicios/identidad/src/main/java/bo/aportaya/identidad/web/InvitacionesController.java
?? servicios/identidad/src/test/java/bo/aportaya/identidad/CU69EmisionYAuditoriaTest.java
?? servicios/identidad/src/test/java/bo/aportaya/identidad/CU69TokenInvitacionTest.java
?? servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/aplicacion/RecargasConProveedor.java
?? servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/dominio/puertos/ProveedorDeRecargas.java
?? servicios/nucleo-financiero/src/main/java/bo/aportaya/nucleofinanciero/infraestructura/RecargasProveedorSimulado.java
?? servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/CU10ProveedorTest.java
?? servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/ConfirmacionesDePrueba.java
?? servicios/nucleo-financiero/src/test/java/bo/aportaya/nucleofinanciero/infraestructura/
?? sql/10_tablas/01_identidad_usuarios/alcance_invitacion.sql
?? sql/10_tablas/02_grupos_turnos/alta_grupo.sql
?? sql/10_tablas/02_grupos_turnos/decision_ingreso.sql
$ git -C PasanakuBackend worktree list
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend                                                                                                    24deb3eb [pablo/troncal/security-cves-f04]
C:/Users/Usuario/AppData/Local/Temp/claude/C--Users-Usuario-Downloads-Entrypoint-GitHUb-PasanakuPromptManager/a405ca9a-2c10-4ebd-b77c-7c31bcfc8dba/scratchpad/pb-leo-ci  c78d8ec9 [wt-leo-ci]
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend-pablo-pr5-cierre                                                                                   a36e7644 [pablo/feature/carril-PR5-cierre]
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend-richard-pr11                                                                                       94b37d05 [richard/frontend/sesion]
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuBackend-richard-pr6                                                                                        9cdb7cfd [richard/frontend/pantalla-piloto]
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuDeploy                                                                                                     59370bbd [pablo/feature/verificacion-correo-gmail-test]

## PasanakuFrontend
$ git -C PasanakuFrontend branch --show-current
codex/mobile-design-handoff-2026-10-07
$ git -C PasanakuFrontend rev-parse HEAD
f744b719e9c066e2c33bc58a7e8aea73bcde00e1
$ git -C PasanakuFrontend log -1 --format='%h %ad %s' --date=short
f744b71 2026-10-07 feat(mobile): ship design motion UX slice
$ git -C PasanakuFrontend status --short   # 112 entradas
 M apps/web/capturas/catalogo-dark-escritorio.png
 M apps/web/capturas/catalogo-dark-telefono.png
 M apps/web/capturas/catalogo-light-escritorio.png
 M apps/web/capturas/catalogo-light-telefono.png
 M apps/web/public/legal/estado-regulatorio.md
 M packages/simulado/ejemplos/aportes/aprobarReembolso.json
 M packages/simulado/ejemplos/aportes/cobrarAporte.json
 M packages/simulado/ejemplos/aportes/darDeAltaProveedor.json
 M packages/simulado/ejemplos/aportes/enrutarCobro.json
 M packages/simulado/ejemplos/aportes/registrarDisputa.json
 M packages/simulado/ejemplos/aportes/solicitarReembolso.json
 M packages/simulado/ejemplos/auditoria/descargarExportacion.json
 M packages/simulado/ejemplos/auditoria/ejecutarReporte.json
 M packages/simulado/ejemplos/auditoria/ejercerDerechos.json
 M packages/simulado/ejemplos/auditoria/publicarTablero.json
 M packages/simulado/ejemplos/cumplimiento/aceptarContrato.json
 M packages/simulado/ejemplos/cumplimiento/declararPep.json
 M packages/simulado/ejemplos/cumplimiento/elevarDiligencia.json
 M packages/simulado/ejemplos/cumplimiento/notificarTitulares.json
 M packages/simulado/ejemplos/cumplimiento/registrarIncidente.json
 M packages/simulado/ejemplos/cumplimiento/registrarRiesgoOperativo.json
 M packages/simulado/ejemplos/cumplimiento/reportarIncidente.json
 M packages/simulado/ejemplos/cumplimiento/verificarAlcance.json
 M packages/simulado/ejemplos/entregas/anotarRespuestaDesembolso.json
 M packages/simulado/ejemplos/entregas/autorizarEntrega.json
 M packages/simulado/ejemplos/entregas/consultarDisponibilidad.json
 M packages/simulado/ejemplos/entregas/designarCuentaPrincipal.json
 M packages/simulado/ejemplos/entregas/ejecutarEntrega.json
 M packages/simulado/ejemplos/entregas/emitirOrdenDesembolso.json
 M packages/simulado/ejemplos/entregas/liquidarEntrega.json
 M packages/simulado/ejemplos/entregas/registrarCuentaDestino.json
 M packages/simulado/ejemplos/entregas/verificarCuentaDestino.json
 M packages/simulado/ejemplos/garantia/aprobarReemplazo.json
 M packages/simulado/ejemplos/garantia/cerrarDisolucion.json
 M packages/simulado/ejemplos/garantia/cubrirIncumplimiento.json
 M packages/simulado/ejemplos/garantia/declararIncumplimiento.json
 M packages/simulado/ejemplos/garantia/devolverFondo.json
 M packages/simulado/ejemplos/garantia/ejecutarAval.json
 M packages/simulado/ejemplos/garantia/ejecutarReemplazo.json
 M packages/simulado/ejemplos/garantia/iniciarDisolucion.json
 M packages/simulado/ejemplos/garantia/levantarRestriccion.json
 M packages/simulado/ejemplos/garantia/presentarDescargo.json
 M packages/simulado/ejemplos/garantia/proponerReemplazo.json
 M packages/simulado/ejemplos/garantia/resolverDescargo.json
 M packages/simulado/ejemplos/garantia/restringirDeudor.json
 M packages/simulado/ejemplos/grupos/calcularPlazoHabil.json
 M packages/simulado/ejemplos/grupos/comprometerSorteo.json
 M packages/simulado/ejemplos/grupos/crearGrupo.json
 M packages/simulado/ejemplos/grupos/invitarAlGrupo.json
 M packages/simulado/ejemplos/grupos/postularAlGrupo.json
 M packages/simulado/ejemplos/grupos/proponerAcuerdo.json
 M packages/simulado/ejemplos/grupos/revelarSorteo.json
 M packages/simulado/ejemplos/grupos/solicitarPermuta.json
 M packages/simulado/ejemplos/grupos/solicitarRetiro.json
 M packages/simulado/ejemplos/grupos/traspasarCupo.json
 M packages/simulado/ejemplos/grupos/votarAcuerdo.json
 M packages/simulado/ejemplos/identidad/autenticar.json
 M packages/simulado/ejemplos/identidad/registrarUsuario.json
 M packages/simulado/ejemplos/notificaciones/procesarRespuestaEntrante.json
 M packages/simulado/ejemplos/nucleo-financiero/acreditarRecarga.json
 M packages/simulado/ejemplos/nucleo-financiero/bloquearPorAutoridad.json
 M packages/simulado/ejemplos/nucleo-financiero/cerrarRetencion.json
 M packages/simulado/ejemplos/nucleo-financiero/emitirExtracto.json
 M packages/simulado/ejemplos/nucleo-financiero/retenerSaldo.json
 M packages/simulado/ejemplos/nucleo-financiero/reversarTransaccion.json
 M packages/simulado/ejemplos/nucleo-financiero/solicitarCierreBilletera.json
 M packages/simulado/ejemplos/nucleo-financiero/solicitarRecarga.json
 M packages/simulado/ejemplos/nucleo-financiero/solicitarRetiro.json
 M packages/simulado/ejemplos/nucleo-financiero/transferirSaldo.json
 M packages/simulado/ejemplos/organizador/activarRegla.json
 M packages/simulado/ejemplos/organizador/anotarEjecucion.json
 M packages/simulado/ejemplos/organizador/apelarSancion.json
 M packages/simulado/ejemplos/organizador/aprobarPostulacion.json
 M packages/simulado/ejemplos/organizador/aprobarTarea.json
 M packages/simulado/ejemplos/organizador/definirRegla.json
 M packages/simulado/ejemplos/organizador/emitirContrato.json
 M packages/simulado/ejemplos/organizador/evaluarDesempeno.json
 M packages/simulado/ejemplos/organizador/firmarContrato.json
 M packages/simulado/ejemplos/organizador/habilitarOrganizador.json
 M packages/simulado/ejemplos/organizador/postularOrganizador.json
 M packages/simulado/ejemplos/organizador/programarTarea.json
 M packages/simulado/ejemplos/organizador/rescindirContrato.json
 M packages/simulado/ejemplos/organizador/resolverApelacion.json
 M packages/simulado/ejemplos/organizador/sancionarOrganizador.json
 M packages/simulado/ejemplos/tarifas/aceptarCotizacion.json
 M packages/simulado/ejemplos/tarifas/anotarCobroDeComision.json
 M packages/simulado/ejemplos/tarifas/cerrarLiquidacion.json
 M packages/simulado/ejemplos/tarifas/cotizarComision.json
 M packages/simulado/ejemplos/tarifas/crearSegmento.json
 M packages/simulado/ejemplos/tarifas/devengarComision.json
 M packages/simulado/ejemplos/tarifas/devolverComision.json
 M packages/simulado/ejemplos/tarifas/emitirFactura.json
 M packages/simulado/ejemplos/tarifas/ponerTarifarioVigente.json
 M packages/simulado/ejemplos/tarifas/publicarTarifario.json
 M packages/simulado/ejemplos/tarifas/resolverPrecio.json
 M packages/simulado/scripts/contratos-a-json.mjs
 M packages/simulado/scripts/generar-ejemplos.mjs
?? .claude/hooks/
?? .claude/rules/
?? docs/trabajo/
?? packages/simulado/ejemplos/grupos/canjearInvitacion.json
?? packages/simulado/ejemplos/grupos/historialAdmision.json
?? packages/simulado/ejemplos/grupos/proponerAdmision.json
?? packages/simulado/ejemplos/grupos/resolverAdmision.json
?? packages/simulado/ejemplos/identidad/confirmarVerificacionCorreo.json
?? packages/simulado/ejemplos/identidad/consumirInvitacion.json
?? packages/simulado/ejemplos/identidad/revocarInvitacion.json
?? packages/simulado/ejemplos/identidad/solicitarVerificacionCorreo.json
?? packages/simulado/ejemplos/identidad/verContenidoDeFoto.json
?? packages/simulado/ejemplos/organizador/consultarHabilitacionPersonal.json
?? packages/simulado/pruebas/catalogo-de-errores.spec.ts
?? packages/simulado/src/catalogo.ts
$ git -C PasanakuFrontend worktree list
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuFrontend        f744b71 [codex/mobile-design-handoff-2026-10-07]
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/wt-mobile-design-merge  6c51c74 [codex/mobile-design-merge-2026-10-07]

## wt-mobile-design-merge
$ git -C wt-mobile-design-merge branch --show-current
codex/mobile-design-merge-2026-10-07
$ git -C wt-mobile-design-merge rev-parse HEAD
6c51c74a01b9d3dd534e60e3b8ce968e172ddfeb
$ git -C wt-mobile-design-merge log -1 --format='%h %ad %s' --date=short
6c51c74 2026-10-08 fix(movil): no confirmar invitación sin respuesta válida
$ git -C wt-mobile-design-merge status --short   # 14 entradas
 M apps/movil/lib/pantallas/pasanaku/detalle_de_invitacion.dart
 M apps/movil/lib/pantallas/pasanaku/pantalla_unirse.dart
 M apps/movil/test/goldens/invitacion_aceptacion_golden_test.dart
 M apps/movil/test/goldens/invitacion_reintento_golden_test.dart
 M apps/movil/test/pasanaku/invitacion_aceptacion_test.dart
 M packages/diseno_flutter/lib/moleculas/alerta.dart
?? apps/movil/test/goldens/imagenes/invitacion_conexion_fallida_320_dark_texto_200.png
?? apps/movil/test/goldens/imagenes/invitacion_conexion_fallida_320_light_texto_200.png
?? apps/movil/test/goldens/imagenes/invitacion_conexion_pendiente_320_dark_texto_200.png
?? apps/movil/test/goldens/imagenes/invitacion_conexion_pendiente_320_light_texto_200.png
?? apps/movil/test/goldens/imagenes/invitacion_conexion_sin_red_320_dark_texto_200.png
?? apps/movil/test/goldens/imagenes/invitacion_conexion_sin_red_320_light_texto_200.png
?? apps/movil/test/goldens/imagenes/invitacion_conexion_sin_red_768_dark_texto_100.png
?? apps/movil/test/goldens/invitacion_conexion_golden_test.dart
$ git -C wt-mobile-design-merge worktree list
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/PasanakuFrontend        f744b71 [codex/mobile-design-handoff-2026-10-07]
C:/Users/Usuario/Downloads/Entrypoint-GitHUb/Pasanaku/wt-mobile-design-merge  6c51c74 [codex/mobile-design-merge-2026-10-07]

## Resumen por prefijo (git status --short)
PasanakuBackend: modificados=54 sin-seguimiento=24 otros=0
PasanakuFrontend: modificados=97 sin-seguimiento=15 otros=0
wt-mobile-design-merge: modificados=6 sin-seguimiento=8 otros=0

## Lectura del inventario (hechos y diferencias)

- **Backend**: HEAD `24deb3eb…` coincide con el declarado en NOTAS.md y en `investigacion/.../INFORME.md` ("Diferencias con la documentación"), pero **el árbol ya no está limpio**: 54 modificados + 24 sin seguimiento (código de Codex: grupos, identidad, organizador, núcleo financiero, SQL fuente `docs/entidades/*.puml`/`scripts/modelo.py` y SQL generado, `herramientas/` y `docs/trabajo/`). No se tocó.
- **Frontend**: NOTAS.md declara como cambios preexistentes 4 capturas, `estado-regulatorio.md`, `.claude/hooks|rules`, y 1 ejemplo. El `git status` actual muestra además **100 archivos** bajo `packages/simulado/ejemplos` modificados (p. ej. `grupos/crearGrupo.json`: pasa de `"codigo":"AP-CU4-44","mensaje":"mensaje"` a `"AP-CU20-01","Sin tarifario vigente"`), `packages/simulado/scripts/{contratos-a-json,generar-ejemplos}.mjs`, `packages/simulado/src/catalogo.ts` y `pruebas/catalogo-de-errores.spec.ts` sin seguimiento. Es trabajo de H3.S1.M1 (catálogo de errores) que NOTAS.md no lista: **discrepancia documental a conciliar por quien lo hizo**; no es evidencia de que esté verificado.
- **wt-mobile-design-merge**: rama `codex/mobile-design-merge-2026-10-07`, HEAD `6c51c74` (2026-10-08, "no confirmar invitación sin respuesta válida"), con 6 modificados y 8 goldens/tests sin seguimiento del móvil; no se tocó.
- Worktrees adicionales del backend visibles (`PasanakuBackend-pablo-pr5-cierre`, `-richard-pr11`, `-richard-pr6`, `PasanakuDeploy`, y un scratch `pb-leo-ci`) quedan **fuera de la base elegida**; no se leyeron ni se tocaron.
- Base elegida (según NOTAS.md): backend `pablo/troncal/security-cves-f04`, frontend `codex/mobile-design-handoff-2026-10-07`.

**No cubierto:** hashes de contenido de los cambios sin commitear; el estado de las otras ramas/worktrees; el repo PasanakuPromptManager.
