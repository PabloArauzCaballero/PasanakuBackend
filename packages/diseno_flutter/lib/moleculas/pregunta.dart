import 'package:flutter/material.dart';

import '../tokens/tokens.dart';

/// Una pregunta de un formulario: etiqueta arriba, control abajo, ayuda debajo.
///
/// El formulario deja de ser una pila de campos y pasa a ser una conversación: cada
/// bloque pregunta una cosa. La ayuda es el lugar donde **se calcula la consecuencia**
/// («Con 10 personas, cada turno junta Bs 2.000,00»), que sirve mucho más que repetir
/// el formato esperado.
class Pregunta extends StatelessWidget {
  const Pregunta({
    super.key,
    required this.etiqueta,
    required this.control,
    this.ayuda,
  });

  final String etiqueta;
  final Widget control;
  final String? ayuda;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Text(etiqueta, style: Tipo.campoEtiqueta.copyWith(color: t.text2)),
        const SizedBox(height: Espacio.s2),
        control,
        if (ayuda != null) ...[
          const SizedBox(height: Espacio.s2),
          Text(ayuda!, style: Tipo.ayuda.copyWith(color: t.text3)),
        ],
      ],
    );
  }
}

/// Un número que se sube y se baja de a uno, con área táctil de verdad en los dos
/// botones. Para «cuántas personas», donde escribir un número con el teclado es más
/// trabajo del que vale.
class Contador extends StatelessWidget {
  const Contador({
    super.key,
    required this.valor,
    required this.onCambio,
    this.minimo = 1,
    this.maximo = 99,
    this.etiqueta = 'Cantidad',
  });

  final int valor;
  final ValueChanged<int> onCambio;
  final int minimo;
  final int maximo;
  final String etiqueta;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      label: etiqueta,
      value: '$valor',
      child: Container(
        height: Tactil.minimo + Espacio.s1,
        decoration: BoxDecoration(
          color: t.field,
          borderRadius: BorderRadius.circular(Radios.md),
          border: Border.all(color: t.fieldBorder, width: Borde.fino),
        ),
        padding: const EdgeInsets.only(left: Espacio.s3, right: Espacio.s1),
        child: Row(
          children: [
            Expanded(
              child: ExcludeSemantics(
                child: Text(
                  '$valor',
                  style: Tipo.cifra.copyWith(color: t.text),
                ),
              ),
            ),
            _Paso(
              icono: Icons.remove,
              etiqueta: 'Quitar uno',
              onTap: valor > minimo ? () => onCambio(valor - 1) : null,
            ),
            const SizedBox(width: Espacio.s1),
            _Paso(
              icono: Icons.add,
              etiqueta: 'Agregar uno',
              onTap: valor < maximo ? () => onCambio(valor + 1) : null,
            ),
          ],
        ),
      ),
    );
  }
}

class _Paso extends StatelessWidget {
  const _Paso({required this.icono, required this.etiqueta, this.onTap});

  final IconData icono;
  final String etiqueta;
  final VoidCallback? onTap;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Semantics(
      button: true,
      enabled: onTap != null,
      label: etiqueta,
      child: Material(
        color: t.surface2,
        borderRadius: BorderRadius.circular(Radios.sm),
        child: InkWell(
          onTap: onTap,
          borderRadius: BorderRadius.circular(Radios.sm),
          child: SizedBox(
            width: Tactil.minimo - Espacio.s2,
            height: Tactil.minimo - Espacio.s2,
            child: Icon(
              icono,
              size: 20,
              color: onTap == null ? t.text3 : t.text,
            ),
          ),
        ),
      ),
    );
  }
}
