import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/moleculas/tarjeta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:go_router/go_router.dart';
import 'package:aportaya_diseno/organismos/pantalla_con_cabecera.dart';

import 'dominio/pautas_de_fotos.dart';
import 'ilustraciones/ilustracion_de_pauta.dart';

class PantallaDePautasDeFotos extends StatefulWidget {
  const PantallaDePautasDeFotos({super.key});

  @override
  State<PantallaDePautasDeFotos> createState() =>
      _PantallaDePautasDeFotosState();
}

class _PantallaDePautasDeFotosState extends State<PantallaDePautasDeFotos> {
  bool _selfie = false;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final consejos = _selfie ? pautasDeSelfie : pautasDeDocumento;
    return Scaffold(
      // Cabecera grande que se compacta al desplazar (ADR-053, H4.S2.M6).
      body: PantallaConCabecera(
        titulo: 'Pautas para tus fotos',
        // Llegando por enlace directo no hay a dónde volver: se vuelve al registro.
        alVolver: () {
          if (context.canPop()) {
            context.pop();
          } else {
            context.go('/registro');
          }
        },
        cuerpo: SafeArea(
          top: false,
          child: ListView(
            padding: const EdgeInsets.fromLTRB(
              Espacio.s4,
              Espacio.s2,
              Espacio.s4,
              Espacio.s7,
            ),
            children: [
              Text(
                'Así se ve una foto que se valida a la primera, y así una que no.',
                style: Tipo.cuerpo.copyWith(color: t.text),
              ),
              const SizedBox(height: Espacio.s5),
              // El selector del sistema: verde de marca, no el naranja de Material
              // (el naranja se reserva para la urgencia de pagar).
              SelectorSegmentado<bool>(
                opciones: const {false: 'Documento', true: 'Foto de frente'},
                valor: _selfie,
                onChanged: (selfie) => setState(() => _selfie = selfie),
              ),
              const SizedBox(height: Espacio.s5),
              Text(
                _selfie
                    ? 'Pautas para las fotos de rostro'
                    : 'Pautas para el documento de identidad',
                style: Tipo.titulo2.copyWith(color: t.text),
              ),
              const SizedBox(height: Espacio.s3),
              for (final consejo in consejos) _TarjetaDePauta(consejo: consejo),
            ],
          ),
        ),
      ),
    );
  }
}

class _TarjetaDePauta extends StatelessWidget {
  const _TarjetaDePauta({required this.consejo});

  final ConsejoDeFoto consejo;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    return Padding(
      padding: const EdgeInsets.only(bottom: Espacio.s3),
      child: Tarjeta(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(consejo.titulo, style: Tipo.titulo3.copyWith(color: t.text)),
            const SizedBox(height: Espacio.s3),
            LayoutBuilder(
              builder: (context, constraints) {
                final apiladas = constraints.maxWidth < 400;
                final correcta = _FichaDeEjemplo(
                  escena: consejo.correcta,
                  etiqueta: consejo.etiquetaCorrecta,
                  valida: true,
                );
                final incorrecta = _FichaDeEjemplo(
                  escena: consejo.incorrecta,
                  etiqueta: consejo.etiquetaIncorrecta,
                  valida: false,
                );
                if (apiladas) {
                  return Column(
                    children: [
                      correcta,
                      const SizedBox(height: Espacio.s3),
                      incorrecta,
                    ],
                  );
                }
                return Row(
                  children: [
                    Expanded(child: correcta),
                    const SizedBox(width: Espacio.s3),
                    Expanded(child: incorrecta),
                  ],
                );
              },
            ),
            const SizedBox(height: Espacio.s3),
            Text(consejo.texto, style: Tipo.cuerpo.copyWith(color: t.text2)),
          ],
        ),
      ),
    );
  }
}

class _FichaDeEjemplo extends StatelessWidget {
  const _FichaDeEjemplo({
    required this.escena,
    required this.etiqueta,
    required this.valida,
  });

  final EscenaDePauta escena;
  final String etiqueta;
  final bool valida;

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final color = valida ? t.okTexto : t.errTexto;
    return Semantics(
      label: '${valida ? 'Correcto' : 'Incorrecto'}: $etiqueta',
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Row(
            children: [
              Icon(
                valida ? Icons.check_circle : Icons.cancel,
                color: color,
                size: 20,
              ),
              const SizedBox(width: Espacio.s1),
              Text(
                valida ? 'Correcto' : 'Incorrecto',
                style: Tipo.etiqueta.copyWith(
                  color: color,
                  fontWeight: FontWeight.w700,
                ),
              ),
            ],
          ),
          const SizedBox(height: Espacio.s1),
          DecoratedBox(
            position: DecorationPosition.foreground,
            decoration: BoxDecoration(
              border: Border.all(color: color, width: Borde.foco),
              borderRadius: BorderRadius.circular(Radios.md),
            ),
            child: IlustracionDePauta(escena: escena),
          ),
          const SizedBox(height: Espacio.s1),
          Text(etiqueta, style: Tipo.cuerpoChico.copyWith(color: t.text2)),
        ],
      ),
    );
  }
}
