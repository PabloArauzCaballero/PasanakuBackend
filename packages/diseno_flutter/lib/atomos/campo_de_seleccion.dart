import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'decoracion_de_campo.dart';

/// Un campo de **una opción entre pocas**, con la misma caja que los demás.
///
/// Se ve igual que un campo de texto —mismo borde, mismo alto, misma ayuda, mismo
/// error— porque es un dato más del formulario: que el de al lado se escriba y este se
/// elija no es razón para que se vean distintos.
///
/// Ocupa **un renglón**, así que entra al lado de otro campo. Una lista de opciones
/// desplegada en la pantalla —chips, por ejemplo— empuja todo lo que viene después y
/// convierte un dato corto en el bloque más grande del formulario.
class CampoDeSeleccion<T> extends StatelessWidget {
  const CampoDeSeleccion({
    super.key,
    required this.etiqueta,
    required this.opciones,
    required this.onElegida,
    this.valor,
    this.ayuda,
    this.error,
    this.exito = false,
    this.habilitado = true,
    this.icono,
    this.textoVacio,
    this.foco,
  });

  final String etiqueta;

  /// Valor y cómo se lee. El orden es el que se muestra: si importa, se ordena antes.
  final List<({T valor, String texto})> opciones;

  final T? valor;
  final ValueChanged<T> onElegida;

  final String? ayuda;
  final String? error;
  final bool exito;
  final bool habilitado;
  final IconData? icono;

  /// Lo que dice cuando todavía no se eligió nada.
  final String? textoVacio;
  final FocusNode? foco;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (etiqueta.isNotEmpty) ...[
          Text(etiqueta, style: texto.labelLarge?.copyWith(color: t.text2)),
          const SizedBox(height: Espacio.s1),
        ],
        DropdownButtonFormField<T>(
          initialValue: valor,
          focusNode: foco,
          isExpanded: true,
          // El menú hereda el tema, no se pinta a mano: así sigue al modo oscuro y al
          // tamaño de letra sin que nadie se acuerde de actualizarlo.
          borderRadius: BorderRadius.circular(Radios.md),
          icon: Icon(Icons.expand_more, color: t.text3),
          style: texto.bodyLarge?.copyWith(color: t.text),
          hint: textoVacio == null
              ? null
              : Text(
                  textoVacio!,
                  style: texto.bodyLarge?.copyWith(color: t.text3),
                  overflow: TextOverflow.ellipsis,
                ),
          decoration: DecoracionDeCampo.de(
            context,
            ayuda: ayuda,
            error: error,
            exito: exito,
            habilitado: habilitado,
            icono: icono,
          ),
          items: [
            for (final o in opciones)
              DropdownMenuItem<T>(
                value: o.valor,
                child: Text(o.texto, overflow: TextOverflow.ellipsis),
              ),
          ],
          onChanged: habilitado
              ? (elegida) {
                  if (elegida != null) onElegida(elegida);
                }
              : null,
        ),
      ],
    );
  }
}
