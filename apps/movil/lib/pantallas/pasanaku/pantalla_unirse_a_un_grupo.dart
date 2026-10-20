import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/cabecera.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';

import '../../dominio/puertos/lector_de_qr_en_imagen.dart';
import '../../navegacion/lectura_de_qr.dart';
import '../../proveedores/lector_de_qr_en_imagen.dart';
import 'textos_enlace_del_grupo.dart';

/// «Unirse a un grupo» (2026-10-20): la persona sube la imagen del QR que le llegó por
/// WhatsApp o pega el enlace. Acá solo se reconoce que el código es de un grupo de
/// Aporteo; que el grupo exista y siga abierto lo dice el servidor en la pantalla
/// siguiente (`/g/<código>`), donde la persona pide unirse. Nada se guarda ni se loguea.
class PantallaUnirseAUnGrupo extends ConsumerStatefulWidget {
  const PantallaUnirseAUnGrupo({super.key, this.appWeb});

  /// La dirección de la app web; por omisión la de la compilación.
  final String? appWeb;

  @override
  ConsumerState<PantallaUnirseAUnGrupo> createState() =>
      _PantallaUnirseAUnGrupoState();
}

class _PantallaUnirseAUnGrupoState
    extends ConsumerState<PantallaUnirseAUnGrupo> {
  final _enlace = TextEditingController();
  bool _leyendo = false;
  String? _errorDelQr;
  String? _errorDelEnlace;

  @override
  void dispose() {
    _enlace.dispose();
    super.dispose();
  }

  String? _ruta(String texto) =>
      rutaDeInvitacionDesdeQr(texto, appWeb: widget.appWeb);

  Future<void> _subirQr() async {
    if (_leyendo) return;
    setState(() {
      _leyendo = true;
      _errorDelQr = null;
    });
    final lectura = await ref.read(lectorDeQrEnImagenProvider).elegirYLeer();
    if (!mounted) return;
    setState(() => _leyendo = false);
    switch (lectura) {
      case LecturaCancelada():
        return;
      case LecturaSinQr():
        setState(() => _errorDelQr = TextosUnirseAUnGrupo.sinQr);
      case LecturaConQr(:final texto):
        final ruta = _ruta(texto);
        if (ruta == null) {
          setState(() => _errorDelQr = TextosUnirseAUnGrupo.qrAjeno);
        } else {
          context.push(ruta);
        }
    }
  }

  void _usarEnlace() {
    final texto = _enlace.text.trim();
    if (texto.isEmpty) {
      setState(() => _errorDelEnlace = TextosUnirseAUnGrupo.enlaceVacio);
      return;
    }
    final ruta = _ruta(texto);
    if (ruta == null) {
      setState(() => _errorDelEnlace = TextosUnirseAUnGrupo.enlaceAjeno);
      return;
    }
    setState(() => _errorDelEnlace = null);
    context.push(ruta);
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final detalle = Tipo.cuerpoChico.copyWith(color: t.text2);
    final errorDelQr = _errorDelQr;
    return Scaffold(
      body: SafeArea(
        child: ListView(
          padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
          children: [
            const CabeceraDeSeccion(titulo: TextosUnirseAUnGrupo.titulo),
            Text(
              TextosUnirseAUnGrupo.ayuda,
              style: Tipo.cuerpo.copyWith(color: t.text2),
            ),
            const SizedBox(height: Espacio.s4),
            Tarjeta(
              etiqueta: TextosUnirseAUnGrupo.conQr,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    TextosUnirseAUnGrupo.conQr,
                    style: Tipo.titulo3.copyWith(color: t.text),
                  ),
                  const SizedBox(height: Espacio.s1),
                  Text(TextosUnirseAUnGrupo.conQrDetalle, style: detalle),
                  const SizedBox(height: Espacio.s3),
                  if (errorDelQr != null) ...[
                    Semantics(
                      liveRegion: true,
                      child: Alerta(titulo: errorDelQr, tono: Tono.error),
                    ),
                    const SizedBox(height: Espacio.s3),
                  ],
                  Boton(
                    key: const ValueKey('subir-imagen-del-qr'),
                    texto: TextosUnirseAUnGrupo.subirQr,
                    variante: BotonVariante.primario,
                    expandido: true,
                    cargando: _leyendo,
                    onPressed: _subirQr,
                  ),
                ],
              ),
            ),
            const SizedBox(height: Espacio.s3),
            Tarjeta(
              etiqueta: TextosUnirseAUnGrupo.conEnlace,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  Text(
                    TextosUnirseAUnGrupo.conEnlace,
                    style: Tipo.titulo3.copyWith(color: t.text),
                  ),
                  const SizedBox(height: Espacio.s3),
                  Campo(
                    etiqueta: TextosUnirseAUnGrupo.enlaceEtiqueta,
                    controlador: _enlace,
                    tipoDeTeclado: TextInputType.url,
                    error: _errorDelEnlace,
                    onSubmitted: (_) => _usarEnlace(),
                  ),
                  const SizedBox(height: Espacio.s3),
                  Boton(
                    key: const ValueKey('buscar-grupo-por-enlace'),
                    texto: TextosUnirseAUnGrupo.buscar,
                    variante: BotonVariante.secundario,
                    expandido: true,
                    onPressed: _usarEnlace,
                  ),
                ],
              ),
            ),
            const SizedBox(height: Espacio.s3),
            Boton(
              texto: TextosUnirseAUnGrupo.usarCamara,
              variante: BotonVariante.enlace,
              expandido: true,
              onPressed: () => context.push('/pasanaku/escanear'),
            ),
            const SizedBox(height: Espacio.s5),
          ],
        ),
      ),
    );
  }
}
