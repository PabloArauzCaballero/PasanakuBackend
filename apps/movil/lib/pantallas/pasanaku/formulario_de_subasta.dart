import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/selector_segmentado.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/dinero/formatear.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/moleculas/seccion.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/subasta_del_cupo.dart';
import 'errores_de_la_subasta.dart';
import 'tarjeta_de_subasta.dart';
import 'textos_de_la_subasta.dart';

/// Poner mi cupo en subasta: cómo funciona, el precio base y cuánto dura. Una hoja
/// confirma antes de abrirla; un rechazo queda arriba del botón y lo escrito no se pierde.
class FormularioDeSubasta extends ConsumerStatefulWidget {
  const FormularioDeSubasta({
    super.key,
    required this.grupoId,
    required this.cupoId,
  });
  final String grupoId;
  final String cupoId;

  @override
  ConsumerState<FormularioDeSubasta> createState() =>
      _FormularioDeSubastaState();
}

class _FormularioDeSubastaState extends ConsumerState<FormularioDeSubasta> {
  String? _base;
  var _horas = 48;
  var _confirmando = false;

  Future<void> _poner() async {
    if (_confirmando || _base == null) return;
    setState(() => _confirmando = true);
    final base = _base!;
    final si = await confirmarEnHoja(
      context,
      titulo: TextosDeLaSubasta.confirmarTitulo,
      detalle: TextosDeLaSubasta.confirmarDetalle(
        formatearMonto(monto: base, moneda: 'BOB'),
        _horas,
      ),
      textoConfirmar: TextosDeLaSubasta.confirmar,
    );
    if (!mounted) return;
    setState(() => _confirmando = false);
    if (si) {
      await ref.read(abrirSubastaProvider.notifier).enviar((
        grupoId: widget.grupoId,
        cupoId: widget.cupoId,
        montoBase: base,
        horas: _horas,
      ));
    }
  }

  @override
  Widget build(BuildContext context) {
    final t = Tokens.of(context);
    final envio = ref.watch(abrirSubastaProvider);
    final rechazo = envio.hasError ? rechazoDeLaSubasta(envio.error!) : null;
    final motivo = _base == null ? TextosDeLaSubasta.escribiElPrecio : null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        Panel(
          hijo: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                TextosDeLaSubasta.comoFunciona,
                style: Tipo.cuerpoFuerte.copyWith(color: t.text),
              ),
              for (final (i, regla) in TextosDeLaSubasta.reglas.indexed)
                Padding(
                  padding: const EdgeInsets.only(top: Espacio.s2),
                  child: Text(
                    '${i + 1}. $regla',
                    style: Tipo.cuerpoChico.copyWith(color: t.text2),
                  ),
                ),
            ],
          ),
        ),
        const SizedBox(height: Espacio.s4),
        CampoMonto(
          key: const ValueKey('precio-base'),
          etiqueta: TextosDeLaSubasta.precioBase,
          ayuda: TextosDeLaSubasta.ayudaBase,
          onChanged: (m) => setState(() => _base = m),
        ),
        const SizedBox(height: Espacio.s4),
        Text(
          TextosDeLaSubasta.duracion,
          style: Tipo.campoEtiqueta.copyWith(color: t.text),
        ),
        const SizedBox(height: Espacio.s2),
        SelectorSegmentado<int>(
          opciones: TextosDeLaSubasta.horas,
          valor: _horas,
          onChanged: (h) => setState(() => _horas = h),
        ),
        const SizedBox(height: Espacio.s4),
        if (rechazo != null) ...[
          Alerta(
            titulo: rechazo.mensaje,
            detalle: rechazo.traza == null
                ? null
                : 'Referencia ${rechazo.traza}',
            tono: Tono.error,
          ),
          const SizedBox(height: Espacio.s3),
        ],
        Boton(
          key: const ValueKey('poner-en-subasta'),
          texto: TextosDeLaSubasta.ponerEnSubasta,
          variante: BotonVariante.primario,
          expandido: true,
          cargando: envio.isLoading,
          motivoDeBloqueo: motivo,
          onPressed: motivo == null && !envio.isLoading && !_confirmando
              ? _poner
              : null,
        ),
      ],
    );
  }
}
