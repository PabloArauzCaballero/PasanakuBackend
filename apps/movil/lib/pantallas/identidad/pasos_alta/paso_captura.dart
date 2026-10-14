import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../dominio/camara_identidad.dart';
import '../textos.dart';

/// Un paso de captura (anverso, reverso o prueba de vida) — CU-01 flujo 3.
///
/// **Gate del carril:** con poca luz o permiso denegado, `Camara.capturar` (el
/// puerto de F2) devuelve `null` — no lanza y no cuelga. Esta pantalla lo trata
/// como una alternativa, nunca como un error fatal: ofrece reintentar o seguir a
/// mano si `permitirEscribirAMano` es true.
class PasoCaptura extends ConsumerStatefulWidget {
  const PasoCaptura({
    super.key,
    required this.titulo,
    required this.esDocumento,
    required this.onCapturado,
    required this.rutaActual,
    required this.onContinuar,
    this.permitirEscribirAMano = true,
  });

  final String titulo;
  final bool esDocumento;
  final ValueChanged<String?> onCapturado;
  final String? rutaActual;
  final VoidCallback onContinuar;
  final bool permitirEscribirAMano;

  @override
  ConsumerState<PasoCaptura> createState() => _PasoCapturaState();
}

class _PasoCapturaState extends ConsumerState<PasoCaptura> {
  bool _fallo = false;
  bool _aMano = false;

  Future<void> _capturar() async {
    final ruta = await ref
        .read(camaraProvider)
        .capturar(esDocumento: widget.esDocumento);
    setState(() => _fallo = ruta == null);
    widget.onCapturado(ruta);
  }

  /// La foto que ya está en el teléfono. Siempre visible, no solo cuando la cámara
  /// falla: mucha gente ya tiene la foto de su carnet, y hay teléfonos con la cámara
  /// rota. En el simulador de iOS —que no tiene cámara ni puede usar la del Mac— es
  /// además la única forma de recorrer el alta entera.
  Future<void> _elegir() async {
    final ruta = await ref.read(camaraProvider).elegirDeLasFotos();
    if (ruta == null) return;
    setState(() => _fallo = false);
    widget.onCapturado(ruta);
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.all(Espacio.s4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Text(widget.titulo),
          const SizedBox(height: Espacio.s4),
          if (_fallo && !_aMano)
            const Alerta(titulo: TextosIdentidad.sinCamara, tono: Tono.aviso),
          if (widget.rutaActual != null && !_fallo)
            Container(
              height: 160,
              alignment: Alignment.center,
              decoration: BoxDecoration(
                border: Border.all(color: Tokens.of(context).border),
                borderRadius: BorderRadius.circular(Radios.md),
              ),
              child: const Icon(Icons.check_circle_outline, size: 40),
            ),
          const SizedBox(height: Espacio.s4),
          if (!_aMano) ...[
            Boton(
              texto: _fallo ? TextosIdentidad.reintentarCaptura : 'Tomar foto',
              icono: Icons.photo_camera_outlined,
              variante: BotonVariante.primario,
              expandido: true,
              onPressed: _capturar,
            ),
            const SizedBox(height: Espacio.s2),
            Boton(
              texto: TextosIdentidad.elegirDeLasFotos,
              icono: Icons.photo_library_outlined,
              variante: BotonVariante.fantasma,
              expandido: true,
              onPressed: _elegir,
            ),
          ],
          if (_fallo && widget.permitirEscribirAMano) ...[
            const SizedBox(height: Espacio.s3),
            Boton(
              texto: TextosIdentidad.escribirAMano,
              variante: BotonVariante.fantasma,
              expandido: true,
              onPressed: () => setState(() => _aMano = true),
            ),
          ],
          if (_aMano)
            const Padding(
              padding: EdgeInsets.only(top: Espacio.s3),
              child: Text(
                'Seguí con los datos que ya escribiste a mano; el equipo de '
                'soporte revisa la foto pendiente después.',
              ),
            ),
          const SizedBox(height: Espacio.s3),
          Boton(
            texto: TextosIdentidad.continuar,
            variante: BotonVariante.secundario,
            expandido: true,
            onPressed: (widget.rutaActual != null || _aMano)
                ? widget.onContinuar
                : null,
          ),
        ],
      ),
    );
  }
}
