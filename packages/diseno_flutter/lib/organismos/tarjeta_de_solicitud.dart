import 'package:flutter/material.dart';

import '../atomos/avatar.dart';
import '../atomos/boton.dart';
import '../atomos/boton_variante.dart';
import '../atomos/chip_estado.dart';
import '../atomos/fecha.dart';
import '../atomos/tono.dart';
import '../moleculas/tarjeta.dart';
import '../tokens/tokens.dart';

/// El pedido de ingreso con el puntaje **descompuesto** y las dos acciones; rechazar
/// exige motivo (D-15).
class TarjetaDeSolicitud extends StatefulWidget {
  const TarjetaDeSolicitud({
    super.key,
    required this.nombre,
    required this.venceIso,
    required this.aFavor,
    required this.enContra,
    required this.onAceptar,
    required this.onRechazar,
    this.venceHoy = false,
  });
  final String nombre;
  final String venceIso;
  final List<String> aFavor;
  final List<String> enContra;
  final VoidCallback onAceptar;
  final ValueChanged<String> onRechazar;
  final bool venceHoy;

  @override
  State<TarjetaDeSolicitud> createState() => _TarjetaDeSolicitudState();
}

class _TarjetaDeSolicitudState extends State<TarjetaDeSolicitud> {
  final _motivo = TextEditingController();
  bool _rechazando = false;
  bool _enviando = false;

  @override
  void dispose() {
    _motivo.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Tarjeta(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Avatar(nombre: widget.nombre),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Text(
                  widget.nombre,
                  style: texto.titleMedium?.copyWith(color: t.text),
                ),
              ),
              ChipEstado(
                texto: widget.venceHoy
                    ? 'Vence hoy'
                    : 'Vence ${Fecha.formatear(widget.venceIso, conHora: false)}',
                tono: widget.venceHoy ? Tono.error : Tono.neutro,
              ),
            ],
          ),
          const SizedBox(height: Espacio.s3),
          for (final f in widget.aFavor)
            Row(
              children: [
                Icon(Icons.add, size: Espacio.s4, color: t.okTexto),
                const SizedBox(width: Espacio.s2),
                Expanded(
                  child: Text(
                    f,
                    style: texto.bodyMedium?.copyWith(color: t.text),
                  ),
                ),
              ],
            ),
          for (final f in widget.enContra)
            Row(
              children: [
                Icon(Icons.remove, size: Espacio.s4, color: t.errTexto),
                const SizedBox(width: Espacio.s2),
                Expanded(
                  child: Text(
                    f,
                    style: texto.bodyMedium?.copyWith(color: t.text),
                  ),
                ),
              ],
            ),
          const SizedBox(height: Espacio.s3),
          if (_rechazando) ...[
            TextField(
              controller: _motivo,
              maxLines: 2,
              decoration: InputDecoration(
                labelText: 'Motivo del rechazo (obligatorio)',
                helperText:
                    'Le llega tal cual lo escribas, con la fecha desde la que puede volver a pedir.',
                filled: true,
                fillColor: t.field,
                border: OutlineInputBorder(
                  borderRadius: BorderRadius.circular(Radios.md),
                ),
              ),
              onChanged: (_) => setState(() {}),
            ),
            const SizedBox(height: Espacio.s3),
          ],
          Row(
            children: [
              Expanded(
                child: _rechazando
                    ? Boton(
                        texto: 'Confirmar rechazo',
                        variante: BotonVariante.peligro,
                        onPressed: _motivo.text.trim().isEmpty
                            ? null
                            : () => widget.onRechazar(_motivo.text.trim()),
                      )
                    : Boton(
                        texto: 'Rechazar',
                        variante: BotonVariante.fantasma,
                        onPressed: () => setState(() => _rechazando = true),
                      ),
              ),
              const SizedBox(width: Espacio.s3),
              Expanded(
                child: Boton(
                  texto: 'Aceptar',
                  variante: BotonVariante.primario,
                  cargando: _enviando,
                  onPressed: () {
                    setState(() => _enviando = true);
                    widget.onAceptar();
                  },
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
