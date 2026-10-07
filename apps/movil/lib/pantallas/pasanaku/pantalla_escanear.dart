import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/puertos/visor_qr.dart';
import '../../navegacion/lectura_de_qr.dart';
import '../../proveedores/ajustes_del_sistema.dart';
import '../../proveedores/visor_qr.dart';
import 'textos.dart';

/// Escanear el QR de una invitación. **Solo lleva a la pantalla de unirse**, donde la
/// persona confirma: escanear no acredita ni mueve nada. Lo que dice el QR no se guarda
/// ni se registra. Si la cámara no anda, el enlace se puede pegar a mano.
class PantallaEscanear extends ConsumerStatefulWidget {
  const PantallaEscanear({super.key});

  @override
  ConsumerState<PantallaEscanear> createState() => _PantallaEscanearState();
}

class _PantallaEscanearState extends ConsumerState<PantallaEscanear> {
  final _cerrojo = CerrojoDeLectura();
  final _enlace = TextEditingController();
  String? _rechazo;
  ProblemaDeCamara? _problemaDeCamara;

  @override
  void dispose() {
    _enlace.dispose();
    super.dispose();
  }

  /// Un QR válido reemplaza esta pantalla (así la cámara se apaga); uno inválido se
  /// avisa una sola vez y se vuelve a abrir el cerrojo pasado un momento.
  void _alLeer(String texto) {
    if (!_cerrojo.tomar()) return;
    final ruta = rutaDeInvitacionDesdeQr(texto);
    if (ruta != null) {
      context.pushReplacement(ruta);
      return;
    }
    setState(() => _rechazo = TextosPasanaku.escanearRechazo);
    Future<void>.delayed(const Duration(seconds: 2), () {
      if (mounted) _cerrojo.liberar();
    });
  }

  void _usarEnlace() {
    final ruta = rutaDeInvitacionDesdeQr(_enlace.text);
    if (ruta == null) {
      setState(() => _rechazo = TextosPasanaku.enlaceInvalido);
      return;
    }
    context.pushReplacement(ruta);
  }

  /// El visor avisa que la cámara no anda. No se puede cambiar el estado en medio del
  /// `build`, así que se anota después del cuadro y el visor sale de la pantalla: el
  /// aviso ocupa su lugar (con el botón de ajustes a la vista, no dentro de una caja).
  Widget _alFallarLaCamara(ProblemaDeCamara problema) {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (mounted && _problemaDeCamara != problema) {
        setState(() => _problemaDeCamara = problema);
      }
    });
    return const SizedBox.shrink();
  }

  Widget _avisoDeCamara(ProblemaDeCamara problema) => Alerta(
    titulo: TextosPasanaku.camaraDenegadaTitulo,
    detalle: switch (problema) {
      ProblemaDeCamara.denegado => TextosPasanaku.camaraDenegadaDetalle,
      ProblemaDeCamara.noDisponible => TextosPasanaku.camaraNoDisponibleDetalle,
      ProblemaDeCamara.otro => TextosPasanaku.camaraOtroDetalle,
    },
    tono: Tono.aviso,
    accion: problema == ProblemaDeCamara.denegado
        ? TextButton(
            onPressed: () => ref.read(ajustesDelSistemaProvider).abrir(),
            child: const Text(TextosPasanaku.abrirAjustes),
          )
        : null,
  );

  @override
  Widget build(BuildContext context) {
    final construirVisor = ref.watch(visorQrProvider);
    return Scaffold(
      appBar: AppBar(title: const Text(TextosPasanaku.tituloEscanear)),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              const Text(TextosPasanaku.escanearAyuda),
              const SizedBox(height: Espacio.s3),
              if (_problemaDeCamara case final problema?)
                _avisoDeCamara(problema)
              else
                SizedBox(
                  height: 320,
                  child: ClipRRect(
                    borderRadius: BorderRadius.circular(Radios.md),
                    child: Semantics(
                      label: TextosPasanaku.tituloEscanear,
                      child: construirVisor(
                        alLeer: _alLeer,
                        problema: _alFallarLaCamara,
                      ),
                    ),
                  ),
                ),
              if (_rechazo != null) ...[
                const SizedBox(height: Espacio.s3),
                Semantics(
                  liveRegion: true,
                  child: Alerta(titulo: _rechazo!, tono: Tono.error),
                ),
              ],
              const SizedBox(height: Espacio.s5),
              Campo(
                etiqueta: TextosPasanaku.pegarEnlaceEtiqueta,
                controlador: _enlace,
                tipoDeTeclado: TextInputType.url,
                onSubmitted: (_) => _usarEnlace(),
              ),
              const SizedBox(height: Espacio.s3),
              Boton(
                texto: TextosPasanaku.usarEnlace,
                variante: BotonVariante.secundario,
                expandido: true,
                onPressed: _usarEnlace,
              ),
            ],
          ),
        ),
      ),
    );
  }
}
