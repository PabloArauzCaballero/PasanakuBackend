import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

import '../tokens/tokens.dart';
import 'borde_con_halo.dart';

/// Seis celdas para códigos de verificación y PIN (CU-01, CU-04). Se anuncia como un
/// solo campo; el foco avanza solo y borrar retrocede.
class CampoOTP extends StatefulWidget {
  const CampoOTP({
    super.key,
    required this.onCompleto,
    this.onCambio,
    this.largo = 6,
    this.oculto = false,
    this.error,
    this.etiqueta = 'Código de verificación',
    this.ejemplo,
    this.habilitado = true,
  });

  final ValueChanged<String> onCompleto;

  /// Cada vez que cambia lo escrito, con lo que hay hasta ese momento (completo o no):
  /// lo necesita quien muestra un botón «Confirmar» que solo se enciende con el código entero.
  final ValueChanged<String>? onCambio;
  final int largo;
  final bool oculto;
  final String? error;
  final String etiqueta;
  final String? ejemplo;
  final bool habilitado;

  @override
  State<CampoOTP> createState() => _CampoOTPState();
}

class _CampoOTPState extends State<CampoOTP> {
  late final List<TextEditingController> _c = List.generate(
    widget.largo,
    (_) => TextEditingController(),
  );
  late final List<FocusNode> _f = List.generate(
    widget.largo,
    (_) => FocusNode(),
  );

  @override
  void dispose() {
    for (final c in _c) {
      c.dispose();
    }
    for (final f in _f) {
      f.dispose();
    }
    super.dispose();
  }

  void _cambio(int i, String v) {
    if (v.length > 1) {
      // Pegado: se reparte entre las celdas.
      for (var k = 0; k < widget.largo; k += 1) {
        _c[k].text = k < v.length ? v[k] : '';
      }
      _f[(v.length - 1).clamp(0, widget.largo - 1)].requestFocus();
    } else if (v.isNotEmpty && i < widget.largo - 1) {
      _f[i + 1].requestFocus();
    } else if (v.isEmpty && i > 0) {
      _f[i - 1].requestFocus();
    }
    final todo = _c.map((c) => c.text).join();
    widget.onCambio?.call(todo);
    if (todo.length == widget.largo) {
      widget.onCompleto(todo);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Semantics(
      label: widget.etiqueta,
      textField: true,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: List.generate(widget.largo, (i) {
              return SizedBox(
                width: Tactil.minimo,
                height: Tactil.minimo + Espacio.s2,
                child: TextField(
                  enabled: widget.habilitado,
                  controller: _c[i],
                  focusNode: _f[i],
                  obscureText: widget.oculto,
                  textAlign: TextAlign.center,
                  keyboardType: TextInputType.number,
                  inputFormatters: [FilteringTextInputFormatter.digitsOnly],
                  style: texto.headlineSmall?.copyWith(
                    fontFamily: Fuente.display,
                    color: t.text,
                  ),
                  decoration: InputDecoration(
                    counterText: '',
                    filled: true,
                    fillColor: t.surface2,
                    contentPadding: EdgeInsets.zero,
                    enabledBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(Radios.md),
                      borderSide: BorderSide(
                        color: widget.error != null ? t.err : t.fieldBorder,
                        width: Borde.fino,
                      ),
                    ),
                    // El mismo foco que `Campo`: línea de marca y anillo suave.
                    focusedBorder: BordeConHalo(
                      borderRadius: BorderRadius.circular(Radios.md),
                      borderSide: BorderSide(
                        color: widget.error != null ? t.err : t.brand,
                        width: Borde.desfase,
                      ),
                      halo: widget.error != null ? t.errBg : t.halo,
                      anchoDeHalo: Borde.foco,
                    ),
                  ),
                  onChanged: (v) => _cambio(i, v),
                ),
              );
            }),
          ),
          if (widget.ejemplo != null) ...[
            const SizedBox(height: Espacio.s1),
            Text(
              widget.ejemplo!,
              style: Tipo.cuerpo.copyWith(color: t.textoDeEjemplo),
            ),
          ],
          if (widget.error != null) ...[
            const SizedBox(height: Espacio.s2),
            Text(
              widget.error!,
              style: texto.bodySmall?.copyWith(color: t.errTexto),
            ),
          ],
        ],
      ),
    );
  }
}
