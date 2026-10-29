import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moviles/hoja_de_confirmacion.dart';
import 'package:aportaya_diseno/moviles/mostrar_snackbar.dart';
import 'package:aportaya_diseno/organismos/estado_de_pantalla.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:aportaya_diseno/errores.dart';
import 'compartir_invitacion.dart';
import 'dominio/enlace_del_grupo.dart';
import 'textos_enlace_del_grupo.dart';
import 'vista_enlace_del_grupo.dart';

/// «Invitar al grupo» (CU-69): dos formas, por QR y por enlace (decisión del dueño,
/// 2026-10-20). El enlace se crea solo al entrar si no hay uno vigente; el QR sale como
/// imagen para el grupo de WhatsApp. Desde acá se ve quién pidió unirse.
class PantallaEnlaceDelGrupo extends ConsumerStatefulWidget {
  const PantallaEnlaceDelGrupo({
    super.key,
    required this.grupoId,
    this.nombre,
    this.appWeb,
  });
  final String grupoId;
  final String? nombre;

  /// La dirección de la app web; por omisión la de la compilación (`APP_WEB`/`API`).
  final String? appWeb;

  @override
  ConsumerState<PantallaEnlaceDelGrupo> createState() =>
      _PantallaEnlaceDelGrupoState();
}

class _PantallaEnlaceDelGrupoState
    extends ConsumerState<PantallaEnlaceDelGrupo> {
  bool _trabajando = false;
  bool _yaIntentoCrear = false;
  Object? _error;

  Future<void> _hacer(Future<void> Function() accion) async {
    if (_trabajando) return;
    setState(() {
      _trabajando = true;
      _error = null;
    });
    try {
      await accion();
      ref.invalidate(enlaceVigenteProvider(widget.grupoId));
    } on Object catch (e) {
      if (mounted) setState(() => _error = e);
    } finally {
      if (mounted) setState(() => _trabajando = false);
    }
  }

  Future<void> _crear() =>
      _hacer(() => ref.read(enlacesDelGrupoProvider).generar(widget.grupoId));

  Future<void> _confirmar(String detalle, bool peligro, VoidCallback si) =>
      HojaDeConfirmacion.mostrar<void>(
        context,
        hoja: HojaDeConfirmacion(
          titulo: peligro
              ? TextosEnlaceDelGrupo.anular
              : TextosEnlaceDelGrupo.generarOtro,
          detalle: Alerta(titulo: detalle, tono: Tono.aviso),
          textoConfirmar: TextosEnlaceDelGrupo.confirmar,
          peligro: peligro,
          onConfirmar: () {
            Navigator.of(context).pop();
            si();
          },
        ),
      );

  /// Sin enlace vigente, se crea uno una sola vez al entrar: invitar no debería pedir
  /// un paso más. Si falla, queda el error y el botón para reintentar.
  void _crearSiHaceFalta(EnlaceGuardado? guardado) {
    if (guardado != null || _yaIntentoCrear) return;
    _yaIntentoCrear = true;
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted) _crear();
    });
  }

  Future<void> _avisar(Future<String?> envio) async {
    final aviso = await envio;
    if (aviso != null && mounted) mostrarSnackbar(context, aviso);
  }

  Future<void> _copiar(String enlace) async {
    await Clipboard.setData(ClipboardData(text: enlace));
    if (mounted) mostrarSnackbar(context, TextosEnlaceDelGrupo.enlaceCopiado);
  }

  @override
  Widget build(BuildContext context) {
    final vigente = ref.watch(enlaceVigenteProvider(widget.grupoId));
    final error = _error;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          children: [
            const CabeceraDeSeccion(titulo: TextosEnlaceDelGrupo.titulo),
            if (error != null) ...[
              Alerta(titulo: _texto(error), tono: Tono.error),
              const SizedBox(height: Espacio.s3),
            ],
            EstadoDePantalla<EnlaceGuardado?>(
              valor: vigente,
              mensajeVacio: '',
              reintentar: () =>
                  ref.invalidate(enlaceVigenteProvider(widget.grupoId)),
              exito: (guardado) {
                // Primera vez sin enlace: se ve «preparando» mientras se crea solo.
                final esperando = guardado == null && !_yaIntentoCrear;
                _crearSiHaceFalta(guardado);
                final enlace = guardado == null
                    ? null
                    : enlaceDelGrupo(guardado.codigo, baseWeb: widget.appWeb);
                return VistaEnlaceDelGrupo(
                  enlace: enlace,
                  expiraEn: guardado?.expiraEn,
                  trabajando: _trabajando || esperando,
                  alCrear: _crear,
                  alCompartirQr: (e) => _avisar(
                    compartirQrDelGrupo(ref, e, nombre: widget.nombre),
                  ),
                  alCompartirEnlace: (e) => _avisar(
                    compartirEnlaceDelGrupo(ref, e, nombre: widget.nombre),
                  ),
                  alCopiar: _copiar,
                  alGenerarOtro: () => _confirmar(
                    TextosEnlaceDelGrupo.generarOtroConfirmar,
                    false,
                    _crear,
                  ),
                  alAnular: () => _confirmar(
                    TextosEnlaceDelGrupo.anularConfirmar,
                    true,
                    () => _hacer(() async {
                      await ref
                          .read(enlacesDelGrupoProvider)
                          .anular(widget.grupoId);
                      if (mounted) {
                        mostrarSnackbar(
                          this.context,
                          TextosEnlaceDelGrupo.anulado,
                        );
                      }
                    }),
                  ),
                );
              },
            ),
            const SizedBox(height: Espacio.s5),
          ],
        ),
      ),
    );
  }
}

String _texto(Object error) => error is ErrorPresentable
    ? error.mensaje
    : 'Algo salió mal de nuestro lado. Probá de nuevo en un momento.';
