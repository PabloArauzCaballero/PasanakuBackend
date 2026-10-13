import 'package:flutter/material.dart';

import 'hundido_al_tocar.dart';
import '../tokens/tokens.dart';
import 'decoracion_de_campo.dart';
import 'hoja_de_seleccion.dart';

class CampoDeSeleccion<T> extends StatelessWidget {
  const CampoDeSeleccion({
    super.key,
    required this.etiqueta,
    required this.opciones,
    required this.onElegida,
    this.valor,
    this.ayuda,
    this.ejemplo = '',
    this.ayudaEnHoja,
    this.error,
    this.exito = false,
    this.habilitado = true,
    this.deshabilitadoPorque,
    this.icono,
    this.textoVacio,
    this.foco,
    this.tecladoDeBusqueda = TextInputType.text,
  });

  final String etiqueta;
  final List<({T valor, String texto, String? detalle})> opciones;
  final T? valor;
  final ValueChanged<T> onElegida;
  final String? ayuda;
  final String? ejemplo;
  final String? ayudaEnHoja;
  final String? error;
  final bool exito;
  final bool habilitado;
  final String? deshabilitadoPorque;
  final IconData? icono;
  final String? textoVacio;
  final FocusNode? foco;
  final TextInputType tecladoDeBusqueda;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final estilo = Theme.of(context).textTheme;
    if (opciones.length == 1) {
      final unica = opciones.single;
      return Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          if (etiqueta.isNotEmpty) ...[
            Text(etiqueta, style: estilo.labelLarge?.copyWith(color: t.text2)),
            const SizedBox(height: Espacio.s1),
          ],
          Semantics(
            container: true,
            label: '$etiqueta: ${unica.texto}',
            child: Padding(
              padding: const EdgeInsets.symmetric(vertical: Espacio.s2),
              child: Text(
                unica.texto,
                style: estilo.bodyLarge?.copyWith(color: t.text),
              ),
            ),
          ),
          if (!habilitado && deshabilitadoPorque != null)
            Padding(
              padding: const EdgeInsets.only(top: Espacio.s1),
              child: Text(
                deshabilitadoPorque!,
                style: Tipo.ayuda.copyWith(color: t.text2),
              ),
            ),
        ],
      );
    }
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        if (etiqueta.isNotEmpty) ...[
          Text(etiqueta, style: estilo.labelLarge?.copyWith(color: t.text2)),
          const SizedBox(height: Espacio.s1),
        ],
        _CampoQueAbreLaHoja<T>(campo: this),
        if (!habilitado && deshabilitadoPorque != null)
          Padding(
            padding: const EdgeInsets.only(top: Espacio.s1),
            child: Text(
              deshabilitadoPorque!,
              style: Tipo.ayuda.copyWith(color: t.text2),
            ),
          ),
      ],
    );
  }
}

class _CampoQueAbreLaHoja<T> extends StatelessWidget {
  const _CampoQueAbreLaHoja({required this.campo});
  final CampoDeSeleccion<T> campo;

  Future<void> _abrir(BuildContext context) async {
    final actual = campo.opciones.indexWhere((o) => o.valor == campo.valor);
    final seleccion = await mostrarHojaDeSeleccion(
      context,
      titulo: campo.etiqueta,
      opciones: [
        for (final opcion in campo.opciones)
          (texto: opcion.texto, detalle: opcion.detalle),
      ],
      elegida: actual < 0 ? null : actual,
      ayuda: campo.ayudaEnHoja ?? campo.ayuda,
      teclado: campo.tecladoDeBusqueda,
    );
    if (seleccion != null) campo.onElegida(campo.opciones[seleccion].valor);
    campo.foco?.requestFocus();
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final estilo = Theme.of(context).textTheme;
    final elegida = campo.opciones.where((o) => o.valor == campo.valor);
    final vacio = elegida.isEmpty;
    final nombre = vacio
        ? (campo.textoVacio ?? 'Elegí una opción')
        : elegida.first.texto;
    return HundidoAlTocar(
      activo: campo.habilitado,
      child: Semantics(
        button: true,
        enabled: campo.habilitado,
        label: campo.etiqueta,
        value: vacio ? null : elegida.first.texto,
        hint: vacio ? 'Tocá para elegir' : 'Tocá para cambiar',
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
              ejemplo: campo.ejemplo,
              error: campo.error,
              exito: campo.exito,
              habilitado: campo.habilitado,
              icono: campo.icono,
              sufijo: Icon(Icons.unfold_more_rounded, color: t.text3),
            ),
            child: Text(
              vacio ? (campo.textoVacio ?? 'Elegí una opción') : nombre,
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
              style: estilo.bodyLarge?.copyWith(
                color: vacio ? t.text3 : t.text,
              ),
            ),
          ),
        ),
      ),
    );
  }
}
