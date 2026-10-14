import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'hundido_al_tocar.dart';
import 'normalizar_para_buscar.dart';

class FilaDeSeleccion extends StatelessWidget {
  const FilaDeSeleccion({
    super.key,
    required this.texto,
    required this.detalle,
    required this.elegida,
    required this.consulta,
    required this.onTap,
  });

  final String texto;
  final String? detalle;
  final bool elegida;
  final String consulta;
  final VoidCallback onTap;

  TextSpan _resaltar(String valor, TextStyle estilo, Color color) {
    final q = normalizarParaBuscar(consulta);
    final normal = normalizarParaBuscar(valor);
    final inicio = q.isEmpty ? -1 : normal.indexOf(q);
    if (inicio < 0) {
      return TextSpan(
        text: valor,
        style: estilo.copyWith(color: color),
      );
    }
    final fin = (inicio + q.length).clamp(0, valor.length);
    return TextSpan(
      style: estilo.copyWith(color: color),
      children: [
        TextSpan(text: valor.substring(0, inicio)),
        TextSpan(
          text: valor.substring(inicio, fin),
          style: const TextStyle(fontWeight: FontWeight.w700),
        ),
        TextSpan(text: valor.substring(fin)),
      ],
    );
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final estilo = Theme.of(context).textTheme.bodyLarge ?? Tipo.cuerpo;
    return Semantics(
      button: true,
      selected: elegida,
      label: detalle == null ? texto : '$texto, $detalle',
      child: ExcludeSemantics(
        child: HundidoAlTocar(
          child: InkWell(
            onTap: onTap,
            child: Container(
              constraints: const BoxConstraints(minHeight: Tactil.minimo),
              // La hoja reserva 48 dp por fila. El detalle ya suma una segunda
              // línea, así que el padding vertical haría que la columna exceda
              // el alto táctil disponible y provoque un overflow de 6 dp.
              padding: const EdgeInsets.symmetric(horizontal: Espacio.s4),
              color: elegida ? t.brandBg : null,
              child: Row(
                children: [
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisAlignment: MainAxisAlignment.center,
                      children: [
                        RichText(
                          text: _resaltar(
                            texto,
                            estilo,
                            elegida ? t.brandTexto : t.text,
                          ),
                        ),
                        if (detalle != null)
                          RichText(
                            text: _resaltar(detalle!, Tipo.ayuda, t.text3),
                          ),
                      ],
                    ),
                  ),
                  if (elegida) Icon(Icons.check_rounded, color: t.brandTexto),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }
}
