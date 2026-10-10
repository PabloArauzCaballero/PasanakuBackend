import 'package:flutter/material.dart';

import '../tokens/tokens.dart';
import 'decoracion_de_campo.dart';
import 'lista_con_buscador.dart';

/// Un campo de **una opción entre pocas**, con la misma caja que los demás.
///
/// Se ve igual que un campo de texto —mismo borde, mismo alto, misma ayuda, mismo
/// error— porque es un dato más del formulario: que el de al lado se escriba y este se
/// elija no es razón para que se vean distintos.
///
/// Ocupa **un renglón**, así que entra al lado de otro campo. Una lista de opciones
/// desplegada en la pantalla —chips, por ejemplo— empuja todo lo que viene después y
/// convierte un dato corto en el bloque más grande del formulario.
///
/// **Con más de [umbralDelBuscador] opciones, se elige buscando.** No es una opción
/// del que lo usa: es la disciplina del sistema. Un desplegable de cien años o de
/// dieciocho actividades tapa la pantalla y obliga a leerlo entero; con más opciones
/// que esas, tocar el campo abre una hoja con buscador (`lista_con_buscador.dart`).
/// El gate del frontend prohíbe armar un `DropdownButton` por fuera de este átomo,
/// así que nadie puede saltearse la regla.
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
    this.tecladoDeBusqueda = TextInputType.text,
  });

  /// Hasta acá, desplegable; desde 13 opciones, hoja con buscador. Doce deja los
  /// meses y los nueve departamentos como desplegable, que se leen de un vistazo.
  static const umbralDelBuscador = 12;

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

  /// El teclado del buscador: numérico para años y días.
  final TextInputType tecladoDeBusqueda;

  bool get _conBuscador => opciones.length > umbralDelBuscador;

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
        if (_conBuscador)
          _CampoConBuscador<T>(campo: this)
        else
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

/// La caja de un [CampoDeSeleccion] con muchas opciones: se ve igual que la del
/// desplegable, y al tocarla —o con Enter o Espacio— abre la hoja con buscador.
class _CampoConBuscador<T> extends StatelessWidget {
  const _CampoConBuscador({required this.campo});
  final CampoDeSeleccion<T> campo;

  Future<void> _abrir(BuildContext context) async {
    final textos = [for (final o in campo.opciones) o.texto];
    final actual = campo.opciones.indexWhere((o) => o.valor == campo.valor);
    final i = await mostrarListaConBuscador(
      context,
      titulo: campo.etiqueta,
      textos: textos,
      elegido: actual < 0 ? null : actual,
      teclado: campo.tecladoDeBusqueda,
    );
    if (i != null) campo.onElegida(campo.opciones[i].valor);
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final texto = Theme.of(context).textTheme;
    final elegida = campo.opciones.where((o) => o.valor == campo.valor);
    final vacio = elegida.isEmpty;
    return Semantics(
      button: true,
      label: campo.etiqueta,
      value: vacio ? null : elegida.first.texto,
      excludeSemantics: true,
      child: InkWell(
        focusNode: campo.foco,
        borderRadius: BorderRadius.circular(Radios.md),
        onTap: campo.habilitado ? () => _abrir(context) : null,
        child: InputDecorator(
          isFocused: campo.foco?.hasFocus ?? false,
          decoration: DecoracionDeCampo.de(
            context,
            ayuda: campo.ayuda,
            error: campo.error,
            exito: campo.exito,
            habilitado: campo.habilitado,
            icono: campo.icono,
            sufijo: Icon(Icons.search, color: t.text3),
          ),
          child: Text(
            vacio ? (campo.textoVacio ?? '') : elegida.first.texto,
            maxLines: 1,
            overflow: TextOverflow.ellipsis,
            style: texto.bodyLarge?.copyWith(color: vacio ? t.text3 : t.text),
          ),
        ),
      ),
    );
  }
}
