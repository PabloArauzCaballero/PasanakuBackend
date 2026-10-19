import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'package:aportaya_diseno/errores.dart';

import '../identidad/dominio/estado_sesion.dart' show mensajeDeError;
import 'dominio/habilitacion_basica.dart';

/// Lo que ve quien todavía no organiza: qué es ser organizador, el contrato completo y
/// una aceptación explícita. Al aceptar queda habilitado como Aprendiz y vuelve al
/// formulario de crear grupo.
class ContratoDeOrganizadorVista extends ConsumerStatefulWidget {
  const ContratoDeOrganizadorVista({
    super.key,
    required this.contrato,
    required this.onAceptado,
  });

  final ContratoDeOrganizador contrato;
  final VoidCallback onAceptado;

  @override
  ConsumerState<ContratoDeOrganizadorVista> createState() =>
      _ContratoDeOrganizadorVistaState();
}

class _ContratoDeOrganizadorVistaState
    extends ConsumerState<ContratoDeOrganizadorVista> {
  bool _acepta = false;
  bool _enviando = false;
  String? _error;
  String? _traza;

  Future<void> _aceptar() async {
    setState(() {
      _enviando = true;
      _error = null;
      _traza = null;
    });
    try {
      await ref
          .read(aceptarContratoDeOrganizadorProvider)
          .ejecutar(widget.contrato);
      if (mounted) widget.onAceptado();
    } on Object catch (e) {
      if (!mounted) return;
      setState(() {
        _error = mensajeDeError(e);
        _traza = e is ErrorPresentable ? e.trazaId : null;
      });
    } finally {
      if (mounted) setState(() => _enviando = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final c = widget.contrato;
    return Column(
      children: [
        Expanded(
          child: SingleChildScrollView(
            padding: const EdgeInsets.all(Espacio.s4),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Organizá tu primer grupo',
                  style: Tipo.titulo2.copyWith(color: t.text),
                ),
                const SizedBox(height: Espacio.s2),
                Text(
                  'Para crear un grupo te habilitamos como organizador nivel '
                  'Aprendiz: hasta ${c.limiteDeGrupos} grupos activos y '
                  'Bs ${c.limiteDeMonto} administrados. Vos también participás '
                  'del grupo: aportás y cobrás tu turno como los demás.',
                  style: Tipo.cuerpo.copyWith(color: t.text2),
                ),
                const SizedBox(height: Espacio.s4),
                Text(c.titulo, style: Tipo.titulo3.copyWith(color: t.text)),
                const SizedBox(height: Espacio.s2),
                Text(
                  'Tus obligaciones',
                  style: Tipo.etiqueta.copyWith(color: t.text2),
                ),
                const SizedBox(height: Espacio.s1),
                Text(
                  c.obligaciones,
                  style: Tipo.cuerpo.copyWith(color: t.text),
                ),
                const SizedBox(height: Espacio.s3),
                Text(
                  'Cuándo se termina',
                  style: Tipo.etiqueta.copyWith(color: t.text2),
                ),
                const SizedBox(height: Espacio.s1),
                Text(
                  c.causalesRescision,
                  style: Tipo.cuerpo.copyWith(color: t.text),
                ),
                if (_error != null) ...[
                  const SizedBox(height: Espacio.s3),
                  Alerta(
                    titulo: _error!,
                    detalle: _traza == null ? null : 'Referencia: $_traza',
                    tono: Tono.error,
                  ),
                ],
              ],
            ),
          ),
        ),
        Padding(
          padding: const EdgeInsets.all(Espacio.s4),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Casilla(
                etiqueta: 'Leí el contrato de organizador y lo acepto',
                valor: _acepta,
                onChanged: _enviando
                    ? null
                    : (v) => setState(() => _acepta = v),
              ),
              const SizedBox(height: Espacio.s3),
              Boton(
                texto: 'Aceptar y organizar',
                variante: BotonVariante.primario,
                expandido: true,
                cargando: _enviando,
                onPressed: _acepta && !_enviando ? _aceptar : null,
              ),
            ],
          ),
        ),
      ],
    );
  }
}
