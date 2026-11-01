import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import '../../dominio/puertos/visor_qr.dart';
import '../../navegacion/lectura_de_qr.dart';
import '../../proveedores/ajustes_del_sistema.dart';
import '../../proveedores/visor_qr.dart';
import 'marco_de_escaneo.dart';
import 'textos_invitacion.dart';

/// Escanear el QR de una invitación. **Solo lleva a la pantalla de unirse**, donde la
/// persona confirma: escanear no acredita ni mueve nada. Lo que dice el QR no se guarda
/// ni se registra. Si la cámara no anda, se puede copiar y pegar el enlace.
class PantallaEscanear extends ConsumerStatefulWidget {
  const PantallaEscanear({super.key});

  @override
  ConsumerState<PantallaEscanear> createState() => _PantallaEscanearState();
}

class _PantallaEscanearState extends ConsumerState<PantallaEscanear> {
  final _cerrojo = CerrojoDeLectura();
  final _enlace = TextEditingController();
  String? _rechazo;

  /// El último QR leído no era una invitación: el marco se pone rojo hasta reabrir.
  bool _qrRechazado = false;
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
    setState(() {
      _rechazo = TextosInvitacion.escanearRechazo;
      _qrRechazado = true;
    });
    Future<void>.delayed(const Duration(seconds: 2), () {
      if (!mounted) return;
      _cerrojo.liberar();
      setState(() => _qrRechazado = false);
    });
  }

  void _usarEnlace() {
    final ruta = rutaDeInvitacionDesdeQr(_enlace.text);
    if (ruta == null) {
      setState(() => _rechazo = TextosInvitacion.enlaceInvalido);
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
    titulo: TextosInvitacion.camaraDenegadaTitulo,
    detalle: switch (problema) {
      ProblemaDeCamara.denegado => TextosInvitacion.camaraDenegadaDetalle,
      ProblemaDeCamara.noDisponible =>
        TextosInvitacion.camaraNoDisponibleDetalle,
      ProblemaDeCamara.otro => TextosInvitacion.camaraOtroDetalle,
    },
    tono: Tono.aviso,
    accion: problema == ProblemaDeCamara.denegado
        ? TextButton(
            onPressed: () => ref.read(ajustesDelSistemaProvider).abrir(),
            child: const Text(TextosInvitacion.abrirAjustes),
          )
        : null,
  );

  @override
  Widget build(BuildContext context) {
    final construirVisor = ref.watch(visorQrProvider);
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: TextosInvitacion.tituloEscanear,
        cuerpo: SafeArea(
          top: false,
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(Espacio.s4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(TextosInvitacion.escanearAyuda),
                const SizedBox(height: Espacio.s3),
                if (_problemaDeCamara case final problema?)
                  _avisoDeCamara(problema)
                else
                  // Un visor cuadrado: el QR es cuadrado y así el marco calza en
                  // cualquier ancho, sin estirarse en tableta.
                  Center(
                    child: ConstrainedBox(
                      constraints: const BoxConstraints(maxWidth: 360),
                      child: AspectRatio(
                        aspectRatio: 1,
                        child: ClipRRect(
                          borderRadius: BorderRadius.circular(Radios.xl),
                          child: Semantics(
                            label: TextosInvitacion.tituloEscanear,
                            child: Stack(
                              fit: StackFit.expand,
                              children: [
                                construirVisor(
                                  alLeer: _alLeer,
                                  problema: _alFallarLaCamara,
                                ),
                                ExcludeSemantics(
                                  child: MarcoDeEscaneo(
                                    pista: TextosInvitacion.escanearPista,
                                    rechazado: _qrRechazado,
                                  ),
                                ),
                              ],
                            ),
                          ),
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
                const SizedBox(height: Espacio.s6),
                // La salida para cuando la cámara no sirve: aparte del visor, con su
                // propio título, para que no parezca parte del escaneo.
                const TituloDeSeccion(titulo: TextosInvitacion.sinCamaraTitulo),
                Tarjeta(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Campo(
                        etiqueta: TextosInvitacion.pegarEnlaceEtiqueta,
                        controlador: _enlace,
                        tipoDeTeclado: TextInputType.url,
                        onSubmitted: (_) => _usarEnlace(),
                      ),
                      const SizedBox(height: Espacio.s3),
                      Boton(
                        texto: TextosInvitacion.usarEnlace,
                        variante: BotonVariante.secundario,
                        expandido: true,
                        onPressed: _usarEnlace,
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
