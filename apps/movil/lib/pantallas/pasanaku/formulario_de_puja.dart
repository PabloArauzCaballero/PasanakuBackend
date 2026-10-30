import 'package:aportaya_cliente_entregas/aportaya_cliente_entregas.dart';
import 'package:aportaya_diseno/atomos/boton.dart';
import 'package:aportaya_diseno/atomos/boton_variante.dart';
import 'package:aportaya_diseno/atomos/campo_monto.dart';
import 'package:aportaya_diseno/atomos/casilla.dart';
import 'package:aportaya_diseno/atomos/tono.dart';
import 'package:aportaya_diseno/moleculas/alerta.dart';
import 'package:aportaya_diseno/tokens/tokens.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import 'dominio/subasta_del_cupo.dart';
import 'errores_de_la_subasta.dart';
import 'tarjeta_de_subasta.dart';
import 'textos_de_la_subasta.dart';

/// Ofrecer por un cupo: el monto (con el mínimo que hoy se acepta), aceptar las reglas
/// del grupo y una hoja que dice cuánto se retiene. El botón apagado dice por qué; un
/// rechazo se muestra arriba del botón y lo escrito se conserva.
class FormularioDePuja extends ConsumerStatefulWidget {
  const FormularioDePuja({super.key, required this.subasta});
  final VistaDeSubasta subasta;

  @override
  ConsumerState<FormularioDePuja> createState() => _FormularioDePujaState();
}

class _FormularioDePujaState extends ConsumerState<FormularioDePuja> {
  String? _monto;
  var _acepto = false;
  var _confirmando = false;

  String? get _motivo => _monto == null
      ? TextosDeLaSubasta.faltaMonto
      : !_acepto
      ? TextosDeLaSubasta.faltaAceptar
      : null;

  Future<void> _ofrecer() async {
    if (_confirmando || _motivo != null) return;
    setState(() => _confirmando = true);
    final monto = _monto!;
    final si = await confirmarEnHoja(
      context,
      titulo: TextosDeLaSubasta.ofrecerTitulo,
      detalle: TextosDeLaSubasta.ofrecerDetalle(
        bs(Dinero(monto: monto, moneda: widget.subasta.montoMinimo.moneda)),
      ),
      textoConfirmar: TextosDeLaSubasta.siOfrecer,
    );
    if (!mounted) return;
    setState(() => _confirmando = false);
    if (si) {
      await ref
          .read(pujarProvider.notifier)
          .enviar(widget.subasta.subastaId, monto);
    }
  }

  @override
  Widget build(BuildContext context) {
    final envio = ref.watch(pujarProvider);
    final estado = envio.value?.estado;
    final rechazo = envio.hasError ? rechazoDeLaSubasta(envio.error!) : null;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        CampoMonto(
          key: const ValueKey('monto-de-la-puja'),
          etiqueta: TextosDeLaSubasta.tuOferta,
          ayuda: TextosDeLaSubasta.minimo(bs(widget.subasta.montoMinimo)),
          moneda: widget.subasta.montoMinimo.moneda.value,
          onChanged: (m) => setState(() => _monto = m),
        ),
        const SizedBox(height: Espacio.s3),
        Casilla(
          key: const ValueKey('acepto-reglas'),
          etiqueta: TextosDeLaSubasta.aceptoReglamento,
          detalle: TextosDeLaSubasta.detalleReglamento,
          valor: _acepto,
          onChanged: (v) => setState(() => _acepto = v),
        ),
        const SizedBox(height: Espacio.s3),
        if (estado == SalidaPujaEstadoEnum.VIGENTE)
          const Alerta(titulo: TextosDeLaSubasta.ganando, tono: Tono.ok),
        if (estado == SalidaPujaEstadoEnum.PENDIENTE)
          const Alerta(titulo: TextosDeLaSubasta.pendiente, tono: Tono.aviso),
        if (rechazo != null)
          Alerta(
            titulo: rechazo.mensaje,
            detalle: rechazo.traza == null ? null : 'Referencia ${rechazo.traza}',
            tono: Tono.error,
          ),
        const SizedBox(height: Espacio.s3),
        Boton(
          key: const ValueKey('ofrecer'),
          texto: TextosDeLaSubasta.ofrecer,
          variante: BotonVariante.primario,
          expandido: true,
          cargando: envio.isLoading,
          motivoDeBloqueo: _motivo,
          onPressed: _motivo == null && !envio.isLoading && !_confirmando
              ? _ofrecer
              : null,
        ),
      ],
    );
  }
}
